# v1.1 / Phase 1 / Etape 4 / Lot 2 - Correction ciblee

## 1. Test modifie

Fichier modifie:
- `src/test/java/fr/ses10doigts/toolkitbridge/security/admin/AdminLoginRateLimiterServiceTest.java`

Changement applique:
- suppression du test base sur reflection (`opportunisticPurgeRemovesExpiredEntries`)
- ajout d'un test purement comportemental: `expiredStateBehavesLikeFreshFirstAttempt`

## 2. Approche comportementale retenue

Scenario teste (sans introspection interne):
1. enregistrer un premier echec pour une IP
2. faire avancer l'horloge au-dela de la fenetre (11 minutes)
3. enregistrer 4 nouveaux echecs
4. verifier que l'IP n'est pas bloquee (comportement de "premiere tentative" redemarree)
5. enregistrer le 5e echec
6. verifier que l'IP devient bloquee

Ce test valide de maniere observable qu'un etat ancien expire ne perturbe pas le nouveau cycle de comptage, sans lire la map interne.

## 3. Validation executee

Commande executee:

```bash
.\mvnw.cmd -q "-Dtest=AdminLoginRateLimiterServiceTest,LoginControllerTest" test
```

Resultat:
- succes, tests du lot 2 passes.

## 4. Auto-review rapide

- Perimetre respecte strictement: uniquement le test cible a ete ajuste.
- Aucun changement sur `AdminLoginRateLimiterService` ni sur la politique `5 echecs / 10 min / blocage 15 min`.
- Point mineur assume: le test reste oriente comportement global (expire -> nouveau cycle) et ne prouve pas l'operation interne de purge en tant que detail d'implementation, ce qui est volontaire pour eviter un couplage fragile.
