# Revue critique d'architecture - Phase 5 Etape 2

## Perimetre relu

Implementation relue:
- `AnalysisReviewWorkflowCli`
- `AnalysisReviewWorkflowCliTest`
- `AnalysisReviewWorkflowRunnerResumeTest`
- `result.2.implements.md`

Objectif du lot:
- stabiliser le contrat externe sans nouveau modele;
- exposer les cles utiles via le CLI;
- verifier les cles obligatoires dans les resultats consolides;
- rester sur `WorkflowStepResult` et `workflow-summary.md`.

## Verdict global

L'implementation est globalement conforme au cadrage. Elle ne modifie pas le modele, ne cree pas de DTO, ne cree pas de JSON et ne touche pas au runtime central. Le CLI expose maintenant un contrat plus complet et plus regulier.

La principale faiblesse est subtile: les fallbacks CLI rendent la sortie robuste, mais peuvent masquer un resultat runtime incomplet. Pour une V1 technique, c'est acceptable, mais il faut garder en tete que la stabilisation reelle doit rester portee par le runner/orchestrator, pas uniquement par l'affichage CLI.

## 1. Faiblesses ou points discutables

### 1.1 Fallbacks CLI utiles mais potentiellement masquants

Le CLI imprime:
- `finalDecision` avec fallback `decision.name()`;
- `nextAction` avec fallback selon `decision`;
- `correctionTriggered=false` si absent.

Point positif: l'appel externe obtient toujours une sortie exploitable.

Point discutable: si un chemin runtime oublie une cle obligatoire, le CLI continuera a afficher un contrat propre. Cela peut masquer une derive du contrat interne.

Correction utile proposee:
- conserver les fallbacks CLI pour l'exposition externe;
- renforcer les tests du runner/orchestrator sur les chemins critiques plutot que supprimer les fallbacks;
- a terme, centraliser les assertions de contrat dans les tests runtime pour eviter que le CLI soit la seule garantie.

### 1.2 Normalisation limitee au CLI

La normalisation des retours ligne est uniquement dans `AnalysisReviewWorkflowCli.safe(...)`.

Point positif: c'est exactement le bon endroit pour une contrainte de presentation `key=value`.

Point discutable: un futur canal Telegram qui consomme directement `WorkflowStepResult` devra probablement refaire sa propre normalisation de message.

Correction utile proposee:
- ne pas creer de brique dediee maintenant;
- documenter que la normalisation actuelle est une normalisation de sortie CLI, pas une normalisation metier;
- si un deuxieme canal arrive, extraire une petite normalisation de presentation partagee, sans modele metier.

### 1.3 Duplication locale des cles de contrat

Les cles `finalDecision`, `nextAction`, `correctionTriggered`, `waitReason`, `workflowSummaryPath` existent deja dans plusieurs classes sous forme de strings.

Point positif: cela evite une abstraction prematuree.

Point faible: plus le contrat externe se stabilise, plus la duplication de strings peut devenir une source d'erreur.

Correction utile proposee:
- ne pas introduire tout de suite un modele externe;
- envisager seulement des constantes partagees si une prochaine etape manipule ces memes cles dans un autre consommateur;
- eviter une classe de constantes globale trop large.

### 1.4 Tests CLI avec mocks et tests runner avec stubs

Les tests sont utiles et rapides. Ils evitent le vrai binaire Codex et isolent bien le comportement.

Point discutable: `AnalysisReviewWorkflowCliTest` utilise a la fois des steps stub et un mock du runner selon les cas. C'est justifie par les cas testes, mais la classe de test commence a melanger deux niveaux:
- integration legere CLI + runner;
- unit test pur du rendu CLI.

Correction utile proposee:
- garder tel quel pour ce lot;
- si le CLI grossit, separer les tests de parsing/rendu des tests de delegation.

### 1.5 Contrat `correctionTriggered` en reprise

Dans `runCorrectionAfterReview(...)`, le runner force `correctionTriggered=true` apres execution de la correction, meme si le `CorrectionStep` a pu retourner un echec precoce avec `correctionTriggered=false` dans ses donnees.

Ce comportement existait deja et les tests l'assument dans le contexte "reprise correction appelee". Il peut se defendre comme "la commande correction a ete declenchee".

