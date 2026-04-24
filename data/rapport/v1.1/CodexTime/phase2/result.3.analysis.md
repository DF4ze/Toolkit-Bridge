Tu travailles sur le projet Toolkit-Bridge.

Phase 2 — Étape 3

Objectif :
analyser la mise en place d’un runner minimal permettant d’exécuter réellement le workflow.

## Analyse uniquement — ne pas modifier le code

Tu dois :

* proposer comment exécuter `WorkflowOrchestrator` + `GlobalAnalysisStep`
* définir un point d’entrée simple (main ou test)
* définir comment initialiser `WorkflowExecutionContext`
* définir les variables minimales nécessaires
* proposer un plan d’implémentation

Tu ne dois PAS :

* créer de code
* implémenter quoi que ce soit

## Attendu

Une analyse claire qui couvre :

1. point d’entrée recommandé (main, test, autre)
2. construction du contexte
3. initialisation des variables
4. instanciation des dépendances
5. exécution orchestrator + step
6. sortie observable attendue

## Contraintes

* solution simple
* pas d’API REST
* pas de Telegram
* pas de framework supplémentaire
* pas d’abstraction inutile

Objectif :
pouvoir lancer le workflow localement et voir les artefacts générés.
