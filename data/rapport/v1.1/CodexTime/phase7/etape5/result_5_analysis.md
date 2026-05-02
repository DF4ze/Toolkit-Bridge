# Phase 7 — Étape 5 — Analyse UX Telegram workflow

## Résumé exécutif

Le module Telegram workflow est déjà fonctionnel pour piloter un run, suivre son état, consulter le summary et reprendre un `WAITING_HUMAN`. En revanche, l’UX reste encore "backend-first" : messages hétérogènes, manque de guidance, vue d’ensemble faible, et plusieurs retours trop techniques ou trop pauvres pour un usage fluide dans Telegram.

Le système est **partiellement prêt** pour l’implémentation UX de cette étape.

Il est prêt sur trois points :
- les données utiles existent déjà dans la session (`projectName`, `phase`, `etape`, `roadmapPath`, `lastSummaryPath`, `lastRunId`, `startedAt`, `updatedAt`, `lastStatus`, `lastMessage`, `lastError`)
- les commandes principales existent déjà
- l’orchestration async est suffisamment stable pour supporter une couche UX plus claire

Il reste à harmoniser l’affichage, clarifier les prochaines actions et mieux exposer l’état courant.

## Inventaire des commandes actuelles

### `/workflow`

Rôle :
- point d’entrée général du module workflow

Message actuel :
- `Workflow Telegram interface is available but not connected yet.`

Limites UX :
- message obsolète par rapport aux fonctionnalités réellement disponibles
- aucune vue synthétique de l’état courant
- ne guide pas l’utilisateur vers la prochaine commande utile

Améliorations possibles :
- en faire l’écran d’accueil du module
- afficher état rapide + commandes disponibles
- recommander une commande selon le statut courant

### `/workflow_run`

Rôle :
- lancer un workflow

Message actuel :
- `Workflow started`
- puis une ligne `Target: project=... phase=... etape=...`

Limites UX :
- format peu lisible
- pas de structure succès / prochaine action
- mélange style "console" et style conversationnel
- ne rappelle pas explicitement `/workflow_status`

Améliorations possibles :
- formater en succès standard
- détailler projet / phase / étape proprement
- ajouter "commande suivante recommandée"

### `/workflow_status`

Rôle :
- afficher l’état courant

Message actuel :
- `Status: ...`
- puis éventuellement `Project`, `Phase`, `Etape`, `Last message`, `Last error`, `Summary`

Limites UX :
- ordre et rendu trop techniques
- pas de distinction visuelle entre `RUNNING`, `FAILED`, `WAITING_HUMAN`, `COMPLETED`
- pas de `roadmapPath`
- pas de `runId`
- pas de `startedAt` / `updatedAt`
- pas de prochaine action recommandée

Améliorations possibles :
- rendre le statut central
- ajouter contexte actif
- ajouter disponibilité roadmap / summary
- afficher la commande utile suivante selon le statut

### `/workflow_summary`

Rôle :
- afficher le summary ou envoyer le fichier si trop long

Message actuel :
- si summary absent : message d’erreur
- si présent : `Workflow summary (1/1)` ou chunks `1/2`, `2/2`
- si trop long : message court + document

Limites UX :
- messages fonctionnels mais peu contextualisés
- pas de lien explicite avec le statut courant
- pas de rappel clair de ce qu’il faut faire après lecture, surtout en `WAITING_HUMAN`

Améliorations possibles :
- enrichir les messages d’erreur
- rappeler la commande suivante pertinente selon le statut

### `/workflow_resume`

Rôle :
- reprendre un workflow en attente humaine

Message actuel :
- `Resume lance`
- ou erreur brute (`Workflow already running`, `Aucun workflow en attente d'action humaine`, etc.)

Limites UX :
- message de succès trop court
- pas de rappel de suivi (`/workflow_status`)
- pas de cohérence visuelle avec `/workflow_run`

Améliorations possibles :
- format succès standard
- rappeler la commande de suivi recommandée

