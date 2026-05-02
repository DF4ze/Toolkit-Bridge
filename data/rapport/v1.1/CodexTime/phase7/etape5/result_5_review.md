# Relecture Phase 7 Étape 5 — UX Telegram

## Résumé exécutif

L’implémentation reste globalement propre : la logique workflow n’a pas bougé, le rendu a été isolé dans un composant dédié, et `/workflow` + `/workflow_status` deviennent enfin des points d’entrée utiles.

Je ne vois pas de dérive d’architecture majeure.

En revanche, il reste **2 points discutables**, dont **1 bug UX réel** à corriger avant validation définitive.

---

## Points faibles identifiés

### 1. Message trompeur quand le summary est trop long
**Fichier :** `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramSummaryService.java`

Quand le summary dépasse la limite, le service retourne toujours :

```text
Summary too long
Sending complete file
```

alors que l’envoi du document n’est effectué **que si** un `TelegramSender` est disponible.

Code concerné : bloc `split.tooLong()`.

Conséquence :
- si `telegramEnabled=false`
- ou si aucun sender n’est résolu

le bot affirme qu’il envoie le fichier alors qu’il ne l’envoie pas.

➡️ **C’est un vrai défaut UX/comportemental**, pas seulement cosmétique.

**Correction utile :**
- soit adapter le message quand aucun sender n’est disponible ;
- soit retourner un message neutre du type `Summary too long` si l’envoi du document n’a pas réellement eu lieu.

---

### 2. `/workflow` et `/workflow_status` exposent des chemins internes inutilement
**Fichier :** `src/main/java/fr/ses10doigts/toolkitbridge/service/telegram/workflow/WorkflowTelegramMessageRenderer.java`

La méthode `pathState(...)` retourne :

```text
loaded (<chemin complet>)
```

Ce rendu est ensuite utilisé pour :
- `Roadmap:`
- `Summary:`

Le prompt d’étape demandait seulement :
- roadmap `loaded/missing`
- summary `available/missing`

Conséquence :
- bruit inutile dans les messages Telegram ;
- fuite de détails d’implémentation filesystem ;
- couplage UX ↔ layout disque plus fort que nécessaire.

➡️ Ce n’est pas bloquant, mais c’est une **petite dette de design** évitable maintenant.

**Correction utile :**
- faire retourner un état simple (`loaded` / `missing`, ou `available` / `missing`) ;
- garder les paths pour le debug interne, pas pour l’UX standard.

---

## Ce qui est bon

- séparation correcte entre orchestration runtime et rendu des messages ;
- pas de modification du runner ni de la CLI ;
- logique de recommandation simple, lisible, et sans automatisme dangereux ;
- pas de sur-abstraction inutile ;
- tests utiles sur les états `IDLE`, `RUNNING`, `WAITING_HUMAN`, `FAILED`.

---

## Corrections recommandées

### À faire maintenant
1. Corriger le message `Summary too long` pour qu’il reflète l’envoi réel du document.
2. Simplifier le rendu `Roadmap` / `Summary` dans le renderer pour ne plus exposer les chemins complets.

### À ne pas faire maintenant
- pas de refactor supplémentaire ;
- pas d’alias de commandes ;
- pas de boutons inline ;
- pas de couche de templating.

---

## Conclusion

**Verdict : partiellement validé**

Le lot est bon sur l’architecture générale et reste dans le périmètre.

Mais je ne mettrais pas encore une validation finale tant que :
- le faux positif `Sending complete file` n’est pas corrigé ;
- le rendu des paths n’est pas simplifié.

Après ces deux ajustements, l’étape pourra être considérée comme proprement stabilisée.
