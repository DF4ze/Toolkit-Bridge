# Analyse — Mapping `projectName → projectPath` + optimisation appel Codex CLI (Phase 8)

Date: 2026-05-03  
Projet: `Toolkit-Bridge` / sous-système `CodexTime`

## 1) Résumé exécutif

CodexTime supporte déjà un `workingDirectory` côté exécution Codex via la variable de contexte `codexWorkingDirectory`, mais l’intégration Telegram ne fournit aujourd’hui **aucun mécanisme explicite** pour résoudre un `projectName` vers un chemin disque fiable. Résultat : même si `/workflow_run project=...` existe, Codex peut s’exécuter dans un répertoire par défaut (ou incohérent) si `codexWorkingDirectory` n’est jamais défini.

Cette analyse propose une extension **minimale et testable** :

- Ajouter une commande Telegram **`/create_project name=<...> path=<...>`** (format `key=value`, cohérent avec le parsing actuel).
- Persister le mapping en DB (SQLite déjà utilisée via JPA `ddl-auto=update`) avec un modèle simple `projectName`, `projectPath`, timestamps.
- Résoudre `projectPath` au moment de `/workflow_run` (et `/workflow_resume`) et injecter `codexWorkingDirectory` dans `WorkflowExecutionContext.variables`.
- Cibler un appel Codex CLI plus robuste pour l’automatisation : `codex exec` en mode non-interactif, avec `--cwd/-C`, prompt via stdin, et options `--sandbox workspace-write`, `--json`, `--output-last-message`.

Conclusion : **prêt pour implémentation**, sous réserve de cadrer la validation minimale du path (sécurité) et de choisir un format d’arguments Telegram compatible avec des chemins contenant des espaces.

## 2) État actuel — Telegram workflow (existant)

### 2.1 Commandes et parsing

`WorkflowTelegramController` parse les arguments au format **`key=value`** (`argValue(...)`) :

- `/workflow_run` : `project|projectName`, `phase`, `etape|step`
- `/workflow_roadmap_load` : `project|projectName`, `path`

Ce parsing est simple et robuste… tant que les valeurs **ne contiennent pas d’espaces** (sinon elles sont séparées en plusieurs `args`).

### 2.2 Résolution du target `/workflow_run`

`WorkflowTelegramRunTargetResolver.resolveRunTarget(...)` :

- résout un triplet `projectName + phase + etape` à partir des args + session,
- applique des règles explicites (combinaisons partielles refusées),
- ne gère pas de `projectPath`.

### 2.3 Construction du contexte workflow (Telegram orchestration)

`WorkflowTelegramOrchestrationService` construit `WorkflowExecutionContext.variables` :

- `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`
- `analysisSourcePath` (RUN seulement)
- `roadmapPath` si présent en session
- éventuellement `llmProvider` / `llmModel` résolus via `AgentDefinitionService`

Point important : **aucune variable `codexWorkingDirectory` n’est injectée** par la voie Telegram.

## 3) État actuel — Exécution Codex (existant)

### 3.1 Le système supporte déjà un working directory

`CodexExecutionRequest` contient déjà :

- `prompt`
- `workingDirectory` (`Path`)
- `timeoutSeconds`

Les steps (ex: `GlobalAnalysisStep`) passent :

- `optionalPath(context, "codexWorkingDirectory")`
- `optionalInt(context, "codexTimeoutSeconds")`

Donc : **le pipeline workflow est déjà prêt** à recevoir un `codexWorkingDirectory` depuis le contexte.

### 3.2 Limites actuelles de l’appel CLI Codex

`CodexWorkflowClient` :

- utilise un binaire hardcodé Windows : `C:\\Users\\Skill Korp\\AppData\\Roaming\\npm\\codex`
- construit la commande comme : `codex <prompt>`
- ne force pas `codex exec`
- n’utilise pas `--cwd/-C` (mais un `ProcessBuilder.directory(...)` si `workingDirectory` est fourni)
- n’injecte pas le prompt via stdin (risque de quoting / longueur / caractères)
- ne profite pas du JSONL, ni de `--output-last-message`

## 4) Documentation Codex CLI pertinente (référence)

Les docs Codex CLI décrivent :

- `codex exec` comme mode **non-interactif** (automation/CI), avec prompt via argument ou stdin.
- `--json` pour émettre des événements JSONL.
- `-C, --cwd` pour définir le working directory.
- `--sandbox workspace-write` (et `--full-auto` équivalent à sandbox workspace-write selon la doc “Exec mode”).
- `--output-last-message` (alias `-o`) pour écrire la dernière réponse dans un fichier.

