# Flusiva per Android

App nativa Kotlin per **Android 10 o successivi**, con elaborazione sul telefono. APK universale per ARM64, ARMv7 e x86_64; include Python, yt-dlp 2026.11.23, QuickJS e FFmpeg. Non richiede Termux, un computer acceso, account o server Flusiva.

## Installazione e uso

Scaricare `Flusiva-VERSIONE-android.apk` dalle [Release ufficiali](https://github.com/CosmoNetinfo/flusiva/releases). Aprire l’APK dal browser o dal gestore file e consentire l’installazione da quella sorgente quando Android lo richiede. L’app non è pubblicata sul Play Store.

1. Incollare un singolo link, oppure scegliere **Condividi → Flusiva** nell’app della piattaforma.
2. Premere **Analizza il link**, scegliere Video MP4 o Audio MP3 e **Scarica sul telefono**.
3. Il file viene salvato in **Download/Flusiva**. Si può aprire o condividere dall’app.

YouTube, TikTok, Vimeo e Dailymotion offrono MP4/MP3; SoundCloud solo MP3. Nessun supporto Spotify, playlist, dirette, contenuti con DRM, account o importazione cookie. Restrizioni e disponibilità dipendono dalle piattaforme; le verifiche desktop non dimostrano automaticamente il funzionamento su ogni telefono o rete.

La qualità massima è attiva inizialmente: cerca la migliore sorgente video/audio disponibile e converte MP3 a 320 kbps. Non può migliorare la qualità della sorgente. Disattivarla nelle Impostazioni per scegliere un limite di risoluzione o un bitrate diverso. MP4 può contenere codec che il lettore del telefono non supporta.

Il download continua tramite servizio in primo piano, con notifica e comando **Annulla**. Consentire le notifiche per vedere l’avanzamento fuori dall’app. L’annullamento rimane disponibile nell’interfaccia anche senza tale permesso. Un file alla volta, massimo 6 ore di contenuto, 8 GiB per traccia e 2 ore per operazione; servono spazio e batteria per i file temporanei e la conversione. Android può imporre ulteriori limiti ai servizi in background. Una chiusura forzata interrompe il lavoro; i temporanei vengono ripuliti alla successiva operazione.

I file sono pubblicati tramite MediaStore, senza accesso generale alla memoria o ai file altrui. Un salvataggio interrotto viene rimosso. Le preferenze sono locali, senza database. La prima versione Android non include coda, temi scuri o ripresa dei file parziali della versione desktop.

In **Impostazioni → Aggiornamenti** si controlla la nuova versione e si apre la Release per il download manuale dell’APK. Android chiede la conferma prima dell’installazione. Per aggiornare un’app già installata il nuovo APK deve avere lo stesso identificativo e la stessa firma.

## Compilazione

JDK 17, Android SDK 36, Build Tools 36.0.0 e NDK 27.0.12077973. Il wrapper Gradle 8.14.3 include il checksum della distribuzione; AGP 8.13.2 e Kotlin 2.2.21 sono fissati nel sorgente.

```sh
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'ndk;27.0.12077973'
node scripts/prepare-android.mjs
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Configurare `ANDROID_HOME`; su Windows usare `gradlew.bat`. La preparazione verifica SHA-256 di yt-dlp e include licenze e runtime C++ del NDK richiesto da FFmpeg. `vendor-android` e `vendor-android-jni` sono generati, esclusi dal repository. Il motore viene aggiornato insieme all’APK, senza download di codice aggiuntivo all’avvio.

I test JVM controllano piattaforme, URL inattesi, qualità e nomi file. I test su emulatore controllano il motore reale, QuickJS, FFmpeg/MP3, salvataggio MediaStore e rimozione di un salvataggio annullato, condivisione del link e Impostazioni. Non dipendono dai servizi remoti.

## Firma e GitHub Actions

La firma Android è gratuita e separata dai certificati Apple. Il repository usa una chiave del progetto conservata nei secret Actions; la chiave privata e la password non fanno parte del codice o degli artifact. Un backup privato deve essere conservato dal proprietario: perderlo impedisce gli aggiornamenti delle installazioni esistenti.

| Secret | Contenuto |
| --- | --- |
| FLUSIVA_ANDROID_KEYSTORE_BASE64 | Keystore codificato Base64, alias flusiva |
| FLUSIVA_ANDROID_STORE_PASSWORD | Password del keystore |
| FLUSIVA_ANDROID_KEY_PASSWORD | Password della chiave |

Impronta SHA-256 del certificato pubblico: `be6d80571ad047cf8f09baf887b82c9e9677c0f9cdd8863c2f9808efba28b7b3`.

Il workflow esegue test JVM, Android Lint e test su emulatore Android 15 x86_64, poi firma l’APK e ne verifica la firma con apksigner. La Release unificata attende sia Android sia le quattro build desktop e include l’APK in SHA256SUMS.txt. Nei tag pubblici una firma mancante blocca la pubblicazione.

Componenti Android: [youtubedl-android 0.18.1](https://github.com/yausername/youtubedl-android/tree/0.18.1), [MediaStore](https://developer.android.com/training/data-storage/shared/media), [servizi in primo piano](https://developer.android.com/develop/background-work/services/fgs/timeout). Si applicano [GPL v3](../LICENSE) e [Uso responsabile](../README.md#uso-responsabile).
