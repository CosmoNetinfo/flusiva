package info.cosmonet.flusiva

import org.junit.Assert.*
import org.junit.Test
class MediaPolicyTest {
    @Test fun normalizesPlatformsAndSharedText() {
        assertEquals("https://www.youtube.com/watch?v=aqz-KE-bpKQ", MediaPolicy.identify("https://youtu.be/aqz-KE-bpKQ?si=tracking").url)
        assertEquals("TikTok", MediaPolicy.identify("https://www.tiktok.com/@creator/video/1234567890123").platform)
        assertTrue(MediaPolicy.identify("https://vm.tiktok.com/Z12345/").short)
        assertEquals("https://vimeo.com/1234", MediaPolicy.identify("https://player.vimeo.com/video/1234").url)
        assertEquals("Dailymotion", MediaPolicy.identify("https://dai.ly/x12345").platform)
        assertEquals("SoundCloud", MediaPolicy.identify("https://soundcloud.com/artist/song").platform)
        assertEquals("https://youtu.be/aqz-KE-bpKQ", MediaPolicy.sharedLink("Guarda questo: https://youtu.be/aqz-KE-bpKQ"))
    }
    @Test fun rejectsUnexpectedAndCommandInputs() {
        for (input in listOf("file:///etc/passwd", "https://youtube.com.evil.test/watch?v=aqz-KE-bpKQ", "https://user:pass@youtu.be/aqz-KE-bpKQ", "https://youtu.be:443/aqz-KE-bpKQ", "https://youtube.com/playlist?list=123", "https://open.spotify.com/track/123", "--exec rm", "https://soundcloud.com/artist/sets")) {
            assertTrue(input, runCatching { MediaPolicy.identify(input) }.isFailure)
        }
    }
    @Test fun maximumQualityPreservesBestSource() {
        val video = MediaPolicy.options("mp4", true, "128", "480")
        assertTrue(video.contains("bv*+ba/b[vcodec!=none]")); assertFalse(video.any { it.contains("height<=") })
        assertTrue(MediaPolicy.options("mp4", false, "320", "720").any { it.contains("height<=720") })
        assertTrue(MediaPolicy.options("mp3", true, "128", "best").contains("320K"))
        assertTrue(MediaPolicy.options("mp3", false, "192", "best").contains("192K"))
        assertTrue(runCatching { MediaPolicy.options("mp3", true, "320;cmd", "best") }.isFailure)
    }
    @Test fun fileNamesCannotEscapeDestination() {
        val name = MediaPolicy.fileName("../song: /test\\track", "mp3")
        assertFalse(name.contains('/')); assertFalse(name.contains('\\')); assertFalse(name.contains(':')); assertTrue(name.endsWith(".mp3"))
        assertTrue(MediaPolicy.fileName("", "mp4").startsWith("Flusiva"))
    }
}
