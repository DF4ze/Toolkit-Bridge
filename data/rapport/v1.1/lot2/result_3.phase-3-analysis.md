# Phase 3 — Analyse d’extraction configuration et rétention

## 1. Fichiers et classes concernés

- Façade cible: `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- Méthodes à extraire: `getConfigurationView()` (lignes ~87-105) et `listRetentionPolicies()` (lignes ~107-112)
- Agrégation indirecte impactée: `getOverview()` (lignes ~114-143) car elle appelle les deux méthodes ci-dessus
- Contrôleurs à conserver inchangés (consommateurs de la façade):
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageController.java`
- Dépendances directes bloc configuration:
- `AgentDefinitionService` (`findAll()`)
- `AdministrableConfigurationGateway` (`loadOpenAiLikeProviders()`)
- `OpenAiLikeProperties` (projection provider -> DTO)
- Dépendances directes bloc rétention:
- `PersistenceRetentionPolicyResolver` (`resolve(PersistableObjectFamily)`)
- `PersistableObjectFamily.values()`
- `RetentionPolicy` (projection vers DTO)
- DTO manipulés (à ne pas modifier):
- `TechnicalAdminView.ConfigItem`
- `TechnicalAdminView.LlmProviderItem`
- `TechnicalAdminView.RetentionItem`
- `TechnicalAdminView.Overview` (contient `configuration` et `retention`)
- Services/objets connexes utiles pour analyser le couplage:
- `AgentDefinitionService` (charge la config admin des agents via `AdministrableConfigurationGateway`)
- `PersistenceRetentionPolicyResolver` et `PersistenceRetentionProperties` (règles de rétention famille/domaine)

Tests existants impactés:

- Façade:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- Cas direct configuration: `configurationViewDoesNotExposeApiKeyValue`
- Cas indirect overview: `getOverviewAggregatesListsAndCountersWithSanitizedLimit`, `getOverviewCountsOnlyErrorTraceTypeForRecentErrors`
- Contrôleurs (contrat façade inchangé, impact faible):
- `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminControllerTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageControllerTest.java`
- Service consommateur indirect de `getOverview()`:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardService.java`
- Test associé:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardServiceTest.java`

## 2. Analyse des blocs configuration et rétention

Bloc configuration (`getConfigurationView()`):

- Nature: lecture pure + projection DTO (pas d’écriture, pas d’accès runtime agent)
- Couplage réel:
- `AgentDefinitionService` pour le count des définitions
- `AdministrableConfigurationGateway` pour la liste des providers OpenAI-like
- Logique métier: faible, essentiellement sécurité de rendu
- `apiKeyConfigured = !isBlank(provider.apiKey())` (ne jamais exposer la clé brute)
- Logique de projection:
- mapping `OpenAiLikeProperties -> TechnicalAdminView.LlmProviderItem`
- assemblage de `TechnicalAdminView.ConfigItem`
- Dépendance runtime: aucune
- Dépendance data: uniquement configuration admin persistée

Bloc rétention (`listRetentionPolicies()`):

- Nature: lecture pure + projection DTO (pas d’écriture)
- Couplage réel:
- `PersistableObjectFamily.values()` pour l’itération des familles
- `PersistenceRetentionPolicyResolver` pour résoudre TTL/disposition
- Logique métier: faible à moyenne (car la logique de fallback est dans le resolver, pas dans la façade)
- Logique de projection:
- mapping `RetentionPolicy -> TechnicalAdminView.RetentionItem` (`family`, `ttl`, `disposition`)
- Remarque importante:
- la vue liste les politiques par famille uniquement (pas les overrides par domaine), conforme au comportement actuel
- Dépendance runtime: aucune
- Dépendance data: pure configuration de rétention (`PersistenceRetentionProperties` via resolver)

## 3. Points sensibles et risques d’extraction

- Risque de régression fonctionnelle sur `getOverview()`:
- `getOverview()` agrège `getConfigurationView()` et `listRetentionPolicies()`; le contrat (types et contenu) doit rester strictement identique
- Risque sécurité côté configuration:
- ne pas perdre la règle `apiKeyConfigured` booléenne; ne jamais exposer `apiKey`
- Risque de duplication de mapping:
- éviter de garder `toRetentionItem`/mapping provider à la fois dans façade et nouveaux services
- Risque de couplage accru:
- ne pas injecter runtime/policy/tools dans les nouveaux services
- les nouveaux services doivent rester des query services de lecture config/rétention uniquement
- Risque de logique transverse cachée déplacée:
- faible sur ces 2 blocs; la logique transverse principale reste dans `listAgents()`/runtime/policy/tools (hors périmètre)
- Comportements strictement à conserver:
- endpoints et contrôleurs inchangés
- DTO `TechnicalAdminView` inchangés
- ordre/présence des familles issues de `PersistableObjectFamily.values()`
- calcul des compteurs `ConfigItem` identique
- `getOverview()` inchangé hors délégation interne

