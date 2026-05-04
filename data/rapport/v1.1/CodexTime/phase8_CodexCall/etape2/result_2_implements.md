# Résultat — Phase 8 CodexCall Étape 2 — Commande Telegram `/workflow_project_set`

Date: 2026-05-03  
Projet: `Toolkit-Bridge` / `CodexTime`

## Objectif atteint

Ajout de la commande Telegram :

`/workflow_project_set name=<projectName> path="<projectPath>"`

qui permet de créer / mettre à jour un mapping `projectName → projectPath` via le registre persistant, avec parsing des paths quotés (espaces supportés), et sans fuite de path absolu dans les messages Telegram.

## Fichiers créés / modifiés

### Production

- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramProjectService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramMessageRenderer.java`

### Tests

- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramProjectServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramControllerTest.java`

## Commande Telegram ajoutée

Dans `WorkflowTelegramController` :

- handler `@Command("/workflow_project_set")` qui délègue à `WorkflowTelegramProjectService`.

## Service Telegram dédié

`WorkflowTelegramProjectService` :

- appelle `WorkflowProjectRegistryService.registerOrUpdate(name, path)`
- mappe le résultat vers :
  - succès : `WorkflowTelegramMessageRenderer.successMessage(...)`
  - erreur : `WorkflowTelegramMessageRenderer.errorMessage(...)`
- masque les raisons qui ressembleraient à des paths absolus (sanitization minimale).

## Parsing `path=` avec quotes

Implémentation localisée dans `WorkflowTelegramProjectService` (à partir de `ctx.getArgs()` via le controller) :

- support `path=D:\\...` (un token)
- support `path="... ..."` en concaténant les tokens jusqu’à la quote de fin
- support `path='... ...'`
- quote non fermée → erreur `Invalid quoted path (missing closing quote)`
- path manquant → erreur `Missing project path`

Note : les paths avec espaces **non quotés** ne sont pas supportés (conforme au prompt).

## UX / non-fuite

- Messages succès/erreur suivent le format standard renderer.
- Les messages succès ne contiennent pas `D:\\`, `C:\\` ni `/home/` (tests inclus).

## Dashboard `/workflow`

`WorkflowTelegramMessageRenderer.workflowHome(...)` liste maintenant :

- `- /workflow_project_set`

Sans modifier la logique `Next`.

## Tests ajoutés

- `WorkflowTelegramProjectServiceTest` :
  - name manquant / path manquant
  - parsing path non-quoté / double-quoté / single-quoté
  - quote non fermée
  - registry failure → message erreur
  - non-fuite sur messages succès
  - dashboard contient `/workflow_project_set`
- `WorkflowTelegramControllerTest` mis à jour pour inclure la nouvelle dépendance service.

## Validation

- Tests exécutés : `./mvnw test`
- Résultat : OK

## Confirmations (contraintes)

- Aucun changement sur `/workflow_run`.
- Aucun changement sur `/workflow_resume`.
- Aucune injection `codexWorkingDirectory` (ce sera une étape ultérieure).
- Aucun changement sur `CodexWorkflowClient`.
- Aucun workflow lancé automatiquement, aucune roadmap chargée automatiquement.
- Pas de WebUI, pas d’abstraction `AgentCoder`.

