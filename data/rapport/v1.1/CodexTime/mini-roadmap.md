# Roadmap détaillée — Intégration d’un workflow d’implémentation assisté Codex dans Toolkit-Bridge

## 1. Objectif de cette roadmap

Cette roadmap vise à intégrer dans Toolkit-Bridge une première brique de workflow capable de dérouler, de manière assistée et traçable, un processus d’implémentation technique basé sur Codex.
Le but n’est pas de construire immédiatement un moteur générique de workflows, ni un framework de scripting, mais de mettre en place une **première implémentation simple, robuste et exploitable**, alignée avec le workflow déjà utilisé aujourd’hui. Ce workflow impose une logique stricte : analyse initiale, découpage en lots, analyse par lot, implémentation cadrée, audit/corrections, validation, puis bilan final.

L’objectif de cette intégration est double :

* **réduire les allers-retours manuels** entre l’utilisateur, ChatGPT et Codex ;
* **préparer une base saine** pour de futurs workflows plus variés, sans sur-concevoir dès maintenant.

Cette roadmap suit donc une logique très progressive :

* d’abord rendre le workflow exécutable localement ;
* ensuite le fiabiliser ;
* puis l’intégrer proprement à Telegram et à Toolkit-Bridge ;
* enfin stabiliser les points qui prépareront une éventuelle généralisation future.

---

## 2. Principes directeurs de l’implémentation

Avant d’entrer dans les phases, il faut poser les règles structurantes.

### 2.1 Ne pas construire un moteur générique trop tôt

Le besoin immédiat n’est pas de supporter tous les workflows imaginables, ni de créer un langage de scripting.
Le besoin est d’intégrer **un workflow concret**, celui déjà documenté, dans Toolkit-Bridge, avec une logique Java claire et lisible.

Cela signifie :

* workflow codé en Java ;
* étapes explicites ;
* artefacts enregistrés comme aujourd’hui ;
* logique métier pilotée par des services simples.

Le générique viendra plus tard si la première implémentation prouve sa valeur.

### 2.2 Garder une séparation stricte des responsabilités

Le système doit séparer clairement :

* le **pilotage du workflow** ;
* l’**exécution Codex** ;
* la **gestion des artefacts** ;
* la **validation technique locale** ;
* l’**interaction humaine via Telegram**.

Cette séparation est importante car, sinon, le système deviendra rapidement :

* difficile à tester ;
* difficile à faire évoluer ;
* opaque en cas d’échec.

### 2.3 Garder le build local comme source de vérité

Codex ne doit jamais être considéré comme la source de validation technique.
Le workflow peut s’appuyer sur Codex pour analyser, proposer, implémenter et corriger, mais la vérité technique reste locale :

* tests ciblés du lot ;
* build Maven en `skipTests` pendant le flux ;
* build complet avec tous les tests en fin d’étape.

Ce point est particulièrement important car l’exécution Maven sous sandbox Codex est fragile ou insuffisante en pratique.

### 2.4 Bloquer sur les décisions d’architecture importantes

Le workflow ne doit pas seulement corriger des bugs.
Lors de la **toute première analyse**, le système doit identifier les sujets qui nécessitent une décision structurante de l’utilisateur :

* arbitrage d’architecture ;
* choix technique significatif ;
* ambiguïté de périmètre ;
* conflit avec une contrainte existante ;
* plusieurs options raisonnables de conception.

Dans ce cas, le système doit :

* lister ces questions ;
* les poser à l’utilisateur via Telegram ;
* suspendre l’implémentation ;
* reprendre uniquement quand les réponses ont été apportées.

Ce point est essentiel, car il évite que Codex “invente” une architecture simplement pour faire avancer le flux.

### 2.5 Reproduire la discipline documentaire actuelle

Le workflow existant repose sur une vraie discipline d’artefacts :

* prompts sauvegardés ;
* rapports d’analyse ;
* rapports d’implémentation ;
* rapports de correction ;
* bilan final.

L’intégration dans Toolkit-Bridge ne doit pas casser cette logique.
Au contraire, elle doit l’automatiser proprement.

---

# Phase 1 — Poser la fondation exécutable minimale

## 1.1 Finalité de la phase

Cette première phase a pour but de créer une base simple mais fonctionnelle dans Toolkit-Bridge, capable :

* de représenter un run de workflow ;
* d’exécuter des étapes explicites ;
* de dialoguer avec Codex ;
* de stocker les artefacts ;
* de garder un état minimal.

À ce stade, on ne cherche pas encore à tout gérer :

* ni Telegram ;
* ni les décisions humaines ;
* ni les boucles complexes ;
* ni la validation Maven complète.

