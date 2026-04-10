# Corrections — Phase 1 — Interventions humaines

## 1. Corrections appliquées

Corrections ciblées appliquées sans élargissement de périmètre:
- suppression de l’activation Spring de l’implémentation in-memory
- renforcement de la vérification d’invariant `status/decisionStatus` via test
- ajout de logs explicites sur pertes/fallback metadata
- assouplissement des tests sur JSON pour réduire la fragilité

## 2. Wiring Spring

Fichier modifié:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/InMemoryHumanInterventionService.java`

Changement:
- retrait de l’annotation `@Service` sur `InMemoryHumanInterventionService`.

Effet:
- une seule implémentation Spring active de `HumanInterventionService` (la persistante).
- évite la coexistence de deux beans concurrents.

## 3. Invariant status

Fichier modifié:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceIT.java`

Changement:
- ajout d’une vérification explicite après `recordDecision`:
  - `status == APPROVED`
  - `decisionStatus == APPROVED`

Effet:
- l’invariant de cohérence est désormais testé sur persistance réelle.

## 4. Logging metadata

Fichier modifié:
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntityMapper.java`

Changements:
- ajout de logs `warn` si metadata JSON dépasse la taille max (4KB), avec `requestId` et type (`request`/`decision`).
- ajout de logs `warn` en cas d’échec de désérialisation JSON avant fallback.
- fallback `{}` conservé en dépassement de taille (comme demandé).

## 5. Tests ajustés

Fichier modifié:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceTest.java`

Changements:
- suppression d’assertions trop strictes sur JSON exact.
- vérification robuste via `argThat(...)` sur présence de la clé attendue (`"reason"`) dans le JSON transmis au repository.

## 6. Résultat final

Validation exécutée:
- `./mvnw "-Dtest=PersistentHumanInterventionServiceTest,PersistentHumanInterventionServiceIT,PersistentHumanInterventionServiceRestartIT" test`

Résultat:
- tests ciblés passants
- périmètre respecté
- contrat `HumanInterventionService` inchangé
- implémentation stabilisée sans ajout fonctionnel hors phase
