# Phase 3 — Implémentation

## 1. Ajustements éventuels de l’analyse

- Aucun ajustement structurel nécessaire avant implémentation.
- Micro-ajustement appliqué: dans les tests de façade, la logique détaillée configuration/rétention a été déplacée vers des tests dédiés des nouveaux services, et la façade est désormais testée en délégation sur ces deux axes.

## 2. Services créés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryService.java`
- Service Spring dédié à `listRetentionPolicies()`.
- Dépendance: `PersistenceRetentionPolicyResolver`.
- Comportement conservé: itération `PersistableObjectFamily.values()`, mapping vers `TechnicalAdminView.RetentionItem`.

- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminConfigQueryService.java`
- Service Spring dédié à `getConfigurationView()`.
- Dépendances: `AgentDefinitionService`, `AdministrableConfigurationGateway`.
- Comportement conservé: projection providers vers `TechnicalAdminView.LlmProviderItem` avec booléen `apiKeyConfigured` uniquement (pas de fuite de clé brute).

## 3. Modifications réalisées dans la façade

Fichier modifié: `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`

- Injection ajoutée:
- `AdminConfigQueryService`
- `RetentionQueryService`

- Injection retirée:
- `AdministrableConfigurationGateway`
- `PersistenceRetentionPolicyResolver`

- Méthodes conservées publiquement et transformées en délégation:
- `getConfigurationView()` -> délègue à `adminConfigQueryService.getConfigurationView()`
- `listRetentionPolicies()` -> délègue à `retentionQueryService.listRetentionPolicies()`

- `getOverview()` conservée en structure et contrat: agrégation inchangée.

- Nettoyage façade:
- suppression des imports/mappings spécifiques configuration/rétention devenus inutiles
- suppression des helpers `isBlank` et `toRetentionItem` (déplacés dans les nouveaux services)
- conservation du bloc agents/runtime/policy/tools et du helper `normalize` inchangés

## 4. Tests ajoutés ou modifiés

Tests ajoutés:

- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryServiceTest.java`
- couvre l’itération sur `PersistableObjectFamily.values()`
- couvre la projection `RetentionPolicy -> TechnicalAdminView.RetentionItem`
- couvre les champs `family`, `ttl`, `disposition`

- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminConfigQueryServiceTest.java`
- couvre le comptage définitions/providers
- couvre le mapping provider -> `LlmProviderItem`
- couvre la non-exposition de la clé API brute via `apiKeyConfigured`

Tests modifiés:

- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- adaptation de la construction de façade aux nouvelles dépendances
- remplacement du test configuration détaillé par un test de délégation façade
- ajout d’un test explicite de délégation `listRetentionPolicies()`
- conservation des tests `getOverview*` avec mocks alignés sur les nouveaux services

Tests conservés inchangés:

- `TechnicalAdminControllerTest`
- `TechnicalAdminPageControllerTest`
- `AdminDashboardServiceTest`

## 5. Résultats d’exécution des tests

Commande exécutée:

- `./mvnw.cmd "-Dtest=TechnicalAdminFacadeTest,RetentionQueryServiceTest,AdminConfigQueryServiceTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest" test`

Résultat:

- `BUILD SUCCESS`
- `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`

## 6. Points d’attention

- L’ordre d’énumération des familles de rétention reste implicitement lié à `PersistableObjectFamily.values()`.
- La façade reste le point d’entrée public, mais sa responsabilité configuration/rétention est désormais strictement délégante.
- Le contrat des contrôleurs et des DTO `TechnicalAdminView` est inchangé.

## 7. Fichiers créés ou modifiés

Créés:

- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminConfigQueryService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminConfigQueryServiceTest.java`
- `data/rapport/v1.1/lot2/result_3.phase-3-implementation.md`

Modifiés:

- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
