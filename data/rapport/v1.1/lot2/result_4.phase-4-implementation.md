# Phase 4 — Implémentation

## 1. Ajustements éventuels de l’analyse
- Aucun ajustement fonctionnel nécessaire avant implémentation.
- Micro-ajustement confirmé: garder `normalize(...)` local au nouveau service extrait pour éviter tout couplage transverse additionnel dans cette phase.

## 2. Service créé
- Service créé: `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryService.java`
- Dépendances injectées:
  - `AgentDefinitionService`
  - `AgentRuntimeRegistry`
  - `AgentPolicyRegistry`
  - `ToolRegistryService`
- Responsabilités déplacées depuis la façade (sans changement de comportement observable):
  - `listAgents()`
  - `toAgentItem(...)`
  - `resolvePolicyWithoutRuntime(...)`
  - `resolveExposedToolsFromPolicy(...)`
  - `toRuntimeItem(...)`
  - `toPolicyItem(...)`
  - `normalize(...)`
- Comportements conservés:
  - branche runtime présent vs runtime absent
  - ordre runtime des `exposedTools` quand runtime présent
  - tri/filtrage policy des `exposedTools` quand runtime absent
  - `runtime == null` dans `AgentItem` si runtime absent
  - fallback `availability = "UNKNOWN"` si null
  - projection `accessibleMemoryScopes` via `Enum::name`

## 3. Modifications réalisées dans la façade
- Fichier modifié: `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- Changements:
  - injection de `AdminAgentQueryService`
  - `listAgents()` transformée en délégation directe vers `adminAgentQueryService.listAgents()`
  - suppression du bloc agents/runtime/policy/tools interne (helpers + mappings)
- `getOverview()` conservée dans la façade avec structure inchangée et appel à `listAgents()` maintenu.
- Aucun changement sur contrôleurs/DTO/domaine runtime-policy-tools.

## 4. Tests ajoutés ou modifiés
- Nouveau test ajouté:
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryServiceTest.java`
  - Couvre:
    - runtime présent: source policy/runtime tools + ordre runtime conservé
    - runtime absent: fallback `policyRegistry.getRequired(...)` + filtrage/tri des tools exposés
    - mapping runtime avec fallback `availability = "UNKNOWN"`
    - mapping policy avec `accessibleMemoryScopes` en `Enum::name`

- Test façade adapté:
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
  - Mise à jour:
    - tests comportementaux `listAgents*` déplacés vers `AdminAgentQueryServiceTest`
    - nouveau test de délégation `listAgents()` côté façade
    - conservation des tests `getOverview*` et autres délégations (tasks/traces/artifacts/config/rétention)

- Tests conservés inchangés:
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminControllerTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageControllerTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardServiceTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/ToolkitBridgeApplicationTests.java`

## 5. Résultats d’exécution des tests
- Commande exécutée:
  - `./mvnw "-Dtest=AdminAgentQueryServiceTest,TechnicalAdminFacadeTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest,ToolkitBridgeApplicationTests" test`
- Résultat global:
  - `BUILD SUCCESS`
  - `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`
- Détail ciblé:
  - `AdminAgentQueryServiceTest`: OK (3 tests)
  - `TechnicalAdminFacadeTest`: OK (8 tests)
  - `TechnicalAdminControllerTest`: OK (2 tests)
  - `TechnicalAdminPageControllerTest`: OK (6 tests)
  - `AdminDashboardServiceTest`: OK (2 tests)
  - `ToolkitBridgeApplicationTests`: OK (1 test)

## 6. Points d’attention
- L’extraction reste volontairement mécanique: aucun changement de logique métier runtime/policy/tools.
- `getOverview()` reste point d’agrégation en façade et dépend toujours du contrat `listAgents()`.
- Le workspace contient d’autres changements non liés (déjà présents) ; ils n’ont pas été modifiés dans cette implémentation.

## 7. Fichiers créés ou modifiés
- Créés:
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryService.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryServiceTest.java`
- Modifiés:
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
