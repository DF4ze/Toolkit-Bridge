Parfait, très bon choix 👍
On garde la cohérence des rapports **sans casser l’historique**, tout en revenant sur ce qui manque vraiment.

Voici ta **sous-roadmap Phase 6**, adaptée à TON système actuel (pas théorique).

---

# Mini-Roadmap — Phase 6 (Validation technique & fiabilisation)

## 🎯 Objectif réel

Passer de :

→ workflow pilotable humainement (mais “théorique”)

à :

→ workflow **fiable techniquement et exploitable en conditions réelles**

Sans casser :

* la CLI
* le runner
* le contrat externe
* la logique actuelle

---

# ⚠️ Problème à corriger

Aujourd’hui :

* aucun build Maven exécuté
* aucun test lancé
* aucune validation réelle du code
* aucune limite de correction

➡️ Le système peut valider du code cassé
➡️ Le workflow peut boucler indéfiniment

---

# Étape 1 — Introduire une validation locale minimale

## Objectif

Pouvoir exécuter une commande locale (Maven) depuis le workflow.

## À faire

* créer un service simple :

    * `WorkflowValidationService`
* exécuter :

    * une commande shell
* capturer :

    * code retour
    * stdout / stderr
* retourner :

    * succès / échec

## Important

* pas de moteur shell générique
* pas de multi-commandes
* une seule commande simple au début

---

# Étape 2 — Ajouter un build Maven simple

## Objectif

Valider que le projet compile après implémentation.

## À faire

* exécuter :

  mvn clean compile -DskipTests

* intégrer après :

    * implémentation
    * correction

## Résultat attendu

* si OK → continuer
* si KO → déclencher correction

## Important

* ne pas lancer tous les tests encore
* rester rapide

---

# Étape 3 — Injecter la validation dans le workflow

## Objectif

Brancher la validation dans le cycle existant.

## À faire

* après `implements`
* après `correction`

Ajouter :

* appel validation
* enrichir `WorkflowStepResult` :

    * validationStatus
    * validationMessage

## Important

* ne pas casser le runner
* ne pas modifier le contrat CLI

---

# Étape 4 — Distinguer bug vs blocage humain

## Objectif

Éviter que le système corrige n’importe quoi.

## À faire

* si validation échoue :

    * analyser erreur
    * décider :

### cas 1 — bug corrigeable

→ RETRY_CORRECTION

### cas 2 — doute / ambigu

→ WAIT_HUMAN

## Important

* logique simple (pas d’IA ici)
* règles basiques au début

---

# Étape 5 — Ajouter un compteur de corrections

## Objectif

Éviter les boucles infinies.

## À faire

* ajouter :

    * compteur dans le contexte
* définir :

    * maxTentatives (ex: 3)

## Comportement

* si dépassement :
  → WAIT_HUMAN
  → message explicite

---

# Étape 6 — Stabiliser les états d’arrêt

## Objectif

Avoir des états clairs et exploitables.

## À faire

clarifier :

* SUCCESS
* WAIT_HUMAN
* FAILED
* MAX_RETRY_REACHED

## Important

* éviter les états ambigus
* éviter UNKNOWN

---

# Étape 7 — Améliorer le workflow-summary.md

## Objectif

Rendre le summary exploitable en réel.

## À faire

ajouter :

* résultat validation
* message d’erreur si KO
* nombre de tentatives
* commande RESUME exacte

## Exemple attendu

```
Validation: FAILED
Error: Compilation error in X.java

Attempts: 2 / 3

Next steps:
1. Fix compilation issue
2. Resume with:
   java ... --mode=RESUME ...
```

---

# Ce qu’il ne faut toujours pas faire

* ❌ pas de Telegram
* ❌ pas de multi-user
* ❌ pas de persistance complexe
* ❌ pas de DSL
* ❌ pas de moteur générique

---

# Résultat attendu

Un système :

* qui compile réellement le code
* qui détecte les erreurs
* qui corrige intelligemment
* qui s’arrête quand il faut
* qui reste simple et lisible

---

# 🧠 Insight clé

Phase 5 = **pilotage humain validé**
Phase 6 = **fiabilité technique réelle**

👉 C’est cette phase qui transforme ton projet en **outil exploitable**

---

Si tu veux, prochaine étape logique :

👉 je te fais le **prompt d’analyse Étape 1 Phase 6** directement (comme d’hab)
