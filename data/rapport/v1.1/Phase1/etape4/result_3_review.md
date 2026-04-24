# Relecture architecture - lot modèle runtime workflow minimal

## Périmètre relu
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/*`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/model/*`

## Vérifications demandées
- Séparation configuration/runtime: OK. Aucun ajout côté configuration Spring/Properties/JPA, uniquement des objets runtime.
- Qualité du modèle: globalement bonne pour un socle minimal (records, validations sobres, defaults explicites, collections défensives).
- Découplage orchestrator/mémoire/tooling/policy/workspace: OK. Aucun couplage ajouté.
- Logique ad hoc/spécifique: faible et maîtrisée; pas de logique métier workflow avancée.
- Couplage bloquant pour futures phases: non détecté sur ce périmètre.
- Cohérence des noms: cohérente (`WorkflowRun`, `LotRun`, `WorkflowExecutionContext`, `*Status`).
- Lisibilité: bonne, code court et compréhensible.
- Tests: utiles et ciblés (immutabilité, validations de base, comportement de construction).

## Points discutables identifiés
1. Risque d'état incohérent sur `currentLotId` si non présent dans `lotRuns`.
2. Risque de chronologie incohérente sur les timestamps (`updatedAt` avant `createdAt`, etc.).
3. Dette mineure: sans garde-fou temporel, la suite aurait dû gérer des états invalides plus tard dans l'orchestrateur.

## Corrections appliquées
1. Ajout d'une validation de cohérence `currentLotId` -> doit référencer un lot existant de `lotRuns`.
2. Ajout de garde-fous temporels minimaux:
   - `updatedAt` ne peut pas être antérieur à `createdAt`
   - `startedAt` ne peut pas être antérieur à `createdAt`
   - `completedAt` ne peut pas être antérieur à `startedAt` (quand `startedAt` est présent)
3. Ajout de tests unitaires ciblés couvrant ces cas.

## Ce qui n'a volontairement pas été fait
- Aucun moteur de transitions.
- Aucun service runtime/orchestrateur.
- Aucune persistance.
- Aucune logique de navigation avancée entre lots.
- Aucun couplage avec Telegram/Codex/Maven.

## Résultat de validation
Commande exécutée:
- `-Dtest=LotRunTest,WorkflowRunTest,WorkflowExecutionContextTest test`

Résultat:
- BUILD SUCCESS
- 9 tests exécutés, 0 échec

## Résumé final
Le lot reste strictement un socle runtime minimal et propre. Les corrections appliquées renforcent la robustesse structurelle (cohérence d'identifiants et de chronologie) sans élargir le périmètre fonctionnel ni introduire de framework prématuré.
