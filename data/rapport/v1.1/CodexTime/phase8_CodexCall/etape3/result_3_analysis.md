# Analyse — Phase 8 CodexCall Étape 3 — Résolution projet dans `/workflow_run` et `/workflow_resume`

Date: 2026-05-03  
Scope: analyse uniquement (pas d’implémentation) — résolution `projectName → projectPath` via registre persistant, stockage en session, préparation injection future `codexWorkingDirectory`.

## 1) Résumé exécutif

Aujourd’hui, `/workflow_run` résout un triplet logique (`projectName`, `phase`, `etape`) via `WorkflowTelegramRunTargetResolver` et met à jour la session via `WorkflowTelegramSessionStore.updateContext(...)`.  
Mais la session ne stocke pas de `projectPath`, et le run/resume ne consultent pas le registre projet.

Pour rendre `/workflow_run` et `/workflow_resume` capables de travailler “dans le bon projet” (préparation `codexWorkingDirectory` à l’étape suivante), il faut :

- ajouter un champ optionnel `projectPath` dans `WorkflowTelegramSession`,
- étendre `WorkflowTelegramSessionStore.updateContext(...)` pour pouvoir poser/mettre à jour ce `projectPath`,
- résoudre `projectPath` dans `WorkflowTelegramOrchestrationService.startRun(...)` et `resume(...)` via `WorkflowProjectRegistryService.lookup(...)`,
- garder `WorkflowTelegramRunTargetResolver` **pur** (aucune dépendance DB) : il reste responsable du triplet logique,
- garantir la non-fuite de path absolu dans `/workflow` et `/workflow_status` (afficher “configured/missing” au besoin).

Conclusion : **prêt pour implémentation**. La conception reste minimale et n’impacte pas `CodexWorkflowClient`.

## 2) État actuel — `WorkflowTelegramSession` / `WorkflowTelegramSessionStore`

### 2.1 `WorkflowTelegramSession` (champs)

Le record contient notamment :

- `projectName`
- `phase`, `etape`
- `roadmapPath`
- `lastSummaryPath`
- `lastStatus`, `running`, `lastRunId`
- timestamps + `lastMessage`, `lastError`

Il ne contient **pas** de `projectPath`.

### 2.2 `WorkflowTelegramSessionStore.updateContext(...)`

Signature actuelle :

`updateContext(chatId, userId, projectName, phase, etape, roadmapPath)`

Elle met à jour `projectName`, `phase`, `etape`, `roadmapPath` (si non null), et laisse les autres champs identiques.

=> Ajouter `projectPath` implique :

- changer le record (nouvel attribut),
- adapter `updateContext(...)`,
- adapter les tests `WorkflowTelegramSessionStoreTest`,
- s’assurer que les renderers Telegram ne leak pas le path.

## 3) `WorkflowTelegramRunTargetResolver` — responsabilité et recommandation

### 3.1 État actuel

Le resolver :

- ne dépend que de la session et des args,
- ne fait que de la logique sur `projectName/phase/etape`,
- renvoie un `WorkflowTelegramRunTarget` (ok/error).

### 3.2 Recommandation

Conserver ce resolver **sans dépendance DB** :

- il reste “pure resolver” du triplet logique,
- la résolution `projectName → projectPath` doit être faite **dans une couche au-dessus** (orchestration service) via le registre projet.

Raison : éviter un couplage entre parsing/logic target et persistence, et préserver la testabilité du resolver.

## 4) Proposition : ajouter `projectPath` dans la session

### 4.1 Champ proposé

Ajouter à `WorkflowTelegramSession` :

`Path projectPath`

Règles :

- optionnel (`null` autorisé)
- ne pas afficher la valeur dans `/workflow` et `/workflow_status`
- utilisé uniquement pour construire le contexte d’exécution (étape suivante : `codexWorkingDirectory`)
- si `projectName` change, on remplace aussi `projectPath` (pour éviter incohérence)

### 4.2 Impact renderer

Le renderer n’affiche aujourd’hui que :

- `Project: <projectName>`
- `Roadmap: loaded/missing`
- `Summary: available/missing`

Deux options :

1) Ne rien afficher sur `projectPath` (minimal)
2) Afficher un état : `Project path: configured/missing` (sans valeur)

Recommandation : **option 2** (utile pour diagnostiquer un refus de run), mais ce choix peut être reporté à l’implémentation si on veut rester ultra-minimal.

## 5) Résolution `projectName → projectPath` pour `/workflow_run`

La résolution doit être implémentée dans `WorkflowTelegramOrchestrationService.startRun(...)` (ou dans un service dédié de résolution projet appelé par l’orchestration).

### Cas A — `project` fourni dans `/workflow_run`

- Lookup DB obligatoire via `WorkflowProjectRegistryService.lookup(projectName)`
- Si trouvé :
  - `sessionStore.updateContext(...)` doit mettre `projectName` + `projectPath`
  - puis résolution phase/etape normale (via resolver existant)
- Si absent :
  - refuser le run avec :
    - `Reason: Unknown project: <name>`
    - `Try: /workflow_project_set ...`

