# Trade Validator — notes pour Claude Code

## But de l'appli

Outil **anti-FOMO** pour rester dans la stratégie de la formation ICT **Trade Legacy** (fiche *Time & Price* : HTF IOF → DOL → POI → TOI → CSD, SMT facultatif).
L'appli ne sert **pas** à gérer l'exécution : pas d'entrée, de stop, de target ni de calcul de taille. Ce choix est volontaire, ne pas les ajouter.
Objectif affiché : « 100 % de trades conformes — pas le payout ».

Le validateur sert **avant** le trade, le journal **après**. Pierre entre souvent au marché : aucun clic n'est attendu au moment de l'entrée.

## Fichiers

- `index.html` : toute l'appli (HTML + CSS + JS vanilla, sans dépendance). Version actuelle : `APP_VERSION = "1.6.0"`.
- `sw.js` : service worker PWA. **Incrémenter `CACHE_NAME`** (actuellement `v21`) à chaque modification de `index.html`.
- `pre_trade_validator.html` : redirection pour les anciennes installations.
- `manifest.json`, icônes : PWA.
- `android-app/` : APK WebView qui charge `https://boykah.github.io/Trade-Validator/`. Les changements n'arrivent sur le téléphone qu'après **commit + push sur `main`** (GitHub Pages). Le bridge `window.TradeValidatorAndroid` gère les notifications TOI.
- `.github/workflows/android-apk.yml` : build de l'APK (déclenché seulement par `android-app/**`).

## Données (localStorage)

- Clé actuelle `ptv_v8` (version 8). Migration automatique depuis `ptv_v7` … `ptv_v1` dans `loadApp()`.
- `app.setups[currentId]` : session en cours. Champs : `traderState`, `criterionStates`, `context` (`poi`, `dol`, `invalidation` ; `narrative` et `toi` gardés pour les anciennes sessions mais plus affichés depuis 1.5.0 / 1.6.0), `toiOverride` / `toiOverrideAt`, `csdObs` (type de CSD : `displacement`, `ifvg`, `mss`, `breaker`, `fpfvg`), `flags`, `noSetup`, `incidentIds`. L'ancien champ `model` a été retiré : `model:"fpfvg"` migre en `csdObs.fpfvg`.
- `app.journal` : entrées `kind: "trade"` ou `"no_trade"`. Un trade stocke `auto` (verdict retrouvé : `category` `ok` | `bad` | `none`), `lossAt`, `exported`, `custom`.
- `app.journalFields` : champs personnalisés (`text`, `number`, `yesno`, `choice`).
- `app.verdictLog` : historique horodaté des verdicts avec un `snap` du contexte. Maximum 3000 entrées et 150 jours. Battement toutes les 30 min si l'appli est visible.
- `app.settings` : `lossPauseMinutes` (10), `dailyTargetR` (0 = désactivé), `csvFormat` (`fr` | `std`), `nextTradeNum`, plus les réglages TOI et Android.
- Toute nouvelle donnée doit passer par `normalizeApp()` et les fonctions `normalize*()`. La sauvegarde JSON (`validateBackupRoot`) accepte les versions 5 à 8.

## Logique du validateur

