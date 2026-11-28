**Flusiva 0.9.1** corregge la verifica dei video Android e aggiunge gli aggiornamenti direttamente dall’app.

### Video Instagram e Facebook

* Il controllo dei file usa FFprobe incluso, indipendente dai codec del telefono. Gli MP4 validi con VP9 non vengono scartati perché Android non li riconosce.
* Restano verificati integrità, durata massima di 6 ore e presenza della traccia richiesta. Errori distinti per file incompleto, durata e tracce.
* Test con un MP4 VP9 sintetico e con file non validi.

### Aggiornamenti Android dall’app

* **Impostazioni → Controlla aggiornamenti → Scarica aggiornamento → Installa aggiornamento**.
* Download con avanzamento e annullamento, gestito da Android anche quando la schermata è chiusa.
* Verifica SHA-256, package, versione e stessa firma dell’app installata prima di aprire l’installer.
* Controllo all’avvio una volta al giorno, disattivabile. Il download richiede il comando dell’utente e l’installazione richiede sempre conferma Android.
* Durante un download di contenuti l’installazione viene rinviata.

**Chi usa 0.8.1 o precedenti deve installare questo APK manualmente una sola volta**, perché le versioni precedenti aprivano soltanto le Release. Poi gli aggiornamenti successivi saranno gestibili dall’app.

### Installazione

**Android 10+: Flusiva-0.9.1-android.apk**. Stessa firma delle versioni precedenti: installare sopra l’app esistente, senza disinstallare, per conservare playlist e preferenze.

Inclusi anche gli installer Windows x64, Linux x64, Mac Intel e Apple Silicon. Su Mac non firmato resta il download manuale.

Video Instagram/Facebook, foto Instagram originali, sette piattaforme, lettore musicale Android, playlist, equalizzatore e ascolto a schermo spento restano disponibili.

[Guida Android](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/ANDROID.md) · [Verifiche](https://github.com/CosmoNetinfo/flusiva/blob/main/docs/VALIDATION.md).
