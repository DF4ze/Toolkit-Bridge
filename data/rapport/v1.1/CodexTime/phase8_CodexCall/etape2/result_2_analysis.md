# Analyse — Phase 8 CodexCall Étape 2 — Commande Telegram `/workflow_project_set`

Date: 2026-05-03  
Contexte: ajout d’une commande Telegram pour exposer le registre projet persistant (étape 1), sans modifier `/workflow_run` ni exécution Codex.

## 1) Résumé exécutif

L’intégration Telegram actuelle supporte des commandes `key=value`, mais le parsing est basé sur `ctx.getArgs()` (tokens séparés par espaces) et ne permet pas nativement de récupérer une valeur `path=` contenant des espaces.  
Pour livrer `/workflow_project_set` de manière robuste dès maintenant, il faut :

- ajouter une extraction dédiée de `path=` capable de reconstruire une valeur **quoted** (`"..."` ou `'...'`) à partir des tokens,
- déléguer l’upsert au `WorkflowProjectRegistryService`,
- retourner un message UX standard via `WorkflowTelegramMessageRenderer` sans exposer le path absolu,
- ajouter la commande dans le dashboard `/workflow` (liste “Commands”).

Conclusion : **prêt pour implémentation**, avec un parsing localisé à cette commande (pas de refactor global requis).

## 2) Existant — Controller Telegram & UX

### 2.1 Où ajouter la commande

`WorkflowTelegramController` est le point d’entrée des commandes workflow et contient déjà :

- `/workflow` (dashboard)
- `/workflow_roadmap_load`
- `/workflow_run`
- `/workflow_status`
- `/workflow_resume`
- `/workflow_summary`

=> `/workflow_project_set` doit être ajouté dans ce controller (même bot `Cortex`), avec un handler fin qui :

- récupère `chatId`, `userId`,
- parse `name` et `path`,
- délègue à un service,
- retourne une `String` UX.

### 2.2 Standard UX existant

Les messages sont standardisés via `WorkflowTelegramMessageRenderer` :

- succès : `Action effectuee / Detail: ... / Next: ...`
- erreur : `Action impossible / Reason: ... / Try: ...`

=> `/workflow_project_set` doit utiliser ces méthodes.

## 3) Parsing actuel des arguments (limites)

### 3.1 Parsing existant

Dans `WorkflowTelegramController`, `argValue(ctx, key)` :

- parcourt `ctx.getArgs()`
- cherche un token qui commence par `key=`
- prend le substring après `=`

Ce modèle suppose implicitement que la valeur est contenue dans **un seul token**.

### 3.2 Limite principale

Si Telegram découpe par espaces, l’entrée :

`path="D:\Documents\Spring\Mon Projet"`

peut devenir :

- `path="D:\Documents\Spring\Mon`
- `Projet"`

et le parsing actuel retournera une valeur tronquée.

## 4) Parsing attendu pour `path=` (stratégie recommandée)

Objectif : supporter :

- `path=D:\Documents\Spring\Toolkit-Bridge` (simple)
- `path="D:\Documents\Spring\Mon Projet"` (double quotes)
- `path='D:\Documents\Spring\Mon Projet'` (single quotes)

### 4.1 Stratégie minimale (sans dépendre du raw message)

Ajouter une méthode dédiée, localisée à `/workflow_project_set`, par exemple :

- `extractPossiblyQuotedArg(List<String> args, String key)`

Algorithme :

1. trouver l’index du token commençant par `key=`
2. récupérer le premier fragment (après `=`)
3. si le fragment commence par `"` ou `'` et ne se termine pas par la même quote :
   - concaténer ` + " " + nextToken` jusqu’à rencontrer un token se terminant par la quote attendue
4. enlever les quotes englobantes si présentes
5. `trim()` final

Cas supplémentaires utiles :

- si `key=` est fourni sans valeur (`path=`) → considérer “missing”
- si quote ouverte non fermée → erreur claire (“Invalid quoted path”)

Pourquoi cette approche :

- ne nécessite pas d’accès au raw message (non garanti par l’API `TelegramUpdateContext`),
- s’appuie uniquement sur `ctx.getArgs()` (même modèle que le reste),
- ne change pas le parsing des autres commandes.

### 4.2 “Reste de ligne”

La demande mentionne : “si `path=` est le dernier argument, permettre de récupérer le reste de la ligne”.  
Avec l’approche ci-dessus, c’est implicitement supporté : on concatène jusqu’à la fin si quote non fermée.  
Recommandation : **ne pas** interpréter “reste de ligne” sans quotes (trop ambigu), garder la règle :

- si `path` contient des espaces : **exiger quotes**.

## 5) Définition de la commande `/workflow_project_set`

### 5.1 Syntaxe recommandée

```text
/workflow_project_set name=<projectName> path="<projectPath>"
```

Clés à supporter :

- `name=` (obligatoire)
- `path=` (obligatoire)

Optionnel (tolérance) :

- accepter aussi `project=` ou `projectName=` en alias de `name=` si on veut s’aligner sur les autres commandes (à décider ; non nécessaire).

### 5.2 Comportement

