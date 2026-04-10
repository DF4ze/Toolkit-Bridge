# Phase 2 — Implémentation

## 1. Ajustements éventuels de l’analyse
- Aucun ajustement structurel majeur n’a été nécessaire avant implémentation.
- L’analyse de phase 2 a été appliquée telle quelle : extraction mécanique des 3 blocs query à faible couplage, façade conservée en délégation.

## 2. Services créés
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryService.java`
  - logique extraite de `listRecentTasks(...)`
  - mapping `AdminTaskSnapshot -> TechnicalAdminView.TaskItem`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`
  - logique extraite de `listRecentTraces(...)`
  - branchement global/agent, tri descendant, limit
  - mapping `AgentTraceEvent -> TechnicalAdminView.TraceItem`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/ArtifactQueryService.java`
  - logique extraite de `listRecentArtifacts(...)`
  - règle conservée strictement : `taskId` > `agentId` > global
  - tri descendant, limit
  - mapping `Artifact -> TechnicalAdminView.ArtifactItem`

## 3. Modifications réalisées dans la façade
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
  - injection des nouveaux services :
    - `AdminTaskQueryService`
    - `TraceQueryService`
    - `ArtifactQueryService`
  - `listRecentTasks(...)`, `listRecentTraces(...)`, `listRecentArtifacts(...)` transformées en délégation simple.
  - `getOverview()` conservée dans la façade (contrat inchangé), en s’appuyant sur les méthodes publiques de la façade.
  - blocs hors périmètre conservés intacts :
    - `listAgents()`
    - configuration
    - rétention
    - runtime/policy/tools

## 4. Tests ajoutés ou modifiés
- Nouveaux tests créés
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/ArtifactQueryServiceTest.java`
- Tests façade adaptés
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
  - déplacement de la logique métier tâches/traces/artefacts vers tests des nouveaux services
  - conservation des tests façade sur :
    - délégation des 3 méthodes publiques
    - `getOverview()`
    - contrat global restant pertinent (`listAgents`, configuration)
- Tests conservés inchangés
  - `TechnicalAdminControllerTest`
  - `TechnicalAdminPageControllerTest`
  - `AdminDashboardServiceTest`

## 5. Résultats d’exécution des tests
- Commande exécutée :
  - `./mvnw -q "-Dtest=TechnicalAdminFacadeTest,AdminTaskQueryServiceTest,TraceQueryServiceTest,ArtifactQueryServiceTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest" test`
- Résultat : succès (tests ciblés passants).
- Remarque : warnings JVM/Mockito observés, sans impact sur le résultat.

## 6. Points d’attention
- La phase reste volontairement mécanique : pas d’abstraction commune entre query services.
- Une légère duplication locale (`isBlank`, patterns tri/limit) est assumée pour garder des services simples et lisibles.
- Le contrat des contrôleurs est inchangé via conservation de `TechnicalAdminFacade` comme point d’entrée public.

## 7. Fichiers créés ou modifiés
- Créés
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryService.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/ArtifactQueryService.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/ArtifactQueryServiceTest.java`
  - `data/rapport/v1.1/lot2/result_2_phase-2-implementation.md`
- Modifiés
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
