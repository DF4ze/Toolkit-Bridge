# Phase 7 — Rapport final — Intégration Telegram du workflow CodexTime (Toolkit-Bridge)

## 0) Sources analysées / état des fichiers

Rapports présents et utilisés (non exhaustif, mais couvrant toutes les étapes livrées) :

- `data/rapport/v1.1/CodexTime/Phase7/etape1/result_1_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape1/result_1_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape1/result_1_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2/result_2_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2A/result_2A_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2B/result_2B_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2C/result_2C_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2C/result_2C_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2D/result_2D_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2D/result_2D_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape2D/result_2D_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape4/result_4_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape4/result_4_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape4/result_4_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape4/result_4_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape5/result_5_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape5/result_5_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape5/result_5_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape5/result_5_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape6/result_6_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape6/result_6_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape6/result_6_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape6/telegram_workflow_security.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape7/result_7_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape7/result_7_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape7/result_7_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape7/result_7_correction.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape8/result_8_analysis.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape8/result_8_implements.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape8/result_8_review.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape8/phase7_telegram_workflow.md`
- `data/rapport/v1.1/CodexTime/Phase7/etape9/result_9_analysis.md`

Fichiers explicitement attendus dans la consigne mais absents ou vides (0 octet) dans le dossier Phase 7 au moment de la rédaction :

- `data/rapport/v1.1/CodexTime/Phase7/etape2A/result_2A_analysis.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2A/result_2A_implements.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2A/result_2A_review.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2B/result_2B_analysis.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2B/result_2B_implements.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2B/result_2B_review.md` (vide)
- `data/rapport/v1.1/CodexTime/Phase7/etape2C/result_2C_analysis.md` (vide)

Dans ce rapport final, les éléments de synthèse des étapes 2A/2B/2C reposent donc sur les fichiers de correction, d’implémentation et de revue réellement présents.

---

## 1) Résumé exécutif

Objectif initial de la Phase 7 : fournir une interface Telegram permettant de piloter le workflow CodexTime de manière distante, sans modifier le moteur workflow (runner/orchestrateur) ni la CLI, et en restant sur une intégration incrémentale (session in-memory, UX progressive, sécurité minimale, erreurs maîtrisées, stabilisation).

Résultat final obtenu : une couche Telegram fonctionnelle et stabilisée, avec commandes de pilotage (`/workflow_run`, `/workflow_status`, `/workflow_summary`, `/workflow_resume`, `/workflow_roadmap_load`) et un tableau de bord (`/workflow`) offrant une UX cohérente. L’exécution est asynchrone, corrélée par `runId`, la session est maintenue en mémoire par `chatId`, et l’accès fichiers (roadmap/summary) est borné à une racine autorisée.

Statut final : **terminé et clôturable** (confirmé par la relecture de stabilisation, Étape 8).

Niveau de maturité : **MVP exploitable** en environnement contrôlé (chat privé recommandé), avec dettes assumées (pas de persistance, pas de multi-user avancé, WebUI workflow non traitée, rate-limit/quota géré par heuristiques, etc.).

---

## 2) Périmètre initial de la Phase 7

Périmètre attendu (d’après la mini-roadmap et les prompts de phase) :

- Bot Telegram minimal (point d’entrée dédié au workflow).
- Lancement du workflow depuis Telegram.
- Affichage du summary dans Telegram avec gestion de limite 4096 chars.
- Gestion `WAIT_HUMAN` avec reprise contrôlée (resume).
- Amélioration UX (messages cohérents, dashboard, “next action”).
- Sécurité minimale (whitelist existante du module Telegram, avertissement).
- Gestion d’erreurs (sanitization + logs côté serveur + cohérence messages).
- Stabilisation (tests de cycle + doc + non-fuite).

Adaptations en cours de route :

- Passage d’une approche “Telegram -> CLI (ProcessBuilder)” évoquée tôt (Étape 2) à une exécution **Java in-process** (Étape 2C), pour respecter les contraintes de robustesse et limiter la complexité d’exécution.
- Découpage fin de l’étape 2 en sous-étapes `2A/2B/2C/2D` (runtime/session, roadmap, run, summary) pour contrôler les risques et stabiliser progressivement.

---

## 3) Synthèse par étape

### Étape 1 — Bot Telegram minimal

Livraison :