Le but est d’obtenir un premier socle exécutable.

---

## 1.2 Étape 1 — Définir le modèle minimal du workflow runtime

### 1.2.1 Problème à traiter

Sans modèle explicite, la logique de workflow va se disperser dans plusieurs services techniques et devenir rapidement illisible.

### 1.2.2 Objectif

Définir les objets techniques minimaux permettant de piloter un workflow concret.

### 1.2.3 Travail à réaliser

* Introduire un objet représentant l’exécution globale du workflow.
* Introduire un objet représentant l’exécution d’un lot.
* Introduire un objet représentant le contexte courant d’exécution.
* Introduire une représentation simple des statuts d’exécution.

### 1.2.4 Sous-découpage recommandé

1. Créer `WorkflowRun`
2. Créer `LotRun`
3. Créer `WorkflowExecutionContext`
4. Créer les statuts :

  * `NEW`
  * `RUNNING`
  * `WAITING_HUMAN`
  * `FAILED`
  * `COMPLETED`
5. Ajouter les métadonnées minimales :

  * étape cible
  * lot courant
  * compteur de corrections
  * chemins d’artefacts
  * timestamps

### 1.2.5 Pourquoi cette étape vient en premier

Parce qu’elle évite que toute la suite repose sur des appels ad hoc sans état lisible.

---

## 1.3 Étape 2 — Définir le contrat des étapes de workflow

### 1.3.1 Problème à traiter

Si tout le workflow est codé dans un seul service, l’implémentation sera vite difficile à maintenir.

### 1.3.2 Objectif

Découper le workflow en étapes explicites, sans tomber dans un framework trop abstrait.

### 1.3.3 Travail à réaliser

* Introduire une interface simple du type `WorkflowStep`.
* Définir un contrat d’exécution clair :

  * entrée : contexte courant
  * sortie : résultat structuré
* Prévoir un résultat d’étape capable d’indiquer :

  * succès
  * échec
  * besoin de correction
  * besoin de décision humaine

### 1.3.4 Sous-découpage recommandé

1. Créer `WorkflowStep`
2. Créer `WorkflowStepResult`
3. Définir les décisions possibles :

  * `CONTINUE`
  * `RETRY_CORRECTION`
  * `WAIT_HUMAN`
  * `STOP_FAILURE`
  * `FINISH`
4. Préparer les premières implémentations concrètes d’étapes

### 1.3.5 Point d’attention

Il ne faut pas créer un système de plugins ici.
On reste sur un contrat simple pour cette première implémentation.

---

## 1.4 Étape 3 — Créer le client Codex dédié

### 1.4.1 Problème à traiter

L’appel à Codex ne doit pas être dispersé dans les étapes.

### 1.4.2 Objectif

Centraliser l’appel Codex dans une brique dédiée, purement technique.

### 1.4.3 Travail à réaliser

* Créer un service responsable de l’exécution Codex.
* Gérer :

  * la commande ;
  * le prompt ;
  * les timeouts ;
  * stdout/stderr ;
  * le code retour ;
  * le résultat structuré.
* Prévoir l’écriture des artefacts associés.

### 1.4.4 Sous-découpage recommandé

1. Créer `CodexWorkflowClient`
2. Créer `CodexExecutionRequest`
3. Créer `CodexExecutionResult`
4. Gérer les erreurs système et timeouts
5. Préparer l’intégration avec le service d’artefacts

### 1.4.5 Pourquoi cette étape est importante

Parce qu’elle isole Codex comme simple exécutant, sans lui donner de responsabilité de pilotage.

---

## 1.5 Étape 4 — Créer le service de gestion des artefacts

### 1.5.1 Problème à traiter

Le workflow existant impose une structure documentaire précise, qui doit être conservée et automatisée.

### 1.5.2 Objectif

Créer une brique dédiée capable de générer, nommer, écrire et relire les artefacts du workflow.

### 1.5.3 Travail à réaliser

* Gérer les chemins de rapport.
* Générer les noms de fichiers attendus.
* Écrire :

  * prompts ;
  * résultats d’analyse ;
  * résultats d’implémentation ;
  * résultats de correction ;
  * bilan final.
* Permettre la relecture des fichiers déjà produits.

### 1.5.4 Sous-découpage recommandé

1. Créer `WorkflowArtifactService`
2. Gérer le mapping logique → chemin physique
3. Supporter les types d’artefacts attendus par le workflow
4. Ajouter la lecture des artefacts existants
5. Garantir l’idempotence sur les écritures nécessaires

### 1.5.5 Point d’attention

Cette brique ne doit pas contenir la logique métier du workflow.
Elle ne fait que gérer la matérialisation documentaire.

