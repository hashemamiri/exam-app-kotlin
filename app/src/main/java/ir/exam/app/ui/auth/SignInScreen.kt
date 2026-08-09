package ir.exam.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** رابط اولیهٔ ورود؛ منطق Supabase Auth در AuthViewModel فاز ورود افزوده می‌شود. */
@Composable fun SignInScreen(onTeacherDemo: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    Scaffold { padding -> Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("سامانه آزمون", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("ایمیل") }, singleLine = true)
        Button(onClick = onTeacherDemo) { Text("ورود آزمایشی معلم") }
    } }
}
