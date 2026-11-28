package info.cosmonet.flusiva

/** Only the last successful inspection, in memory; signed URLs expire quickly. */
class RecentInspection<T>(private val now: () -> Long) {
    private data class Entry<T>(val key: T, val json: String, val at: Long)
    private var entry: Entry<T>? = null
    @Synchronized fun put(key: T, json: String) { entry = Entry(key, json, now()) }
    @Synchronized fun clear() { entry = null }
    @Synchronized fun get(key: T): String? = entry?.takeIf { it.key == key && now() - it.at in 0L..90000L }?.json
}
