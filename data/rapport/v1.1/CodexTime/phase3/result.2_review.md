# Revue critique d’architecture — Lot 2 (Correction conditionnelle)

## Périmètre relu
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestrator.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/orchestrator/WorkflowOrchestratorTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

## Évaluation globale
Implémentation globalement saine, lisible et sobre. Le lot respecte bien le périmètre (pas de nouvelle couche, pas de moteur, pas de modification de `CorrectionStep` ou `WorkflowExecutionContext`).

## 1) Faiblesses / points discutables

### [P1] Contrat review -> orchestrator trop permissif (risque de faux négatif)
- Localisation : `WorkflowOrchestrator.requiresCorrection(...)` lignes 100-109.
- Observation : si `requiresCorrection` est absent, mal typé, ou incohérent, l’orchestrator retombe silencieusement sur `false`.
- Risque : une rupture de contrat côté review peut désactiver la correction sans signal fort.

### [P2] Résolution des directives review ambiguë si plusieurs marqueurs existent
- Localisation : `GlobalReviewStep.parseReviewDirective(...)` lignes 178-197.
- Observation : le premier marqueur rencontré gagne. Si la sortie contient plusieurs directives (`NEED_CORRECTION` + `WAIT_HUMAN`), le résultat dépend de l’ordre des lignes.
- Risque : comportement non déterministe si le prompt produit des sorties bruitées.

### [P3] Couplage par string literal partagé sans contrat explicite
- Localisation : marqueurs `DECISION: ...` dans `GlobalReviewStep` + clé `requiresCorrection` consommée par `WorkflowOrchestrator`.
- Observation : la convention est claire mais “stringly typed”.
- Risque : dette de maintenance si d’autres steps réutilisent la même convention sans centralisation minimale.

## 2) Corrections utiles (sans nouvelle fonctionnalité)

1. Rendre le contrat `requiresCorrection` strict côté orchestrator (fail-fast local)
- Si la review est `CONTINUE` mais que la clé `requiresCorrection` est absente/invalide :
  - soit retourner `STOP_FAILURE` explicite,
  - soit lever une `IllegalStateException` (cohérent avec la validation déjà stricte des décisions).
- Bénéfice : empêche un “skip correction” silencieux.

2. Définir une priorité explicite des directives review
- Règle recommandée : `WAIT_HUMAN` > `NEED_CORRECTION` > `OK` > `NONE`.
- Implémentation sobre : scanner toutes les lignes, mémoriser les drapeaux, appliquer la priorité une fois.
- Bénéfice : résultat déterministe même en sortie mixte.

3. Extraire les clés/markers dans constantes partagées minimales
- Sans nouvelle classe lourde, au minimum : constantes de clé data et messages dans l’orchestrator + step.
- Bénéfice : limite les régressions de renommage et améliore la cohérence future.

## 3) Vérification de vos critères

- Séparation configuration/runtime : OK (aucun ajout de config, logique runtime uniquement).
- Qualité du modèle implémenté : Bonne, mais contrat `requiresCorrection` à durcir.
- Découplage orchestrator/mémoire/tooling/policy/workspace : OK, pas de nouveau couplage transversal.
- Absence de logique ad hoc : Globalement OK ; seul le fallback silencieux est discutable.
- Couplage gênant phases futures : Faible, à condition de verrouiller le contrat du flag.
- Cohérence des noms : Bonne (`executeAnalysisReviewWithOptionalCorrection` est verbeux mais explicite).
- Lisibilité générale : Bonne.
- Tests utiles : Oui, couvrent les cas demandés ; manque juste un test “contrat invalide/absent” pour l’orchestrator.
- Dette technique introduite : Modérée et localisée (string contracts + fallback permissif).
- Risques de refactor futur évitables maintenant : Oui, surtout via contrat strict + priorité directives.

## 4) Résumé final
Le lot est valide et exploitable en l’état. La principale faiblesse architecturale est un contrat inter-step trop permissif (`requiresCorrection`), pouvant masquer une erreur de review. Une correction légère et immédiate (validation stricte du flag + priorité déterministe des directives) renforcerait nettement la robustesse sans élargir le périmètre.
