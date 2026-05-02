# Phase 7 — Etape 2C — Correction — Stabilisation orchestration

## Corrections appliquees

1. Encodage
   * Suppression des chaines corrompues dans services/controllers/tests.
   * Messages normalises:
     * `❌`
     * `✅`
     * `🚀`

2. Externalisation configuration
   * Ajout de `WorkflowTelegramProperties` (prefix: `toolkit.telegram.workflow`) avec:
     * `reportRootDirectory`
     * `reportVersion`
     * `timeoutMinutes`
   * `WorkflowTelegramOrchestrationService` utilise ces valeurs au runtime (plus de hardcode).

3. Executor Spring
   * Ajout de `WorkflowTelegramConfiguration` avec un bean:
     * `ExecutorService workflowExecutor` (single-thread, nomme `workflow-telegram-*`, destroyMethod=`shutdown`)
   * Injection dans `WorkflowTelegramOrchestrationService`.

4. Securisation par `runId`
   * `WorkflowTelegramSessionStore.completeRun(...)` et `failRun(...)` prennent maintenant `runId`.
   * Regle: si `session.lastRunId != runId` -> ignore.
   * Regle additionnelle: si la session n'est plus `RUNNING`, toute mise a jour est ignoree.
   * But: eviter qu'un timeout `FAILED` soit ecrase ensuite par une completion tardive.

5. Timeout robuste
   * Timeout conserve (15 minutes par defaut), mais l’ecrasement d’etat est bloque grace aux regles `runId` + `RUNNING`.

6. Tests
   * Ajout d’un test "timeout -> FAILED" + tentative d’ecrasement par completion tardive (ignore).
   * Ajout d’un test store confirmant que les updates sont ignorees hors RUNNING / runId mismatch.
   * Assertions de presence des caracteres `🚀` / `✅` dans les tests existants.

## Fichiers crees / modifies

Crees:
* `src/main/java/fr/ses10doigts/toolkitbridge/config/telegram/workflow/WorkflowTelegramProperties.java`
* `src/main/java/fr/ses10doigts/toolkitbridge/config/telegram/workflow/WorkflowTelegramConfiguration.java`
* `data/rapport/v1.1/CodexTime/phase7/etape2C/2C.correction.md`

Modifies:
* `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
* `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSessionStore.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSessionStoreTest.java`

## Tests executes

* `./mvnw test` OK.

## Confirmation perimetre

* Pas de modification runner / CLI / orchestrateur.
* Pas de nouvelles commandes Telegram.
* Pas de parsing summary.

