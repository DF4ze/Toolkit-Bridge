# Result — Correction Phase 7 Étape 7 — UX summary trop long

## Fichiers modifiés

- `data/rapport/v1.1/CodexTime/Phase7/etape7/7.correction.md`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryServiceTest.java`

## Correction appliquée

### Cas “summary trop long + document envoyé”

Dans `WorkflowTelegramSummaryService` :
- si `sendDocument(...)` réussit, la commande retourne désormais un **succès** via `WorkflowTelegramMessageRenderer.successMessage(...)`.

Message attendu :
- `Action effectuee`
- `Detail: Summary too long. Complete file sent.`
- `Next: Open the attached document.`

### Cas “document non envoyé”

- si aucun sender n’est disponible ou si `sendDocument(...)` lève une exception :
  - `Action impossible`
  - `Reason: Summary too long. Complete file could not be sent.`
  - `Try: Check the summary file directly.`

## Tests

- Mise à jour du test “too long + sender OK” → attend un succès.
- Ajout d’un test “too long + sendDocument throws” → attend une erreur.

Validation :
- `./mvnw -q test` ✅

## Confirmations

- Aucune modification du runner/CLI/module Telegram.
- Aucune modification de la sanitization globale.
- Stratégie inchangée : `2 chunks max puis document`.
- Aucune logique workflow modifiée (UX texte uniquement).
