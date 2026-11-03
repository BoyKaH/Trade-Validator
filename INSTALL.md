# Installation de Pre-Trade Validator

## 1. Fichiers nécessaires

Garde ces sept fichiers dans le même dossier du serveur local :

```text
index.html
pre_trade_validator.html
manifest.json
sw.js
icon-192.png
icon-512.png
apple-touch-icon.png
```

`index.html` est l’application. `pre_trade_validator.html` conserve la compatibilité avec les installations plus anciennes et redirige vers l’adresse courte. Les fichiers `manifest.json`, `sw.js` et les icônes doivent rester dans le même dossier.

L’application ne nécessite ni installation de dépendance, ni compte, ni connexion Internet.

## 2. Héberger l’application sur le NAS

1. Copie les sept fichiers dans un même dossier du serveur web du NAS.
2. Publie ce dossier comme site statique : aucun PHP, Node.js ou base de données n’est nécessaire.
3. Ouvre l’adresse du dossier, par exemple `https://ton-domaine.fr/trade-validator/`.

Pour y accéder depuis l’extérieur et profiter de l’installation PWA, utilise **HTTPS**. Évite d’exposer directement l’interface d’administration du NAS sur Internet : passe par le serveur web/reverse proxy prévu par ton NAS et son certificat HTTPS.

## 3. Lancer l’application sur le téléphone

1. Copie les sept fichiers dans le dossier servi par ton serveur local.
2. Démarre le serveur depuis ce dossier.
3. Ouvre l’adresse affichée par le serveur dans Edge ou Chrome.

Pour activer correctement l’installation et le mode hors ligne, utilise de préférence :

```text
http://127.0.0.1:PORT/
```

ou :

```text
http://localhost:PORT/
```

Remplace `PORT` par le numéro indiqué par ton serveur.

Ne lance pas l’application en ouvrant directement le fichier (`file:///...`). Le service worker et l’installation PWA ne fonctionneront pas correctement dans ce mode.

## 4. Installer sur l’écran d’accueil — Android / Edge / Chrome

1. Ouvre l’application avec le serveur actif.
2. Ouvre `⚙ Configuration`.
3. Appuie sur `Installer sur l’écran d’accueil`.
4. Valide l’installation proposée par le navigateur.

Si le navigateur n’affiche pas de proposition automatique, utilise le menu `⋮` puis `Installer l’application` ou `Ajouter à l’écran d’accueil`.

Ouvre l’icône installée une première fois pendant que le serveur fonctionne. Cela permet au cache hors ligne de s’amorcer.

## 5. Installer sur iPhone / iPad

Avec Safari :

1. Ouvre l’application avec le serveur actif.
2. Appuie sur `Partager`.
3. Choisis `Sur l’écran d’accueil`.
4. Active `Ouvrir comme app` si l’option est proposée.
5. Appuie sur `Ajouter`.

Sur iPhone, le bouton d’installation intégré peut ne pas apparaître : le menu `Partager` est la méthode normale.

## 6. Utiliser l’application sur ordinateur

Ouvre la même URL HTTPS dans Edge, Chrome, Firefox ou Safari. L’interface passe automatiquement en deux colonnes sur un écran moyen et jusqu’à quatre colonnes sur un grand écran.

Dans Edge ou Chrome, tu peux également utiliser l’icône d’installation située dans la barre d’adresse pour ouvrir le Validator comme une application indépendante.

## 7. Tester le mode hors ligne

Après une première ouverture complète :

1. Ferme l’application.
2. Arrête temporairement le serveur local.
3. Relance l’application depuis l’icône installée.

L’interface doit encore s’ouvrir. Les données restent enregistrées localement dans le navigateur.

Pour remettre l’application à jour, ouvre-la avec Internet ou le réseau local disponible. Lorsqu’une nouvelle version est détectée, utilise le bouton « Mettre à jour » affiché dans l’application.

## 8. Sauvegarder ou transférer son état

Dans `⚙ Configuration` :

1. Appuie sur `Exporter / partager une sauvegarde`.
2. Enregistre le fichier JSON dans un emplacement privé.

Pour restaurer les données :

1. Ouvre `⚙ Configuration`.
2. Appuie sur `Importer une sauvegarde`.
3. Sélectionne le fichier JSON.
4. Confirme l’import.

L’import remplace les données actuelles après confirmation. Il est utile pour transférer la configuration et la session en cours entre Safari, Edge, un autre téléphone ou une nouvelle installation.

Important : héberger l’application sur un NAS ne synchronise pas automatiquement les boutons entre les appareils. Chaque navigateur possède son propre stockage local. Utilise l’export/import JSON si tu veux reprendre exactement le même état sur un autre appareil.

