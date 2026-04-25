# Rapport d'implémentation — Phase 6 Étape 1 — Validation locale minimale

## Résumé

La brique de validation locale a été créée et testée avec succès.
Elle permet d'exécuter une commande locale contrôlée et de retourner un résultat structuré.
Elle n'est pas encore branchée dans le workflow runtime.

**Tests : 25/25 passent. Exit code Maven : 0.**

---

## Fichiers créés

### Code principal

| Fichier | Description |
|---|---|
| `...runtime/validation/ValidationStatus.java` | Enum : `SUCCESS`, `FAILURE`, `TIMEOUT`, `SYSTEM_ERROR` |
| `...runtime/validation/ValidationCommand.java` | Record immuable : commande + workingDirectory + timeoutSeconds optionnel |
| `...runtime/validation/ValidationResult.java` | Record immuable : command, status, exitCode, stdout, stderr, durationMs, errorMessage |
| `...runtime/validation/WorkflowValidationService.java` | Service principal d'exécution |

### Tests

| Fichier | Tests |
|---|---|
| `ValidationCommandTest.java` | 12 tests — validation du constructeur compact + immutabilité |
| `WorkflowValidationServiceTest.java` | 13 tests — SUCCESS, FAILURE, TIMEOUT, SYSTEM_ERROR, stdout/stderr non-null, troncature |

---

## Choix techniques

### Pattern utilisé

Inspiré de `CodexWorkflowClient` / `CodexExecutionResult`, sans réutilisation directe.
Différence principale : les erreurs (IOException, InterruptedException, timeout) sont retournées
comme `ValidationResult(status=SYSTEM_ERROR ou TIMEOUT)` — pas lancées comme exception.
Cela évite un try/catch chez l'appelant futur.

### Sécurité

La commande est toujours un `List<String>` passé directement à `ProcessBuilder`.
Aucun shell interpreter (`sh -c`, `cmd /c`) dans le service lui-même.
`ValidationCommand` interdit null, liste vide, exécutable blank, et éléments null.

### Exécution concurrente stdout/stderr

`ExecutorService` à 2 threads pour lire stdout et stderr en parallèle.
Utilisation de `try-with-resources` (Java 21 AutoCloseable ExecutorService).
`safeGet()` avec timeout de 5 secondes pour la collecte des streams après fin du processus.

### Timeout

`process.waitFor(timeout, TimeUnit.SECONDS)` + `destroy()` + `destroyForcibly()` si le processus
ne se termine pas dans les 2 secondes suivant le signal `destroy`.

### Troncature

stdout et stderr sont plafonnés à 50 000 caractères.
Au-delà, le stream est intégralement drainé (pour éviter le blocage du processus) mais le contenu
est abandonné. Une notice `[output truncated — 50000 chars limit reached]` est ajoutée.

---

## Tests ajoutés

```
ValidationCommandTest (12 tests) :
  refusesNullCommand
  refusesEmptyCommand
  refusesBlankExecutable
  refusesNullExecutable
  refusesNullElementInCommand
  refusesNullWorkingDirectory
  refusesNonExistentWorkingDirectory
  refusesFileAsWorkingDirectory
  refusesZeroTimeout
  refusesNegativeTimeout
  acceptsValidCommandWithoutTimeout
  acceptsValidCommandWithTimeout
  commandListIsImmutable

WorkflowValidationServiceTest (13 tests) :
  throwsOnNullCommand
  returnsSuccessForSuccessfulCommand
  durationIsPositiveForSuccessfulCommand
  commandStringIsIncludedInResult
  returnsFailureForNonZeroExitCode
  returnsSystemErrorForUnknownCommand
  returnsTimeoutWhenProcessExceedsLimit
  stdoutIsNeverNull
  stderrIsNeverNull
  outputIsNotTruncatedWhenWithinLimit
  outputIsTruncatedWhenExceedingLimit
  outputAtExactLimitIsNotTruncated
```

---

## Commandes de test exécutées

```
JAVA_HOME=".../jbr" bash mvnw -q \
  "-Dtest=ValidationCommandTest,WorkflowValidationServiceTest" test
```

**Résultat : Tests run: 25, Failures: 0, Errors: 0, Skipped: 0. EXIT: 0.**

---

## Difficultés rencontrées — commande lente cross-platform

Le test `returnsTimeoutWhenProcessExceedsLimit` nécessite une commande qui tient > 1 seconde.

| Tentative | Commande Windows | Résultat |
|---|---|---|
| 1 | `cmd /c ping -n 6 127.0.0.1` | FAILURE immédiat — ICMP bloqué par le pare-feu |
| 2 | `cmd /c more` | FAILURE immédiat — `more.exe` quitte si stdin n'est pas un TTY |
| 3 | `powershell -NoProfile ... Start-Sleep -Seconds 30` | SYSTEM_ERROR — `powershell` absent du PATH hérité de Git Bash |
| 4 | `cmd /c powershell ...` | FAILURE immédiat — `powershell` absent du PATH même via cmd |
| **5 (retenu)** | **Chemin absolu `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`** | **TIMEOUT ✓** |

La solution finale utilise le chemin absolu vers `powershell.exe` (stable sur Windows Vista → 11)
couplé à `Start-Sleep -Seconds 30`. Ce chemin est indépendant du PATH du processus appelant.

---

## Contraintes respectées

- `AnalysisReviewWorkflowRunner` : non modifié
- `WorkflowStepResult` : non modifié
- Aucune `ValidationStep` créée
- Aucun branchement dans les steps existants
- Aucun `sh -c` ou `cmd /c` dans `WorkflowValidationService`
- Maven non lancé automatiquement (commandes manuelles uniquement)

---

## Limites connues

- Le chemin `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe` est codé en dur dans le test.
  Ce n'est pas un problème en pratique (chemin système stable) mais peut être flaky sur des images
  Windows Server minimalistes sans PowerShell.
- `WorkflowValidationService` n'est pas encore un `@Service` Spring — c'est intentionnel pour cette étape.
- La validation n'est pas encore branchée dans le pipeline `AnalysisReviewWorkflowRunner`.

---

*Phase 6 / Étape 1 — implémentation terminée.*
