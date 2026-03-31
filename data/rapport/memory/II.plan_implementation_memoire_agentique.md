# Plan d’implémentation complet — Système de mémoire agentique

## 1. Objectif du document

Ce document ne redéfinit pas l’architecture métier détaillée des briques mémoire.  
Cette architecture existe déjà dans les rapports dédiés.

L’objectif ici est différent :

- donner à un agent codeur un **ordre d’implémentation sûr**
- réduire le risque de dérive
- découper le travail en **phases exécutables**
- expliciter les **prérequis**
- préciser les **tests attendus**
- définir une **Definition of Done** par phase
- lister les **interdits** pour éviter la sur-ingénierie

Ce document sert donc de **guide d’exécution**.

---

# 2. Résultat attendu en fin de V1

En fin d’implémentation, le système doit permettre :

1. de stocker une mémoire sémantique durable en base SQL
2. de calculer un score dynamique de priorité
3. de récupérer les mémoires pertinentes par filtre + score
4. de stocker l’historique de conversation court terme
5. de stocker les règles de fonctionnement
6. de construire un contexte final pour le LLM
7. de stocker un journal épisodique simple d’événements

Le système doit rester :

- simple
- déterministe
- testable
- indépendant du provider LLM
- extensible sans refonte majeure

---

# 3. Rappel des briques de la V1

Les briques à implémenter sont :

1. `SemanticMemory`
2. `Memory Scoring`
3. `Memory Retrieval`
4. `RuleMemory`
5. `ConversationMemory`
6. `ContextAssembler`
7. `EpisodicMemory`

## Brique conceptuelle transverse

`Gouvernance` ne sera **pas** une brique autonome en V1.

Elle sera traitée comme :
- des règles d’écriture dans chaque service
- des garde-fous simples
- des responsabilités explicites

---

# 4. Environnement de développement
## Technique
- Spring Boot
- Jdk 26 installé là : D:\Installs\Java\jdk-26
- Surtout **utiliser Lombok** pour @Data, @Getter, @Setter, @Builder, @AllArgsConstructor, @NoArgsConstructor, @RequiredArgsConstructor

## Definition of 'Technically' done
- lancer **tous** les tests du projet et vérifier les résultats. Réfléchir si ça vient de l'algo ou du test. Corriger les éventuels bugs trouvés. Boucler 4 fois max.
- faire un maven clean install à la fin de chaque étape. Corriger les éventuels bugs trouvés.
- toujours finir par faire un maven clean install valide avant de considérer l'étape comme réalisée.
- réitérer la correction des bugs trouvés 4 fois. si toujours en erreur, le signaler


# 5. Ordre d’implémentation recommandé

## Ordre retenu

```text
PHASE 1  -> SemanticMemory
PHASE 2  -> Memory Scoring
PHASE 3  -> Memory Retrieval
PHASE 4  -> RuleMemory
PHASE 5  -> ConversationMemory
PHASE 6  -> ContextAssembler
PHASE 7  -> EpisodicMemory
PHASE 8  -> Intégration orchestrateur + tests de flux
```

## Pourquoi cet ordre

### SemanticMemory d’abord
C’est la fondation long terme la plus structurante :
- entités
- repository
- service
- recherche simple

### Memory Scoring juste après
Le retrieval en dépend.

### Memory Retrieval ensuite
Il permet de rendre la mémoire sémantique réellement exploitable.

### RuleMemory ensuite
Simple à intégrer, utile pour le contexte final.

### ConversationMemory après
Assez indépendante, mais nécessaire avant l’assembler.

### ContextAssembler après toutes les sources
Car il dépend des briques amont.

### EpisodicMemory après
Importante, mais non bloquante pour faire tourner la boucle principale.

### Intégration de flux à la fin
Permet de stabiliser le tout.

---

# 6. Contraintes globales à respecter par l’agent codeur

## 5.1 Ce qu’il faut faire

- respecter les interfaces définies dans les rapports
- garder une architecture simple
- privilégier des services Spring lisibles
- séparer :
  - modèle
  - persistance
  - service métier
  - assemblage
- documenter les choix non triviaux dans le code
- écrire les tests au fur et à mesure


## 6.2 Ce qu’il ne faut pas faire

