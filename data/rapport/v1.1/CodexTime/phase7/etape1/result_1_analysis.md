# Phase 7 — Etape 1 — Analyse — Bot Telegram minimal (reception / reponse)

## Resume executif

Le projet est deja equipe d'une integration Telegram operationnelle (dependance Maven + configuration `telegram.*` + controleur Telegram en polling). L'etape "bot minimal" peut donc s'appuyer sur l'existant, en ajoutant une couche de commandes minimale (`/start`, `/ping`, optionnel `/help`) idealement isolee sur un botId dedie (ex: `Workflow`) et/ou un controleur dedie, afin d'eviter toute interference avec les bots Telegram deja utilises pour le runtime agent.

Conclusion: **partiellement pret**. Techniquement pret cote Telegram (stack deja en place), mais il faut cadrer l'isolation (botId dedie / routage) et surtout corriger un risque majeur deja present: des tokens Telegram sont actuellement en clair dans `src/main/resources/application.yml`.

## Etat actuel du projet cote Telegram

### 1) Dependances

Une dependance Telegram est deja declaree dans `pom.xml`:

* `fr.ses10doigts:telegram-bots-mvc:1.4.4`

### 2) Configuration

`src/main/resources/application.yml` contient deja une configuration Telegram complete:

* `telegram.enabled: true`
* `telegram.default-bot-id`
* `telegram.bots[]` (plusieurs bots) avec:
  * `id`
  * `token`
  * `polling-enabled`
  * `auto-register-commands`
  * `configure-menu-button`
  * `security.allowed-user-ids`

Note importante: des **tokens** Telegram sont presents en clair dans `application.yml` (risque securite + risque de fuite git / partage).

### 3) Code deja present

Le projet contient deja un controleur Telegram:

* `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java`
  * annote `@TelegramController`
  * handler `@Chat` recevant un `TelegramUpdateContext`
  * delegue au runtime agent via `AgentRuntimeService.processTelegramMessage(...)`

Il existe aussi une couche "supervision Telegram" (ex: `TelegramSupervisionProperties`, `TelegramSupervisionMessagePublisher`, etc.) avec un toggle `telegram.enabled` utilise via `@Value("${telegram.enabled:true}")`.

## Emplacement recommande pour la couche Telegram (phase 7 / workflow)

Le depot a deja un "canal Telegram" oriente runtime agent (`controler.telegram.DefaultController` + `service.agent.runtime.AgentRuntimeService`).

Pour la phase 7 (interface distante du workflow CLI), je recommande de **ne pas reutiliser** ce controleur, et de creer une couche dediee afin de separer clairement:

* Telegram "agent runtime chat" (existant)
* Telegram "workflow CLI remote UI" (phase 7)

Emplacements possibles, du plus coherent au plus minimal vis-a-vis de l'existant:

