# Analyse — Phase 3 — Traces critiques

## 1. Résumé exécutif

Le flux de traces actuel est déjà structuré autour d'un producteur unique (`DefaultAgentTraceService`) et de sinks multiples (`AgentTraceSink`).

Constat principal:
- la vue admin lit uniquement la mémoire via `AgentTraceQueryService` implémenté par `InMemoryAgentTraceSink`
- les traces JSONL sont durables, mais non requêtées par l'admin
- après redémarrage, la vue admin perd l'historique des traces

Conclusion de cadrage Phase 3:
- l'option la plus propre est d'introduire un sink DB dédié aux traces critiques (filtré par type)
- conserver le flux existant (in-memory, JSONL, Telegram) sans le casser
- faire évoluer la lecture admin vers la DB comme source de vérité (avec coexistence transitoire explicite)

## 2. Modèle existant

### 2.1 `AgentTraceEvent`
- Rôle: objet canonique d'un événement de trace.
- Structure: `occurredAt`, `type`, `source`, `correlation`, `attributes`.
- Nature: `record` immutable, implémente `DurableObject` (famille `TRACE`).
- Cycle de vie: instancié dans `DefaultAgentTraceService.trace(...)`, puis diffusé vers tous les sinks.
- Point clé: aucun champ "message/summary" dédié; toute sémantique métier est dans `attributes`.

### 2.2 `AgentTraceEventType`
- Enum des types de trace: `TASK_STARTED`, `CONTEXT_ASSEMBLED`, `TOOL_CALL`, `DELEGATION`, `DEBATE`, `IMPROVEMENT_OBSERVATION`, `IMPROVEMENT_PROPOSAL`, `RESPONSE`, `ERROR`.
- Les types ciblés de la phase (`ERROR`, `TASK_STARTED`, `RESPONSE`, `DELEGATION`, `TOOL_CALL`) sont bien présents et déjà produits.

### 2.3 `DefaultAgentTraceService`
- Rôle: point d'entrée unique de production des traces.
- Responsabilités:
  - garde-fou global (`toolkit.observability.agent-tracing.enabled` + `type != null`)
  - création `AgentTraceEvent` (timestamp `Instant.now()`)
  - normalisation `source` (`unknown` si vide)
  - assainissement `attributes` (clé non vide, valeur non nulle)
  - fan-out vers tous les `AgentTraceSink`
  - isolation des pannes sink (try/catch par sink, warning log)
- Interaction critique: un sink défaillant n'interrompt pas les autres.

### 2.4 `AgentTraceSink`
- Contrat minimal: `publish(AgentTraceEvent event)`.
- Design actuel favorable à Phase 3: ajout d'un sink DB possible sans toucher les producteurs.

### 2.5 `InMemoryAgentTraceSink`
- Rôle: buffer runtime borné + implémentation de lecture (`AgentTraceQueryService`).
- Structure: `Deque<AgentTraceEvent>` synchronisée.
- Politique:
  - activable via `toolkit.observability.agent-tracing.memory.enabled`
  - taille max via `memory.maxEvents` (défaut 1000)
  - éviction FIFO au dépassement.
- Lecture:
  - `recentEvents()` retourne copie de la deque
  - `recentEventsForAgent(agentId)` filtre par `event.correlation().agentId()`.
- Limite majeure: volatil, perdu au redémarrage.

### 2.6 `JsonLinesAgentTraceSink`
- Rôle: persistance fichier append-only JSONL.
- Politique:
  - activable via `toolkit.observability.agent-tracing.file.enabled`
  - dossier racine configurable (`file.rootPath`, défaut `workspace/observability/agent-traces`)
  - 1 fichier par agent (`<agentId>.jsonl`, `unknown-agent` si absent)
- Format: sérialisation brute complète de `AgentTraceEvent` par ligne.
- Limites admin:
  - pas d'API de requête
  - pas d'index
  - pas d'intégration `TraceQueryService`.

