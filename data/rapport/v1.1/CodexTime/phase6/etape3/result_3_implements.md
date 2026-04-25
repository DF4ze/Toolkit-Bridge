# Resultat implementation Phase 6 - Etape 3

## Objectif

Ajouter une boucle d'auto-correction controlee des erreurs de build Maven :

1. validation Maven ;
2. en cas de `FAILURE`, retour `RETRY_CORRECTION` tant que le nombre de tentatives reste sous le maximum ;
3. execution d'une correction Codex dediee au build ;
4. relance de la validation ;
5. arret sur succes, timeout, erreur systeme ou maximum atteint.

## Fichiers crees

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/BuildErrorCorrectionStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/BuildErrorCorrectionStepTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerValidationRetryTest.java`

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/MavenValidationStep.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowRunnerFactory.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/artifact/WorkflowArtifactType.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/MavenValidationStepTest.java`

## Comportement implemente

### `MavenValidationStep`

- `SUCCESS` retourne `CONTINUE`.
- `FAILURE` retourne `RETRY_CORRECTION` si `buildErrorRetryCount < buildErrorRetryMax`.
- `FAILURE` retourne `STOP_FAILURE` si le maximum de tentatives est atteint.
- `TIMEOUT` retourne `STOP_FAILURE`.
- `SYSTEM_ERROR` retourne `STOP_FAILURE`.
- Les variables `buildErrorRetryCount` et `buildErrorRetryMax` sont lues depuis `context.variables()`.
- Les valeurs par defaut sont :
  - `buildErrorRetryCount = 0`
  - `buildErrorRetryMax = 2`
- Sur `FAILURE` retryable, le compteur est incremente et expose dans `WorkflowStepResult.data`.
- Le contexte d'erreur transmis a la correction reste simple :
  - `buildErrorSummary`
  - `buildArtifactPath`

### `BuildErrorCorrectionStep`

- Construit un prompt Codex minimal a partir de l'extrait d'erreur Maven et du chemin de l'artefact build.
- Ne parse pas Maven de maniere complexe.
- Ecrit un artefact `BUILD_ERROR_CORRECTION`.
- Retourne `CONTINUE` quand l'execution Codex reussit.
- Retourne `STOP_FAILURE` si le contexte est incomplet ou si Codex echoue.

### `AnalysisReviewWorkflowRunner`

- Ajoute `runWithValidationAndRetry(context)`.
- La boucle reste locale au runner, sans modification de l'orchestrateur.
- Flow :
  - `runWithValidation(context)`
  - si `CONTINUE` : succes final
  - si `STOP_FAILURE` : echec final
  - si `RETRY_CORRECTION` : execution de `BuildErrorCorrectionStep`, puis relance
- Le compteur de retry est propage dans un nouveau `WorkflowExecutionContext` entre les tentatives.

### Factory runtime

- `AnalysisReviewWorkflowRunnerFactory.createDefault()` assemble maintenant aussi `BuildErrorCorrectionStep`.
- Aucun Spring, aucun moteur de commande, aucune nouvelle couche applicative.

## Tests ajoutes ou adaptes

- `MavenValidationStepTest`
  - verifie `FAILURE -> RETRY_CORRECTION` quand une tentative reste disponible ;
  - verifie `FAILURE -> STOP_FAILURE` quand le maximum est atteint ;
  - conserve les validations `SUCCESS`, `TIMEOUT`, `SYSTEM_ERROR`.
- `BuildErrorCorrectionStepTest`
  - correction Codex reussie ;
  - contexte d'erreur manquant ;
  - echec Codex.
- `AnalysisReviewWorkflowRunnerValidationRetryTest`
  - retry une fois puis succes ;
  - maximum atteint puis `STOP_FAILURE` ;
  - `TIMEOUT` sans retry ;
  - `SYSTEM_ERROR` sans retry.

## Validation du retry

Le test `runWithValidationAndRetryCorrectsBuildFailureThenSucceeds` demontre le cycle complet :

1. la validation Maven retourne `RETRY_CORRECTION` avec `buildErrorRetryCount = 1` ;
2. `BuildErrorCorrectionStep` est appele avec le contexte d'erreur ;
3. la validation Maven est relancee ;
4. le second passage retourne `CONTINUE`.

## Commandes de validation

- `.\mvnw.cmd -q "-Dtest=MavenValidationStepTest,BuildErrorCorrectionStepTest,AnalysisReviewWorkflowRunnerValidationRetryTest,AnalysisReviewWorkflowRunnerResumeTest,AnalysisReviewWorkflowCliTest,WorkflowOrchestratorTest" test`
- `.\mvnw.cmd -q test`

Resultat : succes.

## Contraintes respectees

- Pas de modification de l'orchestrateur pour cette etape.
- Pas de parsing Maven avance.
- Pas de Telegram.
- Pas de systeme generique.
- Pas de nouvelle architecture.

## Remarque de conception

La boucle d'auto-correction est volontairement portee par `AnalysisReviewWorkflowRunner`, comme demande. Cela evite de transformer l'orchestrateur en moteur de retry et garde l'evolution centree sur le scenario concret build Maven -> correction Codex -> revalidation.
