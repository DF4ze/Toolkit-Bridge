# Phase 7 — Étape 4 — Relecture & revue critique d’architecture

Périmètre relu : ajout `/workflow_resume` (controller + orchestration + tests). Contraintes respectées : runner/CLI inchangés, pas de DB, pas de parsing summary, pas d’incrément automatique d’étape.

## 1) Vérifications demandées (cadre 3.Relecture.md)

### Séparation configuration / runtime
- OK : `WorkflowTelegramOrchestrationService` dépend de `WorkflowTelegramProperties` (report root/version/timeout) et conserve la configuration en champs immuables.
- OK : aucune configuration “hardcodée” critique n’est ajoutée pour RESUME (réutilise report root/version existants).

### Qualité du modèle
- OK : pas d’extension de `WorkflowTelegramSession` ; le RESUME s’appuie sur `lastStatus`, `running`, `phase`, `etape` (suffisant).
- OK : respect de la règle : en `WAITING_HUMAN`, `phase/etape` restent l’étape suspendue (aucune incrémentation introduite ici).

### Découplage orchestration / workspace / tooling / policy
- OK : RESUME ne touche pas à Workspace/tooling/policy ; simple appel runner en-process.
- OK : pas d’appel CLI/ProcessBuilder.

### Absence de logique ad hoc / trop spécifique
- Globalement OK : la logique est strictement “gating + lock + exécution async + mapping de résultat”.
- Point mineur : messages UX en dur (constantes) dans l’orchestrateur — acceptable au stade actuel.

### Cohérence des noms / lisibilité
- OK : `resume(...)`, `executeResume(...)`, `buildResumeContext(...)` clairs.
- Attention : le nom `etape` reste ambigu (fr) mais déjà existant ; l’étape 4 ne l’aggrave pas.

### Tests utiles
- OK : les tests ajoutés couvrent gating, lock (double resume), et mapping de décisions (`CONTINUE/WAIT_HUMAN/STOP_FAILURE`).
- OK : pas de tests “fantaisistes” ni sur-architecture.

## 2) Faiblesses / points discutables

### 2.1 Problème d’encodage (mojibake) dans les messages

Constat : dans `WorkflowTelegramOrchestrationService`, le message de démarrage contient une séquence illisible (`"Ã°Å¸Å¡â‚¬ Workflow started\n"`), symptôme d’un fichier enregistré avec un encodage/BOM incohérent.

Impact :
- UX Telegram dégradée (message affiché incorrectement).
- Risque de réintroduction de problèmes d’encodage sur d’autres chaînes.

### 2.2 UX du resume minimaliste (RunId non renvoyé)

Le prompt d’implémentation demandait seulement `🚀 Resume lancé`. C’est respecté.
Point d’attention : contrairement à `/workflow_run`, le resume ne renvoie pas de détails (target/runId). Ce n’est pas un bug, mais ça peut compliquer le support.

### 2.3 Duplication contrôlée (acceptable, mais à surveiller)

`executeRun(...)` et `executeResume(...)` restent dupliquées (try/catch + handleResult). C’est acceptable ici.
Le facteur `executeAsync(...)` est bien une “petite factorisation” (pas de grosse abstraction).

## 3) Corrections utiles (sans nouvelle fonctionnalité)

### Correction A — Fix encodage du fichier orchestration

Objectif : garantir un fichier source en UTF‑8 sans BOM et des littéraux corrects.
- Remplacer la chaîne corrompue par une chaîne ASCII (ex: `"Workflow started\n..."`) ou re-saisir l’emoji correctement en UTF‑8.
- Vérifier que le fichier n’a pas de BOM parasite.

### Correction B — Tests de robustesse sur chaînes (optionnel)

Sans ajouter de feature : ajouter un test qui vérifie que `startRun(...)` retourne un message contenant `"Workflow started"` (ASCII) plutôt que de matcher l’emoji.
Cela évite des faux négatifs et protège contre l’encodage.

## 4) Dette technique introduite

- Aucune dette structurelle majeure.
- Dette légère possible : gestion des messages UX en constantes dans l’orchestrateur (si le nombre de commandes augmente, on pourra factoriser plus tard).

## 5) Résumé final

- Étape 4 est cohérente et respecte les contraintes : RESUME uniquement en `WAITING_HUMAN`, lock réutilisé, appel runner direct `runCorrectionAfterReview(context)`, mapping résultat réutilisé, pas de changement runner/CLI/DB.
- Point critique à corriger rapidement : **encodage** du fichier `WorkflowTelegramOrchestrationService.java` (message “Workflow started” corrompu).

