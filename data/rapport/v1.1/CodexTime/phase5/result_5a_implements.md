# Résultat implémentation — Phase 5 Étape 5a

## Objectif

Créer la documentation externe du contrat CLI `AnalysisReviewWorkflowCli` pour permettre à un futur consommateur (bot Telegram, script, outil local) de comprendre comment invoquer le workflow sans lire le code Java.

---

## Fichier créé

- `data/rapport/v1.1/Phase5/etape5/cli-reference.md`

---

## Résumé du contenu

### Rôle documenté

Le rôle de `AnalysisReviewWorkflowCli` est décrit : point d'entrée externe, deux modes (`RUN`, `RESUME`), invocable depuis tout environnement JVM.

### Paramètres documentés

**Obligatoires (tous modes) :**
- `--mode`
- `--reportRootDirectory`
- `--reportVersion`
- `--reportPhase`
- `--stepNumber`

**Obligatoire en mode RUN uniquement :**
- `--analysisSourcePath`

**Optionnels :**
- `--codexWorkingDirectory`
- `--codexTimeoutSeconds`
- `--runId`
- `--workflowType`
- `--targetStepRef`

### Codes de sortie documentés

| Code | Signification |
|---|---|
| `0` | Décision valide produite (pas forcément fin du workflow) |
| `1` | Échec runtime ou décision nulle |
| `2` | Argument invalide ou mode inconnu |

Nuance importante documentée : exit code `0` ne signifie pas que le workflow est terminé.

### Format stdout documenté

Clés toujours présentes : `decision`, `message`, `finalDecision`, `nextAction`, `correctionTriggered`.

Clés conditionnelles : `waitReason` (présente si WAIT_HUMAN et raison produite), `workflowSummaryPath` (présente si écriture du summary réussie).

Trois exemples de sorties fournis :
- RUN → WAIT_HUMAN
- RESUME → CONTINUE
- RUN → STOP_FAILURE

### Fichier `workflow-summary.md` documenté

- Format, contenu type, convention de chemin.
- Condition de présence de `workflowSummaryPath`.
- Cas d'absence : runner sans contexte complet, ou échec d'écriture du fichier.

### Chemins locaux documentés

Tous les chemins sont des chemins OS absolus locaux. Non utilisables directement par un bot distant. Stratégie recommandée : lire le contenu localement et l'envoyer, ou traiter le chemin comme texte informatif.

### Cycle RUN → WAIT_HUMAN → RESUME documenté

Séquence d'appel complète avec point critique : les paramètres `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber` doivent être identiques entre RUN et RESUME.

### Décisions documentées

Toutes les valeurs de `WorkflowStepDecision` sont listées avec leur signification, y compris `FINISH` et `RETRY_CORRECTION` avec la note qu'elles ne sont pas produites par le cycle standard actuel.

---

## Limites connues documentées

| Limite | Documentée |
|---|---|
| Pas de rendu Telegram dédié | ✅ |
| Pas de gestion de session RUN → RESUME | ✅ |
| Longueur de message non bornée (limite Telegram 4096 car.) | ✅ |
| `FINISH` et `RETRY_CORRECTION` mappés en UNKNOWN côté simulateur | ✅ |
| Chemins locaux non transférables | ✅ |

---

## Points à vérifier

Deux points ont été identifiés comme non certains à 100% et sont signalés dans `cli-reference.md` :

1. **`--analysisSourcePath` en mode RESUME** : le paramètre est ignoré silencieusement (non injecté dans le contexte par `buildContext()`), mais sa présence n'est pas rejetée par le parser. Ce comportement est implicite dans le code et non documenté.

2. **Longueur maximale observée de `workflow-summary.md`** : non mesurée sur les phases existantes. À vérifier avant implémentation Telegram pour valider si la troncature est nécessaire.

---

## Confirmation

Aucun fichier Java n'a été modifié.
Aucun composant Telegram n'a été créé.
Aucun renderer n'a été créé.
Le runtime n'a pas été modifié.
Le contrat CLI n'a pas été changé.

Seul `cli-reference.md` a été créé, à partir de la lecture directe du code de `AnalysisReviewWorkflowCli`, `AnalysisReviewWorkflowRunner`, `WorkflowArtifactType` et `WorkflowStepDecision`.
