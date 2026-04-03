Tu vas implémenter la phase 1.B de mon projet “Toolkit-Bridge”.

Contexte :
Le cadre global du projet, la logique d’implémentation incrémentale, ainsi que la phase 1.A déjà réalisée, te sont fournis séparément.
Tu dois t’y conformer strictement et t’appuyer sur la modélisation déjà en place.

Objectif de cette phase :
introduire le cycle de vie runtime minimal d’un agent et centraliser l’accès aux runtimes via un registre ou manager dédié.

Cette phase ne doit pas encore implémenter :
- un système complet de task orchestration
- la délégation réelle entre agents
- un bus inter-agent
- un système avancé de supervision
- des métriques complexes
- un dashboard

Le but est de poser un runtime vivant minimal, propre et centralisé.

Périmètre attendu :

1. Introduire un état d’exécution agent
   Je veux un état runtime minimal permettant de suivre au moins :
- disponibilité
- occupation
- tâche courante si pertinent
- contexte actif si pertinent

Attention :
- rester simple
- ne pas inventer tout de suite une machine à états trop lourde
- préparer les futures métriques et la supervision sans les implémenter complètement

2. Centraliser l’accès aux runtimes
   Je veux un registre ou manager des runtimes permettant :
- d’accéder aux agents runtime de manière centralisée
- d’éviter les accès directs dispersés
- de préparer les futures évolutions :
    - supervision
    - résolution par rôle
    - sélection d’agent disponible
    - délégation

Important :
- le registre ne doit pas embarquer prématurément toute la logique future
- il doit surtout devenir le point d’entrée propre vers les runtimes

3. Préparer la future gestion de charge
   Sans l’implémenter réellement, cette phase doit préparer :
- recherche future par rôle
- sélection future d’un agent disponible
- exposition future d’informations de supervision

4. Garder le design simple et maintenable
   Je veux éviter :
- un manager omniscient qui fait tout
- un état runtime trop verbeux
- des accès concurrents mal pensés si le code en suggère déjà
- de la logique métier disséminée hors du registre/runtime

Attendus techniques :
- créer ou adapter les classes nécessaires
- connecter proprement la phase 1.A
- garder des responsabilités nettes
- ajouter ou adapter les tests pertinents
- documenter brièvement les choix structurants si utile

Ce que je veux explicitement éviter :
- implémentation anticipée du bus
- implémentation anticipée des tasks complètes
- logique de délégation réelle
- système de monitoring trop avancé
- sur-conception

Livrables attendus :
1. code complet de la phase 1.B
2. tests
3. résumé court des choix de runtime et registry
4. liste des fichiers modifiés/créés
5. points volontairement laissés ouverts pour la phase 2

Avant de coder :
- rappelle brièvement ton plan d’implémentation en 5 à 10 étapes maximum
- puis implémente