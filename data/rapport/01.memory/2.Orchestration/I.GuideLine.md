Je te propose de le découper en **phases très nettes**, pour que Codex avance sans partir dans tous les sens.

Petit point honnête : je n’ai pas réussi à récupérer proprement le repo via la recherche web dans cet environnement, donc je m’appuie sur **ce qu’on a déjà identifié ensemble sur ta structure mémoire/orchestrator**, pas sur une relecture fraîche du dépôt en ligne. ([GitHub][1])

---

# Objectif global

Faire en sorte que l’orchestrator n’utilise plus seulement la mémoire conversationnelle, mais un **pipeline mémoire complet** :

1. lire les bonnes mémoires avant l’appel LLM,
2. assembler un contexte propre,
3. écrire dans les bonnes mémoires après l’échange,
4. réutiliser ces mémoires plus tard,
5. garder le tout testable et progressif.

---

# Stratégie générale pour Codex

Il faut demander à Codex de travailler **par petits lots atomiques**, avec à chaque fois :

* classes créées/modifiées,
* comportement attendu,
* tests à écrire,
* critères d’acceptation,
* pas de refactor massif non demandé.

Le bon ordre n’est pas “tout brancher d’un coup”, mais :

1. **stabiliser les contrats**
2. **brancher la lecture**
3. **brancher l’écriture**
4. **ajouter la boucle de feedback**
5. **durcir avec les tests**

---

# Plan détaillé à donner à Codex

## Phase 0 — Audit rapide et sécurisation des points d’entrée

### But

Faire un état précis des points déjà présents pour éviter de redévelopper en doublon.

### Travail demandé

* Identifier les classes/interfaces déjà existantes pour :

    * conversation memory
    * semantic memory
    * rule memory
    * episodic memory
    * context assembler
    * memory retrieval
    * orchestrator chat
* Lister :

    * ce qui est déjà appelé par l’orchestrator
    * ce qui existe mais n’est jamais branché
    * les DTO déjà présents et réutilisables

### Livrable attendu

Un petit markdown de diagnostic dans le repo, par exemple :

* `data/rapport/memory/Documentation/memory-integration-audit.md`

### Critère d’acceptation

* aucune nouvelle classe métier créée à cette phase
* seulement un audit concret

---

## Phase 1 — Introduire une façade mémoire unique

### But

Éviter que l’orchestrator parle directement à 6 services mémoire différents.

### Travail demandé

Créer une façade centrale, par exemple :

* `MemoryFacade`
* `DefaultMemoryFacade`

### Contrat attendu

La façade doit exposer au minimum :

* une méthode pour construire le contexte mémoire avant appel LLM
* une méthode pour enregistrer le message utilisateur
* une méthode pour enregistrer la réponse assistant
* une méthode pour enregistrer un événement d’outil / d’action
* une méthode optionnelle pour notifier quelles mémoires ont été injectées

Exemple d’intention :

* `buildContext(...)`
* `onUserMessage(...)`
* `onAssistantMessage(...)`
* `onToolExecution(...)`
* `markContextMemoriesUsed(...)`

### Important

À cette phase, la façade peut au début **simplement déléguer** aux services existants.

### Critère d’acceptation

* l’orchestrator n’appelle plus directement tous les services mémoire dispersés
* il passe par `MemoryFacade`

---

## Phase 2 — Normaliser le modèle d’entrée mémoire

### But

Avoir un objet unique qui décrit le besoin mémoire d’un tour de conversation.

### Travail demandé

Créer ou compléter un DTO de requête, par exemple :

* `MemoryContextRequest`
  ou réutiliser l’existant si déjà proche

### Champs attendus

* `agentId`
* `userId` si disponible
* `botId` si pertinent
* `projectId` si disponible
* `currentUserMessage`
* `conversationId` si tu en as une
* `maxSemanticMemories`
* `maxEpisodes`
* `maxConversationMessages`
* `tokenBudgetHint` optionnel

### But caché

Préparer le terrain pour faire du retrieval multi-source propre.

### Critère d’acceptation

* l’orchestrator construit cet objet une seule fois
* cet objet est transmis à la façade mémoire

