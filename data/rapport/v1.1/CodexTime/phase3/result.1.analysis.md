# Rapport d’analyse — Phase 3 Étape 1 — GlobalReviewStep

## 1) Package recommandé

Package recommandé :
`fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step`

Raison : `GlobalAnalysisStep` est déjà dans ce package, `GlobalReviewStep` est une step métier de même niveau et doit rester au même endroit pour garder une lecture simple du runtime workflow.

## 2) Classes à créer / modifier (analyse)

À créer lors de l’implémentation :
- `GlobalReviewStep` (nouvelle classe)
  - chemin cible : `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `GlobalReviewStepTest` (tests unitaires)
  - chemin cible : `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

À modifier pour cette étape (recommandation) :
- aucune classe existante obligatoire.

Optionnel plus tard (hors étape 1) :
- runner/orchestrator multi-step (prévu étape 2/3 de la mini-roadmap phase 3), mais pas nécessaire pour introduire la step elle-même.

## 3) Responsabilité exacte de `GlobalReviewStep`

### Ce que la step doit faire
- lire dans le `WorkflowExecutionContext` les variables minimales de localisation des artefacts (`reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`).
- reconstruire les chemins d’artefacts d’analyse via `WorkflowArtifactService.buildArtifactPath(..., ANALYSIS_PROMPT/ANALYSIS_RESULT)`.
- lire les contenus d’analyse via `WorkflowArtifactService.readArtifact(...)`.
- construire un prompt de review simple à partir de ces contenus + métadonnées de run.
- appeler `CodexWorkflowClient` avec `CodexExecutionRequest`.
- écrire `REVIEW_PROMPT` puis `REVIEW_RESULT` via `WorkflowArtifactService`.
- retourner un `WorkflowStepResult`.

### Ce que la step ne doit pas faire
- pas d’orchestration de plusieurs steps.
- pas de logique de correction automatique.
- pas de décision globale de workflow.
- pas de boucle/retry.
- pas de parsing sémantique avancé.
- pas de couplage Telegram/Maven.

## 4) Dépendances minimales

Dépendances directes à utiliser :
- `WorkflowExecutionContext`
- `WorkflowArtifactService`
- `WorkflowArtifactType`
- `CodexWorkflowClient`
- (`CodexExecutionRequest` / `CodexExecutionResult` pour l’appel)

À ne pas absorber dans la step :
- responsabilité d’orchestrator (`WorkflowOrchestrator`).
- décisions de pilotage multi-step.
- logique de transport humain (WAIT_HUMAN avancé, Telegram).
- logique d’implémentation/correction métier.

## 5) Rapport aux artefacts existants (sans dupliquer le nommage)

Approche recommandée :
- ne jamais concaténer les noms de fichiers “à la main”.
- utiliser uniquement `WorkflowArtifactService.buildArtifactPath(...)` + `WorkflowArtifactType`.

Lecture attendue :
- source 1 : `ANALYSIS_PROMPT`
- source 2 : `ANALYSIS_RESULT`

Sorties attendues :
- `REVIEW_PROMPT`
- `REVIEW_RESULT`

Cette approche garde une seule source de vérité pour la convention de nommage (`WorkflowArtifactType.fileNameForStep`).

## 6) Rapport au contexte runtime

Réponse demandée : **3. le contexte actuel suffit avec conventions simples**.

Conventions simples à figer :
- `stepNumber` représente le numéro de lot/famille d’artefacts.
- analyse et review d’un même lot partagent le même `stepNumber`.
- `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber` sont obligatoires.

Pas de nouvel objet de contexte requis pour une V1.

## 7) Rapport à Codex (sans dérive orchestrator)

Utilisation recommandée de `CodexWorkflowClient` :
- un seul appel Codex par exécution de step.
- la step prépare un prompt, exécute, persiste le résultat, puis retourne un `WorkflowStepResult`.
- elle ne choisit pas la step suivante, ne relance pas automatiquement, ne boucle pas.

