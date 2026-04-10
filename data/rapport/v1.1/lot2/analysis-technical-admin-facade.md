# Analyse de TechnicalAdminFacade

## 1. Localisation et rôle actuel
- emplacement
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TechnicalAdminFacade.java`
- usages principaux
  - API REST technique admin: `TechnicalAdminController` (`/api/admin/technical/*`)
  - Pages Thymeleaf admin technique: `TechnicalAdminPageController` (`/admin/technical/*`)
  - Dashboard admin global: `AdminDashboardService` via `getOverview(8)`
- rôle perçu dans l’architecture
  - Façade de lecture/agrégation pour la supervision technique admin.
  - Elle fait à la fois: query multi-domaines, assemblage de DTO UI (`TechnicalAdminView.*`), calcul de métriques d’overview et adaptation partielle de politiques runtime.

## 2. Inventaire des dépendances
- liste des dépendances injectées
  - `AgentDefinitionService`
  - `AgentRuntimeRegistry`
  - `AgentPolicyRegistry`
  - `ToolRegistryService`
  - `AgentTraceQueryService`
  - `AdminTaskStore`
  - `ArtifactService`
  - `AdministrableConfigurationGateway`
  - `PersistenceRetentionPolicyResolver`
  - `AdminTechnicalProperties`
- rôle supposé de chacune
  - `AgentDefinitionService`: source des définitions d’agents (configuration admin).
  - `AgentRuntimeRegistry`: état runtime en mémoire (busy, contexte, trace, canal).
  - `AgentPolicyRegistry`: résolution d’une policy quand runtime absent.
  - `ToolRegistryService`: catalogue/outillage pour calculer les outils exposés.
  - `AgentTraceQueryService`: lecture des traces récentes (global/agent).
  - `AdminTaskStore`: lecture des snapshots de tâches admin (store in-memory actuellement).
  - `ArtifactService`: lecture d’artefacts (récent, par agent, par tâche).
  - `AdministrableConfigurationGateway`: lecture de la config admin (providers LLM).
  - `PersistenceRetentionPolicyResolver`: projection des politiques de rétention.
  - `AdminTechnicalProperties`: normalisation de limite (`sanitizeLimit`).
- premières anomalies éventuelles
  - Couplage fort multi-domaines: 10 dépendances directes dans une seule façade.
  - Mélange runtime + configuration + policy + outils dans `listAgents`.
  - Dépendance à des stores mémoire (`AdminTaskStore`, souvent `AgentTraceQueryService` in-memory) qui rend l’admin partiellement volatile.
  - Chemin de config doublonné: la façade lit `AgentDefinitionService` et aussi `AdministrableConfigurationGateway` directement.

## 3. Inventaire des méthodes
Pour chaque méthode :

| Signature | Responsabilité principale | Catégorie | Domaine concerné |
|---|---|---|---|
| `listAgents()` | Construit la vue agent (définition + runtime + policy + outils exposés) | orchestration + UI-adaptation | agents/runtime/policy/tools |
| `listRecentTasks(Integer, String, TaskStatus)` | Lit snapshots de tâches filtrés et mappe en DTO UI | query + UI-adaptation | tâches |
| `listRecentTraces(Integer, String)` | Lit traces globales ou par agent, trie/limite, mappe en DTO UI | query + UI-adaptation | traces |
| `listRecentArtifacts(Integer, String, String)` | Lit artefacts selon filtres (task prioritaire), trie/limite, mappe en DTO UI | query + UI-adaptation | artefacts |
| `getConfigurationView()` | Agrège volume d’agents + providers LLM, masque valeur API key | query + UI-adaptation | configuration admin |
| `listRetentionPolicies()` | Projette les politiques de rétention par famille persistable | query + UI-adaptation | rétention |
| `getOverview(Integer)` | Agrège toutes les sections + calcule KPIs (`busyAgents`, `recentErrors`) | orchestration / agrégation | observabilité transversale |
| `toAgentItem(AgentDefinition)` | Mapping riche agent (runtime/policy/tools) | UI-adaptation + orchestration | agents/runtime/policy |
| `resolvePolicyWithoutRuntime(AgentDefinition)` | Recalcule une policy hors runtime via registry + tool access | transverse (logique technique) | policy/tools |
| `resolveExposedToolsFromPolicy(ResolvedAgentPolicy)` | Traduit policy en liste d’outils exposés | UI-adaptation + transverse | policy/tools |
| `toRuntimeItem(AgentRuntimeExecutionSnapshot)` | Mapping runtime -> DTO | UI-adaptation | runtime |
| `toPolicyItem(ResolvedAgentPolicy)` | Mapping policy -> DTO | UI-adaptation | policy |
| `toTaskItem(AdminTaskSnapshot)` | Mapping tâche -> DTO | UI-adaptation | tâches |
| `toTraceItem(AgentTraceEvent)` | Mapping trace -> DTO | UI-adaptation | traces |
| `toArtifactItem(Artifact)` | Mapping artefact -> DTO | UI-adaptation | artefacts |
| `toRetentionItem(RetentionPolicy)` | Mapping rétention -> DTO | UI-adaptation | rétention |
| `isBlank(String)` | utilitaire local de validation | transverse | commun |
| `normalize(String)` | normalisation lowercase/trim pour matching outils | transverse | commun |

## 4. Regroupement par domaines
- regroupement logique des méthodes
  - Configuration admin: `getConfigurationView`, partie de `getOverview`.
  - Tâches: `listRecentTasks`, `toTaskItem`, partie de `getOverview`.
  - Traces: `listRecentTraces`, `toTraceItem`, partie de `getOverview`.
  - Artefacts: `listRecentArtifacts`, `toArtifactItem`, partie de `getOverview`.
  - Rétention: `listRetentionPolicies`, `toRetentionItem`, partie de `getOverview`.
  - Observabilité/agrégation transverse: `getOverview`.
  - Domaine agents/policy/runtime/tools: `listAgents`, `toAgentItem`, `resolvePolicyWithoutRuntime`, `resolveExposedToolsFromPolicy`, `toRuntimeItem`, `toPolicyItem`.
- cohérence ou incohérences observées
  - Cohérence partielle: toutes les méthodes servent l’admin technique en lecture.
  - Incohérence structurelle: la section agents embarque une logique plus profonde (résolution policy, construction d’`AgentToolAccess`) que les autres sections, qui sont surtout des projections.
  - `getOverview` centralise tous les domaines et fixe des règles métier UI (comptage erreurs via `type().name().equals("ERROR")`).

## 5. Problèmes structurels observés
- mélange de responsabilités
  - Façade = query + mapping UI + calcul d’indicateurs + logique technique policy/runtime.
- couplages
  - Couplage transversal à 10 services/composants.
  - Couplage fort agents<->policy<->tools dans un point d’entrée censé être “admin view”.
- duplication
  - Normalisation des limites à la fois dans la façade (`sanitizeLimit`) et dans `TechnicalAdminPageController`.
  - Multiples patterns de tri/limit/mapping répétés par domaine.
- risques à l’extraction
  - `getOverview` dépend de tous les sous-domaines: extraction naïve peut créer une micro-façade “éclatée” mais toujours centrale.
  - Risque de déplacer du mapping UI partout si on n’isole pas une frontière claire des DTO.
  - Risque de cycles si un futur service de config tente de dépendre à la fois de `AgentDefinitionService` et d’un service qui lui-même dépend déjà du gateway de config.
- points sensibles
  - `listRecentArtifacts`: priorité `taskId` sur `agentId` implicite, non explicitée contractuellement.
  - `recentErrors` basé sur string littérale `"ERROR"` (fragile vs évolution enum).
  - Couverture tests façade incomplète: test direct essentiellement centré sur `getConfigurationView` (masquage api key), peu de tests unitaires sur `listAgents/getOverview`.

## 6. Proposition de découpage progressif
- services candidats à extraire
  - `AdminTaskQueryService`
  - `TraceQueryService`
  - `ArtifactQueryService`
  - `AdminConfigQueryService`
  - `RetentionQueryService`
  - `AdminAgentQueryService` (nom conseillé par le code réel pour isoler runtime/policy/tools)
  - `TechnicalOverviewService` (optionnel, voir arbitrage)
- méthodes à déplacer dans chaque service
  - `AdminTaskQueryService`: `listRecentTasks`, `toTaskItem`.
  - `TraceQueryService`: `listRecentTraces`, `toTraceItem`.
  - `ArtifactQueryService`: `listRecentArtifacts`, `toArtifactItem`.
  - `AdminConfigQueryService`: `getConfigurationView`.
  - `RetentionQueryService`: `listRetentionPolicies`, `toRetentionItem`.
  - `AdminAgentQueryService`: `listAgents`, `toAgentItem`, `resolvePolicyWithoutRuntime`, `resolveExposedToolsFromPolicy`, `toRuntimeItem`, `toPolicyItem`, `normalize`.
  - `TechnicalOverviewService` (si retenu): `getOverview` + règle de calcul erreurs/busy.
- ordre recommandé des extractions
  1. Extraire `AdminTaskQueryService`, `TraceQueryService`, `ArtifactQueryService` (faible risque, frontières nettes).
  2. Extraire `AdminConfigQueryService` et `RetentionQueryService` (lecture pure).
  3. Extraire `AdminAgentQueryService` (plus sensible car croise runtime/policy/tools).
  4. Décider ensuite si `getOverview` reste dans la façade ou migre vers `TechnicalOverviewService`.
- ce qui peut rester temporairement dans la façade
  - `getOverview` comme orchestrateur de compatibilité pour ne pas casser les contrôleurs/tests immédiatement.
  - Méthodes délégantes (wrappers) vers les nouveaux services pendant une phase de transition.
  - `isBlank` utilitaire temporaire, puis suppression au fur et à mesure des déplacements.

## 7. Impact sur les usages existants
- contrôleurs/services impactés
  - `TechnicalAdminController`: impact nul si façade conservée comme adaptateur; impact modéré si injection directe de query services.
  - `TechnicalAdminPageController`: même logique que ci-dessus.
  - `AdminDashboardService`: dépend seulement de `getOverview`; impact faible si contrat conservé.
- tests à adapter
  - Existants vérifiés:
    - `TechnicalAdminFacadeTest`
    - `TechnicalAdminControllerTest`
    - `TechnicalAdminPageControllerTest`
    - `AdminDashboardServiceTest`
    - tests ciblés exécutés en local: OK.
  - À compléter avant/après extraction:
    - tests unitaires dédiés `listAgents` (cas runtime présent/absent, policy inconnue, outils exposés).
    - tests unitaires `getOverview` (calcul `busyAgents`/`recentErrors`, propagation config/rétention).
    - tests de contrat sur règles de filtrage artefacts (`taskId` vs `agentId`).
- risques de régression
  - divergence des DTO `TechnicalAdminView` si mapping dispersé.
  - rupture de comportement sur limites/filtres si la sanitation est déplacée sans règles explicites.
  - incohérences de tri (sources déjà triées vs tri façade) lors du déplacement.

## 8. Questions d’arbitrage pour le développeur
- liste explicite des questions à trancher avant implémentation
  - Sujet: maintien de la façade comme point d’entrée unique.
    - Options possibles: (A) garder la façade en adaptateur délégant; (B) injecter directement les services spécialisés dans les contrôleurs.
    - Conséquences techniques: A minimise le diff et le risque court terme; B réduit la couche intermédiaire mais augmente l’impact immédiat sur contrôleurs/tests.
  - Sujet: emplacement de `getOverview`.
    - Options possibles: (A) rester dans `TechnicalAdminFacade`; (B) extraire `TechnicalOverviewService`.
    - Conséquences techniques: A facilite migration incrémentale; B clarifie l’orchestration mais ajoute une dépendance transverse supplémentaire.
  - Sujet: frontière DTO `TechnicalAdminView`.
    - Options possibles: (A) mapping DTO dans chaque query service; (B) query services renvoient des snapshots internes et mapping centralisé au-dessus.
    - Conséquences techniques: A rapide mais disperse l’adaptation UI; B plus propre long terme mais demande plus de types intermédiaires.
  - Sujet: comportement de filtre artefacts quand `taskId` et `agentId` sont fournis.
    - Options possibles: (A) conserver priorité `taskId`; (B) combiner filtres; (C) rejeter la combinaison comme invalide.
    - Conséquences techniques: A compatible actuel; B plus intuitif mais nécessite support service/store; C simplifie logique mais modifie contrat API/UI.
  - Sujet: ownership de la logique policy/runtime/tools dans la vue agent.
    - Options possibles: (A) la laisser dans un `AdminAgentQueryService`; (B) la déplacer dans un service domaine runtime/policy réutilisable.
    - Conséquences techniques: A extraction rapide ciblée admin; B meilleure réutilisabilité mais périmètre plus large et plus risqué.

## 9. Recommandation finale
- stratégie d’exécution recommandée en 2 à 5 mini-lots
  1. Mini-lot 1 (sécurisation): ajouter tests manquants sur `listAgents` et `getOverview` + tests de contrat de filtres artefacts.
  2. Mini-lot 2 (queries à faible couplage): extraire `AdminTaskQueryService`, `TraceQueryService`, `ArtifactQueryService`, façade en délégation stricte.
  3. Mini-lot 3 (config/rétention): extraire `AdminConfigQueryService` et `RetentionQueryService`, conserver contrats DTO identiques.
  4. Mini-lot 4 (noyau sensible): extraire `AdminAgentQueryService`, puis décider explicitement du sort de `getOverview` (reste façade ou service dédié).
