# Analyse — Phase 9 Logging Étape 1 — Journalisation intelligente CodexTime

---

## Résumé exécutif

Le sous-système CodexTime de Toolkit-Bridge est **actuellement quasi-invisible** en production. Sur les 16 classes identifiées comme prioritaires, **12 n'ont aucun logging**. Les 4 qui en ont quelques-uns sont partiellement couvertes, et l'une d'elles (`WorkflowTelegramOrchestrationService`) utilise `LoggerFactory.getLogger(...)` au lieu de `@Slf4j`.

Le chemin critique — démarrage Telegram → construction contexte → exécution des steps Codex → validation Maven → décision finale — est **entièrement opaque**. En cas d'incident, il est impossible de savoir à quelle étape le workflow s'est arrêté, quel projet était ciblé, ou quelle exception a provoqué un `STOP_FAILURE`.

L'analyse identifie **13 classes à instrumenter**, avec une stratégie sobre et cohérente. Les logs à ajouter sont ciblés sur les transitions métier clés. La confidentialité des données est traitée explicitement pour chaque classe.

**Conclusion : PRÊT pour implémentation.** Les classes sont stables, les patterns de logging sont identifiés précisément, et la migration vers `@Slf4j` est mécanique là où `LoggerFactory` est encore utilisé.

---

## 1. Inventaire des classes concernées

### 1.1 État actuel par classe

| Classe | Package | Logging actuel | Annotation | Priorité |
|--------|---------|---------------|------------|----------|
| `WorkflowTelegramOrchestrationService` | service.telegram.workflow | Partiel (`log.error`, `log.warn`) | `LoggerFactory.getLogger` ⚠️ | P1 |
| `CodexWorkflowClient` | service.agent.workflow.runtime.codex | Partiel (`log.debug`, `log.error`) | `@Slf4j` ✅ | P1 |
| `WorkflowTelegramController` | controler.telegram.workflow | Minimal (1 `log.debug`) | `@Slf4j` ✅ | P1 |
| `WorkflowTelegramRoadmapService` | service.telegram.workflow | Minimal (1 `log.debug`) | `@Slf4j` ✅ | P2 |
| `AnalysisReviewWorkflowRunner` | service.agent.workflow.runtime | Aucun | — | P1 |
| `WorkflowOrchestrator` | service.agent.workflow.runtime.orchestrator | Aucun | — | P1 |
| `GlobalAnalysisStep` | service.agent.workflow.runtime.step | Aucun | — | P1 |
| `GlobalReviewStep` | service.agent.workflow.runtime.step | Aucun | — | P1 |
| `CorrectionStep` | service.agent.workflow.runtime.step | Aucun | — | P1 |
| `BuildErrorCorrectionStep` | service.agent.workflow.runtime.step | Aucun | — | P1 |
| `MavenValidationStep` | service.agent.workflow.runtime.step | Aucun | — | P1 |
| `WorkflowValidationService` | service.agent.workflow.runtime.validation | Aucun | — | P2 |
| `WorkflowProjectRegistryService` | service.agent.workflow.runtime.project | Aucun | — | P2 |
| `WorkflowTelegramProjectService` | service.telegram.workflow | Aucun | — | P2 |
| `WorkflowTelegramSummaryService` | service.telegram.workflow | Aucun | — | P3 |
| `WorkflowArtifactService` | service.agent.workflow.runtime.artifact | Aucun | — | P3 |
| `WorkflowTelegramSessionStore` | service.telegram.workflow | Aucun | — | P3 |

### 1.2 Bilan

- **Aucun logging** : 13 classes
- **Logging partiel** : 3 classes (`WorkflowTelegramOrchestrationService`, `CodexWorkflowClient`, `WorkflowTelegramController`)
- **Migration nécessaire** (LoggerFactory → @Slf4j) : 1 classe (`WorkflowTelegramOrchestrationService`)
- **Classes à ne pas instrumenter** : `WorkflowTelegramSessionStore` (état mémoire pur, les transitions sont loggées par leurs appelants), `WorkflowArtifactService` (utilitaire I/O, les erreurs remontent via exception)

---

## 2. Stratégie de niveaux de log

### INFO — événements métier observables

