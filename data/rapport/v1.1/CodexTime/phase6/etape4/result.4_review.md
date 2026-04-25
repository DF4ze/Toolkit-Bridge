# Revue critique architecture - Phase 6 Etape 4

## Perimetre relu

Implementation relue integralement sur le dernier lot :

- `AnalysisReviewWorkflowRunner`
- `AnalysisReviewWorkflowRunnerValidationRetryTest`
- `data/rapport/v1.1/CodexTime/phase6/etape4/4.implements.md`
- `data/rapport/v1.1/CodexTime/phase6/etape4/result_4_implements.md`

Cette revue ne modifie pas le code applicatif. Elle identifie les faiblesses, les corrections utiles et les risques de refactor futur, sans ajouter de fonctionnalite hors perimetre.

## Evaluation generale

L'intention architecturale est bonne : distinguer un echec technique pur d'un blocage ou l'agent a tente de corriger mais ne peut plus avancer seul.

Le choix de placer la conversion `STOP_FAILURE -> WAIT_HUMAN` dans `AnalysisReviewWorkflowRunner` est coherent :

- le runner voit le cycle complet validation -> correction -> revalidation ;
- l'orchestrateur reste decouple de la politique build ;
- les steps ne sont pas transformes en classifieurs globaux ;
- le CLI et le summary peuvent consommer le contrat existant.

Cependant, l'implementation actuelle contient un probleme de sequence important : le chemin `WAIT_HUMAN` teste ne correspond pas exactement au comportement reel de `MavenValidationStep`. Le risque est que la fonctionnalite centrale de l'etape 4 ne se declenche pas dans le flux reel.

## Points solides

### Separation configuration / runtime

La configuration reste dans `WorkflowExecutionContext.variables()`. Les donnees de runtime build (`buildStatus`, `buildErrorRetryCount`, `buildErrorRetryMax`, `correctionAttemptPath`, `correctionTriggered`) restent portees par `WorkflowStepResult.data` et le contexte courant.

Il n'y a pas de nouvelle couche de configuration ni de dependance a Spring.

### Decouplage

L'orchestrateur n'est pas modifie. Les steps existants ne sont pas modifies. La logique de decision humaine est localisee dans le runner, ce qui correspond au role actuel du runner comme point de coordination du scenario analyse/review/correction/validation.

La memoire, Telegram, les policies globales et le workspace ne sont pas introduits dans cette etape.

### Modele

Le modele reste leger :

- pas de nouveau DTO ;
- pas de classification complexe ;
- usage de `WorkflowStepResult` ;
- usage de cles existantes ou deja introduites dans la phase 6.

Le choix de `waitReason` et `nextAction` est compatible avec le contrat CLI existant.

### Summary

Le summary distingue maintenant deux familles de `WAIT_HUMAN` :

- review humaine ;
- build persistant apres correction.

Cette separation est utile pour Telegram plus tard, car le message humain devient exploitable sans parser Maven.

## Faiblesses ou points discutables

### 1. Le flux reel risque de ne jamais convertir l'echec persistant en `WAIT_HUMAN`

Probleme principal.

Dans le comportement reel de `MavenValidationStep`, un `FAILURE` avec retry disponible retourne `RETRY_CORRECTION` avec un compteur deja incremente. Exemple avec `max=2` :

1. contexte `retryCount=0`
2. Maven echoue -> `RETRY_CORRECTION`, data `buildErrorRetryCount=1`, `buildErrorRetryMax=2`
3. correction build
4. contexte `retryCount=1`
5. Maven echoue encore -> `RETRY_CORRECTION`, data `buildErrorRetryCount=2`, `buildErrorRetryMax=2`
6. le runner voit `RETRY_CORRECTION` puis applique la garde `retryCount >= retryMax`
7. le runner retourne un `STOP_FAILURE` interne "Build retry limit reached"

