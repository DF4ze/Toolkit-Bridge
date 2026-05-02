# Phase 7 — Étape 4 — Correction — Encodage messages Telegram

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`

## Chaînes corrigées

Dans `WorkflowTelegramOrchestrationService.java` :
- suppression des chaînes mojibake détectées (ex: `Ã...`, `â...`, `ð...`) dans les messages utilisateurs
- standardisation des messages en chaînes lisibles et robustes :
  - `Workflow started` (sans dépendance emoji)
  - `Resume lance`
  - `Invalid request`
  - `Workflow already running`
  - `Aucun workflow en attente d'action humaine`
  - `Contexte workflow incomplet`

## Encodage du fichier

- `WorkflowTelegramOrchestrationService.java` enregistré en **UTF‑8 sans BOM** (contrôle effectué).

## Test ajouté / ajusté

Dans `WorkflowTelegramOrchestrationServiceTest.java` :
- ajout d’un test robuste : vérifie que `startRun(...)` retourne un message contenant **`Workflow started`** (assertion sans emoji).

## Tests exécutés

- `mvn test` : OK

## Confirmation “pas de changement fonctionnel”

- aucune modification de logique sur :
  - `/workflow_resume`
  - `/workflow_run`
  - `/workflow_status`
  - `/workflow_summary`
  - runner
  - CLI
  - orchestration async / mapping statuts

- correction limitée à l’encodage/affichage des messages (textes) + test de non-régression sur le libellé `Workflow started`.

