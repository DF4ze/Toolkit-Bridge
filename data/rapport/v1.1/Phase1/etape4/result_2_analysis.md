# v1.1 / Phase 1 / Etape 4 / Lot 2 - Analyse rate limiting login admin

## 1. Resume rapide

Faisabilite: **elevee** avec une implementation locale simple en memoire, sans dependance externe.

Strategie recommandee:
- point d'insertion principal dans `LoginController` (pre-check + post-record)
- service dedie leger de rate limiting en memoire (thread-safe), focalise sur `POST /login`
- cle par IP normalisee
- politique simple: **5 echecs en 10 minutes => blocage 15 minutes**
- message UI coherent (flash error), sans casser le flow actuel `redirect:/login`

Cette approche est la plus alignee avec l'existant (controleur web + session) et minimise le risque d'impact lateral.

---

## 2. Analyse du flux existant

Flux actuel de login admin:

1. `GET /login` arrive dans `LoginController.login(...)`.
- si session deja authentifiee (`adminAuthenticationService.isAuthenticated(request)`), redirection vers `/admin`
- sinon rendu de `login.html`

2. `POST /login` arrive dans `LoginController.submitLogin(...)`.
- lit `token` depuis le formulaire
- appelle `adminAuthenticationService.authenticate(request, token)`

3. `AdminAuthenticationService.authenticate(...)`
- verifie le token via `AdminTokenService.matches(token)`
- en cas succes: cree une session serveur et pose:
  - `toolkit.admin.authenticated = true`
  - `toolkit.admin.authenticatedAt = Instant.now()`
- en cas echec: retourne `false` sans creer de session

4. Retour controleur:
- succes: `redirect:/admin`
- echec: flash attribute `error = "Invalid token."` puis `redirect:/login`

Gestion d'erreur actuelle:
- pas d'exception metier dediee pour le login invalide
- tout repose sur le booleen de retour
- aucun code HTTP specifique (flow purement web/redirect)

Moment d'interception disponible:
- juste avant l'appel a `authenticate(...)` (pre-controle blocage)
- juste apres le resultat d'authentification (increment/reset compteur)

---

## 3. Comparaison des points d'insertion

### Option A - Insertion dans `LoginController` (recommandee)

Avantages:
- point exact du use-case `POST /login`
- tres lisible fonctionnellement (pre-check puis update selon succes/echec)
- controle facile des messages UI via `RedirectAttributes`
- impact limite au perimetre admin login
- tests unitaires simples a etendre (`LoginControllerTest`)

Inconvenients:
- logique de securite partiellement au niveau controleur (et pas 100% service)
- si un second endpoint login est ajoute un jour, il faudra penser a le cabler

Impact existant:
- faible, localise
- aucun changement du mecanisme de session ou du filtre d'auth admin

### Option B - Insertion dans `AdminAuthenticationService`

Avantages:
- centralise "auth + garde-fou" dans un service unique
- reutilisable si d'autres entrees appellent `authenticate(...)`

Inconvenients:
- service moins "pur" (melange verification token et politique anti-abus liee a la requete/IP)
- gestion UX du message de blocage plus indirecte (le service ne gere pas le flash UI)
- necessite enrichir le contrat (statuts detailles plutot que booleen)

Impact existant:
- moyen (signature/contrat potentiellement a faire evoluer)
- retouches de tests de service et controleur

### Option C - Filtre HTTP dedie rate limit (`/login`)

Avantages:
- separation cross-cutting propre dans la chaine HTTP
- blocage tres tot dans le pipeline

Inconvenients:
- surcout de complexite pour un seul endpoint
- gestion des flash messages UI plus compliquee (le filtre ne manipule pas naturellement `RedirectAttributes`)
- risque de comportements subtils (ordre de filtres, redirect/response deja commit)

Impact existant:
- moyen a eleve relativement au besoin
- augmente la complexite operationnelle sans gain proportionne pour ce lot

### Option D - Interceptor MVC

Avantages:
- intermediaire entre controleur et filtre

Inconvenients:
- ajoute un mecanisme de plus a maintenir
- benefice limite vs option controleur pour un endpoint unique

Impact existant:
- moyen, non necessaire a ce stade

Conclusion insertion:
- **choix recommande: Option A (`LoginController`) + service de support dedie.**

---

## 4. Strategie de rate limiting retenue

### 4.1 Cle

Cle principale: IP client normalisee.

Regle pratique:
- si header proxy fiable est explicitement gere cote infra, utiliser premiere IP pertinente
- sinon fallback strict sur `request.getRemoteAddr()`

Note: sans politique proxy claire, il faut eviter de faire confiance aveuglement a `X-Forwarded-For` (contournable).

### 4.2 Stockage memoire

