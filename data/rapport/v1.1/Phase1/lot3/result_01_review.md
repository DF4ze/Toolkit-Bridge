**Faiblesses / points discutables (revue architecture)**

1. **[Moyen] Coexistence de 2 implémentations Spring actives pour `HumanInterventionService`**
- `InMemoryHumanInterventionService` reste annoté `@Service` alors que la nouvelle impl est `@Primary`.
- Impact: résolution implicite correcte aujourd’hui, mais ambiguïté structurelle pour les phases futures/tests.
- Réf: [InMemoryHumanInterventionService.java:16](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/InMemoryHumanInterventionService.java:16), [PersistentHumanInterventionService.java:13](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionService.java:13)

2. **[Moyen] Redondance `status` / `decisionStatus` sans garde d’invariant explicite**
- Les deux colonnes portent potentiellement la même info terminale.
- Impact: risque de divergence future si un chemin d’update n’aligne pas les deux.
- Réf: [HumanInterventionEntity.java:53](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntity.java:53), [HumanInterventionEntity.java:66](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntity.java:66), [HumanInterventionRepository.java:21](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionRepository.java:21)

3. **[Moyen] Politique metadata: dépassement taille transformé en `"{}"` silencieux**
- En cas de >4KB, le mapper remplace par objet vide sans signal.
- Impact: perte silencieuse d’info, diagnostic difficile.
- Réf: [HumanInterventionEntityMapper.java:86](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntityMapper.java:86)

4. **[Faible] Désérialisation metadata “fail-open to empty map” sans trace**
- Toute erreur JSON retourne `Map.of()` sans log.
- Impact: robustesse runtime OK, mais observabilité faible en cas de corruption.
- Réf: [HumanInterventionEntityMapper.java:102](D:/Documents/Spring/Toolkit-Bridge/src/main/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/HumanInterventionEntityMapper.java:102)

5. **[Faible] Tests utiles mais un peu couplés à l’implémentation interne**
- Le test unitaire vérifie un JSON exact dans l’argument repository, ce qui le rend fragile aux micro-ajustements du mapper.
- Réf: [PersistentHumanInterventionServiceTest.java:113](D:/Documents/Spring/Toolkit-Bridge/src/test/java/fr/ses10doigts/toolkitbridge/service/agent/supervision/human/PersistentHumanInterventionServiceTest.java:113)

---

**Corrections utiles (sans élargir le périmètre)**

1. **Clarifier le wiring Spring**
- Retirer `@Service` de l’impl in-memory ou la conditionner explicitement (profil/condition).
- Garder une seule impl active par défaut.

2. **Verrouiller l’invariant de statut**
- Soit supprimer `decisionStatus` (si non indispensable),
- Soit documenter et garantir strictement `decisionStatus == status terminal` partout (et tests dédiés).

3. **Rendre la limite metadata plus explicite**
- Remplacer le fallback `"{}"` par `null` (ou conserver `"{}"` mais avec log `warn`).
- Garder le filtrage simple actuel.

4. **Ajouter une trace minimale en cas d’échec parse metadata**
- Un `debug/warn` discret dans le mapper suffit.

5. **Assouplir un test unitaire fragile**
- Vérifier présence de la clé attendue plutôt qu’un JSON string exact.

---

**Résumé final**

- Globalement, l’implémentation est **propre, lisible, et bien découplée** du reste (orchestrator/memory/tooling/policy/workspace non impactés).
- Le contrat `HumanInterventionService` est respecté, la concurrence `WHERE status=PENDING` est bien appliquée, et les tests couvrent les scénarios essentiels (dont restart).
- La dette principale est **structurelle légère** (double impl Spring active, redondance de statut, perte silencieuse metadata).
- Ces points sont **corrigeables maintenant à faible coût**, sans refactor global ni ajout fonctionnel hors périmètre.