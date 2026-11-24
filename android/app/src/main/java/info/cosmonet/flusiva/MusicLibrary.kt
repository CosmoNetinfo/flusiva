package info.cosmonet.flusiva

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray

data class Song(val uri: Uri, val name: String)
object MusicLibrary {
    private const val LIMIT = 500
    fun list(context: Context): List<Song> {
        val songs = linkedMapOf<String, Song>()
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Downloads.RELATIVE_PATH} IN (?, ?) AND ${MediaStore.Downloads.MIME_TYPE} LIKE ?", arrayOf("Download/Flusiva", "Download/Flusiva/", "audio/%"), "${MediaStore.Downloads.DATE_ADDED} DESC")?.use { cursor ->
            while (cursor.moveToNext() && songs.size < LIMIT) {
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0))
                songs[uri.toString()] = Song(uri, cursor.getString(1) ?: "Brano")
            }
        }
        val stored = runCatching { JSONArray(context.getSharedPreferences("music-library", Context.MODE_PRIVATE).getString("imports", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until minOf(stored.length(), LIMIT)) {
            val uri = Uri.parse(stored.optString(i)); if (uri.scheme != "content" || songs.containsKey(uri.toString())) continue
            // Inspect only the display name here; decoding metadata happens when selected.
            runCatching { context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) songs[uri.toString()] = Song(uri, it.getString(0) ?: "Brano") } }
        }
        return songs.values.take(LIMIT)
    }
    fun remember(context: Context, uris: List<Uri>) {
        val store = context.getSharedPreferences("music-library", Context.MODE_PRIVATE)
        val old = runCatching { JSONArray(store.getString("imports", "[]")) }.getOrDefault(JSONArray())
        val values = linkedSetOf<String>()
        for (uri in uris) { require(uri.scheme == "content"); values.add(uri.toString()) }
        for (i in 0 until old.length()) values.add(old.getString(i))
        store.edit().putString("imports", JSONArray(values.take(LIMIT)).toString()).apply()
    }
}
