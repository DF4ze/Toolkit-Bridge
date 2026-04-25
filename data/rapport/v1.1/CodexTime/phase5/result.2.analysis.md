# Resultat analyse - Phase 5 Etape 2

## 1. Strategie recommandee

Recommandation nette: stabiliser strictement les cles existantes, sans introduire de projection externe maintenant.

Le systeme dispose deja d'un contrat exploitable:
- `WorkflowStepResult.decision()`
- `WorkflowStepResult.message()`
- `WorkflowStepResult.data()`
- `workflow-summary.md`
- sortie CLI `key=value`

L'etape doit rendre ce contrat explicite, regulier et teste. Elle ne doit pas creer un nouveau DTO metier ni un modele parallele. Une projection externe legere peut devenir utile plus tard si un deuxieme consommateur reel apparait, mais elle serait prematuree pour cette etape.

## 2. Liste minimale des cles stabilisees

### Cles obligatoires cote contrat externe

Ces cles doivent etre disponibles pour tout resultat expose vers l'exterieur, soit directement dans `WorkflowStepResult`, soit dans `WorkflowStepResult.data`.

- `decision`
  - source: `WorkflowStepResult.decision()`
  - obligatoire
  - valeurs attendues aujourd'hui: `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`
  - role: etat machine principal du workflow.

- `message`
  - source: `WorkflowStepResult.message()`
  - obligatoire cote affichage externe, meme si la valeur interne peut actuellement etre `null`
  - recommandation: exposer une chaine vide ou un message de fallback cote sortie externe, sans changer le record.

- `finalDecision`
  - source: `WorkflowStepResult.data`
  - obligatoire pour les resultats consolides par le runner/orchestrator
  - fallback acceptable: `decision.name()`
  - role: decision exposee comme contrat stable pour clients simples.

- `nextAction`
  - source: `WorkflowStepResult.data`
  - obligatoire pour les resultats consolides
  - fallback acceptable selon `decision`
  - role: action courte et lisible pour humain ou canal externe.

- `correctionTriggered`
  - source: `WorkflowStepResult.data`
  - obligatoire
  - type recommande: boolean
  - role: indique si une correction a ete effectivement declenchee dans le parcours courant.

### Cles optionnelles stables

- `waitReason`
  - optionnelle
  - presente uniquement quand `decision == WAIT_HUMAN` ou quand une raison explicite existe.
  - absence signifie: aucune raison specifique fournie; le consommateur peut afficher un fallback.

- `workflowSummaryPath`
  - optionnelle
  - presente uniquement si `workflow-summary.md` a ete genere.
  - absence explicite et normale si les variables de reporting manquent ou si l'ecriture du summary echoue.

### Cles d'artefacts utiles mais non minimales

Ces cles existent et sont utiles, mais ne doivent pas devenir le noyau du contrat minimal:
- `promptArtifactPath`
- `resultArtifactPath`
- `analysisResultPath`
- `reviewResultPath`
- `correctionResultPath`
- `reviewPromptPath`
- `correctionPromptPath`

Recommandation: les conserver comme extensions documentaires. Le summary peut les lister automatiquement via les cles finissant par `Path`. Le CLI V1 n'a pas besoin de toutes les exposer.

### Cles a supprimer ?

Aucune suppression recommandee a ce stade. Supprimer des cles existantes augmenterait le risque de regression. Il faut plutot documenter:
- noyau stable minimal;
- cles optionnelles;
- chemins d'artefacts comme extensions.

## 3. Regles de presence et d'absence

Recommandation uniforme: preferer l'absence de cle pour les informations non disponibles, plutot qu'une cle avec `null` ou une valeur sentinelle comme `NONE`.

Raisons:
- `WorkflowStepResult` copie deja les maps et evite les mutations, mais ne donne pas de semantique speciale a `null`.
- Une cle absente exprime naturellement "non applicable" ou "non produit".
- Les valeurs sentinelles ajoutent une convention artificielle que les clients devraient parser.

