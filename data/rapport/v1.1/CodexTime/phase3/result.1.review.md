# Revue critique d’architecture — Phase 3 / Étape 1 (`GlobalReviewStep`)

## Portée relue

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`
- cohérence avec `GlobalAnalysisStep`.

## Verdict global

Implémentation globalement propre, minimale et conforme au scope demandé.
La séparation runtime/configuration est respectée, et la step reste bien bornée (pas d’orchestration cachée).

## 1. Faiblesses / points discutables

### [Moyen] Couverture de test incomplète sur la branche `codexResult.success() == false`
- Référence : `GlobalReviewStep.java` lignes 100-112.
- Constat : la logique gère correctement le cas (retour `STOP_FAILURE`), mais ce scénario n’est pas testé explicitement dans `GlobalReviewStepTest`.
- Risque : régression silencieuse sur le comportement attendu en cas d’échec Codex non-exceptionnel (exit code != 0 / timeout géré par résultat).

### [Faible] Duplication de logique de parsing/validation de variables entre steps
- Références :
  - `GlobalReviewStep.java` lignes 156-227
  - `GlobalAnalysisStep.java` lignes 130-201
- Constat : duplication assumée pour rester minimal, mais dette technique potentielle si d’autres steps arrivent en phase suivante.
- Risque : divergence future (messages d’erreur, conversion `Path`/`int`, validation non homogène).

### [Faible] Couplage implicite aux clés de contexte sous forme de chaînes
- Référence : `GlobalReviewStep.java` lignes 19-24.
- Constat : les clés sont bien centralisées en constantes locales, mais restent non typées (`Map<String,Object>`).
- Risque : erreurs de saisie côté appelant, découvertes tardivement à l’exécution.

## 2. Corrections utiles (sans élargir le périmètre)

1. Ajouter un test dédié : "Codex renvoie `success=false` => `STOP_FAILURE` et artefacts review écrits".
2. Ajouter un test dédié : "Codex timeout (`timedOut=true`) => message explicite `Codex execution timed out`".
3. Garder la duplication actuelle pour cette étape, mais noter dans la roadmap phase suivante une micro-factorisation locale seulement si une 3e step reproduit le même bloc.
4. Conserver les constantes de clés (déjà fait) et documenter explicitement dans le runner la liste des variables obligatoires pour réduire le risque de mauvais wiring.

## 3. Vérifications demandées

### Séparation configuration vs runtime
- OK : la step lit uniquement `WorkflowExecutionContext` runtime et n’embarque pas de configuration Spring/ad hoc.

### Qualité du modèle implémenté
- OK pour V1 : `WorkflowStep` + `WorkflowStepResult` sont respectés, comportement lisible et déterministe.

### Découplage orchestrator / mémoire / tooling / policy / workspace
- OK : aucun couplage direct introduit avec mémoire, policy, tooling global, workspace service ou orchestrator.

### Absence de logique ad hoc / trop spécifique
- OK : logique strictement orientée artefacts analysis -> review, sans moteur générique caché.

### Absence de couplage bloquant les futures phases
- Globalement OK : le principal couplage est aux clés de contexte runtime, déjà présent dans la phase actuelle.

### Cohérence des noms
- OK : nomenclature alignée (`GlobalReviewStep`, `REVIEW_PROMPT`, `REVIEW_RESULT`, préfixe d’erreur).

### Lisibilité générale
- OK : code clair, structure proche de `GlobalAnalysisStep`, faible surprise cognitive.

### Tests réellement utiles
- Majoritairement OK : les cas demandés sont couverts.
- Amélioration recommandée : couvrir les deux branches d’échec par `CodexExecutionResult` (non-success/timeout).

### Dette technique introduite
- Dette légère et maîtrisée : duplication de helpers et contexte non typé.

### Risques de refactor futur évitables maintenant
- Oui, partiellement évitables via 2 tests complémentaires sans modifier l’architecture.

## 4. Résumé final propre

Le lot est architecturalement sain pour une étape 1 de phase 3 : simple, borné, cohérent avec l’existant, sans sur-conception.
Le principal axe d’amélioration immédiat est la couverture de tests sur les branches d’échec Codex retournées par résultat (et non par exception). Le reste peut rester inchangé à ce stade pour préserver la progression incrémentale.