### 2.7 `TraceQueryService`
- Rôle: adaptation des traces pour vue admin (`TechnicalAdminView.TraceItem`).
- Dépendance actuelle: `AgentTraceQueryService` (donc in-memory uniquement).
- Fonctionnement:
  - source globale ou filtrée agent
  - tri descendant par `occurredAt`
  - `limit` borné via `AdminTechnicalProperties`.
- DTO mappé: `occurredAt`, `type`, `source`, `runId`, `agentId`, `messageId`, `taskId`, `attributes`.

### 2.8 DTO / vues admin concernés
- DTO: `TechnicalAdminView.TraceItem`.
- Contrôleurs: `TechnicalAdminController` (API), `TechnicalAdminPageController` (Thymeleaf).
- Template: `admin/technical/traces.html`, qui affiche les attributs en brut (`Map#toString`).
- Faiblesse actuelle: la vue admin dépend d'une source runtime volatile.

## 3. Flux complet des traces

1. Création des traces
- Orchestrateurs (`TaskAgentOrchestrator`, `ChatAgentOrchestrator`): `TASK_STARTED`, `RESPONSE`, `ERROR`.
- Bus inter-agent (`InMemoryAgentMessageBus`): `DELEGATION`, `ERROR`.
- LLM (`DefaultLlmService`): `TOOL_CALL`, `ERROR`.

2. Transit
- tous les producteurs appellent `AgentTraceService.trace(...)`
- `DefaultAgentTraceService` crée l'événement et diffuse vers tous les sinks enregistrés.

3. Sinks alimentés
- `InMemoryAgentTraceSink` (mémoire bornée)
- `JsonLinesAgentTraceSink` (fichiers JSONL)
- `TelegramAgentTraceSink` (notification supervision, non stockage)

4. Consultation admin actuelle
- `TraceQueryService` -> `AgentTraceQueryService`
- implémentation concrète disponible: `InMemoryAgentTraceSink`
- donc la page/API admin lit uniquement la mémoire.

