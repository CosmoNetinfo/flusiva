package info.cosmonet.flusiva

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val blue = Color.rgb(23, 98, 244)
    private val ink = Color.rgb(11, 37, 75)
    private val pale = Color.rgb(246, 249, 255)
    private lateinit var prefs: Preferences
    private lateinit var content: LinearLayout
    private lateinit var link: EditText
    private lateinit var analyze: Button
    private lateinit var download: Button
    private lateinit var photos: Button
    private lateinit var cancel: Button
    private lateinit var open: Button
    private lateinit var listen: Button
    private lateinit var share: Button
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var formats: RadioGroup
    private lateinit var updateStatus: TextView
    private lateinit var source: TextView
    private lateinit var qualityHint: TextView
    private var settings = false
    private var draft = ""
    private var selectedFormat = "mp4"
    private val network = Executors.newSingleThreadExecutor()
    private val observer: (DownloadSnapshot) -> Unit = { if (!settings) render(it) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 16, stroke: Boolean = false) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke) setStroke(dp(1), Color.rgb(216, 229, 255)) }
    private fun text(value: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(ink); if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setPadding(0, dp(6), 0, dp(6)) }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit): Button = MobileUi.button(this, value, primary, action).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) } }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = bg(Color.WHITE, 22, true); setPadding(dp(20), dp(18), dp(20), dp(20))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) }; content.addView(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Preferences(this); selectedFormat = savedInstanceState?.getString("format") ?: prefs.format; draft = savedInstanceState?.getString("link").orEmpty()
        showPage(false); receive(intent)
        network.execute { runCatching { Engine.initialize(applicationContext) } }
    }
    override fun onSaveInstanceState(outState: Bundle) { if (!settings) draft = link.text.toString(); outState.putString("link", draft); outState.putString("format", selectedFormat); super.onSaveInstanceState(outState) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = MediaPolicy.sharedLink(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
            showPage(false); draft = shared; link.setText(shared)
        }
    }
    override fun onResume() { super.onResume(); DownloadState.observers.add(observer); if (!settings) render(DownloadState.value) }
    override fun onPause() { DownloadState.observers.remove(observer); super.onPause() }
    override fun onDestroy() { network.shutdown(); super.onDestroy() }
    private fun showPage(showSettings: Boolean) {
        if (::link.isInitialized && !settings) draft = link.text.toString()
        settings = showSettings
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(pale); isFocusableInTouchMode = true }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets -> val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(22), dp(10), dp(22), dp(10)) }
        bar.addView(ImageView(this).apply { setImageResource(R.drawable.ic_flusiva); contentDescription = "Flusiva" }, LinearLayout.LayoutParams(dp(40), dp(40)))
        bar.addView(text("Flusiva", 24f, true), LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(button(if (settings) "← Download" else "Impostazioni") { showPage(!settings) }.apply { layoutParams = LinearLayout.LayoutParams(-2, dp(50)) })
        root.addView(bar)
        if (!settings) {
            val navigation = LinearLayout(this).apply { setPadding(dp(20), 0, dp(20), dp(8)) }
            navigation.addView(button("Scarica") {}.apply { isSelected = true }, LinearLayout.LayoutParams(0, dp(48), 1f))
            navigation.addView(button("Musica") { startActivity(Intent(this, MusicActivity::class.java)) }, LinearLayout.LayoutParams(0, dp(48), 1f))
            root.addView(navigation)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(32)); layoutParams = ViewGroup.LayoutParams(-1, -2) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        if (settings) settingsPage() else downloadPage()
        root.requestFocus()
        root.requestApplyInsets()
    }
    private fun downloadPage() {
        content.addView(text("Scarica un contenuto", 28f, true))
        content.addView(text("Incolla il link, controlla la fonte e scegli MP4 o MP3.", 15f))
        val input = card()
        input.addView(text("Link del contenuto", 15f, true))
        link = EditText(this).apply { hint = "https://…"; textSize = 16f; setTextColor(ink); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI; maxLines = 3; setPadding(dp(12), dp(14), dp(12), dp(14)); background = bg(pale, 12, true); setText(draft); contentDescription = "Link del contenuto" }
        input.addView(link, LinearLayout.LayoutParams(-1, -2))
        source = text("Fonte: incolla un link supportato", 13f).apply { setTextColor(blue); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }; input.addView(source)
        input.addView(button("Incolla link") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            link.setText(MediaPolicy.sharedLink(clipboard.primaryClip?.getItemAt(0)?.coerceToText(this).toString().takeIf { it != "null" }.orEmpty()))
        })
        analyze = button("Analizza il link", true) { startTask("inspect") }; input.addView(analyze)
        val result = card()
        title = text("Formato e qualità", 20f, true); result.addView(title)
        formats = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        for ((index, name) in listOf("Video MP4", "Audio MP3").withIndex()) formats.addView(RadioButton(this).apply { id = if (index == 0) R.id.format_mp4 else R.id.format_mp3; text = name; textSize = 15f; minHeight = dp(48); setTextColor(ink) }, RadioGroup.LayoutParams(0, -2, 1f))
        formats.check(if (selectedFormat == "mp3") R.id.format_mp3 else R.id.format_mp4)
        formats.setOnCheckedChangeListener { _, checked -> selectedFormat = if (checked == R.id.format_mp3) "mp3" else "mp4"; updateSource() }
        result.addView(formats)
        qualityHint = text("", 12f).apply { setTextColor(blue) }; result.addView(qualityHint)
        download = button("Scarica sul telefono", true) { startTask("download") }; result.addView(download)
        photos = button("Scarica foto da Instagram", true) {
            val items = DownloadState.value.info?.photos.orEmpty()
            if (items.size == 1) startTask("download", 0)
            else if (items.isNotEmpty()) AlertDialog.Builder(this).setTitle("Scegli la foto da scaricare").setItems(items.mapIndexed { index, photo -> "Foto ${index + 1}${if (photo.width > 0 && photo.height > 0) " · ${photo.width}×${photo.height}" else ""}" }.toTypedArray()) { _, index -> startTask("download", index) }.show()
        }; result.addView(photos)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }; result.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(16) })
        status = text(""); status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE; result.addView(status)
        cancel = button("Annulla operazione") { startService(Intent(this, DownloadService::class.java).setAction("cancel")) }; result.addView(cancel)
        open = button("Apri il file") { openSaved(false) }; result.addView(open)
        listen = button("Ascolta in Flusiva") { DownloadState.value.saved?.let { startActivity(Intent(this, MusicActivity::class.java).putExtra("play_uri", it.toString())) } }; result.addView(listen)
        share = button("Condividi il file") { openSaved(true) }; result.addView(share)
        content.addView(text("YouTube · TikTok · Instagram · Facebook\nVimeo · Dailymotion · SoundCloud (solo MP3)", 12f).apply { gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(8)) })
        content.addView(text("I file vengono salvati in Download/Flusiva. Usa contenuti tuoi o che puoi scaricare. Nessun account o server Flusiva.", 12f).apply { gravity = Gravity.CENTER })
        link.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) { updateSource() }; override fun afterTextChanged(value: Editable?) {} })
        render(DownloadState.value)
    }
    private fun updateSource() {
        if (!::download.isInitialized || settings) return
        val item = runCatching { MediaPolicy.identify(link.text.toString()) }.getOrNull()
        MobileUi.text(source, item?.let { "Fonte riconosciuta: ${it.platform}${if (it.platform == "SoundCloud") " · solo audio" else ""}" } ?: if (link.text.isBlank()) "Fonte: verrà riconosciuta dal link" else "Fonte non riconosciuta: usa un link supportato")
        val state = DownloadState.value
        val ready = item != null && state.info?.let { item.url in listOf(it.url, it.sourceUrl) } == true
        val hasPhotos = ready && state.info?.photos?.isNotEmpty() == true
        MobileUi.text(qualityHint, if (hasPhotos) "Immagine originale · nessuna ricompressione" else if (prefs.maximum) "Qualità massima · MP3 fino a 320 kbps" else "Qualità personalizzata nelle Impostazioni")
        photos.visibility = if (hasPhotos) View.VISIBLE else View.GONE
        photos.isEnabled = hasPhotos && !state.busy
        MobileUi.text(photos, "Scarica foto da Instagram${if (hasPhotos && state.info!!.photos.size > 1) " · ${state.info.photos.size} foto" else ""}")
        formats.visibility = if (hasPhotos) View.GONE else View.VISIBLE
        download.visibility = if (hasPhotos) View.GONE else View.VISIBLE
        if (item?.platform == "SoundCloud" && selectedFormat != "mp3") formats.check(R.id.format_mp3)
        formats.findViewById<RadioButton>(R.id.format_mp4).isEnabled = !state.busy && item?.platform != "SoundCloud" && (!ready || state.info?.hasVideo != false)
        formats.findViewById<RadioButton>(R.id.format_mp3).isEnabled = !state.busy && (!ready || state.info?.hasAudio != false)
        download.isEnabled = !state.busy && ready && if (selectedFormat == "mp3") state.info?.hasAudio == true else state.info?.hasVideo == true
        MobileUi.text(download, if (state.busy) "Operazione in corso…" else "Scarica ${selectedFormat.uppercase()}${item?.let { " da ${it.platform}" } ?: " sul telefono"}")
        if (!state.busy && !state.failed && !ready && ::status.isInitialized) MobileUi.text(status, if (item != null) "Premi Analizza il link per preparare il download." else "Incolla un link supportato per iniziare.")
    }
    private fun render(state: DownloadSnapshot) {
        analyze.isEnabled = !state.busy; link.isEnabled = !state.busy
        MobileUi.text(analyze, if (state.busy) "Attendi…" else "Analizza il link")
        status.text = state.message; status.setTextColor(if (state.failed) Color.rgb(170, 35, 50) else ink)
        title.text = state.info?.let { "${it.title}\n${it.author}" } ?: "Formato e qualità"
        progress.visibility = if (state.busy) View.VISIBLE else View.GONE; progress.isIndeterminate = state.percent == null; progress.progress = state.percent?.toInt() ?: 0
        cancel.visibility = if (state.busy) View.VISIBLE else View.GONE
        open.visibility = if (state.saved != null) View.VISIBLE else View.GONE; share.visibility = open.visibility
        listen.visibility = if (state.format in listOf("mp3", "mp4")) open.visibility else View.GONE
        formats.findViewById<RadioButton>(R.id.format_mp4).isEnabled = !state.busy && state.info?.hasVideo != false
        formats.findViewById<RadioButton>(R.id.format_mp3).isEnabled = !state.busy && state.info?.hasAudio != false
        if (state.info?.hasVideo == false && state.info.hasAudio) formats.check(R.id.format_mp3)
        if (state.info?.hasAudio == false && state.info.hasVideo) formats.check(R.id.format_mp4)
        updateSource()
    }
    private fun startTask(action: String, photoIndex: Int? = null) {
        try {
            if (DownloadState.value.busy) return
            val identified = MediaPolicy.identify(link.text.toString())
            val info = DownloadState.value.info
            if (action == "download") check(info != null && identified.url in listOf(info.url, info.sourceUrl)) { "Il link è cambiato. Analizzalo di nuovo." }
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
            val job = Intent(this, DownloadService::class.java).setAction(action).putExtra("url", if (action == "download") info!!.url else identified.url)
                .putExtra("format", if (photoIndex == null) selectedFormat else "image").putExtra("photo", photoIndex ?: 0).putExtra("maximum", prefs.maximum).putExtra("audio", prefs.audio).putExtra("video", prefs.video)
            startForegroundService(job)
            DownloadState.set(DownloadState.value.copy(busy = true, message = "Preparazione…", saved = null, failed = false))
        } catch (error: Exception) { status.text = MediaPolicy.friendly(error); status.setTextColor(Color.rgb(170, 35, 50)) }
    }
    private fun openSaved(send: Boolean) {
        val state = DownloadState.value; val uri = state.saved ?: return
        val type = MediaPolicy.mime(state.format)
        val intent = if (send) Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, uri).apply { clipData = ClipData.newUri(contentResolver, "Flusiva", uri) } else Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
        try { startActivity(Intent.createChooser(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), if (send) "Condividi con" else "Apri con")) } catch (_: ActivityNotFoundException) { Toast.makeText(this, "Nessuna app compatibile installata.", Toast.LENGTH_LONG).show() }
    }
    private fun settingsPage() {
        content.addView(text("Fatta come vuoi tu.", 32f, true)); content.addView(text("Preferenze salvate sul telefono.", 15f))
        val quality = card(); quality.addView(text("Formato e qualità", 21f, true))
        quality.addView(Switch(this).apply { text = "Usa la qualità massima"; textSize = 16f; setTextColor(ink); minHeight = dp(56); isChecked = prefs.maximum; setOnCheckedChangeListener { _, checked -> prefs.maximum = checked; showPage(true) } })
        quality.addView(text("Video alla migliore qualità disponibile. MP3 a 320 kbps: la conversione non migliora la sorgente.", 13f))
        fun choice(label: String, labels: List<String>, values: List<String>, value: String, enabled: Boolean = true, changed: (String) -> Unit) {
            quality.addView(text(label, 14f, true)); quality.addView(Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels); setSelection(values.indexOf(value).coerceAtLeast(0)); isEnabled = enabled; minimumHeight = dp(48); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: AdapterView<*>?) {} override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { changed(values[position]) } } })
        }
        choice("Formato predefinito", listOf("Video MP4", "Audio MP3"), listOf("mp4", "mp3"), prefs.format) { prefs.format = it; selectedFormat = it }
        choice("Bitrate MP3", listOf("128 kbps", "192 kbps", "256 kbps", "320 kbps"), listOf("128", "192", "256", "320"), prefs.audio, !prefs.maximum) { prefs.audio = it }
        choice("Risoluzione massima", listOf("Migliore disponibile", "2160p", "1080p", "720p", "480p"), listOf("best", "2160", "1080", "720", "480"), prefs.video, !prefs.maximum) { prefs.video = it }
        val updates = card(); updates.addView(text("Aggiornamenti", 21f, true)); updates.addView(text("Flusiva ${BuildConfig.VERSION_NAME} · Android\nControlla le nuove versioni e scarica l’APK dalla Release ufficiale. Android richiede la conferma per installarlo.", 13f))
        updateStatus = text(""); updates.addView(updateStatus)
        updates.addView(button("Controlla aggiornamenti") { checkUpdates() })
        updates.addView(button("Apri Release ufficiale") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/CosmoNetinfo/flusiva/releases/latest"))) })
        val about = card(); about.addView(text("Sul tuo telefono", 21f, true)); about.addView(text("Download e conversione avvengono qui. I file restano in Download/Flusiva anche dopo la disinstallazione. Nessun database, pubblicità o telemetria.\n\nYouTube, TikTok, Instagram, Facebook, Vimeo, Dailymotion e SoundCloud. Spotify non è supportato.", 14f))
        about.addView(button("Uso responsabile e licenze") { val message = assets.open("NOTICE.txt").bufferedReader().use { it.readText() }; AlertDialog.Builder(this).setTitle("Flusiva · GPL v3").setMessage(message).setPositiveButton("Chiudi", null).show() })
        about.addView(button("Ripristina preferenze") { prefs.reset(); selectedFormat = prefs.format; showPage(true) })
    }
    private fun checkUpdates() {
        updateStatus.text = "Controllo della Release ufficiale…"
        network.execute {
            val message = runCatching {
                val connection = URI("https://api.github.com/repos/CosmoNetinfo/flusiva/releases/latest").toURL().openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 15000; connection.readTimeout = 15000; connection.setRequestProperty("Accept", "application/vnd.github+json")
                    check(connection.responseCode == 200) { "Controllo non disponibile. Riprova più tardi." }
                    val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val version = release.getString("tag_name").removePrefix("v")
                    check(Regex("\\d+\\.\\d+\\.\\d+").matches(version) && !release.getBoolean("prerelease") && !release.getBoolean("draft"))
                    val current = BuildConfig.VERSION_NAME.split('.').map(String::toInt); val latest = version.split('.').map(String::toInt)
                    val newer = (0..2).firstOrNull { latest[it] != current[it] }?.let { latest[it] > current[it] } ?: false
                    if (newer) "È disponibile Flusiva $version. Apri la Release per scaricare l’APK." else "Flusiva ${BuildConfig.VERSION_NAME} è aggiornata."
                } finally { connection.disconnect() }
            }.getOrElse { MediaPolicy.friendly(it) }
            runOnUiThread { if (!isDestroyed && settings) updateStatus.text = message }
        }
    }
}
