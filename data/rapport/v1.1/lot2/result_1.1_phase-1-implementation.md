# Phase 1 — Implémentation

## 1. Ajustements éventuels de l’analyse
- Aucun ajustement fonctionnel majeur n’a été nécessaire.
- Le plan d’analyse de phase 1 était cohérent avec le dépôt et a pu être implémenté tel quel.

## 2. Modifications réalisées
- Renforcement de `TechnicalAdminFacadeTest` avec des tests comportementaux ciblés sur :
  - `listAgents()`
  - `listRecentArtifacts()`
  - `getOverview()`
- Conservation stricte du périmètre :
  - pas d’extraction de service,
  - pas de modification des contrôleurs,
  - pas de modification des DTO.
- Correction locale de la logique fragile de comptage des erreurs dans `TechnicalAdminFacade#getOverview`.

## 3. Tests ajoutés ou modifiés
- Fichier modifié : `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- Tests ajoutés :
  - `listAgentsUsesRuntimePolicyAndExposedToolsWhenRuntimeExists`
  - `listAgentsFallsBackToPolicyRegistryWhenRuntimeIsMissing`
  - `listAgentsFallbackNormalizesAndSortsExposedTools`
  - `listRecentArtifactsUsesTaskFilterFirstAndAppliesSortingAndLimit`
  - `listRecentArtifactsUsesAgentFilterWhenTaskFilterIsBlank`
  - `listRecentArtifactsFallsBackToGlobalWhenNoFiltersAreProvided`
  - `getOverviewAggregatesListsAndCountersWithSanitizedLimit`
  - `getOverviewCountsOnlyErrorTraceTypeForRecentErrors`
- Test existant conservé :
  - `configurationViewDoesNotExposeApiKeyValue`

## 4. Correction apportée sur `ERROR`
- Fichier modifié : `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- Changement réalisé dans `getOverview()` :
  - Avant : `trace.type() != null && trace.type().name().equals("ERROR")`
  - Après : `trace.type() == AgentTraceEventType.ERROR`
- Portée : minimale, locale, sans impact de contrat externe.

## 5. Résultats d’exécution des tests
- Commande exécutée :
  - `./mvnw -q "-Dtest=TechnicalAdminFacadeTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest" test`
- Résultat : succès (tests ciblés passants).
- Remarque : warnings JVM/Mockito observés, sans échec de test.

## 6. Points d’attention
- Les nouveaux tests restent orientés comportement observable (contrats de sortie + choix de branche principaux), pour limiter le couplage à l’implémentation interne.
- La règle de priorité des filtres artefacts est maintenant verrouillée par tests (taskId > agentId > global).
- Le comptage des erreurs de traces est désormais robuste vis-à-vis du type enum.

## 7. Fichiers modifiés
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- `data/rapport/v1.1/lot2/1.1_result_phase-1-implementation.md`
