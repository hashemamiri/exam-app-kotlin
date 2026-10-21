package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V310.1 — املا (گوش بده و بنویس) در آزمون چاپی دیده شود؛ کنترل‌های حالت ابتدایی
 * در چاپ هم مثل سایت نمایش داده شوند (سایت در هر دو حالت آنلاین و چاپی نشان می‌دهد).
 */
class V310_1_PrintPrimaryParityTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private fun screen(): String = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()

    @Test
    fun `primary mode chooser is not hidden in print mode`() {
        val s = screen()
        assertTrue(s.contains("if (primary && (question.type == QuestionType.FILL_BLANK || question.type == QuestionType.MULTIPLE_CHOICE)) {"))
        assertTrue("secondary response modes must remain visible in print mode", s.contains("SecondaryQuestionControls(question, viewModel)"))
        assertTrue("essay response modes must be offered", s.contains("if (question.type == QuestionType.ESSAY)"))
        assertTrue(!s.contains("if (primary && !printMode && (question.type"))
    }

    @Test
    fun `dictation answer field is shown in print mode`() {
        val s = screen()
        assertTrue(s.contains("else if (!printMode || (primary && question.mode == \"dictation\"))"))
    }

    @Test
    fun `hotspot and order editors are not hidden in print mode`() {
        val s = screen()
        assertTrue(s.contains("if (primary && question.mode == \"hotspot\") PrimaryHotspotEditor"))
        assertTrue(s.contains("if (primary && question.mode == \"order\") PrimaryOrderEditor"))
    }

    @Test
    fun `site shows primary controls for both modes`() {
        val builder = File(root(), "site/src/builder.js").readText()
        assertTrue(builder.contains("if (state.primaryMode && q.type === 'fill' && q.mode === 'dictation')"))
        assertTrue(builder.contains("['dictation', 'املا (گوش بده و بنویس)']"))
    }
}
