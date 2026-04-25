# Rapport d'analyse — Phase 5 Étape 5 — Préparer Telegram sans l'implémenter

---

## Résumé exécutif

Le système est **partiellement prêt** pour une future intégration Telegram.

Les informations essentielles sont disponibles en texte simple, le contrat CLI est stable et parsable sans logique fragile. Le cycle RUN → WAIT_HUMAN → RESUME est techniquement faisable depuis un bot externe.

Cependant, trois points bloquants ou à risque subsistent avant de brancher Telegram de manière propre :
1. Les chemins d'artifacts sont des chemins OS locaux absolus — un bot Telegram doit accéder au disque local ou recevoir le contenu, pas le chemin.
2. `workflowSummaryPath` est **optionnel** dans la sortie CLI — il peut être absent si le contexte est mal configuré.
3. Il n'existe pas encore de rendu Telegram dédié — réutiliser `WorkflowUserInteractionSimulator` sans adaptation serait une dérive.

---

## 1. Fichiers et classes concernés

### Couche d'entrée externe

| Fichier | Rôle |
|---|---|
| `AnalysisReviewWorkflowCli` | Point d'entrée invocable (`--mode=RUN\|RESUME`), produit stdout `key=value` |
| `AnalysisReviewWorkflowRunnerFactory` | Construit le runner par défaut sans Spring |

### Couche de rendu et parsing

| Fichier | Rôle |
|---|---|
| `WorkflowCliConsumerSimulator` | Parse stdout CLI → `SimulatedConsumerResult` |
| `WorkflowUserInteractionSimulator` | `SimulatedConsumerResult` → texte lisible (simulation courante) |

### Couche runtime (lecture seule, ne pas modifier)

| Fichier | Rôle |
|---|---|
| `AnalysisReviewWorkflowRunner` | Génère `workflow-summary.md` et enrichit les résultats |
| `GlobalReviewStep` | Produit `waitReason`, `reviewResultPath`, `finalDecision` |
| `WorkflowArtifactService` | Lit et écrit les artifacts sur disque |
| `WorkflowArtifactType` | Détermine le nom de fichier de chaque artifact (`workflow-summary.md`) |
| `WorkflowStepDecision` | Enum des décisions : `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`, `FINISH`, `RETRY_CORRECTION` |

---

## 2. Exploitabilité Telegram du `workflow-summary.md`

### Contenu actuel généré par `buildSummaryContent()`

```
Decision: WAIT_HUMAN
Reason: Missing decision in review
Correction triggered: false

Artifacts:
- reviewResultPath: D:\reports\v1.1\phase5\result.4.review.md
- workflowSummaryPath: D:\reports\v1.1\phase5\workflow-summary.md

Next steps:
1. Edit review result: D:\reports\v1.1\phase5\result.4.review.md
2. Add a clear decision and optional WAIT_REASON update
3. Resume with runCorrectionAfterReview(...)
```

### Points positifs

- Format texte simple, lisible dans Telegram sans parsing Markdown complexe.
- Contenu structuré : décision, raison, état de correction, artifacts, étapes suivantes.
- Les étapes suivantes pour `WAIT_HUMAN` sont numérotées et explicites.
- Le fichier est écrit sur disque avant que la CLI ne retourne → disponible immédiatement.

### Points de vigilance

- Les **chemins Windows absolus** (`D:\...`) sont inclus dans les artifacts. Telegram ne peut pas utiliser ces chemins directement — le bot doit soit lire le fichier et envoyer le contenu, soit envoyer le chemin comme texte informatif uniquement.
- Pas de limite de taille garantie. Un `workflow-summary.md` peut théoriquement dépasser 4096 caractères (limite Telegram par message). À gérer côté rendu.
- Le fichier `workflow-summary.md` est écrit **uniquement** si `hasSummaryConfiguration` est true dans `AnalysisReviewWorkflowRunner`. Si les variables de contexte (`reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`) sont absentes ou incomplètes, **aucun summary n'est écrit et `workflowSummaryPath` est absent de la sortie CLI**.

---

## 3. Disponibilité des informations en texte simple

