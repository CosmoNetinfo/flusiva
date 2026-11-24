package info.cosmonet.flusiva

import android.content.Intent
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
    @Test fun nativeEngineAndStorageWork() {
        Engine.initialize(context)
        val version = YoutubeDL.execute(YoutubeDLRequest(emptyList()).addOption("--version")).out.trim()
        assertEquals("2026.11.23", version)
        // Exercise the actual Python, QuickJS and FFmpeg payload, including MP3 encoding.
        val native = File(context.applicationInfo.nativeLibraryDir)
        val libraries = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
        val quick = ProcessBuilder(File(native, "libqjs.so").path, "-e", "console.log(6*7)").redirectErrorStream(true).start()
        assertTrue(quick.inputStream.bufferedReader().readText().contains("42")); assertEquals(0, quick.waitFor())
        val sample = File(context.cacheDir, "flusiva-test.mp3")
        val process = ProcessBuilder(File(native, "libffmpeg.so").path, "-hide_banner", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-codec:a", "libmp3lame", "-b:a", "320k", sample.path).redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = libraries.path }.start()
        val log = process.inputStream.bufferedReader().readText(); assertEquals(log, 0, process.waitFor()); assertTrue(sample.length() > 10000)
        val movie = File(context.cacheDir, "flusiva-test.mp4")
        val video = ProcessBuilder(File(native, "libffmpeg.so").path, "-hide_banner", "-y", "-f", "lavfi", "-i", "color=c=blue:s=128x128:r=10:d=1", "-i", sample.path, "-c:v", "mpeg4", "-c:a", "aac", "-shortest", movie.path).redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = libraries.path }.start()
        val videoLog = video.inputStream.bufferedReader().readText(); assertEquals(videoLog, 0, video.waitFor())
        val retriever = MediaMetadataRetriever()
        try { retriever.setDataSource(movie.path); assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)); assertEquals("yes", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)) } finally { retriever.release(); movie.delete() }
        val saved = Downloads.save(context, sample, "Flusiva test", "mp3") { false }
        try { assertTrue(context.contentResolver.openInputStream(saved)!!.use { it.readBytes().size } > 10000) } finally { context.contentResolver.delete(saved, null, null); sample.delete() }
        val cancelled = File(context.cacheDir, "cancel-test.mp3").apply { writeBytes(ByteArray(32)) }
        try { assertTrue(runCatching { Downloads.save(context, cancelled, "Flusiva cancelled", "mp3") { true } }.isFailure) } finally { cancelled.delete() }
    }
    @Test fun sharedLinkAndSettingsAreUsable() {
        val device = UiDevice.getInstance(instrumentation)
        context.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Guarda https://youtu.be/aqz-KE-bpKQ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertNotNull(device.wait(Until.findObject(By.text("Analizza il link")), 10000))
        assertNotNull(device.findObject(By.text("https://youtu.be/aqz-KE-bpKQ")))
        val images = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        device.takeScreenshot(File(images, "android-download.png"))
        device.findObject(By.text("Impostazioni")).click()
        assertNotNull(device.wait(Until.findObject(By.text("Usa la qualità massima")), 5000))
        device.findObject(By.text("Usa la qualità massima")).click()
        assertFalse(Preferences(context).maximum)
        device.takeScreenshot(File(images, "android-settings.png"))
        Preferences(context).reset()
    }
}