- ne pas ajouter de vector database
- ne pas ajouter d’embeddings
- ne pas utiliser de LLM dans la V1
- ne pas inventer d’heuristiques non demandées
- ne pas fusionner plusieurs briques dans une seule classe
- ne pas faire de pipeline “magique”
- ne pas introduire de multi-user complet
- ne pas faire de scoring ML
- ne pas sur-généraliser les abstractions

## 6.3 Principes de qualité obligatoires

- code Java clair
- noms de classes en anglais
- pas de dépendance forte au provider LLM
- logique métier testable indépendamment de Spring
- configuration externalisée
- logs utiles mais sobres
- pas d’optimisation prématurée

---

# 7. Convention de packaging recommandée

Exemple de découpage conseillé :

```text
fr.ses10doigts.toolkitbridge.memory
 ├── semantic
 │    ├── model
 │    ├── repository
 │    ├── service
 │    ├── port
 │    └── config
 ├── scoring
 │    ├── model
 │    ├── service
 │    └── config
 ├── retrieval
 │    ├── model
 │    ├── service
 │    └── port
 ├── rule
 │    ├── model
 │    ├── repository
 │    ├── service
 │    ├── port
 │    └── config
 ├── conversation
 │    ├── model
 │    ├── service
 │    ├── store
 │    ├── port
 │    └── config
 ├── episodic
 │    ├── model
 │    ├── repository
 │    ├── service
 │    ├── port
 │    └── config
 └── context
      ├── model
      ├── service
      ├── port
      └── config
```

Cette convention n’est pas obligatoire au caractère près, mais le principe doit être respecté :
- une brique = un sous-domaine lisible

---

# 8. Phase 1 — Implémenter `SemanticMemory`

## 8.1 Objectif
Créer la mémoire durable structurée.

## 8.2 Livrables attendus
- enums :
  - `MemoryScope`
  - `MemoryType`
  - `MemoryStatus`
- entité `MemoryEntry`
- repository JPA
- service métier
- configuration Spring
- recherche simple SQL/JPQL

## 8.3 Fonctionnalités minimales
- créer une entrée mémoire
- mettre à jour une entrée
- désactiver / archiver une entrée
- rechercher par :
  - `agentId`
  - `scope`
  - `type`
  - texte simple
- stocker :
  - `importance`
  - `usageCount`
  - `lastAccessedAt`
  - tags

## 8.4 Points de vigilance
- ne pas mettre le score en base
- ne pas coupler au scoring ici
- ne pas ajouter de recherche vectorielle
- conserver une entité simple

## 8.5 Tests obligatoires
### Unitaires
- création d’une mémoire valide
- rejet d’une mémoire invalide si validations ajoutées
- mise à jour des champs principaux
- archivage / obsolescence

### Intégration JPA
- recherche par scope
- recherche par type
- recherche texte simple
- isolation par agent

## 8.6 Definition of Done
La phase est finie si :
- on peut persister et relire une mémoire
- on peut filtrer correctement
- les tests passent
- aucune logique de scoring avancée n’a été ajoutée

---

# 9. Phase 2 — Implémenter `Memory Scoring`

## 9.1 Objectif
Créer le moteur de priorisation simple.

## 9.2 Livrables attendus
- interface `ScorableMemory`
- service `MemoryScoringService`
- implémentation `DefaultMemoryScoringService`
- éventuellement propriétés de poids configurables

## 9.3 Règle V1
```text
score = importance + (usageCount * weightUsage) + recencyBoost
```

Avec :
```text
recencyBoost = 1 / (1 + age_in_days)
```

## 9.4 Points de vigilance
- score calculé dynamiquement
- pas de stockage du score
- pas de ML
- pas de feedback loop automatique

## 9.5 Tests obligatoires
- une mémoire plus importante score plus haut
- une mémoire plus utilisée score plus haut
- une mémoire plus récente score plus haut
- score stable et déterministe à date donnée

## 9.6 Definition of Done
La phase est finie si :
- un service de scoring testable existe
- `MemoryEntry` peut être évaluée via `ScorableMemory`
- les résultats sont cohérents sur cas simples

---

# 10. Phase 3 — Implémenter `Memory Retrieval`