Ce `STOP_FAILURE` interne ne contient pas `buildStatus=FAILURE` et force `correctionTriggered=false`. Il ne passe donc pas par la conversion `WAIT_HUMAN`.

Conséquence : dans le flux reel, l'etape 4 peut rester en `STOP_FAILURE` alors que l'objectif demande `WAIT_HUMAN`.

Correction utile :

- ajuster la branche `retryCount >= retryMax` dans le runner ;
- si le retry max est atteint apres une correction deja tentee et que le resultat courant est un `RETRY_CORRECTION` issu d'un build failure, retourner directement un `WAIT_HUMAN` build ;
- conserver `STOP_FAILURE` pour les etats incoherents sans correction prealable.

Cette correction reste locale au runner et ne modifie ni orchestrateur ni steps.

### 2. Les tests simulent un final `STOP_FAILURE` que `MavenValidationStep` ne produit pas dans ce scenario

Le test `runWithValidationAndRetryConvertsPersistentBuildFailureToWaitHuman` force une sequence :

```text
RETRY_CORRECTION -> correction -> STOP_FAILURE avec buildStatus=FAILURE et retry max atteint
```

Mais avec `MavenValidationStep`, la deuxieme erreur peut encore ressortir en `RETRY_CORRECTION` avec compteur incremente au maximum, puis etre arretee par la garde du runner.

Correction utile :

- ajouter un test d'integration unitaire plus proche du step reel :
  - premiere validation : `RETRY_CORRECTION` avec `1/2` ;
  - correction : `CONTINUE`;
  - deuxieme validation : `RETRY_CORRECTION` avec `2/2`;
  - attendu : `WAIT_HUMAN`, pas `STOP_FAILURE`.

Ce test capturerait exactement le risque actuel.

### 3. `correctionTriggered` depend trop du data retourne par le step de correction

Le runner sait qu'il vient d'executer `buildErrorCorrectionStep`. Pourtant, il depend surtout de `correctionResult.data().get("correctionTriggered")` pour savoir qu'une correction a eu lieu.

Si un futur step de correction retourne `CONTINUE` sans cette cle, la conversion `WAIT_HUMAN` ne se declenchera pas.

Correction utile :

- apres succes de `buildErrorCorrectionStep`, le runner devrait imposer `correctionTriggered=true` dans le contexte courant ;
- ne pas s'appuyer uniquement sur la discipline du step.

Cela ne change pas le contrat externe, mais rend le runtime plus robuste.

### 4. Le runner accumule beaucoup de responsabilites locales

Le runner contient maintenant :

- execution analyse/review/correction ;
- validation Maven ;
- retry build ;
- garde anti-boucle ;
- conversion `STOP_FAILURE -> WAIT_HUMAN` ;
- generation du summary ;
- variations du summary par contexte.

Pour cette phase, c'est acceptable car la contrainte demandait explicitement de ne pas modifier l'orchestrateur et de ne pas creer de nouvelle couche. Mais c'est une dette a surveiller.

Correction utile maintenant :

- ne pas creer de nouvelle abstraction tout de suite ;
- extraire seulement des methodes privees plus lisibles si une correction est faite ;
- reporter une eventuelle extraction a une phase ulterieure, quand un deuxieme type de validation ou de decision humaine apparaitra.

### 5. Le summary build repose sur une detection large du contexte build

`hasBuildContext` retourne vrai si une seule cle build est presente (`buildStatus`, `buildArtifactPath`, `buildResultPath` ou `correctionAttemptPath`).

Point discutable : un `WAIT_HUMAN` non-build qui transporterait accidentellement une de ces cles serait affiche avec les instructions build.

Correction utile :

- rendre la detection plus stricte pour le `WAIT_HUMAN` build ;
- par exemple exiger `buildStatus` ou `buildResultPath`, et idealement `waitReason` egal a la raison stable du build persistant.

Cela evite une mauvaise presentation humaine sans ajouter de modele.

