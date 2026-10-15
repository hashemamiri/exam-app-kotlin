package ir.exam.app.ui.audio

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp

/** V309 — پاسخ صوتی دانش‌آموز؛ نشانی‌های صوتی در نمایش پاسخ‌ها با پخش‌کنندهٔ صوت نشان داده می‌شوند. */
fun isAnswerAudioUrl(url: String): Boolean {
    val path = url.substringBefore('?').lowercase()
    return listOf(".m4a", ".webm", ".ogg", ".mp3", ".aac", ".wav").any { path.endsWith(it) }
}

/** V309 — دکمهٔ پخش/توقف برای یک فایل صوتی (نشانی اینترنتی یا محلی). */
@Composable
fun AnswerAudioPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    DisposableEffect(url) {
        onDispose {
            runCatching { player?.release() }
            player = null
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val current = player
            if (current != null) {
                if (playing) { runCatching { current.pause() }; playing = false } else { runCatching { current.start() }; playing = true }
                return@Button
            }
            runCatching {
                val created = MediaPlayer()
                created.setDataSource(context, Uri.parse(url))
                created.setOnPreparedListener { it.start(); playing = true }
                created.setOnCompletionListener { playing = false }
                created.setOnErrorListener { _, _, _ -> playing = false; failed = true; true }
                created.prepareAsync()
                player = created
            }.onFailure { failed = true }
        }) {
            Text(
                when {
                    failed -> "پخش ممکن نشد"
                    playing -> "⏸ توقف پاسخ صوتی"
                    else -> "▶ پخش پاسخ صوتی"
                }
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("🎧", style = MaterialTheme.typography.bodyLarge)
    }
}
