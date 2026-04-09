










Lot **Phase 3B.1** appliqué, strictement ciblé sur les corrections d’architecture demandées.

**Code modifié**

1. **Source unique des defaults mémoire**
- Ajout d’une source centralisée :
    - [MemoryConfigurationDefaults.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryConfigurationDefaults.java)
- Le modèle runtime réutilise cette source :
    - [MemoryRuntimeConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfiguration.java)
- Les propriétés seed/bootstrapping mémoire réutilisent aussi cette source (évite la dérive) :
    - [ContextAssemblerProperties.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/config/ContextAssemblerProperties.java)
    - [GlobalContextProperties.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/config/GlobalContextProperties.java)
    - [MemoryRetrievalProperties.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/config/MemoryRetrievalProperties.java)
    - [MemoryIntegrationProperties.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/config/MemoryIntegrationProperties.java)
    - [MemoryScoringProperties.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/config/MemoryScoringProperties.java)

2. **Découplage runtime ↔ payload admin**
- Ajout d’un adaptateur dédié :
    - [MemoryRuntimeConfigurationMapper.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapper.java)
- Le resolver runtime dépend du mapper, plus du DTO admin directement :
    - [MemoryRuntimeConfigurationResolver.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java)

3. **Validation déplacée hors hot paths**
- Validation faite à l’initialisation du snapshot dans le resolver.
- Suppression des validations répétées à chaque appel métier :
    - [DefaultContextAssembler.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssembler.java)
    - [MarkdownSharedGlobalContextProvider.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java)

4. **Clarification du rôle des `*Properties` mémoire**
- Les `@EnableConfigurationProperties` ambigus runtime ont été retirés des modules mémoire concernés :
    - supprimés :
        - [MemoryIntegrationConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/config/MemoryIntegrationConfiguration.java)
        - [MemoryRetrievalConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/config/MemoryRetrievalConfiguration.java)
        - [ContextAssemblerConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/config/ContextAssemblerConfiguration.java)
- Le binding seed est explicitement porté par :
    - [AdministrableConfigurationSeedPropertiesConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationSeedPropertiesConfiguration.java)
- `MemoryScoringConfiguration` garde uniquement le bean `Clock` :
    - [MemoryScoringConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/config/MemoryScoringConfiguration.java)

**Tests ajoutés/ajustés**

- Mapper/resolver runtime (DB-first + defaults + init) :
    - [MemoryRuntimeConfigurationResolverTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolverTest.java)
- Test d’intégration minimal d’ordre de démarrage bootstrap puis snapshot runtime :
    - [AdministrableConfigurationStartupOrderIT.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationStartupOrderIT.java)
- Ajustements des tests services mémoire migrés au resolver central :
    - [DefaultMemoryFacadeTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/facade/service/DefaultMemoryFacadeTest.java)
    - [DefaultMemoryRetrieverTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/DefaultMemoryRetrieverTest.java)
    - [DefaultMemoryRetrievalFacadeTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/retrieval/facade/DefaultMemoryRetrievalFacadeTest.java)
    - [DefaultMemoryScoringServiceTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/DefaultMemoryScoringServiceTest.java)
    - [DefaultContextAssemblerTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssemblerTest.java)
    - [MarkdownSharedGlobalContextProviderTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProviderTest.java)

**Vérification**
- Exécuté avec succès :
- `.\mvnw.cmd "-Dtest=MemoryRuntimeConfigurationResolverTest,DefaultMemoryFacadeTest,DefaultMemoryRetrieverTest,DefaultMemoryRetrievalFacadeTest,DefaultMemoryScoringServiceTest,DefaultContextAssemblerTest,MarkdownSharedGlobalContextProviderTest,AdministrableConfigurationStartupOrderIT,AdministrableConfigurationSeedServiceTest,AdministrableConfigurationBootstrapTest" test`
- Résultat : `45` tests, `0` échec, `0` erreur.

**Points d’attention**
- Aucun fallback YAML runtime ajouté.
- Aucun reload introduit.
- Mémoire conversationnelle RAM inchangée (`InMemoryConversationMemoryStore` non touché).
- Le test d’intégration d’ordre de démarrage lance un contexte Spring Boot complet (un peu plus coûteux en temps), mais verrouille bien la séquence attendue.