# Rapport d’analyse — Phase 3 Étape 4 — Introduction de `CorrectionStep`

## 1) Recommandation globale

Recommandation nette et sobre :
- introduire une nouvelle step concrète `CorrectionStep` dans le même package que les steps runtime existantes,
- la garder strictement responsable de produire des artefacts de correction à partir d’artefacts amont,
- ne pas la coupler à la logique d’orchestration ni à un cas particulier (`WAIT_HUMAN`) dans cette étape.

## 2) Package recommandé (A)

- `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step`

Pourquoi :
- cohérent avec `GlobalAnalysisStep` et `GlobalReviewStep`,
- garde une topologie simple : une step métier = une classe dans `step`.

## 3) Classes à créer / modifier

À créer :
- `CorrectionStep` (`implements WorkflowStep`).

À modifier (minimal) :
- `src/test/.../step/` : ajouter `CorrectionStepTest`.

À ne pas modifier dans cette étape :
- `WorkflowOrchestrator`,
- modèles runtime (`WorkflowExecutionContext`, `WorkflowStepResult`),
- `WorkflowArtifactType` (les types `CORRECTION_*` existent déjà).

## 4) Responsabilité exacte de `CorrectionStep` (B)

Doit faire :
1. valider les variables minimales du contexte (report root/version/phase/step),
2. reconstruire les chemins d’artefacts requis via `WorkflowArtifactService.buildArtifactPath(...)`,
3. vérifier présence des artefacts d’entrée requis,
4. lire ces artefacts,
5. construire un prompt de correction simple,
6. exécuter `CodexWorkflowClient`,
7. écrire `CORRECTION_PROMPT` et `CORRECTION_RESULT`,
8. retourner `WorkflowStepResult` (`CONTINUE` ou `STOP_FAILURE`).

Ne doit pas faire :
- décider quand elle doit être appelée,
- déclencher des retries,
- boucler sur correction/review,
- gérer une priorisation de corrections,
- gérer Telegram/humain/Maven.

## 5) Dépendances minimales (C)

Dépendances directes recommandées :
- `WorkflowExecutionContext`,
- `WorkflowArtifactService`,
- `WorkflowArtifactType`,
- `CodexWorkflowClient`.

Dépendances à ne pas absorber :
- `WorkflowOrchestrator` (pas d’appel interne),
- mémoire/policy/workspace/routing,
- services de supervision humaine.

## 6) Artefacts d’entrée recommandés (D)

Réponse nette : **3. un sous-ensemble précis analyse + review**.

Sous-ensemble V1 recommandé :
- `ANALYSIS_RESULT` (contexte technique initial),
- `REVIEW_RESULT` (constats/retours à corriger).

Optionnel en V1 :
- `ANALYSIS_PROMPT` ou `REVIEW_PROMPT` seulement si nécessaire au format du prompt de correction.

Pourquoi ce choix :
- `ANALYSIS_RESULT` seul manque les critiques,
- `REVIEW_RESULT` seul manque le contexte,
- le duo résultat analyse + résultat review est minimal et utile.

## 7) Rapport aux artefacts (E)

Recommandation :
- reconstruire tous les chemins via `WorkflowArtifactService.buildArtifactPath(...)`,
- ne jamais hardcoder les noms de fichiers,
- réutiliser le même schéma de validation que les steps existantes (`artifactExists` avant lecture).

Bénéfice :
- pas de duplication de logique de nommage,
- cohérence forte avec l’existant.

## 8) Rapport au contexte runtime (F)

Réponse nette : **3. le contexte actuel suffit avec conventions simples**.

Variables minimales requises (déjà utilisées par les autres steps) :
- `reportRootDirectory`,
- `reportVersion`,
- `reportPhase`,
- `stepNumber`,
- (optionnelles) `codexWorkingDirectory`, `codexTimeoutSeconds`.

Aucun enrichissement de `WorkflowExecutionContext` nécessaire pour cette étape.

## 9) Rapport à Codex (G)

Usage recommandé :
- exactement le pattern existant : `new CodexExecutionRequest(prompt, optionalPath, optionalInt)` puis `codexWorkflowClient.execute(request)`.
- traiter uniquement le résultat de cette exécution locale à la step.

Garde-fou anti-dérive orchestrator :
- pas de branchement multi-step,
- pas d’appel en cascade,
- pas de logique de re-planification.

## 10) Résultat de step recommandé (H)

V1 simple :
- `CONTINUE` si exécution Codex réussie,
- `STOP_FAILURE` si artefacts d’entrée manquants, variables invalides, exception ou exécution non-success/timeout.

`data` recommandé :
- `promptArtifactPath`,
- `resultArtifactPath`.

Pas de `WAIT_HUMAN` dans `CorrectionStep` V1 (non nécessaire ici).

## 11) Lien avec `WAIT_HUMAN` (I + question importante)

Réponse nette :
- `CorrectionStep` doit rester **indépendante** et supposer que ses artefacts requis existent.
- Le fait de l’exécuter après `WAIT_HUMAN` ou review négative relève de l’orchestration future, pas de la step.

Donc, pour la question importante :
- **choix 1** recommandé : `CorrectionStep` réutilisable dès que les artefacts nécessaires existent.
- **ne pas** la coupler explicitement dès maintenant à `WAIT_HUMAN`/review négative.

Pourquoi :
- évite un couplage prématuré step <-> politique de pilotage,
- garde une step métier testable et bornée,
- prépare les futures phases sans framework.

## 12) Risques de sur-conception

Risques principaux :
- introduire un compteur de tentatives dans la step,
- ajouter des branches conditionnelles riches selon contenu de review,
- transformer la step en mini orchestrator (retry/replan),
- introduire un parser sémantique lourd.

## 13) Plan d’implémentation minimal (9 étapes)

1. Créer `CorrectionStep` dans `runtime.step`.
2. Injecter `CodexWorkflowClient` + `WorkflowArtifactService`.
3. Reprendre les constantes de variables contexte déjà utilisées (`report*`, `stepNumber`, options codex).
4. Résoudre les paths d’entrée (`ANALYSIS_RESULT`, `REVIEW_RESULT`) via `buildArtifactPath`.
5. Vérifier présence des artefacts d’entrée (`artifactExists`) sinon `STOP_FAILURE`.
6. Lire les contenus d’entrée et construire un prompt de correction simple.
7. Écrire `CORRECTION_PROMPT`.
8. Exécuter Codex, écrire `CORRECTION_RESULT`, retourner `CONTINUE` ou `STOP_FAILURE`.
9. Ajouter `CorrectionStepTest` sur nominal + erreurs (variables manquantes, artefacts manquants, codex failure/timeout).

## 14) Hors périmètre explicite (J)

Doit rester volontairement absent :
- boucle automatique de retry,
- compteur de tentatives,
- orchestration complète de correction,
- Telegram,
- Maven,
- décisions humaines avancées,
- parsing sémantique lourd,
- moteur de planification,
- stratégie configurable complexe.

## 15) Conclusion

La solution V1 la plus propre est une `CorrectionStep` autonome, minimale et strictement métier, alignée sur les patterns de `GlobalAnalysisStep`/`GlobalReviewStep`, utilisant les artefacts existants (surtout `ANALYSIS_RESULT` + `REVIEW_RESULT`) et laissant toute logique de séquencement/conditions à l’orchestrator des phases suivantes.
