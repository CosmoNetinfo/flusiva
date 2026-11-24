package info.cosmonet.flusiva

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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
    private val blue = Color.rgb(23, 98, 244); private val ink = Color.rgb(11, 37, 75); private val pale = Color.rgb(246, 249, 255)
    private val worker = Executors.newSingleThreadExecutor()
    private var controller: MediaController? = null
    private var connection: ListenableFuture<MediaController>? = null
    private lateinit var store: PlaylistStore
    private lateinit var nowTitle: TextView; private lateinit var nowArtist: TextView; private lateinit var quality: TextView; private lateinit var clock: TextView
    private lateinit var play: Button; private lateinit var seek: SeekBar; private lateinit var message: TextView
    private lateinit var eqPanel: LinearLayout; private lateinit var eqEnabled: Switch; private lateinit var eqReason: TextView; private lateinit var preset: Spinner
    private lateinit var sleep: TextView; private lateinit var shuffle: CheckBox; private lateinit var repeat: CheckBox
    private lateinit var selector: Spinner; private lateinit var rows: LinearLayout; private lateinit var libraryMessage: TextView
    private lateinit var scroll: ScrollView; private lateinit var libraryCard: LinearLayout
    private var songs = emptyList<Song>(); private var playlists = emptyList<MusicPlaylist>(); private var selected: String? = null
    private var selecting = false; private var renderingEq = false; private var eqStructure = ""
    private var dragging = false; private var initialUri: Uri? = null; private var attached = false
    private val sliders = mutableListOf<Pair<SeekBar, TextView>>()
    private val observe: (MusicSnapshot) -> Unit = { render(it) }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { values ->
        val accepted = values.filter { uri -> runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); true }.getOrDefault(false) }
        MusicLibrary.remember(this, accepted); loadLibrary()
        if (accepted.size != values.size) message.text = "Alcuni file non concedono un accesso permanente. Sceglili da un’altra cartella."
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun background(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.rgb(216, 229, 255)) }
    private fun text(value: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(ink); setPadding(0, dp(6), 0, dp(6)); if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply { text = label; isAllCaps = false; textSize = 15f; minHeight = dp(48); setTextColor(if (primary) Color.WHITE else blue); background = background(if (primary) blue else Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }; setOnClickListener { action() } }
    private fun horizontal(parent: LinearLayout, vararg children: View) { val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }; children.forEach { row.addView(it, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) }) }; parent.addView(row) }
    private fun card(parent: LinearLayout) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = background(Color.WHITE); setPadding(dp(18), dp(16), dp(18), dp(18)); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) }; parent.addView(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); volumeControlStream = AudioManager.STREAM_MUSIC; store = PlaylistStore(this); selected = savedInstanceState?.getString("playlist")
        initialUri = if (savedInstanceState?.getBoolean("opened", false) == true) null else intent.getStringExtra("play_uri")?.let(Uri::parse)?.takeIf { it.scheme == "content" }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(pale); isFocusableInTouchMode = true }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets -> val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        header.addView(text("Flusiva · Musica", 23f, true), LinearLayout.LayoutParams(0, -2, 1f)); header.addView(button("← Download") { finish() }, LinearLayout.LayoutParams(-2, -2)); root.addView(header)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(6), dp(22), dp(28)) }
        scroll = ScrollView(this).apply { addView(content) }; root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        content.addView(button("Vai a brani e playlist") { scroll.smoothScrollTo(0, libraryCard.top) })
        val current = card(content)
        current.addView(text("IN ASCOLTO", 11f, true).apply { setTextColor(blue); letterSpacing = .1f })
        nowTitle = text("La tua musica, qui.", 27f, true); current.addView(nowTitle)
        nowArtist = text("Scegli un brano o avvia una playlist.", 14f); current.addView(nowArtist)
        seek = SeekBar(this).apply { max = 1000; contentDescription = "Posizione nel brano"; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onStartTrackingTouch(bar: SeekBar?) { dragging = true }; override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {}; override fun onStopTrackingTouch(bar: SeekBar?) { controller?.let { if (it.duration > 0) it.seekTo(it.duration * seek.progress / 1000) }; dragging = false } }) }; current.addView(seek)
        clock = text("0:00 / 0:00", 13f); current.addView(clock)
        play = button("Riproduci", true) { controller?.let { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition(); if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } } }
        horizontal(current, button("Precedente") { controller?.seekToPrevious() }, play, button("Successivo") { controller?.seekToNext() })
        shuffle = CheckBox(this).apply { text = "Casuale"; setTextColor(ink); setOnCheckedChangeListener { _, value -> if (controller?.shuffleModeEnabled != value) controller?.shuffleModeEnabled = value } }
        repeat = CheckBox(this).apply { text = "Ripeti coda"; setTextColor(ink); setOnCheckedChangeListener { _, value -> controller?.repeatMode = if (value) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF } }
        horizontal(current, shuffle, repeat)
        horizontal(current, button("−10 secondi") { controller?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) } }, button("+10 secondi") { controller?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration.coerceAtLeast(0))) } })
        sleep = text("Timer disattivato", 12f); current.addView(sleep)
        current.addView(button("Timer di spegnimento") { AlertDialog.Builder(this).setTitle("Interrompi la musica tra…").setItems(arrayOf("Disattiva", "10 minuti", "30 minuti", "60 minuti")) { _, which -> command(MusicService.SLEEP, Bundle().apply { putInt("minutes", listOf(0, 10, 30, 60)[which]) }) }.show() })
        message = text(""); message.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE; current.addView(message)
        val details = card(content); details.addView(text("Qualità del file", 21f, true)); quality = text("I dati verranno letti quando scegli un brano.", 14f); details.addView(quality)
        details.addView(text("Sono informazioni del file e del decoder. Non misurano la fedeltà della sorgente né certificano l’uscita Bluetooth o Hi-Res.", 12f))
        val effects = card(content); effects.addView(text("Equalizzatore", 21f, true))
        eqEnabled = Switch(this).apply { text = "Attiva equalizzatore"; setTextColor(ink); minHeight = dp(48); setOnCheckedChangeListener { _, value -> if (!renderingEq) command(MusicService.EQ_ENABLE, Bundle().apply { putBoolean("enabled", value) }) } }; effects.addView(eqEnabled)
        preset = Spinner(this).apply { contentDescription = "Preset equalizzatore"; adapter = ArrayAdapter(this@MusicActivity, android.R.layout.simple_spinner_dropdown_item, EqualizerPolicy.presets + "Personale"); minimumHeight = dp(48); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {}; override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { val name = (EqualizerPolicy.presets + "Personale")[position]; if (!renderingEq && name != "Personale" && name != MusicState.value.preset) command(MusicService.EQ_PRESET, Bundle().apply { putString("preset", name) }) } } }; effects.addView(preset)
        eqReason = text("", 13f); effects.addView(eqReason); eqPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; effects.addView(eqPanel)
        val library = card(content); libraryCard = library; library.addView(text("Brani e playlist", 21f, true))
        selector = Spinner(this).apply { contentDescription = "Playlist"; minimumHeight = dp(48); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {}; override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { if (!selecting) { selected = playlists.getOrNull(position - 1)?.id; renderSongs() } } } }; library.addView(selector)
        horizontal(library, button("Nuova playlist") { editName(null) }, button("Gestisci playlist") { managePlaylist() })
        horizontal(library, button("Avvia playlist", true) { playSongs(visibleSongs(), 0) }, button("Aggiungi brani") { if (selected == null) picker.launch(arrayOf("audio/*")) else choosePlaylistSongs() })
        library.addView(button("Importa file audio") { picker.launch(arrayOf("audio/*")) })
        library.addView(button("Aggiorna elenco") { loadLibrary() })
        libraryMessage = text("Lettura dei file…", 13f); library.addView(libraryMessage)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; library.addView(rows)
        content.addView(text("La riproduzione continua con lo schermo spento. I controlli sono disponibili nella notifica e sulle cuffie. Playlist e preferenze rimangono sul telefono.", 12f))
        render(MusicState.value); loadLibrary(); root.requestFocus(); root.requestApplyInsets()
    }
    override fun onStart() {
        super.onStart(); attached = true; MusicState.observers.add(observe)
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, MusicService::class.java))).buildAsync(); connection = future
        future.addListener({ runCatching { future.get() }.onSuccess { if (attached) { controller = it; render(MusicState.value); initialUri?.let { uri -> initialUri = null; playSongs(listOf(Song(uri, "Brano Flusiva")), 0) } } }.onFailure { message.text = "Impossibile collegare il lettore. Riapri la schermata." } }, ContextCompat.getMainExecutor(this))
    }
    override fun onStop() { attached = false; MusicState.observers.remove(observe); connection?.let(MediaController::releaseFuture); connection = null; controller = null; super.onStop() }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("playlist", selected); outState.putBoolean("opened", initialUri == null); super.onSaveInstanceState(outState) }
    private fun command(action: String, args: Bundle) {
        val player = controller ?: return
        val result = player.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
        result.addListener({ runCatching { result.get() }.onSuccess { if (it.resultCode != SessionResult.RESULT_SUCCESS) { message.text = it.extras.getString("message", "Operazione non disponibile."); renderEq(MusicState.value) } }.onFailure { message.text = "Il lettore non ha applicato il comando." } }, ContextCompat.getMainExecutor(this))
    }
    private fun render(state: MusicSnapshot) {
        nowTitle.text = state.facts?.title ?: state.title; nowArtist.text = state.facts?.artist ?: if (state.playing) "Riproduzione in corso" else "Scegli un brano o avvia una playlist."
        play.text = if (state.playing) "Pausa" else "Riproduci"; play.isEnabled = controller?.mediaItemCount?.let { it > 0 } == true
        controller?.let { player -> if (shuffle.isChecked != player.shuffleModeEnabled) shuffle.isChecked = player.shuffleModeEnabled; val repeating = player.repeatMode == Player.REPEAT_MODE_ALL; if (repeat.isChecked != repeating) repeat.isChecked = repeating }
        if (!dragging) seek.progress = if (state.duration > 0) (state.position * 1000 / state.duration).toInt().coerceIn(0, 1000) else 0
        clock.text = "${EqualizerPolicy.time(state.position)} / ${EqualizerPolicy.time(state.duration)}"
        quality.text = (state.facts?.details() ?: "I dati verranno letti quando scegli un brano.") + (state.decoder?.let { "\nDecoder: $it" } ?: "")
        if (state.error != null) message.text = state.error
        sleep.text = if (state.sleepRemaining > 0) "Timer: ${EqualizerPolicy.time(state.sleepRemaining)}" else "Timer disattivato"
        renderEq(state)
    }
    private fun renderEq(state: MusicSnapshot) {
        renderingEq = true
        eqEnabled.isEnabled = state.eqAvailable; eqEnabled.isChecked = state.eqEnabled; eqReason.text = state.eqReason; preset.isEnabled = state.eqAvailable
        val presetIndex = (EqualizerPolicy.presets + "Personale").indexOf(state.preset).coerceAtLeast(0); if (preset.selectedItemPosition != presetIndex) preset.setSelection(presetIndex)
        val structure = "${state.minGain}/${state.maxGain}/" + state.bands.joinToString { it.hz.toString() }
        if (eqStructure != structure) {
            eqStructure = structure; eqPanel.removeAllViews(); sliders.clear()
            state.bands.forEachIndexed { index, band ->
                val label = text("", 13f); eqPanel.addView(label)
                val slider = SeekBar(this).apply { max = state.maxGain - state.minGain; contentDescription = "Banda ${band.hz} Hz"; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) label.text = "${band.hz} Hz · ${"%.1f".format((value + state.minGain) / 100.0)} dB" }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) { command(MusicService.EQ_BAND, Bundle().apply { putInt("band", index); putInt("level", (bar?.progress ?: 0) + state.minGain) }) }
                }) }; eqPanel.addView(slider); sliders.add(slider to label)
            }
        }
        state.bands.forEachIndexed { index, band -> sliders.getOrNull(index)?.let { (slider, label) -> slider.isEnabled = state.eqAvailable && state.eqEnabled; if (!slider.isPressed) { slider.progress = band.level - state.minGain; label.text = "${band.hz} Hz · ${"%.1f".format(band.level / 100.0)} dB" } } }
        renderingEq = false
    }
    private fun loadLibrary() {
        worker.execute {
            val result = runCatching { MusicLibrary.list(this) }; val saved = store.all()
            runOnUiThread { if (!isDestroyed) { songs = result.getOrDefault(emptyList()); playlists = saved; refreshSelector(); renderSongs(); if (result.isFailure) libraryMessage.text = "Non riesco a leggere l’elenco. Puoi importare i file con il selettore." } }
        }
    }
    private fun refreshSelector() {
        if (playlists.none { it.id == selected }) selected = null
        selecting = true; selector.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tutti i brani") + playlists.map { it.name }); selector.setSelection(playlists.indexOfFirst { it.id == selected } + 1); selecting = false
    }
    private fun visibleSongs(): List<Song> {
        val playlist = playlists.find { it.id == selected } ?: return songs
        val known = songs.associateBy { it.uri.toString() }
        return playlist.uris.map { known[it] ?: Song(Uri.parse(it), "File da verificare") }
    }
    private fun renderSongs() {
        val list = visibleSongs(); rows.removeAllViews(); libraryMessage.text = if (list.isEmpty()) "Nessun brano. Aggiungi i file scaricati o importa un file audio." else "${list.size} brani · tocca per ascoltare · tieni premuto per modificare la playlist"
        list.forEachIndexed { index, song -> rows.addView(button("${index + 1}. ${song.name}") { playSongs(list, index) }.apply { setOnLongClickListener { if (selected != null) editTrack(index); true } }) }
    }
    private fun playSongs(values: List<Song>, start: Int) {
        val player = controller ?: run { message.text = "Collegamento al lettore…"; return }
        if (values.isEmpty()) { message.text = "Aggiungi almeno un brano alla playlist."; return }
        val items = values.map { MediaItem.Builder().setMediaId(it.uri.toString()).setUri(it.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(it.name).build()).build() }
        player.setMediaItems(items, start, 0); player.prepare(); player.play(); message.text = ""
    }
    private fun editName(playlist: MusicPlaylist?) {
        val input = EditText(this).apply { setSingleLine(); setText(playlist?.name.orEmpty()); hint = "Nome playlist" }
        AlertDialog.Builder(this).setTitle(if (playlist == null) "Nuova playlist" else "Rinomina playlist").setView(input).setNegativeButton("Annulla", null).setPositiveButton("Salva") { _, _ ->
            runCatching { if (playlist == null) selected = store.create(input.text.toString()).id else store.rename(playlist.id, input.text.toString()) }.onFailure { message.text = it.message }; loadLibrary()
        }.show()
    }
    private fun managePlaylist() {
        val playlist = playlists.find { it.id == selected } ?: run { message.text = "Seleziona o crea una playlist."; return }
        AlertDialog.Builder(this).setTitle(playlist.name).setItems(arrayOf("Aggiungi o rimuovi brani", "Rinomina", "Elimina playlist")) { _, index -> when (index) {
            0 -> choosePlaylistSongs()
            1 -> editName(playlist)
            2 -> AlertDialog.Builder(this).setTitle("Eliminare ${playlist.name}?").setMessage("I file audio rimangono sul telefono.").setNegativeButton("Annulla", null).setPositiveButton("Elimina") { _, _ -> store.delete(playlist.id); selected = null; loadLibrary() }.show()
        } }.show()
    }
    private fun choosePlaylistSongs() {
        val playlist = playlists.find { it.id == selected } ?: return
        val chosen = playlist.uris.toMutableSet()
        AlertDialog.Builder(this).setTitle("Brani della playlist").setMultiChoiceItems(songs.map { it.name }.toTypedArray(), songs.map { it.uri.toString() in chosen }.toBooleanArray()) { _, index, checked -> val uri = songs[index].uri.toString(); if (checked) chosen.add(uri) else chosen.remove(uri) }.setNegativeButton("Annulla", null).setPositiveButton("Salva") { _, _ ->
            runCatching { store.replace(playlist.id, playlist.uris.filter { it in chosen } + songs.map { it.uri.toString() }.filter { it in chosen && it !in playlist.uris }) }.onFailure { message.text = it.message }; loadLibrary()
        }.show()
    }
    private fun editTrack(index: Int) {
        val playlist = playlists.find { it.id == selected } ?: return
        AlertDialog.Builder(this).setTitle("Modifica brano").setItems(arrayOf("Sposta prima", "Sposta dopo", "Rimuovi dalla playlist")) { _, action ->
            runCatching {
                val values = if (action == 2) playlist.uris.filterIndexed { i, _ -> i != index } else PlaylistPolicy.move(playlist.uris, index, if (action == 0) -1 else 1)
                store.replace(playlist.id, values)
            }.onFailure { message.text = "Il brano è già all’estremità della playlist." }; loadLibrary()
        }.show()
    }
}