| Information | Disponible | Source |
|---|---|---|
| Décision finale | **Toujours** | `finalDecision=` dans stdout CLI |
| Message utilisateur | **Toujours** | `message=` dans stdout CLI |
| Action à effectuer | **Toujours** | `nextAction=` dans stdout CLI (avec fallback dans `defaultNextAction`) |
| Raison d'attente | Optionnel | `waitReason=` dans stdout CLI, absent si non produit par `GlobalReviewStep` |
| Chemin du summary | Optionnel | `workflowSummaryPath=` dans stdout CLI, absent si contexte incomplet |
| Correction déclenchée | **Toujours** | `correctionTriggered=` dans stdout CLI |
| Contenu du summary | Non direct | Fichier local à lire séparément |
| Contenu du review result | Non direct | Chemin dans `reviewResultPath`, fichier local |

**Conclusion** : les informations minimales pour un message Telegram WAIT_HUMAN sont présentes. Le contenu détaillé (summary, review result) nécessite un accès disque.

---

## 4. Risque de parsing fragile ou ambigu pour le bot

### Pas de parsing complexe nécessaire

La sortie CLI est un format `key=value` strict :
- Un seul `=` par ligne comme séparateur.
- `WorkflowCliConsumerSimulator` implémente ce parsing avec filtrage des clés connues.
- Aucun JSON, aucun XML, aucun Markdown dans la sortie stdout.

### Points d'attention mineurs

- La méthode `safe()` dans `AnalysisReviewWorkflowCli` remplace les retours à la ligne par des espaces (`replaceAll("\\R+", " ")`). Cela garantit qu'une valeur multi-ligne ne casse pas le format, mais peut rendre une `nextAction` longue moins lisible.
- `WorkflowCliConsumerSimulator` ignore les clés inconnues — si une future clé est ajoutée à la sortie CLI sans être déclarée dans `KNOWN_KEYS`, elle sera silencieusement ignorée.

---

## 5. Explicité des décisions, messages, actions et chemins

### Décisions

L'enum `WorkflowStepDecision` contient 5 valeurs : `CONTINUE`, `WAIT_HUMAN`, `STOP_FAILURE`, `FINISH`, `RETRY_CORRECTION`.

Le `WorkflowCliConsumerSimulator` mappe uniquement :
- `WAIT_HUMAN` → `WAITING_FOR_HUMAN`
- `STOP_FAILURE` → `FAILED`
- `CONTINUE` → `SUCCESS`

**Point de risque** : `FINISH` et `RETRY_CORRECTION` sont mappés en `UNKNOWN` par le simulateur. Si ces décisions apparaissent en sortie de CLI, un futur bot Telegram recevrait `UNKNOWN` sans explication — ce n'est pas documenté.

### Messages utilisateur

Toujours présents, en texte libre, lisibles directement. Longueur non garantie.

### Actions (`nextAction`)

Toujours présents grâce au fallback `defaultNextAction()`. Texte libre, non parsé. Directement affichable dans Telegram.

### Chemins

Chemins locaux Windows absolus. Non utilisables directement par Telegram. À traiter comme texte informatif ou à résoudre localement.

---

## 6. Dépendances implicites à l'environnement local

### Dépendances identifiées

| Dépendance | Impact Telegram |
|---|---|
| Chemins Windows absolus dans les artifacts | Le bot doit s'exécuter sur la même machine ou avoir accès au système de fichiers |
| Invocation JVM locale (`java -jar ...`) | Le bot Telegram doit être sur la même machine ou passer par un service local |
| Variables CLI non documentées (6 paramètres obligatoires, 2 optionnels) | Le bot doit connaître ces paramètres ou les recevoir via configuration |
| `hasSummaryConfiguration` implicite | Si une variable manque, le summary disparaît silencieusement sans avertissement explicite |
| Convention de nommage des artifacts | Non nécessaire pour le bot (il utilise les chemins exposés), mais non documentée externement |

### Absence de documentation externe

La seule documentation des paramètres CLI se trouve dans `printUsage()` dans le code source. Il n'existe pas de fichier de référence externe (`README`, `cli-reference.md`). Un développeur bot devrait lire le code Java pour connaître les paramètres.