## 9. Problèmes fréquents

### « Adresse non compatible »

Utilise `localhost` ou `127.0.0.1` si le serveur tourne sur le téléphone. Une adresse HTTP du type `192.168.x.x` peut afficher l’application, mais elle ne permet généralement pas l’installation PWA ni le service worker. Dans ce cas, utilise HTTPS ou lance le serveur directement sur le téléphone.

### L’icône ou l’installation n’apparaît pas

Vérifie que `index.html`, `manifest.json`, `sw.js` et les trois fichiers PNG sont bien dans le même dossier, puis recharge la page.

### L’application s’ouvre vide après installation

Le stockage de l’application installée peut être séparé de celui du navigateur, notamment sur iPhone. Exporte une sauvegarde depuis le navigateur, ouvre l’application installée, puis importe le fichier JSON.

### Le mode hors ligne ne fonctionne pas immédiatement

Laisse l’application ouverte une première fois avec le serveur actif, recharge-la, puis arrête le serveur pour refaire le test.

## 10. Fonctionnement des données

Les réglages et les boutons activés sont conservés dans le stockage local du navigateur jusqu’à la création d’une nouvelle session. Ils ne sont pas envoyés au NAS par l’application. Effacer les données du navigateur ou utiliser une réinitialisation complète peut les supprimer ; exporte un JSON si tu veux transférer ton état.

L’application guide successivement l’état du trader, les quatre critères du setup et l’exécution. Le setup vérifie la narrative et le DOL, le POI, la prise de liquidité et le CSD. Le SMT reste facultatif. Elle ne calcule ni le stop, ni la taille de position, ni les limites de risque et ne propose pas d’historique de trades consultable. « Nouvelle session » efface les réponses et les incidents de la session en cours.

## 11. Repères horaires

Le bandeau affiche l’heure de New York, le créneau ICT en cours et le temps restant avant le prochain. Les horaires sont exprimés en heure de New York, avec changement d’heure automatique : Asie 20 h–0 h (du dimanche au jeudi), Londres 2 h–5 h, New York matin 7 h–10 h et New York après-midi 13 h 30–16 h (du lundi au vendredi).

Le statut du NQ est affiché séparément des créneaux ICT. Le marché peut être ouvert entre deux créneaux : « Aucun créneau ICT actif » ne signifie pas que le marché est fermé. Les créneaux et leur progression sont limités aux périodes d’ouverture prévues ; les fenêtres annulées par un jour férié sont exclues du compte à rebours.

Le bandeau TOI utilise les fenêtres du tableau « Horaires ». Il indique une fenêtre en cours, une fenêtre proche lorsqu’elle commence dans les quinze prochaines minutes, ou la prochaine fenêtre disponible. Les jours non applicables et les fenêtres situées après une fermeture anticipée du NQ sont ignorés. Cet indicateur reste informatif et n’a aucun effet sur le verdict.

Le calendrier intégré couvre le 1er janvier 2026 au 2 janvier 2028 inclus. Il tient compte des jours fériés et fermetures anticipées du NQ, de la pause quotidienne et de la pause de 16 h 15 à 16 h 30 à New York. Les horaires 2026 proviennent du [calendrier 2026 NinjaTrader pour les futures sur indices](https://support.ninjatrader.com/s/article/2026-Holiday-Trading-Hours?language=en_US), consulté le 25 octobre 2026. Les dates et indications 2027 proviennent du [calendrier CME Globex](https://www.cmegroup.com/trading-hours.html), consulté le 3 novembre 2026.

CME précise que les horaires de jours fériés sont généralement finalisés environ deux semaines avant la date concernée. L’application affiche donc « horaire prévisionnel » à partir du 4 janvier 2027. Ces horaires devront être contrôlés à mesure de leur publication définitive. Il ne faut pas utiliser les horaires du marché des actions Nasdaq pour le NQ.

L’application fonctionne hors ligne avec ce calendrier intégré ; elle ne télécharge pas automatiquement les calendriers suivants. Hors de sa période de couverture, elle indique « calendrier à actualiser » au lieu d’affirmer que le marché est ouvert. Les interruptions exceptionnelles et les horaires particuliers de votre courtier ne sont pas détectés.

Les calculs utilisent le fuseau `America/New_York`, sans décalage fixe avec la France. Les semaines où les changements d’heure français et américains ne coïncident pas sont donc prises en compte automatiquement. L’affichage utilise l’horloge de l’appareil.

Ces indications restent informatives, sans effet sur le verdict. Le scénario doit déjà préciser sa cible et sa fenêtre de temps dans « Narrative + DOL ».
