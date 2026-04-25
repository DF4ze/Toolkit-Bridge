# Rapport d'analyse — Phase 6 Étape 1 — Validation locale minimale

---

## Résumé exécutif

Le projet dispose déjà d'un pattern complet et testé d'exécution de processus externe : `CodexWorkflowClient` avec `CodexExecutionRequest` / `CodexExecutionResult`. Ce pattern couvre exactement les besoins de la validation locale (ProcessBuilder, flux concurrents, timeout, destroy/destroyForcibly).

Le service `WorkflowValidationService` devra suivre ce pattern sans le réutiliser directement, afin de rester découplé de la couche Codex. Les objets métier `ValidationCommand`, `ValidationResult` et `ValidationStatus` sont à créer dans un package dédié.

Le risque principal est la **sécurité** : ne jamais passer une chaîne libre à un shell. L'utilisation de `List<String>` dans `ProcessBuilder` évite ce risque.

L'implémentation est **prête** à démarrer. Le plan proposé tient en 6 étapes.

---

## 1. Fichiers et classes concernés

### Existant à lire (mais ne pas modifier)

| Fichier | Pourquoi |
|---|---|
| `CodexWorkflowClient` | Pattern de référence pour l'exécution de processus (ProcessBuilder, streams, timeout) |
| `CodexExecutionRequest` | Modèle de référence pour la commande d'entrée |
| `CodexExecutionResult` | Modèle de référence pour le résultat |
| `CodexExecutionException` | Pattern d'exception d'exécution |
| `GlobalAnalysisStep` / `GlobalReviewStep` / `CorrectionStep` | Contexte futur : ces steps liront le `ValidationResult` quand la validation sera branchée |
| `AnalysisReviewWorkflowRunner` | Ne pas toucher, mais comprendre comment il consomme les step results |
| `WorkflowStepResult` | Format du résultat qui portera éventuellement le résultat de validation |

### À créer (périmètre étape 1)

```
src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/validation/
├── ValidationCommand.java
├── ValidationResult.java
├── ValidationStatus.java
└── WorkflowValidationService.java

src/test/java/.../runtime/validation/
└── WorkflowValidationServiceTest.java
```

---

## 2. Emplacement proposé

```
service/agent/workflow/runtime/validation/
```

