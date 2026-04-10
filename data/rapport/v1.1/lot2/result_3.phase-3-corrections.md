# Phase 3 — Corrections finales

## 1. Renforts apportés sur les tests

- Renfort ciblé dans `TechnicalAdminFacadeTest` sur un test `getOverview*` existant (`getOverviewAggregatesListsAndCountersWithSanitizedLimit`).
- Ajout de vérifications explicites de délégation:
- `verify(adminConfigQueryService).getConfigurationView()`
- `verify(retentionQueryService).listRetentionPolicies()`
- Objectif atteint: figer la délégation réelle de l’overview sans multiplier des `verify(...)` superflus dans tous les tests.

## 2. Robustesse ajoutée sur la rétention

- `RetentionQueryServiceTest` a été rendu robuste à l’évolution de l’enum.
- L’attendu des familles n’est plus codé en dur: il est dérivé de `PersistableObjectFamily.values()`.
- Le mock `resolver.resolve(...)` est désormais piloté de manière générique par famille, ce qui conserve la vérification de projection tout en réduisant la fragilité du test si l’enum évolue.

## 3. Dette explicitement assumée

- Dette assumée (inchangée, volontaire pour ce lot): couplage direct `query service -> TechnicalAdminView.*`.
- Ce choix est maintenu temporairement pour conserver une extraction mécanique, lisible et sans refonte hors périmètre.
- Aucune refonte supplémentaire n’est attendue à ce stade; ce point pourra être réévalué dans un lot ultérieur si les besoins d’architecture l’imposent.

## 4. Résultats des tests

Commande exécutée:

- `./mvnw.cmd "-Dtest=TechnicalAdminFacadeTest,RetentionQueryServiceTest,AdminConfigQueryServiceTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest" test`

Résultat:

- `BUILD SUCCESS`
- `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`

## 5. Fichiers modifiés

- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryServiceTest.java`
- `data/rapport/v1.1/lot2/result_3.phase-3-corrections.md`