Regles proposees:
- `finalDecision`: toujours present dans les resultats consolides; fallback externe sur `decision.name()`.
- `nextAction`: toujours present dans les resultats consolides; fallback externe selon `decision`.
- `correctionTriggered`: toujours present dans les resultats consolides; fallback externe `false` uniquement si l'ancien resultat n'est pas encore enrichi.
- `waitReason`: absent sauf `WAIT_HUMAN` avec raison explicite.
- `workflowSummaryPath`: absent si aucun summary n'a ete produit.
- `message`: ne pas exposer `null`; cote sortie externe, convertir en chaine vide ou fallback lisible.

Point sensible: aujourd'hui, certaines erreurs precoces de steps peuvent retourner des donnees minimales. Le runner et l'orchestrator enrichissent deja une grande partie des cas. L'implementation devra verifier les chemins qui echappent encore au noyau stable.

## 4. Coherence CLI / runtime

Le CLI reflete partiellement le contrat runtime:
- il expose `decision`;
- il expose `message`;
- il expose `finalDecision`;
- il expose `nextAction`;
- il expose `workflowSummaryPath` si present.

Manques actuels:
- `correctionTriggered` n'est pas expose par le CLI alors qu'il fait partie du contrat implicite utile.
- `waitReason` n'est pas expose par le CLI alors qu'il est utile en `WAIT_HUMAN`.
- les valeurs `message` / `nextAction` ne sont pas normalisees pour une sortie ligne unique; un retour ligne pourrait casser le format `key=value`.

Recommandation:
- ajouter `correctionTriggered` dans la sortie CLI;
- ajouter `waitReason` uniquement si present;
- garder `workflowSummaryPath` optionnel;
- ne pas exposer tous les chemins d'artefacts dans le CLI V1, sauf besoin explicite;
- normaliser les valeurs en une ligne pour que `key=value` reste consommable.

## 5. Role du `workflow-summary.md`

Position claire: `workflow-summary.md` est le support principal pour lecture humaine, mais pas la source machine principale.

Roles:
- complement humain du contrat machine;
- recapitulatif operationnel lisible;
- support naturel pour Telegram si le bot doit envoyer un message long ou guider l'utilisateur;
- liste des artefacts disponibles.

Il ne doit pas devenir:
- la source de verite machine;
- un document a parser pour determiner la decision;
- une condition de succes du workflow.

La source machine principale doit rester `WorkflowStepResult` et ses cles stabilisees. Le summary est un complement et un fallback humain quand le client externe veut afficher plus de contexte.

## 6. Format externe recommande

Recommandation: garder `key=value` en V1, avec normalisation legere.

Pourquoi:
- tres simple;
- lisible en console;
- suffisant pour un point d'entree technique;
- compatible avec un futur bot qui peut lire les champs utiles sans parsing complexe.

Normalisations recommandees:
- une ligne par champ;
- valeur jamais `null`;
- retours ligne remplaces par espaces dans `message` et `nextAction`;
- champs obligatoires toujours imprimes;
- champs optionnels imprimes seulement s'ils existent.

Format minimal recommande:

```text
decision=WAIT_HUMAN
message=Global review requires human decision
finalDecision=WAIT_HUMAN
nextAction=Edit review result and resume with runCorrectionAfterReview(...)
correctionTriggered=false
waitReason=Missing decision in review
workflowSummaryPath=D:\...\workflow-summary.md
```

Format alternatif futur possible, sans implementation maintenant:
- JSON plat.

Ne pas l'introduire dans cette etape: cela pousserait vers une projection, une serialisation et des tests de schema qui depassent le besoin actuel.

## 7. Strategie de normalisation

La normalisation doit vivre au plus pres du contrat runtime, mais sans creer de couche lourde.

Recommandation nette:
- le runner/orchestrator doivent garantir les cles stables dans `WorkflowStepResult.data`;
- le CLI doit seulement faire la normalisation de presentation (`null` -> chaine vide, ligne unique, filtrage optionnel).

Ne pas mettre toute la normalisation dans le CLI:
- sinon Telegram ou un futur endpoint devront refaire la meme logique.

Ne pas creer une brique dediee maintenant:
- pas encore assez de consommateurs;
- risque de projection externe prematuree.

Compromis sobre:
- consolider les cles dans le runner/orchestrator;
- ajouter des constantes privees ou package-private si necessaire;
- tester le contrat via les resultats du runner et la sortie CLI.