---

## 1.6 Critère de sortie de la Phase 1

La Phase 1 est terminée lorsque :

* le système sait représenter un workflow en cours ;
* les étapes sont modélisées ;
* Codex peut être appelé proprement ;
* les artefacts peuvent être écrits et relus ;
* le socle technique ne repose plus sur du code monolithique.

---

# Phase 2 — Dérouler le workflow d’implémentation en Java

## 2.1 Finalité de la phase

Cette phase vise à coder le workflow concret demandé, en respectant sa structure réelle :

* analyse globale ;
* mini-roadmap/lots ;
* boucle par lot ;
* corrections ;
* clôture finale.

À ce stade, l’objectif n’est toujours pas de généraliser, mais de rendre **ce workflow précis** exécutable dans Toolkit-Bridge.

---

## 2.2 Étape 1 — Implémenter l’analyse globale de l’étape

### 2.2.1 Problème à traiter

Le workflow commence par une analyse globale de l’étape, qui sert de base au découpage et au cadrage du travail.

### 2.2.2 Objectif

Implémenter une première étape Java qui :

* lit la roadmap fournie dans le workspace ;
* identifie l’étape cible ;
* produit le prompt d’analyse globale ;
* exécute Codex ;
* archive le résultat.

### 2.2.3 Travail à réaliser

* Lire les fichiers de contexte fournis à l’agent.
* Préparer le prompt d’analyse initial.
* Exécuter Codex.
* Stocker le rapport global.
* Préparer la suite du workflow.

### 2.2.4 Sous-découpage recommandé

1. Créer `GlobalAnalysisStep`
2. Gérer la lecture du roadmap file
3. Générer le prompt d’analyse
4. Lancer Codex
5. Sauvegarder `0.result_global_analysis.md`
6. Charger ce résultat dans le contexte runtime

### 2.2.5 Point d’attention majeur

Dès cette première analyse, le système doit détecter les **décisions d’architecture importantes** et les signaler.
C’est ici que doivent émerger les premières questions à poser à l’utilisateur.

---

## 2.3 Étape 2 — Extraire les questions bloquantes dès l’analyse initiale

### 2.3.1 Problème à traiter

Si l’on laisse l’implémentation démarrer sans avoir traité les zones d’incertitude structurante, Codex risque de produire une solution arbitraire.

### 2.3.2 Objectif

Introduire une étape dédiée à l’identification et à la remontée des décisions importantes.

### 2.3.3 Travail à réaliser

* Lire le rapport d’analyse globale.
* Détecter les points de décision majeurs.
* Structurer les questions à poser.
* Suspendre le workflow tant que ces réponses ne sont pas fournies.

### 2.3.4 Sous-découpage recommandé

1. Créer `ArchitectureDecisionQuestionStep`
2. Définir le format des questions
3. Introduire un statut `WAITING_HUMAN`
4. Stocker les questions en attente
5. Préparer la reprise du workflow après réponse

### 2.3.5 Pourquoi cette étape est cruciale

Parce qu’elle garantit que les arbitrages structurants restent humains, et ne sont pas absorbés silencieusement par Codex.

---

## 2.4 Étape 3 — Charger et dérouler les lots de la mini-roadmap

### 2.4.1 Problème à traiter

Une fois l’analyse globale effectuée, le workflow doit pouvoir dérouler les lots de façon ordonnée et maîtrisée. La mini-roadmap par lots fait justement partie de la logique de travail existante.

### 2.4.2 Objectif

Implémenter le déroulé séquentiel des lots d’une étape.

### 2.4.3 Travail à réaliser

* Représenter les lots de l’étape courante.
* Définir l’ordre d’exécution.
* Préparer un contexte lot par lot.

### 2.4.4 Sous-découpage recommandé

1. Créer une représentation `LotDefinition`
2. Charger la mini-roadmap fournie
3. Ordonner les lots
4. Initialiser les `LotRun`
5. Préparer l’enchaînement par lot

### 2.4.5 Point d’attention

Pour cette v1, le système peut s’appuyer sur une lecture simple de la mini-roadmap, sans chercher une interprétation sémantique complexe.

---

## 2.5 Étape 4 — Implémenter l’analyse ciblée d’un lot

### 2.5.1 Problème à traiter

Chaque lot doit faire l’objet d’une analyse dédiée avant toute implémentation.

### 2.5.2 Objectif

Créer une étape Java capable d’analyser un lot précis.

### 2.5.3 Travail à réaliser

* Construire le prompt d’analyse du lot.
* Injecter :

  * contexte global ;
  * roadmap ;
  * mini-roadmap ;
  * lot courant ;
  * artefacts précédents utiles.
