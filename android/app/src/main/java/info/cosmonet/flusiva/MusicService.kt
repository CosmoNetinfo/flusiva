package info.cosmonet.flusiva

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

data class EqBand(val hz: Int, val level: Int)
data class MusicSnapshot(val title: String = "La tua musica, qui.", val playing: Boolean = false, val position: Long = 0, val duration: Long = 0, val facts: AudioFacts? = null, val decoder: String? = null, val error: String? = null, val eqAvailable: Boolean = false, val eqEnabled: Boolean = false, val eqReason: String = "L’equalizzatore si attiva quando il lettore apre una sessione audio.", val bands: List<EqBand> = emptyList(), val minGain: Int = -1500, val maxGain: Int = 1500, val preset: String = "Neutro", val sleepRemaining: Long = 0)
object MusicState {
    @Volatile var value = MusicSnapshot(); private set
    val observers = CopyOnWriteArraySet<(MusicSnapshot) -> Unit>()
    fun set(value: MusicSnapshot) { if (this.value == value) return; this.value = value; observers.forEach { it(value) } }
}
@androidx.annotation.OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    companion object {
        const val EQ_ENABLE = "flusiva.eq.enable"
        const val EQ_BAND = "flusiva.eq.band"
        const val EQ_PRESET = "flusiva.eq.preset"
        const val SLEEP = "flusiva.sleep"
        val commands = listOf(EQ_ENABLE, EQ_BAND, EQ_PRESET, SLEEP)
    }
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var equalizer: Equalizer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var sleepEnd = 0L
    private var inspectionGeneration = 0
    private val stopTimer = Runnable { sleepEnd = 0; player.pause(); publish() }
    private val tick = object : Runnable { override fun run() { if (MusicState.observers.isNotEmpty()) publish(); handler.postDelayed(this, 1000) } }
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            trackSelectionParameters = trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
        }
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { publish() }
            override fun onPlayerError(error: PlaybackException) { MusicState.set(MusicState.value.copy(error = "Riproduzione non riuscita. Verifica che il file sia accessibile e il formato supportato.")); publish() }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val generation = ++inspectionGeneration
                MusicState.set(MusicState.value.copy(facts = null, decoder = null, error = null))
                val uri = item?.localConfiguration?.uri ?: return
                worker.execute {
                    val result = runCatching { AudioInspection.read(this@MusicService, uri) }
                    handler.post { if (generation == inspectionGeneration) { MusicState.set(MusicState.value.copy(facts = result.getOrNull())); publish() } }
                }
            }
        })
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) { attachEqualizer(audioSessionId) }
            override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) { MusicState.set(MusicState.value.copy(decoder = decoderName)); publish() }
        })
        val open = PendingIntent.getActivity(this, 30, Intent(this, MusicActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session = MediaSession.Builder(this, player).setSessionActivity(open).setCallback(object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val result = super.onConnect(session, controller)
                if (controller.packageName != packageName) return result
                val available = result.availableSessionCommands.buildUpon()
                commands.forEach { available.add(SessionCommand(it, Bundle.EMPTY)) }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(available.build()).build()
            }
            override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
                if (controller.packageName != packageName || mediaItems.size > 500 || mediaItems.any { it.localConfiguration?.uri?.scheme != "content" }) return Futures.immediateFailedFuture(SecurityException("Sono ammessi solo file scelti nell’app."))
                return Futures.immediateFuture(mediaItems)
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                if (controller.packageName != packageName) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
                val result = runCatching {
                    when (customCommand.customAction) {
                        EQ_ENABLE -> {
                            val eq = equalizer ?: error("Equalizzatore non disponibile su questo dispositivo.")
                            val enabled = args.getBoolean("enabled")
                            check(eq.setEnabled(enabled) == AudioEffect.SUCCESS && eq.enabled == enabled) { "Il sistema non ha attivato l’effetto." }
                            getSharedPreferences("equalizer", MODE_PRIVATE).edit().putBoolean("enabled", enabled).apply(); publishEqualizer()
                        }
                        EQ_BAND -> {
                            val eq = equalizer ?: error("Equalizzatore non disponibile.")
                            val band = args.getInt("band", -1); val level = args.getInt("level", Int.MAX_VALUE)
                            require(band in 0 until eq.numberOfBands.toInt() && level in eq.bandLevelRange[0]..eq.bandLevelRange[1])
                            eq.setBandLevel(band.toShort(), level.toShort())
                            getSharedPreferences("equalizer", MODE_PRIVATE).edit().putInt("hz_${eq.getCenterFreq(band.toShort()) / 1000}", level).putString("preset", "Personale").apply(); publishEqualizer()
                        }
                        EQ_PRESET -> applyPreset(args.getString("preset").orEmpty())
                        SLEEP -> {
                            val minutes = args.getInt("minutes"); require(minutes in listOf(0, 10, 30, 60))
                            handler.removeCallbacks(stopTimer); sleepEnd = if (minutes == 0) 0 else SystemClock.elapsedRealtime() + minutes * 60000L
                            if (minutes > 0) handler.postDelayed(stopTimer, minutes * 60000L); publish()
                        }
                        else -> error("Comando non supportato.")
                    }
                }
                return Futures.immediateFuture(if (result.isSuccess) SessionResult(SessionResult.RESULT_SUCCESS) else SessionResult(SessionError.ERROR_BAD_VALUE, Bundle().apply { putString("message", result.exceptionOrNull()?.message ?: "Operazione non disponibile.") }))
            }
        }).build()
        handler.post(tick)
    }
    private fun publish() {
        MusicState.set(MusicState.value.copy(title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "La tua musica, qui.", playing = player.isPlaying, position = player.currentPosition.coerceAtLeast(0), duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0, sleepRemaining = if (sleepEnd == 0L) 0 else (sleepEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0)))
    }
    private fun attachEqualizer(id: Int) {
        equalizer?.release(); equalizer = null
        if (id == C.AUDIO_SESSION_ID_UNSET || id <= 0) { MusicState.set(MusicState.value.copy(eqAvailable = false, bands = emptyList(), eqEnabled = false)); return }
        runCatching {
            val eq = Equalizer(0, id); equalizer = eq
            check(eq.hasControl() && eq.numberOfBands > 0)
            val store = getSharedPreferences("equalizer", MODE_PRIVATE)
            for (band in 0 until eq.numberOfBands.toInt()) {
                val hz = eq.getCenterFreq(band.toShort()) / 1000
                eq.setBandLevel(band.toShort(), store.getInt("hz_$hz", 0).coerceIn(eq.bandLevelRange[0].toInt(), eq.bandLevelRange[1].toInt()).toShort())
            }
            check(eq.setEnabled(store.getBoolean("enabled", false)) == AudioEffect.SUCCESS)
            publishEqualizer()
        }.onFailure {
            equalizer?.release(); equalizer = null
            MusicState.set(MusicState.value.copy(eqAvailable = false, eqEnabled = false, bands = emptyList(), eqReason = "L’equalizzatore non è disponibile per questa sessione audio sul dispositivo. La riproduzione rimane disponibile."))
        }
    }
    private fun applyPreset(name: String) {
        val eq = equalizer ?: error("Equalizzatore non disponibile."); require(name in EqualizerPolicy.presets)
        val edit = getSharedPreferences("equalizer", MODE_PRIVATE).edit().putString("preset", name)
        for (band in 0 until eq.numberOfBands.toInt()) {
            val hz = eq.getCenterFreq(band.toShort()) / 1000
            val level = EqualizerPolicy.gain(name, hz, eq.bandLevelRange[0].toInt(), eq.bandLevelRange[1].toInt())
            eq.setBandLevel(band.toShort(), level.toShort()); edit.putInt("hz_$hz", level)
        }
        edit.apply(); publishEqualizer()
    }
    private fun publishEqualizer() {
        val eq = equalizer ?: return
        runCatching {
            MusicState.set(MusicState.value.copy(eqAvailable = eq.hasControl(), eqEnabled = eq.enabled, eqReason = "Effetto applicato solo al lettore Flusiva; il file originale rimane invariato.", bands = (0 until eq.numberOfBands.toInt()).map { EqBand(eq.getCenterFreq(it.toShort()) / 1000, eq.getBandLevel(it.toShort()).toInt()) }, minGain = eq.bandLevelRange[0].toInt(), maxGain = eq.bandLevelRange[1].toInt(), preset = getSharedPreferences("equalizer", MODE_PRIVATE).getString("preset", "Neutro") ?: "Neutro"))
        }.onFailure { MusicState.set(MusicState.value.copy(eqAvailable = false, eqEnabled = false, bands = emptyList(), eqReason = "Il sistema non rende disponibili i controlli dell’equalizzatore.")) }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        inspectionGeneration++; handler.removeCallbacksAndMessages(null); worker.shutdownNow(); equalizer?.release(); equalizer = null
        session?.release(); session = null; player.release(); MusicState.set(MusicSnapshot()); super.onDestroy()
    }
}