---

## 7. Cycle RUN → WAIT_HUMAN → RESUME : faisable ?

### Oui, techniquement

```
1. Bot invoque : java ... AnalysisReviewWorkflowCli --mode=RUN --reportRootDirectory=... ...
2. Bot reçoit stdout (key=value), parse via WorkflowCliConsumerSimulator
3. Si status=WAITING_FOR_HUMAN :
   a. Bot lit workflowSummaryPath (fichier local) et envoie le contenu à l'utilisateur
   b. Bot informe l'utilisateur du fichier à éditer (reviewResultPath)
4. Utilisateur édite le fichier localement (hors Telegram)
5. Utilisateur envoie "resume" dans Telegram
6. Bot invoque : java ... AnalysisReviewWorkflowCli --mode=RESUME --reportRootDirectory=... ...
7. Bot reçoit stdout, parse, envoie SUCCESS ou FAILURE à l'utilisateur
```

### Limite actuelle non bloquante

L'étape 4 (édition du fichier) reste manuelle et locale. Telegram ne peut pas éditer le fichier directement. C'est une limitation assumée de l'architecture actuelle — conforme aux contraintes de la sous-roadmap.

### Ce qui manque pour rendre le cycle fiable

- Le bot doit stocker les paramètres CLI de la session en cours (au minimum `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`) pour pouvoir appeler RESUME avec les mêmes valeurs que RUN.
- Aucun mécanisme de stockage d'état entre RUN et RESUME n'existe aujourd'hui dans le code Java.

---

## 8. Risques si Telegram est ajouté trop vite

### Risque 1 — Réutilisation directe de `WorkflowUserInteractionSimulator`

Ce composant est un simulateur de démonstration, pas un composant applicatif Spring. L'utiliser directement comme rendu Telegram sans adaptation introduirait une dépendance implicite sur un outil de simulation.

### Risque 2 — Chemins locaux dans les messages Telegram

Si le bot envoie `workflowSummaryPath=D:\reports\...` directement à l'utilisateur Telegram, cela est inutilisable et confus. Le bot doit soit lire le contenu du fichier, soit ne pas envoyer le chemin brut.

### Risque 3 — Dépassement de la limite Telegram (4096 caractères)

Le contenu du `workflow-summary.md` n'est pas borné. Un summary avec de nombreux artifacts ou un `nextAction` détaillé peut dépasser la limite. Sans gestion de troncature, le bot plantera ou tronquera le message aléatoirement.

### Risque 4 — `FINISH` et `RETRY_CORRECTION` non mappés

Ces deux décisions de `WorkflowStepDecision` apparaissent en UNKNOWN dans le simulateur. Si elles arrivent en production via Telegram, l'utilisateur recevra un message UNKNOWN sans indication sur quoi faire.

### Risque 5 — Absence de gestion de session

Le bot devra mémoriser les paramètres CLI entre un appel RUN et un appel RESUME. Sans ce mécanisme minimal (même en fichier de configuration temporaire), le bot ne peut pas être autonome.

### Risque 6 — `workflowSummaryPath` absent si contexte incomplet

Si les variables de configuration sont incomplètes, la sortie CLI ne contiendra pas `workflowSummaryPath`. Le bot doit gérer ce cas sans planter.

---

## 9. Recommandations minimales avant Telegram

Par ordre de priorité :

### Priorité 1 — Documentation externe des paramètres CLI

Créer un fichier Markdown `cli-reference.md` documentant :
- les paramètres obligatoires et optionnels de `--mode=RUN` et `--mode=RESUME` ;
- les codes de sortie (0, 1, 2) ;
- le format de la sortie stdout.

Aucun code à modifier. Livrable : un fichier Markdown.

### Priorité 2 — Vérification de la présence garantie de `workflowSummaryPath` pour WAIT_HUMAN

Vérifier qu'un contexte correctement configuré produit toujours `workflowSummaryPath` dans la sortie CLI lors d'un WAIT_HUMAN. Ajouter un test si ce n'est pas couvert.

