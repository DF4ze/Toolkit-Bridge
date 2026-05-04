# Rapport d'implémentation — Phase 9 Logging Étape 3 — Logs process, validation bas niveau et registre projet

---

## Résumé

Les 3 classes ciblées ont été instrumentées. `CodexWorkflowClient` possédait déjà `@Slf4j` et un log DEBUG partiel — il a été enrichi et complété. `WorkflowValidationService` et `WorkflowProjectRegistryService` ont reçu `@Slf4j` et une instrumentation complète. Aucune donnée sensible n'est exposée. Les tests ne peuvent pas être lancés depuis le sandbox (Maven non disponible sans réseau), mais la vérification de cohérence est réalisée manuellement.

---

## Fichiers modifiés

| Fichier | Action |
|---------|--------|
| `runtime/codex/CodexWorkflowClient.java` | Enrichissement logs existants + INFO/WARN/DEBUG/ERROR ajoutés |
| `runtime/validation/WorkflowValidationService.java` | Ajout @Slf4j + logs DEBUG/WARN/ERROR |
| `runtime/project/WorkflowProjectRegistryService.java` | Ajout @Slf4j + logs INFO/WARN/DEBUG |

---

## Classes annotées avec `@Slf4j`

`CodexWorkflowClient` possédait déjà `@Slf4j` — conservé sans modification.

`@Slf4j` ajouté sur les deux autres classes :

```java
// WorkflowValidationService
import lombok.extern.slf4j.Slf4j;
// ...
@Slf4j
public class WorkflowValidationService {
```

```java
// WorkflowProjectRegistryService
import lombok.extern.slf4j.Slf4j;
// ...
@Service
@Slf4j
public class WorkflowProjectRegistryService {
```

---

## 1. `CodexWorkflowClient`

### Logs INFO ajoutés

Avant `processBuilder.start()` — workingDirectory loggé `set` ou `none`, jamais le path absolu :

```java
log.info("Codex CLI process starting: workingDirectory={}",
        request.workingDirectory() != null ? "set" : "none");
```

### Logs DEBUG ajoutés / enrichis

Commande construite (sans prompt — le prompt est transmis via stdin, pas dans les args CLI) :

```java
log.debug("Codex CLI command built: command={}", String.join(" ", command));
```

Résultat de l'exécution — log DEBUG existant enrichi avec stdoutLen et stderrLen (l'ancienne version ne loggait que exitCode et durationMs) :

```java
// Avant (supprimé) :
// log.debug("Codex CLI execution finished with exit code {} in {}ms", exitCode, durationMs);
// TODO Transformer les ms en hh:mm:ss

// Après :
log.debug("Codex CLI execution finished: exitCode={}, durationMs={}, stdoutLen={}, stderrLen={}",
        exitCode, durationMs, stdout.length(), stderr.length());
```

### Logs WARN ajoutés

Timeout Codex — ajouté à l'entrée du bloc `!finished`, avant `process.destroy()` :

```java
log.warn("Codex CLI process timed out: durationMs={}", durationMs);
```

### Logs ERROR conservés et complétés

Les deux logs ERROR existants sont conservés :

```java
log.error("Failed to start Codex CLI process", e);   // IOException
log.error("Codex CLI execution interrupted", e);       // InterruptedException
```

Ajout d'un log ERROR dans `writePromptToStdin` pour les erreurs I/O stdin, avant `process.destroy()` et le throw :

```java
log.error("Codex CLI failed to write prompt to stdin", e);
```

Note : le message est statique — aucune valeur du prompt n'est loggée.

---

## 2. `WorkflowValidationService`

### Logs DEBUG ajoutés

Démarrage de la validation — commande Maven (pas de secret) et timeout en secondes :

```java
log.debug("Process validation starting: command={}, timeoutSeconds={}", commandString, timeoutSeconds);
```

Fin de validation (chemin SUCCESS/FAILURE uniquement — timeout et erreurs ont leur propre log) :

```java
log.debug("Process validation finished: status={}, exitCode={}, durationMs={}, stdoutLen={}, stderrLen={}",
        result.status(), result.exitCode(), result.durationMs(),
        result.stdout().length(), result.stderr().length());
```

Pour ce debug de fin, la méthode a été légèrement restructurée : les `safeGet(stdoutFuture)` / `safeGet(stderrFuture)` sont maintenant capturés dans des variables locales `stdout` et `stderr` pour permettre le log avant le `return`. Le comportement est identique.

### Logs WARN ajoutés

Timeout process — ajouté à l'entrée du bloc `!finished`, avant `process.destroy()` :

```java
log.warn("Process validation timed out: command={}, durationMs={}", commandString, durationMs);
```

### Logs ERROR ajoutés

IOException — avant le `return ValidationResult(SYSTEM_ERROR, ...)` :

```java
log.error("Process validation failed to start: command={}", commandString, e);
```

