# Revue critique d’architecture — Dernier lot implémenté (`executeThreeSteps`)

## 1) Faiblesses / points discutables

1. **Duplication de pattern d’orchestration (2 steps vs 3 steps)**
- `executeTwoSteps` et `executeThreeSteps` répètent le même schéma (exécuter, vérifier `CONTINUE`, arrêter sinon).
- Ce n’est pas une erreur, mais c’est une dette de maintenance si une 4e arité apparaît.

2. **Couverture de test non parfaitement symétrique sur les décisions bloquantes**
- Les cas demandés sont couverts et valides.
- Il manque explicitement la variante `step1 = STOP_FAILURE` et `step2 = WAIT_HUMAN` pour la méthode 3 steps (comportement implicite déjà correct via `!= CONTINUE`).

3. **Lisibilité de contexte de test légèrement datée**
- `buildContext()` garde `targetStepRef = "phase-3/step-2"` alors que le lot testé est en phase 4.
- Impact fonctionnel nul, mais petite ambiguïté documentaire.

## 2) Corrections utiles proposées (sans nouvelle fonctionnalité)

1. Ajouter deux tests unitaires complémentaires :
- blocage step1 par `STOP_FAILURE`,
- blocage step2 par `WAIT_HUMAN`.

2. Ajuster la valeur de `targetStepRef` dans le helper de test pour refléter le lot courant.

3. Garder la duplication `executeTwoSteps` / `executeThreeSteps` comme **dette assumée de court terme** tant que la contrainte de minimalisme s’applique (pas de refactor structurel maintenant).

## 3) Vérification des axes demandés

- **Séparation configuration/runtime** : conforme, aucun glissement vers la config.
- **Qualité du modèle** : bonne, contrat `WorkflowStepResult` inchangé et cohérent.
- **Découplage orchestrator/mémoire/tooling/policy/workspace** : bon, orchestrator reste pur runtime et indépendant.
- **Absence de logique ad hoc** : bonne, règle unique `CONTINUE` sinon arrêt.
- **Couplage gênant futur** : faible, principalement duplication d’arité.
- **Cohérence des noms** : bonne (`executeThreeSteps`, `first/second/thirdStep`).
- **Lisibilité générale** : très lisible, progression linéaire explicite.
- **Tests utiles** : oui, scénarios critiques et validations null présents.
- **Dette technique introduite** : légère et localisée.
- **Risques de refactor évitables maintenant** : faibles, surtout renforcer la symétrie de tests.

## 4) Résumé final

Le lot est globalement propre et conforme aux objectifs :
- extension minimale,
- pas de moteur générique,
- bon découplage,
- tests pertinents,
- comportement stable et prévisible.

Les seuls ajustements recommandés sont mineurs (compléments de tests + clarté de donnée de test), sans modification d’architecture ni ajout de fonctionnalité.
