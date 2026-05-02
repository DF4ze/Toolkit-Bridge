# UserGuide — CodexTime (Toolkit-Bridge)

## 1) Présentation rapide

CodexTime est un workflow d’implémentation assisté par Codex, intégré à Toolkit-Bridge.

Il automatise :
- l’exécution d’un cycle **analyse → review → correction (optionnelle)**,
- la production d’artefacts Markdown (prompts + résultats),
- la génération d’un `workflow-summary.md` (interface humaine),
- des arrêts contrôlés (`WAIT_HUMAN`) et une reprise (`RESUME`),
- une validation technique locale (Maven `clean compile -DskipTests`) et une auto-correction bornée des erreurs de build,
- un pilotage distant via Telegram (dashboard + commandes).

Il ne fait pas encore (limites connues) :
- persistance durable des runs/sessions (état Telegram en mémoire),
- WebUI dédiée au pilotage workflow,
- multi-user avancé / rôles / audit trail complet,
- parsing “intelligent” de roadmap (au-delà du chargement sécurisé),
- fallback provider robuste (rate-limit/quota gérés par heuristiques côté Telegram).

Différences à connaître :
- **CodexTime** : workflow/orchestration (Java) + artefacts + décisions.
- **Codex** : exécutant (LLM/CLI) appelé par certaines étapes.
- **Telegram** : interface utilisateur distante (adapter), ne remplace pas le moteur.
- **Toolkit-Bridge** : l’application qui héberge l’ensemble (runtime, WebUI admin/technique, Telegram, etc.).

Schéma simplifié :

```text
Roadmap / Prompt
   ↓
CodexTime Runner
   ↓
Codex + Validation locale (Maven)
   ↓
Reports + workflow-summary.md
   ↓
Telegram / CLI
```

---

## 2) Prérequis

Obligatoire (usage local) :
- Java (version exacte : à confirmer dans le projet ; vérifier `pom.xml` / toolchain).
- Projet `Toolkit-Bridge` compilable.
- Maven Wrapper présent (`mvnw` / `mvnw.cmd`) si vous utilisez la validation Maven intégrée (Phase 6).

Obligatoire si vous exécutez les steps Codex (analyse/review/correction) :
- Codex CLI disponible sur la machine qui exécute la JVM (selon l’implémentation de `CodexWorkflowClient`).

Optionnel (usage Telegram) :
- Telegram activé et configuré (voir `src/main/resources/application.yml`).
- Whitelist utilisateur Telegram non vide recommandée.
- Utilisation recommandée en **chat privé**, pas en groupe (les messages/summaries sont visibles par le chat).

Workspace / dossiers :
- `workspace/shared` doit exister (racine “shared” utilisée pour charger des roadmaps).
- Le dossier des rapports existe (par défaut `data/rapport`, configurable côté Telegram workflow).

---

## 3) Configuration

### 3.1 Configuration du module Telegram (`telegram.*`)

Fichier de référence : `src/main/resources/application.yml`.

Structure (extrait type) :

```yaml
telegram:
  enabled: true
  default-bot-id: Cortex
  bots:
    - id: Cortex
      token: "<TELEGRAM_BOT_TOKEN>"
      polling-enabled: true
      auto-register-commands: true
      configure-menu-button: true
      security:
        allowed-user-ids:
          - 123456789
```

Points importants :
- Les commandes workflow Telegram sont rattachées au bot `Cortex` via `@TelegramController(bot = "Cortex")`.
- La whitelist se fait via `telegram.bots[].security.allowed-user-ids` (filtrage avant appel des handlers).
- Une whitelist vide est risquée (peut ouvrir l’accès selon le comportement du module) : un warning est loggé au démarrage.
- Chat privé recommandé : en groupe, le bot poste dans le chat et les summaries peuvent contenir des infos sensibles.

