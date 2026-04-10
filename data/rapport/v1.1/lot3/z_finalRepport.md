# z_finalRepport.md — Lot 3 — Persistance durable minimale

## 1. Résumé exécutif

Le lot 3 a introduit une **persistance durable ciblée** sur les éléments critiques du système agentique, sans basculer dans une architecture lourde ou prématurée.

Objectif atteint :
- garantir la survie des données essentielles après redémarrage
- améliorer significativement la capacité de diagnostic
- poser un socle propre pour les futures évolutions (observabilité, multi-agent, audit)

Le choix structurant du lot :
👉 **persistance par snapshot ciblé**, et non event sourcing global

---

## 2. Ce qui a été implémenté

### 2.1 Interventions humaines (Phase 1)

- Persistance des demandes et décisions
- Modèle simple : état courant + décision finale
- Gestion de concurrence minimale (update conditionnel)
- Metadata JSON avec filtrage léger

**Résultat :**
- aucune perte de décisions humaines
- traçabilité minimale assurée

---

### 2.2 Tâches admin (Phase 2)

- Persistance des `AdminTaskSnapshot`
- Modèle : dernier état par `taskId`
- Upsert maîtrisé (update puis insert)
- Tri déterministe + normalisation `agentId`

**Résultat :**
- visibilité stable des tâches après redémarrage
- base solide pour supervision technique

---

### 2.3 Traces critiques (Phase 3)

- Introduction d’un **sink DB dédié**
- Sélection des types critiques :
    - ERROR
    - TASK_STARTED
    - RESPONSE
    - DELEGATION
    - TOOL_CALL

- Stockage JSON brut des `attributes` avec sanitation légère
- DB devient source de vérité pour la vue admin

**Résultat :**
- capacité de diagnostic post-mortem réelle
- corrélation partielle via `traceId`
- réduction du bruit par filtrage

---

### 2.4 Rétention minimale (Phase 4)

- Intégration avec `toolkit.persistence.retention`
- TTL par domaine :
    - `trace.critical` → 7 jours
    - `task.admin_snapshot` → 30 jours
- Scheduler quotidien
- Service de purge isolé
- Aucune purge des interventions humaines

**Résultat :**
- volume maîtrisé
- cohérence avec la mécanique existante
- aucune duplication de configuration

---

## 3. Choix d’architecture majeurs

### 3.1 Snapshot vs Event Sourcing

Choix :
- ❌ pas de log complet d’événements
- ✅ snapshot courant + traces critiques ciblées

**Pourquoi :**
- complexité maîtrisée
- implémentation progressive
- coût cognitif réduit

---

### 3.2 Réutilisation de l’existant

- utilisation de `PersistenceRetentionPolicyResolver`
- extension propre de la config existante
- aucun système parallèle introduit

👉 Évite la dette structurelle

---

### 3.3 Séparation des responsabilités

- persistence ≠ admin view
- policy ≠ runtime (cleanup séparé)
- sink DB isolé
- services métier non couplés à la purge

👉 Architecture saine et évolutive

---

### 3.4 Minimalisme volontaire

- pas de cache
- pas d’archivage
- pas de projection SQL avancée
- pas de moteur de recherche

👉 Favorise :
- robustesse
- lisibilité
- itération rapide

---

## 4. Dette technique assumée

### 4.1 Corrélation incomplète (`traceId`)

- nullable en base
- attendu mais non garanti

**Risque :**
- données difficilement corrélables

**À faire plus tard :**
- garantir présence au niveau orchestrator

---

### 4.2 JSON brut pour attributes

- flexible mais non structuré

**Risque :**
- difficile à requêter
- hétérogénéité

**À faire plus tard :**
- projection partielle des champs critiques

---

### 4.3 Terminologie non unifiée (`runId` / `traceId`)

- coexistence de concepts proches

**Risque :**
- confusion
- erreurs de corrélation

**À faire plus tard :**
- unification stricte des identifiants internes

---

### 4.4 TOOL_CALL potentiellement volumineux

- sanitation légère seulement

**Risque :**
- volume élevé
- bruit

**À faire plus tard :**
- affiner le modèle (toolName + params seulement)

---

## 5. Ce qui n’a PAS été fait (volontairement)

- ❌ historisation complète des états
- ❌ event sourcing global
- ❌ moteur de recherche avancé
- ❌ UI avancée
- ❌ normalisation stricte des attributes
- ❌ unification complète des IDs

**Pourquoi :**
👉 éviter la sur-architecture prématurée

---

## 6. Ce que ce lot débloque

### 6.1 Diagnostic réel

- analyse post-mortem possible
- compréhension des flows agents

---

### 6.2 Supervision technique

- vision stable des tâches
- visibilité des erreurs

---

### 6.3 Base pour multi-agent

- corrélation des actions
- traçabilité inter-agent

---

### 6.4 Extension observabilité

- ajout futur de metrics
- enrichissement des traces

---

## 7. Recommandations pour les prochaines phases

### Priorité 1 — Observabilité avancée

- enrichir les traces critiques
- introduire des vues corrélées (`traceId`)

---

### Priorité 2 — Unification des identifiants

- définir :
    - `traceId`
    - `taskId`
    - `conversationId`
- règles strictes

---

### Priorité 3 — Projection partielle JSON

- extraire champs clés :
    - toolName
    - status
    - errorType

---

### Priorité 4 — UI admin

- visualisation des flows
- debugging assisté

---

### Priorité 5 — Rétention avancée

- TTL différencié par type
- éventuellement archivage léger

---

## 8. Conclusion

Le lot 3 est une réussite sur le plan architecture et implémentation :

- progression maîtrisée
- décisions cohérentes
- dette contrôlée
- base solide pour la suite

👉 Tu disposes maintenant d’un **système agentique observable, persistant et exploitable**, prêt pour des évolutions plus avancées sans refonte majeure.