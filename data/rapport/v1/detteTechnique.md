# Audit global de la dette technique — Tool-Bridge / Marcel

## 1. Résumé exécutif

Niveau global de dette technique observé: **modérée à élevée**.

Vision d’ensemble:
- Le projet a progressé vers une architecture plus explicite (runtime agent, policies, séparation chat/task, taxonomie tools, admin web structurée).
- La majorité des lots montrent des corrections pertinentes après relecture, ce qui est positif, mais révèle aussi une **instabilité de conception initiale** (couplages, responsabilités mouvantes, corrections de bord en chaîne).
- La dette principale n’est pas une “grosse erreur unique”, mais un **empilement de dettes d’intégration et d’industrialisation**: cohérence runtime/config, persistance incomplète, gouvernance sécurité partielle, robustesse opérationnelle.

Principaux risques:
- Risque de régressions fonctionnelles lors d’évolutions rapides (facades larges, flux transverses nombreux).
- Risque d’exploitation incomplet (rechargement runtime, observabilité partielle, stores en mémoire non durables).
- Risque sécurité modéré (MVP correct mais non durci: session/login, token maître, contrôles fins incomplets).

---

## 2. Liste des dettes techniques

### Architecture

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Composants “agrégateurs” trop larges (ex: `TechnicalAdminFacade`) | Complexité croissante, effets de bord lors des ajouts de features | Forte | Scinder en query services par domaine (`tasks`, `traces`, `artifacts`, `config`, `retention`) |
| Multiples corrections de couplage inter-couches dans les lots | Coût de maintenance élevé, dette de conception incrémentale | Moyenne | Formaliser des ADR + règles d’architecture (dépendances autorisées/interdites) |
| Dualité persistante “intention durable” vs persistance réelle (`DurableObject`) | Ambiguïté métier/infra, décisions d’implémentation floues | Moyenne | Clarifier le vocabulaire (`persistent-intent` vs `persisted`) + aligner les contrats |
| Typo/héritage structurel (`controler`, `agentIdent` vs `agentId`) | Incohérence API/code, friction onboarding | Faible | Plan de normalisation de nommage progressif (breaking changes maîtrisés) |

### Backend (Spring)

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Stores en mémoire pour éléments techniques clés (`tasks admin`, `human intervention`, bus) | Perte d’historique/redémarrage, supervision incomplète | Forte | Introduire persistance durable ciblée (au moins tasks/traces/human interventions) |
| Façades/services encore en read-modify-write global (config administrable) | Risque de lost updates en concurrence admin | Forte | Ajouter versioning optimiste/ETag + update partiel atomique |
| Hot-reload runtime incomplet pour config admin (LLM/agents) | Nécessité de redémarrage, divergence perçue UI/runtime | Moyenne | Stratégie explicite de reload (manuel versionné ou event-driven) |
| Validation complète souvent limitée à tests ciblés | Régressions possibles en intégration réelle | Moyenne | Renforcer tests d’intégration bout-en-bout sur parcours critiques |

### UI (Thymeleaf)

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Dette de cohérence visuelle résiduelle (`login.html` hors thème central) | Expérience hétérogène, coût d’évolution UI | Faible | Unifier tous les écrans sur le thème Marcel et fragments communs |
| Dépendance CDN (Bootstrap/jQuery/fonts) sans stratégie fallback/CSP | Risque déploiement offline/sécurité CSP | Moyenne | Prévoir stratégie d’assets internes + politique CSP |
| Certaines duplications UI (labels/routes/navigation) | Risque d’incohérence à mesure que le front grossit | Faible | Centraliser constantes UI/menu via modèle de navigation unique |
| Pagination/tri/détails encore limités côté pages techniques | Diagnostic moins efficace en production | Moyenne | Ajouter pagination curseur + liens croisés + vues détail |

### Sécurité

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Sécurité admin en mode MVP (token maître + session), sans durcissements avancés | Surface d’attaque raisonnablement contrôlée mais non industrialisée | Moyenne | Ajouter rate limiting login, rotation token, durcissement cookie (`Secure`, `SameSite=Strict`) |
| Gouvernance permissions encore partiellement coarse-grained | Contrôle insuffisamment fin par action/contexte | Forte | Étendre policy fine (tool/action/contexte/scope) avec audit décisionnel |
| Secrets gérés localement sans stratégie coffre forte explicitée | Risque opérationnel en environnements multiples | Moyenne | Intégrer secret manager (Vault/KMS) + politique de rotation |
| Couplages Telegram encore présents dans points d’entrée | Complexité d’évolution des règles d’accès canal | Faible à moyenne | Introduire policy d’accès Telegram inbound dédiée (au lieu de cas spéciaux controller) |

