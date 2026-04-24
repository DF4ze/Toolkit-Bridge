# Rapport d’analyse — Phase 4 Étape 1 — Enchaînement explicite à 3 steps

## 1) Recommandation d’évolution de l’orchestrator

Recommandation nette :
- faire évoluer **`WorkflowOrchestrator`** avec une **nouvelle méthode dédiée à 3 steps**,
- ne pas créer de nouvelle classe ni abstraction de séquence.

Motif :
- cohérence avec l’existant (`executeSingleStep`, `executeTwoSteps`),
- évolution minimale et lisible,
- zéro dérive vers un moteur générique.

## 2) Package / emplacement (A)

- Conserver l’évolution dans :
  - `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator`

Pas de classe intermédiaire type `WorkflowSequence`.

## 3) Responsabilité exacte (B)

Cette version 3 steps doit :
1. exécuter step 1,
2. si résultat `CONTINUE`, exécuter step 2,
3. si résultat `CONTINUE`, exécuter step 3,
4. arrêter immédiatement sur `WAIT_HUMAN` ou `STOP_FAILURE`,
5. retourner un `WorkflowStepResult` final.

Elle ne doit pas :
- router dynamiquement,
- gérer une liste arbitraire de steps,
- implémenter retry/correction conditionnelle,
- interpréter le contenu métier des artefacts,
- introduire state machine ou transitions configurables.

## 4) Signature recommandée (C)

Signature simple recommandée :

```java
public WorkflowStepResult executeThreeSteps(
        WorkflowExecutionContext context,
        WorkflowStep firstStep,
        WorkflowStep secondStep,
        WorkflowStep thirdStep
)
```

Principe clé :
- réutiliser `executeSingleStep(...)` pour centraliser la validation des décisions supportées.

## 5) Gestion des décisions (D)

Règle stricte :
1. Exécuter step 1.
2. Si step 1 != `CONTINUE` -> retour immédiat de step 1.
3. Exécuter step 2.
4. Si step 2 != `CONTINUE` -> retour immédiat de step 2.
5. Exécuter step 3.
6. Retour du résultat de step 3.

Donc :
- la step suivante ne démarre **que** si la précédente est `CONTINUE`,
- `WAIT_HUMAN` et `STOP_FAILURE` sont bloquants immédiats,
- la décision finale est le dernier résultat exécuté.

## 6) Rapport au contexte runtime (E)

Réponse nette : **3. le contexte actuel suffit tel quel pour cette étape, avec limites assumées**.

- Le même `WorkflowExecutionContext` est transmis aux 3 steps.
- Limites assumées : variables stringly-typed (`Map<String,Object>`) et conventions implicites.
- Aucun changement de modèle nécessaire à cette étape.

## 7) Rapport aux résultats intermédiaires (F)

Réponse nette :
- les steps suivantes doivent **principalement s’appuyer sur les artefacts** déjà écrits,
- pas sur le `data` des `WorkflowStepResult` précédents comme contrat d’entrée.

Argument :
- couplage plus faible entre steps,
- meilleure rejouabilité,
- alignement avec la conception actuelle des steps (`WorkflowArtifactService` comme source de vérité).

## 8) Résultat final de l’orchestrator (G)

Solution simple sans nouveau modèle :
- retourner un `WorkflowStepResult`.
- comportement :
  - premier résultat bloquant (`WAIT_HUMAN`/`STOP_FAILURE`) s’il survient avant la fin,
  - sinon résultat de la 3e step.

## 9) Question importante (Q5)

Réponse nette :
- **ajouter une méthode dédiée à 3 steps** dans `WorkflowOrchestrator`,
- **ne pas** créer une abstraction “workflow sequence” maintenant.

Motif : besoin local, borné, pragmatique, anti-framework.

## 10) Classes à créer / modifier

À modifier :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
  - ajout de `executeThreeSteps(...)`.

À adapter :
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
  - cas nominal 3 steps,
  - blocage sur step1/step2,
  - propagation du résultat de step3.

À créer :
- aucune nouvelle classe nécessaire.

## 11) Risques de sur-conception

Risques :
- introduire une liste de steps,
- introduire des stratégies de transition,
- déplacer la logique métier des steps dans l’orchestrator,
- préparer trop tôt un moteur de workflow complet.

Garde-fous :
- méthode à arité fixe (3 steps),
- décisions supportées strictes,
- aucune logique métier dans l’orchestrator.

## 12) Plan d’implémentation minimal

1. Ajouter `executeThreeSteps(...)` dans `WorkflowOrchestrator`.
2. Valider `context`, `firstStep`, `secondStep`, `thirdStep` (`Objects.requireNonNull`).
3. Exécuter step 1 via `executeSingleStep`.
4. Si résultat step1 != `CONTINUE`, retourner step1.
5. Exécuter step 2 via `executeSingleStep`.
6. Si résultat step2 != `CONTINUE`, retourner step2.
7. Exécuter step 3 via `executeSingleStep` et retourner son résultat.
8. Ajouter les tests unitaires ciblés dans `WorkflowOrchestratorTest`.

## 13) Hors périmètre explicite (H)

Doit rester absent :
- nombre variable de steps,
- retry,
- correction automatique conditionnelle,
- décisions humaines avancées,
- Telegram,
- Maven,
- moteur de transitions,
- multi-lots,
- state machine,
- stratégie configurable.

## 14) Conclusion

L’évolution la plus saine est une extension locale de `WorkflowOrchestrator` avec une méthode explicite à 3 steps, bornée et lisible. Elle permet analysis -> review -> correction avec arrêt immédiat sur blocage, sans introduire de framework ni de complexité prématurée.
