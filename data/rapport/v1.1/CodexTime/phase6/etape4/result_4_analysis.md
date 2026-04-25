# Analyse Phase 6 Etape 4 - STOP_FAILURE vs WAIT_HUMAN

## Synthese

Le systeme sait maintenant corriger automatiquement une erreur Maven, puis revalider. Le prochain seuil architectural n'est pas d'ajouter plus d'automatisation, mais de savoir quand l'automatisation doit s'arreter proprement pour demander une decision humaine.

Recommandation nette :

- conserver `STOP_FAILURE` pour les problemes techniques, incomplets ou invalides ;
- utiliser `WAIT_HUMAN` pour les cas ou le systeme a travaille correctement mais ne peut plus decider seul ;
- integrer cette distinction principalement dans `AnalysisReviewWorkflowRunner`, car c'est lui qui voit le cycle complet validation -> correction -> revalidation ;
- ne pas deplacer cette logique dans l'orchestrateur ;
- ne pas creer de moteur de classification.

## 1. Cas qui devraient devenir `WAIT_HUMAN`

### Erreur persistante apres retry

Cas prioritaire.

Si Maven retourne encore `FAILURE` apres que les tentatives de correction automatique ont ete consommees, ce n'est plus seulement une erreur technique brute. Le systeme a essaye de corriger et n'a pas reussi.

Decision recommandee :

```text
WAIT_HUMAN
```

Raison :

- Codex a deja eu l'occasion de corriger ;
- continuer automatiquement serait probablement une boucle inutile ;
- un humain doit regarder l'erreur, l'artefact build et la correction tentee.

### Correction Codex incertaine ou vide

Si `BuildErrorCorrectionStep` execute Codex avec succes technique mais que la sortie ne contient rien d'exploitable, ou indique explicitement une impossibilite de corriger, le bon comportement est de demander une intervention.

Decision recommandee :

```text
WAIT_HUMAN
```

Exemples simples a detecter plus tard, sans parsing lourd :

- sortie vide ;
- sortie tres courte ;
- message explicite du type "cannot fix", "need more context", "manual intervention".

Pour cette etape, il faut rester prudent : ne pas construire une analyse semantique complete.

### Erreur Maven ambigue

Une erreur Maven peut etre ambigue si elle ne ressemble pas clairement a une erreur de compilation locale.

Exemples :

- dependance introuvable ;
- plugin Maven introuvable ;
- erreur de repository ;
- conflit de version difficile a arbitrer ;
- erreur liee a un module absent.

Decision recommandee :

```text
WAIT_HUMAN
```

Mais seulement si un signal simple existe. Sans signal clair, ne pas classifier agressivement.

### Decision metier necessaire

Certains echecs ne peuvent pas etre resolus par du code seul.

Exemples :

- test ou build implique un choix fonctionnel ;
- suppression ou changement d'API publique ;
- modification de contrat externe ;
- choix de dependance ou de version structurante.

Decision recommandee :

```text
WAIT_HUMAN
```

Dans le systeme actuel, ces cas seront probablement reperes via le message Codex ou via le resume humain, pas par Maven directement.

## 2. Cas qui doivent rester `STOP_FAILURE`

### Erreur technique systeme

Les problemes d'environnement doivent rester `STOP_FAILURE`.

Exemples :

- `SYSTEM_ERROR` ;
- Maven wrapper introuvable ;
- JVM indisponible ;
- erreur d'I/O lors de l'ecriture d'artefact ;
- contexte obligatoire manquant ;
- donnees de retry incoherentes ;
- timeout de process.

Raison :

Ces erreurs ne demandent pas une decision produit ou une correction de code. Elles demandent de reparer l'environnement ou le wiring.

### Timeout

Le timeout doit rester :

```text
STOP_FAILURE
```

Raison :

Un timeout n'indique pas que Codex ne sait pas corriger. Il indique que l'execution technique n'a pas termine dans les bornes attendues.

### Etat interne invalide

Exemples :

- `buildErrorRetryCount` absent alors que le runner recoit `RETRY_CORRECTION` ;
- `buildErrorRetryMax` invalide ;
- chemin d'artefact manquant.

Decision :

```text
STOP_FAILURE
```

Raison :

Il s'agit d'un bug de workflow ou d'une incoherence runtime, pas d'une demande d'arbitrage humain sur le code.

## 3. Ou integrer la logique

### Recommandation principale : dans le runner

La distinction `STOP_FAILURE` vs `WAIT_HUMAN` doit vivre principalement dans `AnalysisReviewWorkflowRunner`.

Raison :

