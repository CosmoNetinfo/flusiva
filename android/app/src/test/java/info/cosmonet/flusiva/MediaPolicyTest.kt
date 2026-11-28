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
    @Test fun normalizesInstagramAndFacebookWithoutTracking() {
        for (url in listOf("https://instagram.com/reel/Chunk8-jurw/?igsh=tracking", "https://m.instagram.com/creator/reels/Chunk8-jurw/")) {
            assertEquals(MediaLink("https://www.instagram.com/reel/Chunk8-jurw/", "Instagram"), MediaPolicy.identify(url))
        }
        assertEquals("https://www.instagram.com/p/aye83DjauH/", MediaPolicy.identify("https://instagram.com/p/aye83DjauH/?img_index=1").url)
        for (url in listOf("https://instagram.com/share/BAWVJE-rtk", "https://instagram.com/share/p/BACiUUUYQV/", "https://instagram.com/share/reel/BARSSL4rTu/")) assertTrue(MediaPolicy.identify(url).short)
        assertEquals("image/jpeg", MediaPolicy.mime("jpg")); assertEquals("image/png", MediaPolicy.mime("png")); assertEquals("image/webp", MediaPolicy.mime("webp"))
        for (url in listOf("https://m.facebook.com/watch/?v=274175099429670&ref=share", "https://facebook.com/reel/274175099429670/", "https://web.facebook.com/creator/videos/274175099429670/", "https://facebook.com/creator/videos/video-title/274175099429670/", "https://facebook.com/video/video.php?v=274175099429670")) {
            assertEquals(MediaLink("https://www.facebook.com/video.php?v=274175099429670", "Facebook"), MediaPolicy.identify(url))
        }
        assertEquals("https://www.facebook.com/creator/posts/pfbidAbc12345/", MediaPolicy.identify("https://facebook.com/creator/posts/pfbidAbc12345/?tracking=1").url)
        for (url in listOf("https://fb.watch/Abc123/?tracking=1", "https://facebook.com/share/v/Abc123/", "https://m.facebook.com/share/r/Abc123/")) assertTrue(MediaPolicy.identify(url).short)
        for (url in listOf("https://instagram.com/creator/", "https://instagram.com/stories/creator/123", "https://instagram.com/share/stories/abc", "https://instagram.com/reels/audio/123", "https://instagram.com.evil.test/reel/abc", "https://facebook.com.evil.test/watch?v=123", "https://facebook.com/creator/videos/", "https://facebook.com/share/p/Abc123/", "https://facebook.com/watch/?v=bad", "https://facebook.com/l.php?u=https://evil.test", "https://fb.watch.evil.test/abc", "https://facebook.com:443/reel/123", "https://user:pass@facebook.com/reel/123")) {
            assertTrue(url, runCatching { MediaPolicy.identify(url) }.isFailure)
        }
        assertTrue(MediaPolicy.friendly(Exception("Not accessible without being logged-in")).contains("accesso"))
        assertTrue(MediaPolicy.friendly(Exception("No video in this post")).contains("singolo video"))
    }
    @Test fun maximumQualityPreservesBestSource() {
        val video = MediaPolicy.options("mp4", true, "128", "480")
        assertTrue(video.contains("bv*+ba/b[vcodec!=none]")); assertFalse(video.any { it.contains("height<=") })
        assertTrue(MediaPolicy.options("mp4", false, "320", "720").any { it.contains("height<=720") })
        assertTrue(MediaPolicy.options("mp3", true, "128", "best").contains("320K"))
        assertTrue(MediaPolicy.options("mp3", false, "192", "best").contains("192K"))
        assertTrue(runCatching { MediaPolicy.options("mp3", true, "320;cmd", "best") }.isFailure)
    }
    @Test fun recentInspectionCannotReuseOtherOrExpiredLinks() {
        var time = 100L
        val cache = RecentInspection<String> { time }
        cache.put("instagram/post-1", "metadata")
        assertEquals("metadata", cache.get("instagram/post-1"))
        assertNull(cache.get("instagram/post-2"))
        time += 90001; assertNull(cache.get("instagram/post-1"))
        cache.put("facebook/video-1", "new metadata")
        assertNull(cache.get("instagram/post-1")); assertEquals("new metadata", cache.get("facebook/video-1"))
        cache.clear(); assertNull(cache.get("facebook/video-1"))
    }
    @Test fun fileNamesCannotEscapeDestination() {
        val name = MediaPolicy.fileName("../song: /test\\track", "mp3")
        assertFalse(name.contains('/')); assertFalse(name.contains('\\')); assertFalse(name.contains(':')); assertTrue(name.endsWith(".mp3"))
        assertTrue(MediaPolicy.fileName("", "mp4").startsWith("Flusiva"))
    }
}
