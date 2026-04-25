# Analyse Phase 6 Etape 5 - Summary exploitable

## Synthese

Le summary actuel est deja utile, mais il n'est pas encore assez proche d'une interface humaine universelle.

Il donne bien :

* une decision
* une raison
* les artefacts
* une prochaine action

Mais il reste construit comme un resume technique produit par le runner, pas comme un message autonome, directement lisible par un humain ou relayable par Telegram sans interpretation supplementaire.

Recommandation nette :

* garder le summary comme source humaine principale ;
* normaliser fortement sa structure ;
* differencier explicitement les 4 cas cibles ;
* conserver la CLI comme contrat machine ;
* ne pas creer de moteur de template.

## 1. Etat actuel du summary

Dans `AnalysisReviewWorkflowRunner.buildSummaryContent(...)`, le summary est genere avec cette trame :

* `Decision: ...`
* `Reason: ...`
* `Correction triggered: ...`
* `Artifacts: ...`
* `Next steps: ...`

Pour `WAIT_HUMAN`, il existe deja deux branches :

* review humaine
* build persistant apres correction

Le summary a donc deja une bonne base fonctionnelle. Le probleme principal est la forme :

* la structure n'est pas explicitement standardisee par type de statut ;
* certaines informations sont encore deduites d'artefacts au lieu d'etre presentees comme message autonome ;
* la distinction entre info technique et action humaine pourrait etre plus nette.

## 2. Problemes identifies

### Lisibilite globale

Le format actuel est lisible par un developpeur, mais il reste un peu trop proche d'un log enrichi.

Points faibles :

* le titre n'indique pas clairement le statut comme priorite visuelle ;
* le champ `Reason` est correct, mais il n'est pas toujours suffisant pour comprendre l'action attendue ;
* la section `Artifacts` est utile, mais elle prend de la place alors que l'humain veut d'abord savoir quoi faire ;
* `Next steps` varie selon le statut, mais sans structure suffisamment stabile.

### Actionabilite

Le summary dit souvent quoi regarder, mais pas toujours pourquoi c'est le bon prochain pas.

Exemples :

* `STOP_FAILURE` met en avant l'echec, mais pas toujours la nature de la correction attendue ;
* `WAIT_HUMAN` review dit d'edit le review result, ce qui est bon pour le flux humain actuel, mais trop technique pour un futur relais Telegram ;
* `WAIT_HUMAN` build est mieux oriente, mais reste encore attache a la representation interne des artefacts.

### Telegram

Le format actuel est presque relayable tel quel, mais il peut encore etre plus robuste si on structure clairement :

* statut
* raison
* contexte
* action
* liens utiles

Sans cette standardisation, un bot Telegram devra peut-etre reformater le summary avant affichage.

## 3. Structure cible

Je recommande une structure standard par statut, tout en gardant une base commune.

### Structure commune

```text
Status: ...
Reason: ...

Context:
...

Actions:
1. ...
2. ...
3. ...

Artifacts:
- ...
```

### SUCCESS

Objectif :

* message court
* contexte minimal
* prochaine action simple

Structure cible :

```text
Status: CONTINUE
Reason: Build and workflow completed successfully

Context:
The workflow completed without requiring human intervention.

Actions:
1. Continue to the next phase.

Artifacts:
- workflowSummaryPath: ...
- buildResultPath: ...
```

### STOP_FAILURE

Objectif :

* signaler un echec technique
* expliquer la cause
* orienter vers la correction ou le rerun

Structure cible :

```text
Status: STOP_FAILURE
Reason: Maven build failed with a system error

Context:
The workflow stopped because the failure is technical and cannot be resolved automatically.

Actions:
1. Inspect the build artifact.
2. Fix the technical issue.
3. Rerun the workflow.

Artifacts:
- buildResultPath: ...
- workflowSummaryPath: ...
```

### WAIT_HUMAN review

Objectif :

* montrer qu'une decision humaine sur la review est attendue
* garder le lien vers l'artefact de review

Structure cible :

```text
Status: WAIT_HUMAN
Reason: Human decision required on the review result

Context:
The review requires a human decision before the workflow can continue.

Actions:
1. Inspect the review result.
2. Update the decision if needed.
3. Resume the workflow.

Artifacts:
- reviewResultPath: ...
- workflowSummaryPath: ...
```

### WAIT_HUMAN build

Objectif :

* indiquer que l'auto-correction a ete tentee
* montrer que l'agent a atteint sa limite
* guider vers l'inspection du build et de la correction

Structure cible :

```text
Status: WAIT_HUMAN
Reason: Build still fails after automatic correction attempts

Context:
The workflow attempted build correction automatically, but the build still fails.

Actions:
1. Inspect the build artifact.
2. Inspect the correction attempt.
3. Fix manually or adjust strategy.
4. Resume the workflow.

Artifacts:
- buildArtifactPath: ...
- correctionAttemptPath: ...
- workflowSummaryPath: ...
```

## 4. Format standard recommande

Je recommande ce format de base, stable et court :

```text
Status: ...
Reason: ...

Context:
...

Actions:
1. ...
2. ...

Artifacts:
- ...
```

