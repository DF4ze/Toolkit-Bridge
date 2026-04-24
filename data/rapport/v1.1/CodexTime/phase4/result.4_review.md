# Resultat revue critique d'architecture - Phase 4 Etape 4

## Perimetre relu
- `WorkflowOrchestrator`
- `AnalysisReviewWorkflowRunner`
- `WorkflowArtifactType`
- `GlobalReviewStep`
- `CorrectionStep`
- tests associes

## 1) Faiblesses / points discutables

### [Critique] Le summary peut casser un run pourtant metierement valide
Dans `AnalysisReviewWorkflowRunner`, la generation du summary (`attachWorkflowSummary`) est sur le chemin critique et peut lever une exception (variables manquantes, parse int, echec ecriture fichier). Dans ce cas, on perd le `WorkflowStepResult` original alors que l'execution metier etait potentiellement reussie.

Impact:
- coupling fort entre observabilite documentaire et succes runtime
- risque de faux echec operationnel

### [Majeur] Regression de contrat sur la reprise manuelle
`runCorrectionAfterReview(...)` et `runAnalysisReviewWithOptionalCorrection(...)` exigent maintenant des variables de reporting (`reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`) alors que le runner etait auparavant utilisable sans ces preconditions pour la reprise.

Impact:
- reprise manuelle plus fragile
- couplage runtime a la config documentaire

### [Majeur] Semantique `correctionTriggered` incoherente dans `CorrectionStep.stopFailure`
`CorrectionStep.stopFailure(...)` renseigne toujours `correctionTriggered=true`, y compris quand la correction n'a pas ete effectivement declenchee (ex: contexte invalide, artefacts manquants avant execution Codex).

Impact:
- observabilite trompeuse
- lecture operatoire faussee

### [Mineur] Duplication des cles d'observabilite et des conventions de message
Les cles (`finalDecision`, `correctionTriggered`, `nextAction`, etc.) sont repetees en litteraux dans plusieurs classes (steps, orchestrator, runner).

Impact:
- derive possible des contrats
- maintenance plus couteuse

### [Mineur] Tests encore insuffisants sur les chemins de panne d'observabilite
Les tests couvrent bien nominal / WAIT_HUMAN / STOP_FAILURE, mais ne couvrent pas:
- echec d'ecriture summary
- absence des variables de report au runner
- verification de non-regression (ne pas casser le resultat metier si summary KO)

## 2) Corrections utiles (dans le perimetre, sans nouvelle feature)

1. **Decoupler l'echec summary du resultat metier**
   - encapsuler la generation summary dans un bloc protege (`try/catch`) dans le runner
   - en cas d'echec: conserver et retourner le `WorkflowStepResult` metier, avec eventuelle note `nextAction`/message enrichi minimal

2. **Restaurer une reprise manuelle robuste**
   - rendre la generation summary conditionnelle a la presence des variables de report
   - si variables absentes: ne pas echouer le run, retourner le resultat metier tel quel

3. **Corriger la semantique de `correctionTriggered`**
   - dans `CorrectionStep.stopFailure`, utiliser `false` pour les echecs de precondition
   - reserver `true` aux cas ou la correction a ete effectivement lancee (ou explicitement tentativee)

4. **Stabiliser les cles de contrat**
   - introduire un petit ensemble de constantes partagees (sans nouveau modele complexe), pour eviter la derive des noms

5. **Completer les tests utiles**
   - test runner: summary non ecrivable => resultat metier conserve
   - test runner: variables report absentes => pas d'exception bloquante
   - test correction step: `correctionTriggered` coherent selon type d'echec

## 3) Verification des axes demandes

- Separation config/runtime: **partiellement correcte**, mais actuellement trop couplee via l'ecriture summary obligatoire dans le runner.
- Qualite du modele implemente: **bonne base** (cles simples, lisibles), mais semantique `correctionTriggered` a clarifier.
- Decouplage orchestrator/memoire/tooling/policy/workspace: **bon** sur memoire/tooling/policy/workspace (pas de couplage ajoute). Le principal couplage cree est runtime <-> reporting dans le runner.
- Absence d'ad hoc: **majoritairement oui**, sauf quelques conventions hardcodees repetitives.
- Couplage futur bloquant: **modere** (risque surtout sur robustesse de reprise et gestion d'erreur summary).
- Coherence des noms: **globalement bonne**.
- Lisibilite generale: **bonne**.
- Tests utiles: **bons sur les cas demandés**, incomplets sur la robustesse aux pannes d'observabilite.
- Dette technique introduite: **faible a moderee**, concentree sur error-handling et duplication des cles.
- Risques de refactor futur evitables maintenant: **oui**, avec les 5 corrections ci-dessus.

## 4) Resume final
L'implementation respecte bien l'objectif de lisibilite locale (summary + cles stables) sans sur-architecture. Le point principal a corriger rapidement est de ne pas faire dependre le succes runtime de l'ecriture du summary. En l'etat, la direction est bonne, mais la robustesse operationnelle de la reprise manuelle doit etre consolidee pour eviter une dette de refactor dans la phase suivante.
