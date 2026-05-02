# Relecture Phase 7 Étape 6 — Sécurité minimale Telegram

## Résumé exécutif

Le lot est dans le bon périmètre : il rend la sécurité minimale **explicite** (doc) et **observable** (warning au démarrage), sans dupliquer la sécurité du module Telegram.

Je ne vois pas de dérive d’architecture.

Verdict : **VALIDATION**.

---

## Points forts

- **Pas de double sécurité** : aucun check userId réimplémenté dans les controllers/services workflow.
- **Découplage correct** : le warning s’appuie sur le `Binder` Spring et ne dépend pas d’API interne du module Telegram.
- **Signal opérationnel utile** : warning non bloquant si la whitelist de `Cortex` est vide.
- **Non-fuite UX** : tests renforcés sur `/workflow` et `/workflow_status` (pas de chemins absolus Windows, pas de token, pas de stack trace).

---

## Points discutables (non bloquants)

1) **Couverture “chemins absolus” limitée à Windows**
- Les tests vérifient l’absence de `:\` (Windows). Ça ne couvre pas un chemin absolu Unix (`/home/...`).
- Contexte actuel Windows : OK. Si le projet vise aussi Linux/containers, ajouter plus tard un check du type `response` ne contient pas `"/"` suivi d’un motif connu (ou meilleure règle, ex: pas de `Path.toAbsolutePath()` dans le renderer).

2) **Warning minimaliste (volontairement)**
- Le warning ne vérifie pas `polling-enabled` ou la présence du `token` (ce n’est pas demandé ici).
- C’est cohérent avec “sécurité minimale” et “ne pas bloquer le démarrage”.

---

## Corrections utiles (optionnelles)

- (Optionnel) Étendre le test “pas de path absolu” avec une règle cross-OS, si le runtime cible n’est pas exclusivement Windows.
- (Optionnel) Ajouter une phrase dans la doc sur le comportement attendu de `/start` (récupération userId), si vous souhaitez expliciter le parcours d’onboarding whitelist.

---

## Résumé final

- Sécurité minimale documentée : OK (`telegram_workflow_security.md`).
- Warning au démarrage whitelist vide `Cortex` : OK.
- Tests non-fuite : OK.
- Aucun impact sur runner/CLI/orchestration/module Telegram.
