# Phase 7 — Étape 4 — Analyse : WAIT_HUMAN → `/workflow_resume`

## 0) Base existante (ancrage)

Telegram (déjà en place) :
- `WorkflowTelegramController` : commandes `/workflow_run`, `/workflow_status`, `/workflow_summary`
- `WorkflowTelegramOrchestrationService` : exécution async + timeout + mapping du résultat vers session
- `WorkflowTelegramSessionStore` / `WorkflowTelegramSession` : état in-memory par chat (inclut `lastStatus`, `running`, `lastSummaryPath`, `phase`, `etape`)

Runner (intouchable) :
- Support explicite RUN vs RESUME dans `AnalysisReviewWorkflowCli` :
  - RUN → `runner.runAnalysisReviewWithOptionalCorrection(context)`
  - RESUME → `runner.runCorrectionAfterReview(context)`
- La reprise (RESUME) exécute `CorrectionStep` (single step) qui dépend d’artefacts existants :
  - `result.<step>.analysis.md`
  - `result.<step>.review.md`

## 1) Quand considérer qu’un resume est possible ?

Source de vérité : `WorkflowTelegramSession.lastStatus`.

Cas valide (resume autorisé) :
- `lastStatus == WAITING_HUMAN`
- ET `running == false` (pas déjà en cours)
- ET `phase` et `etape` non null / > 0 (sinon contexte incomplet)

Cas invalides (resume refusé) :
- `RUNNING` : un run est en cours → refuser (message clair)
- `FAILED` : pas en “attente humaine” (sauf future feature “retry” hors scope) → refuser
- `IDLE` : aucun workflow en cours → refuser
- `COMPLETED` : pas de reprise à faire → refuser

Message UX recommandé (conforme prompt) :
- `❌ Aucun workflow en attente d'action humaine` (et optionnellement rappeler `/workflow_status`)

## 2) Où stocker les infos nécessaires au resume ?

Déjà disponibles en session :
- `phase`, `etape` : nécessaires pour reconstruire `reportPhase` + `stepNumber`
- `lastStatus` : gating
- `running` + `lastRunId` : lock / cohérence
- `projectName` : informatif (pas strictement nécessaire au runner RESUME)
- `lastSummaryPath` : utile côté UX, pas nécessaire pour RESUME

Ce qui semble **ne pas devoir être ajouté** :
- `reviewResultPath` : le runner calcule ce path de manière déterministe via `WorkflowArtifactService.buildArtifactPath(...)`
- “params CLI” : inutile si on fait l’appel Java direct (recommandé)
- “état de correction” : hors scope (le runner renvoie déjà `correctionTriggered`)

Conclusion : **pas besoin d’étendre `WorkflowTelegramSession`** pour RESUME, tant que `phase` et `etape` restent cohérents.

## 3) Comment appeler le resume ?

Option A — CLI :
- techniquement possible, mais ajoute une dépendance process/IO et du câblage “outil” inutile côté orchestration Telegram.

Option B — Java direct (recommandé) :
- `runner.runCorrectionAfterReview(context)`
- cohérent avec le design existant de `WorkflowTelegramOrchestrationService` (appel runner en-process)
- évite de manipuler des arguments CLI (contrainte “ne pas modifier CLI” respectée)

## 4) Différences RUN vs RESUME (paramètres et prérequis)

### RUN
Variables attendues (comme aujourd’hui dans Telegram) :
- `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`
- **+** `analysisSourcePath` (RUN uniquement)

Pré-requis fichiers :
- source `X.analysis.md` existe (le runner le lit via `analysisSourcePath`)

### RESUME
Variables attendues :
- `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`
- **pas** besoin de `analysisSourcePath`

Pré-requis fichiers :
- `result.<step>.analysis.md` existe
- `result.<step>.review.md` existe et a été édité par l’humain

Erreurs possibles (observables sans modifier runner) :
- artefact manquant → le runner renvoie `STOP_FAILURE` avec message “Missing required artifact: …”
- review non modifiée / décision toujours WAIT_HUMAN → selon contenu, le workflow peut rester en attente (ou échouer). C’est acceptable : le mapping session doit refléter le résultat.

## 5) Commande Telegram à ajouter : `/workflow_resume`

Emplacement :
- `WorkflowTelegramController` : ajouter `@Command("/workflow_resume")` qui délègue à l’orchestration (ou à un service “resume” dédié).

