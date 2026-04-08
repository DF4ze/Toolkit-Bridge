# Configuration Inventory and Migration Plan (Phase 18.1.A)

## Scope and intent
This document maps the current configuration landscape in the real repository, classifies each parameter family, and prepares the targeted refactoring for phase 18.2.A.

## Analysis perimeter (real files)
- `src/main/resources/application.yml`
- `src/main/java/fr/ses10doigts/toolkitbridge/config/agent/AgentsProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeProvidersProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/config/workspace/WorkspaceProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/definition/AgentDefinitionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializer.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/workspace/WorkspaceInitializer.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfiguration.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramSupervisionMessagePublisher.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramSupervisionProperties.java`
- Memory, trace, retention, artifact configuration classes under:
  - `memory/**/config/*Properties.java`
  - `service/agent/trace/config/AgentTraceProperties.java`
  - `persistence/retention/PersistenceRetentionProperties.java`
  - `service/agent/artifact/config/ArtifactStorageProperties.java`

## Classification matrix

| Parameter family | Current location | Current access pattern | Category | Target source of truth | Refactor needed | Bootstrap need | Notes |
|---|---|---|---|---|---|---|---|
| `toolkit.agents.definitions` | `application.yml` + `AgentsProperties` | Direct YAML-bound usage in `AgentDefinitionService`, `AgentAccountInitializer`, `WorkspaceInitializer` | Administrable | DB | Yes (critical) | Yes | Structuring runtime config, currently coupled to startup YAML |
| `toolkit.llm.openai-like.providers` | `application.yml` + `OpenAiLikeProvidersProperties` | Direct YAML-bound usage in `OpenAiLikeConfiguration` | Administrable | DB | Yes (critical) | Yes | Includes provider routing data and credentials |
| `toolkit.telegram.supervision.*` | `application.yml` + `TelegramSupervisionProperties` + one `@Value` (`telegram.enabled`) | Mixed config binding and direct scalar property access | Hybrid (ops + admin toggles) | YAML (now), possible DB later | Partial | Optional | Keep technical wiring in YAML now, avoid fragmented access style |
| `telegram.*` bot runtime enablement and tokens | `application.yml` | External library integration + direct property binding | Technical/Sensitive | YAML/secret manager | No for now | No | Keep out of early admin migration scope |
| `spring.*`, `server.*`, logging levels | `application.yml` | Spring native | Technical | YAML | No | No | Infrastructure bootstrap and runtime envelope |
| `app.workspace.*` | `application.yml` + `WorkspaceProperties` | Centralized property object consumed by workspace services | Technical | YAML | No | No | Path boundaries and filesystem wiring |
| `toolkit.memory.conversation.*` | `ConversationMemoryProperties` | Bound + consumed in memory services and conditions | Hybrid (mostly technical runtime tuning) | YAML (for now) | No | No | Could become admin later if hot tuning is needed |
| `toolkit.memory.context.*` and `toolkit.memory.context.global.*` | `ContextAssemblerProperties`, `GlobalContextProperties` | Bound + consumed in context assembly/global context provider | Hybrid | YAML (for now) | No | No | Includes file list for global context, currently acceptable in YAML |
| `toolkit.memory.retrieval.*` | `MemoryRetrievalProperties` | Bound + used in retrieval services | Hybrid | YAML (for now) | No | No | Query and limit tuning |
| `toolkit.memory.integration.*` | `MemoryIntegrationProperties` | Bound + used in facade | Hybrid | YAML (for now) | No | No | Runtime behavior flags |
| `toolkit.memory.scoring.*` | `MemoryScoringProperties` | Bound + used in scoring services | Hybrid | YAML (for now) | No | No | Algorithmic tuning coefficients |
| `toolkit.observability.agent-tracing.*` | `AgentTraceProperties` | Bound + used in tracing sinks/service | Technical | YAML | No | No | Observability pipeline config |
| `toolkit.persistence.retention.*` | `PersistenceRetentionProperties` | Bound + used by central retention resolver | Hybrid (policy-like, but infra-scoped) | YAML (for now) | No | No | Already centralized in dedicated resolver |
| `toolkit.artifacts.*` | `ArtifactStorageProperties` | Bound + used in storage/retention for artifacts | Hybrid | YAML (for now) | No | No | Already centralized in property object |
| External process catalog templates (`external-processes/**`) | `src/main/resources/external-processes` JSON files | File-based content loading | Editable content | Dedicated files | No | No | Already in file-based content source |

## Sensitive architecture points
- Agent and provider definitions are runtime-critical and currently hard-wired to YAML startup binding.
- Multiple components independently consume `toolkit.agents.definitions`, creating coupling to property source.
- Direct `@Value("${telegram.enabled:true}")` introduces implicit scalar access outside typed property contracts.
- Current model lacks explicit separation between bootstrap seed values and live administrable values.

## Refactor/debt risks identified
- **Source-of-truth ambiguity**: YAML currently acts as both bootstrap and effective runtime source for administrable families.
- **Update rigidity**: administrable changes require YAML edits/redeploy instead of persistent mutable storage.
- **Config access scattering**: repeated direct dependency on YAML-bound classes increases migration blast radius.
- **Security/operations risk**: provider credentials and administrable values are mixed in static YAML state.

## Zones to refactor in phase 18.2.A
1. `AgentDefinitionService` (replace direct YAML-bound source with centralized administrable config reader).
2. `AgentAccountInitializer` (same source centralization for account sync).
3. `WorkspaceInitializer` (same source centralization for agent workspace creation).
4. `OpenAiLikeConfiguration` (provider list resolution through centralized administrable config reader).
5. Introduce explicit administrable config persistence boundary (entity + repository + service).

## Out-of-scope for 18.1.A and intentionally deferred
- Full migration of all property families.
- Admin UI/REST CRUD for config management.
- YAML read/write synchronization mechanics.
- Advanced bidirectional conflict resolution YAML <-> DB.

## 18.2.A implementation plan (8 steps)
1. Create administrable config persistence model (`administrable_configuration` table).
2. Define explicit administrable keys for first migration batch.
3. Build centralized read/write service with typed JSON serialization.
4. Build gateway that resolves runtime values from DB first, YAML seed fallback second.
5. Refactor critical agent-definition consumers to this gateway.
6. Refactor LLM provider registry configuration to this gateway.
7. Keep all technical config families on YAML unchanged.
8. Add focused tests validating precedence and fallback behavior.