Structure en memoire thread-safe:
- map `ip -> etat` (compteur d'echecs dans fenetre + timestamp de blocage eventuel)
- acces concurrent sur (ex: `ConcurrentHashMap` + operations atomiques)

### 4.3 Politique proposee

- fenetre d'observation: **10 minutes**
- seuil: **5 echecs**
- blocage: **15 minutes**
- succes login: reset immediat de l'etat IP

Politique simple, comprehensible et suffisante pour un MVP robuste.

### 4.4 Hygiene memoire

Pour eviter la fuite memoire:
- purge opportuniste des entrees expirees a chaque tentative (cout faible car endpoint peu frequent)
- suppression des entrees "propres" au reset succes

Pas besoin de scheduler dedie dans ce lot.

---

## 5. Comportement detaille

### a) Cas nominal (login OK)

1. Pre-check rate limit: IP non bloquee
2. Authentification token reussie
3. Session admin creee comme aujourd'hui
4. Etat rate-limit IP reinitialise
5. Redirection `/admin`

### b) Echec login

1. Pre-check rate limit: IP non bloquee
2. Authentification echoue
3. Compteur IP incremente dans la fenetre
4. Si seuil non atteint: `redirect:/login` avec message actuel ou message equivalent

### c) Depassement seuil

1. A l'echec qui depasse le seuil: IP passe en etat bloque jusqu'a `blockedUntil`
2. Tentatives suivantes pendant blocage: rejet immediat sans verifier le token
3. Reponse UI: `redirect:/login` avec message explicite de temporisation

Message recommande UI:
- `Too many login attempts. Please retry in a few minutes.`

Option plus informative (si souhaite):
- indiquer un temps restant arrondi en minutes

### d) Reset

Reset automatique:
- sur login reussi (securite + UX)
- apres expiration du blocage/fenetre via purge opportuniste

Reset non requis:
- pas de reset au simple passage de `GET /login`

### 5.1 Format de reponse UI/API

UI `/login`:
- rester sur le pattern actuel `redirect + flash error`
- ne pas exposer d'info sensible (pas de detail sur token, ni regle complete)

API:
- aujourd'hui, pas d'endpoint API de login admin dedie
- donc aucun changement API obligatoire dans ce lot
- si un endpoint API login apparait plus tard: prevoir `429 Too Many Requests` avec payload standardise

---

## 6. Impacts techniques

### Session

- aucun changement du mecanisme de session existant
- la session n'est creee qu'apres authentification reussie (inchange)
- rejet en blocage n'ouvre pas de session

### Logs

Recommande:
- log `WARN` lors d'un blocage active (IP masquee/partielle si possible)
- log `DEBUG`/`INFO` leger pour tentative refusee par blocage

A eviter:
- logger token saisi, valeur brute de header auth, ou details excessifs exploitables

### Tests

Tests a prevoir (analyse de perimetre):
- `LoginControllerTest`:
  - blocage actif => redirect login + message rate-limit
  - echec sous seuil => increment sans blocage
  - succes => reset
- tests unitaires service de rate limit:
  - seuil/fenetre/blocage
  - expiration blocage
  - purge memoire
  - concurrence basique

### Architecture

Impact global faible:
- ajout d'un composant technique localise
- pas de dependance externe
- pas de refactor transversal
- coherent avec l'architecture Spring actuelle

---

## 7. Risques et pieges

1. Stockage non thread-safe
- risque d'incoherence de compteurs sous charge concurrente
- mitigation: map/thread-safety + update atomique

2. Fuite memoire
- risque si la map grossit sans nettoyage
- mitigation: purge opportuniste systematique + suppression au reset

3. Blocage global au lieu de par IP
- risque de deni de service involontaire
- mitigation: cle strictement scoped par IP

4. Confiance aveugle dans `X-Forwarded-For`
- risque de contournement par spoofing
- mitigation: fallback `remoteAddr` tant que la chaine proxy de confiance n'est pas explicitee

5. Effet de bord UX
- message trop verbeux ou trop technique
- mitigation: message simple, coherent avec l'existant

6. Sur-complexite
- filtre/interceptor/scheduler inutile pour ce lot
- mitigation: garder une implementation locale et minimale

7. Faux positifs en environnement NAT/proxy
- plusieurs utilisateurs peuvent partager une IP
- acceptable ici car usage admin restreint; a reevaluer si contexte multi-utilisateurs elargi

---

## 8. Decoupage du lot

### Sous-etape 1 - Coeur rate limiting (service en memoire)
- definir le modele d'etat par IP
- implementer politique `5/10min -> 15min`
- inclure logique purge/reset

### Sous-etape 2 - Integration login
- brancher pre-check blocage dans `POST /login`
- brancher enregistrement resultat succes/echec
- conserver flow redirect + flash attributes

### Sous-etape 3 - Tests
- completer tests controleur
- ajouter tests unitaires dedies au service rate limit
- verifier non-regression du flow actuel (succes/echec/logout)

### Sous-etape 4 - Verification operationnelle legere
- valider messages UI
- valider absence d'impact sur `/admin/**` et `AdminAuthFilter`

---

## Conclusion

Le lot est **simple a integrer sans refonte**.
La meilleure trajectoire est un rate limiting local en memoire, **insere dans `LoginController`**, avec une politique claire `5 echecs/10 min => blocage 15 min`, message UI sobre, reset sur succes, et tests cibles.
