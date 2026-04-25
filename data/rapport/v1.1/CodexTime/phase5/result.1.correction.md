# Resultat correction - Phase 5 Etape 1

## Objectif

Isoler le wiring runtime du point d'entree CLI afin d'eviter une duplication future, sans introduire de couche metier ni changer le comportement.

## Corrections appliquees

### 1. Factory minimale d'assemblage

Classe creee:

- `AnalysisReviewWorkflowRunnerFactory`

Responsabilite:
- construire une instance complete de `AnalysisReviewWorkflowRunner`;
- centraliser l'assemblage precedemment contenu dans `AnalysisReviewWorkflowCli.defaultRunner()`;
- rester sans logique metier, sans Spring, sans abstraction supplementaire.

### 2. CLI simplifie

Dans `AnalysisReviewWorkflowCli`:
- suppression de la methode `defaultRunner()`;
- remplacement par `AnalysisReviewWorkflowRunnerFactory.createDefault()` dans `main(...)`;
- conservation du comportement existant pour `RUN` et `RESUME`.

### 3. Constantes privees ajoutees

Ajout de constantes privees pour les cles manipulees par le CLI:
- arguments et variables de contexte (`reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`, `analysisSourcePath`, etc.);
- cles de sortie (`finalDecision`, `nextAction`, `workflowSummaryPath`).

Cela reduit les magic strings locales sans introduire de modele public.

## Fichiers modifies / crees

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowRunnerFactory.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/cli/AnalysisReviewWorkflowCli.java`
- `data/rapport/v1.1/CodexTime/phase5/1.corrections.md`
- `data/rapport/v1.1/CodexTime/phase5/result.1.correction.md`

## Fichiers volontairement non modifies

- `WorkflowOrchestrator`
- `GlobalAnalysisStep`
- `GlobalReviewStep`
- `CorrectionStep`
- `WorkflowStepResult`
- tests existants, car ils couvrent deja le comportement CLI attendu.

## Verification

Commande executee:

```text
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowCliTest,AnalysisReviewWorkflowRunnerResumeTest" test
```

Resultat: OK.

## Bilan

La correction reste strictement structurelle. Le CLI ne porte plus le wiring runtime et devient un simple consommateur du runner assemble par la factory. Aucun comportement metier n'a ete modifie, et aucune nouvelle couche applicative n'a ete introduite.
