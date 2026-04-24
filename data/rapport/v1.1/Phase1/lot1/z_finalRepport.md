# 📌 Rapport des points postponés – Stabilisation & Reload

Ce document synthétise **tous les points identifiés pendant les phases Codex** mais volontairement **reportés (postponed)** pour ne pas ralentir l’avancement.

Objectif :
- garder une trace claire
- éviter de perdre ces améliorations
- pouvoir y revenir facilement plus tard

---

# 🧠 1. Modèle Reload – Améliorations structurelles

## 1.1 Gestion du “SUCCESS dégradé” (warning)
**Statut : POSTPONED**

### Problème
- actuellement :
    - `SUCCESS` même si `invalidateCache()` échoue
    - warning uniquement dans le `message`
- pas structuré → difficilement exploitable (monitoring, UI, logs automatisés)

### Amélioration envisagée
- enrichir `ReloadDomainResult` avec :
    - `warnings` (liste)
    - ou un statut intermédiaire (`SUCCESS_WITH_WARNING`)

### Pourquoi reporté
- impact transversal (memory + LLM + futurs domaines)
- nécessite évolution du contrat global reload

---

## 1.2 Uniformisation des erreurs de reload
**Statut : POSTPONED**

### Problème
- `ReloadErrorType` minimaliste
- pas de distinction claire :
    - validation
    - build
    - post-processing (ex: cache)

### Amélioration
- enrichir les types d’erreurs
- éventuellement catégoriser :
    - `BUILD_FAILED`
    - `VALIDATION_FAILED`
    - `POST_PROCESS_FAILED`

---

## 1.3 Standardisation des messages reload
**Statut : POSTPONED**

### Problème
- messages libres (string)
- parsing difficile côté UI / logs

### Amélioration
- convention stable (ex: `CACHE_INVALIDATION_FAILED:`)
- ou structuration complète (cf. warnings)

---

# 🔁 2. Runtime vs DB – Clarté fonctionnelle

## 2.1 Divergence DB vs Runtime non exposée
**Statut : POSTPONED**

### Problème
- l’admin lit la DB
- le runtime utilise une version snapshot
- aucune distinction visible

### Risque
- confusion utilisateur :
    - “j’ai modifié → pourquoi ça ne change pas ?”

### Amélioration
- exposer :
    - config DB
    - config runtime active
    - état de synchronisation

---

## 2.2 Absence de notion “pending reload”
**Statut : POSTPONED**

### Problème
- aucune indication qu’un changement DB nécessite un reload

### Amélioration
- flag ou état :
    - `DIRTY`
    - `REQUIRES_RELOAD`

---

# ⚙️ 3. Lifecycle & Initialisation

## 3.1 Dépendance au cycle `ApplicationRunner`
**Statut : PARTIELLEMENT TRAITÉ / À SURVEILLER**

### Fait
- introduction de `StartupOrder`

### Reste à améliorer
- dépendance implicite toujours fragile
- pas de vérification runtime du bon ordre

### Amélioration possible
- assertions au démarrage
- ou mécanisme plus explicite (mais attention au sur-design)

---

# 🧪 4. Tests & Validation

## 4.1 Tests encore partiellement couplés aux messages
**Statut : PARTIELLEMENT TRAITÉ**

### Reste
- quelques assertions textuelles

### Amélioration
- tester uniquement :
    - status
    - effets
    - invariants

---

## 4.2 Manque de tests d’intégration transverses
**Statut : PARTIELLEMENT TRAITÉ**

### Fait
- IT reload LLM ajouté

### Reste
- tests globaux multi-domaines (memory + LLM)
- test de séquence reload multiple

---

# 🧩 5. Memory – Points non exploités

## 5.1 Champs `integration.*` non utilisés
**Statut : POSTPONED**

### Problème
- présents en DB
- ignorés runtime

### Risque
- confusion admin

### Amélioration
- soit les utiliser
- soit les masquer
- soit les documenter clairement

---

## 5.2 `@ConfigurationProperties` mémoire encore présents
**Statut : POSTPONED**

### Problème
- encore dans le code runtime (même si plus utilisés)

### Risque
- réactivation accidentelle du YAML runtime

### Amélioration
- déplacer vers package `seed`
- ou supprimer à terme

---

# 🔐 6. Concurrence & Robustesse

## 6.1 Atomicité OK mais non vérifiée globalement
**Statut : ACCEPTÉ**

### Situation actuelle
- `volatile` + swap atomique
- correct

### Amélioration possible
- tests de concurrence plus poussés

---

## 6.2 Invalidation cache non garantie
**Statut : ACCEPTÉ (best effort)**

### Situation actuelle
- cache invalidation = best effort

### Risque
- incohérence temporaire

### Amélioration
- coupler cache avec version config
- ou rendre cache plus robuste

---

# 🧱 7. LLM – Points à améliorer

## 7.1 Validation LLM minimale
**Statut : ACCEPTÉ**

### Actuel
- validation structurelle uniquement

### Amélioration possible
- validation métier plus poussée (optionnelle)

---

## 7.2 Absence de test de fallback “registry vide”
**Statut : POSTPONED**

### Amélioration
- test explicite :
    - runtime sans provider
    - comportement métier

---

# 🧠 8. Architecture globale

## 8.1 Séparation config/runtime excellente mais non exposée
**Statut : POSTPONED**

### Problème
- très propre techniquement
- invisible fonctionnellement

### Amélioration
- UI / admin alignée sur cette séparation

---

## 8.2 Pas de modèle générique multi-domain reload enrichi
**Statut : POSTPONED**

### Problème
- modèle simple aujourd’hui

### Amélioration future
- enrichir si besoin réel (warning, stats, etc.)

---

# 🚀 9. Prochaines extensions naturelles (non faites)

## 9.1 Reload Agents
**À FAIRE**
- agents runtime reloadables
- cohérence avec LLM & memory

## 9.2 Reload Telegram Bots
**À FAIRE**
- rechargement des bots
- routing dynamique

## 9.3 UI Admin Reload
**À FAIRE**
- bouton reload
- feedback utilisateur
- état runtime vs DB

---

# ✅ Conclusion

Le système actuel est :

✔ Stable  
✔ Cohérent  
✔ Sans fallback YAML runtime  
✔ Avec reload explicite fonctionnel  
✔ Découplé proprement

Les points postponés sont :

👉 principalement **de la maturité produit et d’observabilité**,  
pas des problèmes structurels.

---

# 📌 Recommandation

Quand tu reprendras :

1. **Ne pas tout faire d’un coup**
2. Prioriser :
    - Agents reload
    - UI visibilité runtime vs DB
3. Ensuite seulement :
    - enrichissement du modèle reload (warnings, etc.)

---

Tu peux repartir sereinement sur une nouvelle conversation 👍