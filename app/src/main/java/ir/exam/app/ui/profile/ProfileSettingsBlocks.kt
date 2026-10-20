package ir.exam.app.ui.profile

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.push.PushRegistrar
import kotlinx.coroutines.launch

/**
 * V326 — دو بخش تازهٔ تنظیمات: دسته‌بندی اعلان‌ها (با «حالت درس‌آزمون» و ساعت سکوت) و
 * «حافظه و صف ارسال». جدا شد تا فایل اصلی تنظیمات کوچک و خوانا بماند.
 */
/** V326 — دسته‌بندی اعلان‌ها و بازهٔ سکوت (پیش‌تر فقط یک کلید کلی بود). */
@Composable
internal fun NotificationCategoriesBlock(context: Context) {
    val rows = listOf("grades" to "نمرهٔ جدید", "exam" to "باز شدن آزمون", "calendar" to "پیام تقویم", "requests" to "درخواست‌های مدیر")
    var state by remember {
        mutableStateOf(rows.associate { it.first to PushRegistrar.isCategoryEnabled(context, it.first) })
    }
    var quiet by remember { mutableStateOf(PushRegistrar.quietRange(context).first != PushRegistrar.quietRange(context).second) }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text("دسته‌ها", style = MaterialTheme.typography.titleSmall)
        Text("هر دسته را جدا خاموش کنید؛ پیام‌های خاموش در مرکز اعلان‌ها می‌مانند.", style = MaterialTheme.typography.bodySmall)
        rows.forEach { (key, label) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(state[key] ?: true, { on ->
                    state = state + (key to on)
                    PushRegistrar.setCategoryEnabled(context, key, on)
                })
            }
        }
        var study by remember {
            val (f, t) = PushRegistrar.quietRange(context)
            mutableStateOf(f == 0 && t >= 1439)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("حالت درس‌آزمون")
                Text("تا خاموش کردن، هیچ اعلانی نمایش داده نمی‌شود (مناسب جلسهٔ آزمون)", style = MaterialTheme.typography.bodySmall)
            }
            Switch(study, { on ->
                study = on
                if (on) PushRegistrar.setQuietRange(context, 0, 1439)
                else if (!quiet) PushRegistrar.setQuietRange(context, 0, 0)
            })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ساعت سکوت (۲۳ تا ۷)")
                Text("اعلان‌ها در این بازه نمایش داده نمی‌شوند", style = MaterialTheme.typography.bodySmall)
            }
            Switch(quiet, { on ->
                quiet = on
                if (on) PushRegistrar.setQuietRange(context, 23 * 60, 7 * 60)
                else PushRegistrar.setQuietRange(context, 0, 0)
            })
        }
    }
}

/** V326 — «حافظه و صف ارسال»: حجم مصرفی، پاک‌سازی امن و وضعیت موارد در انتظار. */
@Composable
internal fun LocalStorageBlock(context: Context, userId: String?) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<LocalStorageTools.Report?>(null) }
    var pendingRows by remember { mutableStateOf(listOf<LocalStorageTools.PendingRow>()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        busy = true
        scope.launch {
            report = LocalStorageTools.report(context, userId)
            pendingRows = if (userId.isNullOrBlank()) emptyList() else LocalStorageTools.pendingList(context, userId)
            busy = false
        }
    }
    LaunchedEffect(userId) { refresh() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val r = report
        if (r == null) {
            Text("در حال محاسبهٔ حجم…", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("کش و فایل‌های موقت: " + LocalStorageTools.humanBytes(r.totalBytes), style = MaterialTheme.typography.bodyMedium)
            Text("کش سیستم: " + LocalStorageTools.humanBytes(r.cacheBytes) + " · پیش‌نمایش/چاپ: " + LocalStorageTools.humanBytes(r.filesBytes), style = MaterialTheme.typography.bodySmall)
            if (r.lastBackupText != null) Text("آخرین پشتیبان: " + r.lastBackupText, style = MaterialTheme.typography.bodySmall)
            if (pendingRows.isNotEmpty()) {
                Text("در انتظار ارسال: ${PersianDigits.convert(pendingRows.size)} مورد", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { if (userId != null) LocalStorageTools.retryAll(context, userId); refresh() } }, enabled = !busy) { Text("تلاش دوباره") }
                    OutlinedButton(onClick = { scope.launch { if (userId != null) LocalStorageTools.deleteFailed(context, userId); refresh() } }, enabled = !busy) { Text("حذف ناموفق‌ها") }
                }
            } else if (!userId.isNullOrBlank()) {
                Text("همهٔ پاسخ‌ها ارسال شده‌اند.", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        busy = true
                        val freed = LocalStorageTools.clearOldPreviews(context)
                        msg = "پاک شد: " + LocalStorageTools.humanBytes(freed)
                        refresh()
                    }
                }, enabled = !busy) { Text("پاک‌سازی پیش‌نمایش‌های قدیمی") }
            }
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
