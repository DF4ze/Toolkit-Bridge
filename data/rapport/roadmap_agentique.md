# Roadmap technique — plateforme agentique générique

## 0. Principes d’architecture à figer
### 0.1 Runtime agentique comme brique centrale
#### 0.1.1 Formaliser `AgentRuntime`
- Porter l’identité, le rôle, l’orchestrator, la policy, la mémoire, les tools et le workspace.
- Éviter un modèle d’agent réduit à une simple configuration.
- Préparer la délégation et l’observabilité dès le socle.

#### 0.1.2 Séparer configuration et état d’exécution
- Garder une définition statique de l’agent.
- Isoler l’état runtime dans un objet dédié.
- Permettre l’évolution vers supervision, reprise et dashboard.

### 0.2 Contrats explicites entre briques
#### 0.2.1 Isoler les grands sous-systèmes
- Séparer clairement orchestrator, mémoire, bus, tooling, traces et policy.
- Limiter les dépendances croisées.
- Préparer une extension incrémentale sans refactor globale.

#### 0.2.2 Stabiliser les interfaces publiques
- Définir les API internes avant de multiplier les implémentations.
- Préférer des contrats sobres mais extensibles.
- Réserver les optimisations pour plus tard.

---

## 1. Socle runtime multi-agent
### 1.1 Modèle d’agent complet
#### 1.1.1 Définir les composants obligatoires d’un agent
- Orchestrator propre à l’agent.
- Mémoire accessible selon ses scopes.
- Set de tools autorisés, policy et workspace.

#### 1.1.2 Introduire la notion de rôle
- Affecter un rôle fonctionnel à chaque agent.
- Préparer la délégation par rôle.
- Ouvrir la voie à un futur équilibrage de charge.

### 1.2 Cycle de vie runtime
#### 1.2.1 Créer un état d’exécution agent
- Suivre disponibilité, occupation, tâche courante et contexte actif.
- Préparer les futures métriques d’activité.
- Faciliter la supervision.

#### 1.2.2 Centraliser l’accès au runtime
- Fournir un registre ou manager des runtimes.
- Éviter les accès directs dispersés.
- Simplifier les évolutions futures.

---

## 2. Orchestrators multiples
### 2.1 Conserver un orchestrator `chat`
#### 2.1.1 Isoler le mode conversationnel
- Garder un orchestrator simple pour le blabla et les tests rapides.
- Éviter de casser le flux déjà existant.
- Limiter son périmètre fonctionnel.

#### 2.1.2 Définir ses frontières
- Pas de gestion avancée de task.
- Pas de délégation complexe.
- Usage simple et lisible.

### 2.2 Introduire un orchestrator `task`
#### 2.2.1 Créer un flux orienté objectif
- Transformer une demande en tâche exécutable.
- Gérer sous-tâches, outils, délégations et résultat final.
- Structurer les futures capacités autonomes.

#### 2.2.2 Préparer le multi-orchestrator
- Résoudre un agent vers le bon orchestrator.
- Factoriser les briques communes.
- Permettre l’ajout futur d’un orchestrator planner ou critic.

---

## 3. Modèle de tâche et sous-tâches
### 3.1 Introduire un `TaskModel` minimal
#### 3.1.1 Définir l’entité de base
- `taskId`, objectif, initiateur, agent assigné, parentTaskId, metadata.
- Prévoir une liste d’artefacts liés.
- Permettre la corrélation avec les traces.

#### 3.1.2 Décider le point d’entrée
- Une conversation utilisateur peut rester dans `chat`.
- Une exécution agentique passe par `task`.
- Une délégation crée toujours une sous-tâche.

### 3.2 Définir le cycle de vie
#### 3.2.1 Poser les statuts V1
- `CREATED`, `RUNNING`, `WAITING`, `DONE`, `FAILED`, `CANCELLED`.
- Garder un modèle simple mais utile.
- Préparer un futur dashboard.

#### 3.2.2 Encadrer les transitions
- Définir les transitions légitimes entre statuts.
- Bloquer les changements incohérents.
- Faciliter l’audit et le debug.

---

