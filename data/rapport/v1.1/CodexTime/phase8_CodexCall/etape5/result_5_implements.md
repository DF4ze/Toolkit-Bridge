# Phase 8 — CodexCall — Étape 5 — Implémentation — Optimisation appel Codex CLI

## Résumé

`CodexWorkflowClient` utilise maintenant le mode non-interactif Codex CLI :

- `codex exec --cd <workingDirectory> -` si `workingDirectory` est présent
- `codex exec -` si `workingDirectory` est absent

Le prompt est désormais injecté via **stdin**, et n’est plus passé comme argument CLI.

## Implémentation

### 1) Nouvelle commande CLI

Dans `CodexWorkflowClient` :

- remplacement de l’ancien schéma `codex <prompt>` par :
  - `codex exec --cd <dir> -` si `request.workingDirectory() != null`
  - `codex exec -` sinon

Chaque argument est un élément distinct de la `List<String>` (pas de shell string).

### 2) Prompt via stdin

Après `processBuilder.start()` :

- écriture du prompt complet dans `process.getOutputStream()` en UTF-8 ;
- fermeture du stdin (EOF) via try-with-resources.

En cas d’échec d’écriture :

- le process est détruit ;
- une `CodexExecutionException` est levée (comportement d’erreur conservé).

### 3) Working directory

Le `workingDirectory` est appliqué à deux niveaux (si présent) :

- `ProcessBuilder.directory(...)` (comme avant)
- `codex exec --cd <workingDirectory>` (nouveau)

## Tests

Commande exécutée :

```text
.\mvnw.cmd test
```

Résultat : OK.

Tests ajoutés/étendus :

- vérifie la construction de commande :
  - présence de `exec`
  - présence de `--cd <path>` quand `workingDirectory` est présent
  - présence de `-` (stdin) en dernier argument
  - absence du prompt dans les arguments
- vérifie le cas sans `workingDirectory` (pas de `--cd`)

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexWorkflowClient.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexWorkflowClientTest.java`

## Contraintes respectées

- ✔ Telegram inchangé.
- ✔ `/workflow_run` inchangé.
- ✔ `/workflow_resume` inchangé.
- ✔ Résolution projet inchangée.
- ✔ Injection `codexWorkingDirectory` inchangée.
- ✔ Pas de WebUI.
- ✔ Pas d’abstraction `AgentCoder`.
- ✔ `--sandbox workspace-write` non imposé.
- ✔ Pas de `danger-full-access`.
- ✔ Pas de shell string (`cmd /c`, concat) : `List<String>` uniquement.

## Limites / recommandations futures

- `--json` et `--output-last-message` non ajoutés (volontairement) pour garder l’étape minimaliste.
- Une future étape peut ajouter :
  - `--json` pour exploitation d’événements ;
  - `--output-last-message` pour récupérer proprement le dernier message assistant ;
  - une stratégie d’erreurs plus fine (quota/rate-limit) côté exécution Codex.

