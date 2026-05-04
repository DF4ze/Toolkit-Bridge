# Roadmap — Phase 9 — Logging intelligent CodexTime

## Objectif global

Ajouter une journalisation utile, sobre et exploitable dans CodexTime.

Le but n’est pas de mettre des logs partout, mais de rendre visible le chemin critique :

```text
Telegram
→ orchestration workflow
→ runner / orchestrator
→ steps Codex / Maven
→ process Codex CLI / validation
```

Les logs doivent aider à comprendre :

* quel workflow démarre ;
* quel projet / phase / étape est ciblé ;
* quelle décision est prise ;
* quand Codex ou Maven est appelé ;
* pourquoi un workflow est refusé ;
* où une erreur se produit ;
* quand un `WAIT_HUMAN`, `STOP_FAILURE` ou succès survient.

Tous les logs ajoutés doivent utiliser :

```java
@Slf4j
```

---

# Étape 1 — Logs Telegram et orchestration

## Objectif

Rendre visibles les actions déclenchées par l’utilisateur Telegram et les transitions principales du workflow.

## Classes concernées

* `WorkflowTelegramOrchestrationService`
* `WorkflowTelegramController`
* `WorkflowTelegramProjectService`
* `WorkflowTelegramRoadmapService`

## Travail attendu

### `WorkflowTelegramOrchestrationService`

* Migrer vers `@Slf4j` si la classe utilise encore `LoggerFactory`.
* Ajouter des logs `INFO` sur :

    * démarrage d’un run ;
    * démarrage d’un resume ;
    * fin de workflow ;
    * passage en `WAIT_HUMAN` ;
    * `STOP_FAILURE`.
* Ajouter des logs `WARN` sur :

    * run refusé ;
    * resume refusé ;
    * projet inconnu ;
    * session incohérente ;
    * run déjà en cours.
* Garder les `ERROR` uniquement pour les exceptions réelles.

### `WorkflowTelegramController`

* Ajouter des logs `INFO` uniquement pour les commandes à effet :

    * `/workflow_run`
    * `/workflow_resume`
    * `/workflow_project_set`
    * `/workflow_roadmap_load`
* Éviter les logs `INFO` pour les commandes de lecture :

    * `/workflow`
    * `/workflow_status`
    * `/workflow_summary`

### `WorkflowTelegramProjectService`

* Ajouter des logs `INFO` pour :

    * projet créé ;
    * projet mis à jour.
* Ajouter des logs `WARN` pour :

    * nom invalide ;
    * path invalide ;
    * path dangereux ;
    * échec d’enregistrement.

### `WorkflowTelegramRoadmapService`

* Ajouter un log `INFO` quand une roadmap est chargée.
* Ajouter des logs `WARN` pour les refus ou erreurs récupérables.

## Règles de confidentialité

Ne jamais logger en `INFO` :

* `projectPath`;
* `roadmapPath`;
* path absolu ;
* arguments Telegram bruts si un path peut être dedans.

## Critère de sortie

L’étape 1 est terminée si :

* les actions Telegram importantes sont visibles en logs ;
* les refus utilisateur importants sont visibles en `WARN` ;
* les erreurs techniques restent en `ERROR` ;
* aucun path absolu ou secret n’est loggé en `INFO` ;
* les tests passent.

---

# Étape 2 — Logs runner, orchestrator et steps métier

## Objectif

Rendre visible le déroulé interne du workflow CodexTime.

Cette étape doit permettre de suivre :

```text
run
→ analysis
→ review
→ correction éventuelle
→ validation Maven
→ retry éventuel
→ WAIT_HUMAN / STOP_FAILURE / succès
```

## Classes concernées

* `AnalysisReviewWorkflowRunner`
* `WorkflowOrchestrator`
* `GlobalAnalysisStep`
* `GlobalReviewStep`
* `CorrectionStep`
* `BuildErrorCorrectionStep`
* `MavenValidationStep`

## Travail attendu

### `AnalysisReviewWorkflowRunner`

* Ajouter des logs `INFO` pour :

    * démarrage du run ;
    * démarrage du resume ;
    * correction build déclenchée ;
    * limite de retry atteinte ;
    * workflow terminé.
* Ajouter des logs `WARN` pour :

    * passage en `WAIT_HUMAN` ;
    * retry impossible ;
    * summary non généré si non bloquant.
* Ajouter des logs `DEBUG` pour :

    * décision intermédiaire ;
    * retry count ;
    * clés de contexte, sans valeurs sensibles.

### `WorkflowOrchestrator`

* Ajouter des logs `DEBUG` uniquement :

    * step démarrée ;
    * step terminée ;
    * décision retournée.
* Ne pas logger les contenus du `WorkflowStepResult`.

### Steps Codex

Classes :

* `GlobalAnalysisStep`
* `GlobalReviewStep`
* `CorrectionStep`
* `BuildErrorCorrectionStep`

Ajouter des logs `INFO` pour :

* appel Codex démarré ;
* appel Codex terminé ;
* décision importante.

