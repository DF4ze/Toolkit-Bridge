# Rapport d’analyse — Phase 3 Étape 3 — Introduction V1 de `WAIT_HUMAN`

## 1. Recommandation nette (A + question importante)

Recommandation V1 :
- faire retourner `WAIT_HUMAN` par **`GlobalReviewStep`** via une règle explicite et testable,
- **ne pas** créer une nouvelle step dédiée à la décision humaine à ce stade.

Pourquoi :
- le flux reste strictement linéaire (analysis -> review),
- la review est l’endroit naturel où une demande d’arbitrage humain apparaît,
- ajouter une step dédiée maintenant introduirait une abstraction prématurée (pseudo moteur humain) sans valeur immédiate.

## 2. Classes à modifier / créer

A modifier (implémentation future) :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
  - introduire la règle de détection `WAIT_HUMAN`.
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`
  - couvrir les cas `WAIT_HUMAN` et `CONTINUE`.

A garder tel quel (avec éventuels tests complémentaires seulement) :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
  - le comportement de stop sur non-`CONTINUE` est déjà correct pour un flux 2 steps.
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
  - déjà des tests sur `WAIT_HUMAN`; éventuellement enrichir avec le scénario réel issu de `GlobalReviewStep`.

A créer :
- aucune nouvelle classe nécessaire pour cette étape.
- aucun nouveau modèle nécessaire.

## 3. Responsabilité exacte (B)

### `GlobalReviewStep`
Doit :
- produire `REVIEW_PROMPT` et `REVIEW_RESULT` comme aujourd’hui,
- décider `WAIT_HUMAN` si la règle explicite est satisfaite,
- sinon retourner `CONTINUE`.

Ne doit pas :
- contacter un humain,
- gérer une file d’attente,
- persister un ticket humain,
- orchestrer une reprise.

### `WorkflowOrchestrator`
Doit :
- arrêter immédiatement quand une step renvoie `WAIT_HUMAN`,
- propager ce `WorkflowStepResult` tel quel (pas de transformation),
- ne pas exécuter la suite.

Ne doit pas :
- implémenter un moteur de suspension/reprise,
- enrichir un état complexe,
- router dynamiquement des steps.

## 4. Règle de déclenchement recommandée (C)

Règle V1 recommandée :
- si le `REVIEW_RESULT` contient une ligne explicite de convention, par exemple `DECISION: WAIT_HUMAN`, alors la step retourne `WAIT_HUMAN`.
- sinon `CONTINUE`.

Pourquoi cette règle :
- explicite (pas “magique”),
- simple à tester unitairement,
- visible dans l’artefact de review,
- stable avant toute intégration Telegram.

Important :
- rester sur une convention textuelle minimaliste,
- éviter parser sémantique, scoring, ou règles multi-critères.

## 5. `WorkflowStepResult` en cas de `WAIT_HUMAN` (D)

Recommandation sans nouveau modèle :
- `decision` : `WAIT_HUMAN`
- `message` : message court et exploitable (ex: "Global review requires human decision")
- `data` : minimal et utile, par exemple :
  - `promptArtifactPath`
  - `resultArtifactPath`
  - `waitReason` (valeur courte issue de la convention, si disponible)

Ne pas ajouter d’objet dédié de type `HumanInterventionRequest` à ce stade.

## 6. Réaction orchestrator dans le flux 2 steps (E)

Réponse explicite :
- arrêt immédiat : **oui**,
- propagation du résultat : **oui**, retour du `WorkflowStepResult` bloquant,
- mise à jour du contexte runtime : **non** pour cette étape,
- autre : aucune logique supplémentaire.

Concrètement dans le flux :
1. step 1 (analysis) `CONTINUE` -> step 2 exécutée,
2. step 2 renvoie `WAIT_HUMAN` -> orchestration terminée avec ce résultat.

## 7. Rapport au contexte runtime (F)

Réponse nette : **3) le contexte actuel suffit avec conventions simples**.

Justification :
- la décision `WAIT_HUMAN` passe déjà via `WorkflowStepResult`,
- les infos opératoires passent déjà via `message` + `data` + artefacts,
- `WorkflowExecutionContext` est immutable et convient pour cette V1.

Limite assumée :
- pas de mécanisme natif de reprise dans ce contexte (normal, hors périmètre).

## 8. Observabilité locale recommandée (G)

Approche minimale et pragmatique :
- observabilité primaire dans le `WorkflowStepResult` (`decision=WAIT_HUMAN`, message clair),
- observabilité documentaire via `resultArtifactPath` (`result.N.review.md`) qui contient la convention `DECISION: WAIT_HUMAN`.

Ne pas créer d’artefact supplémentaire dédié tant que ce n’est pas nécessaire.

## 9. Risques de sur-conception

Risques principaux :
- introduire trop tôt une step "HumanDecisionStep",
- ajouter un système de queue/tickets,
- implémenter une reprise générique,
- multiplier les règles de classification au lieu d’une convention unique.

Garde-fous :
- une seule règle de déclenchement explicite,
- aucune nouvelle couche technique,
- rester sur `GlobalReviewStep` + orchestrator existant.

## 10. Plan d’implémentation minimal (sobre)

1. Ajouter une convention explicite de décision dans le prompt de review (format `DECISION: ...`).
2. Dans `GlobalReviewStep`, lire `reviewResultContent` et détecter `DECISION: WAIT_HUMAN`.
3. Retourner `WorkflowStepResult(WAIT_HUMAN, message, data)` quand la convention est présente.
4. Sinon conserver le comportement `CONTINUE` actuel.
5. Conserver `STOP_FAILURE` inchangé pour erreurs techniques/exécution Codex.
6. Ajouter tests unitaires `GlobalReviewStepTest` : cas `WAIT_HUMAN` + cas nominal `CONTINUE`.
7. Vérifier que l’orchestrator 2-steps s’arrête bien en pratique sur ce retour (test ciblé ou intégration légère).

## 11. Ce qui doit rester volontairement absent (H)

Hors périmètre explicite de cette étape :
- Telegram,
- boucle homme/machine complète,
- système de reprise,
- file d’attente humaine,
- persistance avancée d’état,
- routing conditionnel riche,
- moteur de décision,
- règles complexes de classification,
- parser sémantique lourd,
- stratégie configurable complexe.

## 12. Conclusion

La V1 la plus propre est :
- **`WAIT_HUMAN` porté par `GlobalReviewStep`**,
- déclenché par une **convention explicite** dans le résultat de review,
- orchestrator qui **s’arrête immédiatement et propage le résultat**,
- **aucune nouvelle abstraction** tant que la vraie reprise humaine n’est pas demandée.
