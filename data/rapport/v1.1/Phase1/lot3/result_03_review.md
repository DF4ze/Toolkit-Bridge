# Revue critique d’architecture — Phase 3 — Persistance des traces critiques

## 1. Périmètre de revue
Relecture complète de l’implémentation Phase 3 sur :
- séparation configuration/runtime,
- qualité du modèle de persistance,
- découplage orchestrator/mémoire/tooling/policy/workspace,
- lisibilité/cohérence de nommage,
- qualité utile des tests,
- dette technique et risques de refactor évitables.

Code relu principalement :
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/*`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/CriticalTraceJpaSink.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryService.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/trace/persistence/*`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/agent/trace/sink/CriticalTraceJpaSinkTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/service/admin/TraceQueryServiceTest.java`

## 2. Points solides (validation)
- Le contrat central `DefaultAgentTraceService` est préservé : producteur unique + fan-out + isolation des erreurs sink.
- L’intégration du stockage critique est faite par extension (`AgentTraceSink`) sans couplage nouveau vers orchestrators, mémoire, tooling, policy ou workspace.
- Le modèle DB est minimal, lisible et aligné avec SQLite/JPA (`critical_agent_trace`, index utiles, corrélations nullable).
- Le filtrage des types critiques est explicite et conforme au cadrage (`ERROR`, `TASK_STARTED`, `RESPONSE`, `DELEGATION`, `TOOL_CALL`).
- La source admin des traces est clairement basculée sur la DB (plus de fallback mémoire dans `TraceQueryService`).
- Les tests demandés existent : unitaires ciblés, intégration pipeline, restart.

## 3. Faiblesses / points discutables

### [M1] Couplage de couche dans le mapper (persistence -> DTO admin)
- Référence : `CriticalAgentTraceMapper` importe `TechnicalAdminView` et expose `toTraceItem(...)` (`CriticalAgentTraceMapper.java:3`, `73-84`).
- Problème : la couche persistance connaît un DTO de présentation admin.
- Impact : fragilise l’évolutivité (changements UI/admin pouvant impacter directement la couche trace persistence), augmente le risque de refactor transversal futur.

### [M2] Frontière configuration/runtime incomplète (règles critiques hardcodées)
- Références : constantes et set critiques en dur (`CriticalAgentTraceMapper.java:25-37`, `172-179`).
- Problème : types critiques et seuils de sanitation sont codés en dur dans une classe runtime.
- Impact : moindre opérabilité (tuning nécessite redéploiement), risque de dette quand la phase suivante voudra ajuster finement les seuils sans toucher le code.

### [M3] Écriture DB synchrone sur le chemin d’exécution des traces
- Références : `CriticalTraceJpaSink.publish(...)` persiste en ligne (`CriticalTraceJpaSink.java:23-30`), appelé par `DefaultAgentTraceService` dans le même flux (`DefaultAgentTraceService.java:45-55`).
- Problème : chaque événement critique ajoute une I/O DB bloquante dans le chemin runtime.
- Impact : risque de latence/pression en charge (notamment `TOOL_CALL`/`DELEGATION`) et couplage temporel runtime <-> persistance.
- Note : acceptable en phase minimale, mais c’est la dette principale de robustesse opérationnelle.

### [L1] Double gestion d’erreur sink (redondance de logs)
- Références : try/catch dans le sink (`CriticalTraceJpaSink.java:28-31`) et try/catch global dans `DefaultAgentTraceService` (`DefaultAgentTraceService.java:45-54`).
- Problème : duplication de responsabilité + bruit potentiel de logs.
- Impact : faible, mais nuit à la lisibilité des responsabilités.

### [L2] Normalisation agentId côté lecture potentiellement insuffisante
- Référence : `TraceQueryService.normalize(...)` fait seulement `trim()` (`TraceQueryService.java:42-47`).
- Problème : si des variantes de casse apparaissent en base (ex. agents historiques), le filtre admin peut sembler incohérent.
- Impact : faible à moyen (expérience admin / diagnostics).

### [L3] Tests utiles mais quelques angles restent peu couverts
- Références : `CriticalTracePersistenceIT`, `CriticalAgentTraceMapperTest`.
- Points manquants :
  - test explicite de comportement quand JSON `attributes_json` est illisible au read-model;
  - test explicite sur tie-break `id DESC` via lecture repository (le tri est utilisé, mais peu verrouillé côté test d’intention).
- Impact : faible.

## 4. Corrections utiles proposées (sans élargir le périmètre)

1. Découpler la conversion persistance/admin
- Déplacer `toTraceItem(...)` hors `CriticalAgentTraceMapper` vers `TraceQueryService` (ou un mapper admin dédié côté `service.admin`).
- Bénéfice : séparation de couches plus propre, réduction du couplage futur.

2. Externaliser les seuils de sanitation en configuration dédiée
- Introduire des propriétés simples pour les longueurs max (ex. message tool/reason error/générique), sans changer la logique métier.
- Bénéfice : meilleure séparation config/runtime et tuning sans refactor.

3. Clarifier la responsabilité d’erreur sink
- Garder la capture d’exception au niveau `DefaultAgentTraceService` uniquement, et simplifier le sink.
- Bénéfice : logs plus lisibles, responsabilité unique.

4. Stabiliser la sémantique de filtre `agentId`
- Documenter et tester explicitement la règle de normalisation (casse incluse), ou normaliser de façon cohérente write/read.
- Bénéfice : comportement admin prévisible.

5. Compléter légèrement la suite de tests
- Ajouter un test de robustesse `attributes_json` invalide -> fallback sûr.
- Ajouter un test d’intention sur l’ordre déterministe `occurredAt DESC, id DESC`.
- Bénéfice : réduction du risque de régression sur la source de vérité admin.

## 5. Vérification demandée (synthèse)
- Séparation configuration/runtime : **correcte globalement**, avec amélioration recommandée sur les seuils hardcodés.
- Qualité du modèle implémenté : **bonne** (minimal, indexé, nullable cohérent).
- Découplage orchestrator/mémoire/tooling/policy/workspace : **préservé**.
- Logique ad hoc/trop spécifique : **limitée**, principalement dans la sanitation hardcodée.
- Couplage gênant futures phases : **modéré** via couplage mapper persistance -> DTO admin.
- Cohérence des noms/lisibilité : **bonne**.
- Tests réellement utiles : **oui**, avec quelques compléments simples utiles.
- Dette technique introduite : **maîtrisée**, principale dette = persistance synchrone et couplage de couche.

## 6. Résumé final
L’implémentation Phase 3 est globalement saine, respecte le périmètre, et atteint l’objectif principal : une persistance critique requêtable avec DB comme source de vérité admin. Les risques les plus évitables dès maintenant, sans changer le scope, sont : découpler le mapper persistance de la couche admin, rendre les seuils de sanitation configurables, et clarifier la responsabilité de gestion d’erreur des sinks. Ces ajustements réduisent significativement la dette de refactor des prochaines phases.
