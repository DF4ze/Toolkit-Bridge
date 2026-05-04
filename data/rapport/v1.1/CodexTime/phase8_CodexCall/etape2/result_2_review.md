# Relecture / Review — Phase 8 CodexCall Étape 2 — `/workflow_project_set`

Date: 2026-05-03  
Lot relu: `phase8_CodexCall/etape2` (commande Telegram `/workflow_project_set` + service dédié + dashboard `/workflow`)

Note: `data/rapport/00.promptWorkflow/3.Relecture.md` mentionne un chemin de sortie sous `phase6/…_review.md`, mais la relecture concerne ici le lot **en cours** (Phase 8 CodexCall Étape 2). Le rapport est donc produit dans `phase8_CodexCall/etape2/`.

## Vérifications demandées

### 1) Séparation configuration vs runtime

- OK : l’étape ajoute une commande Telegram et un service applicatif (`WorkflowTelegramProjectService`) ; pas de nouvelle configuration Spring, pas de propriétés, pas de YAML.
- Le registre persistant reste dans le runtime (DB/JPA) ; Telegram ne fait que l’exposer.

### 2) Qualité du modèle implémenté

- OK : modèle métier (registre) non modifié ; l’étape 2 ne touche pas à `WorkflowProjectEntity/Repository/RegistryService`.
- La commande se contente d’appeler `registerOrUpdate(...)` et de mapper un résultat → message UX.

### 3) Découplage orchestrator / mémoire / tooling / policy / workspace

- OK : aucune dépendance ajoutée vers runner/orchestrator, session store, workspace tooling, ou execution Codex.
- `WorkflowTelegramProjectService` dépend uniquement du `WorkflowProjectRegistryService` + renderer (package-private).

### 4) Absence de logique ad hoc / trop spécifique

Points positifs :

- Parsing “quoted path” est localisé à la commande demandée (pas de refactor global).
- Les règles sont simples : quotes obligatoires si espaces, support `"..."` et `'...'`, erreur si quote non fermée.

Point discutable :

- La sanitization “anti fuite path” dans `WorkflowTelegramProjectService.sanitizeReason(...)` est heuristique (regex drive + `/home/`).  
  - C’est acceptable ici car la contrainte “ne pas exposer path absolu” est forte.
  - Risque : faux positifs si une raison contient `/` sans être un path (ex: message “a/b”). Le filtre actuel ne traite que `startsWith("/")` et `contains("/home/")`, donc le risque est limité.

### 5) Absence de couplage gênant pour les phases futures

- OK : pas d’impact sur `/workflow_run` ni `/workflow_resume`.
- OK : pas d’injection `codexWorkingDirectory` (phase suivante).
- Le service Telegram introduit une méthode utilitaire de parsing. Si d’autres commandes doivent supporter des valeurs “quoted”, il faudra éviter la duplication (mais ce n’est pas à faire maintenant).

### 6) Cohérence des noms

- OK : `WorkflowTelegramProjectService` est cohérent avec `WorkflowTelegramRoadmapService`, `WorkflowTelegramSummaryService`, `WorkflowTelegramOrchestrationService`.
- Commande : `/workflow_project_set` est explicite et alignée “workflow-*”.

### 7) Lisibilité générale

- OK : controller reste fin (délégation).
- OK : parsing + mapping résultat concentrés dans le service dédié.

### 8) Tests réellement utiles

- Bons tests : parsing quoted, quote non fermée, name/path manquant, success/failure mapping, non-fuite.
- Test dashboard `/workflow` : utile pour non-régression UX.
- `WorkflowTelegramControllerTest` mis à jour : cohérent.

### 9) Dette technique introduite

Dette mineure acceptable :

- Parsing quoted spécifique à cette commande (pas encore mutualisé).
- `sanitizeReason(...)` heuristique (mais nécessaire pour garantir la contrainte de non-fuite tant que les messages registry peuvent évoluer).

### 10) Risques de refactor futur encore évitables maintenant

Risque principal : duplication du parsing “quoted arg” si d’autres commandes futures ont le même besoin (ex: roadmap path).  
Recommandation future (pas maintenant) : extraire une petite utilitaire interne `TelegramArgParser` partagée (package `service.telegram.workflow`) une fois 2–3 commandes concernées.

## Faiblesses / points discutables (liste)

1. `sanitizeReason(...)` est une heuristique : peut masquer une raison légitime contenant une forme de path (mais contrainte de non-fuite prime).
2. `WorkflowTelegramController` a gagné une dépendance de plus ; acceptable, mais surveiller l’inflation du controller à mesure que de nouveaux services Telegram arrivent.
3. Parsing quoted ne gère pas les cas avancés (quotes imbriquées, échappement). C’est volontaire, mais à documenter plus tard.

## Corrections utiles proposées (sans nouvelles fonctionnalités)

À faire plus tard si nécessaire, uniquement si la duplication apparaît :

- Mutualiser le parsing `key=value` + quoted dans un utilitaire interne réutilisable.
- Remplacer `sanitizeReason(...)` par un sanitizer commun si un composant de sanitization Telegram est déjà disponible / généralisable (sinon garder local).

## Résumé final

Étape 2 est solide et pragmatique :

- expose le registre via Telegram sans modifier le workflow,
- supporte les paths avec espaces via quotes,
- respecte la contrainte “pas de fuite de path absolu” (tests inclus),
- garde le controller fin et la logique localisée.

