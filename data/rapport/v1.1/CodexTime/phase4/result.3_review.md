# Revue critique d’architecture — Phase 4 Étape 3

Périmètre relu intégralement :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/GlobalAnalysisWorkflowRunner.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/GlobalAnalysisWorkflowRunnerResumeTest.java`
- vérification de cohérence avec `WorkflowOrchestrator` et tests existants (`WorkflowOrchestratorTest`, `GlobalAnalysisWorkflowRunnerTest`).

---

## 1) Faiblesses / points discutables

### [P1] Nom du runner trop spécifique vs responsabilité réelle
- Fichier concerné : `GlobalAnalysisWorkflowRunner.java` (classe entière)
- Point : le nom “GlobalAnalysisWorkflowRunner” suggère un runner centré “analysis”, alors qu’il orchestre `analysis + review + correction` et la reprise manuelle.
- Risque : confusion fonctionnelle dans les phases suivantes, difficulté de découverte des responsabilités.

### [P1] Intégration runtime incomplète (classe non branchée dans la config Spring)
- Fichier concerné : `GlobalAnalysisWorkflowRunner.java`
- Point : classe instanciable mais sans annotation/composition visible (`@Service` ou `@Bean`) dans la couche `config`.
- Risque : duplication d’instanciation ad hoc dans futurs points d’entrée, affaiblissement de la séparation configuration/runtime.

### [P2] Contrat de wiring faible via `WorkflowStep` générique
- Fichier concerné : `GlobalAnalysisWorkflowRunner.java:13-24`
- Point : injection de `WorkflowStep` génériques pour analysis/review/correction.
- Risque : erreur de câblage possible (mauvais step au mauvais rôle) sans garde-fou de type.
- Note : c’est simple et acceptable en V1, mais un peu fragile.

### [P2] Couverture tests reprise utile mais partiellement “simulation pure”
- Fichier concerné : `GlobalAnalysisWorkflowRunnerResumeTest.java`
- Point : les tests de reprise utilisent des lambdas et ne valident pas le flux artefact réel (`ANALYSIS_RESULT` + `REVIEW_RESULT` relus par `CorrectionStep`).
- Risque : le contrat métier “source de vérité = artefacts” est validé indirectement, pas explicitement sur le point d’entrée de reprise.

### [P3] Détail ad hoc dans test
- Fichier concerné : `GlobalAnalysisWorkflowRunnerResumeTest.java:27`
- Point : chemin hardcodé `data/rapport/v1.1/CodexTime/Phase4/result.3.correction.md` dans une donnée de test sans impact réel.
- Risque : bruit, dépendance cosmétique à un chemin spécifique non nécessaire pour tester la logique.

### [P3] Test de robustesse manquant sur null-check
- Fichier concerné : `GlobalAnalysisWorkflowRunnerResumeTest.java`
- Point : pas de test explicite sur `runCorrectionAfterReview(null)`.
- Risque : faible (la méthode protège déjà), mais couverture incomplète du contrat d’entrée.

---

## 2) Corrections utiles (sans ajout de fonctionnalités)

1. Renommer la classe pour refléter sa responsabilité réelle
- Proposition : `AnalysisReviewWorkflowRunner` (ou `WorkflowRuntimeRunner`).
- Impact : lisibilité et maintenabilité uniquement.

2. Brancher explicitement le runner dans la couche de configuration
- Ajouter un wiring Spring explicite (service ou bean unique) pour éviter les instanciations dispersées.
- Objectif : séparation claire config/runtime, sans changer le comportement.

3. Durcir légèrement le contrat de câblage interne
- Option minimale : conserver `WorkflowStep` mais documenter clairement les rôles attendus analysis/review/correction au point d’assemblage.
- Option plus stricte (toujours sobre) : types concrets au constructeur.
- Recommandation V1 : documentation/wiring clair, sans gros refactor.

4. Renforcer les tests de reprise sur le contrat utile
- Ajouter un test `null context`.
- Ajouter au moins un test de reprise avec steps réels (ou mock `CorrectionStep` vérifiant appel unique) pour rendre explicite l’intention “reprise = exécuter correction”.
- Garder le scope actuel, pas d’extension fonctionnelle.

5. Nettoyer les détails ad hoc de test
- Remplacer la valeur de chemin hardcodée par une donnée neutre (`resultArtifactPath = "x"`) si non utilisée par l’assertion.

---

## 3) Vérification des critères demandés

### Séparation configuration / runtime
- Globalement bonne dans le code métier (pas de mélange config dans le runner).
- Point à corriger : absence de branchement explicite du runner côté configuration.

### Qualité du modèle implémenté
- Modèle simple, lisible, minimaliste, aligné avec la demande.
- Faiblesse : nommage et contrat de câblage trop générique.

### Découplage orchestrator / mémoire / tooling / policy / workspace
- Bon découplage : le runner ne dépend que d’orchestrator + steps + context.
- Aucun couplage direct à mémoire, tooling, policy ou workspace introduit.

### Absence de logique ad hoc ou trop spécifique
- Runtime : correct, pas de logique ad hoc.
- Tests : un petit détail ad hoc (chemin hardcodé) à nettoyer.

### Absence de couplage bloquant futures phases
- Aucun couplage majeur bloquant.
- Risque modéré de confusion future si le nom et le wiring restent en l’état.

### Cohérence des noms
- `runCorrectionAfterReview` : bon nom.
- `GlobalAnalysisWorkflowRunner` : discutable vu le périmètre réel.

### Lisibilité générale
- Bonne lisibilité, code court, responsabilités claires.

### Tests réellement utiles
- Oui sur le comportement de reprise (CONTINUE/STOP_FAILURE/exécution).
- Lacune : pas de test null-check et pas de validation explicite du contrat artefact via un test de reprise plus “métier”.

### Dette technique introduite
- Dette légère : nommage + wiring incomplet + tests perfectibles.

### Risques de refactor futur évitables maintenant
- Oui, principalement via renommage cohérent et branchement explicite unique du runner.

---

## 4) Résumé final

Le lot est globalement propre, sobre et conforme au périmètre : la reprise manuelle est implémentée sans moteur de workflow ni couplages indésirables.

Les points à améliorer sont principalement structurels (naming + wiring + quelques tests de robustesse), sans remettre en cause la direction technique. Ces corrections sont légères, localisées, et permettent d’éviter une dette de lisibilité/câblage avant les phases suivantes.
