# Analyse — Phase 8 CodexCall Étape 1 — Registre projet persistant (`projectName → projectPath`)

Date: 2026-05-03  
Projet: `Toolkit-Bridge` / `CodexTime`  
Référence amont: `data/rapport/v1.1/CodexTime/phase8_CodexCall/project_path_codex_cli_analysis.md`

## 1) Résumé exécutif

Le workflow CodexTime supporte déjà un `workingDirectory` via la variable `codexWorkingDirectory`, mais l’intégration Telegram ne sait pas résoudre un `projectName` en chemin disque stable. L’objectif de cette étape est de cadrer un **registre projet persistant** minimal, stocké en DB, pour porter le mapping `projectName → projectPath`.

Recommandation : créer un petit module persistant JPA (SQLite) composé de :

- une entité dédiée (ex: `CodexProjectEntity`),
- un repository simple (lookup par nom normalisé),
- un service `WorkflowProjectRegistryService` (upsert + validation du nom et du path + policy “chemins dangereux”).

Conclusion : **prêt pour implémentation**, en restant strictement sur DB + validation + résultats métier (pas de Telegram dans cette étape).

## 2) Conventions DB existantes (observées)

### 2.1 Technologie et configuration

- DB de référence : SQLite (`jdbc:sqlite:./data/toolkit-bridge.db`).
- Hibernate / JPA : dialect SQLite et `spring.jpa.hibernate.ddl-auto=update` (pas de Flyway/Liquibase détecté).

### 2.2 Conventions de modélisation JPA

Exemples observés :

- `@Table(name=..., indexes=...)` utilisé avec `@Index` pour les contraintes/performances.
- `@Column(name=..., length=..., nullable=..., unique=...)` fréquent.
- Champs `created_at` / `updated_at` :
  - parfois présents avec `@PrePersist` / `@PreUpdate` (ex: `AdministrableConfigurationEntity`, `ScriptedToolMetadata`, `MemoryEntry`, `RuleEntry`, etc.),
  - parfois absents (ex: `AdminTaskSnapshotEntity`).
- `@Version` utilisé ponctuellement (ex: `AdministrableConfigurationEntity`).

Implication : le registre projet peut adopter le style “createdAt/updatedAt + prePersist/preUpdate”, et ajouter une contrainte unique sur le nom.

### 2.3 Repositories

Pattern observé :

- interfaces `extends JpaRepository<..., ...>`
- méthodes de lookup simples (ex: `findByConfigKey(...)`).

### 2.4 Tests persistence

Pattern IT observé :

- tests `@SpringBootTest` ciblant SQLite via propriété :
  - `spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db`
- tests `@Transactional` avec `@Autowired` repository (ex: `RuleEntryRepositoryIT`).

Implication : pour le registre projet, on pourra suivre la même approche pour tester la contrainte unique + upsert réel.

### 2.5 Où placer le registre projet

Deux options cohérentes avec l’organisation actuelle :

1. **Scope “workflow runtime”** : `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project`
2. **Scope “Telegram workflow” (mais persistant/générique)** : `fr.ses10doigts.toolkitbridge.service.telegram.workflow.project`

Recommandation : **workflow runtime** (option 1) pour éviter un couplage Telegram, tout en restant utilisable par Telegram via injection Spring.

## 3) Modèle de données recommandé

### 3.1 Nom de l’entité

Recommandé : `CodexProjectEntity` (ne lie pas au transport Telegram).

Alternative : `WorkflowProjectEntity`.

### 3.2 Champs

Champs minimaux :

- `id: Long` (`@GeneratedValue(strategy = IDENTITY)`)
- `projectKey: String` (clé normalisée, unique)
- `projectName: String` (nom “display” optionnel, peut être égal à `projectKey` si on veut rester minimal)
- `projectPath: String` (chemin absolu normalisé)
- `createdAt: Instant`
- `updatedAt: Instant`

Remarques :

- Le prompt demande `projectName` + `projectPath` uniquement. Si on doit rester strict, on peut omettre `projectName` display et **stocker le nom déjà normalisé dans `projectName`**.  
  - Avantage : schéma minimal.  
  - Inconvénient : UX affiche un nom potentiellement lower-case.
