package info.cosmonet.flusiva

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val cancelled = AtomicBoolean(false)
    private val running = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val deadline = Runnable { cancelled.set(true); Engine.cancel() }
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("downloads", "Download Flusiva", NotificationManager.IMPORTANCE_LOW))
        File(cacheDir, "transfers").mkdirs()
        File(cacheDir, "transfers").listFiles()?.forEach { it.deleteRecursively() }
    }
    private fun notification(message: String, percent: Float? = null): Notification {
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getService(this, 2, Intent(this, DownloadService::class.java).setAction("cancel"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "downloads").setSmallIcon(R.drawable.ic_flusiva).setContentTitle("Flusiva").setContentText(message).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(100, percent?.toInt() ?: 0, percent == null).addAction(0, "Annulla", cancel).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "cancel") { cancelled.set(true); Engine.cancel(); if (!running.get()) stopSelf(); return START_NOT_STICKY }
        if (intent == null) { stopSelf(); return START_NOT_STICKY }
        if (!running.compareAndSet(false, true)) return START_NOT_STICKY
        cancelled.set(false)
        startForeground(1, notification("Preparazione…"), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        DownloadState.set(DownloadState.value.copy(busy = true, message = "Preparazione del motore…", percent = null, saved = null, failed = false))
        handler.postDelayed(deadline, if (intent.action == "inspect") 120000 else 7200000)
        val lock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "flusiva:download").apply { acquire(7200000) }
        executor.execute {
            var directory: File? = null
            var outcome: DownloadSnapshot
            try {
                if (intent.action == "inspect") {
                    val info = Engine.inspect(this, intent.getStringExtra("url").orEmpty(), cancelled::get)
                    outcome = DownloadSnapshot(info = info, message = "${info.platform} · ${info.duration / 60}:${(info.duration % 60).toString().padStart(2, '0')} · pronto")
                } else {
                    val info = DownloadState.value.info ?: error("Analizza prima un link.")
                    check(info.url == intent.getStringExtra("url")) { "Il link è cambiato. Analizzalo di nuovo." }
                    val format = intent.getStringExtra("format") ?: "mp4"
                    val maximum = intent.getBooleanExtra("maximum", true)
                    val audio = intent.getStringExtra("audio") ?: "320"
                    val video = intent.getStringExtra("video") ?: "best"
                    directory = File.createTempFile("download-", "", File(cacheDir, "transfers")).apply { delete(); mkdir() }
                    val file = Engine.download(this, info, format, maximum, audio, video, directory, cancelled::get) { percent, message ->
                        DownloadState.set(DownloadState.value.copy(busy = true, percent = percent, message = message))
                        getSystemService(NotificationManager::class.java).notify(1, notification(message, percent))
                    }
                    DownloadState.set(DownloadState.value.copy(percent = null, message = "Salvataggio in Download/Flusiva…"))
                    val uri = Downloads.save(this, file, info.title, format, cancelled::get)
                    outcome = DownloadSnapshot(info = info, saved = uri, format = format, message = "Salvato in Download/Flusiva")
                }
            } catch (error: Throwable) {
                val message = if (cancelled.get()) "Operazione annullata." else MediaPolicy.friendly(error)
                outcome = DownloadState.value.copy(busy = false, percent = null, saved = null, failed = !cancelled.get(), message = message)
            } finally {
                directory?.deleteRecursively(); handler.removeCallbacks(deadline); if (lock.isHeld) lock.release()
            }
            handler.post { running.set(false); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(startId); DownloadState.set(outcome) }
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) { cancelled.set(true); Engine.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { cancelled.set(true); Engine.cancel(); handler.removeCallbacksAndMessages(null); executor.shutdown(); super.onDestroy() }
}
