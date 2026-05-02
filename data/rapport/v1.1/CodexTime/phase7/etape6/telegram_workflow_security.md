# Sécurité minimale — Telegram workflow (Phase 7)

## Portée

Cette note décrit la sécurité **minimale** attendue pour le pilotage du workflow via Telegram dans `Toolkit-Bridge`.

Objectif : éviter qu’un utilisateur non autorisé puisse déclencher ou inspecter un workflow depuis Telegram, **sans ajouter** de système de rôles ni de double sécurité côté application.

## 1) Isolation par bot

Les commandes workflow sont rattachées au bot `Cortex` via :

- `@TelegramController(bot = "Cortex")`

Conséquence : les commandes workflow ne sont pas exposées sur les autres bots configurés.

## 2) Contrôle d’accès par whitelist

La sécurité repose sur la whitelist du module Telegram :

- `telegram.bots[].security.allowed-user-ids`

Le module filtre les updates **avant** d’appeler les handlers applicatifs.

Recommandation :
- en environnement réel, la whitelist doit être **non vide**.

Note :
- une whitelist vide peut ouvrir l’accès à tous (selon le comportement du module). Même si le module change, une whitelist non vide est la configuration la plus sûre.

## 3) Usage en groupe (risque de confidentialité)

Même avec whitelist, un utilisateur autorisé peut déclencher un workflow dans un **groupe** (chatId groupe). Les messages et summaries postés par le bot seront visibles par tous les membres du groupe.

Recommandation :
- utiliser le bot workflow en **chat privé** quand les summaries/rapports peuvent contenir des informations sensibles.

## 4) Pas de double sécurité

Il ne faut pas dupliquer la sécurité (userId checks) dans les controllers/services workflow :

- éviter les divergences entre “barrières”
- éviter la dette de maintenance

## 5) Vérification au démarrage

L’application logge un warning si :

- le bot `Cortex` est configuré
- et `allowed-user-ids` est vide

Ce warning ne bloque pas le démarrage.
