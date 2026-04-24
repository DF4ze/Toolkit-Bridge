# Phase 2 - Etape 2 - Analyse d'introduction de `GlobalAnalysisStep`

## 1. Package recommande

Package recommande:
- `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step`

Justification:
- `GlobalAnalysisStep` est une implementation directe de `WorkflowStep`.
- Evite de multiplier les couches (`impl`, `engine`, etc.) trop tot.
- Reste coherent avec `WorkflowStep`, `WorkflowStepDecision`, `WorkflowStepResult` deja dans ce package.

## 2. Classes a creer ou modifier

### A creer
- `GlobalAnalysisStep` (classe concrete qui implemente `WorkflowStep`)
- `GlobalAnalysisStepTest` (tests unitaires)

### A ne pas modifier dans cette etape
- `WorkflowExecutionContext`
- `WorkflowStep`
- `WorkflowStepResult`
- `WorkflowOrchestrator`
- modeles runtime (`WorkflowRun`, `LotRun`)
- clients/services existants (`CodexWorkflowClient`, `WorkflowArtifactService`)

## 3. Responsabilite exacte de `GlobalAnalysisStep`

### Ce que la step doit faire
1. Lire les donnees minimales dans `WorkflowExecutionContext`.
2. Construire un prompt d'analyse globale simple (texte brut, sans moteur de template avance).
3. Executer Codex via `CodexWorkflowClient`.
4. Ecrire 2 artefacts via `WorkflowArtifactService`:
   - prompt (`ANALYSIS_PROMPT`)
   - resultat (`ANALYSIS_RESULT`)
5. Retourner un `WorkflowStepResult`.

### Ce qu'elle ne doit pas faire
- Orchestrer plusieurs steps.
- Decider du flux global du workflow.
- Gerer retry/review/correction.
- Devenir un moteur de prompting generique.
- Appeler Telegram/Maven/administration/web.

## 4. Dependances minimales

Dependances directes recommandees:
- `WorkflowExecutionContext`
- `CodexWorkflowClient`
- `WorkflowArtifactService`
- `WorkflowArtifactType`
- JDK (`Path`, `Files`) si lecture d'une source documentaire par chemin

Dependances a ne pas absorber:
- `WorkflowOrchestrator` (la step ne doit pas piloter le runtime)
- autre orchestrator du projet (`service.agent.orchestrator`)
- couches policy/memory/workspace non necessaires
- integration Telegram/Maven

## 5. Rapport au contexte runtime (reponse nette demandee)

### Conclusion retenue: **(3) le contexte actuel suffit si des conventions simples sont imposees**.

Le `WorkflowExecutionContext` actuel (`workflowRun`, `lotRun`, `variables`) est assez flexible.
Il faut toutefois fixer un **petit contrat de variables obligatoires** pour `GlobalAnalysisStep`.

Variables minimales conseillees (dans `context.variables()`):
- `reportRootDirectory` (String ou Path): racine ex. `data/rapport`
- `reportVersion` (String): ex. `v1.1`
- `reportPhase` (String): ex. `CodexTime/Phase2`
- `stepNumber` (Integer): ex. `2`
- `analysisSourcePath` (String ou Path): document source a analyser (ex. workflow de reference)

Variables optionnelles utiles:
- `codexWorkingDirectory` (String/Path)
- `codexTimeoutSeconds` (Integer)

Pourquoi cette option est la plus sobre:
- aucune evolution du modele runtime requise maintenant
- aucun parseur de `targetStepRef` necessaire
- pas d'usine a gaz, juste un contrat clair de cles

## 6. Rapport aux artefacts

Recommandation simple:
- la logique de nommage/path reste centralisee dans `WorkflowArtifactService.buildArtifactPath(...)`
- la step appelle seulement:
  - `buildArtifactPath(..., ANALYSIS_PROMPT)` puis `writeArtifact(...)`
  - `buildArtifactPath(..., ANALYSIS_RESULT)` puis `writeArtifact(...)`

Donc:
- la step pilote le "quoi ecrire"
- `WorkflowArtifactService` garde le "ou/comment ecrire"

## 7. Rapport a Codex

### Ce qui revient a la step
- construire le prompt final
- construire `CodexExecutionRequest`
- interpreter le `CodexExecutionResult` au niveau metier de la step
- transformer ce resultat en artefact + `WorkflowStepResult`

### Ce qui doit rester dans `CodexWorkflowClient`
- execution processus CLI
- timeout/process lifecycle
- collecte stdout/stderr

### Ce qui doit rester hors de la step
- orchestration globale
- gestion multi-step/retry
- politiques de workflow transverses

## 8. `WorkflowStepResult` recommande (version 1)

Recommendation concrete et minimale:
- cas nominal (`CodexExecutionResult.success == true`):
  - `decision = CONTINUE`
  - `message = "Global analysis completed"`
  - `data` contient au minimum:
    - `promptArtifactPath`
    - `resultArtifactPath`
    - `codexExitCode`
    - `codexDurationMs`

- cas echec technique (exception codex, timeout, write artifact impossible, variables obligatoires manquantes):
  - `decision = STOP_FAILURE`
  - `message` explicite et courte
  - `data` optionnelle avec details utiles (`errorType`, `timedOut`, `stderr`)

`WAIT_HUMAN` peut rester non utilise dans cette v1 pour garder la step simple.

## 9. Risques de sur-conception

Risques principaux:
- transformer la step en mini orchestrator (gestion de transitions)
- introduire un mini moteur de template/prompt multi-formats
- ajouter trop d'abstractions des maintenant (router/strategy/pipeline)
- dupliquer la logique de path documentaire hors `WorkflowArtifactService`

Garde-fous:
- une seule methode `execute(context)`
- prompt en construction locale simple
- contrat de variables explicite et restreint
- aucune logique de boucle/retry

## 10. Plan d'implementation minimal (sobre)

1. Creer `GlobalAnalysisStep` (constructeur avec `CodexWorkflowClient` + `WorkflowArtifactService`).
2. Lire/valider les variables obligatoires du contexte.
3. Lire le document source (`analysisSourcePath`) et construire le prompt brut.
4. Appeler `codexWorkflowClient.execute(request)`.
5. Ecrire prompt + resultat via `WorkflowArtifactService`.
6. Retourner `WorkflowStepResult` (`CONTINUE` ou `STOP_FAILURE`).
7. Ajouter tests unitaires cibles.

## 11. Ce qui doit rester volontairement absent

- boucle de correction
- multi-step
- parsing intelligent de roadmap
- decisions humaines avancees
- validation Maven
- Telegram
- logique d'orchestration globale
- moteur de prompt generique

## 12. Recommandation finale

`GlobalAnalysisStep` doit rester une **step metier unique**:
- lit un contexte minimal conventionne
- produit un prompt
- execute Codex
- ecrit 2 artefacts
- retourne un resultat simple

Rien de plus a ce stade.