# Phase 7 - Etape 5 - Implementation UX Telegram

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRoadmapService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramMessageRenderer.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramControllerTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRoadmapServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryServiceTest.java`

## Messages ameliores

- `/workflow` n'est plus un placeholder. La commande affiche maintenant un mini tableau de bord :
  - statut courant
  - projet
  - phase
  - etape
  - roadmap chargee ou absente
  - commandes disponibles
  - prochaine action recommandee

- `/workflow_status` est devenu une vue synthetique complete :
  - status
  - project
  - phase
  - etape
  - roadmap loaded/missing
  - summary loaded/missing
  - runId
  - startedAt
  - updatedAt
  - lastMessage si present
  - lastError si present
  - next action recommandee

- `/workflow_run`, `/workflow_resume` et `/workflow_roadmap_load` utilisent maintenant un format coherent :
  - succes : `Action effectuee`
  - erreur : `Action impossible`
  - detail
  - next

- les messages de `WorkflowTelegramSummaryService` ont ete nettoyes pour rester lisibles et coherents.

## Logique de recommandation ajoutee

Ajout d'une logique simple de recommandation dans `WorkflowTelegramMessageRenderer` :

- `IDLE` -> `/workflow_roadmap_load` ou `/workflow_run`
- `RUNNING` -> `/workflow_status`
- `WAITING_HUMAN` -> `/workflow_summary puis /workflow_resume`
- `COMPLETED` -> `/workflow_summary`
- `FAILED` -> `/workflow_status puis inspecter l'erreur`

Cette logique est purement informative. Aucun lancement automatique n'a ete ajoute.

## Tests ajoutes ou adaptes

- rendu `/workflow` sans contexte actif
- rendu `/workflow` avec contexte actif
- rendu `/workflow_status` pour `IDLE`, `RUNNING`, `WAITING_HUMAN`, `FAILED`
- recommandation correcte selon statut
- adaptation des tests de `/workflow_run`, `/workflow_resume`, `roadmap_load` et `summary` aux nouveaux messages
- verification que les commandes existantes du module sont toujours presentes dans l'accueil workflow

## Validation des tests

- `./mvnw -q test` : OK

## Confirmation de non-regression fonctionnelle

- aucune logique workflow n'a ete modifiee
- runner inchange
- CLI inchangee
- pas de persistance ajoutee
- pas de parsing roadmap
- pas d'alias courts
- pas de boutons inline
- pas de systeme de template complexe

