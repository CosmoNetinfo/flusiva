**Flusiva 0.5.0** arriva su Android. Download e conversioni avvengono sul telefono o sul computer, senza account o server Flusiva. Codice GPL-3.0-only.

### Android

Scarica **Flusiva-0.5.0-android.apk** per Android 10 o successivi. APK universale ARM64, ARMv7 e x86_64, firmato con la chiave gratuita del progetto.

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
