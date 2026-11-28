package info.cosmonet.flusiva

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

data class MediaInfo(val url: String, val platform: String, val title: String, val author: String, val duration: Int, val hasVideo: Boolean, val hasAudio: Boolean, val sourceUrl: String = url, val photos: List<Photo> = emptyList())
object Engine {
    private var initialized = false
    private var pluginsDirectory: String? = null
    private val inspected = RecentInspection<MediaInfo> { SystemClock.elapsedRealtime() }
    private fun remember(info: MediaInfo, json: String): MediaInfo { inspected.put(info, json); return info }
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
        val plugin = File(context.noBackupFilesDir, "flusiva-plugins/flusiva/yt_dlp_plugins/extractor/flusiva.py")
        plugin.parentFile!!.mkdirs()
        context.assets.open("flusiva.py").use { input -> plugin.outputStream().use { input.copyTo(it) } }
        pluginsDirectory = File(context.noBackupFilesDir, "flusiva-plugins").path
        initialized = true
    }
    private fun configured(request: YoutubeDLRequest): YoutubeDLRequest = request.addOption("--ignore-config").addOption("--no-plugin-dirs").apply { pluginsDirectory?.let { addOption("--plugin-dirs", it) } }.addOption("--no-playlist").addOption("--no-warnings").addOption("--socket-timeout", "25").addOption("--retries", "3").addOption("--no-cache-dir")
    fun request(url: String): YoutubeDLRequest = configured(YoutubeDLRequest(url))
    fun requestFromInfo(file: File): YoutubeDLRequest = configured(YoutubeDLRequest(emptyList())).addOption("--load-info-json", file.path)
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
        inspected.clear()
        val link = resolve(value, cancelled)
        check(!cancelled()) { "Operazione annullata." }
        val inspection = request(link.url).addOption("--dump-single-json").addOption("--skip-download").addOption("--retries", "1").addOption("--extractor-retries", "1")
        if (link.platform == "Instagram") inspection.addOption("--ignore-no-formats-error")
        val response = YoutubeDL.execute(inspection, PROCESS_ID, null)
        check(!cancelled()) { "Operazione annullata." }
        val info = JSONObject(response.out)
        if (link.platform == "Instagram") {
            val photos = InstagramPhotos.collect(info)
            if (photos.isNotEmpty()) {
                val canonical = MediaPolicy.identify(info.optString("webpage_url", link.url))
                check(!canonical.short && canonical.platform == link.platform) { "Link inatteso dalla piattaforma." }
                return remember(MediaInfo(canonical.url, link.platform, info.optString("title", "Foto Instagram").replaceFirst("Video by ", "Foto di ").take(1000), info.optString("uploader", "Instagram").take(500), 0, false, false, MediaPolicy.identify(value).url, photos), response.out)
            }
        }
        check(!info.has("entries") && info.optString("_type") != "playlist") { "Scegli un singolo video o una traccia. Album, caroselli e playlist non sono supportati." }
        val duration = info.optDouble("duration", 0.0)
        val unknownDuration = link.platform in listOf("Instagram", "Facebook") && info.isNull("duration")
        check((unknownDuration || (duration.isFinite() && duration > 0 && duration <= 21600)) && !info.optBoolean("is_live") && info.optString("live_status") !in listOf("is_live", "is_upcoming")) { "Scegli un contenuto registrato con durata massima di 6 ore." }
        val canonical = MediaPolicy.identify(info.optString("webpage_url", link.url))
        check(!canonical.short && canonical.platform == link.platform) { "La piattaforma ha restituito un link inatteso." }
        val formats = info.optJSONArray("formats") ?: error("Nessun formato disponibile.")
        var video = false; var audio = false
        for (index in 0 until formats.length()) {
            val f = formats.getJSONObject(index)
            if (!f.optBoolean("has_drm")) {
                video = video || (f.optString("ext") != "mhtml" && (f.optString("vcodec") !in listOf("", "none", "null") || (f.isNull("vcodec") && f.optString("video_ext") !in listOf("", "none", "null"))))
                audio = audio || f.optString("acodec") !in listOf("", "none", "null")
            }
        }
        check(video || audio) { "Nessun formato audio o video disponibile." }
        return remember(MediaInfo(canonical.url, link.platform, info.optString("title", "Flusiva").take(1000), info.optString("uploader", link.platform).take(500), duration.toInt(), video && link.platform != "SoundCloud", audio, MediaPolicy.identify(value).url), response.out)
    }
    fun download(context: Context, info: MediaInfo, format: String, maximum: Boolean, audio: String, video: String, directory: File, cancelled: () -> Boolean, photoIndex: Int = 0, progress: (Float?, String) -> Unit): File {
        initialize(context)
        check(!cancelled()) { "Operazione annullata." }
        if (format == "image") {
            check(info.platform == "Instagram" && photoIndex in info.photos.indices) { "Scegli una foto del post Instagram." }
            progress(null, "Download dell’immagine originale…")
            return InstagramPhotos.download(info.photos[photoIndex], directory, cancelled)
        }
        check(if (format == "mp4") info.hasVideo else info.hasAudio) { "Questo formato non è disponibile." }
        val cached = inspected.get(info)
        val metadata = File(directory, "inspection.json")
        val source = if (cached != null) { metadata.writeText(cached); requestFromInfo(metadata) } else request(info.url)
        val request = source.addCommands(MediaPolicy.options(format, maximum, audio, video, info.hasAudio)).addOption("--concurrent-fragments", "4")
            .addOption("--newline").addOption("--progress").addOption("--progress-delta", "1").addOption("--max-filesize", "8G")
            .addOption("-o", File(directory, "media.%(ext)s").absolutePath)
        try { YoutubeDL.execute(request, PROCESS_ID) { percent, eta, line ->
            if (cancelled()) YoutubeDL.destroyProcessById(PROCESS_ID)
            val conversion = line.startsWith("[ExtractAudio]") || line.startsWith("[Merger]") || line.startsWith("[VideoRemuxer]")
            if (conversion) progress(null, "Conversione e preparazione del file…")
            else if (line.startsWith("[download]") && percent >= 0) progress(percent.coerceIn(0f, 100f), "Download ${percent.toInt()}%${if (eta > 0) " · circa ${eta}s rimanenti" else ""}")
        } } finally { metadata.delete() }
        check(!cancelled()) { "Operazione annullata." }
        return File(directory, "media.$format").also {
            check(it.isFile && it.length() > 0) { "Il motore non ha prodotto il file richiesto." }
            if (info.platform in listOf("Instagram", "Facebook")) verifyOutput(it, format)
        }
    }
    fun verifyOutput(file: File, format: String) {
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(file.path)
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val key = if (format == "mp4") MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO else MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
            check(duration in 1L..21600000L && reader.extractMetadata(key) == "yes") { "Il file non contiene la traccia richiesta o supera la durata massima di 6 ore." }
        } finally { reader.release() }
    }
    fun cancel() { YoutubeDL.destroyProcessById(PROCESS_ID); InstagramPhotos.cancel() }
}