## 10.1 Objectif
Sélectionner les mémoires pertinentes à partir de la mémoire sémantique.

## 10.2 Livrables attendus
- `MemoryQuery`
- `MemoryRetriever`
- `DefaultMemoryRetriever`

## 10.3 Fonctionnalités minimales
- récupérer des candidats depuis `SemanticMemory`
- filtrer par :
  - scope
  - type
  - statut actif
- appliquer le scoring
- trier
- limiter à `top N`

## 10.4 Points de vigilance
- pas de NLP
- pas de LLM
- pas de pipeline multi-step
- pas de vector search

## 10.5 Tests obligatoires
- filtre scope correct
- filtre type correct
- tri par score correct
- limitation `limit` respectée
- aucune mémoire archivée/obsolete injectée

## 10.6 Definition of Done
La phase est finie si :
- on peut obtenir une liste triée et bornée de mémoires pertinentes
- le retrieval reste purement déterministe
- le service est indépendant du LLM

---

# 11. Phase 4 — Implémenter `RuleMemory`

## 11.1 Objectif
Créer la mémoire procédurale de règles.

## 11.2 Livrables attendus
- enums :
  - `RuleScope`
  - `RulePriority`
  - `RuleStatus`
- entité `RuleEntry`
- repository JPA
- service `RuleService`

## 11.3 Fonctionnalités minimales
- créer une règle
- activer/désactiver une règle
- récupérer les règles applicables par :
  - agent
  - projet
  - global
- trier par priorité

## 11.4 Règle de résolution simple V1
- `PROJECT` prioritaire sur `AGENT` si besoin métier futur
- `AGENT` prioritaire sur `GLOBAL`
- `CRITICAL > HIGH > MEDIUM > LOW`

⚠️ En V1, pas de moteur de résolution de conflit complexe.  
Le tri et le filtrage suffisent.

## 11.5 Points de vigilance
- pas de versioning complet
- pas de moteur de contradiction sophistiqué
- pas de calcul de confiance
- pas d’injection illimitée

## 11.6 Tests obligatoires
- récupération des règles actives seulement
- tri correct par priorité
- scope correct
- désactivation bien respectée

## 11.7 Definition of Done
La phase est finie si :
- les règles peuvent être stockées et retrouvées proprement
- l’ordre d’injection peut être maîtrisé
- les règles désactivées ne sortent plus

---

# 12. Phase 5 — Implémenter `ConversationMemory`

## 12.1 Objectif
Créer la mémoire court terme par conversation.

## 12.2 Livrables attendus
- enums et records de conversation
- `ConversationMemoryStore`
- `InMemoryConversationMemoryStore`
- `ConversationSummarizer`
- `ConversationMemoryService`
- `ConversationContextRenderer`

## 12.3 Fonctionnalités minimales
- stocker les messages par :
  - `agentId`
  - `conversationId`
- garder une fenêtre glissante
- résumer si overflow
- reconstruire un contexte texte

## 12.4 Points de vigilance
- ne pas transformer ça en mémoire long terme
- ne pas faire de résumé “intelligent” en V1
- ne pas dépendre d’un LLM pour résumer
- ne pas mélanger plusieurs conversations

## 12.5 Tests obligatoires
- conversation vide
- ajout de messages
- overflow par nombre de messages
- overflow par nombre de caractères
- conservation de `minMessagesToKeep`
- rendu summary + récents
- isolation par `agentId + conversationId`

## 12.6 Definition of Done
La phase est finie si :
- une conversation peut être conservée proprement
- le contexte court terme est reconstructible
- les conversations ne fuient pas entre elles

---

# 13. Phase 6 — Implémenter `ContextAssembler`

## 13.1 Objectif
Construire le contexte final du LLM.

## 13.2 Livrables attendus
- `ContextRequest`
- `ContextAssembler`
- `DefaultContextAssembler`

## 13.3 Fonctionnalités minimales
Assembler, dans cet ordre :

1. règles
2. mémoires sémantiques pertinentes
3. conversation
4. dernier message utilisateur

## 13.4 Limites obligatoires
- limite de règles
- limite de mémoires
- limite totale en caractères

