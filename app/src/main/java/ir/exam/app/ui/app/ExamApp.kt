package ir.exam.app.ui.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ir.exam.app.data.repository.SupabaseAuthRepository
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.auth.AuthViewModel
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.dashboard.TeacherDashboardScreen

/** دروازهٔ اصلی برنامه: تا ورود واقعی انجام نشده، فقط صفحه Auth دیده می‌شود. */
@Composable
fun ExamApp() {
    val viewModel = remember { AuthViewModel(SupabaseAuthRepository()) }
    val state by viewModel.state.collectAsState()
    val user = state.user

    when (user?.role) {
        UserRole.TEACHER -> TeacherDashboardScreen()
        UserRole.STUDENT -> Text("داشبورد دانش‌آموز — مرحلهٔ بعدی")
        null -> SignInScreen(viewModel = viewModel)
    }
}
