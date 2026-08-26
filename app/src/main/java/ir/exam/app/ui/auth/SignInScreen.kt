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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation

/** ورود، ثبت‌نام کادر مدرسه و بازیابی رمز؛ هیچ مسیر آزمایشی یا عبور مستقیم ندارد. */
@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    // V62.0 — پوستهٔ «یخی قطبی» (طرح پیشنهادی کاربر): پس‌زمینهٔ گرادیان با هاله
    // و موج، کارت شیشه‌ای فرم، برف در جریان بازیابی رمز. منطق دست‌نخورده است.
    val recoveryFlow = state.screen in setOf(
        AuthScreen.RECOVERY, AuthScreen.RECOVERY_OTP, AuthScreen.RECOVERY_PASSWORD
    )
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        IceBackdrop(Modifier.fillMaxSize())
        if (recoveryFlow) Snowfall(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // V61.0 — عنوان همیشه وسط‌چین بالای صفحه.
        Text(
            "آزمون آنلاین",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = IceInk,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        // V62.0 — نوار مراحل سه‌گانهٔ بازیابی رمز (ایمیل ← کد ← رمز جدید).
        if (recoveryFlow) {
            StepIndicator(
                steps = listOf("ایمیل", "کد", "رمز جدید"),
                current = when (state.screen) {
                    AuthScreen.RECOVERY -> 0
                    AuthScreen.RECOVERY_OTP -> 1
                    else -> 2
                }
            )
        }
        IceAuthCard {
        // V62.0 — ورود پلکانی فرم با هر تغییر صفحه.
        StaggeredEntrance(key = state.screen) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.screen) {
            AuthScreen.SIGN_IN -> LandingPane(state, viewModel)
            AuthScreen.LOGIN_ROLE -> LoginRolePane(state, viewModel)
            AuthScreen.LOGIN_MANAGER -> StaffLoginPane(state, viewModel, managerRole = true)
            AuthScreen.LOGIN_TEACHER -> StaffLoginPane(state, viewModel, managerRole = false)
            AuthScreen.LOGIN_STUDENT -> StudentLoginPane(state, viewModel)
            AuthScreen.REGISTRATION_ROLE -> RegistrationRolePane(state, viewModel)
            AuthScreen.LOGIN_OTP -> OtpPane(
                title = "ورود با کد یک‌بارمصرف",
                hint = "کد ارسال‌شده به ${state.email} را وارد کنید.",
                state = state,
                onVerify = viewModel::verifyLoginOtp,
                onResend = viewModel::sendLoginOtp,
                onBack = viewModel::showLoginRole,
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
        }
        }
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
}

/**
 * V61.0 — صفحهٔ آغازین: فقط دو دکمهٔ بزرگ «ورود» بالا و «ثبت‌نام» پایین آن.
 */
@Composable
private fun LandingPane(state: AuthUiState, viewModel: AuthViewModel) {
    Spacer(Modifier.height(48.dp))
    Button(
        onClick = viewModel::showLoginRole,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("ورود") }
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = viewModel::showRegistrationRole,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("ثبت‌نام") }
}

/**
 * V61.0 — انتخاب نقش ورود: مدیر/معاون بالا، معلم وسط، دانش‌آموز پایین؛
 * هر دکمه پنجرهٔ اختصاصی ورود همان نقش را باز می‌کند.
 */
@Composable
private fun LoginRolePane(state: AuthUiState, viewModel: AuthViewModel) {
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = viewModel::showManagerLogin,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("مدیر/معاون") }
    Button(
        onClick = viewModel::showTeacherLogin,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("معلم") }
    Button(
        onClick = viewModel::showStudentLogin,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("دانش‌آموز") }
    BackButtonRow(onBack = viewModel::showSignIn, enabled = !state.isLoading)
}

/**
 * V61.0 — پنجرهٔ اختصاصی ورود معلم یا مدیر/معاون + «ورود با گوگل» برای
 * حساب‌هایی که قبلاً با جیمیل ثبت‌نام کرده‌اند (جیمیل جدید به تکمیل ثبت‌نام می‌رود).
 */
