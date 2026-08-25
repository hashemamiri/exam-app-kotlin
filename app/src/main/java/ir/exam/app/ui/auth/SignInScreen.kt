package ir.exam.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation

/** ورود، ثبت‌نام کادر مدرسه و بازیابی رمز؛ هیچ مسیر آزمایشی یا عبور مستقیم ندارد. */
@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("آزمون آنلاین", style = MaterialTheme.typography.headlineMedium)
        when (state.screen) {
            AuthScreen.SIGN_IN -> SignInPane(state, viewModel)
            AuthScreen.REGISTRATION_ROLE -> RegistrationRolePane(state, viewModel)
            AuthScreen.LOGIN_OTP -> OtpPane(
                title = "ورود با کد یک‌بارمصرف",
                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                state = state,
                onVerify = viewModel::verifyLoginOtp,
                onResend = viewModel::sendLoginOtp,
                onBack = viewModel::showSignIn,
                viewModel = viewModel
            )
            AuthScreen.TEACHER_REGISTER -> TeacherRegistrationPane(state, viewModel)
            AuthScreen.TEACHER_REGISTER_OTP -> OtpPane(
                title = "تأیید ایمیل معلم",
                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                state = state,
                onVerify = viewModel::verifyTeacherRegistrationOtp,
                onResend = viewModel::sendTeacherRegistrationOtp,
                onBack = viewModel::showTeacherRegistration,
                viewModel = viewModel
            )
            AuthScreen.TEACHER_REGISTER_SETUP -> TeacherSetupPane(state, viewModel)
            AuthScreen.MANAGER_REGISTER -> ManagerRegistrationPane(state, viewModel)
            AuthScreen.MANAGER_REGISTER_OTP -> OtpPane(
                title = "تأیید ایمیل مدیر/معاون",
                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                state = state,
                onVerify = viewModel::verifyManagerRegistrationOtp,
                onResend = viewModel::sendManagerRegistrationOtp,
                onBack = viewModel::showManagerRegistration,
                viewModel = viewModel
            )
            AuthScreen.MANAGER_REGISTER_SETUP -> ManagerSetupPane(state, viewModel)
            AuthScreen.RECOVERY -> RecoveryPane(state, viewModel)
            AuthScreen.RECOVERY_OTP -> OtpPane(
                title = "تأیید بازیابی حساب",
                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                state = state,
                onVerify = viewModel::verifyRecoveryOtp,
                onResend = viewModel::sendRecoveryOtp,
                onBack = viewModel::showRecovery,
                viewModel = viewModel
            )
            AuthScreen.RECOVERY_PASSWORD -> RecoveryPasswordPane(state, viewModel)
        }

        if (state.isLoading) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
            }
        }
        state.error?.let { Text("خطا: $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun SignInPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("ورود معلم، مدیر/معاون یا دانش‌آموز")
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::setEmail,
        label = { Text("ایمیل کادر مدرسه یا نام کاربری دانش‌آموز") },
        supportingText = { Text("دانش‌آموز همان نام کاربری تحویلی از معلم را وارد کند.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    PasswordField("رمز عبور", state.password, viewModel::setPassword)
    Button(
        onClick = viewModel::signIn,
        enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("ورود با رمز عبور") }
    OutlinedButton(
        onClick = viewModel::sendLoginOtp,
        enabled = !state.isLoading && '@' in state.email,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ورود کادر مدرسه با کد ایمیل") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = viewModel::showRecovery, enabled = !state.isLoading) { Text("رمز را فراموش کرده‌ام") }
        TextButton(onClick = viewModel::showRegistrationRole, enabled = !state.isLoading) { Text("ثبت‌نام") }
    }
}

@Composable
private fun RegistrationRolePane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("نوع ثبت‌نام", style = MaterialTheme.typography.titleLarge)
    Text("نقش حساب را انتخاب کنید.")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = viewModel::showTeacherRegistration,
            enabled = !state.isLoading,
            modifier = Modifier.weight(1f)
        ) { Text("معلم") }
        Button(
            onClick = viewModel::showManagerRegistration,
            enabled = !state.isLoading,
            modifier = Modifier.weight(1f)
        ) { Text("مدیر/معاون") }
    }
    TextButton(onClick = viewModel::showSignIn, enabled = !state.isLoading) { Text("بازگشت به ورود") }
}

