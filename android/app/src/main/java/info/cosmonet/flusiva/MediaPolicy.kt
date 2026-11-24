package info.cosmonet.flusiva

import java.net.URI
import java.net.URLDecoder

data class MediaLink(val url: String, val platform: String, val short: Boolean = false)
object MediaPolicy {
    private fun matches(path: String, regex: String) = Regex(regex).matches(path)
    fun identify(value: String): MediaLink {
        require(value.length <= 2048) { "Il link è troppo lungo." }
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: error("Incolla un link completo https://.")
        require(uri.scheme in listOf("https", "http") && uri.userInfo == null && uri.port == -1) { "Il link non è valido." }
        val host = uri.host?.lowercase() ?: error("Il link non è valido.")
        val path = uri.path ?: ""
        if (host in listOf("spotify.com", "www.spotify.com", "open.spotify.com", "spotify.link")) error("Spotify non è supportato.")
        if (host in listOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")) {
            val id = if (host == "youtu.be") path.removePrefix("/") else if (path == "/watch") {
                uri.rawQuery.orEmpty().split('&').map { it.split('=', limit = 2) }.firstOrNull { it[0] == "v" }?.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") }
            } else Regex("^/(?:shorts|live)/([A-Za-z0-9_-]{11})/?$").find(path)?.groupValues?.get(1)
            require(id != null && matches(id, "[A-Za-z0-9_-]{11}")) { "Scegli un singolo video YouTube." }
            return MediaLink("https://www.youtube.com/watch?v=$id", "YouTube")
        }
        if (host in listOf("tiktok.com", "www.tiktok.com", "m.tiktok.com") && matches(path, "/@[\\w.-]+/video/\\d{10,25}/?")) return MediaLink("https://www.tiktok.com$path", "TikTok")
        if ((host in listOf("vm.tiktok.com", "vt.tiktok.com") && matches(path, "/[A-Za-z0-9_-]+/?")) || (host in listOf("tiktok.com", "www.tiktok.com") && matches(path, "/t/[A-Za-z0-9_-]+/?"))) return MediaLink("https://$host$path", "TikTok", true)
        if (host in listOf("vimeo.com", "www.vimeo.com", "player.vimeo.com")) Regex("^/(?:video/)?(\\d{1,15})/?$").find(path)?.let { return MediaLink("https://vimeo.com/${it.groupValues[1]}", "Vimeo") }
        if (host in listOf("dailymotion.com", "www.dailymotion.com", "geo.dailymotion.com", "dai.ly")) {
            val regex = if (host == "dai.ly") "^/([A-Za-z0-9]{3,20})/?$" else "^/(?:embed/)?video/([A-Za-z0-9]{3,20})(?:_[^/]*)?/?$"
            Regex(regex).find(path)?.let { return MediaLink("https://www.dailymotion.com/video/${it.groupValues[1]}", "Dailymotion") }
        }
        if (host in listOf("soundcloud.com", "www.soundcloud.com", "m.soundcloud.com")) {
            val parts = path.trim('/').split('/')
            if (parts.size == 2 && parts.all { matches(it, "[\\w-]+") } && parts[0] !in listOf("discover", "search", "you", "charts", "stations") && parts[1] !in listOf("sets", "tracks", "likes", "reposts", "albums")) return MediaLink("https://soundcloud.com/${parts.joinToString("/")}", "SoundCloud")
        }
        if (host in listOf("on.soundcloud.com", "soundcloud.app.goo.gl") && matches(path, "/[A-Za-z0-9_-]+/?")) return MediaLink("https://$host$path", "SoundCloud", true)
        error("Usa un singolo contenuto di YouTube, TikTok, Vimeo, Dailymotion o SoundCloud.")
    }
    fun sharedLink(text: String): String = Regex("https?://[^\\s<>]+").find(text)?.value?.trimEnd('.', ',', ')') ?: text.trim()
    fun options(format: String, maximum: Boolean, audio: String, video: String, hasAudio: Boolean = true): List<String> {
        require(format in listOf("mp3", "mp4"))
        require(audio in listOf("128", "192", "256", "320") && video in listOf("best", "2160", "1080", "720", "480"))
        if (format == "mp3") return listOf("-f", "bestaudio/best", "-x", "--audio-format", "mp3", "--audio-quality", if (maximum) "320K" else "${audio}K", "--format-sort", "quality,abr,asr", "--embed-metadata")
        val limit = if (maximum || video == "best") "" else "[height<=$video]"
        val preferred = if (hasAudio) "bv*$limit+ba" else "bv*$limit"
        return listOf("-f", "$preferred/b[vcodec!=none]$limit", "--format-sort", "res,fps", "--merge-output-format", "mp4", "--remux-video", "mp4", "--embed-metadata")
    }
    fun fileName(title: String, format: String): String = title.replace(Regex("[\\p{Cntrl}/\\\\:*?\"<>|]"), " ").trim().take(120).ifBlank { "Flusiva" } + ".$format"
    fun friendly(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            error is InterruptedException || message.contains("annull", true) -> "Operazione annullata."
            Regex("403|401|429|sign.?in|log.?in|cookie|bot", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "La piattaforma limita la richiesta o richiede l’accesso. Prova un contenuto pubblico o riprova più tardi."
            Regex("timeout|timed out|connect|network|resolve", RegexOption.IGNORE_CASE).containsMatchIn(message) -> "Connessione non riuscita. Controlla Internet e riprova."
            else -> message.take(250).ifBlank { "Operazione non riuscita. Riprova." }
        }
    }
}
