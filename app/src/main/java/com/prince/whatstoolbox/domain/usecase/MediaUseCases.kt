package com.prince.whatstoolbox.domain.usecase
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.prince.whatstoolbox.StickerProvider
import com.prince.whatstoolbox.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class SplitVideoUseCase @Inject constructor(@ApplicationContext private val c: Context) { suspend operator fun invoke(u: Uri, clipMs: Long, p: (Int, Int) -> Unit) = VideoSplitter.split(c, u, clipMs, onProgress = p) }
class CompressVideoUseCase @Inject constructor(@ApplicationContext private val c: Context) { suspend operator fun invoke(u: Uri) = MediaJobs.compress(c, u) }
class AudioToVideoUseCase @Inject constructor(@ApplicationContext private val c: Context) { suspend operator fun invoke(img: Uri, audio: Uri) = MediaJobs.audioToVideo(c, img, audio) }
class ConvertVoiceToMp3UseCase @Inject constructor(@ApplicationContext private val c: Context) { suspend operator fun invoke(u: Uri) = MediaJobs.opusToMp3(c, u) }
class PrepareDocumentsUseCase @Inject constructor(@ApplicationContext private val c: Context) {
    suspend operator fun invoke(uris: List<Uri>): List<File> = withContext(Dispatchers.IO) { uris.mapIndexed { i, u ->
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(c.contentResolver.getType(u)) ?: "jpg"
        Out.file(c, "IMG_%03d_ORIGINAL.$ext".format(i + 1)).also { f -> c.contentResolver.openInputStream(u)!!.use { input -> f.outputStream().use { out -> input.copyTo(out) } } } } }
}
class CreateStickerPackUseCase @Inject constructor(@ApplicationContext private val c: Context) {
    suspend operator fun invoke(uris: List<Uri>, name: String, emoji: String): String = withContext(Dispatchers.IO) {
        require(uris.size >= 3) { "Pick at least 3 images" }; val id = "pack_" + System.currentTimeMillis()
        val d = File(StickerProvider.root(c), id).apply { mkdirs() }; File(d, "name").writeText(name.ifBlank { "My Pack" })
        File(d, "emoji").writeText(emoji.trim().ifBlank { "😀" }); File(d, "version").writeText(System.currentTimeMillis().toString())
        uris.forEachIndexed { i, u -> val b = Img.load(c, u, 1024); val bytes = Img.sticker(b)
            require(bytes.size <= 100_000) { "Sticker ${i + 1} is over WhatsApp's 100 KB limit — try a simpler image" }
            File(d, "s%02d.webp".format(i)).writeBytes(bytes); if (i == 0) File(d, "tray.png").writeBytes(Img.tray(b).also { require(it.size <= 50_000) { "Tray icon is over 50 KB" } }) }
        id }
}
enum class Engine { VOSK, SYSTEM_FILE, MIC_ONLY }
class TranscribeUseCase @Inject constructor(@ApplicationContext private val c: Context) {
    private var sys: FileTranscriber? = null
    fun engine() = when { Vosk.available(c) -> Engine.VOSK; Build.VERSION.SDK_INT >= 33 -> Engine.SYSTEM_FILE; else -> Engine.MIC_ONLY }
    suspend fun decode(u: Uri) = withContext(Dispatchers.IO) { Pcm.decode(c, u) }
    suspend fun vosk(pcm: ShortArray) = withContext(Dispatchers.IO) { Vosk.transcribe(c, pcm) }
    fun system(pcm: ShortArray, onText: (String) -> Unit, onStatus: (String) -> Unit) { if (Build.VERSION.SDK_INT >= 33) { sys?.stop(); sys = FileTranscriber(c, onText, onStatus).also { it.start(pcm) } } }
    fun stop() { if (Build.VERSION.SDK_INT >= 33) sys?.stop() }
}