Pourquoi ce format fonctionne :

* il est lisible sans contexte ;
* il est facile a relayer dans Telegram ;
* il reste simple a produire depuis le runner ;
* il n'exige pas de parsing complexe ;
* il se mappe bien sur les decisions existantes.

## 5. Compatibilite Telegram

Le summary peut devenir directement exploitable par Telegram si on respecte trois regles :

* message court et structure ;
* pas de paragraphe long avant le statut et l'action ;
* chemins d'artefacts presents mais secondaires.

Telegram supporte mal les messages trop verbeux. Il faut donc viser :

* une entete tres claire ;
* des actions numerotees courtes ;
* des liens ou chemins en fin de message.

Pas besoin de parser le summary cote bot si le format reste stable et lineaire.

## 6. Autonomie du summary

Le summary doit etre autonome.

Cela veut dire que le lecteur doit comprendre :

* le statut ;
* la raison ;
* l'action ;
* les artefacts pertinents ;
* sans ouvrir le code ;
* sans repartir au CLI.

Le summary n'a pas besoin de remplacer la CLI :

* CLI = machine
* summary = humain

## 7. Donnees essentielles

Les informations qui doivent toujours apparaitre sont :

* `Status`
* `Reason`
* `Actions`
* `Artifacts`
* `workflowSummaryPath` quand il existe

Pour les cas build, il faut aussi garder :

* `buildArtifactPath`
* `correctionAttemptPath`

Pour les cas review, il faut garder :

* `reviewResultPath`

Pour `SUCCESS`, `STOP_FAILURE`, `WAIT_HUMAN`, il faut que le summary reste comprehensible meme si certains chemins sont absents.

## 8. Exemples concrets

### SUCCESS

```text
Status: CONTINUE
Reason: Maven build validation passed

Context:
The workflow completed successfully.

Actions:
1. Continue to the next phase.

Artifacts:
- workflowSummaryPath: data/rapport/v1.1/CodexTime/Phase6/etape5/workflow-summary.md
- buildResultPath: data/rapport/v1.1/CodexTime/Phase6/etape5/result.5.build.md
```

### STOP_FAILURE

```text
Status: STOP_FAILURE
Reason: MavenValidationStep: Failed to start process

Context:
The workflow stopped because the failure is technical and cannot be resolved automatically.

Actions:
1. Inspect the build artifact.
2. Fix the technical issue.
3. Rerun the workflow.

Artifacts:
- workflowSummaryPath: data/rapport/v1.1/CodexTime/Phase6/etape5/workflow-summary.md
- buildResultPath: data/rapport/v1.1/CodexTime/Phase6/etape5/result.5.build.md
```

### WAIT_HUMAN build

```text
Status: WAIT_HUMAN
Reason: Build still fails after automatic correction attempts

Context:
The workflow attempted build correction automatically, but the build still fails.

Actions:
1. Inspect the build artifact.
2. Inspect the correction attempt.
3. Fix manually or adjust strategy.
4. Resume the workflow.

Artifacts:
- workflowSummaryPath: data/rapport/v1.1/CodexTime/Phase6/etape5/workflow-summary.md
- buildArtifactPath: data/rapport/v1.1/CodexTime/Phase6/etape5/result.5.build.md
- correctionAttemptPath: data/rapport/v1.1/CodexTime/Phase6/etape5/result.5.build-error-correction.md
```

### WAIT_HUMAN review

```text
Status: WAIT_HUMAN
Reason: Human decision required on the review result

Context:
The review requires a human decision before the workflow can continue.

Actions:
1. Inspect the review result.
2. Update the decision if needed.
3. Resume the workflow.

Artifacts:
- workflowSummaryPath: data/rapport/v1.1/CodexTime/Phase6/etape5/workflow-summary.md
- reviewResultPath: data/rapport/v1.1/CodexTime/Phase6/etape5/result.5.review.md
```

## 9. Plan d'implementation

1. Introduire une structure de base commune dans `buildSummaryContent(...)`.
2. Standardiser l'entete `Status / Reason / Context / Actions / Artifacts`.
3. Differencier explicitement `WAIT_HUMAN` build et `WAIT_HUMAN` review.
4. Raccourcir les textes pour qu'ils restent lisibles dans Telegram.
5. Garder les chemins d'artefacts, mais les relayer en fin de message.
6. Ajouter des tests sur la forme des summaries par statut.
7. Verifier que `workflowSummaryPath` reste optionnel si la configuration manque.
8. Garder la CLI comme contrat machine, sans la melanger au summary.

## 10. Contraintes respectees

* Pas d'implementation ici.
* Pas de Telegram.
* Pas de moteur de template.
* Pas de rupture du contrat existant.
* Pas de parsing complexe.

## Conclusion

Le summary actuel est deja fonctionnel, mais il doit devenir plus standardise et plus lisible en lecture immediate.

La meilleure direction est une structure simple et stable :

* `Status`
* `Reason`
* `Context`
* `Actions`
* `Artifacts`

Avec cette forme, le summary devient une vraie interface humaine, utilisable sans contexte technique, et suffisamment nette pour servir plus tard de base a Telegram.
