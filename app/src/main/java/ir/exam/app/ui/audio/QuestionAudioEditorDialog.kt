package ir.exam.app.ui.audio

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.core.audio.AudioTranscoder
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.ui.builder.audioChargeForBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * V135 — ویرایشگر صوت سؤال (معلم): انتخاب فایل → شکل موج → کشیدن دو دستگیرهٔ شروع/پایان
 * → پیش‌شنود بازه → «حجم زندهٔ» تخمینی (و هزینه) → فشرده‌سازی تطبیقی AAC (≤ 3MB) →
 * خروجی file:// برای ذخیره در سؤال. اگر پس از فشرده‌سازی از ۳MB بیشتر شد، خطا.
 */
@Composable
fun QuestionAudioEditorDialog(
    questionId: String,
    existingUri: String?,
    existingBytes: Long,
    existingMs: Long,
    onDismiss: () -> Unit,
    onApply: (uri: String, bytes: Long, durationMs: Long) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf<AudioTranscoder.Info?>(null) }
    var wave by remember { mutableStateOf<FloatArray?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var encoding by remember { mutableStateOf(false) }
    var encodeProgress by remember { mutableFloatStateOf(0f) }

    // پیش‌شنود
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var playPosMs by remember { mutableStateOf(0L) }
    fun stopPreview() { runCatching { player?.stop() }; runCatching { player?.release() }; player = null; playing = false }
    DisposableEffect(Unit) { onDispose { stopPreview() } }
    LaunchedEffect(playing) {
        while (playing) {
            val p = player ?: break
            val pos = runCatching { p.currentPosition.toLong() }.getOrDefault(0L)
            playPosMs = pos
            if (pos >= endMs - 40) { stopPreview(); break }
            delay(120)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        stopPreview()
        source = uri; info = null; wave = null; error = null; loading = true; loadProgress = 0f
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val i = AudioTranscoder.info(context, uri)
                    val w = AudioTranscoder.waveform(context, uri, 240) { loadProgress = it }
                    i to w
                }
            }
            loading = false
            r.onSuccess { (i, w) ->
                if (i.durationMs <= 0) { error = "مدت این فایل صوتی خوانده نشد."; return@onSuccess }
                info = i; wave = w; startMs = 0L; endMs = i.durationMs
            }.onFailure { error = it.message ?: "این فایل صوتی پشتیبانی نمی‌شود." }
        }
    }

    val selMs = (endMs - startMs).coerceAtLeast(0L)
    val planned = AudioTranscoder.plannedBitrate(selMs)
    val estBytes = planned?.let { AudioTranscoder.estimateBytes(selMs, it) }
    val tooLong = info != null && planned == null

    AlertDialog(
        onDismissRequest = { if (!encoding) { stopPreview(); onDismiss() } },
        title = { Text("فایل صوتی سؤال") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existingUri != null && source == null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("این سؤال یک فایل صوتی دارد.", fontWeight = FontWeight.Bold)
                            Text("مدت ${fmtMs(existingMs)} · حجم ${AudioTranscoder.formatMb(existingBytes)} مگابایت · هزینه ${PersianDigits.convert(audioChargeForBytes(existingBytes))} تومان",
                                style = MaterialTheme.typography.bodySmall)
                            QuestionAudioPlayer(url = existingUri, durationMs = existingMs, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f), enabled = !encoding) {
                        Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (source == null && existingUri == null) "انتخاب فایل صوتی" else "فایل دیگر")
                    }
                    if (existingUri != null) {
                        OutlinedButton(onClick = { stopPreview(); onRemove() }, enabled = !encoding) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف صوت", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (loading) {
                    LinearProgressIndicator(progress = { loadProgress }, modifier = Modifier.fillMaxWidth())
                    Text("در حال خواندن فایل و ساخت شکل موج…", style = MaterialTheme.typography.bodySmall)
                }
                val i = info
                val w = wave
                if (i != null && w != null) {
                    Text("مدت کل ${fmtMs(i.durationMs)} · ${PersianDigits.convert(i.sampleRate)} هرتز · ${if (i.channels >= 2) "استریو" else "مونو"}" +
                        (if (i.sourceBytes > 0) " · حجم اصلی ${AudioTranscoder.formatMb(i.sourceBytes)} مگابایت" else ""),
                        style = MaterialTheme.typography.bodySmall)
                    Text("برای انتخاب بخش دلخواه، دو دستگیرهٔ آبی را بکشید؛ با لمس روی موج، پخش از همان‌جا شروع می‌شود.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WaveformTrimmer(
                        peaks = w,
                        totalMs = i.durationMs,
                        startMs = startMs,
                        endMs = endMs,
                        playPosMs = if (playing) playPosMs else null,
                        onRange = { s0, e0 -> startMs = s0; endMs = e0; if (playing) stopPreview() },
                        onSeekPlay = { ms ->
                            stopPreview()
                            val src = source ?: return@WaveformTrimmer
                            runCatching {
                                val mp = MediaPlayer()
                                mp.setDataSource(context, src)
                                mp.prepare()
                                mp.seekTo(ms.coerceIn(startMs, endMs).toInt())
                                mp.setOnCompletionListener { playing = false }
                                mp.start()
                                player = mp; playing = true
                            }.onFailure { error = "پخش پیش‌شنود ممکن نشد." }
                        }
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            if (playing) stopPreview() else {
                                val src = source ?: return@IconButton
                                runCatching {
                                    val mp = MediaPlayer()
                                    mp.setDataSource(context, src); mp.prepare()
                                    mp.seekTo(startMs.toInt()); mp.setOnCompletionListener { playing = false }; mp.start()
                                    player = mp; playing = true
                                }.onFailure { error = "پخش پیش‌شنود ممکن نشد." }
                            }
                        }) {
                            Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = if (playing) "توقف" else "پخش بازه")
                        }
                        Text("${fmtMs(startMs)} ← ${fmtMs(endMs)}  (${fmtMs(selMs)})", style = MaterialTheme.typography.bodyMedium)
                    }
                    // حجم زنده
                    Surface(shape = RoundedCornerShape(10.dp), color = if (tooLong) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (tooLong) {
                                Text("این بازه حتی با کمترین کیفیت از ۳ مگابایت بیشتر می‌شود؛ بازه را کوتاه‌تر کنید.", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            } else if (estBytes != null && planned != null) {
                                Text("حجم تقریبی خروجی: ${AudioTranscoder.formatMb(estBytes)} مگابایت (AAC ${PersianDigits.convert(planned / 1000)} kbps${if (planned < 48_000) "، مونو" else ""})", fontWeight = FontWeight.Bold)
                                Text("هزینهٔ صوت این سؤال: ${PersianDigits.convert(audioChargeForBytes(estBytes))} تومان · سقف ۳ مگابایت", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (encoding) {
                    LinearProgressIndicator(progress = { encodeProgress }, modifier = Modifier.fillMaxWidth())
                    Text("در حال برش و فشرده‌سازی…", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            val src = source
            Button(
                enabled = src != null && info != null && !encoding && !loading && !tooLong && selMs >= 500,
                onClick = {
                    val s0 = src ?: return@Button
                    stopPreview(); encoding = true; error = null; encodeProgress = 0f
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                val dir = File(context.filesDir, "question_audio").apply { mkdirs() }
                                AudioTranscoder.transcode(context, s0, startMs, endMs, dir) { encodeProgress = it }
                            }
                        }
                        encoding = false
                        r.onSuccess { res ->
                            if (res.bytes > AudioTranscoder.MAX_BYTES) {
                                res.file.delete()
                                error = "حجم فایل پس از فشرده‌سازی ${AudioTranscoder.formatMb(res.bytes)} مگابایت است؛ سقف ۳ مگابایت. بازهٔ کوتاه‌تری انتخاب کنید."
                            } else onApply(res.file.toURI().toString(), res.bytes, res.durationMs)
                        }.onFailure { error = it.message ?: "فشرده‌سازی ناموفق بود." }
                    }
                }
            ) { Text("برش و افزودن به سؤال") }
        },
        dismissButton = { TextButton(enabled = !encoding, onClick = { stopPreview(); onDismiss() }) { Text("انصراف") } }
    )
}

