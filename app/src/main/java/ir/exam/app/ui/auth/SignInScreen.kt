package ir.exam.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.common.PasswordVisibilityButton
import ir.exam.app.ui.common.passwordTransformation

/** ورود، ثبت‌نام معلم و بازیابی رمز؛ هیچ مسیر آزمایشی یا عبور مستقیم ندارد. */
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
    Text("ورود معلم یا دانش‌آموز")
    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::setEmail,
        label = { Text("ایمیل معلم یا نام کاربری دانش‌آموز") },
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
    ) { Text("ورود معلم با کد ایمیل") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = viewModel::showRecovery, enabled = !state.isLoading) { Text("رمز را فراموش کرده‌ام") }
        TextButton(onClick = viewModel::showTeacherRegistration, enabled = !state.isLoading) { Text("ثبت‌نام معلم") }
    }
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
private fun RecoveryPane(state: AuthUiState, viewModel: AuthViewModel) {
    Text("بازیابی رمز معلم", style = MaterialTheme.typography.titleLarge)
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