### `/workflow_roadmap_load`

Rôle :
- charger la roadmap active

Message actuel :
- `Roadmap loaded`
- `Project: ...`
- `Path: ...`

Limites UX :
- commande longue
- pas de confirmation claire que cette roadmap devient le contexte actif
- pas de suggestion d’action suivante

Améliorations possibles :
- préciser que la roadmap est maintenant active
- suggérer `/workflow_run` ou `/workflow_status`

## Problèmes UX actuels

Les problèmes principaux sont les suivants :

- commandes longues et peu mémorisables pour un usage fréquent
- absence d’écran d’accueil utile sur `/workflow`
- manque de cohérence de ton et de structure dans les messages
- messages encore trop techniques ou trop proches du debug
- plusieurs chaînes restent fragiles côté lisibilité/encodage dans certains services
- pas de "prochaine action" guidée selon le statut
- `WAITING_HUMAN` n’est pas assez assisté
- l’utilisateur ne voit pas facilement quelle roadmap est active
- l’utilisateur ne voit pas toujours clairement quelle phase / étape est sélectionnée
- `status` n’est pas une vraie vue synthétique, seulement un dump partiel de session

## Format standard recommandé

Je recommande un format simple, textuel, stable, réutilisable par commande.

### Succès

```text
✅ Action effectuée
Detail: ...
Next: ...
```

Usage :
- `run`
- `resume`
- `roadmap_load`

### Erreur

```text
❌ Action impossible
Reason: ...
Try: ...
```

Usage :
- contexte incomplet
- pas de summary
- pas de workflow en attente humaine
- run déjà actif

### En cours

```text
⏳ Workflow en cours
Run: ...
Project: ...
Phase: ...
Etape: ...
Since: ...
Next: /workflow_status
```

Usage :
- `RUNNING`

### WAITING_HUMAN

```text
🟠 Action humaine requise
Project: ...
Phase: ...
Etape: ...
Summary: available|missing
Next: /workflow_summary, puis /workflow_resume
```

Usage :
- `WAITING_HUMAN`

## Proposition d’amélioration de `/workflow_status`

`/workflow_status` doit devenir la vue synthétique principale.

Rendu recommandé :

```text
Workflow status

Status: WAITING_HUMAN
Project: Toolkit
Phase: 7
Etape: 4
Roadmap: loaded
Summary: available
Run: tg-...
Started: ...
Updated: ...

Next: /workflow_summary puis /workflow_resume
```

Améliorations concrètes :
- afficher systématiquement le statut en premier
- afficher `project`, `phase`, `etape`
- afficher si une roadmap est active, avec son nom relatif si possible
- afficher si un summary est disponible
- afficher `lastRunId`
- afficher `startedAt` et `updatedAt`
- ajouter une recommandation de prochaine commande selon `lastStatus`

Recommandation par statut :
- `IDLE` : `/workflow_roadmap_load` ou `/workflow_run`
- `RUNNING` : `/workflow_status`
- `WAITING_HUMAN` : `/workflow_summary` puis `/workflow_resume`
- `COMPLETED` : `/workflow_summary`
- `FAILED` : `/workflow_status` puis vérifier `lastError` et éventuellement relancer `/workflow_run`

## Proposition d’amélioration de `/workflow`

`/workflow` doit remplacer le message placeholder actuel et devenir le tableau de bord minimal du module.

Rendu recommandé :

```text
Workflow assistant ready

Current:
- Project: ...
- Phase: ...
- Etape: ...
- Status: ...
- Roadmap: loaded|missing

Commands:
- /workflow_run
- /workflow_status
- /workflow_summary
- /workflow_resume
- /workflow_roadmap_load

Recommended:
- /workflow_run
```

Comportement attendu :
- si aucune session utile : afficher état vide + commandes de départ
- si `RUNNING` : recommander `/workflow_status`
- si `WAITING_HUMAN` : recommander `/workflow_summary`
- si `COMPLETED` : recommander `/workflow_summary`

