# Result — Analyse Phase 7 Étape 6 — Sécurité minimale Telegram

## Résumé exécutif

La sécurité minimale repose déjà sur 2 mécanismes suffisants pour empêcher un utilisateur non autorisé de piloter le workflow :

1) **Filtrage par bot** via `@TelegramController(bot = "Cortex")` : les commandes workflow ne sont pas exposées sur les autres bots.

2) **Whitelist par bot** via `telegram.bots[].security.allowed-user-ids` (module `telegram-bots-mvc`) : les updates des utilisateurs non autorisés sont rejetés avant exécution des handlers.

À ce stade, **il ne faut pas dupliquer la sécurité** dans les controllers/services.

Les points à surveiller sont surtout :
- une whitelist vide (risque d’ouverture involontaire selon configuration) ;
- l’usage en groupes (un user autorisé peut déclencher un run dans un groupe et exposer les messages à tous les membres).

**Conclusion : prêt**, sous réserve de documenter/valider les comportements exacts (whitelist vide, usage groupe) et de recommander une whitelist non vide en environnement réel.

---

## 1) État actuel de la sécurité Telegram

### Configuration
Exemple dans `src/main/resources/application-template.yml` :
- `telegram.enabled`
- `telegram.default-bot-id`
- `telegram.bots[].id/token/polling-enabled/auto-register-commands/configure-menu-button`
- `telegram.bots[].security.allowed-user-ids`

### Bot isolation (`Cortex`)
Les commandes workflow sont déclarées dans `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java` avec `@TelegramController(bot = "Cortex")`.

Implication : même si un autre bot est configuré, ce controller n’est pas censé être attaché aux autres bots.

### Whitelist
Dans le module `telegram-bots-mvc` (dépendance Maven `fr.ses10doigts:telegram-bots-mvc:1.5.4`), la whitelist est portée par la config `security.allowed-user-ids` sur chaque bot.

Le dispatcher (`TelegramUpdateDispatcher`) contient des checks sur :
- `security.allowedUserIds.isEmpty()`
- `security.allowedUserIds.contains(userId)`
- et un traitement spécial pour la commande `/start`.

### Utilisateur non autorisé
Le module rejette l’accès au niveau dispatcher (message embarqué : `Access refused.`). L’objectif est que les handlers applicatifs ne soient pas invoqués.

### Whitelist vide
Le code du dispatcher teste `isEmpty()` avant `contains(...)`. Le pattern le plus probable est :
- **si la whitelist est vide → filtrage désactivé (tout le monde passe)**
- **si la whitelist est non vide → seuls les ids présents passent**

➡️ À confirmer sur une exécution réelle (log DEBUG module), mais dans tous les cas : **whitelist non vide recommandée en prod**.

### `/start` (récupérer son userId)
Le dispatcher contient une référence explicite à `/start` et un message “Add it to : telegram.allowed-user-ids”.

➡️ Hypothèse plausible : `/start` est autorisé même si l’utilisateur n’est pas whitelisté afin de lui permettre de récupérer son id et l’ajouter à la whitelist. À valider sur un bot réel.

---

## 2) Commandes sensibles (risques)

- `/workflow_run` : **élevé** (déclenche exécution, charge, effets sur disque/report)
- `/workflow_resume` : **élevé** (reprise d’exécution, effets similaires au run)
- `/workflow_roadmap_load` : **moyen/élevé** (accès fichier dans shared, changement de contexte)
- `/workflow_summary` : **moyen** (lecture/affichage d’un fichier de rapport ; risque d’exfiltration vers chat/groupe)
- `/workflow_status` : **faible/moyen** (expose contexte + erreurs ; pas d’exécution)
- `/workflow` : **faible/moyen** (dashboard, mais peut révéler du contexte)

---

## 3) Suffisance de la whitelist existante

- La whitelist du module, si activée (non vide), **couvre déjà toutes les commandes**, puisqu’elle filtre l’update avant dispatch.
- Ajouter une seconde sécurité dans les controllers/services serait de la **duplication** (risque d’incohérences et de drift).

