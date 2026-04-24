# Revue critique architecture - Etape 3 (brique `runtime.codex`)

## Périmètre relu
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexExecutionRequest.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexExecutionResult.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexWorkflowClient.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexExecutionException.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexExecutionRequestTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexExecutionResultTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexWorkflowClientTest.java`

## Vérifications demandées

### 1) Séparation claire configuration/runtime
- Conforme.
- Aucune modification de propriétés/config Spring, aucune persistance, aucune couche admin/config.
- Le lot reste strictement runtime technique.

### 2) Qualité du modèle implémenté
- Bonne pour un socle minimal.
- `CodexExecutionRequest` et `CodexExecutionResult` sont sobres, lisibles, avec validations basiques utiles.
- `workingDirectory` est bien typé en `Path` conformément au cadrage.

### 3) Découplage orchestrator/mémoire/tooling/policy/workspace
- Conforme.
- Aucun couplage direct à orchestrator, memory, policy, tooling registry ou Telegram.
- Le client ne dépend pas de `WorkflowStep`.

### 4) Absence de logique ad hoc / trop spécifique
- Conforme globalement.
- Le client exécute un process, capte les flux, gère timeout/interruption, renvoie un résultat technique.
- Pas de décision métier ni de parsing fonctionnel.

### 5) Absence de couplage gênant pour les futures phases
- Globalement conforme.
- Contrat suffisamment neutre pour être consommé ensuite par une orchestration sans refactor transversal.

### 6) Cohérence des noms
- Cohérent et explicite (`CodexExecutionRequest`, `CodexExecutionResult`, `CodexWorkflowClient`, `CodexExecutionException`).

### 7) Lisibilité générale
- Bonne.
- Structure simple et responsabilité claire.

### 8) Tests réellement utiles
- Oui, pour le niveau de ce lot.
- Les tests couvrent les validations de construction et un garde-fou client sur entrée nulle.
- Vérification relancée: BUILD SUCCESS, 7 tests verts.

### 9) Dette technique introduite
- Dette faible, principalement de paramétrage et robustesse opérationnelle, pas de dette d'architecture majeure.

### 10) Risques de refactor futur évitables maintenant
- Deux risques légers et évitables sans élargir le périmètre:
  1. Construction de commande actuellement figée à `codex <prompt>` (pas de mode/flags configurables).
  2. `safeGet(..., 1s)` peut tronquer tardivement stdout/stderr sur certains environnements lents, surtout après timeout.

## 1. Faiblesses / points discutables
1. Commande codex figée (convention implicite sur la CLI réelle).
2. Tolérance limitée de récupération des flux (`safeGet` à 1 seconde).
3. Pas de validation explicite de l'existence/accessibilité du `workingDirectory` avant `start()` (la gestion se fait via exception système, ce qui est acceptable mais peu explicite).

## 2. Corrections utiles proposées (sans nouvelles fonctionnalités)
1. Introduire une constante locale pour le nom du binaire (`CODEX_BINARY = "codex"`) pour réduire l'ancrage en dur.
2. Remonter légèrement le délai de `safeGet` (ou le rendre cohérent avec timeout) pour limiter les sorties perdues au moment de la collecte.
3. Ajouter une validation optionnelle et sobre de `workingDirectory` (si fourni) pour message d'erreur plus clair avant lancement.

Aucune de ces corrections n'est bloquante pour la validation du lot; elles restent des micro-améliorations techniques.

## 3. Résumé final
Le lot est globalement propre et conforme au cadrage: brique technique Codex CLI isolée, sans logique workflow, sans couplage transversal indésirable et sans sur-conception. Les tests sont proportionnés et stables. La dette restante est faible et localisée (robustesse opérationnelle fine), sans impact sur la séparation d'architecture.