/** شکل موج + دو دستگیرهٔ کشیدنی + نشانگر پخش. */
@Composable
private fun WaveformTrimmer(
    peaks: FloatArray,
    totalMs: Long,
    startMs: Long,
    endMs: Long,
    playPosMs: Long?,
    onRange: (Long, Long) -> Unit,
    onSeekPlay: (Long) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)
    val sel = MaterialTheme.colorScheme.primary.copy(alpha = .16f)
    val play = MaterialTheme.colorScheme.error
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(0) } // 0 هیچ، 1 شروع، 2 پایان
    fun xOf(ms: Long) = (ms.toFloat() / totalMs) * widthPx
    fun msOf(x: Float) = ((x / widthPx) * totalMs).toLong().coerceIn(0L, totalMs)
    val minGap = max(500L, totalMs / 200)
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), RoundedCornerShape(10.dp))
            .pointerInput(totalMs) {
                detectDragGestures(
                    onDragStart = { p ->
                        widthPx = size.width.toFloat()
                        val ds = abs(p.x - xOf(startMs)); val de = abs(p.x - xOf(endMs))
                        dragging = if (min(ds, de) > 56f) 0 else if (ds <= de) 1 else 2
                    },
                    onDragEnd = { dragging = 0 },
                    onDragCancel = { dragging = 0 },
                    onDrag = { change, _ ->
                        change.consume()
                        val ms = msOf(change.position.x)
                        when (dragging) {
                            1 -> onRange(min(ms, endMs - minGap).coerceAtLeast(0L), endMs)
                            2 -> onRange(startMs, max(ms, startMs + minGap).coerceAtMost(totalMs))
                        }
                    }
                )
            }
            .pointerInput(totalMs, startMs, endMs) {
                detectTapGestures { p -> widthPx = size.width.toFloat(); onSeekPlay(msOf(p.x)) }
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            widthPx = size.width
            val n = peaks.size
            val bw = size.width / n
            val mid = size.height / 2f
            val xs = xOf(startMs); val xe = xOf(endMs)
            drawRect(sel, topLeft = Offset(xs, 0f), size = Size(xe - xs, size.height))
            for (i in 0 until n) {
                val h = (peaks[i] * (size.height * 0.9f)).coerceAtLeast(2f)
                val x = i * bw
                val inSel = x >= xs && x <= xe
                drawRect(if (inSel) primary else dim, topLeft = Offset(x + bw * 0.15f, mid - h / 2), size = Size(bw * 0.7f, h))
            }
            // دستگیره‌ها
            for ((x, _) in listOf(xs to 1, xe to 2)) {
                drawRect(primary, topLeft = Offset(x - 2f, 0f), size = Size(4f, size.height))
                drawRoundRect(primary, topLeft = Offset(x - 12f, mid - 18f), size = Size(24f, 36f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            }
            playPosMs?.let { pm -> val px = xOf(pm); drawRect(play, topLeft = Offset(px - 1.5f, 0f), size = Size(3f, size.height)) }
        }
    }
}

internal fun fmtMs(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    val m = t / 60; val s = t % 60
    return PersianDigits.convert(String.format(java.util.Locale.US, "%d:%02d", m, s))
}
