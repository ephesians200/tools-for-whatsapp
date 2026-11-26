package com.prince.whatstoolbox.util
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume

data class SplitResult(val files: List<File>, val failed: Int, val total: Int)

object VideoSplitter {
    fun durationMs(c: Context, uri: Uri): Long = MediaMetadataRetriever().run {
        setDataSource(c, uri); val d = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L; release(); d }

    /** Splits into consecutive [clipMs] clips fully on-device (Media3 Transformer → MediaCodec). Failed clips are counted, never silently dropped. */
    suspend fun split(c: Context, uri: Uri, clipMs: Long = 30_000, onProgress: (Int, Int) -> Unit = { _, _ -> }): SplitResult =
        withContext(Dispatchers.Main) { // Transformer needs an app-thread Looper
            val total = durationMs(c, uri); val parts = ((total + clipMs - 1) / clipMs).toInt()
            val dir = File(c.cacheDir, "out").apply { mkdirs() }; val files = ArrayList<File>()
            dir.listFiles { f -> f.name.startsWith("status_part_") }?.forEach { it.delete() }   // leftovers from a previous, longer split
            for (i in 0 until parts) {
                onProgress(i + 1, parts)
                val item = MediaItem.Builder().setUri(uri).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setStartPositionMs(i * clipMs).setEndPositionMs(minOf(total, (i + 1) * clipMs)).build()).build()
                val out = File(dir, "status_part_${i + 1}.mp4")
                val ok = suspendCancellableCoroutine { k ->
                    val t = Transformer.Builder(c).addListener(object : Transformer.Listener {
                        override fun onCompleted(comp: Composition, r: ExportResult) { if (k.isActive) k.resume(true) }
                        override fun onError(comp: Composition, r: ExportResult, e: ExportException) { if (k.isActive) k.resume(false) }
                    }).build()
                    t.start(EditedMediaItem.Builder(item).build(), out.absolutePath)
                    k.invokeOnCancellation { t.cancel() }
                }
                if (ok) files += out
            }
            SplitResult(files, parts - files.size, parts)
        }
}
