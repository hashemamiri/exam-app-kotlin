package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V88.8 — سرصفحهٔ کارت روی گوشی عمودی می‌شد، و لمسِ کارت به‌جای بازکردنِ
 * خودِ کارت یک پنجرهٔ بومی می‌آورد.
 */
class V88_8CardRowAndTapTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the header stays one horizontal row on a phone`() {
        // `@media (max-width:420px)` سرصفحه را column می‌کرد
        assertTrue("#questionsContainer .q-header{" in asset)
        assertTrue("flex-direction:row !important;" in asset)
        assertTrue("flex-wrap:nowrap !important;" in asset)
    }

    @Test
    fun `score and delete stop stretching to full width`() {
        assertTrue("#questionsContainer .q-score{width:74px !important;flex:0 0 auto}" in asset)
        assertTrue("#questionsContainer .q-remove{" in asset)
        assertTrue("width:auto !important;" in asset)
    }

    // V100 — شنوندهٔ «لمسِ کارت» (و رفتارهایش: بازکردنِ کارتِ جمع، عدمِ
    // بستنِ دوباره، و مستثناکردنِ ورودی‌ها) با حذفِ کاملِ «آزمون‌ساز چاپی»
    // از صفحه رفت؛ کارت‌های HTML در حالتِ بومی پنهان‌اند.

    @Test
    fun `the question bridge is still reachable`() {
        // V90 — پلِ `openQuestion` حذف شد (ویرایش در خودِ کارت است)، ولی
        // پلِ خواندن/نوشتنِ سؤال دست‌نخورده می‌ماند.
        assertTrue("window.__qmfQuestionDetail = function" in asset)
        assertTrue("window.__qmfQuestionEdit = function" in asset)
    }
}