Note sécurité :
- `src/main/resources/application.yml` contient actuellement des tokens en clair dans ce repo (constat factuel). En environnement réel, stocker ces secrets via un mécanisme de secrets (env var, fichier hors VCS, vault, etc.).

### 3.2 Configuration workflow Telegram (`toolkit.telegram.workflow.*`)

Classe de binding : `src/main/java/fr/ses10doigts/toolkitbridge/config/telegram/workflow/WorkflowTelegramProperties.java`.

Propriétés :

```yaml
toolkit:
  telegram:
    workflow:
      reportRootDirectory: "data/rapport"
      reportVersion: "v1.1"
      timeoutMinutes: 15
```

Différence clé :
- `telegram.*` configure le **module Telegram** (bots, tokens, polling, whitelist).
- `toolkit.telegram.workflow.*` configure le **runtime workflow côté Telegram** (racine rapports, version, timeout).

### 3.3 Configuration Codex / validation (CLI et runtime)

CLI CodexTime (Phase 5) :
- `--codexWorkingDirectory=<path>` (optionnel)
- `--codexTimeoutSeconds=<seconds>` (optionnel)

Validation Maven (Phase 6) :
- Maven Wrapper utilisé (Windows : `cmd.exe /c mvnw.cmd ...`, Unix : `./mvnw ...`).
- Timeout et working directory : à confirmer dans le code des steps/runner si vous souhaitez les modifier (sinon, utiliser les defaults).

---

## 4) Organisation des fichiers

Racine des rapports CodexTime :
- `data/rapport/v1.1/CodexTime/`

Phases (exemples réels) :
- `data/rapport/v1.1/CodexTime/phase1/`
- `data/rapport/v1.1/CodexTime/phase7/`
- (attention aux variations historiques : `Phase7` vs `phase7` dans certains rapports/chemins Windows)

Rapport final de phase :
- convention principale : `data/rapport/v1.1/CodexTime/phaseX/z_finalRepport.md`
- exception constatée : Phase 5 utilise `data/rapport/v1.1/CodexTime/phase5/z_finalReport.md` (orthographe différente).

Summary :
- la CLI et le runner écrivent un `workflow-summary.md` à un chemin dérivé du contexte :
  - `reportRootDirectory/reportVersion/reportPhase/workflow-summary.md`
  - Exemple typique : `data/rapport/v1.1/CodexTime/phase6/.../workflow-summary.md` (selon contexte exact ; à confirmer au run).

Workspace (fichiers utilisateur) :
- `workspace/shared/` : zone “shared” pour roadmaps et fichiers manipulables.
- Roadmaps : recommandé sous `workspace/shared/projects/{ProjectName}/roadmaps/`.

Qui écrit/qui lit :
- Runner / steps : écrivent prompts, résultats, summary.
- CLI : lance le runner et imprime un contrat `key=value`.
- Telegram : lit la roadmap depuis `workspace/shared` et lit le summary depuis le report root (accès borné).
- Utilisateur : prépare/modifie la roadmap, et modifie certains artefacts en cas de `WAIT_HUMAN` (ex: review result).

---

## 5) Artefacts générés (principaux)

CodexTime produit des artefacts Markdown dans `data/rapport/...` (selon phase/étape).

Artefacts usuels :
- `X.analysis.md` : prompt d’analyse (input utilisateur / prompt codé).
- `result_X_analysis.md` : résultat d’analyse.
- `X.implements.md` : prompt d’implémentation.
- `result_X_implements.md` : résultat implémentation.
- `X.correction.md` : prompt de correction.
- `result_X_correction.md` : résultat correction.
- `result_X_review.md` / `result.X_review.md` : revues (naming historique variable).
- `workflow-summary.md` : summary humain (format standardisé en Phase 6).
- `z_finalRepport.md` / `z_finalReport.md` : bilan final de phase.

Artefacts validation (Phase 6) :
- Build result Maven : écrit en artefact (nom exact dépend de `WorkflowArtifactType` ; à confirmer dans le code si vous en avez besoin).
- Correction build : artefact `BUILD_ERROR_CORRECTION` (contient aussi `Attempt: X / Y`).

