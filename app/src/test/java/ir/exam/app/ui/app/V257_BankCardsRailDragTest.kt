package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V257 — ریل دسکتاپ پهن‌تر + دستگیرهٔ جابه‌جایی؛ بانک سؤال کارتی (اپ + سایت). */
class V257_BankCardsRailDragTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `desktop rail is wider and has hold-to-drag grips`() {
        val css = source("site/src/site.css")
        assertTrue("position:fixed;left:14px;top:22px;bottom:22px;width:84px;" in css)
        assertTrue(".dk .builder{position:fixed;top:22px;bottom:22px;left:124px;right:100px;" in css)
        assertTrue(".dk .b-rail-grip{" in css && ".dk .b-rail-row.dragging{" in css)
        val b = source("site/src/builder.js")
        assertTrue("function railDrag(grip, row, nums, index) {" in b)
        assertTrue("var timer = setTimeout(function () { timer = null; start(ev); }, 220);" in b)
        assertTrue("if (cur !== from) { var q = state.questions.splice(from, 1)[0]; state.questions.splice(cur, 0, q); state.selected = cur; mark(); }" in b)
    }

    @Test
    fun `bank shows question cards on site and app`() {
        val s = source("site/src/school.js")
        assertTrue("var lst = el('div', {class: 'bank-grid'});" in s)
        assertTrue("lst.appendChild(el('div', {class: 'bank-card'}, [" in s)
        assertTrue("el('span', {class: 'chip type', text: TYPE[S.qType(qq.type)] || 'سؤال'})" in s)
        val css = source("site/src/site.css")
        assertTrue(".bank-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px}" in css)
        val k = source("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("private fun BankQuestionCard(" in k)
        assertTrue("BankChip(q.type.faLabel(), accent = true)" in k)
        assertTrue("if (q.type == QuestionType.MULTIPLE_CHOICE && q.options.isNotEmpty()) {" in k)
    }
}
