# Rapport de correction — Phase 3 Étape 3

## Périmètre appliqué

Corrections ciblées sur :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

Aucune autre classe modifiée.

## Corrections réalisées

### 1) Source de vérité de la décision = stdout métier uniquement

- Ajout d’une variable `semanticReviewOutput = codexResult.stdout()`.
- La détection `WAIT_HUMAN` (`requiresHumanDecision(...)`) est désormais basée uniquement sur `semanticReviewOutput`.
- `stderr` reste présent dans `REVIEW_RESULT` via `buildResultContent(...)`, mais n’influence plus la décision.

### 2) Suppression du round-trip disque pour la décision

- Suppression de la relecture `workflowArtifactService.readArtifact(reviewResultPath)` après écriture.
- La décision est prise directement en mémoire, à partir de `semanticReviewOutput`.
- L’écriture de l’artefact `REVIEW_RESULT` est conservée à l’identique pour la traçabilité.

## Tests ajoutés

### a) WAIT_HUMAN sans WAIT_REASON

Test ajouté :
- `returnsWaitHumanWithoutWaitReasonWhenMarkerIsPresentWithoutReason`

Vérifications :
- `decision = WAIT_HUMAN`
- `data` contient `promptArtifactPath` et `resultArtifactPath`
- `data` ne contient pas `waitReason`

### b) Faux positif stderr

Test ajouté :
- `ignoresWaitHumanMarkerWhenPresentOnlyInStderr`

Scénario :
- `stderr` contient `DECISION: WAIT_HUMAN`
- `stdout` ne contient pas la convention

Vérification :
- `decision = CONTINUE`

## Vérification build/tests

Commande exécutée :
- `./mvnw -Dtest=GlobalReviewStepTest test`

Résultat :
- BUILD SUCCESS
- Tests run: 11
- Failures: 0
- Errors: 0
- Skipped: 0

## Contraintes respectées

- `WorkflowOrchestrator` non modifié
- aucune nouvelle classe
- aucun parsing complexe ajouté
- aucun refactor global
- aucune autre modification hors périmètre

## Confirmation

Correction ciblée appliquée conformément à la demande.