Comment les utiliser :
- `workflow-summary.md` : point d’entrée humain : statut, raison, actions, artefacts à consulter.
- Les `result_*` : détails (diagnostic, décisions, correctifs).
- Les `z_final*` : synthèse consolidée pour refactor roadmaps / décisions.

---

## 6) Utilisation via CLI (CodexTime CLI)

Point d’entrée :
- Classe : `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowCli`
- Fichier : `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCli.java`

Référence complète :
- `data/rapport/v1.1/CodexTime/phase5/cli-reference.md`

### 6.1 Modes

- `RUN` : exécute analyse → review → correction optionnelle.
- `RESUME` : reprend après `WAIT_HUMAN` (exécute uniquement la correction via `runCorrectionAfterReview`).

### 6.2 Arguments obligatoires

Tous modes :
- `--mode=RUN|RESUME`
- `--reportRootDirectory=<path>`
- `--reportVersion=<version>` (ex: `v1.1`)
- `--reportPhase=<phase>` (ex: `phase5`)
- `--stepNumber=<number>` (entier > 0)

Mode RUN uniquement :
- `--analysisSourcePath=<path>`

Optionnels (selon `cli-reference.md`) :
- `--codexWorkingDirectory=<path>`
- `--codexTimeoutSeconds=<seconds>`
- `--runId=<id>`
- `--workflowType=<type>`
- `--targetStepRef=<ref>`

### 6.3 Exemple complet (RUN)

Exemple (forme générique, classpath à adapter) :

```bash
java -cp <classpath> fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowCli \
  --mode=RUN \
  --reportRootDirectory=data/rapport \
  --reportVersion=v1.1 \
  --reportPhase=CodexTime/phase7/etape2C \
  --stepNumber=2 \
  --analysisSourcePath=data/rapport/v1.1/CodexTime/phase7/etape2C/2.analysis.md
```

À confirmer dans le code / votre contexte :
- la valeur attendue de `reportPhase` dépend de la convention de votre runner (certains chemins utilisent `CodexTime/PhaseX/etapeY`).

### 6.4 Exemple complet (RESUME)

```bash
java -cp <classpath> fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowCli \
  --mode=RESUME \
  --reportRootDirectory=data/rapport \
  --reportVersion=v1.1 \
  --reportPhase=CodexTime/phase7/etape2C \
  --stepNumber=2
```

Règle importante :
- `--reportRootDirectory`, `--reportVersion`, `--reportPhase`, `--stepNumber` doivent être identiques entre RUN et RESUME.

### 6.5 Codes de sortie

- `0` : décision produite (pas nécessairement “succès final”).
- `1` : erreur runtime.
- `2` : argument invalide.

### 6.6 Format stdout `key=value`

La CLI imprime des lignes :
- `decision=<...>`
- `message=<...>`
- `finalDecision=<...>`
- `nextAction=<...>`
- `correctionTriggered=<true|false>`
- (optionnel) `waitReason=<...>`
- (optionnel) `workflowSummaryPath=<...>`

---

## 7) Utilisation via Telegram

