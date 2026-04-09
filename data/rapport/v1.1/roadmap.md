# Roadmap détaillée de consolidation technique — Tool-Bridge / Marcel

## 1. Objectif de cette roadmap

Cette roadmap vise à transformer la dette technique identifiée dans le rapport en un **plan d’exécution progressif, cohérent et exploitable par un LLM ou par un développeur**.  
L’idée n’est pas simplement de “corriger des points”, mais de **stabiliser le socle architectural**, puis de **réduire les risques d’évolution**, avant de reprendre sereinement les fonctionnalités métier et l’ergonomie.

Le rapport initial montre une situation assez classique dans un projet qui avance vite :
- les briques existent,
- beaucoup de corrections ont déjà été faites,
- mais plusieurs zones restent en **équilibre fragile** :
    - façades trop larges,
    - persistance incomplète,
    - sécurité encore MVP,
    - runtime et configuration pas totalement alignés,
    - observabilité encore pensée dans un mode principalement synchrone.

La bonne stratégie n’est donc pas de tout reprendre brutalement, mais de **corriger dans le bon ordre**.  
Certaines dettes doivent être traitées avant les autres, sinon les corrections suivantes seront plus coûteuses, plus risquées ou devront être refaites.

---

## 2. Principes d’organisation temporelle

Avant d’entrer dans le détail des phases, il faut poser les principes d’enchaînement.

### 2.1 Stabiliser avant d’élargir
Il ne faut pas ajouter de nouvelles features majeures tant que :
- la configuration administrable peut encore subir des conflits d’écriture,
- la persistance technique est partiellement en mémoire,
- les responsabilités principales restent trop concentrées dans quelques services lourds.

Tant que ces points ne sont pas traités, toute nouvelle fonctionnalité risque :
- d’augmenter le couplage,
- de produire des effets de bord,
- ou de masquer des défauts structurels sous une couche fonctionnelle supplémentaire.

### 2.2 Corriger le socle avant la gouvernance fine
Il peut être tentant de commencer par la sécurité fine, les policies détaillées, ou la future industrialisation.
Ce serait prématuré si le modèle runtime, la persistance et la frontière entre configuration et exécution ne sont pas encore suffisamment stables.

La logique doit être :
1. rendre le système techniquement stable,
2. clarifier son modèle,
3. renforcer sa gouvernance,
4. industrialiser sa validation,
5. améliorer son confort d’exploitation.

### 2.3 Découper les lots pour éviter les refontes diffuses
Chaque étape doit être conçue comme un **lot ciblé**, avec :
- un périmètre clair,
- des dépendances connues,
- un résultat observable.

Il faut éviter les prompts de type :
- “refactorise toute l’admin”,
- “améliore la sécurité partout”,
- “nettoie l’architecture”.

À la place, il faut produire des lots comme :
- “ajoute le versioning optimiste sur la configuration administrable”,
- “extrait les responsabilités lecture tâches/traces de la TechnicalAdminFacade”,
- “introduit une persistance durable pour les interventions humaines”.

---

# Phase 1 — Stabilisation critique du socle

## 1.1 Finalité de la phase

Cette première phase a pour but de **faire disparaître les risques structurels immédiats** :
- conflits d’écriture sur la configuration,
- concentration excessive des responsabilités,
- pertes d’état au redémarrage,
- sécurité trop légère pour une interface admin.

Cette phase doit être réalisée avant toute évolution importante, car elle conditionne la stabilité de tout le reste.

---

## 1.2 Étape 1 — Fiabiliser la configuration administrable

### 1.2.1 Problème à traiter
Le rapport identifie un risque fort lié aux opérations de type read-modify-write sur la configuration administrable.  
Dans un tel modèle, deux actions concurrentes peuvent :
- lire un même état,
- le modifier séparément,
- puis écraser mutuellement leurs changements.

Cela crée des **lost updates** et rend la configuration non fiable.

### 1.2.2 Objectif
Mettre en place un mécanisme de contrôle de concurrence clair et explicite.

### 1.2.3 Travail à réaliser
- Introduire une notion de version sur les objets de configuration administrables.
- Exposer cette version dans les DTO d’administration.
- Lors d’un update :
    - vérifier que la version reçue correspond à la version actuelle,
    - refuser l’écriture si ce n’est pas le cas.
- Prévoir une remontée d’erreur propre :
    - message compréhensible,
    - code HTTP adapté,
    - possibilité pour l’UI de recharger l’état courant.
- Étudier si certaines opérations doivent devenir :
    - partielles,
    - atomiques,
    - spécialisées par domaine,
      plutôt qu’un gros remplacement global.

