# Revue critique d'architecture - Phase 5 Etape 3

## Perimetre relu

Implementation relue:
- `WorkflowCliConsumerSimulator`
- `WorkflowCliConsumerSimulatorTest`
- `result.3.implements.md`

Objectif du lot:
- verifier qu'un consommateur externe simule peut exploiter le contrat CLI;
- parser `key=value`;
- interpreter `finalDecision` puis `decision`;
- produire un statut simple;
- ne pas ajouter de bot Telegram, JSON, DTO metier, framework ou refactor runtime.

## Verdict global

L'implementation est conforme au perimetre. Le simulateur prouve que le contrat CLI stabilise peut etre consomme sans lancer de process reel et sans dependance externe.

Architecturalement, le choix est sobre: le simulateur est decouple du runner, de l'orchestrator, de la memoire, du tooling, des policies et du workspace. Il consomme uniquement une sortie texte, ce qui valide bien la promesse d'un contrat externe.

Le principal point de vigilance est le petit modele local introduit (`SimulatedConsumerStatus`, `SimulatedConsumerResult`). Il est acceptable comme modele de simulation, mais il ne doit pas devenir un contrat applicatif global sans decision explicite.

## 1. Faiblesses ou points discutables

### 1.1 Petit modele local a ne pas promouvoir

Le simulateur introduit:
- `SimulatedConsumerStatus`
- `SimulatedConsumerResult`

Ce n'est pas un DTO metier du runtime, et cela reste local a la simulation. Le choix est raisonnable pour tester l'interpretation sans retourner une map brute partout.

Point de vigilance: si une future integration Telegram reutilise directement ces types, ils deviendront de fait une couche de contrat externe. Ce serait premature sans cadrage.

Correction utile proposee:
- conserver ces types comme details locaux du simulateur;
- documenter qu'ils ne sont pas un contrat public;
- ne pas les reutiliser dans le runtime ou un futur bot sans nouvelle analyse.

### 1.2 Filtrage strict des cles connues

Le parser ignore les cles inconnues.

Point positif:
- simule un consommateur strict;
- evite que des donnees non stabilisees deviennent implicitement exploitees.

Point discutable:
- un consommateur de diagnostic pourrait vouloir conserver les cles inconnues pour affichage ou debug.

Correction utile proposee:
- ne rien changer maintenant;
- si un consommateur diagnostic apparait, ajouter un mode ou champ `unknownFields`, mais pas dans ce lot.

### 1.3 Exit code non-zero avec parsing partiel

En cas d'exit code non-zero, le simulateur retourne `ERROR` mais parse quand meme les champs connus.

Point positif:
- si la sortie contient `message`, l'erreur reste exploitable.

Point discutable:
- les erreurs CLI sont souvent sur stderr, alors que le simulateur ne recoit qu'une sortie CLI brute.

Correction utile proposee:
- ne pas ajouter stderr maintenant;
- si un vrai appel process est ajoute plus tard, representer stdout/stderr/exitCode explicitement dans le consommateur de process, sans changer le parser.

### 1.4 `SUCCESS` pour `CONTINUE`

Le mapping `CONTINUE -> SUCCESS` est simple et conforme au prompt.

Point sensible:
- dans un workflow plus long, `CONTINUE` ne signifie pas toujours "termine", mais plutot "etat non bloquant".

Correction utile proposee:
- conserver `SUCCESS` pour cette simulation;
- documenter que c'est un statut du consommateur, pas un statut universel du runtime;
- eviter de reutiliser ce statut comme statut metier global.

### 1.5 Pas de test d'integration avec la sortie reelle du CLI

Les tests simulent des sorties CLI et ne lancent aucun process reel, ce qui est conforme au prompt.

Point discutable:
- cela valide le contrat texte, mais pas l'invocation OS du CLI.

Correction utile proposee:
- ne pas ajouter de process reel maintenant;
- une future etape peut ajouter un test d'appel systeme si le besoin devient concret.

