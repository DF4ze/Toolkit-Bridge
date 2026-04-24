Relecture appliquée.

**Ce que j’ai fait**
1. J’ai exécuté la relecture critique demandée sur le périmètre `workflow.runtime.model`.
2. J’ai appliqué des corrections minimales utiles, sans élargir le scope:
- cohérence `currentLotId` dans [WorkflowRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRun.java)
- garde-fous temporels dans [WorkflowRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRun.java) et [LotRun.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRun.java)
- tests ciblés ajoutés/étendus dans:
    - [WorkflowRunTest.java](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowRunTest.java)
    - [LotRunTest.java](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/LotRunTest.java)

3. J’ai généré le rapport demandé:
- [result_3_review.md](D:/Documents/Spring/Toolkit-Bridge/data/rapport/v1.1/Phase1/etape4/result_3_review.md)

**Validation**
- Tests exécutés: `LotRunTest, WorkflowRunTest, WorkflowExecutionContextTest`
- Résultat: `BUILD SUCCESS`, `9` tests passés, `0` échec.

**Périmètre respecté**
- Aucun ajout hors modèle runtime minimal.
- Aucun couplage Telegram/Codex/Maven/persistance/orchestrateur.