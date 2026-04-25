# Rapport Final — Phase 5 — Workflow externe et préparation Telegram

---

## Résumé exécutif

La Phase 5 a transformé un workflow exclusivement pilotable par tests en un système appelable depuis l'extérieur, pilotable humainement, et préparé pour une future intégration Telegram.

Le runtime n'a pas été modifié. Les ajouts sont une couche CLI externe, une couche de simulation/consommation, et une documentation de contrat.

Le système est **partiellement prêt** pour Telegram. Les fondations sont solides, les manques restants sont identifiés et documentés.

---

## 1. Ce qui a été implémenté

### Point d'entrée externe — `AnalysisReviewWorkflowCli`

Classe créée avec :
- méthode `main(String[] args)` invocable comme exécutable Java ;
- méthode `execute(String[] args)` testable sans démarrer un processus OS ;
- deux modes : `RUN` et `RESUME` (via `--mode=RUN|RESUME`) ;
- parsing d'arguments `--key=value` strict avec validation et messages d'erreur lisibles.

#### Paramètres CLI

Obligatoires (tous modes) : `--mode`, `--reportRootDirectory`, `--reportVersion`, `--reportPhase`, `--stepNumber`.

Obligatoire en mode RUN uniquement : `--analysisSourcePath`.

Optionnels : `--codexWorkingDirectory`, `--codexTimeoutSeconds`, `--runId`, `--workflowType`, `--targetStepRef`.

#### Codes de sortie

| Code | Signification |
|---|---|
| `0` | Décision valide produite |
| `1` | Échec runtime ou décision nulle |
| `2` | Argument invalide ou mode inconnu |

#### Format stdout

```
decision=<valeur>
message=<texte>
finalDecision=<valeur>
nextAction=<texte>
correctionTriggered=<true|false>
waitReason=<texte>          (conditionnel : WAIT_HUMAN uniquement)
workflowSummaryPath=<chemin> (conditionnel : si summary écrit avec succès)
```

Toutes les valeurs sont sur une seule ligne (retours à la ligne remplacés par des espaces via `safe()`).

### Factory standalone — `AnalysisReviewWorkflowRunnerFactory`

Construit le runner complet (`AnalysisReviewWorkflowRunner` + steps + services) sans contexte Spring. Permet une invocation CLI autonome via `createDefault()`.

### Runner — `AnalysisReviewWorkflowRunner`

Runner existant, non modifié dans ses responsabilités. Il orchestre :
- analyse → revue → correction optionnelle (mode RUN) ;
- correction seule (mode RESUME via `runCorrectionAfterReview()`).

Il génère `workflow-summary.md` et enrichit les résultats avec `workflowSummaryPath`, `finalDecision`, `correctionTriggered`, `nextAction`.

### Fichier `workflow-summary.md`

Généré automatiquement à chaque exécution CLI, au chemin :

```
<reportRootDirectory>/<reportVersion>/<reportPhase>/workflow-summary.md
```

Contenu :

```
Decision: <valeur>
Reason: <texte>
Correction triggered: <true|false>

Artifacts:
- reviewResultPath: <chemin>
- workflowSummaryPath: <chemin>

Next steps:
1. Edit review result: <chemin>
2. Add a clear decision and optional WAIT_REASON update
3. Resume with runCorrectionAfterReview(...)
```

### Simulateur CLI — `WorkflowCliConsumerSimulator`

Parse la sortie stdout CLI (`key=value`) en `SimulatedConsumerResult` :
- mapping `WAIT_HUMAN` → `WAITING_FOR_HUMAN` ;
- mapping `STOP_FAILURE` → `FAILED` ;
- mapping `CONTINUE` → `SUCCESS` ;
- mapping inconnu ou `FINISH`/`RETRY_CORRECTION` → `UNKNOWN` ;
- exit code non-zero → `ERROR`.

Expose un record immuable avec tous les champs nécessaires au rendu.

