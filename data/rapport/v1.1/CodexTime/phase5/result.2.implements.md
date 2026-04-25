# Resultat implementation - Phase 5 Etape 2

## Objectif

Stabiliser le contrat externe du workflow sans introduire de nouveau modele, en conservant `WorkflowStepResult` comme source runtime et le CLI comme sortie technique simple.

## Implementation realisee

### 1. CLI aligne sur le contrat externe

`AnalysisReviewWorkflowCli` expose maintenant:
- `decision`
- `message`
- `finalDecision`
- `nextAction`
- `correctionTriggered`
- `waitReason` si present
- `workflowSummaryPath` si present

La sortie reste au format `key=value`, une ligne par champ.

### 2. Normalisation des valeurs CLI

La methode de sortie CLI normalise les valeurs:
- aucune valeur `null` n'est imprimee;
- les retours ligne sont remplaces par des espaces;
- les champs optionnels ne sont imprimes que s'ils sont presents.

Fallbacks ajoutes cote CLI:
- `finalDecision` utilise `decision.name()` si absent;
- `nextAction` utilise une action par defaut selon `decision` si absent;
- `correctionTriggered` vaut `false` si absent.

### 3. Contrat runner verifie par tests

Aucune modification de `WorkflowStepResult`.
Aucune creation de DTO.
Aucune projection JSON.

Le runner/orchestrator n'ont pas necessite de changement fonctionnel: les resultats consolides existants contiennent deja les cles obligatoires sur les chemins couverts.

Des assertions de contrat ont ete ajoutees dans `AnalysisReviewWorkflowRunnerResumeTest` pour verifier:
- `finalDecision`;
- `nextAction`;
- `correctionTriggered`;
- `waitReason` sur `WAIT_HUMAN`;
- absence acceptable de `workflowSummaryPath` quand le summary n'est pas produit.

## Tests ajoutes / ajustes

### `AnalysisReviewWorkflowCliTest`

Ajouts:
- exposition de `correctionTriggered`;
- exposition de `waitReason` en `WAIT_HUMAN`;
- cas sans `workflowSummaryPath`;
- cas `STOP_FAILURE`;
- normalisation des retours ligne dans `message`;
- fallbacks CLI pour `finalDecision`, `nextAction` et `correctionTriggered`.

### `AnalysisReviewWorkflowRunnerResumeTest`

Ajout d'un helper d'assertion du contrat obligatoire:
- `assertMandatoryContract(...)`.

## Verification

Commande executee:

```text
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowCliTest,AnalysisReviewWorkflowRunnerResumeTest" test
```

Resultat: OK.

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCli.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCliTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerResumeTest.java`
- `data/rapport/v1.1/CodexTime/Phase5/2.implements.md`
- `data/rapport/v1.1/CodexTime/Phase5/result.2.implements.md`

## Fichiers volontairement non modifies

- `WorkflowStepResult`
- `WorkflowOrchestrator`
- `AnalysisReviewWorkflowRunner`
- les steps

## Bilan

Le contrat externe V1 est maintenant plus stable cote CLI et verifie par tests sur les chemins critiques. La solution reste basee sur l'existant, sans nouveau modele, sans JSON, sans couche supplementaire.