* Exécuter Codex.
* Sauvegarder le rapport.

### 2.5.4 Sous-découpage recommandé

1. Créer `LotAnalysisStep`
2. Construire le prompt d’analyse ciblée
3. Lancer Codex
4. Sauvegarder `result_[numLot]_analysis.md`
5. Charger le rapport dans le contexte d’exécution

### 2.5.5 Point d’attention

Cette analyse doit pouvoir à son tour détecter :

* un simple point technique corrigeable plus tard ;
* ou une vraie question bloquante à remonter à l’utilisateur.

---

## 2.6 Étape 5 — Implémenter le cadrage puis l’implémentation du lot

### 2.6.1 Problème à traiter

Le workflow impose un cadrage fort avant implémentation, afin d’éviter les dérives de Codex.

### 2.6.2 Objectif

Créer l’étape qui prépare le prompt final d’implémentation, puis l’exécution correspondante.

### 2.6.3 Travail à réaliser

* Lire le rapport d’analyse du lot.
* Générer un prompt d’implémentation cadré.
* Exécuter Codex.
* Sauvegarder les artefacts correspondants.

### 2.6.4 Sous-découpage recommandé

1. Créer `LotImplementationStep`
2. Construire le prompt d’implémentation
3. Exécuter Codex
4. Sauvegarder `result_[numLot]_implements.md`
5. Charger le rapport d’implémentation dans le contexte

### 2.6.5 Point d’attention

Le prompt d’implémentation doit rester strictement borné :

* périmètre ;
* contraintes ;
* structure attendue ;
* limitations.

---

## 2.7 Étape 6 — Implémenter la relecture et la correction du lot

### 2.7.1 Problème à traiter

Le workflow prévoit une phase d’audit/correction après implémentation.

### 2.7.2 Objectif

Créer une boucle capable de :

* relire le résultat d’implémentation ;
* détecter bug ou dérive ;
* lancer une correction si nécessaire.

### 2.7.3 Travail à réaliser

* Lire le résultat d’implémentation.
* Lire la relecture critique de Codex si elle existe.
* Déterminer s’il faut corriger.
* Générer un prompt de correction.
* Réexécuter Codex.

### 2.7.4 Sous-découpage recommandé

1. Créer `LotReviewStep`
2. Créer `LotCorrectionStep`
3. Gérer les états :

  * validé
  * correction requise
  * blocage humain
4. Sauvegarder `result_[numLot]_correction.md`
5. Mettre à jour le compteur de tentative

### 2.7.5 Règle importante

Une correction automatique n’est autorisée que s’il s’agit d’un bug, d’un oubli ou d’une dérive technique raisonnablement corrigeable.
En présence d’un vrai arbitrage technique, le flux doit s’arrêter et remonter une question.

---

## 2.8 Étape 7 — Produire le rapport final de clôture

### 2.8.1 Problème à traiter

Le workflow prévoit explicitement un bilan final d’étape, destiné à documenter ce qui a été fait, ce qui ne l’a pas été, les dettes assumées et les décisions prises.

### 2.8.2 Objectif

Automatiser la génération et la sauvegarde du bilan final.

### 2.8.3 Travail à réaliser

* Rassembler les artefacts de l’étape.
* Produire le bilan final.
* Sauvegarder `z_finalRepport.md`.

### 2.8.4 Sous-découpage recommandé

1. Créer `FinalReportStep`
2. Agréger les résultats des lots
3. Synthétiser :

  * implémenté
  * non implémenté
  * dettes assumées
  * recommandations futures
4. Sauvegarder le rapport final

### 2.8.5 Point d’attention

Le système doit rester transparent : le rapport final doit refléter réellement le déroulé, et non inventer une clôture propre si l’étape a été bloquée.

---

## 2.9 Critère de sortie de la Phase 2

La Phase 2 est terminée lorsque :

* l’analyse globale fonctionne ;
* les questions structurantes peuvent être extraites ;
* les lots peuvent être exécutés dans l’ordre ;
* l’analyse, l’implémentation, la correction et la clôture sont supportées ;
* les artefacts produits respectent le workflow existant.

---

# Phase 3 — Intégrer la validation technique locale

## 3.1 Finalité de la phase

Une fois le workflow logique en place, il faut intégrer la validation locale, car c’est elle qui permettra de distinguer :

* un lot réellement exploitable ;
* un lot seulement “plausible” d’un point de vue LLM.

Cette phase est fondamentale pour faire de l’intégration dans Toolkit-Bridge un vrai outil de travail, et non un simple générateur de rapports.

---

## 3.2 Étape 1 — Introduire un exécuteur local de validation

