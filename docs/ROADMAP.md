# Prossime versioni di Flusiva

Valutazione del 24 novembre 2026. Questa è una proposta di sviluppo: la versione 0.1.0 abilita soltanto YouTube.

## Prima di aggiungere piattaforme

| Priorità | Funzione | Perché | Criterio di completamento |
| --- | --- | --- | --- |
| 1 | Aggiornamento dell’app con verifica delle firme | Gli estrattori cambiano con le piattaforme | Aggiornamento verificabile, rollback e comportamento chiaro senza rete |
| 1 | Firma Windows e firma/notarizzazione macOS | Riduce gli ostacoli all’installazione | Certificati del proprietario configurati come secret, verifica su macchine pulite |
| 1 | Test dei download su tre sistemi | I test automatici con fixture non verificano YouTube | MP4 con audio, MP3, annullamento e disco pieno verificati su ogni OS |
| 2 | Cartella preferita e qualità ricordata | Riduce i clic sui download successivi | Piccolo file JSON locale, senza account o database |
| 2 | Coda di download e ripresa | Utile per più link e interruzioni di rete | Una conversione per volta, spazio su disco controllato, stato comprensibile |
| 2 | Velocità e tempo stimato | Rende chiara l’attesa | Valori reali dal processo, nessuna percentuale simulata durante la conversione |
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

Quest’ordine è una valutazione progettuale, non una misura comparativa dell’affidabilità: le nuove piattaforme non sono state ancora provate dentro Flusiva. Nessuna promessa per contenuti privati, DRM o accessi a pagamento.

## Come estendere il codice

Separare il contratto piattaforma (`match`, `normalize`, `summarize`, `downloadOptions`) dal motore di processo. L’adattatore attuale è `core/youtube.cjs`. Ogni piattaforma deve avere una allowlist dei domini, redirect verificati, test dei metadati e messaggi di errore propri. Il renderer riceverà un modello comune, con `platform`, `title`, `duration`, `formats` e `thumbnail`.

Per TikTok i link abbreviati richiederanno risoluzione controllata prima del download. Non basta togliere il controllo sul dominio: oggi la normalizzazione YouTube evita URL arbitrari e comandi interpretati dalla shell.

## Sequenza consigliata

**0.1.x:** stabilità, firme, spazio su disco, qualità e UX dei download. **0.2:** TikTok pubblico dopo prove su Windows/Linux/macOS. **0.3:** Vimeo e SoundCloud. Valutare Dailymotion e Instagram dopo aver misurato il costo di manutenzione. Nessuna data promessa prima delle prove tecniche.