### 6. Les constantes de contrat restent dispersees

Les cles `buildStatus`, `buildErrorRetryCount`, `buildErrorRetryMax`, `correctionTriggered`, `waitReason` existent dans plusieurs classes.

Pour l'instant c'est acceptable, mais la phase 5 avait stabilise le contrat externe ; plus les phases avancent, plus cette duplication peut devenir source de fautes de frappe.

Correction utile :

- ne pas creer de DTO ;
- envisager plus tard une petite classe de constantes de contrat workflow si une nouvelle etape ajoute encore des consommateurs ou des variantes de summary.

## Corrections recommandees

Corrections utiles et strictement dans le perimetre :

1. Corriger la branche `retryCount >= retryMax` dans `runWithValidationAndRetry` pour convertir en `WAIT_HUMAN` lorsque le `RETRY_CORRECTION` courant represente un build failure persistant apres correction.
2. Forcer `correctionTriggered=true` dans le contexte courant apres succes de `buildErrorCorrectionStep`, meme si le step ne le fournit pas.
3. Ajouter un test couvrant le flux reel `RETRY_CORRECTION 1/2 -> correction -> RETRY_CORRECTION 2/2 -> WAIT_HUMAN`.
4. Rendre `hasBuildContext` plus strict pour eviter de mauvaises instructions de summary.

Corrections a reporter :

1. Ne pas extraire une nouvelle couche de policy maintenant.
2. Ne pas modifier l'orchestrateur.
3. Ne pas creer de DTO ou de modele de classification.
4. Ne pas parser Maven.

## Tests

Les tests ajoutés sont utiles :

- conversion vers `WAIT_HUMAN` ;
- summary build humain ;
- conservation de `STOP_FAILURE` pour timeout, system error et retry invalide.

Mais il manque le test le plus proche du comportement reel de `MavenValidationStep` :

```text
RETRY_CORRECTION 1/2
-> correction CONTINUE
-> RETRY_CORRECTION 2/2
-> WAIT_HUMAN attendu
```

Sans ce test, l'etape peut sembler valide alors que le flux reel reste bloque en `STOP_FAILURE`.

## Dette technique introduite

Dette moderee.

La dette n'est pas due a une sur-architecture, mais a une coordination implicite entre :

- l'increment du retry dans `MavenValidationStep` ;
- la garde anti-boucle du runner ;
- la conversion finale vers `WAIT_HUMAN`.

Cette dette est corrigeable localement dans le runner et dans les tests.

## Risques pour les futures phases

### Telegram

Si la conversion ne se declenche pas dans le flux reel, Telegram recevra encore un `STOP_FAILURE` au lieu d'un message actionnable `WAIT_HUMAN`.

### Summary

Si la detection build est trop large, un futur `WAIT_HUMAN` non-build pourrait recevoir des instructions build incorrectes.

### Evolution du runner

Le runner devient le point central des decisions de fin de workflow. C'est coherent maintenant, mais il faudra surveiller la lisibilite si la phase 6 et la phase 7 ajoutent d'autres types de decision humaine.

## Conclusion

L'orientation de l'etape 4 est bonne : `STOP_FAILURE` doit rester technique, et un echec persistant apres auto-correction doit devenir `WAIT_HUMAN`.

Mais l'implementation doit etre corrigee avant d'empiler la suite : la garde `retryCount >= retryMax` intercepte probablement le flux reel avant la conversion `WAIT_HUMAN`.

La correction recommandee est petite, locale au runner, et ne remet pas en cause le design :

- convertir aussi le cas `RETRY_CORRECTION` au maximum atteint apres correction ;
- renforcer le test avec une sequence proche de `MavenValidationStep` ;
- marquer `correctionTriggered=true` dans le runner apres correction reussie.

Une fois ces points ajustes, le lot sera propre et bien prepare pour l'amelioration du summary humain + Telegram.
