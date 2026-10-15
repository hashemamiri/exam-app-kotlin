package ir.exam.app.ui.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * V302 — دکمهٔ بزرگ «🔊 بخوان» حالت ابتدایی: متن سؤال (و گزینه‌ها) را با
 * TextToSpeech دستگاه می‌خواند (زبان فارسی اگر موتور داشته باشد، وگرنه زبان
 * پیش‌فرض). وقتی معلم خودش صدا ضبط کرده باشد، این دکمه نشان داده نمی‌شود و
 * پخش‌کنندهٔ صدای معلم (V135) می‌آید. با عوض شدن سؤال خواندن قطع می‌شود.
 */
@Composable
fun ReadAloudButton(text: String, modifier: Modifier = Modifier, key: Any? = text) {
    val context = LocalContext.current.applicationContext
    var ready by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    val tts: TextToSpeech = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.let { e ->
                    val fa = runCatching { e.setLanguage(Locale("fa", "IR")) }.getOrDefault(TextToSpeech.LANG_MISSING_DATA)
                    if (fa == TextToSpeech.LANG_MISSING_DATA || fa == TextToSpeech.LANG_NOT_SUPPORTED) {
                        runCatching { e.setLanguage(Locale("fa")) }
                    }
                    e.setSpeechRate(0.85f)
                }
                ready = true
            } else unavailable = true
        }
        engine!!
    }
    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speaking = true }
            override fun onDone(utteranceId: String?) { speaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { speaking = false }
            override fun onError(utteranceId: String?, errorCode: Int) { speaking = false }
        })
        onDispose { runCatching { tts.stop(); tts.shutdown() } }
    }
    // سؤال عوض شد → خواندن قبلی قطع شود
    DisposableEffect(key) { onDispose { runCatching { tts.stop() }; speaking = false } }
    if (unavailable) return
    Button(
        onClick = {
            if (speaking) { runCatching { tts.stop() }; speaking = false }
            else if (ready && text.isNotBlank()) {
                speaking = true
                val chunks = text.chunked(3500) // محدودیت موتورهای TTS ~۴۰۰۰ نویسه
                chunks.forEachIndexed { i, c ->
                    tts.speak(c, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "read-$i")
                }
            }
        },
        enabled = ready,
        modifier = modifier.heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (speaking) "⏹" else "🔊", fontSize = 24.sp)
            Text(if (speaking) "بس است" else "سؤال را برایم بخوان", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
