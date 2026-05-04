# Phase 8 — CodexCall — Étape 5 — Relecture (architecture + qualité)

## Contexte

Étape 5 : optimisation de l’appel Codex CLI dans `CodexWorkflowClient` :

- passage à `codex exec [--cd <dir>] -`
- prompt injecté via **stdin** (et non plus via argument CLI)
- pas d’imposition de sandbox
- conservation de la capture stdout/stderr et du timeout

Périmètre de la relecture : séparation config/runtime, découplage, absence de shell string, robustesse IO (stdin/stdout/stderr), tests réellement utiles, dette potentielle.

---

## Points vérifiés

- Commande construite via `List<String>` : OK (pas de `cmd /c`, pas de concat shell).
- Gestion des espaces dans les paths : OK (argument séparé `--cd`, `Path.toString()`).
- Prompt via stdin : OK (UTF-8, fermeture du flux).
- `workingDirectory` : OK (toujours validé + appliqué via `ProcessBuilder.directory(...)` et `--cd`).
- Timeout + capture stdout/stderr : conservés.
- Tests : ajoutés et ciblés sur la construction de commande (point clé).

---

## Faiblesses / points discutables

1) **Nouveau mode d’échec possible sur écriture stdin**
- `writePromptToStdin(...)` lève une `CodexExecutionException` en cas d’IOException (ex: pipe cassé si le process se termine immédiatement).
- Avant, un échec se matérialisait plutôt via exit code / stderr (et remontait dans `CodexExecutionResult`), sans exception au moment de “passer le prompt”.
- Risque : changement subtil de sémantique d’erreur (exception au lieu de résultat non-success) dans certains cas rares.

2) **Destruction du process sur erreur stdin**
- Le `process.destroy()` dans le catch évite de laisser un process zombie, mais peut aussi empêcher de récupérer un stderr déjà produit.
- Pour le debug, c’est parfois moins “exploitables” qu’un `CodexExecutionResult` complet.

3) **Tests un peu trop stricts sur `containsExactly(...)`**
- `containsExactly("codex", "exec", "--cd", dir, "-")` est précis, mais fragile si on ajoute plus tard une option non bloquante (ex: `--json`) même si le comportement reste correct.
- On veut tester les invariants (présence de `exec`, `--cd`, terminaison `-`, absence du prompt), sans figer la liste complète.

4) **Binary `codex` en dur**
- Ce n’est pas introduit par l’étape 5, mais c’est une contrainte implicite (dépendance au PATH / binaire dispo).
- Acceptable à court terme, mais à documenter si on vise une exécution multi-environnements.

---

## Corrections utiles (sans nouvelle fonctionnalité)

1) **Rendre l’échec d’écriture stdin non-exceptionnel (optionnel)**
- Alternative simple : si l’écriture échoue, tenter `process.waitFor(...)` rapidement et retourner un `CodexExecutionResult` non-success avec stderr/stdout capturés, au lieu de throw.
- Objectif : garder une sémantique de résultat (exitCode/stderr) plus stable.

2) **Rendre les tests moins fragiles**
- Remplacer `containsExactly(...)` par des assertions sur :
  - `command.get(0) == "codex"`
  - `command` contient `"exec"`
  - si workingDirectory : `command` contient `"--cd"` et le path comme argument séparé
  - `command.get(command.size()-1).equals("-")`
  - `command` ne contient pas le prompt

3) **Encodage des rapports**
- Même remarque que l’étape 4 : les fichiers rapport semblent en mojibake. À corriger dans la chaîne d’écriture/édition (qualité documentaire).

---

## Dette technique introduite

- Dette faible : gestion d’erreur stdin potentiellement plus “exception-driven” qu’avant.
- Dette de tests : assertions trop strictes peuvent freiner l’ajout de flags non bloquants (sans changer l’API).

---

## Résumé final

Étape 5 est **globalement très bonne** : elle aligne l’exécution sur `codex exec --cd ... -`, supprime les problèmes de quoting en passant le prompt via stdin, et respecte les contraintes (pas de sandbox imposée, pas de shell string, working directory supporté).

Les améliorations recommandées sont des ajustements de robustesse (stdin error path) et de résilience des tests (éviter `containsExactly`), sans élargir le périmètre fonctionnel.