- Ajout d’un contrôleur Telegram dédié workflow, avec une commande d’entrée `/workflow`.
- Contrôleur isolé par bot via annotation `@TelegramController(bot = "Cortex")`.

Correction appliquée :

- Retrait des modifications sur le contrôleur “runtime agent” (`DefaultController`) afin de ne pas coupler l’interface workflow à la messagerie agent.
- Conformité au framework Telegram MVC : `@Command` et usage de l’attribut `bot` (et non `name`).

Décisions importantes :

- Telegram “workflow” = canal séparé de Telegram “runtime agent chat”.

État final :

- Point d’entrée Telegram présent et stable, sans interaction workflow à ce stade (pas de runner/CLI, pas de fichiers).

### Étape 2A — État runtime Telegram

Livraison (déduite de la correction + de l’état du code mentionné) :

- Mise en place/ajustement d’une session runtime par `chatId` (`WorkflowTelegramSessionStore`).
- Clarification du rôle de `phase + etape`.

Correction appliquée :

- Javadoc sur `WorkflowTelegramSession` : `phase + etape` représentent la **prochaine** étape à exécuter (et non la dernière complétée).
- Renommage `currentRunId` -> `lastRunId`.
- Validation `phase/etape > 0` dans `updateContext(...)`.
- UX du resolver : résolution possible via session (ex: `phase + etape` sans `projectName` si déjà connu).

Limite :

- Les rapports `result_2A_analysis.md`, `result_2A_implements.md`, `result_2A_review.md` sont vides : aucune synthèse additionnelle n’a pu être extraite.

### Étape 2B — Roadmap load

Livraison (déduite de la correction + de l’état du code mentionné) :

- Commande `/workflow_roadmap_load` pour charger une roadmap, sans parsing du contenu.
- Validation de base (fichier `.md`) et accès borné via un layout workspace (résolution “within root”).

Correction appliquée :

- Messages d’erreur homogènes (retour string user-friendly plutôt qu’exception).
- Détection de roadmap “vide” (whitespace-only) sans parsing.
- Nettoyage encodage (mojibake) dans service + tests.

Limite :

- Les rapports `result_2B_analysis.md`, `result_2B_implements.md`, `result_2B_review.md` sont vides.

### Étape 2C — Run workflow

Livraison :

- Commande `/workflow_run` : lancement du workflow **in-process** (pas de CLI, pas de `ProcessBuilder`), exécution asynchrone, timeout.
- Commande `/workflow_status` : consultation de l’état runtime.

Aspects techniques :

- Orchestration via `WorkflowTelegramOrchestrationService` :
  - résolution de la cible via `WorkflowTelegramRunTargetResolver`
  - lock via `tryMarkRunning(...)`
  - exécution async (executor)
  - timeout (valeur fixée au moment de l’étape 2C, puis configurée plus tard)
  - mapping statuts : `WAIT_HUMAN` -> `WAITING_HUMAN`, `STOP_FAILURE` -> `FAILED`, sinon `COMPLETED`
  - mémorisation de `workflowSummaryPath` en session (sans lecture).

Revue (points clés traités par les corrections ultérieures) :

- Problèmes d’encodage (mojibake) sur messages.
- Configuration hardcodée -> externalisation ultérieure.
- Lifecycle executor et risque de race condition (timeout/completion tardive) -> corrigé via corrélation `runId` (étapes ultérieures) et tests de stabilisation.

Limite :

- `result_2C_analysis.md` est vide.

### Étape 2D — Summary Telegram

Livraison :

- Commande `/workflow_summary` : lecture et affichage du `workflow-summary.md` depuis un path stocké en session (`lastSummaryPath`).
- Respect de la limite Telegram via :
  - `MAX_TELEGRAM_TEXT = 3800`
  - `MAX_CHUNKS = 2`
  - split prioritaire sur `\n` puis fallback brut
  - fallback document si > 2 chunks.

Contraintes respectées :

- Pas de parsing du summary.
- Accès disque via une couche “workspace” dédiée (pas de `Files.readString(...)` dans le service métier summary).
- Validation de path via `resolveWithinRoot` (root configurable `reportRootDirectory`).

Corrections UX ultérieures (Étapes 5 et 7) :

- Correction du message “summary too long” pour ne jamais prétendre envoyer un document si ce n’est pas vrai, et pour distinguer succès vs erreur.

### Étape 4 — WAIT_HUMAN / Resume

Livraison :

