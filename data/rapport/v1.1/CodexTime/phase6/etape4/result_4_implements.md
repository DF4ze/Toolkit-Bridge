# Resultat implementation Phase 6 - Etape 4

## Objectif

Introduire un `WAIT_HUMAN` intelligent pour les echecs de build persistants apres correction automatique.

Le comportement vise est :

```text
STOP_FAILURE + buildStatus=FAILURE + correctionTriggered=true + retry max atteint
-> WAIT_HUMAN
```

Les erreurs techniques doivent rester en `STOP_FAILURE`.

## Fichiers crees

- `data/rapport/v1.1/CodexTime/phase6/etape4/4.implements.md`
- `data/rapport/v1.1/CodexTime/phase6/etape4/result_4_implements.md`

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerValidationRetryTest.java`

## Comportement implemente

### Conversion `STOP_FAILURE -> WAIT_HUMAN`

Dans `AnalysisReviewWorkflowRunner.runWithValidationAndRetry`, le resultat final est maintenant inspecte quand il n'est plus `RETRY_CORRECTION`.

La conversion en `WAIT_HUMAN` se produit uniquement si :

- `decision == STOP_FAILURE`
- `buildStatus == FAILURE`
- une correction automatique a deja ete tentee (`correctionTriggered == true`)
- `buildErrorRetryCount >= buildErrorRetryMax`
- `buildErrorRetryMax > 0`

Le resultat converti expose :

```text
decision = WAIT_HUMAN
finalDecision = WAIT_HUMAN
waitReason = Build still fails after automatic correction attempts
nextAction = Inspect build artifact and correction attempt, then decide how to proceed
correctionTriggered = true
```

### Conservation de `STOP_FAILURE`

Restent en `STOP_FAILURE` :

- `TIMEOUT`
- `SYSTEM_ERROR`
- retry state invalide
- build failure sans correction automatique prealable
- build failure qui ne remplit pas les criteres de retry max atteint

### Propagation du contexte de correction

Apres `BuildErrorCorrectionStep`, le runner propage dans le contexte courant :

- `correctionTriggered`
- `correctionAttemptPath`
- `buildErrorRetryCount`
- `buildErrorRetryMax`

Cela permet au cycle final de savoir qu'une correction a reellement ete tentee, sans modifier les steps.

### Summary adapte au contexte build

La generation de `workflow-summary.md` distingue maintenant :

- `WAIT_HUMAN` issu de review : instructions existantes conservees ;
- `WAIT_HUMAN` issu du build : instructions dediees.

Pour un `WAIT_HUMAN` build, le summary affiche :

```text
1. Inspect build artifact
2. Inspect correction attempt
3. Fix manually or adjust strategy
4. Resume workflow
```

## Tests ajoutes ou adaptes

Dans `AnalysisReviewWorkflowRunnerValidationRetryTest` :

- `runWithValidationAndRetryConvertsPersistentBuildFailureToWaitHuman`
- `workflowSummaryUsesBuildInstructionsForWaitHumanBuildFailure`

Les tests existants confirment aussi que :

- `TIMEOUT` reste `STOP_FAILURE`
- `SYSTEM_ERROR` reste `STOP_FAILURE`
- retry state invalide reste `STOP_FAILURE`
- retry max atteint sans correction declenchee reste `STOP_FAILURE`

## Validation des tests

Commandes executees :

```powershell
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowRunnerValidationRetryTest" test
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowRunnerValidationRetryTest,MavenValidationStepTest,BuildErrorCorrectionStepTest,AnalysisReviewWorkflowCliTest" test
.\mvnw.cmd -q test
```

Resultat : succes.

Les logs Maven affichent uniquement des warnings JVM/Mockito et des erreurs attendues par certains tests de resilience existants. Aucun test n'a echoue.

## Contraintes respectees

- Pas de modification de l'orchestrateur.
- Pas de modification des steps.
- Pas de parsing Maven.
- Pas de logique de classification complexe.
- Pas de Telegram.
- Pas de nouvelle couche.
- Flow global conserve.

## Resume

L'agent ne traite plus tous les echecs build de la meme maniere.

Un echec technique reste un `STOP_FAILURE`.

Un build qui echoue encore apres correction automatique devient un `WAIT_HUMAN`, avec une raison stable, une action claire et un summary exploitable par un humain ou par un futur consommateur Telegram.
