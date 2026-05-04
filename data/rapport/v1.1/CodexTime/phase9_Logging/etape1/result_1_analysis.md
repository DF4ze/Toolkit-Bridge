# Rapport — Phase 9 Logging Étape 1 — Logs Telegram et orchestration

---

## Résumé exécutif

Cette étape 1 analyse la couche Telegram et orchestration de CodexTime : 4 classes ciblées, périmètre volontairement restreint.

L'état actuel est le suivant :
- `WorkflowTelegramOrchestrationService` : logging partiel, mais utilise `LoggerFactory.getLogger(...)` — **migration nécessaire vers `@Slf4j`**. Les logs existants couvrent les erreurs techniques mais pas les décisions métier (WAIT_HUMAN, CONTINUE, STOP_FAILURE).
- `WorkflowTelegramController` : `@Slf4j` présent, mais un seul `log.debug` sur `/workflow`. Les commandes à effet (`/workflow_run`, `/workflow_resume`, etc.) ne sont pas loggées.
- `WorkflowTelegramProjectService` : aucun logging, aucune annotation. **À instrumenter.**
- `WorkflowTelegramRoadmapService` : `@Slf4j` présent, un seul `log.debug` sur le chargement réussi — niveau insuffisant (devrait être INFO) et les cas d'erreur ne sont pas loggés.

Les ajouts à faire sont ciblés, sobres et sans risque de fuite de données si les règles ci-dessous sont respectées.

**Conclusion : PRÊT pour implémentation.**

---

## 1. État actuel des logs dans les 4 classes

### `WorkflowTelegramOrchestrationService`

| Aspect | État |
|--------|------|
| Annotation logging | `private static final Logger log = LoggerFactory.getLogger(...)` ⚠️ |
| `startRun` | Aucun log (ni INFO ni WARN sur les refus) |
| `resume` | Aucun log |
| `executeRun` | `log.error(...)` sur exception — correct, mais incomplet |
| `executeResume` | `log.error(...)` sur exception — correct, mais incomplet |
| `handleResult` | Aucun log sur les décisions (WAIT_HUMAN, STOP_FAILURE, CONTINUE) |
| `executeAsync` | `log.warn(...)` sur timeout async — correct |

Logs existants dans `executeRun` et `executeResume` :
```java
log.error(
    "Workflow run failed chatId={} userId={} runId={} project={} phase={} etape={}",
    chatId, userId, runId, ..., e
);
```
Ces logs sont bien structurés. La migration vers `@Slf4j` les laisse intacts.

---

### `WorkflowTelegramController`

| Aspect | État |
|--------|------|
| Annotation logging | `@Slf4j` ✅ |
| `/workflow` | `log.debug(...)` avec chatId, userId, botId — acceptable |
| `/workflow_run` | Aucun log |
| `/workflow_resume` | Aucun log |
| `/workflow_status` | Aucun log — lecture seule, OK |
| `/workflow_summary` | Aucun log — lecture seule, OK |
| `/workflow_roadmap_load` | Aucun log |
| `/workflow_project_set` | Aucun log |

---

### `WorkflowTelegramProjectService`

| Aspect | État |
|--------|------|
| Annotation logging | Aucune (`@RequiredArgsConstructor` seulement) |
| `setProject` | Aucun log |
| Cas succès (registered/updated) | Aucun log |
| Cas erreur (name manquant, path invalide, refus registry) | Aucun log |
| `sanitizeReason` | Déjà en place : masque les paths dans les raisons d'erreur ✅ |

---

### `WorkflowTelegramRoadmapService`

| Aspect | État |
|--------|------|
| Annotation logging | `@Slf4j` ✅ |
| Succès | `log.debug(...)` avec `normalizedProject` et `safeRelative` — niveau trop bas |
| Erreurs (roadmap absente, extension, lecture) | Aucun log |
| Sécurité path | `safeRelative` = chemin relatif calculé par `workspaceLayout.relativize(...)` ✅ |

---

## 2. Proposition de logs pour `WorkflowTelegramOrchestrationService`

### Migration préalable

Remplacer :
```java
private static final Logger log = LoggerFactory.getLogger(WorkflowTelegramOrchestrationService.class);
```

Par l'annotation Lombok sur la classe :
```java
@Slf4j
```

Supprimer l'import `org.slf4j.Logger` et `org.slf4j.LoggerFactory`. L'import Lombok n'est pas nécessaire (traité par l'annotation processor). Les appels `log.*` existants restent identiques.

