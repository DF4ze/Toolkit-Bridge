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