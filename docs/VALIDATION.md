# Verifiche della prima versione

## Verifica MP4 e aggiornamenti Android — 0.9.0

* Il Reel segnalato nello screenshot è stato scaricato completamente su Windows: MP4 da 1.367.597 byte, VP9 (`vp09`) 1916×1078 con audio HE-AAC, durata circa 15.071 secondi. FFprobe lo riconosce correttamente. Il controllo Android precedente dipendeva da MediaMetadataRetriever e poteva rifiutare un contenitore valido se il telefono non ne riconosceva il codec; ora usa FFprobe incluso e distingue file incompleto, durata non verificabile, limite superato e traccia assente.
* Fixture VP9 sintetica inclusa soltanto nell’APK dei test: il Reel dell’utente non è distribuito. Le prove su emulatore verificano accettazione MP4 VP9, rifiuto come file audio, rifiuto di un MP4 corrotto e mantenimento dei controlli della durata.
* Aggiornamento APK: test JVM di versioni stabili, ordinamento numerico e checksum non ambiguo. Test Android con l’APK reale installato, hash/versione/package errati e un APK senza codice con stessa identità/versione ma firma estranea. FileProvider confinato alla cartella degli aggiornamenti. Prova UI della disponibilità del download e dell’assenza del comando Installa prima della verifica.
* Il trasferimento dell’APK usa DownloadManager e la sua notifica di sistema, con ID persistente e ripresa dello stato alla riapertura. L’installazione resta una conferma Android esplicita. Non viene dichiarata una prova di sostituzione completa dell’app su un telefono fisico in questa sessione.

## Instagram, Facebook e foto — 0.8.1

