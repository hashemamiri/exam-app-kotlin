package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V63.5 — دو درخواست کاربر:
 * ۱) ویرایش «ویرایشگر سند» فقط برای چاپ باشد: چیدمان در PrintLayoutStore
 *    محلی به‌ازای exam_id ذخیره می‌شود، مسیر چاپ (preparePrint →
 *    printableExam) همان را می‌خواند و آزمون سرور/دانش‌آموز دست‌نخورده
 *    می‌ماند (builder.save سروری از ویرایشگر حذف شد).
 * ۲) دکمهٔ برگشت گوشی در ویرایشگر سند از برنامه خارج نشود و به «چاپ
 *    آزمون» برگردد (BackHandler چون صفحه بیرون از Scaffold است).
 */
class V63_5PrintOnlyLayoutBackTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val store by lazy { source("app/src/main/java/ir/exam/app/data/local/PrintLayoutStore.kt") }
    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val portability by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    @Test
    fun `document editor saves a local print layout instead of the server exam`() {
        assertTrue("class PrintLayoutStore(" in store)
        assertTrue("print_layout_overrides" in store)
        assertTrue("layoutStore.write(" in editor)
        assertTrue("canonicalQuestions ?: state.questions" in editor)
        assertTrue("layoutStore.readForLatest(examId, latest)?.let(builder::overridePrintLayout)" in editor)
        assertTrue("چیدمان چاپ ذخیره شد؛ فقط در چاپ همین آزمون اعمال می‌شود." in editor)
        // ذخیرهٔ سروری و دیالوگ هزینه از ویرایشگر حذف شدند
        assertFalse("builder.save()" in editor)
        assertFalse("confirmSave" in editor)
        assertFalse("maximumChargeToman" in editor)
    }

    @Test
    fun `print path reads the local layout override`() {
        assertTrue("questionsOverride: List<ir.exam.app.ui.builder.QuestionDraft>? = null" in portability)
        assertTrue("questionsOverride ?: ExamQuestionCodec.decode(exam.questions, key)" in portability)
        assertTrue("viewModel.preparePrint(exam.id, false, header, layoutStore.read(exam.id))" in printCenter)
        // preparePrint with key removed from card
        // ویومدل ویرایشگر جایگزینی درجا دارد (بدون تماس سرور)
        assertTrue("fun overridePrintLayout(questions: List<QuestionDraft>)" in
            source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt"))
    }

    @Test
    fun `hardware back leaves the editor not the app`() {
        assertTrue("BackHandler(onBack = onBack)" in editor)
        // مسیر برگشت ویرایشگر همچنان به صفحهٔ چاپ برمی‌گردد
        assertTrue("editingDocumentExamId = null" in appShell)
        assertTrue("page = MainPage.PRINT" in appShell)
        assertTrue("examId = editingDocumentExamId!!" in appShell)
    }
}
