**Flusiva 0.8.0** aggiunge MP4 da Instagram e Facebook e foto dei post Instagram. Download sul dispositivo, senza server Flusiva. GPL-3.0-only.

### Instagram e Facebook

* Video e Reel pubblici, con fonte riconosciuta e comando esplicito per MP4.
* Link Instagram `share/...`, Facebook `share/v/`, `share/r/` e `fb.watch`, oltre ai link diretti.
* Foto Instagram in JPEG, PNG o WebP senza ricompressione. Nei caroselli si sceglie una foto per download; le copertine video non sono presentate come foto.
* Durata e codec mancanti non fanno scartare un video valido: le tracce MP4 e il limite di 6 ore vengono verificati sul file.

### Tempi Android

* Preparazione del motore in background all’apertura.
* Riuso dell’analisi dello stesso contenuto per 90 secondi, per evitare una seconda estrazione all’avvio del download.
* Fino a quattro frammenti paralleli per i flussi segmentati. Velocità effettiva dipendente da rete e piattaforma; nessuna percentuale di accelerazione dichiarata.

### Installazione

**Android 10+: Flusiva-0.8.0-android.apk**. APK universale ARM64, ARMv7 e x86_64, firmato con la stessa chiave: aggiornare senza disinstallare per conservare playlist e preferenze.

Installer inclusi per Windows x64, Linux x64, Mac Intel e Apple Silicon. Windows/Linux mantengono gli aggiornamenti compatibili; su Mac non firmato il controllo apre il download manuale della Release.

Restano disponibili lettore musicale Android, playlist, equalizzatore, ascolto a schermo spento e download dalle altre cinque piattaforme. Spotify resta rimandato. I contenuti che richiedono login o abbonamenti non vengono sbloccati.

**Verifiche online:** MP4 Instagram 720×1280, MP4 Facebook H.264 + AAC e foto Instagram 1080×1350 scaricati e verificati con FFprobe. Non è una garanzia per ogni link o rete. Usare contenuti propri o autorizzati.

[Guida e piattaforme](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/PLATFORMS.md) · [Guida Android](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/ANDROID.md).