## 4. Bus interne de communication
### 4.1 Mettre en place un bus inter-agent
#### 4.1.1 Créer une abstraction de message
- Enveloppe typée avec identifiant, corrélation, émetteur, destinataire, horodatage.
- Supporter agent cible ou rôle cible.
- Préparer la supervision et la persistance.

#### 4.1.2 Définir les types de messages V1
- `TASK_REQUEST`, `TASK_RESPONSE`, `QUESTION`, `ANSWER`, `EVENT`, `CRITIQUE`.
- Rester minimal mais explicite.
- Éviter le tout-texte non cadré.

### 4.2 Préparer l’adressage par rôle
#### 4.2.1 Découpler rôle et instance
- Permettre de déléguer à un rôle plutôt qu’à un agent précis.
- Préparer le remplacement transparent d’un agent.
- Ouvrir la voie au load balancing futur.

#### 4.2.2 Prévoir une résolution simple
- D’abord un agent libre par rôle.
- Garder la stratégie de sélection interchangeable.
- Éviter de figer trop tôt une logique complexe.

---

## 5. Modèle d’artefact
### 5.1 Introduire un type métier `Artifact`
#### 5.1.1 Définir les familles V1
- Rapport, patch, script, plan, résumé, fichier, mémoire candidate.
- Typage clair pour les échanges.
- Réutilisation dans les tâches et les messages.

#### 5.1.2 Relier artefacts et tâches
- Associer les sorties à une tâche donnée.
- Faciliter reprise, audit et partage inter-agent.
- Sortir du tout-prompt textuel.

### 5.2 Gérer le stockage des artefacts
#### 5.2.1 Séparer métadonnées et contenu
- Métadonnées en base ou modèle structuré.
- Contenu dans workspace ou stockage dédié.
- Éviter de tout mélanger dans les logs.

#### 5.2.2 Prévoir la rétention par type
- Appliquer des règles distinctes selon la nature de l’artefact.
- Faciliter nettoyage et archivage.
- Rester pilotable par configuration.

---

## 6. Système mémoire branché et exploitable
### 6.1 Brancher la mémoire dans l’orchestrator
#### 6.1.1 Créer un `ContextAssembler`
- Injecter conversation, règles, contexte agent et contexte global.
- Contrôler la taille du contexte.
- Centraliser la logique de composition.

#### 6.1.2 Définir les priorités d’injection
- Règles avant faits, faits avant historique, historique avant bruit.
- Poser des caps stricts.
- Garder un prompt stable.

### 6.2 Clarifier les scopes mémoire
#### 6.2.1 Distinguer les niveaux métier
- Mémoire agent, mémoire utilisateur, mémoire projet, mémoire système.
- Ne pas tout noyer dans un unique stock.
- Préparer permissions et retrieval.

#### 6.2.2 Garder la conversation à part
- Conversation memory dédiée au court terme.
- Autres mémoires pour le durable.
- Éviter les confusions de rôle.

---

## 7. Contexte global partagé
### 7.1 Introduire un support de contexte partagé
#### 7.1.1 Utiliser un ou plusieurs fichiers Markdown de référence
- Exemples : profil utilisateur, règles globales, contexte projets.
- Simplicité d’édition humaine.
- Mise en place rapide sans grosse base dédiée.

#### 7.1.2 Prévoir deux modes de chargement
- Lecture à la volée sur chaque assemblage de contexte.
- Ou cache mémoire avec rechargement contrôlé.
- Pilotage du mode via properties.

### 7.2 Formaliser la frontière avec le shared workspace
#### 7.2.1 Réserver le contexte global aux connaissances stables
- Nom utilisateur, règles durables, conventions, contexte projet.
- Fichiers pensés pour être relus par les agents.
- Format lisible et éditable.

#### 7.2.2 Réserver le shared workspace au travail courant
- Rapports, scripts, exports, artefacts et brouillons.
- Éviter d’y mélanger les règles globales.
- Mieux contrôler qui écrit quoi.

---

## 8. Mémoire explicite et tooling mémoire
### 8.1 Introduire des tools mémoire V1
#### 8.1.1 Prévoir un accès RW sur les mémoires utiles
- Lire un contexte mémoire.
- Ajouter ou modifier un fait.
- Ajouter ou modifier une règle.

