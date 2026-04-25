# CLI Reference — AnalysisReviewWorkflowCli

Version : 1.1
Phase : 5

---

## Rôle

`AnalysisReviewWorkflowCli` est le point d'entrée externe du workflow d'analyse et de revue.

Il permet d'invoquer le workflow depuis un script, un outil local ou un futur bot sans dépendance directe au code Java.

Il expose deux modes :

* `RUN` : lance le cycle complet analyse → revue → correction optionnelle.
* `RESUME` : reprend après une interruption humaine (`WAIT_HUMAN`) en exécutant uniquement l'étape de correction.

---

## Invocation

```
java -cp <classpath> fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowCli \
  --mode=<RUN|RESUME> \
  --reportRootDirectory=<path> \
  --reportVersion=<version> \
  --reportPhase=<phase> \
  --stepNumber=<number> \
  [--analysisSourcePath=<path>] \
  [--codexWorkingDirectory=<path>] \
  [--codexTimeoutSeconds=<seconds>] \
  [--runId=<id>] \
  [--workflowType=<type>] \
  [--targetStepRef=<ref>]
```

Chaque argument est de la forme `--key=value`. Les arguments sans valeur ou sans `=` sont rejetés avec exit code `2`.

---

## Paramètres

### Obligatoires (tous modes)

| Paramètre | Description |
|---|---|
| `--mode` | Mode d'exécution : `RUN` ou `RESUME` (insensible à la casse) |
| `--reportRootDirectory` | Répertoire racine des rapports (chemin local) |
| `--reportVersion` | Version du rapport (ex. `v1.1`) |
| `--reportPhase` | Phase du rapport (ex. `phase5`) |
| `--stepNumber` | Numéro d'étape entier strictement positif (ex. `4`) |

### Obligatoire en mode RUN uniquement

| Paramètre | Description |
|---|---|
| `--analysisSourcePath` | Chemin vers le fichier source d'analyse (chemin local) |

Ce paramètre est ignoré en mode `RESUME`.

### Optionnels

| Paramètre | Description | Défaut |
|---|---|---|
| `--codexWorkingDirectory` | Répertoire de travail utilisé par Codex | Répertoire courant du processus |
| `--codexTimeoutSeconds` | Timeout d'exécution Codex en secondes (entier > 0) | Valeur par défaut du client Codex |
| `--runId` | Identifiant de la session de workflow | `workflow-<timestamp>` |
| `--workflowType` | Type de workflow | `analysis-review` |
| `--targetStepRef` | Référence de l'étape cible | `external` |

---

## Codes de sortie

| Code | Signification |
|---|---|
| `0` | Succès : le workflow a produit une décision valide |
| `1` | Échec runtime : une exception s'est produite pendant l'exécution, ou la décision retournée est nulle. Message d'erreur sur stderr. |
| `2` | Argument invalide : un paramètre obligatoire est absent, mal formé, ou `mode` est inconnu. Message d'erreur + usage sur stderr. |

**Important** : exit code `0` ne signifie pas que le workflow est terminé. Il signifie que le workflow a produit une décision (`WAIT_HUMAN`, `CONTINUE`, `STOP_FAILURE`, etc.). Lire `finalDecision` dans stdout pour connaître l'état réel.

---

## Format de la sortie stdout

La sortie standard (`stdout`) est une série de lignes `key=value`.

Chaque valeur est sur une seule ligne (les retours à la ligne internes sont remplacés par des espaces).

### Clés toujours présentes (exit code 0)

| Clé | Description | Valeurs possibles |
|---|---|---|
| `decision` | Décision brute retournée par l'étape exécutée | `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`, `FINISH`, `RETRY_CORRECTION` |
| `message` | Message descriptif de l'état du workflow | Texte libre (peut être vide) |
| `finalDecision` | Décision finale interprétée (peut différer de `decision` selon le contexte) | Même valeurs que `decision` |
| `nextAction` | Action à effectuer par l'opérateur humain ou le système appelant | Texte libre |
| `correctionTriggered` | Indique si une correction automatique a été déclenchée dans ce run | `true` ou `false` |

### Clés conditionnelles

| Clé | Condition de présence | Description |
|---|---|---|
| `waitReason` | Présente uniquement si la décision est `WAIT_HUMAN` et qu'une raison a été produite par l'étape de revue | Raison textuelle de l'attente humaine |
| `workflowSummaryPath` | Présente si l'écriture du summary a réussi (voir §Limites) | Chemin local absolu vers `workflow-summary.md` |

### Exemple de sortie mode RUN — décision WAIT_HUMAN

```
decision=WAIT_HUMAN
message=Global review requires human decision
finalDecision=WAIT_HUMAN
nextAction=Review result requires human decision. Edit the review result, add a clear decision, then run runCorrectionAfterReview(...)
correctionTriggered=false
waitReason=Missing decision in review
workflowSummaryPath=D:\reports\v1.1\phase5\workflow-summary.md
```

### Exemple de sortie mode RESUME — décision CONTINUE

```
decision=CONTINUE
message=Correction completed
finalDecision=CONTINUE
nextAction=Continue workflow execution
correctionTriggered=true
workflowSummaryPath=D:\reports\v1.1\phase5\workflow-summary.md
```

### Exemple de sortie mode RUN — décision STOP_FAILURE

```
decision=STOP_FAILURE
message=GlobalReviewStep: Missing required artifact: D:\reports\v1.1\phase5\result.4.analysis.md
finalDecision=STOP_FAILURE
nextAction=Inspect failure and related artifacts, then rerun
correctionTriggered=false
```

### Sortie en cas d'erreur (exit code 1 ou 2)