### 1.2.4 Sous-découpage recommandé
1. Ajouter la version dans le modèle de configuration.
2. Ajouter la propagation de version dans les endpoints et DTO.
3. Ajouter le contrôle d’écriture côté service.
4. Gérer l’erreur côté UI/admin.
5. Ajouter tests unitaires + tests d’intégration concurrence simple.

### 1.2.5 Pourquoi cette étape est en premier
Parce qu’elle protège toutes les modifications futures sur l’admin.  
Sans elle, même un découpage propre des services restera exposé à des corruptions silencieuses de configuration.

---

## 1.3 Étape 2 — Réduire la taille et la responsabilité de `TechnicalAdminFacade`

### 1.3.1 Problème à traiter
Le rapport pointe `TechnicalAdminFacade` comme un composant agrégateur trop large, avec un risque de dérive en “god facade”.  
Ce type de composant devient rapidement :
- difficile à comprendre,
- difficile à tester,
- fragile à chaque ajout de feature,
- source de dépendances croisées.

### 1.3.2 Objectif
Ramener la façade à un rôle de composition léger, ou la supprimer si elle n’a plus de valeur réelle.

### 1.3.3 Travail à réaliser
- Cartographier précisément les responsabilités actuelles de la façade :
    - tâches,
    - traces,
    - artefacts,
    - configuration,
    - rétention,
    - observabilité,
    - etc.
- Identifier ce qui relève de :
    - lecture,
    - commande,
    - agrégation,
    - adaptation UI.
- Extraire des services spécialisés par domaine.
- Réduire les dépendances croisées entre domaines techniques.
- Réévaluer si la façade doit :
    - subsister comme point d’entrée d’orchestration,
    - ou être remplacée par plusieurs services injectés selon les besoins.

### 1.3.4 Sous-découpage recommandé
1. Faire l’inventaire des méthodes et dépendances de la façade.
2. Grouper les méthodes par domaine fonctionnel.
3. Créer les services spécialisés :
    - `AdminTaskQueryService`
    - `TraceQueryService`
    - `ArtifactQueryService`
    - `AdminConfigService`
    - `RetentionService`
4. Déplacer les méthodes sans changer le comportement.
5. Alléger la façade ou supprimer les usages inutiles.
6. Mettre à jour les tests.
7. Vérifier l’absence de dépendances circulaires.

### 1.3.5 Point d’attention
Il ne faut pas faire un découpage purement cosmétique.  
Le bon critère n’est pas “plus de classes”, mais “moins de responsabilités mélangées”.

---

## 1.4 Étape 3 — Introduire une persistance durable minimale

### 1.4.1 Problème à traiter
Le rapport indique que plusieurs éléments techniques importants sont encore stockés en mémoire :
- tâches admin,
- interventions humaines,
- certains événements critiques,
- potentiellement une partie du bus ou des états techniques.

Cela signifie qu’un redémarrage peut faire perdre :
- l’historique,
- des traces utiles,
- un contexte d’exploitation,
- voire une partie du diagnostic.

### 1.4.2 Objectif
Mettre en place une persistance durable ciblée, sans chercher immédiatement une généralisation excessive.

### 1.4.3 Travail à réaliser
- Identifier les données techniques qui doivent survivre à un redémarrage.
- Prioriser les objets à persister :
    1. interventions humaines,
    2. tâches admin,
    3. traces ou événements critiques,
    4. éventuellement états de supervision.
- Définir des modèles de persistance simples et robustes.
- Introduire des repositories / stores durables.
- Définir une politique minimale de cycle de vie :
    - création,
    - lecture,
    - état,
    - expiration éventuelle,
    - purge future.
- Prévoir la compatibilité avec une future stratégie de rétention.

### 1.4.4 Sous-découpage recommandé
1. Lister les objets encore uniquement en mémoire.
2. Classer :
    - indispensable à persister,
    - utile mais non bloquant,
    - acceptable temporairement en mémoire.
3. Commencer par les interventions humaines.
4. Continuer avec les tâches admin.
5. Ajouter les événements critiques nécessaires au diagnostic.
6. Ajouter tests de redémarrage / rechargement.
7. Documenter ce qui reste volontairement en mémoire.

### 1.4.5 Pourquoi cette étape vient ici
Parce qu’un système admin sans persistance minimale n’est pas exploitable sérieusement.  
Et parce que les phases suivantes vont s’appuyer sur cette base durable.

---

## 1.5 Étape 4 — Durcir la sécurité admin MVP

### 1.5.1 Problème à traiter
La sécurité actuelle est jugée “correcte pour un MVP”, mais insuffisamment industrialisée :
- login/session sans durcissement avancé,
- token maître à gouverner,
- politique cookie à renforcer,
- secrets sans stratégie centralisée.

