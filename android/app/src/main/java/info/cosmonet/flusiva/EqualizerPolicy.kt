package info.cosmonet.flusiva

object EqualizerPolicy {
    val presets = listOf("Neutro", "Bassi", "Voce", "Brillante")
    fun gain(preset: String, frequencyHz: Int, min: Int, max: Int): Int {
        require(preset in presets && min <= max && frequencyHz > 0)
        val db = when (preset) {
            "Bassi" -> when { frequencyHz < 150 -> 4; frequencyHz < 500 -> 2; else -> 0 }
            "Voce" -> when { frequencyHz < 200 -> -2; frequencyHz in 500..4000 -> 3; else -> 0 }
            "Brillante" -> if (frequencyHz >= 4000) 3 else 0
            else -> 0
        }
        return (db * 100).coerceIn(min, max)
    }
    fun time(milliseconds: Long): String { val seconds = milliseconds.coerceAtLeast(0) / 1000; return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60) }
}
