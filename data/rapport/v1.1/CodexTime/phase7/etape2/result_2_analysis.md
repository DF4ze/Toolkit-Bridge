# Phase 7 — Etape 2 — Analyse — Lancer le workflow depuis Telegram (Telegram → CLI → Workflow)

## Resume

L'appel "Telegram → CLI → Workflow" peut etre implemente proprement en ajoutant une petite couche d'adapter cote Telegram qui:

1. construit une commande CLI **sans parametres libres** (valeurs whitelistees / issues de config)
2. execute la CLI avec `ProcessBuilder` + timeout + capture stdout/stderr (pattern deja present dans le depot)
3. parse la sortie avec `WorkflowCliConsumerSimulator` (deja present)
4. rend une reponse utilisateur courte via `WorkflowUserInteractionSimulator` (deja present)

L'objectif de l'etape 2 peut rester minimal: `/workflow run` declenche un RUN avec un contexte fixe (ou quasi fixe) configure en YAML, et renvoie un statut synthese.

## 1) Comment appeler la CLI depuis Telegram ?

### Option A (recommandee pour respecter "Telegram → CLI → Workflow"): ProcessBuilder

Utiliser `ProcessBuilder` pour lancer une vraie execution CLI, comme fait ailleurs dans le projet:

* `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.WorkflowValidationService` (ProcessBuilder + timeout + capture streams)
* `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient` (ProcessBuilder + timeout + capture streams)

Ici, la commande a executer cible `AnalysisReviewWorkflowCli`:

* classe: `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowCli`
* arguments (contract deja stable et teste): `--mode=RUN|RESUME`, `--reportRootDirectory=...`, `--reportVersion=...`, `--reportPhase=...`, `--stepNumber=...`, (RUN) `--analysisSourcePath=...`

Point a clarifier en implementation: quelle forme de "CLI" on execute en prod:

* `java -cp <classpath> fr...AnalysisReviewWorkflowCli ...` (runtime dev)
* `java -jar <toolkit-bridge.jar> ...` (si vous exposez un main/entrypoint qui delegue a la CLI)

### Option B (acceptable mais moins "CLI"): appel in-process

Instancier et appeler `AnalysisReviewWorkflowCli.execute(args)` directement dans la JVM (sans ProcessBuilder).

Avantages: plus simple (pas de gestion classpath / jar), moins fragile sur les environnements.
Inconvenients: ce n'est pas une vraie execution CLI "boite noire", et ca melange Telegram et workflow dans le meme process (meme si le code passe par la couche CLI).

### Pourquoi ne pas reutiliser CodexWorkflowClient ?

`CodexWorkflowClient` est specialise pour lancer le binaire `codex` et capturer stdout/stderr, pas pour lancer une CLI Java.
Son pattern d'execution (ProcessBuilder + timeout + capture) est reutilisable, mais il vaut mieux un service dedie "WorkflowCliExecutor" plutot que detourner un client nomme "Codex".

## 2) Ou placer cette logique ?

Recommandation: **pas dans le controller**.

Le controller Telegram (`WorkflowTelegramController`) doit rester tres fin:

* validation minimale (commande / sous-commande)
* delegation vers un service applicatif

