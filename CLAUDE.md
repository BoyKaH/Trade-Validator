# Trade Validator — notes pour Claude Code

## But de l'appli

Outil **anti-FOMO** pour rester dans la stratégie de la formation ICT **Trade Legacy** (fiche *Time & Price* : HTF IOF → DOL → POI → TOI → CSD, SMT facultatif).
L'appli ne sert **pas** à gérer l'exécution : pas d'entrée, de stop, de target ni de calcul de taille. Ce choix est volontaire, ne pas les ajouter.
Objectif affiché : « 100 % de trades conformes — pas le payout ».

Le validateur sert **avant** le trade, le journal **après**. Pierre entre souvent au marché : aucun clic n'est attendu au moment de l'entrée.

## Fichiers

- `index.html` : toute l'appli (HTML + CSS + JS vanilla, sans dépendance). Version actuelle : `APP_VERSION = "1.20.0"`.
- `sw.js` : service worker PWA. **Incrémenter `CACHE_NAME`** (actuellement `v41`) à chaque modification de `index.html`.
- `pre_trade_validator.html` : redirection pour les anciennes installations.
- `manifest.json`, icônes : PWA.
- `android-app/` : APK WebView qui charge `https://boykah.github.io/Trade-Validator/`. Les changements n'arrivent sur le téléphone qu'après **commit + push sur `main`** (GitHub Pages). Le bridge `window.TradeValidatorAndroid` gère les notifications TOI.
- `.github/workflows/android-apk.yml` : build de l'APK (déclenché seulement par `android-app/**`).

## Données (localStorage)

- Clé actuelle `ptv_v8` (version 8). Migration automatique depuis `ptv_v7` … `ptv_v1` dans `loadApp()`.
- `app.setups[currentId]` : session en cours. Champs : `traderState` (`identity` seulement depuis 1.8.0 ; les anciennes réponses `moneyTarget` / `clearMind` / `acceptsZero` sont ignorées à la migration), `criterionStates`, `context` (`poi`, `dol`, `invalidation` ; `narrative` et `toi` gardés pour les anciennes sessions mais plus affichés depuis 1.5.0 / 1.6.0), `toiOverride` / `toiOverrideAt`, `csdObs` (type de CSD : `displacement`, `ifvg`, `mss`, `breaker`, `fpfvg`), `flags`, `noSetup`, `incidentIds`. L'ancien champ `model` a été retiré : `model:"fpfvg"` migre en `csdObs.fpfvg`.
- `app.journal` : entrées `kind: "trade"`, `"no_trade"` ou `"avoided"` (trade évité : `date`, `time` NY, `reason` `market` (FOMO) | `impulse` | `discord` | `result` | vide). Numéro d'un trade = rang chronologique (date + heure NY) depuis 1, calculé à l'affichage par `tradeNum()` (v1.12.5) : une suppression ou un trade ajouté dans le passé renumérote sans trou. Le champ `num` stocké n'est plus qu'un départage ; le réglage « Numéro du prochain trade » a été retiré. Un trade stocke `auto` (verdict retrouvé : `category` `ok` | `bad` | `none`), `lossAt`, `exported`, `custom`.
- `app.journalFields` : champs personnalisés (`text`, `number`, `yesno`, `choice`).
- `app.verdictLog` : historique horodaté des verdicts avec un `snap` du contexte. Maximum 3000 entrées et 150 jours. Battement toutes les 30 min si l'appli est visible.
- `app.settings` : `lossPauseMinutes` (10), `dailyTargetR` (0 = désactivé), `csvFormat` (`fr` | `std`), `nextTradeNum` (plus affiché depuis 1.12.5), plus les réglages TOI et Android.
- `app.tombstones` (`journal`, `fields` : id → ms) : suppressions mémorisées 400 jours pour la synchro. Supprimer une entrée du journal via `removeJournalEntries()`, jamais par un simple `filter`.

## Synchronisation (v1.7.0)

