# Rapport d’analyse — Phase 4 Étape 2 — Correction conditionnelle

## 1) Recommandation globale

Évolution V1 recommandée :
- garder un flux `Analysis -> Review -> (Correction optionnelle)`
- porter la condition dans l’orchestrateur
- utiliser une convention explicite produite par `GlobalReviewStep`
- ne pas introduire de moteur de règles ni parsing libre.

## 2) A. Où placer la logique de condition

Réponse nette : **dans `WorkflowOrchestrator`**.

Pourquoi :
- la décision “enchaîner ou s’arrêter” est une responsabilité d’orchestration,
- `GlobalReviewStep` doit produire un signal simple, pas piloter le workflow,
- `CorrectionStep` reste un step métier indépendant.

## 3) B. Source de vérité de la décision

Réponse nette : **combinaison simple des deux**, avec priorité claire :
- source opérationnelle : `WorkflowStepResult` de `GlobalReviewStep` (donnée structurée),
- source de traçabilité : artefact `REVIEW_RESULT` (audit humain).

Concrètement en V1 :
- `GlobalReviewStep` lit son output, applique une convention explicite, puis expose un flag structuré dans `WorkflowStepResult.data` (ex: `requiresCorrection=true/false`),
- l’orchestrateur ne parse pas le fichier markdown de review.

## 4) C. Convention recommandée

Convention V1 recommandée, explicite et testable :
- `DECISION: NEED_CORRECTION`
- `DECISION: OK`
- (déjà existant) `DECISION: WAIT_HUMAN`

Règles simples :
- `WAIT_HUMAN` garde priorité fonctionnelle (retour `WAIT_HUMAN`),
- si `CONTINUE` + `NEED_CORRECTION` => lancer correction,
- si `CONTINUE` + `OK` (ou absence de marqueur en mode strict défini) => ne pas lancer correction.

## 5) D. Signature orchestrator recommandée

Réponse nette : **nouvelle méthode dédiée** dans `WorkflowOrchestrator`, sans toucher au comportement générique de `executeThreeSteps(...)`.

Exemple sobre :
```java
public WorkflowStepResult executeAnalysisReviewWithOptionalCorrection(
    WorkflowExecutionContext context,
    WorkflowStep analysisStep,
    WorkflowStep reviewStep,
    WorkflowStep correctionStep
)
```

Pourquoi :
- garde `executeThreeSteps(...)` comme primitive linéaire,
- évite d’injecter un prédicat/gating générique (sur-conception),
- rend le cas d’usage lisible et explicite.

## 6) E. Gestion stricte des décisions

Comportement recommandé :
1. Exécuter Analysis.
2. Si Analysis != `CONTINUE` -> retour immédiat.
3. Exécuter Review.
4. Si Review = `WAIT_HUMAN` -> retour immédiat `WAIT_HUMAN`.
5. Si Review = `STOP_FAILURE` -> retour immédiat `STOP_FAILURE`.
6. Si Review = `CONTINUE` :
   - si correction requise -> exécuter Correction et retourner son résultat,
   - sinon -> arrêter proprement et retourner un `WorkflowStepResult(CONTINUE, "Review completed - no correction required", data...)`.

Résultat final par cas :
- blocage en amont : premier résultat bloquant,
- correction exécutée : résultat de `CorrectionStep`,
- correction non requise : résultat explicite de fin “sans correction”.

## 7) F. Rapport au contexte runtime

Réponse nette : **3) il suffit tel quel avec conventions simples**.

Le `WorkflowExecutionContext` actuel est suffisant pour cette étape.
Aucune évolution de modèle n’est nécessaire en V1.

## 8) G. Rapport à `CorrectionStep`

Réponse nette : **`CorrectionStep` reste indépendante**.

Elle doit seulement être appelée ou non par l’orchestrateur.
Ne pas l’enrichir maintenant avec une “raison d’appel” spécifique.

## 9) H. Observabilité locale recommandée

Sans nouvelle couche technique :
- conserver la convention `DECISION: ...` dans `REVIEW_RESULT`,
- enrichir le `WorkflowStepResult` final avec un indicateur simple (`correctionTriggered=true/false`),
- message final explicite :
  - “correction triggered”
  - ou “no correction required”.

Ainsi, la visibilité est immédiate en test unitaire et en logs applicatifs existants.

## 10) I. Limites de périmètre à maintenir

Doit rester hors scope de cette étape :
- parsing sémantique lourd,
- moteur de règles,
- stratégie configurable,
- retry automatique,
- boucle de correction,
- Telegram,
- Maven,
- multi-lots.

## 11) Question importante (V1)

Réponse nette : **option 1**.

Il faut faire porter à `GlobalReviewStep` une convention explicite (`DECISION: NEED_CORRECTION` / `DECISION: OK`) et **ne pas** déduire le besoin de correction depuis du texte libre.

Argument pragmatique :
- testable,
- déterministe,
- robuste,
- évite les faux positifs/faux négatifs,
- évite d’ouvrir la porte à un pseudo moteur sémantique.

## 12) Classes à modifier / créer

À modifier :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

À créer :
- aucune classe obligatoire en V1.

## 13) Risques de sur-conception

Risques principaux :
- transformer la condition en système configurable,
- faire parser des artefacts par l’orchestrateur,
- introduire des abstractions de séquence/règles trop tôt,
- coupler `CorrectionStep` à des raisons métier.

Garde-fous :
- une méthode dédiée,
- une convention textuelle explicite,
- un booléen de décision simple,
- aucun composant supplémentaire.

## 14) Plan d’implémentation minimal (sobre)

1. Définir la convention review V1 (`NEED_CORRECTION` / `OK` / `WAIT_HUMAN`).
2. Faire exposer par `GlobalReviewStep` un flag structuré (`requiresCorrection`) dans `WorkflowStepResult.data`.
3. Ajouter dans `WorkflowOrchestrator` une méthode dédiée `analysis-review-correction optionnelle`.
4. Conserver l’arrêt immédiat sur `WAIT_HUMAN` et `STOP_FAILURE`.
5. Si review `CONTINUE` + correction requise, exécuter `CorrectionStep`.
6. Sinon, retourner un résultat final explicite de fin sans correction.
7. Ajouter les tests unitaires ciblés (review decision + gating orchestrator).
8. Ne rien ajouter d’autre (pas de moteur, pas de stratégie, pas de retry).

## 15) Ce qui doit rester volontairement absent

- moteur de règles,
- parser sémantique avancé,
- abstraction de workflow générique,
- boucle/réessai automatique,
- nouvelle couche d’observabilité technique,
- intégration Telegram/Maven/multi-lots.