### API / Endpoints

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Historique d’incohérences REST (GET créateur supprimé tardivement, renommages progressifs) | Stabilité contrat API fragilisée sur la durée | Moyenne | Versionner les APIs admin et publier conventions strictes |
| Paramétrage/filtres techniques encore hétérogènes | Complexité côté client UI/API | Faible à moyenne | Uniformiser conventions de pagination/filtrage/tri |
| DTO techniques et DTO UI parfois proches | Risque de fuite de modèle interne | Moyenne | Maintenir séparation stricte view-model vs runtime DTO |

### Intégration globale (LLM / Telegram / Agents)

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Frontière “définitions agents” vs “accounts runtime” encore complexe | Confusion fonctionnelle (qui administre quoi) | Forte | Modéliser explicitement les deux workflows et leurs écrans/processus |
| Telegram admin encore read-only côté web | Intégration fonctionnelle incomplète | Moyenne | Créer source de config Telegram administrable persistée + cycle d’activation |
| Corrélation observabilité basée ThreadLocal (hypothèse synchrone) | Fragilité future si asynchrone/distribué | Moyenne | Préparer propagation explicite de contexte (MDC/correlation envelope) |
| Process externes pilotés par IDs en chaînes statiques | Risque de divergence et d’erreur de câblage | Faible à moyenne | Introduire registre typé des process externes |

### Maintenabilité / Qualité

| Problème | Impact | Gravité | Recommandation |
|---|---|---|---|
| Nombre élevé de corrections “après relecture” sur presque chaque lot | Charge cognitive et dette de stabilisation | Forte | Introduire checklists d’architecture en PR + revues croisées systématiques |
| Plusieurs zones restent intentionnellement non finalisées (persistance, purge, workflow humain, débat) | Empilement de “TODO structurants” | Moyenne | Planifier backlog de consolidation technique avant nouvelles features majeures |
| Validation build/test parfois bloquée (Java 21/env, erreurs préexistantes) | Fiabilité CI locale/équipe réduite | Forte | Verrouiller environnement build standardisé et pipeline de validation complet |

---

## 3. Top 10 des dettes techniques à corriger en priorité

1. Mettre en place un contrôle de concurrence sur la config administrable (éviter les lost updates).
2. Réduire `TechnicalAdminFacade` en services spécialisés pour limiter l’effet “god facade”.
3. Introduire une persistance durable minimale pour `admin tasks`, interventions humaines et événements critiques.
4. Finaliser la gouvernance fine des permissions (tool/action/scope/context) avec audit.
5. Standardiser la stratégie de rechargement runtime des configurations admin (LLM/agents).
6. Durcir la sécurité admin MVP (rate-limit login, rotation token, cookie policy stricte, secrets manager).
7. Clarifier et documenter la frontière fonctionnelle Agents definitions vs Agent accounts runtime.
8. Renforcer les tests d’intégration bout-en-bout sur les parcours admin/sécurité/observabilité.
9. Stabiliser l’observabilité pour futurs traitements asynchrones (propagation de corrélation hors ThreadLocal).
10. Industrialiser la couche Telegram admin (source persistée, activation cohérente, cycle de vie).

---

## 4. Dettes acceptables à court terme

- Stores en mémoire sur certaines fonctions de supervision, tant qu’un historique long terme n’est pas requis en production immédiate.
- Absence de workflow complet de validation humaine, si l’objectif actuel reste “préparation du socle”.
- Moteur de templating des process externes volontairement simple, si les templates restent peu complexes.
- Dette UI mineure (login hors thème, quelques duplications de navigation), si les fonctionnalités backend critiques restent prioritaires.
- Couverture de tests principalement ciblée par lots, à condition de planifier rapidement une passe d’intégration transversale.

---

## 5. Recommandations globales

- Lancer un **lot de consolidation technique** (avant nouvelles features majeures) focalisé sur concurrence config, persistance technique minimale, gouvernance permissions, et tests d’intégration.
- Mettre en place un **cadre d’architecture exécutable**: règles de dépendances, conventions DTO, conventions API, checklist PR “anti-couplage”.
- Stabiliser la **chaîne d’exploitation**: environnement Java/CI homogène, stratégie de reload runtime explicite, politique secrets centralisée.
- Structurer l’admin en deux axes explicites: **admin fonctionnelle** (définitions/config) et **admin technique** (observabilité/diagnostic), avec contrats séparés.
- Planifier une **passe de simplification** sur le vocabulaire et le nommage (durable/persisted, `agentId`, packages), pour réduire la charge cognitive à long terme.

---

## Notes de méthode

- Ce rapport s’appuie uniquement sur les contenus observés dans les fichiers `result*.txt` / `resultat.txt` sous `data/rapport/`.
- Les dettes listées distinguent les points déjà corrigés localement des dettes structurelles encore ouvertes à l’échelle globale.