---

### Méthode `startRun`

**Workflow accepté — log INFO après `tryMarkRunning` réussi :**
```java
log.info("Workflow run started: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, target.projectName(), target.phase(), target.etape());
```

**Run refusé — projet inconnu :**
```java
log.warn("Workflow run rejected — unknown project: chatId={}, project={}", chatId, target.projectName());
```

**Run refusé — déjà en cours :**
```java
log.warn("Workflow run rejected — already running: chatId={}", chatId);
```

**Run refusé — target invalide :**
```java
log.warn("Workflow run rejected — invalid target: chatId={}, reason={}", chatId, target.errorMessage());
```

**Run refusé — projet non configuré :**
```java
log.warn("Workflow run rejected — no project configured: chatId={}", chatId);
```

Placement : ces logs s'insèrent dans les branches de retour anticipé de `startRun`, avant le `return` correspondant au message d'erreur utilisateur. Le log INFO s'insère juste après `sessionStore.tryMarkRunning(chatId, runId)` réussi.

---

### Méthode `resume`

**Resume accepté :**
```java
log.info("Workflow resume started: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, session.projectName(), session.phase(), session.etape());
```

**Resume refusé — déjà en cours :**
```java
log.warn("Workflow resume rejected — already running: chatId={}", chatId);
```

**Resume refusé — pas en WAIT_HUMAN :**
```java
log.warn("Workflow resume rejected — not in WAIT_HUMAN state: chatId={}, status={}",
        chatId, session.lastStatus());
```

**Resume refusé — contexte incomplet :**
```java
log.warn("Workflow resume rejected — incomplete context: chatId={}", chatId);
```

**Resume refusé — projet absent :**
```java
log.warn("Workflow resume rejected — no project configured: chatId={}", chatId);
```

**Resume refusé — project path manquant :**
```java
log.warn("Workflow resume rejected — project path missing: chatId={}, project={}", chatId, session.projectName());
```

---

### Méthode `handleResult`

C'est ici que la décision finale du workflow est connue. C'est le log le plus important de la classe.

**WAIT_HUMAN :**
```java
log.info("Workflow completed — WAIT_HUMAN: chatId={}, runId={}", chatId, runId);
```

**STOP_FAILURE :**
```java
log.info("Workflow completed — STOP_FAILURE: chatId={}, runId={}", chatId, runId);
```

**CONTINUE (succès) :**
```java
log.info("Workflow completed — CONTINUE: chatId={}, runId={}", chatId, runId);
```

**Résultat null (cas anormal) :**
```java
log.warn("Workflow returned null result: chatId={}, runId={}", chatId, runId);
```

---

### Méthodes `executeRun` / `executeResume`

Les `log.error` existants sont corrects et couvrent les exceptions techniques. Aucune modification du contenu, uniquement la migration vers `@Slf4j` (qui ne change pas les appels).

---

### Méthode `executeAsync`

Le `log.warn` existant sur timeout async est correct. Aucune modification.

---

### Ce qu'il ne faut pas logger dans cette classe

- `projectPath` / `resolvedProjectPath` : jamais, même en DEBUG
- `roadmapPath` : jamais
- `variables` du contexte workflow : jamais les valeurs, uniquement les clés si nécessaire en DEBUG
- `reportRootDirectory` : jamais
- `botId` : acceptable en DEBUG, pas en INFO
- Le message de retour utilisateur (`WorkflowTelegramMessageRenderer.*`) : jamais copié dans les logs

---

## 3. Proposition de logs pour `WorkflowTelegramController`

Le contrôleur délègue immédiatement à la couche service. Les logs ici sont légers : ils servent à savoir quelle commande est entrée, pas à dupliquer ce que le service loggue.

### `/workflow_run`

```java
log.info("Command /workflow_run: chatId={}, userId={}, project={}, phase={}, etape={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId(),
        projectName,
        phase,
        etape);
```

Placement : avant l'appel à `orchestrationService.startRun(...)`. Ne pas logger `botId` en INFO (acceptable en DEBUG si nécessaire).

### `/workflow_resume`

```java
log.info("Command /workflow_resume: chatId={}, userId={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId());
```

### `/workflow_roadmap_load`

```java
log.info("Command /workflow_roadmap_load: chatId={}, userId={}, project={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId(),
        projectName);
```