Contrôleur :
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`

Commandes (exactes) :
- `/workflow`
- `/workflow_roadmap_load`
- `/workflow_run`
- `/workflow_status`
- `/workflow_summary`
- `/workflow_resume`

### 7.1 `/workflow` (dashboard)

Objectif :
- afficher un tableau de bord : status, project, phase, etape, roadmap, commandes et “Next”.

Préconditions :
- aucune (fonctionne même sans contexte, mais affichera “(not set)”).

Prochaine action :
- dépend du statut (ex: `IDLE` → `/workflow_roadmap_load` ou `/workflow_run`).

### 7.2 `/workflow_roadmap_load`

Syntaxe :
- `/workflow_roadmap_load path=<relativePath>`
- optionnel : `project=<ProjectName>` (ou `projectName=...`)

Exemple (recommandé) :
- `/workflow_roadmap_load project=ToolkitBridge path=projects/ToolkitBridge/roadmaps/phase7.md`

Règles (code) :
- `path` est **relatif à `workspace/shared`**.
- le fichier doit exister, être lisible, être un `.md` et non vide.
- la résolution est sécurisée (pas d’accès hors `workspace/shared`).

Erreurs fréquentes :
- `Roadmap not found`
- `Roadmap must be a .md file`
- `Roadmap is empty`
- `Invalid roadmap path`

### 7.3 `/workflow_run`

Syntaxes acceptées (code) :
- `/workflow_run project=<name> phase=<n> etape=<n>`
- `/workflow_run phase=<n> etape=<n>` (si `projectName` déjà en session)
- `/workflow_run phase=<n>` (utilise `etape` en session si même phase, sinon default `1`)
- `/workflow_run` (réutilise le contexte en session ; sinon erreur explicite)

Exemples :
- `/workflow_run project=ToolkitBridge phase=7 etape=2`
- `/workflow_run phase=7 etape=2` (si projet déjà sélectionné)
- `/workflow_run phase=7` (lance étape 1 par défaut si pas déjà en session)

Réponses attendues :
- message immédiat (asynchrone) ; “Next” guide généralement vers `/workflow_status`.

### 7.4 `/workflow_status`

Objectif :
- vue synthétique de la session de workflow Telegram.

Statuts possibles (Telegram) :
- `IDLE`
- `RUNNING`
- `COMPLETED`
- `WAITING_HUMAN`
- `FAILED`

Champs affichés :
- project, phase, etape
- roadmap: loaded/missing
- summary: available/missing
- runId, startedAt, updatedAt
- lastMessage / lastError (sanitisés)
- Next action recommandée

### 7.5 `/workflow_summary`

Objectif :
- afficher le contenu du `workflow-summary.md` associé à la session courante.

Comportement (code) :
- summary court : 1 message.
- summary moyen : 2 messages maximum (chunks).
- summary long : envoi d’un document (`workflow-summary.md`) si un sender Telegram est disponible.
- si document impossible : message d’erreur explicite (“Complete file could not be sent”).

Erreurs fréquentes :
- `No summary available` (aucun run n’a produit de summary en session)
- `Summary not found on disk`
- `Summary is empty`

### 7.6 `/workflow_resume`

Objectif :
- reprendre un workflow **uniquement** si la session est en `WAITING_HUMAN`.

Préconditions (code) :
- lastStatus == `WAITING_HUMAN`
- running == false
- phase != null, etape != null

Comportement :
- appelle le runner in-process via `runCorrectionAfterReview(context)` (pas de CLI/process).
- ne fait pas d’auto-advance de phase/étape.

Erreurs fréquentes :
- `Aucun workflow en attente d'action humaine`
- `Contexte workflow incomplet`
- `Workflow already running`

---

## 8) Cycle utilisateur complet (3 scénarios)

### Scénario A — Run nominal terminé

1) Charger la roadmap (optionnel mais recommandé) :
- `/workflow_roadmap_load project=ToolkitBridge path=projects/ToolkitBridge/roadmaps/<file>.md`

2) Lancer le workflow :
- `/workflow_run project=ToolkitBridge phase=7 etape=1`

3) Suivre :
- `/workflow_status` jusqu’à `COMPLETED`

4) Lire le summary :
- `/workflow_summary`

5) Inspecter les artefacts :
- dans `data/rapport/v1.1/CodexTime/...` (selon phase/étape).

### Scénario B — `WAITING_HUMAN` puis resume

1) Lancer un run :
- `/workflow_run project=ToolkitBridge phase=<n> etape=<n>`

2) Obtenir `WAITING_HUMAN` :
- `/workflow_status` indique `WAITING_HUMAN`

