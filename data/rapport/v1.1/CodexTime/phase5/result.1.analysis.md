# Resultat analyse - Phase 5 Etape 1

## 1. Strategie d'exposition recommandee

Recommandation nette: introduire une facade applicative tres legere autour de `AnalysisReviewWorkflowRunner`, pas un nouveau moteur.

Le runner possede deja le coeur utile:
- `runAnalysisReviewWithOptionalCorrection(WorkflowExecutionContext context)` pour le run complet.
- `runCorrectionAfterReview(WorkflowExecutionContext context)` pour la reprise apres intervention humaine.
- enrichissement du `WorkflowStepResult.data` avec `finalDecision`, `correctionTriggered`, `nextAction`, et `workflowSummaryPath` quand la configuration de reporting est presente.
- generation non bloquante de `workflow-summary.md`.

L'exposition externe ne doit donc pas redefinir le workflow. Elle doit seulement fournir un point d'appel stable, construire/recevoir un `WorkflowExecutionContext`, choisir entre RUN et RESUME, puis retourner le `WorkflowStepResult` ou une projection minimale de ses donnees.

## 2. Classes concernees

### Classes existantes a reutiliser

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
  - point d'entree runtime actuel, a reutiliser comme delegue principal.
  - pas besoin de modifier sa logique metier a cette etape.

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
  - ne doit pas etre modifie pour l'exposition externe.
  - il reste responsable du controle de flux interne.

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/WorkflowExecutionContext.java`
  - contrat d'entree technique deja existant.
  - peut rester le conteneur de variables sans creer de DTO lourd.

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/WorkflowStepResult.java`
  - contrat de sortie naturel.
  - a privilegier plutot qu'un nouveau modele riche.

- `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`, `WorkflowArtifactService`, `CodexWorkflowClient`
  - briques a assembler pour un appel hors test.

### Classes a creer probablement lors de l'implementation

- Une facade applicative legere, par exemple `WorkflowCommandService` ou `AnalysisReviewWorkflowCommandService`.
  - Responsabilite: exposer deux methodes simples et deleguer au runner.
  - Pas de logique de workflow.
  - Pas de parsing complexe.

- Une configuration ou fabrique Spring minimale si necessaire, par exemple `WorkflowRuntimeConfiguration`.
  - Constat actuel: `WorkflowArtifactService` est un bean Spring, mais `WorkflowOrchestrator`, `AnalysisReviewWorkflowRunner`, `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep` et `CodexWorkflowClient` ne semblent pas declares comme beans.
  - Si l'entree externe vit dans Spring, il faut un wiring explicite minimal.

### Classes a ne pas modifier a ce stade

- `WorkflowOrchestrator`: pas de changement.
- Les steps: pas de changement fonctionnel.
- `WorkflowStepResult`: pas de nouveau modele lourd.
- `WorkflowArtifactType`: pas de nouveau type d'artefact pour cette etape.

## 3. Faut-il introduire un nouveau service ?

Reponse nette: oui, une petite facade applicative est preferable a une exposition directe brute du runner.

Pourquoi ne pas exposer directement le runner seul ?
- Le runner est propre, mais il reste un composant runtime interne qui attend deja un `WorkflowExecutionContext` correctement construit.
- Un caller externe ne doit pas avoir a connaitre les details d'assemblage des steps, du client Codex et des services d'artefacts.
- Sans facade, chaque futur point d'entree risque de reconstruire la meme logique RUN/RESUME et les memes extractions de sortie.

Pourquoi la facade reste sobre ?
- Elle ne remplace pas le runner.
- Elle ne cree pas une orchestration concurrente.
- Elle expose seulement deux commandes lisibles ou une commande avec mode simple.
- Elle retourne le resultat existant, ou une map/projection tres mince.

Nom recommande: `WorkflowCommandService` si l'on veut rester general phase 5, ou `AnalysisReviewWorkflowCommandService` si l'on veut assumer que ce service expose ce workflow precis.

Preference: `AnalysisReviewWorkflowCommandService`, plus explicite et moins propice a devenir un faux moteur generique.

## 4. Point d'entree recommande

Point d'entree recommande pour cette etape: une methode Java appelable via une facade Spring legere.

Forme pragmatique:
- `run(WorkflowExecutionContext context): WorkflowStepResult`
- `resumeCorrection(WorkflowExecutionContext context): WorkflowStepResult`

Alternative acceptable:
- `execute(String mode, WorkflowExecutionContext context): WorkflowStepResult`, avec `mode` valant simplement `RUN` ou `RESUME`.

