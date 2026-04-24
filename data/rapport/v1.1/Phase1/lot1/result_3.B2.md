Micro-lot final appliqué, limité aux 2 points demandés.

### Code modifié

1. **Test d’intégration “chaîne réelle” bootstrap -> DB -> resolver**
- Remplacement du test partiellement mocké par un test sur contexte applicatif réel, DB SQLite dédiée de test, puis vérification:
    - `memory.configuration` effectivement présente en DB via gateway
    - snapshot resolver aligné sur la valeur seedée
- Fichier:
    - [AdministrableConfigurationStartupOrderIT.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationStartupOrderIT.java)

2. **Contrat explicite des champs `integration` non appliqués runtime**
- Documentation explicite dans le mapper runtime:
    - les champs `integration.max*` sont volontairement ignorés à ce stade
- Fichier:
    - [MemoryRuntimeConfigurationMapper.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapper.java)

3. **Test de verrouillage du contrat**
- Ajout d’un test qui prouve que varier `integration.maxRules / maxSemanticMemories / maxEpisodes / maxConversationMessages / maxContextCharacters` ne change pas la config runtime mappée.
- Fichier:
    - [MemoryRuntimeConfigurationMapperTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapperTest.java)

---

### Tests exécutés

Commande:
- `.\mvnw.cmd "-Dtest=AdministrableConfigurationStartupOrderIT,MemoryRuntimeConfigurationMapperTest,MemoryRuntimeConfigurationResolverTest,AdministrableConfigurationSeedServiceTest,AdministrableConfigurationBootstrapTest" test`

Résultat:
- succès (`11` tests, `0` échec, `0` erreur)

---

### Points d’attention

- Le test d’intégration utilise maintenant le contexte applicatif réel, donc un peu plus coûteux qu’un test purement mocké (mais nettement plus probant sur la séquence réelle).
- Aucun élargissement de périmètre:
    - pas de reload
    - pas de fallback YAML runtime
    - pas de migration conversation RAM
    - pas de nouvelle fonctionnalité

---

### Liste claire des fichiers modifiés (ce micro-lot)

- [AdministrableConfigurationStartupOrderIT.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationStartupOrderIT.java)
- [MemoryRuntimeConfigurationMapper.java](/D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapper.java)
- [MemoryRuntimeConfigurationMapperTest.java](/D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/memory/config/runtime/MemoryRuntimeConfigurationMapperTest.java)