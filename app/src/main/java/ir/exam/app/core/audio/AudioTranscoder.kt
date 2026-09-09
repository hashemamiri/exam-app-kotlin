package ir.exam.app.core.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * V135 — موتور صوت بومی (فقط MediaCodec اندروید، بدون کتابخانهٔ خارجی): هر فایل صوتی که اندروید بتواند دیکد کند
 * (mp3/m4a/ogg/wav/opus/flac/amr…) → PCM 16-bit → AAC-LC داخل m4a.
 *
 * - برش: فقط بازهٔ [startMs, endMs) دیکد و انکد می‌شود.
 * - «بدون افت کیفیت محسوس»: نمونه‌برداری ۴۴٫۱kHz و کانال‌های اصلی (حداکثر استریو)
 *   حفظ می‌شود؛ نرخ بیت به‌صورت تطبیقی از ۱۲۸k پایین می‌آید تا خروجی زیر سقف
 *   [MAX_BYTES] بماند (۱۲۸→۹۶→۸۰→۶۴→۴۸→۴۰→۳۲ kbps؛ زیر ۴۸k به مونو می‌رود).
 * - اگر حتی با کمترین نرخ هم بزرگ‌تر از سقف شد، [AudioTooLargeException] پرتاب می‌شود.
 */
object AudioTranscoder {

    const val MAX_BYTES: Long = 3L * 1024L * 1024L
    private const val OUT_SAMPLE_RATE = 44_100
    private val BITRATES = intArrayOf(128_000, 96_000, 80_000, 64_000, 48_000, 40_000, 32_000)
    private const val TIMEOUT_US = 10_000L

    class AudioTooLargeException(val bytes: Long) :
        IllegalStateException("حجم فایل صوتی پس از فشرده‌سازی ${formatMb(bytes)} مگابایت است؛ سقف مجاز ۳ مگابایت. بازهٔ کوتاه‌تری انتخاب کنید.")

    data class Info(val durationMs: Long, val sampleRate: Int, val channels: Int, val mime: String, val sourceBytes: Long)

    data class Result(val file: File, val bytes: Long, val durationMs: Long, val bitrate: Int, val channels: Int)

