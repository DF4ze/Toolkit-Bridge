# v1.1 / Phase 1 / Étape 4 / Lot 1 — Correction ciblée

## Correction appliquée

Objectif traité: fiabiliser la capture de logs dans les tests de non-divulgation.

Changement réalisé:
- remplacement de `output.getOut()` par `output.getAll()` dans les assertions de logs.

## Fichiers modifiés

- `src/test/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenServiceTest.java`
  - assertions logs mises à jour sur `output.getAll()`

- `src/test/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializerTest.java`
  - assertions logs mises à jour sur `output.getAll()`

## Validation réalisée

Commande exécutée:
- `./mvnw "-Dtest=AdminTokenServiceTest,AgentAccountInitializerTest" test`

Résultat:
- BUILD SUCCESS
- 6 tests exécutés, 0 échec

## Auto-review rapide

- Périmètre strict respecté: **oui**
  - aucun code métier modifié
  - aucune modification de `SensitiveDataMasker`
  - aucune évolution de la politique de masquage
  - aucun élargissement à d’autres logs

- Point discutable restant:
  - aucun nouveau point introduit par ce correctif.
  - la politique “suffixe visible” reste inchangée volontairement (conforme à la consigne).