### 3.2.1 Problème à traiter

Le build Maven ne doit pas dépendre de Codex, notamment à cause des limitations de sandbox et de l’accès au système local.

### 3.2.2 Objectif

Créer un service local dédié à l’exécution de validations techniques.

### 3.2.3 Travail à réaliser

* Créer une brique d’exécution locale.
* Supporter différentes commandes Maven.
* Capturer :

  * logs ;
  * code retour ;
  * durée ;
  * succès/échec.

### 3.2.4 Sous-découpage recommandé

1. Créer `WorkflowValidationService`
2. Créer `ValidationCommand`
3. Créer `ValidationResult`
4. Gérer les timeouts et erreurs système
5. Préparer l’intégration aux étapes de workflow

### 3.2.5 Point d’attention

Ce service doit être borné et contrôlé ; il ne doit pas devenir une exécution shell générique incontrôlée.

---

## 3.3 Étape 2 — Ajouter les tests ciblés par lot

### 3.3.1 Problème à traiter

Pendant le déroulé du lot, la validation la plus utile est locale et ciblée sur le périmètre du lot courant.

### 3.3.2 Objectif

Supporter l’exécution de tests ciblés pendant la boucle d’implémentation/correction.

### 3.3.3 Travail à réaliser

* Permettre à chaque lot d’exprimer ses validations ciblées.
* Exécuter ces tests après implémentation.
* Réinjecter le résultat dans la logique de correction.

### 3.3.4 Sous-découpage recommandé

1. Définir une structure `LotValidationPlan`
2. Permettre des commandes ciblées
3. Exécuter après chaque implémentation
4. Interpréter les échecs comme :

  * bug corrigeable
  * blocage humain
5. Archiver les logs utiles

### 3.3.5 Pourquoi cette étape est importante

Parce qu’elle permet d’avoir une boucle de correction guidée par des signaux techniques concrets.

---

## 3.4 Étape 3 — Ajouter le build Maven intermédiaire en `skipTests`

### 3.4.1 Problème à traiter

Un lot peut faire passer ses tests ciblés tout en cassant le build global de compilation ou d’assemblage.

### 3.4.2 Objectif

Introduire un build local intermédiaire plus global, mais encore compatible avec un déroulé fluide.

### 3.4.3 Travail à réaliser

* Lancer un build local `skipTests` après validation ciblée du lot.
* Réinjecter le résultat dans la logique de review/correction.

### 3.4.4 Sous-découpage recommandé

1. Définir la commande standard intermédiaire
2. Exécuter après chaque lot validé localement
3. Archiver le résultat
4. En cas d’échec :

  * demander correction si bug
  * bloquer si ambiguïté forte

### 3.4.5 Point d’attention

Il faut rester pragmatique : ce build intermédiaire sert à limiter les dérives, pas à remplacer la validation finale complète.

---

## 3.5 Étape 4 — Ajouter la validation complète en fin d’étape

### 3.5.1 Problème à traiter

Une étape ne doit pas être considérée comme terminée tant qu’un build complet avec tous les tests n’a pas été exécuté.

### 3.5.2 Objectif

Faire du build complet final un critère de sortie obligatoire de l’étape.

### 3.5.3 Travail à réaliser

* Lancer la commande complète en fin de workflow.
* Interpréter le résultat.
* Bloquer la clôture en cas d’échec.
* Réinjecter les erreurs dans une éventuelle correction finale si cela reste raisonnable.

### 3.5.4 Sous-découpage recommandé

1. Ajouter `FinalValidationStep`
2. Exécuter le build complet
3. Archiver logs et statut
4. Refuser le statut final `COMPLETED` si le build global est KO

### 3.5.5 Règle importante

La fin d’étape n’est validée que si cette validation finale passe réellement.

---

## 3.6 Critère de sortie de la Phase 3

La Phase 3 est terminée lorsque :

* les validations locales sont exécutables ;
* les lots supportent des tests ciblés ;
* le build intermédiaire `skipTests` est intégré ;
* le build complet final conditionne réellement la clôture de l’étape.

---

# Phase 4 — Intégrer l’interaction humaine via Telegram

## 4.1 Finalité de la phase

Le workflow doit pouvoir être déclenché et supervisé via Telegram, puisque c’est le canal retenu.
Cette phase introduit le lien entre la brique workflow et le canal utilisateur.

---

## 4.2 Étape 1 — Déclenchement du workflow via Telegram

### 4.2.1 Problème à traiter

Le run doit pouvoir être démarré simplement à partir d’un agent Telegram, avec référence à une roadmap présente dans son workspace.

### 4.2.2 Objectif

