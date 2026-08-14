package ir.exam.app.ui.update

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberUpdatedState
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("بروزرسانی برنامه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        state.update?.takeIf { state.downloading && it.notesFa.isNotEmpty() }?.let { remote ->
            item { ChangeListCard("تغییرات نسخه ${remote.name}", remote.notesFa) }
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
                        Text("${readableBytes(state.downloadedBytes)}${state.totalBytes?.let { " از ${readableBytes(it)}" }.orEmpty()}")
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
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ChangeListCard(version: String, notes: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            notes.forEach { Text("• $it") }
        }
    }
}

private fun readableBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f مگابایت".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f کیلوبایت".format(bytes / 1024.0)
    else -> "$bytes بایت"
}
