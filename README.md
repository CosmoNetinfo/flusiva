# Flusiva

App desktop per scaricare **video MP4 e audio MP3 da YouTube**, con interfaccia in italiano. Windows, Linux e macOS (Intel e Apple Silicon). Il computer dell'utente esegue download e conversione: nessun server remoto, account, hosting o database.

## Scaricare l'app

Gli installer sono nella sezione [Releases](https://github.com/CosmoNetinfo/flusiva/releases). Dopo ogni push su `main`, GitHub Actions genera anche gli artifact scaricabili dalla scheda [Actions](https://github.com/CosmoNetinfo/flusiva/actions).

| Sistema | File |
| --- | --- |
| Windows 10/11 x64 | Installer `.exe` |
| Linux x64 | `.AppImage` oppure pacchetto Debian/Ubuntu `.deb` |
| macOS Intel | `.dmg` / `.zip` con `x64` nel nome |
| macOS Apple Silicon | `.dmg` / `.zip` con `arm64` nel nome |

Le prime build non sono firmate né notarizzate. Windows e macOS possono mostrare avvisi sull'editore. Una distribuzione con firma richiede certificati del proprietario. Non è configurato un aggiornamento automatico: installare le nuove release per aggiornare anche yt-dlp.

Su Linux: rendere eseguibile l'AppImage (`chmod +x Flusiva-*.AppImage`) e avviarla; se FUSE non è disponibile usare il `.deb` o `--appimage-extract-and-run`. Non eseguire l'app come root e non disabilitare la sandbox.

## Utilizzo

1. Incollare il link di un singolo video YouTube e scegliere **Analizza il link**.
2. Selezionare **Video MP4** o **Solo audio MP3** e la qualità.
3. Premere **Scarica** e scegliere dove salvare il file.

Video: migliore combinazione di tracce MP4/M4A disponibile, senza ricodifica del video. Risoluzioni fino a 8K se fornite dalla sorgente; i codec più recenti possono richiedere un lettore aggiornato. MP3: 128, 192, 256 o 320 kbps, partendo dal migliore audio disponibile. La conversione MP3 è con perdita e non può aumentare la qualità della sorgente.

Un download alla volta, durata massima 6 ore, limite 8 GiB per singola traccia, timeout 2 ore. I file vengono preparati nello spazio temporaneo del sistema, copiati alla destinazione e rimossi al termine, in caso di errore o annullamento. Serve spazio per tracce, file finale e copia di salvataggio. Una chiusura forzata del processo può lasciare una cartella `flusiva-*` nella directory temporanea: si può rimuovere quando l'app è chiusa.

Dirette in corso/programmate, playlist, video privati e contenuti che richiedono login non sono supportati. YouTube può cambiare o bloccare le richieste: nessuna app può garantire il download di ogni video. Usare contenuti propri o autorizzati. Solo YouTube è abilitato; l'adattatore è in `core/youtube.cjs` per future piattaforme.

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

Compilare sul sistema di destinazione. GitHub Actions esegue automaticamente i controlli, il test d'avvio Electron e la compilazione su quattro runner nativi. I test deterministici non contattano YouTube. Prima di una release pubblica verificare un download MP4 e MP3 reale su ciascun sistema con un video autorizzato.

Per pubblicare gli installer creare e inviare un tag coerente con `package.json`:

```sh
git tag v0.1.0
git push origin v0.1.0
```

Il workflow pubblica una GitHub Release solo quando tutte e quattro le build riescono. Include `SHA256SUMS.txt`. Nessun workflow Pages è necessario.

## Architettura

* `desktop/`: processo Electron e bridge IPC ristretto, renderer in sandbox con isolamento del contesto, navigazione e permessi bloccati.
* `public/`: interfaccia locale HTML/CSS/JavaScript con CSP, senza script remoti, telemetria o server HTTP.
* `core/`: validazione YouTube, metadati, processi senza shell, avanzamento, annullamento e pulizia dei temporanei.
* `scripts/prepare-tools.mjs`: dipendenze native e controllo di integrità.
* `.github/workflows/build.yml`: test, installer e pubblicazione release.

Le copertine sono recuperate da `i.ytimg.com`; i contenuti sono richiesti direttamente a YouTube. Nessun link viene inviato a un servizio Flusiva.

Licenze dei componenti in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
