# v1.1 / Phase 1 / Étape 4 / Lot 1 — Analyse sécurité logs

## 1. Résumé rapide

État global: la base est correcte mais il existe des **fuites directes critiques** et plusieurs **fuites probables** liées à des logs de payload/preview.

Niveau de risque global: **ÉLEVÉ** (avec 2 points **CRITIQUES** immédiats).

---

## 2. Liste des fuites identifiées

### Fuite 1
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenService.java:53`
- Code concerné (résumé): log du token maître généré au startup
- Donnée exposée: **master token complet**
- Gravité: **CRITIQUE**
- Contexte: startup / bootstrap sécurité admin
- Explication: le secret principal d’accès admin est écrit en clair dans les logs.

### Fuite 2
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/service/auth/AgentAccountInitializer.java:51`
- Code concerné (résumé): log de création automatique de compte agent
- Donnée exposée: **API key agent complète**
- Gravité: **CRITIQUE**
- Contexte: startup / bootstrap comptes agents
- Explication: chaque clé nouvellement provisionnée est loggée en clair.

### Fuite 3
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/service/llm/openai/OpenAiLikeProvider.java:66`
- Code concerné (résumé): `log.debug("... {}", payload)`
- Donnée exposée: **payload LLM complet** (messages système/utilisateur, tool calls)
- Gravité: **ÉLEVÉ**
- Contexte: exécution LLM (debug)
- Explication: le payload peut contenir des secrets transmis dans les prompts ou arguments outils; log objet complet.

### Fuite 4
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeService.java:46`
- Code concerné (résumé): preview du message entrant Telegram
- Donnée exposée: contenu utilisateur partiel (160 chars)
- Gravité: **ÉLEVÉ**
- Contexte: runtime agent (debug)
- Explication: un utilisateur peut envoyer token/mot de passe/secret; il sera loggé partiellement.

### Fuite 5
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/runtime/AgentRuntimeService.java:84`
- Code concerné (résumé): preview de la réponse agent
- Donnée exposée: contenu réponse partiel (160 chars)
- Gravité: **ÉLEVÉ**
- Contexte: runtime agent (debug)
- Explication: si la réponse contient un secret (echo, dump, erreur), il est loggé.

### Fuite 6
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java:35`
- Code concerné (résumé): preview message Telegram
- Donnée exposée: contenu utilisateur partiel (160 chars)
- Gravité: **ÉLEVÉ**
- Contexte: entrée contrôleur Telegram (debug)
- Explication: duplication du risque de fuite de saisies sensibles.

### Fuite 7
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/controler/telegram/DefaultController.java:46`
- Code concerné (résumé): preview réponse Telegram
- Donnée exposée: contenu réponse partiel (160 chars)
- Gravité: **ÉLEVÉ**
- Contexte: sortie contrôleur Telegram (debug)
- Explication: la réponse peut contenir des informations sensibles.

### Fuite 8
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/service/ImplicitMemoryWritePipeline.java:48`
- Code concerné (résumé): log d’échec de persistance avec `candidate.getContent()`
- Donnée exposée: contenu mémoire implicite (texte brut)
- Gravité: **ÉLEVÉ**
- Contexte: pipeline mémoire (warn)
- Explication: le contenu mémoire peut contenir secrets copiés depuis interactions/outils.

### Fuite 9
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/memory/integration/service/ImplicitMemoryWritePipeline.java:67`
- Code concerné (résumé): log d’échec promotion règle avec `candidate.getContent()`
- Donnée exposée: contenu texte de règle promue
- Gravité: **ÉLEVÉ**
- Contexte: pipeline mémoire (warn)
- Explication: même risque que Fuite 8.

### Fuite 10
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/service/llm/DefaultLlmService.java:163`
- Code concerné (résumé): log args preview des tool calls
- Donnée exposée: arguments outils partiellement masqués
- Gravité: **MOYEN**
- Contexte: orchestration outils (info)
- Explication: masquage existant par nom de clé (`token`, `key`, `secret`, etc.), mais contournable si secret sous clé non reconnue (`input`, `query`, `text`, etc.).

### Fuite 11
- Fichiers:
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/llm/provider/ProviderHttpExecutor.java:33,40,47`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/ChatAgentOrchestrator.java:160`
  - `src/main/java/fr/ses10doigts/toolkitbridge/service/agent/orchestrator/impl/TaskAgentOrchestrator.java:199`
- Code concerné (résumé): exceptions enrichies avec body/message provider puis loggées avec stacktrace (`..., e`)
- Donnée exposée: potentiellement body d’erreur provider (trim 500 chars)
- Gravité: **MOYEN**
- Contexte: erreurs provider LLM
- Explication: fuite indirecte possible si l’upstream renvoie des fragments sensibles dans son erreur.

