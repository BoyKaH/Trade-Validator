# Application Android Trade Validator

Cette enveloppe Android charge l’interface publiée à l’adresse :

`https://boykah.github.io/Trade-Validator/`

Elle ajoute les fonctions qui ne peuvent pas être garanties par la PWA seule :

- notifications locales TOI lorsque l’application est fermée ;
- alarmes exactes avec un délai de 5, 10, 15 ou 30 minutes ;
- activation indépendante de chacun des créneaux TOI ;
- renouvellement automatique quotidien des prochaines alarmes ;
- restauration des alarmes après redémarrage ou changement d’heure ;
- ouverture de la page des horaires depuis une notification ;
- aucun serveur de notifications et aucun compte utilisateur.

## Obtenir l’APK

Avant de quitter l’ancienne PWA, utilise **Réglages → Exporter / partager une sauvegarde**. Le stockage de l’APK est distinct de celui de Chrome : tu pourras importer ce fichier dans l’APK afin de récupérer tes réglages et la session en cours.

Après avoir poussé le dossier `android-app` sur la branche `main` :

1. Ouvrir l’onglet **Actions** du dépôt GitHub.
2. Ouvrir l’exécution **Construire l’APK Android**.
3. Télécharger l’artefact **Trade-Validator-Android**.
4. Extraire puis installer `Trade-Validator.apk` sur le téléphone.
5. Ouvrir les réglages de l’APK et importer la sauvegarde créée depuis la PWA.

Android demandera d’autoriser l’installation depuis le navigateur ou le gestionnaire de fichiers utilisé. Cette autorisation peut être retirée après l’installation.

## Activer les alertes

Dans Trade Validator :

1. Ouvrir **Réglages**.
2. Activer **Alertes TOI**.
3. Autoriser les notifications.
4. Autoriser les alarmes exactes.
5. Utiliser **Envoyer une notification de test**.

Android conserve les occurrences futures du calendrier et recharge automatiquement les prochaines alarmes chaque jour. Il n’y a rien à reprogrammer manuellement. Les jours de fermeture et horaires réduits présents dans le calendrier NQ sont automatiquement exclus.

## Mises à jour

Les changements de `index.html`, `sw.js` et du calendrier arrivent toujours par GitHub Pages. Il n’est donc pas nécessaire de réinstaller l’APK pour une modification de l’interface ou des horaires.

Un nouvel APK est requis uniquement si le code situé dans `android-app` change.