Réservé aux transitions importantes qui ont une valeur opérationnelle directe. Un opérateur qui lit les logs INFO doit comprendre ce qui s'est passé sans lire le code.

- Démarrage d'un workflow (`startRun`)
- Démarrage d'une reprise (`resume`)
- Démarrage d'un appel Codex (par step)
- Fin d'un appel Codex avec décision
- Démarrage de la validation Maven
- Fin de la validation Maven (succès ou échec)
- Passage en `WAIT_HUMAN`
- Fin de workflow (CONTINUE ou STOP_FAILURE)
- Projet enregistré ou mis à jour dans le registre
- Roadmap chargée

### DEBUG — détails utiles au diagnostic

Réservé aux informations qui aident à reproduire ou diagnostiquer un problème, mais qui seraient trop bruyantes en INFO.

- Résolution du `projectName` → `projectPath`
- Clés des variables du contexte workflow (jamais les valeurs sensibles)
- Décision retournée par chaque step
- Code de sortie du processus
- Durée d'exécution (également utile en INFO pour Codex/Maven)
- Nombre de caractères stdout/stderr (jamais le contenu)
- Nombre de chunks summary envoyés
- Détail des arguments Telegram parsés

### WARN — anomalies récupérables

Réservé aux situations qui dégradent le service sans le bloquer, ou qui signalent une mauvaise utilisation.

