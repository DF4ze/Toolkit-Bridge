# Rapport d'analyse — Phase 6 Étape 2 — Intégration Maven

## Résumé exécutif

L'architecture actuelle supporte l'ajout d'un step de validation Maven sans modification des
classes existantes (`WorkflowStepResult`, `WorkflowStep`, `WorkflowOrchestrator`), à l'exception
de `AnalysisReviewWorkflowRunner` et `AnalysisReviewWorkflowRunnerFactory` qui devront être étendus.

La `WorkflowValidationService` est prête. Il suffit de créer un `MavenValidationStep` qui l'adapte
au contrat `WorkflowStep → WorkflowStepResult`, puis de le câbler dans le runner.

**Conclusion : prêt.**

---

## 1. Où intégrer la validation Maven ?

### Réponse : dans un step dédié `MavenValidationStep`

Ni dans `GlobalReviewStep`, ni dans `CorrectionStep`.

**Pourquoi pas dans `CorrectionStep` ?**
`CorrectionStep` envoie un prompt à Codex et récupère un résultat. Sa responsabilité s'arrête là.
Lui ajouter de la logique de compilation crée un couplage entre l'IA et le build local — deux
domaines indépendants. Un test unitaire de `CorrectionStep` deviendrait fragile.

**Pourquoi pas dans `GlobalReviewStep` ?**
La review est une analyse sémantique portée par Codex. Elle n'a aucune raison de lancer Maven.

**Pourquoi un step dédié ?**
- Il respecte le principe de responsabilité unique.
- Il peut être injecté optionnellement selon les workflows (validation optionnelle).
- Il est testable de manière complètement indépendante.
- Il s'intègre naturellement dans le pattern `WorkflowStep` existant.

---

## 2. Faut-il créer un `ValidationStep` ?

### Réponse : oui — nommé `MavenValidationStep`

**Avantages :**
- Respecte le pattern existant : `WorkflowStep.execute(context) → WorkflowStepResult`
- Testable en isolation complète (mock de `WorkflowValidationService`)
- Séparation nette entre compilation (local) et intelligence (Codex)
- Peut être réutilisé dans un futur workflow différent

**Inconvénients :**
- Ajoute un fichier supplémentaire
- Demande d'étendre le runner et la factory

Il n'y a pas d'alternative raisonnable : intégrer la logique Maven directement dans `CorrectionStep`
ou dans le runner serait une violation de l'architecture en place.

---

## 3. Quelle commande exacte utiliser ?

### Règle principale : `mvnw`, pas `mvn`

Le Maven Wrapper garantit la version Maven correcte pour le projet. `mvn` dépend de l'installation
locale et peut différer. Le projet dispose de `mvnw` et `mvnw.cmd` à la racine.

### Multi-OS

| OS | Commande | Détail |
|---|---|---|
| Windows | `cmd.exe /c mvnw.cmd clean compile -DskipTests` | `cmd.exe` toujours résolvable ; `mvnw.cmd` est un batch file |
| Linux/Mac | `./mvnw clean compile -DskipTests` | script shell standard |

Sur Windows, le même problème de PATH rencontré à l'étape 1 s'applique. `mvnw.cmd` est un fichier
batch (`.cmd`) — il ne peut pas être lancé directement par `ProcessBuilder` sans passer par
`cmd.exe`. La commande Windows est donc :

```
List.of("cmd.exe", "/c", "mvnw.cmd", "clean", "compile", "-DskipTests")
```

Le répertoire de travail (`workingDirectory`) doit pointer vers la **racine du projet** (là où
`pom.xml` et `mvnw.cmd` sont présents). Ce chemin sera passé via une variable de contexte.

### `-DskipTests`

Les tests ne doivent pas être exécutés ici : on veut uniquement vérifier la compilation.
Les tests sont lents, peuvent avoir des effets de bord, et ne sont pas l'objectif de cette
validation. `compile` seul (sans `clean`) pourrait laisser des artefacts stale ; `clean compile`
est plus fiable.

---

