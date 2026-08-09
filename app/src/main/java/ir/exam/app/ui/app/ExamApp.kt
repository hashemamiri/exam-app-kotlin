package ir.exam.app.ui.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.exam.app.core.navigation.AppRoute
import ir.exam.app.ui.auth.SignInScreen
import ir.exam.app.ui.dashboard.TeacherDashboardScreen

/** گرهٔ اتصال Navigation به featureها؛ featureهای بعدی بدون تغییر Activity افزوده می‌شوند. */
@Composable fun ExamApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = AppRoute.SignIn.value) {
        composable(AppRoute.SignIn.value) { SignInScreen(onTeacherDemo = { nav.navigate(AppRoute.TeacherDashboard.value) }) }
        composable(AppRoute.TeacherDashboard.value) { TeacherDashboardScreen() }
        composable(AppRoute.Otp.value) { Text("ورود با کد یک‌بارمصرف") }
        composable(AppRoute.StudentDashboard.value) { Text("داشبورد دانش‌آموز") }
    }
}
