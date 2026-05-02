# Result — Analyse Phase 7 Étape 7 — Gestion des erreurs Telegram workflow

## Résumé exécutif

Le socle actuel est fonctionnel : la majorité des erreurs sont converties en messages utilisateur (via `WorkflowTelegramMessageRenderer` ou messages dédiés), la concurrence est protégée (lock + `runId`), et les vues `/workflow` / `/workflow_status` évitent les chemins internes.

Mais la gestion des erreurs n’est pas encore totalement “stabilisée” :

- **debug serveur insuffisant** : les exceptions du runner sont capturées puis stockées en session, mais **pas loggées** (perte de stacktrace côté serveur).
- **risque de fuite indirecte via `lastError`/`lastMessage`** : on réaffiche tel quel `exception.getMessage()` et/ou `WorkflowStepResult.message()` dans `/workflow_status` (ça peut contenir des paths/secrets selon l’origine).
- **standardisation partielle** : certains services renvoient des messages hors renderer (`/workflow_summary`, partiellement), et les libellés sont mixés FR/EN.

Conclusion : **partiellement prêt**. La prochaine itération doit se concentrer sur :
1) logging serveur systématique des exceptions (avec contexte) ;
2) sanitization + bornage des messages stockés/affichés ;
3) homogénéisation des formats de retour.

---

## 1) Inventaire des erreurs actuelles (par commande)

### `/workflow` (dashboard)
- Erreurs possibles : `chatId` null
- Message actuel : `Action impossible / Reason: Invalid request / Try: /workflow_status`
- Attendu : OK
- Données utiles session : aucune (pure lecture)

### `/workflow_status`
- Erreurs possibles : `chatId` null
- Message actuel : via renderer (Invalid request)
- Attendu : OK
- Données utiles session : `lastStatus`, `lastRunId`, `startedAt`, `updatedAt`, `lastError`, `lastMessage`

### `/workflow_run`
- Erreurs possibles :
  - invalid target (combinaisons params/contexte manquants)
  - double run
  - exceptions runtime dans `updateContext` (phase/etape <= 0) ou downstream
  - exception runner
  - timeout
  - result null
  - `STOP_FAILURE`
- Messages actuels :
  - invalid target : `Action impossible` avec reason issu du resolver
  - already running : `Action impossible` / `Workflow already running`
  - async failures : visibles via `/workflow_status` (Last error)
- Attendu :
  - message immédiat `RUNNING` clair + `runId`
  - erreurs “paramètres invalides” doivent rester user-friendly
- Données utiles session : `lastRunId`, `startedAt`, `updatedAt`, `lastError` (sanitisé), `lastMessage` (sanitisé)

### `/workflow_resume`
- Erreurs possibles :
  - pas en `WAITING_HUMAN`
  - run déjà en cours
  - contexte incomplet (phase/etape null)
  - exception runner / timeout / STOP_FAILURE
- Messages actuels : renderer errors + status consultable
- Attendu : OK (mêmes remarques que run)

### `/workflow_roadmap_load`
- Erreurs possibles :
  - path vide
  - sharedRoot inaccessible (IOException)
  - path hors `sharedRoot` (ForbiddenCommandException)
  - fichier absent / non lisible / extension != .md / vide
- Message actuel : renderer errors avec “Try: /workflow_roadmap_load path=<file.md>”
- Attendu : OK
- Données utiles session : `roadmapPath` (déjà) ; éventuellement `projectName` (déjà)

### `/workflow_summary`
- Erreurs possibles :
  - summary absent en session
  - path invalide / hors reportRoot
  - fichier absent / non lisible
  - lecture IO
  - contenu vide
  - contenu trop long
  - sender indisponible (doc non envoyé)
- Messages actuels : chaînes dédiées (`No summary available`, `Summary not found on disk`, etc.)
- Attendu :
  - idéalement format erreur standard (mais sans parsing profond)
- Données utiles session : `lastSummaryPath` (déjà)

---

## 2) Erreurs d’exécution workflow (runner)

Cas couverts côté orchestration :
- exception Java dans `runner.*` : catch `RuntimeException` → `sessionStore.failRun(..., safeError(e))`
- timeout : `.orTimeout(...).exceptionally(...)` → `failRun(..., timeoutError(ex))`
- `STOP_FAILURE` : `failRun(..., result.message())`
- `WAIT_HUMAN` : `completeRun(..., WAITING_HUMAN, ..., result.message())`
- résultat null : `failRun(..., "Workflow returned no result")`
- completion tardive : ignorée par le store si `runId` ne matche plus / état pas RUNNING

Problèmes à régler (niveau visibilité) :
- **User-visible** : une “raison courte” est OK, mais elle doit être **sanitisée**.
- **Server-visible** : il faut logger l’exception complète (stacktrace) avec contexte (`chatId`, `runId`, target) sinon debug difficile.

---

## 3) Erreurs de configuration

