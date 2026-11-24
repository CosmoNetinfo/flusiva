# Prossime versioni di Flusiva

**Android 0.5:** app nativa implementata, con link condivisi, download MP4/MP3, qualità massima, notifiche, salvataggio MediaStore e Impostazioni. Restano da estendere su Android coda, temi e ripresa dei file parziali; prove su dispositivi fisici da raccogliere. [Guida Android](ANDROID.md).

Valutazione del 24 novembre 2026, aggiornata per 0.4.0. YouTube, TikTok, Vimeo, Dailymotion e SoundCloud sono implementati. La 0.3 aggiunge coda seriale, dati del trasferimento, qualità massima e controllo delle nuove versioni. [Esiti reali e limiti delle prove](PLATFORMS.md). Spotify è stato rimandato su richiesta del proprietario.

## Prima di aggiungere piattaforme

| Priorità | Funzione | Perché | Criterio di completamento |
| --- | --- | --- | --- |
| Completata in 0.4, firma da configurare | Aggiornamento dell’app | Gli estrattori cambiano con le piattaforme | Windows/AppImage automatici, Debian esplicito; SHA-512 e feed ufficiale. macOS automatico solo con Developer ID. Rollback non implementato |
| 1 | Firma Windows e firma/notarizzazione macOS | Riduce gli ostacoli all’installazione | Certificati del proprietario configurati come secret, verifica su macchine pulite |
| 1 | Test dei download su tre sistemi | I test automatici con fixture non verificano YouTube | MP4 con audio, MP3, annullamento e disco pieno verificati su ogni OS |
| Completata in 0.2 | Cartella preferita, formato e qualità ricordati | Riduce i clic sui download successivi | File JSON locale atomico, pagina Impostazioni, temi chiaro/scuro e ripristino |
| Completata in 0.3 | Coda e tentativi individuali | Utile per più link | Un file per volta, pausa dopo quello attuale, annullamento e tentativi dall’inizio |
| Completata in 0.3 | Velocità e tempo stimato | Rende chiara l’attesa | Valori reali dal processo, nessuna percentuale simulata durante la conversione |
| Completata in 0.3 | Controllo delle nuove versioni | Rende disponibili gli aggiornamenti del motore tramite il nuovo installer | Release ufficiali stabili, controllo manuale o facoltativo all’avvio; installazione manuale |
| Completata in 0.3 | Qualità massima | Cerca la migliore sorgente esposta dal servizio | Selezione video migliore senza preferenza per MP4 inferiori; MP3 a 320 kbps senza promesse di qualità superiore alla sorgente |
| 3 | Formato audio originale M4A/Opus | Evita una ricodifica quando MP3 non serve | Formato e compatibilità spiegati prima del download |
| 3 | Sottotitoli e intervallo temporale | Consente di salvare solo la parte utile | Selezione dei sottotitoli disponibili e taglio con durata verificata |

## Piattaforme candidate

yt-dlp include estrattori per TikTok, Vimeo, SoundCloud, Dailymotion e Instagram, ma la presenza nell’elenco **non garantisce** che ogni contenuto sia scaricabile. La documentazione upstream invita a verificare i singoli casi perché i siti cambiano. [Elenco ufficiale degli estrattori](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

| Ordine proposto | Piattaforma | Ambito iniziale | Motivazione | Rischi da verificare |
| --- | --- | --- | --- | --- |
| 1 | TikTok | Un singolo video pubblico; MP4 oppure MP3 | È la prima estensione richiesta dal proprietario | Link abbreviati e redirect, restrizioni geografiche, disponibilità dell’audio; niente promessa di rimozione watermark |
| 2 | Vimeo | Video pubblici che l’utente è autorizzato a scaricare | Aggiunge video di autori e studi | Password, restrizioni dell’autore, formati audio/video disponibili |
| 3 | SoundCloud | Singole tracce pubbliche autorizzate | Completa il caso d’uso audio | Non aumentare artificialmente la qualità; distinguere sorgenti MP3/AAC/Opus |
| 4 | Dailymotion | Un singolo video pubblico | Estensione del flusso MP4 esistente | Formati e restrizioni di accesso |
| 5 | Instagram | Video/Reel pubblici singoli | Richiesta plausibile per i contenuti social | Login, rate limit e frequenti cambiamenti; manutenzione da valutare prima di promettere supporto |

Le prime quattro estensioni sono implementate in 0.2.0. Le sonde online hanno confermato i metadati di Dailymotion e SoundCloud; TikTok e Vimeo hanno limitato le richieste. Instagram resta una proposta. Nessuna promessa per contenuti privati, DRM o accessi a pagamento.

## Come estendere il codice

Separare il contratto piattaforma (`match`, `normalize`, `summarize`, `downloadOptions`) dal motore di processo. L’adattatore attuale è `core/youtube.cjs`. Ogni piattaforma deve avere una allowlist dei domini, redirect verificati, test dei metadati e messaggi di errore propri. Il renderer riceverà un modello comune, con `platform`, `title`, `duration`, `formats` e `thumbnail`.

Per TikTok i link abbreviati richiederanno risoluzione controllata prima del download. Non basta togliere il controllo sul dominio: oggi la normalizzazione YouTube evita URL arbitrari e comandi interpretati dalla shell.

## Sequenza consigliata

**0.3:** coda, avanzamento, qualità massima e controllo delle versioni completati. **Prossimo ciclo:** prove sulle reti degli utenti, affidabilità degli estrattori, certificati di firma e gestione dello spazio. **Poi:** ripresa di file parziali, coda persistente facoltativa e valutazione Instagram. Nessuna data promessa prima delle prove tecniche. Spotify rimane fuori dal piano attuale.