- Si on veut gérer proprement la casse/doublons sans dépendre des collations SQLite/JPA : ajouter `projectKey` est le meilleur compromis.

### 3.3 Contraintes / index

- unique index sur `projectKey` (ou `projectName` si pas de `projectKey`).
- index sur `updated_at` optionnel (pas nécessaire au début).

### 3.4 Normalisation du nom (casse)

Recommandation : **case-insensitive** via normalisation en service :

- `projectKey = trim(projectName).toLowerCase(Locale.ROOT)`

Ainsi, `ToolkitBridge`, `toolkitbridge` deviennent le même projet (évite doublons).

### 3.5 Stockage du path

Recommandation : stocker **le chemin absolu normalisé** en String :

- `normalized = Path.of(input).toAbsolutePath().normalize()`
- `projectPath = normalized.toString()`

### 3.6 `@Version` ?

Optionnel. Recommandation :

- **ne pas ajouter `@Version` dans un premier temps** : on n’a pas de concurrence forte attendue, et l’upsert est simple.
- à reconsidérer si plusieurs admins écrivent en parallèle ou si WebUI admin apparaît.

## 4) Repository recommandé

Interface type :

- `Optional<CodexProjectEntity> findByProjectKey(String projectKey);`
- (optionnel) `boolean existsByProjectKey(String projectKey);`

Si pas de `projectKey` :

- `findByProjectName(String projectName)` sur nom déjà normalisé.

## 5) Service registry recommandé

Nom proposé : `WorkflowProjectRegistryService` (service applicatif).

Responsabilités :

- `registerOrUpdate(String projectName, String projectPath)` :
  - normalise le nom,
  - valide le path (existence + dossier + read/write),
  - applique la policy “chemins dangereux”,
  - fait upsert DB,
  - retourne un résultat métier (créé vs mis à jour + raison si refus).
- `lookup(String projectName)` :
  - normalise le nom,
  - lookup DB,
  - retourne un résultat métier (found + path normalisé).

Important : dans cette étape, **ne pas lier à Telegram** (pas de dépendance au `TelegramUpdateContext`).

## 6) Règles de validation — `projectName`

Recommandation (simple et stable pour Telegram) :

- `null` / blank → invalide
- `trim()`
- longueur max : 80–120 (aligner avec d’autres colonnes de type “key”, ex `config_key` length 120)
- caractères autorisés (optionnel mais utile) :
  - `[A-Za-z0-9._-]` + éventuellement espaces (mais espaces compliquent Telegram)

Recommandation pragmatique : **interdire espaces** dans le nom projet pour éviter une UX fragile côté Telegram :

- `ToolkitBridge`, `toolkit-bridge`, `toolkit_bridge`

Normalisation :

- `projectKey = trimmed.toLowerCase(Locale.ROOT)`

## 7) Règles de validation — `projectPath` (sémantique)

Validation “filesystem” (à faire dans le service) :

1. non null / non blank
2. parse OS (`Path.of(...)`) → capturer `InvalidPathException` (ou `IllegalArgumentException`)
3. `toAbsolutePath().normalize()`
4. `Files.exists(path)`
5. `Files.isDirectory(path)`
6. `Files.isReadable(path)`
7. `Files.isWritable(path)` (recommandé si Codex doit modifier le repo)

Note : si plus tard Codex est exécuté en `--sandbox workspace-write`, écrire dans le repo doit être autorisé ; donc “writable” est cohérent.

## 8) Chemins dangereux à refuser (policy minimaliste)

Objectif : refuser les “racines” et les dossiers système évidents, sans moteur complexe.

### 8.1 Principe

Sur le path normalisé :

- Refuser si c’est une **racine** (pas de parent significatif).
- Refuser si le path est égal (ou est inclus dans) une liste de **forbidden roots**.
- Refuser si le path est exactement `user.home` (home direct), mais autoriser les sous-dossiers.

### 8.2 Liste Windows (recommandée)

Refuser si path == (case-insensitive) :

