Implémentation terminée, strictement dans le périmètre demandé.

**Fichiers ajoutés**
- [WorkflowRunStatus.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRunStatus.java)
- [LotRunStatus.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRunStatus.java)
- [LotRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRun.java)
- [WorkflowRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRun.java)
- [WorkflowExecutionContext.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowExecutionContext.java)
- [LotRunTest.java](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRunTest.java)
- [WorkflowRunTest.java](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRunTest.java)
- [WorkflowExecutionContextTest.java](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowExecutionContextTest.java)

**Ce que j’ai implémenté**
- Enums minimales demandées:
    - `WorkflowRunStatus`: `NEW`, `RUNNING`, `WAITING_HUMAN`, `FAILED`, `COMPLETED`
    - `LotRunStatus`: `PENDING`, `IN_PROGRESS`, `NEEDS_CORRECTION`, `BLOCKED_BY_QUESTION`, `VALIDATED`, `FAILED`
- Modèles runtime minimaux:
    - `WorkflowRun` avec `runId`, `workflowType`, `targetStepRef`, `status`, `lotRuns`, `currentLotId`, timestamps, `context`
    - `LotRun` avec `lotId`, `lotCode`, `executionOrder`, `status`, `correctionAttemptCount`, `artifactReferences`, timestamps, `summary`
    - `WorkflowExecutionContext` comme transport technique: `workflowRun`, `lotRun`, `variables`
- Style aligné projet:
    - validations minimales constructeur
    - defaults sobres (`NEW`, `PENDING`, maps/listes vides)
    - copies défensives (`List.copyOf`, `Map.copyOf`)
    - normalisation légère (`trim`, blank -> `null` pour champs optionnels)

**Tests exécutés**
- Commande: `-Dtest=LotRunTest,WorkflowRunTest,WorkflowExecutionContextTest test`
- Résultat: `BUILD SUCCESS`, `6` tests passés, `0` échec.

**Relecture critique**
- Point fort: le modèle reste volontairement simple et lisible, sans logique moteur ni couplage externe.
- Point d’attention: `targetStepRef` est obligatoire (choix de sobriété pour éviter un run partiellement défini).
- Point d’attention: `currentLotId` est volontairement non validé contre la liste des lots pour éviter une logique de navigation prématurée.

**Ce que je n’ai volontairement pas implémenté**
- Aucun `WorkflowStep`
- Aucun orchestrator/service runtime
- Aucun moteur de transition
- Aucun couplage Telegram/Codex/Maven
- Aucune persistance DB
- Aucun lien fonctionnel avec `Task`
- Aucune logique de parsing/fichiers workspace/roadmap

**À prévoir pour l’étape suivante**
1. Définir où et comment mettre à jour `status`, `currentLotId`, `updatedAt` côté orchestration (dans un service dédié futur).
2. Clarifier la convention métier de `targetStepRef` (format attendu).
3. Décider si `currentLotId` doit rester ID ou devenir index (toujours simple, sans navigation complexe).