package ir.exam.app.ui.update

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.BuildConfig
import ir.exam.app.core.update.ApkUpdateManager

@Composable
fun AboutScreen(
    viewModel: UpdateViewModel,
    apkUpdateManager: ApkUpdateManager
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val latestApkPath by rememberUpdatedState(state.downloadedApkPath)

    fun openInstaller(path: String) {
        apkUpdateManager.launchInstaller(path)
            .onSuccess { viewModel.reportInstallerOpened() }
            .onFailure(viewModel::reportInstallError)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val path = latestApkPath
        if (path != null && apkUpdateManager.canRequestPackageInstalls()) {
            openInstaller(path)
        } else if (path != null) {
            viewModel.reportPermissionRequired()
        }
    }

    val requestInstall: (String) -> Unit = { path ->
        if (apkUpdateManager.canRequestPackageInstalls()) {
            openInstaller(path)
        } else {
            viewModel.reportPermissionRequired()
            try {
                permissionLauncher.launch(apkUpdateManager.unknownSourcesSettingsIntent())
            } catch (error: ActivityNotFoundException) {
                viewModel.reportInstallError(error)
            }
        }
    }

    // با بازشدن صفحه، بررسی خودکار انجام می‌شود تا لیست تغییرات نسخهٔ جدید
    // بدون نیاز به لمس دکمه دیده شود.
    LaunchedEffect(Unit) { viewModel.check(BuildConfig.VERSION_CODE) }

    LaunchedEffect(state.autoInstallPending, state.downloadedApkPath) {
        val path = state.downloadedApkPath
        if (state.autoInstallPending && path != null) {
            viewModel.markAutoInstallHandled()
            requestInstall(path)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("بروزرسانی برنامه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.check(BuildConfig.VERSION_CODE) },
                    enabled = !state.checking && !state.downloading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.checking) CircularProgressIndicator()
                    else Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                    Text(if (state.checking) "در حال بررسی" else "بررسی بروزرسانی")
                }
                state.update?.let { remote ->
                    if (state.downloading) {
                        state.downloadFraction?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                            ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        state.progressText?.let { Text(it) }
                    } else if (state.downloadedApkPath == null) {
                        Button(onClick = viewModel::downloadAndInstall, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Text("دریافت نسخه ${remote.name}")
                        }
                    } else {
                        Button(
                            onClick = { state.downloadedApkPath?.let(requestInstall) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                            Text("نصب نسخه دریافت‌شده")
                        }
                    }
                }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                    // مسیر جایگزین وقتی DownloadManager سیستم ناموفق است؛ بازکردن صریح در مرورگر.
                    val url = state.update?.apkUrl
                    if (url != null) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure(viewModel::reportInstallError)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Text("دریافت با مرورگر")
                        }
                    }
                }
            }
        }
        // V221 — «تماس با ما و نظرات»: ایمیل پشتیبانی (آینه در سایت: mobile.js aboutSection).
        item { ContactCard(onError = viewModel::reportInstallError) }
        // V282 — شرایط استفاده و حریم خصوصی (متن داخل برنامه + نشانی وب)
        item { LegalCard() }
        // V132 — لیست تغییرات نسخهٔ جدید زیرِ دکمه‌های بررسی/دریافت (همیشه دیده می‌شود؛ چه قبل از دانلود چه بعد از آن).
        // V208 — سه نسخهٔ آخر، هر کدام با تیترهای کوتاه (بلوک اول = نسخهٔ جدید).
        state.update?.takeIf { it.notesFa.isNotEmpty() }?.let { remote ->
            val blocks = ir.exam.app.core.update.ReleaseNotes.lastVersions(remote.notesFa, 3)
            if (blocks.isEmpty()) {
                item { ChangeListCard("تغییرات نسخه ${remote.name}", remote.notesFa) }
            } else {
                blocks.forEachIndexed { index, block ->
                    // V300 — شمارهٔ داخلی Vxxx نمایش داده نمی‌شود؛ شمارهٔ اپ (از پرانتز عنوان) یا نسخهٔ سرور
                    val label = block.appVersion.ifBlank { if (index == 0) remote.name else "" }
                    val title = if (index == 0) "تغییرات نسخه $label (جدید)" else if (label.isNotBlank()) "نسخه $label" else "نسخهٔ قبلی"
                    item { ChangeListCard(title, block.bullets) }
                }
            }
        }
    }
}

const val SUPPORT_EMAIL = "info@onlineexam.ir"

@Composable
private fun ContactCard(onError: (Throwable) -> Unit) {
    val context = LocalContext.current
    fun copySupportEmail() {
        context.getSystemService(android.content.ClipboardManager::class.java)
            ?.setPrimaryClip(android.content.ClipData.newPlainText("support_email", SUPPORT_EMAIL))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تماس با ما و نظرات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "پیشنهاد، گزارش اشکال یا هر نظری دارید، برای ما بنویسید؛ همهٔ پیام‌ها خوانده می‌شوند.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(SUPPORT_EMAIL, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                                    .putExtra(Intent.EXTRA_SUBJECT, "نظر دربارهٔ آزمون‌ساز")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure {
                            copySupportEmail()
                            onError(IllegalStateException("برنامهٔ ایمیل پیدا نشد؛ نشانی کپی شد."))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("ارسال ایمیل") }
                OutlinedButton(
                    onClick = { copySupportEmail() },
                    modifier = Modifier.weight(1f)
                ) { Text("کپی نشانی") }
            }
        }
    }
}

@Composable
private fun ChangeListCard(version: String, notes: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            notes.forEach { raw ->
                // پاک‌سازی علائم markdown تا متن فارسی تمیز و راست‌به‌چپ بماند.
                val clean = raw.trim().removePrefix("-").removePrefix("•").trim().replace("`", "")
                if (clean.isNotEmpty()) {
                    Text("• $clean", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


/** V282 — کارت «شرایط استفاده و حریم خصوصی»: دو دکمه، هر کدام متن کامل را در پنجرهٔ اسکرول‌شونده نشان می‌دهد. */
@Composable
private fun LegalCard() {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("شرایط استفاده و حریم خصوصی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("آخرین به‌روزرسانی: ${ir.exam.app.core.legal.LegalTexts.UPDATED} · نسخهٔ وب: onlineexam.ir/terms.html و onlineexam.ir/privacy.html", style = MaterialTheme.typography.bodySmall)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open = "terms" }, modifier = Modifier.weight(1f)) { Text("شرایط استفاده") }
                OutlinedButton(onClick = { open = "privacy" }, modifier = Modifier.weight(1f)) { Text("حریم خصوصی") }
            }
        }
    }
    open?.let { which ->
        val sections = if (which == "terms") ir.exam.app.core.legal.LegalTexts.TERMS else ir.exam.app.core.legal.LegalTexts.PRIVACY
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(if (which == "terms") "شرایط استفاده" else "حریم خصوصی") },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sections.size) { i ->
                        Text(sections[i].first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(sections[i].second, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { open = null }) { Text("بستن") } }
        )
    }
}
