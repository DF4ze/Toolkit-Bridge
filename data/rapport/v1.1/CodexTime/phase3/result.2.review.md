# Revue critique d’architecture — Phase 3 Étape 2

## Périmètre relu

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`

## Vérifications demandées

### 1) Séparation claire entre configuration et runtime
- Conforme.
- `WorkflowOrchestrator` reste 100% runtime, sans dépendance de configuration, sans état interne, sans couplage Spring.

### 2) Qualité du modèle implémenté
- Conforme au modèle existant.
- La nouvelle méthode `executeTwoSteps(...)` réutilise `executeSingleStep(...)` et conserve `WorkflowStepResult` comme contrat unique.

### 3) Découplage orchestrator / mémoire / tooling / policy / workspace
- Conforme.
- Aucun couplage ajouté vers mémoire, policy, tooling, workspace, Telegram ou Maven.

### 4) Absence de logique ad hoc ou trop spécifique
- Globalement conforme.
- La logique est volontairement spécifique à 2 steps (ce qui est attendu dans cette étape) sans glisser vers un moteur générique.

### 5) Absence de couplage bloquant les futures phases
- Conforme à court terme.
- L’API `executeTwoSteps(...)` reste simple et n’empêche pas une évolution ultérieure, tout en évitant la sur-conception maintenant.

### 6) Cohérence des noms
- Conforme.
- Noms explicites (`executeTwoSteps`, `firstStep`, `secondStep`).

### 7) Lisibilité générale
- Bonne.
- Contrat clair, flux lisible en 5 étapes, sans branchement inutile.

### 8) Tests réellement utiles
- Bons et ciblés.
- Les 4 scénarios demandés sont couverts et valident bien le comportement fonctionnel attendu.

### 9) Dette technique introduite
- Faible.
- Dette principalement côté couverture de tests périphériques (voir points discutables), pas de dette structurelle.

### 10) Risques de refactor futur encore évitables maintenant
- Risques faibles et évitables avec 1-2 tests complémentaires (sans changer l’architecture).

---

## 1. Faiblesses / points discutables

1. Couverture incomplète des validations `null` de `executeTwoSteps(...)`.
- La méthode valide `context`, `firstStep`, `secondStep`, mais ces cas ne sont pas testés explicitement.

2. Cohérence contextuelle mineure dans les tests.
- `buildContext()` utilise `targetStepRef = "phase-2/step-1"` alors que les tests ajoutés concernent l’étape 2 de phase 3.
- Ce n’est pas bloquant techniquement, mais c’est un léger bruit de lisibilité documentaire.

---

## 2. Corrections utiles proposées (sans hors périmètre)

1. Ajouter des tests unitaires simples pour `executeTwoSteps(...)` avec paramètres `null` (`context`, `firstStep`, `secondStep`) et assertions sur `NullPointerException`.
2. Ajuster la valeur de `targetStepRef` de `buildContext()` dans le test pour refléter `phase-3/step-2` (cohérence de lecture uniquement).

Aucune autre correction architecturale nécessaire sur ce lot.

---

## 3. Rappel de contrainte respectée

Aucune proposition ci-dessus n’ajoute de fonctionnalité hors périmètre.

---

## 4. Résumé final

Le lot est architecturalement sain, minimal et conforme à la trajectoire de phase 3 :
- pas de moteur caché,
- pas de nouvelle abstraction,
- pas de couplage transverse.

La méthode `executeTwoSteps(...)` est propre, lisible, et correctement bornée.
Les tests ajoutés sont utiles et valident le cœur du comportement attendu.
Les seuls ajustements recommandés sont mineurs (robustesse test `null` + cohérence de libellé de contexte), sans impact sur l’architecture.
