# Implémentation — Phase 3 — Traces critiques

## 1. Résumé des modifications

La persistance durable des traces critiques a été ajoutée via un sink JPA dédié, sans refactor global du pipeline de traces.

Modifications clés:
- ajout d'une table JPA dédiée `critical_agent_trace`
- ajout d'un sink `CriticalTraceJpaSink` branché automatiquement dans le fan-out existant `AgentTraceSink`
- filtrage strict sur les types critiques validés: `ERROR`, `TASK_STARTED`, `RESPONSE`, `DELEGATION`, `TOOL_CALL`
- sanitation légère des `attributes` avant persistance JSON
- bascule de `TraceQueryService` vers lecture DB (plus de lecture mémoire pour la vue admin)

## 2. Entité et repository

### Entité
- `CriticalAgentTraceEntity`
- table: `critical_agent_trace`
- colonnes:
  - `id` (PK auto-increment)
  - `occurred_at` (NOT NULL)
  - `event_type` (enum string, NOT NULL)
  - `source` (NOT NULL)
  - `run_id` (nullable)
  - `agent_id` (nullable)
  - `message_id` (nullable)
  - `task_id` (nullable)
  - `attributes_json` (`TEXT`, NOT NULL)
  - `ingested_at` (NOT NULL)

### Index
- `idx_critical_trace_occurred_at`
- `idx_critical_trace_agent_occurred_at` (`agent_id,occurred_at`)
- `idx_critical_trace_type_occurred_at` (`event_type,occurred_at`)
- `idx_critical_trace_run_id`
- `idx_critical_trace_task_id`

### Repository
- `CriticalAgentTraceRepository`
- méthodes:
  - `findByOrderByOccurredAtDescIdDesc(Pageable)`
  - `findByAgentIdOrderByOccurredAtDescIdDesc(String, Pageable)`

## 3. Sink DB critique

### Nouveau sink
- `CriticalTraceJpaSink implements AgentTraceSink`
- comportement:
  - reçoit tous les événements via le fan-out existant
  - persiste uniquement les types critiques (via `CriticalAgentTraceMapper.isCriticalType`)
  - ignore les autres types
  - log un warning en cas d'échec de persistance

### Compatibilité architecture
- aucun changement de contrat de `DefaultAgentTraceService`
- `DefaultAgentTraceService` reste producteur unique + fan-out + isolation des erreurs sink
- `InMemoryAgentTraceSink` et `JsonLinesAgentTraceSink` restent en place

## 4. Gestion des attributes

Sanitation implémentée dans `CriticalAgentTraceMapper` avant sérialisation JSON:

- suppression des clés invalides / valeurs nulles
- troncature générique des textes longs
- limitation de profondeur/volume sur structures imbriquées (maps/listes)
- règles ciblées:
  - `TOOL_CALL.message` tronqué fortement
  - `ERROR.reason` tronqué
- conservation du format JSON brut (`attributes_json`) pour rester souple en phase transitoire

## 5. Lecture admin

`TraceQueryService` lit désormais directement la DB critique:
- limite via `AdminTechnicalProperties.sanitizeLimit`
- filtre optionnel par agentId
- tri fourni par repository: `occurredAt DESC, id DESC`
- mapping DB -> `TechnicalAdminView.TraceItem` via `CriticalAgentTraceMapper`

La vue admin traces n'utilise plus la source mémoire.

## 6. Source de vérité retenue

Source de vérité admin après implémentation:
- DB `critical_agent_trace`

Sources conservées mais secondaires:
- mémoire: buffer runtime
- JSONL: forensic brut

Il n'y a pas de fallback mémoire durable dans `TraceQueryService`.

## 7. Tests ajoutés

### Unitaires
- `CriticalAgentTraceMapperTest`
  - sanitation `TOOL_CALL.message`
  - sanitation `ERROR.reason`
  - corrélation partielle/null
  - prédicat type critique/non critique
- `CriticalTraceJpaSinkTest`
  - type retenu -> persistance
  - type non retenu -> ignoré
- `TraceQueryServiceTest` (adapté)
  - source DB globale
  - filtre agent DB

### Intégration
- `CriticalTracePersistenceIT`
  - pipeline complet `AgentTraceService` -> sink DB
  - exclusion type non retenu
  - `TraceQueryService` lit DB avec tri/filtre/limite

### Restart
- `CriticalTracePersistenceRestartIT`
  - traces critiques écrites avant arrêt
  - restitution après redémarrage via `TraceQueryService` (sans dépendance mémoire)

### Commande exécutée
- `./mvnw -q "-Dtest=TraceQueryServiceTest,CriticalAgentTraceMapperTest,CriticalTraceJpaSinkTest,CriticalTracePersistenceIT,CriticalTracePersistenceRestartIT" test`

## 8. Points d’attention

- `runId`/`traceId` est conservé tel quel (`runId`), sans rigidification du contrat.
- la sanitation est volontairement légère: protège les cas volumineux évidents sans imposer de schéma fort.
- le stockage JSON brut reste un compromis transitoire assumé.

## 9. Limites connues

- pas de projection SQL fine des `attributes`
- pas de recherche avancée ni pagination complexe
- pas de politique de rétention dédiée pour `critical_agent_trace` dans cette phase
- hétérogénéité possible des `attributes` selon producteurs (acceptable en phase actuelle)