## 2. Verification des axes demandes

### Separation configuration / runtime

Respectee. Le simulateur ne configure rien et ne construit aucun runner. Il consomme une sortie CLI deja produite.

### Qualite du modele implemente

Bonne pour le perimetre:
- enum de statuts tres simple;
- record de resultat local;
- pas de modele metier global.

Dette potentielle: ces types locaux ne doivent pas sortir de leur role de simulation.

### Decouplage orchestrator / memoire / tooling / policy / workspace

Respecte. Le simulateur n'importe aucune classe de ces sous-systemes. Il depend seulement de `java.util` et du contrat texte.

### Absence de logique ad hoc

Globalement respectee. Le mapping des decisions est explicite et limite aux decisions stabilisees:
- `WAIT_HUMAN`
- `STOP_FAILURE`
- `CONTINUE`

Le statut `UNKNOWN` protege les futures valeurs inattendues.

### Absence de couplage genant pour les futures phases

Pas de couplage bloquant. Le simulateur peut etre jete, garde comme outil de test, ou inspire un futur consommateur sans forcer l'architecture.

Point a surveiller: ne pas transformer cette classe en couche d'integration universelle.

### Coherence des noms

Les noms sont clairs:
- `WorkflowCliConsumerSimulator`
- `SimulatedConsumerStatus`
- `SimulatedConsumerResult`

Point mineur: `SUCCESS` peut etre legerement optimiste pour `CONTINUE`, mais le prompt demandait ce mapping. Rien a corriger maintenant.

### Lisibilite generale

Bonne. La classe est courte, les responsabilites sont visibles, les tests sont comprehensibles.

### Tests réellement utiles

Oui. Les tests couvrent:
- parsing nominal;
- lignes invalides;
- cles inconnues;
- champs manquants;
- `WAIT_HUMAN`;
- `STOP_FAILURE`;
- `CONTINUE`;
- decision inconnue;
- sortie invalide;
- exit code non-zero.

Ils valident exactement le contrat externe sans dependance au vrai CLI.

### Dette technique introduite

Dette faible:
- petit modele local de simulation;
- cles string locales;
- absence de representation stderr;
- pas de test process reel.

Ces dettes sont acceptables pour une etape demonstrative.

## 3. Corrections utiles proposees

Corrections utiles sans nouvelle fonctionnalite:

1. Ajouter un commentaire court ou une note documentaire indiquant que `SimulatedConsumerResult` et `SimulatedConsumerStatus` ne sont pas un contrat runtime public.

2. Conserver le filtrage des cles connues, mais mentionner que les cles inconnues sont ignorees volontairement pour eviter une dependance a des donnees non stabilisees.

3. Si une future etape ajoute un vrai appel process, separer clairement:
   - execution du process CLI;
   - parsing stdout;
   - interpretation du contrat.

4. Ne pas extraire de couche commune maintenant. Le simulateur est encore trop petit pour justifier une abstraction.

## 4. Ce qui doit rester volontairement absent

- Pas de bot Telegram.
- Pas de framework.
- Pas de JSON.
- Pas de DTO metier.
- Pas de couche applicative.
- Pas de process reel dans les tests.
- Pas de refactor runtime.
- Pas de parsing de `nextAction`.
- Pas de parsing de `workflow-summary.md`.
- Pas de persistence d'etat.

## 5. Resume final

Le lot 3 remplit bien son role: il demontre que le contrat CLI est exploitable par un consommateur externe minimal. Le simulateur est decouple, testable, tolerant aux sorties imparfaites, et ne complexifie pas le runtime.

Aucune correction immediate n'est necessaire. Le seul point important pour les phases suivantes est de ne pas confondre ce modele local de simulation avec un futur contrat applicatif global. Si Telegram arrive ensuite, il faudra refaire un cadrage minimal plutot que reutiliser automatiquement le simulateur comme architecture d'integration.
