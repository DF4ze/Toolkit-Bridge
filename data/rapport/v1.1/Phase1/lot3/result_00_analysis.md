# Analyse préparatoire — Lot 3 — Persistance durable minimale

## 1. Résumé exécutif

Le dépôt contient déjà une base de persistance durable (SQLite/JPA + stockage fichier workspace), mais les objets techniques ciblés par ce lot sont encore majoritairement en mémoire pour la consultation opérationnelle.

Constats clés:
- Les interventions humaines existent en modèle/service (`HumanIntervention*`) mais sont stockées uniquement en `ConcurrentHashMap` et ne sont pas encore branchées dans un flux métier effectif.
- Les tâches admin affichées dans Marcel (`/api/admin/technical/tasks`) viennent d’un store en mémoire borné (`InMemoryAdminTaskStore`) alimenté par le `TaskAgentOrchestrator`.
- Les traces sont doublées: buffer mémoire consultable via admin + JSONL fichier durable. Après redémarrage, la vue admin ne voit plus l’historique (elle lit uniquement le sink mémoire).
- Les états de supervision runtime (registry d’agents, snapshots d’exécution, debug LLM) sont volatils par conception et plutôt des états “instantanés” que des historiques.

Recommandation globale lot 3:
- Prioriser la persistance queryable des 3 blocs demandés (interventions humaines, tâches admin, événements critiques/traces utiles), sans chercher à persister tout le flux observabilité.
- Laisser explicitement hors périmètre les états runtime instantanés et les mémoires conversationnelles court terme.

## 2. Cartographie des objets techniques encore en mémoire

| Objet / concept | Classes principales | Stockage actuel | Utilité | Impact au redémarrage | Criticité persistance | Difficulté estimée | Dépendances / risques |
|---|---|---|---|---|---|---|---|
| Demandes d’intervention humaine | `InMemoryHumanInterventionService`, `HumanInterventionRequest`, `HumanInterventionDecision` | `ConcurrentHashMap<String, HumanInterventionRequest>` | Gouvernance actions sensibles, validation humaine | Perte totale des demandes en attente et décisions | Indispensable (dès que flux branché) | Faible à moyenne | Service actuellement non consommé par contrôleur/orchestrateur; risque de “persister du vide” sans arbitrage fonctionnel |
| Snapshots de tâches admin | `InMemoryAdminTaskStore`, `AdminTaskSnapshot`, `TaskAgentOrchestrator` | `LinkedHashMap<String, AdminTaskSnapshot>` bornée (`maxEvents`) | Suivi technique récent dans Marcel (tasks) | Historique perdu; plus aucune tâche récente visible après restart | Indispensable | Faible à moyenne | `Task` est `DurableObject` mais sans repository; risque d’ambiguïté entre “snapshot d’exploitation” vs “journal métier” |
| Traces consultables admin | `InMemoryAgentTraceSink`, `TraceQueryService`, `TechnicalAdminFacade` | `Deque<AgentTraceEvent>` bornée (`maxEvents`) | Diagnostic rapide via UI admin | Historique UI perdu au restart | Utile à indispensable (au moins pour erreurs/événements critiques) | Moyenne | Déjà un sink fichier JSONL durable en parallèle; risque de double source non alignée |
| Traces fichier (durables mais non requêtables par UI admin) | `JsonLinesAgentTraceSink`, `AgentTraceProperties` | Fichiers `workspace/observability/agent-traces/*.jsonl` | Audit brut et forensic | Survit au restart, mais non exploité par `TraceQueryService` | Utile (déjà en place) | N/A (déjà implémenté) | Pas d’index, pas de purge active ici, pas de lecture côté admin |
| États runtime de supervision agent | `AgentRuntimeRegistry`, `AgentRuntimeState`, `AgentRuntimeExecutionSnapshot`, `AdminAgentQueryService` | `ConcurrentHashMap` + objets en mémoire | “Qui est busy maintenant” | Remise à zéro de l’état instantané | Différable / souvent non pertinent à persister | Moyenne | État dynamique très volatile; persistance brute peut induire des faux états “busy” au reboot |
| Snapshot debug LLM par agent | `LlmDebugStore`, `LlmDebugController` | `ConcurrentHashMap<String, LlmDebugSnapshot>` | Debug ponctuel dernier appel LLM | Dernier debug perdu au restart | Utile mais différable | Faible | Données potentiellement sensibles (prompts/réponses), besoin de politique de rétention stricte |
| Mémoire conversationnelle court terme | `InMemoryConversationMemoryStore`, `DefaultConversationMemoryService` | `ConcurrentHashMap<ConversationMemoryKey, ConversationMemoryState>` + TTL scheduler | Contexte conversationnel à chaud | Contexte perdu au restart | Différable (hors cible lot 3) | Moyenne à élevée | Comportement volontairement éphémère; persister ici changerait fortement le produit |

