# v1.1 / Phase 1 / Etape 4 / Lot 2 - Implementation rate limiting login admin

## 1. Fichiers modifies

- `src/main/java/fr/ses10doigts/toolkitbridge/controler/web/LoginController.java`
- `src/main/java/fr/ses10doigts/toolkitbridge/security/admin/AdminLoginRateLimiterService.java` (nouveau)
- `src/test/java/fr/ses10doigts/toolkitbridge/controler/web/LoginControllerTest.java`
- `src/test/java/fr/ses10doigts/toolkitbridge/security/admin/AdminLoginRateLimiterServiceTest.java` (nouveau)

## 2. Strategie appliquee

Strategie appliquee strictement selon la consigne:

- integration dans `LoginController` uniquement pour `POST /login`
- service dedie, local, en memoire, thread-safe
- cle de limitation: `request.getRemoteAddr()` uniquement
- politique exacte:
  - 5 echecs
  - dans 10 minutes
  - blocage 15 minutes
- si succes login: reset immediat de l'etat IP
- si IP bloquee: rejet immediat avant `authenticate(...)`, redirection `/login`, message flash:
  - `Too many login attempts. Please retry in a few minutes.`

Aucun filtre HTTP, aucun interceptor, aucune dependance externe, aucune modification de `AdminAuthenticationService`.

## 3. Choix techniques

### 3.1 Service de rate limiting

`AdminLoginRateLimiterService`:

- stockage: `ConcurrentHashMap<String, AttemptState>`
- operations:
  - `isBlocked(ip)`
  - `recordFailure(ip)`
  - `reset(ip)`
- gestion temporelle:
  - fenetre de 10 min (basee sur la date du premier echec de fenetre)
  - blocage de 15 min a partir du 5e echec
- purge opportuniste:
  - execution sur les appels publics (`isBlocked`, `recordFailure`, `reset`)
  - suppression des etats non bloques et hors fenetre
- testabilite:
  - `Clock` injectable (constructeur package-private) pour tests deterministes

### 3.2 Integration controleur

Dans `LoginController.submitLogin(...)`:

1. lecture IP via `request.getRemoteAddr()`
2. pre-check `isBlocked(clientIp)`
3. si bloque: message flash rate-limit + `redirect:/login` (sans appel auth)
4. sinon tentative auth existante
5. si succes: `reset(clientIp)`
6. si echec: `recordFailure(clientIp)` + message existant `Invalid token.`

Le flow historique est conserve (redirects, session, message d'erreur d'auth).

## 4. Tests ajoutes / adaptes

### 4.1 Service (`AdminLoginRateLimiterServiceTest`)

Couverts:

- pas de blocage initial
- blocage apres 5 echecs dans la fenetre
- expiration du blocage (apres 15 min)
- reset sur succes (etat supprime)
- purge opportuniste des entrees expirees

### 4.2 Controleur (`LoginControllerTest`)

Couverts:

- login reussi inchange
- login echoue sous le seuil inchange
- IP bloquee => `redirect:/login` + message rate limiting
- blocage => `authenticate(...)` non appele
- succes => reset rate limiter appele

## 5. Verification execution

Commande executee:

```bash
.\mvnw.cmd -q "-Dtest=AdminLoginRateLimiterServiceTest,LoginControllerTest" test
```

Resultat: succes (tests passes).

## 6. Limites assumees (dans le perimetre du lot)

- limitation strictement par IP (`remoteAddr`), donc possible effet de bord en NAT/proxy partage
- etat en memoire uniquement (perdu au redemarrage)
- pas de message de temps restant (choix impose)
- pas d'extension a d'autres endpoints

## 7. Auto-review critique

### Risques restants

- en cas de tres fort volume d'IPs uniques, la map peut croitre transitoirement (attenue par purge opportuniste)
- la granularite par IP peut penaliser des utilisateurs legitimes derriere la meme IP

### Points limites

- la purge est opportuniste uniquement (pas de nettoyage periodique actif)
- la fenetre est basee sur le premier echec de sequence, pas sur une sliding window mathematiquement fine

### Ameliorations possibles plus tard (hors perimetre actuel)

- ajouter des metriques techniques (compteurs de blocages) sans information sensible
- externaliser la configuration des seuils/fenetres si besoin d'ajustement runtime
- evoluer vers une limitation plus riche si le besoin multi-noeuds apparait
