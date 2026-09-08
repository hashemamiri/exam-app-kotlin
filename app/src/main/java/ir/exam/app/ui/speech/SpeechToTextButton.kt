package ir.exam.app.ui.speech

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.speech.SpeechMathConverter

/**
 * V108–V112 — میکروفونِ گفتار به متن (فارسی/انگلیسی) کنار آیکن تصویر.
 *
 * V112 — چرا بازنویسی شد: نسخه‌های V109–V111 با `SpeechRecognizer` داخل برنامه و
 * «شنیدن پیوسته» کار می‌کردند؛ هر قطعهٔ چندثانیه‌ای یک `startListening` تازه بود که
 * ۱) بوق سیستمیِ شروع/پایان می‌زد («آلارم»)، ۲) پنجره را تکان می‌داد، ۳) چون
 * موتور هر بار از صفر شروع می‌کرد تشخیص ضعیف بود.
 *
 * حالا از **پنجرهٔ استاندارد گفتارِ خود اندروید/گوگل** (`ACTION_RECOGNIZE_SPEECH`)
 * استفاده می‌شود: یک بوق، یک پنجرهٔ آشنا، بهترین کیفیت تشخیص گوگل، و مجوز
 * میکروفون را هم خودش مدیریت می‌کند. نتیجه (n-best → بهترین گزینه، اعداد گفتاری →
 * رقم، بدون هیچ فرمولی) در کادرِ قابل‌ویرایش می‌نشیند؛ «ضبط بیشتر» جملهٔ بعدی را
 * به انتهای متن اضافه می‌کند؛ «درج» متن را در سؤال می‌گذارد. هیچ عنصر لحظه‌ای
 * (نوار صدا، نتیجهٔ جزئی، وضعیت) در برنامه وجود ندارد.
 */
@Composable
fun SpeechToTextButton(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    var languageMenu by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("fa-IR") }
    var transcript by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // لغو توسط کاربر یا سکوت: اگر متنی از قبل هست، ویرایشگر باز بماند.
            if (transcript.isBlank()) editorOpen = false
            return@rememberLauncherForActivityResult
        }
        val list = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val best = SpeechMathConverter.pickBest(list)
        val cleaned = SpeechMathConverter.convertPlain(best)
        if (cleaned.isNotBlank()) {
            transcript = if (transcript.isBlank()) cleaned else transcript.trimEnd() + " " + cleaned
        }
        editorOpen = true
    }

    fun launchRecognizer(lang: String) {
        language = lang
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (lang == "fa-IR") "متن سؤال را بگویید…" else "Speak the question text…")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        try {
            recognizer.launch(intent)
        } catch (_: ActivityNotFoundException) {
            error = "سرویس گفتار به متن روی این دستگاه نصب نیست. برنامهٔ Google را نصب/به‌روز کنید."
        }
    }

    IconButton(onClick = { languageMenu = true }, modifier = modifier) {
        Icon(
            Icons.Outlined.Mic,
            contentDescription = "گفتار به متن (فارسی/انگلیسی)",
            tint = MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
        DropdownMenuItem(text = { Text("🎤 فارسی") }, onClick = { languageMenu = false; transcript = ""; launchRecognizer("fa-IR") })
        DropdownMenuItem(text = { Text("🎤 English") }, onClick = { languageMenu = false; transcript = ""; launchRecognizer("en-US") })
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (language == "fa-IR") "متن شنیده‌شده" else "Recognized text") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "متن را در صورت نیاز اصلاح کنید. «ضبط بیشتر» جملهٔ بعدی را به انتهای متن اضافه می‌کند.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { launchRecognizer(language) }) { Text("ضبط بیشتر") }
                    TextButton(
                        onClick = {
                            val out = transcript.trim()
                            if (out.isNotBlank()) currentOnText(out)
                            transcript = ""; editorOpen = false
                        },
                        enabled = transcript.isNotBlank()
                    ) { Text("درج") }
                }
            },
            dismissButton = { TextButton(onClick = { transcript = ""; editorOpen = false }) { Text("انصراف") } }
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