---

## Phase 3 — Propager réellement le `projectId`

### But

Activer enfin le scope projet qui semble prévu mais pas vraiment utilisé.

### Travail demandé

* Identifier d’où peut venir le `projectId` dans l’orchestrator
* Le faire remonter jusqu’au `MemoryContextRequest`
* Le propager aux services de retrieval/rule lookup/semantic lookup qui savent l’utiliser

### Attention

Ne pas inventer un `projectId` si le flux n’en a pas.
Faire un flux propre :

* présent si connu
* nul sinon

### Critère d’acceptation

* quand un `projectId` est fourni, il influence bien la sélection mémoire
* quand il est absent, le comportement existant reste stable

### Tests à demander

* test sans `projectId`
* test avec `projectId`
* test de non-régression

---

## Phase 4 — Faire un vrai retrieval multi-source

### But

Aujourd’hui il faut sortir d’un retrieval uniquement “semantic memory entry” et construire une récupération mémoire structurée.

### Travail demandé

Créer un service du style :

* `MemoryRetrievalFacade`
  ou faire évoluer le `MemoryRetriever` existant

### Résultat attendu

Retourner un objet structuré du style :

* `RetrievedMemories`

    * `rules`
    * `semanticMemories`
    * `episodicMemories`
    * `conversationSlice`

### Règles de récupération

* `rules` :

    * toutes les règles actives applicables au scope
* `semanticMemories` :

    * top N pertinentes selon scoring
* `episodicMemories` :

    * top N épisodes récents et/ou pertinents
* `conversationSlice` :

    * fenêtre conversationnelle courte

### Important

Ne pas injecter de logique de prompt ici.
Cette phase ne fait que **récupérer**, pas formater.

### Critère d’acceptation

* l’orchestrator ou la façade mémoire récupère un objet multi-source
* la mémoire épisodique n’est plus seulement un journal mort

---

## Phase 5 — Brancher la mémoire épisodique à la lecture

### But

Faire en sorte que les épisodes servent réellement au contexte.

### Travail demandé

* ajouter dans la récupération mémoire une sélection d’épisodes :

    * récents
    * filtrés par agent / projet / type si possible
* définir un format résumé minimal pour l’injection future :

    * type
    * statut
    * résumé
    * date
    * cible éventuelle

### Priorité

Commencer simple :

* derniers épisodes utiles
* sans faire de moteur sémantique complexe

### Critère d’acceptation

* un épisode récemment écrit peut être relu et réinjecté au tour suivant

### Tests à demander

* un épisode de succès remonte bien
* un épisode d’erreur remonte bien
* les épisodes hors scope ne remontent pas

---

## Phase 6 — Faire évoluer le `ContextAssembler`

### But

Assembler proprement toutes les sources mémoire avant l’appel LLM.

### Travail demandé

Modifier `ContextAssembler` pour qu’il prenne l’objet `RetrievedMemories` structuré, et pas seulement la conversation + quelques entries.

### Ordre d’assemblage attendu

1. system prompt agent
2. rules applicables
3. semantic memories
4. episodic memories utiles
5. conversation récente
6. message utilisateur courant

### Contraintes

* chaque bloc doit être clairement séparé
* éviter le bruit
* limiter le volume
* aucun doublon évident

### Format attendu

Un contexte lisible et stable, du style :

* section rules
* section known facts
* section recent relevant episodes
* recent conversation

### Critère d’acceptation

* le contexte final contient les 4 familles utiles
* la conversation n’écrase pas le reste

---

## Phase 7 — Brancher l’écriture automatique de mémoire sémantique

### But

Faire en sorte que des faits durables soient extraits des échanges.

### Travail demandé

Créer un composant dédié, par exemple :

* `SemanticMemoryExtractor`
* `DefaultSemanticMemoryExtractor`

### Entrées possibles

* message utilisateur
* réponse assistant
* éventuellement résultat d’outil

### Politique V1 à demander à Codex

Pas d’extraction LLM complexe au début.
Commencer avec une politique heuristique simple :

* préférences explicites
* conventions projet explicites
* informations durables de config/architecture
* choix techniques stables
* exclusions claires

