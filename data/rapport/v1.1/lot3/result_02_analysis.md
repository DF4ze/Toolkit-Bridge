# Analyse — Phase 2 — Tâches admin

## 1. Résumé exécutif

Le flux actuel des tâches admin est déjà bien structuré autour d’un contrat (`AdminTaskStore`) consommé par l’orchestrateur (`TaskAgentOrchestrator`) et la couche query (`AdminTaskQueryService`).

Limite principale:
- `InMemoryAdminTaskStore` stocke uniquement en mémoire (`LinkedHashMap`), donc perte totale au redémarrage.

Constat clé pour la phase 2:
- la décision “dernier snapshot par taskId uniquement” est naturellement alignée avec l’impl actuelle (la map remplace déjà l’état précédent par taskId).
- on peut donc remplacer le backend store sans toucher les APIs ni le flux orchestrateur.

Recommandation:
- implémentation repository-backed de `AdminTaskStore` avec table “snapshot courant”, lecture DB directe, requêtes triées/filtrées/limit.

## 2. Modèle existant

### 2.1 `AdminTaskSnapshot`

Rôle:
- projection d’exploitation d’une tâche pour les vues admin techniques.

Structure:
- identité/corrélation: `taskId`, `parentTaskId`, `traceId`
- contenu: `objective`, `initiator`, `assignedAgentId`, `entryPoint`
- état: `status`, `errorMessage`, `artifactCount`
- contexte canal: `channelType`, `conversationId`
- temporalité: `firstSeenAt`, `lastSeenAt`

### 2.2 `AdminTaskStore`

Rôle:
- port d’accès abstrait pour stockage/lecture des snapshots admin.

Méthodes:
- `record(Task task, String channelType, String conversationId, String errorMessage)`
- `recent(int limit, String agentId, TaskStatus status)`

### 2.3 `InMemoryAdminTaskStore`

Rôle:
- impl actuelle du port en mémoire.

Comportement:
- `record`:
  - ignore `task == null`
  - supprime l’ancien snapshot du même `taskId` puis réinsère (maintien “dernier état”)
  - conserve `firstSeenAt` initial, met à jour `lastSeenAt`
  - normalise `errorMessage`
  - calcule `artifactCount` depuis `task.artifacts().size()`
  - éviction FIFO si dépassement `maxEvents`
- `recent`:
  - parcours inverse de l’ordre d’insertion (plus récents d’abord)
  - filtre optionnel `assignedAgentId` (case-insensitive)
  - filtre optionnel `status`
  - limite résultat

### 2.4 `TaskAgentOrchestrator`

Rôle pour ce lot:
- producteur des snapshots admin.

Points d’écriture dans `AdminTaskStore`:
- après création task: `CREATED`
- après transition: `RUNNING`
- fin nominale: `DONE`
- erreurs: `FAILED` (+ message)

Observation:
- ce composant ne dépend que du contrat `AdminTaskStore`; bon découplage déjà en place.

### 2.5 `AdminTaskQueryService`

Rôle:
- lecture côté exploitation via `taskStore.recent(...)`.

Comportement:
- applique `sanitizeLimit` via `AdminTechnicalProperties`
- mappe `AdminTaskSnapshot` vers DTO `TechnicalAdminView.TaskItem`

## 3. Cycle de vie des tâches

Cycle observé dans `TaskAgentOrchestrator`:
1. `TaskFactory.createObjectiveTask(...)` -> `TaskStatus.CREATED`
2. `task.transitionTo(RUNNING)`
3. issue:
- succès -> `DONE`
- échec provider/orchestration/empty response -> `FAILED`

Champs stables sur la vie d’une task:
- `taskId`
- `parentTaskId`
- `objective`
- `initiator`
- `assignedAgentId`
- `traceId`
- `entryPoint`
- `channelType` / `conversationId` (dans ce flux)
- `firstSeenAt` (dans le store)

Champs évolutifs:
- `status`
- `lastSeenAt`
- `errorMessage`
- `artifactCount`

Invariants utiles:
- un snapshot courant unique par `taskId`
- `firstSeenAt <= lastSeenAt`
- terminalité possible via `DONE/FAILED/CANCELLED` (même si orchestrateur utilise surtout DONE/FAILED)

## 4. Analyse du snapshot

Champs critiques à persister:
- `taskId` (clé métier)
- `status`
- `firstSeenAt`, `lastSeenAt`
- `assignedAgentId` (filtre exploitation)
- `traceId` (corrélation)
- `errorMessage` (diagnostic)
- `artifactCount`
- `objective`, `entryPoint`, `initiator`, `channelType`, `conversationId`, `parentTaskId` (lecture utile)

