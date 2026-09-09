package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V105 — پیش‌نمایش/چاپ: هفت قالب سربرگ اصلی، جدول «ردیف/متن سؤال/بارم» و
 * حرکت آزاد شکل‌ها روی کل برگه بدون اسکرول صفحه هنگام درگ.
 */
class V105_PrintPreviewParityTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val renderer by lazy { File(root(), "app/src/main/assets/print/exam_print_renderer_legacy.html").readText() }
    private val headerSettings by lazy { File(root(), "app/src/main/java/ir/exam/app/ui/printing/PrintHeaderSettings.kt").readText() }

    @Test
    fun `all seven reference header templates are rendered with their original structure`() {
        listOf(
            "function buildClassicHeader()", "function buildFormalHeader()", "function buildSamaHeader()",
            "function buildSchoolHeader()", "function buildEduHeader()", "function buildDetailedSchoolHeader()",
            "function buildMinistryHeader()"
        ).forEach { assertTrue("سازندهٔ سربرگ نیست: $it", it in renderer) }
        listOf(
            "class=\"exam-header\"", "class=\"scores-table\"", "class=\"exam-header2\"", "class=\"exam-header3\"",
            "class=\"exam-header4\"", "class=\"exam-header5\"", "class=\"exam-header6\"", "class=\"exam-header7\"",
            ".h2-info-table", ".h3-info", ".h4-table", ".h5-bottom", ".h6-correct", ".h7-wrap", ".iau-logo"
        ).forEach { assertTrue("ساختار/CSS سربرگ مرجع نیست: $it", it in renderer) }
        assertTrue("قالب از f_headerTemplate خوانده نمی‌شود", "state.fields.f_headerTemplate" in renderer)
        // سربرگِ سادهٔ عمومیِ d5fc2ca (سه‌ستونیِ فیلدها) نباید برگردد
        assertFalse("سربرگ عمومیِ قدیمی برگشته است", "header-fields" in renderer)
        assertFalse("TEMPLATE_META قدیمی برگشته است", "TEMPLATE_META" in renderer)
    }

    @Test
    fun `header markup is built only from escaped values`() {
        assertTrue("v() مقدارها را escape نمی‌کند", "function v(id) { var x = text(state.fields[id]); return x.trim() ? esc(x) : ''; }" in renderer)
        assertTrue("markup با DOMParser وارد سند می‌شود", "new DOMParser().parseFromString(" in renderer)
        assertFalse("innerHTML در رندرر ممنوع است", "innerHTML" in renderer)
    }

    @Test
    fun `question table keeps row number, text and score columns`() {
        listOf("table.className = 'questions-print-table'", "'ردیف'", "'متن سؤال'", "'بارم'", "qno-head", "qscore-head",
            "question-no-td", "question-main-td", "question-score-td", "function toPersianNum(n)")
            .forEach { assertTrue("ستون جدول سؤال‌ها نیست: $it", it in renderer) }
        assertTrue("کلید آزمون در نسخهٔ استاد نیست", "function answerKeyTable()" in renderer)
        assertTrue("پاورقی مرجع نیست", "function footerText()" in renderer)
    }

    @Test
    fun `figures move freely across the whole sheet and the page does not scroll while dragging`() {
        assertTrue("شکل شناور فرزند برگه نمی‌شود", "if (figure.parentNode !== sheet) sheet.appendChild(figure);" in renderer)
        assertTrue("مبدأ مختصات هنگام شناور شدن حفظ نمی‌شود", "function sheetOffsetOf(element)" in renderer)
        assertTrue("حد حرکت به کل برگه نیست", "sheet.scrollHeight - height" in renderer)
        assertFalse("حرکت به سلول سؤال محدود مانده", "closest('.question-content')" in renderer)
        assertTrue("touch-action:none روی شکل نیست", ".print-figure{" in renderer && "touch-action:none" in renderer)
        assertTrue("body.dragging نیست", "body.dragging{touch-action:none;overflow:hidden" in renderer)
        assertTrue("touchmove هنگام درگ لغو نمی‌شود", "document.addEventListener('touchmove', function (event) { if (activeDrag && event.cancelable) event.preventDefault(); }, {passive:false});" in renderer)
        assertTrue("pointermove غیرpassive نیست", "document.addEventListener('pointermove', moveDrag, {passive:false});" in renderer)
        assertTrue("slot جای شکل را در جریان متن نگه نمی‌دارد", "slot.classList.add('free-slot')" in renderer)
        assertTrue("دوبار لمس برای ویرایش نیست", "now - lastTap < 320" in renderer)
    }

    @Test
    fun `header settings dialog preselects the saved template`() {
        assertTrue("قالب ذخیره‌شده پیش‌انتخاب نمی‌شود", "currentValues[\"f_headerTemplate\"]" in headerSettings)
    }
}
