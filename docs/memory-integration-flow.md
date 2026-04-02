# Memory Integration Flow

## Overview
The chat orchestrator now uses a unified memory pipeline through `MemoryFacade`.

Flow:
1. Receive user message.
2. `memoryFacade.onUserMessage(...)` writes conversation and episodic entries, then applies semantic extraction and optional rule promotion.
3. Build `MemoryContextRequest`.
4. `memoryFacade.buildContext(...)` retrieves multi-source memories, enforces limits, assembles final context, and returns injected semantic memory ids.
5. Call LLM.
6. `memoryFacade.onAssistantMessage(...)` persists assistant output in conversation and episodic memory, with optional semantic/rule extraction.
7. Optional `memoryFacade.onToolExecution(...)` writes standardized tool episodes.
8. `memoryFacade.markContextMemoriesUsed(...)` increments usage only for semantic memories effectively injected.

## Memory Roles
- Rules: stable operating constraints from `RuleService`, plus explicit promotions from user/assistant/tool text.
- Semantic memories: durable facts/preferences/decisions extracted heuristically and retrieved by relevance.
- Episodic memories: structured events for conversation outcomes and tool execution history.
- Conversation slice: short recent history for continuity.

## Assembly Order
`DefaultContextAssembler` builds context in this order:
1. Rules
2. Known facts (semantic)
3. Recent episodes
4. Conversation
5. Current user input

It also returns `injectedSemanticMemoryIds` for precise `markUsed` feedback.

## Extraction Policy (V1)
`DefaultSemanticMemoryExtractor` keeps only durable statements with explicit markers (preferences, conventions, architecture choices, stable constraints) and ignores ephemeral requests.

## Rule Promotion Policy (V1)
`DefaultRulePromotionService` promotes only explicit formulations (e.g. always/never/toujours/never) and ignores vague preferences.

## Limits and Budget
`MemoryIntegrationProperties` controls:
- extraction toggles (`enableSemanticExtraction`, `enableRulePromotion`)
- episodic injection toggle (`enableEpisodicInjection`)
- semantic `markUsed` toggle (`markUsedEnabled`)
- max rules/memories/episodes
- max context character budget

## Extension Points
- Improve semantic extraction with stronger NLP or LLM classification.
- Add confidence score and source metadata for promoted rules.
- Add richer episodic scoring and semantic retrieval over episodes.
- Add strict token counting with provider tokenizer.
