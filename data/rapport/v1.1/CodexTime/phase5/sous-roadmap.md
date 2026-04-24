# Mini-Roadmap — Phase 5 : Pilotage externe du workflow

## Objectif global

Faire passer le système d’un workflow local, testable et observable à un workflow **pilotable de l’extérieur**, tout en conservant :

* la simplicité actuelle,
* la séparation des responsabilités,
* et l’absence de moteur générique prématuré.

La Phase 5 doit permettre :

* de déclencher un workflow à distance,
* de relancer une reprise manuelle après `WAIT_HUMAN`,
* d’exposer proprement les informations utiles à un canal externe,
* sans intégrer tout de suite une architecture lourde.

⚠️ Toujours sans :

* moteur d’agents complet,
* bus d’événements,
* orchestration distribuée,
* système multi-utilisateur complexe.

---

# Étape 1 — Introduire un service applicatif de pilotage du workflow

## Objectif

Créer une façade simple permettant de piloter le workflow sans exposer directement les classes runtime internes.

## À faire

* Introduire un service applicatif du type :

    * `WorkflowCommandService`
    * ou `WorkflowRuntimeService`
* Ce service doit proposer des actions simples :

    * lancer le workflow complet
    * relancer la correction après `WAIT_HUMAN`
    * retourner un résultat exploitable

## Contraintes

* pas de logique métier nouvelle
* pas de moteur
* pas de duplication avec le runner
* le service délègue au runner/runtime existant

## Résultat attendu

Un point d’entrée propre côté application, prêt à être appelé par Telegram plus tard.

---

# Étape 2 — Définir un contrat d’entrée/sortie minimal pour un canal externe

## Objectif

Stabiliser ce qu’un canal externe doit fournir et recevoir.

## À faire

* définir les entrées minimales :

    * chemin ou référence du contexte de travail
    * mode de lancement :

        * run complet
        * reprise correction
* définir les sorties minimales :

    * décision finale
    * message
    * `workflowSummaryPath`
    * `nextAction`
    * artefacts utiles

## Contraintes

* pas de DTO géant
* pas de modèle complexe
* pas de sérialisation distribuée avancée

## Résultat attendu

Un contrat applicatif simple, stable, consommable plus tard par Telegram ou autre.

---

# Étape 3 — Brancher un premier point d’entrée externe simple

## Objectif

Permettre de déclencher le workflow hors tests, sans encore intégrer Telegram.

## À faire

* choisir un point d’entrée simple :

    * commande locale
    * endpoint technique minimal
    * point d’entrée d’admin simple
* l’utiliser pour :

    * lancer le workflow
    * lancer la reprise après `WAIT_HUMAN`

## Contraintes

* pas d’interface complète
* pas de dashboard
* pas de couche complexe

## Résultat attendu

Le workflow peut être déclenché “pour de vrai”, en dehors d’un test JUnit.

---

# Étape 4 — Préparer l’intégration Telegram sans la déployer totalement

## Objectif

Faire en sorte que le workflow soit déjà prêt à être piloté par Telegram, sans brancher encore toute la boucle.

## À faire

* stabiliser les messages retournés
* stabiliser le summary
* définir ce qu’un message Telegram devra afficher :

    * décision
    * raison
    * action suivante
    * fichier à consulter / relancer
* vérifier que le service applicatif fournit déjà ces infos

## Contraintes

* pas encore de bot métier complet
* pas de parser de commandes avancé
* pas de gestion de conversation complexe

## Résultat attendu

Le système est “Telegram-ready” sans dépendre encore du bot.

---

# Ce que la Phase 5 ne doit PAS faire

* pas de moteur agent complet
* pas de système multi-workflow configurable
* pas de bus d’événements
* pas d’orchestration distribuée
* pas de gestion multi-utilisateur complète
* pas de persistance métier lourde
* pas de dashboard riche
* pas de DSL de workflow

---

# Résultat attendu fin Phase 5

Un workflow :

* déclenchable depuis l’extérieur,
* relançable après `WAIT_HUMAN`,
* pilotable via un service applicatif clair,
* prêt à être branché à Telegram.

```text
Canal externe
    ↓
WorkflowCommandService
    ↓
AnalysisReviewWorkflowRunner
    ↓
WorkflowOrchestrator + Steps
    ↓
Artifacts + workflow-summary.md
```

---

# Philosophie

Toujours :

* explicite
* pilotable
* testable
* sobre

Jamais :

* générique trop tôt
* abstrait sans besoin réel
* plus complexe que nécessaire

---

👉 À la fin de la Phase 5, ton système ne sera plus seulement un workflow local solide :
il deviendra un **workflow pilotable en situation réelle**, prêt pour une interaction humaine à distance.