- Tentative de démarrage alors qu'un run est déjà en cours
- Projet inconnu lors d'un `startRun`
- Path projet absent pour un resume
- Roadmap absente
- Timeout Codex détecté (avant de renvoyer un résultat d'échec)
- Validation Maven échouée mais correction automatique disponible (retry)
- Quota ou rate-limit détecté côté provider
- Workflow async qui se termine en erreur non fatale

### ERROR — erreurs réelles qui causent un échec

Réservé aux exceptions ou états qui empêchent le workflow de produire un résultat valide.

- Exception lors du lancement du processus Codex CLI
- Exception lors du lancement du processus Maven
- Timeout de processus non récupéré
- Exception dans `executeRun` ou `executeResume` qui provoque un `failRun`
- `STOP_FAILURE` associé à une cause technique (avec exception)

---

## 3. Champs utiles à logger

### Champs à utiliser de manière cohérente

| Champ | Type | Niveau | Note |
|-------|------|--------|------|
| `runId` | String | INFO, ERROR | Toujours présent dès que le run est créé |
| `chatId` | Long | INFO, WARN, ERROR | Identifiant de session Telegram |
| `userId` | Long | INFO (optionnel) | Peut être null |
| `botId` | String | DEBUG | Identifiant bot pour résolution LLM |
| `projectName` | String | INFO | Jamais le path absolu en INFO |
| `phase` | Integer | INFO | |
| `etape` | Integer | INFO | |
| `decision` | String | INFO | Valeur de `WorkflowStepDecision` |
| `status` | String | INFO | Valeur de `ValidationStatus` ou `WorkflowTelegramRunStatus` |
| `durationMs` | Long | INFO/DEBUG | Toujours pour Codex et Maven |
| `exitCode` | Integer | DEBUG | Code retour du processus |
| `artifactType` | String | DEBUG | Type d'artifact écrit |
| `stdoutLen` | int | DEBUG | Longueur stdout, jamais le contenu |
| `stderrLen` | int | DEBUG | Longueur stderr, jamais le contenu |
| `retryCount` | Integer | INFO/WARN | Pour le cycle de correction Maven |
| `retryMax` | Integer | INFO/WARN | |

### Champs à ne jamais logger

- Token Telegram (présent dans les propriétés de configuration)
- Clé API LLM
- Prompt complet Codex (peut contenir le contenu de fichiers source)
- Contenu complet du rapport d'analyse ou de correction
- Contenu complet du summary
- Path absolu en INFO si non indispensable (acceptable en DEBUG)
- `stderr` complet (longueur seulement)
- `stdout` complet (longueur seulement)

---

## 4. Proposition classe par classe

### 4.1 `WorkflowTelegramOrchestrationService`

**Action préalable** : migrer de `LoggerFactory.getLogger(...)` vers `@Slf4j`.

**Méthode `startRun`**

```java
// Au démarrage effectif (après résolution du target et du path)
log.info("Workflow run requested: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, target.projectName(), target.phase(), target.etape());

// Si run rejeté (déjà en cours)
log.warn("Workflow run rejected — already running: chatId={}", chatId);

// Si projet inconnu
log.warn("Workflow run rejected — unknown project: chatId={}, project={}", chatId, projectName);
```

**Méthode `resume`**

```java
log.info("Workflow resume requested: chatId={}, runId={}, project={}, phase={}, etape={}",
        chatId, runId, session.projectName(), session.phase(), session.etape());

// Si résumé alors qu'aucune attente humaine
log.warn("Workflow resume rejected — not in WAIT_HUMAN state: chatId={}, status={}", chatId, session.lastStatus());
```

**Méthode `handleResult`**

```java
// WAIT_HUMAN
log.info("Workflow completed with WAIT_HUMAN: chatId={}, runId={}", chatId, runId);

// STOP_FAILURE
log.info("Workflow completed with STOP_FAILURE: chatId={}, runId={}, reason={}", chatId, runId, sanitized);

// CONTINUE
log.info("Workflow completed successfully: chatId={}, runId={}", chatId, runId);
```

**Méthodes `executeRun` / `executeResume`** — les `log.error` existants sont corrects, mais enrichir avec `durationMs` si disponible.

**Ce qu'il ne faut pas logger** : `roadmapPath` absolu en INFO, `projectPath` absolu en INFO, contenu du contexte.

---

### 4.2 `AnalysisReviewWorkflowRunner`

Ajouter `@Slf4j`. Cette classe orchestre les cycles de retry, c'est un point d'observation clé.

**Méthode `runWithValidationAndRetry`**

```java
// Début de cycle
log.info("Starting workflow run with validation+retry: runId={}", context.workflowRun().runId());

// Décision du cycle
log.debug("Workflow cycle result: runId={}, decision={}", runId, result.decision());

// Correction déclenchée
log.info("Build error correction triggered: runId={}, retryCount={}/{}", runId, retryCount, retryMax);

// Limite de retry atteinte
log.warn("Build retry limit reached: runId={}, retryCount={}/{}", runId, retryCount, retryMax);
```

**Méthode `runCorrectionAfterReview`**

```java
log.info("Running correction after review: runId={}", context.workflowRun().runId());
```

**Méthode `attachWorkflowSummarySafely`**

```java
// En cas d'échec du summary (exception catchée)
log.warn("Workflow summary generation failed: runId={}", runId);
```

**Ce qu'il ne faut pas logger** : les variables du contexte, les paths absolus en INFO.

---

### 4.3 `WorkflowOrchestrator`

Ajouter `@Slf4j`. Cette classe exécute la chaîne analyse → revue → correction. Logger uniquement les transitions de décision.

**Méthodes `executeSingleStep`, `executeAnalysisReviewWithOptionalCorrection`**

```java
// Avant chaque step (DEBUG uniquement)
log.debug("Executing workflow step: stepType={}, runId={}", step.getClass().getSimpleName(), runId);

// Après chaque step
log.debug("Step result: stepType={}, decision={}", step.getClass().getSimpleName(), result.decision());

// Correction déclenchée
log.debug("Correction triggered after review: runId={}", runId);
```

Note : le `runId` est extrait de `context.workflowRun().runId()`. Ne pas logger les données du step result (peuvent contenir des paths).

---

### 4.4 `GlobalAnalysisStep`

Ajouter `@Slf4j`. Point d'entrée du premier appel Codex.

**Méthode `execute`**

```java
// Avant l'appel Codex
log.info("Codex analysis call started: runId={}, phase={}, etape={}",
        runId, reportPhase, stepNumber);

// Après l'appel Codex (succès)
log.info("Codex analysis call completed: runId={}, success={}, durationMs={}",
        runId, codexResult.success(), codexResult.durationMs());

// Timeout
log.warn("Codex analysis call timed out: runId={}, durationMs={}", runId, codexResult.durationMs());

// Échec non-timeout
log.error("Codex analysis call failed: runId={}, exitCode={}, durationMs={}",
        runId, codexResult.exitCode(), codexResult.durationMs());
```

**Ce qu'il ne faut pas logger** : `sourceContent`, `prompt`, le contenu stdout/stderr (longueur seulement en DEBUG si utile).

---

### 4.5 `GlobalReviewStep`

Ajouter `@Slf4j`. Point clé : c'est ici que la décision `WAIT_HUMAN` / `NEED_CORRECTION` / `OK` est prise.

**Méthode `execute`**

```java
// Avant l'appel Codex
log.info("Codex review call started: runId={}, phase={}, etape={}",
        runId, reportPhase, stepNumber);

// Après l'appel Codex
log.info("Codex review call completed: runId={}, decision={}, durationMs={}",
        runId, reviewDirective, codexResult.durationMs());

// WAIT_HUMAN explicite
log.info("Review decision: WAIT_HUMAN — runId={}", runId);

// Timeout
log.warn("Codex review call timed out: runId={}, durationMs={}", runId, codexResult.durationMs());
```

**Ce qu'il ne faut pas logger** : `analysisPromptContent`, `analysisResultContent`, `waitReason` en clair (peut contenir du contenu de rapport).

---

### 4.6 `CorrectionStep`

Ajouter `@Slf4j`. Troisième appel Codex dans le cycle.

**Méthode `execute`**

```java
log.info("Codex correction call started: runId={}, phase={}, etape={}",
        runId, reportPhase, stepNumber);

log.info("Codex correction call completed: runId={}, success={}, durationMs={}",
        runId, codexResult.success(), codexResult.durationMs());

// Timeout
log.warn("Codex correction call timed out: runId={}", runId);
```

**Ce qu'il ne faut pas logger** : `analysisResultContent`, `reviewResultContent`, `correctionPrompt`.

---

### 4.7 `BuildErrorCorrectionStep`

Ajouter `@Slf4j`. Appel Codex spécifique à la correction d'erreur de build.

**Méthode `execute`**

```java
log.info("Codex build error correction started: runId={}, retryCount={}/{}, phase={}, etape={}",
        runId, retryCount, retryMax, reportPhase, stepNumber);

log.info("Codex build error correction completed: runId={}, success={}, durationMs={}",
        runId, codexResult.success(), codexResult.durationMs());

// Timeout
log.warn("Codex build error correction timed out: runId={}, durationMs={}", runId, codexResult.durationMs());
```

**Ce qu'il ne faut pas logger** : `buildErrorSummary` (peut contenir des stacktraces complètes), `buildPrompt`.

---

### 4.8 `MavenValidationStep`

Ajouter `@Slf4j`. C'est le point de validation le plus observable.

**Méthode `execute`**

```java
// Démarrage
log.info("Maven validation started: runId={}, phase={}, etape={}, timeoutSeconds={}",
        runId, reportPhase, stepNumber, timeoutSeconds);

// Succès
log.info("Maven validation passed: runId={}, durationMs={}",
        runId, buildResult.durationMs());

// Échec avec retry disponible
log.warn("Maven validation failed — correction will be attempted: runId={}, exitCode={}, retryCount={}/{}, durationMs={}",
        runId, buildResult.exitCode(), retryCount + 1, retryMax, buildResult.durationMs());

// Échec final sans retry
log.error("Maven validation failed — no retry: runId={}, exitCode={}, durationMs={}",
        runId, buildResult.exitCode(), buildResult.durationMs());

// Timeout
log.warn("Maven validation timed out: runId={}, durationMs={}", runId, buildResult.durationMs());
```

**Ce qu'il ne faut pas logger** : `stderr` complet (longueur en DEBUG si utile), `stdout` complet.

---

### 4.9 `CodexWorkflowClient`

Déjà `@Slf4j`. Enrichir les logs existants et ajouter un log INFO de démarrage.

**Méthode `execute`** — logs existants à enrichir :

```java
// Ajouter avant le lancement du processus (le log de fin existe déjà en DEBUG)
log.info("Codex CLI process starting: workingDirectory={}", safeWorkingDir);

// Enrichir le log DEBUG existant (exit code + durée) — déjà bien placé
log.debug("Codex CLI execution finished: exitCode={}, durationMs={}, stdoutLen={}, stderrLen={}",
        exitCode, durationMs, stdout.length(), stderr.length());

// Timeout (manquant actuellement)
log.warn("Codex CLI process timed out: durationMs={}", durationMs);

// Les log.error existants pour IOException et InterruptedException sont corrects.
```

`safeWorkingDir` = `request.workingDirectory() != null ? "[set]" : "[none]"` en INFO (pas le path absolu), ou le path en DEBUG.

---

### 4.10 `WorkflowValidationService`

Ajouter `@Slf4j`. Exécuteur de processus Maven sous-jacent.

**Méthode `validate`**

```java
// Avant démarrage
log.debug("Process validation starting: command={}, timeoutSeconds={}", commandString, timeoutSeconds);

// Après
log.debug("Process validation finished: status={}, exitCode={}, durationMs={}",
        result.status(), exitCode, durationMs);

// Timeout
log.warn("Process validation timed out: command={}, durationMs={}", commandString, durationMs);

// Erreur système
log.error("Process validation failed to start: command={}", commandString, e);
```

Note : `commandString` = `mvnw clean compile -DskipTests` — pas de secret, safe à logger.

---

### 4.11 `WorkflowProjectRegistryService`

Ajouter `@Slf4j`. Events de registre projet.

**Méthode `registerOrUpdate`**

```java
// Création
log.info("Workflow project registered: projectName={}, projectKey={}", projectName, projectKey);

// Mise à jour
log.info("Workflow project updated: projectName={}, projectKey={}", projectName, projectKey);

// Échec validation
log.warn("Workflow project registration failed: reason={}", reason);
```

**Méthode `lookup`**

```java
// Projet non trouvé
log.debug("Workflow project lookup: projectName={}, found={}", normalizedName.projectKey(), lookup.found());
```

**Ce qu'il ne faut pas logger** : `projectPath` absolu en INFO.

---

### 4.12 `WorkflowTelegramProjectService`

Ajouter `@Slf4j`. Façade Telegram pour le registre projet.

**Méthode `setProject`**

```java
// Succès enregistrement
log.info("Project set via Telegram: name={}, action={}", name, result.created() ? "created" : "updated");

// Échec
log.warn("Project set via Telegram failed: name={}, reason={}", name, sanitizedReason);
```

**Ce qu'il ne faut pas logger** : le path brut reçu en entrée (peut être invalide et contenir des données utilisateur), le résultat de `sanitizeReason`.

---

### 4.13 `WorkflowTelegramController`

Déjà `@Slf4j`. Enrichir avec les commandes importantes manquantes.

**Méthodes `workflowRun`, `workflowResume`**

```java
// /workflow_run
log.info("Command /workflow_run: chatId={}, userId={}, project={}, phase={}, etape={}",
        chatId, userId, projectName, phase, etape);

// /workflow_resume
log.info("Command /workflow_resume: chatId={}, userId={}", chatId, userId);
```

Les commandes `/workflow_status`, `/workflow_summary` ne nécessitent pas de log INFO (lecture seule, peu de valeur opérationnelle). Un `log.debug` suffit si nécessaire.

---

### 4.14 Classes à ne pas instrumenter (justification)

**`WorkflowTelegramSessionStore`** : store en mémoire pur. Les transitions (tryMarkRunning, completeRun, failRun) sont déjà loggées côté `WorkflowTelegramOrchestrationService` qui les appelle. Ajouter des logs ici créerait de la duplication.

**`WorkflowArtifactService`** : utilitaire I/O. Les erreurs remontent via `IllegalStateException` catchée dans les steps qui loggeront l'échec. Un log DEBUG sur `writeArtifact` serait acceptable mais non prioritaire.

**`WorkflowTelegramSummaryService`** : la commande `/workflow_summary` est une lecture. Un log DEBUG suffit si nécessaire. Le cas `tooLong` (envoi de fichier) mérite un log INFO si implémenté.

---

## 5. Règles de confidentialité des logs

### Règles générales

| Niveau | Paths absolus | Prompts Codex | Contenu stdout/stderr | Tokens/clés | Stacktrace |
|--------|--------------|--------------|----------------------|-------------|------------|
| INFO | ❌ Non | ❌ Non | ❌ Non | ❌ Non | ❌ Non |
| DEBUG | ✅ Acceptable | ❌ Non | Longueur seulement | ❌ Non | ❌ Non |
| WARN | ❌ Non | ❌ Non | ❌ Non | ❌ Non | ❌ Non |
| ERROR | ❌ Non | ❌ Non | ❌ Non | ❌ Non | ✅ Côté serveur uniquement |

### Risques identifiés et mitigations

**Fuite de paths absolus** — Risque : `workingDirectory`, `reportRootDirectory`, `projectPath` sont des paths absolus présents dans le contexte. Mitigation : ne logger que le nom de projet (`projectName`) en INFO. En DEBUG, le path est acceptable mais jamais en combinaison avec d'autres données sensibles.

**Fuite de prompts** — Risque : `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep` construisent des prompts qui incluent le contenu de fichiers source et de rapports. Mitigation : ne jamais passer `prompt`, `sourceContent`, `analysisResultContent`, `reviewResultContent` dans les logs. Logger uniquement la longueur en DEBUG si utile pour le diagnostic.

**Fuite de summary** — Risque : `WorkflowTelegramSummaryService` lit le fichier summary et l'envoie. Mitigation : ne logger que le chatId, le statut et le nombre de chunks, jamais le contenu.

**Fuite de tokens Telegram** — Risque : aucune des classes inspectées ne manipule directement les tokens (présents dans la configuration Spring). Risque faible. Mitigation : ne jamais logger les propriétés de configuration.

**Stacktraces trop verbeuses côté Telegram** — Risque : `WorkflowTelegramOrchestrationService.sanitizeForSession(RuntimeException e)` convertit déjà les exceptions en messages épurés avant de les stocker en session. La stacktrace complète doit rester côté serveur (log ERROR). Mitigation : déjà en place via `WorkflowTelegramErrorSanitizer`.

**Logs Telegram trop bavards** — Risque : logguer les commandes `/workflow_status` (appelées fréquemment en polling) générerait du bruit. Mitigation : INFO uniquement pour les commandes avec side-effects (`/workflow_run`, `/workflow_resume`, `/workflow_project_set`). DEBUG pour les commandes de lecture.

**`buildErrorSummary` dans les logs** — Risque : ce champ contient les 10 premières lignes de stderr Maven, qui peuvent inclure des paths de fichiers internes. Ne jamais le logger tel quel ; utiliser uniquement le retryCount en INFO.

---

## 6. Plan d'implémentation

Le plan est ordonné du plus visible (Telegram) vers le plus bas niveau (processus), pour que chaque étape soit indépendamment testable.

### Lot 1 — Migration et enrichissement de l'orchestration Telegram

Classes : `WorkflowTelegramOrchestrationService`

- Migrer `LoggerFactory.getLogger(...)` vers `@Slf4j`
- Ajouter `log.info` sur `startRun` (après résolution du target) et `resume`
- Enrichir `handleResult` avec `log.info` sur chaque décision finale
- Vérifier les `log.warn` et `log.error` existants

### Lot 2 — Logs des commandes Telegram importantes

Classes : `WorkflowTelegramController`, `WorkflowTelegramProjectService`, `WorkflowTelegramRoadmapService`

- Ajouter `log.info` sur `/workflow_run` et `/workflow_resume` dans le contrôleur
- Ajouter `@Slf4j` et `log.info` sur `setProject` dans `WorkflowTelegramProjectService`
- Enrichir le `log.debug` de `WorkflowTelegramRoadmapService` en `log.info`

### Lot 3 — Logs du runner et de l'orchestrateur

Classes : `AnalysisReviewWorkflowRunner`, `WorkflowOrchestrator`

- Ajouter `@Slf4j` sur les deux classes
- Logger les démarrages de cycle et les transitions de décision
- Logger les corrections déclenchées et les limites de retry atteintes

### Lot 4 — Logs des steps Codex

Classes : `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`, `BuildErrorCorrectionStep`

- Ajouter `@Slf4j` sur chaque step
- Logger démarrage et fin de chaque appel Codex (INFO)
- Logger timeout en WARN, exception en ERROR

### Lot 5 — Logs de la validation Maven

Classes : `MavenValidationStep`, `WorkflowValidationService`

- Ajouter `@Slf4j` sur les deux classes
- Logger démarrage et résultat de la validation Maven (INFO)
- Logger les cas de retry (WARN) et d'échec final (ERROR)

### Lot 6 — Logs du registre projet

Classes : `WorkflowProjectRegistryService`

- Ajouter `@Slf4j`
- Logger la création et la mise à jour de projets (INFO)
- Logger les échecs de validation (WARN)

### Lot 7 — Enrichissement de CodexWorkflowClient

Classes : `CodexWorkflowClient`

- Ajouter `log.info` au démarrage du processus (avant `processBuilder.start()`)
- Enrichir le `log.debug` existant avec `stdoutLen` et `stderrLen`
- Ajouter `log.warn` pour le timeout (actuellement absent)

### Lot 8 — Vérifications et non-régression

- Compilation propre avec tous les `@Slf4j` ajoutés
- Revue manuelle des messages pour détecter toute fuite de contenu sensible
- Vérification que les tests existants compilent et passent sans modification

---

## 7. Tests et vérifications

### Ce qui doit compiler et passer

Tous les tests existants doivent continuer à passer sans modification. L'ajout de `@Slf4j` et de `log.*` ne modifie pas le comportement métier.

### Vérifications manuelles obligatoires

Avant de livrer chaque lot, vérifier ligne par ligne :

- Aucun `log.*` n'inclut `prompt`, `sourceContent`, `analysisResultContent`, `reviewResultContent`, `correctionPrompt`, ou tout champ de type "contenu de fichier".
- Aucun `log.*` n'inclut de token ou de clé API.
- Les paths absolus ne sont pas présents dans les messages INFO.
- `buildErrorSummary` n'est pas loggé.

### Tests unitaires à ne pas écrire

Ne pas écrire de tests qui assertent la valeur exacte d'un message de log. Ces tests sont fragiles et cassent à chaque reformulation du message.

### Test unitaire acceptable (si ajouté)

Si un utilitaire `LogSanitizer` ou `SafeLogField` est introduit pour masquer automatiquement les paths ou le contenu, un test unitaire sur ce composant est approprié. Ce n'est pas prévu dans cette analyse (pas de nouveau composant), mais si un besoin émerge lors de l'implémentation, le test doit être ajouté dans le même lot.

### Vérification de la compilation Lombok

S'assurer que `lombok.jar` est bien déclaré en annotation processor dans le `pom.xml` (déjà le cas d'après les classes existantes avec `@Slf4j` et `@RequiredArgsConstructor`).

