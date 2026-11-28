package info.cosmonet.flusiva

import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import android.media.MediaMetadataRetriever
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun capture(device: UiDevice, name: String) {
        val file = File(context.cacheDir, name)
        check(device.takeScreenshot(file)) { "Screenshot non disponibile" }
        val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "image/png"); put(MediaStore.Downloads.RELATIVE_PATH, "Download/Flusiva-tests") }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use { output -> file.inputStream().use { it.copyTo(output) } }
        file.delete()
    }
    @Test fun nativeEngineAndStorageWork() {
        Engine.initialize(context)
        val version = YoutubeDL.execute(YoutubeDLRequest(emptyList()).addOption("--version")).out.trim()
        assertEquals("2026.11.23", version)
        // Exercise the actual Python, QuickJS and FFmpeg payload, including MP3 encoding.
        val native = File(context.applicationInfo.nativeLibraryDir)
        val libraries = File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr/lib").path + ":" + File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib").path
        val quick = ProcessBuilder(File(native, "libqjs.so").path, "-e", "console.log(6*7)").redirectErrorStream(true).start()
        assertTrue(quick.inputStream.bufferedReader().readText().contains("42")); assertEquals(0, quick.waitFor())
        val sample = File(context.cacheDir, "flusiva-test.mp3")
        val process = ProcessBuilder(File(native, "libffmpeg.so").path, "-hide_banner", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-codec:a", "libmp3lame", "-b:a", "320k", sample.path).redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = libraries }.start()
        val log = process.inputStream.bufferedReader().readText(); assertEquals(log, 0, process.waitFor()); assertTrue(sample.length() > 10000)
        val movie = File(context.cacheDir, "flusiva-test.mp4")
        val video = ProcessBuilder(File(native, "libffmpeg.so").path, "-hide_banner", "-y", "-f", "lavfi", "-i", "color=c=blue:s=128x128:r=10:d=1", "-i", sample.path, "-c:v", "mpeg4", "-c:a", "aac", "-shortest", movie.path).redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = libraries }.start()
        val videoLog = video.inputStream.bufferedReader().readText(); assertEquals(videoLog, 0, video.waitFor())
        Engine.verifyOutput(movie, "mp4")
        assertTrue(runCatching { Engine.verifyOutput(sample, "mp4") }.isFailure)
        val retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(movie.path); assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)); assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)) } finally { retriever.release() }
        val extracted = File(context.cacheDir, "flusiva-extracted.mp3")
        try {
            // File URLs are enabled only in this offline fixture; public links reject them.
            val metadata = File(context.cacheDir, "fixture-info.json")
            try {
                metadata.writeText(YoutubeDL.execute(Engine.request(movie.toURI().toString()).addOption("--enable-file-urls").addOption("--dump-single-json").addOption("--skip-download")).out)
                val request = Engine.requestFromInfo(metadata).addOption("--enable-file-urls").addCommands(MediaPolicy.options("mp3", true, "128", "480")).addOption("-o", File(context.cacheDir, "flusiva-extracted.%(ext)s").path)
                assertFalse(request.buildCommand().contains(movie.toURI().toString()))
                YoutubeDL.execute(request)
            } finally { metadata.delete() }
            assertTrue("Il percorso completo yt-dlp/FFprobe/FFmpeg deve produrre MP3", extracted.length() > 30000)
        } finally { movie.delete(); extracted.delete(); File(context.cacheDir, "flusiva-extracted.mp4").delete() }
        val saved = Downloads.save(context, sample, "Flusiva test", "mp3") { false }
        try { assertTrue(context.contentResolver.openInputStream(saved)!!.use { it.readBytes().size } > 10000); val facts = AudioInspection.read(context, saved); assertEquals(44100, facts.sampleRate); assertTrue(facts.bitrate!! >= 300000); assertEquals("audio/mpeg", facts.mime) } finally { context.contentResolver.delete(saved, null, null); sample.delete() }
        val cancelled = File(context.cacheDir, "cancel-test.mp3").apply { writeBytes(ByteArray(32)) }
        try { assertTrue(runCatching { Downloads.save(context, cancelled, "Flusiva cancelled", "mp3") { true } }.isFailure) } finally { cancelled.delete() }
    }
    @Test fun instagramPhotosCanBeChosenAndSavedAsImages() {
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
        Preferences(context).reset()
        val url = "https://www.instagram.com/p/BqvsDleB3lV/"
        val raw = org.json.JSONObject("""{"id":"photo","entries":[{"flusiva_media_type":"image","thumbnails":[{"url":"https://scontent.cdninstagram.com/small.jpg","width":320,"height":320},{"url":"https://scontent.cdninstagram.com/original.jpg","width":1080,"height":1350}]},{"flusiva_media_type":"image","thumbnails":[{"url":"https://scontent.cdninstagram.com/second.jpg","width":1080,"height":1080}]},{"flusiva_media_type":"video","thumbnails":[{"url":"https://scontent.cdninstagram.com/poster.jpg"}]}]}""")
        val photos = InstagramPhotos.collect(raw)
        assertEquals(2, photos.size); assertEquals(1080, photos[0].width)
        assertFalse(InstagramPhotos.trusted("https://cdninstagram.com.evil.test/image.jpg"))
        assertFalse(InstagramPhotos.trusted("http://127.0.0.1/secret"))
        context.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertNotNull(device.wait(Until.findObject(By.text("Fonte riconosciuta: Instagram")), 10000))
        try {
            instrumentation.runOnMainSync { DownloadState.set(DownloadSnapshot(info = MediaInfo(url, "Instagram", "Foto di prova", "Fixture locale", 0, false, false, url, photos), message = "2 foto · scegli quella da salvare")) }
            val button = device.wait(Until.findObject(By.text("Scarica foto da Instagram · 2 foto")), 5000)
            assertNotNull(button); assertTrue(button.isEnabled)
            capture(device, "android-photos.png")
            button.click(); assertNotNull(device.wait(Until.findObject(By.text("Scegli la foto da scaricare")), 5000))
            assertNotNull(device.findObject(By.text("Foto 2 · 1080×1080"))); device.pressBack()
            val file = File(context.cacheDir, "fixture.jpg")
            val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
            try { file.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)) } } finally { bitmap.recycle() }
            try {
                val saved = Downloads.save(context, file, "Foto di prova", "jpg") { false }
                try { assertEquals("image/jpeg", context.contentResolver.getType(saved)); assertTrue(context.contentResolver.openInputStream(saved)!!.use { it.readBytes().isNotEmpty() }) } finally { context.contentResolver.delete(saved, null, null) }
            } finally { file.delete() }
        } finally { instrumentation.runOnMainSync { DownloadState.set(DownloadSnapshot()) }; Preferences(context).reset() }
    }
    @Test fun metaSharedLinksShowSourceAndMp4() {
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
        try {
            for ((platform, url) in listOf("Instagram" to "https://www.instagram.com/share/BAWVJE-rtk", "Facebook" to "https://www.facebook.com/share/r/Abc123/")) {
                Preferences(context).reset()
                instrumentation.runOnMainSync { DownloadState.set(DownloadSnapshot()) }
                context.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Guarda $url").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                assertNotNull(device.wait(Until.findObject(By.text("Fonte riconosciuta: $platform")), 10000))
                assertNotNull(device.findObject(By.text("Scarica MP4 da $platform")))
                assertFalse(device.findObject(By.text("Scarica MP4 da $platform")).isEnabled)
                capture(device, "android-${platform.lowercase()}.png")
            }
            // An inspected video without a known audio codec must still offer MP4,
            // even when MP3 was previously selected. No network is used by this UI test.
            device.findObject(By.text("Audio MP3")).click()
            val url = MediaPolicy.identify("https://www.facebook.com/share/r/Abc123/").url
            instrumentation.runOnMainSync { DownloadState.set(DownloadSnapshot(info = MediaInfo("https://www.facebook.com/video.php?v=123456", "Facebook", "Video di prova", "Autore", 0, true, false, url), message = "Durata non fornita · verifica sul file")) }
            assertNotNull(device.wait(Until.findObject(By.text("Scarica MP4 da Facebook").enabled(true)), 5000))
        } finally {
            instrumentation.runOnMainSync { DownloadState.set(DownloadSnapshot()) }
            Preferences(context).reset()
        }
    }
    @Test fun sharedLinkAndSettingsAreUsable() {
        Preferences(context).reset()
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
        context.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Guarda https://youtu.be/aqz-KE-bpKQ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        device.wait(Until.hasObject(By.pkg(context.packageName)), 10000)
        capture(device, "android-startup.png")
        assertNotNull(device.wait(Until.findObject(By.text("Analizza il link")), 10000))
        assertNotNull(device.findObject(By.text("https://youtu.be/aqz-KE-bpKQ")))
        assertNotNull(device.findObject(By.text("Fonte riconosciuta: YouTube")))
        assertNotNull(device.findObject(By.text("Scarica MP4 da YouTube")))
        capture(device, "android-download.png")
        device.findObject(By.text("Musica")).click()
        assertNotNull(device.wait(Until.findObject(By.text("Flusiva · Musica")), 5000))
        assertNotNull(device.findObject(By.text("Apri libreria")))
        assertFalse(device.findObject(By.desc("Riproduci")).isEnabled)
        capture(device, "android-music-empty.png")
        device.findObject(By.text("Scarica")).click()
        assertNotNull(device.wait(Until.findObject(By.text("Analizza il link")), 5000))
        device.findObject(By.text("Impostazioni")).click()
        assertNotNull(device.wait(Until.findObject(By.text("Usa la qualità massima")), 5000))
        device.findObject(By.text("Usa la qualità massima")).click()
        val deadline = android.os.SystemClock.elapsedRealtime() + 5000
        while (Preferences(context).maximum && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
        assertFalse(Preferences(context).maximum)
        capture(device, "android-settings.png")
        Preferences(context).reset()
    }
}
