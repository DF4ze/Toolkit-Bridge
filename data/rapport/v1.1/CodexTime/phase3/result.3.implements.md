# Rapport d’implémentation — Phase 3 Étape 3 — WAIT_HUMAN minimal

## Objectif atteint

Introduction de `WAIT_HUMAN` dans le workflow via `GlobalReviewStep`, sans modifier l’orchestrator ni ajouter de nouvelle couche.

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

## Détails de l’implémentation

### 1. Détection explicite `WAIT_HUMAN`

Dans `GlobalReviewStep` :
- après exécution Codex et écriture de `REVIEW_RESULT`, le contenu est relu,
- si le contenu contient la convention exacte `DECISION: WAIT_HUMAN`, la step retourne :
  - `decision = WAIT_HUMAN`
  - `message = "Global review requires human decision"`
  - `data` avec :
    - `promptArtifactPath`
    - `resultArtifactPath`
    - `waitReason` optionnel (si une ligne `WAIT_REASON:` est présente).

### 2. Cas nominal inchangé

Si la convention n’est pas présente :
- comportement conservé,
- `decision = CONTINUE`.

### 3. Cas erreur inchangé

Les chemins existants `STOP_FAILURE` restent inchangés :
- erreur d’artefacts,
- exception Codex,
- exécution Codex non-success / timeout.

## Tests ajoutés

Dans `GlobalReviewStepTest` :

1. `returnsWaitHumanWhenReviewResultContainsWaitHumanDecisionMarker`
- mock Codex avec `DECISION: WAIT_HUMAN`
- vérifie `decision = WAIT_HUMAN`
- vérifie `message` clair
- vérifie `data` et `waitReason`

2. `returnsContinueWhenReviewResultDoesNotContainDecisionMarker`
- mock Codex sans convention
- vérifie `decision = CONTINUE`

## Vérification exécutée

Commande lancée :
- `./mvnw -Dtest=GlobalReviewStepTest test`

Résultat :
- BUILD SUCCESS
- Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

## Respect des contraintes

- `WorkflowOrchestrator` non modifié
- aucune nouvelle step
- aucune nouvelle classe
- parsing volontairement simple et explicite
- pas de moteur de décision
