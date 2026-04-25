# Resultat implementation - Phase 5 Etape 1

## Objectif

Rendre le workflow `AnalysisReviewWorkflowRunner` appelable depuis l'exterieur avec un point d'entree simple, sans modifier le coeur runtime et sans introduire de facade applicative inutile.

## Implementation realisee

### 1. Point d'entree CLI simple

Ajout de `AnalysisReviewWorkflowCli`.

Responsabilites:
- assembler directement `AnalysisReviewWorkflowRunner` en mode autonome;
- accepter un mode simple `RUN` ou `RESUME`;
- construire un `WorkflowExecutionContext` minimal;
- appeler directement le runner existant;
- imprimer uniquement le contrat externe utile.

Commandes runtime appelees:
- RUN -> `runner.runAnalysisReviewWithOptionalCorrection(context)`
- RESUME -> `runner.runCorrectionAfterReview(context)`

Aucune facade applicative n'a ete ajoutee, car le besoin immediat est couvert par la classe CLI.

### 2. Contrat d'entree CLI

Arguments requis communs:
- `--mode=RUN|RESUME`
- `--reportRootDirectory=<path>`
- `--reportVersion=<version>`
- `--reportPhase=<phase>`
- `--stepNumber=<number>`

Argument requis uniquement pour RUN:
- `--analysisSourcePath=<path>`

Arguments optionnels:
- `--codexWorkingDirectory=<path>`
- `--codexTimeoutSeconds=<seconds>`
- `--runId=<id>`
- `--workflowType=<type>`
- `--targetStepRef=<ref>`

### 3. Contrat de sortie

Sortie texte simple en lignes `key=value`:
- `decision`
- `message`
- `finalDecision`
- `nextAction`
- `workflowSummaryPath` si present

Aucun DTO complexe n'a ete cree.

## Tests ajoutes

Ajout de `AnalysisReviewWorkflowCliTest`.

Cas couverts:
- RUN via le point d'entree, avec arret `WAIT_HUMAN` et affichage du contrat externe.
- RESUME via le point d'entree, avec execution de la correction et affichage du contrat externe.
- validation que RUN exige `analysisSourcePath`.

Les tests utilisent des steps stub pour verifier le point d'entree sans lancer le vrai binaire Codex.

## Verification

Commande executee:

```text
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowCliTest,AnalysisReviewWorkflowRunnerResumeTest" test
```

Resultat: OK.

## Fichiers crees

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCli.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCliTest.java`
- `data/rapport/v1.1/CodexTime/phase5/1.implements.md`
- `data/rapport/v1.1/CodexTime/phase5/result.1.implements.md`

## Fichiers non modifies volontairement

- `WorkflowOrchestrator`
- `GlobalAnalysisStep`
- `GlobalReviewStep`
- `CorrectionStep`
- `WorkflowStepResult`

## Decisions structurantes

- Pas de facade `AnalysisReviewWorkflowCommandService`, car la classe CLI suffit pour cette etape.
- Pas de Spring lourd: le point d'entree assemble explicitement les dependances runtime.
- Pas de moteur de commande: le mode reste limite a `RUN` et `RESUME`.
- Pas de duplication de logique workflow: toute execution est deleguee au runner existant.

## Points d'attention

- Le CLI execute le vrai `CodexWorkflowClient` en usage reel; les tests stubent les steps pour rester rapides et deterministes.
- `workflowSummaryPath` reste conditionnel: il est affiche uniquement si le runner le produit.
- Le code de sortie CLI indique la validite de l'appel technique; l'etat metier du workflow reste expose par `decision` et `finalDecision`.