## 13.5 Points de vigilance
- ordre strict
- format stable
- pas d’appel LLM ici
- pas de logique “magique”
- pas de trimming destructeur mal placé

## 13.6 Tests obligatoires
- ordre des sections respecté
- limites respectées
- contexte trimé sans erreur
- conversation bien intégrée
- règles bien intégrées
- mémoire sémantique bien intégrée

## 13.7 Definition of Done
La phase est finie si :
- on peut générer un prompt de contexte stable
- l’assembler ne dépend pas du provider LLM
- l’ordre d’injection est déterministe

---

# 14. Phase 7 — Implémenter `EpisodicMemory`

## 14.1 Objectif
Créer le journal des actions et résultats.

## 14.2 Livrables attendus
- enums :
  - `EpisodeEventType`
  - `EpisodeStatus`
- entité `EpisodeEvent`
- repository JPA
- service `EpisodicMemoryService`

## 14.3 Fonctionnalités minimales
- enregistrer un événement
- relire les derniers événements
- filtrer par agent / scope
- stocker :
  - action
  - détails
  - statut
  - score éventuel
  - date

## 14.4 Points de vigilance
- pas de résumé automatique
- pas d’analyse prédictive
- pas d’injection systématique dans le prompt
- append-only autant que possible

## 14.5 Tests obligatoires
- enregistrement d’un événement
- récupération des plus récents
- isolation par agent
- type d’événement correct

## 14.6 Definition of Done
La phase est finie si :
- les événements peuvent être journalisés proprement
- le service reste simple et audit-friendly

---

# 15. Phase 8 — Intégration orchestrateur et flux complet

## 15.1 Objectif
Brancher les briques dans le vrai flux agentique.

## 15.2 Flux cible

```text
Incoming message
    -> resolve agentId
    -> resolve conversationId
    -> append user message in ConversationMemory
    -> ContextAssembler.buildContext(...)
    -> call LLM
    -> append assistant response in ConversationMemory
    -> optionally write episodic event
    -> return response
```

## 15.3 Intégrations minimales
- branchement avec l’orchestrateur principal
- branchement avec l’agent courant
- récupération correcte de `conversationId`
- tests d’intégration

## 15.4 Tests de flux obligatoires
- message utilisateur écrit en mémoire
- contexte assemblé avant appel LLM
- réponse assistant écrite en mémoire
- règles visibles dans le contexte
- mémoires pertinentes visibles dans le contexte
- conversation isolée par agent et conversation

## 15.5 Definition of Done
La phase est finie si :
- le flux complet fonctionne
- les briques coopèrent correctement
- aucun couplage fort au provider n’a été ajouté

---

# 16. Backlog d’exécution prêt pour un agent codeur

## Ticket 1 — Créer le socle `SemanticMemory`
### À faire
- créer enums
- créer entité
- créer repository
- créer service
- créer config
- écrire tests unitaires et intégration

### Sortie attendue
- mémoire sémantique persistée et requêtable

---

## Ticket 2 — Créer `MemoryScoring`
### À faire
- créer interface `ScorableMemory`
- implémenter scoring service
- brancher sur `MemoryEntry`
- écrire tests

### Sortie attendue
- moteur de score déterministe

---

## Ticket 3 — Créer `MemoryRetriever`
### À faire
- créer `MemoryQuery`
- créer interface
- implémenter retrieval
- brancher scoring
- écrire tests

### Sortie attendue
- top N mémoires pertinentes

---

## Ticket 4 — Créer `RuleMemory`
### À faire
- créer enums
- créer entité
- créer repository
- créer service
- écrire tests

### Sortie attendue
- règles stockées et récupérables par priorité/scope

---

## Ticket 5 — Créer `ConversationMemory`
### À faire
- créer modèle
- créer store in-memory
- créer summarizer simple
- créer renderer
- créer service
- écrire tests

### Sortie attendue
- mémoire court terme conversationnelle stable

---

## Ticket 6 — Créer `ContextAssembler`
### À faire
- créer request
- créer interface
- implémenter assembler
- brancher RuleMemory + Retrieval + ConversationMemory
- écrire tests

### Sortie attendue
- contexte final prêt pour le LLM

---

