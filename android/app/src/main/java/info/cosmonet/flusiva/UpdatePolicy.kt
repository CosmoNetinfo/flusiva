package info.cosmonet.flusiva

object UpdatePolicy {
    fun code(version: String): Long {
        require(Regex("(?:0|[1-9]\\d{0,2})\\.(?:0|[1-9]\\d{0,2})\\.(?:0|[1-9]\\d{0,2})").matches(version)) { "Versione aggiornamento non valida." }
        val parts = version.split('.').map(String::toLong)
        return parts[0] * 1000000 + parts[1] * 1000 + parts[2]
    }
    fun apkUrl(version: String) = "https://github.com/CosmoNetinfo/flusiva/releases/download/v$version/Flusiva-$version-android.apk".also { code(version) }
    fun checksum(text: String, name: String): String {
        val matches = text.lineSequence().map { it.trim().split(Regex("\\s+"), limit = 2) }.filter { it.size == 2 && it[1].removePrefix("*") == name }.toList()
        require(matches.size == 1 && Regex("[a-fA-F0-9]{64}").matches(matches[0][0])) { "Checksum dell’aggiornamento mancante o non valido." }
        return matches[0][0].lowercase()
    }
    fun validHash(value: String) = Regex("[a-f0-9]{64}").matches(value)
}
