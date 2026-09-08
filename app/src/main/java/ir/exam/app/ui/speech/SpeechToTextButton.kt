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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
 * V108/V109 — میکروفونِ گفتار به متن (فارسی/انگلیسی) کنار آیکن تصویر.
 *
 * V109 — نسخهٔ حرفه‌ای:
 * - **شنیدنِ پیوسته:** موتور اندروید هر جمله را یک «قطعه» می‌بندد؛ اینجا پس از هر
 *   قطعه (یا خطای «سکوت») بلافاصله قطعهٔ بعدی شروع می‌شود تا وقتی کاربر خودش
 *   «پایان» یا «درج» را بزند. پنجره هرگز خودبه‌خود بسته نمی‌شود.
 * - **n-best:** از هر قطعه چند حدس گرفته می‌شود و [SpeechMathConverter.pickBest]
 *   حدسِ سازگارتر با واژگان ریاضی/عددی را برمی‌گزیند.
 * - **اصلاح خودکار** خطاهای رایج فارسی (بتوان→به توان، اکس→ایکس، …) در مبدل.
 * - **ویرایش پیش از درج:** متنِ تجمیعی در کادر قابل‌ویرایش است؛ پیش‌نمایشِ
 *   «نتیجهٔ درج» (متن/فرمول) زیر آن دیده می‌شود؛ حالت «فرمول» را می‌توان
 *   دستی تغییر داد. «درج» متن را می‌فرستد (فرمول → ویرایشگر فرمول).
 * - نشانگر شدتِ صدا (RMS) و وضعیت («در حال شنیدن» / «در حال پردازش»).
 */