### 1.5.2 Objectif
Passer d’une sécurité “fonctionnelle” à une sécurité “déjà saine”, sans attendre la phase de gouvernance fine.

### 1.5.3 Travail à réaliser
- Ajouter un rate limiting sur la connexion admin.
- Vérifier et durcir la gestion de session :
    - `HttpOnly`,
    - `Secure`,
    - `SameSite=Strict`,
    - expiration cohérente,
    - invalidation propre.
- Clarifier le rôle et la durée de vie du token maître.
- Préparer la rotation de token.
- Vérifier les logs pour éviter la fuite de secrets ou d’artefacts sensibles.
- Préparer le terrain pour une intégration ultérieure avec un secret manager.

### 1.5.4 Sous-découpage recommandé
1. Audit rapide de l’existant sur login/session/token.
2. Durcissement cookie/session.
3. Ajout rate limiting.
4. Revue des logs et messages d’erreur.
5. Formalisation de la rotation token.
6. Tests d’intégration sécurité basique.

### 1.5.5 Point d’attention
Cette étape ne remplace pas la future phase de policy fine.  
Elle stabilise seulement la surface d’exposition immédiate.

---

## 1.6 Critère de sortie de la Phase 1
La Phase 1 peut être considérée comme terminée lorsque :
- la configuration ne peut plus être écrasée silencieusement,
- les services admin larges ont commencé à être réellement découpés,
- les états techniques critiques ne disparaissent plus au restart,
- l’accès admin est mieux protégé qu’un simple MVP.

---

# Phase 2 — Clarification du modèle runtime et des frontières système

## 2.1 Finalité de la phase

Une fois le socle stabilisé, la priorité devient la **compréhension du système**.  
Le rapport montre qu’une partie importante de la dette provient non pas d’un bug isolé, mais d’une **ambiguïté de modèle** :
- distinction partielle entre configuration et runtime,
- frontière floue entre définition d’agent et compte/runtime effectif,
- reload incomplet,
- vocabulaire parfois ambigu (`DurableObject`, durable vs persisted, etc.).

Cette phase vise donc à rendre l’architecture **lisible, prévisible et documentable**.

---

## 2.2 Étape 1 — Clarifier la frontière Agent Definition / Agent Runtime

### 2.2.1 Problème à traiter
Le rapport insiste sur la complexité de la frontière entre :
- ce qui décrit un agent en configuration,
- ce qui correspond à une instance runtime active,
- ce qui est réellement administrable,
- ce qui relève d’un compte, d’un binding Telegram, d’un provider LLM, etc.

Si cette frontière reste floue, chaque future feature risque d’être implémentée au mauvais niveau.

### 2.2.2 Objectif
Rendre explicites les deux mondes :
- **définition** : ce qui est configuré et stocké,
- **runtime** : ce qui est instancié, actif, résolu, câblé et exécuté.

### 2.2.3 Travail à réaliser
- Décrire précisément les concepts existants.
- Identifier les objets qui sont aujourd’hui mixtes ou ambigus.
- Définir une séparation de responsabilités :
    - définition déclarative,
    - état runtime,
    - compte / activation,
    - canaux associés,
    - capacités résolues.
- Revoir les DTO et noms de classes si nécessaire.
- Vérifier que l’UI reflète correctement cette séparation.

### 2.2.4 Sous-découpage recommandé
1. Écrire une cartographie des concepts.
2. Lister les classes / DTO ambigus.
3. Renommer ou restructurer si nécessaire.
4. Clarifier les flux :
    - création définition,
    - activation runtime,
    - binding Telegram,
    - reload,
    - désactivation.
5. Aligner l’admin web sur cette modélisation.
6. Documenter le tout sous forme d’ADR ou de document d’architecture.

### 2.2.5 Pourquoi cette étape est essentielle
Parce que beaucoup de futures corrections dépendent de cette distinction :
- reload runtime,
- sécurité contextuelle,
- observabilité,
- gestion Telegram,
- administration multi-agent.

---

## 2.3 Étape 2 — Définir une stratégie claire de reload runtime

### 2.3.1 Problème à traiter
Le rapport souligne que le rechargement runtime des configurations admin est incomplet.  
Cela crée une divergence possible entre :
- ce que voit l’UI,
- ce qui est réellement chargé en runtime.

### 2.3.2 Objectif
Définir une stratégie claire, explicite et assumée pour l’application des changements de configuration.

### 2.3.3 Travail à réaliser
- Décider du modèle cible :
    - reload manuel,
    - reload versionné,
    - reload à la demande,
    - reload event-driven,
    - redémarrage partiel par domaine.
