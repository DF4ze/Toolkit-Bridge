# Rapport de correction — Phase 3 Étape 4

## Correction appliquée

Modification ciblée dans `CorrectionStep` pour aligner l’observabilité de `CORRECTION_RESULT` avec les autres steps :

- ajout d’une méthode interne `buildResultContent(CodexExecutionResult result)`
- comportement :
  - si `stderr` est vide : contenu = `stdout`
  - si `stderr` est non vide : contenu =

    ```
    <stdout>

    [stderr]
    <stderr>
    ```

- `CORRECTION_RESULT` est désormais écrit avec ce format enrichi.

## Périmètre respecté

- aucune modification de logique métier
- `WorkflowOrchestrator` inchangé
- aucun refactor des helpers de contexte
- aucune nouvelle abstraction
- aucune autre modification hors cible

## Fichier modifié

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStep.java`

## Vérification

Commande exécutée :
- `./mvnw -Dtest=CorrectionStepTest test`

Résultat :
- BUILD SUCCESS
- Tests run: 5
- Failures: 0
- Errors: 0
- Skipped: 0

## Confirmation

La correction technique demandée est appliquée et validée.
