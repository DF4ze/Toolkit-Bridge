# Correction — Isolation des bases de test

## 1. Modifications appliquées
Objectif atteint: suppression de la dépendance à une DB de test globale persistante et isolation par classe pour les tests `@SpringBootTest` qui n’avaient pas de datasource dédiée.

### 1.1 Suppression de la DB test globale
- Fichier modifié: `src/test/resources/application.yml`
- Changement: suppression de `spring.datasource.url: jdbc:sqlite:./data/toolkit-bridge-test.db`
- Le driver SQLite reste défini (`org.sqlite.JDBC`).

### 1.2 Isolation par classe des `@SpringBootTest` sans datasource
Ajout de `spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db` sur les classes suivantes:
- `src/test/java/fr/ses10doigts/toolkitbridge/ToolkitBridgeApplicationTests.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/context/service/ContextAssemblerSpringIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/conversation/service/ConversationMemorySpringIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/episodic/repository/EpisodeEventRepositoryIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/retrieval/service/MemoryRetrieverIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/rule/repository/RuleEntryRepositoryIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/scoring/service/MemoryScoringServiceIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/memory/semantic/repository/MemoryEntryRepositoryIT.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/configuration/admin/AdministrableConfigurationEntityVersionIT.java`

Vérification post-changement:
- toutes les classes `@SpringBootTest` du repository ont désormais une datasource explicite.

## 2. Tests restart
- Aucun changement de logique sur les tests restart.
- Vérification faite: ils utilisent déjà une DB SQLite unique via fichier temporaire par test (`Files.createTempFile(...)`) et redémarrent sur cette même DB dédiée.

Tests restart rejoués avec succès:
- `PersistentAdminTaskStoreRestartIT`
- `CriticalTracePersistenceRestartIT`
- `PersistentHumanInterventionServiceRestartIT`

## 3. Vérification MemoryScoringServiceIT
- `MemoryScoringServiceIT` est désormais isolé sur une DB unique (`target/test-db-${random.uuid}.db`).
- Résultat observé: test vert.
- Effet recherché confirmé: plus de dépendance à une config persistée d’un run précédent.

## 4. Vérifications exécutées
### 4.1 Maven ciblé (classes modifiées)
Commande:
- `./mvnw -Dtest="ToolkitBridgeApplicationTests,MemoryScoringServiceIT,MemoryRetrieverIT,ContextAssemblerSpringIT,ConversationMemorySpringIT,EpisodeEventRepositoryIT,RuleEntryRepositoryIT,MemoryEntryRepositoryIT,AdministrableConfigurationEntityVersionIT" test`

Résultat:
- `BUILD SUCCESS`
- `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`

### 4.2 Maven restart tests
Commande:
- `./mvnw -Dtest="PersistentAdminTaskStoreRestartIT,CriticalTracePersistenceRestartIT,PersistentHumanInterventionServiceRestartIT" test`

Résultat:
- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

### 4.3 Build global
Commande:
- `./mvnw clean install`

Résultat:
- `BUILD SUCCESS`
- synthèse surefire: `Tests run: 371, Failures: 0, Errors: 0, Skipped: 1`

## 5. Limite connue (IntelliJ)
- Je ne peux pas exécuter IntelliJ directement depuis cet environnement terminal.
- La validation réalisée couvre l’équivalent via exécution Maven ciblée + build Maven global.

## 6. Conclusion
- La source de pollution inter-runs (DB de test globale persistante) est supprimée.
- Les tests Spring Boot précédemment non isolés sont maintenant déterministes au niveau datasource.
- Le cas `MemoryScoringServiceIT` est stabilisé.
- Le build Maven reste vert après correction.
