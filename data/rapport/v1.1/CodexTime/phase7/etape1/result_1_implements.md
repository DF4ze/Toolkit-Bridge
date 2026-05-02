# Phase 7 — Etape 1 — Implementation — Controller Telegram Workflow minimal

## Fichiers crees / modifies

Crees:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramControllerTest.java`
* `data/rapport/v1.1/CodexTime/Phase7/etape1/1.implements.md`

Modifies:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultControllerTest.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/ToolkitBridgeApplicationTests.java`

## Comportement ajoute

* Ajout d'un controller Telegram dedie au canal workflow: `WorkflowTelegramController`.
* Une seule commande geree: `/workflow`.
* Reponse: `Workflow Telegram interface is available but not connected yet.`
* Isolation par botId:
  * si `TelegramUpdateContext.getBotId()` est renseigne et different de `Workflow`, le message est ignore (retour `null`)
  * si le botId n'est pas disponible (null/blank), le controller peut repondre sur `/workflow`

Important:

* Aucun appel au runner, a la CLI, ou au workflow n'est fait.
* Aucune lecture de `workflow-summary.md`.
* Aucune gestion de `WAIT_HUMAN`.

## Isolation vis-a-vis de DefaultController

Le `DefaultController` (runtime agent) ignore maintenant explicitement la commande `/workflow` afin de laisser la place au controller dedie workflow et surtout d'eviter d'envoyer `/workflow` au runtime agent.

## Tests ajoutes

* `WorkflowTelegramControllerTest`:
  * `/workflow` retourne le message attendu
  * commande inconnue ignoree
  * botId non correspondant ignore quand disponible
* `DefaultControllerTest`:
  * verifie que `/workflow` est ignore par `DefaultController` et qu'aucun appel a `AgentRuntimeService` n'est fait

## Confirmation: commandes existantes non modifiees

* Aucune logique n'a ete ajoutee pour `/start`, `/help`, `/ping`.
* Aucune modification de `/whoiam` (aucune occurrence modifiee dans le code projet).

## Confirmation: aucun workflow branche

* Aucun appel a `AnalysisReviewWorkflowCli` (ou autre CLI) n'a ete ajoute.
* Aucun appel au runner/orchestrateur workflow n'a ete ajoute.

## Verification

* `./mvnw test` passe.

