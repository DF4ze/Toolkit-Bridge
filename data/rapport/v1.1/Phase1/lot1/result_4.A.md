### 1) Fichiers / classes concernés

**Memory (reloadable maintenant, avec limites)**
- Référence active actuelle: `volatile snapshot` dans [MemoryRuntimeConfigurationResolver.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java):23
- Init startup: [MemoryRuntimeConfigurationResolver.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java):26
- Mapping payload DB -> runtime snapshot: [MemoryRuntimeConfigurationMapper.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapper.java):20
- Consommateurs runtime snapshot:
    - [DefaultContextAssembler.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssembler.java):48
    - [DefaultMemoryRetrievalFacade.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/facade/DefaultMemoryRetrievalFacade.java):57
    - [DefaultMemoryRetriever.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/DefaultMemoryRetriever.java):54
    - [DefaultMemoryScoringService.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/DefaultMemoryScoringService.java):27
    - [DefaultMemoryFacade.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/facade/service/DefaultMemoryFacade.java):92
    - [MarkdownSharedGlobalContextProvider.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java):41
- Source DB: [AdministrableConfigurationGateway.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationGateway.java):57

**LLM (partiellement prêt, mais pas remplaçable proprement en runtime aujourd’hui)**
- Construction registry au boot Spring: [OpenAiLikeConfiguration.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfiguration.java):22
- Registry immuable: [LlmProviderRegistry.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/provider/LlmProviderRegistry.java):11
- Consommation: [DefaultLlmService.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmService.java):37 et :51
- Écriture admin DB (sans reload runtime): [LlmAdminFacade.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/admin/functional/LlmAdminFacade.java):61
- Signal explicite actuel “restart may be required”: [LlmAdminPageController.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/controler/web/admin/llm/LlmAdminPageController.java):90

**Autres candidats plus tard (hors phase)**
- `agent.definitions` (même pattern DB-first via gateway)
- `retention.configuration`, `artifacts.configuration` (mêmes clés administrables)

---

### 2) Points sensibles d’architecture (priorisés)

1. **P1 – LLM n’a pas de “référence active swappable”**  
   Aujourd’hui, la référence active est le bean `LlmProviderRegistry` injecté une fois; pas de holder runtime.

2. **P1 – Cache global context memory potentiellement incohérent après reload**  
   [MarkdownSharedGlobalContextProvider.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java) garde `cachedSnapshot` sans fingerprint de config; un reload memory devrait invalider explicitement ce cache.

3. **P1 – Risque startup LLM “empty registry” au premier boot**  
   Inférence: bootstrap seed est en `ApplicationRunner` ([AdministrableConfigurationBootstrap.java](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java):13), alors que le bean LLM est construit avant les runners. Donc seed post-construct peut ne pas être pris en compte sans restart.

4. **P2 – Domaines memory admin != memory runtime complet**  
   `MemoryConfigurationPayload.conversation` existe, mais le runtime central actuel ne l’expose pas (mapper centré sur sections déjà câblées).

5. **P2 – Divergence DB vs runtime observable**  
   `TechnicalAdminFacade` lit la DB (pas l’instance runtime active), donc après futur reload partiel il faudra expliciter ce que l’UI “configuration” affiche.

---

### 3) Recommandation de design (simple, extensible, testable)

**Contrat interne reload**
- `ReloadService` (interne) avec méthode unique type `reload(Set<ReloadDomain>)`.
- `ReloadDomain`: `MEMORY_RUNTIME`, `LLM_REGISTRY`.
- `ReloadReport` structuré:
    - `requestedDomains`
    - `results[]` par domaine (`SUCCESS|FAILED|SKIPPED`)
    - `startedAt`, `endedAt`, `durationMs`
    - `errorCode`, `errorMessage` (si échec)
    - `metadata` (ex: counts providers, source=db)

**Composants**
- Un orchestrateur léger (`ExplicitReloadService`) + 2 reloaders concrets:
    - `MemoryRuntimeReloader`
    - `LlmRegistryReloader`
- Pas de moteur générique plugin-heavy; juste une map `domain -> reloader`.

**Stratégie d’échec**
- Toujours **build/validate d’abord** dans des variables locales.
- Swap de la référence active **seulement si build complet OK**.
- Si fail: aucune mutation visible (ancienne version conservée).

**Journalisation**
- Logs structurés par domaine:
    - `reload.start domain=...`
    - `reload.success domain=... durationMs=...`
    - `reload.failure domain=... reason=...`
- Pas de logs de secrets (apiKey).

---

### 4) Stratégie de remplacement runtime recommandée

**Memory**
- Modèle actuel favorable: `volatile snapshot`.
- Recommandé: ajouter un chemin `reload()` dans resolver (ou composant dédié) qui:
    1. charge DB
    2. mappe
    3. valide
    4. swap `snapshot` en une affectation atomique
    5. invalide cache global context

**LLM**
- Introduire une **référence active dédiée** (`AtomicReference<LlmProviderRegistry>` ou `volatile` holder), consommée par `DefaultLlmService`.
- Builder du registry factorisé (réutilisé au boot + reload).
- Swap atomique du registry actif après validation complète (duplicates, noms, baseUrl, etc.).

**Concurrence**
- In-flight requests peuvent finir sur l’ancienne instance, ce qui est acceptable.
- Nouveau trafic lit la nouvelle référence immédiatement après swap.
- Aucun état partiel exposé si build-before-swap strict.

---

### 5) Plan d’implémentation recommandé (sans coder ici)

1. Définir `ReloadDomain`, `ReloadReport`, `ReloadDomainResult` (contrat interne).
2. Ajouter `ExplicitReloadService` orchestration simple multi-domaines.
3. Extraire/factoriser la logique de build memory runtime (réutilisable boot + reload).
4. Ajouter reloader memory avec validation + swap atomique + invalidation cache global context.
5. Extraire/factoriser la logique de build `LlmProviderRegistry` hors config Spring.
6. Introduire un holder runtime pour registry LLM et brancher `DefaultLlmService` dessus.
7. Ajouter reloader LLM (load DB -> build -> validate -> swap).
8. Ajouter tests unitaires de non-régression:
    - succès reload memory
    - échec reload memory conserve ancienne version
    - succès reload llm
    - échec reload llm conserve ancienne version
9. Ajouter test concurrence léger (reads pendant reload => jamais d’état partiel).
10. Ajouter logs structurés et vérifier absence de fuite de secrets.

---

### Ce qu’il ne faut surtout pas faire

- Ne pas déclencher un reload automatique après chaque write DB.
- Ne pas patcher en place des objets runtime déjà partagés.
- Ne pas coupler reload aux contrôleurs/UI maintenant.
- Ne pas créer un framework générique de reload sur-abstrait.
- Ne pas mélanger “DB config stockée” et “runtime actif” sans contrat explicite.

Si tu veux, je peux enchaîner avec un design cible très concret de `ReloadReport` (champs exacts + exemples JSON) pour verrouiller Reload B/C avant code.