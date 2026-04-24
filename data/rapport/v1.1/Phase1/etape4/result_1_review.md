# v1.1 / Phase 1 / Étape 4 / Lot 1 — Review d’architecture

## 1. Constat global

Le lot est **globalement propre et conforme au périmètre**:
- correction ciblée des 2 fuites critiques (`AdminTokenService`, `AgentAccountInitializer`)
- ajout d’un utilitaire simple (`SensitiveDataMasker`) sans sur-architecture
- aucun impact sur orchestrator / mémoire / tooling / policy / workspace
- tests ciblés utiles et verts

Aucune dette structurelle majeure n’a été introduite.

---

## 2. Faiblesses / points discutables

### [MOYEN] Couverture de capture logs potentiellement partielle
- Fichiers:
  - `src/test/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenServiceTest.java:75-76`
  - `src/test/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializerTest.java:65-66`
- Observation:
  - les assertions vérifient uniquement `output.getOut()`.
- Risque:
  - si la configuration de logging route les logs vers `stderr`, le test peut passer sans réellement vérifier la sortie concernée.

### [FAIBLE] Maintien d’un suffixe de secret en logs
- Fichiers:
  - `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenService.java:54`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializer.java:52-54`
- Observation:
  - le masquage conserve les 4 derniers caractères.
- Risque:
  - faible, mais expose une information dérivée du secret.
  - acceptable fonctionnellement pour le lot, discutable selon politique sécurité cible.

### [FAIBLE] Positionnement utilitaire (discussion d’organisation)
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/security/SensitiveDataMasker.java`
- Observation:
  - utilitaire statique placé dans `security`, utilisé depuis `security.admin` et `service.auth`.
- Risque:
  - faible: pas de couplage problématique, mais ce composant deviendra probablement transversal aux logs applicatifs et pourrait migrer plus tard vers un package de support logging.

---

## 3. Corrections utiles proposées (sans élargir le périmètre)

1. Renforcer les tests de logs
- Remplacer `output.getOut()` par `output.getAll()` dans les 2 tests de non-divulgation.
- Bénéfice: robustesse face au backend de logging.

2. Option de durcissement minimal
- Si la politique sécurité est “zéro dérivé”, retirer complètement la valeur masquée des logs critiques et ne garder qu’un message sans token/apiKey.
- Bénéfice: réduction maximale du risque de corrélation.

3. Documentation locale
- Ajouter un court commentaire JavaDoc sur `SensitiveDataMasker` indiquant que c’est destiné à l’observabilité et non à la cryptographie.
- Bénéfice: clarification d’intention.

---

## 4. Vérification demandée (checklist architecture)

### Séparation configuration / runtime
- **OK**: aucun mélange nouveau introduit.
- Le changement reste dans la couche runtime logging des services concernés.

### Qualité du modèle implémenté
- **OK**: modèle minimal, explicite, déterministe.
- `SensitiveDataMasker` répond exactement à la règle métier demandée.

### Découplage orchestrator / mémoire / tooling / policy / workspace
- **OK**: inchangé; aucune dépendance ajoutée vers ces sous-systèmes.

### Absence de logique ad hoc
- **Globalement OK**: logique simple, centralisée dans un utilitaire unique.

### Absence de couplage gênant pour phases futures
- **OK**: couplage faible (appel statique utilitaire).

### Cohérence des noms
- **OK**: `SensitiveDataMasker` est explicite et cohérent.

### Lisibilité générale
- **OK**: diff court, intention claire, impact local.

### Tests réellement utiles
- **OK avec réserve**: bons tests ciblés sur la non-divulgation + non-régression login/token.
- Réserve: utiliser `getAll()` pour éviter faux positifs selon stream de log.

### Dette technique introduite
- **Faible**: pas de dette significative.

### Risques de refactor futur évitables maintenant
- principal risque évitable immédiatement: robustesse des tests de capture logs (cf. correction 1).

---

## 5. Résumé final

Implémentation **propre, minimale, conforme au périmètre strict** et adaptée à la phase.

Points à traiter en priorité faible:
1. fiabiliser les assertions de logs (`getAll()`)
2. arbitrer si la politique cible accepte les suffixes masqués ou préfère absence totale de valeur

En dehors de ces points, le lot peut être considéré comme **architecturalement sain** pour la suite.
