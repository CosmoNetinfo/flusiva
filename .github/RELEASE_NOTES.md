**Flusiva 0.3.0** aggiunge coda di download, velocità e tempo rimanente, qualità massima e controllo delle nuove versioni. Download e conversione avvengono sul computer: nessun server, account o database. Codice open source con licenza GPL v3.

### Scegli il download

| Sistema | Pacchetto |
| --- | --- |
| Windows 10/11, PC Intel/AMD a 64 bit | `win-x64.exe` |
| Linux Intel/AMD a 64 bit | `linux-x86_64.AppImage` oppure `linux-amd64.deb` |
| macOS con processore Intel | `mac-x64.dmg` oppure `.zip` |
| macOS con chip Apple Silicon | `mac-arm64.dmg` oppure `.zip` |

Su macOS apri il DMG e trascina l'app in Applicazioni. Su Linux rendi eseguibile l’AppImage, oppure installa il pacchetto Debian/Ubuntu. Le distribuzioni includono yt-dlp, FFmpeg e FFprobe: non serve installarli a parte.

### Cosa contiene

* Coda con più link: un file alla volta, pausa dopo quello attuale, annullamento, tentativi individuali e rimozione dei file terminati.
* Byte, dimensione, velocità e tempo rimanente reali per la traccia corrente. Nessuna stima inventata durante la conversione.
* Qualità massima attiva inizialmente: migliore sorgente video/audio disponibile e conversione MP3 a 320 kbps. Disattivabile nelle Impostazioni.
* Controllo delle Release ufficiali GitHub, manuale o facoltativo all’avvio. L’installazione rimane manuale.

* YouTube, TikTok, Vimeo e Dailymotion per video MP4 o MP3; SoundCloud per tracce MP3.
* Link brevi TikTok e SoundCloud con destinazioni verificate.
* Impostazioni: cartella, formato e qualità predefiniti, richiesta di destinazione, apertura della cartella al termine e ripristino.
* Tema chiaro, scuro o di sistema; preferenze locali persistenti, senza database.
* Salvataggio automatico con nomi univoci, senza sovrascrivere i file esistenti.
* MP4 con video e audio uniti alla migliore qualità disponibile; MP3 fino a 320 kbps.
* Anteprima, selezione della qualità, cartella di destinazione, avanzamento e annullamento.
* Nuovo logo puntinato e interfaccia in blu, azzurro e bianco.
* Pulizia dei temporanei e apertura della cartella a download completato.

### Prima di installare

La release è **senza firma digitale e senza notarizzazione Apple**. Il sistema può segnalare un editore non verificato. Verifica la provenienza dalla repository ufficiale e, se necessario, confronta il file con `SHA256SUMS.txt`. Non vengono installati aggiornamenti automaticamente. La coda vive solo nella sessione corrente.

Le piattaforme possono limitare alcuni contenuti. Nelle prove di questa versione YouTube, Dailymotion e SoundCloud hanno restituito metadati corretti; TikTok e Vimeo hanno limitato le richieste o richiesto accesso. Il loro supporto è implementato ma non garantito su ogni rete o contenuto. [Dettaglio delle prove](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/PLATFORMS.md).

Dirette in corso, playlist, video privati e contenuti che richiedono login non sono supportati. La qualità dipende dalla sorgente: convertire a 320 kbps non migliora l’audio originale. Usare contenuti propri o autorizzati. Instagram resta nella roadmap.

Spotify è stato rimandato su richiesta del proprietario: nessun download MP3 da Spotify o sostituzione automatica della fonte. Il progetto include licenza GPL-3.0-only e la sezione [Uso responsabile](https://github.com/CosmoNetinfo/flusiva#uso-responsabile).

Per dettagli, sviluppo e risoluzione dei problemi: [README](https://github.com/CosmoNetinfo/flusiva#readme). [Studio del logo](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/BRAND.md), [roadmap](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/ROADMAP.md) e [segnalazioni](https://github.com/CosmoNetinfo/flusiva/issues).
