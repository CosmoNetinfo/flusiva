**Flusiva 0.7.1** ridisegna Android con pagine distinte per download, lettore, playlist e audio. Comandi musicali più compatti e risposta diretta agli eventi del lettore. Download e conversioni restano sul dispositivo. Codice GPL-3.0-only.

### Nuova navigazione Android

* **Scarica**: fonte riconosciuta dal link e formato esplicito, ad esempio **Scarica MP3 da YouTube**.
* **Musica → Lettore**: riproduzione, posizione, precedente/successivo, casuale, ripetizione e timer, con icone e pulsante centrale.
* **Musica → Playlist**: libreria, importazione e playlist persistenti in una pagina dedicata.
* **Musica → Audio**: qualità del file, preset e bande dell’equalizzatore, separati dai controlli del lettore.
* Pulsanti piatti con feedback al tocco e stati disabilitati; lista brani con righe riutilizzate e salvataggi playlist in background.

### Musica e playlist su Android

* Apri **Musica e playlist** o scegli **Ascolta in Flusiva** dopo un download.
* Crea, rinomina e modifica playlist dei file scaricati o importati. Riordina i brani tenendoli premuti; rimuovere un riferimento non elimina il file.
* Continua l’ascolto con lo schermo spento, usando i controlli della notifica o delle cuffie. Posizione, precedente/successivo, casuale, ripetizione e timer.
* Visualizza codec, bitrate audio, frequenza, canali e decoder. I dati assenti non vengono inventati; non sono una misura della qualità della sorgente o dell’uscita Bluetooth.
* Equalizzatore legato alla sessione Flusiva: bande reali del dispositivo, preset e regolazione manuale. Se il dispositivo non lo supporta i controlli vengono disabilitati con una spiegazione.

Le nuove funzioni musicali sono **per Android**. Gli installer Windows, Linux e Mac restano inclusi nella Release. [Guida musica e playlist](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/MUSIC.md).

### Android

Scarica **Flusiva-0.7.1-android.apk** per Android 10 o successivi. APK universale ARM64, ARMv7 e x86_64, firmato con la stessa chiave della versione precedente: aggiorna senza disinstallare per conservare preferenze e playlist.

* Incolla il link o usa **Condividi → Flusiva** da YouTube e dalle altre piattaforme supportate.
* Analizza il contenuto e scarica video MP4 o audio MP3, con qualità massima o personalizzata.
* I file finiscono in **Download/Flusiva**, pronti da aprire o condividere. Python, yt-dlp, QuickJS e FFmpeg sono inclusi.
* Notifica di avanzamento, annullamento, preferenze e controllo delle nuove versioni.
* Android richiede la conferma per installare e aggiornare l’APK. La prima versione Android gestisce un file alla volta, con tema chiaro.

[Guida Android](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/ANDROID.md). Nessuna pubblicazione sul Play Store.

### Aggiornamenti automatici

* Controllo dopo l’avvio e ogni 6 ore, download con avanzamento e verifica SHA-512 dal repository ufficiale.
* Windows e Linux AppImage: pulsante **Riavvia e installa** oppure installazione alla chiusura normale con automatismo attivo e nessun lavoro pendente.
* Debian/Ubuntu: download automatico, installazione richiesta dal pulsante con eventuale autorizzazione del sistema.
* Nessuna installazione mentre download o coda hanno lavori da completare. Preferenza per disattivare l’automatismo.
* macOS: percorso automatico predisposto per build firmate Developer ID. **Questa release non è firmata: su Mac l’aggiornamento resta manuale.**

**Se usi 0.3 o precedenti, installa 0.4 manualmente una volta.** Le versioni precedenti non contengono il sistema di installazione degli aggiornamenti.

### Scegli il download

| Sistema | Pacchetto |
| --- | --- |
| Windows 10/11 x64 | win-x64.exe |
| Linux x64 | linux-x86_64.AppImage oppure linux-amd64.deb |
| macOS Intel | mac-x64.dmg oppure .zip |
| macOS Apple Silicon | mac-arm64.dmg oppure .zip |

Apri il DMG e trascina Flusiva in Applicazioni. Su Linux rendi eseguibile l’AppImage originale da una cartella scrivibile oppure installa il pacchetto Debian. yt-dlp, FFmpeg e FFprobe sono inclusi.

Le build attuali sono senza firma dell’editore e senza notarizzazione Apple: il sistema può mostrare avvisi. Verifica provenienza e SHA256SUMS.txt. I checksum controllano l’integrità e non sostituiscono la firma del codice.

Restano disponibili YouTube, TikTok, Vimeo, Dailymotion e SoundCloud, coda, preferenze, qualità massima e MP3 fino a 320 kbps. La qualità dipende dalla sorgente. Nelle prove precedenti YouTube, Dailymotion e SoundCloud hanno funzionato; TikTok e Vimeo hanno limitato le richieste o richiesto accesso. Spotify resta rimandato. Usare contenuti propri o autorizzati.

41 test deterministici e test di avvio Electron sui runner nativi. L’installazione completa di un aggiornamento su tutti i sistemi degli utenti resta da verificare.

[README](https://github.com/CosmoNetinfo/flusiva#readme) · [Aggiornamenti e firma](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/UPDATES.md) · [Uso responsabile](https://github.com/CosmoNetinfo/flusiva#uso-responsabile) · [Segnalazioni](https://github.com/CosmoNetinfo/flusiva/issues).
