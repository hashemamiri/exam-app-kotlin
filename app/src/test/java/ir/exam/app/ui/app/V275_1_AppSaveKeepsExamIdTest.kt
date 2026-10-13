package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V275.1 — اپ: پس از ذخیرهٔ آزمون تازه، شناسه/کد در وضعیت می‌ماند و یادآوری «آزمون بسته است» نشان داده می‌شود. */
class V275_1_AppSaveKeepsExamIdTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `save result carries exam id and screen shows closed hint`() {
        assertTrue("val examId: String? = null," in src("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt"))
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("examId = examId, // V275.1" in repo && "wasNew = state.examId == null" in repo)
        val vm = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("examId = result.examId ?: it.examId," in vm && "code = it.code ?: result.code," in vm)
        val scr = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("if (state.lastSaveResult?.wasNew == true) {" in scr)
        assertTrue("آزمون فعلاً بسته است؛ در فهرست آزمون‌ها قفل آن را باز کنید" in scr)
    }
}
