# Phase 8 — CodexCall — Étape 3 — Correction (Stabilisation résolution projet)

## Contexte

L’étape 3 a introduit la résolution `projectName → projectPath` pour `/workflow_run` et `/workflow_resume`, et a ajouté `projectPath` dans la session Telegram.

La relecture demandait 2 corrections qualité, sans modifier le comportement fonctionnel :

1. supprimer le string-matching sur les erreurs du resolver (fragile) ;
2. ajouter une surcharge `updateContext(...)` dans `WorkflowTelegramSessionStore` pour éviter les appels trop verbeux.

## Corrections appliquées

### 1) Suppression du string-matching (signal structuré d’erreur)

- Ajout de `WorkflowTelegramRunTargetErrorCode` (enum) et propagation dans `WorkflowTelegramRunTarget`.
- `WorkflowTelegramRunTargetResolver` renseigne désormais `errorCode` pour les cas d’erreurs structurables (notamment `MISSING_PROJECT`).
- `WorkflowTelegramOrchestrationService.startRun(...)` utilise `errorCode` (ex: `MISSING_PROJECT`) pour choisir le message utilisateur adapté, sans dépendre du texte libre.

Objectif atteint : l’orchestration ne fait plus de détection basée sur `errorMessage.toLowerCase().contains(...)` pour le cas “projet manquant”.

### 2) Surcharge `updateContext(...)` dans le session store

- Ajout d’un overload :

```java
updateContext(chatId, userId, projectName, phase, etape, roadmapPath)
```

qui délègue à la version complète incluant `projectPath`.

Objectif atteint : compatibilité interne + réduction de verbosité + moins de risque d’erreur d’ordre d’arguments.

## Fichiers modifiés / ajoutés

### Ajout

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRunTargetErrorCode.java`

### Modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRunTarget.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRunTargetResolver.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSessionStore.java`
- Tests :
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRunTargetResolverTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSessionStoreTest.java`

## Tests

Commande exécutée :

```text
.\mvnw.cmd test
```

Résultat : OK.

## Confirmations (contraintes)

- ✔ `CodexWorkflowClient` non modifié.
- ✔ `codexWorkingDirectory` non injecté dans cette correction.
- ✔ Aucun path absolu exposé dans Telegram.
- ✔ Pas de DB dans le resolver (pas de dépendance repository/registry ajoutée au resolver).
- ✔ Correction limitée à : signal structuré d’erreur + overload `updateContext(...)` + ajustement tests.

