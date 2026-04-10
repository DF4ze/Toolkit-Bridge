# Phase 5 — Analyse du nettoyage final de TechnicalAdminFacade

## 1. État actuel de la façade
- Fichier analysé : `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`.
- Méthodes présentes (7) :
  - `listAgents()`
  - `listRecentTasks(Integer limit, String agentId, TaskStatus status)`
  - `listRecentTraces(Integer limit, String agentId)`
  - `listRecentArtifacts(Integer limit, String agentId, String taskId)`
  - `getConfigurationView()`
  - `listRetentionPolicies()`
  - `getOverview(Integer limit)`
- Méthodes de délégation pures (6) :
  - `listAgents` -> `AdminAgentQueryService`
  - `listRecentTasks` -> `AdminTaskQueryService`
  - `listRecentTraces` -> `TraceQueryService`
  - `listRecentArtifacts` -> `ArtifactQueryService`
  - `getConfigurationView` -> `AdminConfigQueryService`
  - `listRetentionPolicies` -> `RetentionQueryService`
- `getOverview()` est bien le point d’agrégation central.
- Logique résiduelle observée dans `getOverview()` :
  - normalisation de limite via `technicalProperties.sanitizeLimit(limit)`
  - agrégation des listes
  - calcul de compteurs (`busyAgents`, `recentErrors`)
  - construction du DTO `TechnicalAdminView.Overview`
- Conclusion section 1 : façade déjà majoritairement délégante, avec une logique d’assemblage locale limitée à `getOverview()`.

## 2. Helpers et logique locale restante
- Aucune méthode utilitaire locale détectée (`isBlank` ou équivalent absent).
- Aucun helper privé dans la façade.
- Mapping métier complexe : non détecté.
- Logique restante dans `getOverview()` :
  - `busyAgents` : filtre sur `agent.runtime() != null && agent.runtime().busy()`
  - `recentErrors` : filtre sur `trace.type() == AgentTraceEventType.ERROR`
- Distinction claire :
  - Acceptable (orchestration légère) :
    - sanitation de limite
    - collecte des vues via services de query
    - comptages simples pour les indicateurs d’overview
    - assemblage final du DTO d’overview
  - À supprimer :
    - rien d’évident immédiatement sans changer le contrat fonctionnel actuel de l’overview.
- Point d’attention : le compteur `recentErrors` dépend d’un enum technique (`AgentTraceEventType`), ce qui reste acceptable ici car limité à un agrégat de façade.

## 3. Dépendances et injections
- Dépendances injectées (7) :
  - `AdminAgentQueryService`
  - `AdminTaskQueryService`
  - `TraceQueryService`
  - `ArtifactQueryService`
  - `AdminConfigQueryService`
  - `RetentionQueryService`
  - `AdminTechnicalProperties`
- Utilisation réelle :
  - les 7 dépendances sont utilisées dans le corps de la classe.
- Dépendances devenues inutiles :
  - aucune détectée à ce stade.
- Dépendances métier lourdes directement manipulées :
  - aucune (la façade ne manipule que des services de query + propriété technique de limite).
- Conclusion section 3 : injection cohérente et minimale au regard des responsabilités actuelles.

## 4. Imports et dette locale
- Imports présents :
  - `TechnicalAdminView`
  - `AdminTechnicalProperties`
  - `TaskStatus`
  - `AgentTraceEventType`
  - `Service`
  - `Instant`
  - `List`
- Imports inutilisés :
  - aucun import inutilisé détecté.
- Imports obsolètes après extraction :
  - aucun import obsolète détecté.
- Restes de code mort/commenté :
  - aucun commentaire mort, `TODO`, `FIXME`, ou bloc commenté résiduel détecté.
- Cohérence globale du fichier :
  - bonne; classe courte, lisible, sans duplication notable.

## 5. Risques de nettoyage
- Suppressions possibles sans risque immédiat :
  - aucune suppression « sûre et utile » évidente (hors micro-ajustements de style).
- Éléments à conserver impérativement :
  - les 6 méthodes de délégation publiques (contrat de façade)
  - `getOverview()` en tant que point d’agrégation
  - `technicalProperties.sanitizeLimit(limit)` pour garder la protection de bornage
  - les compteurs `busyAgents` et `recentErrors` si l’overview attendu doit rester inchangé
- Risques de casse `getOverview()` :
  - modifier le calcul de `busyAgents` (gestion `runtime` null)
  - modifier le critère de `recentErrors` (type `ERROR` uniquement)
  - contourner la sanitation de limite
- Risques de casse délégation existante :
  - faibles, mais impact possible si signatures publiques changent.
- Risques sur les tests :
  - tests existants couvrent explicitement délégation + agrégation + sanitation de limite;
  - tout retrait/altération de ces comportements fera échouer les tests.

## 6. Plan de nettoyage recommandé
- Étape 1 : ne pas toucher aux signatures publiques de la façade.
- Étape 2 : conserver `getOverview()` tel que point d’agrégation unique.
- Étape 3 : vérifier qu’aucune logique supplémentaire n’a été réintroduite depuis les phases précédentes (gardiennage de périmètre).
- Étape 4 : micro-nettoyage local uniquement si nécessaire : homogénéiser le style et conserver la lisibilité (sans refactor structurel).
- Étape 5 : conserver les 7 injections actuelles, toutes utilisées.
- Étape 6 : exécuter les tests de façade pour valider qu’aucune régression n’est introduite.
- Étape 7 : conclure la phase 5 avec statut « façade propre, centrée orchestration légère ».
