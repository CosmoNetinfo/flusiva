# Pubblicazione degli aggiornamenti

Da 0.4 Flusiva include electron-updater 6.8.11 come dipendenza di produzione. Il provider è fissato a `CosmoNetinfo/flusiva` su GitHub. Sono accettate solo versioni stabili successive alla corrente. Gli utenti di 0.3 o precedenti devono installare 0.4 manualmente una volta.

## Pipeline

Il tag deve corrispondere alla versione in package.json. GitHub Actions esegue test e build native per Windows x64, Linux x64, Mac Intel e Apple Silicon. `scripts/verify-update-config.cjs` verifica il feed e la dipendenza dentro ogni pacchetto.

`scripts/prepare-release.cjs` raccoglie gli artifact separati, rifiuta file mancanti e collisioni e genera `latest.yml`, `latest-linux.yml`, `latest-mac.yml`. Il manifest Mac contiene ZIP e DMG di entrambe le architetture. SHA-512 e dimensioni vengono calcolati sui file finali; viene prodotto anche SHA256SUMS.txt. Il workflow carica gli asset in una bozza e la rende pubblica solo al termine. Non sovrascrive una release già pubblicata.

Non rimuovere i manifest né i file delle versioni pubblicate. L’updater confronta versione e integrità prima di eseguire l’installer. Il checksum non autentica autonomamente l’editore: la fiducia dipende anche dal repository e dalla connessione HTTPS. La firma digitale del codice è un passaggio distinto.

## Firma macOS

L’aggiornamento nativo macOS richiede firma Developer ID valida. L’app verifica la firma locale con codesign e, quando manca, mantiene controllo versione e apertura della Release senza tentare installazioni. Un file ZIP per ogni architettura è necessario all’updater.

Per produrre build firmate, il proprietario deve configurare nei **Settings → Secrets and variables → Actions** del repository:

| Secret | Contenuto |
| --- | --- |
| MAC_CSC_LINK | Certificato Developer ID Application con chiave privata, esportato .p12 codificato Base64 |
| MAC_CSC_KEY_PASSWORD | Password del .p12 |
| APPLE_ID | Account Apple Developer per la notarizzazione |
| APPLE_APP_SPECIFIC_PASSWORD | Password specifica per app dell’account |
| APPLE_TEAM_ID | Team Apple Developer |

Il workflow passa questi valori solo allo step di build macOS. Non inserirli nel codice, nelle issue o nei log. Senza secret la compilazione resta non firmata e l’aggiornamento Mac resta manuale. Prima di distribuire build firmate verificare firma, notarizzazione e aggiornamento da una precedente build firmata su entrambi i tipi di Mac.

Documentazione: [electron-builder](https://www.electron.build/docs/features/auto-update/), [Electron macOS](https://www.electronjs.org/docs/latest/api/auto-updater).

## Comportamento locale

Il controllo avviene dopo 10 secondi dall’avvio e ogni 6 ore quando abilitato. Il pacchetto scaricato rimane nella cache gestita da electron-updater. Disattivare l’opzione non interrompe un download già avviato, ma disabilita i controlli periodici e l’installazione alla chiusura.

Windows/AppImage installano al riavvio esplicito o alla chiusura normale con automatismo attivo e nessun lavoro pendente. AppImage deve essere avviata dal file originale in una cartella scrivibile. Il pacchetto Debian richiede il pulsante esplicito e può chiedere autorizzazione del sistema; non installa alla chiusura. La coda, anche in pausa, impedisce l’installazione finché contiene elementi da completare.

Non sono implementati rollback automatico, aggiornamento separato di yt-dlp e firma crittografica autonoma dei manifest. Le build Windows attuali non hanno firma dell’editore.

## Android dalla 0.9

L’app usa la Release stabile del medesimo repository GitHub. Il controllo all’avvio viene eseguito al massimo una volta al giorno, è disattivabile e non avvia il trasferimento dell’APK. **Scarica aggiornamento** crea un download Android DownloadManager con destinazione nella cartella esterna privata `Download/updates` dell’app; l’ID viene salvato per recuperarne lo stato dopo la chiusura o un riavvio.

La notifica del trasferimento è gestita da Android. Al completamento l’eventuale notifica Flusiva apre le Impostazioni dell’app, senza avviare direttamente l’installer. La verifica legge l’hash SHA-256 dell’asset GitHub (oppure il file `SHA256SUMS.txt` della stessa Release), controlla dimensione, package, versionCode e certificato corrispondente all’app installata. Un errore elimina il file di aggiornamento e consente di riprovare; i file multimediali e le playlist rimangono separati.

Solo dopo la verifica il pulsante **Installa aggiornamento** offre all’installer un URI FileProvider con accesso temporaneo in lettura. Il provider espone soltanto la cartella degli APK di aggiornamento. Android può chiedere l’abilitazione delle installazioni da Flusiva; l’utente torna poi nell’app e conferma l’installazione di sistema. Non viene eseguita un’installazione silenziosa. Un download di contenuti in corso impedisce l’avvio dell’installazione.

La firma resta quella Android del progetto già usata nelle Release 0.5–0.8, senza certificati Apple o servizi esterni. Per passare da 0.8.1 o precedenti alla 0.9 serve una prima installazione manuale dell’APK sopra l’app esistente. Le prove automatiche verificano gli archivi reali e i rifiuti, ma non sostituiscono una prova completa di aggiornamento su un telefono fisico.
