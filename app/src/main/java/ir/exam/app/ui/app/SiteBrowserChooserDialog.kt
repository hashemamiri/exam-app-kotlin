package ir.exam.app.ui.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/** V132 — نشانی سایت (کارت «سایت» منوی همبرگری). */
const val SITE_URL = "https://onlineexam.ir"

/** یک مرورگرِ نصب‌شده که می‌تواند نشانیِ https را باز کند. */
internal data class BrowserApp(val packageName: String, val activityName: String, val label: String, val icon: android.graphics.Bitmap?)

/** فهرستِ مرورگرهای نصب‌شده (برای API 30+ نیازمند `<queries>` در مانیفست است). */
internal fun listBrowsers(context: Context, url: String = SITE_URL): List<BrowserApp> {
    val pm = context.packageManager
    val probe = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val infos = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
        else pm.queryIntentActivities(probe, 0)
    }.getOrDefault(emptyList())
    return infos
        .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { info ->
            BrowserApp(
                packageName = info.activityInfo.packageName,
                activityName = info.activityInfo.name,
                label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.activityInfo.packageName),
                icon = runCatching { info.loadIcon(pm).toBitmap(96, 96) }.getOrNull()
            )
        }
        .sortedBy { it.label }
}

internal fun openSiteWith(context: Context, app: BrowserApp?, url: String = SITE_URL, onError: (String) -> Unit) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (app != null) intent.setClassName(app.packageName, app.activityName)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        onError("مرورگری برای بازکردن سایت پیدا نشد.")
    } catch (e: SecurityException) {
        onError("بازکردن سایت با این مرورگر مجاز نیست.")
    }
}

/**
 * V132 — کارت «سایت»: فهرستِ مرورگرهای گوشی را نشان می‌دهد؛ با انتخابِ هر کدام
 * onlineexam.ir در همان مرورگر باز می‌شود. اگر فقط یک مرورگر باشد یا فهرست خالی
 * باشد، مستقیم باز می‌شود.
 */
@Composable
fun SiteBrowserChooserDialog(onDismiss: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val browsers = remember { listBrowsers(context) }
    if (browsers.size <= 1) {
        openSiteWith(context, browsers.firstOrNull(), onError = onError)
        onDismiss()
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بازکردن سایت با…") },
        text = {
            Column {
                Text(SITE_URL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(browsers, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    openSiteWith(context, app, onError = onError)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            app.icon?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp)) }
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
