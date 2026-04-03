Tu vas implémenter la phase 1.A de mon projet “Toolkit-Bridge”.

Contexte :
Le cadre global du projet et la logique d’implémentation incrémentale te sont fournis séparément.
Tu dois t’y conformer strictement.

Objectif de cette phase :
poser le socle du runtime multi-agent en complétant le modèle d’agent autour de ses composants obligatoires, et en introduisant proprement la notion de rôle.

Cette phase ne doit pas encore implémenter :
- le registre central complet des runtimes
- un cycle de vie runtime avancé
- la supervision complète
- la délégation effective
- le task model complet

Le but est de structurer le modèle, pas d’implémenter tout le comportement futur.

Périmètre attendu :

1. Compléter le modèle d’agent
   Je veux qu’un agent soit clairement modélisé comme une entité de runtime cohérente, avec au minimum :
- un orchestrator propre à l’agent
- une mémoire accessible selon ses scopes
- un set de tools autorisés ou un accès encadré aux tools
- une policy
- un workspace

Attention :
- rester cohérent avec la phase 0 déjà posée
- ne pas dupliquer inutilement ce qui a déjà été introduit
- clarifier les responsabilités sans créer un framework interne trop lourd

2. Introduire la notion de rôle
   Je veux qu’un agent possède un rôle fonctionnel explicite.
   Ce rôle doit :
- être porté par le modèle d’agent ou de runtime selon ce qui est le plus sain
- être exploitable plus tard pour la délégation par rôle
- préparer un futur équilibrage de charge sans l’implémenter maintenant

Important :
- le rôle doit être un vrai concept métier, pas juste une string magique disséminée partout
- garder un modèle simple, clair et extensible

3. Clarifier les points d’accès aux sous-systèmes de l’agent
   Je veux que le modèle rende explicite comment l’agent accède à :
- son orchestrator
- sa mémoire
- ses tools
- sa policy
- son workspace

Le but est d’éviter un assemblage implicite dispersé dans plusieurs services.

4. Préparer les futures extensions sans les implémenter
   Cette phase doit préparer proprement :
- le futur registre des runtimes
- l’état d’exécution agent
- la délégation par rôle
- les tasks
- la supervision

Mais il ne faut pas implémenter prématurément ces comportements.

Attendus techniques :
- créer ou adapter les classes/interfaces nécessaires
- garder un code lisible et testable
- conserver des noms de classes en anglais
- rester cohérent avec Spring Boot et l’architecture actuelle du projet
- ajouter ou adapter les tests pertinents

Ce que je veux explicitement éviter :
- rôle codé en dur dans plusieurs endroits
- mélange confus entre définition statique et runtime vivant
- ajout de logique de délégation réelle
- ajout d’un registry trop tôt
- sur-ingénierie

Livrables attendus :
1. code complet de la phase 1.A
2. tests
3. résumé court des choix de modélisation
4. liste des fichiers modifiés/créés
5. points volontairement laissés ouverts pour la phase 1.B

Avant de coder :
- rappelle brièvement ton plan d’implémentation en 5 à 10 étapes maximum
- puis implémente