- Commande `/workflow_resume` pour reprendre un workflow uniquement quand la session est `WAITING_HUMAN`.
- Préconditions strictes : `lastStatus==WAITING_HUMAN`, `running==false`, `phase/etape` présents.
- Lock réutilisé (`tryMarkRunning`) pour éviter un double resume.

Choix d’architecture :

- Reprise via appel Java direct du runner : `runner.runCorrectionAfterReview(context)` (pas de CLI/process).
- Contexte RESUME : `reportRootDirectory`, `reportVersion`, `reportPhase`, `stepNumber` (sans `analysisSourcePath`).
- Règle explicitée : ne pas incrémenter `etape` dans le cas `WAITING_HUMAN` (phase/étape restent celles de l’étape suspendue).

Correction :

- Correction encodage messages (mojibake) et test robuste vérifiant la présence de “Workflow started” sans dépendre d’emoji.

### Étape 5 — UX Telegram

Livraison :

- `/workflow` devient un tableau de bord :
  - status, projet, phase, étape, roadmap (loaded/missing), commandes, recommandation Next.
- `/workflow_status` devient une vue synthétique :
  - status, projet, phase, étape, roadmap loaded/missing, summary available/missing
  - runId, startedAt, updatedAt, lastMessage/lastError, Next.
- Standardisation des messages succès/erreur sur plusieurs commandes (`/workflow_run`, `/workflow_resume`, `/workflow_roadmap_load`, erreurs summary).
- Logique simple de recommandation de prochaine action (informatif uniquement).

Correction (stabilisation UX) :

- “summary too long” ne ment plus sur l’envoi document.
- Suppression d’exposition de chemins internes (roadmap/summary) dans les vues `/workflow` et `/workflow_status` (remplacés par loaded/missing, available/missing).

### Étape 6 — Sécurité minimale

Livraison :

- Documentation sécurité minimale :
  - rattachement bot `Cortex`
  - whitelist `telegram.bots[].security.allowed-user-ids`
  - risque whitelist vide
  - usage en groupe déconseillé si infos sensibles.
- Warning au démarrage si whitelist vide pour `Cortex` (non bloquant).
- Tests non-fuite : `/workflow` et `/workflow_status` ne doivent pas exposer token/stacktrace/path absolu.

Décision structurante :

- Pas de double sécurité côté application (pas de check userId dans controllers/services workflow).

### Étape 7 — Gestion des erreurs

Livraison :

- Ajout d’un sanitizer d’erreurs `WorkflowTelegramErrorSanitizer` :
  - suppression stack trace / lignes `at ...`
  - redaction chemins absolus Windows/Unix -> `[path]`
  - redaction patterns secrets (token/api-key/authorization/bearer, etc.)
  - limitation de longueur (par défaut ~380 chars)
- Détection rate-limit/quota (patterns) et message user-friendly.
- Application de la sanitization avant stockage en session (fail/complete).
- Logs serveur enrichis (chatId/userId/runId/project/phase/etape) et stacktrace côté serveur uniquement.
- Homogénéisation format d’erreurs utilisateur (Reason / Try).

Correction UX summary trop long (success vs error) :

- Si document réellement envoyé : message de **succès** (pas “Action impossible”).
- Sinon : message d’erreur cohérent.

### Étape 8 — Stabilisation

Livraison :

