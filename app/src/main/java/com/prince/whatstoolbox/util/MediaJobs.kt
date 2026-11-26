package com.prince.whatstoolbox.util
import android.content.Context
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume

object MediaJobs {
    private suspend fun export(c: Context, out: File, enc: DefaultEncoderFactory? = null, audioMime: String? = null, item: EditedMediaItem? = null, comp: Composition? = null): Boolean =
        withContext(Dispatchers.Main) { out.delete(); suspendCancellableCoroutine { k ->
            val b = Transformer.Builder(c); enc?.let { b.setEncoderFactory(it) }; audioMime?.let { b.setAudioMimeType(it) }
            val t = b.addListener(object : Transformer.Listener {
                override fun onCompleted(co: Composition, r: ExportResult) { if (k.isActive) k.resume(true) }
                override fun onError(co: Composition, r: ExportResult, e: ExportException) { if (k.isActive) k.resume(false) }
            }).build()
            if (comp != null) t.start(comp, out.absolutePath) else t.start(item!!, out.absolutePath)
            k.invokeOnCancellation { t.cancel() } } }

    /** 720p @ ~1.5 Mbps, small enough for Status. */
    suspend fun compress(c: Context, uri: Uri): File? { val out = Out.file(c, "status_compressed.mp4")
        val enc = DefaultEncoderFactory.Builder(c).setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(1_500_000).build()).build()
        val item = EditedMediaItem.Builder(MediaItem.fromUri(uri)).setEffects(Effects(listOf(), listOf(Presentation.createForHeight(720)))).build()
        return if (export(c, out, enc = enc, item = item)) out else null }

    /** Static image + audio (max 30s) → MP4. */
    suspend fun audioToVideo(c: Context, img: Uri, audio: Uri): File? { val out = Out.file(c, "status_audio.mp4")
        val ms = minOf(30_000L, VideoSplitter.durationMs(c, audio)).coerceAtLeast(1000)
        val image = EditedMediaItem.Builder(MediaItem.fromUri(img)).setDurationUs(ms * 1000).setFrameRate(30).build()
        val a = EditedMediaItem.Builder(MediaItem.Builder().setUri(audio).setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(ms).build()).build()).build()
        val comp = Composition.Builder(listOf(EditedMediaItemSequence(listOf(image)), EditedMediaItemSequence(listOf(a)))).build()
        return if (export(c, out, comp = comp)) out else null }

    /** Opus/any audio → real MP3 via bundled LAME (mono 16 kHz, 64 kbps — ideal for voice). Falls back to AAC if the native library can't load. */
    suspend fun opusToMp3(c: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val pcm = Pcm.decode(c, uri); val out = Out.file(c, "voice_note.mp3")
            val lame = com.naman14.androidlame.LameBuilder().setInSampleRate(Pcm.RATE).setOutChannels(1).setOutBitrate(64).setOutSampleRate(Pcm.RATE).setQuality(5).build()
            out.outputStream().use { os -> val buf = ByteArray(8192 + pcm.size / 4); var i = 0
                while (i < pcm.size) { val n = minOf(Pcm.RATE, pcm.size - i); val chunk = pcm.copyOfRange(i, i + n)
                    val w = lame.encode(chunk, chunk, n, buf); if (w > 0) os.write(buf, 0, w); i += n }
                val f = lame.flush(buf); if (f > 0) os.write(buf, 0, f) }
            lame.close(); out
        }.getOrNull() ?: opusToAac(c, uri)
    }

    /** Decodes Opus and re-encodes AAC (.m4a) — used as the fallback for opusToMp3. */
    suspend fun opusToAac(c: Context, uri: Uri): File? { val out = Out.file(c, "voice_note.m4a")
        val item = EditedMediaItem.Builder(MediaItem.fromUri(uri)).setRemoveVideo(true).build()
        return if (export(c, out, audioMime = MimeTypes.AUDIO_AAC, item = item)) out else null }
}
