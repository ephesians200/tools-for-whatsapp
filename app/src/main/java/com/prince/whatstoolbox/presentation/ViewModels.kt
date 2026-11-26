package com.prince.whatstoolbox.presentation
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prince.whatstoolbox.domain.*
import com.prince.whatstoolbox.domain.repository.*
import com.prince.whatstoolbox.domain.usecase.*
import com.prince.whatstoolbox.util.KeepAliveService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ---------- Dashboard ----------
data class DashboardState(val query: String = "", val category: Category? = null, val onlyFavorites: Boolean = false, val searching: Boolean = false,
                          val favorites: Set<String> = emptySet(), val tools: List<Tool> = ToolRegistry.all,
                          val favoriteTools: List<Tool> = emptyList(), val recentTools: List<Tool> = emptyList())
sealed interface DashboardIntent {
    data class Query(val q: String) : DashboardIntent; data class Select(val c: Category?) : DashboardIntent
    data object ToggleFavorites : DashboardIntent; data object ToggleSearch : DashboardIntent; data class ToggleFavorite(val id: String) : DashboardIntent
    data class Open(val id: String) : DashboardIntent
}
@HiltViewModel class DashboardViewModel @Inject constructor(observeFavs: ObserveFavoritesUseCase, private val toggleFav: ToggleFavoriteUseCase,
                                                           private val filter: FilterToolsUseCase, private val settings: SettingsRepository) : ViewModel() {
    private val ui = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = combine(ui, observeFavs(), settings.settings) { u, f, s ->
        u.copy(favorites = f, tools = filter(u.query, u.category, u.onlyFavorites, f),
            favoriteTools = ToolRegistry.all.filter { it.id in f }, recentTools = s.recents.mapNotNull { ToolRegistry.byIdOrNull(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
    fun onIntent(i: DashboardIntent) { when (i) {
        is DashboardIntent.Query -> ui.update { it.copy(query = i.q) }; is DashboardIntent.Select -> ui.update { it.copy(category = i.c) }
        DashboardIntent.ToggleFavorites -> ui.update { it.copy(onlyFavorites = !it.onlyFavorites) }
        DashboardIntent.ToggleSearch -> ui.update { it.copy(searching = !it.searching, query = "") }
        is DashboardIntent.ToggleFavorite -> viewModelScope.launch { toggleFav(i.id) }
        is DashboardIntent.Open -> settings.addRecent(i.id) } }
}
// ---------- Vault ----------
data class VaultState(val kind: VaultKind = VaultKind.SNIPPET, val items: List<VaultEntry> = emptyList())
sealed interface VaultIntent {
    data class Select(val kind: VaultKind) : VaultIntent
    /** [createdAt] must be passed when editing so the entry keeps its position in the list. */
    data class Save(val id: Long, val title: String, val body: String, val extra: String, val createdAt: Long = 0L) : VaultIntent
    data class Delete(val id: Long) : VaultIntent
}
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel class VaultViewModel @Inject constructor(private val observe: ObserveVaultUseCase, private val save: SaveVaultEntryUseCase, private val delete: DeleteVaultEntryUseCase) : ViewModel() {
    private val kind = MutableStateFlow(VaultKind.SNIPPET)
    val state: StateFlow<VaultState> = kind.flatMapLatest { k -> observe(k).map { VaultState(k, it) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultState())
    fun onIntent(i: VaultIntent) { when (i) {
        is VaultIntent.Select -> kind.value = i.kind
        is VaultIntent.Save -> viewModelScope.launch { save(VaultEntry(i.id, kind.value.key, i.title, i.body, i.extra, i.createdAt)) }
        is VaultIntent.Delete -> viewModelScope.launch { delete(i.id) } } }
}
// ---------- Pro / paywall ----------
@HiltViewModel class PremiumViewModel @Inject constructor(private val billing: com.prince.whatstoolbox.data.BillingManager) : ViewModel() {
    val premium = billing.premium; val price = billing.price; val ready = billing.ready; val messages = billing.messages
    val paywallSeen get() = billing.paywallSeen
    fun markPaywallSeen() = billing.markPaywallSeen()
    fun buy(a: android.app.Activity) = billing.purchase(a); fun restore() = billing.restore()
}
// ---------- Settings / self note ----------
@HiltViewModel class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val state = repo.settings
    fun setAmoled(v: Boolean) = repo.setAmoled(v); fun setDynamic(v: Boolean) = repo.setDynamicColor(v)
    fun setThemeMode(v: Int) = repo.setThemeMode(v); fun setCountryCode(v: String) = repo.setCountryCode(v) }
@HiltViewModel class SelfNoteViewModel @Inject constructor(private val store: SecureStore) : ViewModel() {
    val saved: String = store.get("self_number"); fun save(n: String) = store.put("self_number", n) }
// ---------- Direct chat ----------
data class DirectChatState(val cc: String = "233", val number: String = "", val message: String = "", val clean: String = "", val valid: Boolean = false,
                           val link: String = "", val recents: List<String> = emptyList())
sealed interface DirectChatIntent {
    data class Cc(val v: String) : DirectChatIntent; data class Number(val v: String) : DirectChatIntent
    data class Message(val v: String) : DirectChatIntent; data class Remember(val clean: String) : DirectChatIntent
}
@HiltViewModel class DirectChatViewModel @Inject constructor(private val build: BuildWaLinkUseCase, private val settings: SettingsRepository, private val store: SecureStore) : ViewModel() {
    private val _s = MutableStateFlow(DirectChatState(cc = settings.settings.value.countryCode, recents = store.get("recent_numbers").split(",").filter { it.isNotBlank() }))
    val state = _s.asStateFlow()
    private fun rebuild(s: DirectChatState): DirectChatState { val l = build(s.number, s.cc, s.message); return s.copy(clean = l.clean, valid = l.valid, link = l.link) }
    fun onIntent(i: DirectChatIntent) { when (i) {
        is DirectChatIntent.Cc -> { val d = i.v.filter(Char::isDigit).take(4); if (d.isNotBlank()) settings.setCountryCode(d); _s.update { rebuild(it.copy(cc = d)) } }
        is DirectChatIntent.Number -> _s.update { rebuild(it.copy(number = i.v)) }
        is DirectChatIntent.Message -> _s.update { rebuild(it.copy(message = i.v)) }
        is DirectChatIntent.Remember -> _s.update { s -> val l = (listOf(i.clean) + s.recents.filter { it != i.clean }).take(5); store.put("recent_numbers", l.joinToString(",")); s.copy(recents = l) } } }
}
// ---------- Chat analytics / extractors ----------
@HiltViewModel class ChatViewModel @Inject constructor(private val analyze: AnalyzeChatUseCase) : ViewModel() {
    private val _r = MutableStateFlow(ChatReport.EMPTY); val report = _r.asStateFlow()
    fun load(raw: String) { viewModelScope.launch(Dispatchers.Default) { _r.value = analyze(raw) } }
}
// ---------- Text studio ----------
data class TextStudioState(val text: String = "", val chars: Int = 0, val words: Int = 0, val seconds: Int = 0, val fonts: List<Pair<String, String>> = emptyList())
sealed interface TextStudioIntent { data class Edit(val t: String) : TextStudioIntent; data class Apply(val op: MdOp) : TextStudioIntent }
@HiltViewModel class TextStudioViewModel @Inject constructor(private val md: ApplyMarkdownUseCase, private val fancy: FancyTextUseCase) : ViewModel() {
    private val _s = MutableStateFlow(TextStudioState()); val state = _s.asStateFlow()
    private fun set(t: String) { _s.value = TextStudioState(t, t.length, com.prince.whatstoolbox.util.TextStats.words(t), com.prince.whatstoolbox.util.TextStats.readSeconds(t), fancy(t)) }
    fun onIntent(i: TextStudioIntent) { when (i) { is TextStudioIntent.Edit -> set(i.t); is TextStudioIntent.Apply -> set(md(i.op, _s.value.text)) } }
}
// ---------- Generic form tools ----------
data class FormState(val spec: FormSpec? = null, val values: Map<String, String> = emptyMap(), val output: String = "", val error: String? = null)
sealed interface FormIntent { data class Edit(val key: String, val value: String) : FormIntent }
@HiltViewModel class FormViewModel @Inject constructor() : ViewModel() {
    private var id: String? = null; private val _s = MutableStateFlow(FormState()); val state = _s.asStateFlow()
    private fun compute(spec: FormSpec, v: Map<String, String>): FormState = runCatching { spec.run(v) }
        .fold({ FormState(spec, v, it) }, { FormState(spec, v, "", it.message?.takeIf { m -> m.isNotBlank() } ?: "Couldn't generate a result — check the values above.") })
    fun load(toolId: String) { if (id == toolId) return; id = toolId; val spec = ToolSpecs.forms[toolId] ?: return
        _s.value = compute(spec, spec.fields.associate { it.key to it.def }) }
    fun onIntent(i: FormIntent) { when (i) { is FormIntent.Edit -> _s.update { s -> val spec = s.spec ?: return@update s; compute(spec, s.values + (i.key to i.value)) } } }
}
// ---------- Media jobs ----------
/** [progress] is 0..1 when a job can report it, otherwise null (indeterminate). */
data class MediaState(val busy: Boolean = false, val status: String = "", val files: List<File> = emptyList(), val text: String = "", val packId: String? = null,
                      val packName: String = "", val progress: Float? = null, val done: Boolean = false, val warning: String = "")
@HiltViewModel class MediaViewModel @Inject constructor(
    @ApplicationContext private val app: Context,
    private val split: SplitVideoUseCase, private val compress: CompressVideoUseCase, private val av: AudioToVideoUseCase, private val mp3: ConvertVoiceToMp3UseCase,
    private val docs: PrepareDocumentsUseCase, private val stickers: CreateStickerPackUseCase, private val tr: TranscribeUseCase) : ViewModel() {
    private val _s = MutableStateFlow(MediaState()); val state = _s.asStateFlow()
    private var running: Job? = null

    /** Runs one exclusive job under a foreground service; [block] returns the final state. Cancellation is reported, not treated as failure. */
    private fun launchJob(start: MediaState, block: suspend () -> MediaState) {
        running?.cancel()
        running = viewModelScope.launch {
            KeepAliveService.start(app, start.status); _s.value = start.copy(busy = true)
            try { _s.value = block() }
            catch (e: CancellationException) { _s.value = MediaState(status = "Cancelled"); throw e }
            catch (e: Exception) { _s.value = MediaState(status = "Failed: ${e.message}") }
            finally { KeepAliveService.stop(app) }
        }
    }
    private fun filesJob(msg: String, block: suspend () -> List<File>) = launchJob(MediaState(status = msg)) {
        val f = block(); MediaState(files = f, status = if (f.isEmpty()) "Failed" else "Done ✓", done = f.isNotEmpty()) }

    fun cancel() { running?.cancel(); tr.stop(); _s.update { if (it.busy) MediaState(status = "Cancelled") else it } }
    fun splitVideo(u: Uri, clipSeconds: Int) = launchJob(MediaState(status = "Splitting…", progress = 0f)) {
        val r = split(u, clipSeconds * 1000L) { i, n -> _s.update { it.copy(status = "Exporting clip $i of $n…", progress = (i - 1f) / n) } }
        MediaState(files = r.files, status = if (r.files.isEmpty()) "Failed" else "Done ✓", done = r.files.isNotEmpty(),
            warning = if (r.failed > 0) "${r.failed} of ${r.total} clips failed to export — don't send this set as-is." else "") }
    fun compressVideo(u: Uri) = filesJob("Compressing…") { listOfNotNull(compress(u)) }
    fun audioToVideo(img: Uri, audio: Uri) = filesJob("Rendering…") { listOfNotNull(av(img, audio)) }
    fun toMp3(u: Uri) = filesJob("Converting…") { listOfNotNull(mp3(u)) }
    fun prepareDocs(u: List<Uri>) = filesJob("Preparing…") { docs(u) }
    fun createStickers(u: List<Uri>, name: String, emoji: String) = launchJob(MediaState(status = "Building pack…")) {
        val id = stickers(u, name, emoji); MediaState(status = "Pack ready — confirm in WhatsApp", packId = id, packName = name, done = true) }
    fun consumePack() = _s.update { it.copy(packId = null) }
    fun engine() = tr.engine(); fun editText(t: String) = _s.update { it.copy(text = t) }; fun setStatus(s: String) = _s.update { it.copy(status = s, busy = false) }
    fun transcribe(u: Uri) { running?.cancel(); running = viewModelScope.launch { _s.value = MediaState(busy = true, status = "Decoding audio…")
        val pcm = runCatching { tr.decode(u) }.getOrNull(); if (pcm == null || pcm.isEmpty()) { _s.value = MediaState(status = "Could not decode this file."); return@launch }
        when (tr.engine()) {
            Engine.VOSK -> { _s.update { it.copy(status = "Transcribing offline…") }; val t = runCatching { tr.vosk(pcm) }
                _s.value = MediaState(text = t.getOrDefault(""), status = t.fold({ "Done ✓" }, { "Failed: ${it.message}" }), done = t.isSuccess) }
            Engine.SYSTEM_FILE -> tr.system(pcm, { t -> _s.update { it.copy(text = it.text + t + " ") } }, { st -> _s.update { it.copy(status = st, busy = !(st.startsWith("Done") || st.startsWith("This device")), done = st.startsWith("Done")) } })
            Engine.MIC_ONLY -> _s.value = MediaState(status = "Needs Android 13+ or the offline Vosk model.") } } }
    override fun onCleared() { tr.stop() }
}
