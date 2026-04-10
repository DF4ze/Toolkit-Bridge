# Analyse — Phase 4 — Rétention minimale

## 1. Résumé exécutif

Le projet dispose déjà d’un socle de rétention centralisé (`toolkit.persistence.retention`) avec résolution par famille/domaine et exposition admin, mais il n’existe pas encore de purge DB planifiée pour les tables du lot 3.

Conclusion opérationnelle pour la phase 4:
- Réutiliser la mécanique existante `PersistenceRetentionPolicyResolver` plutôt que créer une config parallèle.
- Ajouter une purge planifiée dédiée uniquement pour:
  - `critical_agent_trace` (TTL cible 7 jours)
  - `admin_task_snapshot` (TTL cible 30 jours)
- Garder `human_intervention` hors purge.
- Introduire seulement une petite config de scheduling (`enabled` + `cron`) sous le même namespace de rétention.

## 2. Existant réutilisable

Éléments déjà en place et réutilisables:
- Politique de rétention centralisée:
  - `PersistenceRetentionProperties` (`toolkit.persistence.retention`) avec TTL/disposition par famille et domaines.
  - `PersistenceRetentionPolicyResolver` pour résoudre `ttl + disposition` et calculer des expirations.
- Exposition admin de la rétention:
  - `RetentionQueryService` + vues techniques (`/admin/technical/retention`).
- Persistance lot 3:
  - Traces critiques: `CriticalTraceJpaSink` -> `CriticalAgentTraceRepository` -> table `critical_agent_trace`.
  - Tâches admin: `PersistentAdminTaskStore` -> `AdminTaskSnapshotRepository` -> table `admin_task_snapshot`.
  - Interventions humaines: `PersistentHumanInterventionService` -> `HumanInterventionRepository` -> table `human_intervention`.
- Scheduling Spring déjà activé globalement (`@EnableScheduling`) et pattern existant de cleanup (`ConversationMemoryCleanupScheduler`).

Points de vigilance d’alignement:
- La config de rétention existe déjà; créer `toolkit.persistence.retention.traces-days/tasks-days` ferait doublon.
- Valeur par défaut actuelle pour la famille `TASK`: `30d + PRESERVE`, pas `PURGE`.
  - Donc, pour respecter la phase 4 sans casser la logique globale future, mieux vaut passer par un domaine dédié (ex: `admin_snapshot`) plutôt que modifier brutalement la famille `TASK`.

## 3. Données ciblées par la purge

### 3.1 Traces critiques

- Table/repository:
  - table `critical_agent_trace`
  - `CriticalAgentTraceRepository`
- Timestamp de référence recommandé:
  - `occurred_at` (cohérent métier: âge de l’événement)
  - `ingested_at` existe mais reflète l’ingestion, pas l’occurrence métier.
- Volumétrie potentielle:
  - élevée, car les types critiques incluent `ERROR`, `TASK_STARTED`, `RESPONSE`, `DELEGATION`, `TOOL_CALL`.
  - `TOOL_CALL` peut produire beaucoup d’entrées.
- Impact fonctionnel d’une purge:
  - réduit l’historique visible dans l’admin technique (`TraceQueryService`).
  - diminue la profondeur d’audit court terme, mais conserve l’objectif de supervision récente.

### 3.2 Tâches admin

- Table/repository:
  - table `admin_task_snapshot`
  - `AdminTaskSnapshotRepository`
- Timestamp de référence recommandé:
  - `last_seen_at` (et non `first_seen_at`), car le snapshot est upserté par `task_id`.
- Impact fonctionnel d’une purge:
  - retire des tâches anciennes des vues admin.
  - pas d’impact sur l’exécution en cours si TTL raisonnable.
- Vigilance `firstSeenAt` / `lastSeenAt`:
  - `first_seen_at` = date de création snapshot.
  - `last_seen_at` = date du dernier update d’état.
  - Purger sur `first_seen_at` supprimerait trop tôt des tâches longues/mises à jour récemment.

### 3.3 Interventions humaines

Raison de rester hors périmètre de purge en phase 4:
- la table `human_intervention` porte des décisions humaines (`decision_status`, `decision_decided_at`) et des demandes potentiellement encore en attente.
- la suppression prématurée peut casser le suivi opérationnel et la traçabilité de décisions sensibles.
- la phase cible explicitement seulement traces critiques + tâches admin.

## 4. Stratégie de purge recommandée

Stratégie minimale, robuste, lisible:
- Critère de purge:
  - traces: `delete where occurred_at < cutoff`
  - tâches admin: `delete where last_seen_at < cutoff`
- Cutoff:
  - `cutoff = now - ttl`
  - TTL résolu via `PersistenceRetentionPolicyResolver` (famille + domaine dédié).
- Fréquence recommandée:
  - 1 exécution quotidienne (cron configurable), suffisante pour TTL en jours.
- Comportement DB vide:
  - suppression retourne `0`, log info succinct.
- TTL absent / invalide / <= 0:
  - ne pas purger ce type de données.
  - log `WARN` explicite avec la valeur.