Permettre à l’utilisateur de demander le déroulé d’une étape donnée via Telegram.

### 4.2.3 Travail à réaliser

* Définir une commande Telegram.
* Permettre d’indiquer :

  * le fichier roadmap ;
  * éventuellement la mini-roadmap ;
  * l’étape à dérouler.
* Initialiser un `WorkflowRun`.

### 4.2.4 Sous-découpage recommandé

1. Créer la commande Telegram dédiée
2. Résoudre les fichiers du workspace
3. Créer le run
4. Retourner un message d’état initial

### 4.2.5 Point d’attention

Pour cette v1, l’interface Telegram peut rester simple et textuelle.

---

## 4.3 Étape 2 — Remonter les questions bloquantes à l’utilisateur

### 4.3.1 Problème à traiter

Les décisions d’architecture importantes doivent être posées à l’utilisateur dès qu’elles sont détectées, en particulier dès l’analyse initiale.

### 4.3.2 Objectif

Permettre au workflow de se suspendre proprement et de poser ses questions via Telegram.

### 4.3.3 Travail à réaliser

* Définir le format de question remonté à Telegram.
* Associer la question à un `WorkflowRun`.
* Passer le run en `WAITING_HUMAN`.

### 4.3.4 Sous-découpage recommandé

1. Créer `WorkflowTelegramBridge`
2. Gérer l’envoi des questions
3. Gérer le stockage des questions en attente
4. Permettre la reprise du run

### 4.3.5 Point d’attention

Il faut distinguer clairement :

* question bloquante de conception ;
* simple information de suivi.

---

## 4.4 Étape 3 — Gérer la reprise après réponse humaine

### 4.4.1 Problème à traiter

Poser une question ne suffit pas ; il faut pouvoir reprendre le workflow exactement au bon point après réponse.

### 4.4.2 Objectif

Ajouter un mécanisme simple de reprise.

### 4.4.3 Travail à réaliser

* Associer la réponse Telegram au run en attente.
* Réinjecter la réponse dans le contexte.
* Reprendre à l’étape suspendue.

### 4.4.4 Sous-découpage recommandé

1. Créer le mapping `run ↔ question`
2. Gérer la réception de la réponse
3. Réhydrater le contexte
4. Reprendre le step concerné

### 4.4.5 Pourquoi cette étape est importante

Parce qu’un workflow semi-automatique n’a de valeur que s’il sait reprendre proprement sans perdre son contexte.

---

## 4.5 Étape 4 — Remonter les statuts et le suivi d’exécution

### 4.5.1 Problème à traiter

L’utilisateur doit savoir où en est le workflow, sans avoir à explorer manuellement tous les fichiers.

### 4.5.2 Objectif

Ajouter un minimum de feedback via Telegram :

* lancement ;
* lot courant ;
* question en attente ;
* correction en cours ;
* étape terminée ;
* échec.

### 4.5.3 Travail à réaliser

* Définir des messages d’état standardisés.
* Les envoyer aux moments clés du workflow.

### 4.5.4 Sous-découpage recommandé

1. Définir les événements utilisateur importants
2. Créer un mapping événement → message Telegram
3. Limiter le bruit
4. Préserver la lisibilité de la conversation

### 4.5.5 Point d’attention

Il faut éviter un spam de logs ; Telegram sert ici à la supervision et à l’arbitrage humain.

---

## 4.6 Critère de sortie de la Phase 4

La Phase 4 est terminée lorsque :

* le workflow peut être déclenché via Telegram ;
* les questions bloquantes peuvent être posées ;
* les réponses humaines permettent la reprise ;
* l’utilisateur reçoit un suivi d’exécution minimal mais utile.

---

# Phase 5 — Fiabiliser la boucle de correction et les règles d’arrêt

## 5.1 Finalité de la phase

Une fois le workflow exécutable de bout en bout, il faut stabiliser les comportements de correction pour éviter :

* les boucles infinies ;
* les corrections absurdes ;
* les validations trop permissives ;
* les blocages mal gérés.

---

## 5.2 Étape 1 — Formaliser les cas “bug corrigeable” vs “blocage humain”

### 5.2.1 Problème à traiter

Le système doit distinguer ce qui relève d’une correction automatique raisonnable de ce qui nécessite un arbitrage humain.

### 5.2.2 Objectif

Définir une politique de décision explicite.

### 5.2.3 Travail à réaliser

* Lister les cas de correction automatique autorisés.
* Lister les cas de blocage humain.
* Définir des règles d’interprétation.

### 5.2.4 Sous-découpage recommandé

1. Définir la politique de correction
2. Définir la politique de blocage
3. Intégrer cette logique dans les steps de review/validation
4. Documenter les règles

