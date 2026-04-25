# Resultat analyse - Phase 5 Etape 3

## 1. Strategie recommandee

Recommandation nette: implementer un consommateur simule minimal autour du CLI existant, pas un bot Telegram reel, pas une nouvelle couche applicative.

La forme la plus pragmatique est un petit consommateur technique qui:
- execute le CLI en mode `RUN` ou `RESUME`;
- lit la sortie `key=value`;
- construit une map locale de valeurs;
- applique une logique de pilotage tres simple;
- affiche ou retourne une decision d'action.

Deux formes sont possibles:
- script shell/PowerShell;
- classe Java simple lanceuse de processus.

Recommandation: classe Java simple de simulation, testee avec un processus factice ou avec une methode de parsing isolee.

Pourquoi Java plutot qu'un script:
- testable dans Maven/JUnit;
- portable dans le projet;
- evite les differences PowerShell/Bash;
- reste sans framework;
- valide quand meme le contrat CLI.

Cette classe ne doit pas devenir une facade metier. Elle doit representer un consommateur externe minimal, proche d'un futur Telegram, mais sans Telegram.

## 2. Type de consommateur recommande

Nom possible:
- `WorkflowCliConsumerSimulator`
- `AnalysisReviewWorkflowCliConsumer`
- `WorkflowExternalConsumerSimulator`

Preference: `WorkflowCliConsumerSimulator`.

Responsabilites strictes:
- recevoir une sortie CLI brute;
- parser `key=value`;
- interpreter les champs stabilises;
- produire une decision de pilotage simple ou un texte humain.

Il ne doit pas:
- construire le runner;
- appeler directement `AnalysisReviewWorkflowRunner`;
- connaitre `WorkflowOrchestrator`;
- parser `workflow-summary.md`;
- envoyer des messages Telegram;
- persister un etat.

## 3. Logique de parsing

Parsing recommande:
- lire la sortie ligne par ligne;
- ignorer les lignes vides;
- pour chaque ligne contenant `=`:
  - cle = partie avant le premier `=`;
  - valeur = partie apres le premier `=`;
  - trim de la cle;
  - conserver la valeur telle quelle ou trim leger;
- ignorer ou collecter les lignes invalides comme avertissements.

Gestion des champs optionnels:
- `waitReason`: absent si non applicable;
- `workflowSummaryPath`: absent si le summary n'a pas ete produit.

Gestion des erreurs:
- si le CLI retourne un exit code non-zero, le consommateur doit traiter cela comme une erreur technique;
- si `finalDecision` manque, fallback sur `decision`;
- si `nextAction` manque, fallback texte court: `Inspect workflow result`;
- si `correctionTriggered` manque, fallback `false`;
- si `decision` et `finalDecision` manquent, resultat invalide.

Le parsing doit rester volontairement simple. Pas de JSON, pas de schema, pas de serialisation avancee.

## 4. Logique de pilotage minimale

Logique concrete recommandee:

```text
if exitCode != 0:
    status = TECHNICAL_FAILURE
    display stderr / error message

else if finalDecision == WAIT_HUMAN:
    status = WAITING_FOR_HUMAN
    display message
    display waitReason if present
    display nextAction
    display workflowSummaryPath if present
    do not auto-resume

else if finalDecision == STOP_FAILURE:
    status = FAILED
    display message
    display nextAction
    display workflowSummaryPath if present

else if finalDecision == CONTINUE:
    status = COMPLETED_OR_CONTINUE
    display message
    display nextAction
    if correctionTriggered == true:
        mention correction executed

else:
    status = UNKNOWN_DECISION
    display raw fields
```

Cette logique suffit a prouver que le contrat est exploitable par un consommateur externe.

## 5. Gestion de `WAIT_HUMAN`

Flow simple:

1. Le consommateur lance le CLI en mode `RUN`.
2. Il parse la sortie.
3. Si `finalDecision=WAIT_HUMAN`:
   - afficher `message`;
   - afficher `waitReason` si present;
   - afficher `nextAction`;
   - afficher `workflowSummaryPath` si present;
   - ne pas relancer automatiquement;
   - attendre une intervention humaine sur le fichier de review indique dans le summary ou dans les artefacts.
4. Une fois l'intervention faite, le consommateur peut lancer une commande de reprise explicite.

Important: `WAIT_HUMAN` est un stop volontaire. Le consommateur ne doit pas deviner ni automatiser la decision humaine.

## 6. Utilisation de `nextAction`

Recommandation claire: `nextAction` doit etre utilise comme guidance humaine, pas comme condition machine principale.

La condition machine doit rester:
- `finalDecision` ou `decision`.

`nextAction` sert a:
- afficher l'action recommandee;
- enrichir un message Telegram futur;
- guider l'operateur.

Le consommateur ne doit pas parser le texte de `nextAction` pour decider du mode suivant. Exemple: ne pas chercher `resume` dans le texte. Pour reprendre, il doit se baser sur `finalDecision=WAIT_HUMAN` puis une action explicite de l'humain.

## 7. Utilisation de `workflow-summary.md`

Position simple: le summary est un support humain, pas une source machine.

Le consommateur peut:
- afficher le chemin `workflowSummaryPath`;
- lire le contenu et l'afficher brut dans un mode humain;
- fournir ce contenu a un futur message Telegram si la taille le permet.

Le consommateur ne doit pas:
- parser le summary pour obtenir la decision;
- deduire la reprise depuis le texte du summary;
- echouer si `workflowSummaryPath` est absent.