Je recommande deux methodes explicites plutot qu'un mode au depart. C'est plus simple, plus type par l'API Java, et evite d'introduire un enum ou un mini systeme de commande premature.

Un `main` ou un endpoint REST minimal peut venir ensuite, mais pour l'etape 1 le meilleur point d'entree est une facade Java. Cela rend le workflow appelable par CLI, REST, tests d'integration ou Telegram plus tard, sans choisir prematurement le canal externe.

## 5. Contrat d'entree minimal

Contrat minimal actuel: `WorkflowExecutionContext` avec un `WorkflowRun` valide et une map `variables`.

Variables minimales pour RUN complet:
- `reportRootDirectory`: racine des rapports, en `Path` ou string convertible en `Path`.
- `reportVersion`: exemple `v1.1`.
- `reportPhase`: exemple `CodexTime/phase5`.
- `stepNumber`: numero d'etape strictement positif.
- `analysisSourcePath`: fichier source du prompt d'analyse, requis par `GlobalAnalysisStep`.

Variables optionnelles utiles:
- `codexWorkingDirectory`: repertoire de travail Codex.
- `codexTimeoutSeconds`: timeout Codex.
- `humanDecisionNote`: note documentaire deja utilisee dans les tests, sans role structurel fort pour l'instant.

Variables minimales pour RESUME correction:
- `reportRootDirectory`.
- `reportVersion`.
- `reportPhase`.
- `stepNumber`.

Pour RESUME, `analysisSourcePath` n'est pas necessaire: `CorrectionStep` relit `result.<step>.analysis.md` et `result.<step>.review.md` via les chemins reconstruits par `WorkflowArtifactService`.

Il ne faut pas introduire de DTO complexe. Si un confort d'appel est necessaire, une methode helper peut accepter les primitives ci-dessus et construire le `WorkflowExecutionContext`, mais le contrat de fond doit rester celui-ci.

## 6. Contrat de sortie minimal

Contrat de sortie recommande: retourner `WorkflowStepResult` tel quel au niveau Java.

Pour un appelant externe, les champs utiles sont:
- `decision`: depuis `WorkflowStepResult.decision()`.
- `message`: depuis `WorkflowStepResult.message()`.
- `finalDecision`: depuis `data.get("finalDecision")`, avec fallback sur `decision.name()`.
- `nextAction`: depuis `data.get("nextAction")`.
- `workflowSummaryPath`: depuis `data.get("workflowSummaryPath")` si present.
- `waitReason`: depuis `data.get("waitReason")` si present.
- `correctionTriggered`: depuis `data.get("correctionTriggered")`.

Pas besoin d'un modele lourd. Si un endpoint REST arrive plus tard, il pourra projeter ces valeurs dans une reponse plate, mais cette projection ne doit pas piloter le runtime.

## 7. Rapport au runner existant

Reponse nette: le runner doit etre encapsule legerement, utilise tel quel, et non adapte pour cette etape.

- Utilise tel quel pour les deux operations existantes.
- Encapsule par une facade applicative qui choisit la methode a appeler.
- Pas de modification du runner tant que le contrat `WorkflowStepResult.data` suffit.

Le runner a deja la bonne responsabilite: entree runtime + summary documentaire. La facade doit rester au-dessus, cote usage applicatif, sans deplacer cette responsabilite.

## 8. Gestion RUN vs RESUME

Solution recommandee: deux methodes publiques explicites.

- `runAnalysisReview(context)` appelle `runner.runAnalysisReviewWithOptionalCorrection(context)`.
- `resumeCorrection(context)` appelle `runner.runCorrectionAfterReview(context)`.

Si un point d'entree textuel est necessaire plus tard, accepter un simple string `mode` (`RUN` ou `RESUME`) au bord du systeme, puis router vers ces deux methodes. Ne pas introduire de DSL, de moteur de commande, ni d'enum complexe maintenant.

Semantique:
- RUN: execute analyse -> review -> correction optionnelle, avec arret possible sur `WAIT_HUMAN` ou `STOP_FAILURE`.
- RESUME: execute uniquement la correction, apres modification humaine du review existant.

## 9. Observabilite cote appelant

Le caller comprend le resultat avec deux niveaux:

1. Niveau machine: `WorkflowStepResult`
   - `decision` indique l'etat final technique: `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`.
   - `data.finalDecision` stabilise la decision exposee.
   - `data.nextAction` donne l'action recommandee.
   - `data.workflowSummaryPath` donne le chemin du recapitulatif quand disponible.
   - les cles finissant par `Path` pointent vers les artefacts produits.