### 5.2.5 Point d’attention

Il faut rester simple : la v1 n’a pas besoin d’un moteur de policy sophistiqué.

---

## 5.3 Étape 2 — Ajouter un compteur de tentatives et une règle d’arrêt

### 5.3.1 Problème à traiter

Sans limite, la boucle de correction peut tourner indéfiniment.

### 5.3.2 Objectif

Bornage simple et sûr du nombre de corrections automatiques.

### 5.3.3 Travail à réaliser

* Stocker un compteur de tentatives.
* Définir un maximum N.
* Bloquer proprement lorsque la limite est atteinte.

### 5.3.4 Sous-découpage recommandé

1. Ajouter le compteur au `LotRun`
2. Ajouter un paramètre maximum
3. Arrêter avec un statut explicite au dépassement
4. Remonter ce blocage via Telegram

### 5.3.5 Pourquoi cette étape est essentielle

Parce qu’elle rend le système exploitable et prévisible.

---

## 5.4 Étape 3 — Ajouter les protections sur le périmètre d’implémentation

### 5.4.1 Problème à traiter

Codex peut parfois sortir du périmètre demandé.

### 5.4.2 Objectif

Détecter les dérives majeures de périmètre.

### 5.4.3 Travail à réaliser

* Comparer les fichiers touchés avec le périmètre attendu.
* Signaler les écarts importants.
* Traiter cela comme correction ou blocage selon les cas.

### 5.4.4 Sous-découpage recommandé

1. Définir un périmètre attendu par lot
2. Ajouter une comparaison basique des fichiers modifiés
3. Intégrer le résultat dans la logique de review
4. Bloquer en cas de dérive importante

### 5.4.5 Point d’attention

Il ne faut pas viser une précision parfaite, seulement un garde-fou utile.

---

## 5.5 Étape 4 — Stabiliser la reprise et les états d’échec

### 5.5.1 Problème à traiter

Un workflow interrompu doit pouvoir être compris et repris, sans ambiguïté.

### 5.5.2 Objectif

Fiabiliser les cas :

* crash ;
* timeout ;
* build KO ;
* question sans réponse ;
* limite de tentatives atteinte.

### 5.5.3 Travail à réaliser

* Clarifier les statuts d’échec.
* Permettre la relecture du contexte.
* Assurer une reprise ou un arrêt propre.

### 5.5.4 Sous-découpage recommandé

1. Définir les états d’arrêt explicites
2. Garantir que les artefacts restent lisibles
3. Permettre la reprise contrôlée
4. Vérifier l’absence de statut incohérent

### 5.5.5 Pourquoi cette étape vient ici

Parce qu’elle fiabilise l’exploitation réelle de la brique workflow.

---

## 5.6 Critère de sortie de la Phase 5

La Phase 5 est terminée lorsque :

* la boucle de correction est bornée ;
* les blocages humains sont correctement distingués ;
* les dérives de périmètre sont détectées ;
* les états d’arrêt et de reprise sont lisibles.

---

# Phase 6 — Consolidation et préparation des évolutions futures

## 6.1 Finalité de la phase

Cette dernière phase ne vise pas à généraliser tout de suite le système, mais à préparer proprement la suite :

* futurs workflows ;
* éventuelle abstraction plus générique ;
* possible externalisation hors Java ;
* meilleure intégration au reste de Toolkit-Bridge.

---

## 6.2 Étape 1 — Nettoyer l’architecture interne de la brique workflow

### 6.2.1 Problème à traiter

Une première implémentation utile peut rapidement accumuler des couplages techniques.

### 6.2.2 Objectif

Stabiliser les responsabilités internes avant toute ouverture plus large.

### 6.2.3 Travail à réaliser

* Vérifier les responsabilités des services.
* Réduire les dépendances inutiles.
* Clarifier les interfaces internes.

### 6.2.4 Sous-découpage recommandé

1. Revoir les services créés
2. Détecter les couplages excessifs
3. Extraire si nécessaire de petites abstractions
4. Documenter l’architecture obtenue

### 6.2.5 Point d’attention

Il ne faut pas réécrire la brique trop tôt ; seulement consolider ce qui a émergé de la v1.

---

## 6.3 Étape 2 — Documenter les points d’extension futurs

### 6.3.1 Problème à traiter

Tu envisages déjà, à terme, une externalisation des workflows hors Java, potentiellement via script ou format déclaratif.

### 6.3.2 Objectif

Préparer cette possibilité sans la mettre en œuvre maintenant.

### 6.3.3 Travail à réaliser

