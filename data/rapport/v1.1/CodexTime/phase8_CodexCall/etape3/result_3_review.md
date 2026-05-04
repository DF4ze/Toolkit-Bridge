# Relecture / Review — Phase 8 CodexCall Étape 3 — Résolution projet dans `/workflow_run` et `/workflow_resume`

Date: 2026-05-03  
Lot relu: `phase8_CodexCall/etape3` (ajout `projectPath` en session + lookup registry dans orchestration)

Note: `data/rapport/00.promptWorkflow/3.Relecture.md` demande un rapport review. Le template mentionne `phase6/...`, mais le lot concerné est **Phase 8 CodexCall Étape 3**, donc sortie dans `phase8_CodexCall/etape3/`.

## 1) Vérifications demandées (checklist)

### Séparation configuration vs runtime

- OK : pas de nouvelle configuration globale/YAML. La résolution projet est du runtime applicatif (lookup DB via service).
- Le registre projet reste une brique “données” (DB), et l’orchestration Telegram l’utilise au runtime.

### Qualité du modèle implémenté

- OK : ajout `projectPath: Path` dans `WorkflowTelegramSession` est minimal, optionnel, et aligné avec l’objectif futur (`codexWorkingDirectory`).
- OK : `WorkflowTelegramSessionStore.updateContext(...)` évite l’incohérence `projectName A` + `projectPath B` via un clear lors du changement de projet (si aucun nouveau path fourni).

### Découplage orchestrator / mémoire / tooling / policy / workspace

- Globalement OK : `WorkflowTelegramRunTargetResolver` reste pur (aucune dépendance DB), la lookup DB est en orchestration.
- Couplage ajouté volontairement : `WorkflowTelegramOrchestrationService` dépend maintenant de `WorkflowProjectRegistryService`. C’est cohérent fonctionnellement (c’est bien là que doit vivre la résolution).

### Absence de logique ad hoc / trop spécifique

- Point discutable : détection “missing projectName” via matching de texte d’erreur (`err.toLowerCase().contains(...)`) dans `startRun(...)`. C’est un contournement fragile (voir faiblesses).
- Le reste (Cas A/B/C/D) reste simple et conforme au prompt.

### Absence de couplage gênant pour les futures phases

- OK : aucune injection `codexWorkingDirectory` (explicitement reportée à l’étape 4).
- OK : `CodexWorkflowClient` inchangé.
- Le stockage `projectPath` en session prépare l’étape 4 sans imposer de format UX.

### Cohérence des noms

- OK : `projectPath` cohérent avec `projectName`, `roadmapPath`, `lastSummaryPath`.
- Messages et constantes : cohérents avec `WorkflowTelegramMessageRenderer` (format standard).

### Lisibilité générale

- Lisible, mais `WorkflowTelegramOrchestrationService` grossit et mélange :
  - validation de target (resolver)
  - résolution projet (lookup)
  - gestion du run async
  - mapping de statut
  - sanitization des erreurs

Ce n’est pas nouveau, mais l’étape 3 ajoute un peu de logique supplémentaire ; à surveiller.

### Tests réellement utiles

- OK : les tests couvrent les cas introduits (unknown project, no project configured, resume unknown project).
- OK : non-régression sur l’existant après extension de la session/store.
- OK : tests non-fuite restent valides (pas d’ajout de `projectPath` dans le renderer).

### Dette technique introduite

- Dette mineure : string-matching sur message d’erreur resolver (fragile).
- Dette mineure : signature `updateContext(...)` étendue impacte beaucoup d’appels/tests (verbeux).

### Risques de refactor futur encore évitables maintenant

- La logique “résolution projet + erreurs” dans `WorkflowTelegramOrchestrationService` peut devenir un point chaud si d’autres résolutions (agent, provider, repo, etc.) s’ajoutent. On peut encore éviter une dette lourde en isolant un petit helper interne (sans changer le comportement).

## 2) Faiblesses / points discutables

1. **Détection fragile des erreurs “missing projectName”**  
   `startRun(...)` détecte le cas via `contains("missing projectname")` / `contains("projectname is required")`.  
   Risque : changement futur du texte côté resolver = régression silencieuse.

2. **Règle Cas D / “No project configured” partiellement dépendante du resolver**  
   Le cas “aucun projet exploitable” peut arriver à deux moments :
   - target invalide (resolver) → on fait un mapping ad hoc,
   - target valide mais lookup path impossible → on renvoie `NO_PROJECT_MESSAGE`.
   Ce n’est pas faux, mais cela introduit une logique de fallback un peu complexe à maintenir.

3. **Constructeurs multiples et injection partielle**  
   `WorkflowTelegramOrchestrationService` a plusieurs constructeurs “test” qui passent `projectRegistryService=null`.  
   C’est OK, mais nécessite une vigilance dans les tests : un oubli de registry provoque des erreurs inattendues (“Unknown project”) au lieu d’exécuter le runner.

4. **Signature `updateContext(...)` devenue lourde**  
   L’ajout de `projectPath` rend les calls plus verbeux et augmente le risque d’erreur de paramètre (ordre des arguments).  
   Ce n’est pas bloquant, mais c’est un vecteur de bugs simples.

## 3) Corrections utiles proposées (sans nouvelles fonctionnalités)

1. **Remplacer le string-matching par un signal structuré (minimal)**  
   Sans refondre le resolver ni ajouter de DB dedans, une correction minimale serait :
   - enrichir `WorkflowTelegramRunTarget` avec un “errorCode” (ex: `MISSING_PROJECT`, `MISSING_PHASE`, …) **ou**
   - ajouter un booléen `missingProjectName` lors de `WorkflowTelegramRunTarget.error(...)` dans les cas concernés.
   Cela supprime la dépendance au texte libre.

2. **Ajouter un overload `updateContext(...)` pour compat interne**  
   Conserver la signature actuelle, mais ajouter un overload :
   - `updateContext(chatId, userId, projectName, phase, etape, roadmapPath)` qui délègue à la version complète avec `projectPath=null`.  
   Objectif : réduire la verbosité et éviter erreurs d’ordre, sans changer le comportement.

3. **Isoler la résolution projet dans un helper privé**  
   La logique existe déjà via `resolveProjectPathForRun(...)` / `resolveProjectPathForResume(...)`.  
   On peut améliorer la lisibilité en centralisant la production de message d’erreur (unknown/no project/path missing) dans une méthode unique, sans changer les règles.

## 4) Résumé final

Étape 3 est globalement conforme et propre :

- `projectPath` est stocké en session sans fuite dans l’UX Telegram,
- `/workflow_run` et `/workflow_resume` consultent le registre projet au bon endroit (orchestration),
- le resolver logique reste pur,
- aucune modification de `CodexWorkflowClient` ni injection `codexWorkingDirectory`,
- tests passent et couvrent les nouveaux cas.

Les améliorations recommandées restent mineures et dans le périmètre qualité :

- supprimer la dépendance fragile au texte d’erreur du resolver,
- réduire la verbosité et le risque d’erreur via overload `updateContext(...)`.

