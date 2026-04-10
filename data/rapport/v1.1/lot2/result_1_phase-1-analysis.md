# Phase 1 — Analyse de sécurisation avant extraction

## 1. Fichiers et classes concernés
- Façade cible
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- DTO/Types manipulés directement par les 3 méthodes ciblées
  - `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/admin/technical/TechnicalAdminView.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/model/AgentTraceEvent.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/model/AgentTraceEventType.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/artifact/model/Artifact.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/task/AdminTaskSnapshot.java`
- Dépendances de `listAgents()`
  - `AgentDefinitionService` (`src/main/java/fr/ses10doigts/toolkitbridge/service/agent/definition/AgentDefinitionService.java`)
  - `AgentRuntimeRegistry` + modèles runtime (`.../service/agent/runtime/...`)
  - `AgentPolicyRegistry`, `AgentPolicy`, `ResolvedAgentPolicy` (`.../service/agent/policy/...`)
  - `ToolRegistryService`, `ToolDescriptor` (`.../service/tool/...`)
- Dépendances de `getOverview()`
  - Appels internes de la façade: `listAgents`, `listRecentTasks`, `listRecentTraces`, `listRecentArtifacts`, `getConfigurationView`, `listRetentionPolicies`
  - `AdminTechnicalProperties` pour `sanitizeLimit`
- Dépendances de `listRecentArtifacts()`
  - `ArtifactService` (`src/main/java/fr/ses10doigts/toolkitbridge/service/agent/artifact/service/ArtifactService.java`)
- Tests déjà présents autour de la façade et de ses usages
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminControllerTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageControllerTest.java`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardServiceTest.java`
- Classes d’usage à risque de régression (sans changement prévu en phase 1)
  - `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminController.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageController.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardService.java`

## 2. État actuel des tests existants
- Ce qui est déjà testé
  - `TechnicalAdminFacadeTest`: 1 test unique sur `getConfigurationView()` (masquage de la clé API et comptages de base).
  - `TechnicalAdminControllerTest`: délégation des endpoints vers la façade (pas de logique métier).
  - `TechnicalAdminPageControllerTest`: délégation + mapping modèle/vue + normalisation filtre page (pas de logique interne façade).
  - `AdminDashboardServiceTest`: consommation de `getOverview()` via mock de façade, y compris mode dégradé.
- Ce qui manque réellement (phase 1)
  - `listAgents()` n’a pas de test de comportement direct (runtime présent/absent, résolution policy fallback, exposition outils, mapping runtime/policy).
  - `getOverview()` n’a pas de test de calcul métier direct (busy agents, recent errors, tailles agrégées, propagation limit).
  - `listRecentArtifacts()` n’a pas de test de contrat direct (priorité filtre `taskId`, fallback `agentId`, fallback global, tri desc, limite effective).
  - Absence de test dédié verrouillant la règle de comptage erreur (type enum vs string).
- Sensibilité de couverture actuelle
  - Les tests existants “passent” même si la logique interne de la façade dérive, car ils mockent la façade au niveau controllers/dashboard.
  - La phase 1 doit donc ajouter de vrais tests unitaires sur la façade elle-même, pas seulement sur ses consommateurs.

## 3. Comportements à verrouiller avant refactor
- `listAgents()`
  - Cas runtime présent: la policy et les outils exposés doivent provenir du runtime (et non du fallback policy registry).
  - Cas runtime absent: fallback via `policyRegistry.getRequired()` + `policy.resolve(...)` + `ToolRegistryService`.
  - Mapping stable des champs `AgentItem` (`role`, `orchestrator`, `runtime`, `policy`, `exposedTools`).
  - Normalisation des noms d’outils (`normalize`) et ordre trié des `exposedTools` en fallback.
- `getOverview()`
  - Application de `sanitizeLimit` sur la limite demandée.
  - Agrégation cohérente des sous-listes (agents/tasks/traces/artifacts) et des compteurs dérivés.
  - `busyAgents` calculé uniquement sur `agent.runtime()!=null && busy==true`.
  - `recentErrors` compté uniquement sur traces de type erreur (après correction du point fragile).
  - Cohérence entre compteurs et tailles des listes renvoyées.
