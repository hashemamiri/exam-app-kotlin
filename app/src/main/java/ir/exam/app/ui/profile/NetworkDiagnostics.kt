package ir.exam.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.exam.app.core.network.SupabaseRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * V335.20 — «تست اتصال به سرور»: دکمه‌ای که همهٔ مسیرهای ممکن (پل روی دامنهٔ خودمان و اتصال مستقیم
 * به Supabase) را می‌سنجد و نشان می‌دهد کدام سالم است. برای شبکه‌هایی که دامنهٔ مستقیم را بسته‌اند
 * (نمونه: دادهٔ همراه اول) کاربر می‌تواند مسیر سالم را دستی «قفل» کند یا به انتخاب خودکار برگردد.
 */
@Composable
internal fun NetworkDiagnosticsCard() {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var probes by remember { mutableStateOf(SupabaseRouter.lastProbe) }
    var message by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf(SupabaseRouter.currentBase()) }
    var manual by remember { mutableStateOf(SupabaseRouter.manualBase()) }

    suspend fun runTest() {
        testing = true
        val results = withContext(Dispatchers.IO) { SupabaseRouter.probeAll() }
        probes = results
        SupabaseRouter.refreshAsync(force = true)
        active = SupabaseRouter.currentBase()
        manual = SupabaseRouter.manualBase()
        testing = false
        message = if (results.any { it.ok }) "اتصال سالم است." else "هیچ مسیری در این شبکه پاسخ نداد؛ اینترنت را بررسی کنید."
    }

    LaunchedEffect(Unit) { if (probes.isEmpty()) runTest() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "اگر اپ در شبکهٔ یک اپراتور (مثل دادهٔ همراه اول) به سرور وصل نمی‌شود، این تست نشان می‌دهد کدام مسیر سالم است. " +
                "برنامه به‌طور خودکار مسیر سالم را انتخاب می‌کند و با تغییر شبکه دوباره آزمایش می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { runTest() } }, enabled = !testing) {
                if (testing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("در حال تست…")
                } else Text("تست اتصال")
            }
            OutlinedButton(
                onClick = {
                    SupabaseRouter.selectManual(null)
                    active = SupabaseRouter.currentBase()
                    manual = null
                    message = "انتخاب خودکار مسیر فعال شد."
                },
                enabled = manual != null
            ) { Text("انتخاب خودکار") }
            TextButton(
                onClick = { clipboard.setText(AnnotatedString(SupabaseRouter.report())) },
                enabled = probes.isNotEmpty()
            ) { Text("کپی گزارش") }
        }
        probes.forEach { probe ->
            val isDirect = probe.base.equals(SupabaseRouter.directBase, ignoreCase = true)
            val isActive = probe.base.equals(active, ignoreCase = true)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (isDirect) "اتصال مستقیم" else "مسیر جایگزین") + (if (isActive) " — فعال" else ""),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (probe.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        (if (probe.ok) "سالم" else "ناموفق") + " · " + probe.ms + " میلی‌ثانیه · " + probe.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (probe.ok && !isActive) {
                    TextButton(onClick = {
                        SupabaseRouter.selectManual(probe.base)
                        active = SupabaseRouter.currentBase()
                        manual = SupabaseRouter.manualBase()
                        message = "این مسیر انتخاب شد."
                    }) { Text("انتخاب") }
                }
            }
        }
        if (probes.isEmpty() && !testing) {
            Text("هنوز تستی انجام نشده است.", style = MaterialTheme.typography.bodySmall)
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        Text(
            "برای فعال کردن مسیر جایگزین، یک‌بار راهنمای «پل Supabase روی Cloudflare» را انجام دهید؛ پس از آن اپ روی همهٔ شبکه‌ها کار می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
    }
}
