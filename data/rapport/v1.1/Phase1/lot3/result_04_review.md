# Revue — Phase 4 — Rétention minimale

## 1. Faiblesses / points discutables (ordonnés par sévérité)

### [P1] Couplage configuration policy + scheduling dans la même classe
- **Référence**: `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionProperties.java:16-18,73-77`
- **Constat**: `PersistenceRetentionProperties` porte à la fois:
  - la politique de rétention (`defaultTtl`, `families`)
  - la mécanique runtime de planification (`cleanup.enabled`, `cleanup.cron`)
- **Risque architecture**: mélange “quoi retenir” (policy) et “quand exécuter” (runtime scheduling), ce qui brouille la séparation configuration métier/technique et peut gêner l’évolution future (ex: autre déclencheur que scheduler).

### [P2] Domaine de purge codé en dur dans le service
- **Référence**: `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionCleanupService.java:17-18`
- **Constat**: les domaines `critical` et `admin_snapshot` sont des littéraux dans la classe de runtime.
- **Risque architecture**: dette de maintenabilité (risque de divergence YAML/code), et logique partiellement ad hoc (la convention de domaine n’est pas encapsulée comme contrat explicite).

### [P2] Couverture de tests incomplète sur le scheduler
- **Référence**: absence de test dédié pour `PersistenceRetentionCleanupScheduler` (`src/main/java/.../PersistenceRetentionCleanupScheduler.java:7-25`)
- **Constat**: les tests valident le service et l’IT de purge, mais pas le câblage scheduler (`@ConditionalOnProperty`, appel du service, cron binding).
- **Risque**: régression silencieuse possible sur l’activation/désactivation par propriété.

### [P3] Double valeur par défaut du cron
- **Référence**:
  - `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionProperties.java:76`
  - `src/main/java/fr/ses10doigts/toolkitbridge/persistence/retention/PersistenceRetentionCleanupScheduler.java:22`
- **Constat**: la valeur par défaut est dupliquée dans deux endroits.
- **Risque**: incohérence future si un seul des deux defaults est modifié.

## 2. Corrections utiles proposées

### Correction A (prioritaire)
- **But**: restaurer une séparation claire configuration policy / runtime.
- **Action**: sortir `cleanup.enabled` et `cleanup.cron` de `PersistenceRetentionProperties` vers une classe dédiée (ex: `PersistenceRetentionCleanupProperties`) tout en gardant le même namespace `toolkit.persistence.retention.cleanup`.
- **Impact**: aucun changement fonctionnel, meilleure lisibilité et meilleure extensibilité.

### Correction B
- **But**: réduire l’ad hoc des domaines.
- **Action**: centraliser les domaines dans un petit contrat interne (constantes dédiées ou enum locale) partagé par le service et ses tests.
- **Impact**: évite les divergences de chaînes, améliore la robustesse sans élargir le périmètre.

### Correction C
- **But**: sécuriser le comportement du scheduler.
- **Action**: ajouter un test unitaire du scheduler pour vérifier:
  - appel `cleanupService.cleanup()` depuis `cleanupPersistenceData()`
  - activation conditionnelle par propriété (test Spring léger ou slice dédiée).
- **Impact**: couverture plus utile, pas de feature supplémentaire.

### Correction D
- **But**: supprimer la duplication de default cron.
- **Action**: source unique de défaut (idéalement dans la classe properties dédiée au cleanup), et utiliser cette source partout.
- **Impact**: dette évitable retirée immédiatement.

## 3. Vérifications demandées (synthèse)

- **Séparation configuration/runtime**: partiellement correcte, mais à renforcer (point P1).
- **Qualité du modèle**: bonne simplicité globale, service ciblé et sans sur-architecture.
- **Découplage orchestrator/mémoire/tooling/policy/workspace**: bon; aucune dépendance directe ajoutée vers ces sous-systèmes.
- **Absence de logique ad hoc**: globalement oui, sauf les domaines codés en dur (P2).
- **Absence de couplage bloquant futures phases**: plutôt oui, sous réserve de corriger P1/P2.
- **Cohérence des noms**: cohérente (`CleanupService`, `CleanupScheduler`, domaines explicites).
- **Lisibilité générale**: bonne (service court, règles explicites, logs utiles).
- **Tests utiles**: bons sur la logique centrale + IT réelle, mais manque le scheduler (P2).
- **Dette technique introduite**: faible à modérée (P1, P3 principalement).
- **Risques de refactor futur évitables maintenant**: oui, via les corrections A/B/D.

## 4. Résumé final

L’implémentation est solide sur le périmètre demandé: simple, opérationnelle, alignée sur `toolkit.persistence.retention`, et correctement découplée des flux métier (orchestrator/mémoire/tooling/policy/workspace).

Les principaux points à traiter sont structurels et encore peu coûteux:
1. séparer proprement properties de scheduling et properties de policy,
2. formaliser les domaines de purge pour éviter les chaînes “magiques”,
3. couvrir explicitement le scheduler en test,
4. supprimer la duplication de default cron.

Ces ajustements restent strictement dans le périmètre de la phase et réduisent la dette pour les phases suivantes.