1. **Recommande (clean architecture dans l'esprit du cadre)**:
   * `fr.ses10doigts.toolkitbridge.adapter.telegram.workflow` (ou `...interface.telegram.workflow`)
   * y mettre le controleur Telegram et un petit service de routage de commandes
2. **Minimaliste et coherent avec le naming actuel**:
   * `fr.ses10doigts.toolkitbridge.controler.telegram.workflow`

Dans les deux cas, l'objectif est d'eviter que des "commandes workflow" se melangent au handler chat generique deja en place.

## Reutiliser un module existant, package interne, ou dependance TelegramBots ?

Etat actuel: le projet depend deja d'une lib Telegram (`telegram-bots-mvc`) et l'utilise via annotations (`@TelegramController`, `@Chat`) et polling.

Recommandation pour l'etape 1:

* **Reutiliser l'existant** (dependance + mecanisme polling + configuration `telegram.*`).
* Creer un **package interne** pour le bot minimal "workflow".
* Ne pas introduire maintenant une nouvelle dependance (ex: module officiel `org.telegram:telegrambots`) tant que l'existant repond au besoin minimal.

La raison principale est de limiter le risque et la complexite (un seul framework Telegram dans l'appli).

## Choix technique: polling vs webhook (ou autre)

Recommandation: **polling (long polling)**.

Justification pragmatique:

* deja utilise (`polling-enabled: true` dans `application.yml`)
* pas besoin d'exposer un endpoint public (donc pas de config TLS / reverse proxy / secret path Telegram)
* plus simple pour demarrer un bot minimal et valider "reception + reponse"

Webhook ne devient pertinent que lorsque vous voulez:

* une infra exposee publiquement et stable
* une maitrise plus fine des latences / scaling

## Fichiers / classes a creer pour un bot minimal

Objectif etape 1: demarrer, recevoir une commande, repondre.

Je recommande le trio suivant:

1. Un controleur Telegram dedie "workflow minimal"
   * ex: `WorkflowTelegramController` (package dedie ci-dessus)
   * role: parser `/start`, `/ping`, `/help` et renvoyer une reponse simple
2. Une classe de configuration de properties (si on veut isoler du `telegram.*` existant)
   * ex: `WorkflowTelegramProperties`
   * role: `enabled`, `botId`, `allowedUserIds` (optionnel) pour filtrer
   * alternative minimaliste: reutiliser `telegram.bots[].security.allowed-user-ids` deja present
3. Un petit service de routage / logique pure (facile a tester)
   * ex: `WorkflowTelegramCommandHandler`
   * role: transformer `(command, userId, chatId)` -> `String response`

Note: pour eviter interference avec les bots deja existants, la strategie la plus robuste est:

* ajouter un **botId dedie** (ex: `Workflow`) dans `telegram.bots[]`
* le controleur "workflow" ne repond que si `ctx.getBotId()` correspond a ce botId

## Configuration minimale a prevoir

Minimum recommande (alignement avec ce qui existe deja dans `application.yml`):

* `telegram.enabled` (global)
* `telegram.bots[]`
  * `id` (botId)
  * `token` (secret)
  * `polling-enabled: true`
  * `security.allowed-user-ids` (whitelist minimale)

Optionnel mais utile:

* `telegram.bots[].username` (si la lib l'exploite; sinon pas necessaire)
* `telegram.default-bot-id` (si vous avez plusieurs bots et une notion de "default")
* `workflow.telegram.enabled` (toggle local a la fonctionnalite phase 7), si vous voulez pouvoir desactiver l'interface workflow sans desactiver Telegram pour le runtime agent

Point d'architecture / securite:

* les tokens ne devraient pas etre commites en clair dans `src/main/resources/application.yml`.
  * a deplacer vers un mecanisme de secret (env var, fichier ignore, vault, etc.)

## Comment eviter de coupler Telegram au workflow des cette etape ?

Principe: **Telegram doit rester un canal UI** sans connaitre le runner / orchestrateur.

Concretement a l'etape 1:

* aucune reference a `AnalysisReviewWorkflowCli`, au runner, ni a des objets workflow
* le controleur Telegram ne fait que:
  * reconnaitre la commande
  * renvoyer une reponse statique ou une reponse "health/ping"
* pas de lecture/ecriture d'artifacts (pas de `workflow-summary.md`)
* pas de gestion WAIT_HUMAN

En pratique, cela revient a implementer un "echo" structure:

* `/start` -> message de bienvenue minimal + rappel des commandes
* `/ping` -> `pong`
* `/help` -> liste des commandes

## Tester le bot sans appeler reellement Telegram

Approches compatibles avec un bot minimal:

1. **Tests unitaires purs** (recommande)
   * tester `WorkflowTelegramCommandHandler` (pas Spring, pas reseau)
   * entree: texte, userId, chatId
   * sortie: message reponse
2. **Tests unitaires du controleur** (sans reseau)
   * instancier le controleur et passer un `TelegramUpdateContext` mocke (Mockito)
   * verifier la string retournee (et `null` quand non applicable)
3. **Tests Spring MVC** ne sont pas adaptes ici (ce n'est pas un endpoint HTTP), sauf si la lib expose une surface HTTP, ce qui ne semble pas etre le cas d'apres le code actuel.
4. **MockWebServer** (present en dependance test) n'est interessant que si vous voulez simuler l'API HTTP Telegram et que la lib permet de configurer l'URL de base (a verifier avant de choisir cette voie).

## Risques d'architecture identifies

1. **Risque majeur securite**: tokens Telegram en clair dans `src/main/resources/application.yml`.
2. **Interference entre controleurs Telegram**: le projet a deja un handler `@Chat` (DefaultController). Ajouter un second handler sans strategie claire (botId dedie / ordre / "return null") peut provoquer:
   * reponses en double
   * un handler qui "mange" les commandes de l'autre
3. **Couplage accidentel**: tendance a glisser rapidement vers "Telegram appelle le workflow" (etape 2) dans le meme code que l'etape 1. Il faudra separer strictement les packages / services.
4. **Configuration etendue / fragmentation**: presence simultanee de `telegram.*` (runtime bots) et `toolkit.telegram.supervision.*` (supervision) + un toggle global `telegram.enabled`. Il faut eviter d'ajouter un 3e style de config incoherent.
5. **Nommage / couches**: package `controler` (typo) existe deja; corriger partout serait un refactor large. Pour phase 7, mieux vaut rester coherent avec l'existant et limiter la portee.

## Plan d'implementation (5 a 10 etapes max)

1. Decider l'isolation: botId dedie `Workflow` (recommande) vs reuse d'un bot existant.
2. Ajouter la config minimale (token, polling-enabled, whitelist) pour ce bot dans un fichier de config non versionne ou injecte par environnement.
3. Creer un package dedie `...telegram.workflow`.
4. Creer `WorkflowTelegramCommandHandler` (logique pure) + tests unitaires (start/ping/help + unknown).
5. Creer `WorkflowTelegramController` (`@TelegramController`) qui:
   * ignore si `ctx.getBotId()` != bot workflow
   * route vers `WorkflowTelegramCommandHandler`
6. Verifier le demarrage applicatif (Spring) sans utiliser la CLI / workflow.
7. Ajouter un test d'integration leger de demarrage de contexte (si vous voulez verrouiller le wiring Spring) sans reseau.
8. Ajuster la journalisation pour rester exploitable (INFO minimal, pas de secrets dans les logs).

## Conclusion

**Partiellement pret pour implementation.**

* Pret: le socle Telegram est deja present (dependance, config, polling, controleur).
* A cadrer avant d'implementer l'etape 1: isolation du bot "workflow minimal" vis-a-vis du runtime agent, et gestion des secrets (tokens).

