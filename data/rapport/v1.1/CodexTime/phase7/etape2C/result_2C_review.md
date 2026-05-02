# Phase 7 — Etape 2C — Relecture critique (architecture)

Perimetre relu:

* `fr.ses10doigts.toolkitbridge.controler.telegram.workflow.WorkflowTelegramController`
* `fr.ses10doigts.toolkitbridge.service.telegram.workflow.*` (session/store/resolver/roadmap/orchestration)
* tests associes `...telegram.workflow.*Test`

## Points positifs

* Separation correcte: controller Telegram minimal (parsing + delegation) vs services.
* Etat runtime Telegram reste en memoire (`WorkflowTelegramSessionStore`), sans persistance ni couplage au runner.
* Protection contre double-run presente (`tryMarkRunning`).
* Roadmap load: securise (path relatif + `resolveWithinRoot`), et on ne parse pas le Markdown.
* Orchestration 2C respecte les contraintes: appel in-process (pas de CLI), async, timeout fixe, mapping `WAIT_HUMAN/FAILED/COMPLETED`.

## Faiblesses / points discutables

1. **Encodage des messages (mojibake)**
   * Plusieurs messages utilisateurs contiennent `âŒ`, `âœ…`, `ðŸš€` au lieu de `❌`, `✅`, `🚀`.
   * Impact: UX degradee, et risque de duplication future (tests asserts sur des chaines corrompues).

2. **Melange configuration vs runtime dans `WorkflowTelegramOrchestrationService`**
   * `reportRootDirectory` et `reportVersion` sont hardcodes (`data/rapport`, `v1.1`) et deduits via `Path.of(\".\")`.
   * Impact: comportement dependant du working directory, et difficile a configurer proprement en prod/test.
   * Cela casse la separation "configuration applicative" vs "etat runtime Telegram".

3. **Gestion executor / lifecycle**
   * L’executor est cree dans le constructeur du service Spring (single-thread, daemon) et n’est jamais ferme.
   * Impact: fuite de thread a l’arret (mineure) + pas de controle (taille pool, monitoring, nommage standard).

4. **Timeout sans annulation reelle**
   * `CompletableFuture.orTimeout(...).exceptionally(...)` marque `FAILED`, mais n’annule pas le travail du runner.
   * Impact: le runner peut continuer en background et appeler ensuite `completeRun/failRun`, ecrasant l’etat de session (race condition).

5. **Idempotence / correlation de run**
   * Le store conserve `lastRunId` mais `completeRun` / `failRun` ne verifient pas que l’update correspond au run courant.
   * Impact: si 2 executions se chevauchent (timeout + continuation, ou bug), les mises a jour peuvent se melanger.

6. **Couplage direct a une factory non-Spring**
   * `AnalysisReviewWorkflowRunnerFactory.createDefault()` est appele directement dans le constructeur Spring.
   * Impact: contournement de l’injection Spring (pas de remplacement facile en tests d’integration, pas de config, pas de lifecycle).

7. **Robustesse du "target -> analysisSourcePath"**
   * `analysisSourcePath` est deduit automatiquement du target et d’une convention de chemin.
   * Impact: si le fichier `X.analysis.md` n’existe pas a cet emplacement, le workflow echoue immediatement (ce qui est acceptable, mais doit etre explicite cote UX).

8. **Tests utiles mais un peu "optimistes"**
   * Les tests 2C valident surtout le mapping de statut + prevention double-run.
   * Ils ne couvrent pas les chemins d’erreur importants (timeout, WAIT_HUMAN, STOP_FAILURE, missing analysisSourcePath).

## Corrections utiles (sans ajouter de nouvelles fonctionnalites)

1. Fix encodage
   * Remplacer les chaines mojibake par les symboles attendus (`❌/✅/🚀`) dans:
     * `WorkflowTelegramOrchestrationService`
     * `WorkflowTelegramRoadmapService`
     * tests associes

2. Externaliser la configuration (minimale)
   * Deplacer `reportRootDirectory`, `reportVersion`, `timeout` dans une config Spring (properties), ou au minimum les injecter via constructor.
   * Garder `WorkflowTelegramSessionStore` strictement runtime.

3. Lifecycle executor
   * Remplacer l’executor par un bean Spring `TaskExecutor` / `ExecutorService` injectable et ferme proprement.

4. Correlation de run pour eviter les races
   * Faire porter `runId` dans `completeRun/failRun` et ne mettre a jour que si `session.lastRunId == runId`.
   * Sinon ignorer (ou retourner une indication) afin d’eviter l’ecrasement d’etat.

5. Tests additionnels (cibles, pas de feature)
   * Ajouter 2-3 tests unitaires:
     * mapping `WAIT_HUMAN -> WAITING_HUMAN`
     * mapping `STOP_FAILURE -> FAILED`
     * timeout -> `FAILED` + running=false

## Dette technique introduite

* Dette principale: configuration hardcode + executor lifecycle + risque de race condition sur timeout.
* Dette secondaire: encodage / messages "mojibake" (a corriger vite car ca se propage dans les asserts).

## Resume final

L’etape 2C est **fonctionnelle et globalement bien isolee**, mais elle introduit trois fragilites a corriger rapidement:

1. encodage des messages
2. configuration hardcode dans le service d’orchestration
3. gestion timeout/executor pouvant creer des races (etat de session incoherent)

Le reste (separation couches, tests de base, securite roadmap) est coherent avec une progression incrementale.