#### 8.1.2 Commencer petit
- Tool de rappel de contexte.
- Tool d’écriture d’un fait.
- Tool d’écriture d’une règle.

### 8.2 Distinguer explicite et implicite
#### 8.2.1 Garder une écriture automatique séparée
- Extraction implicite après exécution ou échange.
- Pipeline distinct des commandes explicites.
- Réduire les ambiguïtés métier.

#### 8.2.2 Donner une vraie intention d’écriture
- Une règle explicite ne doit pas être traitée comme un simple fait.
- Un contexte projet ne doit pas être noyé dans la conversation.
- Poser des types métier clairs.

---

## 9. Retrieval, scoring et injection mémoire
### 9.1 Mettre en place un retrieval borné
#### 9.1.1 Filtrer avant de scorer
- Sélectionner par scope, type et projet.
- Réduire le bruit dès l’entrée.
- Garder un coût maîtrisé.

#### 9.1.2 Limiter le top N
- N petit et configurable.
- Protection contre l’explosion du prompt.
- Résultat stable et prévisible.

### 9.2 Introduire un scoring simple V1
#### 9.2.1 Utiliser des critères basiques
- Importance, récence, fréquence.
- Implémentation lisible et testable.
- Ajustements plus fins plus tard.

#### 9.2.2 Préparer l’évolution
- Garder la formule interchangeable.
- Faciliter une future pondération adaptative.
- Éviter de coder le scoring dans l’orchestrator.

---

## 10. Policies et permissions V1
### 10.1 Créer une policy simple par agent
#### 10.1.1 Définir les autorisations utiles
- Tools autorisés.
- Scopes mémoire accessibles.
- Délégation autorisée ou non.

#### 10.1.2 Ajouter les capacités sensibles
- Accès web autorisé ou non.
- Écriture shared workspace autorisée ou non.
- Exécution de tools scriptés autorisée ou non.

### 10.2 Centraliser le contrôle
#### 10.2.1 Vérifier avant exécution
- Toute action sensible passe par la policy.
- Aucune décision de sécurité disséminée.
- Journaliser les refus.

#### 10.2.2 Préparer la montée en finesse
- Architecture compatible avec validation humaine future.
- Pas d’usine IAM dès le départ.
- Squelette durable contre la dette technique.

---

## 11. Plateforme de tools
### 11.1 Catégoriser officiellement les tools
#### 11.1.1 Distinguer `native` et `scripted`
- Tools natifs robustes et testables.
- Tools scriptés plus souples mais plus encadrés.
- Contrats clairs pour chaque catégorie.

#### 11.1.2 Associer un niveau de risque
- Lecture seule, écriture locale, exécution, sortie externe.
- Aider la policy et la validation future.
- Préparer la gouvernance.

### 11.2 Structurer le registre de tools
#### 11.2.1 Décrire chaque tool proprement
- Nom, catégorie, description, schéma, capacités, niveau de risque.
- Supporter filtrage par agent.
- Mieux exposer les possibilités aux LLM.

#### 11.2.2 Préparer les futures extensions
- Web, git, mémoire, fichiers, exécution, agents, scripts.
- Éviter les implémentations ad hoc.
- Conserver une vision homogène.

---

## 12. Scripted tools et évolution V2 préparée
### 12.1 Poser l’emplacement des scripted tools
#### 12.1.1 Prévoir un stockage persistant
- Un tool développé doit survivre au redémarrage.
- Métadonnées versionnées et contenu sauvegardé.
- Activation explicite.

#### 12.1.2 Garder V2 hors du chemin critique
- Ne pas bloquer la V1 dessus.
- Préparer seulement les abstractions nécessaires.
- Éviter le gros chantier prématuré.

### 12.2 Préparer la validation future
#### 12.2.1 Anticiper plusieurs classes de risque
- Lecture seule, écriture, exfiltration, modification système.
- Niveau de validation potentiellement différent.
- Support pour validation humaine ou automatisée.

#### 12.2.2 Ouvrir la voie à la revue multi-agent
- Agent dev produit.
- Agent sécurité ou autre rôle analyse.
- Humain tranche sur les cas sensibles.

---

