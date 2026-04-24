# Rapport d’analyse — Phase 3 Étape 2 — Enchaînement minimal 2 steps

## 1) Recommandation d’évolution de l’orchestrator

Recommandation nette :
- **faire évoluer `WorkflowOrchestrator` avec une seule nouvelle méthode dédiée à l’enchaînement de 2 steps**, sans nouvelle abstraction.

Pourquoi :
- cohérent avec l’état actuel (`executeSingleStep` déjà simple et lisible)
- évite une sur-architecture prématurée (pas de `workflow sequence`, pas de moteur)
- répond exactement au besoin de phase 3 étape 2 : 2 steps linéaires, point.

## 2) Package / emplacement (A)

- Garder l’évolution dans :
  - `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator`
- Ne pas introduire de nouvelle classe d’orchestration à ce stade.

## 3) Responsabilité exacte (B)

Cette version 2 steps de l’orchestrator doit :
1. exécuter la step 1 (analysis)
2. lire la décision
3. si `CONTINUE`, exécuter la step 2 (review)
4. si `WAIT_HUMAN` ou `STOP_FAILURE`, s’arrêter immédiatement
5. retourner un `WorkflowStepResult` final simple

Elle ne doit surtout pas faire :
- routing dynamique
- boucle / retry
- moteur de transitions
- interprétation métier des artefacts
- parsing sémantique des résultats
- décisions humaines avancées

## 4) Signature recommandée (C)

Recommandation nette :
- ajouter une méthode dans `WorkflowOrchestrator` (pas de surcharge complexe, pas de modèle nouveau), par exemple :

```java
public WorkflowStepResult executeTwoSteps(
        WorkflowExecutionContext context,
        WorkflowStep firstStep,
        WorkflowStep secondStep
)
```

Principes :
- signature explicite et bornée à 2 steps
- réutilisation de `executeSingleStep(...)` en interne pour garder un point unique de validation des décisions supportées

## 5) Gestion des décisions (D)

Règle stricte recommandée :
1. Exécuter `firstStep`.
2. Si décision `CONTINUE` -> exécuter `secondStep`.
3. Si décision `WAIT_HUMAN` -> arrêt immédiat, retourner le résultat de la step 1.
4. Si décision `STOP_FAILURE` -> arrêt immédiat, retourner le résultat de la step 1.
5. Le résultat final est :
   - résultat de la step 2 si elle a été exécutée
   - sinon premier résultat bloquant (step 1)

Note :
- la step 2 peut elle-même renvoyer `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE` ; ce résultat devient le résultat final de l’orchestrator dans ce flux minimal.

## 6) Rapport au contexte runtime (E)

Réponse nette demandée : **3. il suffit tel quel pour cette étape, avec limites assumées**.

- Le même `WorkflowExecutionContext` peut être transmis aux 2 steps.
- Limites assumées : contexte non typé (`Map<String,Object>`) et conventions de variables partagées.
- Pas de changement de modèle `WorkflowExecutionContext` nécessaire en étape 2.

## 7) Rapport aux résultats intermédiaires (F)

Réponse nette :
- **la 2e step ne doit pas dépendre de `WorkflowStepResult.data` de la 1re step**.
- **elle doit s’appuyer sur les artefacts déjà écrits + conventions de contexte**.

Arguments :
- couplage plus faible entre steps
- meilleure rejouabilité (review relançable si artefacts présents)
- cohérence avec `GlobalReviewStep` déjà conçu autour de `WorkflowArtifactService`

`WorkflowStepResult.data` reste utile pour observabilité, pas comme contrat d’entrée de la step suivante.

## 8) Résultat final de l’orchestrator (G)

Solution simple recommandée (sans nouveau modèle) :
- retourner `WorkflowStepResult`.
- comportement :
  - si step 1 bloque (`WAIT_HUMAN` ou `STOP_FAILURE`) -> retourner ce premier résultat bloquant
  - sinon -> retourner le résultat de step 2

Pas de nouvel objet agrégat nécessaire à ce stade.

## 9) Question importante (Q5)

Réponse nette :
- **faire évoluer `WorkflowOrchestrator` avec une simple nouvelle méthode dédiée à l’enchaînement de 2 steps**.
- **ne pas créer maintenant une abstraction “workflow sequence”**.

Motif : besoin local, borné, et explicitement anti-framework pour cette phase.

## 10) Classes à modifier / créer

À modifier :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
  - ajout de la méthode 2 steps

À adapter en tests :
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
  - ajouter tests sur le flux 2 steps (`CONTINUE->step2`, `WAIT_HUMAN` stop, `STOP_FAILURE` stop)

À créer maintenant :
- aucune nouvelle classe obligatoire.

## 11) Risques de sur-conception

Risques principaux :
- transformer `executeTwoSteps` en mini moteur paramétrable
- accepter déjà une liste de steps ou des stratégies de transition
- ajouter des décisions/règles non nécessaires

Garde-fous :
- signature fermée à 2 steps
- uniquement `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`
- aucun état supplémentaire
- aucun routing

## 12) Plan d’implémentation minimal (sobre)

1. Ajouter `executeTwoSteps(context, firstStep, secondStep)` dans `WorkflowOrchestrator`.
2. Valider `context`, `firstStep`, `secondStep` (`Objects.requireNonNull`).
3. Exécuter `firstStep` via `executeSingleStep`.
4. Si résultat step 1 != `CONTINUE`, retourner immédiatement ce résultat.
5. Sinon exécuter `secondStep` via `executeSingleStep`.
6. Retourner le résultat de la step 2.
7. Ajouter tests unitaires ciblés dans `WorkflowOrchestratorTest`.

## 13) Hors périmètre explicite (H)

Doit rester absent de cette étape :
- nombre variable de steps
- retry
- correction automatique
- décisions humaines avancées
- Telegram
- Maven
- moteur de transitions
- multi-lots
- state machine
- plugin/routing/config stratégique

## 14) Conclusion

La meilleure option est une **évolution locale, explicite et bornée de `WorkflowOrchestrator`** : une méthode dédiée à l’enchaînement de **2 steps strictement linéaires**, réutilisant `executeSingleStep`, sans nouveau framework ni abstraction anticipée.