- Définir quels composants sont reloadables à chaud :
    - agents,
    - providers LLM,
    - policies,
    - bots Telegram,
    - outils.
- Définir ce qui ne l’est pas.
- Rendre visibles les écarts d’état entre config et runtime.
- Ajouter une journalisation claire des événements de reload.

### 2.3.4 Sous-découpage recommandé
1. Dresser la liste des éléments aujourd’hui modifiables.
2. Classer :
    - hot reload possible,
    - reload différé,
    - redémarrage requis.
3. Définir un mécanisme d’application des changements.
4. Ajouter un état de version ou d’activation runtime.
5. Ajouter vues ou endpoints de diagnostic.
6. Ajouter tests de cohérence config/runtime.

### 2.3.5 Point d’attention
Le danger est de vouloir un hot reload total partout.  
Il vaut mieux une stratégie plus limitée mais claire qu’un faux “reload dynamique” partiellement fiable.

---

## 2.4 Étape 3 — Nettoyer le vocabulaire et les contrats techniques

### 2.4.1 Problème à traiter
Le rapport relève plusieurs incohérences :
- `DurableObject` dont le sens réel n’est pas parfaitement aligné avec une persistance effective,
- typos ou héritages structurels comme `controler`,
- variabilité de nommage (`agentIdent`, `agentId`, etc.).

Ces points peuvent sembler mineurs, mais ils augmentent la charge cognitive et rendent les prompts LLM moins fiables.

### 2.4.2 Objectif
Ramener le langage du code vers un vocabulaire plus précis, plus stable et plus homogène.

### 2.4.3 Travail à réaliser
- Lister les termes ambigus.
- Décider s’ils doivent être :
    - renommés,
    - documentés,
    - dépréciés progressivement.
- Uniformiser les noms d’identifiants.
- Corriger les packages ou classes manifestement hérités d’un ancien état du projet.
- Éviter les renommages massifs non nécessaires, mais traiter les zones structurantes.

### 2.4.4 Sous-découpage recommandé
1. Lister les incohérences de nommage.
2. Prioriser les plus structurantes.
3. Renommer d’abord les DTO / interfaces publics internes.
4. Continuer sur les packages et services.
5. Ajouter documentation / ADR.
6. Prévoir compatibilité temporaire si besoin.

### 2.4.5 Pourquoi cette étape ici
Parce qu’il vaut mieux clarifier le modèle avant d’ajouter davantage de règles de gouvernance ou d’automatisation.

---

## 2.5 Étape 4 — Préparer une observabilité compatible avec l’asynchrone

### 2.5.1 Problème à traiter
Le rapport note que la corrélation observabilité repose encore sur une logique de type `ThreadLocal`, adaptée à un flux synchrone simple, mais fragile si le système devient :
- asynchrone,
- distribué,
- multi-thread plus intensif,
- ou multi-agent plus riche.

### 2.5.2 Objectif
Préparer dès maintenant un modèle de corrélation robuste.

### 2.5.3 Travail à réaliser
- Identifier où la corrélation actuelle est injectée et consommée.
- Introduire un `correlationId` ou équivalent comme donnée explicite.
- Utiliser MDC pour l’enrichissement des logs.
- Prévoir la propagation dans les événements, appels de services et traitements différés.
- Éviter de dépendre implicitement du thread courant comme source unique de vérité.

### 2.5.4 Sous-découpage recommandé
1. Cartographier les usages `ThreadLocal`.
2. Définir un modèle de contexte de corrélation.
3. L’ajouter dans les flux critiques.
4. Brancher les logs MDC.
5. Tester la propagation sur plusieurs cas.
6. Documenter la stratégie.

### 2.5.5 Valeur de cette étape
Elle évite que le système se bloque plus tard sur une hypothèse invisible : “tout reste synchrone”.

---

## 2.6 Critère de sortie de la Phase 2
La Phase 2 est réussie quand :
- la distinction config / runtime est claire,
- la stratégie de reload est explicitement définie,
- le vocabulaire principal est stabilisé,
- l’observabilité n’est plus prisonnière d’une hypothèse strictement synchrone.

---

# Phase 3 — Gouvernance de sécurité et contrôle fin des capacités

## 3.1 Finalité de la phase

Une fois le système stabilisé et clarifié, il devient pertinent de renforcer la **gouvernance des actions**.  
Cette phase ne sert pas seulement à “sécuriser davantage”, mais à rendre les décisions d’accès :
- explicites,
- traçables,
- contextualisées,
- extensibles.

C’est particulièrement important dans un projet agentique où la question n’est pas seulement “qui est connecté ?”, mais aussi :
- quel agent peut faire quoi,
- dans quel contexte,
- avec quelle justification,
- sur quel périmètre.