- Fichier `journal.json` dans un dépôt GitHub **privé** de Pierre (`BoyKaH/Trade-Validator-data` par défaut), via l'API Contents (`readRemote` / `writeRemote`). Jeton fine-grained limité à ce dépôt, Contents read/write.
- Réglages de synchro et jeton dans la clé `ptv_sync`, **hors de `app`** : jamais dans les sauvegardes JSON ni dans le fichier synchronisé.
- Synchronisés : `journal`, `journalFields`, `tombstones`, `verdictLog`, `nextTradeNum`. Non synchronisés : session en cours, incidents, réglages d'appareil.
- `mergeSyncData()` : fusion par id, `updatedAt` le plus récent gagne ; `exported` et `lossAt` sont conservés d'un côté ou de l'autre. Pas d'écriture si l'empreinte (`canonicalSyncSig`) est identique. Conflit de sha (409/422) → relecture et nouvel essai (3 fois).
- Déclencheurs : ouverture de l'appli, retour au premier plan ou focus (au plus toutes les 20 s), 3 s après un changement du journal, passage en arrière-plan, retour du réseau, **relecture toutes les 2 min quand l'appli est visible**, et toutes les 10 min pour l'historique des verdicts. Dans l'APK, le retour au premier plan passe aussi par l'événement `android-notification-permission-changed` (envoyé par `onResume`) : la WebView ne déclenche pas toujours `visibilitychange` (bug vu en 1.12.0 : une suppression faite sur le PC restait visible sur le téléphone).
- Après une fusion qui change le journal, l'écran est re-rendu (aussi le validateur), sauf si une feuille est ouverte ou un champ en cours de saisie.
- En service avec un vrai jeton depuis le 22/11/2026 (historique visible dans le dépôt de données).
- Bandeau « Cet appareil n'est pas synchronisé » en haut du Journal quand aucune synchro n'est connectée (v1.17.0, bouton → ⚙ défilé jusqu'à `#sync-group`). Cas vécu le 28/11/2026 : 2 trades saisis sur l'ordi du boulot, sans synchro, jamais arrivés sur GitHub.
- Toute nouvelle donnée doit passer par `normalizeApp()` et les fonctions `normalize*()`. La sauvegarde JSON (`validateBackupRoot`) accepte les versions 5 à 8.

## Calendrier NQ

- Écrit en dur (`NQ_CALENDAR`, sources CME / NinjaTrader) : horaires normaux + jours fériés du 01/01/2026 au 02/01/2028. **Aucune mise à jour automatique** : prolonger à la main chaque année.
- Prévisionnel : toute l'année 2027 (`provisionalFrom`) et les dates marquées `provisional:true` (24/12 et 31/12/2026). Revérifié le 28/11/2026 : CME n'avait pas encore publié 2027. Retirer `provisional` / avancer `provisionalFrom` quand CME confirme.
- v1.16.0 : la frise TOI suit le calendrier (heures fermées hachurées, TOI seulement quand le NQ est ouvert) ; avertissement « NQ fermé » dans `verdict.warnings` (jamais bloquant) ; rappel dans ⚙ à 60 jours de la fin (`CALENDAR_REMINDER_DAYS`, `calendarDaysLeft()`) et « calendrier à prolonger » dans l'en-tête à 30 jours.

## Logique du validateur

- **Validateur express seul depuis 1.20.0** (Pierre : le mode en 4 étapes faisait « usine à gaz » avant un trade ; le mode complet a été **retiré**, pas seulement masqué). Un écran (`renderValidator`) : bandeaux, frise TOI, carte « Contexte HTF » (POI, DOL : jusqu'à 4 valeurs récentes en gros boutons tirées des `snap` de `verdictLog` via `recentContextValues()`, + « Autre / écrire » avec champ texte et chips), carte « Avant d'entrer » (`renderExpressChecks`) : Prix dans mon POI, Liquidité attendue prise, question d'état Oui / Non, CSD présent, SMT facultatif, puis une ligne d'état (« Il manque : … » / NO GO / Conforme). « Pas de setup » et « Nouvelle session » en dessous. Sur ordi : 2 colonnes.
- Plus saisis depuis 1.20.0 : types de CSD (`csdObs`), signaux d'abstention (`flags`), invalidation, phrase. Les champs restent dans les données (anciennes sessions, `snap`) mais le journal ne pré-remplit plus qu'avec contexte (POI · DOL), SMT, « CSD » / « Aucune » et plan respecté : Pierre complète le reste à la main.
- Les étapes internes (`stageStatus`) servent encore au verdict et à la pulsation du bouton Trade évité.
- ÉTAT = une seule question : « Est-ce que cette action est cohérente avec le trader que je suis en train de devenir ? » (NON → incident `identity`, NO GO pour la session). **Garde-fou ajouté** (vidéo Astra Mindset sur discipline et identité), **pas une règle de la formation**. Remise à zéro à chaque session et après une perte.
- `getVerdict()` : incidents, pause après perte et limite de 2 SL du jour NY (`block`, `dailySlReached()`), puis champs de contexte obligatoires (POI, DOL), critères `c_htf_zone` et `c_liquidity_taken`, puis question d'état, puis CSD `c_setup_liquidity`. Ensuite `nogo` > `wait` > `premium` (SMT) > `conforme`.
- `verdict.warnings` (signaux d'abstention) : **avertissement seulement, ne bloque jamais**.
- TOI unique (`TOI_CHECK`, `currentToi()`) : `REFERENCE_WINDOWS` (XX:45 → XX:15) + ouverture RTH 09:30 → 09:45 NY. **Hors TOI = avertissement dans `verdict.warnings`, jamais bloquant** (Pierre : une entrée à XX:40 peut arriver). Correction manuelle valable 60 min. Plus de champ TOI texte : la phrase et le journal reprennent la fenêtre détectée. Plus de choix « Modèle » : Time & Price est le processus, le First Presented FVG est un type de CSD coché à l'étape 4.
- Narrative HTF retirée de l'écran à la demande de Pierre (souvent floue en live). La fiche garde l'étape HTF IOF : c'est un choix de Pierre, pas une règle de la formation.
- Les champs de contexte tapés ne déclenchent pas `render()` (perte du focus) : on passe par `refreshLive()`. `updateTiming()` tourne chaque seconde et re-rend si la signature du verdict change.

## Journal

- `lookupVerdict(date, heure NY)` retrouve le dernier verdict avant la minute d'entrée. Au-delà de 2 h, la validation est périmée (« Sans validation »). Un avertissement s'affiche si le verdict a changé à ±2 min.
- Pré-remplissage des champs non modifiés : contexte, SMT, confirmation (d'après le type de CSD coché : 1st presented FVG > MSS > IFVG > CSD), plan respecté. Le biais n'est plus pré-rempli, sauf depuis d'anciens `snap` qui contiennent une narrative.
- Une perte récente (< 6 h) déclenche la pause et remet à zéro les questions d'état.
- Bouton « Trade évité » (barre du bas, v1.9.0, renommé en 1.11.0) : un tap = une entrée `avoided` horodatée, puis une feuille avec la raison facultative (FOMO / Impulsif / Discord / Résultat), l'heure modifiable et la suppression. Compteurs jour / semaine (lundi, NY) / total dans le bandeau et le journal. Pas d'entrée automatique sur NO GO.
- **Discipline (gamification, v1.11.0)** : carte en haut de l'onglet Journal › Trades (`renderDisciplineCard`, `disciplineStats()`), tout recalculé depuis le journal (rien de stocké en plus). But (Pierre) : l'inciter à **ne pas prendre de trades merdiques et à cliquer sur « Trade évité »**, pas à trader. Barème choisi par Pierre : trade évité +1 (5 par jour max), trade conforme **avec « Plan respecté : Oui »** +1 (sinon 0, sans casser la série), « Pas de setup » +1 (1 par jour : validateur utilisé sans trade = bonne décision). **Pas de bonus de journée** : un jour sans trade n'a pas de valeur en soi (Pierre peut simplement ne pas trader) ; la série (« jours clean d’affilée ») compte les jours avec au moins une entrée et sert aux badges ; la flamme 🔥 ne s’affiche qu’à partir de 3 jours (`STREAK_FLAME_MIN`), avant on montre juste le chiffre. Un trade hors process ou sans validation **remet seulement la série à zéro** (pas de points négatifs). **Le résultat en R ne rapporte jamais de points.** Un jour sans entrée ne casse pas la série. **Pas de niveaux** (retirés : sans signification) et pas de compteur « conformes d'affilée » (il poussait à trader). La carte montre : total de points, points du jour, série 🔥, trades évités, sessions sans trade. Depuis 1.18.0, chaque compteur dit ce qui le fait bouger, et une ligne « Dernier effet » (`lastDisciplineEffect()`) explique l'effet de la dernière entrée saisie ou modifiée (ex. « trade #1 sans validation → série remise à 0, 0 pt »). Un trade saisi sans passage par le validateur reste « Sans validation » même avec « Plan respecté : Oui ». Le bandeau du jour reprend « +N pts · 🔥 N ». Chaque action affiche son retour (`disciplineFeedback()`) : « +1 pt », « série remise à zéro », badge. Badges (`BADGES`) orientés retenue (trades évités, jours clean d’affilée, session sans trade), sur les records, jamais perdus ; toast « Badge débloqué » via `newBadgeText()`.
- Bouton du bas **« Trade évité »** (ex-« FOMO évité », `kind:"avoided"` inchangé) : FOMO ou trade impulsif retenu. Raisons : FOMO (id `market`, ancien « Marché »), Impulsif, Discord, Résultat. Il affiche le compteur du jour (« 🛡 N ») et **pulse seulement au moment de la tentation** : NO GO, ou WAIT une fois le SETUP validé (`renderSticky`). Une pulsation permanente pendant le remplissage ne se voyait plus.
- Liste du journal : la raison d'un trade évité n'apparaît qu'à droite. Plus de pastille numérotée sur les cartes d'étape (les onglets du haut numérotent déjà).
- Bandeau du jour unique (`renderDayNotices`) : trades évités, trades pris, R, SL ; il passe en rouge à la limite. **Limite de 2 SL par jour (`DAILY_SL_LIMIT`)** : règle de Pierre, pas de la formation → **NO GO jusqu'au lendemain** (jour = heure de New York, choix de Pierre).
- Les trades « Sans validation » ont un cadre d'alerte dans la liste. Les cases Conformes / Hors process / Sans validation montrent le nombre de trades, « N % des trades » (part du total, pas un winrate) et, depuis 1.19.0 (demande de Pierre : voir l'impact du process sur la performance), le winrate et le total en R de la catégorie, calculés comme le Dashboard (trades avec R et date, 0 R exclus du winrate).
- Tous les réglages sont sous ⚙ : Garde-fous (pause, objectif), Journal (format de copie), champs personnalisés. « Nouvelle session » est sous « Pas de setup », plus dans la barre du bas. La TOI détaillée n'est que dans la ligne du haut ; la frise TOI porte la correction manuelle (petit lien).
- Onglet Journal → sous-onglets « Trades » / « Dashboard ». Le Dashboard reprend celui de l'Excel (`tradeStats()`, trades avec R et date seulement) : nombre de trades, winrate hors 0 R, expectancy, profit factor, Sharpe par trade (écart-type d'échantillon, non annualisé), RR moyen (gain moyen / perte moyenne), drawdown max depuis 0, pertes consécutives max (un BE remet à zéro) ; courbes R cumulés et drawdown ; filtre début/fin (KPI et courbes seulement) ; calendrier mensuel et bilan des 12 derniers mois sur tous les trades. Vérifié contre l'Excel : mêmes valeurs. Filtres en mémoire (`dashState`), non sauvegardés.
- Bloc « CONTEXTE HTF » teinté en bleu (v1.13.0, demande de Pierre). Chips en groupes (`chipGroups`) : POI « Zones » (FVG H1, Breaker H1) + « Niveaux » ; DOL « Niveaux ». Niveaux en paires H / L (`LEVEL_PAIRS` : PW, PD, Asian, London, AM, Data, Cycle 90 précédent) + chip « 09:30 ». Old PD Array, Key Level et Opening Price retirés à la demande de Pierre (repères de la fiche qu'il n'utilise pas).
- Frise TOI (v1.13.0, `renderToiTimeline` / `refreshToiTimeline`) au-dessus du contexte : 4 h autour de l'heure NY, fenêtres `TOI_CHECK`, trait = maintenant, « Prochaine TOI dans N min ». **Visuel seulement.** Elle remplace la case OUI / PAS ENCORE ; la correction manuelle reste en petit lien. Dans le validateur, la ligne TOI du haut (`#toi-indicator`) est masquée ; dans le Journal elle reste, en heure NY (`nyRange`).
- Ergonomie (v1.14.0, audit) : bouton retour (Android / navigateur) ferme la feuille ou la confirmation ouverte au lieu de quitter l'appli (`layerOpened` / `layerClosed` / `popstate`, une seule entrée d'historique). Dans le Journal, le bandeau du bas cache le verdict (`journal-mode`) et garde « Trade évité » + ⚙, sans pulsation. Carte Discipline : badges et règles repliés dans « Badges et règles » (`discMoreOpen`, en mémoire). Toujours vérifier mobile **et** ordi (Pierre utilise les deux).
- Ergonomie (v1.15.0, audit suite) : textes un cran plus grands (min 11,5 px, surtout 12,5–13,5 px), gras allégé, plus de majuscules sauf badges et verdicts (WAIT / NO GO / CONFORME), titres en casse normale. Cibles tactiles ≥ 40 px (chips, H / L, lien de correction TOI). En-tête plus compact. Formulaire de trade : date, heure, sens, R, plan respecté en haut ; le reste dans « Détails » replié (`journalDraft.moreOpen`). Suppression d'une entrée du journal **sans confirmation**, avec toast « Annuler » 6 s (`showToast(message,{label,run})`) : l'annulation retire la pierre tombale et met `updatedAt` à maintenant pour que la synchro garde l'entrée. Réglages : procédure de jeton GitHub repliée.
- Contexte : ordre POI → DOL → case TOI automatique → invalidation (choix de Pierre ; la fiche liste DOL avant POI, l'exemple 2 cherche le POI H1 d'abord).
- Bouton « Exporter » → feuille avec deux choix :
  - **Fichier Excel `.xlsx`** (`exportJournalXlsx`, écrit à la main : ZIP sans compression + SpreadsheetML, sans dépendance ; plus de CSV depuis 1.10.0). Colonnes : ID, **Type** (Trade | Trade évité), puis les 13 autres colonnes de l'Excel de Pierre (`XLSX_COLUMNS`). Une ligne par trade pris et par trade évité, ordre chronologique ; la raison du trade évité va dans « Comportement / Psycho ». Dates, heures et R en vraies valeurs Excel. Vérifié dans Excel.
  - « Copier les nouveaux trades » : TSV des 14 colonnes (`JOURNAL_COLUMNS`) pour Google Sheets, trades seulement.
  - Les champs personnalisés ne sont pas exportés, sauf demande contraire.

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