Ne pas logger `path` (argument brut qui peut contenir un path absolu fourni par l'utilisateur).

### `/workflow_project_set`

```java
log.info("Command /workflow_project_set: chatId={}, userId={}",
        ctx == null ? null : ctx.getChatId(),
        ctx == null ? null : ctx.getUserId());
```

Ne pas logger `ctx.getArgs()` directement (contient le path absolu brut passé par l'utilisateur).

### `/workflow`, `/workflow_status`, `/workflow_summary`

Commandes de lecture. Le `log.debug` existant sur `/workflow` est suffisant. Aucun log à ajouter sur `/workflow_status` et `/workflow_summary`.

---

### Règle spécifique au contrôleur

Les arguments bruts Telegram (`ctx.getArgs()`) **ne doivent jamais être loggés directement**. Ils peuvent contenir :
- un path absolu (`path="D:\Documents\Spring\..."`)
- des données utilisateur non filtrées

Seuls les champs extraits et validés (`projectName`, `phase`, `etape`) sont loggables.

---

## 4. Proposition de logs pour `WorkflowTelegramProjectService`

Ajouter `@Slf4j` sur la classe (compatible avec `@RequiredArgsConstructor` déjà présent).

### Méthode `setProject`

**Succès — projet enregistré :**
```java
log.info("Workflow project registered: name={}", result.projectName());
```

**Succès — projet mis à jour :**
```java
log.info("Workflow project updated: name={}", result.projectName());
```

Ces deux cas peuvent être factorisés :
```java
log.info("Workflow project {}: name={}",
        result.created() ? "registered" : "updated",
        result.projectName());
```

**Nom manquant :**
```java
log.warn("Workflow project set rejected — missing name");
```

**Path manquant ou invalide :**
```java
log.warn("Workflow project set rejected — invalid path: reason={}", extractedPath.reason());
```

**Refus du registry (path dangereux, inexistant, non lisible…) :**
```java
log.warn("Workflow project set rejected — registry refused: name={}, reason={}", name, sanitizeReason(result.reason()));
```

Note : `sanitizeReason` est déjà implémentée dans la classe et masque les paths absolus. L'utiliser systématiquement pour les WARN.

### Ce qu'il ne faut pas logger

- `extractedPath.path()` : jamais — c'est le path absolu brut fourni par l'utilisateur
- `result.normalizedProjectPath()` : jamais
- Les arguments bruts (`args`) : jamais

---

## 5. Proposition de logs pour `WorkflowTelegramRoadmapService`

La classe a déjà `@Slf4j`. Le champ `safeRelative` (chemin relatif calculé) est safe à logger.

### Méthode `loadRoadmap`

**Succès — monter le niveau de DEBUG à INFO :**

Remplacer :
```java
log.debug("Roadmap loaded for project={} path={}", normalizedProject, safeRelative);
```

Par :
```java
log.info("Roadmap loaded: project={}, path={}", normalizedProject, safeRelative);
```

`safeRelative` est le chemin relatif par rapport à la racine partagée — pas un path absolu. Safe en INFO.

**Roadmap absente (`!Files.exists`) :**
```java
log.warn("Roadmap not found: chatId={}, relativePath={}", chatId, relativePath);
```

**Extension invalide :**
```java
log.warn("Roadmap rejected — invalid extension: chatId={}", chatId);
```

**Fichier vide :**
```java
log.warn("Roadmap rejected — empty file: chatId={}", chatId);
```

**Path refusé (ForbiddenCommandException / IllegalArgumentException) :**
```java
log.warn("Roadmap rejected — forbidden path: chatId={}", chatId);
```

**Erreur lors de l'accès à `sharedRoot` (IOException dans `workspaceLayout.sharedRoot()`) :**
```java
log.warn("Roadmap load failed — workspace root unavailable: chatId={}", chatId);
```

**Fichier non régulier ou non lisible :**
```java
log.warn("Roadmap rejected — not a readable file: chatId={}", chatId);
```

### Ce qu'il ne faut pas logger

- `relativePath` brut en INFO (c'est une valeur utilisateur non validée avant résolution) — acceptable en WARN uniquement si la résolution a échoué, et uniquement s'il n'est pas absolu
- `resolved` (path absolu complet) : jamais
- `sharedRoot` (path absolu) : jamais
- Le message retourné à l'utilisateur : jamais copié dans les logs

---

## 6. Champs recommandés

| Champ | Type | Niveaux | Commentaire |
|-------|------|---------|-------------|
| `chatId` | Long | INFO, WARN, ERROR | Toujours présent dès qu'une session est identifiée |
| `userId` | Long | INFO (optionnel) | Peut être null — à inclure si disponible sans effort |
| `runId` | String | INFO, ERROR | Présent dès que le run est alloué |
| `projectName` | String | INFO, WARN | Jamais le path |
| `phase` | Integer | INFO | Uniquement dans les logs de démarrage |
| `etape` | Integer | INFO | Uniquement dans les logs de démarrage |
| `decision` | String | INFO | Valeur de `WorkflowStepDecision` |
| `status` | String | WARN | Valeur de `WorkflowTelegramRunStatus` |
| `reason` | String | WARN | Courte, sanitisée — jamais brute depuis un path |
| `safeRelative` | String | INFO | Uniquement pour roadmap — chemin relatif calculé |

---

## 7. Règles de confidentialité

### Ce qu'il ne faut jamais logger

| Donnée | Risque | Classe concernée |
|--------|--------|-----------------|
| `projectPath` / `resolvedProjectPath` | Path absolu local | `WorkflowTelegramOrchestrationService` |
| `roadmapPath` | Path absolu local | `WorkflowTelegramOrchestrationService` |
| `reportRootDirectory` | Path absolu local | `WorkflowTelegramOrchestrationService` |
| `ctx.getArgs()` bruts | Peut contenir path absolu | `WorkflowTelegramController` |
| `extractedPath.path()` brut | Path absolu utilisateur | `WorkflowTelegramProjectService` |
| `resolved` (roadmap) | Path absolu | `WorkflowTelegramRoadmapService` |
| `sharedRoot` | Path absolu | `WorkflowTelegramRoadmapService` |
| Token Telegram | Secret | Toutes |
| Message retourné à l'utilisateur | Peut contenir des détails internes | Toutes |
| Contenu du summary | Données métier potentiellement volumineuses | Toutes |

### Garde-fous simples

**Règle 1 — Pas de path en INFO.** Tout ce qui vient d'un `Path`, d'une propriété de configuration ou d'un argument utilisateur brut est interdit en INFO.

**Règle 2 — `sanitizeReason` obligatoire.** Tout message de raison d'erreur passé dans un log WARN doit passer par `sanitizeReason(...)` ou une vérification équivalente avant d'être loggé. Cette méthode existe déjà dans `WorkflowTelegramProjectService`.

**Règle 3 — Pas de `ctx.getArgs()` dans les logs.** Extraire uniquement les champs parsés (`projectName`, `phase`, `etape`) avant de les inclure dans un log.

**Règle 4 — `safeRelative` uniquement pour les paths de roadmap.** Ce champ est calculé par `workspaceLayout.relativize(...)` et est garanti relatif. C'est le seul path acceptable en INFO.

**Règle 5 — Le message utilisateur n'est pas un log.** Les chaînes retournées par `WorkflowTelegramMessageRenderer.*` sont des messages pour l'utilisateur Telegram. Ne jamais les passer dans un `log.*`.

---

## 8. Risques

### Risque principal — fuite de path absolu via les args Telegram

Les commandes `/workflow_roadmap_load path="..."` et `/workflow_project_set path="..."` reçoivent des paths absolus fournis directement par l'utilisateur. Si `ctx.getArgs()` est loggé tel quel, le path absolu se retrouve dans les logs serveur.

**Mitigation** : ne logger que les champs extraits et validés (`projectName`, `phase`, `etape`). Le path brut n'est jamais loggé.

### Risque modéré — fuite via message de raison d'erreur

Les raisons d'erreur du registry (`result.reason()`) peuvent contenir des paths absolus si le code de validation les inclut dans le message. `sanitizeReason(...)` atténue ce risque mais ne couvre que les patterns connus.

**Mitigation** : utiliser `sanitizeReason(...)` systématiquement sur tout message de raison avant de le logger en WARN.

### Risque faible — logs INFO trop nombreux

`startRun` peut être appelée fréquemment depuis Telegram. Avec un log INFO à chaque appel accepté, les logs peuvent être volumineux si plusieurs utilisateurs utilisent le système en parallèle.

**Mitigation** : acceptable à ce stade. La solution n'est pas temps-réel et les volumes resteront faibles. Si nécessaire, passer le log de démarrage en DEBUG et ne garder qu'un log INFO sur la décision finale.

### Risque faible — duplication entre contrôleur et service

Le contrôleur logguera la commande reçue, le service logguera la décision prise. En cas d'erreur rapide (refus immédiat), on aura deux logs : un INFO contrôleur + un WARN service. Ce n'est pas un problème — c'est le comportement attendu.

---

## 9. Tests et vérifications

### Vérifications de compilation

- Ajouter `@Slf4j` sur `WorkflowTelegramProjectService` : vérifier la compatibilité avec `@RequiredArgsConstructor` (les deux annotations sont compatibles — Lombok gère les deux).
- Supprimer `private static final Logger log = LoggerFactory.getLogger(...)` de `WorkflowTelegramOrchestrationService` : vérifier que les imports `org.slf4j.Logger` et `org.slf4j.LoggerFactory` sont bien supprimés.
- Compiler le module après chaque lot.

### Vérifications comportementales

- Tous les tests existants (`WorkflowTelegramOrchestrationServiceTest`, `WorkflowTelegramControllerTest`, `WorkflowTelegramProjectServiceTest`, `WorkflowTelegramRoadmapServiceTest`) doivent passer sans modification.
- Les ajouts de `log.*` ne modifient aucune valeur de retour, aucune exception lancée, aucun comportement de session.

### Vérifications manuelles des messages

Avant de valider chaque lot, relire chaque nouveau `log.*` et vérifier :
- Aucun champ de type `Path`, `String` de path absolu, ou `List<String>` d'args bruts.
- Aucun message utilisateur (`WorkflowTelegramMessageRenderer.*`) inclus.
- `sanitizeReason(...)` appliqué sur toute raison d'erreur loggée en WARN.

### Ce qu'il ne faut pas tester

Ne pas écrire de tests unitaires qui vérifient l'exacte valeur d'un message de log (via `LogCaptor`, `ListAppender`, etc.). Ces tests sont fragiles. La seule exception justifiée serait un test sur `sanitizeReason(...)` — mais cette méthode est déjà couverte dans les tests existants.

---

## 10. Plan d'implémentation

### Lot 1 — Migration `WorkflowTelegramOrchestrationService`

- Supprimer `private static final Logger log = LoggerFactory.getLogger(...)` et ses imports
- Ajouter `@Slf4j` sur la classe
- Compiler et vérifier que les `log.error` et `log.warn` existants fonctionnent toujours

### Lot 2 — Logs INFO/WARN sur `startRun` et `resume`

Dans `WorkflowTelegramOrchestrationService` :
- Ajouter `log.info` après `tryMarkRunning` réussi dans `startRun`
- Ajouter `log.warn` sur chaque branche de refus dans `startRun` et `resume`
- Ajouter `log.info` sur chaque décision dans `handleResult`

### Lot 3 — Logs dans `WorkflowTelegramController`

- Ajouter `log.info` sur `/workflow_run`, `/workflow_resume`, `/workflow_roadmap_load`, `/workflow_project_set`
- Ne logger que les champs parsés, jamais les args bruts

### Lot 4 — Instrumentation de `WorkflowTelegramProjectService`

- Ajouter `@Slf4j` sur la classe
- Ajouter `log.info` sur création et mise à jour de projet
- Ajouter `log.warn` sur les refus (en passant par `sanitizeReason`)

### Lot 5 — Correction et enrichissement de `WorkflowTelegramRoadmapService`

- Monter le `log.debug` de succès en `log.info`
- Ajouter `log.warn` sur les cas d'erreur (roadmap absente, extension invalide, fichier vide, path refusé)

### Lot 6 — Vérification finale

- Compilation propre
- Passage des tests existants
- Relecture manuelle de tous les nouveaux `log.*` (contrôle confidentialité)

---

## Conclusion

**PRÊT pour implémentation.**

Les 4 classes sont stables, les patterns de logging sont définis précisément, et la migration de `LoggerFactory` vers `@Slf4j` dans `WorkflowTelegramOrchestrationService` est sans risque.

Les ajouts sont sobres : environ 15 appels `log.*` répartis sur les 4 classes, concentrés sur les événements à valeur opérationnelle réelle. Aucun nouveau framework. Aucune modification comportementale.

La priorité d'implémentation est le **Lot 2** (logs de démarrage et décisions dans `WorkflowTelegramOrchestrationService`) : c'est ce qui rend le workflow visible en production en premier, après la migration obligatoire du Lot 1.
