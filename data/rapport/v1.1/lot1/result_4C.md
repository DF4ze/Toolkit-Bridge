Implémentation Reload C (LLM runtime) réalisée dans le cadre validé, sans endpoint/UI et sans reload auto.

**Résumé**
- Suppression du chemin “bean registry actif figé au boot”.
- Mise en place d’un runtime LLM explicite, unique, initialisé au démarrage (`ApplicationRunner`) après bootstrap.
- Stratégie `build / validate / swap` atomique pour le reload LLM.
- `DefaultLlmService` lit désormais le registry actif via le holder runtime.
- Ajout du handler reload pour `LLM_PROVIDER_REGISTRY`.
- Politique conservée: registry vide autorisé, démarrage possible, erreur métier seulement quand le provider demandé est absent.

**Code modifié**
- `OpenAiLikeConfiguration` expose maintenant la factory (plus de bean `LlmProviderRegistry` figé).
- Nouvelle factory dédiée de construction/validation du registry.
- Nouveau holder runtime LLM (`volatile snapshot`, `reloadFromDatabase()` synchronisé, init explicite au boot).
- Nouveau handler reload LLM branché sur `ReloadDomain.LLM_PROVIDER_REGISTRY`.
- `DefaultLlmService` branché sur le runtime holder.

**Tests ajoutés/ajustés**
- Ajustés:
1. `OpenAiLikeConfigurationTest`
2. `DefaultLlmServiceTest`
- Ajoutés:
1. `LlmProviderRegistryFactoryTest`
2. `LlmProviderRegistryRuntimeTest`
3. `LlmProviderRegistryReloadHandlerTest`

Commande exécutée:
```bash
./mvnw "-Dtest=OpenAiLikeConfigurationTest,DefaultLlmServiceTest,LlmProviderRegistryFactoryTest,LlmProviderRegistryRuntimeTest,LlmProviderRegistryReloadHandlerTest,ExplicitReloadServiceTest" test
```
Résultat: succès (22 tests, 0 échec).

**Points d’attention**
- Le runtime LLM est désormais l’unique source active pour la résolution provider.
- `reloadFromDatabase()` ne fait aucune validation réseau (volontaire).
- Le message de reload LLM retourne un `providers=<count>`; `count=0` reste un cas valide.
- Je n’ai pas lancé toute la suite projet, seulement la batterie ciblée.

**Fichiers modifiés**
- [OpenAiLikeConfiguration.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfiguration.java)
- [DefaultLlmService.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmService.java)
- [LlmProviderRegistryFactory.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryFactory.java)
- [LlmProviderRegistryRuntime.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryRuntime.java)
- [LlmProviderRegistryReloadHandler.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/reload/LlmProviderRegistryReloadHandler.java)
- [OpenAiLikeConfigurationTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfigurationTest.java)
- [DefaultLlmServiceTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmServiceTest.java)
- [LlmProviderRegistryFactoryTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryFactoryTest.java)
- [LlmProviderRegistryRuntimeTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryRuntimeTest.java)
- [LlmProviderRegistryReloadHandlerTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/reload/LlmProviderRegistryReloadHandlerTest.java)

après relecture :
### Revue critique (findings d’abord)

1. **[P1] Contrat de cycle de vie implicite et fragile (ordre de `ApplicationRunner`)**  
   Le runtime LLM dépend d’un ordre numérique implicite (`@Order(2)`), qui suppose que le bootstrap DB est déjà passé. Ce couplage “magic number” peut se casser silencieusement si d’autres runners évoluent.  
   Références: [LlmProviderRegistryRuntime.java:17](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryRuntime.java:17), [AdministrableConfigurationBootstrap.java:13](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java:13), [MemoryRuntimeConfigurationResolver.java:16](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java:16)

2. **[P2] Couverture de tests encore incomplète sur le contrat runtime/reload**  
   Les tests unitaires sont bons, mais il manque un test d’intégration Spring ciblé qui vérifie que `ExplicitReloadService` route bien réellement vers le handler LLM enregistré dans le contexte (pas seulement en construction manuelle/mock).  
   Références: [LlmProviderRegistryReloadHandlerTest.java:13](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/reload/LlmProviderRegistryReloadHandlerTest.java:13), [ExplicitReloadServiceTest.java:137](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/reload/ExplicitReloadServiceTest.java:137)

