# Rapport d'implémentation — Phase 9 Logging Étape 1 — Logs Telegram et orchestration

---

## Résumé

Les 4 classes ciblées ont été instrumentées. La migration `LoggerFactory → @Slf4j` est effectuée. Les logs sont sobres, ciblés sur les événements métier, et aucune donnée sensible n'est exposée. Les tests ne peuvent pas être lancés depuis le sandbox (Maven non disponible sans réseau), mais la vérification de cohérence est réalisée manuellement.

---

## Fichiers modifiés

| Fichier | Action |
|---------|--------|
| `service/telegram/workflow/WorkflowTelegramOrchestrationService.java` | Migration @Slf4j + logs INFO/WARN/ERROR |
| `controler/telegram/workflow/WorkflowTelegramController.java` | Logs INFO sur commandes à effet |
| `service/telegram/workflow/WorkflowTelegramProjectService.java` | Ajout @Slf4j + logs INFO/WARN |
| `service/telegram/workflow/WorkflowTelegramRoadmapService.java` | debug→info + logs WARN sur erreurs |

---

## 1. `WorkflowTelegramOrchestrationService`

### Migration @Slf4j effectuée

Supprimé :
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ...
private static final Logger log = LoggerFactory.getLogger(WorkflowTelegramOrchestrationService.class);
```

Ajouté :
```java
import lombok.extern.slf4j.Slf4j;
// ...
@Service
@Slf4j
public class WorkflowTelegramOrchestrationService {
```

Les appels `log.error(...)` et `log.warn(...)` existants dans `executeRun`, `executeResume` et `executeAsync` sont conservés sans modification.

### Logs WARN ajoutés — `startRun`

```java
// Target invalide — projet non configuré
log.warn("Workflow run rejected — no project configured: chatId={}", chatId);

// Target invalide — autre raison
log.warn("Workflow run rejected — invalid target: chatId={}", chatId);

// Projet explicite mais inconnu dans le registre
log.warn("Workflow run rejected — unknown project: chatId={}, project={}", chatId, target.projectName());

// Pas de projet configuré en session
log.warn("Workflow run rejected — no project configured: chatId={}", chatId);

// Run déjà en cours
log.warn("Workflow run rejected — already running: chatId={}", chatId);
```

### Log INFO ajouté — `startRun` (run accepté)

```java
log.info("Workflow run started: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, target.projectName(), target.phase(), target.etape());
```

Placé après `tryMarkRunning` réussi, avant `executeAsync`.

### Logs WARN ajoutés — `resume`

```java
log.warn("Workflow resume rejected — already running: chatId={}", chatId);
log.warn("Workflow resume rejected — not in WAIT_HUMAN state: chatId={}, status={}", chatId, session.lastStatus());
log.warn("Workflow resume rejected — incomplete context: chatId={}", chatId);
log.warn("Workflow resume rejected — no project configured: chatId={}", chatId);
log.warn("Workflow resume rejected — unknown project: chatId={}, project={}", chatId, session.projectName());
log.warn("Workflow resume rejected — project path missing: chatId={}, project={}", chatId, session.projectName());
log.warn("Workflow resume rejected — already running: chatId={}", chatId);  // tryMarkRunning
```

### Log INFO ajouté — `resume` (resume accepté)

```java
log.info("Workflow resume started: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, session.projectName(), session.phase(), session.etape());
```

### Logs ajoutés — `handleResult`

```java
log.warn("Workflow returned null result: chatId={}, runId={}", chatId, runId);

log.info("Workflow completed — WAIT_HUMAN: chatId={}, runId={}", chatId, runId);

log.info("Workflow completed — STOP_FAILURE: chatId={}, runId={}", chatId, runId);

log.info("Workflow completed — CONTINUE: chatId={}, runId={}", chatId, runId);
```

---

## 2. `WorkflowTelegramController`

`@Slf4j` déjà présent. Aucune modification d'import.

### Logs INFO ajoutés sur les commandes à effet

**`/workflow_run`** — après extraction des paramètres parsés, avant l'appel au service :
```java
log.info("Command /workflow_run: chatId={}, userId={}, project={}, phase={}, etape={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId(),
        projectName, phase, etape);
```

**`/workflow_resume`** — avant l'appel au service :
```java
log.info("Command /workflow_resume: chatId={}, userId={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId());
```

**`/workflow_roadmap_load`** — après extraction de `projectName`, avant l'appel au service (`path` non loggé) :
```java
log.info("Command /workflow_roadmap_load: chatId={}, userId={}, project={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId(),
        projectName);
```

**`/workflow_project_set`** — avant l'appel au service (`ctx.getArgs()` non loggé) :
```java
log.info("Command /workflow_project_set: chatId={}, userId={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId());
```

Aucun log ajouté sur `/workflow`, `/workflow_status`, `/workflow_summary`.

---

## 3. `WorkflowTelegramProjectService`

### Ajout @Slf4j

```java
import lombok.extern.slf4j.Slf4j;
// ...
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTelegramProjectService {
```

Compatible avec `@RequiredArgsConstructor` déjà présent.

### Logs WARN sur les refus

```java
log.warn("Workflow project set rejected — missing name");

log.warn("Workflow project set rejected — invalid path: name={}, reason={}", name, extractedPath.reason());

log.warn("Workflow project set rejected — registry refused: name={}, reason={}", name, sanitizeReason(result.reason()));
```

Note : `extractedPath.reason()` contient uniquement des messages internes codés en dur (`"Missing project path"`, `"Invalid quoted path (missing closing quote)"`) — pas de valeur utilisateur. `sanitizeReason(...)` est appliqué systématiquement sur les raisons du registry.

### Log INFO sur le succès

```java
log.info("Workflow project {}: name={}", result.created() ? "registered" : "updated", result.projectName());
```

`projectName` loggé = valeur validée et normalisée retournée par le registry, pas le path.

---

## 4. `WorkflowTelegramRoadmapService`

`@Slf4j` déjà présent. Aucune modification d'import.

### Log succès : debug → info

Avant :
```java
log.debug("Roadmap loaded for project={} path={}", normalizedProject, safeRelative);
```

Après :
```java
log.info("Roadmap loaded: project={}, path={}", normalizedProject, safeRelative);
```

`safeRelative` est calculé par `workspaceLayout.relativize(sharedRoot, resolved)` — chemin relatif garanti, jamais un path absolu.

### Logs WARN ajoutés sur les erreurs

```java
log.warn("Roadmap rejected — missing path: chatId={}", chatId);
log.warn("Roadmap load failed — workspace root unavailable: chatId={}", chatId);
log.warn("Roadmap rejected — forbidden path: chatId={}", chatId);
log.warn("Roadmap not found: chatId={}", chatId);
log.warn("Roadmap rejected — not a readable file: chatId={}", chatId);
log.warn("Roadmap rejected — invalid extension: chatId={}", chatId);
log.warn("Roadmap rejected — empty file: chatId={}", chatId);
log.warn("Roadmap rejected — read error: chatId={}", chatId);
```

Seul `chatId` est loggé sur les erreurs — ni path résolu, ni `sharedRoot`, ni message utilisateur.

---

## Vérification confidentialité

Contrôle réalisé par grep sur chaque fichier modifié.

| Donnée interdite | Présente dans un log ? |
|-----------------|----------------------|
| `projectPath` / `resolvedProjectPath` | ❌ Non |
| `roadmapPath` | ❌ Non |
| `reportRootDirectory` | ❌ Non |
| `ctx.getArgs()` bruts | ❌ Non |
| Path absolu (`resolved`, `sharedRoot`) | ❌ Non |
| Token Telegram | ❌ Non |
| Prompt / contenu de fichier | ❌ Non |
| `extractedPath.path()` brut | ❌ Non |

Seuls champs présents dans les logs : `chatId`, `userId`, `runId`, `projectName` (normalisé), `phase`, `etape`, `decision` (enum), `status` (enum), `reason` (message interne ou sanitisé), `safeRelative` (chemin relatif calculé).

---

## Tests

Maven non disponible dans l'environnement sandbox (le wrapper tente un téléchargement réseau non autorisé). La commande à exécuter sur la machine locale est :

```
.\mvnw.cmd test
```

La vérification manuelle confirme :

- Aucun import `Logger` / `LoggerFactory` restant dans les 4 fichiers.
- `@Slf4j` correctement positionné sur toutes les classes modifiées.
- Tous les `log.*` existants dans `executeRun`, `executeResume`, `executeAsync` sont conservés sans modification.
- Aucun appel `log.*` ne contient de données sensibles (contrôle grep).
- Aucune modification de logique métier, de valeur de retour, ou de message Telegram.

---

## Confirmation : aucun comportement métier modifié

- Toutes les valeurs de retour des méthodes sont identiques.
- Les messages envoyés à l'utilisateur Telegram (`WorkflowTelegramMessageRenderer.*`) sont inchangés.
- La logique de session (`sessionStore.*`), de résolution projet, et d'exécution async est intacte.
- Les `log.error` existants dans `executeRun`, `executeResume`, et `executeAsync` sont conservés tels quels.
