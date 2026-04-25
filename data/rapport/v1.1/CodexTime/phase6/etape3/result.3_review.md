# Revue critique architecture - Phase 6 Etape 3

## Perimetre relu

Implementation relue integralement sur le dernier lot :

- `MavenValidationStep`
- `BuildErrorCorrectionStep`
- `AnalysisReviewWorkflowRunner`
- `AnalysisReviewWorkflowRunnerFactory`
- `WorkflowArtifactType`
- `MavenValidationStepTest`
- `BuildErrorCorrectionStepTest`
- `AnalysisReviewWorkflowRunnerValidationRetryTest`

Cette revue ne modifie pas le code applicatif. Elle identifie les points discutables et les corrections utiles, sans ajouter de fonctionnalite hors perimetre.

## Evaluation generale

L'implementation respecte globalement l'objectif : ajouter une boucle d'auto-correction build controlee, basee sur le runner existant, sans modifier l'orchestrateur et sans introduire de moteur generique.

Le decoupage reste lisible :

- `MavenValidationStep` detecte le resultat Maven et decide si une correction build est possible.
- `BuildErrorCorrectionStep` porte l'appel Codex dedie a l'erreur de build.
- `AnalysisReviewWorkflowRunner` porte la boucle locale de retry.
- La factory assemble le wiring runtime sans Spring.

Le lot reste sobre et coherent avec les contraintes. Les risques identifies concernent surtout la robustesse de la boucle, la clarte des artefacts et le couplage entre retry build et reexecution du workflow complet.

## Points solides

### Separation configuration / runtime

La configuration reste portee par `WorkflowExecutionContext.variables()`. Les valeurs de runtime (`buildErrorRetryCount`, `buildErrorRetryMax`, `buildErrorSummary`, `buildArtifactPath`) sont explicites et limitees au scenario build.

La factory isole correctement l'assemblage runtime par defaut. Il n'y a pas d'introduction de Spring ou de couche applicative lourde.

### Qualite du modele

Le modele reste volontairement minimal :

- pas de nouveau DTO ;
- pas de nouvel objet de commande ;
- usage direct de `WorkflowStepResult.data` ;
- decision `RETRY_CORRECTION` reutilisee comme signal de boucle.

C'est adapte a l'etape, tant que la boucle reste unique et dediee au build.

### Decouplage orchestrator / tooling / workspace

L'orchestrateur n'est pas etendu pour gerer le retry, ce qui respecte le prompt. Le tooling Maven reste encapsule dans `WorkflowValidationService` et le workspace Maven reste une variable de contexte.

La memoire, la policy et Telegram ne sont pas introduits dans ce lot.

### Tests

Les tests couvrent les cas demandes :

- retry une fois puis succes ;
- maximum atteint ;
- timeout sans retry ;
- system error sans retry ;
- correction Codex reussie ;
- correction impossible faute de contexte ;
- echec Codex.

Ils sont utiles car ils valident le contrat comportemental sans lancer Maven ni Codex reellement.

## Faiblesses ou points discutables

### 1. La boucle du runner n'impose pas elle-meme de borne de securite

`runWithValidationAndRetry` boucle tant que `runWithValidation` retourne `RETRY_CORRECTION`. En pratique, `MavenValidationStep` incremente le compteur et finit par retourner `STOP_FAILURE`.

Point discutable : la condition d'arret est donc portee par le step de validation, pas par le runner qui execute la boucle. Si un autre step, un test ou une evolution future retourne `RETRY_CORRECTION` sans compteur correct, la boucle peut devenir non bornee.

Correction utile :

- garder la decision principale dans `MavenValidationStep` ;
- ajouter dans le runner une garde simple basee sur `buildErrorRetryCount` / `buildErrorRetryMax` avant de relancer une correction ;
- en cas de donnees absentes ou incoherentes, retourner `STOP_FAILURE` avec un message clair.

Cette correction ne cree pas de nouvelle architecture. Elle rend seulement la boucle defensive.

### 2. `BuildErrorCorrectionStep` expose un `promptArtifactPath` trompeur

Le step retourne :

- `promptArtifactPath`
- `resultArtifactPath`
- `correctionAttemptPath`

Mais il n'ecrit qu'un seul artefact `BUILD_ERROR_CORRECTION`, contenant le resultat Codex. Le `promptArtifactPath` pointe donc vers le meme fichier que le resultat, sans artefact de prompt distinct.

Point discutable : pour un appelant externe ou un humain, cela peut laisser croire que le prompt a ete archive separement.

Correction utile :

- soit supprimer `promptArtifactPath` dans ce step ;
- soit ecrire explicitement un artefact prompt dedie plus tard, si le besoin apparait.

La correction minimale recommandee maintenant est de supprimer la cle trompeuse.

### 3. Les tentatives de correction build ecrasent probablement le meme artefact

`WorkflowArtifactType.BUILD_ERROR_CORRECTION` produit un chemin base sur le numero d'etape. Si plusieurs retries se produisent dans la meme etape, les corrections successives risquent d'ecrire dans le meme fichier.

