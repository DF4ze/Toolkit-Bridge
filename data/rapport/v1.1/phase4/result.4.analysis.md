# Resultat d'analyse - Phase 4 - Etape 4

## 1) Recommandation V1 (nette)
Recommandation: **combiner de facon simple**
- enrichir `WorkflowStepResult.message/data` pour un contrat machine stable
- **et** ajouter un petit artefact recapitulatif `workflow summary` pour la lisibilite humaine locale

Justification pragmatique:
- `message/data` seuls sont utiles au runtime mais peu confortables pour l'humain
- les artefacts existants sont disperses (analysis/review/correction) et ne donnent pas un etat final compact
- un resume markdown unique reste leger, testable, et prepare une future notification (Telegram) sans l'implementer

Reponse explicite a la question importante (1 vs 2):
- Pour une V1 utile, je recommande **2 (ajouter un petit artefact recapitulatif)**,
- avec un enrichissement minimal des `data` pour garder un contrat propre.

## 2) Etat actuel (analyse de l'existant)
Le socle est deja propre:
- les decisions sont standardisees (`CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`) via `WorkflowStepResult`
- `GlobalReviewStep` sait deja exposer `waitReason` et `requiresCorrection`
- l'orchestrateur applique l'arret court-circuit et la correction conditionnelle
- la reprise manuelle existe (`AnalysisReviewWorkflowRunner.runCorrectionAfterReview(...)`)
- les artefacts sont bien normalises par `WorkflowArtifactService` / `WorkflowArtifactType`

Limite observee aujourd'hui:
- il n'existe pas de point de lecture unique donnant en un coup d'oeil:
  - decision finale
  - raison d'arret
  - correction lancee ou non
  - quoi faire en cas de `WAIT_HUMAN`

## 3) Portee minimale a rendre visible (sobre)
Minimum recommande:
- `finalDecision`
- `stopReason` (ou `waitReason` si `WAIT_HUMAN`)
- `correctionTriggered` (true/false)
- `resumeSupported` (true/false)
- chemins artefacts utiles:
  - `analysisPromptPath`, `analysisResultPath`
  - `reviewPromptPath`, `reviewResultPath`
  - `correctionPromptPath`, `correctionResultPath` (si applicable)
- `nextAction` court (ex: "Edit review result puis relancer runCorrectionAfterReview")

Ne pas aller au-dela en V1.

## 4) Emplacement de la logique (recommandation nette)
Repartition conseillee:
- **Orchestrator**: consolider l'etat final de run (decision finale, correction declenchee ou non)
- **Steps**: continuer a produire leurs metadonnees metier locales (pas de logique d'observabilite transversale)
- **Runner**: produire l'artefact recapitulatif final (point d'entree operatoire)
- **Petite brique dediee**: un composant tres leger de rendu de resume (ex: `WorkflowSummaryFormatter`)

=> Eviter de charger les steps avec une vision globale du workflow.

## 5) Classes concernees (a modifier / a creer lors de l'implementation)
Classes a **modifier** (plus probable):
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java` (stabilisation des cles/message)
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/CorrectionStep.java` (ajout explicite de metadonnees de resume)
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/artifact/WorkflowArtifactType.java` (type d'artefact summary)

Classe a **creer** (option minimale propre):
- `WorkflowSummaryFormatter` (ou `WorkflowSummaryWriter`) dans `workflow/runtime/` pour produire un markdown compact

Tests a ajuster/ajouter:
- `WorkflowOrchestratorTest`
- `AnalysisReviewWorkflowRunnerResumeTest`
- `GlobalReviewStepTest`
- + test(s) du nouveau formateur de summary

## 6) Rapport aux artefacts existants
Les artefacts existants restent la source detaillee. Ils ne suffisent pas pour la lecture operationnelle rapide.

Ajout recommande:
- un artefact **documentaire uniquement** (ex: `result.<step>.workflow-summary.md`)
- contenu court, stable, sans nouveau modele lourd

## 7) WAIT_HUMAN: recommandation concrete
Rendre `WAIT_HUMAN` exploitable localement via:
- `message` standardise (ex: "Global review requires human decision")
- `waitReason` courte obligatoire quand possible
- dans le summary:
  - "Pourquoi on attend"
  - "Quel fichier regarder/editer" (review result)
  - "Comment reprendre" (`runCorrectionAfterReview(...)`)

Format pratique (3 lignes d'action):
1. Ouvrir `reviewResultPath`
2. Ajouter/ajuster la decision humaine
3. Relancer la reprise manuelle

## 8) Reprise manuelle: rendre explicite sans intelligence supplementaire
A expliciter dans le summary:
- **Ce qui doit etre edite**: resultat review (et note humaine eventuelle)
- **Ce qui peut etre relance**: `runCorrectionAfterReview(...)`
- **Comment savoir que c'est pret**: presence d'une decision claire dans le review + note humaine non vide (convention)

Pas de moteur de validation complexe; simple convention documentaire suffisante en V1.

## 9) Contexte runtime (reponse nette)
Reponse: **3) le contexte actuel suffit avec conventions simples**.

`WorkflowExecutionContext.variables` est deja adequat pour V1.
A ce stade, inutile d'ajouter un nouveau modele runtime.

Conventions recommandees seulement:
- cles stables (`waitReason`, `requiresCorrection`, `correctionTriggered`, `nextAction`)
- valeurs courtes, textuelles/booleennes

## 10) Preparation Telegram plus tard (sans implementation)
A faire maintenant pour preparer proprement:
- stabiliser un mini-schema de champs (`finalDecision`, `waitReason`, `correctionTriggered`, `nextAction`, paths)
- stabiliser un template de message court (le meme que le summary)
- centraliser la generation du recapitulatif dans une seule brique

Benefice: plus tard, Telegram consommera juste ce resume/champs sans refactor lourde.

## 11) Risques de sur-conception
Risques a eviter:
- creer un bus d'evenements
- introduire une persistence d'etat riche
- multiplier les DTO/aggregate d'observabilite
- transformer les logs en pseudo monitoring distribue
- coupler fort les steps a un futur canal (Telegram)

Garde-fou V1:
- 1 artefact summary max
- 1 mini composant de rendu
- 0 infra supplementaire

## 12) Plan d'implementation minimal (5-10 etapes)
1. Definir les cles d'observabilite minimales (contrat stable)
2. Aligner `WorkflowStepResult.data` des steps sur ces cles
3. Faire remonter `correctionTriggered` explicitement depuis l'orchestrateur
4. Ajouter un type d'artefact summary (ou chemin dedie simple)
5. Ajouter un petit formateur markdown de summary
6. Ecrire le summary en fin de run dans le runner
7. Ajouter une section speciale en cas de `WAIT_HUMAN` (quoi faire ensuite)
8. Ajouter couverture de tests unite (orchestrateur/runner/summary)
9. Verifier que rien n'introduit une nouvelle couche d'architecture

## 13) Ce qui doit rester volontairement absent
Exclusions explicites (hors perimetre V1):
- Telegram
- API REST
- dashboard web
- systeme d'evenements
- persistance d'etat avancee
- observabilite distribuee
- metriques complexes
- moteur de notification

## Conclusion courte
L'amelioration la plus utile et sobre est: **petit summary workflow + contrats `data` stabilises**, portes par orchestrateur/runner, sans nouvelle architecture.
