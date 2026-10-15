package ir.exam.app.ui.student

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.ui.audio.AnswerAudioPlayer
import kotlinx.coroutines.delay
import java.io.File

/**
 * V309 — پاسخ صوتی دانش‌آموز: ضبط با سقف مدت تعیین‌شدهٔ معلم (خودکار پایان می‌یابد).
 * فایل AAC کم‌حجم در ظرف m4a ذخیره می‌شود و مثل نقاشی به‌عنوان پاسخ سؤال ثبت می‌شود.
 */
@Composable
internal fun StudentVoiceAnswerEntry(
    questionId: String,
    current: List<String>,
    maxSeconds: Int,
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String, String) -> Unit
) {
    val context = LocalContext.current
    var recorder by remember(questionId) { mutableStateOf<MediaRecorder?>(null) }
    var file by remember(questionId) { mutableStateOf<File?>(null) }
    var elapsed by remember(questionId) { mutableIntStateOf(0) }
    var error by remember(questionId) { mutableStateOf<String?>(null) }
    val recording = recorder != null
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording(context, questionId, { recorder = it }, { file = it }, { error = it })
        else error = "برای ضبط پاسخ صوتی باید اجازهٔ میکروفون را بدهید."
    }

    fun stopVoiceRecording() {
        val active = recorder ?: return
        recorder = null
        val saved = file
        file = null
        val ok = runCatching { active.stop() }.isSuccess
        runCatching { active.release() }
        if (ok && saved != null && saved.length() > 0) {
            error = null
            onAdd(questionId, listOf(Uri.fromFile(saved).toString()))
        } else {
            saved?.delete()
            error = "ضبط خیلی کوتاه بود؛ دوباره ضبط کنید."
        }
    }

    // شمارش زمان و پایان خودکار در سقف مدت
    LaunchedEffect(recorder) {
        val active = recorder ?: return@LaunchedEffect
        elapsed = 0
        while (recorder === active && elapsed < maxSeconds) {
            delay(1000)
            if (recorder === active) elapsed++
        }
        if (recorder === active) stopVoiceRecording()
    }

    DisposableEffect(questionId) {
        onDispose {
            recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
            recorder = null
            file?.delete()
            file = null
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("پاسخ صوتی — سقف ضبط: ${PersianDigits.convert(maxSeconds)} ثانیه", style = MaterialTheme.typography.bodyLarge)
        when {
            recording -> {
                Text("● در حال ضبط: ${PersianDigits.convert(elapsed)} از ${PersianDigits.convert(maxSeconds)} ثانیه")
                Button(onClick = { stopVoiceRecording() }) { Text("⏹ پایان ضبط") }
            }
            current.isNotEmpty() -> {
                AnswerAudioPlayer(current.first())
                TextButton(onClick = {
                    error = null
                    onRemove(questionId, current.first())
                }) { Text("🗑 حذف و ضبط دوباره") }
            }
            else -> {
                Button(onClick = {
                    error = null
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) startVoiceRecording(context, questionId, { recorder = it }, { file = it }, { error = it })
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("🎙 شروع ضبط") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** V309 — شروع ضبط در فایل موقت؛ خطاها به onError گزارش می‌شوند. */
private fun startVoiceRecording(
    context: Context,
    questionId: String,
    onRecorder: (MediaRecorder?) -> Unit,
    onFile: (File?) -> Unit,
    onError: (String?) -> Unit
) {
    val safe = questionId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
    val target = File(context.cacheDir, "voice_${safe}_${System.currentTimeMillis()}.m4a")
    @Suppress("DEPRECATION")
    val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
    try {
        created.setAudioSource(MediaRecorder.AudioSource.MIC)
        created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        created.setAudioChannels(1)
        created.setAudioSamplingRate(22050)
        created.setAudioEncodingBitRate(32000)
        created.setOutputFile(target.absolutePath)
        created.prepare()
        created.start()
        onFile(target)
        onRecorder(created)
        onError(null)
    } catch (_: Exception) {
        runCatching { created.release() }
        target.delete()
        onError("ضبط صدا شروع نشد. دوباره تلاش کنید.")
    }
}