Ajouter des logs `WARN` pour :

* timeout ;
* échec récupérable ;
* `WAIT_HUMAN` déclenché.

Ajouter des logs `ERROR` uniquement si une exception réelle est interceptée et transforme le step en échec.

Ajouter des logs `DEBUG` possibles pour :

* durée ;
* exit code ;
* taille stdout/stderr ;
* type d’artifact écrit.

### `MavenValidationStep`

Ajouter des logs `INFO` pour :

* validation Maven démarrée ;
* validation Maven réussie.

Ajouter des logs `WARN` pour :

* validation échouée avec correction possible ;
* timeout.

Ajouter des logs `ERROR` pour :

* échec final non récupérable.

## Règles de confidentialité

Ne jamais logger :

* prompt Codex ;
* contenu analysé ;
* rapport complet ;
* summary complet ;
* stdout/stderr complets ;
* `buildErrorSummary`.

Les tailles de contenu peuvent être loggées en `DEBUG`, par exemple :

```text
stdoutLen=...
stderrLen=...
```

## Critère de sortie

L’étape 2 est terminée si :

* le cycle interne du workflow est compréhensible via les logs ;
* les décisions principales sont visibles ;
* les steps Codex et Maven sont observables ;
* aucun contenu sensible n’est loggé ;
* les tests passent.

---

# Étape 3 — Logs process, validation bas niveau et registre projet

## Objectif

Rendre visibles les interactions bas niveau avec les processus externes et le registre projet.

Cette étape doit aider à diagnostiquer :

* Codex CLI qui ne démarre pas ;
* timeout Codex ;
* Maven qui échoue ;
* projet introuvable ou invalide ;
* path projet refusé.

## Classes concernées

* `CodexWorkflowClient`
* `WorkflowValidationService`
* `WorkflowProjectRegistryService`

## Travail attendu

### `CodexWorkflowClient`

* Conserver `@Slf4j` si déjà présent.
* Ajouter ou enrichir les logs `INFO` pour :

    * démarrage du process Codex ;
    * présence ou absence de working directory, sans afficher le path.
* Ajouter des logs `DEBUG` pour :

    * commande construite sans prompt ;
    * durée ;
    * exit code ;
    * `stdoutLen`;
    * `stderrLen`.
* Ajouter un log `WARN` pour :

    * timeout.
* Garder les logs `ERROR` pour :

    * exception de démarrage process ;
    * interruption ;
    * erreur I/O.

### `WorkflowValidationService`

* Ajouter `@Slf4j`.
* Ajouter des logs `DEBUG` pour :

    * démarrage validation process ;
    * commande exécutée si non sensible ;
    * status ;
    * exit code ;
    * durée.
* Ajouter des logs `WARN` pour :

    * timeout.
* Ajouter des logs `ERROR` pour :

    * process impossible à démarrer ;
    * interruption.

### `WorkflowProjectRegistryService`

* Ajouter `@Slf4j`.
* Ajouter des logs `INFO` pour :

    * projet créé ;
    * projet mis à jour.
* Ajouter des logs `WARN` pour :

    * nom invalide ;
    * path invalide ;
    * path dangereux ;
    * path non lisible / non writable ;
    * projet introuvable si le lookup est utilisé dans un contexte d’exécution.

## Règles de confidentialité

Ne jamais logger en `INFO` :

* chemin absolu projet ;
* prompt ;
* stdout/stderr complets ;
* contenu de fichier ;
* token ;
* secret.

En `DEBUG`, les chemins peuvent être tolérés seulement si nécessaires au diagnostic, mais les prompts et secrets restent interdits.

## Critère de sortie

L’étape 3 est terminée si :

* les appels process Codex/Maven sont observables ;
* les timeouts sont visibles ;
* les échecs de registre projet sont visibles ;
* aucun prompt ni secret n’est loggé ;
* les tests passent.

---

# Règles globales Phase 9

## Niveaux de logs

### INFO

Uniquement les événements utiles à l’exploitation.

### DEBUG

Détails techniques utiles au diagnostic, sans contenu sensible.

### WARN

Anomalies récupérables ou mauvaises utilisations.

### ERROR

Exceptions réelles et échecs non récupérables.

## Interdits stricts

Ne jamais logger :

* token Telegram ;
* clé API ;
* prompt complet ;
* contenu de fichier ;
* contenu complet de rapport ;
* contenu complet de summary ;
* stdout/stderr complets ;
* `buildErrorSummary`.

## Tests

Ne pas écrire de tests fragiles qui vérifient les messages exacts de logs.

La validation attendue est :

```text
./mvnw test
```

ou équivalent Windows :

```text
.\mvnw.cmd test
```

## Critère de sortie Phase 9

La Phase 9 est terminée lorsque :

* les actions Telegram principales sont visibles ;
* le workflow interne est traçable ;
* les appels Codex/Maven sont observables ;
* les erreurs importantes sont loggées au bon niveau ;
* les logs restent sobres ;
* aucun contenu sensible n’est loggé ;
* tous les tests passent.
