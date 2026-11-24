package info.cosmonet.flusiva

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

data class MusicPlaylist(val id: String, val name: String, val uris: List<String>)
object PlaylistPolicy {
    fun name(value: String): String = value.trim().also { require(it.isNotEmpty() && it.length <= 80) { "Usa un nome da 1 a 80 caratteri." } }
    fun tracks(values: List<String>): List<String> {
        require(values.size <= 500) { "Una playlist può contenere al massimo 500 brani." }
        values.forEach { require(runCatching { URI(it).scheme == "content" }.getOrDefault(false)) { "Sono ammessi solo file locali scelti nell’app." } }
        return values.distinct()
    }
    fun move(values: List<String>, index: Int, offset: Int): List<String> {
        require(offset in listOf(-1, 1) && index in values.indices && index + offset in values.indices)
        return values.toMutableList().apply { add(index + offset, removeAt(index)) }
    }
}
class PlaylistStore(context: Context) {
    private val preferences = context.getSharedPreferences("music-playlists", Context.MODE_PRIVATE)
    @Synchronized fun all(): List<MusicPlaylist> {
        val array = runCatching { JSONArray(preferences.getString("playlists", "[]")) }.getOrDefault(JSONArray())
        return (0 until minOf(array.length(), 50)).mapNotNull { i -> runCatching {
            val item = array.getJSONObject(i); val tracks = item.getJSONArray("uris")
            MusicPlaylist(item.getString("id"), PlaylistPolicy.name(item.getString("name")), PlaylistPolicy.tracks((0 until tracks.length()).map { tracks.getString(it) }))
        }.getOrNull() }
    }
    private fun save(values: List<MusicPlaylist>) {
        val array = JSONArray(); values.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("uris", JSONArray(it.uris))) }
        check(preferences.edit().putString("playlists", array.toString()).commit()) { "Impossibile salvare le playlist sul telefono." }
    }
    @Synchronized fun create(name: String): MusicPlaylist {
        val values = all(); require(values.size < 50) { "Puoi creare al massimo 50 playlist." }
        val valid = PlaylistPolicy.name(name); require(values.none { it.name.equals(valid, true) }) { "Esiste già una playlist con questo nome." }
        val item = MusicPlaylist(UUID.randomUUID().toString(), valid, emptyList()); save(values + item); return item
    }
    @Synchronized fun rename(id: String, name: String) {
        val valid = PlaylistPolicy.name(name); val values = all(); require(values.any { it.id == id })
        require(values.none { it.id != id && it.name.equals(valid, true) }) { "Esiste già una playlist con questo nome." }
        save(values.map { if (it.id == id) it.copy(name = valid) else it })
    }
    @Synchronized fun replace(id: String, uris: List<String>) { val tracks = PlaylistPolicy.tracks(uris); val values = all(); require(values.any { it.id == id }); save(values.map { if (it.id == id) it.copy(uris = tracks) else it }) }
    @Synchronized fun delete(id: String) { save(all().filterNot { it.id == id }) }
}
