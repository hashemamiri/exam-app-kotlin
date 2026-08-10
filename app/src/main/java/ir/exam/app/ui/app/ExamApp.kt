package ir.exam.app.ui.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.exam.app.data.repository.SupabaseAuthRepository
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.auth.AuthViewModel
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.builder.ExamBuilderScreen
import ir.exam.app.ui.builder.ExamBuilderViewModel
import ir.exam.app.ui.dashboard.TeacherDashboardScreen
import ir.exam.app.ui.student.StudentHomeScreen

private enum class TeacherPage { DASHBOARD, BUILDER }

@Composable
fun ExamApp() {
    val authViewModel = remember { AuthViewModel(SupabaseAuthRepository()) }
    val authState by authViewModel.state.collectAsState()
    var page by remember { mutableStateOf(TeacherPage.DASHBOARD) }

    when (authState.user?.role) {
        UserRole.TEACHER -> when (page) {
            TeacherPage.DASHBOARD -> TeacherDashboardScreen(onCreateExam = { page = TeacherPage.BUILDER })
            TeacherPage.BUILDER -> ExamBuilderScreen(viewModel = remember { ExamBuilderViewModel() })
        }
        UserRole.STUDENT -> StudentHomeScreen()
        null -> SignInScreen(viewModel = authViewModel)
    }
}