### À ne pas faire

* ne pas stocker tout et n’importe quoi
* ne pas copier chaque phrase
* ne pas écrire de mémoire sémantique pour du temporaire

### Critère d’acceptation

* un fait durable détecté crée une entrée sémantique
* une phrase purement contextuelle/éphémère n’en crée pas

### Tests à demander

* “l’utilisateur préfère YAML” => stocké
* “fais-moi un résumé aujourd’hui” => non stocké
* “les noms de classes sont en anglais” => stocké

---

## Phase 8 — Brancher l’écriture des règles de fonctionnement

### But

Faire vivre la `RuleMemory`, mais de façon contrôlée.

### Travail demandé

Créer un composant par exemple :

* `RulePromotionService`
  ou
* `RuleMemoryInterpreter`

### Politique V1

Ne promouvoir en règle que les formulations explicites du type :

* “toujours…”
* “dorénavant…”
* “à partir de maintenant…”
* “ne fais jamais…”
* “utilise systématiquement…”

### Important

* pas d’auto-règle libre inventée par le LLM
* promotion stricte et prudente
* idéalement avec un niveau de confiance / origine

### Critère d’acceptation

* une instruction explicite devient une règle
* une préférence vague ne devient pas automatiquement une règle

### Tests à demander

* “toujours poser une question si ambigu” => règle
* “j’aime bien quand c’est clair” => pas forcément règle

---

## Phase 9 — Structurer l’écriture épisodique

### But

Faire de l’épisodique un journal utile et cohérent.

### Travail demandé

Standardiser les épisodes écrits par l’orchestrator :

* user message received
* llm response generated
* tool execution success
* tool execution failure
* orchestration branch / retry / fallback éventuel

### Si ce n’est pas déjà fait

Créer un petit builder/factory, par exemple :

* `EpisodicEventFactory`

### Bénéfice

Tu évites que chaque appel écrive des épisodes au format libre.

### Critère d’acceptation

* les épisodes sont homogènes
* le résumé est exploitable par la phase de retrieval

---

## Phase 10 — Ajouter la boucle `markUsed`

### But

Récompenser les mémoires réellement injectées dans le contexte.

### Travail demandé

Une fois que le `ContextAssembler` a sélectionné les mémoires injectées :

* notifier la façade mémoire
* appeler `markUsed(...)` sur les mémoires sémantiques concernées
* éventuellement tracer l’usage des règles / épisodes plus tard, mais priorité au sémantique

### Important

`markUsed` doit être appelé sur les mémoires **effectivement injectées**, pas juste candidates.

### Critère d’acceptation

* `usageCount` monte quand une mémoire sert vraiment
* `lastAccessedAt` se met à jour

### Tests à demander

* mémoire candidate mais non injectée => pas marquée
* mémoire injectée => marquée

---

## Phase 11 — Ajouter un budget et des limites

### But

Empêcher l’explosion du prompt.

### Travail demandé

Ajouter des limites configurables :

* max rules
* max semantic memories
* max episodes
* max conversation messages
* budget caractères ou budget tokens approximatif

### V1 recommandée

Pas besoin d’un tokenizer exact au début.
Une limite simple en taille texte ou nombre d’éléments suffit.

### Critère d’acceptation

* le contexte reste borné
* les éléments les moins pertinents sautent avant les plus utiles

---

## Phase 12 — Configuration Spring / properties

### But

Rendre le comportement pilotable.

### Travail demandé

Créer ou compléter un `@ConfigurationProperties`, par exemple :

* `MemoryIntegrationProperties`

### Champs utiles

* enableSemanticExtraction
* enableRulePromotion
* enableEpisodicInjection
* maxSemanticMemories
* maxEpisodes
* maxConversationMessages
* markUsedEnabled

### Critère d’acceptation

* comportement activable/désactivable sans recoder
* valeurs bornées depuis la conf

---

## Phase 13 — Refactor léger de l’orchestrator

### But

Garder un orchestrator lisible.

### Travail demandé

Faire évoluer le flux de l’orchestrator pour suivre ce schéma :