## 8. Normalisation de `message` et `nextAction`

Objectif: champs lisibles et stables, sans parsing complexe.

Regles proposees:
- `message`: phrase courte, descriptive, sans chemin obligatoire dedans.
- `nextAction`: phrase imperative ou action courte, orientee exploitation.
- pas de multi-ligne dans la sortie CLI.
- pas de codes caches dans le texte.
- les decisions structurantes doivent rester dans `decision` / `finalDecision`, pas seulement dans le message.

Exemples:
- `WAIT_HUMAN`: `Edit review result and resume with runCorrectionAfterReview(...)`
- `STOP_FAILURE`: `Inspect failure and related artifacts, then rerun`
- `CONTINUE`: `Continue workflow execution`

## 9. Preparation Telegram

Un bot Telegram aura besoin de:
- `decision` ou `finalDecision` pour choisir le type de notification;
- `message` pour le titre court;
- `nextAction` pour dire quoi faire;
- `waitReason` si present pour expliquer un blocage humain;
- `workflowSummaryPath` pour acceder au recapitulatif;
- eventuellement les chemins d'artefacts si le bot doit envoyer ou pointer vers des fichiers.

Le contrat actuel peut suffire pour une premiere integration Telegram si:
- `correctionTriggered` est expose en CLI ou accessible dans les donnees runtime;
- `waitReason` est expose quand present;
- `workflow-summary.md` reste lisible et non obligatoire;
- aucune decision machine ne depend du parsing du summary.

Conclusion Telegram: suffisant pour preparer, pas encore assez formalise pour une integration robuste multi-cas tant que les regles de presence ne sont pas testees.

## 10. Risques de sur-conception

Risques a eviter:
- creer un DTO externe dedie alors que `WorkflowStepResult` suffit;
- creer un mapper de projection sans deuxieme consommateur;
- introduire JSON/schema/versioning trop tot;
- dupliquer `finalDecision` / `nextAction` dans plusieurs objets;
- faire du summary une source de verite parseable;
- creer un systeme multi-consommateurs ou une API REST complete.

Le bon geste est plus petit: rendre explicites les cles existantes et aligner le CLI dessus.

## 11. Plan d'implementation minimal

1. Documenter dans le rapport d'implementation la liste des cles stables et leurs regles.
2. Verifier dans `AnalysisReviewWorkflowRunner` que les resultats consolides contiennent toujours:
   - `finalDecision`
   - `nextAction`
   - `correctionTriggered`
3. Garder `waitReason` optionnel, seulement quand applicable.
4. Garder `workflowSummaryPath` optionnel, seulement si le summary est produit.
5. Adapter le CLI pour exposer aussi:
   - `correctionTriggered`
   - `waitReason` si present
6. Normaliser la sortie CLI en ligne unique par champ.
7. Ajouter des tests cibles:
   - sortie CLI avec `correctionTriggered`;
   - sortie CLI avec `waitReason`;
   - sortie CLI sans `workflowSummaryPath`;
   - fallback `finalDecision` / `nextAction` si necessaire.
8. Ne pas modifier `WorkflowStepResult`.
9. Ne pas ajouter de DTO metier ou projection JSON.

## 12. Ce qui doit rester volontairement absent

- Pas de creation d'un DTO metier.
- Pas de refonte de `WorkflowStepResult`.
- Pas d'API REST complete.
- Pas de modele JSON complexe.
- Pas de serialisation avancee.
- Pas de versioning du contrat.
- Pas de systeme multi-consommateurs.
- Pas de bus d'evenements.
- Pas de persistence avancee.
- Pas de parsing machine de `workflow-summary.md`.

## Conclusion

Pour cette etape, il vaut mieux stabiliser strictement les cles existantes plutot qu'introduire une projection externe.

Le contrat V1 doit rester fonde sur `WorkflowStepResult` et enrichi par `workflow-summary.md` pour l'humain. La correction utile est d'expliciter les regles, de combler les petits ecarts CLI/runtime (`correctionTriggered`, `waitReason`) et de normaliser la sortie texte. C'est suffisant pour preparer Telegram sans complexifier le runtime.
