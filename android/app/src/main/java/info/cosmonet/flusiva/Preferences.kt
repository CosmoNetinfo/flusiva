package info.cosmonet.flusiva

import android.content.Context
class Preferences(context: Context) {
    private val store = context.getSharedPreferences("flusiva", Context.MODE_PRIVATE)
    var maximum: Boolean get() = store.getBoolean("maximum", true); set(value) { store.edit().putBoolean("maximum", value).apply() }
    var format: String get() = store.getString("format", "mp4")!!; set(value) { require(value in listOf("mp4", "mp3")); store.edit().putString("format", value).apply() }
    var audio: String get() = store.getString("audio", "320")!!; set(value) { require(value in listOf("128", "192", "256", "320")); store.edit().putString("audio", value).apply() }
    var video: String get() = store.getString("video", "best")!!; set(value) { require(value in listOf("best", "2160", "1080", "720", "480")); store.edit().putString("video", value).apply() }
    fun reset() { store.edit().clear().apply() }
}