* Identifier ce qui est aujourd’hui spécifique au workflow dev Codex.
* Identifier ce qui pourrait être abstrait plus tard.
* Documenter les candidats à une future externalisation.

### 6.3.4 Sous-découpage recommandé

1. Lister les éléments “workflow-specific”
2. Lister les éléments “réutilisables”
3. Écrire une note d’architecture sur les futurs workflows
4. Identifier les points de bascule possibles vers un format externe

### 6.3.5 Point d’attention

La documentation doit rester factuelle et ne pas forcer prématurément une solution type JS, YAML ou DSL.

---

## 6.4 Étape 3 — Ajouter un minimum de tests d’intégration du workflow

### 6.4.1 Problème à traiter

Une brique orchestratrice sans tests d’intégration devient vite fragile.

### 6.4.2 Objectif

Couvrir les parcours critiques du workflow.

### 6.4.3 Travail à réaliser

* Simuler un run nominal.
* Simuler un run avec question bloquante.
* Simuler une boucle de correction.
* Simuler un échec de validation finale.

### 6.4.4 Sous-découpage recommandé

1. Définir 3 à 5 scénarios critiques
2. Mock ou stub de Codex là où nécessaire
3. Vérifier les statuts finaux
4. Vérifier les artefacts et transitions

### 6.4.5 Valeur de cette étape

Elle sécurise la brique avant son usage plus intensif.

---

## 6.5 Critère de sortie de la Phase 6

La Phase 6 est terminée lorsque :

* la brique workflow est proprement consolidée ;
* ses responsabilités sont claires ;
* les futures pistes d’évolution sont documentées ;
* les scénarios critiques sont couverts par des tests pertinents.

---

# 7. Ordonnancement recommandé

## 7.1 Enchaînement global

Ordre recommandé :

1. **Phase 1 — Fondation exécutable minimale**
2. **Phase 2 — Dérouler le workflow d’implémentation en Java**
3. **Phase 3 — Intégrer la validation technique locale**
4. **Phase 4 — Intégrer l’interaction humaine via Telegram**
5. **Phase 5 — Fiabiliser la boucle de correction et les règles d’arrêt**
6. **Phase 6 — Consolidation et préparation des évolutions futures**

Cet ordre est important :

* la Phase 1 crée le socle ;
* la Phase 2 apporte la logique métier du workflow ;
* la Phase 3 rend le système techniquement crédible ;
* la Phase 4 le rend réellement utilisable ;
* la Phase 5 le rend exploitable dans la durée ;
* la Phase 6 prépare proprement la suite.

---

## 7.2 Priorités absolues

Si un arbitrage est nécessaire, les priorités absolues sont :

1. Modèle runtime minimal du workflow
2. Étapes Java explicites
3. Client Codex dédié
4. Gestionnaire d’artefacts
5. Analyse initiale + extraction des décisions d’architecture
6. Déroulé par lots
7. Validation locale Maven
8. Questions/réponses via Telegram
9. Boucle de correction bornée
10. Rapport final de clôture

---

# 8. Format d’exploitation pour l’implémentation

Pour réaliser cette roadmap proprement dans ton workflow habituel, il est conseillé de la découper :

* **phase par phase** ;
* **étape par étape** ;
* avec un prompt Codex ciblé pour chaque lot d’implémentation.

Exemples de bons découpages :

* modèle runtime minimal du workflow
* interface `WorkflowStep` et résultats structurés
* `CodexWorkflowClient`
* `WorkflowArtifactService`
* `GlobalAnalysisStep`
* extraction des questions d’architecture
* déroulé des lots
* validation Maven locale
* bridge Telegram
* boucle de correction bornée
* rapport final

Il faut éviter les prompts trop vastes qui mélangent :

* orchestration ;
* Codex ;
* Telegram ;
* validation ;
* persistance ;
* futures abstractions de workflows.

---

# 9. Conclusion

Cette roadmap construit volontairement une **première brique simple mais sérieuse** dans Toolkit-Bridge :

* suffisamment structurée pour être maintenable ;
* suffisamment concrète pour être utile vite ;
* suffisamment bornée pour ne pas dériver en “framework de workflows” prématuré.

Elle respecte l’esprit du workflow actuel :

* analyser avant d’implémenter ;
* cadrer strictement Codex ;
* corriger avec méthode ;
* documenter tout le déroulé ;
* garder l’humain sur les vraies décisions.

Le résultat attendu n’est donc pas un système magique ou totalement autonome, mais une brique de travail fiable, qui automatise la mécanique répétitive tout en conservant :

* la validation locale comme vérité ;
* Telegram comme canal humain ;
* et l’utilisateur comme arbitre des choix d’architecture importants.
