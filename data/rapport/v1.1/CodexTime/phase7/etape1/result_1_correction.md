# Phase 7 — Etape 1 — Correction — Controller Telegram Workflow

## Corrections appliquees

### 1) Retour sur DefaultController

Annulation de toutes les modifications precedemment introduites dans le controller runtime agent:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultControllerTest.java`

Resultat:

* `DefaultController` ne connait pas `/workflow` et n'a aucune logique d'exclusion associee.

### 2) WorkflowTelegramController conforme au module TelegramBots

Mise en conformite du controller workflow:

* routage par bot porte par l'annotation `@TelegramController(bot = "Cortex")`
  * note: dans la version `telegram-bots-mvc:1.4.4`, l'attribut est `bot` (pas `name`)
* commande declaree via une methode `@Command(value = "/workflow", description = "Workflow interface")`
* suppression de toute logique inutile:
  * plus de `@Chat`
  * plus de parsing manuel de commande
  * plus de gestion manuelle de botId
  * plus de fallback "botId null"

## Fichiers modifies (reellement)

Modifies:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramControllerTest.java`
* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java` (retour etat initial)
* `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultControllerTest.java` (retour etat initial)

Ajoutes:

* `data/rapport/v1.1/CodexTime/Phase7/etape1/1.correction.md`
* `data/rapport/v1.1/CodexTime/Phase7/etape1/result_1_correction.md`

## Tests

* `./mvnw test` execute avec succes.

Tests verifies:

* la methode commande `/workflow` retourne le message attendu
* le controller est bien dedie au bot `Cortex` via annotation
* aucun wiring CLI/workflow/runner: le controller n'a aucune dependance injectee

## Confirmations de contraintes

* Pas de gestion de `/start`, `/help`, `/ping`.
* Aucune modification de `/whoiam`.
* Aucun workflow n'est branche (aucun appel runner/CLI, aucun acces `workflow-summary.md`, aucune gestion `WAIT_HUMAN`).

