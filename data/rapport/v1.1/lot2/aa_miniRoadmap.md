# Mini-roadmap — Phase 1.3 Étape 2
## Réduction progressive de `TechnicalAdminFacade`

## 1. Décisions d’architecture retenues

Les arbitrages retenus pour cette étape sont les suivants :

1. **`TechnicalAdminFacade` est conservée**
    - Elle reste le point d’entrée unique pour les contrôleurs existants.
    - Son rôle cible devient une **façade fine délégante**.
    - On évite ainsi un refactoring trop large et trop risqué à ce stade.

2. **`getOverview()` reste dans la façade pour le moment**
    - Cette méthode est transverse et agrège plusieurs sous-domaines.
    - L’extraire maintenant ajouterait de la complexité sans gain immédiat suffisant.
    - Elle sera éventuellement réévaluée plus tard, une fois les autres extractions stabilisées.

3. **Le mapping DTO reste dans chaque QueryService**
    - Choix pragmatique pour garder des lots simples et lisibles.
    - Cela évite d’introduire tout de suite une couche intermédiaire supplémentaire.
    - Le contrat DTO actuel doit rester inchangé.

4. **La logique `agents / runtime / policy / tools` reste dans le périmètre admin**
    - Elle sera isolée dans un `AdminAgentQueryService`.
    - On évite volontairement un refactoring métier plus profond à ce stade.

5. **La logique fragile autour de `"ERROR"` doit être corrigée**
    - Remplacer la comparaison string littérale par un mécanisme typé et robuste.
    - Ce point doit être traité avant ou pendant la sécurisation de `getOverview()`. :contentReference[oaicite:0]{index=0}

---

## 2. Objectif global de l’étape

Transformer `TechnicalAdminFacade` en **façade légère de composition**, en extrayant progressivement ses responsabilités de lecture par domaine, tout en :
- conservant les contrôleurs existants,
- gardant les DTO actuels,
- limitant les régressions,
- sécurisant d’abord les comportements existants par des tests ciblés.

Cette stratégie suit directement les constats issus de l’analyse Codex :
- façade trop large,
- mélange query / mapping / agrégation / logique technique,
- couplage fort à de nombreux services,
- tests incomplets sur les zones sensibles,
- `getOverview()` trop central mais encore utile comme point d’agrégation. :contentReference[oaicite:1]{index=1}

---

## 3. Découpage recommandé en mini-lots

## 3.1 Mini-lot 1 — Sécurisation avant extraction

### Objectif
Verrouiller les comportements actuels avant de commencer à déplacer du code.

### Travaux attendus
- Compléter les tests unitaires sur `listAgents()`
    - runtime présent
    - runtime absent
    - policy résolue sans runtime
    - outils exposés correctement
- Compléter les tests unitaires sur `getOverview()`
    - calcul de `busyAgents`
    - calcul de `recentErrors`
    - intégration des sections agrégées
- Ajouter des tests de contrat sur `listRecentArtifacts()`
    - comportement quand `taskId` est fourni
    - comportement quand `agentId` est fourni
    - comportement quand les deux sont fournis
- Corriger la logique fragile basée sur `"ERROR"`
    - remplacer la comparaison par un mécanisme typé ou explicitement robuste

### Résultat attendu
Le comportement existant est documenté par les tests avant refactoring.  
On réduit fortement le risque de casser l’admin technique en extrayant les services.

---

## 3.2 Mini-lot 2 — Extraire les queries à faible couplage

### Objectif
Sortir de la façade les domaines les plus simples et les moins risqués.

### Services à créer
- `AdminTaskQueryService`
- `TraceQueryService`
- `ArtifactQueryService`

### Méthodes concernées
- `listRecentTasks()` + `toTaskItem()`
- `listRecentTraces()` + `toTraceItem()`
- `listRecentArtifacts()` + `toArtifactItem()`

### Principes d’implémentation
- Les nouveaux services prennent en charge la lecture + mapping DTO
- `TechnicalAdminFacade` ne fait plus que déléguer
- Les signatures publiques de la façade restent inchangées
- Les contrôleurs ne changent pas

### Point d’attention
Le comportement de filtrage des artefacts doit rester identique tant qu’aucun nouveau choix n’est acté sur le contrat.

### Résultat attendu
Une première réduction nette de la façade, sans impact externe.

---

## 3.3 Mini-lot 3 — Extraire les queries de configuration et de rétention