Références (docs) :

- https://www.mintlify.com/openai/codex/cli/exec
- https://www.mintlify.com/openai/codex/advanced/exec-mode
- https://www.mintlify.com/openai/codex/concepts/non-interactive-mode

## 5) Modèle DB recommandé — mapping projet

### Option recommandée (simple, dédiée)

Créer une entité dédiée (ex : `WorkflowProjectEntity` ou `CodexProjectEntity`) :

- `id` (Long, identity)
- `projectName` (unique, normalisé, case-insensitive idéalement mais SQLite + simplicité → au minimum `trim()` + conserver case)
- `projectPath` (String, chemin absolu ou canonical)
- `createdAt`, `updatedAt`
- index unique sur `projectName`

Repository : `findByProjectName(String)`.

Upsert (service) :

- `findByProjectName(...)`
- si absent → create
- si présent → update path (+ updatedAt)

Pourquoi : lecture/écriture simple, testable, et indépendant des autres systèmes.

### Option alternative (réutiliser `AdministrableConfigurationEntity`)

Stocker `projectName → projectPath` dans `administrable_configuration.payload_json` (ex: JSON map).

Avantage : pas de table supplémentaire.  
Inconvénients : sérialisation JSON à gérer, risques de merge concurrent, moins requêtable.

Recommandation : **entité dédiée**.

## 6) Sécurité minimale pour `projectPath`

Objectifs :

- éviter les chemins invalides / fichiers au lieu de répertoires,
- réduire le risque d’exécution Codex dans un endroit dangereux,
- éviter l’injection shell (déjà mitigée si `ProcessBuilder(List<String>)` est respecté).

Validation minimale recommandée côté service (sans complexifier) :

1. `Path.of(input).toAbsolutePath().normalize()` (capturer `InvalidPathException`).
2. `Files.exists` + `Files.isDirectory` + `Files.isReadable` (+ éventuellement `isWritable` si `workspace-write` attendu).
3. Option “safe-by-default” : exiger que le path soit **sous un root autorisé** (ex : `workspace/shared/projects/` ou une liste de roots configurables).
4. Ne jamais exécuter via `cmd /c ...` : conserver `List<String>` pour `ProcessBuilder`.

Note UX Telegram : ne pas afficher le chemin absolu dans les messages standards. Afficher uniquement :

- `Project: <name>` (et éventuellement `Project path: configured` en mode debug).

## 7) Commande Telegram `/create_project` — format recommandé

### Contrainte importante : paths avec espaces

Le parsing actuel (`ctx.getArgs()` + split par espaces) rend les chemins contenant des espaces difficiles sans support de quoting par la lib Telegram (non confirmé ici).

Recommandation pragmatique (robuste immédiatement) :

- Garder le format `key=value` (cohérent avec `/workflow_run` et `/workflow_roadmap_load`)
- Recommander des chemins sans espaces, ou des chemins “courts” Windows (`PROGRA~1`) si nécessaire
- Documenter la limitation tant qu’un parsing “reste de ligne” n’est pas ajouté

Syntaxe recommandée :

```text
/create_project name=ToolkitBridge path=D:\Documents\Spring\Toolkit-Bridge
```

Erreurs à gérer :

- name manquant / vide
- path manquant / vide
- path invalide / inexistant / non dossier

Message succès (sans fuite de path) :

```text
✅ Action effectuée
Detail: Project registered: ToolkitBridge
Next: /workflow_run project=ToolkitBridge phase=... etape=...
```

Message upsert (si remplace) :

```text
✅ Action effectuée
Detail: Project updated: ToolkitBridge
Next: /workflow_run project=ToolkitBridge ...
```

## 8) Résolution `projectName → projectPath` dans `/workflow_run`

### Règle proposée

- Si `/workflow_run project=...` est fourni :
  - lookup en DB,
  - si absent → refuser (message clair + “Try: /create_project ...”),
  - si présent → injecter `codexWorkingDirectory` dans le contexte RUN.
- Si `/workflow_run` est appelé sans `project=` :
  - conserver la logique actuelle via session,
  - si la session a `projectName` mais pas de `projectPath` en mémoire, relookup DB (ou refuser si absent).

### Stockage en session (optionnel)

Ajouter `projectPath` en `WorkflowTelegramSession` peut :

- réduire les queries DB,
- rendre le run stable si mapping change en cours.

Mais contrainte UX : ne pas l’afficher tel quel dans `/workflow` et `/workflow_status`.

Recommandation : **oui, stocker en session** (type `Path`) pour figer le contexte du run, mais ne jamais l’exposer en UX standard.

