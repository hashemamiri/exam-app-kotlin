package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V272 — «۵۰ سؤال بساز، ۲۰ تا تصادفی» با بخش‌بندی بر اساس نوع سؤال (اپ + SQL). */
class V272_PickRulesTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `pick rules travel through dto codec viewmodel and screen`() {
        assertTrue(File(root(), "supabase/migrations/20261013_native_pick_rules_v272.sql").isFile)
        assertTrue("@SerialName(\"pick_rules\") val pickRules: Map<String, Int> = emptyMap()" in src("app/src/main/java/ir/exam/app/data/dto/ExamDetailDto.kt"))
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamPackageCodec.kt")
        assertTrue("put(\"pick_rules\", buildJsonObject { source.pickRules.forEach { (k, v) -> if (v > 0) put(k, v) } }) // V272" in codec)
        val vm = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("fun setPickRule(type: QuestionType, value: String) {" in vm)
        val scr = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("Text(\"انتخاب تصادفی بر اساس نوع سؤال (خالی = همهٔ آن نوع)\", style = MaterialTheme.typography.labelLarge)" in scr)
        assertTrue("{ viewModel.setPickRule(type, it) }" in scr)
    }
}