2. Niveau humain: `workflow-summary.md`
   - format texte simple.
   - decision, raison, correction declenchee, artefacts, prochaines etapes.
   - en cas de `WAIT_HUMAN`, indique explicitement quel review modifier et comment reprendre.

Point important: la generation du summary est non bloquante. Un caller doit donc savoir fonctionner meme sans `workflowSummaryPath`, en s'appuyant sur `decision`, `message` et `nextAction`.

## 10. Preparation Telegram

A garantir maintenant pour Telegram plus tard:
- `message` et `nextAction` doivent rester lisibles en texte simple.
- `workflow-summary.md` doit rester comprehensible sans parsing complexe.
- `finalDecision`, `nextAction`, `waitReason`, `workflowSummaryPath` doivent rester stables comme cles exposees.
- Le canal Telegram ne doit pas avoir besoin de connaitre les details de `WorkflowOrchestrator` ni des steps.
- Les chemins d'artefacts doivent etre explicites pour permettre une notification ou un lien futur.
- Le resultat doit etre consommable sans etat implicite cache ailleurs que les artefacts et le contexte fourni.

A ne pas faire maintenant:
- formatter des messages Telegram dedies.
- ajouter un publisher Telegram.
- introduire une persistance de conversation ou d'etat utilisateur.

## 11. Risques de sur-conception

Risques principaux:
- Creer un `WorkflowEngine` generique alors que le runner existe deja.
- Ajouter un DTO d'entree/sortie riche qui duplique `WorkflowExecutionContext` et `WorkflowStepResult`.
- Introduire un enum de commande, un dispatcher, un bus ou un routeur dynamique pour seulement deux operations.
- Deplacer de la logique de controle de flux hors de `WorkflowOrchestrator`.
- Faire de la facade un second orchestrator.
- Coupler prematurement l'exposition a REST ou Telegram.
- Ajouter une persistance avancee de run alors que la reprise actuelle repose volontairement sur les artefacts.

## 12. Points d'architecture sensibles

- Le wiring applicatif: actuellement les composants runtime ne sont pas tous des beans Spring. L'implementation devra choisir une configuration minimale sans transformer chaque classe en service si ce n'est pas necessaire.
- Les cles string dans `WorkflowStepResult.data`: elles sont assumeees en phase 4, mais il faut eviter d'en creer de nouvelles inutilement.
- Le summary conditionnel: un caller externe ne doit pas supposer qu'il existe toujours.
- La reprise RESUME depend des artefacts existants `result.<step>.analysis.md` et `result.<step>.review.md`; le contrat d'appel doit le dire clairement.
- Les chemins phase/version doivent etre coherents, sinon les steps chercheront les mauvais artefacts.

## 13. Plan d'implementation minimal propose

1. Creer une facade applicative explicite, par exemple `AnalysisReviewWorkflowCommandService`.
2. Injecter ou construire via configuration minimale `AnalysisReviewWorkflowRunner` et ses dependances.
3. Exposer deux methodes: `run(context)` et `resumeCorrection(context)`.
4. Deleguer directement au runner, sans logique de workflow additionnelle.
5. Ajouter une petite methode privee optionnelle pour extraire un fallback stable si `workflowSummaryPath` manque, uniquement si un appelant en a besoin.
6. Ajouter des tests unitaires de facade: RUN delegue a `runAnalysisReviewWithOptionalCorrection`, RESUME delegue a `runCorrectionAfterReview`.
7. Ajouter un test d'integration leger seulement si le wiring Spring est introduit.
8. Documenter brievement le contrat d'entree/sortie dans le rapport d'implementation futur ou dans un commentaire court si necessaire.

## 14. Ce qui doit rester volontairement absent

- Pas de moteur d'agent.
- Pas d'orchestration avancee.
- Pas de multi-utilisateur.
- Pas de systeme de permissions.
- Pas d'API REST complete.
- Pas de DTO complexe.
- Pas de bus d'evenements.
- Pas de persistance avancee.
- Pas de DSL.
- Pas de routing dynamique.
- Pas de refactor global du runtime.
- Pas d'integration Telegram.

## Conclusion

Pour cette etape, le meilleur choix n'est pas l'exposition brute du runner a tous les appelants, ni une nouvelle architecture. Le bon compromis est une facade applicative tres mince, explicite, centree sur `AnalysisReviewWorkflowRunner`, avec deux operations: RUN complet et RESUME correction.

Cette approche rend le workflow appelable depuis l'exterieur, prepare naturellement une CLI, un endpoint technique ou Telegram plus tard, et preserve le coeur runtime sans duplication ni anticipation excessive.