## 4. Mapping `ValidationResult` → `WorkflowStepDecision`

| `ValidationStatus` | `WorkflowStepDecision` | Justification |
|---|---|---|
| `SUCCESS` (exit 0) | `CONTINUE` | Build OK — le code compile |
| `FAILURE` (exit ≠ 0) | `STOP_FAILURE` | Erreur de compilation — Codex a produit du code invalide |
| `TIMEOUT` | `STOP_FAILURE` | Build trop long — probablement un environnement cassé |
| `SYSTEM_ERROR` | `STOP_FAILURE` | Maven ou JVM non trouvé — problème d'environnement |

**Pourquoi `STOP_FAILURE` et non `RETRY_CORRECTION` pour `FAILURE` ?**

`RETRY_CORRECTION` n'est pas géré par `WorkflowOrchestrator.executeSingleStep()` — il lève une
`IllegalStateException`. L'orchestrateur ne boucle pas. Si le build échoue, l'erreur de compilation
doit être remontée à l'humain pour investigation (les détails de stdout/stderr de Maven sont
précieux). Un retry aveugle sur Codex sans feedback structuré ne serait pas fiable.

Les données utiles à inclure dans le `WorkflowStepResult.data` :
- `buildStatus` : la valeur de `ValidationStatus`
- `buildExitCode` : l'exit code Maven
- `buildDurationMs` : durée du build
- `buildResultPath` : chemin vers le fichier artifact contenant stdout/stderr Maven

---

## 5. Position dans le cycle

### Après `CorrectionStep`, avant la fin du cycle

La séquence complète devient :

```
GlobalAnalysisStep
  → (CONTINUE) → GlobalReviewStep
    → (CONTINUE, requiresCorrection=false) → [fin sans correction] → CONTINUE
    → (CONTINUE, requiresCorrection=true) → CorrectionStep
      → (CONTINUE) → MavenValidationStep
        → (CONTINUE) → fin du cycle — le build compile
        → (STOP_FAILURE) → build KO — reporter à l'humain
    → (WAIT_HUMAN) → attente humaine
    → (STOP_FAILURE) → échec
  → (STOP_FAILURE/WAIT_HUMAN) → propagation
```

**Question : faut-il valider aussi quand `requiresCorrection=false` ?**

Oui, pour avoir un signal systématique. Si Codex n'a pas touché au code mais que la codebase
ne compile pas, c'est utile de le savoir. La validation devient un test de régression systématique.
La position la plus propre : toujours après le cycle analyse/review/correction, quel que soit
le chemin suivi.

---

## 6. Impacts sur les classes existantes

### `WorkflowStepResult`
**Aucun impact.** Le record est générique par conception. Les champs de build seront passés dans
`Map<String, Object> data` comme les autres steps le font.

### `WorkflowOrchestrator`
**Impact minimal.** L'orchestrateur dispose de `executeSingleStep`, `executeTwoSteps`, et
`executeThreeSteps`. Il faudra ajouter :

```
executeAnalysisReviewWithCorrectionAndValidation(
    context, analysisStep, reviewStep, correctionStep, validationStep)
```

qui reprend `executeAnalysisReviewWithOptionalCorrection` en chaînant le step de validation
après le cycle existant. La logique de chaînage est déjà établie.

### `AnalysisReviewWorkflowRunner`
**Modification requise.** Ajout d'un 4ème paramètre `validationStep` dans le constructeur + une
nouvelle méthode publique `runAnalysisReviewWithValidation(context)`. L'ancienne méthode
`runAnalysisReviewWithOptionalCorrection` reste intacte (backward compatible).

### `AnalysisReviewWorkflowRunnerFactory`
**Modification requise.** Instanciation de `MavenValidationStep` dans `createDefault()`.

### Steps existants (`GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`)
**Aucun impact.** Aucune modification.

---

## 7. Risques

### Lenteur (risque : élevé)
`mvn clean compile` sur un projet Spring Boot : 30 à 90 secondes en conditions normales.
Premier lancement avec téléchargement de dépendances : potentiellement > 2 minutes.