- 4 étapes : `state` (ÉTAT), `context` (CONTEXTE), `setup` (SETUP), `entry` (affichée « CSD »).
- `getVerdict()` : incidents et pause après perte (`block`), puis questions d'état, champs de contexte obligatoires (POI, DOL), critères `c_htf_zone` et `c_liquidity_taken`, puis CSD `c_setup_liquidity`. Ensuite `nogo` > `wait` > `premium` (SMT) > `conforme`.
- `verdict.warnings` (signaux d'abstention) : **avertissement seulement, ne bloque jamais**.
- TOI unique (`TOI_CHECK`, `currentToi()`) : `REFERENCE_WINDOWS` (XX:45 → XX:15) + ouverture RTH 09:30 → 09:45 NY. **Hors TOI = avertissement dans `verdict.warnings`, jamais bloquant** (Pierre : une entrée à XX:40 peut arriver). Correction manuelle valable 60 min. Plus de champ TOI texte : la phrase et le journal reprennent la fenêtre détectée. Plus de choix « Modèle » : Time & Price est le processus, le First Presented FVG est un type de CSD coché à l'étape 4.
- Narrative HTF retirée de l'écran à la demande de Pierre (souvent floue en live). La fiche garde l'étape HTF IOF : c'est un choix de Pierre, pas une règle de la formation.
- Les champs de contexte tapés ne déclenchent pas `render()` (perte du focus) : on passe par `refreshLive()`. `updateTiming()` tourne chaque seconde et re-rend si la signature du verdict change.

## Journal

- `lookupVerdict(date, heure NY)` retrouve le dernier verdict avant la minute d'entrée. Au-delà de 2 h, la validation est périmée (« Sans validation »). Un avertissement s'affiche si le verdict a changé à ±2 min.
- Pré-remplissage des champs non modifiés : contexte, SMT, confirmation (d'après le type de CSD coché : 1st presented FVG > MSS > IFVG > CSD), plan respecté. Le biais n'est plus pré-rempli, sauf depuis d'anciens `snap` qui contiennent une narrative.
- Une perte récente (< 6 h) déclenche la pause et remet à zéro les questions d'état.
- Onglet Journal → sous-onglets « Trades » / « Dashboard ». Le Dashboard reprend celui de l'Excel (`tradeStats()`, trades avec R et date seulement) : nombre de trades, winrate hors 0 R, expectancy, profit factor, Sharpe par trade (écart-type d'échantillon, non annualisé), RR moyen (gain moyen / perte moyenne), drawdown max depuis 0, pertes consécutives max (un BE remet à zéro) ; courbes R cumulés et drawdown ; filtre début/fin (KPI et courbes seulement) ; calendrier mensuel et bilan des 12 derniers mois sur tous les trades. Vérifié contre l'Excel : mêmes valeurs. Filtres en mémoire (`dashState`), non sauvegardés.
- Contexte : ordre POI → DOL → case TOI automatique → invalidation (choix de Pierre ; la fiche liste DOL avant POI, l'exemple 2 cherche le POI H1 d'abord).
- Export : **exactement les 14 colonnes de l'Excel de Pierre** (`JOURNAL_COLUMNS`). Les champs personnalisés ne sont pas exportés, sauf demande contraire. « Copier les nouveaux » produit du TSV pour Google Sheets ; « Exporter CSV » produit un CSV avec BOM.

## Règles de contenu

- La méthode vient de la formation (skill `transcriptions-trade-legacy` : fiche `process_time_price_eleves`, exemples Time & Price, lives des 12, 13 et 19/11/2026). Ne pas inventer de règle ICT.
- Distinguer règle de la formation et suggestion. La pause après perte et les rappels d'arrêt sont des garde-fous ajoutés, pas des règles du formateur.
- Pas de délai forcé avant CONFORME : Pierre l'a refusé.
- Interface en français, heure NY pour tout ce qui touche aux bougies. Cible mobile d'abord (Android), textes ≥ 11 px.

## Vérifier un changement

1. `node --check` sur le script extrait de `index.html`.
2. Ouvrir l'appli servie en local (`python3 -m http.server`). Faire le parcours : état → contexte → setup → CSD → journal (nouveau trade, perte → pause, export CSV). Tester la migration depuis une sauvegarde `ptv_v7`.
3. Incrémenter `APP_VERSION` et `CACHE_NAME`, puis commit + push.

## Pistes non faites

- Import des 5 trades de l'Excel : **refusé par Pierre**, ne pas le faire.
- Les notifications Android TOI utilisent encore seulement `REFERENCE_WINDOWS` (pas la fenêtre d'ouverture 09:30 → 09:45).
