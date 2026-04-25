# Resultat analyse - Phase 5 Etape 4

## 1. Strategie recommandee

Recommandation nette: simuler une mini conversation complete `run -> WAIT_HUMAN -> resume`, plutot que simuler seulement l'interpretation de sorties CLI.

Pourquoi:
- l'etape 3 valide deja l'interpretation d'une sortie CLI isolee;
- l'etape 4 doit valider le pilotage humain;
- le risque principal a tester est l'enchainement: commande utilisateur, reponse systeme, attente humaine, reprise explicite, nouvelle reponse systeme.

La simulation doit rester locale, testable et sans framework. Elle peut prendre la forme d'une petite classe Java de "fake chat" ou meme d'un test JUnit qui orchestre deux interactions. La solution la plus sobre est:
- une classe simple de rendu/interaction simulee;
- des tests JUnit qui alimentent cette classe avec des sorties CLI deja simulees;
- reutilisation de `WorkflowCliConsumerSimulator`.

## 2. Forme de simulation recommandee

Forme recommandee: classe Java simple de simulation de conversation, testee par JUnit.

Nom possible:
- `WorkflowUserInteractionSimulator`
- `WorkflowFakeChatSimulator`
- `WorkflowHumanInteractionSimulator`

Preference: `WorkflowUserInteractionSimulator`, car le besoin est de simuler l'interaction utilisateur, pas un chat Telegram complet.

Responsabilites:
- recevoir une commande utilisateur symbolique (`run`, `resume`);
- recevoir ou simuler la sortie CLI associee;
- deleguer l'interpretation a `WorkflowCliConsumerSimulator`;
- produire un message systeme lisible;
- ne pas conserver d'etat durable;
- ne pas relancer automatiquement `RESUME`.

Alternative encore plus sobre:
- ne pas creer de classe source et faire uniquement un test JUnit de conversation.

Mais comme l'objectif est de preparer un futur canal humain, une petite classe de rendu local est acceptable si elle ne devient pas une architecture de conversation.

## 3. Role du consommateur existant

Recommandation: reutiliser `WorkflowCliConsumerSimulator` tel quel.

Il ne doit pas etre enrichi dans cette etape sauf blocage concret.

Son role:
- parser la sortie CLI;
- produire un statut consommateur;
- exposer `message`, `nextAction`, `waitReason`, `workflowSummaryPath`.

Le simulateur d'interaction doit rester au-dessus:
- il transforme le resultat consomme en message humain;
- il decide si la conversation attend une intervention;
- il ne parse pas lui-meme `key=value`.

Ne pas transformer `WorkflowCliConsumerSimulator` en moteur de dialogue.

## 4. Format des messages simules

Messages systeme recommandes: texte simple, proche d'un futur message Telegram, mais sans syntaxe Telegram specifique.

Format conceptuel:

```text
Status: WAITING_FOR_HUMAN
Message: Global review requires human decision
Reason: Missing decision in review
Next action: Edit review result and resume with runCorrectionAfterReview(...)
Summary: D:\...\workflow-summary.md
```

Pour `SUCCESS`:

```text
Status: SUCCESS
Message: Correction completed
Next action: Continue workflow execution
```

Pour `FAILED`:

```text
Status: FAILED
Message: Correction failed
Next action: Inspect failure and related artifacts, then rerun
Summary: D:\...\workflow-summary.md
```

Regles:
- afficher `message` si present;
- afficher `waitReason` seulement si present;
- afficher `nextAction` comme guidance humaine;
- afficher `workflowSummaryPath` seulement si present;
- ne pas parser `nextAction`;
- ne pas parser le summary.

Pas de moteur de template. Une concatenation simple suffit.

## 5. Flow `RUN -> WAIT_HUMAN -> RESUME`

Flow propose:

1. Commande simulee utilisateur:

```text
run phase5 step4
```

2. Le simulateur recoit une sortie CLI de RUN, par exemple:

```text
decision=WAIT_HUMAN
message=Global review requires human decision
finalDecision=WAIT_HUMAN
nextAction=Edit review result and resume with runCorrectionAfterReview(...)
correctionTriggered=false
waitReason=Missing decision in review
workflowSummaryPath=D:\...\workflow-summary.md
```

3. `WorkflowCliConsumerSimulator` retourne `WAITING_FOR_HUMAN`.

4. Le simulateur produit un message systeme:

```text
WAITING_FOR_HUMAN - Global review requires human decision
Reason: Missing decision in review
Next action: Edit review result and resume with runCorrectionAfterReview(...)
Summary: D:\...\workflow-summary.md
```

5. Le simulateur ne reprend pas automatiquement.

6. Commande simulee utilisateur apres intervention:

```text
resume phase5 step4
```

7. Le simulateur recoit une sortie CLI de RESUME:

```text
decision=CONTINUE
message=Correction completed
finalDecision=CONTINUE
nextAction=Continue workflow execution
correctionTriggered=true
workflowSummaryPath=D:\...\workflow-summary.md
```

8. Il produit:

```text
SUCCESS - Correction completed
Next action: Continue workflow execution
Summary: D:\...\workflow-summary.md
```

## 6. Comment savoir quand declencher `RESUME`

Regle: le systeme ne declenche jamais `RESUME` automatiquement.

