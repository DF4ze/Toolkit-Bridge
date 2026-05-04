# Phase 8 — CodexCall — Étape 5 — Correction — Stabilisation tests Codex CLI

## Contexte

L’étape 5 a modifié `CodexWorkflowClient` pour utiliser :

- `codex exec --cd <workingDirectory> -` (si `workingDirectory` présent)
- `codex exec -` (sinon)

avec injection du prompt via stdin.

La review a signalé que certains tests étaient trop stricts (assertion sur la liste complète des arguments), ce qui rend les tests fragiles si on ajoute plus tard des options non bloquantes.

## Objectif

Assouplir les tests pour vérifier les **invariants importants** sans figer la commande complète, sans modifier le runtime.

## Correction appliquée

Dans `CodexWorkflowClientTest` :

- remplacement de `containsExactly(...)` par des assertions sur :
  - premier argument = `codex`
  - présence de `exec`
  - présence/absence de `--cd` selon le cas
  - `--cd` suivi du path comme argument séparé (quand présent)
  - dernier argument = `-`
  - le prompt n’est pas présent dans les arguments
  - sécurité de base : pas de `cmd`, pas de `/c`

## Fichiers modifiés

- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/codex/CodexWorkflowClientTest.java`

## Tests

Commande exécutée :

```text
.\mvnw.cmd test
```

Résultat : OK.

## Confirmation (runtime inchangé)

- ✔ `CodexWorkflowClient` inchangé (pas de modification runtime).
- ✔ Aucune modification Telegram.
- ✔ Pas de changement sur `/workflow_run`, `/workflow_resume`, `codexWorkingDirectory`.
- ✔ Pas d’ajout `--sandbox`, `--json`, `--output-last-message`.

## Dette conservée

Le point suivant est volontairement conservé (hors scope de cette correction) :

- `TODO: réfléchir à une sémantique plus stable pour les erreurs d’écriture stdin (exception vs result).`

