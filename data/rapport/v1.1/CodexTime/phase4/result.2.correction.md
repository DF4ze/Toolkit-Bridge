# Résultat correction — Phase 4 Étape 2

## Corrections appliquées

### 1) Contrat strict `requiresCorrection` dans `WorkflowOrchestrator`
- Le fallback silencieux a été supprimé.
- Si le résultat de review est `CONTINUE` mais que `requiresCorrection` est absent ou non booléen :
  - `IllegalStateException` explicite est levée avec message clair sur le contrat invalide.

### 2) Priorité déterministe des directives dans `GlobalReviewStep`
- `parseReviewDirective(...)` scanne désormais tout le contenu.
- Les marqueurs détectés sont consolidés puis résolus avec priorité explicite :
  - `WAIT_HUMAN > NEED_CORRECTION > OK > NONE`
- Le comportement n’est plus dépendant de l’ordre des lignes.

## Tests ajoutés

### `WorkflowOrchestratorTest`
- `failsExplicitlyWhenReviewContinueResultDoesNotProvideRequiresCorrectionFlag`
  - review `CONTINUE` sans `requiresCorrection`
  - attendu : `IllegalStateException` explicite

### `GlobalReviewStepTest`
- `appliesDirectivePriorityNeedCorrectionOverOk`
  - input : `OK` puis `NEED_CORRECTION`
  - attendu : `requiresCorrection=true`
- `appliesDirectivePriorityWaitHumanOverNeedCorrection`
  - input : `NEED_CORRECTION` puis `WAIT_HUMAN`
  - attendu : décision `WAIT_HUMAN`

## Contraintes respectées
- `CorrectionStep` : inchangée
- `WorkflowExecutionContext` : inchangé
- aucune nouvelle classe
- aucune abstraction/moteur ajouté
- patch minimal

## Validation
- Commande :
  - `./mvnw "-Dtest=GlobalReviewStepTest,WorkflowOrchestratorTest" test`
- Résultat :
  - `BUILD SUCCESS`
  - `43` tests exécutés, `0` échec

## Fichiers modifiés
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`
