# Phase 4 — Analyse d’extraction du bloc agents runtime policy tools

## 1. Fichiers et classes concernés

- Façade principale concernée:
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
  - Bloc cible actuel: `listAgents()`, `toAgentItem(...)`, `resolvePolicyWithoutRuntime(...)`, `resolveExposedToolsFromPolicy(...)`, `toRuntimeItem(...)`, `toPolicyItem(...)`, `normalize(...)`.

- Services/registries utilisés par le bloc agents:
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/definition/AgentDefinitionService.java` (source des définitions agent, côté configuration admin).
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeRegistry.java` (source du runtime actif en mémoire).
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/policy/AgentPolicyRegistry.java` (résolution de policy en fallback si runtime absent).
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/tool/ToolRegistryService.java` (catalogue des tools pour fallback policy et exposition tools).

- Modèles runtime/policy/tools manipulés:
  - `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/agent/definition/AgentDefinition.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentRuntime.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentRuntimeState.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentRuntimeExecutionSnapshot.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/model/AgentToolAccess.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/policy/AgentPolicy.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/policy/ResolvedAgentPolicy.java`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/tool/ToolDescriptor.java`

- DTO manipulés (à conserver inchangés):
  - `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/admin/technical/TechnicalAdminView.java`
  - Sous-types directement utilisés: `AgentItem`, `RuntimeItem`, `PolicyItem`, `Overview`.

- Tests existants impactés (directement ou indirectement):
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacadeTest.java` (principal verrou comportemental du bloc à extraire).
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminControllerTest.java` (contrat API façade inchangé attendu).
  - `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/admin/technical/TechnicalAdminPageControllerTest.java` (usage façade côté pages).
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardServiceTest.java` (usage indirect via `getOverview()`).
  - `src/test/java/fr/ses10doigts/toolkitbridge/ToolkitBridgeApplicationTests.java` (wiring Spring global, sensible à la nouvelle injection).

