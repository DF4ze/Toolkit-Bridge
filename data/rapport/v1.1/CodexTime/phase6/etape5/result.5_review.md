# Revue critique d’architecture — Phase 6 Étape 5

## Verdict
L’implémentation remplit bien l’objectif fonctionnel: le `workflow-summary.md` est désormais lisible, structuré, et compatible avec les usages externes déjà en place.

Le niveau de risque reste faible, mais trois points méritent encore d’être corrigés ou, au minimum, cadrés maintenant pour éviter une dette inutile dans les phases suivantes.

## Faiblesses ou points discutables

### 1. `Status` du summary dépend encore de `data.finalDecision`
Dans `AnalysisReviewWorkflowRunner.buildSummaryContent(...)`, la ligne de statut prend `finalDecision` depuis `result.data()` avec fallback sur `decision().name()` ([AnalysisReviewWorkflowRunner.java:341-344](../../../../src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java#L341-L344)).

Point discutable:
- le summary peut donc refléter une valeur de data, pas forcément la décision runtime réelle;
- si un futur changement introduit une divergence entre `decision()` et `finalDecision`, le document humain deviendra trompeur.

Correction utile:
- privilégier `decision().name()` comme source principale pour `Status`;
- garder `finalDecision` comme fallback seulement si nécessaire.

### 2. La sélection des artefacts reste heuristique et stringly-typed
La méthode `summaryArtifacts(...)` mélange des clés littérales (`reviewResultPath`, `resultArtifactPath`) avec des constantes internes et une détection par heuristique build (`isBuildWaitHuman(...)`) ([AnalysisReviewWorkflowRunner.java:410-465](../../../../src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunner.java#L410-L465)).

Point discutable:
- le rendu est correct aujourd’hui, mais la logique dépend encore de conventions implicites sur les clés de `data`;
- si un futur step réutilise un même nom de raison ou un même artefact, la classification peut dériver sans alerte.

Correction utile:
- centraliser les clés de résultat dans des constantes privées;
- garder une règle unique et explicite pour distinguer `WAIT_HUMAN` build et review;
- éviter de multiplier les chemins de fallback non nommés.

### 3. Les tests valident bien la structure, mais restent très sensibles au texte exact
Les tests du summary vérifient beaucoup de libellés textuels exacts, notamment dans `AnalysisReviewWorkflowRunnerResumeTest` et `AnalysisReviewWorkflowRunnerValidationRetryTest` ([AnalysisReviewWorkflowRunnerResumeTest.java:50-107](../../../../src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerResumeTest.java#L50-L107), [AnalysisReviewWorkflowRunnerValidationRetryTest.java:214-233](../../../../src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/AnalysisReviewWorkflowRunnerValidationRetryTest.java#L214-L233)).

Point discutable:
- les tests sont utiles pour figer le contrat;
- mais ils sont aussi très couplés au wording, donc une amélioration éditoriale future peut casser les tests sans changer le comportement.

Correction utile:
- conserver quelques assertions précises sur les en-têtes et chemins;
- alléger les assertions sur le wording secondaire quand il ne porte pas le contrat externe.

## Points solides

- le summary est maintenant autonome et lisible sans accès au code;
- la distinction `SUCCESS` / `STOP_FAILURE` / `WAIT_HUMAN review` / `WAIT_HUMAN build` est claire;
- la compatibilité du contrat existant est conservée;
- la couverture de tests est cohérente avec le périmètre de l’étape.

## Risques de refactor futur encore évitables maintenant

- éviter de laisser plusieurs variantes de la même information circuler entre runner, CLI et summary;
- éviter d’ajouter de nouvelles clés de `data` sans les référencer dans une seule convention centrale;
- éviter d’étendre encore le bloc de rendu du summary sans isoler au moins les textes constants par statut.

## Résumé final
L’étape 5 est globalement saine et aboutit au résultat attendu. Les corrections encore utiles sont légères: rendre `Status` moins dépendant de `data`, durcir la sélection des artefacts avec des constantes explicites, et desserrer un peu les tests sur le wording pour préserver la capacité de retouche éditoriale.
