package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V276 — گردش کار دستی تست UI اپ روی شبیه‌ساز + پین‌های کد که تست UI به آن‌ها تکیه دارد. */
class V276_AppUiTestWorkflowTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `workflow, managed device and ui test exist`() {
        val wf = src(".github/workflows/app-ui-test.yml")
        assertTrue("workflow_dispatch:" in wf && ":app:uiPixel6Api34DebugAndroidTest" in wf && "UI_TEST_TEACHER_PASS" in wf)
        assertTrue("add-mask" in wf)
        val gradle = src("app/build.gradle.kts")
        assertTrue("\"uiPixel6Api34\"" in gradle && "androidx.compose.ui:ui-test-junit4" in gradle && "testInstrumentationRunner = \"androidx.test.runner.AndroidJUnitRunner\"" in gradle)
        val ui = src("app/src/androidTest/java/ir/exam/app/uitest/TeacherExamFlowUiTest.kt")
        assertTrue("getString(\"teacherUser\")" in ui && "آزمون فعلاً بسته است" in ui && "assertEquals(" in ui)
        assertTrue("11111111" !in ui && "teach\"" !in ui) // هیچ اعتبارنامه‌ای در کد
    }

    @Test
    fun `ui hooks the test relies on`() {
        assertTrue(".testTag(\"builder-question-text\")" in src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt"))
        val scr = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("LaunchedEffect(state.saving) { if (state.saving) dismissedSavedCode = null }" in scr)
        assertTrue("contentDescription = \"ذخیره آزمون\"" in scr && "Text(\"تأیید و ذخیره\")" in scr && "Text(\"ادامهٔ ویرایش\")" in scr)
        assertTrue("contentDescription = \"بازکردن افزودن سریع\"" in src("app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt"))
        assertTrue("BuilderRadialAction(\"صحیح/غلط\"" in src("app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt"))
    }
}
