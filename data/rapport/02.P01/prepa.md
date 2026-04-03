Avant d’implémenter la phase 0, analyse le code existant du projet Toolkit-Bridge.

Je veux que tu identifies :
1. Comment les agents sont actuellement représentés
2. Où se trouvent les responsabilités suivantes :
    - orchestrator
    - mémoire
    - tools
    - workspace
    - configuration agent
3. Les points de couplage actuels
4. Les limites de l’architecture actuelle pour évoluer vers :
    - un AgentRuntime complet
    - multi-agent
    - task model
    - bus inter-agent

Ensuite propose :
- un plan d’implémentation en 5 à 10 étapes maximum
- les classes à créer ou modifier
- les risques de refactor

N’implémente rien encore.