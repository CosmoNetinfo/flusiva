package info.cosmonet.flusiva

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

data class DownloadSnapshot(val busy: Boolean = false, val message: String = "Incolla un link per iniziare.", val percent: Float? = null, val info: MediaInfo? = null, val saved: Uri? = null, val format: String? = null, val failed: Boolean = false)
object DownloadState {
    @Volatile var value = DownloadSnapshot(); private set
    val observers = CopyOnWriteArraySet<(DownloadSnapshot) -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    @Synchronized fun set(snapshot: DownloadSnapshot) { value = snapshot; main.post { observers.forEach { it(snapshot) } } }
}
object Downloads {
    fun save(context: Context, file: File, title: String, format: String, cancelled: () -> Boolean): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, MediaPolicy.fileName(title, format))
            put(MediaStore.Downloads.MIME_TYPE, if (format == "mp3") "audio/mpeg" else "video/mp4")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Flusiva")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Impossibile creare il file in Download.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { check(!cancelled()) { "Operazione annullata." }; val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count) }
            } } ?: error("Impossibile salvare il file.")
            check(!cancelled()) { "Operazione annullata." }
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1) { "Salvataggio incompleto." }
            return uri
        } catch (error: Throwable) { resolver.delete(uri, null, null); throw error }
    }
}