---

## 3.2 Étape 1 — Formaliser le modèle de permission fine

### 3.2.1 Problème à traiter
Le rapport indique une gouvernance encore partiellement coarse-grained.  
Autrement dit, on sait parfois si quelqu’un ou quelque chose peut accéder à une zone, mais pas toujours avec suffisamment de finesse sur :
- l’action,
- le scope,
- le contexte,
- le type d’outil ou d’opération.

### 3.2.2 Objectif
Définir un cadre de permission structuré et uniforme.

### 3.2.3 Travail à réaliser
- Définir les dimensions de permission nécessaires :
    - sujet,
    - ressource,
    - action,
    - portée,
    - contexte,
    - canal éventuel.
- Éviter les règles éparpillées dans les contrôleurs.
- Centraliser l’évaluation des permissions.
- Distinguer :
    - permission technique,
    - permission métier,
    - permission liée au canal.

### 3.2.4 Sous-découpage recommandé
1. Cartographier les permissions existantes.
2. Identifier les règles implicites ou dispersées.
3. Définir un modèle cible de policy.
4. Introduire un service central d’évaluation.
5. Migrer progressivement les usages existants.
6. Ajouter des tests de décision.

### 3.2.5 Point d’attention
Il faut éviter de partir trop vite sur un moteur ultra-complexe.  
Le premier objectif est la cohérence, pas la sophistication maximale.

---

## 3.3 Étape 2 — Ajouter un audit décisionnel

### 3.3.1 Problème à traiter
Une policy fine perd beaucoup de valeur si ses décisions sont invisibles.  
Or le rapport insiste sur le besoin d’audit.

### 3.3.2 Objectif
Enregistrer et exposer les décisions d’autorisation importantes.

### 3.3.3 Travail à réaliser
- Définir quels événements doivent être audités :
    - autorisation accordée,
    - refus,
    - décision partielle,
    - fallback,
    - bypass éventuel.
- Enregistrer :
    - sujet,
    - ressource,
    - action,
    - contexte,
    - décision,
    - motif,
    - timestamp,
    - corrélation.
- Prévoir un format exploitable pour diagnostic et sécurité.
- Ajouter un minimum de consultation côté admin technique.

### 3.3.4 Sous-découpage recommandé
1. Définir les événements d’audit.
2. Définir le schéma de stockage.
3. Brancher la journalisation des décisions.
4. Ajouter un service de lecture.
5. Ajouter filtre/recherche minimum.
6. Tester la cohérence.

### 3.3.5 Valeur de cette étape
Elle rend le système compréhensible en exploitation, ce qui est vital quand les règles deviennent plus riches.

---

## 3.4 Étape 3 — Renforcer la gestion des secrets

### 3.4.1 Problème à traiter
Le rapport souligne que la gestion des secrets reste locale et sans stratégie coffre forte clairement établie.

### 3.4.2 Objectif
Préparer un modèle propre pour les environnements multiples.

### 3.4.3 Travail à réaliser
- Identifier tous les secrets manipulés :
    - tokens,
    - clés API,
    - credentials éventuels,
    - secrets Telegram,
    - clés providers.
- Définir leur niveau de criticité.
- Créer une abstraction de récupération de secret.
- Préparer une intégration future avec Vault / KMS / secret manager.
- Réduire la présence des secrets dans :
    - config claire,
    - logs,
    - dumps,
    - erreurs.

### 3.4.4 Sous-découpage recommandé
1. Inventaire des secrets.
2. Classification par criticité.
3. Abstraction d’accès.
4. Remplacement des accès directs.
5. Préparation de l’intégration manager externe.
6. Documentation exploitation.

### 3.4.5 Point d’attention
Le plus important n’est pas de brancher tout de suite un outil externe, mais de **ne plus enfermer l’architecture dans un modèle local rigide**.

---

## 3.5 Étape 4 — Isoler proprement la sécurité des canaux, notamment Telegram

### 3.5.1 Problème à traiter
Le rapport mentionne encore des couplages Telegram dans certains points d’entrée, avec une logique de sécurité ou de règles potentiellement mêlée au reste.

### 3.5.2 Objectif
Faire de Telegram un canal intégré, mais non envahissant architecturalement.

### 3.5.3 Travail à réaliser
- Identifier les règles spécifiques Telegram actuellement dispersées.
- Introduire une policy inbound dédiée au canal Telegram.
- Séparer :
    - authentification/identification du canal,
    - autorisation d’accès,
    - résolution de l’agent cible,
    - règles spécifiques de sécurité.
- Éviter les “cas spéciaux Telegram” dans les couches génériques.

