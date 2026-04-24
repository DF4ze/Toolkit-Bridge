Analyse uniquement réalisée, sans modification de code.

**Constat Existant**
- Le runtime minimal est déjà en place dans le package `model` :
  [WorkflowRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRun.java), [LotRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRun.java), [WorkflowExecutionContext.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowExecutionContext.java).
- Il n’y a pas encore de package `step`, ce qui laisse une place propre pour ce lot.

**Placement Recommandé**
- `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step`
- Raison : séparer clairement le contrat d’exécution d’étape (`step`) des objets d’état (`model`), sans créer de nouvelle couche d’orchestration.

**Contrat Minimal Proposé**
1. `WorkflowStep` (interface)
- Méthode unique : `execute(WorkflowExecutionContext context) -> WorkflowStepResult`
- Aucun état interne, aucune méthode par défaut “intelligente”.

2. `WorkflowStepDecision` (enum)
- `CONTINUE`
- `RETRY_CORRECTION`
- `WAIT_HUMAN`
- `STOP_FAILURE`
- `FINISH`

3. `WorkflowStepResult` (objet simple)
- `decision` obligatoire
- `message` optionnel
- `data` optionnelle en `Map<String, Object>` défensive
- Validations minimales : `decision != null`, `Map.copyOf(...)`

**Points d’Architecture Sensibles**
- Ne pas introduire de notions de “next step”, ordre, pipeline, transition.
- Ne pas enrichir `WorkflowStepResult` avec des champs métier (retry count, routing, etc.) à ce stade.
- Garder `WorkflowStep` purement contractuel, sans dépendance externe.

**Tests Minimaux Recommandés**
- `WorkflowStepResultTest`
- Vérifie construction nominale, `decision` obligatoire, immutabilité défensive de `data`.
- Éventuellement un test dummy local d’implémentation `WorkflowStep` retournant un résultat statique.

**Ce qui serait implémenté au lot (si on passe en exécution)**
- `WorkflowStep`
- `WorkflowStepDecision`
- `WorkflowStepResult`
- 1 à 2 tests unitaires ciblés

**Ce qui reste volontairement absent**
- `WorkflowRunner`
- orchestrator / gestion de séquence
- transitions automatiques
- retry logic
- lien Codex/Telegram/Maven
- steps métier concrets

**Étape suivante (future)**
- Introduire une orchestration très légère qui consomme ce contrat, sans framework générique.