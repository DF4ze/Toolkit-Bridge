# Analyse — Phase 1 — Interventions humaines

## 1. Résumé exécutif

L’existant `HumanIntervention*` est cohérent mais entièrement volatile: les demandes et leur statut vivent dans `InMemoryHumanInterventionService` via `ConcurrentHashMap`, avec perte totale au redémarrage.

Points structurants observés:
- Le contrat fonctionnel minimal est déjà clair (`open`, `findById`, `findPending`, `recordDecision`).
- Le modèle distingue bien intention d’ouverture (`HumanInterventionDraft`), état courant (`HumanInterventionRequest`) et décision (`HumanInterventionDecision`).
- Limite majeure actuelle: `recordDecision` ne conserve que le nouveau statut dans `HumanInterventionRequest`; les détails de décision (`decidedAt`, `actorId`, `channel`, `comment`, `metadata`) ne sont pas stockés durablement ni même en mémoire après l’appel (hors side-effects notifier).
- Le service est aujourd’hui peu branché dans le flux métier (pas d’appel applicatif trouvé à `HumanInterventionService.open(...)` en production).

Recommandation phase 1:
- Introduire une persistance minimale par table dédiée “interventions humaines” (état courant + décision finale en colonnes nullable), sans historisation complète.
- Garder strictement le contrat de service existant.
- Lire directement depuis la base (pas de cache de réhydratation au boot pour cette phase).

## 2. Modèle existant

### 2.1 `HumanInterventionDraft`

Rôle:
- Objet d’entrée pour `open(...)`.
- Porte les données initiales avant création d’une intervention.

Structure:
- `traceId` (optionnel normalisé)
- `agentId` (obligatoire, trim)
- `sensitiveAction` (obligatoire)
- `kind` (obligatoire)
- `summary` (obligatoire, trim)
- `detail` (optionnel normalisé)
- `metadata: Map<String, Object>` (optionnel -> `Map.of()`)

Contraintes:
- validations dans le compact constructor.
- copie immuable de `metadata` (`Map.copyOf`).

### 2.2 `HumanInterventionRequest`

Rôle:
- État courant persistant/logique d’une intervention.

Structure:
- `requestId` (obligatoire)
- `createdAt` (obligatoire)
- `traceId` (optionnel)
- `agentId` (obligatoire)
- `sensitiveAction` (obligatoire)
- `kind` (obligatoire)
- `status` (obligatoire)
- `summary` (obligatoire)
- `detail` (optionnel)
- `metadata: Map<String, Object>` (optionnel -> immutable map)

Particularité:
- `withStatus(...)` ne modifie que `status`; aucune trace de la décision finale n’est incorporée dans l’objet.

### 2.3 `HumanInterventionDecision`

Rôle:
- Commande de décision terminale appliquée via `recordDecision(...)`.

Structure:
- `requestId` (obligatoire)
- `status` terminal (`APPROVED` ou `REJECTED`, jamais `PENDING`)
- `decidedAt` (obligatoire)
- `actorId` (optionnel)
- `channel` (optionnel)
- `comment` (optionnel)
- `metadata: Map<String, Object>` (optionnel)

Observation critique:
- Ces informations décisionnelles ne sont pas conservées dans le store actuel, sauf passage au notifier au moment du call.

### 2.4 `HumanInterventionService`

Rôle:
- Contrat métier minimal pour la phase.

Méthodes:
- `open(HumanInterventionDraft)`
- `findById(String)`
- `findPending()`
- `recordDecision(HumanInterventionDecision)`

### 2.5 `InMemoryHumanInterventionService`

Rôle:
- Implémentation courante du contrat.

Stockage:
- `Map<String, HumanInterventionRequest> requests = new ConcurrentHashMap<>()`

Flux:
- `open`: construit un `HumanInterventionRequest` PENDING, `put`, notifie.
- `findById`: lookup map.
- `findPending`: filtre sur `PENDING`, tri `createdAt` ascendant.
- `recordDecision`: `computeIfPresent`; applique transition seulement si status courant `PENDING`; notifie si update effectif.

Relations:
- Dépend de `HumanInterventionNotifier` (Telegram implémentée).

## 3. Cycle de vie détaillé

### 3.1 Création (`open`)

Séquence:
1. validation draft non null.
2. génération `requestId = UUID`.
3. `createdAt = Instant.now()`.
4. projection du draft vers request avec `status=PENDING`.
5. insertion map.
6. notification `onRequestOpened` best-effort.

