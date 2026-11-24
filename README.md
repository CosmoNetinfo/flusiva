# Flusiva

<img src="docs/brand/flusiva-wordmark.svg" alt="Flusiva" width="250">

App desktop per scaricare **video MP4 e audio MP3 da YouTube, TikTok, Vimeo, Dailymotion e SoundCloud**, con interfaccia in italiano. Windows, Linux e macOS (Intel e Apple Silicon). Il computer dell'utente esegue download e conversione: nessun server remoto, account, hosting o database.

Flusiva è gratuito e open source, distribuito con licenza [GNU GPL v3](LICENSE). La versione 0.4 aggiunge download e installazione degli aggiornamenti dell’app, con avanzamento e protezione dei download in corso.

![Interfaccia desktop di Flusiva](docs/images/flusiva-desktop.png)

## Scaricare l'app

Gli installer sono nella sezione [Releases](https://github.com/CosmoNetinfo/flusiva/releases). Dopo ogni push su `main`, GitHub Actions genera anche gli artifact scaricabili dalla scheda [Actions](https://github.com/CosmoNetinfo/flusiva/actions).

| Sistema | File |
| --- | --- |
| Windows 10/11 x64 | Installer `.exe` |
| Linux x64 | `.AppImage` oppure pacchetto Debian/Ubuntu `.deb` |
| macOS Intel | `.dmg` / `.zip` con `x64` nel nome |
| macOS Apple Silicon | `.dmg` / `.zip` con `arm64` nel nome |

Le prime build non sono firmate né notarizzate. Windows e macOS possono mostrare avvisi sull'editore. Una distribuzione con firma richiede certificati del proprietario. **Da 0.3 o precedenti occorre installare manualmente la 0.4 una volta:** il sistema di aggiornamento è incluso dalla 0.4. Gli aggiornamenti includono anche gli strumenti distribuiti con l’app.

Su Windows eseguire l’installer e scegliere la cartella. Su macOS aprire il DMG e trascinare Flusiva in Applicazioni; selezionare la variante adatta in **Informazioni su questo Mac**. Le build sono separate: `arm64` per chip Apple, `x64` per Intel. Verificare l’origine dalla repository ufficiale e il checksum della release prima di autorizzare un’app non firmata. Requisiti macOS ed Electron: [supporto delle piattaforme](https://www.electronjs.org/docs/latest/tutorial/support#supported-platforms).

Su Linux: rendere eseguibile l'AppImage (`chmod +x Flusiva-*.AppImage`) e avviarla; se FUSE non è disponibile usare il `.deb` o `--appimage-extract-and-run`. Non eseguire l'app come root e non disabilitare la sandbox.

## Utilizzo

1. Incollare il link di un singolo video o di una traccia e scegliere **Analizza il link**.
2. Selezionare **Video MP4** o **Solo audio MP3** e la qualità.
3. Premere **Scarica** e scegliere dove salvare il file.

Video: migliore sorgente video/audio disponibile, unita o rimuxata in MP4 senza ricodifica del video. Risoluzioni fino a 8K se fornite dalla sorgente; i codec più recenti possono richiedere un lettore aggiornato. MP3: 320 kbps in modalità qualità massima, oppure 128, 192, 256 o 320 kbps con scelta manuale. La conversione MP3 è con perdita e non può aumentare la qualità della sorgente.

Un download alla volta, durata massima 6 ore, limite 8 GiB per singola traccia, timeout 2 ore. I file vengono preparati nello spazio temporaneo del sistema, copiati alla destinazione e rimossi al termine, in caso di errore o annullamento. Serve spazio per tracce, file finale e copia di salvataggio. Una chiusura forzata del processo può lasciare una cartella `flusiva-*` nella directory temporanea: si può rimuovere quando l'app è chiusa.

Dirette in corso/programmate, playlist, video privati e contenuti che richiedono login non sono supportati. Le piattaforme possono cambiare o bloccare le richieste: nessuna app può garantire il download di ogni contenuto. Usare contenuti propri o autorizzati.

## Piattaforme

| Piattaforma | Link accettati | Output |
| --- | --- | --- |
| YouTube | Video, Shorts, YouTube Music, `youtu.be` | MP4 e MP3 |
| TikTok | Singolo video pubblico, `vm.tiktok.com`, `vt.tiktok.com`, `tiktok.com/t/` | MP4 e MP3 |
| Vimeo | `vimeo.com/ID` e `player.vimeo.com/video/ID` pubblici | MP4 e MP3 |
| Dailymotion | Singolo video, embed e `dai.ly` | MP4 e MP3 |
| SoundCloud | Singola traccia pubblica e link brevi `on.soundcloud.com` | MP3 |

Per SoundCloud viene selezionato MP3 e MP4 resta disabilitato. I link brevi sono risolti con controllo di ogni redirect, della piattaforma e del numero di passaggi. Le copertine sono accettate soltanto dai CDN delle piattaforme; se non disponibili viene mostrato il simbolo Flusiva.

**Disponibilità:** nelle prove online del 24 novembre 2026 sono riusciti i download MP4 e MP3 da YouTube e Dailymotion e il download MP3 da SoundCloud, con tracce verificate tramite FFprobe e pulizia dei temporanei. Le richieste di prova a TikTok e Vimeo sono state limitate o hanno richiesto accesso: il loro supporto è implementato, ma non è stato verificato un download riuscito in questa sessione. Il funzionamento dipende da contenuto, rete e restrizioni della piattaforma. [Valutazione e verifiche](docs/PLATFORMS.md).

## Impostazioni

Aprire **Impostazioni** nella barra superiore. Le modifiche entrano in vigore con **Salva preferenze**; il tema viene mostrato in anteprima mentre lo si sceglie.

* **Cartella dei download:** selettore nativo del sistema; nessun percorso deve essere digitato.
* **Chiedi sempre dove salvare:** attivo per impostazione iniziale. Disattivandolo, l'app usa la cartella scelta e aggiunge `(1)`, `(2)` e così via ai nomi già presenti, senza sovrascriverli.
* **Apri la cartella al termine:** facoltativo, mostra il file appena pronto.
* **Formato e qualità predefiniti:** MP4 o MP3, risoluzione massima e bitrate MP3. Il singolo download può usare una scelta diversa.
* **Usa sempre la qualità massima:** attivo inizialmente, prevale sui limiti manuali. Video alla migliore risoluzione e frequenza disponibile; per MP3 si cerca la migliore sorgente audio esposta e si converte a 320 kbps. Disattivarlo per scegliere bitrate o risoluzioni inferiori.
* **Aspetto:** tema chiaro, scuro o di sistema, con opzione per ridurre le animazioni. Viene rispettata anche la preferenza di movimento del sistema.
* **Piattaforme e versione:** riepilogo dei formati, motore incluso e collegamento alle release ufficiali.
* **Ripristina preferenze:** torna ai valori iniziali; non elimina i file scaricati.
* **Aggiornamenti automatici:** attivi inizialmente. Controlla e scarica le Release ufficiali GitHub senza inviare i link scaricati. Disattivabile; controllo e download manuali restano disponibili.

Le preferenze sono in `settings.json` nella cartella dati dell’app (`app.getPath('userData')`), con scrittura atomica. Nessun database o sincronizzazione online. Un file corrotto viene recuperato con valori iniziali. Le impostazioni scelte durante un download si applicano a quelli successivi.

![Impostazioni, tema chiaro](docs/images/settings-light.png)

![Impostazioni, tema scuro](docs/images/settings-dark.png)

## Coda di download

Aprire **Coda** nella barra superiore. Aggiungere il contenuto già analizzato con **Aggiungi alla coda**, oppure espandere **Aggiungi più link** e incollare un link per riga. Fino a 50 link per inserimento e 100 elementi complessivi, inclusi quelli terminati. I duplicati già in attesa con la stessa richiesta vengono saltati. Un batch contenente un link non valido viene rifiutato interamente, senza alterare la coda esistente.

**Avvia coda** elabora un file alla volta. **Pausa coda** impedisce l’avvio del prossimo elemento e lascia terminare quello attuale; **Annulla file** lo interrompe. I file falliti o annullati possono essere ritentati singolarmente, ripartendo dall’inizio. Gli errori di un elemento non fermano quelli successivi. **Rimuovi terminati** libera spazio nell’elenco; **Annulla coda** ferma l’elemento corrente e annulla quelli in attesa.

SoundCloud viene indicato come MP3 fin dall’inserimento, anche se il batch usa MP4. La modalità qualità massima si applica anche alla coda. Se è attiva la richiesta della destinazione, apparirà un selettore per ogni file; disattivarla nelle Impostazioni per usare la cartella preferita automaticamente. I file esistenti non vengono sovrascritti nel salvataggio automatico.

La coda vive soltanto nella sessione: chiudere l’app la annulla, dopo un avviso. Non viene salvata una cronologia o un database. I download singoli e la coda non possono usare il motore contemporaneamente.

![Coda di download](docs/images/queue.png)

## Avanzamento e qualità

Durante il trasferimento l’app mostra byte scaricati, dimensione prevista, velocità e tempo rimanente quando yt-dlp li fornisce. I dati si riferiscono alla **traccia corrente**: video e audio separati possono ricominciare la percentuale da zero. Le dimensioni stimate sono indicate come tali. Durante conversione e salvataggio non vengono inventate percentuali, velocità o tempi.

La qualità massima seleziona la migliore sorgente esposta dal servizio, senza preferire un MP4 inferiore soltanto per il contenitore. FFmpeg unisce o rimuxa le tracce in MP4 senza promettere miglioramenti della sorgente. Il codec risultante può richiedere un lettore aggiornato. MP3 è una conversione con perdita: 320 kbps in uscita non rende una sorgente compressa lossless e non supera restrizioni di accesso della piattaforma.

## Aggiornamenti automatici

Con l’opzione attiva, l’app controlla la Release stabile ufficiale circa 10 secondi dopo l’avvio e ogni 6 ore mentre rimane aperta. Scarica in background una versione successiva e mostra l’avanzamento in **Impostazioni → Aggiornamenti**. **Riavvia e installa** è disponibile soltanto quando non ci sono download o elementi della coda da completare. Un errore di rete o checksum lascia utilizzabile la versione corrente e permette di riprovare.

| Sistema | Comportamento dalla 0.4 |
| --- | --- |
| Windows installer | Download automatico; installazione al clic su **Riavvia e installa** oppure alla chiusura normale se l’opzione è attiva e non ci sono lavori pendenti |
| Linux AppImage | Stesso comportamento; avviare il file AppImage originale da una cartella scrivibile |
| Linux Debian/Ubuntu | Download automatico; installazione richiesta esplicitamente dal pulsante, con eventuale autorizzazione del sistema |
| macOS | Installazione automatica disponibile per build con firma Developer ID valida. Le build attuali non firmate controllano la versione e aprono la Release per l’installazione manuale |
| Avvio dal sorgente | Solo controllo della versione e collegamento alla Release; nessuna installazione automatica |

Disattivando l’opzione e salvando le preferenze si fermano i controlli periodici e l’installazione alla chiusura; un trasferimento già iniziato può terminare. Si possono usare **Controlla aggiornamenti**, **Scarica aggiornamento** e **Riavvia e installa** per procedere esplicitamente.

Il feed è fissato al repository ufficiale CosmoNetinfo/flusiva, via HTTPS. electron-updater verifica il checksum SHA-512 del pacchetto prima di dichiararlo pronto. I checksum verificano l’integrità rispetto al feed; non sostituiscono una firma dell’editore. Non vengono installati prerelease o downgrade. Niente token GitHub, cookie dei servizi o telemetria. [Configurazione firma e pubblicazione](docs/UPDATES.md).

![Aggiornamenti](docs/images/updates.png)

## Spotify

Spotify è stato rimandato su richiesta del proprietario. Non è abilitato come sorgente MP3. La Web API documenta i metadati e un eventuale `preview_url` di 30 secondi, deprecato e talvolta assente; non espone un MP3 completo. Flusiva non sostituisce silenziosamente un brano Spotify con audio proveniente da un altro servizio. [Documentazione ufficiale](https://developer.spotify.com/documentation/web-api/reference/get-track).

## Sviluppo

Richiede Node.js 24 LTS e Git. Python e FFmpeg di sistema non servono: i binari vengono preparati per la piattaforma corrente.

```sh
npm ci --ignore-scripts
npm run setup
npm start
```

`prepare:tools` scarica yt-dlp dalla release ufficiale fissata nel sorgente e ne verifica SHA-256. Copia FFmpeg e FFprobe dai pacchetti npm e include le licenze. Il runtime JavaScript usato da yt-dlp è Node incluso in Electron (`ELECTRON_RUN_AS_NODE=1`), quindi non serve installare Deno.

```sh
npm run check
npm test
npm run pack
npm run dist
```

Test online facoltativo: `node scripts/online-smoke.cjs`. Scarica il cortometraggio Creative Commons Big Buck Bunny della Blender Foundation in MP4 a risoluzione limitata e MP3, verifica le tracce con FFprobe e rimuove i temporanei. Richiede Internet e circa 70 MB di trasferimento; un URL alternativo autorizzato può essere passato come argomento. Per un contenuto solo audio viene verificato soltanto MP3. Non fa parte della CI per evitare dipendenza dai servizi esterni nei controlli deterministici.

Sonde dei soli metadati: `node scripts/platform-smoke.cjs`. Controlla contenuti pubblici sulle cinque piattaforme senza scaricare i file multimediali e registra esiti e restrizioni in `test-results/platform-probes.json`. Un blocco remoto viene registrato come tale, non come successo.

Compilare sul sistema di destinazione. GitHub Actions esegue automaticamente i controlli, il test d'avvio Electron e la compilazione su quattro runner nativi. I 41 test deterministici non contattano i servizi esterni. Il test Electron verifica anche navigazione, preferenze via IPC, rifiuto di valori non validi, ripristino e presenza delle cinque piattaforme, con profilo isolato. Prima di una release pubblica verificare un download MP4 e MP3 reale su ciascun sistema con un video autorizzato.

Per pubblicare gli installer creare e inviare un tag coerente con `package.json`:

```sh
git tag v0.4.0
git push origin v0.4.0
```

Il workflow pubblica una GitHub Release solo quando tutte e quattro le build riescono. Prepara i feed Windows/Linux/macOS con SHA-512 e unisce le architetture Mac. Carica tutti i file in una bozza e la pubblica solo al termine. Include `SHA256SUMS.txt`. Nessun workflow Pages è necessario.

## Architettura

* `desktop/`: processo Electron e bridge IPC ristretto, renderer in sandbox con isolamento del contesto, navigazione e permessi bloccati.
* `public/`: interfaccia locale HTML/CSS/JavaScript con CSP, senza script remoti, telemetria o server HTTP.
* `core/platforms.cjs`: riconoscimento dei cinque servizi, link brevi, metadati comuni e opzioni dei formati.
* `core/settings.cjs` e `core/files.cjs`: preferenze locali validate, aggiornamenti atomici e salvataggio senza sovrascrittura.
* `core/queue.cjs`: coda seriale, pausa, annullamento, tentativi e deduplicazione.
* `core/progress.cjs` e `core/quality.cjs`: dati del trasferimento e politica di qualità massima.
* `core/updates.cjs`, `core/auto-updates.cjs`, `desktop/updater.cjs`: controllo versioni, download verificato, installazione e scelta delle capacità del sistema.
* `core/`: processi senza shell, avanzamento, annullamento e pulizia dei temporanei.
* `scripts/prepare-tools.mjs`: dipendenze native e controllo di integrità.
* `.github/workflows/build.yml`: test, installer e pubblicazione release.

Copertine e contenuti sono richiesti direttamente alle piattaforme e ai loro CDN. Nessun link viene inviato a un servizio Flusiva. Gli aggiornamenti contattano GitHub e i suoi endpoint di download quando richiesti o se l’opzione automatica è attiva. Il browser si apre solo al clic sul collegamento alle Release.

Licenze dei componenti in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Logo, grafica e prossimi sviluppi

Identità in blu, azzurro e bianco, con una F costruita da puntini. [Studio del logo e tre proposte](docs/BRAND.md), [tavola ad alta risoluzione](docs/brand/logo-study.png), [roadmap e piattaforme candidate](docs/ROADMAP.md), [note di rilascio](CHANGELOG.md) e [verifiche della versione](docs/VALIDATION.md).

La versione 0.2.0 implementa TikTok, Vimeo, Dailymotion e SoundCloud; la 0.3.0 aggiunge coda, dati di avanzamento, qualità massima e controllo delle versioni. La 0.4 aggiunge gli aggiornamenti installabili su Windows/Linux e il percorso per macOS firmato. Restano in roadmap Instagram, certificati di firma e ripresa dei file parziali. Spotify è rimandato.

## Risolvere un problema

| Problema | Cosa verificare |
| --- | --- |
| YouTube richiede accesso o blocca la richiesta | Riprovare più tardi e verificare le nuove release. Flusiva non importa cookie né credenziali del browser |
| MP4 non disponibile | Scegliere la migliore qualità disponibile oppure MP3; i formati dipendono dal video |
| Video senza riproduzione nel lettore di sistema | I file possono usare AV1/VP9/H.264 secondo la sorgente; usare un lettore aggiornato. MP4 è un contenitore, non una garanzia sul codec |
| Download o conversione lenti | Dipendono da Internet, dimensione del file e computer. La fase di conversione non mostra una percentuale inventata |
| Spazio su disco esaurito | Liberare spazio nella cartella temporanea del sistema e nella destinazione |
| AppImage non si avvia | Verificare FUSE e i permessi, oppure usare il pacchetto `.deb`. Se il sistema impedisce la sandbox Chromium, preferire il pacchetto installato e non usare `--no-sandbox` |
| Antivirus o Gatekeeper mostrano un avviso | Le prime release sono prive di firma dell’editore; verificare provenienza e checksum. La firma è prevista in roadmap |

Per una segnalazione aprire una [issue](https://github.com/CosmoNetinfo/flusiva/issues) con sistema, architettura, versione e messaggio di errore. Non allegare cookie, password o link privati. Per i formati, indicare un video pubblico che si ha il permesso di utilizzare.

## Uso responsabile

Flusiva è un programma gratuito e open source, pensato per scaricare contenuti propri, liberi da diritti (ad esempio con licenza Creative Commons o di pubblico dominio) o per i quali l'uso personale è consentito dalla legge.

Scaricare contenuti protetti da diritto d'autore senza il permesso del titolare, o in violazione dei termini di servizio delle piattaforme, può non essere consentito. Ogni utente è l'unico responsabile dell'uso che fa del programma e del rispetto delle leggi e delle condizioni d'uso del proprio Paese e dei siti da cui scarica.

Flusiva non ospita, non distribuisce e non memorizza alcun contenuto: tutto avviene sul computer di chi lo usa. Il programma è fornito "così com'è", senza garanzie, e gli autori non rispondono dell'uso che ne viene fatto.

## Licenza

Copyright © 2026 CosmoNet. Il codice di Flusiva è distribuito con licenza **GPL-3.0-only**; il testo integrale è in [LICENSE](LICENSE) ed è incluso negli installer. Electron, yt-dlp, FFmpeg, FFprobe e le relative dipendenze conservano le proprie licenze, descritte in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
