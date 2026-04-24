Implémentation Phase 3B réalisée dans le périmètre validé, avec initialisation explicite du snapshot mémoire **après bootstrap** et sans reload.

**Code modifié**
- Ajout d’un modèle runtime mémoire typé + merge DB-first avec defaults internes (pas YAML runtime) :
    - [MemoryRuntimeConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfiguration.java)
- Ajout d’un resolver central initialisé au démarrage (`ApplicationRunner`, `@Order(1)`) :
    - [MemoryRuntimeConfigurationResolver.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java)
- Migration des services mémoire vers le resolver central (plus de lecture `*Properties` en runtime mémoire) :
    - [DefaultMemoryFacade.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/facade/service/DefaultMemoryFacade.java)
    - [DefaultMemoryRetriever.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/DefaultMemoryRetriever.java)
    - [DefaultMemoryRetrievalFacade.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/facade/DefaultMemoryRetrievalFacade.java)
    - [DefaultMemoryScoringService.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/DefaultMemoryScoringService.java)
    - [DefaultContextAssembler.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssembler.java)
    - [MarkdownSharedGlobalContextProvider.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java)

**Tests ajoutés/ajustés**
- Nouveau test resolver DB-first + defaults :
    - [MemoryRuntimeConfigurationResolverTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolverTest.java)
- Tests adaptés pour injecter le resolver central :
    - [DefaultMemoryFacadeTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/facade/service/DefaultMemoryFacadeTest.java)
    - [DefaultMemoryRetrieverTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/DefaultMemoryRetrieverTest.java)
    - [DefaultMemoryRetrievalFacadeTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/retrieval/facade/DefaultMemoryRetrievalFacadeTest.java)
    - [DefaultMemoryScoringServiceTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/DefaultMemoryScoringServiceTest.java)
    - [DefaultContextAssemblerTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssemblerTest.java)
    - [MarkdownSharedGlobalContextProviderTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProviderTest.java)

**Vérification**
- Tests ciblés exécutés avec succès (`39` tests, `0` échec, `0` erreur) :
    - `.\mvnw.cmd "-Dtest=MemoryRuntimeConfigurationResolverTest,DefaultMemoryFacadeTest,DefaultMemoryRetrieverTest,DefaultMemoryRetrievalFacadeTest,DefaultMemoryScoringServiceTest,DefaultContextAssemblerTest,MarkdownSharedGlobalContextProviderTest" test`

**Points d’attention**
- Le snapshot runtime mémoire est figé après init (pas de reload).
- Si un composant tente d’accéder au resolver avant son `run`, une `IllegalStateException` explicite est levée.
- `ConversationMemoryProperties` et `InMemoryConversationMemoryStore` ne sont pas modifiés (conversation RAM inchangée).
- Les champs `integration` non encore consommés en métier restent dans le payload/snapshot, sans logique ad hoc ajoutée.


Après relecture :
**Constats (revue critique, par sévérité)**

1. **[P2] Risque de dérive des valeurs par défaut (duplication seed vs runtime)**
- Runtime defaults codés en dur dans [MemoryRuntimeConfiguration.java:18](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfiguration.java:18).
- Seed YAML->DB construit avec d’autres sources dans [AdministrableConfigurationSeedService.java:290](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationSeedService.java:290).
- Risque: divergence silencieuse entre “default runtime” et “default seed” si un côté évolue seul.

2. **[P2] Couplage runtime mémoire ↔ DTO admin**
- Le runtime dépend directement du payload admin via import dans [MemoryRuntimeConfiguration.java:3](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfiguration.java:3) et mapping dans `fromPayload(...)` [ligne 28](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfiguration.java:28).
- Risque: évolution du contrat admin qui casse le runtime mémoire.

3. **[P3] Validation répétée sur chemin d’exécution**
- Validation appelée à chaque build context dans [DefaultContextAssembler.java:49](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/service/DefaultContextAssembler.java:49).
- Validation appelée à chaque accès global context dans [MarkdownSharedGlobalContextProvider.java:42](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/global/service/MarkdownSharedGlobalContextProvider.java:42).
- Ce n’est pas bloquant, mais c’est du bruit runtime évitable.

4. **[P3] Séparation runtime/seed encore un peu floue dans la config Spring**
- Les `@EnableConfigurationProperties` mémoire restent activés dans modules runtime:
    - [MemoryIntegrationConfiguration.java:7](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/config/MemoryIntegrationConfiguration.java:7)
    - [MemoryRetrievalConfiguration.java:7](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/retrieval/config/MemoryRetrievalConfiguration.java:7)
    - [MemoryScoringConfiguration.java:10](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/scoring/config/MemoryScoringConfiguration.java:10)
    - [ContextAssemblerConfiguration.java:8](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/context/config/ContextAssemblerConfiguration.java:8)
- Fonctionnellement OK, mais architecturalement ambigu (runtime DB-first déjà migré).

5. **[P3] Gap de test d’intégration “ordre bootstrap puis snapshot runtime”**
- Le test resolver est unitaire mocké ([MemoryRuntimeConfigurationResolverTest.java:16](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolverTest.java:16)).
- Pas de test Spring intégration prouvant explicitement la séquence `@Order(0)` bootstrap puis `@Order(1)` resolver ([AdministrableConfigurationBootstrap.java:13](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java:13), [MemoryRuntimeConfigurationResolver.java:16](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java:16)).

**Corrections utiles recommandées (sans élargir le périmètre)**

1. Centraliser les defaults mémoire dans une seule source partagée (utilisée par seed et resolver).
2. Introduire un petit mapper/adaptateur entre payload admin et modèle runtime pour casser le couplage direct.
3. Déplacer la validation de config au moment de l’initialisation du snapshot (une fois), pas dans les hot paths.
4. Clarifier le rôle des `*Properties` mémoire restants (seed-only) en les regroupant côté seed config, hors runtime métier.
5. Ajouter un test d’intégration Spring minimal qui verrouille l’ordre bootstrap -> runtime snapshot.

**Résumé final**
- La Phase 3B est globalement propre et cohérente: DB-first centralisé, pas de fallback YAML runtime, pas de reload, conversation RAM préservée.
- Les points discutables sont surtout de la **dette d’architecture préventive** (duplication defaults, couplage DTO admin, ambiguïté de config restante, couverture d’intégration sur l’ordre de démarrage).
- Aucun défaut bloquant immédiat, mais ces corrections maintenant réduiraient nettement le coût des phases suivantes.