# Implémentation — Phase 4 — Rétention minimale

## 1. Résumé des modifications

Implémentation réalisée pour appliquer une purge planifiée minimale sur:
- traces critiques (`critical_agent_trace`)
- snapshots de tâches admin (`admin_task_snapshot`)

Sans refactor global ni configuration parallèle:
- réutilisation stricte de `toolkit.persistence.retention`
- réutilisation de `PersistenceRetentionPolicyResolver`
- interventions humaines explicitement non concernées

## 2. Réutilisation de la mécanique existante

Réutilisation directe du socle existant:
- `PersistenceRetentionPolicyResolver` pour résoudre `ttl + disposition`.
- familles/domaine utilisés:
  - `TRACE` + domaine `critical`
  - `TASK` + domaine `admin_snapshot`

Décision d’implémentation:
- aucune config `traces-days` / `tasks-days` ajoutée.
- aucune logique de TTL codée en dur dans le service.

## 3. Configuration scheduling

Ajouts sur le namespace existant `toolkit.persistence.retention`:
- `toolkit.persistence.retention.cleanup.enabled`
- `toolkit.persistence.retention.cleanup.cron`

Valeurs ajoutées:
- `enabled: true`
- `cron: "0 0 3 * * *"` (1 fois/jour)

TTL/disposition configurés en YAML via familles/domaine:
- `toolkit.persistence.retention.families.trace.domains.critical.ttl: 7d`
- `toolkit.persistence.retention.families.trace.domains.critical.disposition: PURGE`
- `toolkit.persistence.retention.families.task.domains.admin_snapshot.ttl: 30d`
- `toolkit.persistence.retention.families.task.domains.admin_snapshot.disposition: PURGE`

## 4. Service de purge

Créé:
- `PersistenceRetentionCleanupService`

Responsabilités:
- résolution des policies via `PersistenceRetentionPolicyResolver`
- calcul `cutoff = now - ttl`
- purge traces: `deleteByOccurredAtBefore(cutoff)`
- purge tâches: `deleteByLastSeenAtBefore(cutoff)` (jamais `firstSeenAt`)
- isolation des erreurs par type purgé:
  - échec traces n’empêche pas purge tâches
  - échec tâches n’empêche pas purge traces

Gestion de garde:
- disposition != `PURGE` -> skip + `WARN`
- TTL `null`, `0`, négatif -> skip + `WARN`

## 5. Scheduler

Créé:
- `PersistenceRetentionCleanupScheduler`

Comportement:
- `@Scheduled(cron = "${toolkit.persistence.retention.cleanup.cron:0 0 3 * * *}")`
- activation conditionnelle:
  - `@ConditionalOnProperty(prefix="toolkit.persistence.retention.cleanup", name="enabled", havingValue="true", matchIfMissing=true)`
- délègue à `PersistenceRetentionCleanupService.cleanup()`

## 6. Logging

Implémenté au niveau minimal demandé:
- `INFO`:
  - cible purgée (`critical_trace` / `admin_task_snapshot`)
  - famille + domaine
  - TTL
  - cutoff
  - nombre de lignes supprimées
  - durée
- `WARN`:
  - purge ignorée (disposition non `PURGE`)
  - purge ignorée (TTL invalide)
- `ERROR`:
  - exception pendant purge d’une cible, avec poursuite de l’autre purge

## 7. Tests

### Tests unitaires ajoutés

`PersistenceRetentionCleanupServiceTest`:
- purge normale quand policies `PURGE` + TTL valide
- skip quand disposition != `PURGE`
- skip quand TTL invalide (`null`/`0`)
- purge partielle: exception traces, purge tâches continue

### Test d’intégration ajouté

`PersistenceRetentionCleanupIT` (SQLite):
- vérifie suppression des traces anciennes et conservation des récentes
- vérifie suppression des snapshots de tâches basée sur `lastSeenAt`
- vérifie que `human_intervention` n’est pas touché

### Commande exécutée

`./mvnw "-Dtest=PersistenceRetentionCleanupServiceTest,PersistenceRetentionCleanupIT" test`

Résultat: succès.

## 8. Limites connues

- La purge est volontairement minimale (pas d’archivage, pas de batch avancé).
- L’isolation d’erreur est gérée par bloc fonctionnel traces/tâches, sans orchestration transactionnelle complexe par cible.
- Les logs d’erreur de test unitaire de panne simulée sont attendus (cas de test de robustesse).

