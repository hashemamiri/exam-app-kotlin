package ir.exam.app.ui.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.exam.app.core.speech.SpeechMathConverter

/**
 * V108–V111 — میکروفونِ گفتار به متن (فارسی/انگلیسی) کنار آیکن تصویر.
 *
 * V111 — نسخهٔ آرام و فقط‌متن:
 * - تشخیص فرمول از روی صدا **کامل حذف** شد؛ فقط متن (با تبدیل عددهای گفتاری
 *   به رقم و نمادهای ساده مثل درصد/علامت سؤال) درج می‌شود.
 * - پنجره هیچ عنصرِ متغیری ندارد: نه نتایج جزئی، نه وضعیت لحظه‌ای، نه نوار
 *   صدا. تنها چیزی که تغییر می‌کند کادر متن است، آن هم فقط وقتی یک جمله
 *   نهایی تشخیص داده شد. در سکوت بی‌سروصدا گوش می‌دهد.
 * - شنیدن پیوسته تا «توقف»؛ بستن فقط با «درج» یا «انصراف».
 */
@Composable
fun SpeechToTextButton(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    var languageMenu by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("fa-IR") }
    var transcript by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    // وضعیت موتور خارج از Compose نگه داشته می‌شود تا هیچ recomposition اضافه‌ای رخ ندهد.
    val engine = remember { SpeechEngineHolder() }

    fun appendSegment(segment: String) {
        val cleaned = SpeechMathConverter.convertPlain(segment)
        if (cleaned.isBlank()) return
        transcript = if (transcript.isBlank()) cleaned else transcript.trimEnd() + " " + cleaned
    }

    fun recognizerIntent(lang: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20000L)
    }

    fun startSegment() {
        if (!engine.wantContinuous) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            engine.wantContinuous = false; recording = false
            error = "سرویس تشخیص گفتار روی این دستگاه در دسترس نیست (Google app / سرویس گفتار را فعال کنید)."
            return
        }
        val sr = engine.recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { engine.recognizer = it }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(code: Int) {
                when (code) {
                    // سکوت / تشخیص‌نشدن: بی‌سروصدا دوباره گوش بده.
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> engine.schedule(200L) { startSegment() }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> {
                        engine.release(); engine.schedule(700L) { startSegment() }
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        engine.wantContinuous = false; recording = false; error = "مجوز میکروفون داده نشده است."
                    }
                    else -> engine.schedule(1000L) { startSegment() }
                }
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = SpeechMathConverter.pickBest(list)
                if (best.isNotBlank()) appendSegment(best)
                engine.schedule(200L) { startSegment() }
            }
        })
        runCatching { sr.startListening(recognizerIntent(language)) }
            .onFailure { engine.release(); engine.schedule(800L) { startSegment() } }
    }

    fun stopListening() {
        engine.wantContinuous = false
        engine.cancelScheduled()
        runCatching { engine.recognizer?.stopListening() }
        recording = false
    }

    fun closeAll() {
        stopListening()
        engine.release()
        dialogOpen = false
        transcript = ""
    }

    fun beginSession(lang: String) {
        language = lang
        transcript = ""
        dialogOpen = true
        recording = true
        engine.wantContinuous = true
        startSegment()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val lang = pendingLanguage
        pendingLanguage = null
        if (granted && lang != null) beginSession(lang) else if (!granted) error = "بدون مجوز میکروفون امکان تبدیل گفتار نیست."
    }

    fun requestStart(lang: String) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) beginSession(lang) else { pendingLanguage = lang; permission.launch(Manifest.permission.RECORD_AUDIO) }
    }

    DisposableEffect(Unit) { onDispose { engine.cancelScheduled(); engine.release() } }

    IconButton(onClick = { if (dialogOpen) stopListening() else languageMenu = true }, modifier = modifier) {
        Icon(
            if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic,
            contentDescription = "گفتار به متن (فارسی/انگلیسی)",
            tint = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
        DropdownMenuItem(text = { Text("🎤 فارسی") }, onClick = { languageMenu = false; requestStart("fa-IR") })
        DropdownMenuItem(text = { Text("🎤 English") }, onClick = { languageMenu = false; requestStart("en-US") })
    }

    if (dialogOpen) {
        AlertDialog(
            // پنجره فقط با دکمه‌های خودش بسته می‌شود.
            onDismissRequest = {},
            title = { Text(if (language == "fa-IR") "گفتار به متن — فارسی" else "Speech to text — English") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (recording) "🔴 در حال ضبط — صحبت کنید؛ هر جمله پس از تشخیص در کادر زیر نوشته می‌شود."
                        else "ضبط متوقف شد — می‌توانید متن را ویرایش و درج کنید.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        label = { Text("متن شنیده‌شده (قابل ویرایش)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (recording) {
                        TextButton(onClick = { stopListening() }) { Text("توقف") }
                    } else {
                        TextButton(onClick = { recording = true; engine.wantContinuous = true; startSegment() }) { Text("ادامهٔ ضبط") }
                    }
                    TextButton(
                        onClick = {
                            stopListening()
                            val out = transcript.trim()
                            if (out.isNotBlank()) currentOnText(out)
                            closeAll()
                        },
                        enabled = transcript.isNotBlank()
                    ) { Text("درج") }
                }
            },
            dismissButton = { TextButton(onClick = { closeAll() }) { Text("انصراف") } }
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("باشد") } }
        )
    }
}

/** وضعیتِ غیر-Compose موتور گفتار (recognizer، زمان‌بندی قطعهٔ بعدی). */
private class SpeechEngineHolder {
    var recognizer: SpeechRecognizer? = null
    @Volatile var wantContinuous: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    fun schedule(delayMs: Long, block: () -> Unit) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ if (wantContinuous) block() }, delayMs)
    }
    fun cancelScheduled() = handler.removeCallbacksAndMessages(null)
    fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
