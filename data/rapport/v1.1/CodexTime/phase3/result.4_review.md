# Revue critique d’architecture — Phase 3 Étape 4 (`CorrectionStep`)

## 1) Faiblesses / points discutables

### [P2] Observabilité de `CORRECTION_RESULT` plus faible que les autres steps
**Constat**
- `CorrectionStep` persiste uniquement `stdout` dans `CORRECTION_RESULT`.
- `GlobalAnalysisStep` et `GlobalReviewStep` conservent `stderr` dans l’artefact résultat (section `[stderr]`).

**Impact**
- Diagnostic plus difficile sur échecs non bloquants ou sorties partielles.
- Incohérence de format entre artifacts de steps, ce qui complique l’exploitation future.

**Localisation**
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStep.java` (lignes ~88-99).

### [P3] Duplication technique des helpers de contexte
**Constat**
- `requiredString/requiredInt/requiredPath/optional*` sont recopiés dans plusieurs steps (`GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`).

**Impact**
- Dette de maintenance (correction de bug ou évolution à répliquer N fois).
- Risque de divergence silencieuse entre steps.

**Localisation**
- `CorrectionStep.java` (lignes ~150-221) + classes steps existantes.

### [P3] Couverture de test correcte mais incomplète côté robustesse de contrat
**Constat**
- Les 4 scénarios demandés sont couverts (+ non-success), c’est bien.
- Il manque des tests de contrat utiles et peu coûteux :
  - variable de contexte manquante (`stepNumber` etc.),
  - vérification explicite du contenu de message `STOP_FAILURE` pour timeout/non-success déjà partiellement couverte,
  - vérification de format artefact correction en présence de `stderr` (selon décision d’observabilité).

**Impact**
- Risque de régression discret sur les validations d’entrée.

**Localisation**
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStepTest.java`.

---

## 2) Vérification des axes demandés

### Séparation configuration / runtime
- Bonne séparation : aucune config nouvelle introduite, logique confinée à la step runtime.

### Qualité du modèle implémenté
- Bonne sobriété : `WorkflowStep` / `WorkflowStepResult` réutilisés sans nouvelle abstraction.
- Décisions limitées à `CONTINUE` / `STOP_FAILURE` comme attendu.

### Découplage orchestrator / mémoire / tooling / policy / workspace
- Correct : pas de dépendance nouvelle sur mémoire, policy ou workspace.
- `WorkflowOrchestrator` inchangé.
- Couplage limité à `CodexWorkflowClient` et `WorkflowArtifactService` (cohérent pour une step).

### Absence de logique ad hoc
- Globalement oui : flux simple, lisible, sans branche métier cachée.
- Point mineur ad hoc : format d’artefact résultat différent des autres steps (voir P2).

### Couplage bloquant pour futures phases
- Pas de couplage structurel bloquant introduit.
- Dette la plus probable : duplication des helpers de parsing contexte.

### Cohérence des noms
- Cohérents et explicites (`CorrectionStep`, `CORRECTION_*`, messages, variables).

### Lisibilité générale
- Bonne lisibilité, structure alignée avec les autres steps.

### Tests réellement utiles
- Oui : les scénarios principaux demandés sont couverts et pertinents.

### Dette technique introduite
- Dette faible à modérée : duplication utilitaire + observabilité moins uniforme.

### Risques de refactor futur évitables maintenant
- Harmoniser la persistance de résultat (stdout/stderr) et réduire la duplication des helpers évitera un refactor transversal plus tard.

---

## 3) Corrections utiles proposées (sans nouvelles fonctionnalités)

1. **Aligner le format de `CORRECTION_RESULT`** sur les autres steps : inclure `stderr` en section dédiée tout en gardant `stdout` comme contenu métier.
2. **Facteur commun léger pour lecture des variables contexte** (dans une utilité runtime interne), pour éviter les duplications entre steps.
3. **Compléter 1-2 tests de contrat** sur variables manquantes dans `CorrectionStepTest`.

Ces corrections restent techniques, n’ajoutent pas de fonctionnalités métier, et réduisent la dette de maintenance.

---

## 4) Résumé final propre

L’implémentation du lot est globalement saine :
- séparation runtime/config respectée,
- découplage conservé,
- step autonome et bornée,
- tests utiles sur les cas principaux.

Points discutables à traiter à coût faible :
- homogénéiser l’observabilité de l’artefact `CORRECTION_RESULT`,
- réduire la duplication des helpers de contexte,
- renforcer légèrement la couverture tests sur les validations d’entrée.

Aucune dérive architecturale majeure n’a été introduite sur ce lot.
