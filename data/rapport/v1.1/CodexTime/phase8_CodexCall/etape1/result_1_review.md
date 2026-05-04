# Relecture / Review — Phase 8 CodexCall Étape 1 — Registre projet persistant

Date: 2026-05-03  
Lot relu: `phase8_CodexCall/etape1` (registre projet persistant)

Note: le template `data/rapport/00.promptWorkflow/3.Relecture.md` mentionne une sortie `phase6/result.[num]_review.md`, mais la relecture concerne ici le lot **en cours** (Phase 8 CodexCall Étape 1). Le rapport est donc produit dans `phase8_CodexCall/etape1/`.

## 1) Vérifications demandées (checklist)

### Séparation configuration vs runtime

- Le registre est un service applicatif (`WorkflowProjectRegistryService`) + persistance JPA + policy : c’est du runtime applicatif, pas de la config.
- Pas d’ajout de propriétés Spring ou de configuration globale : OK.
- Point discutable : `WorkflowProjectRegistryService` lit `System.getProperty("os.name")` et `user.home`. C’est acceptable comme **donnée runtime**, mais à documenter comme dépendance d’environnement.

### Qualité du modèle implémenté

- Entité `WorkflowProjectEntity` simple et cohérente :
  - `projectKey` unique (bonne décision pour éviter doublons de casse),
  - `projectName` conservé (affichable),
  - `projectPath` string (persistable),
  - timestamps `createdAt/updatedAt` via `@PrePersist/@PreUpdate` (cohérent avec les conventions).
- Pas de `@Version` : conforme à la contrainte.

### Découplage (orchestrator / mémoire / tooling / policy / workspace)

- Le registre est isolé dans `service.agent.workflow.runtime.project` :
  - pas de dépendance Telegram,
  - pas de dépendance runner/orchestrator,
  - pas de dépendance workspace tooling (pas de `WorkspaceLayout`).
- La policy (`WorkflowProjectPathPolicy`) est séparée des accès FS (`Files.*`) : bon découplage.

### Absence de logique ad hoc / trop spécifique

- La validation du nom et la policy “dangerous path” sont minimalistes et testables : OK.
- Point discutable : la policy Windows ne bloque explicitement que des sous-arbres `C:\...` (Windows) ; voir section faiblesses.

### Couplages gênants pour phases futures

- Pas de couplage Telegram : OK.
- Couplage implicite à “Codex va écrire dans le dossier” via `Files.isWritable` : c’est cohérent avec l’objectif futur, mais pourrait être rendu configurable si besoin (pas demandé pour cette étape).

### Cohérence des noms

- Noms globalement clairs : `WorkflowProjectEntity`, `WorkflowProjectRepository`, `WorkflowProjectRegistryService`.
- `PathOsFamily` est explicite.
- `WorkflowProjectPathPolicy` : OK.

### Lisibilité générale

- Code court et lisible, sans abstraction inutile.
- Résultats métier sous forme de `record` simples : OK.

### Tests réellement utiles

- Tests unitaires de policy (Windows/Unix) : utiles et OS-indépendants (via `PathOsFamily`).
- Tests unitaires du service (`@TempDir`, mocks repository) : utiles.
- Test IT repository (SQLite) : utile pour vérifier l’intégration JPA.

### Dette technique introduite / risques de refactor

- Quelques points mineurs à corriger maintenant pour éviter une dette triviale (imports, injection Spring, policy Windows trop “C:\”-centrée).

## 2) Faiblesses / points discutables

### 2.1 Injection Spring de la policy

`WorkflowProjectRegistryService` instancie `new WorkflowProjectPathPolicy()` dans le constructeur `@Autowired`.  
Ce n’est pas bloquant, mais :

- empêche de surcharger facilement la policy via Spring (test, config future),
- rend la policy moins “composable” si on veut un jour ajouter une variante (ex: allowlist root).

### 2.2 Import inutile et micro-bruit

`WorkflowProjectRegistryService` importe `java.nio.file.InvalidPathException` mais ne l’utilise pas (après correction compilation). Petit bruit.

### 2.3 Policy Windows incomplète par rapport à la spec

La spec demandait notamment :

- refus des racines disque `C:\`, `D:\`, etc. (OK via `isDriveRoot`)
- refus de `C:\Windows`, `C:\Program Files*`, `C:\Users` (OK)
- mais aussi, de manière plus générale, refuser les “dossiers système” indépendamment du drive, et refuser le “home direct”.

Dans `WorkflowProjectPathPolicy`, les chemins Windows interdits sont hardcodés **uniquement sous `C:\...`**.  
Sur une machine où le système ou les users sont ailleurs (ou si le chemin enregistré est `D:\Users\...`), la policy peut laisser passer des chemins considérés sensibles.

### 2.4 Mélange “policy” vs “environnement courant”

La policy est OS-paramétrable, mais le service décide `osFamily` en runtime via `os.name`.  
Ce n’est pas un bug, mais ça rend certains tests “end-to-end” plus difficiles à paramétrer si on veut simuler un autre OS.

### 2.5 Validation “writable” potentiellement trop stricte (à confirmer)

`Files.isWritable` est cohérent si Codex doit modifier le projet.  
Mais dans certains contextes (repo read-only, permissions), cela peut bloquer un usage “read-only”.  
Ce n’est pas hors-scope, mais à garder en tête : si un mode futur “read-only” apparaît, il faudra rendre cette règle optionnelle.

## 3) Corrections utiles proposées (sans nouvelles fonctionnalités)

### 3.1 Nettoyage et qualité

- Supprimer l’import `InvalidPathException` inutilisé dans `WorkflowProjectRegistryService`.
- Ordonner les imports Spring (style).

### 3.2 Découplage policy (amélioration faible risque)

Sans ajouter de feature :

- Déclarer `WorkflowProjectPathPolicy` en bean Spring (`@Component`) et l’injecter.
  - Cela ne change pas le comportement, uniquement la façon de construire l’objet.
  - Conserve les tests unitaires.

### 3.3 Renforcer la policy Windows sans complexifier

Toujours sans nouvelle feature (uniquement alignement spec) :

- Étendre la policy Windows pour refuser :
  - `X:\Windows`, `X:\Program Files`, `X:\Program Files (x86)`, `X:\Users` pour **tout drive root** détectable, ou
  - plus simplement : refuser `\Windows`, `\Program Files` etc **si ce sont les premiers segments** du path (après le drive).

L’objectif est de ne pas être “C:\”-centré.

### 3.4 Tests complémentaires (si correction policy)

Si on renforce la policy Windows pour d’autres drives :

- ajouter un test “`D:\Users` refusé” et “`D:\Windows` refusé” (policy pure).

## 4) Résumé final

Le lot est **globalement propre** : modèle simple, séparation claire (entité/repo/service/policy), pas de couplage Telegram/runner, tests pertinents.

Les points à corriger sont mineurs mais utiles :

- injection de la policy (qualité/maintenabilité),
- import inutile,
- policy Windows à rendre moins dépendante de `C:\`.

Ces corrections restent dans le périmètre (pas de nouvelle fonctionnalité) et réduisent une dette triviale avant les étapes suivantes (commande Telegram et injection `codexWorkingDirectory`).