## 4. Plan d’extraction recommandé

Ordre recommandé:

1. Extraire d’abord la rétention (`RetentionQueryService`), puis la configuration (`AdminConfigQueryService`).

Justification:

- Le bloc rétention est le plus isolé (1 dépendance principale + 1 mapping), donc idéal pour valider le pattern de délégation sans toucher au reste.
- Le bloc configuration a une contrainte de sécurité de projection (api key) et deux sources de données; il vient ensuite.

Plan progressif proposé:

1. Créer `RetentionQueryService` dans `service.admin`.
2. Déplacer dans ce service:
- `listRetentionPolicies()` (logique d’itération des familles)
- mapping `RetentionPolicy -> TechnicalAdminView.RetentionItem`
3. Modifier `TechnicalAdminFacade` pour déléguer `listRetentionPolicies()` vers `RetentionQueryService`.
4. Vérifier que `getOverview()` reste inchangé et continue d’appeler la méthode façade `listRetentionPolicies()`.
5. Créer `AdminConfigQueryService` dans `service.admin`.
6. Déplacer dans ce service:
- `getConfigurationView()`
- mapping `OpenAiLikeProperties -> TechnicalAdminView.LlmProviderItem`
- helper local `isBlank` (ou équivalent local au service)
7. Modifier `TechnicalAdminFacade` pour déléguer `getConfigurationView()` vers `AdminConfigQueryService`.
8. Conserver temporairement dans la façade uniquement les méthodes publiques délégantes (point d’entrée stable).
9. Nettoyer les imports/dépendances façade retirées (`AdministrableConfigurationGateway`, `PersistenceRetentionPolicyResolver`, `OpenAiLikeProperties`, `RetentionPolicy`, `PersistableObjectFamily`, `AgentDefinition` si plus utilisé localement).

Ce qui doit rester temporairement dans la façade:

- signature publique des méthodes `getConfigurationView()` et `listRetentionPolicies()`
- orchestration globale `getOverview()`
- bloc agents/runtime/policy/tools inchangé

Helpers locaux:

- `toRetentionItem` doit quitter la façade (appartient au bloc rétention)
- `isBlank` utilisé uniquement par config doit quitter la façade
- `normalize` (tools/policy) doit rester dans la façade

## 5. Impact sur les tests

Tests à adapter:

- `TechnicalAdminFacadeTest`:
- adapter la construction de façade aux nouvelles dépendances (`AdminConfigQueryService`, `RetentionQueryService`)
- transformer le test configuration en test de délégation façade, puis couvrir la logique détaillée dans un test dédié du nouveau service
- ajuster les tests `getOverview*` pour mocker les nouveaux query services plutôt que gateway/resolver

Tests à conserver (peu ou pas de changement):

- `TechnicalAdminControllerTest` (contrat façade inchangé)
- `TechnicalAdminPageControllerTest` (contrat façade inchangé)
- `AdminDashboardServiceTest` (utilise `getOverview()`, inchangé en surface)

Tests à ajouter (recommandé pour sécuriser l’extraction):

- `AdminConfigQueryServiceTest`:
- vérifie count définitions + count providers
- vérifie projection provider
- vérifie que seule l’info booléenne `apiKeyConfigured` est exposée
- `RetentionQueryServiceTest`:
- vérifie résolution par famille + mapping DTO (`family`, `ttl`, `disposition`)
- vérifie l’itération sur toutes les familles `PersistableObjectFamily.values()`
- (optionnel) dans `TechnicalAdminFacadeTest`, ajouter un test explicite de délégation `listRetentionPolicies()` pour homogénéité avec les services extraits en phase 2

## 6. Recommandation finale

La phase 3 est bien candidate à une extraction simple et progressive, sans refonte large.

Recommandation opérationnelle:

- extraire `RetentionQueryService` puis `AdminConfigQueryService`
- garder `TechnicalAdminFacade` comme façade publique délégante
- ne pas toucher aux contrôleurs ni aux DTO `TechnicalAdminView`
- conserver `getOverview()` tel quel (agrégation identique), en ne changeant que ses sources internes via délégation
- sécuriser par tests dédiés de services extraits + tests de délégation façade

Cette approche minimise le risque de régression, réduit le couplage de la façade, et reste parfaitement alignée avec la réduction progressive déjà engagée en phase 2.