### Simulateur d'interaction — `WorkflowUserInteractionSimulator`

Produit un message texte lisible à partir d'un `SimulatedConsumerResult` :

```
Status: WAITING_FOR_HUMAN
Message: Global review requires human decision
Reason: Missing decision in review
Next action: Edit review result and resume with runCorrectionAfterReview(...)
Summary: D:\reports\v1.1\phase5\workflow-summary.md
```

Sans état, sans bot, sans state machine.

### Tests

`WorkflowUserInteractionSimulatorTest` : 7 cas couvrant `WAIT_HUMAN`, pas de reprise automatique, `SUCCESS`, `STOP_FAILURE`, `ERROR`, `UNKNOWN`, résultat null.

### Documentation — `cli-reference.md`

Fichier Markdown décrivant intégralement le contrat CLI : paramètres, codes de sortie, format stdout, contenu du summary, chemins locaux, limites connues pour Telegram.

---

## 2. Ce qui n'a PAS été fait

### Volontairement hors scope

- Pas de bot Telegram.
- Pas d'endpoint REST.
- Pas de state machine.
- Pas de moteur de conversation.
- Pas de persistance de session.
- Pas de multi-user.
- Pas de parsing du contenu du summary ou du review result.
- Pas d'invocation de processus OS réel dans les tests.

### Préparé mais non implémenté

- Plan d'implémentation Telegram proposé en 5 étapes (5a–5e), dont seul 5a a été réalisé.
- `TelegramMessageRenderer` : décision prise de ne pas réutiliser `WorkflowUserInteractionSimulator` pour Telegram, mais le composant n'a pas été créé.
- Gestion de session RUN → RESUME : décision d'architecture non encore implémentée.

### Encore absent pour une intégration Telegram complète

- Rendu Telegram dédié avec gestion de la limite 4096 caractères.
- Stockage minimal de l'état de session (paramètres RUN mémorisés pour RESUME).
- Stratégie de livraison du contenu du summary (envoi du texte vs fichier joint Telegram).
- Tests de bout en bout avec simulation du cycle complet RUN → WAIT_HUMAN → RESUME.

---

## 3. Contrat externe réel

### Appel CLI

```bash
java -cp <classpath> AnalysisReviewWorkflowCli \
  --mode=RUN \
  --reportRootDirectory=/path/to/reports \
  --reportVersion=v1.1 \
  --reportPhase=phase5 \
  --stepNumber=4 \
  --analysisSourcePath=/path/to/4.analysis.md
```

```bash
java -cp <classpath> AnalysisReviewWorkflowCli \
  --mode=RESUME \
  --reportRootDirectory=/path/to/reports \
  --reportVersion=v1.1 \
  --reportPhase=phase5 \
  --stepNumber=4
```

### Sortie stdout typique (WAIT_HUMAN)

```
decision=WAIT_HUMAN
message=Global review requires human decision
finalDecision=WAIT_HUMAN
nextAction=Review result requires human decision. Edit the review result, add a clear decision, then run runCorrectionAfterReview(...)
correctionTriggered=false
waitReason=Missing decision in review
workflowSummaryPath=D:\reports\v1.1\phase5\workflow-summary.md
```

### Décisions possibles

| `finalDecision` | Signification |
|---|---|
| `WAIT_HUMAN` | Intervention humaine requise |
| `CONTINUE` | Étape terminée sans blocage |
| `STOP_FAILURE` | Échec non récupérable automatiquement |
| `FINISH` | Fin de workflow (non produit par les étapes actuelles) |
| `RETRY_CORRECTION` | Correction à rejouer (non produit par les étapes actuelles) |

### Limites du contrat actuel

- `workflowSummaryPath` absent si l'écriture du fichier échoue.
- `waitReason` absent si l'étape de revue ne produit pas de raison.
- Chemins Windows absolus dans toutes les sorties — non transférables directement.
- Valeurs multi-lignes aplaties en une seule ligne par `safe()`.