@Composable
private fun StaffLoginPane(state: AuthUiState, viewModel: AuthViewModel, managerRole: Boolean) {
    Text(if (managerRole) "ورود مدیر/معاون" else "ورود معلم", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::setEmail,
        label = { Text("ایمیل یا نام کاربری") },
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
    ) { Text("ورود با کد ایمیل") }
    // V61.0 — ورود با گوگل؛ همان جریان Credential Manager ثبت‌نام (idToken).
    GoogleAuthButton(
        state = state,
        viewModel = viewModel,
        role = if (managerRole) "manager" else "teacher"
    ) { Text("ورود با گوگل") }
    TextButton(onClick = viewModel::showRecovery, enabled = !state.isLoading) { Text("رمز را فراموش کرده‌ام") }
    BackButtonRow(onBack = viewModel::showLoginRole, enabled = !state.isLoading)
}

/** V61.0 — پنجرهٔ اختصاصی ورود دانش‌آموز با نام کاربری تحویلی معلم. */
@Composable
private fun StudentLoginPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("ورود دانش‌آموز", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::setEmail,
        label = { Text("نام کاربری دانش‌آموز") },
        supportingText = { Text("همان نام کاربری تحویلی از معلم را وارد کنید.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    PasswordField("رمز عبور", state.password, viewModel::setPassword)
    Button(
        onClick = viewModel::signIn,
        enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("ورود") }
    BackButtonRow(onBack = viewModel::showLoginRole, enabled = !state.isLoading)
}

/** V61.0 — دکمهٔ بازگشت وسط‌چین پایین همهٔ پنجره‌های ورود/ثبت‌نام. */
@Composable
private fun BackButtonRow(onBack: () -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onBack, enabled = enabled) { Text("بازگشت") }
    }
}

@Composable
private fun RegistrationRolePane(state: AuthUiState, viewModel: AuthViewModel) {
    // V61.0 — نقش‌ها عمودی: مدیر/معاون بالا و معلم پایین آن.
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = viewModel::showManagerRegistration,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("مدیر/معاون") }
    Button(
        onClick = viewModel::showTeacherRegistration,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) { Text("معلم") }
    BackButtonRow(onBack = viewModel::showSignIn, enabled = !state.isLoading)
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
    BackButtonRow(onBack = viewModel::showRegistrationRole, enabled = !state.isLoading)
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
    BackButtonRow(onBack = viewModel::showRegistrationRole, enabled = !state.isLoading)
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
    BackButtonRow(onBack = viewModel::showLoginRole, enabled = !state.isLoading)
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
    // V62.0 — باکس‌های کد با Paste و Backspace طبیعی (فیلد مخفی)؛ کد سوپابیس
    // ۶ تا ۸ رقمی است و باکس‌ها با طول کد گسترده می‌شوند.
    Text("کد یک‌بارمصرف ۶ تا ۸ رقم", style = MaterialTheme.typography.labelMedium)
    OtpBoxes(
        value = state.otp,
        onValueChange = viewModel::setOtp,
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
    BackButtonRow(onBack = onBack, enabled = !state.isLoading)
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
    GoogleAuthButton(state = state, viewModel = viewModel, role = role) {
        Text("ثبت‌نام با گوگل")
    }
}

/**
 * V61.0 — دکمهٔ مشترک گوگل برای ثبت‌نام و ورود؛ متن دکمه از بیرون می‌آید و
 * جریان Credential Manager یکی است (حساب موجود مستقیم وارد می‌شود و حساب
 * تازه به صفحهٔ تکمیل ثبت‌نام همان نقش می‌رود).
 */
@Composable
private fun GoogleAuthButton(
    state: AuthUiState,
    viewModel: AuthViewModel,
    role: String,
    label: @Composable () -> Unit
) {
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
        label()
    }
}