## 13. Traces agentiques et observabilité
### 13.1 Séparer logs techniques et traces métier
#### 13.1.1 Conserver les logs applicatifs classiques
- Erreurs Java, stacktraces, infra et Spring.
- Rester compatibles avec l’écosystème standard.
- Ne pas surcharger les logs existants.

#### 13.1.2 Créer des traces agentiques structurées
- Événements métier au format JSON.
- Corrélation par `runId`, `taskId`, `agentId`, `messageId`.
- Faciliter debug, audit et analyse.

### 13.2 Définir les événements clés
#### 13.2.1 Couvrir les étapes utiles
- Début de tâche, assemblage contexte, tool call, délégation, réponse, erreur.
- Vision chronologique claire.
- Utilisable par dashboard ou supervision.

#### 13.2.2 Préparer plusieurs destinations
- Fichier JSON par agent ou flux centralisé.
- Exploitation future par UI d’admin.
- Base saine pour méta-analyse.

---

## 14. Supervision Telegram
### 14.1 Exposer les échanges inter-agent
#### 14.1.1 Relier le bus à un groupe Telegram
- Publier une vue lisible des messages importants.
- Offrir un monitoring humain simple.
- Aider au debug en temps réel.

#### 14.1.2 Garder le groupe en lecture seule d’abord
- Aucun impact direct sur le bus au début.
- Réduction du risque de perturbation.
- Mise en place progressive.

### 14.2 Préparer l’intervention humaine
#### 14.2.1 Envisager un mode validation plus tard
- Un humain pourra approuver ou bloquer une action.
- Le groupe servira alors d’interface légère.
- Évolution naturelle après stabilisation.

#### 14.2.2 Ne pas faire de Telegram le cœur du système
- Le bus interne reste la source de vérité.
- Telegram n’est qu’une exposition.
- Le métier reste indépendant du canal.

---

## 15. Débat et collaboration inter-agents
### 15.1 Définir le débat comme un protocole
#### 15.1.1 Formaliser les rôles complémentaires
- Planner, critic, sécurité, finance, commercial, exécuteur.
- Chaque rôle apporte une lecture différente.
- Réduire le biais du “tout va bien”.

#### 15.1.2 Encadrer les échanges
- Question initiale, réponses, critiques, synthèse.
- Corrélation stricte des messages.
- Limiter la dérive conversationnelle.

### 15.2 Commencer par des scénarios simples
#### 15.2.1 Revue croisée sur un artefact
- Un agent produit.
- Un autre critique selon son domaine.
- Le résultat est traçable.

#### 15.2.2 Garder la mécanique petite en V1/V2
- Pas de grand parlement multi-agent tout de suite.
- Commencer avec 2 ou 3 rôles.
- Évaluer le gain réel.

---

## 16. Process externes et auto-amélioration encadrée
### 16.1 Sortir les process du code dur
#### 16.1.1 Stocker les process dans des fichiers modifiables
- Fichiers versionnables, sauvegardables et auditables.
- Support d’un agent méta dédié.
- Réduction du risque sur le code Java.

#### 16.1.2 Prévoir backup avant modification
- Sauvegarde automatique avant toute réécriture.
- Historique simple des changements.
- Retour arrière possible.

### 16.2 Poser une auto-amélioration par proposition
#### 16.2.1 Commencer par l’observation
- Détecter lenteurs, répétitions, points faibles et idées d’outils.
- Produire des recommandations.
- Ne pas modifier silencieusement le runtime.

#### 16.2.2 Ouvrir plus tard la modification autonome encadrée
- D’abord suggestion.
- Puis génération de patch ou process.
- Activation selon règle ou validation.

---

## 17. Rétention et persistance pilotables
### 17.1 Typage des objets persistables
#### 17.1.1 Définir les familles durables
- Tasks, messages, traces, artefacts, mémoires, tools scriptés.
- Chaque type a son cycle de vie.
- Rendre les règles compréhensibles.

#### 17.1.2 Éviter les objets hybrides
- Clarifier éphémère vs durable.
- Ne pas laisser la persistance implicite.
- Faciliter maintenance et nettoyage.

### 17.2 Appliquer des règles de rétention par type
#### 17.2.1 Rendre la durée configurable
- Rétention pilotée par properties.
- Ajustable selon le domaine métier.
- Compatible avec des besoins futurs plus stricts.

