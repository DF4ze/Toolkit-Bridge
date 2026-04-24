# Bilan final — Phase 3 (CodexTime)

## 1. Résumé de la phase

La Phase 3 a fait passer le workflow runtime d’un exécuteur mono-step à un mini workflow crédible avec steps métier explicites.
Le socle est resté volontairement simple : enchaînement linéaire court, artefacts markdown, décisions limitées (`CONTINUE`, `STOP_FAILURE`, `WAIT_HUMAN`), sans moteur générique.

## 2. Ce qui a été implémenté

- Ajout de la step métier `GlobalReviewStep` (`WorkflowStep`) avec :
  - lecture des artefacts d’analyse,
  - génération de `REVIEW_PROMPT` et `REVIEW_RESULT`,
  - appel `CodexWorkflowClient`,
  - retour `WorkflowStepResult`.
- Extension de `WorkflowOrchestrator` avec `executeTwoSteps(...)` :
  - exécute step 2 uniquement si step 1 retourne `CONTINUE`,
  - stop immédiat sinon.
- Introduction réelle de `WAIT_HUMAN` dans `GlobalReviewStep` :
  - détection via convention explicite `DECISION: WAIT_HUMAN` dans le `stdout` métier,
  - `waitReason` optionnel,
  - arrêt propre propagé par l’orchestrator.
- Ajout de `CorrectionStep` (`WorkflowStep`) :
  - entrées `ANALYSIS_RESULT` + `REVIEW_RESULT`,
  - génération `CORRECTION_PROMPT` + `CORRECTION_RESULT`,
  - appel Codex,
  - retour `CONTINUE`/`STOP_FAILURE`.
- Alignement observabilité `CorrectionStep` :
  - `CORRECTION_RESULT` inclut `stdout` + section `[stderr]` si non vide.
- Couverture tests consolidée :
  - `WorkflowOrchestratorTest`,
  - `GlobalReviewStepTest` (dont cas `WAIT_HUMAN` + anti faux positif stderr),
  - `CorrectionStepTest` (nominal, artefact manquant, exception, non-success, timeout).

## 3. Ce qui n’a pas été implémenté (volontairement)

- Aucun moteur générique de workflow (liste dynamique de steps, routing, state machine).
- Aucune boucle automatique de correction/retry.
- Aucun compteur de tentatives.
- Aucune orchestration complète de reprise après attente humaine.
- Aucune intégration Telegram ou transport humain réel.
- Aucune validation Maven intégrée au flux.
- Aucun parsing sémantique lourd / stratégie configurable complexe.

## 4. Décisions d’architecture

- Garder les steps métier dans `runtime.step`, chacune autonome et bornée.
- Utiliser `WorkflowArtifactService` + `WorkflowArtifactType` comme source unique de nommage/accès artefacts.
- Garder `WorkflowOrchestrator` minimal (1-step + 2-steps), sans logique métier des steps.
- Conserver `WorkflowStepResult` comme contrat unique de sortie (pas de nouveau modèle d’état).
- Introduire `WAIT_HUMAN` par convention explicite dans la review, pas par moteur de décision.
- Garder `CorrectionStep` indépendante de la politique d’appel (pas de couplage direct à `WAIT_HUMAN`).

## 5. Dette technique assumée

- Duplication des helpers de lecture/validation de variables de contexte dans les steps.
- Variables de contexte encore basées sur des clés string (`Map<String,Object>`) plutôt que contrat typé.
- Orchestrator limité à 2 steps (choix volontaire de phase, non extensible tel quel).
- Sorties prompts/résultats en texte brut sans structuration plus riche.

## 6. Ce que la phase permet maintenant

- Exécuter un flux runtime simple avec steps métier distinctes.
- Produire des artefacts d’analyse, review et correction de manière homogène.
- Arrêter proprement le workflow sur `STOP_FAILURE` ou `WAIT_HUMAN`.
- Propager un résultat final exploitable et observable localement.
- Tester les comportements critiques de chaque step et de l’orchestrator.

## 7. Prochaines étapes naturelles

1. Définir un enchaînement explicite à 3 steps (analysis -> review -> correction) piloté côté orchestration, sans généralisation excessive.
2. Poser la règle d’entrée en correction (quand lancer `CorrectionStep`) côté orchestrator, pas dans les steps.
3. Préparer un premier mécanisme de reprise contrôlée après `WAIT_HUMAN` (toujours sans moteur générique complet).
4. Réduire la duplication technique des helpers de contexte (refactor local et sobre).
5. Stabiliser les conventions de contenu des artefacts pour les phases suivantes.
