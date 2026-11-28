package info.cosmonet.flusiva

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    @Test fun versionsAreStableAndMonotonic() {
        assertTrue(UpdatePolicy.code("0.10.0") > UpdatePolicy.code("0.9.99"))
        assertEquals(9000L, UpdatePolicy.code("0.9.0"))
        for (value in listOf("0.9.0-beta", "v0.9.0", "0.09.0", "../../file", "1.1000.0", "999999999999.0.0")) assertTrue(value, runCatching { UpdatePolicy.code(value) }.isFailure)
        assertEquals("https://github.com/CosmoNetinfo/flusiva/releases/download/v0.9.0/Flusiva-0.9.0-android.apk", UpdatePolicy.apkUrl("0.9.0"))
    }
    @Test fun checksumMustMatchExactlyOneApk() {
        val hash = "a".repeat(64); val name = "Flusiva-0.9.0-android.apk"
        assertEquals(hash, UpdatePolicy.checksum("$hash  $name\n${"b".repeat(64)}  windows.exe\n", name))
        assertEquals(hash, UpdatePolicy.checksum("$hash *$name", name))
        for (input in listOf("$hash other.apk", "not-a-hash $name", "$hash $name\n$hash $name")) assertTrue(runCatching { UpdatePolicy.checksum(input, name) }.isFailure)
    }
}
