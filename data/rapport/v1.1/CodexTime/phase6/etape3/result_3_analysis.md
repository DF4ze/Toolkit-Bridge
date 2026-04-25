# Rapport d'analyse — Phase 6 Étape 3 — Gestion intelligente des erreurs de build

## Résumé exécutif

Oui, on peut relancer une correction après erreur Maven. Le pattern existe déjà : `RETRY_CORRECTION`
est défini dans `WorkflowStepDecision` mais jamais utilisé. Il faut :

1. Créer un step dédié : `BuildErrorCorrectionStep`
2. Gérer `RETRY_CORRECTION` dans l'orchestrateur
3. Passer l'erreur Maven à Codex via un artifact dédié
4. Implémenter un compteur de retry dans le contexte

**Conclusion : prêt, avec retours mineurs.**

---

## 1. Peut-on relancer une correction après un échec Maven ?

### Réponse : OUI — dans les cas bien délimités

**Cas oui :**
- Erreur de compilation simple (symbole non trouvé, typo, import manquant)
- Erreur d'import ou dépendance
- Erreur de syntaxe

**Cas non :**
- Timeout Maven (problème d'environnement)
- Missing Maven wrapper (problème d'infrastructure)
- Erreur ambiguë ou non-reproductible

**Pattern :** `STOP_FAILURE` → humain intervient, ou `RETRY_CORRECTION` → boucle d'auto-correction

---

## 2. Où intégrer la logique ?

### Réponse : Step dédié + orchestrateur

**Architecture retenue :**

```
MavenValidationStep
  SUCCESS → CONTINUE
  FAILURE → RETRY_CORRECTION (si retry count < seuil)
  FAILURE → STOP_FAILURE (si retry count >= seuil)
  TIMEOUT → STOP_FAILURE (pas de retry)
  SYSTEM_ERROR → STOP_FAILURE (pas de retry)
```

Ensuite, l'orchestrateur capture `RETRY_CORRECTION` et relance `BuildErrorCorrectionStep`,
qui appelle Codex avec le contexte d'erreur Maven.

**Avantages :**
- Séparation claire : validation vs. correction d'erreurs
- Chaque step a une responsabilité unique
- Réutilisable pour autres types d'erreurs futures

**Inconvénient :**
- Ajout d'un step (mais nécessaire)

---

## 3. Quelle donnée fournir à Codex ?

### Réponse : extrait structuré de l'erreur Maven

**Contenu du prompt :**

```
Compilation error context:

[Build Error Summary]
Status: FAILURE
Exit Code: 1
Duration: 3450 ms

[Maven Output (stderr + relevant lines from stdout)]
[First 5000 chars — focus on first few error lines]

[File Context]
Last modified files from the correction:
- src/main/java/Foo.java
- src/main/java/Bar.java

[Request]
Fix the compilation errors above. Provide corrected code.
```

**Règles :**
- Ne pas passer 100% du stdout (trop long, bruit)
- Extraire les premières **100 lignes stderr** + 50 lignes stdout avant la première erreur
- Structurer l'erreur (error type, file, line quand possible)
- Passer le BUILD_RESULT artifact en fullPath (Codex peut le relire)

**Data à passer dans le contexte :**
```
buildErrorSummary : "src/main/java/Foo.java:42: error: cannot find symbol"
buildErrorLines : [...] (première occurrence de l'erreur, environ 10 lignes)
buildArtifactPath : path vers le BUILD_RESULT complet
```

---

## 4. Comment éviter les boucles infinies ?

### Stratégie : Compteur + seuil + détection de pattern

**Compteur dans le contexte :**
```java
"buildErrorRetryCount" : 0  // initialisé à 0, incrémenté après chaque tentative
```

**Seuil :**
- Max 2 retries (donc 3 attempts au total : initial + 2 retry)
- Configurable via `buildErrorRetryMax` (optionnel, défaut 2)

**Détection de pattern (prévention) :**
- Si le même **type** d'erreur réapparaît (même ligne, même symbole), on arrête après 1 retry
- Hasher la première ligne d'erreur, comparer avec la tentative précédente
- Si identical → STOP_FAILURE

**Arrêt conditionnel :**
- Si exit code = 0 → SUCCESS (sortie saine)
- Si timeout → STOP_FAILURE immédiat
- Si SYSTEM_ERROR → STOP_FAILURE immédiat
- Si FAILURE + retry_count >= max → STOP_FAILURE

---

## 5. Mapping décision

| Cas | Valeurs | Décision | Raison |
|---|---|---|---|
| Build SUCCESS | exit=0 | `CONTINUE` | Build valide |
| Build FAILURE, retry<max, pas pattern | status=FAILURE, count<2 | `RETRY_CORRECTION` | Peut être fixé auto |
| Build FAILURE, retry>=max | status=FAILURE, count>=2 | `STOP_FAILURE` | Épuisé les tentatives |
| Build FAILURE, pattern détecté | status=FAILURE + hash match | `STOP_FAILURE` | Même erreur, pas d'amélioration |
| Build TIMEOUT | status=TIMEOUT | `STOP_FAILURE` | Infrastructure, pas auto-corrigible |
| Build SYSTEM_ERROR | status=SYSTEM_ERROR | `STOP_FAILURE` | Infrastructure, pas auto-corrigible |

---

## 6. Impacts architecturaux

### `WorkflowStepDecision`
**Impact : AUCUN.** `RETRY_CORRECTION` existe déjà, jamais utilisé.

### `WorkflowOrchestrator`
**Impact : MODÉRÉ — ajouter gestion de RETRY_CORRECTION.**

```
executeSingleStep doit reconnaître RETRY_CORRECTION au lieu de lever ISE.
Exemple : executeSingleStep(context, step) → reconnaît RETRY_CORRECTION, retourne le résultat.

La logique de boucle est en amont (dans le runner ou un nouvel orchestrateur wrapper).
```

### `AnalysisReviewWorkflowRunner`
**Impact : MODÉRÉ — ajouter une méthode de retry.**

```
runWithValidationAndRetry(context) {
    result = runWithValidation(context);
    if (result.decision() == RETRY_CORRECTION) {
        // relancer buildErrorCorrectionStep
        // réexécuter validationStep
        // répéter jusqu'à CONTINUE ou STOP_FAILURE
    }
}
```

Alternativement, la boucle peut vivre dans `WorkflowOrchestrator` via une nouvelle méthode
`executeWithRetry(...)`.

### Existing steps
**Impact : AUCUN.** `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep` non modifiés.

---

## 7. Risques

### Risque : Boucle infinie
**Atténuation :** Compteur + seuil (max 2 retries). Détection de pattern (même erreur = stop).

### Risque : Correction faulty (pire que l'original)
**Atténuation :** Chaque correction est exécutée + validée. Si elle Empire, elle sera détectée
à la prochaine itération et arrêtée.

### Risque : Bruit dans les prompts Codex
**Atténuation :** Passer seulement l'extrait d'erreur (100 lignes max), pas 50000 chars.
Structurer le context.

### Risque : Lenteur
**Atténuation :** Max 3 tentatives de build = 3 × ~30-60s = ~2-3 min au max.
Acceptable pour une correction intelligente.

### Risque : Compteur perdu
**Atténuation :** Stocké dans `context.variables()`. Persistent dans le contexte de workflow.

---

## 8. Plan d'implémentation

| # | Action | Fichiers | Effort |
|---|---|---|---|
| 1 | Modifier `MavenValidationStep.execute()` → retourner RETRY_CORRECTION au lieu de STOP_FAILURE si retry<max | `step/MavenValidationStep.java` | Faible |
| 2 | Créer `BuildErrorCorrectionStep` — step qui appelle Codex avec erreur Maven en contexte | `step/BuildErrorCorrectionStep.java` | Moyen |
| 3 | Modifier `WorkflowOrchestrator` → traiter RETRY_CORRECTION au lieu de lever ISE | `orchestrator/WorkflowOrchestrator.java` | Faible |
| 4 | Modifier `WorkflowOrchestrator` → ajouter `executeWithRetry(...)` method pour boucler | `orchestrator/WorkflowOrchestrator.java` | Moyen |
| 5 | Modifier `AnalysisReviewWorkflowRunner` → ajouter `runWithValidationAndRetry(context)` | `AnalysisReviewWorkflowRunner.java` | Faible |
| 6 | Ajouter `BuildErrorCorrectionStepTest` | `step/BuildErrorCorrectionStepTest.java` | Moyen |
| 7 | Ajouter tests pour RETRY_CORRECTION dans orchestrateur | `orchestrator/WorkflowOrchestratorTest.java` | Moyen |
| 8 | Mettre à jour factory et tests runner | `cli/AnalysisReviewWorkflowRunnerFactory.java` + tests | Faible |

**Total : ~8 étapes, estimation 2-3 jours pour implémentation complète.**

---

## Alternative considérée : boucle dans le runner

Au lieu d'ajouter une méthode d'orchestrateur, on pourrait mettre la boucle directement dans
`AnalysisReviewWorkflowRunner.runWithValidationAndRetry()`. Avantage : plus simple. Inconvénient :
moins réutilisable si d'autres workflows utilisent l'orchestrateur.

**Recommandation : mettre la boucle dans l'orchestrateur** pour futur-proofing, même si c'est
un peu plus complexe maintenant.

---

## Données de contexte requises (nouvelles)

| Variable | Type | Défaut | Obligatoire |
|---|---|---|---|
| `buildErrorRetryMax` | int | 2 | non |
| `buildErrorRetryCount` | int | 0 | non (auto-init) |

Ces variables sont optionnelles — l'orchestrateur les initialise si absent.

---

## Conclusion

**Prêt.**

L'architecture supporte naturellement une boucle d'auto-correction. `RETRY_CORRECTION` est un
signal prévu mais jamais implémenté. Il faut 8 étapes courtes pour l'activer.

**Pas de risque de breaking change.** Les modifications sont additives :
- MavenValidationStep : condition supplémentaire (simple)
- Orchestrateur : nouvelle branche RETRY_CORRECTION + nouvelle méthode executeWithRetry
- Runner : wrapper autour de runWithValidation
- Nouveau step : BuildErrorCorrectionStep (standalone)

Aucune étape existante n'est modifiée.

---

*Phase 6 / Étape 3 — analyse terminée.*
