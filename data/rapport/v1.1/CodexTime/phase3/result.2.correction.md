# Résultat correction légère — Phase 3 Étape 2

## Modifications effectuées (tests uniquement)

Fichier modifié :
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`

Ajouts :
1. `rejectsNullContextForExecuteTwoSteps`
- vérifie `context == null`
- attendu : `NullPointerException`

2. `rejectsNullFirstStepForExecuteTwoSteps`
- vérifie `firstStep == null`
- attendu : `NullPointerException`

3. `rejectsNullSecondStepForExecuteTwoSteps`
- vérifie `secondStep == null`
- attendu : `NullPointerException`

Ajustement optionnel appliqué :
- `targetStepRef` du `buildContext()` aligné en `phase-3/step-2` pour cohérence du lot.

## Contraintes respectées

- `WorkflowOrchestrator` non modifié
- aucun refactor
- aucune logique métier ajoutée
- tests uniquement

## Validation

Commande exécutée :
```bash
./mvnw -q "-Dtest=WorkflowOrchestratorTest" test
```

Résultat : build vert (succès).

## Confirmation

Correction demandée réalisée, sans autre modification hors périmètre.
