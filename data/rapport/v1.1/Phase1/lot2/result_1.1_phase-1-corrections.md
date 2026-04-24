# Phase 1 — Corrections finales

## 1. Corrections apportées sur les tests
- Mise à jour de `TechnicalAdminFacadeTest` pour conserver les vérifications utiles aux règles de branchement critiques.
- Conservation des `verify(...)` sur :
  - fallback policy quand runtime absent (`listAgentsFallsBackToPolicyRegistryWhenRuntimeIsMissing`)
  - priorité des filtres artefacts (`taskId` > `agentId` > global)
- Allègement des vérifications non indispensables dans `getOverviewAggregatesListsAndCountersWithSanitizedLimit` au profit d’assertions sur le résultat observable.

## 2. Réduction du couplage à l’implémentation
- Suppression des `verify(...)` internes de `getOverview...` qui figeaient trop le détail d’appel des dépendances.
- Les assertions principales reposent désormais davantage sur le contrat de sortie (`Overview` : compteurs, tailles, cohérence).
- Le couplage de test est recentré sur les règles métier de branchement explicitement attendues.

## 3. Correction Mockito
- Correction du risque de faux positif sur `never().getRequired(anyString())`.
- Vérification remplacée par une version couvrant aussi le cas `null` : `never().getRequired(any())`.

## 4. Nettoyage effectué
- Suppression de l’import inutilisé `isNull`.
- Petite factorisation de lisibilité : ajout d’un helper `facadeWith(...)` pour éviter la répétition du constructeur long de `TechnicalAdminFacade` dans les tests.

## 5. Résultats des tests
- Commande exécutée :
  - `./mvnw -q "-Dtest=TechnicalAdminFacadeTest,TechnicalAdminControllerTest,TechnicalAdminPageControllerTest,AdminDashboardServiceTest" test`
- Résultat : succès, tous les tests ciblés passent.

## 6. Fichiers modifiés
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- `data/rapport/v1.1/lot1/result_1.1_phase-1-corrections.md`
