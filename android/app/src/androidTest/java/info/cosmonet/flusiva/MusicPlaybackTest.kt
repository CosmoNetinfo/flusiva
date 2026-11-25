package info.cosmonet.flusiva

import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MusicPlaybackTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun await(message: String, condition: () -> Boolean) { val end = System.currentTimeMillis() + 15000; while (System.currentTimeMillis() < end) { if (condition()) return; Thread.sleep(100) }; fail(message) }
    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun fixture(): Uri {
        val rate = 44100; val samples = rate * 90; val bytes = samples * 2
        val buffer = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(bytes)
        for (sample in 0 until samples) buffer.putShort((kotlin.math.sin(2 * Math.PI * 440 * sample / rate) * 4000).toInt().toShort())
        val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, "Flusiva player test.wav"); put(MediaStore.Downloads.MIME_TYPE, "audio/wav"); put(MediaStore.Downloads.RELATIVE_PATH, "Download/Flusiva") }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use { it.write(buffer.array()) }; return uri
    }
    private fun capture(device: UiDevice, name: String) {
        val file = File(context.cacheDir, name); check(device.takeScreenshot(file))
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "image/png"); put(MediaStore.Downloads.RELATIVE_PATH, "Download/Flusiva-tests") })!!
        context.contentResolver.openOutputStream(uri)!!.use { output -> file.inputStream().use { it.copyTo(output) } }; file.delete()
    }
    @Test fun playlistsPersistAndPlaybackContinuesInBackground() {
        val uri = fixture(); val store = PlaylistStore(context); val playlist = store.create("Ascolto test ${System.currentTimeMillis()}")
        var controller: MediaController? = null
        var uiPlaylistId: String? = null
        try {
            store.replace(playlist.id, listOf(uri.toString())); store.rename(playlist.id, "Playlist test ${System.currentTimeMillis()}")
            assertEquals(listOf(uri.toString()), PlaylistStore(context).all().first { it.id == playlist.id }.uris)
            assertTrue(MusicLibrary.list(context).any { it.uri == uri })
            val facts = AudioInspection.read(context, uri); assertEquals(44100, facts.sampleRate); assertEquals(1, facts.channels); assertTrue(facts.duration!! >= 89000)
            val device = UiDevice.getInstance(instrumentation); device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
            context.startActivity(Intent(context, MusicActivity::class.java).putExtra("play_uri", uri.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertNotNull(device.wait(Until.findObject(By.text("Flusiva · Musica")), 10000))
            await("Il lettore deve avviare il file") { MusicState.value.playing && MusicState.value.facts?.sampleRate == 44100 }
            var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
            onMain { future = MediaController.Builder(context, SessionToken(context, ComponentName(context, MusicService::class.java))).buildAsync() }
            controller = future!!.get(15, TimeUnit.SECONDS); var player = controller!!
            onMain { player.seekTo(10000) }; await("Seek non applicato") { MusicState.value.position >= 10000 }
            if (MusicState.value.eqAvailable) {
                var response: com.google.common.util.concurrent.ListenableFuture<SessionResult>? = null
                onMain { response = player.sendCustomCommand(SessionCommand(MusicService.EQ_ENABLE, Bundle.EMPTY), Bundle().apply { putBoolean("enabled", true) }) }
                assertEquals(SessionResult.RESULT_SUCCESS, response!!.get(10, TimeUnit.SECONDS).resultCode)
                await("EQ non attivo") { MusicState.value.eqEnabled }
                onMain { response = player.sendCustomCommand(SessionCommand(MusicService.EQ_PRESET, Bundle.EMPTY), Bundle().apply { putString("preset", "Bassi") }) }
                assertEquals(SessionResult.RESULT_SUCCESS, response!!.get(10, TimeUnit.SECONDS).resultCode)
                assertTrue(MusicState.value.bands.any { it.level > 0 })
            } else assertTrue(MusicState.value.eqReason.isNotBlank())
            var timer: com.google.common.util.concurrent.ListenableFuture<SessionResult>? = null
            onMain { timer = player.sendCustomCommand(SessionCommand(MusicService.SLEEP, Bundle.EMPTY), Bundle().apply { putInt("minutes", 10) }) }
            assertEquals(SessionResult.RESULT_SUCCESS, timer!!.get(10, TimeUnit.SECONDS).resultCode); assertTrue(MusicState.value.sleepRemaining > 0)
            onMain { timer = player.sendCustomCommand(SessionCommand(MusicService.SLEEP, Bundle.EMPTY), Bundle().apply { putInt("minutes", 0) }) }; timer!!.get(10, TimeUnit.SECONDS)
            capture(device, "android-music.png")
            assertNotNull(device.wait(Until.findObject(By.desc("Pausa")), 1000))
            device.findObject(By.desc("Pausa")).click()
            assertNotNull(device.wait(Until.findObject(By.desc("Riproduci")), 1000))
            await("La pausa dal tasto non è applicata") { !MusicState.value.playing }
            device.findObject(By.desc("Riproduci")).click(); await("La riproduzione dal tasto non riparte") { MusicState.value.playing }
            device.findObject(By.text("Audio")).click()
            assertNotNull(device.wait(Until.findObject(By.text("Audio ed equalizzatore")), 5000))
            assertNull(device.findObject(By.desc("Pausa")))
            device.waitForIdle(); capture(device, "android-equalizer.png")
            device.findObject(By.text("Lettore")).click()
            var before = 0L; onMain { before = player.currentPosition; player.release(); controller = null }
            device.pressHome(); device.sleep(); Thread.sleep(2500); device.wakeUp(); device.executeShellCommand("wm dismiss-keyguard")
            onMain { future = MediaController.Builder(context, SessionToken(context, ComponentName(context, MusicService::class.java))).buildAsync() }
            controller = future!!.get(15, TimeUnit.SECONDS); player = controller!!
            var after = 0L; onMain { assertTrue(player.isPlaying); after = player.currentPosition }; assertTrue(after > before)
            onMain { player.pause() }; await("Pausa non applicata") { !MusicState.value.playing }
            val item = MediaItem.Builder().setUri(uri).setMediaId(uri.toString()).setMediaMetadata(MediaMetadata.Builder().setTitle("Playlist test").build()).build()
            onMain { player.setMediaItems(listOf(item, item), 0, 0); player.prepare(); player.play() }
            await("La playlist non è stata caricata") { MusicState.value.playing }
            onMain { player.seekToNextMediaItem() }; var index = -1; onMain { index = player.currentMediaItemIndex }; assertEquals(1, index)
            onMain { player.stop() }
            context.startActivity(Intent(context, MusicActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertNotNull(device.wait(Until.findObject(By.text("Playlist")), 10000))
            device.findObject(By.text("Playlist")).click()
            assertNotNull(device.wait(Until.findObject(By.text("Nuova playlist")), 5000))
            device.findObject(By.text("Nuova playlist")).click()
            val editor = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5000)!!
            val name = "Playlist UI ${System.currentTimeMillis()}"; editor.text = name
            device.findObject(By.res("android:id/button1")).click()
            await("La playlist creata dalla UI non è persistita") { PlaylistStore(context).all().any { it.name == name } }
            uiPlaylistId = PlaylistStore(context).all().first { it.name == name }.id
            assertNotNull(device.wait(Until.findObject(By.text(name)), 5000))
            device.findObject(By.text("Aggiungi brani")).click()
            assertNotNull(device.wait(Until.findObject(By.text("Brani della playlist")), 5000))
            device.findObject(By.text("Flusiva player test.wav")).click(); device.findObject(By.res("android:id/button1")).click()
            await("Il brano selezionato non è nella playlist") { PlaylistStore(context).all().firstOrNull { it.id == uiPlaylistId }?.uris?.contains(uri.toString()) == true }
            device.waitForIdle(); capture(device, "android-playlists.png")
            device.findObject(By.text("Avvia playlist")).click()
            await("La playlist avviata dalla UI non suona") { MusicState.value.playing }
            assertNotNull(device.wait(Until.findObject(By.desc("Pausa")), 1000))
            onMain { player.pause() }
            store.delete(playlist.id); assertTrue(PlaylistStore(context).all().none { it.id == playlist.id })
            assertNotNull(context.contentResolver.openInputStream(uri)?.use { it.read() })
        } finally { onMain { controller?.release() }; store.delete(playlist.id); uiPlaylistId?.let(store::delete); context.contentResolver.delete(uri, null, null); onMain { context.stopService(Intent(context, MusicService::class.java)) } }
    }
}
