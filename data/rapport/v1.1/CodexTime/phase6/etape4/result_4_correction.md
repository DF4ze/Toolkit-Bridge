# Resultat correction Phase 6 - Etape 4

## Objectif

Corriger le point faible de l'etape 4 : dans le flux reel, le retry max atteint sur un build encore en echec etait intercepte par la garde du runner avant la conversion en `WAIT_HUMAN`.

L'objectif de cette correction etait de garantir que le vrai chemin :

```text
RETRY_CORRECTION (1/2)
-> correction
-> RETRY_CORRECTION (2/2)
-> WAIT_HUMAN
```

fonctionne bien avec le comportement reel de `MavenValidationStep`.

## Corrections appliquees

### 1. Garde `retryCount >= retryMax` corrigee

Dans `AnalysisReviewWorkflowRunner.runWithValidationAndRetry`, la branche :

```text
retryCount >= retryMax
```

ne retourne plus systematiquement `STOP_FAILURE`.

Le nouveau comportement est :

```text
si retryCount >= retryMax
  ET decision courante == RETRY_CORRECTION
  ET buildStatus == FAILURE
  ET correctionTriggered == true
-> WAIT_HUMAN

sinon -> STOP_FAILURE
```

Cette logique est centralisee dans :

- `stopAtRetryLimit(...)`
- `isRetryLimitWaitHumanCase(...)`
- `buildPersistentBuildFailureWaitHuman(...)`

### 2. `correctionTriggered=true` force cote runner

Apres succes de `BuildErrorCorrectionStep`, le runner injecte maintenant explicitement :

```text
correctionTriggered = true
```

dans le contexte courant, en plus des metadonnees de retry.

Cela evite de dependre uniquement du contenu du `WorkflowStepResult.data` retourne par le step de correction.

### 3. `hasBuildContext` rendu plus strict

La detection du contexte build pour le summary n'est plus basee sur n'importe quelle cle `*Path`.

Elle est maintenant limitee a :

- `buildStatus == FAILURE`
- ou `waitReason == Build still fails after automatic correction attempts`

Cela reduit le risque de rendre des instructions build pour un autre type de `WAIT_HUMAN`.

## Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerValidationRetryTest.java`

## Fichiers de tracabilite crees

- `data/rapport/v1.1/CodexTime/phase6/etape4/4.correction.md`
- `data/rapport/v1.1/CodexTime/phase6/etape4/result_4_correction.md`

## Nouveau test ajoute

Test important ajoute :

- `runWithValidationAndRetryConvertsRealMavenRetryLimitFlowToWaitHuman`

Ce test utilise le comportement reel de `MavenValidationStep` avec un `WorkflowValidationService` mocke qui retourne deux fois `ValidationStatus.FAILURE`.

Sequence validee :

1. premier build en echec -> `RETRY_CORRECTION` avec compteur `1/2`
2. correction build reussie
3. second build en echec -> `RETRY_CORRECTION` avec compteur `2/2`
4. le runner convertit ce cas reel en `WAIT_HUMAN`

Le test verifie notamment :

- `decision = WAIT_HUMAN`
- `finalDecision = WAIT_HUMAN`
- `waitReason = Build still fails after automatic correction attempts`
- `buildStatus = FAILURE`
- `correctionTriggered = true`
- `buildErrorRetryCount = 2`
- `buildErrorRetryMax = 2`

## Validation des tests

Commandes executees :

```powershell
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowRunnerValidationRetryTest" test
.\mvnw.cmd -q "-Dtest=AnalysisReviewWorkflowRunnerValidationRetryTest,MavenValidationStepTest,BuildErrorCorrectionStepTest,AnalysisReviewWorkflowCliTest" test
.\mvnw.cmd -q test
```

Resultat : succes.

Les sorties Maven contiennent uniquement les warnings JVM/Mockito habituels et des erreurs attendues dans des tests de resilience existants. Aucun test n'a echoue.

## Confirmation du flux reel

La correction valide maintenant explicitement le cas reel attendu :

```text
MavenValidationStep FAILURE -> RETRY_CORRECTION (1/2)
-> BuildErrorCorrectionStep CONTINUE
-> MavenValidationStep FAILURE -> RETRY_CORRECTION (2/2)
-> AnalysisReviewWorkflowRunner WAIT_HUMAN
```

Le systeme ne s'arrete donc plus en `STOP_FAILURE` par erreur sur ce chemin.

## Contraintes respectees

- Pas de modification de l'orchestrateur.
- Pas de modification des steps.
- Pas de parsing Maven.
- Pas de logique complexe ajoutee.
- Pas de nouvelle couche.
- Pas de Telegram.

## Resume

Le lot est maintenant aligne avec l'intention de l'etape 4 :

- l'echec technique reste `STOP_FAILURE`
- l'echec build persistant apres auto-correction devient bien `WAIT_HUMAN`
- le summary build reste coherent
- le flux reel Maven est couvert par un test dedie.