Invariants:
- une request créée est toujours `PENDING`.
- `requestId` unique attendu (UUID).
- `agentId`, `summary`, `sensitiveAction`, `kind` non nuls/non blancs.

### 3.2 État `PENDING`

- visible via `findPending`.
- tri chronologique croissant.
- aucune expiration/purge.

### 3.3 Transition de décision (`recordDecision`)

Séquence:
1. validation decision non null.
2. `computeIfPresent(requestId)`.
3. si request absente => aucun effet, `Optional.empty()`.
4. si request non PENDING => aucun effet, `Optional.empty()`.
5. si request PENDING => nouvelle request `withStatus(decision.status())`.
6. notification `onDecisionRecorded` best-effort.

Invariants:
- transition autorisée uniquement de `PENDING` vers terminal.
- idempotence partielle: une 2e décision sur request déjà terminale est ignorée.

### 3.4 État final

- `status` devient `APPROVED` ou `REJECTED`.
- la request reste dans le store.
- les données détaillées de décision ne sont pas conservées côté store.

### 3.5 Cas limites identifiés

- `recordDecision` sur `requestId` inconnu: pas d’exception, `Optional.empty()`.
- `recordDecision` concurrent sur même request: `computeIfPresent` évite double transition effective.
- Notifier en erreur: la demande/décision reste traitée (best-effort).
- redémarrage: perte de toutes les interventions (pending + finalisées).

## 4. Analyse du champ metadata

### 4.1 Où il est défini

- `HumanInterventionDraft.metadata: Map<String, Object>`
- `HumanInterventionRequest.metadata: Map<String, Object>`
- `HumanInterventionDecision.metadata: Map<String, Object>`

### 4.2 Type réel et usage observé

Type:
- `Map<String, Object>` immutable par `Map.copyOf`.

Contenus observés dans le code/tests:
- `Map.of("toolName", "deploy")` (test d’ouverture)
- `Map.of()` (plusieurs tests)

Usage effectif:
- Copié draft -> request à l’ouverture.
- Non exploité dans la logique métier du service.
- Non utilisé par `TelegramHumanInterventionNotifier` (qui affiche seulement `requestId`, `agentId`, `action`, `kind`, `summary`, `trace`, et certains champs de décision).
- Métadonnées de décision non persistées en l’état actuel du store.

### 4.3 Utilité et risques

Utilité potentielle:
- stocker du contexte technique (nom d’outil, cible, identifiants corrélés).
- utile pour audit rapide et diagnostic.

Risques:
- données sensibles possibles (tokens, chemins sensibles, payloads bruts).
- croissance non contrôlée (objets volumineux/imbriqués).
- hétérogénéité forte (Map libre), faible requêtabilité SQL.

### 4.4 Options de persistance proposées

1. Stockage complet (JSON brut)
- Principe: conserver `request.metadata` + `decision.metadata` en JSON text.
- Avantages: zéro perte fonctionnelle, très simple à intégrer.
- Inconvénients: risque sécurité/volume élevé, faible gouvernance.

2. Stockage filtré (recommandé)
- Principe: whitelist de clés autorisées + limite taille valeur/JSON + redaction simple.
- Avantages: meilleur compromis valeur/risque, conserve le contexte utile.
- Inconvénients: nécessite arbitrage des clés et règles.

3. Exclusion partielle
- Principe: ne pas stocker metadata brute; garder seulement champs dérivés utiles (ex: `toolName`, `target`, `reasonCode`).
- Avantages: sécurité et lisibilité élevées.
- Inconvénients: perte d’informations de diagnostic non prévues.

## 5. Proposition de modèle de persistance

Modèle minimal recommandé: une table unique d’état courant enrichie des champs de décision finale.

Nom proposé:
- `human_intervention`

Champs obligatoires:
- `request_id` (TEXT, unique, not null)
- `created_at` (TIMESTAMP/Instant, not null)
- `agent_id` (TEXT, not null)
- `sensitive_action` (TEXT enum, not null)
- `kind` (TEXT enum, not null)
- `status` (TEXT enum, not null)
- `summary` (TEXT, not null)

Champs optionnels:
- `trace_id` (TEXT, nullable)
- `detail` (TEXT, nullable)
- `request_metadata_json` (TEXT, nullable)

