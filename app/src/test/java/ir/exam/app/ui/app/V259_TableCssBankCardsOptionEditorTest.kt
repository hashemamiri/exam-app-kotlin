package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V259 — جدول در سایت (tbx-*) استایل دارد؛ کارت فشردهٔ بانک با چشم (اپ و سایت)؛ کادر گزینه‌ها در اپ = InlineMathTextEditor. */
class V259_TableCssBankCardsOptionEditorTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `site copies table css from print engine`() {
        val b = source("site/src/builder.js")
        val st = source("site/src/student.js")
        assertTrue("|tbx|tbx-[a-z0-9-]+|qmf-tab|tb-[a-z-]+)\\b/;" in b)
        assertTrue("|tbx|tbx-[a-z0-9-]+|qmf-tab|tb-[a-z-]+)\\b/;" in st)
        assertTrue(".tbx-t{" in source("app/src/main/assets/print/web/tools_styles.css"))
    }

    @Test
    fun `bank cards are compact with eye preview and exam title`() {
        val sc = source("site/src/school.js")
        assertTrue("el('div', {class: 'bank-card compact'}" in sc)
        assertTrue("class: 'icon-btn bk-eye', title: 'نمایش محتوای سؤال'" in sc && "onclick: showQuestion" in sc)
        assertTrue("el('div', {class: 'v', text: qq.examTitle || '—'})" in sc)
        val b = source("site/src/builder.js")
        assertTrue("comb.examTitle = (state.title || '').trim();" in b)
        val k = source("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("Icons.Outlined.Visibility" in k && "contentDescription = \"نمایش محتوای سؤال\"" in k)
        assertTrue("fun BankQuestionContent(q: QuestionDraft)" in k) // V260: internal (در پنجرهٔ بانکِ سازنده هم استفاده می‌شود)
        assertTrue("Text(item.examTitle.orEmpty().ifBlank { \"—\" }" in k)
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("(\"examTitle\" to kotlinx.serialization.json.JsonPrimitive(examTitle.trim()))" in repo)
        assertTrue("examTitle = combined[\"examTitle\"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)" in repo)
        assertTrue("repository.saveToBank(question, state.value.subject, categoryIds, examTitle = state.value.title)" in source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt"))
    }

    @Test
    fun `app option fields use inline math editor like question box`() {
        val b = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("InlineMathTextEditor(\n                                    source = option," in b)
        assertTrue("showToolbar = false," in b)
        assertTrue("openFieldFigureEditor(InsertMenuRef(\"option\", index, optionLabel), occurrence, spec)" in b)
        assertTrue("viewModel.deleteFieldFigure(question.id, \"option\", index, occurrence)" in b)
        assertTrue("fieldEdit != null -> {" in b)
        // تعریف تابع محلی باید پیش از استفاده باشد
        assertTrue(b.indexOf("fun openFieldFigureEditor(") < b.indexOf("openFieldFigureEditor(InsertMenuRef("))
        assertFalse("visualTransformation = FigTokenVisuals.transformation(MaterialTheme.colorScheme.primary),\n                                    modifier = Modifier.fillMaxWidth()\n                                )\n                                if ('\$' in option" in b)
        val vm = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("fun updateFieldFigure(id: String, field: String, index: Int, occurrenceIndex: Int, spec: FigureSpec)" in vm)
        assertTrue("fun deleteFieldFigure(id: String, field: String, index: Int, occurrenceIndex: Int)" in vm)
        val ed = source("app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt")
        assertTrue("showToolbar: Boolean = true" in ed && "if (showToolbar) Row(" in ed)
    }
}