    fun info(context: Context, uri: Uri): Info {
        val ex = MediaExtractor()
        try {
            setSource(context, ex, uri)
            val (_, fmt) = audioTrack(ex) ?: error("این فایل صوتی قابل خواندن نیست.")
            val bytes = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)
            return Info(
                durationMs = (fmt.getLongOrDefault(MediaFormat.KEY_DURATION, 0L) / 1000L),
                sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty(),
                sourceBytes = bytes
            )
        } finally {
            ex.release()
        }
    }

    /**
     * شکل موج برای ویرایشگر: [buckets] عدد ۰..۱ (اوجِ دامنه در هر بازه).
     * برای سرعت، فقط با گام‌های نمونه‌برداری‌شده خوانده می‌شود.
     */
    fun waveform(context: Context, uri: Uri, buckets: Int = 240, onProgress: ((Float) -> Unit)? = null): FloatArray {
        val peaks = FloatArray(buckets)
        val totalUs = info(context, uri).durationMs * 1000L
        if (totalUs <= 0) return peaks
        decodePcm(context, uri, 0L, Long.MAX_VALUE) { pcm, ptsUs, channels ->
            val idx = ((ptsUs.toDouble() / totalUs) * buckets).toInt().coerceIn(0, buckets - 1)
            var p = 0f
            var i = 0
            val n = pcm.remaining() / 2
            val step = max(1, n / 512)
            while (i < n) {
                val s = abs(pcm.getShort(pcm.position() + i * 2).toInt()) / 32768f
                if (s > p) p = s
                i += step * channels
            }
            if (p > peaks[idx]) peaks[idx] = p
            onProgress?.invoke((ptsUs.toDouble() / totalUs).toFloat().coerceIn(0f, 1f))
        }
        // نرمال‌سازی ملایم تا فایل‌های آرام هم دیده شوند.
        val mx = peaks.maxOrNull() ?: 0f
        if (mx > 0.05f) for (i in peaks.indices) peaks[i] = (peaks[i] / mx).coerceIn(0f, 1f)
        return peaks
    }

    /** تخمین حجم خروجی برای نمایش زنده (بایت) با نرخ بیت مشخص. */
    fun estimateBytes(durationMs: Long, bitrate: Int): Long = (durationMs / 1000.0 * bitrate / 8.0).toLong() + 2_048L

    /** بهترین نرخ بیتی که تخمینش زیر سقف است (برای نمایش زنده). */
    fun plannedBitrate(durationMs: Long): Int? = BITRATES.firstOrNull { estimateBytes(durationMs, it) <= MAX_BYTES }

    /**
     * برش + فشرده‌سازی تطبیقی. خروجی در [outDir] با نام یکتا. در صورت عبور از
     * سقف حتی با کمترین نرخ، [AudioTooLargeException].
     */
    fun transcode(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        outDir: File,
        onProgress: ((Float) -> Unit)? = null
    ): Result {
        require(endMs > startMs) { "بازهٔ صوتی نامعتبر است." }
        outDir.mkdirs()
        val durationMs = endMs - startMs
        var last: Result? = null
        for (br in BITRATES) {
            if (estimateBytes(durationMs, br) > MAX_BYTES * 1.25) continue // بی‌فایده؛ سریع رد شو
            val out = File(outDir, "audio-${System.currentTimeMillis()}-${br / 1000}k.m4a")
            val channels = if (br < 48_000) 1 else 2
            val r = encodeOnce(context, uri, startMs, endMs, out, br, channels, onProgress)
            last?.file?.delete()
            last = r
            if (r.bytes <= MAX_BYTES) return r
        }
        val bytes = last?.bytes ?: estimateBytes(durationMs, BITRATES.last())
        last?.file?.delete()
        throw AudioTooLargeException(bytes)
    }

    // ---------------------------------------------------------------- داخلی

    private fun encodeOnce(
        context: Context, uri: Uri, startMs: Long, endMs: Long, out: File,
        bitrate: Int, wantChannels: Int, onProgress: ((Float) -> Unit)?
    ): Result {
        val srcInfo = info(context, uri)
        val outChannels = min(wantChannels, max(1, srcInfo.channels))
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, OUT_SAMPLE_RATE, outChannels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxStarted = false
        val info = MediaCodec.BufferInfo()
        var encodedUs = 0L
        val spanUs = (endMs - startMs) * 1000L
        val resampler = Resampler(srcInfo.sampleRate, OUT_SAMPLE_RATE, srcInfo.channels, outChannels)

        fun drainEncoder(endOfStream: Boolean) {
            while (true) {
                val idx = encoder.dequeueOutputBuffer(info, if (endOfStream) TIMEOUT_US * 5 else 0L)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(encoder.outputFormat); muxer.start(); muxStarted = true
                    }
                    idx >= 0 -> {
                        val buf = encoder.getOutputBuffer(idx)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0 && muxStarted) {
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buf, info)
                            encodedUs = info.presentationTimeUs
                        }
                        encoder.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        fun feedEncoder(pcm: ByteBuffer, ptsUs: Long) {
            // ورودی انکدر را به قطعه‌های ≤ ظرفیت تقسیم می‌کنیم.
            var pts = ptsUs
            while (pcm.hasRemaining()) {
                val inIdx = encoder.dequeueInputBuffer(TIMEOUT_US)
                if (inIdx < 0) { drainEncoder(false); continue }
                val inBuf = encoder.getInputBuffer(inIdx)!!
                inBuf.clear()
                val n = min(inBuf.remaining(), pcm.remaining())
                val slice = pcm.duplicate().apply { limit(pcm.position() + n) }
                inBuf.put(slice)
                pcm.position(pcm.position() + n)
                encoder.queueInputBuffer(inIdx, 0, n, pts, 0)
                pts += (n / (2L * outChannels)) * 1_000_000L / OUT_SAMPLE_RATE
                drainEncoder(false)
            }
        }

        try {
            var outPts = 0L
            decodePcm(context, uri, startMs * 1000L, endMs * 1000L) { pcm, _, _ ->
                val conv = resampler.process(pcm)
                if (conv.hasRemaining()) {
                    val frames = conv.remaining() / (2 * outChannels)
                    feedEncoder(conv, outPts)
                    outPts += frames * 1_000_000L / OUT_SAMPLE_RATE
                    onProgress?.invoke((outPts.toDouble() / spanUs).toFloat().coerceIn(0f, 0.98f))
                }
            }
            val tail = resampler.flush()
            if (tail.hasRemaining()) feedEncoder(tail, outPts)
            val inIdx = encoder.dequeueInputBuffer(TIMEOUT_US * 10)
            if (inIdx >= 0) encoder.queueInputBuffer(inIdx, 0, 0, outPts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drainEncoder(true)
        } finally {
            runCatching { encoder.stop() }; encoder.release()
            runCatching { if (muxStarted) muxer.stop() }; muxer.release()
        }
        onProgress?.invoke(1f)
        return Result(out, out.length(), (endMs - startMs), bitrate, outChannels)
    }

    /** دیکد به PCM 16-bit little-endian؛ callback با بافرِ PCM، زمانِ ارائه و تعداد کانال. */
    private fun decodePcm(
        context: Context, uri: Uri, fromUs: Long, toUs: Long,
        sink: (pcm: ByteBuffer, ptsUs: Long, channels: Int) -> Unit
    ) {
        val ex = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            setSource(context, ex, uri)
            val (track, fmt) = audioTrack(ex) ?: error("این فایل صوتی قابل خواندن نیست.")
            ex.selectTrack(track)
            if (fromUs > 0) ex.seekTo(fromUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val mime = fmt.getString(MediaFormat.KEY_MIME)!!
            decoder = MediaCodec.createDecoderByType(mime).also { it.configure(fmt, null, null, 0); it.start() }
            val dec = decoder
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = dec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = dec.getInputBuffer(inIdx)!!
                        val n = ex.readSampleData(buf, 0)
                        val pts = ex.sampleTime
                        if (n < 0 || pts > toUs) {
                            dec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true
                        } else {
                            dec.queueInputBuffer(inIdx, 0, n, pts, 0); ex.advance()
                        }
                    }
                }
                val outIdx = dec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                        channels = dec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    outIdx >= 0 -> {
                        val buf = dec.getOutputBuffer(outIdx)!!
                        if (info.size > 0 && info.presentationTimeUs >= fromUs && info.presentationTimeUs < toUs) {
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            val copy = ByteBuffer.allocate(info.size).order(ByteOrder.LITTLE_ENDIAN)
                            copy.put(buf); copy.flip()
                            sink(copy, info.presentationTimeUs, channels)
                        }
                        dec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            runCatching { decoder?.stop() }; decoder?.release(); ex.release()
        }
    }

    private fun setSource(context: Context, ex: MediaExtractor, uri: Uri) {
        if (uri.scheme.equals("file", true)) ex.setDataSource(uri.path!!)
        else context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { ex.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            ?: error("فایل صوتی باز نشد.")
    }

    private fun audioTrack(ex: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until ex.trackCount) {
            val f = ex.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) return i to f
        }
        return null
    }

    private fun MediaFormat.getLongOrDefault(key: String, def: Long): Long =
        if (containsKey(key)) getLong(key) else def

    fun formatMb(bytes: Long): String = String.format(java.util.Locale.US, "%.2f", bytes / (1024.0 * 1024.0))

    /**
     * تبدیل نرخ نمونه (درون‌یابی خطی) + تغییر تعداد کانال (downmix/duplicate).
     * ورودی/خروجی PCM 16-bit LE interleaved.
     */
    private class Resampler(private val inRate: Int, private val outRate: Int, private val inCh: Int, private val outCh: Int) {
        private var carryL = 0f
        private var carryR = 0f
        private var hasCarry = false
        private var frac = 0.0
        private val step = inRate.toDouble() / outRate

        fun process(input: ByteBuffer): ByteBuffer {
            input.order(ByteOrder.LITTLE_ENDIAN)
            val inFrames = input.remaining() / (2 * inCh)
            if (inFrames == 0) return ByteBuffer.allocate(0)
            // نمونه‌های ورودی به مونو/استریوی float
            val l = FloatArray(inFrames + 1)
            val r = FloatArray(inFrames + 1)
            var off = 0
            if (hasCarry) { l[0] = carryL; r[0] = carryR; off = 1 }
            for (i in 0 until inFrames) {
                var sl = 0f; var sr = 0f
                if (inCh == 1) { sl = input.getShort(input.position() + i * 2) / 32768f; sr = sl }
                else {
                    sl = input.getShort(input.position() + i * 2 * inCh) / 32768f
                    sr = input.getShort(input.position() + i * 2 * inCh + 2) / 32768f
                    if (inCh > 2) { // میانگین کانال‌های اضافه
                        var acc = 0f
                        for (c in 0 until inCh) acc += input.getShort(input.position() + (i * inCh + c) * 2) / 32768f
                        sl = acc / inCh; sr = sl
                    }
                }
                l[i + off] = sl; r[i + off] = sr
            }
            val total = inFrames + off
            val outMax = ((total - 1 - frac) / step).toInt() + 2
            val out = ByteBuffer.allocate(max(0, outMax) * 2 * outCh).order(ByteOrder.LITTLE_ENDIAN)
            var pos = frac
            while (pos < total - 1 && out.remaining() >= 2 * outCh) {
                val i0 = pos.toInt(); val t = (pos - i0).toFloat()
                val vl = l[i0] + (l[i0 + 1] - l[i0]) * t
                val vr = r[i0] + (r[i0 + 1] - r[i0]) * t
                if (outCh == 1) out.putShort(clip((vl + vr) * 0.5f))
                else { out.putShort(clip(vl)); out.putShort(clip(vr)) }
                pos += step
            }
            frac = pos - (total - 1)
            carryL = l[total - 1]; carryR = r[total - 1]; hasCarry = true
            input.position(input.limit())
            out.flip()
            return out
        }

        fun flush(): ByteBuffer {
            if (!hasCarry) return ByteBuffer.allocate(0)
            val out = ByteBuffer.allocate(2 * outCh).order(ByteOrder.LITTLE_ENDIAN)
            if (outCh == 1) out.putShort(clip((carryL + carryR) * 0.5f)) else { out.putShort(clip(carryL)); out.putShort(clip(carryR)) }
            hasCarry = false
            out.flip()
            return out
        }

        private fun clip(v: Float): Short = (v.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
    }
}
