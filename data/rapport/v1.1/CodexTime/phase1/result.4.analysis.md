# Etape 4 - Résultat d'analyse

## Analyse de l'existant
- Un module `service.agent.artifact` existe déjà mais il est orienté artefacts agentiques génériques (métadonnées, content store, policy).
- Pour éviter couplage opportuniste, la brique demandée doit rester dans le sous-espace workflow runtime.

## Package retenu
- `fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact`

Raison:
- cohérence avec `runtime.model`, `runtime.step`, `runtime.codex`
- séparation claire des responsabilités
- profondeur raisonnable

## Modèle minimal retenu
1. `WorkflowArtifactType`
- centralise le nommage des artefacts (analysis/implements/review/correction, prompt/result)
- évite les magic strings

2. `WorkflowArtifactService`
- construit un `Path` d'artefact à partir d'un contexte simple
- écrit un artefact texte
- lit un artefact texte
- vérifie l'existence
- crée les répertoires nécessaires à l'écriture

## Hors périmètre explicite
- aucune logique de pilotage workflow
- aucun parsing sémantique
- aucun lien orchestrator/step/codex/telegram/maven

## Tests minimaux prévus
- nommage de fichier par type/étape
- construction de chemin
- écriture puis lecture
- test d'existence
