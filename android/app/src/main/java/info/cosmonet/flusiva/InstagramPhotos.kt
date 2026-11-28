package info.cosmonet.flusiva

import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

data class Photo(val url: String, val width: Int = 0, val height: Int = 0)
object InstagramPhotos {
    @Volatile private var active: HttpURLConnection? = null
    fun cancel() { active?.disconnect() }
    fun trusted(value: String): Boolean = runCatching {
        val uri = URI(value); val host = uri.host?.lowercase().orEmpty()
        uri.scheme == "https" && uri.userInfo == null && uri.port == -1 && listOf("cdninstagram.com", "fbcdn.net").any { host == it || host.endsWith(".$it") }
    }.getOrDefault(false)
    fun collect(info: JSONObject): List<Photo> {
        val entries = info.optJSONArray("entries")
        require(entries == null || entries.length() <= 20) { "Il post contiene troppi elementi. Massimo 20." }
        val items = if (entries == null) listOf(info) else (0 until entries.length()).map { entries.getJSONObject(it) }
        return items.mapNotNull { item ->
            if (item.optString("flusiva_media_type") != "image" || (item.optJSONArray("formats")?.length() ?: 0) > 0 || item.optBoolean("has_drm") || item.optBoolean("is_live")) return@mapNotNull null
            val candidates = item.optJSONArray("thumbnails") ?: return@mapNotNull null
            (0 until candidates.length()).map { candidates.getJSONObject(it) }.filter { trusted(it.optString("url")) }
                .map { Photo(it.getString("url"), it.optInt("width", 0), it.optInt("height", 0)) }
                .withIndex().maxWithOrNull(compareBy<IndexedValue<Photo>> { it.value.width.toLong() * it.value.height }.thenBy { it.index })?.value
        }
    }
    fun download(photo: Photo, directory: File, cancelled: () -> Boolean): File {
        var url = photo.url
        repeat(6) {
            check(trusted(url)) { "Indirizzo dell’immagine non valido." }; check(!cancelled()) { "Operazione annullata." }
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            active = connection
            try {
                connection.instanceFollowRedirects = false; connection.connectTimeout = 15000; connection.readTimeout = 25000
                connection.setRequestProperty("Referer", "https://www.instagram.com/")
                if (connection.responseCode in listOf(301, 302, 303, 307, 308)) {
                    val next = connection.getHeaderField("Location") ?: error("Reindirizzamento dell’immagine non valido.")
                    url = URI(url).resolve(next).toString()
                } else {
                    check(connection.responseCode == 200) { "HTTP Error ${connection.responseCode}" }
                    check(connection.contentLengthLong <= 50L * 1024 * 1024) { "Immagine troppo grande (massimo 50 MiB)." }
                    val file = File(directory, "image.part")
                    connection.inputStream.use { input -> file.outputStream().use { output ->
                        var total = 0L; val buffer = ByteArray(65536)
                        while (true) { check(!cancelled()) { "Operazione annullata." }; val count = input.read(buffer); if (count < 0) break; total += count; check(total <= 50L * 1024 * 1024) { "Immagine troppo grande (massimo 50 MiB)." }; output.write(buffer, 0, count) }
                    } }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(file.path, bounds)
                    check(bounds.outWidth > 0 && bounds.outHeight > 0) { "La piattaforma non ha restituito un’immagine valida." }
                    val extension = when (bounds.outMimeType) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"; else -> error("Formato immagine non supportato.") }
                    return File(directory, "media.$extension").also { check(file.renameTo(it)) { "Impossibile preparare l’immagine." } }
                }
            } finally { connection.disconnect(); if (active === connection) active = null }
        }
        error("Troppi reindirizzamenti dell’immagine.")
    }
}
