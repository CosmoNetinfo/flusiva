package info.cosmonet.flusiva

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class MusicActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var connection: ListenableFuture<MediaController>? = null
    private lateinit var store: PlaylistStore
    private lateinit var title: TextView; private lateinit var artist: TextView; private lateinit var format: TextView; private lateinit var clock: TextView
    private lateinit var status: TextView; private lateinit var quality: TextView; private lateinit var seek: SeekBar; private lateinit var choose: Button
    private lateinit var play: ImageButton; private lateinit var previous: ImageButton; private lateinit var next: ImageButton
    private lateinit var shuffle: ImageButton; private lateinit var repeat: ImageButton; private lateinit var timer: ImageButton
    private lateinit var eqPanel: LinearLayout; private lateinit var eqEnabled: Switch; private lateinit var eqReason: TextView; private lateinit var preset: Spinner
    private lateinit var selector: Spinner; private lateinit var list: ListView; private lateinit var libraryMessage: TextView
    private val tabs = mutableListOf<Button>(); private val pages = mutableListOf<View>()
    private var page = 0; private var selected: String? = null; private var initialUri: Uri? = null
    private var songs = emptyList<Song>(); private var playlists = emptyList<MusicPlaylist>(); private var visible = emptyList<Song>()
    private var attached = false; private var selecting = false; private var renderingEq = false; private var dragging = false
    private var generation = 0; private var lastFacts: AudioFacts? = null; private var lastDecoder: String? = null; private var lastEq: MusicSnapshot? = null
    private var eqStructure = ""; private var playingUri: String? = null
    private val sliders = mutableListOf<Pair<SeekBar, TextView>>()
    private val adapter = SongAdapter()
    private val observe: (MusicSnapshot) -> Unit = { render(it) }
    private val playerListener = object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) { transport(); position() } }
    private val pulse = object : Runnable { override fun run() { if (attached && page == 0) position(); handler.postDelayed(this, 250) } }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { values ->
        libraryMessage.text = "Importazione…"
        worker.execute {
            val accepted = values.filter { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); true }.getOrDefault(false) }
            MusicLibrary.remember(this, accepted)
            runOnUiThread { if (!isDestroyed) { loadLibrary(); if (accepted.size != values.size) announce("Alcuni file non concedono un accesso permanente.") } }
        }
    }
    private fun dp(value: Int) = MobileUi.dp(this, value)
    private fun text(value: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(MobileUi.ink); if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setPadding(0, dp(4), 0, dp(4)) }
    private fun column(padding: Int = 20) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(padding), dp(12), dp(padding), dp(20)) }
    private fun row(parent: LinearLayout, vararg views: View) { parent.addView(LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; views.forEach { addView(it, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) }) } }) }
    private fun section(parent: LinearLayout, label: String) { parent.addView(text(label, 19f, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(8) }) }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit) = MobileUi.button(this, label, primary, action)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); volumeControlStream = AudioManager.STREAM_MUSIC; store = PlaylistStore(this)
        selected = savedInstanceState?.getString("playlist"); page = savedInstanceState?.getInt("page", 0) ?: 0
        initialUri = if (savedInstanceState?.getBoolean("opened", false) == true) null else intent.getStringExtra("play_uri")?.let(Uri::parse)?.takeIf { it.scheme == "content" }
        val root = column(0).apply { setPadding(0, 0, 0, 0); setBackgroundColor(MobileUi.pale); isFocusableInTouchMode = true }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets -> val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(4), dp(12), dp(4)) }
        header.addView(text("Flusiva · Musica", 21f, true).apply { gravity = Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(button("Scarica") { finish() }, LinearLayout.LayoutParams(-2, dp(48))); root.addView(header)
        val navigation = LinearLayout(this).apply { setPadding(dp(12), 0, dp(12), dp(8)) }
        listOf("Lettore", "Playlist", "Audio").forEachIndexed { index, label -> val tab = button(label) { showPage(index) }; tabs.add(tab); navigation.addView(tab, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) }) }; root.addView(navigation)
        val frame = FrameLayout(this); root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        pages.add(playerPage()); pages.add(libraryPage()); pages.add(audioPage()); pages.forEach { frame.addView(it, FrameLayout.LayoutParams(-1, -1)) }
        setContentView(root); showPage(page); render(MusicState.value); loadLibrary(); root.requestFocus(); root.requestApplyInsets()
    }
    private fun playerPage(): View {
        val body = column()
        body.addView(ImageView(this).apply { setImageDrawable(MusicIcon("music", MobileUi.blue)); background = MobileUi.touch(this@MusicActivity); setPadding(dp(16), dp(16), dp(16), dp(16)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(72), dp(72)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(12); bottomMargin = dp(12) })
        title = text("Nessun brano selezionato", 25f, true).apply { gravity = Gravity.CENTER; maxLines = 2; ellipsize = TextUtils.TruncateAt.END }; body.addView(title)
        artist = text("Apri la libreria per iniziare.").apply { gravity = Gravity.CENTER; setTextColor(MobileUi.muted); maxLines = 2 }; body.addView(artist)
        format = text("", 12f).apply { gravity = Gravity.CENTER; setTextColor(MobileUi.blue) }; body.addView(format)
        choose = button("Apri libreria", true) { showPage(1) }; body.addView(choose, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        seek = SeekBar(this).apply { max = 1000; contentDescription = "Posizione nel brano"; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar?) { dragging = true }
            override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) MobileUi.text(clock, "${EqualizerPolicy.time((controller?.duration ?: 0).coerceAtLeast(0) * value / 1000)} / ${EqualizerPolicy.time((controller?.duration ?: 0).coerceAtLeast(0))}") }
            override fun onStopTrackingTouch(bar: SeekBar?) { controller?.let { if (it.duration > 0) it.seekTo(it.duration * seek.progress / 1000) }; dragging = false; position() }
        }) }; body.addView(seek, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(12) })
        clock = text("0:00 / 0:00", 12f).apply { setTextColor(MobileUi.muted); gravity = Gravity.CENTER }; body.addView(clock)
        previous = MobileUi.icon(this, "previous", "Brano precedente") { controller?.seekToPrevious(); transport() }
        next = MobileUi.icon(this, "next", "Brano successivo") { controller?.seekToNext(); transport() }
        play = MobileUi.icon(this, "play", "Riproduci", 72, true) {
            controller?.let { if (it.playWhenReady) it.pause() else { if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition(); if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } }; transport()
        }
        body.addView(LinearLayout(this).apply { gravity = Gravity.CENTER; addView(previous, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(26) }); addView(play, LinearLayout.LayoutParams(dp(72), dp(72))); addView(next, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginStart = dp(26) }) }, LinearLayout.LayoutParams(-1, dp(88)))
        shuffle = MobileUi.icon(this, "shuffle", "Riproduzione casuale") { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }; transport() }
        repeat = MobileUi.icon(this, "repeat", "Ripeti coda") { controller?.let { it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL }; transport() }
        timer = MobileUi.icon(this, "timer", "Timer di spegnimento") { AlertDialog.Builder(this).setTitle("Timer di spegnimento").setItems(arrayOf("Disattiva", "10 minuti", "30 minuti", "60 minuti")) { _, index -> command(MusicService.SLEEP, Bundle().apply { putInt("minutes", listOf(0, 10, 30, 60)[index]) }) }.show() }
        row(body, shuffle, button("−10 s") { controller?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) }; position() }, timer, button("+10 s") { controller?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration.coerceAtLeast(0))) }; position() }, repeat)
        status = text("", 13f).apply { gravity = Gravity.CENTER; setTextColor(MobileUi.muted); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }; body.addView(status)
        body.addView(text("Ascolto anche a schermo spento. Usa i controlli della notifica o delle cuffie.", 12f).apply { gravity = Gravity.CENTER; setTextColor(MobileUi.muted) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        return ScrollView(this).apply { addView(body) }
    }
    private fun libraryPage(): View {
        val body = column(16); body.addView(text("Libreria e playlist", 23f, true))
        selector = Spinner(this).apply { contentDescription = "Playlist selezionata"; minimumHeight = dp(48); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {}; override fun onItemSelected(parent: AdapterView<*>?, view: View?, index: Int, id: Long) { if (!selecting) { selected = playlists.getOrNull(index - 1)?.id; renderSongs() } } } }; body.addView(selector)
        row(body, button("Nuova playlist") { editName(null) }, button("Gestisci") { managePlaylist() }, button("Importa") { picker.launch(arrayOf("audio/*")) })
        row(body, button("Avvia playlist", true) { playSongs(visible, 0) }, button("Aggiungi brani") { if (selected == null) picker.launch(arrayOf("audio/*")) else choosePlaylistSongs() })
        libraryMessage = text("Lettura dei file…", 12f).apply { setTextColor(MobileUi.muted) }; body.addView(libraryMessage)
        list = ListView(this).apply { adapter = this@MusicActivity.adapter; divider = null; setOnItemClickListener { _, _, index, _ -> playSongs(visible, index) }; setOnItemLongClickListener { _, _, index, _ -> if (selected != null) editTrack(index); true } }; body.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(button("Aggiorna elenco") { loadLibrary() }, LinearLayout.LayoutParams(-1, dp(48)))
        return body
    }
    private fun audioPage(): View {
        val body = column(); body.addView(text("Audio ed equalizzatore", 23f, true)); section(body, "Qualità del file")
        quality = text("Scegli un brano per vedere i dati audio."); body.addView(quality)
        body.addView(button("Informazioni sui dati audio") { AlertDialog.Builder(this).setTitle("Dati del file").setMessage("Codec, bitrate, frequenza e canali descrivono il file. Non misurano la fedeltà della sorgente né certificano l’uscita Bluetooth o Hi-Res. Il file originale non viene modificato dall’equalizzatore.").setPositiveButton("Chiudi", null).show() })
        section(body, "Equalizzatore")
        eqEnabled = Switch(this).apply { text = "Attiva equalizzatore"; setTextColor(MobileUi.ink); minHeight = dp(48); setOnCheckedChangeListener { _, value -> if (!renderingEq) command(MusicService.EQ_ENABLE, Bundle().apply { putBoolean("enabled", value) }) } }; body.addView(eqEnabled)
        preset = Spinner(this).apply { contentDescription = "Preset equalizzatore"; adapter = ArrayAdapter(this@MusicActivity, android.R.layout.simple_spinner_dropdown_item, EqualizerPolicy.presets + "Personale"); minimumHeight = dp(48); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {}; override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { val name = (EqualizerPolicy.presets + "Personale")[position]; if (!renderingEq && name != "Personale" && name != MusicState.value.preset) command(MusicService.EQ_PRESET, Bundle().apply { putString("preset", name) }) } } }; body.addView(preset)
        eqReason = text("", 12f).apply { setTextColor(MobileUi.muted) }; body.addView(eqReason)
        eqPanel = column(0); body.addView(eqPanel)
        return ScrollView(this).apply { addView(body) }
    }
    private fun showPage(value: Int) {
        page = value.coerceIn(0, 2)
        pages.forEachIndexed { index, view -> view.visibility = if (index == page) View.VISIBLE else View.GONE }
        tabs.forEachIndexed { index, view -> view.isSelected = index == page; view.setTextColor(if (index == page) MobileUi.blue else MobileUi.muted); view.setAccessibilityHeading(index == page) }
        if (page == 0 && ::seek.isInitialized) { transport(); position() }
    }
    override fun onStart() {
        super.onStart(); attached = true; MusicState.observers.add(observe); handler.post(pulse)
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, MusicService::class.java))).buildAsync(); connection = future
        future.addListener({ runCatching { future.get() }.onSuccess { if (attached) { controller = it; it.addListener(playerListener); render(MusicState.value); initialUri?.let { uri -> initialUri = null; playSongs(listOf(Song(uri, "Brano Flusiva")), 0) } } }.onFailure { if (attached) announce("Impossibile collegare il lettore. Riapri Musica.") } }, ContextCompat.getMainExecutor(this))
    }
    override fun onStop() { attached = false; handler.removeCallbacks(pulse); controller?.removeListener(playerListener); MusicState.observers.remove(observe); connection?.let(MediaController::releaseFuture); connection = null; controller = null; super.onStop() }
    override fun onDestroy() { generation++; worker.shutdownNow(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("playlist", selected); outState.putInt("page", page); outState.putBoolean("opened", initialUri == null); super.onSaveInstanceState(outState) }
    private fun announce(value: String) { MobileUi.text(status, value); Toast.makeText(this, value, Toast.LENGTH_SHORT).show() }
    private fun command(action: String, args: Bundle) {
        val player = controller ?: return
        val result = player.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
        result.addListener({ runCatching { result.get() }.onSuccess { if (it.resultCode != SessionResult.RESULT_SUCCESS && attached) announce(it.extras.getString("message", "Operazione non disponibile.")) }.onFailure { if (attached) announce("Il lettore non ha applicato il comando.") } }, ContextCompat.getMainExecutor(this))
    }
    private fun transport() {
        if (!::play.isInitialized) return
        val player = controller; val loaded = (player?.mediaItemCount ?: 0) > 0; val active = player?.playWhenReady == true
        val icon = if (active) "pause" else "play"; if (play.tag != icon) { play.tag = icon; play.setImageDrawable(MusicIcon(icon, Color.WHITE)); play.contentDescription = if (active) "Pausa" else "Riproduci" }
        play.isEnabled = loaded; seek.isEnabled = loaded; previous.isEnabled = loaded; next.isEnabled = loaded; shuffle.isEnabled = loaded; repeat.isEnabled = loaded
        choose.visibility = if (loaded) View.GONE else View.VISIBLE
        shuffle.isSelected = player?.shuffleModeEnabled == true; repeat.isSelected = player?.repeatMode == Player.REPEAT_MODE_ALL
        timer.isSelected = MusicState.value.sleepRemaining > 0
        val facts = MusicState.value.facts
        MobileUi.text(title, facts?.title ?: player?.currentMediaItem?.mediaMetadata?.title ?: "Nessun brano selezionato")
        MobileUi.text(artist, facts?.artist ?: if (!loaded) "Apri la libreria per iniziare." else if (player?.playbackState == Player.STATE_BUFFERING) "Preparazione audio…" else if (active) "Riproduzione in corso" else "In pausa")
        val uri = player?.currentMediaItem?.mediaId; if (uri != playingUri) { playingUri = uri; adapter.notifyDataSetChanged() }
    }
    private fun position() {
        val player = controller; val duration = player?.duration?.takeIf { it > 0 } ?: 0; val position = player?.currentPosition?.coerceAtLeast(0) ?: 0
        if (!dragging) { val value = if (duration > 0) (position * 1000 / duration).toInt().coerceIn(0, 1000) else 0; if (seek.progress != value) seek.progress = value; MobileUi.text(clock, "${EqualizerPolicy.time(position)} / ${EqualizerPolicy.time(duration)}") }
    }
    private fun render(state: MusicSnapshot) {
        transport(); if (!dragging) position()
        if (state.facts != lastFacts || state.decoder != lastDecoder) {
            lastFacts = state.facts; lastDecoder = state.decoder
            MobileUi.text(quality, (state.facts?.details() ?: "Scegli un brano per vedere i dati audio.") + (state.decoder?.let { "\nDecoder: $it" } ?: ""))
            MobileUi.text(format, state.facts?.let { listOfNotNull(it.mime?.removePrefix("audio/")?.uppercase(), it.sampleRate?.let { hz -> "${hz / 1000.0} kHz" }).joinToString(" · ") }.orEmpty())
        }
        MobileUi.text(status, state.error ?: if (state.sleepRemaining > 0) "Timer · ${EqualizerPolicy.time(state.sleepRemaining)}" else "")
        val old = lastEq
        if (old == null || old.bands != state.bands || old.eqAvailable != state.eqAvailable || old.eqEnabled != state.eqEnabled || old.eqReason != state.eqReason || old.preset != state.preset) { lastEq = state; renderEq(state) }
    }
    private fun renderEq(state: MusicSnapshot) {
        renderingEq = true; eqEnabled.isEnabled = state.eqAvailable; eqEnabled.isChecked = state.eqEnabled; MobileUi.text(eqReason, state.eqReason); preset.isEnabled = state.eqAvailable
        val index = (EqualizerPolicy.presets + "Personale").indexOf(state.preset).coerceAtLeast(0); if (preset.selectedItemPosition != index) preset.setSelection(index)
        val structure = "${state.minGain}/${state.maxGain}/" + state.bands.joinToString { it.hz.toString() }
        if (eqStructure != structure) {
            eqStructure = structure; eqPanel.removeAllViews(); sliders.clear()
            state.bands.forEachIndexed { bandIndex, band ->
                val label = text("", 13f); eqPanel.addView(label)
                var lastSent = 0L
                val slider = SeekBar(this).apply { max = state.maxGain - state.minGain; contentDescription = "Banda ${band.hz} Hz"; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) { MobileUi.text(label, "${band.hz} Hz · ${"%.1f".format((value + state.minGain) / 100.0)} dB"); if (SystemClock.elapsedRealtime() - lastSent >= 100) { lastSent = SystemClock.elapsedRealtime(); send(value) } } }
                    override fun onStopTrackingTouch(bar: SeekBar?) { send(bar?.progress ?: 0) }
                    private fun send(value: Int) { command(MusicService.EQ_BAND, Bundle().apply { putInt("band", bandIndex); putInt("level", value + state.minGain) }) }
                }) }; eqPanel.addView(slider, LinearLayout.LayoutParams(-1, dp(48))); sliders.add(slider to label)
            }
        }
        state.bands.forEachIndexed { index, band -> sliders.getOrNull(index)?.let { (slider, label) -> slider.isEnabled = state.eqAvailable && state.eqEnabled; if (!slider.isPressed) { if (slider.progress != band.level - state.minGain) slider.progress = band.level - state.minGain; MobileUi.text(label, "${band.hz} Hz · ${"%.1f".format(band.level / 100.0)} dB") } } }
        renderingEq = false
    }
    private fun loadLibrary() {
        val current = ++generation; MobileUi.text(libraryMessage, "Lettura della libreria…")
        worker.execute { val result = runCatching { MusicLibrary.list(this) }; val saved = store.all(); runOnUiThread { if (!isDestroyed && current == generation) { songs = result.getOrDefault(emptyList()); playlists = saved; refreshSelector(); renderSongs(); if (result.isFailure) MobileUi.text(libraryMessage, "Importa i file dal selettore Android per aggiungerli.") } } }
    }
    private fun refreshSelector() { if (playlists.none { it.id == selected }) selected = null; selecting = true; selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tutti i brani") + playlists.map { it.name }); selector.setSelection(playlists.indexOfFirst { it.id == selected } + 1); selecting = false }
    private fun renderSongs() {
        val known = songs.associateBy { it.uri.toString() }; visible = playlists.find { it.id == selected }?.uris?.map { known[it] ?: Song(Uri.parse(it), "File da verificare") } ?: songs
        MobileUi.text(libraryMessage, if (visible.isEmpty()) "Nessun brano. Aggiungi i download o importa un file audio." else "${visible.size} brani · tieni premuto per modificarne l’ordine")
        adapter.notifyDataSetChanged()
    }
    private inner class SongAdapter : BaseAdapter() {
        override fun getCount() = visible.size
        override fun getItem(position: Int) = visible[position]
        override fun getItemId(position: Int) = visible[position].uri.toString().hashCode().toLong()
        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
            val row = recycled as? LinearLayout ?: LinearLayout(this@MusicActivity).apply {
                gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(64); setPadding(dp(8), dp(6), dp(8), dp(6)); background = MobileUi.touch(this@MusicActivity)
                addView(ImageView(this@MusicActivity).apply { setImageDrawable(MusicIcon("music", MobileUi.blue)); setPadding(dp(8), dp(8), dp(8), dp(8)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(text("", 15f).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            val song = visible[position]; MobileUi.text(row.getChildAt(1) as TextView, song.name); row.isSelected = song.uri.toString() == playingUri; return row
        }
    }
    private fun playSongs(values: List<Song>, start: Int) {
        val player = controller ?: run { announce("Collegamento al lettore…"); return }; if (values.isEmpty()) { announce("Aggiungi almeno un brano alla playlist."); return }
        player.setMediaItems(values.map { MediaItem.Builder().setMediaId(it.uri.toString()).setUri(it.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(it.name).build()).build() }, start, 0)
        player.prepare(); player.play(); showPage(0); transport(); position()
    }
    private fun <T> change(work: () -> T, done: (T) -> Unit = {}) {
        MobileUi.text(libraryMessage, "Salvataggio…")
        worker.execute { val result = runCatching(work); runOnUiThread { if (!isDestroyed) { result.onSuccess { done(it); loadLibrary() }.onFailure { announce(it.message ?: "Salvataggio non riuscito."); renderSongs() } } } }
    }
    private fun editName(playlist: MusicPlaylist?) {
        val input = EditText(this).apply { setSingleLine(); setText(playlist?.name.orEmpty()); hint = "Nome playlist" }
        AlertDialog.Builder(this).setTitle(if (playlist == null) "Nuova playlist" else "Rinomina playlist").setView(input).setNegativeButton("Annulla", null).setPositiveButton("Salva") { _, _ -> val value = input.text.toString(); if (playlist == null) change({ store.create(value) }) { selected = it.id } else change({ store.rename(playlist.id, value) }) }.show()
    }
    private fun managePlaylist() {
        val item = playlists.find { it.id == selected } ?: run { announce("Seleziona o crea una playlist."); return }
        AlertDialog.Builder(this).setTitle(item.name).setItems(arrayOf("Aggiungi o rimuovi brani", "Rinomina", "Elimina playlist")) { _, index -> when (index) { 0 -> choosePlaylistSongs(); 1 -> editName(item); 2 -> AlertDialog.Builder(this).setTitle("Eliminare ${item.name}?").setMessage("I file audio rimangono sul telefono.").setNegativeButton("Annulla", null).setPositiveButton("Elimina") { _, _ -> change({ store.delete(item.id) }) { selected = null } }.show() } }.show()
    }
    private fun choosePlaylistSongs() {
        val item = playlists.find { it.id == selected } ?: return; val options = songs.toList(); val chosen = item.uris.toMutableSet()
        AlertDialog.Builder(this).setTitle("Brani della playlist").setMultiChoiceItems(options.map { it.name }.toTypedArray(), options.map { it.uri.toString() in chosen }.toBooleanArray()) { _, index, checked -> val uri = options[index].uri.toString(); if (checked) chosen.add(uri) else chosen.remove(uri) }.setNegativeButton("Annulla", null).setPositiveButton("Salva") { _, _ -> change({ store.replace(item.id, item.uris.filter { it in chosen } + options.map { it.uri.toString() }.filter { it in chosen && it !in item.uris }) }) }.show()
    }
    private fun editTrack(index: Int) {
        val item = playlists.find { it.id == selected } ?: return
        AlertDialog.Builder(this).setTitle("Modifica brano").setItems(arrayOf("Sposta prima", "Sposta dopo", "Rimuovi dalla playlist")) { _, action -> change({ store.replace(item.id, if (action == 2) item.uris.filterIndexed { i, _ -> i != index } else PlaylistPolicy.move(item.uris, index, if (action == 0) -1 else 1)) }) }.show()
    }
}
