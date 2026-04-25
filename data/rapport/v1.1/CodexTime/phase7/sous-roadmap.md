Parfait, très bon choix 👍
Tu arrives au moment où ton projet devient **utilisable en réel**.

Je te fais une roadmap **claire, progressive et sans casser ton existant**.

---

# 🚀 Mini-Roadmap — Phase 7 — Intégration Telegram

## 🎯 Objectif global

Passer de :

```text
CLI → humain local
```

à :

```text
CLI → Telegram → humain distant
```

Sans modifier :

* le workflow
* le runner
* le contrat CLI

👉 Telegram = **simple couche d’interface**

---

# 🧱 Principe d’architecture (très important)

👉 Tu ne touches PAS au moteur

```text
[Workflow Engine]
        ↑
        |
[CLI Adapter]
        ↑
        |
[Telegram Adapter]
```

👉 Telegram appelle la CLI, point.

---

# 📦 Phase 7 — découpage

---

# 🟢 Étape 1 — Bot Telegram minimal (réception / envoi)

## Objectif

Avoir un bot capable de :

* recevoir un message
* répondre

## À faire

* créer module / package :

    * `telegram/`
* config :

    * token
* endpoint :

    * polling (simple)

## Fonctionnalités

* `/start`
* `/ping`

---

## Résultat attendu

👉 Bot vivant
👉 communication OK

---

# 🟢 Étape 2 — Lancer le workflow depuis Telegram

## Objectif

Permettre :

```text
/user → /run → lance CLI
```

## À faire

* appeler :

```text
java ... AnalysisReviewWorkflowCli --mode=RUN ...
```

* récupérer stdout
* parser via `WorkflowCliConsumerSimulator`

## Réponse Telegram

```text
Status: ...
Reason: ...
Actions: ...
```

---

## Important

👉 Ne PAS parser le summary encore
👉 utiliser la sortie CLI

---

# 🟢 Étape 3 — Envoyer le summary dans Telegram

## Objectif

Afficher :

👉 `workflow-summary.md`

## À faire

* lire le fichier via `workflowSummaryPath`
* envoyer :

    * soit en texte
    * soit en fichier Telegram

---

## Gestion longueur

Telegram limite :

```text
4096 caractères
```

👉 prévoir :

* split message
* ou envoi fichier

---

# 🟢 Étape 4 — Gérer WAIT_HUMAN

## Objectif

Permettre :

```text
Bot → WAIT_HUMAN → User répond → Resume
```

---

## À faire

* stocker contexte session :

    * reportRootDirectory
    * phase
    * step
* commande :

```text
/resume
```

→ appelle CLI `--mode=RESUME`

---

## Important

👉 stockage simple :

* Map en mémoire (au début)
* pas de DB

---

# 🟢 Étape 5 — Améliorer UX Telegram

## Objectif

Rendre l’expérience fluide

---

## À faire

* format messages :

    * titre clair
    * emojis légers (optionnel)
* commandes :

```text
/run
/resume
/status
/help
```

* boutons inline (plus tard)

---

# 🟢 Étape 6 — Sécurité minimale

## Objectif

Éviter usage externe

---

## À faire

* whitelist userId Telegram
* ignorer autres utilisateurs

---

# 🟢 Étape 7 — Gestion erreurs

## À gérer

* CLI crash
* timeout
* fichier summary absent

---

## Réponse utilisateur

```text
❌ Workflow failed to execute
```

---

# 🟢 Étape 8 — Stabilisation

## Objectif

Rendre le système fiable

---

## À faire

* logs propres
* retry simple si CLI échoue
* test complet :

```text
RUN → WAIT_HUMAN → RESUME → SUCCESS
```

---

# 🧠 Ce qu’il ne faut PAS faire (très important)

* ❌ ne pas intégrer Telegram dans le runner
* ❌ ne pas modifier CLI
* ❌ ne pas créer un moteur agent dans Telegram
* ❌ ne pas sur-architecturer

---

# 🎯 Résultat final Phase 7

Tu obtiens :

```text
Telegram
   ↓
Command (/run)
   ↓
CLI
   ↓
Workflow Engine
   ↓
Summary
   ↓
Telegram
```

---

# 🔥 Insight clé

👉 Tu ne construis pas un bot

👉 Tu ajoutes :

```text
une interface distante à ton agent
```

---

# 🚀 Suite possible (Phase 8+)

Après ça :

* multi-user
* UI web
* agent multi-rôle
* supervision

---

# 👉 Prochaine étape

Si tu veux, on peut faire :

👉 **prompt d’analyse Étape 1 Telegram** (comme d’hab)

ou

👉 design technique du module Telegram directement

---

Tu veux qu’on démarre l’étape 1 avec un prompt propre ?
