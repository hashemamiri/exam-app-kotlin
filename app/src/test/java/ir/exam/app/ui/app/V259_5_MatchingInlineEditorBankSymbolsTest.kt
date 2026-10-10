package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V259.5 — اپ: موارد جورکردنی مثل گزینه‌ها (InlineMathTextEditor + همهٔ ابزارها)؛ سایت: فهرست بانک با نماد، منوی گالری همیشه در دید، پیش‌نمایش/صفحهٔ دانش‌آموز جورکردنی با ستون راست حرفی. */
class V259_5_MatchingInlineEditorBankSymbolsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `app matching items use inline math editor with figure editing`() {
        val m = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        assertFalse("OutlinedTextField(" in m)
        assertTrue("onFigureEdit: (side: String, index: Int, occurrence: Int, spec: FigureSpec) -> Unit" in m)
        assertTrue("onDeleteFigure = { occurrence -> viewModel.deleteFieldFigure(question.id, \"matching_right\", index, occurrence) }" in m)
        assertTrue("onDeleteFigure = { occurrence -> viewModel.deleteFieldFigure(question.id, \"matching_left\", index, occurrence) }" in m)
        val b = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("openFieldFigureEditor(InsertMenuRef(\"matching_\$side\", itemIndex, label), occurrence, spec)" in b)
    }

    @Test
    fun `site bank list renders symbols and atlas menu stays in view`() {
        val js = source("site/src/builder.js")
        // V260 — فهرست بانک به کارت‌های فشرده با چشم تبدیل شد (محتوا در پنجرهٔ bankQuestionModal)
        assertTrue("window.SiteSchool.bankQuestionModal(it)" in js)
        assertTrue("var mh = m.offsetHeight, mw = m.offsetWidth, vh = window.innerHeight, vw = document.documentElement.clientWidth;" in js)
        val css = source("site/src/site.css")
        assertTrue(".b-atlas-menu{max-height:calc(100vh - 16px);overflow-y:auto;scrollbar-width:none}" in css)
    }

    @Test
    fun `matching preview and student page label right column with letters`() {
        val js = source("site/src/builder.js")
        assertTrue("card.appendChild(el('div', {class: 'st-match-right'}, (q.matchingRight || []).map(function (r, ri)" in js)
        val st = source("site/src/student.js")
        assertTrue("var ABm = ['الف', 'ب', 'ج', 'د', 'ه', 'و', 'ز', 'ح', 'ط', 'ی'];" in st)
        assertTrue("text: ABm[di] || fa(di + 1), onclick:" in st)
        val ad = source("site/src/admin.js")
        assertTrue("function ABL(i)" in ad && "' ← ' + ABL(Number(r[l]))" in ad)
    }
}
