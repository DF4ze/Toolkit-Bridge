# Mini-Roadmap — Phase 5 (version corrigée)

## Objectif réel

Passer de :

→ workflow local pilotable

à :

→ workflow **appelable et exploitable depuis l’extérieur**

Sans modifier le cœur runtime.

---

# Étape 1 — Exposer le runner existant

## Objectif

Utiliser directement :

* `AnalysisReviewWorkflowRunner`

comme point d’entrée applicatif.

## À faire

* créer un point d’accès simple (classe ou service léger)
* déléguer directement au runner
* ne pas introduire de logique métier

## Important

* ne pas créer de couche abstraite inutile
* ne pas remplacer le runner

---

# Étape 2 — Stabiliser le contrat réel

## Objectif

Formaliser ce qui est déjà produit :

* `WorkflowStepResult`
* `workflow-summary.md`

## À faire

* documenter les clés utilisées :

  * `finalDecision`
  * `correctionTriggered`
  * `nextAction`
  * `waitReason`
* standardiser leur usage

## Important

* ne pas créer de DTO complexe
* ne pas dupliquer les données

---

# Étape 3 — Ajouter un point d’entrée externe simple

## Objectif

Permettre un appel hors test.

## Options simples

* main Java
* commande CLI
* endpoint REST minimal (optionnel)

## À faire

* appeler le runner
* retourner:

  * decision
  * message
  * summary path

---

# Étape 4 — Exploitation humaine réelle (Terminé ✓)

## Objectif

Permettre à un humain de piloter le workflow.

## À faire

* utiliser `workflow-summary.md` (Terminé ✓)
* standardiser : (Terminé ✓)

  * les messages (Terminé ✓)
  * les actions à faire (Terminé ✓)
* tester le cycle complet : (Terminé ✓)

  * run → WAIT_HUMAN → edit → reprise (Terminé ✓)

---

# Étape 5 — Préparer Telegram (sans l’implémenter)

## Objectif

Rendre le système prêt à être branché.

## À faire

* garantir que tout est lisible via :

  * texte simple
  * summary
* vérifier :

  * aucun besoin de parsing complexe
  * aucune dépendance implicite

---

# Ce qu’il ne faut toujours pas faire

* pas de moteur d’agent
* pas de DSL
* pas de routing dynamique
* pas de persistence complexe
* pas de multi-user

---

# Résultat attendu

Un système :

* appelable de l’extérieur
* pilotable humainement
* prêt pour Telegram
* sans modification du runtime