InterruptedException — avant le `return ValidationResult(SYSTEM_ERROR, ...)` :

```java
log.error("Process validation interrupted: command={}", commandString, e);
```

Note : `commandString` est `String.join(" ", command.command())` — contient la commande Maven wrapper sans secret (`cmd.exe /c mvnw.cmd clean compile -DskipTests` ou `./mvnw clean compile -DskipTests`).

---

## 3. `WorkflowProjectRegistryService`

### Logs INFO ajoutés

Après `repository.save(entity)`, selon que l'entité existait déjà :

```java
// Mise à jour
log.info("Workflow project updated: projectName={}, projectKey={}",
        normalizedName.projectName(), normalizedName.projectKey());

// Création
log.info("Workflow project registered: projectName={}, projectKey={}",
        normalizedName.projectName(), normalizedName.projectKey());
```

`projectPath` n'est jamais loggé en INFO.

### Logs WARN ajoutés

Dans `registerOrUpdate` — refus nom invalide (avant le `return failed(...)`) :

```java
log.warn("Workflow project registration rejected: reason={}", normalizedName.reason());
```

Dans `registerOrUpdate` — refus path invalide, nom disponible (avant le `return failed(...)`) :

```java
log.warn("Workflow project registration rejected: projectName={}, reason={}",
        normalizedName.projectName(), normalizedPath.reason());
```

`reason` contient uniquement des messages internes codés en dur (`"projectPath does not exist"`, `"projectPath must be a directory"`, `"projectPath is not allowed"`, etc.) — jamais le path lui-même.

Dans `lookup` — nom invalide :

```java
log.warn("Workflow project lookup failed — invalid name: reason={}", normalizedName.reason());
```

Dans `lookup` — projet introuvable :

```java
log.warn("Workflow project lookup: project not found: projectKey={}", normalizedName.projectKey());
```

### Logs DEBUG ajoutés

Dans `lookup`, après l'appel `repository.findByProjectKey(...)` :

```java
log.debug("Workflow project lookup: projectKey={}, found={}", normalizedName.projectKey(), entity.isPresent());
```

La méthode `lookup` a été légèrement restructurée : le `Optional` est capturé dans une variable locale `entity` pour permettre le log avant le `.map(...).orElseGet(...)`. Le comportement est identique.

---

## Vérification confidentialité

Contrôle réalisé par grep sur les 3 fichiers modifiés (pattern : `prompt|stdout\b|stderr\b|projectPath|token|secret|password|getPath\b`).

| Donnée interdite | Présente dans un log ? |
|-----------------|----------------------|
| `request.prompt()` | ❌ Non |
| `stdout` / `stderr` complets | ❌ Non — uniquement `.length()` |
| `projectPath` en INFO | ❌ Non — jamais loggé |
| Path absolu en INFO | ❌ Non — `workingDirectory=set\|none` uniquement |
| Token / clé API / secret | ❌ Non |
| Commande avec secret | ❌ Non — `commandString` = commande Maven publique |
| Message contenant le mot "prompt" | ✅ Vérifié — `"Codex CLI failed to write prompt to stdin"` est un message statique décrivant l'opération, sans interpolation de valeur |

---

## Tests

Maven non disponible dans l'environnement sandbox. La commande à exécuter sur la machine locale est :

```
.\mvnw.cmd test
```

La vérification manuelle confirme :

- `@Slf4j` présent sur les 3 classes (conservé sur `CodexWorkflowClient`, ajouté sur les 2 autres).
- `import lombok.extern.slf4j.Slf4j;` ajouté dans `WorkflowValidationService` et `WorkflowProjectRegistryService`.
- Aucun appel `log.*` ne contient de données sensibles (contrôle grep).
- La restructuration dans `WorkflowValidationService` (variables `stdout`/`stderr` locales) ne change pas le comportement — mêmes appels `safeGet(...)` dans le même ordre.
- La restructuration dans `WorkflowProjectRegistryService.lookup` (capture du `Optional` en variable locale) ne change pas le comportement — même appel `repository.findByProjectKey(...)`, même chain `.map().orElseGet()`.

---

## Confirmation : aucun comportement métier modifié

- Les statuts retournés (`ValidationStatus`, `WorkflowProjectRegistrationResult`, `WorkflowProjectLookupResult`) sont inchangés.
- La commande Codex CLI (`buildCommand(...)`) est inchangée — aucun `--json`, `--sandbox`, `--output-last-message` ajouté.
- La logique de validation process est intacte — même gestion timeout, même `destroy()` / `destroyForcibly()`.
- La validation de projet (nom, path, dangerosité) est intacte — les WARN sont ajoutés avant les `return failed(...)` existants, pas à la place.
- Les messages Telegram (`WorkflowTelegramMessageRenderer.*`) ne sont pas concernés par ces classes.
- Aucune nouvelle fonctionnalité ajoutée.
- Aucun framework de logging ajouté.