## 3. Analyse détaillée par objet / composant

### 3.1 Interventions humaines

- Création: `InMemoryHumanInterventionService.open(...)` crée un `requestId` UUID, statut `PENDING`, `createdAt=Instant.now()`, puis `requests.put(...)`.
- Lecture: `findById(...)`, `findPending()` (tri par `createdAt`).
- Mise à jour: `recordDecision(...)` via `computeIfPresent`, transition uniquement si statut courant `PENDING`.
- Suppression: aucune suppression ni purge.
- Effet restart: perte complète de `requests` (en attente + décidées).
- Ambiguïté majeure: aucune utilisation effective du `HumanInterventionService` trouvée dans les flux orchetrateurs/contrôleurs actuels.

### 3.2 Tâches admin

- Création/mises à jour: dans `TaskAgentOrchestrator.handle(...)` via `adminTaskStore.record(...)` sur transitions `CREATED`, `RUNNING`, puis `DONE` ou `FAILED`.
- Modèle stocké: `AdminTaskSnapshot` (taskId, parentTaskId, objective, initiator, assignedAgentId, traceId, entryPoint, status, channel, firstSeen/lastSeen, errorMessage, artifactCount).
- Lecture: `AdminTaskQueryService.listRecentTasks(...)` -> `AdminTaskStore.recent(...)` -> endpoint `/api/admin/technical/tasks`.
- Suppression: implicite par éviction FIFO (`evictOverflow`) selon `toolkit.admin.technical.tasks.max-events`.
- Effet restart: liste des tâches récentes vidée.

### 3.3 Traces et événements utiles au diagnostic

- Production: `DefaultAgentTraceService.trace(...)` publie vers tous les `AgentTraceSink`.
- Sinks actifs:
  - `InMemoryAgentTraceSink`: buffer mémoire borné, sert aussi `AgentTraceQueryService`.
  - `JsonLinesAgentTraceSink`: append en JSONL par agent (persistant fichier).
  - `TelegramAgentTraceSink`: notification supervision (non stockage).
- Lecture admin: `TraceQueryService` consomme uniquement `AgentTraceQueryService` (donc uniquement in-memory aujourd’hui).
- Effet restart:
  - buffer admin perdu,
  - fichiers JSONL conservés mais non réhydratés/requêtés par la couche admin.
- Événements disponibles: `TASK_STARTED`, `CONTEXT_ASSEMBLED`, `TOOL_CALL`, `DELEGATION`, `DEBATE`, `IMPROVEMENT_OBSERVATION`, `IMPROVEMENT_PROPOSAL`, `RESPONSE`, `ERROR`.

### 3.4 États de supervision runtime

- `AgentRuntimeRegistry`: cache singleton par agent (`getOrCreate`, `findByAgentId`, `findByRole`, `findAll`).
- `AgentRuntimeState`: snapshot courant (`busy`, `traceId`, `channel`, etc.) avec transitions `startExecution/finishExecution`.
- Consommation: `AdminAgentQueryService` pour vue agents techniques.
- Effet restart: état instantané réinitialisé (souvent acceptable).

### 3.5 Debug LLM

- `LlmDebugStore.recordSuccess/recordFailure` écrase le dernier snapshot par agent.
- Consultation via `/api/admin/technical/llms/debug/{agentId}`.
- Effet restart: perte du dernier snapshot debug.
- Risque: données potentiellement sensibles malgré sanitation partielle.

## 4. Priorisation recommandée

