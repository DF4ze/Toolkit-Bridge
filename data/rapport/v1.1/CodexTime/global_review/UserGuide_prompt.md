À enregistrer par exemple dans :

`data/rapport/v1.1/CodexTime/global_review/UserGuide_prompt.md`

---

# PROMPT — Génération du UserGuide CodexTime

## Contexte

Tu interviens sur le projet `Toolkit-Bridge`.

Le sous-système **CodexTime** est un orchestrateur de workflow d’implémentation assisté par Codex.

Il permet notamment :

* d’exécuter un workflow d’analyse / review / correction ;
* de produire des artefacts de rapport ;
* de générer un `workflow-summary.md` ;
* de gérer des états `WAIT_HUMAN` ;
* de reprendre un workflow après intervention humaine ;
* de lancer et suivre le workflow via Telegram ;
* d’utiliser une CLI externe ;
* d’exécuter des validations locales / Maven ;
* de gérer certaines erreurs comme quota/rate-limit ;
* de conserver une traçabilité dans `data/rapport/v1.1/CodexTime/...`.

Tu dois produire un **guide utilisateur complet** permettant à un utilisateur développeur de comprendre et tester tout ce qui est disponible.

Ce guide doit être orienté usage concret, pas architecture interne uniquement.

---

## Fichiers à analyser

Analyser le projet et les rapports disponibles, notamment :

```text
data/rapport/v1.1/CodexTime/global_review/global_codextime_final_report.md
data/rapport/v1.1/CodexTime/mini-roadmap.md
data/rapport/00.promptWorkflow/0.workflow.md
```

Analyser aussi tous les rapports finaux disponibles :

```text
data/rapport/v1.1/CodexTime/phase1/z_finalRepport.md
data/rapport/v1.1/CodexTime/phase2/z_finalRepport.md
data/rapport/v1.1/CodexTime/phase3/z_finalRepport.md
data/rapport/v1.1/CodexTime/phase4/z_finalRepport.md
data/rapport/v1.1/CodexTime/phase5/z_finalReport.md
data/rapport/v1.1/CodexTime/phase6/z_finalRepport.md
data/rapport/v1.1/CodexTime/phase7/z_finalRepport.md
```

Si certains fichiers ont une casse différente (`PhaseX` vs `phaseX`) ou une orthographe différente (`z_finalReport.md` vs `z_finalRepport.md`), les prendre en compte et le signaler.

Analyser également le code source lié à CodexTime, notamment les packages concernant :

* workflow runtime ;
* runner ;
* steps ;
* Codex client ;
* validation Maven ;
* CLI ;
* Telegram workflow ;
* session Telegram ;
* summary ;
* sécurité ;
* erreurs ;
* configuration.

---

## Objectif

Créer un fichier :

```text
data/rapport/v1.1/CodexTime/UserGuide.md
```

Ce fichier doit expliquer **tout ce qu’un utilisateur peut faire avec CodexTime**.

Le guide doit permettre de :

* configurer CodexTime ;
* lancer un workflow ;
* suivre son état ;
* lire les rapports produits ;
* reprendre après `WAIT_HUMAN` ;
* utiliser les commandes Telegram ;
* utiliser la CLI ;
* comprendre les fichiers générés ;
* tester les cas principaux ;
* identifier les limites connues ;
* repérer les futurs points d’amélioration.

---

# Structure attendue du UserGuide.md

## 1. Présentation rapide

Expliquer en termes simples :

* ce qu’est CodexTime ;
* à quoi il sert ;
* ce qu’il automatise ;
* ce qu’il ne fait pas encore ;
* la différence entre CodexTime, Codex, Telegram et Toolkit-Bridge.

Inclure un schéma texte simple du type :

```text
Roadmap / Prompt
   ↓
CodexTime Runner
   ↓
Codex + Validation locale
   ↓
Reports + workflow-summary.md
   ↓
Telegram / CLI
```

---

## 2. Prérequis

Lister précisément les prérequis :

* Java / Maven ;
* projet Toolkit-Bridge compilable ;
* Codex CLI disponible si nécessaire ;
* configuration Telegram si usage Telegram ;
* workspace / répertoires de rapports ;
* accès aux fichiers de roadmap ;
* profils Spring éventuels.

