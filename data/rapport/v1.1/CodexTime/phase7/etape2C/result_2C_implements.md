# Phase 7 — Etape 2C — Implementation — Run workflow (Telegram -> runner in-process)

## Comportement ajoute

* Orchestration Telegram permettant de lancer le workflow in-process (asynchrone) via:
  * `/workflow_run`
* Commande de lecture d'etat via:
  * `/workflow_status`

Regles respectees:

* aucun appel CLI / `ProcessBuilder`
* aucun changement runner / orchestrateur / contrat CLI
* pas de push Telegram (reponse immediate uniquement)
* pas de parsing du summary (on ne lit pas le fichier, on memorise seulement le path)

## Details techniques

1. `WorkflowTelegramOrchestrationService`
   * resout la cible via `WorkflowTelegramRunTargetResolver`
   * lock via `WorkflowTelegramSessionStore.tryMarkRunning(...)`
   * execute le workflow en async (executor single-thread)
   * timeout fixe 15 minutes
   * mapping de fin:
     * `WAIT_HUMAN` -> `WAITING_HUMAN`
     * `STOP_FAILURE` -> `FAILED`
     * sinon -> `COMPLETED`
   * memorise `workflowSummaryPath` (si present dans `WorkflowStepResult.data`)

2. `WorkflowTelegramController`
   * ajout de:
     * `@Command("/workflow_run")`
     * `@Command("/workflow_status")`
   * parsing minimal `key=value` (project/projectName, phase, etape/step)

## Fichiers crees / modifies

Crees:

* `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`

Modifies:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramControllerTest.java`
* `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRunTargetResolver.java` (bean Spring + UX)

## Tests ajoutes

* `WorkflowTelegramOrchestrationServiceTest`
  * start run -> session COMPLETED
  * double run -> refuse si deja RUNNING
  * status -> invalid request si chatId null

## Tests executes

* `./mvnw test` OK.

## Confirmations

* Aucun workflow n'est execute au demarrage: uniquement sur `/workflow_run`.
* Aucune commande existante n'a ete modifiee (/help, /start, /ping, /whoiam).