---

## 4. Architecture obtenue

### Découpage en couches

```
┌─────────────────────────────────────────────┐
│               Runtime workflow               │
│  WorkflowOrchestrator                        │
│  GlobalAnalysisStep / GlobalReviewStep       │
│  CorrectionStep                              │
│  WorkflowArtifactService                    │
│  CodexWorkflowClient                         │
│  → NON MODIFIÉ en Phase 5                   │
└─────────────────────────────────────────────┘
         ↑ injecté par factory
┌─────────────────────────────────────────────┐
│           CLI externe (couche Phase 5)       │
│  AnalysisReviewWorkflowCli                   │
│  AnalysisReviewWorkflowRunnerFactory         │
│  → dépend du runner uniquement               │
└─────────────────────────────────────────────┘
         ↓ consomme stdout (contrat texte)
┌─────────────────────────────────────────────┐
│       Simulation / consommation (Phase 5)    │
│  WorkflowCliConsumerSimulator                │
│  WorkflowUserInteractionSimulator            │
│  → aucune dépendance au runtime Java         │
└─────────────────────────────────────────────┘
```

### Qualité du découplage

**Points propres :**
- La couche simulation ne connaît pas les types du runtime (`WorkflowStepResult`, `WorkflowStepDecision`, etc.).
- La couche CLI ne connaît que le runner via son interface publique.
- Le runtime n'a été modifié pour aucune raison liée à l'exposition externe.
- `AnalysisReviewWorkflowRunnerFactory` permet une invocation standalone sans Spring.

**Points de vigilance :**
- `WorkflowUserInteractionSimulator` est dans `src/main/java` sans annotation Spring. Son statut (composant applicatif ou outil de simulation) n'est pas documenté dans le code.
- Duplication des helpers (`requiredString`, `requiredInt`, `requiredPath`, `optionalPath`) présente dans `GlobalReviewStep`, `GlobalAnalysisStep`, `CorrectionStep` et `AnalysisReviewWorkflowRunner`. Dette héritée des phases précédentes, non introduite par la Phase 5.

### Verdict architectural

L'architecture obtenue est **propre** dans les limites du périmètre. Il n'y a pas de dérive observable. Les ajouts Phase 5 ne créent pas de couplage avec le runtime. La séparation texte/Java (contrat stdout) est efficace et testable.

---

## 5. Capacité d'exploitation humaine

### Ce que l'opérateur reçoit en cas de WAIT_HUMAN

1. Via la sortie CLI : message lisible + waitReason + chemin du summary.
2. Via `workflow-summary.md` : décision, raison, état de correction, liste des artifacts concernés, étapes numérotées à effectuer.
3. Chemin du fichier à éditer (review result) inclus dans les étapes.

### Ce que l'opérateur doit faire

Les étapes dans `workflow-summary.md` sont explicites :

```
1. Edit review result: D:\reports\v1.1\phase5\result.4.review.md
2. Add a clear decision and optional WAIT_REASON update
3. Resume with runCorrectionAfterReview(...)
```

### Ce qui manque encore pour une exploitation sans ambiguïté

- La commande exacte RESUME n'est pas incluse dans le summary (seulement une mention de `runCorrectionAfterReview(...)`).
- L'opérateur doit se souvenir ou noter les paramètres CLI du run précédent pour construire la commande RESUME.

### Verdict exploitation humaine

**Fonctionnel.** Un opérateur qui lit le summary comprend clairement quoi faire. La seule friction restante est la construction manuelle de la commande RESUME avec les bons paramètres.

---

## 6. Préparation Telegram

### Verdict

**Partiellement prêt.**