Le simulateur peut indiquer que `resume` est autorise ou attendu quand:
- statut consommateur = `WAITING_FOR_HUMAN`;
- l'utilisateur a fait une intervention manuelle externe;
- l'utilisateur envoie explicitement la commande `resume`.

Il ne doit pas:
- parser `nextAction` pour detecter le mot `resume`;
- supposer que l'intervention humaine est terminee;
- enchainer automatiquement apres `WAIT_HUMAN`.

## 7. Gestion des erreurs

Gestion simple:

### Erreur technique CLI

Si `WorkflowCliConsumerSimulator` retourne `ERROR`:
- afficher un message technique;
- afficher `message` si present;
- ne pas proposer `resume` automatiquement.

### `STOP_FAILURE`

Si statut `FAILED`:
- afficher `message`;
- afficher `nextAction`;
- afficher `workflowSummaryPath` si present;
- ne pas reprendre automatiquement.

### Decision inconnue

Si statut `UNKNOWN`:
- afficher "Unknown workflow decision";
- afficher les champs disponibles;
- demander inspection manuelle.

### Champs manquants

Regles:
- `message` absent -> message vide ou "No message provided";
- `nextAction` absent -> "Inspect workflow result";
- `waitReason` absent -> ne pas afficher de ligne Reason;
- `workflowSummaryPath` absent -> ne pas afficher de ligne Summary.

## 8. Rapport au summary

Position claire: le summary reste un support humain uniquement.

La simulation doit:
- afficher le chemin `workflowSummaryPath` si present;
- eventuellement, dans un test dedie futur, montrer que le contenu pourrait etre lu et affiche brut;
- ne pas parser le contenu;
- ne pas echouer si le summary est absent.

Pour cette etape, afficher le chemin suffit. Lire le contenu serait plus proche d'un futur Telegram, mais risque de pousser vers gestion de taille, fichiers absents et rendu. Ce n'est pas necessaire pour prouver le flow `run/resume`.

## 9. Preparation Telegram

Oui, cette simulation prepare Telegram.

Messages futurs presque identiques:
- message WAIT_HUMAN:
  - titre/status;
  - message;
  - raison;
  - nextAction;
  - summary path ou extrait.
- message SUCCESS:
  - status;
  - message;
  - nextAction.
- message FAILED:
  - status;
  - message;
  - nextAction;
  - summary path si disponible.

Informations restant a brancher plus tard:
- mapping entre utilisateur Telegram et contexte workflow;
- reception de la commande `/run` ou `/resume`;
- envoi reel via API Telegram;
- gestion des droits;
- gestion de la taille du summary;
- eventuelle resolution de chemins locaux en pieces jointes ou liens.

Tout cela doit rester hors perimetre maintenant.

## 10. Risques de sur-conception

Risques:
- creer une vraie state machine de conversation;
- stocker un etat de chat;
- introduire des commandes type Telegram;
- creer un moteur de template;
- ajouter une API REST;
- faire une abstraction "channel";
- transformer le simulateur en service applicatif.

Garde-fou:
- une conversation = une sequence de test;
- pas d'etat durable;
- pas d'automatisation de reprise;
- pas de dependance externe;
- rendu texte simple.

## 11. Plan d'implementation minimal

1. Creer une classe simple, par exemple `WorkflowUserInteractionSimulator`.
2. Lui injecter ou lui fournir un `WorkflowCliConsumerSimulator`.
3. Ajouter une methode qui transforme un `SimulatedConsumerResult` en message systeme lisible.
4. Ajouter une methode optionnelle pour traiter une commande symbolique (`run`, `resume`) avec une sortie CLI fournie par le test.
5. Ne pas appeler de process CLI reel.
6. Ajouter des tests JUnit:
   - `run -> WAIT_HUMAN` produit un message avec reason, nextAction, summary.
   - `WAIT_HUMAN` ne declenche pas resume automatiquement.
   - `resume -> SUCCESS` produit un message exploitable.
   - `STOP_FAILURE` produit un message d'erreur fonctionnelle.
   - `ERROR` produit un message d'erreur technique.
   - `UNKNOWN` produit un message d'inspection manuelle.
7. Documenter dans le rapport que cette simulation prepare Telegram mais ne l'implemente pas.

## 12. Ce qui doit rester volontairement absent

- Pas de vrai bot Telegram.
- Pas de gestion de chat state.
- Pas de multi-utilisateur.
- Pas de permissions.
- Pas de webhook.
- Pas de polling.
- Pas d'API REST.
- Pas de persistance de conversation.
- Pas de moteur de dialogue.
- Pas de state machine.
- Pas de moteur de template.
- Pas de parsing de `nextAction`.
- Pas de parsing du summary.
- Pas de reprise automatique.

## Conclusion

Pour cette etape, il vaut mieux simuler une mini conversation complete `run/resume`. L'interpretation isolee est deja couverte par l'etape 3; la valeur nouvelle est de prouver que l'utilisateur peut piloter le workflow avec des messages simples.

La simulation doit reutiliser `WorkflowCliConsumerSimulator` tel quel, produire des messages texte tres simples, afficher le summary path sans parser le fichier, et imposer que la reprise soit declenchee explicitement par l'utilisateur.