1. recevoir le message user
2. `memoryFacade.onUserMessage(...)`
3. construire `MemoryContextRequest`
4. `memoryFacade.buildContext(...)`
5. construire la requête LLM
6. appeler le LLM
7. `memoryFacade.onAssistantMessage(...)`
8. en cas d’outil : `memoryFacade.onToolExecution(...)`
9. `memoryFacade.markContextMemoriesUsed(...)` si nécessaire

### Critère d’acceptation

* l’orchestrator ne contient pas lui-même la logique de sélection mémoire
* il reste orchestrateur, pas moteur mémoire

---

## Phase 14 — Tests unitaires ciblés

### But

Sécuriser sans noyer le projet.

### Blocs de tests à demander

#### A. `DefaultMemoryFacadeTest`

* délégation correcte
* buildContext appelle les bons composants
* onUser/onAssistant/onTool déclenchent les bons writers

#### B. `ContextAssemblerTest`

* ordre des sections
* pas de doublons grossiers
* respect des limites

#### C. `SemanticMemoryExtractorTest`

* stocke les faits durables
* ignore l’éphémère

#### D. `RulePromotionServiceTest`

* promeut les règles explicites
* ignore les formulations vagues

#### E. `MemoryRetriever` / retrieval multi-source test

* règles + sémantique + épisodes + conversation présents
* filtrage par scope/projet

#### F. `ChatAgentOrchestratorMemoryIntegrationTest`

* test d’intégration orienté flux complet
* un échange écrit puis relit correctement la mémoire

---

## Phase 15 — Documentation technique

### But

Laisser une base claire pour toi plus tard.

### Travail demandé

Créer un doc du style :

* `docs/memory-integration-flow.md`

### À documenter

* pipeline lecture/écriture
* rôle de chaque mémoire
* ordre d’assemblage
* politique d’extraction
* politique de promotion de règles
* points d’extension futurs

---

# Ordre réel d’exécution que je recommande à Codex

Voici l’ordre optimal, très concret :

### Lot 1 — fondation

1. audit
2. `MemoryFacade`
3. `MemoryContextRequest`
4. propagation `projectId`

### Lot 2 — lecture

5. retrieval multi-source
6. injection épisodique
7. évolution `ContextAssembler`

### Lot 3 — écriture

8. `SemanticMemoryExtractor`
9. `RulePromotionService`
10. structuration des épisodes

### Lot 4 — feedback et robustesse

11. `markUsed`
12. limites / budget
13. properties Spring
14. refactor orchestrator final
15. tests + doc

---

# Ce qu’il faut dire explicitement à Codex

Tu peux lui imposer ces règles :

* ne pas faire de gros refactor transverse non demandé
* réutiliser un maximum des DTO/services déjà présents
* ne pas renommer inutilement les packages
* faire des commits logiques par phase
* toujours écrire les tests du lot avant de passer au suivant
* ne pas introduire de dépendance externe lourde
* rester en Java/Spring idiomatique
* privilégier une V1 heuristique simple plutôt qu’une pseudo-intelligence floue

---

# Point de vigilance principal

Le plus gros risque, ce n’est pas la technique.
C’est que Codex essaie de “faire intelligent” trop tôt.

Il faut lui demander une **V1 pragmatique** :

* règles explicites seulement,
* extraction sémantique heuristique simple,
* épisodes résumés simplement,
* scoring existant réutilisé,
* pas de surcouche LLM pour piloter la mémoire à ce stade.

---

# Résultat attendu à la fin

À la fin, tu dois obtenir un système où :

* la conversation continue de marcher comme aujourd’hui,
* les règles sont réellement lues et peuvent être alimentées proprement,
* les faits durables sont stockés en sémantique,
* les épisodes sont réutilisés,
* le `projectId` sert enfin,
* le scoring apprend via `markUsed`,
* l’orchestrator reste propre.

Si tu veux, je peux maintenant te rédiger ça sous forme de **prompt Codex prêt à copier-coller**, très directif, avec les lots et les critères d’acceptation.

[1]: https://github.com/dfint/df-translation-toolkit?utm_source=chatgpt.com "dfint/df-translation-toolkit"