### 3.5.4 Sous-découpage recommandé
1. Audit du couplage actuel Telegram.
2. Définition d’une policy d’entrée canal.
3. Déplacement des règles spécifiques.
4. Alignement avec la policy globale.
5. Tests inbound Telegram.

### 3.5.5 Pourquoi cette étape est importante
Parce que plus le projet grandira, plus les canaux risquent de se multiplier.  
Telegram doit devenir un cas supporté, pas une exception structurelle.

---

## 3.6 Critère de sortie de la Phase 3
La Phase 3 est réussie lorsque :
- les permissions ne sont plus implicites ou dispersées,
- les décisions sont auditées,
- les secrets ne sont plus gérés de manière ad hoc,
- les règles Telegram sont intégrées proprement au modèle général.

---

# Phase 4 — Industrialisation de la qualité et des validations

## 4.1 Finalité de la phase

Cette phase vise à réduire le risque de régression et à améliorer la capacité à faire évoluer le projet sereinement.  
Le rapport insiste sur :
- de nombreuses corrections “après relecture”,
- des validations parfois limitées,
- un build/test pas toujours parfaitement verrouillé selon l’environnement.

Il faut donc transformer le projet en système **plus testable, plus prévisible et plus reproductible**.

---

## 4.2 Étape 1 — Renforcer les tests d’intégration bout-en-bout

### 4.2.1 Problème à traiter
Beaucoup de tests semblent ciblés par lot, ce qui est utile mais insuffisant pour des parcours transverses.

### 4.2.2 Objectif
Couvrir les flux critiques réels.

### 4.2.3 Travail à réaliser
Définir des scénarios d’intégration pour les parcours les plus structurants :
- modification de configuration admin,
- sécurité d’accès,
- persistance d’événements / tâches,
- cohérence config/runtime,
- lecture d’observabilité,
- cycle d’activation d’un agent,
- flux Telegram ou autre entrée principale.

### 4.2.4 Sous-découpage recommandé
1. Sélectionner les 5 à 10 parcours critiques.
2. Définir pour chacun :
    - état initial,
    - action,
    - état attendu,
    - effet secondaire attendu.
3. Écrire les tests d’intégration.
4. Ajouter jeux de données réalistes.
5. Vérifier stabilité et lisibilité des tests.

### 4.2.5 Point d’attention
Il vaut mieux peu de tests transverses mais très pertinents, plutôt qu’un grand nombre de tests superficiels.

---

## 4.3 Étape 2 — Standardiser l’environnement de build et de validation

### 4.3.1 Problème à traiter
Le rapport indique que le build/test est parfois bloqué ou perturbé par des questions d’environnement, notamment Java 21 ou des différences locales.

### 4.3.2 Objectif
Rendre le build reproductible et stable sur tous les environnements de développement ou CI.

### 4.3.3 Travail à réaliser
- Fixer clairement la version Java cible et les contraintes associées.
- Uniformiser la chaîne Maven/Gradle si nécessaire.
- Vérifier les profils, variables, dépendances implicites.
- Ajouter une documentation de setup simple.
- Mettre en place un pipeline CI représentant réellement la cible attendue.

### 4.3.4 Sous-découpage recommandé
1. Définir l’environnement de référence.
2. Vérifier le build local propre.
3. Vérifier le build CI.
4. Corriger les écarts.
5. Ajouter doc d’installation / validation.
6. Ajouter vérifications automatiques minimales.

### 4.3.5 Valeur de cette étape
Elle réduit la friction quotidienne et améliore la fiabilité des lots produits par un LLM.

---

## 4.4 Étape 3 — Introduire un cadre d’architecture exécutable

### 4.4.1 Problème à traiter
Le rapport mentionne des corrections récurrentes de couplage et une dette de stabilisation liée à l’absence de garde-fous plus systématiques.

### 4.4.2 Objectif
Créer des mécanismes qui empêchent le projet de retomber dans les mêmes dérives.

### 4.4.3 Travail à réaliser
- Définir des règles d’architecture simples :
    - dépendances autorisées,
    - séparation DTO internes / UI,
    - contrôleurs sans logique métier,
    - politiques centralisées,
    - pas de couplage canal dans le cœur générique.
- Documenter ces règles.
- Ajouter si possible des tests d’architecture ou de convention.
- Créer une checklist de revue de code.

### 4.4.4 Sous-découpage recommandé
1. Rédiger les règles d’architecture.
2. Créer une checklist PR.
3. Ajouter tests d’architecture si pertinent.
4. Aligner progressivement les zones existantes.
5. Documenter les exceptions justifiées.

### 4.4.5 Pourquoi cette étape est structurante
Parce qu’elle transforme la qualité en processus, au lieu de dépendre uniquement des relectures humaines a posteriori.

