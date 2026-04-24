# Mini-Roadmap — Phase 2 : Activation du Workflow

## Objectif global

Passer d’un socle technique à un workflow réellement exécutable :

* une étape
* un enchaînement simple
* un résultat observable

Sans introduire de complexité inutile.

---

# Étape 1 — Orchestrator minimal

## Objectif

Créer une première boucle d’exécution capable de lancer une step.

## À faire

* Introduire un `WorkflowOrchestrator` très simple
* Entrée : `WorkflowExecutionContext`
* Exécution :

    * appeler UNE step
    * récupérer `WorkflowStepResult`
* Aucun enchaînement complexe

## Contraintes

* pas de gestion multi-step
* pas de retry
* pas de boucle
* pas de logique métier
* pas de gestion Telegram

## Résultat attendu

Pouvoir exécuter manuellement une step.

---

# Étape 2 — Première step métier : GlobalAnalysisStep

## Objectif

Créer la première vraie step du workflow.

## À faire

* Implémenter `GlobalAnalysisStep implements WorkflowStep`
* Responsabilités :

    * lire le contexte
    * construire un prompt d’analyse
    * appeler `CodexWorkflowClient`
    * enregistrer :

        * prompt
        * résultat
    * retourner un `WorkflowStepResult`

## Contraintes

* pas de logique de décision avancée
* pas de retry
* pas de parsing intelligent
* pas de workflow complet

## Résultat attendu

Une step réellement exécutée via Codex + artefacts générés.

---

# Étape 3 — Orchestration simple (1 step + décision)

## Objectif

Donner un minimum de “vie” au workflow.

## À faire

* Dans `WorkflowOrchestrator` :

    * exécuter `GlobalAnalysisStep`
    * interpréter `WorkflowStepResult.decision`
    * gérer uniquement :

        * CONTINUE → fin simple
        * WAIT_HUMAN → stop
        * STOP_FAILURE → stop

## Contraintes

* pas de boucle complexe
* pas de gestion multi-lots
* pas de retry automatique
* pas de state machine

## Résultat attendu

Premier workflow exécutable avec décision simple.

---

# Étape 4 — Point d’entrée (test ou runner simple)

## Objectif

Pouvoir déclencher le workflow facilement.

## À faire

* créer un runner simple (main ou test)
* initialiser :

    * `WorkflowRun`
    * `WorkflowExecutionContext`
* lancer l’orchestrator

## Contraintes

* pas de Telegram encore
* pas d’API REST
* pas d’intégration externe

## Résultat attendu

Exécution complète locale :

* step exécutée
* Codex appelé
* fichiers générés

---

# Ce que la Phase 2 ne doit PAS faire

* pas de multi-step avancé
* pas de système de retry complexe
* pas de gestion de questions humaines avancées
* pas de parsing de roadmap
* pas d’intégration Telegram
* pas de validation Maven
* pas de moteur générique

---

# Résultat attendu fin Phase 2

Un workflow minimal fonctionnel :

* 1 orchestrator
* 1 step métier
* 1 appel Codex
* 1 génération d’artefacts
* 1 décision simple

👉 Un vrai “end-to-end” exploitable

---

# Philosophie

Faire fonctionner → puis améliorer

Pas :
Concevoir parfaitement → ne jamais exécuter
