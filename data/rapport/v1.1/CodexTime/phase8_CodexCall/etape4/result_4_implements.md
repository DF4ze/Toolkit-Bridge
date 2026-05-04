# Phase 8 — CodexCall — Étape 4 — Implémentation — Injection `codexWorkingDirectory`

## Résumé

Injection de `codexWorkingDirectory=<projectPath>` ajoutée dans le `WorkflowExecutionContext` pour :

- `/workflow_run` (RUN)
- `/workflow_resume` (RESUME)

Le type injecté est un `Path`.

## Implémentation

### 1) Flow RUN

Dans `WorkflowTelegramOrchestrationService` :

- le `projectPath` déjà résolu en amont est passé explicitement à :
  - `executeRun(...)`
  - `buildContext(...)`
- `buildContext(...)` injecte :

```java
variables.put("codexWorkingDirectory", projectPath);
```

### 2) Flow RESUME

Dans `WorkflowTelegramOrchestrationService` :

- le `projectPath` déjà résolu en amont est passé explicitement à :
  - `executeResume(...)`
  - `buildResumeContext(...)`
- `buildResumeContext(...)` injecte :

```java
variables.put("codexWorkingDirectory", projectPath);
```

### 3) Non-régression variables

- RUN conserve `analysisSourcePath`.
- RESUME ne contient pas `analysisSourcePath`.
- Les variables existantes restent inchangées : `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`, `roadmapPath`, `llmProvider`, `llmModel`.

### 4) Garde défensive

Ajout d’une garde minimale dans `executeRun(...)` et `executeResume(...)` :

- si `projectPath == null` : on évite une NPE et on marque le run en échec via `sessionStore.failRun(...)`.

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`

## Tests

Commande exécutée :

```text
.\mvnw.cmd test
```

Résultat : OK.

Tests ajoutés/étendus :

- RUN : vérifie que `codexWorkingDirectory` est présent dans le contexte, type `Path`, valeur = `projectPath` résolu.
- RUN : sans `project=...` mais session complète, injecte aussi `codexWorkingDirectory`.
- RESUME : vérifie `codexWorkingDirectory` et absence de `analysisSourcePath`.

## Confirmations (contraintes)

- ✔ `CodexWorkflowClient` non modifié.
- ✔ Appel CLI Codex non modifié.
- ✔ Pas de `codex exec --cd` (reporté à l’étape 5).
- ✔ Aucun lookup DB dans `buildContext(...)` / `buildResumeContext(...)`.
- ✔ Aucun path absolu exposé dans les messages Telegram.
- ✔ `/workflow_project_set` inchangé.
- ✔ Règles de résolution projet de l’étape 3 inchangées.

