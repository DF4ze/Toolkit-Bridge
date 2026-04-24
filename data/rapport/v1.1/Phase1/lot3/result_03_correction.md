# Corrections — Phase 3 — Traces critiques

## 1. Corrections appliquées

Corrections structurelles appliquées sans changement de comportement fonctionnel :
- découplage du mapper persistence vis-à-vis du DTO admin,
- externalisation des seuils de sanitation en configuration,
- simplification de la gestion d’erreur du sink DB,
- ajout des tests complémentaires demandés.

## 2. Découplage mapper

Changement appliqué :
- `CriticalAgentTraceMapper` ne dépend plus de `TechnicalAdminView`.
- la méthode `toTraceItem(...)` a été retirée du mapper persistence.
- le mapping vers `TechnicalAdminView.TraceItem` est désormais réalisé dans `TraceQueryService`.

Impact architecture :
- suppression du couplage persistence -> couche admin.
- meilleure séparation des couches sans modifier les contrats publics exposés.

Fichiers :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/CriticalAgentTraceMapper.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`

## 3. Configuration sanitation

Changement appliqué :
- ajout d’une configuration dédiée : `CriticalTraceSanitizationProperties`.
- seuils désormais configurables (avec valeurs par défaut inchangées) :
  - `maxGenericText`
  - `maxErrorReasonText`
  - `maxToolMessageText`
  - `maxCollectionItems`
  - `maxMapItems`
  - `maxDepth`
- injection de ces propriétés dans `CriticalAgentTraceMapper`.

Wiring :
- activation via `AgentTraceConfiguration`.

Fichiers :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/config/CriticalTraceSanitizationProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/config/AgentTraceConfiguration.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/CriticalAgentTraceMapper.java`

## 4. Gestion erreurs

Changement appliqué :
- retrait du `try/catch` dans `CriticalTraceJpaSink.publish(...)`.
- la gestion des erreurs sink est laissée au mécanisme déjà en place dans `DefaultAgentTraceService`.

Résultat :
- responsabilité d’erreur unifiée,
- suppression de la double gestion/log potentiellement redondante,
- contrat et comportement global inchangés.

Fichier :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/CriticalTraceJpaSink.java`

## 5. Tests

Tests complémentaires ajoutés/adaptés :

1. JSON invalide -> fallback sûr
- ajout dans `CriticalAgentTraceMapperTest` :
  - `parseAttributesJsonShouldFallbackSafelyOnInvalidJson()`

2. Tri déterministe (`occurredAt DESC, id DESC`)
- ajout dans `CriticalTracePersistenceIT` :
  - `repositoryShouldSortDeterministicallyByOccurredAtThenIdDesc()`

Adaptations suite à injection des propriétés de sanitation :
- `CriticalAgentTraceMapperTest`
- `CriticalTraceJpaSinkTest`
- `TraceQueryServiceTest`

Commande exécutée :
- `./mvnw -q "-Dtest=TraceQueryServiceTest,CriticalAgentTraceMapperTest,CriticalTraceJpaSinkTest,CriticalTracePersistenceIT,CriticalTracePersistenceRestartIT" test`

Résultat : OK

## 6. Résultat final

Les corrections demandées ont été appliquées dans le périmètre strict, sans refactor global ni changement de contrat public.

Bilan :
- structure plus maintenable,
- séparation des couches améliorée,
- sanitation configurable sans complexification,
- gestion d’erreurs sink clarifiée,
- couverture de tests renforcée sur les deux points ciblés.

## 7. Rapport d'execution — Correction complémentaire agentId

Corrections appliquées :
- normalisation `agentId` à l’écriture dans `CriticalAgentTraceMapper` (`trim + lower-case`, null conservé si absent)
- normalisation identique à la lecture dans `TraceQueryService` (`trim + lower-case`)
- ajout du commentaire de documentation demandé dans les deux points de normalisation :
  - `agentId is a technical identifier, normalized to lower-case.`
  - `It is not intended for UI display.`

Tests ajustés :
- `CriticalAgentTraceMapperTest` (correlation partielle avec `agentId` mixte/espaces)
- `TraceQueryServiceTest` (filtre `agentId` mixte/espaces)

Validation exécutée :
- `./mvnw -q "-Dtest=TraceQueryServiceTest,CriticalAgentTraceMapperTest,CriticalTracePersistenceIT" test`
- Résultat : OK