Recommandation :
- conserver une seule barrière (module) et documenter le “must-have” : whitelist non vide.

---

## 4) Configuration à vérifier (minimum)

- `telegram.enabled=true`
- bot `Cortex` présent dans `telegram.bots[]` et son `token` fourni via environnement/secret
- `polling-enabled=true` pour `Cortex`
- `security.allowed-user-ids` non vide en environnement réel
- `auto-register-commands=true` (optionnel mais utile UX)

---

## 5) Fuites d’informations

Constats (état actuel) :
- pas de tokens renvoyés dans les messages ;
- pas de stack traces renvoyées ;
- les vues `/workflow` et `/workflow_status` n’affichent plus de chemins complets ;
- `workflow_summary` lit un fichier mais le path est validé via `WorkspaceLayout.resolveWithinRoot(...)`.

Point produit à surveiller :
- le contenu de `workflow-summary.md` peut contenir des infos sensibles (dépend du workflow). Si le bot est utilisé dans un **groupe**, tout le monde voit ce que le bot poste.

---

## 6) Sécurité des chemins

### `/workflow_roadmap_load`
- résout le path uniquement via `WorkspaceLayout.resolveWithinRoot(sharedRoot, relativePath, ...)`.
- refuse les extensions non `.md`.

➡️ Contournement évident “../” attendu comme bloqué par `resolveWithinRoot`.

### `/workflow_summary`
- n’utilise jamais `Files.readString`.
- ne lit le fichier qu’après :
  - relativisation sous `reportRootDirectory`
  - `resolveWithinRoot(reportRootDirectory, relativeText, ...)`
  - `WorkspaceTextFileService.exists/isRegularReadableFile/readUtf8Text`

➡️ Contournement “path hors reportRootDirectory” attendu comme bloqué.

---

## 7) Sécurité du run async

- double run : protégé par `tryMarkRunning(chatId, runId)`.
- cohérence d’état : protégée par `runId` (late completion ignorée côté store).
- timeout : la session bascule en `FAILED` et une completion tardive ne doit pas pouvoir écraser l’état.
- isolation : session en mémoire par `chatId`.

Risque minimal restant :
- **chat de groupe** : la session est partagée par `chatId` (le groupe). Un user whitelisté peut déclencher un run et tous les membres voient les messages/résumés.

---

## 8) Recommandations minimales

### À faire maintenant (sans implémenter ici)
- Documenter explicitement :
  - “whitelist non vide obligatoire en prod”
  - “éviter l’usage en groupes pour les workflows contenant des infos sensibles”

### À documenter seulement
- Le module `telegram-bots-mvc` gère déjà :
  - le filtrage userId par bot
  - le rejet avant dispatch
  - probablement `/start` pour récupérer son id

### À reporter
- interdiction d’usage en groupe (contrôle `chatType`) côté app
- audit trail / journal d’actions
- rôles / multi-user / approbations

---

## 9) Tests à prévoir (proposition)

Sans toucher au module Telegram :
- tests d’intégration (si le module expose des points d’extension testables) :
  - utilisateur non autorisé → handler non exécuté (ou “Access refused”) ;
  - whitelist vide → comportement explicitement validé (allow-all vs deny-all) ;
- tests applicatifs :
  - vérifier que les réponses `/workflow` et `/workflow_status` n’exposent pas de chemins absolus.

---

## 10) Plan (si corrections minimales nécessaires)

1. Clarifier dans la doc/properties la politique whitelist vide (recommandation : non vide).
2. Ajouter un check de configuration au démarrage (warning log si whitelist vide pour `Cortex`).
3. Documenter “usage recommandé en chat privé” pour éviter fuite en groupes.
4. Ajouter un test de non-régression sur l’absence de paths absolus dans les réponses.
5. (Optionnel, plus tard) bloquer les commandes sensibles en groupe si `ctx` expose le type de chat.

---

## Conclusion

**Prêt** pour la suite, à condition de traiter la sécurité comme “minimale mais explicite” :
- whitelist non vide en environnement réel
- usage en groupes considéré comme un risque de confidentialité (produit)
