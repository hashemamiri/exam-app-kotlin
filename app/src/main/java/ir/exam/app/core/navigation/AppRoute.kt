package ir.exam.app.core.navigation

/** همهٔ مسیرهای اصلی؛ هر feature Native مسیر مستقل دارد. */
sealed class AppRoute(val value: String) {
    data object Splash : AppRoute("splash")
    data object SignIn : AppRoute("sign_in")
    data object Otp : AppRoute("otp")
    data object TeacherDashboard : AppRoute("teacher_dashboard")
    data object StudentDashboard : AppRoute("student_dashboard")
    data object SchoolManagement : AppRoute("school_management")
    data object Grading : AppRoute("grading")
    data object Reports : AppRoute("reports")
    data object StudentResults : AppRoute("student_results")
    data object About : AppRoute("about")
    data object ExamBuilder : AppRoute("exam_builder/{examId}")
    data object StudentExam : AppRoute("student_exam/{examId}")
}
