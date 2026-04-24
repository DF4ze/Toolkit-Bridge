# Mini-Roadmap — Phase 3 : Enchaînement minimal du workflow

## Objectif global

Passer d’un workflow exécutable avec une seule step à un workflow capable d’enchaîner plusieurs steps simples, sans introduire de moteur générique, de boucle complexe ni de logique de décision avancée.

La Phase 3 doit rester pragmatique :

* valider le chaînage réel des steps
* garder les responsabilités propres
* préparer la suite sans sur-architecture

---

# Étape 1 — Introduire une deuxième step métier : `GlobalReviewStep`

## Objectif

Ajouter une seconde step simple, dédiée à la relecture globale du résultat produit par `GlobalAnalysisStep`.

## À faire

* créer `GlobalReviewStep implements WorkflowStep`
* lire les artefacts générés par l’analyse :

    * `ANALYSIS_PROMPT`
    * `ANALYSIS_RESULT`
* construire un prompt de review simple
* appeler `CodexWorkflowClient`
* écrire :

    * `REVIEW_PROMPT`
    * `REVIEW_RESULT`
* retourner un `WorkflowStepResult`

## Contraintes

* pas de correction automatique
* pas de boucle
* pas de parsing sémantique avancé
* pas de décision humaine complexe
* pas de logique de pilotage global

## Résultat attendu

Une deuxième vraie step métier, simple et bien bornée, branchée sur les artefacts de la première.

---

# Étape 2 — Étendre légèrement l’orchestrator pour enchaîner 2 steps

## Objectif

Permettre un premier enchaînement réel :

* analyse
* review

## À faire

* garder l’orchestrator minimal
* ajouter une exécution séquentielle très simple de 2 steps
* interpréter uniquement :

    * `CONTINUE`
    * `WAIT_HUMAN`
    * `STOP_FAILURE`

## Contraintes

* pas de liste dynamique de steps
* pas de moteur de transitions
* pas de retry
* pas de multi-lots
* pas de state machine

## Résultat attendu

Un workflow linéaire minimal capable d’exécuter :

1. `GlobalAnalysisStep`
2. `GlobalReviewStep`

et de s’arrêter proprement au premier échec ou blocage.

---

# Étape 3 — Ajouter un runner local de phase 3

## Objectif

Exécuter réellement le chaînage de 2 steps et observer les artefacts générés.

## À faire

* faire évoluer le runner local existant
* instancier :

    * `GlobalAnalysisStep`
    * `GlobalReviewStep`
    * `WorkflowOrchestrator`
* exécuter les 2 steps dans l’ordre
* vérifier :

    * artefacts d’analyse générés
    * artefacts de review générés
    * résultat final cohérent

## Contraintes

* pas de Telegram
* pas d’API REST
* pas de framework supplémentaire
* pas de système de lancement générique

## Résultat attendu

Première exécution locale réelle d’un workflow à plusieurs steps.

---

# Étape 4 — Introduire un premier vrai arrêt fonctionnel : `WAIT_HUMAN`

## Objectif

Préparer la future interaction humaine sans encore brancher Telegram.

## À faire

* définir un cas simple où `GlobalReviewStep` peut retourner `WAIT_HUMAN`
* faire en sorte que l’orchestrator s’arrête proprement sur ce cas
* vérifier le comportement dans les tests / runner

## Contraintes

* pas de boucle de questions/réponses
* pas de transport humain réel
* pas d’intégration Telegram dans cette phase
* juste un arrêt propre et observable

## Résultat attendu

Le workflow sait maintenant :

* continuer
* échouer
* se mettre en attente humaine

sans complexité excessive.

---

# Ce que la Phase 3 ne doit PAS faire

* pas de moteur générique de workflow
* pas de routing de steps
* pas de retry automatique
* pas de correction automatique
* pas de parsing intelligent de roadmap
* pas de multi-lots
* pas de validation Maven
* pas d’intégration Telegram
* pas de système pluginable

---

# Résultat attendu fin Phase 3

Un workflow minimal mais déjà crédible :

* 2 steps métier réelles
* un chaînage simple
* des artefacts produits à chaque étape
* un arrêt propre sur `WAIT_HUMAN` ou `STOP_FAILURE`

👉 À la fin de la Phase 3, le système ne sera plus seulement “exécutable” mais déjà **capable de dérouler un mini workflow complet**.

---

# Philosophie

Enchaîner vraiment avant de généraliser.

Pas :
concevoir un moteur complet en avance

Mais :
faire tourner un flux simple, observer, puis élargir proprement
