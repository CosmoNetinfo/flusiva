package info.cosmonet.flusiva

import android.content.*
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class UpdateFlowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    @Test fun onlyMatchingApkHashPackageVersionAndSignerAreAccepted() {
        val own = File(context.applicationInfo.sourceDir)
        val expected = AppRelease(BuildConfig.VERSION_NAME, hash(own), own.length())
        AndroidUpdates.verifyArchive(context, own, expected)
        assertTrue(runCatching { AndroidUpdates.verifyArchive(context, own, expected.copy(sha256 = "0".repeat(64))) }.isFailure)
        assertTrue(runCatching { AndroidUpdates.verifyArchive(context, own, expected.copy(version = "0.0.1")) }.isFailure)
        val foreign = File(instrumentation.context.applicationInfo.sourceDir)
        assertTrue(runCatching { AndroidUpdates.verifyArchive(context, foreign, AppRelease(BuildConfig.VERSION_NAME, hash(foreign), foreign.length())) }.isFailure)
        val wrongSigner = File(context.cacheDir, "wrong-signer.apk")
        try {
            instrumentation.context.assets.open("wrong-signer.apk").use { input -> wrongSigner.outputStream().use { input.copyTo(it) } }
            val failure = runCatching { AndroidUpdates.verifyArchive(context, wrongSigner, AppRelease("0.9.0", hash(wrongSigner), wrongSigner.length())) }.exceptionOrNull()
            assertNotNull(failure); assertTrue(failure!!.message.orEmpty().contains("firma"))
        } finally { wrongSigner.delete() }
        val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates/provider-test.apk")
        target.parentFile!!.mkdirs()
        try {
            target.writeBytes(byteArrayOf(1, 2, 3))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", target)
            assertEquals("content", uri.scheme)
            assertEquals(3L, context.contentResolver.openFileDescriptor(uri, "r")!!.use { it.statSize })
            assertTrue(runCatching { FileProvider.getUriForFile(context, "${context.packageName}.updates", File(context.cacheDir, "outside.apk")) }.isFailure)
        } finally { target.delete() }
    }
    @Test fun updatePageShowsDownloadAndRequiresVerificationBeforeInstall() {
        val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
        Preferences(context).automaticUpdates = false
        prefs.edit().clear().putString("version", "99.0.0").putString("sha256", "a".repeat(64)).putLong("size", 170000000).commit()
        val device = UiDevice.getInstance(instrumentation)
        device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
        try {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            val navigation = device.wait(Until.findObject(By.textStartsWith("Impostazioni")), 10000)
            assertNotNull(navigation); navigation.click()
            var download = device.findObject(By.text("Scarica aggiornamento"))
            for (attempt in 0..5) {
                if (download != null) break
                device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.8f)
                download = device.wait(Until.findObject(By.text("Scarica aggiornamento")), 1000)
            }
            assertNotNull(download); assertTrue(download!!.isEnabled)
            assertNull(device.findObject(By.text("Installa aggiornamento")))
            assertNotNull(device.findObject(By.text("Cerca aggiornamenti all’avvio")))
        } finally {
            prefs.edit().clear().commit(); AndroidUpdates.refresh(context)
            Preferences(context).automaticUpdates = false
        }
    }
}