- upsert dans le registre via `WorkflowProjectRegistryService.registerOrUpdate(name, path)`
- ne charge pas de roadmap
- ne lance pas de workflow
- ne modifie pas la session `/workflow_run` (hors scope)
- **ne retourne jamais le path absolu** dans le message standard

## 6) Intégration avec `WorkflowProjectRegistryService`

Deux options :

### Option A — Appel direct depuis le controller

Pro :

- rapide, peu de classes

Con :

- controller plus chargé (validation/parsing + mapping de résultat)
- moins aligné avec l’architecture “controller fin” déjà utilisée (`roadmapService`, `orchestrationService`, `summaryService`)

### Option B — Service Telegram dédié (recommandé)

Créer un `WorkflowTelegramProjectService` (ou similaire) responsable de :

- parsing `name/path` (ou parsing minimal dans controller + service reçoit des strings déjà extraites),
- appel registry,
- mapping résultat → `WorkflowTelegramMessageRenderer`.

Recommandation : **Option B**, cohérente avec les patterns existants (controller délègue).

## 7) Messages utilisateur (cibles)

### Succès création

- `Action effectuee`
- `Detail: Project registered: <projectName>`
- `Next: /workflow_run project=<projectName> phase=... etape=...`

### Succès update

- `Action effectuee`
- `Detail: Project updated: <projectName>`
- `Next: /workflow_run project=<projectName> phase=... etape=...`

### Erreurs (format standard)

Nom manquant :

- `Action impossible`
- `Reason: Missing project name`
- `Try: /workflow_project_set name=ToolkitBridge path=\"<project-path>\"`

Path manquant :

- `Action impossible`
- `Reason: Missing project path`
- `Try: /workflow_project_set name=ToolkitBridge path=\"<project-path>\"`

Path quoted invalide :

- `Action impossible`
- `Reason: Invalid quoted path (missing closing quote)`
- `Try: /workflow_project_set name=ToolkitBridge path=\"D:\\...\\Mon Projet\"`

Erreur registry :

- `Action impossible`
- `Reason: <reason>` (reprendre `WorkflowProjectRegistrationResult.reason`, qui est déjà court)
- `Try: /workflow_project_set name=... path=\"...\"`

Important :

- ne pas inclure `projectPath` dans `Detail` ni `Reason` (évite fuite).
  - Si `reason` contient un path (peu probable aujourd’hui), prévoir un filtrage minimal (ou réutiliser un sanitizer existant si accessible).

## 8) Impact dashboard `/workflow`

`WorkflowTelegramMessageRenderer.workflowHome(...)` liste actuellement les commandes en dur :

- `/workflow_run`
- `/workflow_status`
- `/workflow_summary`
- `/workflow_resume`
- `/workflow_roadmap_load`

Recommandation : ajouter :

- `/workflow_project_set`

Sans changer le “Next action” (la recommandation peut rester `/workflow_roadmap_load` si roadmap absente).

## 9) Tests à prévoir (implémentation future)

### Parsing (unitaire)

- `path=D:\\Documents\\Spring\\Toolkit-Bridge` (sans quotes)
- `path=\"D:\\Documents\\Spring\\Mon Projet\"` (double quotes → reconstitution)
- `path='D:\\Documents\\Spring\\Mon Projet'` (single quotes → reconstitution)
- quote non fermée → erreur

### Commande (service/controller)

- succès création (registry returns created) → message “registered”
- succès update (registry returns updated) → message “updated”
- name manquant → erreur + Try command
- path manquant → erreur + Try command
- registry failure → erreur + reason
- non fuite : vérifier que le message ne contient pas `D:\\` ni `C:\\` ni `/home/` (assert substring)

### Dashboard `/workflow`

- `/workflow` contient la ligne `- /workflow_project_set`

## 10) Risques

- Parsing quotes fragile (cas `\"` interne, quotes imbriquées) : acceptable en v1, documenter “quotes simples/englobantes seulement”.
- Duplication de parsing si on réplique la logique ailleurs : garder la méthode privée dans le controller/service Telegram dédié.
- Fuite path absolu : ne pas inclure `projectPath` dans UX ; prévoir un test “non leak”.
- Controller trop chargé : éviter en créant `WorkflowTelegramProjectService`.

## 11) Plan d’implémentation (5–8 étapes)

1. Ajouter `WorkflowTelegramProjectService` (dépend de `WorkflowProjectRegistryService` + renderer).
2. Ajouter commande `/workflow_project_set` dans `WorkflowTelegramController` (délégation).
3. Implémenter parsing `name=` standard + parsing `path=` quoted (reconstruction depuis `ctx.getArgs()`).
4. Mapper `WorkflowProjectRegistrationResult` vers `successMessage/errorMessage` sans fuite path.
5. Ajouter `/workflow_project_set` au dashboard renderer `/workflow`.
6. Ajouter tests parsing + tests de commande + test dashboard.
7. Vérifier non-régression des autres commandes (compilation + `mvn test`).

## 12) Conclusion

Statut : **prêt pour implémentation**.

Le point clé est le parsing `path=` avec espaces : la stratégie recommandée (reconstruction depuis tokens + quotes) est localisée, testable et n’impacte pas `/workflow_run` ni `/workflow_roadmap_load`.

