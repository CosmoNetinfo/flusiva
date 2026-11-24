package info.cosmonet.flusiva

import org.junit.Assert.*
import org.junit.Test
class MusicPolicyTest {
    @Test fun playlistsPreserveOrderAndValidateLocalReferences() {
        assertEquals(listOf("content://media/1", "content://media/2"), PlaylistPolicy.tracks(listOf("content://media/1", "content://media/2", "content://media/1")))
        assertEquals(listOf("b", "a", "c"), PlaylistPolicy.move(listOf("a", "b", "c"), 0, 1))
        assertEquals(listOf("a", "c", "b"), PlaylistPolicy.move(listOf("a", "b", "c"), 2, -1))
        assertTrue(runCatching { PlaylistPolicy.move(listOf("a"), 0, -1) }.isFailure)
        assertTrue(runCatching { PlaylistPolicy.tracks(listOf("https://example.com/song.mp3")) }.isFailure)
        assertTrue(runCatching { PlaylistPolicy.tracks(listOf("file:///private/song.mp3")) }.isFailure)
        assertTrue(runCatching { PlaylistPolicy.name(" ") }.isFailure)
        assertEquals("Viaggio", PlaylistPolicy.name(" Viaggio "))
    }
    @Test fun equalizerPresetsRespectDeviceRangeAndNeutralIsFlat() {
        for (preset in EqualizerPolicy.presets) for (frequency in listOf(60, 230, 910, 3600, 14000)) assertTrue(EqualizerPolicy.gain(preset, frequency, -200, 200) in -200..200)
        assertEquals(0, EqualizerPolicy.gain("Neutro", 60, -1500, 1500))
        assertTrue(EqualizerPolicy.gain("Bassi", 60, -1500, 1500) > 0)
        assertEquals(0, EqualizerPolicy.gain("Bassi", 14000, -1500, 1500))
        assertTrue(runCatching { EqualizerPolicy.gain("inventato", 100, -1500, 1500) }.isFailure)
    }
    @Test fun timesDoNotShowNegativeOrOverflowAtOneHour() {
        assertEquals("0:00", EqualizerPolicy.time(-1)); assertEquals("1:05", EqualizerPolicy.time(65000)); assertEquals("1:01:05", EqualizerPolicy.time(3665000))
    }
}
