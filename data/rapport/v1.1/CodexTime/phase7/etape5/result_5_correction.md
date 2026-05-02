# Result — Correction Phase 7 Étape 5 — Stabilisation UX Telegram

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramMessageRenderer.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`

## Corrections appliquées

### 1) Summary trop long : message non trompeur

Dans `WorkflowTelegramSummaryService`, le message retourné dépend maintenant de la réalité de l’envoi :

- si un `TelegramSender` est résolu et que `sendDocument(...)` passe :
  - `Summary too long. Complete file sent.`
- si aucun sender n’est disponible (ou exception à l’envoi) :
  - `Summary too long. Complete file could not be sent.`

➡️ L’UX ne prétend plus qu’un document a été envoyé quand ce n’est pas le cas.

### 2) Renderer : suppression des chemins internes

Dans `WorkflowTelegramMessageRenderer` :

- `Roadmap:` affiche uniquement `loaded` / `missing`
- `Summary:` affiche uniquement `available` / `missing`

➡️ `/workflow` et `/workflow_status` n’exposent plus les chemins complets roadmap/summary.

## Tests

- Tests mis à jour pour refléter :
  - le nouveau rendu `Summary: available`
  - l’absence de chemins dans les vues
  - le cas `summary too long` avec et sans sender

- Exécution : `./mvnw -q test` ✅

## Confirmations

- Aucun changement sur : runner, CLI, orchestration, module Telegram.
- Aucune nouvelle commande, aucun alias, aucun bouton inline.
- Aucune logique métier workflow modifiée.
