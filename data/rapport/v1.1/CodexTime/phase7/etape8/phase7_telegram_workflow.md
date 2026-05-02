# Phase 7 — Telegram workflow (stabilisation)

## Commandes (bot: Cortex)

- `/workflow` : tableau de bord (etat + contexte + next)
- `/workflow_roadmap_load path=<file.md> [project=<name>]` : charge une roadmap depuis `workspace/shared`
- `/workflow_run [project=<name>] [phase=<n>] [etape=<n>]` : lance le workflow en async
- `/workflow_status` : etat courant
- `/workflow_summary` : affiche le summary (chunks) ou envoie le fichier si trop long
- `/workflow_resume` : reprend uniquement si etat `WAITING_HUMAN`

## Configuration

### 1) Configuration du module Telegram

Ces proprietes configurent les bots, le polling, et la whitelist (securite).

- `telegram.enabled`
- `telegram.default-bot-id`
- `telegram.bots[].id`
- `telegram.bots[].token`
- `telegram.bots[].polling-enabled`
- `telegram.bots[].auto-register-commands`
- `telegram.bots[].configure-menu-button`
- `telegram.bots[].security.allowed-user-ids`

### 2) Configuration du workflow Telegram

Ces proprietes configurent le runtime workflow cote application (distinct du module Telegram).

Prefix : `toolkit.telegram.workflow`

- `toolkit.telegram.workflow.reportRootDirectory` (defaut: `data/rapport`)
- `toolkit.telegram.workflow.reportVersion` (defaut: `v1.1`)
- `toolkit.telegram.workflow.timeoutMinutes` (defaut: `15`)

Note importante :
- le prefix `toolkit.telegram.workflow.*` n'est pas sous `telegram.*`.

## Securite minimale

- Les commandes workflow sont rattachees au bot `Cortex` via `@TelegramController(bot = "Cortex")`.
- Le filtrage userId repose sur la whitelist du module Telegram : `telegram.bots[].security.allowed-user-ids`.
- Une whitelist non vide est recommandee en environnement reel.
- Un warning au demarrage est logge si `Cortex` a une whitelist vide.

## Limites connues

- Session en memoire (par `chatId`) : perdue au redemarrage de l'application.
- Usage en groupe : le bot poste dans le chat (les summaries peuvent etre visibles par tous).
- Summary : peut contenir des informations sensibles (utiliser de preference en chat prive).
- Pas de persistance.
- Pas de roles/multi-user avance.
- Pas d'automatisation d'etape / pas de "phase suivante".
- Pas de parsing complet de roadmap.

## Notes d'erreurs

- Les erreurs du runner sont loggees cote serveur avec contexte.
- Les messages stockes en session et visibles dans Telegram sont sanitisés (stacktrace/paths/secrets + longueur borne).
- Les patterns quota/rate-limit sont detectes et converts en message user-friendly.