## 9) Propagation dans le contexte workflow (RUN + RESUME)

### Cible technique

Injecter `codexWorkingDirectory` (variable existante) dans :

- `WorkflowTelegramOrchestrationService.buildContext(...)`
- `WorkflowTelegramOrchestrationService.buildResumeContext(...)`

Source de vérité :

- DB mapping (au moment de startRun/resume), ou `WorkflowTelegramSession.projectPath` si présent.

Effet attendu :

- `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`, `BuildErrorCorrectionStep` passeront automatiquement le `workingDirectory` au `CodexExecutionRequest`,
- `CodexWorkflowClient` exécutera dans ce répertoire via `ProcessBuilder.directory(...)`.

## 10) Optimisation recommandée de l’appel Codex CLI

### Cible (automation stable)

D’après la doc CLI, viser :

- `codex exec` (non-interactif)
- prompt via stdin (`codex exec -`)
- `--cwd <projectPath>` ou `-C <projectPath>`
- `--sandbox workspace-write` (ou `--full-auto` si souhaité)
- `--json` si l’on veut un flux exploitable (optionnel selon besoin)
- `--output-last-message <file>` (recommandé pour capturer proprement le résultat final)

Exemples :

```text
codex exec -C <projectPath> --sandbox workspace-write -o <resultFile> -
```

ou, si besoin d’événements :

```text
codex exec -C <projectPath> --sandbox workspace-write --json -o <resultFile> -
```

### Impacts attendus (si implémenté plus tard)

- meilleure robustesse Windows (moins de quoting),
- récupération plus fiable du “dernier message” sans parser stdout,
- possibilité future de suivre la progression via JSONL.

### Points à vérifier avant implémentation

- compatibilité de la version installée du binaire `codex`,
- comportement exact de `--sandbox workspace-write` sur le repo ciblé,
- format exact des événements JSONL si on souhaite les exploiter.

## 11) Tests recommandés (si implémentation)

### DB / service project registry

- création nouveau mapping
- upsert mapping existant (remplacement path)
- refus path inexistant
- refus path fichier au lieu de dossier

### Commande Telegram `/create_project`

- parsing `name` manquant → erreur
- parsing `path` manquant → erreur
- succès création + message
- succès update + message

### `/workflow_run` et injection `codexWorkingDirectory`

- `/workflow_run project=Unknown` → refus + “Try: /create_project …”
- `/workflow_run project=Known` → contexte contient `codexWorkingDirectory`
- session conserve `projectName` et (optionnellement) `projectPath` sans fuite en UX

### Codex CLI invocation (tests unitaires)

- `CodexWorkflowClient.buildCommand(...)` produit `codex exec ...` (si refactor vers exec)
- prompt envoyé via stdin (testable en isolant la construction de commande + “request payload”)
- working directory appliqué correctement

## 12) Risques & dérives à éviter

- Ne pas mélanger ce mapping avec la session Telegram de manière irréversible (la DB est source de vérité).
- Ne pas exposer les chemins absolus dans `/workflow` / `/workflow_status`.
- Ne pas refondre massivement `CodexWorkflowClient` : garder une migration progressive (ex: d’abord `--cwd` + stdin, puis JSONL).
- Ne pas introduire de WebUI sur ce sujet dans cette phase.
- Ne pas créer une “configuration globale” exhaustive ici : rester sur `projectName → projectPath`.

## 13) Plan d’implémentation recommandé (5–10 étapes)

1. Ajouter entité DB `CodexProjectEntity` + repository.
2. Ajouter service `ProjectRegistryService` (upsert + validation path).
3. Ajouter commande Telegram `/create_project` (controller + service).
4. Résoudre `projectPath` dans `/workflow_run` (lookup DB, erreurs claires).
5. Stocker `projectPath` en session (optionnel mais recommandé), sans l’afficher.
6. Injecter `codexWorkingDirectory` dans `buildContext` et `buildResumeContext`.
7. Améliorer `CodexWorkflowClient` : passer à `codex exec`, prompt via stdin, `-C/--cwd`, `--sandbox workspace-write`, `-o`.
8. Ajouter tests unitaires (DB/service + orchestration + client command builder).
9. Mettre à jour la documentation (UserGuide + doc Telegram) une fois validé.

## 14) Conclusion

Statut : **prêt pour implémentation**.

Les fondations existent déjà (`codexWorkingDirectory` supporté), il manque principalement :

- la persistance et la commande UX pour créer le mapping projet,
- la résolution et l’injection du path dans le contexte Telegram,
- une mise à niveau de l’appel Codex CLI vers `codex exec` pour un mode non-interactif stable.

