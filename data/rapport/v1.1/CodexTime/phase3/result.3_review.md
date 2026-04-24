# Revue critique d’architecture — Phase 3 Étape 3

## Périmètre relu
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalReviewStepTest.java`

## 1. Faiblesses / points discutables

### [P1] Risque de faux positif `WAIT_HUMAN` via concaténation stdout/stderr
Constat :
- La décision est détectée sur `reviewResultContent` produit par `buildResultContent(...)`.
- Ce contenu agrège stdout + section `[stderr]`.
- Si `DECISION: WAIT_HUMAN` apparaît en stderr (bruit outil, message intermédiaire), la step bascule en `WAIT_HUMAN` même si la sortie métier ne le demandait pas.

Impact :
- Décision de workflow potentiellement incorrecte.
- Couplage implicite entre format de logs techniques et décision métier.

Localisation :
- `GlobalReviewStep.java` lignes ~92, ~165-170, ~117-119.

### [P2] Round-trip disque inutile pour la décision (fragilité et coût)
Constat :
- La step écrit `REVIEW_RESULT`, puis relit immédiatement le fichier pour décider (`persistedReviewResult`).
- Le contenu en mémoire (`reviewResultContent`) est déjà disponible.

Impact :
- I/O supplémentaire sans gain fonctionnel immédiat.
- Point de défaillance additionnel (lecture disque) dans le chemin nominal.

Localisation :
- `GlobalReviewStep.java` lignes ~101-102 puis ~117.

### [P3] Couverture de test encore partielle sur la robustesse de la convention
Constat :
- Les tests ajoutés couvrent bien les cas demandés (`WAIT_HUMAN` / `CONTINUE`).
- Il manque des tests de robustesse minimaux autour de la convention :
  - `WAIT_HUMAN` sans `WAIT_REASON` (vérifier absence de clé),
  - marqueur présent uniquement en stderr (protection anti faux positif),
  - présence de bruit autour de la convention.

Impact :
- Risque de régression non détectée si format de sortie Codex évolue.

Localisation :
- `GlobalReviewStepTest.java`.

## 2. Corrections utiles proposées (sans nouvelle fonctionnalité)

### Correction 1 (prioritaire)
Décorréler la décision métier du stderr.
- Détecter `DECISION: WAIT_HUMAN` sur la sortie métier seule (stdout),
- Conserver stderr uniquement pour observabilité (artefact), pas pour la décision.

Bénéfice :
- Réduit le couplage ad hoc logs -> décision.
- Stabilise le comportement pour les phases futures.

### Correction 2
Éviter la relecture immédiate de l’artefact pour décider.
- Décider à partir du contenu déjà calculé en mémoire,
- Conserver l’écriture d’artefact pour traçabilité.

Bénéfice :
- Moins de fragilité runtime,
- Chemin nominal plus simple et lisible.

### Correction 3
Compléter légèrement les tests sans élargir le scope.
- Ajouter un test `WAIT_HUMAN` sans `WAIT_REASON`,
- Ajouter un test anti faux positif lié au stderr.

Bénéfice :
- Sécurise la convention minimale introduite à cette étape.

## 3. Vérification des axes demandés

### Séparation configuration / runtime
- Conforme : pas d’introduction de config dynamique ni de couplage config/règles.
- La logique reste dans la couche runtime de step.

### Qualité du modèle implémenté
- Bonne sobriété : usage de `WorkflowStepResult` existant, sans nouveau modèle.
- Convention textuelle explicite et lisible.

### Découplage orchestrator / mémoire / tooling / policy / workspace
- Conforme au périmètre : orchestrator non touché.
- Pas de couplage ajouté avec mémoire/policy/workspace.
- Dépendance au tooling limitée à `CodexWorkflowClient` déjà existante.

### Absence de logique ad hoc excessive
- Globalement correcte.
- Point à corriger : décision métier actuellement sensible au format agrégé stdout/stderr.

### Couplages gênants pour les futures phases
- Aucun couplage structurel majeur introduit.
- Le faux positif possible sur stderr peut compliquer une future reprise humaine fiable.

### Cohérence des noms
- Noms clairs et cohérents (`WAIT_HUMAN_MESSAGE`, `WAIT_HUMAN_MARKER`, `extractWaitReason`).

### Lisibilité générale
- Bonne lisibilité globale.
- Une simplification possible : éviter la variable `persistedReviewResult` pour la décision.

### Tests réellement utiles
- Oui, les deux tests ajoutés valident exactement le comportement demandé.
- Compléments conseillés ci-dessus pour robustesse.

### Dette technique introduite
- Dette faible et localisée : dépendance de décision à un contenu agrégé logs+résultat, plus I/O redondante.

### Risques de refactor futur évitables maintenant
- Oui : sécuriser maintenant la source de vérité de la décision (`stdout` métier) évitera des corrections ultérieures plus coûteuses lors de l’introduction d’un vrai transport humain.

## 4. Résumé final

Implémentation globalement propre, minimale et conforme au périmètre (pas de nouvelle couche, pas de modification orchestrator, tests ciblés présents).

Les points à améliorer restent limités et localisés :
1. éviter les faux positifs `WAIT_HUMAN` dus au stderr,
2. supprimer la relecture disque immédiate pour la décision,
3. ajouter 1 à 2 tests de robustesse supplémentaires.

Ces corrections sont sobres, compatibles avec l’étape actuelle, et réduisent la dette avant les phases suivantes.