| Critère | État |
|---|---|
| CLI invocable depuis l'extérieur | ✅ |
| Contrat stdout parsable sans logique fragile | ✅ |
| `workflow-summary.md` lisible sans parsing | ✅ |
| Cycle RUN → WAIT_HUMAN → RESUME fonctionnel | ✅ |
| Toutes les informations disponibles en texte simple | ✅ |
| Rendu Telegram dédié | ❌ Non créé |
| Gestion de session (mémoriser les paramètres entre RUN et RESUME) | ❌ Non implémentée |
| Stratégie de livraison du contenu du summary | ❌ Non décidée |
| Gestion de la limite 4096 caractères Telegram | ❌ Non traitée |
| `FINISH` et `RETRY_CORRECTION` gérés dans le simulateur | ⚠️ Mappés en UNKNOWN |
| Chemins locaux Windows → utilisables depuis Telegram | ⚠️ Non transférables directement |

### Points déjà OK pour Telegram

- Le contrat CLI est stable, sans parsing JSON ni format ambigu.
- `finalDecision`, `message`, `nextAction`, `correctionTriggered` sont toujours présents.
- Le cycle est documenté dans `cli-reference.md`.
- Le simulateur de consommation est réutilisable ou inspirant pour un bot.

### Manques restants

- **Rendu Telegram** : créer un `TelegramMessageRenderer` distinct de `WorkflowUserInteractionSimulator`, gérant la limite de caractères et le formatage Markdown Telegram.
- **Session** : stocker localement les paramètres du run courant (en config ou fichier temporaire) pour construire la commande RESUME automatiquement.
- **Livraison du summary** : le bot lit `workflowSummaryPath` localement et envoie le contenu (ou le fichier joint Telegram).

---

## 7. Dette technique et risques

### Dette faible — acceptable

| Dette | Risque |
|---|---|
| `WorkflowUserInteractionSimulator` dans `src/main/java` sans @Component | Ambiguïté de statut si Spring est introduit plus tard |
| Tests `renderRunWaitHumanMessage` / `renderResumeSuccessMessage` avec nommage trompeur | Lecture du test induisant une fausse attente de transition RUN/RESUME |
| Assertion `.name()` sur enum dans les tests | Fragile face à un renommage de l'enum |
| `FINISH` et `RETRY_CORRECTION` → UNKNOWN dans le simulateur | Non exploitable si ces décisions apparaissent en production |

### Dette héritée (non introduite par Phase 5)

- Duplication des helpers `requiredString/requiredInt/requiredPath` dans plusieurs steps.
- Absence de test d'intégration réel avec invocation de processus OS.

### Risques si Telegram est ajouté sans préparation

1. **Réutilisation directe de `WorkflowUserInteractionSimulator`** → dérive : ce n'est pas un composant applicatif.
2. **Envoi de chemins Windows bruts** à l'utilisateur Telegram → inutilisable et confus.
3. **Dépassement de la limite 4096 caractères** → message tronqué ou plantage bot.
4. **Absence de session** → bot ne peut pas appeler RESUME sans connaître les paramètres du RUN précédent.
5. **UNKNOWN silencieux** sur `FINISH`/`RETRY_CORRECTION` → l'utilisateur reçoit un statut inexplicable.

---

## 8. Décisions structurantes prises

### CLI plutôt qu'endpoint REST

Pas de dépendance Spring MVC, pas de serveur HTTP, pas de gestion de routes. La CLI est invocable depuis n'importe quel environnement JVM, testable directement avec `execute(String[])`.

### Fichiers texte comme artifacts

`workflow-summary.md`, `result.X.review.md`, etc. sont des fichiers Markdown locaux. Lisibles par un humain, un bot, ou un script, sans base de données ni format propriétaire.

### Format `key=value` pour stdout

Parsable avec n'importe quel langage sans bibliothèque JSON. Stable, extensible (nouvelles clés ignorées par les anciens consommateurs). Documenté explicitement dans `cli-reference.md`.

### Isolation stricte du runtime

