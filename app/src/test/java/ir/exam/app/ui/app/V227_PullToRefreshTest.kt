package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V227 — «کشیدن برای بازخوانی»: نشانگر از پرچم جداگانهٔ refreshing می‌خواند (نه loading که با حافظهٔ موقت V212–V214
 * false می‌ماند و نه actionLoading)؛ هر ViewModel یک refresh() با حداقل نمایش ۵۰۰ms و بدون اجرای هم‌زمان دارد.
 */
class V227_PullToRefreshTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    private val screens = listOf(
        "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt",
        "app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt",
        "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt",
        "app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt",
        "app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt"
    )
    private val viewModels = listOf(
        "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt",
        "app/src/main/java/ir/exam/app/ui/billing/BillingViewModel.kt",
        "app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt",
        "app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt",
        "app/src/main/java/ir/exam/app/ui/calendar/CalendarViewModel.kt"
    )

    @Test
    fun screensUseDedicatedRefreshingFlag() {
        screens.forEach { rel ->
            val s = src(rel)
            assertTrue(rel, "isRefreshing = state.refreshing, // V227" in s)
            assertTrue(rel, "onRefresh = viewModel::refresh," in s)
            assertFalse(rel, "isRefreshing = state.loading" in s)
            assertFalse(rel, "isRefreshing = state.loading || state.actionLoading" in s)
        }
    }

    @Test
    fun viewModelsExposeSingleFlightRefresh() {
        viewModels.forEach { rel ->
            val s = src(rel)
            assertTrue(rel, "val refreshing: Boolean = false, // V227" in s)
            assertTrue(rel, "private var refreshJob: Job? = null" in s)
            assertTrue(rel, "if (refreshJob?.isActive == true) return" in s)
            assertTrue(rel, "_state.update { it.copy(refreshing = true) }" in s)
            assertTrue(rel, "val rest = 500L - (System.currentTimeMillis() - started)" in s)
            assertTrue(rel, "_state.update { it.copy(refreshing = false) }" in s)
        }
        assertTrue("private fun loadMonth(): Job {" in src("app/src/main/java/ir/exam/app/ui/calendar/CalendarViewModel.kt"))
    }
}