Indiquer ce qui est obligatoire et ce qui est optionnel.

---

## 3. Configuration

Documenter toutes les propriétés utilisateur connues.

### 3.1 Configuration Telegram module

Inclure les propriétés du module Telegram, par exemple :

```yaml
telegram:
  enabled:
  default-bot-id:
  bots:
    - id:
      token:
      polling-enabled:
      auto-register-commands:
      configure-menu-button:
      security:
        allowed-user-ids:
```

Expliquer :

* rôle du bot `Cortex` si c’est l’état actuel ;
* pourquoi `Cortex` ne doit pas forcément rester codé en dur à terme ;
* whitelist ;
* comportement recommandé en chat privé ;
* risque d’usage en groupe.

### 3.2 Configuration workflow Telegram

Documenter :

```yaml
toolkit:
  telegram:
    workflow:
      reportRootDirectory:
      reportVersion:
      timeoutMinutes:
```

Expliquer clairement la différence entre :

* `telegram.*`
* `toolkit.telegram.workflow.*`

### 3.3 Configuration Codex / validation

Documenter ce qui est configurable ou attendu :

* binaire Codex ;
* working directory ;
* timeout ;
* Maven wrapper ;
* report root ;
* version de rapport ;
* phase / étape.

Si certains paramètres sont encore implicites ou codés dans le système, le signaler.

---

## 4. Organisation des fichiers

Décrire les chemins importants.

Inclure au minimum :

```text
data/rapport/v1.1/CodexTime/
data/rapport/v1.1/CodexTime/PhaseX/
data/rapport/v1.1/CodexTime/PhaseX/etapeY/
data/rapport/v1.1/CodexTime/PhaseX/z_finalRepport.md
data/rapport/v1.1/CodexTime/PhaseX/workflow-summary.md
workspace/shared/
workspace/shared/projects/{ProjectName}/roadmaps/
workspace/shared/projects/{ProjectName}/reports/
```

Pour chaque chemin, expliquer :

* rôle ;
* qui l’écrit ;
* qui le lit ;
* s’il est généré automatiquement ;
* si l’utilisateur doit le modifier.

---

## 5. Artefacts générés

Documenter les artefacts possibles :

* prompts d’analyse ;
* rapports d’analyse ;
* rapports d’implémentation ;
* rapports de review ;
* rapports de correction ;
* `workflow-summary.md`;
* `z_finalRepport.md`;
* logs de validation ;
* résultats Maven ;
* éventuels fichiers de roadmap.

Pour chaque artefact :

* nom ;
* exemple de chemin ;
* rôle ;
* moment où il est produit ;
* comment l’utilisateur doit l’utiliser.

---

## 6. Utilisation via CLI

Documenter la CLI CodexTime si elle existe.

Inclure :

* classe / point d’entrée ;
* modes disponibles ;
* paramètres obligatoires ;
* paramètres optionnels ;
* exemples de commande ;
* codes de sortie ;
* format stdout ;
* format `key=value`.

Documenter au minimum :

```text
--mode=RUN
--mode=RESUME
--reportRootDirectory=...
--reportVersion=...
--reportPhase=...
--stepNumber=...
--analysisSourcePath=...
```

Expliquer :

* quand utiliser `RUN` ;
* quand utiliser `RESUME` ;
* ce que produit la CLI ;
* comment lire la sortie ;
* limites connues.

Ajouter un exemple complet réaliste.

---

## 7. Utilisation via Telegram

Documenter toutes les commandes disponibles.

Pour chaque commande, fournir :

* nom exact ;
* objectif ;
* syntaxe ;
* options ;
* exemples ;
* préconditions ;
* réponse attendue ;
* erreurs possibles ;
* prochaine commande recommandée.

Commandes à documenter :

```text
/workflow
/workflow_run
/workflow_status
/workflow_summary
/workflow_resume
/workflow_roadmap_load
```

### 7.1 `/workflow`

Expliquer que c’est le dashboard.

Inclure exemple de sortie.

### 7.2 `/workflow_roadmap_load`

Documenter la syntaxe :

```text
/workflow_roadmap_load path=projects/{ProjectName}/roadmaps/{file}.md project={ProjectName}
```

Expliquer :

* path relatif à `workspace/shared` ;
* extension `.md` ;
* validation du chemin ;
* erreurs possibles.

