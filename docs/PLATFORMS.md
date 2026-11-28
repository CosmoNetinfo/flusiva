# Valutazione delle piattaforme — 0.8.1

Aggiornamento del 28 novembre 2026: sette servizi con metadati comuni e processi locali yt-dlp/FFmpeg. Le prove delle cinque piattaforme iniziali risalgono al 24 novembre; Instagram e Facebook sono stati provati il 28 novembre. La presenza di un estrattore non garantisce il funzionamento di tutti i link: [avvertenza upstream ed elenco ufficiale](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

| Servizio | Implementazione Flusiva | Prova online in questa sessione | Limite |
| --- | --- | --- | --- |
| YouTube | Video/Shorts/Music, MP4/MP3 | Download MP4 e MP3 riusciti e nuovamente verificati con il motore 0.2.0 | Blocchi bot, restrizioni del contenuto e cambi upstream |
| Dailymotion | Video ed embed, `dai.ly`, MP4/MP3 | MP4 da 10.893.232 byte con H.264 + AAC; MP3 da 4.483.378 byte; tracce verificate e temporanei rimossi | Prova su un contenuto pubblico; nessuna garanzia per tutti i link |
| SoundCloud | Tracce pubbliche e link brevi, MP3 | MP3 da 3.439.774 byte, traccia verificata e temporanei rimossi | Nessun accesso a tracce private; prova su una singola traccia pubblica |
| TikTok | Video singoli, link brevi con redirect controllati, MP4/MP3 | Due contenuti pubblici hanno ricevuto una limitazione/richiesta di accesso | Non è dichiarato un download riuscito; restrizioni dipendenti da rete e contenuto |
| Vimeo | Video pubblici e URL player, MP4/MP3 | Un vecchio video di test è stato rimosso; un secondo link pubblico ha richiesto accesso | Niente password, contenuti riservati o link con token privati |
| Instagram | Reel, post video, foto e link `share/` | MP4 H.264 da 1.950.833 byte, 720×1280, circa 5 s, senza traccia audio nella sorgente; JPEG originale da 383.468 byte, 1080×1350. Verifica FFprobe e pulizia completate | Nessun login; i caroselli video non sono supportati, quelli fotografici permettono una foto per download |
| Facebook | Video, Reel, `watch?v=`, URL mobili, `share/v/`, `share/r/`, `fb.watch` | MP4 da 4.541.117 byte, H.264 400×300 + AAC, 131 s; tracce e durata verificate, temporanei rimossi | Risoluzione massima esposta dal contenuto di prova; non garantisce HD per ogni link |

Le sonde `platform-smoke` leggono solo i metadati e non equiparano la presenza dei formati a un trasferimento completato. Separatamente sono stati eseguiti i download completi di YouTube, Dailymotion e SoundCloud indicati nella tabella. I test deterministici verificano selezione del formato, rifiuto delle playlist, nomi portabili, errori, annullamento, pulizia e separazione dei servizi con fixture controllate.

## Perché queste piattaforme

TikTok è stato richiesto nella conversazione. Vimeo e Dailymotion estendono il flusso video; SoundCloud copre il caso solo audio. Tutti hanno un estrattore nel motore già distribuito. Le relative implementazioni ufficiali sono consultabili in [TikTok](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/tiktok.py), [Vimeo](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/vimeo.py), [Dailymotion](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/dailymotion.py) e [SoundCloud](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/soundcloud.py).

Instagram e Facebook sono implementati dalla 0.8 con gli estrattori ufficiali [Instagram](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/instagram.py) e [Facebook](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/facebook.py). Un piccolo plugin GPL incluso conserva il tipo del contenuto Instagram dichiarato dall’API: soltanto gli elementi `image` diventano foto scaricabili, mai la copertina di un video. Non sono aggiunti login, importazione di cookie dal browser, contenuti a pagamento o DRM.

Il link dello screenshot `instagram.com/share/BAWVJE-rtk` è stato risolto online nel corrispondente Reel. La risoluzione del link è distinta dal download completo; i trasferimenti verificati nella tabella usano contenuti di prova pubblici degli estrattori upstream.

## Scelte tecniche

* Host e percorsi espliciti per contenuti singoli; niente URL generici verso siti arbitrari.
* Link brevi: redirect manuali, massimo sei passaggi, timeout e annullamento; ogni destinazione deve restare nella stessa piattaforma.
* SoundCloud: MP4 rifiutato anche dal processo principale, non soltanto disabilitato nella UI.
* Metadati comuni: piattaforma, titolo, autore, durata, formati audio/video e risoluzioni. Le playlist e i formati dichiarati DRM vengono esclusi.
* Immagini: URL HTTPS da un elenco di CDN verificato; fallback al marchio Flusiva per copertine mancanti o errori.
* MP4: migliore sorgente video/audio disponibile, con fallback e remux del contenitore tramite FFmpeg. Non viene promesso un aumento della qualità né una ricodifica a H.264.
* Meta può omettere durata o codec: il tipo `video_ext` permette di riconoscere la traccia senza inventare il codec. Se l’audio non è dichiarato non viene promesso MP3. Un MP4 Meta è accettato soltanto dopo verifica delle tracce e della durata sul file.
* Foto Instagram: migliore candidato fornito dal servizio, fino a 20 elementi e 50 MiB ciascuno; JPEG/PNG/WebP mantenuti senza ricompressione. Redirect limitati ai CDN HTTPS di Meta, validazione del file prima del salvataggio. Nei caroselli misti si elencano soltanto le foto; i video non vengono convertiti in immagini.
* Se un servizio limita la richiesta viene mostrato il relativo errore; l’app non comunica un successo simulato.

## Riprodurre le verifiche

`npm test` per le verifiche deterministiche. `node scripts/platform-smoke.cjs` per sonde online dei metadati; i risultati sono in `test-results/platform-probes.json`. `node scripts/online-smoke.cjs URL` per un download completo MP4 e MP3 di un contenuto video autorizzato, o solo MP3 se il contenuto è audio. Le prove online dipendono dallo stato dei servizi e non fanno parte della CI.

Per ripetere le tre prove Meta: `node scripts/platform-smoke.cjs instagram facebook instagram-photo --download-mp4`. Il comando salva gli esiti in `test-results/platform-downloads.json`, verifica i file con FFprobe e rimuove i temporanei. Il parametro storico `--download-mp4` scarica il file immagine originale quando la sonda è `instagram-photo`.