3) Lire le summary :
- `/workflow_summary` (il indique quoi faire : fichier à inspecter/éditer, puis reprise)

4) Effectuer l’action humaine (typiquement : éditer un artefact review result ou agir sur build failure persistant).

5) Reprendre :
- `/workflow_resume`

6) Vérifier le statut final :
- `/workflow_status` puis `/workflow_summary`.

### Scénario C — Échec / quota / rate-limit

1) Lancer :
- `/workflow_run ...`

2) Si `FAILED` :
- `/workflow_status` (voir `Last error`)

3) Si le message mentionne quota/rate-limit :
- attendre et relancer plus tard,
- ou changer provider/model (selon configuration globale).

Note :
- Les messages d’erreur visibles dans Telegram sont sanitisés (pas de stacktrace, pas de paths absolus).

---

## 9) Comprendre `WAIT_HUMAN`

`WAIT_HUMAN` signifie : le workflow s’arrête volontairement car une action/décision humaine est requise.

Où lire la raison :
- CLI : champ `waitReason` (si présent) et `workflowSummaryPath`.
- Telegram : `/workflow_status` + `/workflow_summary`.

Comment savoir quoi modifier :
- ouvrir le `workflow-summary.md` : section “Actions” et “Artifacts”.

Comment reprendre :
- CLI : relancer en `--mode=RESUME` avec le même contexte.
- Telegram : utiliser `/workflow_resume` (uniquement en `WAITING_HUMAN`).

Ce qu’il ne faut pas faire :
- relancer un `/workflow_run` “au hasard” sans contexte,
- spamer `/workflow_resume` si ce n’est pas `WAITING_HUMAN`,
- traiter un `WAIT_HUMAN` comme un bug : c’est un stop contrôlé.

---

## 10) Comprendre `workflow-summary.md`

Rôle :
- interface humaine principale : explique le statut, la raison, le contexte, les actions à faire, et les artefacts.

Format (Phase 6) :

```text
Status: ...
Reason: ...

Context:
...

Actions:
1. ...
2. ...

Artifacts:
- ...
```

Où il se trouve :
- CLI : `workflowSummaryPath=<...>` dans stdout (si écrit).
- Telegram : path mémorisé en session, consultable via `/workflow_summary`.

Comment Telegram l’utilise :
- il lit le fichier depuis `toolkit.telegram.workflow.reportRootDirectory` (racine autorisée) et applique la stratégie chunk/document.

---

## 11) Validation locale et Maven (Phase 6)

Rôle :
- `WorkflowValidationService` : exécute une commande locale contrôlée, retourne un résultat structuré.
- `MavenValidationStep` : adapte Maven (wrapper) au workflow via `WorkflowStepResult`.

Validation exécutée :
- `clean compile -DskipTests` via `mvnw`/`mvnw.cmd`.

Auto-correction build :
- en cas d’échec de compilation, le workflow peut déclencher un retry borné :
  - `RETRY_CORRECTION` → `BuildErrorCorrectionStep` → revalidation Maven.

Arrêt :
- si l’échec persiste après retry max : conversion en `WAIT_HUMAN` (actionnable).

Ce que l’utilisateur voit :
- Telegram : `WAITING_HUMAN` + summary “build instructions” si le build reste en échec après auto-correction.
- Artefacts : build result + correction attempt (voir section “Artifacts” du summary).

---

## 12) Erreurs et diagnostics (pratique)

Erreurs fréquentes côté Telegram :
- Roadmap introuvable : `Roadmap not found` → vérifier `workspace/shared/<path>`.
- Path non autorisé : `Invalid roadmap path` → le path doit rester sous `workspace/shared`.
- Summary absent : `No summary available` → lancer d’abord `/workflow_run`.
- Summary introuvable : `Summary not found on disk` → fichier supprimé ou racine rapports différente.
- Workflow already running : un run est déjà en cours → attendre ou consulter `/workflow_status`.
- Contexte incomplet : `/workflow_run` sans contexte → fournir `project/phase/etape` ou charger la roadmap.
- Quota/rate-limit : message friendly → attendre / changer provider.

