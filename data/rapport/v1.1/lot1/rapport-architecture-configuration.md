# Rapport d'analyse architecture configuration (Phase sans implémentation)

Date: 2026-04-09  
Projet: Toolkit-Bridge  
Périmètre: audit du dépôt actuel pour préparer la stratégie "DB source de vérité métier".

## 0) Méthode et limites
- Analyse faite uniquement sur le code présent dans le dépôt.
- Aucune implémentation n'a été réalisée.
- Le rapport se concentre sur: YAML, fallback YAML->DB, stockage DB, mémoire, bootstrap, consommation runtime.

## 1) Cartographie précise de l'existant

### 1.1 Chargement de configuration YAML (direct Spring Boot)

Classes/propriétés métiers/techniques actuellement bindées depuis `application.yml`:

- Agents:
  - `src/main/java/fr/ses10doigts/toolkitbridge/config/agent/AgentsProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/config/agent/AgentConfiguration.java`
- LLM OpenAI-like:
  - `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeProvidersProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfiguration.java`
- Mémoire:
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/config/ConversationMemoryProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/context/config/ContextAssemblerProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/config/GlobalContextProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/config/MemoryRetrievalProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/config/MemoryIntegrationProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/config/MemoryScoringProperties.java`
- Telegram supervision (métier/opérationnel):
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramSupervisionProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramSupervisionMessagePublisher.java` (avec `@Value("${telegram.enabled:true}")`)
- Workspace:
  - `src/main/java/fr/ses10doigts/toolkitbridge/config/workspace/WorkspaceProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/workspace/WorkspaceLayout.java`
- Technique/système:
  - `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/config/AdminSecurityProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/config/AdminTechnicalProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/config/AgentTraceProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/artifact/config/ArtifactStorageProperties.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionProperties.java`

Fichier YAML observé:
- `src/main/resources/application.yml`
- `src/main/resources/application-template.yml`

### 1.2 Fallback YAML -> DB existant

Fallback explicite présent uniquement pour:
- Agent definitions
- OpenAI-like providers

Implémentation:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationGateway.java`
  - `loadAgentDefinitions()` => lit DB, sinon `loadAgentDefinitionsSeed()` (YAML)
  - `loadOpenAiLikeProviders()` => lit DB, sinon `loadOpenAiLikeProvidersSeed()` (YAML)

Test qui confirme ce fallback:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationGatewayTest.java`
  - `shouldFallbackToYamlSeedWhenDatabaseConfigIsMissing`

### 1.3 Stockage DB de configuration métier

Stockage central actuel (générique JSON):
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationRepository.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationStoreService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigKey.java`

Clés actuellement supportées:
- `agent.definitions`
- `llm.openai_like.providers`

Important:
- Il n'existe pas aujourd'hui de clé DB pour la configuration mémoire.
- Il n'existe pas aujourd'hui de stockage DB administrable pour la configuration Telegram bots (au sens catalogue de bots runtime).

### 1.4 Configuration mémoire (où elle est définie et où elle est consommée)

Définition YAML-only actuelle:
- `ConversationMemoryProperties`
- `ContextAssemblerProperties`
- `GlobalContextProperties`
- `MemoryRetrievalProperties`
- `MemoryIntegrationProperties`
- `MemoryScoringProperties`

Consommation runtime:
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/service/DefaultConversationMemoryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/service/ConversationMemoryCleanupScheduler.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssembler.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/DefaultMemoryRetriever.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/facade/DefaultMemoryRetrievalFacade.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/facade/service/DefaultMemoryFacade.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/DefaultMemoryScoringService.java`

Nuance critique:
- Les données mémoire (facts/rules/episodes) sont bien en DB via JPA (`MemoryEntry`, `RuleEntry`, `EpisodeEvent`).
- Mais la configuration de comportement mémoire reste majoritairement YAML.
- La mémoire conversationnelle est stockée en mémoire JVM (`InMemoryConversationMemoryStore`), pas en DB.

### 1.5 Bootstrap / initialisation existants

Bootstraps détectés:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java`
  - `ApplicationRunner @Order(0)`
  - seed DB depuis YAML si clé absente
- `src/main/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializer.java`
  - synchronise comptes agents avec définitions déclarées
  - crée les comptes manquants
  - supprime les comptes non déclarés (`deleteByAgentIdentNotIn`)
- `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenInitializer.java`
  - initialise token admin technique

### 1.6 Runtime qui consomme ces configurations

Agents:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/definition/AgentDefinitionService.java`
  - lit via gateway (DB ou fallback seed)
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeService.java`
  - résout agent par `telegramBotId`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeFactory.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/ChatAgentOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestrator.java`

LLM:
- `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfiguration.java`
  - construit `LlmProviderRegistry` une fois au démarrage depuis gateway
- `src/main/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmService.java`
  - consomme ce registry

Admin métiers:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/functional/AgentDefinitionAdminFacade.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/functional/LlmAdminFacade.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/functional/TelegramBotAdminFacade.java`

Telegram supervision:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/*`

## 2) Zones incompatibles avec l'architecture cible

### 2.1 Fallback runtime YAML -> DB encore actif
- `AdministrableConfigurationGateway` applique `orElseGet(seed YAML)` en lecture runtime.
- Incompatible avec la cible "pas de fallback implicite runtime".

### 2.2 Bootstrap pas "champ par champ"
- `bootstrapSeedsIfMissing()` raisonne au niveau "clé globale" (liste entière) et non au niveau champ.
- Incompatible avec exigence d'injection fine champ par champ.

