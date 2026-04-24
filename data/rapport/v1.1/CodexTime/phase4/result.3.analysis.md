# Analyse — Phase 4 Étape 3 (`WAIT_HUMAN` + reprise manuelle simple)

## 1) Comportement à l’arrêt (`WAIT_HUMAN`)

### Constat existant
- `GlobalReviewStep` retourne déjà `WorkflowStepDecision.WAIT_HUMAN` avec un message clair (`Global review requires human decision`) et des métadonnées (`promptArtifactPath`, `resultArtifactPath`, `waitReason` optionnel).
- `WorkflowOrchestrator.executeAnalysisReviewWithOptionalCorrection(...)` stoppe déjà immédiatement sur `WAIT_HUMAN` et ne lance pas `CorrectionStep`.

### Recommandation V1
- Conserver ce comportement tel quel.
- Contrat d’arrêt explicite côté appelant :
  - `decision = WAIT_HUMAN`
  - `message` lisible
  - `data.resultArtifactPath` = source concrète à relire/éditer par l’humain
  - `data.waitReason` (si présent) = raison courte à afficher

Aucune mécanique supplémentaire n’est nécessaire à l’arrêt.

---

## 2) Stratégie de reprise recommandée (réponse nette)

### Recommandation
- **V1: reprise ciblée après intervention humaine, en relançant uniquement `CorrectionStep`**.
- **Ne pas relancer le workflow complet**.

### Pourquoi
- Relancer tout le flux (`Analysis -> Review`) peut écraser la modification humaine du `REVIEW_RESULT`.
- Relancer uniquement `CorrectionStep` est simple, déterministe, peu coûteux, et reste aligné avec l’existant (ce step relit déjà `ANALYSIS_RESULT` + `REVIEW_RESULT`).
- Ce n’est pas un “moteur de reprise” : c’est juste une relance manuelle d’un step connu dans un cas connu.

Réponse à la question 5 : **choix 2 (reprendre à l’étape interrompue de manière ciblée, ici via `CorrectionStep`)**.

---

## 3) Source de vérité pour la reprise

### Recommandation nette
- **Source principale : artefacts sur disque** (`result.<n>.review.md` en priorité, plus `result.<n>.analysis.md`).
- **Source secondaire : variables de contexte** pour signaler l’intention de reprise et optionnellement une note humaine.

### Conventions simples proposées
- Artefact principal éditable par l’humain : `REVIEW_RESULT`.
- Variables runtime optionnelles (non persistées) :
  - `humanResume` = `true`
  - `humanDecisionNote` = texte libre court (optionnel)

Cette combinaison est suffisante et reste sobre.

---

## 4) Évolution de l’orchestrator

### Recommandation
- **Réutiliser les méthodes existantes** (surtout `executeSingleStep(...)`) pour la reprise V1.
- **Ne pas ajouter de nouvelle méthode dédiée maintenant**.

### Justification
- Le besoin immédiat est couvert par :
  - exécution normale via `executeAnalysisReviewWithOptionalCorrection(...)`
  - reprise manuelle via `executeSingleStep(context, correctionStep)`
- Ajouter une méthode “resume...” maintenant apporterait surtout de la duplication de flux.

---

## 5) Rapport au contexte runtime

### Réponse demandée (E)
- **3. Le contexte actuel suffit tel quel avec conventions simples.**

### Justification
- `WorkflowExecutionContext.variables` existe déjà et permet d’injecter une décision humaine à la relance.
- Pas besoin de modifier `WorkflowExecutionContext` ni `WorkflowRun` pour cette V1.
- Pas de tracking de position nécessaire.

---

## 6) Rapport aux artefacts (intervention humaine)

### Intervention humaine V1
- Option 1 : éditer `result.<n>.review.md` (cas principal).
- Option 2 : ajouter une note décisionnelle simple (dans le même fichier ou via `humanDecisionNote` en variable).

### Relecture côté workflow
- `CorrectionStep` relit déjà `ANALYSIS_RESULT` et `REVIEW_RESULT`.
- La reprise se fait donc naturellement sans parsing avancé.
- Pas de versioning, pas d’historique, pas de format DSL.

---

## 7) Résultat attendu après reprise

### Contrat de retour
- Retour = résultat standard de `CorrectionStep` :
  - `CONTINUE` + `Correction completed` + chemins d’artefacts si succès
  - `STOP_FAILURE` + message explicite sinon

### Comment vérifier que la reprise a bien eu lieu
- `decision` final de la relance.
- Présence/mise à jour de `result.<n>.correction.md`.
- Cohérence des chemins retournés dans `data.resultArtifactPath`.

---

## 8) Classes à modifier / ne pas modifier (cible implémentation)

### À conserver inchangé (V1 recommandée)
- `WorkflowExecutionContext`
- `WorkflowRun`
- `WorkflowRunStatus`
- `GlobalReviewStep` (détection `WAIT_HUMAN` déjà en place)

### À ajuster au minimum lors de l’implémentation
- Point d’entrée de lancement workflow (runner/service appelant) pour offrir 2 actions explicites :
  - exécution normale complète
  - reprise manuelle ciblée (appel direct `executeSingleStep(..., correctionStep)`)
- Tests d’orchestration pour couvrir la reprise manuelle ciblée.

### À ne pas créer
- Aucune nouvelle classe de moteur de reprise.
- Aucun registre de checkpoint.
- Aucune state machine.

---

## 9) Signature recommandée (sobre)

Pour l’implémentation, garder des signatures simples au niveau du service appelant :

- `runAnalysisReviewCorrection(WorkflowExecutionContext context): WorkflowStepResult`
- `resumeAfterHumanReview(WorkflowExecutionContext context): WorkflowStepResult`

Et en interne, la reprise appelle simplement :
- `workflowOrchestrator.executeSingleStep(context, correctionStep)`

Note : ces signatures sont proposées au niveau du **runner/service applicatif**, pas comme nouvelle API complexe de l’orchestrator.

---

## 10) Risques de sur-conception à éviter

- Introduire un moteur générique de transitions d’état.
- Stocker une “position de workflow” persistée.
- Ajouter des mécanismes de retry automatiques.
- Ajouter un système événementiel/queue pour un besoin local et manuel.
- Multiplier les abstractions `resume strategy` / `checkpoint manager`.

---

## 11) Plan d’implémentation minimal (5 étapes)

1. Garder le flux nominal actuel inchangé (`Analysis -> Review -> optional Correction`).
2. Ajouter dans le service appelant une action explicite de reprise manuelle post-`WAIT_HUMAN`.
3. Cette reprise lance uniquement `CorrectionStep` via `executeSingleStep(...)`.
4. Documenter les conventions opératoires : fichier review à éditer + variables runtime optionnelles.
5. Ajouter tests ciblés de reprise manuelle (succès/échec) sans ajouter de persistance ni nouveaux sous-systèmes.

---

## 12) Périmètre explicitement exclu (H)

- système de file d’attente
- orchestration distribuée
- gestion multi-utilisateur
- Telegram
- API REST dédiée de reprise
- persistance avancée de checkpoints
- retry automatique
- moteur de reprise complexe

---

## Décision finale (synthèse)

- Arrêt `WAIT_HUMAN` : déjà correct, à conserver.
- Reprise V1 : **manuelle, contrôlée par l’utilisateur, basée d’abord sur les artefacts, puis relance ciblée de `CorrectionStep`**.
- Architecture : **pas de nouveau moteur, pas de state machine, pas de persistance additionnelle**.
