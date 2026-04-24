# Etape 3 - Résultat d'analyse

## 1. Analyse de l'existant

### 1.1 Structure workflow actuelle
Le module workflow contient déjà:
- `runtime.model` pour l'état runtime minimal
- `runtime.step` pour le contrat d'étape

La séparation est propre. Il est cohérent d'ajouter un package dédié au client technique Codex, séparé de `model` et `step`.

### 1.2 Exécution de process existante
Composants inspectés:
- `BashToolHandler`
- `BashRequest` / `BashResponse`
- `ToolExecutionService`

Constat:
- `BashToolHandler` est orienté "tooling agent" (whitelist de commandes, `WorkspaceService`, `BashSecurityService`, `ToolExecutionResult`).
- `ToolExecutionService` est couplé au système d'outils et aux permissions agent.
- Cette pile est utile pour les outils LLM, mais trop spécifique pour un client technique Codex workflow.

Conclusion:
- Réutilisation directe déconseillée (couplage opportuniste et sémantique inadaptée).
- Mieux vaut une implémentation locale minimale dans le module workflow runtime codex.

## 2. Package recommandé

`fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex`

Raison:
- séparation nette des responsabilités:
  - `runtime.model` = état
  - `runtime.step` = contrat d'étapes
  - `runtime.codex` = exécution technique CLI
- évite d'introduire un package trop profond.

## 3. Contrat minimal proposé

### 3.1 `CodexExecutionRequest`
Objet simple, fields minimaux:
- `prompt` (obligatoire)
- `workingDirectory` (optionnel, `String` ou `Path`)
- `timeoutSeconds` (optionnel, valeur par défaut raisonnable)
- éventuellement `arguments` optionnels simples (`List<String>`) si nécessaire

Règles:
- pas de champ métier workflow (lot, étape, décision, statut)
- validations minimales: `prompt` non blank, timeout > 0 si fourni
- pas de builder complexe.

### 3.2 `CodexExecutionResult`
Objet technique neutre:
- `command` (représentation utile de la commande)
- `exitCode`
- `stdout`
- `stderr`
- `success`
- `timedOut`
- éventuellement `durationMs`

Règles:
- pas d'interprétation métier
- pas de parsing intelligent.

### 3.3 `CodexWorkflowClient`
Responsabilité unique:
- reçoit un `CodexExecutionRequest`
- lance le process Codex CLI
- capte stdout/stderr, timeout, code retour
- renvoie `CodexExecutionResult`

Hors périmètre explicite:
- pas d'orchestration
- pas de retry
- pas de routing
- pas de décision métier.

## 4. Gestion d'exécution - approche minimale

Approche recommandée:
- `ProcessBuilder` local
- capture stdout/stderr via lecteurs dédiés
- `waitFor(timeout, TimeUnit.SECONDS)`
- destruction propre si timeout
- calcul simple de durée (`System.nanoTime`)

Points de robustesse basiques:
- limite raisonnable sur taille de sortie pour éviter explosion mémoire
- normalisation défensive (`null` -> "" pour stdout/stderr)
- mapping explicite des erreurs système en résultat technique (ou exception runtime dédiée technique)

## 5. Tests minimaux recommandés

1. `CodexExecutionRequestTest`
- prompt obligatoire
- timeout invalide rejeté

2. `CodexExecutionResultTest`
- construction nominale
- cohérence des champs basiques

3. `CodexWorkflowClientTest` (sobre)
- test d'échec commande introuvable (ou commande simulée triviale)
- test timeout sur commande bloquante uniquement si stable localement

Si les tests process sont jugés trop fragiles:
- limiter aux tests request/result + 1 test client de validation d'entrée.

## 6. Risques / vigilance

Risque principal:
- dérive vers un mini-orchestrateur dans le client.

Garde-fou:
- conserver `CodexWorkflowClient` au niveau "transport technique process" uniquement.

## 7. Ce qui sera implémenté à l'étape suivante
- `CodexExecutionRequest`
- `CodexExecutionResult`
- `CodexWorkflowClient`
- tests unitaires minimaux ciblés

## 8. Ce qui restera volontairement non implémenté
- orchestrator / workflow runner
- étapes métier
- décisions de suite (`continue`, `review`, etc.)
- intégration Telegram
- validation Maven
- parsing roadmap
