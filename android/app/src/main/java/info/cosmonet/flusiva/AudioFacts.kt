package info.cosmonet.flusiva

import android.content.Context
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

data class AudioFacts(val title: String, val artist: String?, val mime: String?, val bitrate: Int?, val sampleRate: Int?, val channels: Int?, val bitDepth: Int?, val duration: Long?, val bytes: Long?) {
    fun details(): String = buildList {
        add("Codec: ${mime ?: "non indicato"}")
        add("Bitrate: ${bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "non indicato"}")
        add("Frequenza: ${sampleRate?.let { "${it / 1000.0} kHz" } ?: "non indicata"}")
        add("Canali: ${channels?.let { when (it) { 1 -> "mono"; 2 -> "stereo"; else -> it.toString() } } ?: "non indicati"}")
        if (bitDepth != null) add("PCM: $bitDepth bit")
        if (bytes != null) add("Dimensione: ${"%.1f".format(bytes / 1048576.0)} MB")
    }.joinToString("\n")
}
object AudioInspection {
    fun read(context: Context, uri: Uri): AudioFacts {
        require(uri.scheme == "content") { "Scegli un file audio dal telefono." }
        var name = "Brano"; var size: Long? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) { name = cursor.getString(0) ?: name; if (!cursor.isNull(1)) size = cursor.getLong(1) } }
        val extractor = MediaExtractor()
        var audio: MediaFormat? = null; var video = false
        try {
            extractor.setDataSource(context, uri, null)
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index); val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/") && audio == null) audio = format
                if (mime.startsWith("video/")) video = true
            }
        } finally { extractor.release() }
        val format = audio ?: error("Questo file non contiene una traccia audio leggibile.")
        fun number(key: String): Int? = if (format.containsKey(key)) runCatching { format.getInteger(key) }.getOrNull()?.takeIf { it > 0 } else null
        val retriever = MediaMetadataRetriever()
        var title: String? = null; var artist: String? = null; var duration: Long? = null; var bitrate: Int? = number(MediaFormat.KEY_BIT_RATE)
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            // The container bitrate also includes video. Never label that as the audio bitrate.
            if (bitrate == null && !video) bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.takeIf { it > 0 }
        } catch (_: Exception) { /* Track metadata remains usable when tags are absent. */ } finally { retriever.release() }
        val mime = format.getString(MediaFormat.KEY_MIME)
        val bits = if (mime == "audio/raw") when (number(MediaFormat.KEY_PCM_ENCODING)) { AudioFormat.ENCODING_PCM_16BIT -> 16; AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24; AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32; else -> null } else null
        return AudioFacts(title?.takeIf { it.isNotBlank() } ?: name, artist, mime, bitrate, number(MediaFormat.KEY_SAMPLE_RATE), number(MediaFormat.KEY_CHANNEL_COUNT), bits, duration, size)
    }
}