## Recommandation sur `/workflow_help`

Je ne recommande **pas** d’ajouter `/workflow_help` maintenant.

Raison :
- le module Telegram a déjà `/help`
- le périmètre est encore petit
- enrichir `/workflow` suffit pour l’instant
- éviter une dispersion entre `/help`, `/workflow`, `/workflow_help`

Conclusion :
- **à faire maintenant** : enrichir `/workflow`
- **à ne pas faire maintenant** : créer `/workflow_help`

## Recommandation sur les alias courts

Les alias courts peuvent être utiles, mais je ne recommande pas de les introduire tout de suite.

Pourquoi reporter :
- risque de bruit fonctionnel pour un gain UX modéré
- augmente le nombre de commandes à maintenir et tester
- priorité plus forte à la clarté des réponses qu’à la réduction de frappe

Verdict :
- garder les commandes existantes comme référence
- réévaluer plus tard des alias simples comme `/w`, `/w_status`, `/w_resume`
- ne rien casser ni déprécier maintenant

## Recommandation sur les boutons inline

Les boutons inline sont intéressants, surtout autour de `WAITING_HUMAN`, mais je recommande de les **reporter**.

Valeur potentielle :
- accès rapide à `summary`, `status`, `resume`
- moins de saisie
- meilleure fluidité sur mobile

Raisons de ne pas implémenter maintenant :
- ajoute de la complexité au module Telegram
- demande un vrai cadrage UI et des tests supplémentaires
- l’UX textuelle n’est pas encore stabilisée

Conclusion :
- utile plus tard
- pas nécessaire pour cette étape

## Résumé des recommandations

### À faire maintenant

- remplacer le placeholder de `/workflow` par un écran d’accueil utile
- standardiser les messages succès / erreur / en cours / attente humaine
- enrichir `/workflow_status` avec `roadmap`, `summary`, `runId`, `startedAt`, `updatedAt`, prochaine commande
- harmoniser le ton des messages sur toutes les commandes workflow
- corriger les chaînes encore corrompues dans les services Telegram workflow si elles existent encore

### À reporter

- `/workflow_help`
- alias courts
- boutons inline
- menus Telegram plus riches
- logique "Étape suivante"

## Tests à prévoir

Tests de rendu recommandés :

- rendu `/workflow_status` en `IDLE`
- rendu `/workflow_status` en `RUNNING`
- rendu `/workflow_status` en `WAITING_HUMAN`
- rendu `/workflow_status` en `FAILED`
- rendu `/workflow` sans contexte actif
- rendu `/workflow` avec contexte actif
- commande recommandée correcte selon `IDLE`
- commande recommandée correcte selon `RUNNING`
- commande recommandée correcte selon `WAITING_HUMAN`
- commande recommandée correcte selon `COMPLETED`
- commande recommandée correcte selon `FAILED`

## Plan d’implémentation proposé

1. Définir un format de message standard partagé pour les réponses workflow.
2. Remplacer le placeholder de `/workflow` par une vue d’accueil simple basée sur la session courante.
3. Refactoriser `/workflow_status` pour produire une vue synthétique orientée utilisateur.
4. Ajouter la notion de "commande suivante recommandée" selon le statut.
5. Harmoniser les messages de `/workflow_run`, `/workflow_resume` et `/workflow_roadmap_load`.
6. Ajouter les tests de rendu pour `/workflow` et `/workflow_status`.
7. Vérifier qu’aucune commande existante n’est cassée.

## Conclusion

Le module est **partiellement prêt** pour l’implémentation UX.

Il est suffisamment stable pour absorber une amélioration textuelle simple et utile, sans toucher au moteur workflow. En revanche, l’UX actuelle n’est pas encore assez homogène pour considérer le module "fini" côté utilisateur. Une étape d’amélioration des messages et des vues `/workflow` / `/workflow_status` est justifiée et à faible risque.