- Usages indirects via `getOverview()`:
  - `TechnicalAdminFacade.getOverview(...)` appelle `listAgents()` en interne.
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/web/AdminDashboardService.java` consomme `technicalAdminFacade.getOverview(8)`.
  - Les contrôleurs techniques (`TechnicalAdminController`, `TechnicalAdminPageController`) exposent aussi `getOverview(...)`.

## 2. Analyse du bloc agents runtime policy tools

- Dépendances injectées réellement nécessaires au bloc à extraire:
  - `AgentDefinitionService`
  - `AgentRuntimeRegistry`
  - `AgentPolicyRegistry`
  - `ToolRegistryService`
- Dépendances non nécessaires à ce bloc (restent façade):
  - `AdminTaskQueryService`, `TraceQueryService`, `ArtifactQueryService`, `AdminConfigQueryService`, `RetentionQueryService`, `AdminTechnicalProperties`.

- Logique actuelle de fallback runtime absent:
  - Si runtime présent: source de vérité = `runtime.policy()` + `runtime.toolAccess().exposedTools()`.
  - Si runtime absent: reconstruction policy via `policyRegistry.getRequired(definition.policyName())` puis `policy.resolve(definition, AgentToolAccess(...))` avec tools venant de `ToolRegistryService`.
  - Ce fallback reproduit la logique de préparation policy/tools utilisée lors de la création d’un runtime (cf. `AgentRuntimeFactory`), mais uniquement pour projection admin.

- Logique de résolution policy/tools:
  - `resolvePolicyWithoutRuntime(...)` construit un `AgentToolAccess` avec:
    - `enabled = definition.toolsEnabled()`
    - `allowedTools = toolRegistryService.getToolNames()`
    - `registeredTools = toolRegistryService.getToolDescriptors()`
    - `exposedTools = []`
  - `resolveExposedToolsFromPolicy(...)`:
    - part de `policy.allowedTools()` (déjà normalisé par `ResolvedAgentPolicy`)
    - conserve uniquement les tools présents dans le registry
    - normalise les noms via `normalize(...)`
    - trie alphabétiquement la liste finale

- Logique de mapping DTO:
  - `toAgentItem(...)` projette définition + runtime/policy/tools.
  - `toRuntimeItem(...)` mappe le snapshot runtime avec fallback local `availability = "UNKNOWN"` si null.
  - `toPolicyItem(...)` mappe `ResolvedAgentPolicy` vers `TechnicalAdminView.PolicyItem`, notamment `accessibleMemoryScopes` en `Set<String>` (`Enum::name`).
  - Contrat important: si runtime absent, `runtime` dans `AgentItem` est `null`.

- Logique transverse cachée:
  - Différence de comportement volontaire entre runtime présent et fallback:
    - runtime présent: ordre des `exposedTools` = ordre de `runtime.toolAccess().exposedTools()` (pas de tri).
    - runtime absent: `exposedTools` triés par nom.
  - `normalize(...)` local sert uniquement au matching tools dans le fallback; il ne doit pas modifier d’autres flux.
  - `getOverview()` dépend des détails de `listAgents()` pour `busyAgents` (via `runtime.busy()`).

- Dépendances configuration vs runtime:
  - Configuration: `AgentDefinitionService` (définitions), `AgentPolicyRegistry` (catalogue policies), `ToolRegistryService` (catalogue tools).
  - Runtime: `AgentRuntimeRegistry` et `AgentRuntimeExecutionSnapshot`.
  - Le bloc mélange donc projection d’état runtime instantané et reconstruction config/policy en mode dégradé.

## 3. Points sensibles et risques d’extraction

- Risque de mélange config/runtime:
  - Si le futur service “simplifie” le fallback, on perd la symétrie actuelle runtime présent vs runtime absent.
  - Le fallback doit rester strictement limité à la projection admin, sans modifier la façon dont les runtimes sont réellement créés.

- Risque de déplacer de la logique métier trop profonde:
  - Ne pas déplacer la logique métier policy/tool dans le domaine runtime/policy.
  - Garder le service cible comme un service de lecture/projection admin, pas un nouveau “policy engine”.

- Risque de casser `getOverview()`:
  - `getOverview()` calcule `busyAgents` à partir de `listAgents()`.
  - Toute dérive de `listAgents()` impacte KPI dashboard (`recentTasks/recentErrors` restent ailleurs, mais `busyAgents` et `agents` dépendent du bloc extrait).

- Risque sur tests existants:
  - `TechnicalAdminFacadeTest` verrouille précisément les deux branches critiques (`runtime exists` vs `runtime missing`).
  - Si les assertions d’ordre et de fallback changent, régression immédiate.

- Risque de duplication/helpers mal placés:
  - `normalize(...)` existe déjà dans plusieurs classes du projet.
  - Le dupliquer à nouveau hors périmètre augmente la dette; pour cette phase, il doit être local au service qui en a l’usage (pas utilitaire global).

- Ce qui doit impérativement rester inchangé:
  - Signatures publiques de `TechnicalAdminFacade`.
  - Contrats DTO `TechnicalAdminView`.
  - Contrôleurs et endpoints existants.
  - Présence de `getOverview()` dans la façade et son rôle d’agrégation.
  - Comportement observé des tests actuels (runtime present/missing, mapping runtime/policy, exposition tools).

## 4. Plan d’extraction recommandé

1. Créer `AdminAgentQueryService` (package `service.admin`) avec dépendances minimales:
   - `AgentDefinitionService`, `AgentRuntimeRegistry`, `AgentPolicyRegistry`, `ToolRegistryService`.

2. Déplacer en l’état (copie comportementale) dans ce service:
   - `listAgents()`
   - `toAgentItem(...)`
   - `resolvePolicyWithoutRuntime(...)`
   - `resolveExposedToolsFromPolicy(...)`
   - `toRuntimeItem(...)`
   - `toPolicyItem(...)`
   - `normalize(...)` (temporairement local à ce service)

3. Adapter `TechnicalAdminFacade` pour déléguer `listAgents()` au nouveau service, sans modifier ses autres méthodes publiques.

4. Laisser `getOverview()` dans `TechnicalAdminFacade` exactement comme orchestrateur:
   - il continue d’appeler `listAgents()` de la façade (qui délègue désormais), puis tasks/traces/artifacts/config/rétention comme aujourd’hui.

5. Conserver temporairement tout helper du bloc agents dans `AdminAgentQueryService`:
   - pas d’extraction utilitaire transverse dans cette phase.

6. Ne pas toucher au domaine runtime/policy/tools:
   - aucun changement dans `AgentRuntimeFactory`, `AgentPolicyRegistry`, `ToolRegistryService`, modèles runtime/policy.

7. Valider la parité de comportement avant nettoyage:
   - même ordre/liste `exposedTools` selon branche runtime/fallback
   - même mapping `RuntimeItem`/`PolicyItem`
   - mêmes nullability semantics (`runtime` null si runtime absent).

8. Option de stabilisation légère (sans refonte):
   - garder `normalize(...)` dans `AdminAgentQueryService` pour la phase 4.
   - décider une mutualisation éventuelle uniquement en phase ultérieure dédiée à la dette technique.

Décision recommandée sur `normalize(...)`:
- Le faire migrer avec le bloc vers `AdminAgentQueryService`.
- Ne pas le laisser dans la façade (sinon couplage résiduel inutile).
- Ne pas le promouvoir en helper partagé global à ce stade.

## 5. Impact sur les tests

- Tests façade à adapter:
  - `TechnicalAdminFacadeTest`:
    - convertir les tests `listAgents*` en vérification de délégation vers `AdminAgentQueryService` (nouveau mock injecté).
    - conserver les tests de délégation existants pour tasks/traces/artifacts/config/rétention.
    - conserver les tests `getOverview*` (ils doivent rester au niveau façade et vérifier l’agrégation).

- Tests à déplacer vers le futur service:
  - les scénarios comportementaux riches de `listAgents` actuellement dans `TechnicalAdminFacadeTest`:
    - runtime présent
    - runtime absent + fallback policy/tool
    - mapping runtime/policy
    - exposition tools
  - nouveau fichier recommandé: `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/AdminAgentQueryServiceTest.java`.

- Nouveaux tests à créer:
  - `AdminAgentQueryServiceTest` dédié, couvrant:
    - fallback policy quand runtime absent
    - filtrage+tri `resolveExposedToolsFromPolicy(...)`
    - conservation de l’ordre runtime quand runtime présent
    - mapping `toRuntimeItem(...)` avec `availability == null` => `"UNKNOWN"`
    - mapping `toPolicyItem(...)` (`accessibleMemoryScopes` en `Enum::name`).

- Tests à conserver tels quels:
  - `TechnicalAdminControllerTest` (contrats de délégation API).
  - `TechnicalAdminPageControllerTest` (contrats de délégation page).
  - `AdminDashboardServiceTest` (usage indirect via `getOverview()`).
  - `ToolkitBridgeApplicationTests` (vérification wiring global).

## 6. Recommandation finale

- L’extraction en `AdminAgentQueryService` est pertinente et faisable sans refonte métier, à condition de faire une extraction strictement comportementale.
- La frontière recommandée est claire:
  - façade = orchestration publique admin et agrégation (`getOverview()` inclus)
  - nouveau service = projection agents/runtime/policy/tools.
- Le principal garde-fou de la phase 4 est la parité exacte des deux branches de `listAgents()` (runtime existant vs fallback config).
- Le chemin le plus sûr est de déplacer le bloc tel quel, d’abord, puis d’ajuster les tests autour de la nouvelle responsabilité sans modifier les DTO, contrôleurs ni domaine runtime/policy/tools.