### Bloc 1 — Interventions humaines

Pourquoi maintenant:
- C’est le plus critique côté contrôle opérationnel: une demande `PENDING` perdue après reboot est une perte de gouvernance.

Périmètre minimal pertinent:
- Persister au moins: ouverture de demande + décision terminale.
- Rendre consultables: `findById`, `findPending`, historique récent.

Hors périmètre explicite:
- Workflow d’escalade avancé, SLA, notifications enrichies, UI complète de pilotage.

### Bloc 2 — Tâches admin

Pourquoi maintenant:
- Déjà alimenté en continu par `TaskAgentOrchestrator`; valeur immédiate en exploitation et post-mortem court.

Périmètre minimal pertinent:
- Persister les snapshots d’état par `taskId` + timestamps first/last seen + error.
- Conserver API actuelle `/technical/tasks` sans changer le contrat.

Hors périmètre explicite:
- Historique complet de toutes transitions détaillées si non nécessaire à court terme.

### Bloc 3 — Événements critiques / traces utiles

Pourquoi maintenant (mais ciblé):
- Le stockage fichier existe déjà, mais la consultation admin est volatile.
- Il faut éviter de persister tout le bruit (`TOOL_CALL` volumineux, traces fines).

Périmètre minimal pertinent:
- Persister une vue “critical trace” requêtable (ex: `ERROR`, `TASK_STARTED`, `RESPONSE`, `DELEGATION`) avec attributs réduits.
- Garder JSONL brut en parallèle pour forensic.

Hors périmètre explicite:
- Centralisation full observability, indexation complète de tous attributs, moteur de recherche avancé.

### Bloc 4 — États de supervision éventuels

Pourquoi pas maintenant (par défaut):
- Ce sont des états instantanés runtime, pas des faits métier; la valeur de les restaurer après reboot est faible.

Périmètre minimal si absolument requis:
- Un “last-seen snapshot” purement informatif, clairement marqué “stale”.

Hors périmètre explicite:
- Restauration automatique d’un état `busy`/`running` au démarrage.

## 5. Périmètre minimal recommandé pour ce lot

1. Ajouter une persistance durable queryable pour:
- `HumanInterventionRequest` (+ décision terminale)
- `AdminTaskSnapshot` (état courant par tâche)
- `CriticalTraceEvent` (sous-ensemble d’événements de trace)

2. Préserver les points d’entrée actuels:
- `AdminTaskStore` reste façade métier
- `AgentTraceService` reste producteur unique
- `TraceQueryService` et `AdminTaskQueryService` continuent à exposer les mêmes DTO

3. Limiter le scope technique:
- Réutiliser SQLite/JPA déjà présent
- Pas de refactor transversal des orchestrateurs
- Pas de migration de la conversation memory dans ce lot

## 6. Objets à laisser volontairement en mémoire pour l’instant

- `AgentRuntimeRegistry` / `AgentRuntimeState` / `AgentRuntimeExecutionSnapshot`: état live uniquement.
- `AgentContextHolder` et `AgentTraceContextHolder` (ThreadLocal): strictement runtime thread.
- `InMemoryConversationMemoryStore`: mémoire de contexte court terme (TTL + compression), hors priorité lot 3.
- `LlmProviderRegistryRuntime.snapshot` et `MemoryRuntimeConfigurationResolver.snapshot`: caches reconstruits depuis DB.

## 7. Questions de conception à arbitrer

1. Interventions humaines: persister seulement l’état courant ou aussi chaque transition (audit complet) ?
2. Interventions humaines: faut-il stocker `metadata` complet (potentiellement sensible) ou un sous-ensemble whitelisté ?
3. Tâches admin: conserver uniquement le dernier snapshot par `taskId` ou historiser les changements d’état ?
4. Traces: quelle liste exacte d’`AgentTraceEventType` est “critique” pour persistance DB ?
5. Traces: conserver `attributes` en JSON brut ou projeter des colonnes dédiées (category/status/reason/agent/task) ?
6. Traces: faut-il charger l’historique fichier JSONL en fallback lecture admin, ou séparer strictement “brut fichier” et “vue critique DB” ?
7. Rétention: appliquer dès ce lot une purge automatique sur nouvelles tables techniques, ou livrer d’abord sans job de purge ?
8. Redémarrage: pour interventions/tâches, faut-il recharger en mémoire au boot ou lire directement depuis repository à la demande ?
9. Human intervention flux: brancher dès ce lot l’ouverture de demande lors des refus de permissions (`AgentPermissionDeniedException`) ou rester au niveau “socle persistence only” ?