@Composable
fun SpeechToTextButton(
    onText: (String) -> Unit,
    onFormula: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    val currentOnFormula by rememberUpdatedState(onFormula)
    var languageMenu by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("fa-IR") }
    var transcript by remember { mutableStateOf("") }
    var partial by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var forceFormula by remember { mutableStateOf<Boolean?>(null) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var wantContinuous by remember { mutableStateOf(false) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    fun appendSegment(segment: String) {
        val cleaned = SpeechMathConverter.correct(segment)
        if (cleaned.isBlank()) return
        transcript = if (transcript.isBlank()) cleaned else transcript.trimEnd() + " " + cleaned
        partial = ""
    }

    fun recognizerIntent(lang: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        // مکث‌های طولانی‌تر پیش از بستنِ قطعه (موتور ممکن است نادیده بگیرد).
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
    }

    // نگه‌دارندهٔ تابعِ شروعِ قطعه (بازگشتی: listener قطعهٔ بعدی را زمان‌بندی می‌کند).
    val segmentStarter = remember { arrayOfNulls<() -> Unit>(1) }

    fun scheduleNextSegment(delayMs: Long = 250L) {
        if (!wantContinuous || !dialogOpen) return
        handler.postDelayed({ if (wantContinuous && dialogOpen) segmentStarter[0]?.invoke() }, delayMs)
    }

    fun startSegment() { segmentStarter[0]?.invoke() }

    segmentStarter[0] = start@{
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "سرویس تشخیص گفتار روی این دستگاه در دسترس نیست (Google app / سرویس گفتار را فعال کنید)."
            wantContinuous = false; listening = false
            return@start
        }
        val sr = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; processing = false; status = "در حال شنیدن…" }
            override fun onBeginningOfSpeech() { status = "در حال شنیدن…" }
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { processing = true; status = "در حال پردازش…" }
            override fun onError(code: Int) {
                listening = false; processing = false
                when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        status = "سکوت… ادامه دهید"; scheduleNextSegment()
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> {
                        // موتور مشغول/در حال بستن قطعهٔ قبلی: کمی بعد دوباره
                        recognizer?.destroy(); recognizer = null
                        scheduleNextSegment(600L)
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { wantContinuous = false; error = "مجوز میکروفون داده نشده است." }
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        status = "اتصال اینترنت ضعیف است؛ دوباره تلاش می‌شود…"; scheduleNextSegment(1200L)
                    }
                    else -> { status = "خطای موتور گفتار (کد $code)؛ دوباره تلاش می‌شود…"; scheduleNextSegment(800L) }
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false; processing = false
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = SpeechMathConverter.pickBest(list)
                if (best.isNotBlank()) appendSegment(best)
                status = "ادامه دهید یا «درج» را بزنید"
                scheduleNextSegment()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        listening = true
        runCatching { sr.startListening(recognizerIntent(language)) }
            .onFailure { recognizer?.destroy(); recognizer = null; scheduleNextSegment(800L) }
    }

    fun stopListening() {
        wantContinuous = false
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.stopListening() }
        listening = false; processing = false
        status = "متوقف شد — می‌توانید متن را ویرایش و درج کنید"
    }

    fun closeAll() {
        stopListening()
        runCatching { recognizer?.destroy() }
        recognizer = null
        dialogOpen = false
        transcript = ""; partial = ""; forceFormula = null; status = null; level = 0f
    }

    fun beginSession(lang: String) {
        language = lang
        dialogOpen = true
        wantContinuous = true
        transcript = ""; partial = ""; forceFormula = null
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

    DisposableEffect(Unit) {
        onDispose { handler.removeCallbacksAndMessages(null); runCatching { recognizer?.destroy() }; recognizer = null }
    }

    IconButton(onClick = { if (dialogOpen) stopListening() else languageMenu = true }, modifier = modifier) {
        Icon(
            if (listening) Icons.Outlined.Stop else Icons.Outlined.Mic,
            contentDescription = "گفتار به متن (فارسی/انگلیسی)",
            tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
        DropdownMenuItem(text = { Text("🎤 فارسی") }, onClick = { languageMenu = false; requestStart("fa-IR") })
        DropdownMenuItem(text = { Text("🎤 English") }, onClick = { languageMenu = false; requestStart("en-US") })
    }

    if (dialogOpen) {
        val preview = remember(transcript, forceFormula) {
            val r = SpeechMathConverter.convert(transcript)
            val asFormula = forceFormula ?: r.containsFormula
            if (asFormula) "\$" + (if (r.containsFormula) r.text.trim('$') else SpeechMathConverter.toLatexOnly(transcript)) + "\$" else r.text
        }
        val isFormula = preview.length >= 2 && preview.startsWith("$") && preview.endsWith("$")
        AlertDialog(
            // پنجره فقط با دکمه‌های خودش بسته می‌شود (نه لمس بیرون / بازگشت).
            onDismissRequest = {},
            title = { Text(if (language == "fa-IR") "گفتار به متن — فارسی" else "Speech to text — English") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (processing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(status ?: "آمادهٔ شنیدن…", style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(
                        progress = { if (listening) level else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                    if (partial.isNotBlank()) Text("…$partial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        label = { Text("متن شنیده‌شده (قابل ویرایش)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !isFormula, onClick = { forceFormula = false }, label = { Text("متن") })
                        FilterChip(selected = isFormula, onClick = { forceFormula = true }, label = { Text("فرمول") })
                    }
                    if (transcript.isNotBlank()) {
                        Text("نتیجهٔ درج:", style = MaterialTheme.typography.labelMedium)
                        Text(preview, style = MaterialTheme.typography.bodyMedium)
                        if (isFormula) Text("فرمول در ویرایشگر فرمول باز می‌شود تا بازبینی و درج کنید.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (listening || wantContinuous) {
                        TextButton(onClick = { stopListening() }) { Text("توقف") }
                    } else {
                        TextButton(onClick = { wantContinuous = true; startSegment() }) { Text("ادامهٔ ضبط") }
                    }
                    TextButton(
                        onClick = {
                            stopListening()
                            if (transcript.isNotBlank()) {
                                if (isFormula) currentOnFormula(preview.trim('$')) else currentOnText(preview)
                            }
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