3. **[P3] Test de config très faible signal**  
   Le test de config vérifie juste “non null”, ce qui protège peu contre une régression de wiring réelle.  
   Référence: [OpenAiLikeConfigurationTest.java:14](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfigurationTest.java:14)

---

### Points validés (positifs)

- Séparation config/runtime: nette et propre (`configuration` -> factory stateless, runtime actif dans un holder dédié).
- Pas de double chemin actif (plus de bean `LlmProviderRegistry` figé).
- `build/validate/swap` atomique correct (`candidate` construit avant assignation).
- Échec de rebuild: l’ancienne référence reste active.
- Découplage global correct: pas de nouveau couplage orchestrator/mémoire/tooling/policy/workspace.
- Nommage global lisible et cohérent.

---

### Corrections utiles proposées (sans élargir le périmètre)

1. **Durcir le contrat d’ordre de démarrage**  
   Remplacer les valeurs d’ordre “en dur” par des constantes partagées (ex: `StartupOrder.CONFIG_BOOTSTRAP`, `StartupOrder.MEMORY_RUNTIME`, `StartupOrder.LLM_RUNTIME`) pour rendre l’intention explicite et réduire la dette.

2. **Ajouter un test d’intégration Spring ciblé reload LLM**  
   Vérifier qu’un `reload(LLM_PROVIDER_REGISTRY)` passe bien par le handler réel du contexte et que le runtime snapshot reste cohérent après reload.

3. **Renforcer/supprimer le test de config faible**  
   Soit enrichir [OpenAiLikeConfigurationTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfigurationTest.java) avec assertions de comportement, soit l’enlever si sa valeur reste trop faible.

---

### Résumé final

Architecture globalement saine pour cette phase: la séparation runtime/config est bien posée, le modèle de reload explicite est cohérent avec mémoire, et le couplage transversal est maîtrisé.  
Les points discutables restants sont surtout de **robustesse de lifecycle** et de **couverture de tests d’intégration**; ils sont encore faciles à corriger maintenant, sans refonte.


--- 
Micro correction :
Micro-lot C.1 appliqué dans le périmètre demandé.

**Code modifié**
- Ajout d’une source centrale d’ordres de démarrage:
   - [StartupOrder.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/StartupOrder.java)
- Remplacement des ordres “magiques”:
   - [AdministrableConfigurationBootstrap.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java)
   - [MemoryRuntimeConfigurationResolver.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java)
   - [LlmProviderRegistryRuntime.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryRuntime.java)
- Test de config faible supprimé:
   - supprimé [OpenAiLikeConfigurationTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfigurationTest.java)

**Tests ajoutés/ajustés**
- Nouveau test d’intégration Spring ciblé reload LLM:
   - [LlmProviderRegistryReloadSpringIT.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/reload/LlmProviderRegistryReloadSpringIT.java)

Ce test prouve que:
1. `ExplicitReloadService` utilise bien le handler LLM enregistré dans le contexte Spring
2. un `reload(LLM_PROVIDER_REGISTRY)` met à jour le runtime actif
3. le snapshot runtime reste cohérent après reload (`seed` disparaît, `reloaded` devient actif)

**Exécution de tests**
Commande exécutée:
```bash
./mvnw "-Dtest=LlmProviderRegistryFactoryTest,LlmProviderRegistryRuntimeTest,DefaultLlmServiceTest,ExplicitReloadServiceTest,LlmProviderRegistryReloadHandlerTest,AdministrableConfigurationStartupOrderIT,LlmProviderRegistryReloadSpringIT" test
```
Résultat: succès (`23` tests, `0` échec).

**Points d’attention**
- Le contrat de cycle de vie est maintenant explicite et centralisé, sans framework additionnel.
- Le test d’intégration reload LLM reste volontairement compact et focalisé chaîne réelle Spring + runtime.
- Aucun élargissement fonctionnel: pas d’endpoint/UI, pas de reload auto, pas de modif memory reload.

**Fichiers modifiés (micro-lot C.1)**
- [StartupOrder.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/StartupOrder.java)
- [AdministrableConfigurationBootstrap.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationBootstrap.java)
- [MemoryRuntimeConfigurationResolver.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationResolver.java)
- [LlmProviderRegistryRuntime.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/llm/runtime/LlmProviderRegistryRuntime.java)
- [LlmProviderRegistryReloadSpringIT.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/reload/LlmProviderRegistryReloadSpringIT.java)
- supprimé [OpenAiLikeConfigurationTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeConfigurationTest.java)