- `MavenValidationStep` voit un resultat Maven ponctuel, pas l'histoire complete ;
- `BuildErrorCorrectionStep` voit la tentative Codex, mais pas forcement le resultat de revalidation ;
- le runner voit le cycle complet : validation, retry, correction, revalidation, maximum atteint ;
- l'orchestrateur ne doit pas devenir un moteur de politique decisionnelle.

### Role de `MavenValidationStep`

`MavenValidationStep` doit rester responsable de la classification technique Maven :

- `SUCCESS -> CONTINUE`
- `FAILURE + retry disponible -> RETRY_CORRECTION`
- `TIMEOUT -> STOP_FAILURE`
- `SYSTEM_ERROR -> STOP_FAILURE`

Il peut exposer des donnees utiles (`buildStatus`, `buildErrorSummary`, `buildArtifactPath`, retry count), mais il ne devrait pas decider seul qu'un echec persistant devient `WAIT_HUMAN`.

### Role de `BuildErrorCorrectionStep`

`BuildErrorCorrectionStep` peut aider a signaler une incertitude simple :

- sortie Codex vide ;
- echec Codex ;
- correction impossible explicite, si detectee sobrement.

Mais il ne doit pas devenir un analyseur semantique de sortie Codex.

### Orchestrateur

Ne pas modifier l'orchestrateur.

Il sait deja propager `WAIT_HUMAN` et `STOP_FAILURE`. La politique specifique build retry appartient au runner.

## 4. Donnees a utiliser

### Donnees fiables maintenant

- `WorkflowStepDecision`
- `buildStatus`
- `buildErrorRetryCount`
- `buildErrorRetryMax`
- `buildErrorSummary`
- `buildArtifactPath`
- `correctionAttemptPath`
- `resultArtifactPath`
- `message`
- `nextAction`

### Donnees a utiliser en priorite

1. `buildStatus`
2. `buildErrorRetryCount`
3. `buildErrorRetryMax`
4. presence d'une correction tentee
5. message d'erreur final

### Donnees a eviter pour l'instant

- parsing complexe de Maven ;
- classification fine des erreurs Java ;
- analyse semantique complete de la sortie Codex ;
- heuristiques fragiles sur de longues traces.

## 5. Nouveau mapping recommande

| Cas | Decision | Raison |
| --- | --- | --- |
| Maven `SUCCESS` | `CONTINUE` | Build valide |
| Maven `FAILURE`, retry disponible | `RETRY_CORRECTION` | Correction automatique encore possible |
| Maven `FAILURE`, retry max atteint apres correction | `WAIT_HUMAN` | Le systeme a tente de corriger mais n'a pas reussi |
| Maven `FAILURE`, retry max atteint sans contexte de correction | `STOP_FAILURE` | Etat technique incoherent ou execution non conforme |
| Maven `FAILURE` ambigue detectee sobrement | `WAIT_HUMAN` | Decision ou diagnostic humain necessaire |
| Codex correction vide/incertaine | `WAIT_HUMAN` | Codex n'a pas produit de correction exploitable |
| Codex execution failure technique | `STOP_FAILURE` | Probleme d'execution Codex |
| Maven `TIMEOUT` | `STOP_FAILURE` | Probleme technique ou build bloque |
| Maven `SYSTEM_ERROR` | `STOP_FAILURE` | Probleme d'environnement |
| Contexte runtime invalide | `STOP_FAILURE` | Bug de workflow ou wiring incorrect |

## 6. Impact sur le workflow

### Runner

Impact modere.

Le runner doit transformer certains arrets apres retry en `WAIT_HUMAN`, avec un `waitReason` explicite.

Exemple de data utile :

```text
finalDecision=WAIT_HUMAN
waitReason=Build still fails after automatic correction attempts
nextAction=Inspect build artifact and correction attempt, then decide whether to fix manually or rerun
buildArtifactPath=...
correctionAttemptPath=...
```

### Steps

Impact faible.

`MavenValidationStep` peut rester quasiment identique. Il continue a retourner `STOP_FAILURE` quand le retry max est atteint. Le runner peut ensuite convertir ce cas en `WAIT_HUMAN` seulement s'il sait qu'une correction automatique a deja ete tentee.

`BuildErrorCorrectionStep` peut eventuellement exposer une information simple en data si la correction est vide ou incertaine, mais ce n'est pas indispensable pour une premiere implementation.

### Orchestrateur

Aucun impact recommande.

Il doit rester un executeur de steps, pas un classifieur decisionnel.

### CLI

Impact faible.

Le CLI expose deja :

- `decision`
- `message`
- `finalDecision`
- `nextAction`
- `waitReason`
- `workflowSummaryPath`