- Purge partiellement échouée:
  - exécuter traces et tâches dans deux blocs isolés.
  - si une purge échoue, log `ERROR`, continuer l’autre, ne pas faire échouer tout le scheduler.

## 5. Configuration proposée

### Principe

Étendre l’existant au lieu de créer un mécanisme concurrent.

### Recommandation

Conserver la politique de rétention sous:
- `toolkit.persistence.retention.*` (déjà existant)

Ajouter uniquement la config de planification:
- `toolkit.persistence.retention.cleanup.enabled` (bool)
- `toolkit.persistence.retention.cleanup.cron` (cron)

Configurer TTL/disposition via domaines dédiés:
- `toolkit.persistence.retention.families.trace.domains.critical.ttl: 7d`
- `toolkit.persistence.retention.families.trace.domains.critical.disposition: PURGE`
- `toolkit.persistence.retention.families.task.domains.admin_snapshot.ttl: 30d`
- `toolkit.persistence.retention.families.task.domains.admin_snapshot.disposition: PURGE`

Pourquoi ce modèle:
- évite `traces-days/tasks-days` en double.
- évite de changer la disposition globale `TASK` (actuellement `PRESERVE`) pour tous les usages futurs.
- exploite une capacité déjà disponible dans `PersistenceRetentionProperties` (domaines).

## 6. Stratégie d’intégration

Intégration recommandée:
- Créer un service dédié de purge (responsabilité unique):
  - exécute purge traces + purge tâches admin
  - résout politiques via `PersistenceRetentionPolicyResolver`
  - appelle les repositories de suppression
- Créer un scheduler dédié:
  - `@Scheduled(cron = ...)`
  - `@ConditionalOnProperty` sur `toolkit.persistence.retention.cleanup.enabled`
- Ne pas coupler la purge aux services métier:
  - pas d’appel depuis orchestrateurs, bus, services de persistance métier.
  - exécution indépendante et planifiée.

Éviter l’ambiguïté avec la mécanique existante:
- documenter explicitement que cette purge est l’exécution opérationnelle des policies `toolkit.persistence.retention`, pas une seconde politique.
- nommer clairement les domaines utilisés (`critical`, `admin_snapshot`).

## 7. Logging et observabilité

Logs minimum recommandés:
- `INFO` par type purgé:
  - type (`critical_trace` / `admin_task_snapshot`)
  - cutoff
  - TTL résolu
  - nombre de lignes supprimées
  - durée d’exécution
- `WARN`:
  - purge skip car disposition != `PURGE`
  - TTL null/non positif
- `ERROR`:
  - exception de purge par type, avec contexte (type + cutoff)

À éviter:
- logs par ligne supprimée
- logs verbeux à chaque scan sans action.

## 8. Plan de tests

Tests unitaires à prévoir:
- résolution policy:
  - domaine `critical` -> TTL 7d purge
  - domaine `admin_snapshot` -> TTL 30d purge
- service de purge:
  - purge normale traces/tâches (repositories appelés avec cutoff correct)
  - TTL invalide/non positif -> skip
  - disposition non `PURGE` -> skip
  - erreur sur traces n’empêche pas purge tâches (et inversement)
- scheduler:
  - déclenche le service quand enabled
  - n’exécute pas quand disabled

Tests d’intégration à prévoir:
- SQLite IT purge traces:
  - données anciennes supprimées, données récentes conservées.
- SQLite IT purge tâches:
  - purge basée sur `last_seen_at`, pas `first_seen_at`.
- Vérification non-régression:
  - `human_intervention` inchangé.

Cas limites minimum:
- purge avec DB vide
- TTL absent/invalide/désactivé
- conservation stricte des données récentes
- non-purge des interventions humaines.

## 9. Risques et vigilance

- Risque de doublon de mécanique:
  - créer `traces-days/tasks-days` en parallèle de `toolkit.persistence.retention` fragmenterait la gouvernance.
- Risque de purge trop agressive:
  - `TASK` global est `PRESERVE`; le passer globalement en `PURGE` peut impacter des persistances futures.
  - d’où recommandation domaine `admin_snapshot`.
- Risque de couplage métier:
  - si purge appelée depuis flux runtime (orchestrateurs), impact perf/comportement.
- Risque scheduler envahissant:
  - fréquence trop courte inutile pour TTL en jours.
- Risque de dette futures phases:
  - sans convention de domaines, la rétention `TASK/TRACE` peut devenir ambiguë quand d’autres tables apparaîtront.

## 10. Références code

- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionProperties.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionPolicyResolver.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/RetentionQueryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/CriticalTraceJpaSink.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/CriticalAgentTraceEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/CriticalAgentTraceRepository.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/CriticalAgentTraceMapper.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshotEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshotRepository.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/PersistentAdminTaskStore.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionRepository.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/memory/conversation/service/ConversationMemoryCleanupScheduler.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/ToolkitBridgeApplication.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigKey.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationSeedService.java`
- `src/main/resources/application.yml`
