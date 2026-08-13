package ir.exam.app.ui.update

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.BuildConfig
import ir.exam.app.core.update.ApkUpdateManager

@Composable
fun AboutScreen(
    viewModel: UpdateViewModel,
    apkUpdateManager: ApkUpdateManager
) {
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

    LaunchedEffect(state.autoInstallPending, state.downloadedApkPath) {
        val path = state.downloadedApkPath
        if (state.autoInstallPending && path != null) {
            viewModel.markAutoInstallHandled()
            requestInstall(path)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppIdentityCard()
        }
        item {
            UpdateCard(
                state = state,
                onCheck = { viewModel.check(BuildConfig.VERSION_CODE) },
                onDownload = viewModel::downloadAndInstall,
                onInstall = { state.downloadedApkPath?.let(requestInstall) }
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("نصب امن", fontWeight = FontWeight.Bold)
                        Text(
                            "APK فقط از نشانی HTTPS دریافت می‌شود. نام بسته، versionCode، امضای برنامه و در صورت ثبت، SHA-256 نیز پیش از نصب کنترل می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Android برای مرحله نهایی نصب، تأیید شما را نمایش می‌دهد.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIdentityCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text("آزمون آنلاین", style = MaterialTheme.typography.headlineSmall)
                    Text("نسخه بومی Android با Kotlin و Jetpack Compose")
                }
            }
            HorizontalDivider()
            Text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}")
            Text("کد نسخه: ${BuildConfig.VERSION_CODE}")
            Text(
                "شناسه بسته: ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun UpdateCard(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                Text("بروزرسانی برنامه", style = MaterialTheme.typography.titleLarge)
            }

            OutlinedButton(
                onClick = onCheck,
                enabled = !state.checking && !state.downloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("در حال بررسی...")
                } else {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("بررسی بروزرسانی")
                }
            }

            state.message?.let { message ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            state.update?.let { remote ->
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("نسخه جدید ${remote.name}", fontWeight = FontWeight.Bold)
                        Text("کد نسخه ${remote.code}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (remote.required) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "ضروری",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (remote.notesFa.isNotEmpty()) {
                    Text("تغییرات این نسخه:", fontWeight = FontWeight.SemiBold)
                    remote.notesFa.forEach { note -> Text("• $note") }
                }

                if (state.downloading) {
                    state.downloadFraction?.let { fraction ->
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    val totalText = state.totalBytes?.let(::readableBytes)
                    Text(
                        buildString {
                            append("در حال دانلود: ")
                            append(readableBytes(state.downloadedBytes))
                            if (totalText != null) append(" از $totalText")
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (state.downloadedApkPath == null) {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("دانلود و نصب نسخه ${remote.name}")
                    }
                } else {
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("بازکردن نصب‌کننده")
                    }
                }
            }
        }
    }
}

private fun readableBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f مگابایت".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f کیلوبایت".format(bytes / 1024.0)
    else -> "$bytes بایت"
}
