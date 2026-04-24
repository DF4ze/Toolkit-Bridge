# Revue critique d'architecture - Phase 2 Etape 2 (`GlobalAnalysisStep`)

## Portee relue

- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalAnalysisStep.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/workflow/runtime/step/GlobalAnalysisStepTest.java`
- verification de coherence avec `WorkflowOrchestrator`

## 1) Faiblesses / points discutables

### [P2] Capture d'erreurs trop large dans la step
- Localisation: `GlobalAnalysisStep.java:41-102`
- Constat: `catch (Exception e)` encadre toute la methode et convertit indistinctement toute erreur en `STOP_FAILURE`.
- Risque: masque des erreurs de programmation (NPE, bug logique) qui devraient remonter clairement en phase dev/test, et rend le diagnostic plus difficile.

### [P3] Contrat de variables non type (cles String dispersables)
- Localisation: `GlobalAnalysisStep.java:18-24`, `125-196`
- Constat: la step repose sur des cles `Map<String,Object>` et conversions dynamiques `Object -> Path/int/String`.
- Risque: fragilite aux fautes de frappe et aux variations de type dans les etapes suivantes; couplage implicite au lieu d'un contrat lisible centralise.

### [P3] Couverture de tests utile mais incomplète sur les branches d'echec fonctionnel
- Localisation: `GlobalAnalysisStepTest.java`
- Constat: les tests couvrent nominal, variable manquante, exception Codex, ecriture d'artefacts, mais pas le cas `CodexExecutionResult.success == false` (sans exception).
- Risque: regression possible sur la branche `STOP_FAILURE` metier quand Codex repond mais en echec.

### [P3] Prompt minimal correct mais message de sortie d'echec peu structure
- Localisation: `GlobalAnalysisStep.java:79-81`, `100-101`
- Constat: messages d'echec sont textuels et heterogenes selon le chemin.
- Risque: observabilite moindre pour les prochaines phases, surtout pour distinguer erreur de contexte, erreur I/O source, erreur Codex non-success, erreur artefact.

## 2) Corrections utiles (sans ajout de fonctionnalite hors perimetre)

1. **Restreindre le bloc `catch`**
- Capter explicitement les erreurs attendues du flux (variables invalides, lecture source, Codex, artefacts), et laisser remonter les erreurs de programmation inattendues.
- Effet: meilleur signal de bug sans changer le comportement metier principal.

2. **Centraliser formellement les cles de variables et de data**
- Conserver le `Map<String,Object>` (pas de nouvelle abstraction lourde), mais regrouper les cles dans une zone unique stable et verifier leur usage de maniere uniforme.
- Effet: reduction du risque de typo/couplage implicite, sans enrichir le runtime.

3. **Completer un test d'echec fonctionnel Codex non-exception**
- Ajouter un test ou `CodexExecutionResult.success=false` et verifier `STOP_FAILURE` + artefacts ecrits.
- Effet: verrouille la branche metier d'echec deja codee.

4. **Uniformiser les messages d'echec**
- Garder des messages simples mais plus coherents (prefixe commun + cause courte).
- Effet: meilleure lisibilite de logs/rapports sans sur-conception.

## 3) Verification des axes demandes

- **Separation configuration/runtime**: correcte. La step reste runtime, sans melange avec config applicative.
- **Qualite du modele implemente**: bonne pour une V1 minimaliste; points de robustesse ci-dessus.
- **Decouplage orchestrator/memoire/tooling/policy/workspace**: bon. Aucune dependance parasite introduite.
- **Absence de logique ad hoc trop specifique**: globalement bonne; prompt volontairement simple.
- **Couplage futur**: principal risque sur cles String non typees.
- **Cohérence des noms**: bonne (`GlobalAnalysisStep`, variables explicites, `promptArtifactPath`/`resultArtifactPath`).
- **Lisibilite generale**: bonne, methode lineaire, intention claire.
- **Tests utiles**: oui, socle pertinent; une branche d'echec metier manque.
- **Dette technique introduite**: moderee et maitrisable (gestion d'erreur globale + contrat par cles).
- **Risques de refactor futur evitables maintenant**: oui, surtout en clarifiant gestion d'erreur et contrat des cles sans changer l'architecture.

## 4) Resume final

L'implementation est **coherente et globalement saine** pour le perimetre minimal de l'etape 2.

Les points a corriger en priorite (toujours sans sur-conception) sont:
1. reduire la capture d'erreurs trop generale,
2. fiabiliser le contrat des cles de contexte/data,
3. completer un test sur l'echec Codex non-exception.

Ces ajustements restent legers et evitent une dette de diagnostic/couplage pour les phases suivantes.