#### 17.2.2 Préparer archivage et purge
- Purger ce qui est jetable.
- Archiver ce qui est critique.
- Préserver l’historique quand il a de la valeur.

---

## 18. Administration et pilotage
### 18.1 Cartographie et classification de la configuration
#### 18.1.1 Recenser les paramètres existants
Identifier les propriétés actuellement dispersées dans le code et le YAML.
Repérer les constantes métier, techniques et hybrides.
Lister les zones où la source de vérité est encore floue.

#### 18.1.2 Classer par nature et source cible
Distinguer configuration technique, configuration administrable et contenu éditable.
Déterminer ce qui doit rester dans le YAML, aller en base ou rester en fichiers.
Préparer une matrice claire de migration et de responsabilité.

#### 18.1.3 Identifier les accès à refactorer
Repérer les composants qui lisent directement le YAML ou des properties devenues métier.
Prévoir le remplacement par des services de lecture centralisés.
Éviter les accès implicites ou disséminés à la configuration.

### 18.2 Externalisation, refactoring et bootstrap de first-initialisation
#### 18.2.1 Définir la nouvelle source de vérité
Conserver le YAML pour le bootstrap et la configuration technique.
Déplacer en base les paramètres administrables modifiables à chaud.
Réserver les fichiers aux contenus métier lisibles et éditables.

#### 18.2.2 Refactorer l’accès à la configuration administrable
Introduire des services ou repositories dédiés à la lecture/écriture de config métier.
Remplacer progressivement les lectures directes du YAML devenues inadaptées.
Rendre explicite la frontière entre config statique et config administrable.

#### 18.2.3 Mettre en place un bootstrap de first-initialisation
Initialiser la base avec des valeurs par défaut si aucune configuration administrable n’existe.
Utiliser le YAML comme seed initial et non comme source vivante principale.
Garantir un démarrage cohérent sans reconfiguration manuelle complète.

#### 18.2.4 Préparer l’évolution sans dépendre du YAML en écriture
Éviter le RW applicatif sur application.yml comme mécanisme principal.
Permettre la persistance durable des changements faits depuis l’admin.
Préparer proprement les futures interfaces REST et UI d’administration.

### 18.3 Ne pas commencer par une grosse UI
#### 18.3.1 Stabiliser d’abord le modèle d’administration
- Agents, policies, tools, mémoire, bus, tâches et rétention.
- Éviter de figer une UI sur un modèle mouvant.
- Prioriser les concepts.

#### 18.3.2 Commencer par de l’admin technique simple
- Properties propres.
- Endpoints REST d’admin ciblés.
- Traces et états lisibles.

### 18.4 Préparer une future UI web d’administration
#### 18.4.1 Cibler les usages prioritaires
- Voir les agents, tâches, traces, policies et artefacts.
- Gérer les configurations liées.
- Aider au debug et à l’exploitation.

#### 18.4.2 La brancher sur un modèle déjà propre
- L’UI devient une couche de confort.
- Pas un pansement sur un modèle confus.
- Réduction forte des futures refontes.

---

## Ordre d’implémentation recommandé
### A. Fondations immédiates
#### A.1 À lancer tout de suite
- Runtime agent complet.
- Orchestrator `task`.
- Task model.
- Bus interne.
- Traces agentiques.

#### A.2 À intégrer dans la foulée
- ContextAssembler.
- Scopes mémoire.
- Contexte global partagé.
- Policy V1.
- Typage des artefacts.

### B. Capacité agentique utile
#### B.1 Première montée en puissance
- Tools mémoire V1.
- Retrieval et scoring.
- Supervision Telegram.
- Délégation simple par agent ou rôle.

#### B.2 Deuxième montée en puissance
- Débat ciblé entre rôles.
- Shared workspace mieux gouverné.
- Rétention configurable.
- Premiers endpoints d’admin.

### C. Évolutions avancées
#### C.1 V2 technique
- Scripted tools persistants.
- Revue multi-agent de tools.
- Validation humaine ciblée.
- Dashboard des tâches.

#### C.2 V2+ stratégique
- Auto-amélioration encadrée.
- Résolution avancée par rôle.
- UI d’administration complète.
- Optimisations de performance mémoire.
