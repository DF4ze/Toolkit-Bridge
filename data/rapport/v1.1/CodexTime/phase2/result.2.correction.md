# Phase 2 - Etape 2 - Correction ciblee

## Corrections appliquees

### 1) Gestion des erreurs resserree
Fichier modifie:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalAnalysisStep.java`

Changements:
- suppression du `catch (Exception)` global.
- capture restreinte aux erreurs attendues:
  - `IllegalArgumentException` (validation variables)
  - `IOException` (lecture source)
  - `CodexExecutionException` (appel Codex)
  - `IllegalStateException` (ecriture artefacts / erreurs runtime attendues)
- les erreurs de programmation inattendues ne sont plus masquees.

### 2) Uniformisation des messages d'erreur
- ajout d'un prefixe commun: `GlobalAnalysisStep:`
- message court + cause conservee.

### 3) Test manquant ajoute
Fichier modifie:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalAnalysisStepTest.java`

Nouveau test:
- `returnsStopFailureWhenCodexResultIsNotSuccessfulAndKeepsArtifacts`

Verification faite dans ce test:
- `decision == STOP_FAILURE`
- message coherent avec prefixe
- artefacts prompt/resultat toujours ecrits

## Verification

Commande executee:
- `./mvnw -Dtest=GlobalAnalysisStepTest test`

Resultat:
- BUILD SUCCESS
- Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

## Perimetre respecte

- pas de changement d'architecture
- pas de nouvelle abstraction
- pas de refactor massif
- aucun autre module touche