# Résultat correction ciblée — Phase 3 Étape 1

## Changements effectués

Fichier modifié :
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

Tests ajoutés :
1. `returnsStopFailureAndKeepsArtifactsWhenCodexResultIsNotSuccessful`
- Simule `CodexExecutionResult.success == false`
- Vérifie :
  - `WorkflowStepDecision.STOP_FAILURE`
  - message explicite `Codex execution was not successful`
  - artefacts review (`promptArtifactPath`, `resultArtifactPath`) bien écrits

2. `returnsStopFailureWhenCodexExecutionTimesOut`
- Simule `CodexExecutionResult.timedOut == true`
- Vérifie :
  - `WorkflowStepDecision.STOP_FAILURE`
  - message explicite `Codex execution timed out`

## Conformité aux contraintes

- `GlobalReviewStep` inchangé
- aucune refactorisation
- aucune logique métier ajoutée
- aucune autre classe modifiée

## Build / tests

Commande exécutée :
```bash
./mvnw -q "-Dtest=GlobalReviewStepTest" test
```

Résultat : vert (succès).

## Confirmation

Correction ciblée réalisée exactement sur le périmètre demandé.