### 7.3 `/workflow_run`

Documenter toutes les formes possibles :

```text
/workflow_run
/workflow_run project=ProjectName
/workflow_run project=ProjectName phase=7
/workflow_run project=ProjectName phase=7 etape=3
```

Expliquer la résolution :

* utilisation de la session si certains paramètres manquent ;
* `phase + etape` ;
* comportement si contexte incomplet.

### 7.4 `/workflow_status`

Expliquer les statuts :

```text
IDLE
RUNNING
COMPLETED
WAITING_HUMAN
FAILED
```

Documenter les champs affichés :

* project ;
* phase ;
* etape ;
* roadmap ;
* summary ;
* runId ;
* startedAt ;
* updatedAt ;
* lastMessage ;
* lastError ;
* next action.

### 7.5 `/workflow_summary`

Expliquer :

* summary court envoyé directement ;
* summary moyen envoyé en 2 chunks max ;
* summary long envoyé en document ;
* comportement si document non envoyé ;
* comportement si summary absent.

### 7.6 `/workflow_resume`

Expliquer :

* utilisable uniquement en `WAITING_HUMAN` ;
* reprend le workflow via correction/review ;
* ne doit pas être utilisé si aucun workflow n’attend une action humaine ;
* ne fait pas avancer automatiquement la phase.

---

## 8. Cycle utilisateur complet

Décrire un scénario complet pas à pas.

Exemple :

```text
1. Préparer la roadmap dans workspace/shared/...
2. Charger la roadmap avec /workflow_roadmap_load
3. Lancer /workflow_run project=... phase=... etape=...
4. Suivre avec /workflow_status
5. Lire /workflow_summary
6. Si WAITING_HUMAN :
   - modifier le fichier demandé
   - relancer /workflow_resume
7. Lire le status final
8. Consulter les rapports générés dans data/rapport/...
```

Inclure au moins 3 scénarios :

### Scénario A — Run nominal terminé

### Scénario B — WAIT_HUMAN puis resume

### Scénario C — Échec / quota / rate-limit

---

## 9. Comprendre `WAIT_HUMAN`

Expliquer clairement :

* ce que signifie `WAIT_HUMAN` ;
* pourquoi le workflow s’arrête ;
* où lire la raison ;
* comment savoir quel fichier modifier ;
* comment reprendre ;
* ce qu’il ne faut pas faire.

Inclure un exemple concret.

---

## 10. Comprendre le `workflow-summary.md`

Expliquer :

* son rôle ;
* son format ;
* où il se trouve ;
* quand il est généré ;
* comment Telegram l’utilise ;
* comment l’utilisateur doit s’en servir.

Inclure un exemple de structure :

```text
Status:
Reason:

Context:

Actions:

Artifacts:
```

---

## 11. Validation locale et Maven

Documenter :

* rôle de `WorkflowValidationService` ;
* rôle de `MavenValidationStep` ;
* validation `compile`;
* auto-correction build ;
* retry borné ;
* conversion en `WAIT_HUMAN` si échec persistant.

Expliquer ce que l’utilisateur voit dans Telegram ou dans les rapports.

---

## 12. Erreurs et diagnostics

Documenter les erreurs fréquentes :

* roadmap introuvable ;
* path non autorisé ;
* summary absent ;
* workflow already running ;
* contexte incomplet ;
* timeout ;
* STOP_FAILURE ;
* quota / rate-limit ;
* Codex indisponible ;
* Maven échoue ;
* Telegram sender indisponible.

Pour chaque erreur :

* cause probable ;
* message utilisateur ;
* où regarder ;
* action recommandée.

Inclure une section :

```text
Ce qui est affiché dans Telegram
Ce qui est seulement loggé côté serveur
```

---

## 13. Sécurité

Documenter :

* whitelist Telegram ;
* bot `Cortex` actuel ;
* risque whitelist vide ;
* usage recommandé en chat privé ;
* risque d’usage en groupe ;
* absence de double sécurité dans les controllers ;
* sécurité des chemins via workspace.

Indiquer les limites :

* pas de rôles avancés ;
* pas de multi-user complet ;
* pas d’audit trail complet.

---

## 14. Limites connues

Lister explicitement :

