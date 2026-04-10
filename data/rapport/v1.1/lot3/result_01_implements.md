# Implémentation — Phase 1 — Interventions humaines

## 1. Résumé des modifications

La persistance durable des interventions humaines est implémentée en périmètre minimal, sans modification de l’API publique `HumanInterventionService`.

Livré:
- entité JPA dédiée (table unique)
- repository avec update conditionnel `status=PENDING`
- service persistant `PersistentHumanInterventionService` (bean Spring principal)
- mapping Entity ↔ Domain centralisé
- gestion JSON metadata (filtrage + limite)
- tests unitaires + tests intégration + test restart

Non livré volontairement:
- historisation complète des transitions
- cache mémoire de réhydratation
- workflow avancé d’intervention

## 2. Entité et repository

Fichiers:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionRepository.java`

Entité:
- table `human_intervention`
- index:
  - unique `request_id`
  - composite `(status, created_at)`
- enums en `STRING`
- JSON en `TEXT` (colonnes `request_metadata_json`, `decision_metadata_json`)
- décision finale stockée dans la même table:
  - `decision_status`, `decision_decided_at`, `decision_actor_id`, `decision_channel`, `decision_comment`, `decision_metadata_json`

Repository:
- `findByRequestId(...)`
- `findByStatusOrderByCreatedAtAsc(...)`
- `updateDecisionIfPending(...)` via JPQL `update ... where requestId=:requestId and status=:pendingStatus`
- retour `int` = nombre de lignes modifiées

## 3. Implémentation du service

Fichier:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionService.java`

Comportement:
- implémente `HumanInterventionService` sans changer la signature des méthodes
- `@Service` + `@Primary` pour rendre cette implémentation active
- `open(...)`:
  - UUID + `Instant.now()`
  - statut initial `PENDING`
  - persist + notification `onRequestOpened`
- `findById(...)`:
  - lecture DB directe
- `findPending()`:
  - lecture DB `status=PENDING`, ordre ascendant `createdAt`
- `recordDecision(...)`:
  - update conditionnel `PENDING -> terminal`
  - si 0 ligne: `Optional.empty()`
  - sinon reload par `requestId` + notification `onDecisionRecorded`

Le comportement métier de l’implémentation in-memory est conservé:
- transition refusée si déjà terminal
- id inconnu => `Optional.empty()`
- notifications best-effort

## 4. Gestion metadata

Fichier:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntityMapper.java`

Règles implémentées:
- sérialisation JSON via `ObjectMapper`
- filtrage léger des clés sensibles (contains, case-insensitive):
  - `token`
  - `secret`
  - `password`
- taille max JSON: `4096` caractères
- si dépassement: fallback JSON minimal `{}`
- désérialisation robuste vers `Map<String,Object>` (fallback `Map.of()` en cas d’erreur)

## 5. Gestion concurrence

Mécanisme:
- `recordDecision(...)` repose sur `updateDecisionIfPending(...)` conditionné par `status=PENDING`.

Effets:
- première décision terminale gagne
- décisions concurrentes suivantes retournent `Optional.empty()` (0 ligne modifiée)
- pas de double transition

## 6. Tests ajoutés

Fichiers:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceRestartIT.java`

Couverture:
- unitaires:
  - `open` persiste et notifie
  - `findById` (dont id blank)
  - `findPending`
  - `recordDecision` succès
  - `recordDecision` ignoré si déjà traité/inconnu
- intégration Spring/SQLite:
  - open/findById/findPending sur DB réelle
  - transition `pending -> approved` + seconde décision ignorée
- restart:
  - contexte 1: open + decision
  - contexte 2 (même DB): relecture `findById` OK

Commandes exécutées:
- `./mvnw "-Dtest=PersistentHumanInterventionServiceTest,PersistentHumanInterventionServiceIT,PersistentHumanInterventionServiceRestartIT" test`
- `./mvnw "-Dtest=PersistentHumanInterventionServiceRestartIT" test`

Résultat:
- tests passants

## 7. Points d’attention

- L’ancienne implémentation in-memory est toujours présente mais la nouvelle est prioritaire (`@Primary`).
- `HumanInterventionRequest` n’expose toujours pas les champs de décision détaillés (choix conforme au contrat actuel).
- Le filtrage metadata est top-level uniquement (pas d’inspection récursive profonde).

## 8. Limites connues

- Pas d’historisation des transitions (seulement état courant + décision finale).
- Pas de stratégie de rétention/purge dédiée à cette table à ce stade.
- En cas de metadata très volumineuse, fallback `{}` (perte volontaire de détail pour respecter la contrainte de taille).
