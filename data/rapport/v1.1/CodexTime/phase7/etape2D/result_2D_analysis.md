# Phase 7 — Étape 2D — Analyse (affichage `workflow-summary.md` dans Telegram)

## Contexte projet (rappel)

Conformément au cadrage `data/rapport/00.promptWorkflow/0.workflow.md` et `data/rapport/00.promptWorkflow/1.Cadre.md` :
- objectif : ajouter une UX exploitable sans dérive, simple et testable
- méthode : analyser avant de coder, identifier fichiers/risques, proposer un plan 5–10 étapes
- contrainte 2D : ne pas modifier le runner, ne pas modifier la CLI, ne pas parser le summary en profondeur, rester simple

---

## 0) Ce qui existe déjà (points d’ancrage)

Le workflow écrit un artefact `workflow-summary.md` et expose son chemin via `workflowSummaryPath`.
Dans la couche Telegram :
- la session par chat conserve `lastSummaryPath` : `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSession.java`
- l’orchestrateur persist(e) ce `lastSummaryPath` quand un run se termine : `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramOrchestrationService.java`
- l’entrée Telegram est un controller à commandes : `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/workflow/WorkflowTelegramController.java`

Ces 3 points sont suffisants pour ajouter une commande `/workflow_summary` sans toucher au runner.

---

## 1) Comment lire le summary ?

Source de vérité côté Telegram : `WorkflowTelegramSession.lastSummaryPath()`.

Lecture recommandée (simple, robuste) :
- récupérer la session depuis le `WorkflowTelegramSessionStore` (par `chatId`)
- prendre `lastSummaryPath` (peut être `null`)
- valider le chemin (sécurité, cf. §7)
- lire le fichier en UTF‑8 (`Files.readString(path, UTF_8)`)
- gérer les erreurs :
  - `null` → “Aucun summary disponible (lance un run d’abord).”
  - fichier absent / supprimé → “Summary introuvable sur disque.”
  - accès refusé → “Accès non autorisé au fichier.”

Note : ne pas “recalculer” le chemin ni déduire le path à partir d’autres champs ; on s’appuie sur celui stocké en session (cohérence avec le run).

---

## 2) Gestion de taille (limite Telegram ~4096)

Objectif : ne jamais dépasser la limite “message text”.

Pratique recommandée :
- choisir une marge : ex. `MAX_TELEGRAM_TEXT = 3800` (réserve pour en-têtes “(1/3)” + aléas encodage)
- si `content.length() <= MAX_TELEGRAM_TEXT` : envoyer en 1 message
- sinon :
  - stratégie “chunks” (split en plusieurs messages) jusqu’à une limite raisonnable (anti-spam)
  - au-delà, fallback “document” (envoi du fichier en pièce jointe) si techniquement disponible dans la lib Telegram utilisée

Important : faire le split sur des frontières “safe” (idéalement sur `\n`) pour préserver la lisibilité.

---

## 3) Stratégies comparées (recommandation)

### A) Troncation simple
✅ ultra simple, zéro dépendance.
❌ perd de l’info précisément quand c’est le plus utile (gros run, beaucoup d’actions/artifacts).

Usage : acceptable uniquement si on propose aussi un fallback “envoyer en fichier”.

### B) Split en chunks (recommandé par défaut)
✅ garde 100% du contenu.
✅ pas besoin de “comprendre” le markdown (respect contrainte “pas de parsing profond”).
⚠️ risque spam si le summary est énorme.

Mitigation :
- limite de messages (ex. max 5)
- si > 5 chunks : envoyer en document + envoyer juste un extrait (premiers ~3800 chars) en texte

### C) Résumé automatique
❌ contredit “ne pas parser en profondeur” (et nécessite une logique de synthèse/LLM).
❌ risque de déformer le diagnostic.

À exclure ici.

### D) Envoi document (fallback “pro”)
✅ aucune limite 4096.
✅ conserve le markdown original.
⚠️ nécessite support “sendDocument” côté lib/bot.

Recommandation : chunking en texte par défaut + fallback document si trop long.

---

## 4) Format Telegram (lisibilité + robustesse)

Le piège principal n’est pas le texte, mais le *parse mode* (Markdown/HTML) :
- MarkdownV2 impose un escaping strict, sinon erreurs/symboles mangés
- le summary peut contenir des caractères spéciaux (backticks, underscores, etc.)