- `listRecentArtifacts()`
  - Priorité contractuelle actuelle: `taskId` (si non blank) > `agentId` (si non blank) > global `findRecent`.
  - Le résultat final doit rester trié par `createdAt` descendant puis limité à `effectiveLimit`.
  - Traitement des filtres blank/null et de la limite via `AdminTechnicalProperties`.

## 4. Analyse du point fragile autour de `ERROR`
- Où se trouve exactement la logique
  - `TechnicalAdminFacade#getOverview`, filtre:
  - `trace -> trace.type() != null && trace.type().name().equals("ERROR")`
- Type réel manipulé
  - `trace.type()` est de type `AgentTraceEventType` (enum).
  - L’enum contient explicitement `ERROR` (`AgentTraceEventType.ERROR`).
- Robustesse disponible déjà dans le code
  - Le projet manipule déjà cet enum dans d’autres composants (`switch` et appels typés), donc un test enum direct est possible et cohérent.
- Correction la plus saine et minimale
  - Remplacer la comparaison string par une comparaison d’enum:
  - `trace.type() == AgentTraceEventType.ERROR`
  - Bénéfices: suppression d’une dépendance à `name()`, sécurité de refactor enum, lisibilité métier meilleure.
  - Impact: local, sans changement de contrat DTO/API, sans changement d’architecture.

## 5. Risques et points d’attention
- Faux positifs de tests
  - Risque: vérifier des mocks/implémentation interne au lieu du contrat observable.
  - Mitigation: assertions sur résultat DTO + vérifications ciblées des appels de dépendances essentielles seulement.
- Tests trop couplés à l’implémentation
  - Risque: tests fragiles si réorganisation interne (sans changement de comportement).
  - Mitigation: privilégier tests orientés comportement (input/output), limiter les `verify()` aux embranchements clés.
- Règles métier implicites non documentées
  - Priorité filtre artefacts (`taskId` > `agentId`) actuellement implicite.
  - Calcul `recentErrors` actuellement implicite via la trace.
  - Mitigation: tests nommés explicitement “contract” pour figer ces règles avant extraction.
- Ambiguïté filtres artefacts
  - Si `taskId` et `agentId` sont fournis, `taskId` gagne actuellement; ce n’est pas exposé explicitement côté API.
  - Mitigation: un test dédié doit verrouiller ce comportement tant qu’aucune décision produit contraire n’est prise.
- Effets de bord potentiels hors façade
  - Dashboard/admin pages/REST dépendent de `getOverview` et des listes; un changement non maîtrisé peut altérer les métriques affichées.
  - Mitigation: garder les tests existants controllers/dashboard + ajouter tests unitaires façade phase 1.

## 6. Plan d’implémentation recommandé
1. Créer un jeu de fixtures minimal dans `TechnicalAdminFacadeTest` pour construire agents/runtime/policy/tools/traces/artifacts avec timestamps contrôlés.
2. Ajouter des tests `listAgents()` couvrant runtime présent vs absent et la source des `exposedTools`/policy.
3. Ajouter un test `listAgents()` sur fallback policy avec normalisation/tri des tools exposés.
4. Ajouter des tests `listRecentArtifacts()` de contrat sur la priorité des filtres (`taskId` puis `agentId` puis global).
5. Ajouter un test `listRecentArtifacts()` sur tri descendant par `createdAt` et application de limite effective.
6. Ajouter des tests `getOverview()` pour vérifier l’agrégation des tailles/listes et `busyAgents`.
7. Corriger la comparaison fragile `"ERROR"` en comparaison enum `AgentTraceEventType.ERROR` dans `getOverview()`.
8. Ajouter un test `getOverview()` verrouillant le comptage `recentErrors` avec traces `ERROR` et non-`ERROR`.
9. Exécuter les tests ciblés (`TechnicalAdminFacadeTest` + tests controllers/dashboard existants) pour valider l’absence de régression de surface.

## 7. Recommandation finale
- La phase 1 doit rester strictement une phase de sécurisation comportementale de la façade actuelle.
- Le levier principal est d’augmenter la couverture unitaire de `TechnicalAdminFacade` sur les trois zones sensibles (`listAgents`, `getOverview`, `listRecentArtifacts`) avant toute extraction.
- La correction `ERROR` doit être faite maintenant, localement et minimalement (enum), accompagnée d’un test de verrouillage explicite.
- Aucun changement d’architecture, de contrôleurs, ni de contrat DTO n’est nécessaire pour atteindre l’objectif de cette phase.