* 48 test Node riusciti: link diretti/condivisi, rifiuto di host ingannevoli, redirect nella stessa piattaforma, metadati Meta incompleti, controllo del file MP4 e pulizia in caso di durata o tracce non valide. Test foto per scelta del candidato maggiore, distinzione da copertine video, URL CDN, contenuto immagine, limite di dimensione e annullamento.
* Build Android, 9 test JVM e Lint locali riusciti con JDK 17. Test aggiunti per riconoscimento di Instagram/Facebook, URI condivisi e mancato riuso di un’analisi per un altro contenuto o dopo la scadenza di 90 secondi.
* App Electron reale: test d’avvio con sette piattaforme e nuova scelta delle foto, qualità selezionabile anche in modalità qualità massima, MP4/MP3 disabilitati per post fotografici.
* Prove online Windows con il motore incluso: MP4 Instagram e Facebook e JPEG Instagram trasferiti completamente e verificati con FFprobe. Dettagli e limiti in [PLATFORMS.md](PLATFORMS.md). Il link `share/` dello screenshot è stato risolto nel Reel di destinazione.
* Android su emulatore: aggiunti controlli UI per fonte e formato Meta, selezione di foto da fixture esplicitamente locale, MIME e salvataggio dell’immagine. La conversione nativa di prova usa un info-json, senza un secondo URL di estrazione. Gli esiti CI vengono verificati prima della pubblicazione.
* Verifica del tag `v0.8.1`: tutti i 5 test strumentati su Android 15 riusciti (70 secondi complessivi), inclusi i controlli precedenti di playlist e ascolto a schermo spento. Screenshot Instagram, Facebook e foto ispezionati e inclusi nella guida. Build native Windows, Linux, Mac Intel e Mac Apple Silicon riuscite nella stessa [esecuzione Actions](https://github.com/CosmoNetinfo/flusiva/actions/runs/46177213875).
* Esiti delle prove online, senza URL CDN o token temporanei: [JSON di verifica](evidence/meta-downloads-2026-11-28.json).
* Non è stata misurata una percentuale di accelerazione su un telefono fisico. Le prove online Windows non sostituiscono quelle su tutte le reti e i dispositivi Android.

## Interfaccia Android 0.7.1

* Compilazione locale e Android Lint senza errori; test JVM esistenti superati.
* Nuovi controlli su emulatore per riconoscimento della fonte YouTube, comando Scarica MP4 da YouTube, separazione tra Audio e Lettore e cambio delle icone Riproduci/Pausa dopo il tocco.
* Sul tag `v0.7.1` i 3 test strumentati su Android 15 sono riusciti, compresa la riproduzione a schermo spento. Verificati anche lettore vuoto, controllo Riproduci disabilitato e accesso alla libreria. Screenshot finali di Scarica, Lettore, Playlist, Audio e Impostazioni ispezionati e copiati in `docs/images`.
* Build e verifiche dei pacchetti Windows, Linux, Mac Intel e Mac Apple Silicon riuscite nella stessa esecuzione [GitHub Actions](https://github.com/CosmoNetinfo/flusiva/actions/runs/45563876545).
* Libreria virtualizzata con ListView e righe riutilizzate. I salvataggi delle playlist e le letture dei file avvengono fuori dal thread UI; cambiare una playlist non esegue una nuova scansione dei file.
* L’aggiornamento del cursore è locale alla pagina Lettore visibile. Informazioni del file e bande EQ vengono aggiornate quando cambiano; gli snapshot identici non vengono notificati di nuovo.
* Non è stato misurato un confronto di latenza su un telefono fisico. Il comportamento reale viene verificato su emulatore insieme a playlist e riproduzione a schermo spento.

## Lettore musicale Android 0.6.0

* 7 test JVM riusciti: ai 4 test Android precedenti si aggiungono validazione e ordinamento playlist, preset EQ e limiti del dispositivo, rappresentazione della durata.
* Android Lint senza errori e build APK riuscita. Media3 1.11.1 risolto e incluso nel pacchetto.
* Prima prova Actions su Android 15 riuscita: 3 test su emulatore, inclusi lettura dei dati MP3, persistenza e cancellazione delle playlist senza cancellazione dell’audio, WAV reale con 44.1 kHz/mono/16 bit, seek, pausa, passaggio al brano successivo, timer e gestione dell’equalizzatore.
* La prova ha rilasciato il controller della schermata, spento lo schermo e riconnesso un controller verificando che la riproduzione fosse ancora attiva e avanzata. Profilo dell’emulatore isolato.
* Le prove su cuffie Bluetooth, altri effetti audio e telefoni fisici rimangono da raccogliere. Non è stata misurata la qualità acustica dell’uscita.
* Verifica finale sul tag v0.6.0 riuscita: tutti i job di build e i test su emulatore hanno avuto esito positivo. La prova UI ha creato una playlist, aggiunto il file selezionato e avviato la riproduzione. Screenshot reali di lettore, EQ attivo e scelta dei brani ispezionati e inclusi nella guida.

## Estensione Android 0.5.0

* Compilazione locale dell’APK riuscita, Android Lint senza errori e 4 test JVM superati per URL, piattaforme, qualità e nomi file.
* Verificata la presenza delle architetture ARM64, ARMv7 e x86_64, minSdk 29 e targetSdk 36 nell’APK.
* GitHub Actions aggiunge test su emulatore Android 15 per motore nativo, QuickJS, FFmpeg/MP3 e MP4 con audio, MediaStore, annullamento del salvataggio, link condivisi e Impostazioni. Gli esiti della CI vengono verificati prima della pubblicazione.
* 41 test desktop superati anche dopo l’aggiunta dell’APK alla preparazione dei checksum della Release.
* I 2 test su emulatore Android 15 sono riusciti nelle Actions: avvio e link condiviso, preferenze, Python/yt-dlp 2026.11.23, QuickJS, MP4 con video/audio, estrazione MP3 con yt-dlp e FFmpeg, MediaStore e salvataggio annullato. Firma APK verificata con lo stesso certificato documentato per le installazioni future.
* Verifica ripetuta sul tag v0.5.0: test Android e firma APK riusciti. Screenshot reali scaricati dall’emulatore, ispezionati e inclusi in docs/images e nella guida Android.
* Le prove Android non dimostrano un download online riuscito da tutte le piattaforme su tutti i telefoni. Test sui dispositivi fisici e sulle reti degli utenti ancora necessari.

## Estensione 0.4.1

* Release v0.4.1 pubblicata dopo build riuscite su Windows x64, Linux x64, Mac Intel e Mac Apple Silicon. Su Linux un HTTP 504 del servizio di download yt-dlp ha richiesto la riesecuzione del job, poi completato.
* Verifica online con NsisUpdater reale e HTTP Electron: rilevata la Release ufficiale 0.4.1, scaricato l’installer Windows completo (189.327.957 byte), superata la verifica nativa SHA-512 e ricevuto l’evento update-downloaded. Cache e profilo isolati; installazione e riavvio disabilitati nel test. Esito locale in test-results/native-update.json.
* 41 test deterministici superati: 8 nuovi controlli per capacità dei sistemi, download automatico/manuale, verifica fallita, deduplicazione richieste, blocco con coda attiva, Debian e fallback Mac non firmato.
* Preparazione feed verificata con checksum dei file effettivi, entrambe le architetture Mac e selezione architettura della classe reale MacUpdater.
* Test Electron esteso ai nuovi controlli, lettura stato via IPC e rifiuto installazione quando non pronta. Il profilo di test disabilita gli aggiornamenti nativi.
* La CI verifica che ogni pacchetto includa electron-updater e il feed del repository ufficiale.
* L’installazione completa di un aggiornamento su macchine Windows, Linux e macOS degli utenti resta da verificare. Nessuna build macOS firmata disponibile in questa sessione; il fallback manuale è intenzionale.


## Estensione 0.3.0

* 33 test deterministici: includono serializzazione della coda, continuazione dopo errore, annullamento, pausa, tentativi, deduplicazione, limiti e validazione atomica dei batch, MP3 SoundCloud, dati di avanzamento mancanti/stimati, politica qualità massima e controllo sicuro delle Release stabili.
* Test Electron reale: batch inserito dal modulo della UI, lettura IPC, rifiuto dei link non ammessi, annullamento e nuovo tentativo, svuotamento e presenza dei controlli Aggiornamenti. Nessuna rete nei test CI; profilo isolato.
* Prova online: il confronto con l’ultima Release GitHub ha restituito correttamente 0.2.0 durante lo sviluppo della 0.3.0.
* Download reale del video Creative Commons di prova: rilevati byte, velocità, ETA e formato della traccia in 27 campioni; file temporanei rimossi.
* Selezione qualità massima senza download del video intero: risoluzione selezionata 2160p, uguale alla massima risoluzione disponibile.
* Download SoundCloud reale con conversione MP3: FFprobe ha verificato codec MP3 e bitrate in uscita 320.000 bit/s. Non è una misura della fedeltà della sorgente.
* TikTok e Vimeo conservano le limitazioni documentate in PLATFORMS.md. Spotify non viene implementato nella 0.3.0, come richiesto dal proprietario.
* Screenshot reali della coda con elementi in attesa e delle nuove Impostazioni, senza risultati di download simulati.

## Estensione 0.2.0

* 19 test deterministici: includono ora cinque piattaforme, redirect sicuri e limitati, immagini CDN, contenuti audio-only, preferenze persistenti dopo riapertura, aggiornamenti concorrenti, recupero da JSON corrotto e salvataggi senza sovrascrittura.
* Test nell’app Electron reale: apertura Impostazioni, elenco delle cinque piattaforme, lettura/scrittura preferenze via IPC, valori e percorsi non autorizzati rifiutati, ripristino. Profilo temporaneo isolato dalle preferenze dell’utente.
* Screenshot verificati dei temi chiaro e scuro e della schermata download. Salvati in `docs/images`.
* Prove online e limitazioni documentate separatamente in [PLATFORMS.md](PLATFORMS.md): nessuna dichiarazione di download riuscito per le piattaforme bloccate.
* Download reali verificati con il nuovo motore: MP4/MP3 YouTube, MP4 H.264 + AAC e MP3 Dailymotion, MP3 SoundCloud. Ogni output è stato verificato con FFprobe e rimosso con i temporanei. Dimensioni e limiti sono registrati nella valutazione piattaforme.
* La sandbox Windows locale ha impedito il rename atomico dei JSON nella propria cartella temporanea. La riesecuzione autorizzata dei test fuori da tale sandbox ha superato tutti i controlli; non è stato introdotto un salvataggio meno sicuro per aggirare il limite dell’ambiente.

GATE: ERROR — i controlli della skill grafica basati su Chrome non sono partiti nell’ambiente locale (`Chrome did not start` / `Chrome exited early`). Non è un esito positivo del controllo. La verifica visiva è stata eseguita separatamente nell’app Electron reale.

## Ambiente locale Windows

* Controllo sintassi e configurazione del renderer: `npm run check`.
* Nove test deterministici: URL YouTube e host non ammessi, formati, metadati, nomi file, processi, timeout, annullamento, progressi e pulizia dei temporanei.
* Avvio Electron reale in sandbox: bridge IPC, assenza di `require` nel renderer, presenza dei controlli, assenza di overflow orizzontale, eseguibilità dei tre strumenti e runtime Node incluso.
* Screenshot dell’app reale: `docs/images/flusiva-desktop.png`.
* Test online completato sul video Big Buck Bunny della Blender Foundation: MP4 di 46.284.688 byte con tracce AV1 + AAC; MP3 di 25.388.243 byte con traccia MP3. File temporanei rimossi al termine. I formati e le dimensioni possono cambiare con YouTube.
* Il vecchio video breve di prova di yt-dlp non era disponibile: non è stato considerato un test passato.

## CI e pacchetti

Il workflow testa ogni build con Electron e i binari della piattaforma prima di produrre gli installer. Su Ubuntu il runner configura proprietario e permessi del sandbox helper Chromium; la sandbox resta attiva.

La build 0.1.0 ha evidenziato anche un errore `UnknownVizError` durante `capturePage` nel display virtuale Linux. Da 0.1.1 gli screenshot sono un passaggio locale di documentazione, separato dal test CI: i controlli DOM, IPC e binari restano obbligatori su tutti i runner.

Le build macOS sono separate per Intel e Apple Silicon. Non è stata verificata l’installazione interattiva su macchine pulite macOS/Linux in questa sessione. La compilazione e il test d’avvio non sostituiscono questa verifica. Le prime build non sono firmate né notarizzate.

## Verifica visiva e scelte

App desktop a schermata singola: la richiesta di essenzialità prevale sulle sezioni di marketing e sugli effetti della skill. Palette originale della prima impostazione sostituita con blu/azzurro/bianco su richiesta esplicita del proprietario. I puntini sono statici; l’ingresso iniziale rispetta `prefers-reduced-motion`. Nessuna fotografia, recensione, statistica o prova sociale inventata. Il marchio è originale e le alternative sono in `docs/BRAND.md`.

## Verifiche manuali consigliate dopo l’installazione

1. Installare il pacchetto corretto su una macchina pulita.
2. Scaricare MP4 con audio e MP3 da un video autorizzato.
3. Provare annullamento, scelta di una cartella non scrivibile e un video non disponibile.
4. Verificare sostituzione di un file esistente e apertura della cartella.
5. Controllare consumo e pulizia dello spazio temporaneo.
