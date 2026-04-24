# Mini-Roadmap — Phase 4 : Orchestration pilotée par décision

## Objectif global

Faire évoluer le système d’un enchaînement fixe de steps vers un **workflow piloté par décision**, tout en restant simple, lisible et sans moteur générique.

La Phase 4 doit permettre :

* d’exécuter 3 steps (`Analysis → Review → Correction`)
* de conditionner l’exécution de `CorrectionStep`
* d’introduire une première notion de reprise contrôlée après `WAIT_HUMAN`

⚠️ Toujours sans :

* moteur de workflow générique
* routing dynamique complexe
* abstraction prématurée

---

# Étape 1 — Introduire un enchaînement explicite à 3 steps

## Objectif

Permettre à l’orchestrator d’exécuter :

Analysis → Review → Correction

## À faire

* ajouter une méthode simple dans `WorkflowOrchestrator`

    * type : `executeThreeSteps(...)`
* enchaîner :

    1. `GlobalAnalysisStep`
    2. `GlobalReviewStep`
    3. `CorrectionStep`

## Règle

* si step 1 ≠ CONTINUE → stop
* si step 2 ≠ CONTINUE → stop
* sinon exécuter step 3

## Contraintes

* pas de liste de steps
* pas de boucle
* pas de stratégie configurable

## Résultat attendu

Un workflow linéaire complet à 3 étapes fonctionnel.

---

# Étape 2 — Conditionner l’exécution de `CorrectionStep`

## Objectif

Ne pas exécuter systématiquement `CorrectionStep`.

## À faire

* introduire une condition simple basée sur `REVIEW_RESULT`

### Règle recommandée (V1)

* si le review contient un indicateur de correction (ex :

    * `DECISION: NEED_CORRECTION`
    * ou absence de validation explicite)
      → exécuter `CorrectionStep`

Sinon :
→ ne pas exécuter `CorrectionStep`

## Important

* cette logique doit rester dans l’orchestrator
* ne pas modifier les steps métier

## Contraintes

* pas de parsing complexe
* pas de classification avancée
* simple `contains(...)`

## Résultat attendu

Un workflow qui décide de corriger ou non.

---

# Étape 3 — Introduire une première gestion de reprise après `WAIT_HUMAN`

## Objectif

Préparer la reprise du workflow sans implémenter un système complet.

## À faire

* détecter `WAIT_HUMAN` dans le résultat
* arrêter le workflow proprement
* exposer un message clair

### Simulation simple

* injecter une réponse humaine via `WorkflowExecutionContext.variables`
* relancer uniquement `CorrectionStep`

## Contraintes

* pas de file d’attente
* pas de persistance avancée
* pas de mécanisme de reprise complet

## Résultat attendu

Un workflow capable de :

* s’arrêter
* être relancé manuellement
* continuer après intervention

---

# Étape 4 — Stabiliser les conventions métier

## Objectif

Éviter les dérives dans les artefacts.

## À faire

* standardiser les conventions :

    * `DECISION: WAIT_HUMAN`
    * `DECISION: NEED_CORRECTION`
* stabiliser la structure des prompts
* garantir cohérence des artefacts entre steps

## Contraintes

* pas de parsing sémantique lourd
* pas de DSL

## Résultat attendu

Des artefacts exploitables et cohérents pour les phases suivantes.

---

# Ce que la Phase 4 ne doit PAS faire

* pas de moteur générique de workflow
* pas de liste dynamique de steps
* pas de routing avancé
* pas de retry automatique
* pas de système complet de reprise
* pas d’intégration Telegram
* pas de validation Maven
* pas de plugin system

---

# Résultat attendu fin Phase 4

Un workflow piloté par décision :

```text
Analysis → Review → (STOP | WAIT_HUMAN | Correction)
```

Avec :

* exécution conditionnelle
* arrêt contrôlé
* reprise simple
* comportement lisible

---

# Philosophie

Toujours :

* explicite
* simple
* testable

Jamais :

* générique trop tôt
* abstrait sans besoin réel
* difficile à comprendre

---

👉 À la fin de la Phase 4, ton système ne sera plus seulement un pipeline intelligent, mais un **workflow décisionnel piloté**, prêt à accueillir de l’interaction humaine réelle.