* session Telegram en mémoire ;
* perte d’état au redémarrage ;
* pas de persistance durable des runs ;
* pas de WebUI workflow ;
* pas d’auto-advance de phase ;
* pas de parsing roadmap complet ;
* pas de boutons inline ;
* pas d’alias courts ;
* pas de fallback provider ;
* classification quota/rate-limit heuristique ;
* agent/bot workflow pas encore totalement configurable ;
* reset contexte Codex non implémenté ;
* pas encore de vrai mode “stop workflow après étape”.

---

## 15. Conseils de test manuel

Proposer une checklist de test utilisateur.

Inclure :

```text
[ ] /workflow affiche le dashboard
[ ] /workflow_roadmap_load charge une roadmap valide
[ ] /workflow_run démarre un run
[ ] /workflow_status passe à RUNNING puis COMPLETED/WAITING_HUMAN
[ ] /workflow_summary affiche ou envoie le summary
[ ] /workflow_resume fonctionne en WAITING_HUMAN
[ ] double /workflow_run est refusé
[ ] path invalide roadmap est refusé
[ ] summary absent retourne une erreur claire
[ ] quota/rate-limit donne un message compréhensible
```

Ajouter aussi une checklist fichiers :

```text
[ ] les rapports sont créés au bon endroit
[ ] workflow-summary.md est généré
[ ] z_finalRepport.md est généré en fin de phase
[ ] les chemins absolus ne fuitent pas dans Telegram
```

---

## 16. Points d’amélioration à surveiller

Créer une section dédiée pour aider l’utilisateur à noter ses observations.

Inclure les thèmes :

* ergonomie Telegram ;
* commandes trop longues ;
* messages confus ;
* erreurs mal qualifiées ;
* manque de WebUI ;
* besoin de reset contexte Codex ;
* besoin de stop workflow ;
* besoin de mode d’emploi plus détaillé ;
* besoin d’agent workflow configurable ;
* besoin de configuration DB/WebUI ;
* besoin de provider fallback.

---

## 17. FAQ

Ajouter une FAQ avec au minimum :

* Quelle différence entre RUN et RESUME ?
* Pourquoi mon workflow est en WAITING_HUMAN ?
* Où est le summary ?
* Où sont les rapports ?
* Pourquoi Telegram dit “workflow already running” ?
* Que faire si Codex est en quota ?
* Que faire si Maven échoue ?
* Puis-je utiliser CodexTime depuis la WebUI ?
* Est-ce que l’état survit à un redémarrage ?
* Pourquoi le bot s’appelle Cortex ?
* Peut-on changer l’agent responsable ?
* Peut-on lancer plusieurs workflows en parallèle ?

---

## 18. Annexes

Ajouter des annexes utiles :

### Annexe A — Commandes Telegram

Table complète.

### Annexe B — Paramètres CLI

Table complète.

### Annexe C — Chemins importants

Table complète.

### Annexe D — Statuts

Table complète.

### Annexe E — Glossaire

Inclure :

* CodexTime ;
* Codex ;
* runner ;
* step ;
* artifact ;
* summary ;
* WAIT_HUMAN ;
* STOP_FAILURE ;
* RETRY_CORRECTION ;
* roadmap ;
* phase ;
* etape ;
* runId ;
* chatId.

---

## Contraintes strictes

* Ne pas modifier le code.
* Ne pas modifier les roadmaps.
* Ne pas inventer de commandes.
* Ne pas inventer de propriétés.
* Ne pas inventer de fichiers.
* Si une information n’est pas trouvée, écrire explicitement : “À confirmer dans le code”.
* Le guide doit être utilisable par un développeur réel.
* Le guide doit être concret, pas marketing.
* Donner des exemples complets.
* Mentionner les limites connues.
* Mentionner les comportements non encore implémentés.
* Ne pas masquer les dettes.
* Ne pas supposer que `Cortex` restera toujours le bot final.
* Ne pas présenter la WebUI comme disponible si elle ne pilote pas encore CodexTime.

## Résultat attendu

Créer :

```text
data/rapport/v1.1/CodexTime/UserGuide.md
```

Le guide doit être assez complet pour permettre à l’utilisateur de tester CodexTime de bout en bout et d’identifier les points d’amélioration avant la mise à jour complète des roadmaps.

