package com.prince.whatstoolbox.util
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Pcm {
    const val RATE = 16_000
    /** Decodes any Android-supported audio (incl. WhatsApp Ogg/Opus) to mono 16 kHz 16-bit PCM. Call off the main thread. */
    fun decode(c: Context, uri: Uri): ShortArray {
        val ex = MediaExtractor().apply { setDataSource(c, uri, null) }
        val ti = (0 until ex.trackCount).first { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/") }
        ex.selectTrack(ti); val fmt = ex.getTrackFormat(ti)
        val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!); codec.configure(fmt, null, null, 0); codec.start()
        var sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE); var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val info = MediaCodec.BufferInfo(); val raw = ByteArrayOutputStream(); var eos = false; var done = false
        while (!done) {
            if (!eos) { val i = codec.dequeueInputBuffer(10_000); if (i >= 0) { val b = codec.getInputBuffer(i)!!; val n = ex.readSampleData(b, 0)
                if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos = true } else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() } } }
            val o = codec.dequeueOutputBuffer(info, 10_000)
            if (o >= 0) { val b = codec.getOutputBuffer(o)!!; val bytes = ByteArray(info.size); b.get(bytes); raw.write(bytes); codec.releaseOutputBuffer(o, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true }
            else if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { val f = codec.outputFormat; sr = f.getInteger(MediaFormat.KEY_SAMPLE_RATE); ch = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }
        }
        codec.stop(); codec.release(); ex.release()
        val s = ByteBuffer.wrap(raw.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().let { ShortArray(it.remaining()).also { a -> it.get(a) } }
        val mono = ShortArray(s.size / ch) { i -> var sum = 0; for (k in 0 until ch) sum += s[i * ch + k]; (sum / ch).toShort() }
        if (sr == RATE) return mono
        val n = (mono.size.toLong() * RATE / sr).toInt()
        return ShortArray(n) { i -> val p = i.toDouble() * sr / RATE; val a = p.toInt().coerceAtMost(mono.size - 1); val b = (a + 1).coerceAtMost(mono.size - 1)
            (mono[a] + (mono[b] - mono[a]) * (p - a)).toInt().toShort() }
    }
}

/** Feeds decoded audio to Android's SpeechRecognizer via EXTRA_AUDIO_SOURCE (API 33+), in ≤50 s chunks. */
@RequiresApi(33)
class FileTranscriber(private val c: Context, private val onText: (String) -> Unit, private val onStatus: (String) -> Unit) {
    private var rec: SpeechRecognizer? = null; private var chunks = listOf<File>(); private var idx = 0
    fun start(pcm: ShortArray) {
        stop(); val size = Pcm.RATE * 50
        chunks = (0 until (pcm.size + size - 1) / size).map { i -> val part = pcm.copyOfRange(i * size, minOf(pcm.size, (i + 1) * size))   // no boxing of samples
            File(c.cacheDir, "tr_$i.pcm").also { f -> f.writeBytes(ByteBuffer.allocate(part.size * 2).order(ByteOrder.LITTLE_ENDIAN).also { b -> part.forEach { s -> b.putShort(s) } }.array()) } }
        idx = 0; rec = if (SpeechRecognizer.isOnDeviceRecognitionAvailable(c)) SpeechRecognizer.createOnDeviceSpeechRecognizer(c) else SpeechRecognizer.createSpeechRecognizer(c); next()
    }
    private fun next() {
        val r = rec ?: return
        if (idx >= chunks.size) { onStatus("Done ✓"); stop(); return }
        onStatus("Transcribing part ${idx + 1} of ${chunks.size}…")
        val pfd = ParcelFileDescriptor.open(chunks[idx], ParcelFileDescriptor.MODE_READ_ONLY)
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}; override fun onBeginningOfSpeech() {}; override fun onRmsChanged(v: Float) {}; override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}; override fun onPartialResults(p: Bundle?) {}; override fun onEvent(t: Int, p: Bundle?) {}
            override fun onError(e: Int) { pfd.close(); onStatus("This device's speech service couldn't process the file (error $e). Check the language pack / on-device speech is installed."); stop() }
            override fun onResults(res: Bundle?) { pfd.close(); onText(res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()); idx++; next() }
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pfd).putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT).putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, Pcm.RATE))
    }
    fun stop() { rec?.destroy(); rec = null; chunks.forEach { it.delete() } }
}

/** Optional fully-offline engine. Works on any Android version and needs NO microphone permission.
 *  Activate by unzipping a Vosk model (e.g. vosk-model-small-en-us-0.15) into app/src/main/assets/model-en/. */
object Vosk {
    private const val DIR = "model-en"; private var model: org.vosk.Model? = null
    fun available(c: Context) = runCatching { c.assets.list(DIR).orEmpty().isNotEmpty() }.getOrDefault(false)
    private fun copy(c: Context, path: String, dst: File) { dst.mkdirs()
        for (k in c.assets.list(path).orEmpty()) { val sub = "$path/$k"
            if (c.assets.list(sub).orEmpty().isNotEmpty()) copy(c, sub, File(dst, k)) else c.assets.open(sub).use { i -> File(dst, k).outputStream().use { o -> i.copyTo(o) } } } }
    @Synchronized private fun load(c: Context): org.vosk.Model { model?.let { return it }
        val dst = File(c.filesDir, DIR); if (!File(dst, "am").exists()) copy(c, DIR, dst); return org.vosk.Model(dst.absolutePath).also { model = it } }
    fun transcribe(c: Context, pcm: ShortArray): String {
        val rec = org.vosk.Recognizer(load(c), Pcm.RATE.toFloat()); val bb = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN); pcm.forEach { bb.putShort(it) }
        val b = bb.array(); val sb = StringBuilder(); var i = 0
        while (i < b.size) { val n = minOf(8000, b.size - i); if (rec.acceptWaveForm(b.copyOfRange(i, i + n), n)) sb.append(org.json.JSONObject(rec.result).optString("text")).append(' '); i += n }
        sb.append(org.json.JSONObject(rec.finalResult).optString("text")); rec.close(); return sb.toString().trim()
    }
}