La CLI et les simulateurs ne connaissent pas les types internes du runtime. Le contrat entre les couches est uniquement textuel (stdout) ou via le runner public. Le runtime reste modifiable sans impacter la couche externe.

### Pas de Telegram maintenant

Décision explicite de ne pas implémenter Telegram en Phase 5. Le système est préparé (contrat texte simple, documentation, simulateurs), mais le branchement est reporté pour éviter une intégration prématurée avant que les fondations soient complètement stables.

---

## 9. Recommandations pour la Phase 6

### Axe 1 — Gestion de session minimale

Créer un mécanisme simple (fichier de session temporaire ou configuration) pour mémoriser les paramètres d'un run courant (`reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber`). Cela rend la commande RESUME constructible automatiquement par un bot.

### Axe 2 — `TelegramMessageRenderer`

Créer un composant de rendu dédié Telegram, distinct de `WorkflowUserInteractionSimulator` :
- gestion de la limite 4096 caractères ;
- lecture du contenu de `workflowSummaryPath` et envoi comme texte ou fichier ;
- formatage Markdown Telegram.

### Axe 3 — Commande RESUME dans le summary

Inclure dans `workflow-summary.md` la commande exacte à exécuter pour le RESUME, avec les bons paramètres. L'opérateur ou le bot peut alors copier-coller la commande directement.

### Axe 4 — Mapping complet des décisions dans le simulateur

Ajouter `FINISH` et `RETRY_CORRECTION` dans `WorkflowCliConsumerSimulator` avec des statuts et messages explicites. Même si ces décisions ne sont pas produites actuellement, les mapper correctement évite des UNKNOWN silencieux en production.

### Axe 5 — Tests bot de bout en bout

Créer un test simulant le cycle complet depuis un point de vue bot :
- appel CLI (simulé via `WorkflowCliConsumerSimulator`) ;
- rendu message Telegram ;
- vérification que le contenu est en dessous de la limite de caractères ;
- cycle RESUME.

### Axe 6 — Consolider les helpers dupliqués dans les steps (optionnel)

Extraire `requiredString/requiredInt/requiredPath/optionalPath/optionalInt` dans une classe utilitaire `WorkflowContextAccessor`. Réduire la duplication dans `GlobalAnalysisStep`, `GlobalReviewStep`, `CorrectionStep`, `AnalysisReviewWorkflowRunner`. Cet axe est optionnel — uniquement si la Phase 6 touche plusieurs steps simultanément.

---

## Fichiers Phase 5 créés ou modifiés

### Code Java

| Fichier | Action |
|---|---|
| `AnalysisReviewWorkflowCli` | Créé — point d'entrée CLI externe |
| `AnalysisReviewWorkflowRunnerFactory` | Créé — factory standalone |
| `WorkflowCliConsumerSimulator` | Créé — parsing stdout + statuts |
| `WorkflowUserInteractionSimulator` | Créé — rendu texte utilisateur |
| `WorkflowCliConsumerSimulatorTest` | Créé — tests parsing |
| `WorkflowUserInteractionSimulatorTest` | Créé — tests rendu |
| Fichiers runtime existants | Non modifiés |

### Documentation

| Fichier | Contenu |
|---|---|
| `cli-reference.md` | Référence complète du contrat CLI |
| `sous-roadmap.md` | Roadmap Phase 5 (étapes 1 à 5) |
| `result.4_review.md` | Revue critique étape 4 |
| `result_5_analysis.md` | Analyse préparation Telegram |
| `result_5a_implements.md` | Rapport implémentation étape 5a |

---

## Conclusion

La Phase 5 remplit son objectif : le workflow est désormais appelable depuis l'extérieur, pilotable humainement, et préparé pour Telegram sans l'implémenter.

Le runtime est intact. L'architecture est propre et découplée. Le contrat est documenté.

Les manques restants (session, rendu Telegram, livraison du summary) sont clairement identifiés et forment une base solide pour démarrer la Phase 6.