Si le runner retourne `WAIT_HUMAN` avec `waitReason`, le CLI peut deja le transmettre.

### Summary

Impact faible a modere.

Le summary gere deja `WAIT_HUMAN`, mais son texte actuel est oriente review :

```text
Edit review result...
```

Pour cette phase, il faudra ajuster le summary pour que `WAIT_HUMAN` build affiche une action adaptee :

- inspecter l'artefact build ;
- inspecter la correction tentee ;
- corriger manuellement ou relancer apres decision.

Il ne faut pas creer un moteur de templates. Une branche simple basee sur la presence de `buildArtifactPath` ou `buildStatus` suffit.

## 7. Impact Telegram

Cette distinction prepare directement Telegram.

Un futur bot pourra lire :

- `finalDecision=WAIT_HUMAN`
- `waitReason`
- `nextAction`
- `workflowSummaryPath`
- `buildArtifactPath`
- `correctionAttemptPath`

Message futur presque direct :

```text
Le build echoue encore apres correction automatique.
Raison : Build still fails after automatic correction attempts.
Action : inspecter l'artefact build et choisir une correction manuelle ou une relance.
Summary : ...
```

Information restante a brancher plus tard :

- routage du message vers le bon chat ;
- lien ou lecture securisee des artefacts ;
- commande de reprise ;
- etat conversationnel minimal.

Rien de cela ne doit etre implemente ici.

## 8. Risques

### Trop de `WAIT_HUMAN`

Risque : transformer toute erreur en demande humaine et perdre l'interet de l'auto-correction.

Mitigation :

- ne classifier en `WAIT_HUMAN` que les cas persistants apres tentative ou les incertitudes explicites ;
- garder `SYSTEM_ERROR` et `TIMEOUT` en `STOP_FAILURE`.

### Pas assez de `WAIT_HUMAN`

Risque : continuer a traiter les echecs persistants comme des pannes techniques alors qu'un humain doit decider.

Mitigation :

- convertir le cas "retry max atteint apres correction tentee" en `WAIT_HUMAN`.

### Mauvaise classification

Risque : une erreur technique de dependance devient `WAIT_HUMAN`, ou une erreur metier devient `STOP_FAILURE`.

Mitigation :

- utiliser d'abord des signaux simples ;
- ne pas multiplier les heuristiques ;
- conserver dans `data` les artefacts permettant a l'humain de comprendre.

### Complexification prematuree

Risque : creer un classifieur, une policy globale ou une taxonomie trop fine.

Mitigation :

- pas de nouveau service ;
- pas de modele complexe ;
- une methode privee dans le runner suffit pour commencer.

## 9. Plan d'implementation minimal

1. Ajouter une petite methode privee dans `AnalysisReviewWorkflowRunner`, par exemple `buildWaitHumanAfterRetryFailure(...)`.
2. Dans `runWithValidationAndRetry`, detecter le cas final `STOP_FAILURE` avec `buildStatus=FAILURE` et correction deja tentee.
3. Convertir uniquement ce cas en `WAIT_HUMAN`.
4. Ajouter `waitReason` avec un texte stable.
5. Ajouter ou conserver `nextAction` oriente inspection humaine des artefacts build/correction.
6. Ne pas convertir `TIMEOUT`, `SYSTEM_ERROR` ou etat de retry invalide.
7. Ajuster le summary pour que `WAIT_HUMAN` build ne parle pas de review result.
8. Ajouter tests runner :
   - failure apres retry max -> `WAIT_HUMAN` ;
   - timeout -> `STOP_FAILURE` ;
   - system error -> `STOP_FAILURE` ;
   - retry state invalide -> `STOP_FAILURE`.
9. Ajouter test CLI si necessaire pour confirmer `waitReason` expose.

## 10. Ce qui doit rester absent

- Pas de Telegram reel.
- Pas de moteur de classification.
- Pas de policy engine.
- Pas de refactor orchestrateur.
- Pas de parsing Maven avance.
- Pas de nouveau DTO.
- Pas de systeme multi-utilisateur.
- Pas de persistance de decision humaine.
- Pas de state machine.

## Conclusion

Le bon premier pas est de convertir en `WAIT_HUMAN` seulement les echecs persistants apres correction automatique, car ce cas est objectivement une limite de l'agent et non une simple panne technique.

`STOP_FAILURE` doit rester reserve aux erreurs techniques, aux timeouts, aux erreurs systeme et aux incoherences runtime.

Cette evolution prepare Telegram sans l'introduire : un consommateur externe pourra afficher un message clair, pointer vers les artefacts et attendre une intervention humaine.