Recommandation “safe” (simple) :
- envoyer en **texte brut** (sans parse mode), éventuellement précédé d’un petit en-tête
- optionnel : encapsuler dans un bloc “code” uniquement si votre sender gère bien l’échappement (sinon rester brut)

Structure minimale du message :
- `Workflow summary (N/M)` en première ligne
- le contenu ensuite (brut)

---

## 5) Commande `/workflow_summary`

Endroit naturel :
- ajouter une méthode `@Command("/workflow_summary")` dans `WorkflowTelegramController`
- déléguer au `WorkflowTelegramOrchestrationService` (ou un service dédié “summary display” si vous voulez garder l’orchestrateur plus fin)

Comportement :
- récupère la session (par chatId via ctx)
- lit et envoie le contenu (chunk/doc)
- répond explicitement dans les cas d’erreur (cf. §6)

---

## 6) UX (cas à couvrir)

1) `lastSummaryPath == null`
- Message : “Aucun summary disponible. Lance `/workflow_run` puis réessaie.”

2) fichier absent
- Message : “Summary introuvable sur disque (chemin enregistré: …).”
- (option) suggérer de relancer le run

3) fichier vide / blanc
- Message : “Summary vide.”

4) fichier OK (<= 3800 chars)
- 1 message

5) fichier long
- chunks numérotés (1/N …)
- si trop long (au-delà du max chunks) : fallback document + extrait

---

## 7) Sécurité (path valide uniquement / pas d’accès hors workspace)

Même si le path provient du workflow, on ne fait pas confiance aveuglément :
- définir une *base directory* autorisée (ex. workspace root `D:\Documents\Spring\Toolkit-Bridge` ou sous-répertoire `data\rapport`)
- résoudre en chemin canonique :
  - `base = baseDir.toRealPath()`
  - `resolved = summaryPath.toRealPath()`
- vérifier `resolved.startsWith(base)` ; sinon refuser (message “Chemin non autorisé”)

Refuser également :
- chemins `null`
- chemins relatifs ambigus (forcer la résolution via `base.resolve(path).normalize()` si vous acceptez le relatif)

Objectif : empêcher toute lecture arbitraire de fichiers (ex. `C:\Windows\…`) via corruption de session / bug / injection.

---

## 8) Tests (ciblés, simples)

Objectif : valider le comportement sans runner/CLI.

Tests unitaires recommandés :
- `readSummary` :
  - path absent → message d’erreur stable
  - path présent, UTF‑8 → contenu exact
  - path vide → “Summary vide”
- `chunking` :
  - < max → 1 chunk
  - = max → 1 chunk
  - > max → N chunks + numérotation
  - split sur `\n` quand possible
- `path validation` :
  - chemin dans base → OK
  - chemin hors base → refus

Tests d’intégration légers (si existants patterns) :
- commande `/workflow_summary` retourne une réponse attendue quand session a `lastSummaryPath`

---

## 9) Risques & mitigations

- Spam Telegram (summary énorme)
  - limiter le nombre de messages (ex. 5)
  - fallback document au-delà
- Messages trop longs
  - chunk size à 3800
  - compter aussi l’en-tête par chunk
- Markdown “cassé”
  - envoyer en texte brut (sans parse mode) pour une robustesse maximale
- Fuite de fichier (path traversal)
  - canonical path + startsWith(base)

---

## 10) Plan d’implémentation (5–10 étapes)

1) Identifier le point d’accès session (chatId → session) et l’API d’envoi de messages disponible dans la stack Telegram.
2) Ajouter la commande `/workflow_summary` dans `WorkflowTelegramController` (signature cohérente avec les autres commandes).
3) Implémenter un petit service “SummaryReader” (lecture + validation path + UTF‑8) ou le mettre dans l’orchestrateur si vous préférez minimal.
4) Implémenter `chunkTextByLines(content, maxLen, maxChunks)` avec numérotation.
5) Implémenter une stratégie de fallback : si > maxChunks → envoyer document (si possible) + extrait texte.
6) Cadrer la base directory autorisée (config/properties) et ajouter la validation canonique.
7) Ajouter les tests unitaires (lecture, chunking, sécurité).
8) Vérifier la non-régression : commandes existantes (`/workflow_run`, `/workflow_status`) inchangées, runner/CLI inchangés.

