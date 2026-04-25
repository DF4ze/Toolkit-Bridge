# Resultat correction Phase 6 - Etape 3

## Objectif

Appliquer les corrections issues de la revue critique de l'auto-correction build :

- securiser la boucle de retry ;
- clarifier le contrat de sortie de `BuildErrorCorrectionStep` ;
- tracer la tentative dans l'artefact de correction.

## Corrections appliquees

### 1. Garde defensive dans le runner

Dans `AnalysisReviewWorkflowRunner.runWithValidationAndRetry`, une verification locale du retry state a ete ajoutee avant l'appel a `BuildErrorCorrectionStep`.

La boucle s'arrete maintenant en `STOP_FAILURE` si :

- `buildErrorRetryCount` est absent ;
- `buildErrorRetryMax` est absent ;
- une valeur de retry n'est pas numerique ;
- une valeur est negative ;
- `buildErrorRetryMax` vaut `0` ;
- `buildErrorRetryCount >= buildErrorRetryMax`.

Cela garantit que le runner ne peut pas boucler indefiniment si un step retourne `RETRY_CORRECTION` avec des donnees incoherentes.

### 2. Suppression de `promptArtifactPath`

Dans `BuildErrorCorrectionStep`, la cle `promptArtifactPath` a ete retiree du `WorkflowStepResult.data`.

Raison : aucun artefact de prompt distinct n'est ecrit pour cette correction. Conserver cette cle aurait cree un contrat trompeur pour les consommateurs externes.

### 3. Ajout de l'info de tentative dans l'artefact

L'artefact `BUILD_ERROR_CORRECTION` contient maintenant une ligne :

```text
Attempt: X / Y
```

Si les informations ne sont pas disponibles, la valeur affichee est `unknown`.

### 4. Enrichissement leger du `data`

`BuildErrorCorrectionStep` propage maintenant, quand elles sont disponibles :

- `buildErrorRetryCount`
- `buildErrorRetryMax`

Cela permet a un appelant ou a un rapport humain de comprendre quelle tentative a produit l'artefact.

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/BuildErrorCorrectionStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerValidationRetryTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/BuildErrorCorrectionStepTest.java`

## Fichiers de tracabilite crees

- `data/rapport/v1.1/CodexTime/phase6/etape3/3.correction.md`
- `data/rapport/v1.1/CodexTime/phase6/etape3/result_3_correction.md`

## Tests ajoutes ou adaptes

### `AnalysisReviewWorkflowRunnerValidationRetryTest`

Ajouts :

- `runWithValidationAndRetryStopsOnInvalidRetryState`
- `runWithValidationAndRetryStopsWhenRetryStateReachedMaxBeforeCorrection`

Ces tests verifient que :

- une decision `RETRY_CORRECTION` sans metadonnees de retry ne lance pas la correction ;
- une decision `RETRY_CORRECTION` avec compteur au maximum ne lance pas la correction ;
- le resultat final est `STOP_FAILURE`.

### `BuildErrorCorrectionStepTest`

Adaptations :

- verification que `promptArtifactPath` est absent ;
- verification que `buildErrorRetryCount` et `buildErrorRetryMax` sont propages ;
- verification que l'artefact contient `Attempt: 1 / 2`.

## Validation des tests

Commandes lancees :

```powershell
.\mvnw.cmd -q "-Dtest=MavenValidationStepTest,BuildErrorCorrectionStepTest,AnalysisReviewWorkflowRunnerValidationRetryTest" test
.\mvnw.cmd -q test
```

Resultat : succes.

Les logs contiennent uniquement des warnings JVM/Mockito et des erreurs attendues par certains tests existants de resilience. Aucun test n'a echoue.

## Confirmation absence de boucle infinie

La boucle `runWithValidationAndRetry` ne depend plus uniquement de `MavenValidationStep` pour s'arreter.

Avant chaque correction build, le runner valide les metadonnees de retry. En cas d'etat invalide ou de maximum atteint, il retourne directement :

```text
decision = STOP_FAILURE
finalDecision = STOP_FAILURE
correctionTriggered = false
```

La correction build n'est donc pas appelee dans ces cas.

## Contraintes respectees

- Pas de modification de l'orchestrateur.
- Pas de changement du flow global.
- Pas de nouvelle feature.
- Pas de nouveau modele.
- Pas de Telegram.
- Pas de refactor global.

## Resume

La correction stabilise le lot sans le complexifier. Le runner devient defensif, le contrat de sortie ne contient plus de chemin trompeur, et l'artefact de correction indique maintenant clairement la tentative concernee.