- `reportRootDirectory` / `reportVersion` :
  - orchestration : `Path.of(properties.getReportRootDirectory())` (peut throw au bootstrap si invalid)
  - summary : `reportRootDirectory` utilisé pour relativiser et valider (sinon “not found”)
- bot `Cortex` mal configuré / whitelist vide : warning au démarrage existe (Étape 6)
- roadmap absente : gérée via `/workflow_roadmap_load` + recommandation `/workflow`/`Next`
- phase/etape manquantes : erreurs explicites du resolver ou `INCOMPLETE_CONTEXT_MESSAGE` pour resume

Recommandation minimale :
- faire en sorte que les messages Telegram restent “Action impossible / Reason / Try” pour erreurs de config côté commande.

---

## 4) Erreurs de fichiers

- roadmap : validation forte via `WorkspaceLayout.resolveWithinRoot(sharedRoot, ...)` + checks existence/extension/blank
- summary :
  - validation path via `resolveWithinRoot(reportRootDirectory, relativeText, ...)`
  - lecture via `WorkspaceTextFileService`
  - split safe + document send si possible

Point d’attention :
- `/workflow_roadmap_load` utilise `Files.*` en direct (pas via workspace tool). C’est acceptable si `resolveWithinRoot` est la barrière de sécurité, mais ça diverge du pattern “workspace tool only”.

---

## 5) Erreurs de concurrence

Couverts :
- double run / double resume : `tryMarkRunning` → message immédiat `Workflow already running`
- timeout puis completion tardive : store refuse d’écraser si état != RUNNING ou runId mismatch

Cas UX à cadrer :
- `/workflow_summary` pendant RUNNING : peut afficher l’ancien summary si présent (car basé sur `lastSummaryPath`). Ce n’est pas dangereux mais peut être surprenant.

---

## 6) Standardisation recommandée

Format cible (ASCII) :

```text
Action impossible
Reason: <short>
Try: <command>
```

Règles :
- toujours proposer une commande utile (`/workflow_status`, `/workflow_summary`, `/workflow_roadmap_load ...`)
- ne jamais afficher stacktrace / chemins absolus
- bornage longueur (ex: 200–400 chars) sur `Reason`, et tronquage propre

---

## 7) Niveau de détail autorisé

Ne doit pas apparaître dans Telegram :
- stack traces, `\tat ...`
- tokens, API keys
- chemins absolus
- messages système complets (IOException détaillées, etc.)
- payloads longs

Peut apparaître :
- statut
- runId (déjà affiché dans status)
- raison courte (sanitisée)
- action recommandée
- summary disponible/missing

---

## 8) Logs

À viser :
- `WARN` : erreurs utilisateur (invalid target, not found) sans exception
- `ERROR` : exceptions runner/IO/timeouts avec stacktrace
- champs contexte : `chatId`, `userId`, `runId`, `project`, `phase`, `etape`
- jamais de secrets dans les logs

Écart actuel : orchestration ne log pas les exceptions runner (perte d’info debug).

---

## 9) Tests à prévoir

- timeout (déjà couvert partiellement)
- exception runner (vérifier `FAILED` + message safe)
- STOP_FAILURE → session FAILED + message sanitisé
- résumé long : sender dispo vs non dispo (déjà couvert)
- invalid target / missing context messages cohérents
- non-fuite : `/workflow` et `/workflow_status` (déjà couvert partiellement)
- completion tardive ignorée (déjà couvert)

À ajouter dans l’étape 7 implémentation : tests sur sanitization (paths, stacktrace) si ajoutée.

---

## 10) Recommandations

### À faire maintenant
1. Logger systématiquement les exceptions runner (avec `chatId`, `runId`, target) côté serveur.
2. Sanitiser + borner `lastError` et `lastMessage` avant stockage/affichage.
3. Harmoniser `/workflow_summary` sur le format d’erreur standard (sans parsing du contenu).

### À reporter
- audit trail complet
- retry automatique / alerting
- persistance session

---

## 11) Plan d’implémentation (5–10 étapes)

1. Introduire une fonction de sanitization (strip stacktrace markers, paths absolus, tronquage).
2. Appliquer sanitization avant `sessionStore.failRun(...)` et `completeRun(... lastMessage ...)`.
3. Ajouter logging `ERROR` dans `executeRun/executeResume` et dans le handler timeout.
4. Factoriser les messages d’erreur `/workflow_summary` via renderer (ou un helper léger) pour cohérence.
5. Ajouter tests unitaires sur sanitization + non-fuite.
6. Vérifier que les messages restent ASCII (ou décider explicitement emoji on/off).
7. Vérifier non-régression sur runId/race/timeout tests.

---

## Conclusion

**Partiellement prêt** : le système gère déjà les cas principaux, mais il manque la couche minimale “stabilisation erreurs” (logs + sanitization + cohérence inter-commandes) pour être robuste en exploitation.
