package ir.exam.app.ui.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.exam.app.core.security.AppLockManager

private const val SYSTEM_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable
fun AppLockGate(userId: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val manager = remember { AppLockManager(context) }
    var locked by remember(userId) { mutableStateOf(manager.enabled(userId)) }
    var error by remember { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current
    val prompt = rememberSystemBiometricPrompt(
        onSuccess = { locked = false; error = null },
        onError = { error = it }
    )

    DisposableEffect(lifecycle, userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && manager.enabled(userId)) locked = true
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    if (!locked) {
        content()
        return
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("برنامه قفل است", style = MaterialTheme.typography.headlineSmall)
        Text(
            "با روش امن فعال دستگاه—اثر انگشت، چهره، الگو، PIN یا رمز دستگاه—هویت خود را تأیید کنید.",
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(
            onClick = {
                error = null
                prompt?.authenticate(systemPromptInfo())
                    ?: run { error = "این صفحه باید در Activity امن برنامه باز شود." }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تأیید با قفل امن دستگاه")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun AppLockSettings(userId: String, embedded: Boolean = false) {
    val context = LocalContext.current
    val manager = remember { AppLockManager(context) }
    var enabled by remember(userId) { mutableStateOf(manager.enabled(userId)) }
    var message by remember { mutableStateOf<String?>(null) }
    // V62.3 — درخواست کاربر: فعال/غیرفعال کردن قفل باید خودش قفل امن دستگاه
    // را طلب کند؛ وضعیت هدف تا تأیید موفق اینجا نگه داشته می‌شود.
    var pendingToggle by remember { mutableStateOf<Boolean?>(null) }
    val available = remember(context) {
        BiometricManager.from(context).canAuthenticate(SYSTEM_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    val prompt = rememberSystemBiometricPrompt(
        onSuccess = {
            val target = pendingToggle
            if (target != null) {
                manager.setEnabled(userId, target)
                enabled = target
                pendingToggle = null
                message = if (target) "قفل برنامه فعال شد." else "قفل برنامه غیرفعال شد."
            } else {
                message = "قفل امن دستگاه با موفقیت تأیید شد."
            }
        },
        onError = {
            pendingToggle = null
            message = it
        }
    )

    val body: @Composable () -> Unit = {
        Column(
            modifier = if (embedded) Modifier.fillMaxWidth() else Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!embedded) Text("قفل برنامه", style = MaterialTheme.typography.titleMedium)
            Text(
                "برنامه از پنجره رسمی Android استفاده می‌کند و نوع رمز یا داده زیستی را ذخیره نمی‌کند."
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("قفل هنگام بازگشت به برنامه")
                    Text(
                        if (available) "اثر انگشت، چهره، الگو، PIN یا رمز فعال دستگاه"
                        else "ابتدا یک قفل امن در تنظیمات دستگاه فعال کنید.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = enabled,
                    enabled = available,
                    onCheckedChange = { target ->
                        // V62.3 — تغییر وضعیت فقط پس از تأیید قفل امن دستگاه اعمال می‌شود.
                        message = null
                        pendingToggle = target
                        prompt?.authenticate(togglePromptInfo(target)) ?: run {
                            pendingToggle = null
                            message = "این صفحه باید در Activity امن برنامه باز شود."
                        }
                    }
                )
            }
            if (enabled) {
                OutlinedButton(
                    onClick = { prompt?.authenticate(systemPromptInfo()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("آزمایش قفل امن دستگاه")
                }
            }
            if (!available) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure { message = "بازکردن تنظیمات امنیتی دستگاه ممکن نشد." }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بازکردن تنظیمات امنیتی دستگاه")
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (embedded) body() else Card(Modifier.fillMaxWidth()) { body() }
}

@Composable
private fun rememberSystemBiometricPrompt(
    onSuccess: () -> Unit,
    onError: (String) -> Unit
): BiometricPrompt? {
    val context = LocalContext.current
    val activity = context.findFragmentActivity() ?: return null
    val latestSuccess by rememberUpdatedState(onSuccess)
    val latestError by rememberUpdatedState(onError)
    return remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    latestSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    latestError(
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> "تأیید هویت لغو شد."
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "قفل دستگاه موقتاً اجازه تلاش نمی‌دهد."
                            else -> errString.toString().take(160).ifBlank { "تأیید هویت انجام نشد." }
                        }
                    )
                }

                override fun onAuthenticationFailed() {
                    latestError("هویت تأیید نشد؛ دوباره تلاش کنید.")
                }
            }
        )
    }
}

private fun systemPromptInfo(): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle("بازکردن آزمون آنلاین")
    .setSubtitle("از قفل امن فعال دستگاه استفاده کنید")
    .setAllowedAuthenticators(SYSTEM_AUTHENTICATORS)
    .build()

/** V62.3 — پنجرهٔ تأیید هویت مخصوص فعال/غیرفعال کردن قفل برنامه. */
private fun togglePromptInfo(enable: Boolean): BiometricPrompt.PromptInfo =
    BiometricPrompt.PromptInfo.Builder()
        .setTitle(if (enable) "فعال‌سازی قفل برنامه" else "غیرفعال‌سازی قفل برنامه")
        .setSubtitle("برای تغییر وضعیت قفل، هویت خود را با قفل امن دستگاه تأیید کنید")
        .setAllowedAuthenticators(SYSTEM_AUTHENTICATORS)
        .build()

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