Ce qui est affiché dans Telegram :
- messages courts, sanitisés (pas de stacktrace, pas de chemins absolus, longueur bornée).

Ce qui est loggé côté serveur :
- exceptions complètes avec contexte (chatId, runId, phase, etape, etc.).

---

## 13) Sécurité

Contrôle d’accès :
- whitelist via `telegram.bots[].security.allowed-user-ids`.
- warning au démarrage si whitelist vide pour `Cortex`.

Isolation :
- commandes workflow rattachées au bot `Cortex` via annotation.

Fichiers / chemins :
- roadmap : résolution bornée à `workspace/shared` (refus hors racine).
- summary : résolution bornée à `toolkit.telegram.workflow.reportRootDirectory` (refus hors racine).

Limites actuelles :
- pas de rôles/multi-user avancé,
- pas de persistance de session,
- pas d’audit trail complet.

---

## 14) Limites connues (liste)

- Session Telegram en mémoire (perdue au redémarrage).
- Pas de persistance durable des runs.
- Pas de WebUI workflow de pilotage.
- Pas d’auto-advance de phase/étape.
- Roadmap chargée sans parsing métier complet.
- Pas de boutons inline, pas d’alias courts.
- Pas de fallback provider ; quota/rate-limit : heuristique de détection.
- Bot “Cortex” utilisé actuellement, mais à rendre configurable si besoin.
- CLI : nécessite de gérer le classpath/invocation ; l’outillage exact dépend de votre packaging.

---

## 15) Conseils de test manuel (checklist)

Checklist Telegram :

```text
[ ] /workflow affiche le dashboard
[ ] /workflow_roadmap_load charge une roadmap valide
[ ] /workflow_run démarre un run
[ ] /workflow_status passe à RUNNING puis COMPLETED/WAITING_HUMAN
[ ] /workflow_summary affiche ou envoie le summary
[ ] /workflow_resume fonctionne en WAITING_HUMAN
[ ] double /workflow_run est refusé
[ ] path invalide roadmap est refusé
[ ] summary absent retourne une erreur claire
[ ] quota/rate-limit donne un message compréhensible
```

Checklist fichiers :

```text
[ ] les rapports sont créés sous data/rapport/v1.1/CodexTime/...
[ ] workflow-summary.md est généré et lisible
[ ] les chemins absolus ne fuitent pas dans /workflow et /workflow_status
[ ] les tokens/stacktraces ne fuitent pas dans Telegram
```

---

## 16) Points d’amélioration à surveiller (notes utilisateur)

À noter pendant les tests :
- ergonomie Telegram (longueur commandes, clarté Next),
- cohérence des messages (succès/erreur/running/waiting),
- erreurs mal qualifiées (quota vs réseau vs bug),
- besoin de “reset context”,
- besoin d’un “stop workflow” explicite,
- besoin d’une WebUI read-only,
- besoin de persistance (runs/sessions),
- besoin de provider fallback.

---

## 17) FAQ

Q: Quelle différence entre RUN et RESUME ?
- RUN lance le cycle complet (analyse → review → correction optionnelle). RESUME reprend après un `WAIT_HUMAN` en relançant uniquement la correction.

Q: Pourquoi mon workflow est en WAITING_HUMAN ?
- Le moteur s’est arrêté volontairement pour demander une action humaine. Lire `/workflow_summary` (ou `workflow-summary.md`) pour les actions à faire, puis reprendre.

Q: Où est le summary ?
- CLI : champ `workflowSummaryPath` si présent. Telegram : `/workflow_summary`.

Q: Pourquoi Telegram dit “workflow already running” ?
- Un run est en cours dans la session (par chatId). Utiliser `/workflow_status`.