---

## 4.5 Étape 4 — Structurer les ADR et la documentation de décisions

### 4.5.1 Problème à traiter
Beaucoup de choix semblent avoir évolué au fil des corrections, ce qui peut être sain, mais devient coûteux si les raisons des décisions ne sont pas conservées.

### 4.5.2 Objectif
Éviter de redébattre sans cesse les mêmes sujets.

### 4.5.3 Travail à réaliser
- Créer un format ADR simple.
- Documenter les décisions structurantes :
    - modèle definition/runtime,
    - reload,
    - policies,
    - persistance technique,
    - sécurité admin,
    - observabilité.
- Garder les ADR courts mais précis.

### 4.5.4 Sous-découpage recommandé
1. Créer template ADR.
2. Lister les décisions déjà prises.
3. Écrire les ADR les plus critiques.
4. Référencer les ADR dans le code/doc projet.
5. Ajouter une habitude de création lors des futurs choix structurants.

### 4.5.5 Valeur de cette étape
Très forte dans un projet agentique, où les concepts évoluent souvent plus vite que le code.

---

## 4.6 Critère de sortie de la Phase 4
La Phase 4 est aboutie quand :
- les parcours critiques sont couverts par de vrais tests d’intégration,
- l’environnement de build est reproductible,
- les règles d’architecture sont explicites,
- les décisions structurantes ne reposent plus uniquement sur la mémoire du moment.

---

# Phase 5 — Consolidation fonctionnelle, exploitation et qualité d’usage

## 5.1 Finalité de la phase

Cette phase arrive volontairement après les autres.  
Elle ne vise pas à “faire joli”, mais à rendre le système :
- plus agréable à exploiter,
- plus lisible pour l’admin,
- plus cohérent dans son expérience.

Le rapport montre que la dette UI existe, mais qu’elle est secondaire par rapport aux dettes de socle.  
Il est donc pertinent de l’aborder en dernier dans ce cycle.

---

## 5.2 Étape 1 — Unifier l’interface admin

### 5.2.1 Problème à traiter
Le rapport signale une cohérence visuelle encore incomplète :
- `login.html` hors thème central,
- fragments pas totalement unifiés,
- navigation potentiellement dupliquée.

### 5.2.2 Objectif
Obtenir une interface admin homogène, simple à faire évoluer.

### 5.2.3 Travail à réaliser
- Revoir l’ensemble des pages admin.
- Centraliser :
    - layout,
    - fragments,
    - thème Marcel,
    - navigation,
    - style des tableaux et vues de détail.
- Réduire les duplications UI.
- Uniformiser les comportements de filtre, pagination et navigation.

### 5.2.4 Sous-découpage recommandé
1. Cartographie des pages et fragments.
2. Définition d’un layout unique.
3. Unification de la navigation.
4. Harmonisation login / pages techniques / vues de détail.
5. Revue visuelle globale.

---

## 5.3 Étape 2 — Améliorer les vues techniques d’exploitation

### 5.3.1 Problème à traiter
Le rapport note que pagination, tri, détails et diagnostics restent parfois limités pour les pages techniques.

### 5.3.2 Objectif
Rendre l’admin réellement utile en exploitation.

### 5.3.3 Travail à réaliser
- Ajouter pagination cohérente.
- Ajouter tri et filtres standardisés.
- Ajouter vues détail sur :
    - tâches,
    - traces,
    - événements,
    - audits,
    - interventions humaines.
- Ajouter liens croisés utiles entre objets techniques corrélés.

### 5.3.4 Sous-découpage recommandé
1. Définir les listes critiques à améliorer.
2. Standardiser pagination/tri/filtre.
3. Ajouter vues détail.
4. Ajouter liens de navigation croisée.
5. Vérifier la lisibilité en cas de gros volume.

### 5.3.5 Valeur de cette étape
Elle augmente fortement la capacité de diagnostic sans toucher au cœur de l’architecture.

---

## 5.4 Étape 3 — Réduire la dépendance aux CDN et préparer une politique CSP

### 5.4.1 Problème à traiter
Le rapport relève une dépendance CDN pour Bootstrap, jQuery, fonts, etc., sans stratégie de fallback ni politique CSP claire.

### 5.4.2 Objectif
Rendre l’UI plus robuste, plus sécurisable et plus déployable dans des environnements contraints.

### 5.4.3 Travail à réaliser
- Identifier les dépendances externes front.
- Préparer une stratégie d’assets locaux.
- Introduire progressivement une CSP réaliste.
- Vérifier que l’interface reste fonctionnelle hors dépendances externes.

