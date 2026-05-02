# Result — Analyse Phase 7 Étape 9 — Point WebUI workflow

## Résumé exécutif

La WebUI actuelle (admin + technique) **n’expose pas** et **ne pilote pas** le workflow Codex au sens Telegram (`/workflow_*`).

Elle fournit principalement :
- une UI admin (agents / LLMs / bots Telegram)
- une UI technique en lecture (agents states, tasks, traces, artifacts, configuration, retention)
- quelques endpoints REST utilitaires (`/api/admin/*`, `/api/files/*`, `/api/command/run`)

Donc :
- **UX Telegram** : pilotage workflow (run/status/summary/resume/roadmap)
- **UX Web** : supervision technique générique (tasks/traces/artifacts), pas de commandes workflow

Conclusion : **WebUI absente pour le workflow** (à date). Elle est “partiellement prête” uniquement si on considère la supervision (traces/tasks) comme une visibilité indirecte.

---

## 1) Inventaire WebUI existant

### Pages / routes (Thymeleaf)
Templates sous `src/main/resources/templates/` :
- `login.html` (login admin)
- `admin/dashboard.html`
- `admin/ui-preview.html`
- `admin/agents/*` (list/detail/form)
- `admin/llms/*` (list/detail/form)
- `admin/telegram-bots/*` (list/detail)
- `admin/technical/*` :
  - `overview.html`
  - `agents.html`
  - `tasks.html`
  - `traces.html`
  - `artifacts.html`
  - `configuration.html`
  - `retention.html`

### Controllers web (pages)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/LoginController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/AdminPageController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/agent/AgentAdminPageController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/llm/LlmAdminPageController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/telegram/TelegramBotAdminPageController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageController.java`

### Endpoints REST (utilisés par la WebUI / admin)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminController.java` (`/api/admin/technical/*`)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/functional/*` (agents, llms, telegram-bots)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/auth/AuthController.java` (`/api/auth/me`)

### Endpoints utilitaires (potentiellement sensibles)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/AgentBashController.java` (`/api/command/run`)
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/AgentFileController.java` (`/api/files/*`)

Ces endpoints ne sont pas une UI workflow, mais peuvent permettre d’agir sur le filesystem/commandes via le tooling.

---

## 2) État workflow visible côté WebUI

La WebUI “Technique” affiche :
- **Tasks** (id, objective, agent, status, traceId, errorMessage, artifactCount) via `admin/technical/tasks.html`
- **Traces** (occurredAt, type, agentId, taskId, source, runId, attributes) via `admin/technical/traces.html`
- **Artifacts** (id, type, agent, task, storage, dates, size) via `admin/technical/artifacts.html`

Elle n’affiche pas explicitement (workflow Codex Telegram) :
- project / phase / etape
- lastSummaryPath
- roadmap active
- état WAITING_HUMAN au sens du workflow Telegram

Ce qui est visible indirectement :
- des `runId` de traces (mais pas le `runId` Telegram `tg-...` ni un modèle de “workflow run”)
- des erreurs / statuses de tasks (mais ce ne sont pas les statuts `WorkflowTelegramRunStatus`)

---

## 3) Interaction workflow côté WebUI

Aucun controller web/admin n’expose d’actions :
- “run workflow Codex”
- “resume after WAITING_HUMAN”
- “load roadmap”
- “show workflow summary”

Donc : **WebUI = lecture/supervision**, pas de pilotage workflow.

---

## 4) Comparaison Telegram / WebUI

| Capacité     | Telegram | WebUI |
| ------------ | -------- | ----- |
| dashboard    | oui      | partiel (admin/tech overview, pas workflow) |
| run          | oui      | non   |
| status       | oui      | partiel (tasks/traces, pas workflow) |
| summary      | oui      | non   |
| resume       | oui      | non   |
| roadmap load | oui      | non   |

---

## 5) Architecture recommandée (pour une future WebUI)

Ne pas réutiliser directement les services Telegram workflow :
- ils sont centrés sur une session **par chatId** (modèle Telegram) et un renderer Telegram.

Approche recommandée :
- créer une couche “workflow UI-agnostic” (ex: `WorkflowRunService` / `WorkflowRunFacade`) qui expose :
  - start/resume/status/summary/roadmap-load via un modèle **indépendant de Telegram**
  - un état “workflow run” non confondu avec `WorkflowTelegramSessionStore`

Stratégie prudente :
- commencer WebUI en **read-only** (liste des runs / statuts / liens vers artifacts/summaries)
- n’ajouter les actions (run/resume) qu’avec un cadrage sécurité strict (auth + restrictions + logs)

---

## 6) Risques

- **Duplication** : si WebUI réimplémente la logique Telegram (session store, renderer, locking), on duplique les invariants.
- **Couplage** : confusion entre état Telegram (par chatId) et “état global” côté Web.
- **Sécurité** : exposer des actions `run/resume` sur le web augmente la surface (auth, CSRF, audit).
- **Exposition infos** : risque de fuite (paths, secrets) si la sanitization Telegram n’est pas réutilisée côté Web.

Note : les endpoints `/api/files/*` et `/api/command/run` sont puissants ; ils ne constituent pas une UX workflow mais ils augmentent l’impact d’une exposition Web mal sécurisée.

---

## 7) Recommandations

### À faire maintenant
- Documenter explicitement : “WebUI ne pilote pas le workflow Codex ; seul Telegram le fait actuellement.”
- Clarifier dans la doc Phase 7 que l’état workflow Telegram est **in-memory** et **par chatId** (déjà présent côté Telegram).

### À reporter
- Implémentation d’une WebUI workflow read-only (page “workflow runs”, liens artifacts/summaries, statut)
- Puis actions contrôlées (run/resume) après cadrage sécurité

---

## 8) Plan futur possible (WebUI workflow) (5–10 étapes)

1. Définir un modèle “workflow run” générique (indépendant de Telegram).
2. Exposer une API read-only (liste/dernier run/status/summary link) basée sur artifacts/traces ou un store dédié.
3. Ajouter une page Thymeleaf (dashboard workflow read-only).
4. Ajouter des liens vers summaries/artifacts (sans paths absolus).
5. Ajouter sanitization côté Web identique à Telegram (ou composant partagé).
6. Ajouter l’action “run” derrière auth admin + confirmation.
7. Ajouter l’action “resume” derrière préconditions strictes.
8. Ajouter tests d’intégration web + contrôle d’accès.

---

## Conclusion

**WebUI absente pour le workflow Codex** (pilotage). Elle offre uniquement une supervision générique (tasks/traces/artifacts) qui peut aider au debug mais ne remplace pas l’UX Telegram.
