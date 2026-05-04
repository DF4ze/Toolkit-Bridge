# Résultat — Correction Phase 8 CodexCall Étape 1 — Stabilisation registre projet

Date: 2026-05-03  
Projet: `Toolkit-Bridge` / `CodexTime`

## Objectif

Stabiliser le registre projet (qualité + alignement spec) sans ajouter de fonctionnalité :

- pas d’intégration Telegram
- pas de modification `/workflow_run` / `/workflow_resume`
- pas de modification `CodexWorkflowClient`
- pas de changement du modèle DB

## Fichiers modifiés

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectPathPolicy.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRegistryService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectPathPolicyTest.java`

## Corrections appliquées

### 1) Injection Spring de `WorkflowProjectPathPolicy`

- `WorkflowProjectPathPolicy` est maintenant un bean Spring (`@Component`).
- `WorkflowProjectRegistryService` injecte `WorkflowProjectPathPolicy` via constructeur `@Autowired` (plus de `new WorkflowProjectPathPolicy()` côté service).

### 2) Import inutile supprimé

- Suppression de l’import inutile `java.nio.file.InvalidPathException` dans `WorkflowProjectRegistryService`.

### 3) Policy Windows renforcée (non centrée sur `C:\\`)

Avant : refus basé sur des chemins hardcodés `C:\\Windows`, `C:\\Program Files...`, `C:\\Users`.  
Maintenant : refus basé sur le **premier segment après le drive** (case-insensitive) :

- `Windows` → refus du sous-arbre
- `Program Files` → refus du sous-arbre
- `Program Files (x86)` → refus du sous-arbre
- `Users` → refus de `X:\\Users` (dossier racine users), tout en conservant :
  - refus du home direct utilisateur (`user.home`)
  - autorisation des sous-dossiers du home (`C:\\Users\\<user>\\...`)

Le refus des racines disque (`C:\\`, `D:\\`, etc.) est conservé.

## Tests ajoutés / adaptés

- Ajout de cas Windows sur drive `D:\\` dans `WorkflowProjectPathPolicyTest` :
  - `D:\\Windows` refusé
  - `D:\\Program Files` refusé
  - `D:\\Program Files (x86)` refusé
  - `D:\\Users` refusé
- Les cas existants sur le home direct vs sous-dossiers home restent couverts.

## Validation des tests

- Tests exécutés : `./mvnw test`
- Résultat : OK

## Confirmation périmètre

- Aucun changement hors du registre projet (pas de Telegram, pas de workflow, pas de Codex CLI).