Note : si on garde l’ordre actuel (resolver puis updateContext), on devra faire le lookup projectPath **après** avoir obtenu `target.projectName()`. C’est OK.

### Cas B — `project` absent, session complète

- Si session contient `projectName + projectPath` :
  - réutiliser sans requête DB
- Poursuivre la résolution phase/etape via resolver

### Cas C — `project` absent, session incomplète

- Si session contient `projectName` mais pas `projectPath` :
  - lookup DB
  - si trouvé : compléter session avec `projectPath`
  - sinon : erreur “Unknown project” (ou “Project path missing and project not found”)

### Cas D — aucun projet

- Erreur :
  - `Reason: No project configured for this workflow session`
  - `Try: /workflow_project_set ...`

## 6) Résolution `projectName → projectPath` pour `/workflow_resume`

`/workflow_resume` ne prend pas d’args aujourd’hui (pas de `project=...`). Le contrat est donc simple : le projet du resume est celui de la session.

Règles :

- Resume autorisé uniquement si `WAITING_HUMAN` (déjà en place)
- Si session contient `projectPath` : OK (aucun lookup)
- Sinon si session contient `projectName` :
  - lookup DB
  - si trouvé : compléter session + continuer
  - sinon : refuser (projet introuvable)
- Sinon : refuser (session incohérente)

Important : ne jamais changer de projet pendant resume (pas de mécanisme de switch).

## 7) Messages utilisateur (UX)

Recommandation : réutiliser `WorkflowTelegramMessageRenderer.errorMessage(...)` avec des messages courts, sans path :

### Projet inconnu

- Reason: `Unknown project: <name>`
- Try: `/workflow_project_set name=<name> path="<project-path>"`

### Projet manquant en session

- Reason: `No project configured for this workflow session`
- Try: `/workflow_project_set name=ToolkitBridge path="<project-path>"`

### Path manquant au resume

- Reason: `Project path is missing for this workflow session`
- Try: `/workflow_project_set name=<project> path="<project-path>"`

## 8) Préparation injection future `codexWorkingDirectory`

Points d’injection futurs (à l’étape 4) :

- `WorkflowTelegramOrchestrationService.buildContext(...)` (RUN)
- `WorkflowTelegramOrchestrationService.buildResumeContext(...)` (RESUME)

Cible :

- ajouter `variables.put("codexWorkingDirectory", <session.projectPath>)`

Cette étape 3 doit uniquement garantir que :

- la session stocke le `projectPath`,
- et que `startRun(...)` / `resume(...)` ont la donnée disponible (ou savent la résoudre) avant de lancer l’async.

## 9) Tests recommandés (implémentation future)

### Session/store

- `updateContext` met à jour `projectPath` (et le remplace quand `projectName` change)
- `/workflow` et `/workflow_status` n’affichent jamais `C:\`, `D:\`, `/home/`, `/var/`

### `/workflow_run` (orchestration)

- `project=Known` :
  - lookup DB called
  - session projectPath set
  - run démarre (sans modifier le runner)
- `project=Unknown` : erreur + Try command
- pas de `project` :
  - session complète (name+path) : pas de lookup
  - session incomplète (name seul) : lookup puis set
  - aucun projet : erreur

### `/workflow_resume`

- session WAITING_HUMAN + projectPath présent : OK
- WAITING_HUMAN + projectName seulement : lookup puis OK
- WAITING_HUMAN + project inconnu : refus
- tentative de changement de projet : non applicable (commande sans args), donc test “pas de changement” implicite

## 10) Risques

- Mélanger la responsabilité du resolver logique avec un lookup DB (à éviter).
- Non-régression : modification de la session record peut casser des tests existants ; prévoir une mise à jour systématique.
- Fuite de path : toute introduction de `projectPath` dans la session doit être accompagnée de tests non-fuite sur `/workflow`, `/workflow_status`, et messages d’erreur.
- Resume : ne pas introduire de parsing `project=` sur resume (risque de “switch project”).

## 11) Plan d’implémentation (5–8 étapes)

1. Ajouter `projectPath` à `WorkflowTelegramSession` (champ `Path` optionnel).
2. Étendre `WorkflowTelegramSessionStore.updateContext(...)` pour accepter/mettre à jour `projectPath`.
3. Ajouter une méthode helper (ex: `resolveProjectPath(...)`) dans `WorkflowTelegramOrchestrationService` (ou service dédié) qui applique les règles A/B/C/D.
4. Appliquer la résolution dans `startRun(...)` avant de lancer l’async (refus si inconnu).
5. Appliquer la résolution dans `resume(...)` (refus si path introuvable).
6. Adapter `WorkflowTelegramMessageRenderer` pour afficher au plus `projectPath: configured/missing` (optionnel) sans fuite.
7. Ajouter tests orchestration/session non-fuite.
8. Valider `./mvnw test`.

## 12) Conclusion

Statut : **prêt pour implémentation**.

La stratégie recommandée conserve les responsabilités claires (resolver logique vs lookup registry) et prépare proprement l’étape suivante d’injection `codexWorkingDirectory` sans toucher à `CodexWorkflowClient`.

