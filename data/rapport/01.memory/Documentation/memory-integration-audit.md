# Memory Integration Audit

## Portee
Audit des modules deja presents autour de la memoire et de l�orchestration chat. Objectif: inventorier l�existant pour eviter les doublons.

## Inventaire des classes et interfaces existantes

### Conversation memory
- Configuration
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\config\ConversationMemoryConfiguration.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\config\ConversationMemoryProperties.java`
- Ports
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\port\ConversationMemoryService.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\port\ConversationMemoryStore.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\port\ConversationSummarizer.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\port\ConversationContextRenderer.java`
- Services
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\service\DefaultConversationMemoryService.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\service\SimpleConversationSummarizer.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\service\DefaultConversationContextRenderer.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\service\ConversationMemoryCleanupScheduler.java`
- Store
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\store\InMemoryConversationMemoryStore.java`
- Modeles
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMemoryKey.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMemoryState.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMessage.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationSummary.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationRole.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationEntryType.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationCompressionReason.java`

### Semantic memory
- Configuration
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\config\SemanticMemoryConfiguration.java`
- Service
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\service\SemanticMemoryService.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\service\DefaultSemanticMemoryService.java`
- Repository
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\repository\MemoryEntryRepository.java`
- Modeles
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryEntry.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryType.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryStatus.java`

### Rule memory
- Configuration
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\config\RuleMemoryConfiguration.java`
- Service
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\service\RuleService.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\service\DefaultRuleService.java`
- Repository
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\repository\RuleEntryRepository.java`
- Modeles
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleEntry.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RulePriority.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleStatus.java`

### Episodic memory
- Configuration
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\config\EpisodicMemoryConfiguration.java`
- Service
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\service\EpisodicMemoryService.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\service\DefaultEpisodicMemoryService.java`
- Repository
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\repository\EpisodeEventRepository.java`
- Modeles
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeEvent.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeEventType.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeStatus.java`

### Context assembler
- Configuration
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\config\ContextAssemblerConfiguration.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\config\ContextAssemblerProperties.java`
- Port
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\port\ContextAssembler.java`
- Service
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\service\DefaultContextAssembler.java`
- Modele
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\model\ContextRequest.java`

### Memory retrieval
- Port
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\retrieval\port\MemoryRetriever.java`
- Service
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\retrieval\service\DefaultMemoryRetriever.java`
- Modele
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\retrieval\model\MemoryQuery.java`

### Orchestrator chat
- Orchestrator
- `src\main\java\fr\ses10doigts\toolkitbridge\service\agent\orchestrator\impl\ChatAgentOrchestrator.java`
- Interfaces de registry
- `src\main\java\fr\ses10doigts\toolkitbridge\service\agent\orchestrator\AgentOrchestrator.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\service\agent\orchestrator\AgentOrchestratorRegistry.java`

## Ce qui est deja appele par l'orchestrator
Orchestrator principal: `ChatAgentOrchestrator`.

Appels directs:
- `ConversationMemoryService.appendMessage(...)` pour stocker les messages user et assistant.
- `ContextAssembler.buildContext(...)` pour construire le prompt enrichi.
- `EpisodicMemoryService.record(...)` pour journaliser les episodes.

Appels indirects via `DefaultContextAssembler`:
- `RuleService.getApplicableRules(...)` pour injecter les regles.
- `MemoryRetriever.retrieve(...)` pour recuperer les memories semantiques.
- `ConversationMemoryService.buildContext(...)` pour reconstituer l�historique.

## Ce qui existe mais n'est jamais branche dans le flux principal
Elements presents en `src\main\java` mais sans appel depuis l'orchestration chat:
- `SemanticMemoryService` (create/update/archive/markUsed/search). Aucune reference en production autre que sa declaration.
- `DefaultSemanticMemoryService` est uniquement invoque par les tests et non par l�orchestrator.
- Les methodes de lecture d�`EpisodicMemoryService` (`findRecent`, `findRecentByScope`) ne sont pas utilisees dans le flux chat.

## DTO et modeles reutilisables deja presents
### DTOs generaux cote agent/orchestrator
- `src\main\java\fr\ses10doigts\toolkitbridge\model\dto\agent\comm\AgentRequest.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\model\dto\agent\comm\AgentResponse.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\model\dto\agent\definition\AgentDefinition.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\model\dto\agent\definition\AgentOrchestratorType.java`

### Modeles de memoire utilisables comme DTOs techniques
- Conversation
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMemoryKey.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMessage.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationSummary.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\conversation\model\ConversationMemoryState.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\context\model\ContextRequest.java`
- Semantic
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryEntry.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryType.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\semantic\model\MemoryStatus.java`
- Rule
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleEntry.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RulePriority.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\rule\model\RuleStatus.java`
- Episodic
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeEvent.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeEventType.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeScope.java`
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\episodic\model\EpisodeStatus.java`
- Retrieval
- `src\main\java\fr\ses10doigts\toolkitbridge\memory\retrieval\model\MemoryQuery.java`

## Notes
- Aucun ajout de classe. Ce document est un audit statique de l�existant.