Point sensible: la semantique peut etre ambigue entre:
- correction demandee / chemin de correction execute;
- appel Codex de correction effectivement lance.

Correction utile proposee:
- ne pas changer dans ce lot;
- documenter la semantique actuelle: cote runner resume, `correctionTriggered=true` signifie que la reprise correction a ete lancee;
- si une distinction devient necessaire, elle devra etre traitee explicitement plus tard sans surcharger cette cle.

## 2. Verification des axes demandes

### Separation configuration / runtime

Respectee. Le lot 2 ne rajoute pas de wiring runtime. Le CLI reste consommateur du runner via la factory introduite a l'etape precedente.

### Qualite du modele implemente

Bonne pour le perimetre demande: aucun nouveau modele. Le contrat reste fonde sur `WorkflowStepResult`, avec normalisation de presentation dans le CLI.

### Decouplage orchestrator / memoire / tooling / policy / workspace

Respecte. Le lot ne couple pas l'orchestrator a la memoire, au tooling, aux policies ou au workspace. Aucun nouveau lien transversal n'est introduit.

### Absence de logique ad hoc

Globalement respectee. Les fallbacks `nextAction` dans le CLI sont specifiques au workflow, mais ils correspondent au contrat externe demande et restent limites a la presentation.

### Couplage avec futures phases

Risque modere:
- duplication de cles si Telegram consomme les memes champs;
- duplication de normalisation si un deuxieme canal est ajoute.

Le risque est acceptable maintenant, car une abstraction commune serait prematuree sans second consommateur reel.

### Coherence des noms

Les noms sont coherents:
- `OUTPUT_CORRECTION_TRIGGERED`;
- `OUTPUT_WAIT_REASON`;
- `OUTPUT_WORKFLOW_SUMMARY_PATH`;
- `assertMandatoryContract`.

Point mineur: `OUTPUT_*` designe en fait des cles issues du contrat runtime autant que des cles imprimees. Ce n'est pas bloquant.

### Lisibilite generale

La lecture reste simple. Le CLI commence a accumuler parsing, contexte, execution et rendu, mais cela avait deja ete accepte comme point d'entree technique minimal.

### Tests réellement utiles

Oui. Les tests couvrent les chemins importants:
- `WAIT_HUMAN`;
- `RESUME`;
- absence de summary;
- `STOP_FAILURE`;
- normalisation de message;
- cles obligatoires runner.

### Dette technique introduite

Dette faible et assumee:
- cles string du contrat encore dupliquees;
- normalisation CLI non partagee;
- fallbacks CLI pouvant masquer une derive runtime;
- semantique de `correctionTriggered` a clarifier si le besoin devient plus fin.

## 3. Corrections utiles proposees

Corrections utiles, sans nouvelle fonctionnalite:

1. Ajouter a terme un test runner/orchestrator couvrant un echec precoce d'analyse/review si ce chemin devient critique.

2. Documenter explicitement la semantique de `correctionTriggered`:
   - dans le runner resume: reprise correction lancee;
   - dans un step correction: appel correction effectivement atteint ou non selon le chemin.

3. Si un second consommateur externe arrive, extraire uniquement la normalisation de presentation (`safe`, fallback `nextAction`) dans une petite brique partagee. Ne pas le faire maintenant.

4. Si les cles sont reutilisees par Telegram, introduire des constantes partagees tres limitees au contrat workflow. Ne pas creer de DTO.

## 4. Ce qu'il ne faut pas ajouter maintenant

- Pas de DTO externe.
- Pas de JSON.
- Pas de serializer.
- Pas de versioning du contrat.
- Pas d'API REST.
- Pas de couche de projection.
- Pas de refactor global du runner/orchestrator.
- Pas de changement de `WorkflowStepResult`.

## 5. Resume final

Le lot 2 stabilise correctement le contrat externe V1. Le CLI expose maintenant les champs necessaires a un consommateur simple, les valeurs sont normalisees en `key=value`, et les tests verifient les chemins critiques.

La solution reste sobre et alignee avec le workflow: elle renforce l'existant plutot que de construire un modele parallele. Les seules dettes a surveiller sont la duplication des cles string et le fait que certains fallbacks vivent cote CLI. Ces points ne justifient pas de correction immediate tant qu'un second consommateur externe n'existe pas.