---

## 8. Risques

### Risque faible — régression de comportement

Les `log.*` ne modifient pas le flux métier. Risque très faible si on ne touche pas à la logique existante.

### Risque modéré — fuite accidentelle de données sensibles

Le risque principal est d'inclure par inadvertance un champ qui contient un prompt ou du contenu de fichier. La revue manuelle des messages (section 7) atténue ce risque.

### Risque faible — migration LoggerFactory → @Slf4j

`WorkflowTelegramOrchestrationService` utilise `LoggerFactory.getLogger(...)` manuellement. La migration vers `@Slf4j` est mécanique : supprimer le champ `log` et l'import, ajouter l'annotation. Les messages existants (`log.error`, `log.warn`) restent identiques.

### Risque faible — bruit dans les logs

Si les logs DEBUG sont activés en production, les appels à `WorkflowOrchestrator.executeSingleStep` seront répétés à chaque step. Ce n'est pas un problème si le niveau DEBUG n'est activé qu'à la demande (via `logging.level.*` dans la configuration Spring Boot).

---

## Conclusion

**PRÊT pour implémentation.**

L'analyse est complète et précise. Les 8 lots sont indépendants et progressifs. Chaque classe est traitée avec son niveau d'annotation, ses méthodes cibles, ses messages recommandés et ses contraintes de confidentialité.

Le risque d'impact sur les tests existants est minimal. Le risque de fuite de données sensibles est contrôlable par revue manuelle des messages.

Points clés à retenir pour l'implémentation :

- Priorité absolue au Lot 1 (migration LoggerFactory + logs INFO sur startRun/resume) : c'est ce qui rend le workflow exploitable en production en premier.
- Les steps Codex (Lots 3 et 4) sont la zone la plus opaque actuellement — leur instrumentation a le plus de valeur diagnostique.
- `buildErrorSummary`, `prompt`, et tout contenu de fichier ne doivent jamais apparaître dans un `log.*`.