Comportement attendu :
- valider la session (`WAITING_HUMAN` uniquement)
- verrouiller via `tryMarkRunning(chatId, newRunId)`
- lancer async : `executeResume(chatId, runId, userId, sessionSnapshot)`
- retourner `🚀 Resume lancé`

Message d’erreur attendu :
- `❌ Aucun workflow en attente d'action humaine`
- et éventuellement `❌ Workflow already running` si lock

## 6) Gestion du lock (anti double-resume / anti concurrence)

Réutiliser strictement :
- `WorkflowTelegramSessionStore.tryMarkRunning(chatId, runId)`

Règles :
- si `running == true` → refuser (pas de “resume parallèle”)
- en cas de timeout/exceptions : `failRun(chatId, runId, ...)` (comme `/workflow_run`)

## 7) UX utilisateur (messages)

Succès (immédiat) :
- `🚀 Resume lancé`

Refus :
- `❌ Aucun workflow en attente d'action humaine`
- `❌ Workflow already running`

Option utile (sans parsing) :
- rappeler où éditer : afficher le path du review artifact si disponible (peut être construit déterministiquement) — à discuter en implémentation, mais attention au scope (ça reste de l’UX, pas de parsing).

## 8) Cas d’erreur (à couvrir sans complexité)

1) Session incohérente
- `phase`/`etape` manquants alors que `WAITING_HUMAN` → refuser (message “invalid request” / “context missing”)

2) Artefacts manquants
- laisser le runner produire `STOP_FAILURE`, et mapper la session à `FAILED` + `lastError`

3) Review non modifiée / toujours WAIT_HUMAN
- la reprise peut re-produire `WAIT_HUMAN` (ou échouer) → mapper en `WAITING_HUMAN` et conserver les paths (dont summary)

4) Timeout resume
- même mécanique que RUN (timeout → `failRun`)

## 9) Impact architecture

### Orchestration service
- ajout d’un “resume pipeline” parallèle à `startRun(...)`, mais réutilisant :
  - `tryMarkRunning`
  - exécution async + timeout
  - `handleResult(...)` existant (si compatible)

Point d’attention :
- ne pas dupliquer tout le code : idéalement factoriser un builder de `WorkflowExecutionContext` paramétré (RUN vs RESUME) sans toucher au runner.

### Session store
- pas besoin de nouveaux champs.
- réutilisation de `completeRun(...)` / `failRun(...)` et statuts existants.

### Runner
- aucune modification.

## 10) Tests (unitaires, ciblés)

Objectif : couvrir le comportement Telegram sans runner modifié.

Tests recommandés :
- resume OK :
  - session `WAITING_HUMAN`
  - `tryMarkRunning` true
  - runner renvoie `CONTINUE` → session `COMPLETED`, `lastSummaryPath` mis à jour
- resume sans WAITING_HUMAN :
  - `IDLE/FAILED/RUNNING/COMPLETED` → message de refus, pas d’exécution
- double resume :
  - `tryMarkRunning` false → message “already running”
- mapping résultat :
  - `STOP_FAILURE` → session `FAILED`
  - `WAIT_HUMAN` → session `WAITING_HUMAN`

## 11) Plan d’implémentation (5–10 étapes)

1) Identifier les variables minimales pour RESUME (sans `analysisSourcePath`) et les prérequis artefacts.
2) Ajouter `@Command("/workflow_resume")` dans `WorkflowTelegramController` (délégation à orchestration/service).
3) Ajouter une méthode `resume(...)` dans `WorkflowTelegramOrchestrationService` (ou service dédié) :
   - vérifier session `WAITING_HUMAN`
   - `tryMarkRunning`
4) Construire `WorkflowExecutionContext` RESUME (mêmes `reportRootDirectory/reportVersion/reportPhase/stepNumber`) et appeler `runner.runCorrectionAfterReview(context)` dans l’executor.
5) Réutiliser `handleResult(...)` pour mapper le résultat en `COMPLETED/WAITING_HUMAN/FAILED` + paths.
6) Ajouter tests unitaires d’orchestration (runner mocké) pour : OK / refus / double / mapping.
7) Vérifier non-régression : `/workflow_run`, `/workflow_status`, `/workflow_summary` inchangés.