5. Persistance vs perte
- Survit redémarrage: JSONL (fichier), pas exploité par admin.
- Perdu redémarrage: buffer mémoire (source actuelle de l'admin).

Différences explicites:
- Trace mémoire: rapide, filtrable, volatile, source admin actuelle.
- Trace JSONL: durable, brute, non indexée/non requêtable côté admin.
- Future trace critique persistée: durable, requêtable admin, filtrée par types critiques.

## 4. Analyse des types retenus

### `ERROR`
- Utilité diagnostic: très élevée (incidents, causes de rejet, échecs provider/délégation).
- Volume probable: modéré en nominal, élevé en incident.
- Sensibilité: potentiellement élevée (`reason`, messages d'exception).
- Bruit: moyen (erreurs techniques parfois redondantes).
- Valeur de persistance: prioritaire.
- Vigilance: prévoir plafonnement/troncature de champs textuels (`reason`).

### `TASK_STARTED`
- Utilité diagnostic: élevée pour timeline d'exécution et démarrage effectif.
- Volume probable: proportionnel aux tâches (généralement contrôlé).
- Sensibilité: faible à moyenne (peut porter `projectId`, `conversationId`).
- Bruit: faible.
- Valeur de persistance: élevée (ancrage temporel).
- Vigilance: `projectId`/`conversationId` à considérer comme potentiellement sensibles.

### `RESPONSE`
- Utilité diagnostic: élevée (fin de cycle, succès, durée).
- Volume probable: élevé mais corrélé aux interactions (1+ par run).
- Sensibilité: faible dans l'état actuel (pas de contenu réponse, seulement métriques).
- Bruit: moyen.
- Valeur de persistance: élevée.
- Vigilance: conserver le format métrique actuel (éviter ajout de contenu brut en phase 3).

### `DELEGATION`
- Utilité diagnostic: très élevée pour les flux multi-agent (resolved/unroutable/delivered/denied).
- Volume probable: potentiellement élevé en architecture multi-agent.
- Sensibilité: moyenne (identifiants agent, statut de policy).
- Bruit: moyen à élevé selon granularité.
- Valeur de persistance: élevée.
- Vigilance: risque de bruit -> recommandation de conserver mais surveiller le ratio `resolved/delivered`.

### `TOOL_CALL`
- Utilité diagnostic: très élevée pour comprendre actions outillées.
- Volume probable: potentiellement élevé (boucles tools, max 20 rounds x 8 calls).
- Sensibilité: moyenne à élevée (`message` résultat outil peut contenir du sensible).
- Bruit: élevé possible.
- Valeur de persistance: élevée, mais type le plus à surveiller.
- Vigilance:
  - conserver seulement metadata utile (toolName, success, round, keys)
  - garder prudence sur `message` même tronqué.

## 5. Analyse du champ attributes

Type réel:
- `Map<String, Object>` dans le modèle de trace.

Usage réel observé:
- `TASK_STARTED`: `entryPoint`, `objectiveLength`, `conversationId`, `projectId`
- `RESPONSE`: `responseLength`, `durationMs`, `success`
- `ERROR`: `category`, `reason`, parfois `provider`, `model`, `status`, `recipientKind`, etc.
- `DELEGATION`: `status`, `recipientKind`, `messageType`, `senderAgentId`, `resolvedAgentId`, `responseError`, `responseLength`
- `TOOL_CALL`: `toolName`, `toolCallId`, `round`, `success`, `message` (tronqué 300), `argumentKeys`

Comportement technique:
- nettoyage des clés/valeurs nulles dans `DefaultAgentTraceService`
- pas de schéma fort, pas de projection SQL aujourd'hui
- affichage admin actuel brute-force (`Map#toString`).

Volume potentiel:
- globalement modéré par événement, sauf risques:
  - `message` des tool calls
  - `reason` d'exception verbeuse.

Risque sensibilité:
- réel (identifiants conversation/projet, message d'erreur provider, résultat outil partiellement exposé).

Décision phase 3:
- le stockage JSON brut est acceptable comme choix transitoire, cohérent avec les décisions validées.
- points de vigilance à documenter:
  - normaliser/tronquer les champs textuels les plus risqués
  - ne pas y stocker de payloads complets d'entrées/sorties LLM
  - préparer une future phase de projection ciblée si besoin analytique.

## 6. Modèle de persistance proposé

Table dédiée proposée: `critical_agent_trace`

Colonnes minimales:
- `id` (PK, `Long`, auto-increment)
- `occurred_at` (`Instant`, NOT NULL)
- `event_type` (`String` enum, NOT NULL)
- `source` (`String`, NOT NULL, défaut logique possible `unknown`)
- `run_id` (`String`, NULL)
- `agent_id` (`String`, NULL)
- `message_id` (`String`, NULL)
- `task_id` (`String`, NULL)
- `attributes_json` (`TEXT`, NOT NULL, JSON sérialisé)

Optionnelles utiles mais non obligatoires:
- `ingested_at` (`Instant`, NOT NULL) pour distinguer l'horodatage de capture DB de `occurred_at`.

Index minimaux:
- `idx_critical_trace_occurred_at` sur `occurred_at`
- `idx_critical_trace_agent_occurred_at` sur `(agent_id, occurred_at)`
- `idx_critical_trace_type_occurred_at` sur `(event_type, occurred_at)`
- `idx_critical_trace_run_id` sur `run_id`
- optionnel selon usage UI: `idx_critical_trace_task_id` sur `task_id`

Position sur `traceId/runId`:
- ne pas contraindre en NOT NULL ni unique
- l'existant peut produire `runId` nul ou non structuré selon chemin
- le stocker tel quel comme corrélation opportuniste (utile mais non contractualisée strictement).

## 7. Stratégie d'intégration

Recommandation: nouveau sink DB dédié, sans refactor transversal.

Approche:
1. Ajouter `CriticalTraceJpaSink implements AgentTraceSink`.
2. Filtrer dans ce sink sur les types critiques validés.
3. Mapper `AgentTraceEvent` -> entité `CriticalAgentTraceEntity`.
4. Sérialiser `attributes` en JSON brut (`TEXT`).
5. Conserver `DefaultAgentTraceService` inchangé (fan-out existant).
6. Conserver `InMemoryAgentTraceSink` et `JsonLinesAgentTraceSink` inchangés.

Pourquoi cette approche:
- respecte l'architecture actuelle par extension (Open/Closed)
- évite la double logique de publication
- limite le risque de régression sur les usages existants (admin runtime, JSONL forensic, Telegram supervision).

## 8. Stratégie de lecture admin

Cible recommandée après implémentation:
- la DB `critical_agent_trace` devient la source de vérité de la vue admin traces.

Évolution de `TraceQueryService`:
- introduire un service de lecture dédié DB (ex: `CriticalTraceQueryService`)
- faire pointer `TraceQueryService` vers cette source principale
- conserver temporairement une cohabitation explicite (fallback mémoire contrôlé) seulement pendant migration.

Règle de gouvernance recommandée:
- à terme, pas d'ambiguïté: `TraceQueryService` lit uniquement la table critique.
- mémoire/JSONL restent des canaux opérationnels secondaires, non sources de vérité admin.

## 9. Concurrence, volume et lecture

Concurrence:
- publication concurrente probable (orchestrateurs + bus + LLM).
- pattern existant montre tolérance aux erreurs sink.
- sink DB doit rester non bloquant pour le flux global (même philosophie que les sinks actuels).

Volume:
- `TOOL_CALL` et `DELEGATION` sont les principaux contributeurs.
- `ERROR` faible en nominal mais critique.
- `TASK_STARTED`/`RESPONSE` plutôt linéaires avec le trafic.

Ordre de lecture:
- tri principal recommandé: `occurred_at DESC`.
- tie-breaker recommandé: `id DESC` pour stabilité si timestamps identiques.

Limitation lecture:
- conserver un `limit` borné (cohérent avec `AdminTechnicalProperties.maxListLimit`).
- pagination complète non nécessaire en phase 3 si `limit` est strict.

## 10. Plan de tests

Unitaires:
- sink DB: persiste un événement retenu.
- sink DB: ignore un type non retenu.
- mapping: corrélation null partiellement remplie.
- sérialisation `attributes` JSON brut (Map vide, clés diverses).

Intégration:
- pipeline complet `DefaultAgentTraceService` + sinks:
  - publication type retenu -> ligne DB présente
  - publication type non retenu -> pas de ligne DB
- requête admin (`TraceQueryService`) tri + filtre agent + limit.

Restart test:
- persister un événement critique
- redémarrer le contexte
- vérifier récupération via service de lecture DB
- vérifier que la donnée mémoire n'est pas requise pour la restitution.

## 11. Risques et vigilance

- Sur-persistance:
  - si le filtre de types dérive, la table peut grossir vite (surtout `TOOL_CALL`).

- JSON brut:
  - risque d'hétérogénéité de schéma et dette analytique future
  - acceptable à court terme si assumé/documenté.

- Données sensibles:
  - `reason` et `message` (tool call) peuvent exposer des infos sensibles
  - vigilance de troncature/sanitization à l'écriture.

- Divergence mémoire / JSONL / DB:
  - pendant coexistence, possibles écarts temporels ou de contenu
  - nécessité de désigner rapidement la DB comme vérité admin.

- Dette future attributes libres:
  - plus le temps passe sans projection minimale, plus l'évolution sera coûteuse
  - prévoir une phase ultérieure de normalisation ciblée si l'usage admin se stabilise.

## 12. Références code

- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/model/AgentTraceEvent.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/model/AgentTraceEventType.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/DefaultAgentTraceService.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/AgentTraceSink.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/InMemoryAgentTraceSink.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/JsonLinesAgentTraceSink.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/model/dto/admin/technical/TechnicalAdminView.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminController.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageController.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/resources/templates/admin/technical/traces.html`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestrator.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/ChatAgentOrchestrator.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmService.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/communication/bus/InMemoryAgentMessageBus.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/AgentTraceCorrelationFactory.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/support/OrchestrationRequestContextFactory.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/resources/application.yml`
- `D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryServiceTest.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/JsonLinesAgentTraceSinkTest.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshotEntity.java`
- `D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/PersistentAdminTaskStore.java`
