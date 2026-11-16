# Sporty-Bara — Documentation technique (Partie 1)

## 1. But de l'application

Sporty-Bara est une application Android de suivi de paris sportifs **sans argent réel** : la monnaie est virtuelle (Bara Coins), elle ne s'achète pas et ne se retire pas. L'application compare les cotes de trois bookmakers pour un même match et met en évidence la meilleure. Elle calcule une **cote de référence** sans la marge des bookmakers (moyenne des probabilités du marché, normalisée) et indique si une cote est trop basse, correcte ou trop haute (écart de ±5 %). Le thème est celui de la capybara (mascotte Bara) ; les ligues présentes sont La Liga et la Superliga.

Le prototype de la Partie 1 fonctionne avec des données simulées en mémoire. Les étapes suivantes ajouteront Firebase (authentification, Firestore), des fonctions Cloud Python, des cotes réelles et un assistant IA.

## 2. Choix technologiques

| Technologie | Rôle | Pourquoi |
|---|---|---|
| Android natif, Java (cible 11), vues XML | Application mobile | Langage et approche imposés par le cours et le cahier des charges |
| Material 3 (Material Components 1.10.0) | Composants et thème | Barre de navigation, chips, snackbar et dialogues standard, thème personnalisé |
| Police Nunito (4 graisses, incluse) | Typographie | Police arrondie et lisible, cohérente avec le thème |
| RecyclerView 1.3.2 | Liste des matchs | Recyclage des vues, adapté aux listes |
| minSdk 26 / targetSdk 36 | Compatibilité | Android 8.0 et plus, ciblage API 36 |
| Gradle (AGP 9.1.0) + catalogue `libs.versions.toml` | Build | Versions des dépendances centralisées dans un seul fichier |
| JUnit 4 | Tests unitaires | Teste la logique métier sans émulateur ; exécutés par la CI GitHub Actions |
| Firebase, Cloud Functions Python, API de cotes | Étapes suivantes | Données partagées et calculs côté serveur ; les clés API ne sont jamais dans l'application |

## 3. Structure du projet

```
Sporty-Bara/
├── app/src/main/java/ro/sportybara/app/
│   ├── MainActivity.java          navigation par 5 onglets
│   ├── data/                      modèle et logique métier, sans interface
│   │   ├── Match.java             cotes par bookmaker, meilleure cote, cote de référence, verdict
│   │   ├── Outcome.java           1 / X / 2
│   │   ├── Verdict.java           FAIR, LOW, HIGH
│   │   ├── Selection.java         match + issue + cote
│   │   ├── Ticket.java            bilet (sélections + mise)
│   │   ├── SportyStore.java       magasin en mémoire (singleton, listeners)
│   │   ├── FakeData.java          six matchs simulés
│   │   └── StoreException.java    erreurs métier
│   └── ui/
│       ├── home/                  HomeFragment (liste, filtre ligue), MatchAdapter
│       ├── match/                 MatchActivity (comparateur, verdict, ajout au bilet)
│       ├── PlaceholderFragment    écran provisoire des onglets
│       └── Odds.java              formatage des cotes
├── app/src/main/res/              layout/, values/ (couleurs, styles, thème), font/, drawable/, menu/
├── app/src/test/                  SportyStoreTest (9 tests JUnit)
├── functions/                     code Python serveur (modèle, collecte), pas encore relié à l'application
├── firestore.rules                règles Firestore (écriture interdite côté client)
└── docs/                          plans, design, documentation
```

Les données sont simulées en mémoire. `FakeData` fournit six matchs (trois de La Liga, trois de la Superliga), chacun avec les cotes de Superbet, Betano et Unibet. `SportyStore` est un singleton (`SportyStore.get()`) qui garde les matchs, le bilet, les tickets et le solde ; il n'utilise ni base de données ni réseau. Chaque modification notifie les écrans abonnés (interface `Listener`) : ils s'abonnent dans `onStart`, se désabonnent dans `onStop` et se rafraîchissent.

## 4. Écrans et navigation

- **Accueil** (onglet 1) : liste des matchs, filtre par ligue (Toutes, La Liga, Superliga), nombre de sélections sur le bilet. Un appui ouvre l'écran Match.
- **Match** (ouvert depuis Accueil, avec bouton retour) : tableau des cotes des trois bookmakers, meilleure cote en vert, ligne Sporty-Bara (cote de référence), verdict pour 1 / X / 2, boutons qui ajoutent la sélection au bilet. Un message propose l'action « Vezi biletul », qui ouvre l'onglet Bilete.
- **Bilete** (onglet 2, issue #33) : affiche les sélections du bilet et permet d'en retirer une, saisir la mise (10 à 500), voir la cote totale et le gain possible, et placer le bilet. Liste des tickets avec modification de la mise (dialogue) et suppression (la mise est remboursée). Les erreurs s'affichent à l'écran. Sans ticket, un écran vide est affiché.
- **Profil** (onglet 5, issue #34) : avatar, nom simulé, solde en Bara Coins mis à jour en direct, rang (« Pui de capibara ») et statistiques (nombre de tickets, total misé).

La barre du bas comporte cinq onglets : Acasă, Bilete, Roata, Clasament, Profil. Roata et Clasament restent des écrans provisoires dans ce prototype.

## 5. Fonctionnalités de base (CRUD simulé)

| Opération | Méthode | Écran |
|---|---|---|
| Ajout d'une sélection | `addToSlip(match, outcome)` | Match |
| Ajout d'un bilet | `placeTicket(stake)` | Bilete |
| Modification de la mise | `updateStake(ticketId, stake)` | Bilete |
| Suppression d'un bilet | `deleteTicket(ticketId)` | Bilete |
| Suppression d'une sélection | `removeFromSlip(match)` | Bilete |

Règles :

- solde initial de 1000 Bara Coins ; mise entre 10 et 500 ;
- la mise est déduite au placement, la modification ajuste la différence, la suppression rembourse la mise ;
- une seule sélection par match sur le bilet (un nouvel ajout remplace l'ancien) ;
- erreurs signalées par `StoreException` : `EMPTY_SLIP`, `INVALID_STAKE`, `INSUFFICIENT_FUNDS`, `NOT_FOUND`.

Tests : `SportyStoreTest` contient 9 tests JUnit (meilleure cote, marge, sélections, bilet, modification, suppression, erreurs, retrait de sélection, notification des listeners).
