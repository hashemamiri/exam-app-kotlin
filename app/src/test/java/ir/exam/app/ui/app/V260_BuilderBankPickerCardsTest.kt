package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260 — پنجرهٔ بانک سؤال در سازنده (اپ و سایت): تمام‌صفحه/پهن، کارت‌های فشرده با تیک انتخاب چندتایی، چشم و افزودن تکی. */
class V260_BuilderBankPickerCardsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `app builder bank dialog is full screen with multi select cards`() {
        val b = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("onAdd: (List<Long>) -> Unit" in b)
        assertTrue("properties = DialogProperties(usePlatformDefaultWidth = false)" in b)
        assertTrue("private fun BuilderBankPickCard(" in b)
        assertTrue("Checkbox(checked = checked, onCheckedChange = onCheckedChange)" in b)
        assertTrue("ir.exam.app.ui.bank.BankQuestionContent(item.question)" in b)
        assertTrue("ids.forEach { viewModel.addFromBank(it) }" in b)
        assertFalse("heightIn(max = 560.dp)" in b)
        val bank = source("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("internal fun BankQuestionContent(q: QuestionDraft)" in bank && "internal fun BankChip(" in bank)
    }

    @Test
    fun `site builder bank picker uses compact cards with eye and checkbox`() {
        val js = source("site/src/builder.js")
        assertTrue("class: 'bank-card compact b-bank-pick'" in js)
        assertTrue("el('label', {class: 'bk-check', title: 'انتخاب'}, [cb])" in js)
        assertTrue("window.SiteSchool.bankQuestionModal(it)" in js)
        assertTrue("class: 'b-bank bank-grid b-bank-grid'" in js)
        val sc = source("site/src/school.js")
        assertTrue("function bankQuestionModal(it)" in sc && "window.SiteSchool = {bankQuestionModal: bankQuestionModal," in sc)
        assertTrue("function showQuestion() { bankQuestionModal(it); }" in sc)
        val css = source("site/src/site.css")
        assertTrue(".bank-card.b-bank-pick.on{border-color:var(--brand,#39758a);" in css)
        assertTrue("html body:not(.m-mode) .bank-card label.bk-check > input[type=checkbox]" in css)
    }
}