### Objectif
Retirer de la façade les lectures “pures” qui ne portent quasiment pas de logique transverse.

### Services à créer
- `AdminConfigQueryService`
- `RetentionQueryService`

### Méthodes concernées
- `getConfigurationView()`
- `listRetentionPolicies()`
- mappings associés

### Principes d’implémentation
- conserver les DTO `TechnicalAdminView`
- garder exactement le même rendu fonctionnel
- éviter toute évolution de contrat pendant ce lot

### Résultat attendu
La façade cesse d’être responsable des lectures techniques simples de configuration et de rétention.

---

## 3.4 Mini-lot 4 — Extraire le bloc sensible `agents / runtime / policy / tools`

### Objectif
Isoler la partie la plus dense et la plus couplée de `TechnicalAdminFacade`.

### Service à créer
- `AdminAgentQueryService`

### Méthodes concernées
- `listAgents()`
- `toAgentItem()`
- `resolvePolicyWithoutRuntime()`
- `resolveExposedToolsFromPolicy()`
- `toRuntimeItem()`
- `toPolicyItem()`
- `normalize()`

### Principes d’implémentation
- rester strictement dans le périmètre admin
- ne pas refactorer le domaine runtime/policy en profondeur
- conserver la façade comme couche de délégation
- ne pas changer les DTO ni les endpoints

### Point d’attention
C’est le lot le plus sensible :
- nombreuses dépendances
- logique d’agrégation plus riche
- risque de dérive vers un refactoring métier trop large

### Résultat attendu
Le plus gros nœud de responsabilité est sorti de la façade.

---

## 3.5 Mini-lot 5 — Allègement final et nettoyage local

### Objectif
Finir la transformation de `TechnicalAdminFacade` en façade légère.

### Travaux attendus
- supprimer les helpers devenus inutiles (`isBlank`, éventuellement autres)
- vérifier que la façade ne contient plus que :
    - de la délégation
    - `getOverview()`
    - éventuellement un tout petit minimum de glue code temporaire
- nettoyer les imports et dépendances devenues obsolètes
- vérifier la lisibilité finale

### Résultat attendu
`TechnicalAdminFacade` est devenue une façade d’orchestration légère, cohérente avec son nom.

---

## 4. Règles de mise en œuvre pour Codex

Pour tous les mini-lots, il faudra respecter les règles suivantes :

- **Ne pas modifier les contrôleurs** tant que ce n’est pas nécessaire
- **Ne pas casser les contrats DTO** existants
- **Ne pas faire de refactoring métier profond**
- **Déplacer sans réinventer**
- **Conserver la compatibilité comportementale**
- **Ajouter ou adapter les tests à chaque lot**
- **Procéder par délégation d’abord, suppression ensuite**

---

## 5. Ordre d’exécution recommandé

Ordre conseillé :

1. **Mini-lot 1 — Sécurisation par tests + correction `ERROR`**
2. **Mini-lot 2 — Extraction tâches / traces / artefacts**
3. **Mini-lot 3 — Extraction configuration / rétention**
4. **Mini-lot 4 — Extraction agents / runtime / policy / tools**
5. **Mini-lot 5 — Nettoyage final de la façade**

Cet ordre est le plus sûr car il commence par :
- figer le comportement,
- extraire les zones simples,
- repousser le bloc le plus délicat à la fin.

---

## 6. Critère de sortie de l’étape

L’étape pourra être considérée comme terminée lorsque :

- `TechnicalAdminFacade` ne portera plus l’essentiel de la logique de lecture par domaine
- les domaines techniques seront répartis dans des services spécialisés
- les contrôleurs continueront à fonctionner sans changement majeur
- `getOverview()` restera stable et testé
- la comparaison fragile sur `"ERROR"` aura disparu
- les tests couvriront correctement les zones sensibles identifiées par l’analyse

---

## 7. Résumé court

Cette étape ne vise pas à supprimer brutalement `TechnicalAdminFacade`, mais à la **décharger progressivement**.  
La stratégie retenue est :

- garder la façade,
- garder `getOverview()` dedans pour l’instant,
- extraire d’abord les queries simples,
- isoler ensuite le bloc agents/policy/runtime/tools,
- sécuriser tout cela par des tests avant chaque déplacement.

C’est une approche prudente, compatible avec une implémentation par lots Codex, et alignée avec l’analyse réalisée. :contentReference[oaicite:2]{index=2}