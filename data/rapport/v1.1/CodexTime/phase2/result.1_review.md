# Revue critique d'architecture - Dernier lot (WorkflowOrchestrator)

## Portee de la revue

Fichiers relus integralement:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`

Axe de verification:
- separation configuration/runtime
- qualite du modele implemente
- decouplage orchestrator vs memoire/tooling/policy/workspace
- absence d'ad hoc
- couplages futurs
- nommage, lisibilite
- utilite des tests
- dette technique
- risques de refactor evitable

## 1) Faiblesses / points discutables

### P2 - Couverture incomplete des decisions officiellement supportees en test
- Constat: le test de succes ne couvre que `CONTINUE`.
- Impact: un changement accidentel sur la branche `WAIT_HUMAN` ou `STOP_FAILURE` pourrait passer inaperçu.
- Localisation: `WorkflowOrchestratorTest.java` (test `executesStepOnceAndReturnsResultForSupportedDecision`).

### P3 - Couverture partielle des decisions non supportees
- Constat: la branche "unsupported" est testee avec `RETRY_CORRECTION` uniquement, pas avec `FINISH`.
- Impact: risque faible (meme branche `default`), mais la specification demande explicitement les deux rejets.
- Localisation: `WorkflowOrchestratorTest.java` (test `rejectsUnsupportedDecision`).

## 2) Corrections utiles (sans hors-perimetre)

1. Ajouter deux tests de succes distincts:
   - `returnsResultForWaitHumanDecision`
   - `returnsResultForStopFailureDecision`

2. Ajouter un test de rejet explicite pour `FINISH`:
   - `rejectsUnsupportedDecisionFinish`

3. Conserver strictement le design actuel (pas de refactor d'architecture):
   - pas de nouveau contrat
   - pas de moteur
   - pas de multi-step

## 3) Points verifies et juges sains

- **Separation config/runtime**: correcte.
  - Aucun element de configuration ajoute.
  - Classe placee dans `...workflow.runtime.orchestrator`.

- **Qualite du modele implemente**: bonne pour ce lot.
  - Contrat minimal, validation defensive claire (`context`, `step`, `result`).
  - Methode unique, responsabilite unique.

- **Decouplage**: bon.
  - Aucune dependance vers memoire, tooling, policy, workspace, codex, artifacts, admin/web.

- **Absence d'ad hoc specifique**: correcte.
  - Aucune logique metier embarquee.

- **Couplage futur**: faible.
  - Le couplage se limite a `WorkflowStep*` et `WorkflowExecutionContext`, attendu pour cette phase.

- **Nommage et lisibilite**: bons.
  - `WorkflowOrchestrator` et `executeSingleStep(...)` sont explicites.
  - Lecture lineaire, sans abstractions cachees.

- **Dette technique introduite**: faible et contenue.
  - Dette principale: couverture de test a completer sur les branches de decision.

## 4) Resume final

Le lot est **architecturalement propre et conforme au perimetre minimal**: orchestration single-step stricte, sans derive vers un moteur generique.

A corriger de maniere utile et sobre:
- completer la couverture de test pour `WAIT_HUMAN`, `STOP_FAILURE` et `FINISH`.

Hors cela, aucune faiblesse structurelle majeure n'a ete identifiee a ce stade.