- Tests de cycle :
  - `RUN -> WAITING_HUMAN -> RESUME -> COMPLETED` (runner mock)
  - vérification “late completion ignored” (cohérence `runId`)
  - `RUN -> FAILED` avec non-fuite (stacktrace + path Unix) et message sanitisé
  - non-fuite cross-OS : absence de `:\` et absence de `/home/` dans `/workflow` et `/workflow_status`.
- Documentation Phase 7 :
  - commandes et préconditions
  - distinction `telegram.*` vs `toolkit.telegram.workflow.*`
  - limites connues (session in-memory, groupe, etc.)
- Passe encodage pragmatique (suppression d’un caractère Unicode potentiellement sensible dans le sanitizer).

Verdict :

- Relecture Étape 8 : **VALIDATION** (Phase 7 clôturable).

### Étape 9 — Analyse WebUI

Constat :

- WebUI existante = UI admin/technique (agents/LLMs/bots, overview tasks/traces/artifacts/config).
- WebUI **ne pilote pas** le workflow (pas de run/status/summary/resume/roadmap load).
- Visibilité workflow seulement indirecte via tasks/traces/artifacts.

Recommandation :

- Ne pas coupler WebUI aux services Telegram (modèle chatId spécifique).
- Éventuelle future phase WebUI : commencer read-only, puis actions contrôlées après cadrage sécurité.

---

## 4) Ce qui a été implémenté (capacités finales)

### Commandes Telegram livrées (bot : `Cortex`)

- `/workflow` : dashboard (état + contexte + commandes + “Next”).
- `/workflow_roadmap_load path=<file.md> [project=<name>]` : charge une roadmap (accès borné, validation simple).
- `/workflow_run [project=<name>] [phase=<n>] [etape=<n>]` : lance un workflow (async).
- `/workflow_status` : affiche une vue synthétique de session.
- `/workflow_summary` : affiche le summary (1 ou 2 chunks) ou envoie le fichier si trop long.
- `/workflow_resume` : reprise après `WAITING_HUMAN` (préconditions strictes).

### Orchestration / exécution

- Exécution asynchrone (executor) + timeout.
- Protection double run/double resume via lock (`tryMarkRunning`).
- Corrélation par `runId` et gestion de completion tardive (stabilisée par tests).

### Session runtime

- Session in-memory par `chatId` via `WorkflowTelegramSessionStore`.
- Stockage du contexte (projet/phase/étape) et des chemins (roadmap/summary) avec règles de validation.

### Workspace / fichiers

- Roadmap : résolution bornée à une racine autorisée (layout), validation `.md` + non vide (whitespace-only).
- Summary : lecture via service “workspace”, pas de lecture directe `Files.*` dans service métier summary ; split + document fallback.

### UX / cohérence messages

- Messages standardisés (succès / erreur / running / waiting human).
- “Next action” informatif selon statut.
- Suppression d’exposition de chemins absolus (roadmap/summary) dans l’UX standard.

### Sécurité minimale

- Protection par whitelist du module Telegram (`allowed-user-ids`) + doc.
- Warning au démarrage si whitelist vide (bot `Cortex`).
- Recommandations d’usage (chat privé, groupe déconseillé si contenu sensible).

### Erreurs / observabilité

- Sanitization des erreurs (stacktrace/paths/secrets/longueur).
- Détection quota/rate-limit par patterns et message user-friendly.
- Logs serveur enrichis, sans fuite dans Telegram.

### Documentation et tests

- Docs Phase 7 (commandes + config + limites).
- Tests unitaires/services + tests de cycle (WAIT_HUMAN -> RESUME) + tests non-fuite cross-OS.

---

## 5) Ce qui n’a pas été fait (hors-scope assumé)

Les points suivants ne font pas partie de la Phase 7 livrée (reportés volontairement) :

- Persistance de session / runs (session in-memory uniquement).
- Multi-user avancé (groupes, roles, RBAC).
- Système de rôles / permissions applicatives côté workflow.
- Boutons inline Telegram, menus avancés, UX riche.
- Alias de commandes courtes (`/w_*`) et refonte de `/help` (explicitement évité).
- Parsing complet de roadmap (la roadmap est chargée/validée minimalement, sans parsing métier).
- Passage automatique à “étape suivante” / “phase suivante” (pas d’auto-advance).
- WebUI workflow (dashboard/pilotage workflow côté web).
- Audit trail complet, historisation, et observabilité “produit” (au-delà des logs serveur).
- Retry automatique avancé, stratégie de backoff, scheduling.
- Provider fallback / gestion multi-provider élaborée (quota/rate-limit par heuristique seulement).

---

## 6) Décisions structurantes

Décisions documentées par les rapports de phase :

1. Telegram est une couche d’interface (adapter) : pas de logique métier workflow, pas de modifications runner/CLI.
2. Exécution **Java in-process** plutôt que CLI/ProcessBuilder pour piloter le workflow depuis Telegram (robustesse + simplicité d’environnement).
3. Orchestration asynchrone, avec lock et corrélation `runId`.
4. Session runtime par `chatId`, stockée en mémoire (acceptée en MVP).
5. `phase + etape` représentent l’étape cible/suspendue selon le contexte ; en `WAITING_HUMAN`, l’étape n’est pas incrémentée.
6. Summary traité comme interface humaine principale (affichage + document fallback).
7. Sécurité minimale confiée au module Telegram (whitelist), sans double vérification dans les controllers/services applicatifs.
8. WebUI workflow non traitée dans Phase 7 ; analyse isolée pour cadrer une future phase.

---

## 7) Impacts techniques

Impacts principaux de la Phase 7 :

- Pilotage distant : possibilité de lancer/observer/reprendre un workflow depuis Telegram, sans accès local à la machine.
- Robustesse : réduction des risques de concurrence (double run/resume, completions tardives) grâce à `runId`, lock et tests de cycle.
- Exploitabilité : summary accessible depuis Telegram, gestion anti-limite (chunks/document), et messages plus guidants.
- Séparation des responsabilités : Telegram = adapter UI ; workflow engine inchangé.
- Observabilité : logs serveur enrichis + messages Telegram sanitisés et cohérents.

Impacts / risques restants :

- Session in-memory : perte au redémarrage, pas d’historique, pas de reprise “après crash”.
- Contexte par chatId : en groupe, l’état est partagé et les messages visibles à tous.
- Surface web existante : endpoints utilitaires (`/api/command/run`, `/api/files/*`) identifiés par l’audit WebUI comme potentiellement sensibles (hors périmètre Phase 7, mais à garder en tête).

---

## 8) Dettes techniques assumées

Dette 1 — Session non persistée :

- Acceptable en Phase 7 : MVP, faible complexité, limitation des risques de stockage.
- À reprendre : phase dédiée persistance (DB), modèle “workflow run” durable, migration du store in-memory.

Dette 2 — Modèle centré Telegram (`chatId`) :

- Acceptable : adapter Telegram, pas de besoin multi-interface immédiat.
- À reprendre : couche UI-agnostic “workflow run service/facade” pour mutualiser (WebUI, CLI, etc.).

Dette 3 — Bot `Cortex` comme référence :

- Acceptable : intégration pragmatique.
- À reprendre : rendre configurable le bot cible et clarifier isolation (notamment en multi-bots/environnements).

Dette 4 — Rate-limit/quota par patterns :

- Acceptable : amélioration rapide de l’UX d’erreur sans moteur complexe.
- À reprendre : classification plus fine (provider, quotas Codex spécifiques, stratégies d’attente, fallback).

Dette 5 — Roadmap non parsée :

- Acceptable : contrainte explicite ; évite de figer un format trop tôt.
- À reprendre : parsing roadmap et modèle “étape suivante” uniquement quand la roadmap devient contractuelle.

Dette 6 — WebUI workflow absente :

- Acceptable : périmètre Phase 7 centré Telegram.
- À reprendre : WebUI read-only puis actions contrôlées, avec exigences sécurité.

Dette 7 — Tests non-fuite Unix partiels :

- Acceptable : couvre le cas ciblé (`/home/`), + Windows.
- À reprendre : élargir à d’autres patterns (`/var/`, `/etc/`, etc.) si besoin.

Dette 8 — Bruit de logs en tests :

- Acceptable : les tests déclenchent volontairement des exceptions/erreurs, cohérent pour valider le logging.
- À reprendre : configuration logging de test si nécessaire (non bloquant).

---

## 9) Dérives utiles et adaptations

Écarts constatés vs mini-roadmap initiale (sains et assumés) :

- Découpage 2A/2B/2C/2D : amélioration de la maîtrise de scope et de la stabilisation incrémentale.
- Passage de “Telegram -> CLI” (Étape 2 initiale) à “Telegram -> runner Java in-process” : simplification d’environnement, respect des contraintes de non-régression, réduction de fragilité classpath/jar.
- Summary traité comme interface humaine de référence (affichage + document fallback) : amélioration UX et exploitabilité.
- UX Telegram plus riche (dashboard + next action + messages standardisés) : dérive utile, car elle améliore l’usage réel sans changer le moteur.
- Traitement “propre” de sécurité minimale + sanitization erreurs : dérive positive (stabilisation “pro”).
- Analyse WebUI ajoutée en fin de phase : utile pour cadrer la suite, sans modifier l’existant.

Points à surveiller :

- La documentation/rapports contiennent encore des traces de mojibake dans certains fichiers historiques (ex: `sous-roadmap.md`), sans impact fonctionnel mais à nettoyer si ces documents sont réutilisés comme sources de vérité.

---

## 10) WebUI : état et recommandation

État (Étape 9) :

- WebUI existante : administration et supervision technique (agents, tasks, traces, artifacts, configuration, retention, bots Telegram).
- WebUI ne fournit pas de capacités workflow équivalentes à Telegram (pas de run/status/summary/resume/roadmap load).

Recommandation :

- Ne pas réutiliser directement les services Telegram (session par chatId + renderer Telegram).
- Créer une couche “workflow run” UI-agnostic si une WebUI workflow est envisagée.
- Démarrer en read-only, puis ajouter des actions seulement après cadrage sécurité (auth, restrictions, logs).

---

## 11) Configuration globale : points à reporter

Besoins émergents à remonter (au-delà de la Phase 7) :

- Rendre configurable le bot “workflow” (ne pas supposer `Cortex` comme unique cible).
- Centraliser et clarifier la configuration :
  - module Telegram (`telegram.*`) vs workflow Telegram (`toolkit.telegram.workflow.*`)
  - sources (YAML, env vars, secrets)
- Clarifier les choix de persistance (in-memory vs DB) et les impacts (redémarrage, reprise, audit).
- Poser une base de configuration cohérente pour l’extension multi-interface (Telegram/WebUI/CLI).

---

## 12) Quota / rate-limit : points à reporter

État Phase 7 :

- Ajout d’une détection simple par patterns (quota/rate limit/429/weekly/5h, etc.) et message utilisateur standardisé.

À reporter :

- Typologie plus fine des limites (Codex 5h vs weekly, rate-limit provider API, etc.).
- Stratégies : attente/backoff, report, notifications, bascule provider/model.
- Observabilité : métriques et agrégation (au-delà des logs).

---

## 13) Tests et validation

Validations explicitement mentionnées dans les rapports :

- Exécution tests : `./mvnw test` ou `./mvnw -q test` (plusieurs étapes).
- Tests unitaires et de service sur :
  - orchestration run/resume + mapping statuts
  - summary (absent/introuvable/vide/court/moyen/long + document)
  - roadmap load (validation simple + messages)
  - sanitizer (stacktrace/path win/unix/longueur + quota/rate-limit)
- Tests de cycle (Étape 8) :
  - `RUN -> WAITING_HUMAN -> RESUME -> COMPLETED`
  - late completion ignored (cohérence état)
  - `RUN -> FAILED` + non-fuite
  - non-fuite cross-OS (Windows + `/home/`).

Limites connues de couverture :

- Non-fuite Unix : test ciblé `/home/` (les autres patterns Unix ne sont pas tous couverts).
- Tests E2E Telegram réels (réseau) non inclus (volontaire).

---

## 14) Recommandations futures

### À court terme

- Produire un **rapport global CodexTime** s’appuyant sur ce rapport Phase 7 (consolidation décisions + dettes).
- Réaligner les roadmaps parentes et clarifier la stratégie d’interface (Telegram vs WebUI).
- Clarifier et centraliser la configuration (bot cible, secrets, paramètres workflow).
- Définir un modèle “workflow run” plus générique (UI-agnostic) si d’autres UIs arrivent.

### À moyen terme

- WebUI workflow read-only (liste des runs/états/liens vers artifacts/summaries) avec authentification stricte.
- Persistance session/run (DB) et gestion de reprise après redémarrage.
- Parsing roadmap (si besoin de progression automatique), en gardant la compatibilité de l’existant.
- Classification plus robuste des erreurs provider (catégories + remédiations).

### À long terme

- Moteur de workflow plus générique (UI-agnostic, observabilité, audit).
- Multi-user, rôles, restrictions fines (Telegram + Web).
- Audit trail, alerting, gouvernance sécurité.
- Provider fallback et stratégie de capacité (quotas, priorités, throttling).
- WebUI de pilotage (actions) uniquement après maturité sécurité et persistance.

---

## 15) Conclusion

La Phase 7 est **terminée et clôturable** : elle fournit une interface Telegram stable pour piloter le workflow CodexTime (run/status/summary/resume/roadmap) avec une UX cohérente, une sécurité minimale explicite (whitelist + warning), et une gestion d’erreurs sanitisée et loggée côté serveur. Les tests de stabilisation (dont cycle WAIT_HUMAN -> RESUME) et la documentation de configuration confirment la maturité MVP.

Cette phase transforme CodexTime d’un moteur/CLI “local” en un assistant pilotable à distance, tout en conservant l’invariance fondamentale : **le moteur workflow reste inchangé** et Telegram reste un adapter.

La suite logique est la production d’un rapport global CodexTime et la décision sur la trajectoire d’industrialisation (persistance, modèle UI-agnostic, WebUI read-only, gestion avancée des quotas/providers), en gardant les dettes explicitement documentées ci-dessus.

