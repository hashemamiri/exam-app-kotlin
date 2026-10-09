package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V252 — بازبینی کد به کد اپ: فایل‌های SAF امن و روی نخ IO، پیش‌نویس مخصوص حساب، لغو تایمر نمونهٔ قدیمی، چرخش بدون بازسازی. */
class V252_AppAuditTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `no raw SAF stream in screens - all go through DocumentIo`() {
        val screens = listOf(
            "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt",
            "app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt",
            "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt",
            "app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt",
            "app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt",
            "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt"
        )
        screens.forEach { p ->
            val s = source(p)
            assertFalse(p, "openOutputStream(" in s)
            assertFalse(p, "openInputStream(" in s)
            assertTrue(p, "DocumentIo." in s)
        }
        val io = source("app/src/main/java/ir/exam/app/core/io/DocumentIo.kt")
        assertTrue("withContext(Dispatchers.IO)" in io && "runCatching" in io)
    }

    @Test
    fun `answer drafts are scoped to the signed-in student`() {
        val repo = source("app/src/main/java/ir/exam/app/data/repository/RoomAnswerDraftRepository.kt")
        assertTrue("if (ownerUserId.isBlank()) examId else \"\$ownerUserId|\$examId\"" in repo)
        assertTrue("RoomAnswerDraftRepository(database.answerDraftDao(), userId)" in source("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt"))
        val worker = source("app/src/main/java/ir/exam/app/data/work/PendingActionWorker.kt")
        assertTrue("clearDraft(action.ownerUserId, payload.examId)" in worker)
        assertFalse("database.answerDraftDao().delete(payload.examId)" in worker)
    }

    @Test
    fun `student exam view model releases timers and ignores draft echo`() {
        val vm = source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        assertTrue("fun release() {" in vm && "viewModelScope.cancel()" in vm)
        assertTrue("if (echo || draftSaveJob?.isActive == true) return@collect" in vm)
        assertTrue("runCatching { drafts.save(examId, draft) }" in vm)
        assertTrue("DisposableEffect(viewModel) { onDispose { viewModel.release() } }" in source("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt"))
        assertTrue("runCatching { draftStore.save(ownerUserId, current) }" in source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt"))
    }

    @Test
    fun `activity survives configuration changes`() {
        val m = source("app/src/main/AndroidManifest.xml")
        assertTrue("android:configChanges=\"orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|uiMode|density|fontScale\"" in m)
    }
}