Proposition de placement (coherent avec l'existant):

* controller: `fr.ses10doigts.toolkitbridge.controler.telegram.workflow`
* service adapter Telegram->CLI: `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli` ou `fr.ses10doigts.toolkitbridge.service.telegram.workflow`

Le choix depend de votre vision de couches:

* si vous considerez "CLI execution" comme faisant partie du workflow runtime: placer le service sous `service.agent.workflow.runtime.cli`
* si vous considerez Telegram comme un adapter: placer sous `service.telegram.workflow` et importer la couche CLI consumer (`WorkflowCliConsumerSimulator`)

Dans tous les cas: eviter que le controller construise une ligne de commande complete lui-meme.

## 3) Comment parser la reponse ?

Recommandation: reutiliser `WorkflowCliConsumerSimulator` (deja present).

Justification:

* la CLI imprime un contrat `key=value` (voir `AnalysisReviewWorkflowCli.printResult(...)`)
* `WorkflowCliConsumerSimulator` parse deja: `decision/finalDecision/message/nextAction/waitReason/workflowSummaryPath`
* il existe deja `WorkflowUserInteractionSimulator` pour formatter une reponse utilisateur a partir du resultat parse

Alternative (non recommandee): parser "a la main" stdout.
Risque: duplication et divergence de contrat.

## 4) Que renvoyer a l’utilisateur ?

Etape 2 (connexion au moteur) doit rester simple.

Recommandation: renvoyer une **version simplifiee** (pas stdout brut, pas lecture du summary):

* `Status: <...>`
* `Message: <...>`
* `Reason: <...>` (si WAIT_HUMAN)
* `Next action: <...>`
* optionnel: `Summary: <path>` (sans lire le fichier, juste le chemin)

Vous pouvez obtenir exactement ce format via `WorkflowUserInteractionSimulator.renderMessage(...)`.

## 5) Gestion des erreurs (CLI crash / timeout / exit != 0)

Recommandation: aligner le comportement sur `WorkflowValidationService`:

* timeout: tuer le process + repondre "TIMEOUT" avec une action ("retry" / "increase timeout")
* exit code != 0: remonter un statut "ERROR" + inclure un extrait de stderr (tronque)
* exception au lancement: "SYSTEM_ERROR" (impossible de demarrer le process)

Important Telegram: limiter la taille renvoyee (ex: 3-5k chars) et tronquer stderr/stdout.

## 6) Gestion du contexte (reportRootDirectory / phase / step)

Contraintes: pas de persistance, pas de multi-user.

Recommandation etape 2: contexte **configure** (YAML) et non derive de l'utilisateur:

* `reportRootDirectory`: valeur fixe (ex: `data/rapport`)
* `reportVersion`: fixe (ex: `v1.1`)
* `reportPhase` / `stepNumber`: fixe au depart (ex: `CodexTime/Phase7`, `stepNumber=2`) ou derive de l'arborescence de phase courante si vous avez une convention unique
* `analysisSourcePath`: fixe (ex: `data/rapport/v1.1/CodexTime/Phase7/etape2/2.analysis.md`) si vous voulez que /workflow run declenche exactement le prompt courant

Evolution (etape 3/4): un mapping en memoire "chatId -> dernier contexte" deviendra necessaire pour RESUME / WAIT_HUMAN, mais ce n'est pas demande ici.

## 7) Securite (injection / limiter parametres)

Recommandations minimales:

* **pas de string "shell"**: utiliser `ProcessBuilder(List<String>)` avec arguments separes
* ne jamais interpoler du texte Telegram dans un argument de chemin/commande
* whitelister strictement les modes (`RUN`/`RESUME`) et les parametres autorises
* preferer un set de valeurs en config (paths, timeouts) plutot que des params utilisateur
* conserver la whitelist Telegram existante (allowed-user-ids)

## 8) Impact architecture

* Controller: reste fin, ne connait pas ProcessBuilder.
* CLI: aucun changement (contrat deja teste via `AnalysisReviewWorkflowCliTest`).
* Workflow: aucun changement (runner/orchestrator inchanges).
* Nouveau risque: execution de process depuis l'app (ressources, timeouts, concurrence).

Pour limiter la dette:

* encapsuler l'execution CLI dans un service dedie, testable
* encapsuler les valeurs de contexte dans une config type `@ConfigurationProperties`

## 9) Plan d’implementation (5 a 10 etapes)

1. Definir une config "workflow telegram cli" (paths + defaults + timeout) en YAML et binder avec `@ConfigurationProperties`.
2. Ajouter un service `WorkflowCliExecutor` (ProcessBuilder + timeout + capture stdout/stderr + truncation).
3. Dans le service, utiliser `WorkflowCliConsumerSimulator` pour parser stdout (exitCode + stdout).
4. Ajouter un rendu message via `WorkflowUserInteractionSimulator`.
5. Ajouter une commande Telegram `/workflow run` (toujours sous `@TelegramController(bot="Cortex")`) qui appelle le service et renvoie le rendu.
6. Ajouter tests unitaires:
   * executor: simuler un process (ou isoler la construction de commande) et tester parsing / rendu
   * controller: mock du service, verifie aucune fuite de stdout brut et aucune dependance workflow directe
7. Ajouter gestion timeout/exit!=0 avec message stable.
8. Verifier taille message Telegram (tronquage) + logs sans secrets.

