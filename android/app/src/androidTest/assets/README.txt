Fixture locali dei test Android, escluse dall'app distribuita.

vp9-in-mp4.mp4: tre fotogrammi sintetici blu, 192x108, VP9 in MP4,
generati con FFmpeg/libvpx-vp9; nessun contenuto del Reel dell'utente.

wrong-signer.apk: manifest senza codice né attività, package
info.cosmonet.flusiva, versione 0.9.0 / code 9000. Creato con aapt2 e
firmato con una chiave RSA di test estranea alla chiave di distribuzione.
Serve a verificare il rifiuto degli APK con firma diversa.
