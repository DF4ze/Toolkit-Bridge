# Phase 8 — CodexCall — Étape 4 — Relecture (architecture + qualité)

## Contexte

Étape 4 : injection de `codexWorkingDirectory` (type `Path`) dans le `WorkflowExecutionContext` pour RUN et RESUME, sans modifier `CodexWorkflowClient`.

Périmètre de la relecture : vérifier séparation config/runtime, découplage, absence de logique ad hoc, cohérence des noms, lisibilité, utilité des tests, dette potentielle.

---

## Points vérifiés

- Séparation configuration vs runtime : OK (injection via `variables` runtime, pas de nouvelle config).
- Découplage orchestration vs tooling : OK (le contexte porte une variable, les steps Codex consomment déjà la clé).
- Pas de lookup DB dans `buildContext(...)` / `buildResumeContext(...)` : OK (conforme contrainte).
- Cohérence des noms : OK (`codexWorkingDirectory` existe déjà côté steps/CLI).
- Non-fuite UX : OK (aucune exposition du path dans Telegram).
- Tests : OK (capture du `WorkflowExecutionContext` et vérification de la variable).

---

## Faiblesses / points discutables

1) **Garde défensive au niveau `executeRun/executeResume`**
- Le failRun `"Project path is missing for workflow execution"` est safe (pas de fuite), mais c’est une erreur “runtime” qui ne devrait jamais arriver si la résolution amont est correcte.
- Risque mineur : si un bug futur invalide le chemin, on obtient un échec tardif (async) plutôt qu’un refus immédiat.

2) **Orchestration service déjà très “gros”**
- L’étape 4 elle-même reste minimale, mais elle s’insère dans une classe qui concentre : parsing/validation, résolution, orchestration async, construction contexte, mapping résultats.
- Ce n’est pas une régression introduite par l’étape 4, mais l’accumulation rend les évolutions futures plus risquées.

3) **Qualité des rapports écrits (encoding)**
- Les fichiers de rapport générés dans `data/rapport/...` apparaissent en mojibake (ex: `Ã‰tape`, `ImplÃ©mentation`).
- Ce point n’impacte pas le runtime, mais nuit à la lisibilité et à la maintenabilité du workflow documentation/review.

---

## Corrections utiles (sans nouvelle fonctionnalité)

1) **Refuser plus tôt si `projectPath` est absent (optionnel)**
- Au lieu de laisser `executeRun/executeResume` gérer le `null`, garantir (par assertion/garde) que la résolution amont ne peut pas lancer l’async sans `projectPath`.
- Cela reste une correction “qualité” (pas de changement fonctionnel attendu si l’invariant tient).

2) **Réduire le risque de tests trop couplés**
- Les tests actuels vérifient le type `Path` + présence de `codexWorkingDirectory`, ce qui est bon.
- Garder les assertions sur les invariants (présence/absence de `analysisSourcePath` selon RUN/RESUME) plutôt que sur la totalité des variables.

3) **Corriger l’encodage des rapports (à traiter rapidement)**
- S’assurer que les fichiers `result_4_implements.md` et autres sont écrits en UTF-8 (sans BOM) par l’outil/process utilisé.
- Cette correction est hors “runtime”, mais directement dans la chaîne qualité.

---

## Dette technique introduite

- Dette faible : la garde `projectPath == null` dans l’exécution async masque une hypothèse (invariant) au lieu de l’exprimer clairement (précondition).
- Dette documentaire : mojibake dans les rapports.

---

## Résumé final

Étape 4 est **correcte, simple et bien cadrée** : l’injection `codexWorkingDirectory` est faite au bon niveau (variables runtime), sans DB lookup tardif, sans changement du client Codex, et avec des tests utiles.

Les seuls points à améliorer sont surtout “qualité de chaîne” (encoding rapports) et maintien d’invariants (refus le plus tôt possible si `projectPath` était absent).