## Ticket 7 — Créer `EpisodicMemory`
### À faire
- créer modèle
- créer repository
- créer service
- écrire tests

### Sortie attendue
- journal d’événements simple

---

## Ticket 8 — Brancher dans l’orchestrateur
### À faire
- brancher `ConversationMemory`
- brancher `ContextAssembler`
- écrire dans `EpisodicMemory` si utile
- écrire tests d’intégration

### Sortie attendue
- flux complet opérationnel

---

# 17. Gouvernance V1 — à traiter sans brique dédiée

## Décision retenue
Pas de module autonome `Governance` en V1.

## Règles simples à appliquer
- `RuleMemory` : écriture principalement manuelle / système
- `SemanticMemory` : écriture contrôlée par service
- `ConversationMemory` : écriture orchestrateur
- `EpisodicMemory` : écriture système / instrumentation

## Interdit V1
- ne pas laisser n’importe quelle brique écrire partout
- ne pas créer de passerelle implicite entre mémoire conversationnelle et mémoire durable
- ne pas auto-promouvoir une conversation en mémoire durable sans règle explicite

---

# 18. Questions que l’agent codeur ne doit PAS résoudre seul

Si une ambiguïté apparaît, l’agent codeur ne doit **pas** improviser sur les sujets suivants :

1. stratégie de résumé LLM
2. ajout de vector search
3. multi-user complet
4. résolution avancée de conflits de règles
5. apprentissage à partir des épisodes
6. usage d’un LLM local pour le ranking
7. migration Redis / Postgres / Qdrant avancée
8. refonte globale de l’orchestrateur

👉 Dans ces cas, il doit rester dans le périmètre V1.

---

# 19. Checklist finale de validation

## Architecture
- [ ] chaque brique est séparée
- [ ] les dépendances vont dans le bon sens
- [ ] pas de couplage direct au provider LLM

## Données
- [ ] `SemanticMemory` persistée
- [ ] `RuleMemory` persistée
- [ ] `EpisodicMemory` persistée
- [ ] `ConversationMemory` isolée par conversation

## Services
- [ ] scoring opérationnel
- [ ] retrieval opérationnel
- [ ] assembler opérationnel

## Flux
- [ ] message utilisateur enregistré
- [ ] contexte assemblé
- [ ] réponse assistant enregistrée

## Qualité
- [ ] tests unitaires présents
- [ ] tests d’intégration présents
- [ ] pas de features hors périmètre V1

---

# 20. Recommandation finale d’exécution

Pour maximiser les chances de réussite de l’expérience :

## Recommandation 1
Demander à l’agent de traiter **une phase à la fois**.

## Recommandation 2
Exiger :
- code
- tests
- explication synthétique des choix
- liste des fichiers créés/modifiés

## Recommandation 3
Refuser qu’il code la phase suivante tant que :
- les tests de la phase courante ne passent pas
- la phase n’est pas validée

## Recommandation 4
Lui rappeler explicitement :
- de ne pas sortir du périmètre 
- de ne pas complexifier
- de ne pas introduire d’outils ou technos non demandés

---

# 21. Prompt de pilotage conseillé pour l’agent codeur

Tu peux piloter l’agent avec une instruction de ce type :

```text
Tu dois implémenter le système de mémoire agentique par phases strictes.
Respecte l’ordre du plan d’implémentation fourni.
Ne sors pas du périmètre V1.
À chaque phase :
1. implémente uniquement ce qui est demandé
2. écris les tests associés
3. donne la liste des fichiers créés/modifiés
4. explique en quelques points les choix réalisés
5. n’ajoute aucune techno ou complexité non demandée

Tu ne passes pas à la phase suivante tant que la phase courante n’est pas terminée proprement.
```

---

# 22. Conclusion

Les rapports détaillés par brique donnent la **cible architecturale**.  
Ce document donne le **mode opératoire d’exécution**.

En combinant :
- les rapports détaillés
- ce plan d’implémentation
- une validation phase par phase

… tu maximises nettement les chances qu’un agent codeur produise quelque chose de propre, stable et exploitable.

La clé de réussite pour cette première expérience n’est pas d’être “plus intelligent” :
👉 c’est d’être **plus discipliné que créatif**.
