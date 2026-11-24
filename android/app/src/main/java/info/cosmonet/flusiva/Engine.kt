package info.cosmonet.flusiva

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

data class MediaInfo(val url: String, val platform: String, val title: String, val author: String, val duration: Int, val hasVideo: Boolean, val hasAudio: Boolean, val sourceUrl: String = url)
object Engine {
    private var initialized = false
    const val PROCESS_ID = "flusiva-download"
    @Synchronized fun initialize(context: Context) {
        if (initialized) return
        YoutubeDL.init(context)
        FFmpeg.init(context)
        // Upstream FFmpeg 0.18.1 requires libc++ for librubberband but omits it.
        File(context.applicationInfo.nativeLibraryDir, "libc++_shared.so").copyTo(File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib/libc++_shared.so"), overwrite = true)
        val metadata = JSONObject(context.assets.open("engine-info.json").bufferedReader().use { it.readText() })
        val target = File(context.noBackupFilesDir, "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}/${YoutubeDL.ytdlpBin}")
        val expected = metadata.getString("sha256")
        fun digest(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        if (!target.exists() || digest(target) != expected) {
            val staging = File(target.parentFile, "yt-dlp.new")
            try {
                context.assets.open("yt-dlp").use { input -> staging.outputStream().use { input.copyTo(it) } }
                check(digest(staging) == expected) { "Verifica del motore non riuscita." }
                check(staging.renameTo(target)) { "Impossibile preparare il motore." }
            } finally { staging.delete() }
        }
        initialized = true
    }
    fun request(url: String): YoutubeDLRequest = YoutubeDLRequest(url).addOption("--ignore-config").addOption("--no-playlist").addOption("--no-warnings").addOption("--socket-timeout", "25").addOption("--retries", "3").addOption("--no-cache-dir")
    private fun resolve(value: String, cancelled: () -> Boolean): MediaLink {
        var item = MediaPolicy.identify(value)
        val platform = item.platform
        repeat(6) {
            check(!cancelled()) { "Operazione annullata." }
            if (!item.short) return item
            val connection = URI(item.url).toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false; connection.connectTimeout = 15000; connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                check(connection.responseCode in listOf(301, 302, 303, 307, 308)) { "Copia il link completo del contenuto." }
                val location = connection.getHeaderField("Location") ?: error("Destinazione del link non disponibile.")
                item = MediaPolicy.identify(URI(item.url).resolve(location).toString())
                check(item.platform == platform) { "Il link rimanda a un’altra piattaforma." }
            } finally { connection.disconnect() }
        }
        error("Troppi reindirizzamenti. Copia il link completo.")
    }
    fun inspect(context: Context, value: String, cancelled: () -> Boolean): MediaInfo {
        initialize(context)
        val link = resolve(value, cancelled)
        check(!cancelled()) { "Operazione annullata." }
        val response = YoutubeDL.execute(request(link.url).addOption("--dump-single-json").addOption("--skip-download"), PROCESS_ID, null)
        check(!cancelled()) { "Operazione annullata." }
        val info = JSONObject(response.out)
        check(!info.has("entries") && info.optString("_type") != "playlist") { "Scegli un singolo contenuto." }
        val duration = info.optDouble("duration", 0.0)
        check(duration.isFinite() && duration > 0 && duration <= 21600 && !info.optBoolean("is_live") && info.optString("live_status") !in listOf("is_live", "is_upcoming")) { "Scegli un contenuto registrato con durata massima di 6 ore." }
        val canonical = MediaPolicy.identify(info.optString("webpage_url", link.url))
        check(!canonical.short && canonical.platform == link.platform) { "La piattaforma ha restituito un link inatteso." }
        val formats = info.optJSONArray("formats") ?: error("Nessun formato disponibile.")
        var video = false; var audio = false
        for (index in 0 until formats.length()) {
            val f = formats.getJSONObject(index)
            if (!f.optBoolean("has_drm")) {
                video = video || (f.optString("vcodec") !in listOf("", "none", "null") && f.optString("ext") != "mhtml")
                audio = audio || f.optString("acodec") !in listOf("", "none", "null")
            }
        }
        check(video || audio) { "Nessun formato audio o video disponibile." }
        return MediaInfo(canonical.url, link.platform, info.optString("title", "Flusiva").take(1000), info.optString("uploader", link.platform).take(500), duration.toInt(), video && link.platform != "SoundCloud", audio, MediaPolicy.identify(value).url)
    }
    fun download(context: Context, info: MediaInfo, format: String, maximum: Boolean, audio: String, video: String, directory: File, cancelled: () -> Boolean, progress: (Float?, String) -> Unit): File {
        initialize(context)
        check(!cancelled()) { "Operazione annullata." }
        check(if (format == "mp4") info.hasVideo else info.hasAudio) { "Questo formato non è disponibile." }
        val request = request(info.url).addCommands(MediaPolicy.options(format, maximum, audio, video, info.hasAudio))
            .addOption("--newline").addOption("--progress").addOption("--progress-delta", "1").addOption("--max-filesize", "8G")
            .addOption("-o", File(directory, "media.%(ext)s").absolutePath)
        YoutubeDL.execute(request, PROCESS_ID) { percent, eta, line ->
            if (cancelled()) YoutubeDL.destroyProcessById(PROCESS_ID)
            val conversion = line.startsWith("[ExtractAudio]") || line.startsWith("[Merger]") || line.startsWith("[VideoRemuxer]")
            if (conversion) progress(null, "Conversione e preparazione del file…")
            else if (line.startsWith("[download]") && percent >= 0) progress(percent.coerceIn(0f, 100f), "Download ${percent.toInt()}%${if (eta > 0) " · circa ${eta}s rimanenti" else ""}")
        }
        check(!cancelled()) { "Operazione annullata." }
        return File(directory, "media.$format").also { check(it.isFile && it.length() > 0) { "Il motore non ha prodotto il file richiesto." } }
    }
    fun cancel() { YoutubeDL.destroyProcessById(PROCESS_ID) }
}
