# Verifiche della prima versione

GATE: ERROR — i controlli della skill grafica basati su Chrome non sono partiti nell’ambiente locale (`Chrome did not start` / `Chrome exited early`). Non è un esito positivo del controllo. La verifica visiva è stata eseguita separatamente nell’app Electron reale.

## Ambiente locale Windows

* Controllo sintassi e configurazione del renderer: `npm run check`.
* Nove test deterministici: URL YouTube e host non ammessi, formati, metadati, nomi file, processi, timeout, annullamento, progressi e pulizia dei temporanei.
* Avvio Electron reale in sandbox: bridge IPC, assenza di `require` nel renderer, presenza dei controlli, assenza di overflow orizzontale, eseguibilità dei tre strumenti e runtime Node incluso.
* Screenshot dell’app reale: `docs/images/flusiva-desktop.png`.
* Test online completato sul video Big Buck Bunny della Blender Foundation: MP4 di 46.284.688 byte con tracce AV1 + AAC; MP3 di 25.388.243 byte con traccia MP3. File temporanei rimossi al termine. I formati e le dimensioni possono cambiare con YouTube.
* Il vecchio video breve di prova di yt-dlp non era disponibile: non è stato considerato un test passato.

## CI e pacchetti

Il workflow testa ogni build con Electron e i binari della piattaforma prima di produrre gli installer. Su Ubuntu il runner configura proprietario e permessi del sandbox helper Chromium; la sandbox resta attiva.

Le build macOS sono separate per Intel e Apple Silicon. Non è stata verificata l’installazione interattiva su macchine pulite macOS/Linux in questa sessione. La compilazione e il test d’avvio non sostituiscono questa verifica. Le prime build non sono firmate né notarizzate.

## Verifica visiva e scelte

App desktop a schermata singola: la richiesta di essenzialità prevale sulle sezioni di marketing e sugli effetti della skill. Palette originale della prima impostazione sostituita con blu/azzurro/bianco su richiesta esplicita del proprietario. I puntini sono statici; l’ingresso iniziale rispetta `prefers-reduced-motion`. Nessuna fotografia, recensione, statistica o prova sociale inventata. Il marchio è originale e le alternative sono in `docs/BRAND.md`.

## Verifiche manuali consigliate dopo l’installazione

1. Installare il pacchetto corretto su una macchina pulita.
2. Scaricare MP4 con audio e MP3 da un video autorizzato.
3. Provare annullamento, scelta di una cartella non scrivibile e un video non disponibile.
4. Verificare sostituzione di un file esistente e apertura della cartella.
5. Controllare consumo e pulizia dello spazio temporaneo.