Aucune sortie sur `stdout`. Le message d'erreur est écrit sur `stderr`. En cas de code `2`, l'usage de la CLI est également affiché sur `stderr`.

---

## Le fichier `workflow-summary.md`

### Contenu

Lorsque `workflowSummaryPath` est présent dans stdout, ce fichier contient un résumé lisible destiné à l'opérateur humain :

```
Decision: WAIT_HUMAN
Reason: Missing decision in review
Correction triggered: false

Artifacts:
- reviewResultPath: D:\reports\v1.1\phase5\result.4.review.md
- workflowSummaryPath: D:\reports\v1.1\phase5\workflow-summary.md

Next steps:
1. Edit review result: D:\reports\v1.1\phase5\result.4.review.md
2. Add a clear decision and optional WAIT_REASON update
3. Resume with runCorrectionAfterReview(...)
```

### Emplacement

Le chemin suit la convention :

```
<reportRootDirectory>/<reportVersion>/<reportPhase>/workflow-summary.md
```

Le numéro d'étape n'est pas inclus dans le nom du fichier — un seul `workflow-summary.md` par combinaison `version/phase`.

### Présence conditionnelle

`workflowSummaryPath` est **absent** dans les cas suivants :

1. Le runner a été appelé sans contexte de summary complet (usage programmatique sans CLI).
2. L'écriture du fichier a échoué (problème système de fichiers). Dans ce cas, `nextAction` contient un message indiquant l'échec de génération du summary, mais le résultat du workflow reste valide.

**Lorsque la CLI est invoquée avec des arguments valides, `workflowSummaryPath` est présent dans les résultats `WAIT_HUMAN` et `CONTINUE`.**

---

## Chemins dans les sorties

Tous les chemins retournés dans stdout et dans `workflow-summary.md` sont des **chemins locaux absolus** au format OS du serveur exécutant la JVM.

Sous Windows : `D:\reports\v1.1\phase5\workflow-summary.md`

Ces chemins ne sont pas utilisables directement depuis un bot Telegram ou un système distant. Un consommateur externe doit :

* soit lire le contenu du fichier localement et l'envoyer ;
* soit traiter le chemin comme une information textuelle uniquement.

---

## Cycle RUN → WAIT_HUMAN → RESUME

```
1. Appel CLI --mode=RUN
   → stdout: finalDecision=WAIT_HUMAN + workflowSummaryPath + waitReason

2. L'opérateur humain :
   a. Lit le fichier workflow-summary.md
   b. Lit et édite le fichier indiqué dans Next steps (review result)
   c. Ajoute une décision claire dans le review result

3. Appel CLI --mode=RESUME (mêmes paramètres que RUN, sans --analysisSourcePath)
   → stdout: finalDecision=CONTINUE ou STOP_FAILURE
```

**Important** : les paramètres `--reportRootDirectory`, `--reportVersion`, `--reportPhase` et `--stepNumber` doivent être identiques entre l'appel RUN et l'appel RESUME pour que la correction retrouve les bons artifacts.

---

## Décisions connues

| Valeur `finalDecision` | Signification |
|---|---|
| `WAIT_HUMAN` | Le workflow est en attente d'une intervention humaine |
| `CONTINUE` | Le workflow a terminé cette étape sans blocage |
| `STOP_FAILURE` | Le workflow a échoué et ne peut pas continuer sans intervention |
| `FINISH` | Fin normale du workflow (non produit par les étapes standards actuelles) |
| `RETRY_CORRECTION` | Correction à rejouer (non produit par les étapes standards actuelles) |

`FINISH` et `RETRY_CORRECTION` ne sont pas produits par le cycle standard RUN/RESUME via `AnalysisReviewWorkflowCli`. Ils font partie de l'enum `WorkflowStepDecision` mais ne sont pas utilisés dans les étapes actuelles (`GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`). Voir §Limites connues.

---

## Limites connues pour une intégration future

### Pas de rendu Telegram dédié

Il n'existe pas de composant destiné à formater les messages pour Telegram. `WorkflowUserInteractionSimulator` est un outil de simulation de démonstration, pas un composant applicatif. Un futur rendu Telegram devra être créé séparément.

### Pas de gestion de session RUN → RESUME

La CLI ne stocke aucun état entre les appels. L'appelant (script, bot) est responsable de mémoriser les paramètres utilisés lors de RUN pour les réutiliser lors de RESUME.

### Longueur de message non bornée

La valeur de `message`, `nextAction`, `waitReason` et le contenu de `workflow-summary.md` ne sont pas bornés en taille. Telegram impose une limite de 4096 caractères par message. Un rendu Telegram devra gérer la troncature ou l'envoi en fichier.

### `FINISH` et `RETRY_CORRECTION` mappés en UNKNOWN côté simulateur

`WorkflowCliConsumerSimulator` (utilisé dans les tests et potentiellement dans un futur bot) mappe uniquement `WAIT_HUMAN`, `STOP_FAILURE` et `CONTINUE`. Les valeurs `FINISH` et `RETRY_CORRECTION` sont actuellement mappées en `UNKNOWN`. Si ces décisions apparaissent en production, le consommateur retournera `UNKNOWN` sans indication exploitable.

### Chemins locaux non transférables

Les chemins dans stdout et dans `workflow-summary.md` sont locaux à la machine hébergeant la JVM. Ils ne peuvent pas être utilisés directement par un bot distant.

---

## Points à vérifier

* Comportement exact lorsque `--analysisSourcePath` est fourni en mode `RESUME` : le paramètre est ignoré silencieusement (non injecté dans le contexte), mais sa présence n'entraîne pas d'erreur de parsing.
* Longueur maximale observée en pratique pour `workflow-summary.md` sur les phases existantes — à mesurer avant implémentation Telegram.