### Fuite 12
- Fichier: `src/main/java/fr/ses10doigts/toolkitbridge/exception/GlobalExceptionHandler.java:93`
- Code concerné (résumé): `log.error("Unexpected error: {}", ex.getMessage(), ex)`
- Donnée exposée: message d’exception potentiellement sensible
- Gravité: **MOYEN**
- Contexte: gestion d’erreurs globale
- Explication: dépend de la provenance du message; peut remonter une valeur sensible injectée dans le message d’exception.

---

## 3. Risques indirects

### Objets sensibles présents dans le modèle
- `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/OpenAiLikeProperties.java:6` (`apiKey`)
- `src/main/java/fr/ses10doigts/toolkitbridge/config/llm/LlmEndpointConfig.java:7` (`apiKey`)
- `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/auth/AgentProvisioningResult.java:5` (`apiKey`)
- `src/main/java/fr/ses10doigts/toolkitbridge/model/dto/admin/agent/AgentAdminCreateResponse.java:5` (`apiKey`)
- `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/AdminTokenService.java:31` (`masterToken`)

Risque: records Java exposent un `toString()` complet par défaut; si un de ces objets est loggé en entier plus tard, fuite immédiate.

### Risques liés à la verbosité
- `src/main/resources/application-template.yml:34` et `src/test/resources/application.yml:25` configurent `fr.ses10doigts.toolkitbridge: DEBUG`.
- En DEBUG, les logs preview/payload augmentent mécaniquement la surface de fuite.

### Risque “double exposition”
- même message sensible peut être loggé à deux niveaux:
  - `DefaultController` (preview)
  - `AgentRuntimeService` (preview)

---

## 4. Analyse des pratiques de logging

### Points positifs observés
- Masquage partiel déjà présent:
  - `DefaultLlmService.sanitizeArgValue(...)` masque certains noms de clés sensibles.
  - `LlmDebugStore.sanitize(...)` masque `sk-*`, bearer, token Telegram, lignes `api-key`.
  - `HumanInterventionEntityMapper` exclut clés contenant `token|secret|password`.
- Plusieurs logs sont de type métrique (durée, compte, ids techniques), donc non sensibles.

### Problèmes structurels
- Pas de politique unifiée de redaction pour tout le projet (implémentations locales hétérogènes).
- Masquage surtout basé sur **fragments de clé** (facile à contourner).
- Logs de contenu brut (payload/preview/content) encore présents dans zones critiques runtime/LLM/memory.
- Présence de logs de secrets en clair (2 cas critiques).

### Incohérences
- Certaines zones font de la redaction proactive (`LlmDebugStore`, `HumanInterventionEntityMapper`), d’autres loggent du texte brut.
- Le commentaire “never logged by value” existe dans `AdministrableConfigurationSeedService`, mais la règle n’est pas appliquée globalement.

---

## 5. Recommandations

### Stratégie simple de masquage
1. Introduire une règle commune “safe logging” (utility/service léger) pour:
- redacter tokens/api keys/passwords/authorization
- couper strictement la taille
- éviter les payloads complets

2. Appliquer cette règle à toute entrée utilisateur, sortie LLM, contenu mémoire, erreurs provider.

### Règles claires

Ce qui peut être loggé:
- IDs techniques (traceId, agentId, taskId)
- métriques (durée, count, status)
- états booléens (`apiKeyConfigured`)

Ce qui ne doit jamais être loggé en clair:
- token maître
- API keys (agent, LLM, Telegram)
- headers Authorization / bearer token
- mots de passe
- payloads complets contenant prompts/messages/contenus mémoire

### Zones à corriger en priorité
1. `AdminTokenService` (token maître)
2. `AgentAccountInitializer` (apiKey)
3. `OpenAiLikeProvider` (payload complet)
4. logs preview texte (`DefaultController`, `AgentRuntimeService`)
5. logs `content='{}'` (`ImplicitMemoryWritePipeline`)

---

## 6. Découpage du lot

### A. Corrections critiques immédiates
- retirer toute sortie de secret en clair:
  - `AdminTokenService:53`
  - `AgentAccountInitializer:51`
- remplacer par logs non sensibles (ex: fingerprint court, longueur, chemin).

### B. Corrections secondaires
- supprimer/remplacer logs de payload/preview/content:
  - `OpenAiLikeProvider:66`
  - `AgentRuntimeService:46,84`
  - `DefaultController:35,46`
  - `ImplicitMemoryWritePipeline:48,67`
- conserver uniquement métadonnées techniques + tailles.

### C. Améliorations optionnelles
- harmoniser redaction via utilitaire unique.
- compléter la détection de clés sensibles (`api_key`, `authorization`, `bearer`, `credential`, etc.).
- ajouter des tests dédiés “no secret in logs” sur les flux bootstrap + runtime.

---

## Synthèse opérationnelle

Le lot 1 doit d’abord fermer **2 fuites critiques directes**, puis éliminer les logs de contenu brut dans les flux runtime/LLM/memory. La base de masquage existe déjà par endroits, mais elle est fragmentée et insuffisante pour garantir l’absence de fuite de secrets.
