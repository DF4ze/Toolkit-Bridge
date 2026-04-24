# Rapport d’implémentation — Phase 3 Étape 4 — `CorrectionStep`

## Objectif atteint

`CorrectionStep` est implémentée de façon simple, autonome, et conforme au design validé.

## Fichiers créés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStepTest.java`

## Comportement implémenté

### 1) Lecture/validation du contexte

Variables validées :
- `reportRootDirectory`
- `reportVersion`
- `reportPhase`
- `stepNumber`

Variables optionnelles conservées pour Codex :
- `codexWorkingDirectory`
- `codexTimeoutSeconds`

### 2) Résolution d’artefacts via `WorkflowArtifactService`

Entrées :
- `ANALYSIS_RESULT`
- `REVIEW_RESULT`

Sorties :
- `CORRECTION_PROMPT`
- `CORRECTION_RESULT`

### 3) Validation des entrées

Si un artefact d’entrée manque :
- retour `STOP_FAILURE`
- message explicite avec préfixe `CorrectionStep:`

### 4) Prompt de correction

Prompt texte simple contenant :
- section `Analysis result`
- section `Review result`
- instruction `Provide a corrected version based on the review`

### 5) Exécution Codex

- Appel via `CodexWorkflowClient`
- Contenu métier récupéré depuis `stdout`

### 6) Écriture des artefacts

- écriture de `CORRECTION_PROMPT`
- écriture de `CORRECTION_RESULT`

### 7) `WorkflowStepResult`

Succès :
- `decision = CONTINUE`
- message `Correction completed`
- `data` : `promptArtifactPath`, `resultArtifactPath`

Échec :
- `decision = STOP_FAILURE`
- message explicite
- sur échec Codex/timeout, paths d’artefacts présents dans `data`

## Tests ajoutés (`CorrectionStepTest`)

1. Cas nominal (`CONTINUE`) avec artefacts présents + Codex OK.
2. Artefact d’entrée manquant (`STOP_FAILURE`).
3. Exception d’exécution Codex (`STOP_FAILURE`).
4. Codex non-success (`STOP_FAILURE`).
5. Timeout (`STOP_FAILURE`).

## Vérification

Commande exécutée :
- `./mvnw -Dtest=CorrectionStepTest test`

Résultat :
- BUILD SUCCESS
- Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

## Contraintes respectées

- `WorkflowOrchestrator` non modifié
- aucune nouvelle abstraction
- aucune boucle/retry
- aucun usage de `WAIT_HUMAN`
- implémentation sobre, sans sur-conception
