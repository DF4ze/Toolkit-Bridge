# Phase 8 — CodexCall — Étape 4 — Analyse — Injection `codexWorkingDirectory`

## Résumé exécutif

L’injection de `codexWorkingDirectory=<projectPath>` est faisable de manière minimale dans `WorkflowTelegramOrchestrationService` lors de la construction du `WorkflowExecutionContext` (RUN et RESUME), **sans** modifier `CodexWorkflowClient` ni l’appel CLI Codex.

Les steps Codex lisent déjà la variable `codexWorkingDirectory` depuis `WorkflowExecutionContext.variables` et acceptent une valeur **`Path` ou `String`**. La recommandation est d’injecter un **`Path`** (le `projectPath` déjà résolu en étape 3) dans le contexte, pour RUN et RESUME.

Conclusion : **prêt pour implémentation**.

---

## 1) Analyse du contexte RUN

### État actuel (construction)

Dans `WorkflowTelegramOrchestrationService`, le contexte RUN est construit par :

- `buildContext(runId, chatId, userId, botId, target, roadmapPath)`

Variables injectées aujourd’hui :

- `reportRootDirectory` (Path)
- `reportVersion` (String)
- `reportPhase` (String, ex: `phase7/etape3`)
- `stepNumber` (Integer)
- `analysisSourcePath` (Path) — **RUN uniquement**
- `roadmapPath` (Path) — si présent
- `llmProvider` / `llmModel` (String) — si résolus via `resolveAgentLlmConfiguration(botId)`

### Point d’injection recommandé

Ajouter :

```java
variables.put("codexWorkingDirectory", projectPath);
```

où `projectPath` est le chemin projet déjà résolu en étape 3 (et validé avant de démarrer le run).

### Source de `projectPath`

Contrainte : **ne pas refaire de lookup DB** dans `buildContext`.

Recommandation (simple et robuste) :

- résoudre le `projectPath` avant d’entrer dans l’exécution async (déjà fait en étape 3),
- puis **le passer** à `executeRun(...)` et à `buildContext(...)` (signature ajoutée), plutôt que de re-consulter le store dans `buildContext`.

Garde défensive recommandée (sans changer le comportement fonctionnel) :

- si `projectPath == null` au moment de construire le contexte : throw/stopFailure (mais normalement impossible car le run est refusé avant).

---

## 2) Analyse du contexte RESUME

### État actuel (construction)

Le contexte RESUME est construit par :

- `buildResumeContext(runId, chatId, userId, target)`

Variables injectées aujourd’hui :

- `reportRootDirectory`
- `reportVersion`
- `reportPhase`
- `stepNumber`

Le resume n’injecte pas `analysisSourcePath` (cohérent : variable RUN-only).

### Point d’injection recommandé

Ajouter :

```java
variables.put("codexWorkingDirectory", projectPath);
```

Même recommandation que RUN :

- le `projectPath` doit venir de la session (déjà résolu/validé avant le lancement du resume),
- et être **passé** explicitement à `executeResume(...)` puis à `buildResumeContext(...)` (signature ajoutée), sans lookup DB.

---

## 3) Vérification des steps Codex (lecture de `codexWorkingDirectory`)

Les steps Codex déclarent déjà la clé `codexWorkingDirectory` :

- `GlobalAnalysisStep`
- `GlobalReviewStep`
- `CorrectionStep`
- `BuildErrorCorrectionStep`

Exemple de comportement (constaté dans `GlobalAnalysisStep`) :

- lecture via `optionalPath(context, "codexWorkingDirectory")`
- conversion : accepte `Path` directement, ou `String` convertible en `Path`
- si absent : working directory non forcé (fallback)

=> Injecter un `Path` est compatible et évite tout problème de parsing/normalisation côté step.

---

## 4) Vérification `CodexExecutionRequest`

`CodexExecutionRequest` contient déjà :

- `Path workingDirectory`

Les steps construisent déjà :

```java
new CodexExecutionRequest(prompt, optionalPath(context, "codexWorkingDirectory"), optionalInt(...))
```

=> l’injection dans le contexte suffit pour propager le working directory jusqu’au client, sans changement de contrat.

---

## 5) Vérification `CodexWorkflowClient`

`CodexWorkflowClient` applique déjà le working directory si présent :

- `ProcessBuilder.directory(request.workingDirectory().toFile())`

=> aucune modification nécessaire en étape 4.
=> l’optimisation `codex exec --cd ... -` reste bien **pour l’étape 5**.

---

## 6) Forme de la variable recommandée

Recommandation : **Option A (Path)**.

```java
variables.put("codexWorkingDirectory", projectPath);
```

Justification :

- les steps utilisent déjà `optionalPath(...)` et supportent `Path`
- évite toute ambiguïté `String` vs `Path`
- évite de dépendre du parsing `Path.of(String)` côté step

---

## 7) Tests recommandés

### Tests orchestration (service)

- `/workflow_run project=Known` : le contexte transmis au runner contient `codexWorkingDirectory` (Path)
- `/workflow_run` sans `project` mais session déjà complète : injecte `codexWorkingDirectory`
- `/workflow_resume` : injecte `codexWorkingDirectory`

### Tests de non-régression variables

- RUN : `analysisSourcePath` reste présent
- RESUME : `analysisSourcePath` reste absent
- `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber` inchangés

### Tests non-fuite Telegram

- aucun message Telegram (dashboard/status/erreurs) n’affiche `projectPath`

---

## 8) Risques identifiés

- injection au mauvais type (`String` vs `Path`) : mitigé en injectant `Path`
- `projectPath` null (NPE) : mitigé par la résolution étape 3 + garde défensive légère
- fuite de path absolu via logs/UX : mitigé en n’affichant jamais la valeur en Telegram (déjà règle existante)
- confusion `reportRootDirectory` vs `codexWorkingDirectory` : mitigé en gardant la clé explicite `codexWorkingDirectory`

---

## 9) Plan d’implémentation (4–6 étapes)

1. Ajouter `projectPath` (Path) comme paramètre à `executeRun(...)` et `buildContext(...)`
2. Injecter `variables.put("codexWorkingDirectory", projectPath)` dans `buildContext(...)`
3. Ajouter `projectPath` (Path) comme paramètre à `executeResume(...)` et `buildResumeContext(...)`
4. Injecter `variables.put("codexWorkingDirectory", projectPath)` dans `buildResumeContext(...)`
5. Ajouter/adapter tests d’orchestration pour valider l’injection (RUN + RESUME) et la non-régression

