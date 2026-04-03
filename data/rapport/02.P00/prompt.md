Tu vas implémenter la phase 0 de mon projet “Toolkit-Bridge”.

Contexte :
Je construis une plateforme agentique générique en Java Spring, pensée pour évoluer vers :
- multi-agent
- mémoire avancée
- orchestrators multiples
- tasks et sous-tâches
- communication inter-agent
- tools natifs et scriptés
- policies
- supervision
- futures capacités de débat inter-agent et auto-amélioration

Cette phase 0 ne doit pas implémenter toutes les fonctionnalités finales.
Son but est de figer proprement les principes d’architecture et de poser les premières briques structurelles pour éviter de gros refactorings plus tard.

Objectif de la phase 0 :
poser un socle d’architecture propre autour du runtime agentique et des contrats entre sous-systèmes.

Périmètre attendu :

1. Formaliser la notion de `AgentRuntime`
   Je veux un vrai runtime agentique avec état, distinct de la simple configuration.
   Ce runtime doit porter au minimum :
- l’identité ou définition de l’agent
- son rôle
- son orchestrator
- sa mémoire accessible
- ses tools autorisés ou leur accès
- sa policy
- son workspace
- son état d’exécution minimal si pertinent

Attention :
- ne pas tout rendre “vivant” artificiellement si le projet n’est pas encore à ce stade
- mais introduire le bon objet central pour l’avenir

2. Séparer clairement configuration statique et runtime
   Je veux éviter qu’un agent soit seulement un assemblage implicite de services.
   Il faut distinguer :
- ce qui relève de la définition/configuration
- ce qui relève de l’exécution et de l’état courant

3. Stabiliser les contrats entre grands sous-systèmes
   Je veux que les frontières deviennent plus explicites entre :
- orchestrator
- mémoire
- tooling
- policy
- workspace
- runtime agentique

Il ne s’agit pas forcément de tout réécrire.
Mais je veux que les dépendances aillent dans une direction saine et que le projet prépare les futures phases.

4. Préparer les futures extensions sans les implémenter complètement
   Cette phase doit préparer les futures briques suivantes, sans les réaliser entièrement :
- task model
- bus inter-agent
- traces agentiques
- policy plus fine
- outils mémoire
- orchestrator de type task

Donc :
- introduire les bons points d’accroche
- éviter les abstractions inutiles
- mais ne pas bloquer les phases suivantes

5. Conserver l’existant autant que possible
   Je veux éviter une refonte brutale.
   Le code existant doit être réorganisé proprement si nécessaire, mais sans casser inutilement ce qui fonctionne déjà.

Attendus techniques :
- créer ou adapter les objets/classes/interfaces nécessaires
- améliorer le découplage si besoin
- clarifier les responsabilités
- garder les noms en anglais
- rester cohérent avec Spring Boot
- ajouter ou adapter les tests pertinents

Ce que je veux explicitement éviter :
- sur-engineering
- framework interne inutilement complexe
- architecture trop abstraite pour rien
- implémentation prématurée de features de phase suivante
- régression de l’existant

Livrables attendus :
1. code complet de la phase
2. tests
3. résumé court des choix d’architecture
4. liste des fichiers modifiés/créés
5. points restant volontairement ouverts pour les phases suivantes

Important :
si tu hésites entre une solution “rapide” et une solution “structurante mais simple”, privilégie la seconde.
Le but principal est de poser une base saine, facile à maintenir et solide.