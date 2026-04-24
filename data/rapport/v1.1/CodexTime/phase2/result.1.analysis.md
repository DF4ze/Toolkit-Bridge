# Phase 2 - Etape 1 - Analyse d'introduction d'un orchestrator minimal

## 1) Package recommande

Package recommande: `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator`

Pourquoi:
- coherence avec le decoupage existant de `runtime` (`model`, `step`, `codex`, `artifact`)
- evite la confusion avec l'autre systeme d'orchestrator deja present (`service.agent.orchestrator` pour les agents CHAT/TASK)
- garde la responsabilite "pilotage d'execution workflow runtime" au meme niveau que les contrats `WorkflowStep`

## 2) Classes a creer / modifier

### A creer
- `WorkflowOrchestrator` (classe concrete, minimaliste, annotable `@Service` au moment de l'implementation)
- `WorkflowOrchestratorTest` (tests unitaires de comportement minimal)

### A ne pas modifier a ce stade (recommande)
- `WorkflowStep`
- `WorkflowStepResult`
- `WorkflowStepDecision`
- `WorkflowExecutionContext`
- `WorkflowRun`, `LotRun`, et statuts associes

Justification:
- les contrats minimaux sont deja suffisants pour executer une step et interpreter une decision
- toute extension de contrat maintenant pousserait a la sur-conception

## 3) Responsabilite exacte

### Ce que l'orchestrator minimal doit faire
- recevoir un `WorkflowExecutionContext`
- recevoir une `WorkflowStep`
- appeler `step.execute(context)` exactement une fois
- recuperer un `WorkflowStepResult`
- interpreter minimalement `decision` pour les 3 cas cibles:
  - `CONTINUE`
  - `WAIT_HUMAN`
  - `STOP_FAILURE`
- renvoyer le `WorkflowStepResult` au caller (avec un comportement strict en cas de resultat invalide)

### Ce qu'il ne doit surtout pas faire
- enchainer plusieurs steps
- implementer une boucle
- gerer retry/correction
- executer des transitions globales de workflow
- appeler directement Codex, Telegram, Maven, ou parser des roadmaps
- introduire une strategie/routing/plugin system

## 4) Dependances minimales

### Dependances directes recommandees
- `WorkflowExecutionContext`
- `WorkflowStep`
- `WorkflowStepResult`
- `WorkflowStepDecision`

### Dependances a ne pas absorber dans cette etape
- `CodexWorkflowClient` (doit rester dans les steps metier)
- `WorkflowArtifactService` (doit rester dans les steps metier)
- couches admin/web/persistence
- orchestrateurs agent CHAT/TASK (`service.agent.orchestrator`)

## 5) Rapport a WorkflowStep (point D)

Appel recommande sans enrichissement du contrat:
- signature cible de methode orchestrator (proposition):
  - `WorkflowStepResult executeSingleStep(WorkflowExecutionContext context, WorkflowStep step)`

Comportement minimal:
- validation defensive: `context != null`, `step != null`
- appel direct: `WorkflowStepResult result = step.execute(context)`
- si `result == null` -> erreur explicite (`IllegalStateException`) pour garder un contrat strict

Aucun besoin d'ajouter une nouvelle interface, ni un wrapper de step, ni un bus d'execution.

## 6) Rapport a WorkflowStepResult (point E)

Interpretation minimale recommandee:
- `CONTINUE`: execution terminee normalement pour cette unique step
- `WAIT_HUMAN`: execution stoppee volontairement en attente humaine
- `STOP_FAILURE`: execution stoppee en echec

Traitement minimal conseille:
- switch explicite sur `result.decision()`
- accepter uniquement ces 3 decisions dans cette etape
- pour les autres decisions existantes (`RETRY_CORRECTION`, `FINISH`): lever une erreur claire de "non supporte en Phase 2 - Etape 1"

Ce garde-fou evite d'introduire implicitement un mini moteur de transition.

## 7) Rapport au contexte runtime (point F)

Recommendation nette: ne pas modifier `WorkflowExecutionContext` dans cette etape.

Raisons:
- `WorkflowExecutionContext` est un `record` immutable (pas de mutation structurelle)
- vouloir reconstruire `WorkflowRun`/`LotRun` maintenant ajouterait une complexite prematuree
- l'objectif de l'etape est l'orchestration minimale d'une seule step, pas la gestion de lifecycle runtime complet

Conclusion pratique:
- l'orchestrator lit le contexte et le transmet tel quel a la step
- l'eventuelle mise a jour d'etat global sera traitee dans une etape suivante dediee

## 8) Risques de sur-conception a eviter

- creer une state machine des maintenant
- introduire des interfaces generiques type `StepRouter`, `TransitionPolicy`, `ExecutionPipeline`
- gerer des collections de steps ou un graphe de transitions
- absorber des concerns metier (prompting, artefacts, I/O technique) dans l'orchestrator
- anticiper des cas futurs (multi-lots, retries, human-loop riche) dans la structure de base

## 9) Plan d'implementation minimal (sobre)

1. Creer `WorkflowOrchestrator` dans `...runtime.orchestrator` avec une seule methode publique `executeSingleStep(...)`.
2. Ajouter validations d'entree (`context`, `step`) et de sortie (`result != null`).
3. Ajouter un `switch` strict sur `WorkflowStepDecision`:
   - `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE` -> retour du `result`
   - default -> erreur explicite "decision non supportee a ce stade".
4. Ecrire des tests unitaires minimaux:
   - execute la step et retourne le resultat
   - refuse context null
   - refuse step null
   - refuse result null
   - refuse decision hors perimetre minimal

## 10) Perimetre a laisser volontairement absent (point G)

Doit rester hors de cette etape:
- multi-step
- retry
- multi-lots
- orchestration complete
- Telegram
- Maven
- questions humaines avancees
- parsing de roadmap

Ajouts egalement a exclure maintenant:
- moteur generique de transitions
- pipeline d'execution
- strategie de routing
- architecture pluginable

## 11) Lecture de l'existant qui justifie ce design

Contrats deja suffisants observes dans le code:
- `WorkflowStep` expose deja `execute(WorkflowExecutionContext)`
- `WorkflowStepResult` porte deja `decision + message + data`
- `WorkflowExecutionContext` impose deja un socle runtime minimal et defensif

Donc la plus petite evolution utile est bien: une couche d'appel + interpretation stricte, sans enrichir les modeles.