Champs potentiellement dérivables:
- `artifactCount` est dérivable depuis artefacts, mais utile en lecture rapide; garder pour éviter jointure/coût.

Champs sensibles:
- `objective` et `errorMessage` peuvent contenir du texte potentiellement sensible (mais déjà exposé en admin aujourd’hui).

Modèle minimal recommandé:
- conserver l’ensemble du record actuel pour préserver compatibilité UI/query.

## 5. Modèle de persistance proposé

Table proposée: `admin_task_snapshot`

Colonnes:
- `task_id` (TEXT, unique, not null)
- `parent_task_id` (TEXT, nullable)
- `objective` (TEXT, not null)
- `initiator` (TEXT, not null)
- `assigned_agent_id` (TEXT, not null)
- `trace_id` (TEXT, not null)
- `entry_point` (TEXT enum string, not null)
- `status` (TEXT enum string, not null)
- `channel_type` (TEXT, nullable)
- `conversation_id` (TEXT, nullable)
- `first_seen_at` (TIMESTAMP/Instant, not null)
- `last_seen_at` (TIMESTAMP/Instant, not null)
- `error_message` (TEXT, nullable)
- `artifact_count` (INTEGER, not null)

Index minimaux:
- unique: `task_id`
- index lecture récente: `(last_seen_at DESC)`
- index filtres: `(assigned_agent_id, last_seen_at DESC)`, `(status, last_seen_at DESC)`
- optionnel corrélation: `(trace_id)`

Note:
- pas de table historique, pas de purge dans cette phase.

## 6. Stratégie d’intégration

Approche recommandée:
- créer une implémentation DB de `AdminTaskStore` (ex: `PersistentAdminTaskStore`) et la rendre bean principal.
- conserver `AdminTaskStore` inchangé.
- ne pas toucher `TaskAgentOrchestrator` ni `AdminTaskQueryService` (ils injectent déjà l’interface).

Mapping:
- `Task + context(channel/conversation/error)` -> entité snapshot
- entité -> `AdminTaskSnapshot`

Écriture:
- upsert logique par `taskId`:
  - insert si absent
  - update si existant en préservant `firstSeenAt` existant

## 7. Gestion du “recent”

Équivalent DB de la logique actuelle:
- tri principal: `lastSeenAt DESC`
- filtres optionnels:
  - `assignedAgentId` (normalisation case-insensitive)
  - `status`
- limitation: `LIMIT effectiveLimit`

Remplacement du `maxEvents` mémoire:
- `maxEvents` ne pilote plus l’éviction.
- la limitation se fait en lecture (`sanitizeLimit` + `LIMIT`).
- conservation illimitée en phase 2 (décision validée).

## 8. Concurrence

Situation actuelle:
- in-memory synchronisé (`synchronized`) évite les races locales.

Risque en DB:
- écritures successives rapprochées pour un même `taskId` (CREATED -> RUNNING -> DONE/FAILED).

Stratégie simple recommandée:
- clé unique `task_id` + update ciblé par `task_id`.
- en cas de collision insert, fallback update.
- pas de verrouillage complexe ni versioning obligatoire pour cette phase.

Point de vigilance:
- préserver `firstSeenAt` initial lors des updates.

## 9. Tests

Tests unitaires à prévoir:
- `record`:
  - création snapshot
  - mise à jour du même `taskId` conserve `firstSeenAt` et met `lastSeenAt`
  - normalisation `errorMessage`
- `recent`:
  - ordre `lastSeenAt DESC`
  - filtre agent (case-insensitive)
  - filtre status
  - limite

Tests intégration (SQLite/JPA):
- upsert par `taskId` (pas de duplication)
- `recent` avec filtres/limit retourne le même comportement que store actuel

Test restart:
- écrire snapshots
- redémarrer contexte
- vérifier lecture `recent` non vide et cohérente

Non requis phase 2:
- tests de purge/rétention
- tests d’historisation (hors périmètre)

## 10. Risques

- Divergence de sémantique si `recent` ne respecte pas exactement l’ordre/filtrage actuels.
- Perte de `firstSeenAt` si update écrase la valeur initiale.
- Accumulation de volume DB (acceptée pour cette phase, mais dette future à cadrer lot ultérieur).
- Risque de couplage si l’impl DB expose des détails persistence vers orchestrateur/query (à éviter via `AdminTaskStore`).

## 11. Références code

- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshot.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/InMemoryAdminTaskStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/config/AdminTechnicalProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/admin/technical/TechnicalAdminView.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/task/model/Task.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/task/model/TaskStatus.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/task/model/TaskEntryPoint.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/task/model/TaskLifecycle.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/task/InMemoryAdminTaskStoreTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminTaskQueryServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestratorTest.java`
- `src/main/resources/application-template.yml`