**Mitigation :** utiliser un timeout explicite de 180 à 300 secondes dans `MavenValidationStep`.
Le default de 120s dans `WorkflowValidationService` est trop court pour un cold start.

### Faux positifs (risque : modéré)
Si la codebase a des erreurs de compilation avant l'intervention de Codex, la validation
échouera toujours — Codex sera injustement accusé. Le résultat serait un `STOP_FAILURE`
trompeur.

**Mitigation :** envisager une validation pré-cycle ("base compilation gate") dans une étape
ultérieure. Pour l'étape 2, ce n'est pas critique.

### Dépendance environnement (risque : modéré)
- `JAVA_HOME` doit être défini et pointer vers un JDK (pas un JRE)
- `mvnw.cmd` doit être exécutable (droits fichier)
- Connexion internet ou dépôt local (`~/.m2`) si les dépendances ne sont pas en cache

**Mitigation :** `SYSTEM_ERROR` couvre ce cas. Les messages d'erreur Maven seront capturés
dans `stderr` et remontés dans l'artifact.

### Multi-OS (risque : faible pour le projet actuel)
La machine de développement est Windows. La commande Windows avec `cmd.exe /c mvnw.cmd`
est déjà validée indirectement (le test `returnsSuccessForSuccessfulCommand` utilise
`cmd /c echo ok`).

Le risque est résiduel si le projet est exécuté sur Linux/Mac sans adaptation. À documenter.

### Durée totale du cycle (risque : faible)
Le cycle complet (Codex analysis + Codex review + Codex correction + Maven build) peut
dépasser 5 minutes. Ce n'est pas un problème fonctionnel mais un point d'attention UX.

---

## 8. Plan d'implémentation

| # | Action | Fichiers touchés |
|---|---|---|
| 1 | Créer `MavenValidationStep` — implémente `WorkflowStep`, injecte `WorkflowValidationService` + `WorkflowArtifactService`. Construit la `ValidationCommand` selon l'OS, mappe `ValidationResult` → `WorkflowStepResult`. | `step/MavenValidationStep.java` (nouveau) |
| 2 | Écrire `MavenValidationStepTest` — tests des 4 statuts (SUCCESS, FAILURE, TIMEOUT, SYSTEM_ERROR), null context, variables manquantes | `step/MavenValidationStepTest.java` (nouveau) |
| 3 | Ajouter `executeAnalysisReviewWithCorrectionAndValidation` dans `WorkflowOrchestrator` — chaîne le step de validation après le cycle existant | `orchestrator/WorkflowOrchestrator.java` |
| 4 | Écrire les tests de la nouvelle méthode de `WorkflowOrchestrator` | `orchestrator/WorkflowOrchestratorTest.java` |
| 5 | Ajouter `validationStep` dans `AnalysisReviewWorkflowRunner` — nouveau paramètre constructeur + méthode `runWithValidation(context)` | `AnalysisReviewWorkflowRunner.java` |
| 6 | Écrire les tests de `AnalysisReviewWorkflowRunner` avec validation | `AnalysisReviewWorkflowRunnerTest.java` |
| 7 | Mettre à jour `AnalysisReviewWorkflowRunnerFactory.createDefault()` — instancier `MavenValidationStep` | `cli/AnalysisReviewWorkflowRunnerFactory.java` |
| 8 | Vérifier build complet : `mvnw test` | — |

---

## Conclusion

**Prêt.**

L'architecture existante absorbe l'ajout sans refactor majeur. `WorkflowStep` est une interface
fonctionnelle générique : `MavenValidationStep` s'y branche naturellement. La `WorkflowValidationService`
est déjà testée et opérationnelle. Les deux seules classes à étendre (`WorkflowOrchestrator` et
`AnalysisReviewWorkflowRunner`) ont des patterns d'extension clairs et répétables.

Aucune modification des steps existants, de `WorkflowStepResult`, ni des modèles du domaine.

---

*Phase 6 / Étape 2 — analyse terminée.*
