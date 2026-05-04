# Résultat — Phase 8 CodexCall Étape 1 — Registre projet persistant

Date: 2026-05-03  
Projet: `Toolkit-Bridge` / `CodexTime`

## Objectif atteint

Création de la base persistante et applicative d’un registre projet minimal stockant :

`projectName → projectPath`

Sans intégration Telegram et sans modification de :

- `/workflow_run`
- `/workflow_resume`
- `CodexWorkflowClient`

## Fichiers créés / modifiés

### Production

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectEntity.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRepository.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRegistryService.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectPathPolicy.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/PathOsFamily.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRegistrationResult.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectLookupResult.java`

### Tests

- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectPathPolicyTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRegistryServiceTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/project/WorkflowProjectRepositoryIT.java`

## Modèle DB ajouté

### `WorkflowProjectEntity`

- Table: `workflow_project`
- Colonnes:
  - `project_key` (unique, non null)
  - `project_name` (non null)
  - `project_path` (non null)
  - `created_at`, `updated_at` (via `@PrePersist/@PreUpdate`)
- Pas de `@Version` (conforme aux contraintes)

## Repository ajouté

`WorkflowProjectRepository` :

- `Optional<WorkflowProjectEntity> findByProjectKey(String projectKey)`
- `boolean existsByProjectKey(String projectKey)`

## Service registry ajouté

`WorkflowProjectRegistryService` :

- `registerOrUpdate(projectName, projectPath)` :
  - validation nom (trim, max 120, pas d’espaces, regex `[A-Za-z0-9._-]+`)
  - normalisation `projectKey = lowercase(Locale.ROOT)`
  - validation path (parse `Path`, `toAbsolutePath().normalize()`, existe, dossier, lisible, writable)
  - application policy “chemins dangereux”
  - upsert DB
  - retour `WorkflowProjectRegistrationResult` (success + created/updated + reason)
- `lookup(projectName)` :
  - normalisation nom
  - lookup DB
  - retour `WorkflowProjectLookupResult`

## Policy chemins dangereux

`WorkflowProjectPathPolicy` :

- API : `isDangerous(Path normalizedAbsolutePath, PathOsFamily osFamily, Path userHome)`
- Règles couvertes :
  - Windows : refus drive root + `C:\Windows` + `C:\Program Files*` + `C:\Users` + home direct (mais sous-dossiers home autorisés)
  - Unix : refus `/` + dossiers système (`/etc`, `/usr`, `/var`, etc.) + home direct (mais sous-dossiers home autorisés)
- Testable indépendamment de l’OS via `PathOsFamily`.

## Tests ajoutés

- Validation nom : null/blank, trim, lowercasing projectKey, espaces refusés, caractères invalides refusés.
- Policy paths (pure) : Windows/Unix roots, dossiers système, home direct refusé, sous-dossiers home autorisés.
- Service (`@TempDir`) : path inexistant refusé, fichier refusé, create puis update, lookup inconnu.
- Repository IT (SQLite) : save + lookup par `projectKey`.

## Validation des tests

- Tests exécutés : `./mvnw test`
- Résultat : OK

## Confirmations (contraintes)

- Aucun handler Telegram ajouté/modifié pour cette étape.
- Aucun changement sur `/workflow_run` et `/workflow_resume`.
- Aucun changement sur `CodexWorkflowClient`.
- Pas de WebUI.
- Pas de `@Version`.
- Aucun affichage UX de path absolu introduit (registre backend uniquement).