## 8. Proposition de sous-phases d’implémentation

### Phase 1 — Persistance interventions humaines (socle)

Objectif:
- Rendre durables les demandes et décisions d’intervention humaine.

Périmètre:
- Nouveau store/repository dédié + adaptation `HumanInterventionService` sans changer les signatures publiques.

Testabilité:
- Tests unitaires du service + tests repository sur cycle `open -> pending -> decision`.

Risque maîtrisé:
- Aucun refactor des orchestrateurs.

### Phase 2 — Persistance tâches admin (snapshot)

Objectif:
- Ne plus perdre la vue `tasks` après redémarrage.

Périmètre:
- Implémentation durable de `AdminTaskStore` (snapshot courant par `taskId`, bornage via requête/retention).

Testabilité:
- Tests de tri, filtres agent/status, mise à jour firstSeen/lastSeen, overflow logique.

Risque maîtrisé:
- Conserver `TaskAgentOrchestrator` inchangé hors wiring store.

### Phase 3 — Traces critiques requêtables

Objectif:
- Disposer d’un historique admin stable pour événements critiques.

Périmètre:
- Nouveau stockage “critical traces” alimenté depuis `DefaultAgentTraceService`/sink dédié.
- `TraceQueryService` lit cette source (ou stratégie hybride claire).

Testabilité:
- Tests par type d’événement conservé/exclu, filtres agent, ordre temporel.

Risque maîtrisé:
- Ne pas toucher à la trace JSONL brute existante.

### Phase 4 — Rétention/purge minimale

Objectif:
- Éviter l’accumulation illimitée des nouvelles données durables.

Périmètre:
- Purge simple par TTL (alignée avec `PersistenceRetentionPolicyResolver`).

Testabilité:
- Tests unitaires de calcul expiration + purge.

Risque maîtrisé:
- Pas d’archivage complexe ni tiering.

### Phase 5 (optionnelle) — États de supervision persistés

Objectif:
- Seulement si besoin explicite exploitation.

Périmètre:
- Snapshot “last seen” non bloquant, marqué obsolète au boot.

Testabilité:
- Tests de lecture UI + indicateur stale.

Risque maîtrisé:
- Ne jamais restaurer un état d’exécution actif.

## 9. Risques et points de vigilance

- Risque de sur-persistance: persister tout `TOOL_CALL`/observabilité créerait du bruit et de la dette de rétention.
- Risque de divergence des sources trace: mémoire, JSONL, DB doivent avoir des rôles explicitement définis.
- Risque sécurité: champs `metadata`, prompts/debug et `attributes` peuvent contenir des données sensibles.
- Risque de faux états supervision: restaurer des états runtime “busy” après redémarrage serait trompeur.
- Risque de périmètre caché: `HumanInterventionService` n’est pas encore branché; clarifier l’objectif fonctionnel avant d’étendre.
- Risque dette modèle: `Task`/`AgentTraceEvent` sont `DurableObject` mais sans persistance homogène actuelle; éviter une implémentation partielle incohérente.

## 10. Références code analysées

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/InMemoryHumanInterventionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionRequest.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionDraft.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionDecision.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/InMemoryAdminTaskStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshot.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/DefaultAgentTraceService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/InMemoryAgentTraceSink.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/JsonLinesAgentTraceSink.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/config/AgentTraceProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeRegistry.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentRuntimeState.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentRuntimeExecutionSnapshot.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/llm/debug/LlmDebugStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/LlmDebugController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/model/DurableObject.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/model/PersistableObjectFamily.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionPolicyResolver.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/store/InMemoryConversationMemoryStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/service/DefaultConversationMemoryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/process/store/WorkspaceExternalProcessStore.java`
- `src/main/resources/application-template.yml`
