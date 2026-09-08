package ir.exam.app.ui.speech

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import ir.exam.app.core.speech.SpeechMathConverter

/**
 * V108 — میکروفونِ گفتار به متن (فارسی/انگلیسی) کنار آیکن تصویر.
 * لمس: منوی زبان (فارسی / English) → شناسایی گفتار با SpeechRecognizer
 * دستگاه → [SpeechMathConverter] عدد/نماد/فرمول را هوشمندانه می‌سازد →
 * اگر عبارت ریاضی باشد `onFormula(tex)` (بازکردن ویرایشگر فرمول برای
 * بازبینی/درج) وگرنه `onText(text)` (درج مستقیم در متن سؤال).
 * بدون شبکهٔ اختصاصی؛ فقط سرویسِ گفتارِ خودِ اندروید. مجوز RECORD_AUDIO
 * هنگام نیاز پرسیده می‌شود.
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
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun deliver(spoken: String) {
        val result = SpeechMathConverter.convert(spoken)
        if (result.text.isBlank()) return
        if (result.containsFormula) currentOnFormula(result.text.trim('$')) else currentOnText(result.text)
    }

    fun stop() {
        recognizer?.stopListening()
        listening = false
    }

    fun start(language: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "سرویس تشخیص گفتار روی این دستگاه در دسترس نیست. (Google app / سرویس گفتار را فعال کنید)"
            return
        }
        recognizer?.destroy()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; partial = "" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(code: Int) {
                listening = false
                error = when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "چیزی شنیده نشد؛ دوباره تلاش کنید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون داده نشده است."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "برای تشخیص گفتار به اینترنت نیاز است."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس گفتار مشغول است؛ لحظه‌ای بعد."
                    else -> "خطای تشخیص گفتار (کد $code)."
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val best = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (best.isNotBlank()) deliver(best)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer = sr
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        listening = true
        sr.startListening(intent)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val lang = pendingLanguage
        pendingLanguage = null
        if (granted && lang != null) start(lang) else if (!granted) error = "بدون مجوز میکروفون امکان تبدیل گفتار نیست."
    }

    fun requestStart(language: String) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) start(language) else { pendingLanguage = language; permission.launch(Manifest.permission.RECORD_AUDIO) }
    }

    DisposableEffect(Unit) { onDispose { recognizer?.destroy(); recognizer = null } }

    IconButton(
        onClick = { if (listening) stop() else languageMenu = true },
        modifier = modifier
    ) {
        Icon(
            if (listening) Icons.Outlined.Stop else Icons.Outlined.Mic,
            contentDescription = if (listening) "توقف ضبط" else "گفتار به متن (فارسی/انگلیسی)",
            tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
        DropdownMenuItem(text = { Text("🎤 فارسی") }, onClick = { languageMenu = false; requestStart("fa-IR") })
        DropdownMenuItem(text = { Text("🎤 English") }, onClick = { languageMenu = false; requestStart("en-US") })
    }
    if (listening) {
        AlertDialog(
            onDismissRequest = { stop() },
            title = { Text("در حال شنیدن…") },
            text = { Text(partial.ifBlank { "صحبت کنید. عددها، نمادها و فرمول‌ها (مثل «ایکس به توان دو») به‌طور خودکار تبدیل می‌شوند." }) },
            confirmButton = { TextButton(onClick = { stop() }) { Text("پایان") } }
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