Q: Que faire si Codex est en quota / rate-limit ?
- Attendre et relancer, ou changer provider/model (selon configuration). Telegram masque les détails et affiche un message friendly.

Q: Que faire si Maven échoue ?
- Lire le summary : il indique build artifact + correction attempt. Si l’échec persiste après auto-correction, l’état devient `WAIT_HUMAN` pour intervention humaine.

Q: Puis-je utiliser CodexTime depuis la WebUI ?
- À date : non pour piloter le workflow. La WebUI est surtout admin/supervision technique (cf. audit Phase 7).

Q: Est-ce que l’état survit à un redémarrage ?
- Non : la session Telegram est in-memory.

Q: Pourquoi le bot s’appelle Cortex ?
- C’est le bot actuellement configuré pour le workflow ; ce n’est pas supposé rester une contrainte définitive.

Q: Peut-on lancer plusieurs workflows en parallèle ?
- Telegram : un run par chatId (lock running). Plusieurs chats peuvent lancer en parallèle, mais attention aux ressources et au partage de fichiers/rapport root.

---

## 18) Annexes

### Annexe A — Commandes Telegram

| Commande | Rôle | Paramètres |
|---|---|---|
| `/workflow` | Dashboard | aucun |
| `/workflow_roadmap_load` | Charger une roadmap | `path=...` (+ `project=...` optionnel) |
| `/workflow_run` | Lancer un run | `project=...` / `phase=...` / `etape=...` (selon contexte) |
| `/workflow_status` | État | aucun |
| `/workflow_summary` | Summary | aucun |
| `/workflow_resume` | Reprise | aucun (uniquement en WAITING_HUMAN) |

### Annexe B — Paramètres CLI (principaux)

| Paramètre | Obligatoire | Mode | Description |
|---|---:|---|---|
| `--mode` | oui | RUN/RESUME | `RUN` ou `RESUME` |
| `--reportRootDirectory` | oui | RUN/RESUME | Racine rapports |
| `--reportVersion` | oui | RUN/RESUME | Ex: `v1.1` |
| `--reportPhase` | oui | RUN/RESUME | Chemin phase (convention) |
| `--stepNumber` | oui | RUN/RESUME | Étape (int > 0) |
| `--analysisSourcePath` | oui | RUN | Fichier source analyse |
| `--codexWorkingDirectory` | non | RUN/RESUME | WD pour Codex |
| `--codexTimeoutSeconds` | non | RUN/RESUME | Timeout Codex |

### Annexe C — Chemins importants

| Chemin | Rôle |
|---|---|
| `data/rapport/v1.1/CodexTime/` | Racine rapports CodexTime |
| `workspace/shared/` | Racine roadmaps partagées |
| `src/main/resources/application.yml` | Config Telegram / toolkit |

### Annexe D — Statuts (Telegram)

| Statut | Signification |
|---|---|
| `IDLE` | aucun run en cours |
| `RUNNING` | run en cours |
| `COMPLETED` | terminé |
| `WAITING_HUMAN` | action humaine requise |
| `FAILED` | échec |

### Annexe E — Glossaire

- CodexTime : workflow assisté (runner + steps + artefacts).
- Codex : outil/CLI d’exécution LLM.
- Runner : exécute et coordonne les steps.
- Step : unité d’exécution (analyse/review/correction/validation).
- Artifact : fichier produit (prompt/result/build/etc.).
- Summary : `workflow-summary.md`, interface humaine.
- WAIT_HUMAN : arrêt contrôlé en attente d’action humaine.
- STOP_FAILURE : arrêt pour échec technique.
- RETRY_CORRECTION : signal de retry (utilisé pour auto-correction build en Phase 6).
- Roadmap : fichier `.md` chargé depuis `workspace/shared`.
- Phase / etape : ciblage du workflow (convention de dossiers).
- runId : identifiant de run.
- chatId : identifiant Telegram du chat (clé de session in-memory).