### 5.4.4 Sous-découpage recommandé
1. Inventaire des assets externes.
2. Internalisation progressive.
3. Mise en place d’une CSP compatible.
4. Ajustements JS/CSS si besoin.
5. Tests de rendu et de chargement.

### 5.4.5 Pourquoi cette étape arrive ici
Parce qu’elle améliore la maturité d’exploitation, mais n’est pas prioritaire tant que le socle technique n’est pas stabilisé.

---

## 5.5 Étape 4 — Finaliser l’admin Telegram et les cycles de vie associés

### 5.5.1 Problème à traiter
Le rapport indique que Telegram admin reste encore read-only côté web, et que le cycle de vie complet n’est pas totalement industrialisé.

### 5.5.2 Objectif
Permettre une vraie administration fonctionnelle du canal Telegram.

### 5.5.3 Travail à réaliser
- Introduire une source de configuration Telegram administrable et persistée.
- Permettre activation / désactivation cohérente.
- Aligner le runtime Telegram avec la stratégie de reload définie en Phase 2.
- Afficher clairement l’état courant :
    - configuré,
    - actif,
    - suspendu,
    - en erreur,
    - en attente de reload.

### 5.5.4 Sous-découpage recommandé
1. Clarifier le modèle Telegram administrable.
2. Persister la configuration.
3. Ajouter endpoints + UI d’édition.
4. Ajouter cycle d’activation.
5. Ajouter diagnostic runtime Telegram.

### 5.5.5 Valeur de cette étape
Elle complète l’industrialisation d’un canal clé du projet.

---

## 5.6 Critère de sortie de la Phase 5
La Phase 5 est terminée quand :
- l’admin est visuellement cohérente,
- les pages techniques sont réellement exploitables,
- l’UI n’est plus dépendante de choix fragiles d’assets,
- Telegram peut être administré proprement du point de vue fonctionnel et runtime.

---

# 6. Ordonnancement recommandé

## 6.1 Enchaînement global
Ordre recommandé :

1. **Phase 1 — Stabilisation critique**
2. **Phase 2 — Clarification du modèle**
3. **Phase 3 — Gouvernance et sécurité fine**
4. **Phase 4 — Industrialisation qualité**
5. **Phase 5 — Exploitation et confort d’usage**

Cet ordre est important car chaque phase prépare la suivante :
- la Phase 1 réduit les risques techniques immédiats,
- la Phase 2 donne un modèle clair,
- la Phase 3 applique une gouvernance sur ce modèle clarifié,
- la Phase 4 fiabilise les validations,
- la Phase 5 améliore l’exploitation sans remettre en cause le socle.

---

## 6.2 Lots prioritaires absolus
Si un arbitrage est nécessaire, les priorités absolues sont :

1. Contrôle de concurrence de la config administrable
2. Découpage des services trop larges
3. Persistance technique minimale
4. Durcissement sécurité admin
5. Clarification Agent Definition / Runtime
6. Stratégie explicite de reload
7. Observabilité hors dépendance ThreadLocal
8. Policy fine + audit
9. Tests d’intégration critiques
10. Finalisation exploitation / UI

---

# 7. Format d’exploitation pour un LLM

Pour réinjecter cette roadmap à un LLM, il est conseillé de l’utiliser :
- **phase par phase**,
- **étape par étape**,
- avec un objectif de lot très précis.

Exemple de bon découpage :
- Prompt 1 : versioning optimiste config admin
- Prompt 2 : extraction des services depuis `TechnicalAdminFacade`
- Prompt 3 : persistance des interventions humaines
- Prompt 4 : persistance des tâches admin
- Prompt 5 : durcissement login/session/token
- Prompt 6 : clarification Agent Definition / Runtime
- Prompt 7 : stratégie de reload
- etc.

Il faut éviter les prompts trop vastes qui mélangent :
- architecture,
- persistance,
- sécurité,
- UI,
- runtime.

Le plus efficace sera de traiter cette roadmap comme une **suite de chantiers indépendants mais ordonnés**.

---

# 8. Conclusion

Le rapport de dette technique montre un projet qui a déjà beaucoup gagné en structure, mais qui est arrivé à un point charnière.  
Le risque principal n’est pas un défaut isolé ; c’est la combinaison de plusieurs dettes moyennes qui, ensemble, peuvent freiner fortement l’évolution du produit.

La bonne réponse n’est donc pas une refonte brutale, mais une **consolidation méthodique** :
- d’abord stabiliser,
- ensuite clarifier,
- puis gouverner,
- ensuite industrialiser,
- enfin améliorer l’exploitation.

Cette roadmap suit précisément cette logique et peut servir de base à :
- un plan de développement,
- une série de prompts Codex,
- un backlog de consolidation,
- ou un cadrage d’architecture plus formel.