@Composable
private fun TeacherRegistrationPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("ثبت‌نام معلم", style = MaterialTheme.typography.titleLarge)
    Text("دانش‌آموز نباید از این بخش ثبت‌نام کند؛ حساب دانش‌آموز را معلم می‌سازد.")
    OutlinedTextField(
        state.fullName,
        viewModel::setFullName,
        label = { Text("نام و نام خانوادگی") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        state.email,
        viewModel::setEmail,
        label = { Text("ایمیل معلم") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = viewModel::sendTeacherRegistrationOtp,
        enabled = !state.isLoading && state.fullName.trim().length >= 2 && '@' in state.email,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ارسال کد تأیید") }
    // V60.0 — ثبت‌نام با گوگل: انتخاب جیمیل ثبت‌شده روی گوشی (Credential Manager).
    GoogleRegisterButton(state = state, viewModel = viewModel, role = "teacher")
    TextButton(onClick = viewModel::showSignIn, enabled = !state.isLoading) { Text("بازگشت به ورود") }
}

@Composable
private fun TeacherSetupPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("تکمیل حساب معلم", style = MaterialTheme.typography.titleLarge)
    Text("ایمیل تأیید شد. نام کاربری نمایشی و رمز ورود را تعیین کنید.")
    OutlinedTextField(
        state.username,
        viewModel::setUsername,
        label = { Text("نام کاربری انگلیسی") },
        supportingText = { Text("۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط؛ ورود معلم همچنان با ایمیل انجام می‌شود.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        state.teacherInviteCode,
        viewModel::setTeacherInviteCode,
        label = { Text("کد دعوت مدرسه (اختیاری)") },
        supportingText = { Text("اگر مدیر مدرسه کد ۶ حرفی یا کد TCH داده است، آن را اینجا وارد کنید.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword)
    PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword)
    Button(
        onClick = viewModel::completeTeacherRegistration,
        enabled = !state.isLoading && state.username.length >= 4 &&
            state.newPassword.length >= 8 && state.newPassword == state.confirmPassword,
        modifier = Modifier.fillMaxWidth()
    ) { Text("تکمیل ثبت‌نام و ورود") }
    TextButton(onClick = viewModel::cancelVerifiedFlow, enabled = !state.isLoading) { Text("انصراف و خروج") }
}

@Composable
private fun ManagerRegistrationPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("ثبت‌نام مدیر/معاون", style = MaterialTheme.typography.titleLarge)
    Text("پس از تأیید ایمیل، یک مدرسهٔ مستقل ایجاد می‌شود.")
    OutlinedTextField(
        state.fullName, viewModel::setFullName,
        label = { Text("نام و نام خانوادگی") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        state.email, viewModel::setEmail,
        label = { Text("ایمیل مدیر/معاون") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = viewModel::sendManagerRegistrationOtp,
        enabled = !state.isLoading && state.fullName.trim().length >= 2 && '@' in state.email,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ارسال کد تأیید") }
    // V60.0 — ثبت‌نام با گوگل برای مدیر/معاون.
    GoogleRegisterButton(state = state, viewModel = viewModel, role = "manager")
    TextButton(onClick = viewModel::showRegistrationRole, enabled = !state.isLoading) { Text("بازگشت") }
}

@Composable
private fun ManagerSetupPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("تکمیل حساب مدیر/معاون", style = MaterialTheme.typography.titleLarge)
    Text("ایمیل تأیید شد. مدرسه و اطلاعات ورود را تعیین کنید.")
    OutlinedTextField(
        state.schoolName, viewModel::setSchoolName,
        label = { Text("نام مدرسه") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state.province, viewModel::setProvince,
            label = { Text("استان") }, singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            state.city, viewModel::setCity,
            label = { Text("شهر") }, singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        state.username, viewModel::setUsername,
        label = { Text("نام کاربری انگلیسی") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword)
    PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword)
    Button(
        onClick = viewModel::completeManagerRegistration,
        enabled = !state.isLoading && state.schoolName.trim().length >= 2 &&
            state.username.length >= 4 && state.newPassword.length >= 8 &&
            state.newPassword == state.confirmPassword,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ساخت مدرسه و ورود") }
    TextButton(onClick = viewModel::cancelVerifiedFlow, enabled = !state.isLoading) { Text("انصراف و خروج") }
}

@Composable
private fun RecoveryPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("بازیابی رمز کادر مدرسه", style = MaterialTheme.typography.titleLarge)
    Text("کد فقط به ایمیل حساب موجود فرستاده می‌شود و حساب تازه‌ای ساخته نمی‌شود.")
    OutlinedTextField(
        state.email,
        viewModel::setEmail,
        label = { Text("ایمیل حساب") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = viewModel::sendRecoveryOtp,
        enabled = !state.isLoading && '@' in state.email,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ارسال کد بازیابی") }
    TextButton(onClick = viewModel::showSignIn, enabled = !state.isLoading) { Text("بازگشت به ورود") }
}

@Composable
private fun RecoveryPasswordPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("تعیین رمز تازه", style = MaterialTheme.typography.titleLarge)
    state.recoveredUsername?.let { Text("نام کاربری حساب: $it") }
    PasswordField("رمز جدید ۸ تا ۷۲ کاراکتر", state.newPassword, viewModel::setNewPassword)
    PasswordField("تکرار رمز جدید", state.confirmPassword, viewModel::setConfirmPassword)
    Button(
        onClick = viewModel::saveRecoveredPassword,
        enabled = !state.isLoading && state.newPassword.length >= 8 &&
            state.newPassword == state.confirmPassword,
        modifier = Modifier.fillMaxWidth()
    ) { Text("ذخیره رمز و ورود") }
    TextButton(onClick = viewModel::cancelVerifiedFlow, enabled = !state.isLoading) { Text("انصراف و خروج") }
}

@Composable
private fun OtpPane(
    title: String,
    hint: String,
    state: AuthUiState,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(hint)
    OutlinedTextField(
        value = state.otp,
        onValueChange = viewModel::setOtp,
        label = { Text("کد یک‌بارمصرف ۶ تا ۸ رقم") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = onVerify,
        enabled = !state.isLoading && state.otp.length in 6..8,
        modifier = Modifier.fillMaxWidth()
    ) { Text("تأیید کد") }
    OutlinedButton(onClick = onResend, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
        Text("ارسال دوباره کد")
    }
    TextButton(onClick = onBack, enabled = !state.isLoading) { Text("بازگشت") }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = passwordTransformation(visible),
        trailingIcon = {
            PasswordVisibilityButton(visible = visible, onToggle = { visible = !visible })
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * V60.1 — «ثبت‌نام با گوگل» با Credential Manager مستقیم (مسیر رسمی مستندات
 * Supabase): پنجرهٔ انتخاب جیمیل‌های گوشی → idToken → ورود IDToken به
 * Supabase → ثبت نقش → نشستن user در state (ورود خودکار به برنامه).
 * V60.0 با پلاگین compose-auth بود که روی برخی دستگاه‌ها پس از انتخاب جیمیل
 * callback را گم می‌کرد («اتفاقی نمی‌افتد»).
 */
@Composable
private fun GoogleRegisterButton(state: AuthUiState, viewModel: AuthViewModel, role: String) {
    if (ir.exam.app.BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
        Text(
            "ثبت‌نام با گوگل: کلید GOOGLE_WEB_CLIENT_ID در local.properties تنظیم نشده است.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            scope.launch {
                try {
                    val rawNonce = java.util.UUID.randomUUID().toString()
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(rawNonce.toByteArray())
                    val hashedNonce = digest.joinToString("") { "%02x".format(it) }
                    val googleIdOption =
                        com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(ir.exam.app.BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setNonce(hashedNonce)
                            .build()
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    val result = credentialManager.getCredential(request = request, context = context)
                    val googleCredential =
                        com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                            .createFrom(result.credential.data)
                    viewModel.signInWithGoogleIdToken(googleCredential.idToken, rawNonce, role)
                } catch (cancel: androidx.credentials.exceptions.GetCredentialCancellationException) {
                    // بستن پنجره توسط کاربر خطا نیست.
                } catch (error: Throwable) {
                    viewModel.reportGoogleError(error.message ?: "ورود گوگل ناموفق بود.")
                }
            }
        },
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        // V60.2 — لوگوی رسمی چهاررنگ گوگل؛ tint خنثی تا رنگ‌ها حفظ شوند.
        Icon(
            imageVector = GoogleLogo,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("ثبت‌نام با گوگل")
    }
}

