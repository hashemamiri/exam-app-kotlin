package ir.exam.app.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.BuildConfig
import ir.exam.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

/**
 * V135 — پخش‌کنندهٔ صوت سؤال (دانش‌آموز و پیش‌نمایش معلم): پلی/پاز، نوار قابل‌کشیدن
 * جلو/عقب، زمان جاری/کل. برای URLهای باکت خصوصی Supabase توکن نشست در هدر می‌رود
 * (همان قرارداد SupabaseAuthImageInterceptor).
 */
@Composable
fun QuestionAudioPlayer(url: String, durationMs: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(url) { mutableStateOf(false) }
    var preparing by remember(url) { mutableStateOf(false) }
    var playing by remember(url) { mutableStateOf(false) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var total by remember(url) { mutableStateOf(durationMs.coerceAtLeast(0L)) }
    var pos by remember(url) { mutableStateOf(0L) }
    var scrub by remember(url) { mutableFloatStateOf(-1f) }

    fun release() { runCatching { player?.release() }; player = null; prepared = false; playing = false }
    DisposableEffect(url) { onDispose { release() } }

    fun ensurePlayer(then: (MediaPlayer) -> Unit) {
        player?.takeIf { prepared }?.let { then(it); return }
        if (preparing) return
        preparing = true; error = null
        val mp = MediaPlayer()
        mp.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
        runCatching {
            val headers = authHeaders(url)
            if (headers.isEmpty()) mp.setDataSource(context, Uri.parse(url)) else mp.setDataSource(context, Uri.parse(url), headers)
            mp.setOnPreparedListener {
                prepared = true; preparing = false
                if (it.duration > 0) total = it.duration.toLong()
                then(it)
            }
            mp.setOnCompletionListener { playing = false; pos = total }
            mp.setOnErrorListener { _, _, _ -> error = "پخش فایل صوتی ممکن نشد."; preparing = false; playing = false; true }
            mp.prepareAsync()
            player = mp
        }.onFailure { error = "پخش فایل صوتی ممکن نشد."; preparing = false; runCatching { mp.release() } }
    }

    LaunchedEffect(playing) {
        while (playing) {
            player?.let { p -> if (scrub < 0f) pos = runCatching { p.currentPosition.toLong() }.getOrDefault(pos) }
            delay(200)
        }
    }

    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (playing) { runCatching { player?.pause() }; playing = false }
                    else ensurePlayer { p ->
                        if (pos >= total - 200 && total > 0) { p.seekTo(0); pos = 0 }
                        p.start(); playing = true
                    }
                }) {
                    when {
                        preparing -> CircularProgressIndicator(Modifier.padding(6.dp), strokeWidth = 2.dp)
                        playing -> Icon(Icons.Outlined.Pause, contentDescription = "توقف")
                        pos >= total - 200 && total > 0 && prepared -> Icon(Icons.Outlined.Replay, contentDescription = "پخش دوباره")
                        else -> Icon(Icons.Outlined.PlayArrow, contentDescription = "پخش")
                    }
                }
                val frac = if (total > 0) (if (scrub >= 0f) scrub else pos.toFloat() / total).coerceIn(0f, 1f) else 0f
                Slider(
                    value = frac,
                    onValueChange = { scrub = it; pos = (it * total).toLong() },
                    onValueChangeFinished = {
                        val target = (scrub * total).toLong().coerceIn(0L, total)
                        scrub = -1f
                        pos = target
                        ensurePlayer { p -> p.seekTo(target.toInt()) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(pos), style = MaterialTheme.typography.labelSmall)
                Text(fmtMs(total), style = MaterialTheme.typography.labelSmall)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) }
        }
    }
}

private fun authHeaders(url: String): Map<String, String> {
    val base = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    if (base.isBlank() || !url.startsWith("$base/storage/v1/object/")) return emptyMap()
    val token = runCatching { SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken }.getOrNull()
    if (token.isNullOrBlank()) return emptyMap()
    return mapOf("Authorization" to "Bearer $token", "apikey" to BuildConfig.SUPABASE_ANON_KEY)
}