Si `workflowSummaryPath` est absent:
- afficher le contrat machine disponible;
- continuer selon `finalDecision`.

## 8. Declenchement de la reprise

Recommandation: utiliser le mode CLI existant `RESUME`.

Commande conceptuelle:

```text
AnalysisReviewWorkflowCli --mode=RESUME --reportRootDirectory=<path> --reportVersion=<version> --reportPhase=<phase> --stepNumber=<n>
```

Le consommateur doit conserver ou recevoir les memes parametres de reporting que le `RUN` initial:
- `reportRootDirectory`
- `reportVersion`
- `reportPhase`
- `stepNumber`

Pas de commande separee, pas de flag supplementaire, pas de DSL.

## 9. Limites du contrat actuel

Limites identifiees:

1. Pas de liste exhaustive des chemins d'artefacts dans la sortie CLI.
   - Le summary les contient souvent.
   - Le consommateur minimal peut se contenter du summary path.

2. `key=value` est simple mais moins robuste que JSON si les valeurs contiennent des caracteres complexes.
   - La normalisation actuelle en une ligne limite le risque.
   - Ne justifie pas JSON maintenant.

3. Le CLI ne separe pas explicitement erreur technique et decision metier via un modele riche.
   - L'exit code gere l'erreur technique.
   - `finalDecision` gere l'etat metier.

4. `nextAction` est du texte humain.
   - Ne doit pas etre parse.

5. `workflowSummaryPath` est optionnel.
   - Le consommateur doit accepter son absence.

6. Le consommateur doit connaitre les arguments necessaires au `RESUME`.
   - C'est acceptable: il les a deja fournis au `RUN`.

## 10. Robustesse recommandee

Le consommateur doit se proteger ainsi:

- verifier l'exit code CLI;
- lire stderr si exit code non-zero;
- exiger au minimum `decision` ou `finalDecision`;
- fallback `finalDecision = decision` si absent;
- fallback `message = ""`;
- fallback `nextAction = "Inspect workflow result"`;
- fallback `correctionTriggered = false`;
- accepter l'absence de `waitReason`;
- accepter l'absence de `workflowSummaryPath`;
- traiter toute decision inconnue comme `UNKNOWN_DECISION`;
- ne jamais parser `workflow-summary.md` pour piloter la machine.

## 11. Preparation Telegram

Un bot Telegram pourrait consommer ce contrat tel quel avec conditions.

Champs directement utiles:
- `finalDecision`: choix du type de message;
- `message`: titre court;
- `nextAction`: instruction;
- `waitReason`: contexte de blocage humain;
- `workflowSummaryPath`: lien ou contenu a afficher;
- `correctionTriggered`: information de contexte.

Ajustements minimaux eventuels avant Telegram reel:
- une fonction de rendu texte Telegram qui prend la map parsee;
- limites de taille si le summary est lu et envoye;
- gestion des chemins locaux si Telegram ne peut pas ouvrir directement les fichiers.

Pas necessaire maintenant:
- bot reel;
- webhook;
- API Telegram;
- persistance;
- modele de notification.

## 12. Le contrat actuel est-il suffisant ?

Reponse: oui, avec conditions.

Il est suffisant pour un consommateur externe minimal parce que:
- la sortie CLI est stable et ligne par ligne;
- les decisions machine sont explicites;
- `nextAction` donne une guidance humaine;
- `workflowSummaryPath` donne un support humain quand disponible;
- `RESUME` est deja un mode explicite.

Conditions:
- le consommateur doit respecter la difference entre decision machine et texte humain;
- il doit accepter les champs optionnels absents;
- il doit se proteger contre les exit codes non-zero;
- il ne doit pas parser `nextAction` ni `workflow-summary.md` pour piloter la logique.

## 13. Plan d'implementation minimal

1. Creer un petit consommateur simule, par exemple `WorkflowCliConsumerSimulator`.
2. Ajouter une methode de parsing de sortie CLI `key=value` vers `Map<String, String>`.
3. Ajouter une methode d'interpretation qui produit un statut simple:
   - `TECHNICAL_FAILURE`
   - `WAITING_FOR_HUMAN`
   - `FAILED`
   - `CONTINUE`
   - `UNKNOWN_DECISION`
4. Simuler les sorties CLI dans des tests unitaires, sans lancer le vrai Codex.
5. Tester:
   - `WAIT_HUMAN` avec `waitReason` et `workflowSummaryPath`;
   - `STOP_FAILURE`;
   - `CONTINUE`;
   - champs optionnels absents;
   - sortie invalide;
   - exit code non-zero.
6. Optionnel: ajouter un test avec `AnalysisReviewWorkflowCli` stubbe pour valider le flux complet sans processus OS.
7. Documenter que `nextAction` et `workflow-summary.md` sont humains, non parseables pour pilotage.

## 14. Ce qui doit rester volontairement absent

- Pas de bot Telegram reel.
- Pas de framework externe.
- Pas de JSON.
- Pas de DTO metier.
- Pas de couche applicative nouvelle.
- Pas de systeme d'evenements.
- Pas de persistence.
- Pas de refactor du runtime.
- Pas de parsing machine du summary.
- Pas d'automatisation de la decision humaine.

## Conclusion

La meilleure etape suivante est un consommateur simule minimal qui prouve que le contrat CLI est exploitable. Il doit parser `key=value`, appliquer une logique simple sur `finalDecision`, afficher `nextAction` et utiliser `workflow-summary.md` uniquement comme support humain.

Cela valide le chemin vers Telegram sans construire Telegram, et sans complexifier le runtime.