**Justification :**
- Parallèle à `codex/` dans le même package `runtime/` — cohérent avec la structure existante.
- Pas à `service/validation/` (trop générique, hors contexte workflow).
- Pas à `cli/` (la validation n'est pas une couche CLI).
- La validation est une brique runtime au même titre que Codex — elle sera branchée dans les steps ou le runner dans une étape future.

**Package Java :**
```
fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation
```

---

## 3. Objets métier à introduire

### `ValidationStatus` (enum)

```java
public enum ValidationStatus {
    SUCCESS,       // exitCode == 0
    FAILURE,       // exitCode != 0, processus terminé normalement
    TIMEOUT,       // processus tué après timeout
    SYSTEM_ERROR   // IOException, InterruptedException — processus non lancé ou interrompu
}
```

### `ValidationCommand` (record)

```java
public record ValidationCommand(
        List<String> command,
        Path workingDirectory,
        Integer timeoutSeconds
)
```

Contraintes :
- `command` : non null, non vide, premier élément non blank (executable).
- `workingDirectory` : non null, doit exister et être un répertoire.
- `timeoutSeconds` : optionnel, si présent > 0.

**Important** : `command` est une `List<String>`, jamais une `String`. Cela garantit l'absence de shell intermédiaire dans `ProcessBuilder`. Voir §7.

### `ValidationResult` (record)

```java
public record ValidationResult(
        String command,
        ValidationStatus status,
        Integer exitCode,
        String stdout,
        String stderr,
        long durationMs,
        String errorMessage
)
```

- `exitCode` : null si `TIMEOUT` ou `SYSTEM_ERROR`.
- `stdout` / `stderr` : jamais null (chaîne vide si absent).
- `errorMessage` : null sauf pour `SYSTEM_ERROR`.
- `command` : représentation lisible de la commande exécutée (`String.join(" ", command)`).

### Pourquoi ne pas réutiliser `CodexExecutionResult` ?

`CodexExecutionResult` est couplé au concept "Codex". Le nommer comme résultat de validation serait sémantiquement incorrect et créerait une dépendance transverse entre des couches sans relation directe. Les deux records restent indépendants.

---

## 4. Stratégie d'exécution locale

### Pattern à suivre (identique à `CodexWorkflowClient`)

```java
ProcessBuilder processBuilder = new ProcessBuilder(command.command());
processBuilder.directory(command.workingDirectory().toFile());

Process process = processBuilder.start();
// stream reading en parallèle via ExecutorService
// waitFor(timeout)
// destroy + destroyForcibly si timeout
```

### Règle stricte : `List<String>`, pas de shell

```java
// ✅ Correct — pas de shell
new ProcessBuilder(List.of("./mvnw", "clean", "compile", "-DskipTests"))

// ❌ Interdit — injection shell possible
Runtime.getRuntime().exec("./mvnw clean compile " + userInput)
Runtime.getRuntime().exec(new String[]{"sh", "-c", command})
```

`ProcessBuilder(List<String>)` invoque directement l'exécutable. Il n'y a pas de shell intermédiaire, donc pas d'injection shell.

### Commande Maven cible (à utiliser en étape 2)

Sous Windows (projet actuel) :
```
["cmd", "/c", "mvnw.cmd", "clean", "compile", "-DskipTests"]
```

Ou via le wrapper direct (recommandé, plus portable) :
```
[System.getProperty("os.name").contains("Windows") ? "mvnw.cmd" : "./mvnw",
 "clean", "compile", "-DskipTests"]
```

La commande exacte sera définie en étape 2 — pas en étape 1.

---

## 5. Stratégies de gestion des cas limites

### Timeout

Pattern identique à `CodexWorkflowClient` :

```java
boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
if (!finished) {
    process.destroy();
    if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
    }
    return new ValidationResult(..., ValidationStatus.TIMEOUT, null, ...);
}
```

Timeout par défaut suggéré : **120 secondes** (identique à `CodexWorkflowClient`). Configurable via `ValidationCommand.timeoutSeconds()`.

### Code retour non zéro

Pas d'exception — `exitCode != 0` → `ValidationStatus.FAILURE`. Le message d'erreur est dans `stderr`. L'appelant lit `status`, `exitCode`, et `stderr`.

```java
int exitCode = process.exitValue();
ValidationStatus status = exitCode == 0 ? ValidationStatus.SUCCESS : ValidationStatus.FAILURE;
```

### Exception système

`IOException` lors du `processBuilder.start()` → `ValidationStatus.SYSTEM_ERROR` avec `errorMessage`. Ne pas propager en exception pour que l'appelant puisse gérer sans try/catch.

```java
} catch (IOException e) {
    return new ValidationResult(..., ValidationStatus.SYSTEM_ERROR, null, "", "",
            durationMs, "Failed to start process: " + e.getMessage());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return new ValidationResult(..., ValidationStatus.SYSTEM_ERROR, null, "", "",
            durationMs, "Process execution interrupted");
}
```

**Nuance** : contrairement à `CodexWorkflowClient` qui lance une `CodexExecutionException`, le `WorkflowValidationService` retourne un `ValidationResult` de statut `SYSTEM_ERROR`. Ce choix est préférable car la validation sera consommée dans un contexte workflow où le résultat doit toujours être observable.

### stdout/stderr volumineux

Maven compile peut produire des sorties larges (centaines de lignes). Stratégie :

- Lire le flux intégralement en mémoire (identique à `CodexWorkflowClient`).
- Appliquer un **cap de 50 000 caractères** par flux avec notice de troncature : `"[truncated — original size: N chars]"`.
- Conserver le **début** de stdout (informations de build) et la **fin** de stderr (erreurs, dernières lignes de stacktrace).

Alternative : ne tronquer que si nécessaire en Phase 6, et décider d'une stratégie de streaming en Phase 7. Le cap simple est suffisant pour l'étape 1.

---

## 6. Stratégie de test

### Problème

`WorkflowValidationService` utilise `ProcessBuilder`. Lancer Maven dans chaque test est lent, fragile, et dépendant de l'environnement.

### Option retenue : interface `CommandExecutor`

Introduire une interface minimale :

```java
@FunctionalInterface
public interface CommandExecutor {
    ValidationResult execute(ValidationCommand command);
}
```

`WorkflowValidationService` implémente cette interface **et** peut être construit avec une instance de `CommandExecutor` pour les tests.

**Ou** : pattern plus simple — `WorkflowValidationService` prend une `ProcessBuilderFactory` (functional interface) en paramètre de constructeur pour les tests.

**Choix recommandé** pour ce projet (minimal, cohérent avec le style existant) : tester `WorkflowValidationService` avec une **commande OS triviale** qui fonctionne sur toutes les plateformes :

- Windows : `["cmd", "/c", "echo", "ok"]` → exit 0, stdout="ok"
- Cross-platform via Java : `[ProcessHandle.current().info().command().orElse("java"), "-version"]`

Alternativement, créer un `FakeCommandExecutor` ou utiliser une lambda directement dans les tests :

```java
// Test avec un executor simulé
WorkflowValidationService service = command ->
    new ValidationResult("echo ok", ValidationStatus.SUCCESS, 0, "ok", "", 10L, null);
```

Ce pattern est identique à la philosophie utilisée pour les simulateurs CLI en Phase 5 : tester le comportement sans invoquer le processus réel.

### Cas à tester

- Commande réussie (exit 0) → `SUCCESS`
- Commande échouée (exit 1) → `FAILURE`
- Timeout → `TIMEOUT`
- Commande introuvable (IOException) → `SYSTEM_ERROR`
- stdout/stderr tronqués
- ValidationCommand avec workingDirectory invalide → IllegalArgumentException

---

## 7. Risques d'architecture et de sécurité

### Risque 1 — Injection de commande shell (critique)

Si un consommateur passe une chaîne libre interprétée par un shell, n'importe quelle commande peut être exécutée sur le serveur.

**Mitigation** : `List<String>` obligatoire dans `ValidationCommand`. Jamais de `ProcessBuilder(String)` ni de `Runtime.exec(String)`. Documenter cette contrainte dans la Javadoc de `ValidationCommand`.

### Risque 2 — Commande trop permissive

`ValidationCommand` ne filtre pas les commandes autorisées. Un appelant pourrait en théorie passer `["rm", "-rf", "/"]`.

**Mitigation pour Phase 6** : La validation sera uniquement appelée depuis les steps internes du runtime. Il n'y a pas d'entrée utilisateur dans la chaîne. Document the constraint: `WorkflowValidationService` n'est pas un exécuteur de commandes arbitraires — il exécute uniquement des commandes de build définies statiquement dans les steps.

**Mitigation future** : Si un bot Telegram pouvait déclencher des validations, une allowlist des commandes autorisées serait nécessaire.

### Risque 3 — Processus zombie

Si le thread principal est interrompu avant le `waitFor()`, le processus fils peut continuer. `destroy()` + `destroyForcibly()` mitigent ce risque, mais ne sont pas guarantis (processus daemon OS).

**Mitigation** : Même pattern que `CodexWorkflowClient` avec destroy + destroyForcibly. Acceptable pour Phase 6.

### Risque 4 — stdout/stderr en mémoire

Un processus qui génère des gigaoctets de sortie (ex: Maven avec logging debug) peut saturer la JVM.

**Mitigation** : Cap à 50 000 caractères par flux dès l'implémentation initiale.

### Risque 5 — Couplage futur avec le runner

Si `WorkflowValidationService` est branché directement dans `AnalysisReviewWorkflowRunner` sans passer par un `WorkflowStep`, le runner deviendrait dépendant de la validation. La bonne intégration future est via un `ValidationStep` implémentant `WorkflowStep`.

**Mitigation** : Ne pas toucher le runner en Phase 6. La validation sera consommée via un step dédié.

---

## 8. Préparation de l'étape suivante (lancement de `mvn clean compile`)

L'étape 2 branchera la commande Maven concrète. Pour l'anticiper sans sur-architecturer :

1. **Définir la commande Maven** comme constante dans le futur `ValidationStep` (pas dans `WorkflowValidationService` qui reste générique).
2. **Résoudre le répertoire projet** : le step recevra `codexWorkingDirectory` ou `reportRootDirectory` comme `workingDirectory`.
3. **Décider du wrapper** : `mvnw.cmd` (Windows) vs `./mvnw` (Unix) — gérer via `System.getProperty("os.name")` dans le step.
4. **Décider du timeout** : 120 secondes est raisonnable pour un `compile -DskipTests`.

---

## 9. Plan d'implémentation en 6 étapes

### Étape 1.1 — Objets métier

Créer :
- `ValidationStatus` (enum : `SUCCESS`, `FAILURE`, `TIMEOUT`, `SYSTEM_ERROR`)
- `ValidationCommand` (record : `List<String> command`, `Path workingDirectory`, `Integer timeoutSeconds`)
- `ValidationResult` (record : `String command`, `ValidationStatus status`, `Integer exitCode`, `String stdout`, `String stderr`, `long durationMs`, `String errorMessage`)

Tests des validations du constructeur uniquement (compact constructors dans les records).

### Étape 1.2 — `WorkflowValidationService`

Créer le service avec la méthode `ValidationResult validate(ValidationCommand command)`.

Implémenter :
- `ProcessBuilder(command.command())` avec `directory(workingDirectory)`
- streams concurrents via `ExecutorService` (2 threads)
- `waitFor(timeout)` + destroy + destroyForcibly
- gestion IOException → `SYSTEM_ERROR`
- gestion InterruptedException → `SYSTEM_ERROR` + interrupt thread
- cap stdout/stderr à 50 000 caractères

Sans `@Service` Spring pour Phase 6 (même décision que `WorkflowUserInteractionSimulator`). Annoter si nécessaire en Phase 7.

### Étape 1.3 — Tests avec commande OS simple

Tester `WorkflowValidationService` avec des commandes triviales :
- Windows : `["cmd", "/c", "echo", "ok"]` → SUCCESS
- Exit non-zero : `["cmd", "/c", "exit", "1"]` → FAILURE
- Commande introuvable : `["commandqui_nexiste_pas_du_tout"]` → SYSTEM_ERROR
- Timeout : command avec délai et timeout court → TIMEOUT

Ces tests n'ont pas besoin de Maven et fonctionnent sur la machine de développement.

### Étape 1.4 — Tests comportementaux avec executor simulé

Ajouter une surcharge de constructeur ou un `CommandExecutor` fonctionnel pour les tests purement unitaires :

```java
@Test
void successResultIsReturnedWhenExitCodeIsZero() {
    WorkflowValidationService service = ... ; // lambda ou mock
    ValidationResult result = service.validate(command);
    assertThat(result.status()).isEqualTo(ValidationStatus.SUCCESS);
}
```

### Étape 1.5 — Documentation de sécurité

Ajouter dans `ValidationCommand` un commentaire Javadoc court indiquant :
- que `command` est une liste d'arguments (pas une chaîne shell) ;
- que `ProcessBuilder` n'utilise pas de shell intermédiaire ;
- qu'aucune commande arbitraire ne doit être passée depuis l'extérieur.

### Étape 1.6 — Vérification des tests existants

Exécuter les tests existants pour confirmer qu'aucun test n'est cassé par l'ajout du package `validation/`.

---

## Conclusion

**Verdict : Prêt pour implémentation.**

Le pattern d'exécution de processus existe déjà dans `CodexWorkflowClient`. Les objets à créer sont simples et clairement délimités. Les contraintes de sécurité sont identifiées et mitigables dès la conception.

La validation peut être implémentée en 6 étapes sans toucher au runtime existant, sans Telegram, sans persistance, et sans moteur shell générique.

Le seul point d'attention architectural : ne pas brancher `WorkflowValidationService` directement dans le runner. La bonne intégration future est via un `ValidationStep` implémentant `WorkflowStep`.
