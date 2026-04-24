# Revue critique d'architecture - Phase 2 Etape 3 (runner local)

## Portee

Fichier principal relu:
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/GlobalAnalysisWorkflowRunnerTest.java`

Contexte de coherence verifie avec:
- `GlobalAnalysisStep`
- `WorkflowOrchestrator`

## 1) Faiblesses / points discutables

### [P2] Assert de decision fragile (string au lieu d'enum)
- Localisation: `GlobalAnalysisWorkflowRunnerTest.java:66`
- Constat: assertion `result.decision().name() == "CONTINUE"`.
- Risque: moins robuste qu'une comparaison directe sur l'enum; fragilise la maintenance (renommage, typo string).

### [P2] Test d'integration potentiellement non observable en CI/locaux sans Codex
- Localisation: `GlobalAnalysisWorkflowRunnerTest.java:26`, `86-94`
- Constat: test skippe si `codex` absent, ce qui est volontaire et bon pour la stabilite.
- Risque: lot "vert" sans execution end-to-end effective sur certains environnements.

### [P3] Couplage ad hoc a des chemins de projet fixes
- Localisation: `GlobalAnalysisWorkflowRunnerTest.java:28`, `54-60`
- Constat: chemins hardcodes (`data/rapport/...`, `Path.of(".")`).
- Risque: faible mais present si execution depuis un cwd inattendu; peut reduire la portabilite locale.

### [P3] Strategie d'observabilite minimale
- Localisation: ensemble du test
- Constat: assertions sobes (bien), mais pas de log explicite sur les chemins d'artefacts en sortie.
- Risque: debug manuel un peu plus lent en cas d'echec local.

## 2) Corrections utiles (sans ajout de fonctionnalite hors perimetre)

1. Remplacer l'assert string par enum:
- `assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);`

2. Conserver le `assumeTrue` (bonne pratique ici), mais documenter dans le nom/commentaire du test qu'il est "local et explicite" pour eviter toute ambiguite d'usage.

3. Limiter le couplage aux chemins relatifs en fixant explicitement le `workingDirectory` sur la racine projet resolue depuis le test (sans nouvelle abstraction).

4. Ajouter une assertion/context info simple en cas d'echec (ex: verifier parent des artefacts attendu) pour faciliter le diagnostic local, sans sur-specifier le contenu Codex.

## 3) Verification des axes demandes

- Separation configuration/runtime: **OK**. Le runner reste en test, sans couche config additionnelle.
- Qualite du modele implemente: **OK** pour un runner minimal; contexte explicite et dependencies directes.
- Decouplage orchestrator/memoire/tooling/policy/workspace: **OK**. Aucun couplage parasite introduit.
- Absence de logique ad hoc trop specifique: **globalement OK**; seuls les chemins fixes sont un point mineur.
- Absence de couplage bloquant futures phases: **OK**. Rien de structurellement bloquant.
- Coherence des noms: **OK** (`GlobalAnalysisWorkflowRunnerTest`, `runsWorkflowLocallyAndGeneratesAnalysisArtifacts`).
- Lisibilite generale: **bonne**.
- Tests utiles: **oui**, test local concret et garde-fou sur disponibilite `codex`.
- Dette technique introduite: **faible** (assert string + chemins fixes).
- Risques de refactor futur evitables maintenant: **oui**, via corrections 1 et 3 ci-dessus.

## 4) Resume final

L'implementation est conforme au besoin: un **point d'entree local minimal**, concret, sans sur-conception.

Points a corriger utilement (leger, sans changer l'architecture):
1. comparer la decision avec l'enum plutot qu'une string,
2. reduire legerement la dependance au cwd par un chemin de travail plus explicite,
3. clarifier le caractere "local/explicite" du test pour l'usage equipe.

Globalement: lot sain, dette technique faible, pas de couplage majeur introduit.