Politique de retour V1 :
- résultat Codex réussi -> `CONTINUE`.
- résultat Codex non réussi / timeout / exception technique -> `STOP_FAILURE`.

## 8) Résultat `WorkflowStepResult` recommandé (V1)

Décision :
- `CONTINUE` en nominal.
- `STOP_FAILURE` sur erreur.

Message :
- nominal : `Global review completed`
- erreur : préfixe explicite (ex: `GlobalReviewStep: ...`) comme `GlobalAnalysisStep`.

Data minimal :
- `promptArtifactPath` (chemin vers `REVIEW_PROMPT`)
- `resultArtifactPath` (chemin vers `REVIEW_RESULT`)

Important :
- ne pas dépendre de `WorkflowStepResult.data` d’une step précédente pour fonctionner.

## 9) Réponse explicite à la question clé (StepResult.data vs reconstruction)

Recommandation nette :
**`GlobalReviewStep` doit reconstruire les chemins via `WorkflowArtifactService` à partir des conventions de contexte, et ne pas dépendre de `WorkflowStepResult.data` de la step précédente.**

Arguments :
- couplage plus faible entre steps.
- robustesse : la review peut être rejouée indépendamment si les artefacts existent.
- cohérence : un seul mécanisme de résolution de chemins (service + enum).
- simplicité : pas de pipeline de “propagation de chemins” entre résultats de steps.

`WorkflowStepResult.data` reste utile pour diagnostic et observabilité, pas comme mécanisme principal d’entrée pour la step suivante.

## 10) Risques de sur-conception à éviter

Risques principaux :
- transformer `GlobalReviewStep` en mini-orchestrator.
- introduire un moteur de règles de review/correction.
- créer des abstractions génériques prématurées (pipeline/graph/state machine).
- parser sémantiquement le résultat d’analyse en profondeur dès la V1.

Garde-fous :
- 1 input contract simple (contexte + artefacts).
- 1 appel Codex.
- 2 artefacts produits.
- 1 `WorkflowStepResult` simple.

## 11) Plan d’implémentation minimal (sobre)

1. Créer `GlobalReviewStep` dans le package `runtime.step` avec les deux dépendances (`CodexWorkflowClient`, `WorkflowArtifactService`).
2. Reprendre le même pattern de validation de variables que `GlobalAnalysisStep` (sans extraction utilitaire globale à ce stade).
3. Construire les chemins des artefacts d’analyse via `WorkflowArtifactService` + `WorkflowArtifactType`.
4. Lire `ANALYSIS_PROMPT` et `ANALYSIS_RESULT`.
5. Construire un prompt de review simple (runId, workflowType, targetStepRef + contenu d’analyse).
6. Écrire l’artefact `REVIEW_PROMPT`.
7. Appeler `CodexWorkflowClient` (workingDirectory/timeout optionnels depuis contexte).
8. Écrire l’artefact `REVIEW_RESULT` puis retourner `WorkflowStepResult` (`CONTINUE` ou `STOP_FAILURE`).
9. Ajouter des tests unitaires dédiés (`nominal`, variable manquante, échec Codex, vérification des artefacts).

## 12) Ce qui doit rester volontairement absent de cette étape

- correction automatique
- retry
- boucle
- multi-step avancé
- Telegram
- Maven
- décisions humaines avancées
- parsing sémantique lourd
- refactor structurel transverse du runtime workflow

## 13) Points d’architecture sensibles observés dans l’existant

- la validation des variables est actuellement locale dans `GlobalAnalysisStep` (copie potentielle dans `GlobalReviewStep`) ; acceptable en V1, à factoriser seulement si une 3e/4e step confirme la duplication.
- `WorkflowExecutionContext.variables` est non typé (`Map<String,Object>`) ; pour cette étape, garder ce choix et documenter les clés obligatoires, plutôt que d’introduire un nouveau modèle prématuré.
- `WorkflowOrchestrator` n’interprète pas encore `FINISH/RETRY_CORRECTION` ; ne pas étendre ce comportement dans cette étape d’introduction de step.
