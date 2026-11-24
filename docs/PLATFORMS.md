# Valutazione delle piattaforme — 0.2.0

Studio e implementazione del 24 novembre 2026. Sono abilitati cinque servizi, con un modello di metadati comune e processi locali yt-dlp/FFmpeg. La presenza di un estrattore non garantisce il funzionamento di tutti i link: [avvertenza upstream ed elenco ufficiale](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

| Servizio | Implementazione Flusiva | Prova online in questa sessione | Limite |
| --- | --- | --- | --- |
| YouTube | Video/Shorts/Music, MP4/MP3 | Metadati riusciti; download MP4 e MP3 reali verificati nella prima versione | Blocchi bot, restrizioni del contenuto e cambi upstream |
| Dailymotion | Video ed embed, `dai.ly`, MP4/MP3 | Metadati riusciti, presenza di tracce video e audio | Download completo non verificato nella nuova integrazione |
| SoundCloud | Tracce pubbliche e link brevi, MP3 | Metadati riusciti, audio disponibile e video assente | Nessun accesso a tracce private; download completo non verificato |
| TikTok | Video singoli, link brevi con redirect controllati, MP4/MP3 | Due contenuti pubblici hanno ricevuto una limitazione/richiesta di accesso | Non è dichiarato un download riuscito; restrizioni dipendenti da rete e contenuto |
| Vimeo | Video pubblici e URL player, MP4/MP3 | Un vecchio video di test è stato rimosso; un secondo link pubblico ha richiesto accesso | Niente password, contenuti riservati o link con token privati |

Le sonde leggono solo i metadati. Non equiparano la presenza dei formati a un trasferimento del file completato. I test deterministici verificano invece selezione del formato, rifiuto delle playlist, nomi portabili, errori, annullamento, pulizia e separazione dei servizi con fixture controllate.

## Perché queste piattaforme

TikTok è stato richiesto nella conversazione. Vimeo e Dailymotion estendono il flusso video; SoundCloud copre il caso solo audio. Tutti hanno un estrattore nel motore già distribuito. Le relative implementazioni ufficiali sono consultabili in [TikTok](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/tiktok.py), [Vimeo](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/vimeo.py), [Dailymotion](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/dailymotion.py) e [SoundCloud](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/soundcloud.py).

Instagram resta da valutare con prove specifiche e gestione delle restrizioni. Non sono aggiunti login, lettura di cookie dal browser, contenuti a pagamento o DRM.

## Scelte tecniche

* Host e percorsi espliciti per contenuti singoli; niente URL generici verso siti arbitrari.
* Link brevi: redirect manuali, massimo sei passaggi, timeout e annullamento; ogni destinazione deve restare nella stessa piattaforma.
* SoundCloud: MP4 rifiutato anche dal processo principale, non soltanto disabilitato nella UI.
* Metadati comuni: piattaforma, titolo, autore, durata, formati audio/video e risoluzioni. Le playlist e i formati dichiarati DRM vengono esclusi.
* Immagini: URL HTTPS da un elenco di CDN verificato; fallback al marchio Flusiva per copertine mancanti o errori.
* MP4: preferenza per tracce MP4/M4A, con fallback e remux del contenitore tramite FFmpeg. Non viene promesso un aumento della qualità né una ricodifica a H.264.
* Se un servizio limita la richiesta viene mostrato il relativo errore; l’app non comunica un successo simulato.

## Riprodurre le verifiche

`npm test` per le verifiche deterministiche. `node scripts/platform-smoke.cjs` per sonde online dei metadati; i risultati sono in `test-results/platform-probes.json`. `node scripts/online-smoke.cjs URL` per un download completo MP4 e MP3 di un contenuto video autorizzato. Le prove online dipendono dallo stato dei servizi e non fanno parte della CI.