Point discutable : la derniere tentative reste visible, mais l'historique des tentatives est perdu.

Correction utile :

- a court terme, accepter ce comportement si l'objectif est seulement le resultat final ;
- si l'historique devient utile, ajouter l'index de tentative dans le contenu de l'artefact ou dans un nom dedie.

Ne pas introduire maintenant un systeme complet de journalisation des retries.

### 4. La boucle relance l'ensemble analyse/review/correction a chaque retry

Le pseudocode demande `runWithValidation()` dans la boucle. L'implementation suit donc le prompt.

Point discutable architecturalement : apres une correction d'erreur build, le besoin naturel est souvent de relancer uniquement la validation Maven. Relancer analyse/review/correction peut etre couteux et modifier des artefacts qui ne sont pas directement lies a l'erreur build.

Correction utile :

- ne pas changer le comportement sans validation fonctionnelle ;
- si le prochain lot confirme ce besoin, isoler dans le runner une methode privee qui relance uniquement la validation apres `BuildErrorCorrectionStep`.

Cette correction resterait locale au runner et ne necessiterait pas de modifier l'orchestrateur.

### 5. Les cles de contexte sont dupliquees entre steps et runner

Les cles `buildErrorRetryCount`, `buildErrorRetryMax`, `buildErrorSummary`, `buildArtifactPath` sont declarees localement dans plusieurs classes.

Point discutable : la duplication reste acceptable a ce stade, mais elle peut devenir fragile si d'autres consommateurs manipulent ces memes cles.

Correction utile :

- attendre un second usage reel avant de creer une petite classe de constantes ;
- ne pas creer de modele externe ou DTO maintenant.

### 6. Les tests ne verifient pas l'ecrasement ou la pluralite d'artefacts

Les tests valident le flux comportemental mais pas la question des artefacts multi-tentatives.

Correction utile :

- si on conserve l'ecrasement volontaire, le documenter ;
- sinon ajouter un test simple lorsque la strategie d'artefacts sera ajustee.

## Corrections recommandees maintenant

Corrections utiles, sans extension fonctionnelle :

1. Ajouter une garde defensive dans `AnalysisReviewWorkflowRunner.runWithValidationAndRetry` pour stopper si `RETRY_CORRECTION` arrive sans compteur coherent ou si `buildErrorRetryCount >= buildErrorRetryMax`.
2. Retirer `promptArtifactPath` de `BuildErrorCorrectionStep` tant qu'aucun prompt distinct n'est ecrit.
3. Documenter dans le rapport ou dans un commentaire de test que l'artefact `BUILD_ERROR_CORRECTION` est le dernier resultat de tentative, pas un historique.

Corrections a reporter :

1. Ne pas creer tout de suite une classe globale de constantes.
2. Ne pas changer tout de suite le flow pour relancer uniquement Maven, car l'implementation suit le pseudocode valide.
3. Ne pas ajouter un journal de retry ou un modele de tentative.

## Risques de dette technique

Dette faible a moderee.

Le lot est propre pour une premiere boucle agentique, mais deux petites dettes peuvent gener les futures phases :

- dependance implicite du runner a la bonne discipline de `MavenValidationStep` pour stopper la boucle ;
- confusion possible dans les artefacts exposes par `BuildErrorCorrectionStep`.

Ces dettes sont corrigeables localement, sans refactor global.

## Risques de refactor futur evitables maintenant

Le seul refactor vraiment evitable maintenant est la garde defensive de boucle dans le runner. Elle protegerait les futures phases sans changer le design.

La suppression de `promptArtifactPath` eviterait aussi une dette de contrat externe : mieux vaut ne pas exposer une cle tant que l'artefact correspondant n'existe pas vraiment.

## Limites volontairement absentes

A ne pas introduire dans cette correction :

- moteur generique de retry ;
- orchestration avancee ;
- systeme d'evenements ;
- persistance detaillee des tentatives ;
- DTO de retry build ;
- parsing Maven avance ;
- Telegram ;
- API REST ;
- state machine.

## Validation

Les validations lancees apres implementation etaient positives :

- `.\mvnw.cmd -q "-Dtest=MavenValidationStepTest,BuildErrorCorrectionStepTest,AnalysisReviewWorkflowRunnerValidationRetryTest,AnalysisReviewWorkflowRunnerResumeTest,AnalysisReviewWorkflowCliTest,WorkflowOrchestratorTest" test`
- `.\mvnw.cmd -q test`

Resultat : succes.

## Resume final

L'implementation est validee architecturalement pour cette etape. Elle reste simple, lisible et centree sur le runner existant.

Les corrections les plus utiles avant d'empiler les phases suivantes sont petites :

1. rendre la boucle defensive cote runner ;
2. supprimer ou rendre exact le `promptArtifactPath` ;
3. clarifier la strategie d'artefact pour les retries multiples.

Il n'y a pas de signe de sur-architecture. Le principal risque est plutot l'inverse : quelques contrats implicites restent a verrouiller avant que le mecanisme ne soit consomme par des couches plus externes.