- racines : `X:\` (tout drive root)
- `C:\Windows`
- `C:\Program Files`
- `C:\Program Files (x86)`
- `C:\Users`

Pour le “home direct” :

- refuser `C:\Users\<user>` (valeur `System.getProperty("user.home")`)
- autoriser les sous-dossiers (ex: `...\Documents\Spring\Toolkit-Bridge`)

### 8.3 Liste Linux (recommandée)

Refuser :

- `/`
- `/bin`, `/boot`, `/dev`, `/etc`, `/lib`, `/lib64`, `/proc`, `/root`, `/sbin`, `/sys`, `/usr`, `/var`
- home direct (`System.getProperty("user.home")`) mais pas ses sous-dossiers.

### 8.4 Testabilité (sans dépendre de l’OS)

Pour éviter des tests fragiles selon l’OS :

- isoler la policy dans une méthode **pure** basée sur `String normalizedPath` (ou `Path` + `String osName` injecté),
- tester les cas Windows/Linux via des inputs textuels et un mode explicite (ex: enum `PathOsFamily { WINDOWS, UNIX }`).

Et séparer :

- la validation “dangereux” (pure),
- de la validation “Files.exists/isDirectory” (I/O, testée avec `@TempDir`).

## 9) Upsert : comportement attendu

Règle :

- si `projectKey` n’existe pas → création
- si `projectKey` existe → update `projectPath`
- `createdAt` conservé
- `updatedAt` mis à jour

Implementation côté service (plus tard) :

- `repo.findByProjectKey(key)`
- `save(entity)` (update ou create)

## 10) Résultats métier (service)

Objectif : ne pas exposer d’exceptions brutes aux couches appelantes (Telegram plus tard).

Recommandation : deux records/DTO simples :

- `ProjectRegistrationResult` :
  - `boolean success`
  - `boolean created` (sinon updated)
  - `String projectKey`
  - `Path normalizedProjectPath` (ou String)
  - `String reason` (si !success)
- `ProjectLookupResult` :
  - `boolean found`
  - `String projectKey`
  - `Path normalizedProjectPath` (si found)
  - `String reason` (si !found)

Les exceptions restent réservées à des erreurs de programmation (null impossible, etc.), pas à des erreurs utilisateur.

## 11) Tests recommandés

### 11.1 Tests unitaires (purs)

- validation `projectName` :
  - null/blank refusé
  - trim appliqué
  - normalisation lower-case (`projectKey`)
  - longueur max
- policy “dangerous paths” (pure) :
  - Windows drive root refusé
  - Windows system dirs refusés
  - Unix root + system dirs refusés
  - home direct refusé, sous-dossier autorisé

Ces tests doivent être indépendants de l’OS en utilisant une policy paramétrée (Windows vs Unix).

### 11.2 Tests unitaires (filesystem)

Avec `@TempDir` :

- path inexistant refusé
- path fichier refusé
- path répertoire accepté (readable/writable)

### 11.3 Tests d’intégration repository

Pattern recommandé identique à `RuleEntryRepositoryIT` :

- `@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db")`
- `@Transactional`
- vérification contrainte unique sur `projectKey`
- vérification upsert réel (create puis update)

## 12) Risques identifiés

- Casse/doublons `projectName` si on ne normalise pas (ex: `ToolkitBridge` vs `toolkitbridge`).
- Validation “dangereux” trop stricte (bloque un vrai repo), ou trop permissive (permet `C:\Users` ou `/`).
- Tests OS fragiles si la policy n’est pas paramétrée (Windows vs Unix).
- Risque futur de fuite de path absolu dans Telegram : le registre doit rester **backend-only**, et les renderers Telegram doivent afficher “configured/missing”.

## 13) Plan d’implémentation (5–8 étapes)

1. Créer l’entité `CodexProjectEntity` (table + unique index sur `projectKey`).
2. Créer `CodexProjectRepository` (lookup by `projectKey`).
3. Implémenter la policy pure “dangerous path” (Windows/Unix) + tests unitaires.
4. Implémenter `WorkflowProjectRegistryService` (register/lookup) + tests unitaires `@TempDir`.
5. Ajouter un test IT repository/service sur SQLite (pattern `RuleEntryRepositoryIT`).
6. Vérifier que le service ne loggue ni n’expose de secrets/path en clair (contrat : résultats métiers).

## 14) Conclusion

Statut : **prêt pour implémentation**.

Le projet a déjà les patterns JPA/SQLite et un style de tests IT adapté. La complexité principale est la validation “chemins dangereux” : elle doit rester simple, paramétrable, et testée de façon OS-indépendante.

