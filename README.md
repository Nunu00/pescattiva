# Previsioni Pesca iOS Application

Un'applicazione iOS nativa scritta in Swift e SwiftUI che calcola in modo **completamente offline** (senza alcuna chiave API a pagamento) le previsioni di pesca basandosi su maree armoniche locali, effemeridi del sole/luna e teoria solunare di John Alden Knight.

L'applicazione è progettata per essere compilata automaticamente tramite **GitHub Actions** in un file `.ipa` non firmato, pronto per essere installato su iPhone tramite **SideStore** (o AltStore/TrollStore), senza bisogno di un Mac fisico.

---

## ⚙️ Funzionalità principali

1. **Previsioni Astronomiche Offline**: Calcolo esatto di alba/tramonto del Sole, alba/tramonto della Luna, transito lunare (Luna al meridiano) e antitransito lunare (Luna al nadir) tramite la libreria **SwiftAA** (algoritmi di Jean Meeus).
2. **Calcolatore Maree Offline**: Un motore armonico in Swift che modella l'altezza delle maree combinando i 5 costituenti principali ($M_2, S_2, N_2, K_1, O_1$) per le località calabresi (Sibari, Trebisacce, Corigliano, Rossano, Cetraro, Amantea, Tropea, Scilla), trovando l'ora esatta di alte/basse maree giornaliere e calcolando il **Coefficiente di Marea** (da 20 a 120) compensato con la distanza Terra-Luna.
3. **Periodi Solunari**: Calcolo automatico dei periodi Maggiori (transiti $\pm 1\text{h}$) e Minori (sorgere/tramonto luna $\pm 30\text{m}$) con marcatura dei "picchi potenziati" se coincidono con alba o tramonto del sole.
4. **Algoritmo Attività Pesci (Rules Engine)**: Regole che integrano la velocità della corrente di marea sinusoidale normalizzata per l'escursione di sizigia del porto, i fattori lunari, il vento sostenuto gaussiano e le previsioni meteo marine (Open-Meteo) per ricavare un punteggio giornaliero ed orario dell'attività pesci (Bassa, Moderata, Buona, Alta, Molto Alta).
5. **Previsioni SST ad "Anomalia Persistente"**: Integrazione delle temperature marine reali e meteorologiche con decay esponenziale dell'anomalia termica basato su costanti di decorrelazione stagionale per le query di date a lungo termine oltre l'orizzonte di previsione standard.
6. **Interfaccia Utente a Calendario**: Visualizzazione mensile con colorazione ad alta saturazione per le previsioni reali a 7 giorni e celle semi-trasparenti per le stime a lungo termine, corredata da timeline oraria ed andamento grafico delle maree.

---

## 📲 Come installare l'app su iPhone

### 1. Clona il Repository e attiva il Workflow GitHub
1. Carica questo codice sul tuo repository GitHub personale (`https://github.com/Nunu00/pescattiva`).
2. Vai sulla scheda **Actions** del tuo repository su GitHub.
3. Seleziona il workflow **Build Unsigned IPA** a sinistra e clicca su **Run workflow** (oppure fai un push sulla branch `main` per farlo partire automaticamente).
4. Attendi il completamento della compilazione (circa 3-4 minuti, dato che scarica e compila `SwiftAA` e le sue dipendenze C++).
5. A fine processo, clicca sull'esecuzione del workflow e scarica l'artifact **Previsioni-Pesca-Unsigned-IPA.zip**.
6. Estrai il file `.zip` sul tuo computer o direttamente sul tuo iPhone per ottenere il file `PrevisioniPesca.ipa`.

### 2. Installazione tramite SideStore (Senza Mac)
Segui le varie guide presenti online.

---

## 📂 Struttura del Progetto

- `MeteoPesca/Models.swift`: Modelli dati condivisi per maree, solunare ed intervalli di attività.
- `MeteoPesca/AstronomyEngine.swift`: Motore che esegue i calcoli solari e lunari Meeus tramite `SwiftAA`.
- `MeteoPesca/TideEngine.swift`: Motore che esegue il calcolo delle maree basato sui costituenti armonici per i porti italiani (con selezione automatica della stazione più vicina).
- `MeteoPesca/RulesEngine.swift`: Regole per l'assegnazione dei punteggi di attività dei pesci.
- `MeteoPesca/ContentView.swift`: Interfaccia utente a schermata singola in SwiftUI.
- `MeteoPesca/MeteoPescaApp.swift`: Punto di ingresso dell'app SwiftUI.
- `MeteoPesca.xcodeproj/`: Configurazione Xcode compatibile con SPM.
