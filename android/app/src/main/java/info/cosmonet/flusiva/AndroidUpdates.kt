package info.cosmonet.flusiva

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class AppRelease(val version: String, val sha256: String, val size: Long) {
    val code get() = UpdatePolicy.code(version)
    val url get() = UpdatePolicy.apkUrl(version)
}
data class UpdateSnapshot(val status: String = "idle", val message: String = "Controlla se è disponibile una nuova versione.", val release: AppRelease? = null, val percent: Int? = null)

object AndroidUpdates {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val refreshing = AtomicBoolean(false)
    private val checking = AtomicBoolean(false)
    val observers = CopyOnWriteArraySet<(UpdateSnapshot) -> Unit>()
    @Volatile var value = UpdateSnapshot(); private set
    private fun publish(state: UpdateSnapshot) { value = state; main.post { observers.forEach { it(state) } } }
    private fun store(context: Context) = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private fun manager(context: Context) = context.getSystemService(DownloadManager::class.java)
    private fun file(context: Context, release: AppRelease): File = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: error("Spazio aggiornamenti non disponibile."), "updates/Flusiva-${release.version}-android.apk")
    @Suppress("DEPRECATION") private fun installedCode(context: Context) = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    private fun saved(context: Context): AppRelease? = runCatching {
        val prefs = store(context); val version = prefs.getString("version", null) ?: return@runCatching null
        val hash = prefs.getString("sha256", "").orEmpty(); val size = prefs.getLong("size", 0)
        require(UpdatePolicy.validHash(hash) && size in 1..536870912L)
        AppRelease(version, hash, size).also { it.code }
    }.getOrNull()
    private fun clearDownload(context: Context, release: AppRelease?) {
        val prefs = store(context); val id = prefs.getLong("download", -1)
        if (id >= 0) runCatching { manager(context).remove(id) }
        runCatching { release?.let { file(context, it).delete() } }
        context.getSystemService(NotificationManager::class.java).cancel(1003)
        prefs.edit().remove("download").remove("verified").apply()
    }
    private fun fetchText(url: String, limit: Int): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Flusiva-Android/${BuildConfig.VERSION_NAME}")
            check(connection.responseCode == 200) { "Controllo aggiornamenti non disponibile (HTTP ${connection.responseCode}). Riprova più tardi." }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) { val n = input.read(buffer); if (n < 0) break; check(output.size() + n <= limit) { "Risposta aggiornamenti troppo grande." }; output.write(buffer, 0, n) }
            }
            val bytes = output.toByteArray()
            check(bytes.size <= limit) { "Risposta aggiornamenti troppo grande." }
            return bytes.toString(Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
    fun attach(context: Context) {
        val app = context.applicationContext
        refresh(app)
        worker.execute { val age = System.currentTimeMillis() - store(app).getLong("checked", 0); if (Preferences(app).automaticUpdates && age !in 0L..86400000L) check(app) }
    }
    fun check(context: Context) {
        val app = context.applicationContext
        if (store(app).getLong("download", -1) >= 0) { refresh(app); return }
        if (!checking.compareAndSet(false, true)) return
        publish(UpdateSnapshot("checking", "Controllo della Release ufficiale…", value.release))
        worker.execute {
            try {
                store(app).edit().putLong("checked", System.currentTimeMillis()).apply()
                val json = JSONObject(fetchText("https://api.github.com/repos/CosmoNetinfo/flusiva/releases/latest", 524288))
                check(!json.optBoolean("draft", true) && !json.optBoolean("prerelease", true)) { "Release stabile non disponibile." }
                val version = json.getString("tag_name").removePrefix("v")
                if (UpdatePolicy.code(version) <= installedCode(app)) {
                    clearDownload(app, saved(app)); store(app).edit().remove("version").apply()
                    publish(UpdateSnapshot("current", "Flusiva ${BuildConfig.VERSION_NAME} è aggiornata."))
                } else {
                    val name = "Flusiva-$version-android.apk"; val url = UpdatePolicy.apkUrl(version)
                    val assets = json.getJSONArray("assets")
                    val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull { it.optString("name") == name && it.optString("state") == "uploaded" } ?: error("APK ufficiale non disponibile nella Release.")
                    check(apk.getString("browser_download_url") == url) { "Indirizzo aggiornamento non valido." }
                    val size = apk.getLong("size"); check(size in 1..536870912L) { "Dimensione aggiornamento non valida." }
                    val digest = apk.optString("digest").removePrefix("sha256:")
                    val hash = if (UpdatePolicy.validHash(digest)) digest else UpdatePolicy.checksum(fetchText("https://github.com/CosmoNetinfo/flusiva/releases/download/v$version/SHA256SUMS.txt", 65536), name)
                    val old = saved(app); if (old != null && old.version != version) clearDownload(app, old)
                    val release = AppRelease(version, hash, size)
                    store(app).edit().putString("version", version).putString("sha256", hash).putLong("size", size).apply()
                    publish(UpdateSnapshot("available", "Disponibile Flusiva $version. Scarica l’aggiornamento dall’app.", release))
                }
                store(app).edit().putLong("checked", System.currentTimeMillis()).apply()
            } catch (error: Exception) { publish(UpdateSnapshot("error", error.message ?: "Controllo non riuscito. Riprova.", saved(app))) }
            finally { checking.set(false) }
        }
    }
    fun download(context: Context) {
        val app = context.applicationContext
        worker.execute {
            try {
                val release = saved(app) ?: error("Controlla prima gli aggiornamenti.")
                check(release.code > installedCode(app)) { "Questa versione è già installata." }
                if (store(app).getLong("download", -1) >= 0) { refresh(app); return@execute }
                val target = file(app, release); target.parentFile!!.mkdirs(); target.delete()
                val request = DownloadManager.Request(Uri.parse(release.url)).setTitle("Flusiva ${release.version}")
                    .setDescription("Aggiornamento dell’app").setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, "updates/${target.name}")
                val id = manager(app).enqueue(request)
                if (!store(app).edit().putLong("download", id).putBoolean("verified", false).commit()) {
                    manager(app).remove(id); error("Impossibile conservare lo stato dell’aggiornamento. Libera spazio e riprova.")
                }
                publish(UpdateSnapshot("downloading", "Download aggiornamento in corso…", release))
            } catch (error: Exception) { publish(UpdateSnapshot("error", error.message ?: "Download non riuscito.", saved(app))) }
        }
    }
    fun cancel(context: Context) {
        val app = context.applicationContext
        worker.execute { val release = saved(app); clearDownload(app, release); publish(UpdateSnapshot("available", "Download aggiornamento annullato.", release)) }
    }
    fun refresh(context: Context, verify: Boolean = true, complete: (() -> Unit)? = null) {
        val app = context.applicationContext
        if (!refreshing.compareAndSet(false, true)) { complete?.invoke(); return }
        worker.execute {
            try {
                val release = saved(app) ?: run { if (!checking.get()) publish(UpdateSnapshot()); return@execute }
                if (release.code <= installedCode(app)) { clearDownload(app, release); store(app).edit().remove("version").apply(); publish(UpdateSnapshot("current", "Flusiva è aggiornata.")); return@execute }
                val prefs = store(app); val id = prefs.getLong("download", -1)
                if (id < 0) { publish(UpdateSnapshot("available", "Disponibile Flusiva ${release.version}.", release)); return@execute }
                val query = manager(app).query(DownloadManager.Query().setFilterById(id)) ?: error("Gestore download Android non disponibile.")
                query.use { cursor ->
                    if (!cursor.moveToFirst()) { clearDownload(app, release); publish(UpdateSnapshot("available", "Download non presente. Puoi riprovare.", release)); return@execute }
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            if (!verify && !prefs.getBoolean("verified", false)) {
                                publish(UpdateSnapshot("downloaded", "APK scaricato. Apri Flusiva per verificarlo e installarlo.", release, 100))
                                runCatching { notifyReady(app, release, false) }
                                return@execute
                            }
                            if (!prefs.getBoolean("verified", false) || !file(app, release).isFile) {
                                publish(UpdateSnapshot("verifying", "Verifica dell’APK scaricato…", release))
                                verifyArchive(app, file(app, release), release)
                                prefs.edit().putBoolean("verified", true).apply()
                                runCatching { notifyReady(app, release, true) }
                            }
                            publish(UpdateSnapshot("ready", "Flusiva ${release.version} è pronta. Conferma l’installazione per aggiornare.", release, 100))
                        }
                        DownloadManager.STATUS_FAILED -> { clearDownload(app, release); publish(UpdateSnapshot("error", "Download aggiornamento non riuscito. Controlla connessione e spazio, poi riprova.", release)) }
                        else -> {
                            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val percent = if (total > 0) (bytes * 100 / total).toInt().coerceIn(0, 100) else null
                            val paused = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_PAUSED
                            publish(UpdateSnapshot("downloading", if (paused) "Download aggiornamento in pausa. Android attende una connessione disponibile." else "Download aggiornamento${percent?.let { " · $it%" }.orEmpty()}. Continua anche chiudendo la schermata.", release, percent))
                        }
                    }
                }
            } catch (error: Exception) { val release = saved(app); clearDownload(app, release); publish(UpdateSnapshot("error", error.message ?: "Verifica aggiornamento non riuscita.", release)) }
            finally { refreshing.set(false); complete?.invoke() }
        }
    }
    @Suppress("DEPRECATION") fun verifyArchive(context: Context, file: File, release: AppRelease) {
        check(file.isFile && file.length() == release.size) { "APK incompleto: scarica di nuovo l’aggiornamento." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(UpdatePolicy.validHash(release.sha256) && hash == release.sha256) { "Verifica SHA-256 non riuscita. L’APK non verrà installato." }
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: error("APK non valido.")
        val own = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        check(archive.packageName == context.packageName && archive.longVersionCode == release.code) { "L’APK non corrisponde a Flusiva o alla versione prevista." }
        val incoming = archive.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        val installed = own.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
        check(incoming.isNotEmpty() && incoming == installed) { "La firma dell’APK è diversa da quella di Flusiva installata." }
    }
    private fun notifyReady(context: Context, release: AppRelease, verified: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel("updates", "Aggiornamenti Flusiva", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(context, MainActivity::class.java).putExtra("updates", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 1003, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notifications.notify(1003, NotificationCompat.Builder(context, "updates").setSmallIcon(R.drawable.ic_flusiva).setContentTitle("Flusiva ${release.version} scaricata")
            .setContentText(if (verified) "Apri Flusiva per installare l’aggiornamento verificato." else "Apri Flusiva per verificare e installare l’aggiornamento.").setContentIntent(pending).setOnlyAlertOnce(true).setAutoCancel(true).build())
    }
    fun prepareInstall(context: Context, result: (Intent?, String?) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            try {
                check(!DownloadState.value.busy) { "Attendi la fine del download in corso prima di installare." }
                val release = saved(app) ?: error("Aggiornamento non disponibile.")
                check(value.status == "ready" && release.code > installedCode(app)) { "Scarica e verifica prima l’aggiornamento." }
                verifyArchive(app, file(app, release), release)
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", file(app, release))
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("Aggiornamento Flusiva", uri) }
                main.post { result(intent, null) }
            } catch (error: Exception) { main.post { result(null, error.message ?: "Installazione non disponibile.") } }
        }
    }
}

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val expected = context.getSharedPreferences("updates", Context.MODE_PRIVATE).getLong("download", -1)
        if (expected < 0 || intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2) != expected) return
        val pending = goAsync(); AndroidUpdates.refresh(context, verify = false) { pending.finish() }
    }
}