Champs décision finale (nullable tant que PENDING):
- `decision_status` (TEXT enum, nullable)
- `decision_decided_at` (TIMESTAMP/Instant, nullable)
- `decision_actor_id` (TEXT, nullable)
- `decision_channel` (TEXT, nullable)
- `decision_comment` (TEXT, nullable)
- `decision_metadata_json` (TEXT, nullable)

Indexes minimaux:
- `ux_human_intervention_request_id` unique (`request_id`)
- `idx_human_intervention_status_created_at` (`status`, `created_at`) pour `findPending`
- `idx_human_intervention_agent_created_at` (`agent_id`, `created_at`) utile exploitation
- optionnel: `idx_human_intervention_trace_id` (`trace_id`)

Remarques compatibilité SQLite/JPA:
- format enum en `STRING`.
- JSON en `TEXT` (ObjectMapper côté service/convertisseur).
- aucun besoin de relation join pour phase 1.

## 6. Stratégie d’intégration

Objectif:
- conserver strictement l’API `HumanInterventionService`.

Approche recommandée: remplacement interne par implémentation durable (pas wrapper cache)
- créer une implémentation repository-backed de `HumanInterventionService`.
- conserver signatures et sémantique (Optional empty, transition only from PENDING, notification best-effort).
- conserver `InMemoryHumanInterventionService` uniquement si nécessaire pour tests ciblés ou profil spécifique.

Pourquoi ce choix:
- pas de refactor des appelants (contrat inchangé).
- évite les doubles sources de vérité (DB + map).
- limite le risque de divergences de cohérence.

Points de vigilance d’intégration:
- `recordDecision` doit rester atomique (équivalent logique du `computeIfPresent`).
- préserver l’ordre et le périmètre des notifications (`onRequestOpened`, `onDecisionRecorded`).

## 7. Stratégie de chargement

Question: recharger en mémoire au boot vs lecture DB directe ?

Recommandation phase 1: lecture DB directe, sans cache de réhydratation au démarrage.

Arguments:
- volume attendu faible en phase 1.
- cohérence simple: une seule source de vérité.
- évite logique de bootstrap/sync cache qui ajoute de la complexité inutile.
- aligne avec d’autres services queryables déjà DB-backed (ex: artifacts metadata, config administrable).

Option future (si volume augmente):
- cache lecture seule pour `findPending`, invalidé sur `open`/`recordDecision`.

## 8. Plan de tests

### 8.1 Tests unitaires service

- `open`:
  - crée `PENDING` avec champs normalisés.
  - appelle notifier `onRequestOpened`.
- `findById`:
  - retourne vide pour id null/blank/inconnu.
- `findPending`:
  - ne retourne que `PENDING`, tri chronologique.
- `recordDecision`:
  - applique transition `PENDING -> APPROVED/REJECTED`.
  - ignore id inconnu.
  - ignore décision sur request déjà terminale.
  - notifie `onDecisionRecorded` uniquement si transition effective.

### 8.2 Tests intégration persistence (SQLite/JPA)

- scénario `open -> findById -> findPending` persistant.
- scénario `open -> recordDecision -> findPending vide`.
- persistance des champs décision finale.
- persistance/lecture `metadata` selon stratégie retenue (complet/filtré).

### 8.3 Test “après restart”

- écrire des interventions avec un contexte Spring A.
- redémarrer contexte Spring B pointant la même DB test.
- vérifier récupération `findById` et `findPending`.

## 9. Risques et vigilance

Risques techniques:
- perte de sémantique actuelle si `recordDecision` devient “upsert” non contrôlé.
- gestion concurrente de décision à sécuriser (optimistic locking ou update conditionnel sur statut).

Risques sécurité (metadata):
- stockage de secrets/payloads sensibles.
- croissance de JSON non bornée.

Risques d’évolution:
- si historisation demandée plus tard, table d’état courant seule ne suffit pas pour audit complet.
- service peu branché aujourd’hui: risque de livrer une persistance correcte mais non utilisée sans étape d’intégration fonctionnelle.

Mesures minimales recommandées:
- limite de taille pour `summary/detail/metadata`.
- stratégie explicite de redaction metadata.
- index `status + created_at` pour stabilité de `findPending`.

## 10. Références code

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/InMemoryHumanInterventionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionRequest.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionDecision.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionDraft.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionNotifier.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramHumanInterventionNotifier.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/InMemoryHumanInterventionServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/telegram/TelegramHumanInterventionNotifierTest.java`
- `src/main/resources/application-template.yml`
