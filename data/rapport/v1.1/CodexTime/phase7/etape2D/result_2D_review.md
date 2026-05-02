# Phase 7 — Étape 2D — Relecture & revue critique d’architecture

Périmètre relu : implémentation `/workflow_summary` (commande + service + lecture workspace + tests) — sans modification runner/CLI.

## 1) Points forts (ce qui est bien verrouillé)

- **Séparation controller / service** : `WorkflowTelegramController` délègue entièrement à `WorkflowTelegramSummaryService`.
- **Pas de parsing du summary** : affichage en texte brut, contenu inchangé.
- **Anti-limite Telegram** : `MAX_TELEGRAM_TEXT=3800` + split max 2 chunks + fallback document.
- **Prévention du spam** : au-delà de 2 chunks, bascule en document au lieu d’émettre N messages.
- **Chemin source unique** : le path vient uniquement de `WorkflowTelegramSession.lastSummaryPath()`.
- **Tests utiles** : cas “absent / introuvable / vide / court / moyen / long” couverts, avec vérification des appels `sendMessage`/`sendDocument`.

## 2) Points discutables / faiblesses

### 2.1 “Workspace/tool sécurisé” : implémentation vs intention

- `WorkflowTelegramSummaryService` n’utilise pas `Files.*` directement (bien).
- La lecture et les checks existent via `WorkspaceTextFileService`, qui encapsule `Files.*`.

Discussion :
- C’est conforme à l’intention “ne pas accéder directement au filesystem depuis le service métier Telegram”.
- Mais cela **n’est pas** une réutilisation d’un “tool handler” existant ; c’est un nouveau service d’infra.

➡️ À noter comme dette légère : nomenclature/placement “workspace” peut laisser penser que *tout* accès disque doit passer par là (ce qui est plutôt bien), mais il faut éviter de multiplier les services similaires (ReadFileToolHandler / autres).

### 2.2 Sécurité de path : validation OK mais dépend d’un choix de root

- La validation se fait via `WorkspaceLayout.resolveWithinRoot(reportRootDirectory, relativeText, ...)`.
- Le root est `toolkit.telegram.workflow.reportRootDirectory` (default `data/rapport`) converti en absolu.

Points d’attention :
- Le runner produit un `workflowSummaryPath` (souvent sous `data/rapport/...`). Si demain le runner écrit ailleurs (ou si un run cible un autre dossier), l’accès sera refusé.
- C’est acceptable et même souhaitable si `reportRootDirectory` est bien la zone autorisée (contrat à garder clair).

### 2.3 Ordre d’envoi “warning puis document”

Exigence UX demandée :
1) envoyer message court “Summary trop long”
2) envoyer document

Implémentation actuelle :
- `sendDocument(...)` est déclenché **dans** le service avant le retour du message texte (qui sera envoyé ensuite par la mécanique `@Command`).

Risque :
- selon l’implémentation du framework Telegram, le document peut arriver avant le texte (ordre non garanti).

### 2.4 Dégradation quand `TelegramSenderRegistry` indisponible

Cas “contenu moyen” (2 chunks) :
- si le `TelegramSender` est résolu : OK (1er chunk en retour + 2ème chunk via `sendMessage`).
- sinon : fallback en 1 message (troncation implicite via `MAX_TELEGRAM_TEXT`).

Risque :
- perte du 2e chunk dans ce scénario (rare en prod si Telegram activé, mais possible en test ou config partielle).

### 2.5 Cohérence du modèle / responsabilités

- `WorkflowTelegramSummaryService` contient à la fois :
  - résolution/validation de path
  - lecture du fichier (via service)
  - split + format des messages
  - logique d’envoi du “message 2/2” et du document

Ce n’est pas “mauvais” ici (périmètre petit), mais c’est un point à surveiller si d’autres commandes “affichage d’artefacts” arrivent : risque de duplication (split, format, doc fallback).

## 3) Corrections utiles (sans nouvelles fonctionnalités)

### Correction A — Garantir l’ordre “warning puis document”

Option recommandée (minimaliste) :
- si `TelegramSender` est disponible :
  1) envoyer le warning via `sender.sendMessage(chatId, ...)`
  2) envoyer le document via `sender.sendDocument(...)`
  3) retourner une chaîne vide (ou un message neutre) pour éviter un second warning par le `@Command`

Si le framework n’accepte pas un “message vide” :
- retourner `null` si la lib le supporte, sinon retourner un texte court et accepter le doublon.

### Correction B — Centraliser la stratégie “split + format”

Si d’autres artefacts doivent être affichés ensuite :
- extraire un petit composant interne (package-private) de type `TelegramTextChunker` (sans dépendances Spring) testé unitairement.

Ce n’est pas nécessaire immédiatement, mais ça évite la duplication future.

### Correction C — Clarifier le contrat `reportRootDirectory`

Documenter (dans `WorkflowTelegramSummaryService` ou dans une doc de config) que :
- seuls les summary sous `toolkit.telegram.workflow.reportRootDirectory` sont autorisés
- si le runner change de root, il faut changer cette propriété

## 4) Dette technique introduite (mineure)

- Un nouveau service d’infra (`WorkspaceTextFileService`) qui peut devenir un “fourre-tout” si on n’encadre pas ses usages.

Recommandation :
- garder ce service minimal (texte UTF‑8 + limite taille), et ajouter d’autres capacités sous forme de services dédiés si nécessaire (ex: binaire/document).

## 5) Résumé final

- Implémentation globalement propre : séparation controller/service, pas de parsing, taille gérée, tests pertinents, et pas d’impact runner/CLI.
- Deux points UX/systèmes restent perfectibles sans changer le scope :
  - ordre d’envoi “warning puis document” non garanti
  - fallback “2 chunks” si sender indisponible (perte d’info)

