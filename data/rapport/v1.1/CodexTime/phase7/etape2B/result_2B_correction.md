# Phase 7 — Etape 2B — Correction — Ajustements service roadmap

## Corrections appliquees

1. Gestion d'erreur homogene
   * `WorkflowTelegramRoadmapService.loadRoadmap(...)` ne jette pas d'exception pour une requete invalide.
   * Retourne maintenant toujours une string user-friendly (ex: `❌ Invalid request`).

2. Roadmap "vide"
   * La validation ne se limite pas a `size > 0`.
   * Ajout d'une verification "whitespace only" (lecture limitee) sans parsing roadmap.

3. Normalisation des messages
   * Remplacement des chaines "mojibake" (`âŒ`, `âœ…`) par les symboles attendus (`❌`, `✅`) dans le service et les tests.

## Fichiers modifies

* `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRoadmapService.java`
* `src/test/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramRoadmapServiceTest.java`

## Validation

* `./mvnw test` OK.

## Confirmation perimetre

* Pas de parsing roadmap (contenu non charge, non stocke).
* Aucun lancement de workflow.
* Aucun appel runner / CLI.

