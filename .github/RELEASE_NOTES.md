**Flusiva 0.4.1** implementa gli aggiornamenti dell’app: controllo, download verificato e installazione. Download multimediali e conversioni continuano a svolgersi sul computer, senza account o server Flusiva. Codice GPL-3.0-only.

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