### 2.3 Couplage stockage / application runtime incohérent
- Agents: lus via gateway à chaque accès métier (effet runtime potentiellement immédiat après modification DB).
- LLM providers: snapshot au startup (registry singleton), donc modifications DB non prises en compte sans redémarrage.
- Ce comportement mixte crée une incohérence forte vis-à-vis de la règle "stockage d'abord, application runtime plus tard".

### 2.4 Telegram bots administrables non réellement en DB
- `TelegramBotAdminFacade` lit `TelegramSupervisionProperties` (YAML), et `createTelegramBot()` est non implémenté.
- L'objet "BotTelegram" n'est pas géré comme configuration métier DB unique.

### 2.5 Mémoire: configuration non administrable
- Toute la configuration mémoire est encore sur `@ConfigurationProperties` YAML.
- Incompatible avec "totalité du paramétrage mémoire administrable en DB".

### 2.6 Contraintes YAML de validation encore structurantes
- `AgentsProperties` impose `@NotEmpty` sur les définitions YAML.
- Cette contrainte peut bloquer/parasiter la transition vers un mode où YAML ne porte plus la config métier.

## 3) Risques de refactor / dette

1. Risque de rupture au démarrage si on supprime brutalement fallback sans bootstrap explicite robuste.
2. Risque d'incohérence runtime pendant transition (agents "live" vs providers LLM figés au startup).
3. Risque sécurité: secrets métier présents en clair dans `application.yml` (tokens Telegram, clés API LLM).
4. Risque d'effets de bord sur `AgentAccountInitializer` (création/suppression de comptes) si la source de vérité devient temporairement incomplète.
5. Risque de dette fonctionnelle si l'admin continue à exposer des écrans non branchés DB (Telegram).
6. Risque testabilité: peu de contrats explicites "config DB complète requise"; beaucoup de services supposent config disponible.
7. Risque de confusion conceptuelle entre:
   - données mémoire (facts/rules/episodes)
   - configuration mémoire (limites, toggles, policies)

## 4) Proposition de découpage Étape 1 (8 étapes max)

1. Figer le contrat de frontières de config.
- Définir une matrice officielle "System YAML" vs "Business DB" par clé.
- Introduire une convention de nommage stable des clés DB (anglais).

2. Étendre le modèle DB de configuration administrable.
- Ajouter les nouvelles clés de config métier (Telegram bots, memory settings, autres objets métier).
- Garder l'API store/gateway, mais préparer lecture stricte DB.

3. Introduire un bootstrap explicite champ par champ.
- Créer un composant bootstrap dédié qui:
  - vérifie chaque champ attendu,
  - injecte uniquement les manquants,
  - n'écrase jamais un champ existant.
- Ajouter logs explicites "missing -> seeded" / "existing -> kept".

4. Désactiver le fallback implicite runtime.
- Remplacer les `orElseGet(seed)` runtime par lecture DB stricte.
- Le seed reste cantonné au bootstrap de démarrage.

5. Découpler stockage admin et application runtime.
- Introduire des "runtime snapshots" construits au démarrage uniquement.
- L'admin écrit en DB sans modifier ces snapshots en live (pas de reload implicite).

6. Migrer la configuration mémoire vers DB.
- Remplacer l'injection directe de `@ConfigurationProperties` mémoire dans les services métier par des objets "resolved from DB snapshot".
- Garder éventuellement des valeurs techniques minimales côté YAML pour bootstrap uniquement.

7. Aligner Telegram/Bot sur la même stratégie DB.
- Faire de la config bot/supervision un objet administrable DB.
- Limiter YAML Telegram au socle technique non administrable (si nécessaire).

8. Sécuriser par tests d'architecture.
- Tests bootstrap champ par champ.
- Tests "no runtime fallback".
- Tests "admin write != runtime apply" jusqu'au redémarrage.

## 5) Ce qui doit rester YAML vs passer DB vs ambigu

### 5.1 À garder côté système/YAML
- `spring.*` (datasource, JPA, SQL init)
- `server.*`
- `logging.*`
- Sécurité technique globale (ex: `toolkit.admin.security.master-token-file`)
- Chemins infra/workspace techniques (`app.workspace.*`) si non administrés fonctionnellement
- Observabilité/retention purement technique selon gouvernance ops

### 5.2 À passer côté DB (source de vérité métier/agentique)
- Agents (définitions, prompts, policies métier, outils activés)
- LLM providers métier (catalogue, baseUrl métier, defaultModel, credentials gérés proprement)
- BotTelegram (catalogue bots, associations agent-bot, paramètres fonctionnels)
- Paramétrage mémoire administrable (conversation/context/retrieval/integration/scoring/global context behavior)

### 5.3 À revoir car ambigus
- `toolkit.telegram.supervision.*`
  - partiellement opérationnel, partiellement métier.
- `toolkit.artifacts.*` et `toolkit.persistence.retention.*`
  - selon ton usage, peut être considéré comme paramétrage ops (YAML) ou gouvernance métier (DB).
- `toolkit.observability.agent-tracing.*`
  - généralement système, mais certains toggles peuvent être exposés admin.

## Synthèse exécutable pour la suite
- Le dépôt a déjà une base DB administrable utile (`administrable_configuration`) mais limitée à Agents + LLM et avec fallback runtime YAML->DB.
- La cible demandée nécessite de:
  - stopper le fallback runtime,
  - déplacer la config mémoire et bot Telegram en DB,
  - introduire un bootstrap explicite champ par champ,
  - clarifier le cycle "stockage DB" vs "application runtime" (startup/reload explicite futur).

Aucune implémentation n'a été faite dans cette phase.
