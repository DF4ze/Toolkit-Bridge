# Phase 4 — Corrections finales

## 1. Renforts apportés sur les tests
- Renforcement du test runtime présent dans `AdminAgentQueryServiceTest` pour verrouiller explicitement le mapping principal de `AgentItem`:
  - `provider`
  - `model`
  - `orchestrator`
  - `policyName`
  - `toolsEnabled`
  - `runtime`
  - `policy`
  - `exposedTools`
- Ajout d’un test de verrouillage comportemental dédié à la préparation fallback pour policy/tools.

## 2. Verrouillage de parité fallback/runtime
- Ajout du test `listAgentsFallbackBuildsPolicyInputAlignedWithRuntimePreparationContract` dans `AdminAgentQueryServiceTest`.
- Ce test capture l’argument `AgentToolAccess` envoyé à `policy.resolve(...)` en fallback admin (runtime absent), puis vérifie:
  - `enabled` aligné sur `definition.toolsEnabled()`
  - `allowedTools` aligné sur `toolRegistryService.getToolNames()`
  - `registeredTools` aligné sur `toolRegistryService.getToolDescriptors()`
  - `exposedTools` vide à ce stade
- Objectif atteint: verrouiller l’alignement attendu avec les entrées de préparation runtime, sans factorisation ni refonte.

## 3. Documentation locale ajoutée
- Ajout d’un commentaire technique bref dans `resolvePolicyWithoutRuntime(...)` de `AdminAgentQueryService`.
- Le commentaire rappelle que:
  - cette logique est un fallback de projection admin
  - elle doit rester alignée avec les entrées de préparation runtime
  - aucune refonte n’est introduite dans cette phase

## 4. Résultats des tests
- Commande exécutée:
  - `./mvnw "-Dtest=AdminAgentQueryServiceTest,TechnicalAdminFacadeTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest,ToolkitBridgeApplicationTests" test`
- Résultat:
  - `BUILD SUCCESS`
  - `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`
- Détail ciblé:
  - `AdminAgentQueryServiceTest`: OK (4 tests)
  - `TechnicalAdminFacadeTest`: OK (8 tests)
  - `TechnicalAdminControllerTest`: OK (2 tests)
  - `TechnicalAdminPageControllerTest`: OK (6 tests)
  - `AdminDashboardServiceTest`: OK (2 tests)
  - `ToolkitBridgeApplicationTests`: OK (1 test)

## 5. Fichiers modifiés
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryServiceTest.java`
