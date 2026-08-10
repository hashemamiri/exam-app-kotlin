package ir.exam.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** رابط ورود واقعی. هیچ login آزمایشی یا عبور مستقیم به داشبورد ندارد. */
@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("سامانه آزمون", style = MaterialTheme.typography.headlineMedium)
        Text(if (state.otpSent) "کد ارسال‌شده به ایمیل را وارد کنید" else "ورود معلم یا دانش‌آموز")

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            label = { Text("ایمیل") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.otpSent) {
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::setOtp,
                label = { Text("کد یک‌بارمصرف (۶ تا ۸ رقم)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = viewModel::verifyOtp,
                enabled = !state.isLoading && state.otp.length in 6..8,
                modifier = Modifier.fillMaxWidth()
            ) { Text("تأیید و ورود") }
            Button(
                onClick = viewModel::sendOtp,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("ارسال دوبارهٔ کد") }
        } else {
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text("رمز عبور") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = viewModel::signIn,
                enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("ورود با رمز عبور") }
            Button(
                onClick = viewModel::sendOtp,
                enabled = !state.isLoading && state.email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("ورود با کد یک‌بارمصرف (۶ تا ۸ رقم)") }
        }

        if (state.isLoading) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        state.error?.let { Text("خطا: $it", color = MaterialTheme.colorScheme.error) }
    }
}