### Priorité 3 — Créer un `TelegramMessageRenderer` dédié (sans l'implémenter encore)

Planifier qu'un rendu Telegram sera distinct de `WorkflowUserInteractionSimulator`. Il devra gérer :
- la limite de 4096 caractères ;
- la lecture du contenu du `workflowSummaryPath` ;
- le formatage Markdown Telegram (gras, italique, blocs de code).

Ne pas créer ce composant maintenant — uniquement décider qu'il sera distinct.

### Priorité 4 — Décider de la stratégie de lecture du summary

Choisir entre :
a. Le bot lit le fichier local et envoie le contenu dans le message.
b. Le bot envoie le fichier comme document Telegram.
c. Le bot ne lit pas le summary et affiche uniquement les informations de la sortie CLI.

Cette décision ne nécessite pas de code — c'est un choix d'architecture à documenter.

### Priorité 5 — Documenter le mapping incomplet de `FINISH` et `RETRY_CORRECTION`

Ajouter une note dans `WorkflowCliConsumerSimulator` ou un document technique indiquant que ces deux décisions sont actuellement mappées en UNKNOWN et ne doivent pas apparaître dans les flux humains standards.

---

## 10. Plan d'implémentation proposé (5 étapes)

### Étape 5a — Référence CLI externe

Créer `data/rapport/v1.1/phase5/etape5/cli-reference.md` documentant les paramètres CLI, codes de sortie et format de sortie. Aucun code.

### Étape 5b — Vérification de la garantie du summary path

Ajouter un test vérifiant que pour un contexte complet, `workflowSummaryPath` est toujours présent dans la sortie CLI en cas de WAIT_HUMAN. Corriger si nécessaire.

### Étape 5c — Décision d'architecture Telegram

Rédiger un document court (ADR minimal) précisant :
- le bot s'exécute sur la même machine ;
- il lit le summary localement et envoie le contenu ;
- il stocke les paramètres CLI de session en configuration simple ;
- il utilisera un `TelegramMessageRenderer` distinct de `WorkflowUserInteractionSimulator`.

### Étape 5d — Vérification de lisibilité complète du cycle

Créer un test de bout en bout simulant :
- `consumer.consume(0, sortieCLI_WAIT_HUMAN)` → vérifier présence de tous les champs nécessaires ;
- Lecture simulée du summary (string hardcodée) → vérifier que le contenu est sous 4096 caractères pour un cas standard ;
- `consumer.consume(0, sortieCLI_RESUME_SUCCESS)` → vérifier présence de tous les champs nécessaires.

### Étape 5e — Validation finale de préparation

Relecture croisée de toutes les sorties CLI possibles par rapport à la checklist Telegram :
- message non null ✓
- finalDecision non null ✓
- nextAction non null ✓
- workflowSummaryPath présent pour WAIT_HUMAN ✓ (à valider par 5b)
- FINISH et RETRY_CORRECTION documentés ✓ (à valider par 5a)

---

## Conclusion

**Verdict : Partiellement prêt**

| Axe | État |
|---|---|
| Contrat CLI stable et parsable | ✅ Prêt |
| Informations utiles en texte simple | ✅ Prêt |
| Pas de parsing fragile ou ambigu | ✅ Prêt |
| Cycle RUN → WAIT_HUMAN → RESUME techniquement faisable | ✅ Prêt |
| `workflowSummaryPath` garanti pour WAIT_HUMAN | ⚠️ À vérifier |
| Chemins locaux Windows exploitables depuis Telegram | ⚠️ Nécessite décision d'architecture |
| Longueur du summary bornée pour Telegram | ⚠️ À gérer côté rendu |
| `FINISH` et `RETRY_CORRECTION` gérés | ⚠️ Mappés en UNKNOWN, à documenter |
| Documentation CLI externe | ❌ Absente |
| Rendu Telegram dédié | ❌ À créer (ne pas réutiliser le simulator) |
| Gestion de session minimale (paramètres RUN → RESUME) | ❌ Absente |

Les 5 étapes proposées au §10 permettent de passer à l'état "prêt pour implémentation Telegram" sans modifier le runtime ni anticiper excessivement.
