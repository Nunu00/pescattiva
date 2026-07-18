# MeteoPesca iOS Application

Un'applicazione iOS nativa scritta in Swift e SwiftUI che calcola in modo **completamente offline** (senza alcuna chiave API a pagamento) le previsioni di pesca basandosi su maree armoniche locali, effemeridi del sole/luna e teoria solunare di John Alden Knight.

L'applicazione è progettata per essere compilata automaticamente tramite **GitHub Actions** in un file `.ipa` non firmato, pronto per essere installato su iPhone tramite **SideStore** (o AltStore/TrollStore), senza bisogno di un Mac fisico.

## Funzionalità principali

1. **Previsioni Astronomiche Offiline**: Calcolo esatto di alba/tramonto del Sole, alba/tramonto della Luna, transito lunare (Luna al meridiano) e antitransito lunare (Luna al nadir) tramite la libreria **SwiftAA** (algoritmi di Jean Meeus).
2. **Calcolatore Maree Offline**: Un motore armonico in Swift che modella l'altezza delle maree combinando i 5 costituenti principali (M2, S2, N2, K1, O1) per i porti italiani (default Sibari, Crotone, Reggio Calabria, Salerno, ecc.), trovando l'ora esatta di alte/basse maree giornaliere.
3. **Periodi Solunari**: Calcolo automatico dei periodi Maggiori (transiti ±1h) e Minori (sorgere/tramonto luna ±30m) con marcatura dei "picchi potenziati" se coincidono con alba o tramonto del sole.
4. **Algoritmo Attività Pesci**: Regole che combinano l'escursione di marea con i bonus solunari e lunari per determinare l'indice di attività pesci orario e giornaliero (Bassa/Media/Alta/Molto Alta).
5. **UI SwiftUI Premium**: Visualizzazione a timeline oraria, dettagli della Luna con disegno delle fasi lunari ed andamento delle maree disegnato su grafico interattivo.

## Come installare l'app su iPhone

### 1. Clona il Repository e attiva il Workflow GitHub
1. Carica questo codice sul tuo repository GitHub personale.
2. Vai sulla scheda **Actions** del tuo repository su GitHub.
3. Seleziona il workflow **Build Unsigned IPA** a sinistra e clicca su **Run workflow** (oppure fai un push sulla branch `main` per farlo partire automaticamente).
4. Attendi il completamento della compilazione (circa 3-4 minuti, dato che scarica e compila `SwiftAA` e le sue dipendenze C++).
5. A fine processo, clicca sull'esecuzione del workflow e scarica l'artifact **MeteoPesca-Unsigned-IPA.zip**.
6. Estrai il file `.zip` sul tuo computer o direttamente sul tuo iPhone per ottenere il file `MeteoPesca.ipa`.

### 2. Installazione tramite SideStore (Senza Mac)
[SideStore](https://sidestore.io/) è una piattaforma che consente di firmare e installare app sideloaded sul tuo iPhone utilizzando il tuo ID Apple gratuito direttamente dal dispositivo via Wi-Fi.

1. Installa SideStore sul tuo iPhone seguendo la guida ufficiale su [sidestore.io](https://sidestore.io/).
2. Apri l'app **SideStore** sul tuo iPhone.
3. Vai nella scheda **My Apps** e tocca il tasto **+** in alto a sinistra.
4. Seleziona il file `MeteoPesca.ipa` che hai scaricato ed estratto.
5. Inserisci le credenziali del tuo ID Apple gratuito quando richiesto (la firma avviene localmente sul tuo iPhone usando una VPN locale di SideStore, i dati non vengono trasmessi a terzi).
6. L'app verrà installata e sarà visibile nella schermata Home del tuo iPhone. Sarà necessario aggiornarla (refresh) una volta alla settimana tramite SideStore sotto la stessa rete Wi-Fi.

## Struttura del Progetto

- `MeteoPesca/Models.swift`: Modelli dati condivisi per maree, solunare ed intervalli di attività.
- `MeteoPesca/AstronomyEngine.swift`: Motore che esegue i calcoli solari e lunari Meeus tramite `SwiftAA`.
- `MeteoPesca/TideEngine.swift`: Motore che esegue il calcolo delle maree basato sui costituenti armonici per i porti italiani (con selezione automatica della stazione più vicina).
- `MeteoPesca/RulesEngine.swift`: Regole per l'assegnazione dei punteggi di attività dei pesci.
- `MeteoPesca/ContentView.swift`: Interfaccia utente a schermata singola in SwiftUI.
- `MeteoPesca/MeteoPescaApp.swift`: Punto di ingresso dell'app SwiftUI.
- `MeteoPesca.xcodeproj/`: Configurazione Xcode compatibile con SPM.
