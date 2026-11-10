package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankFormula
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.domain.bank.withFormulaTokens
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V361 — فرمولهای MathJax شارد بانک مرجع به توکن `$...$` ویرایشگر فرمول
 * تبدیل میشوند؛ بُردارها بین کاتلین، جاوااسکریپت و سازندهٔ شارد مشترکاند.
 */
class V361_FormulaTokensTest {

    @Serializable
    data class Vector(val raw: String, val tokens: String)

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String = File(root(), path).readText()

    private fun vectors(): List<Vector> = Json.decodeFromString(
        src("app/src/test/resources/v361_formula_vectors.json")
    )

    @Test
    fun sharedVectorsMatchKotlinConverter() {
        val vs = vectors()
        assertTrue("بُردارها باید پر باشند", vs.size >= 8)
        vs.forEach { v ->
            assertEquals("تبدیل نادرست: " + v.raw, v.tokens, ReferenceBankFormula.mathJaxToTokens(v.raw))
        }
    }

    @Test
    fun conversionIsIdempotent() {
        val once = ReferenceBankFormula.mathJaxToTokens("\\(\\displaystyle \\text{۸}-\\text{۳}=\\text{۵}\\)")
        assertEquals(once, ReferenceBankFormula.mathJaxToTokens(once))
        assertEquals(2, ReferenceBankFormula.tokenCount("کدام \\(a\\) یا \\(b\\)؟"))
    }

    @Test
    fun questionFieldsAreAllConverted() {
        val q = ReferenceBankQuestion(
            id = 1,
            type = "multiple",
            text = "کدام \\(x^2\\) درست است؟",
            qHtml = "<span class='rz-formula'>\\(x^2\\)</span>",
            answerHtml = "<span class='az-formula'>\\(\\text{۴}\\)</span>",
            options = listOf("\\(\\text{۲}\\)", "\\(\\text{۴}\\)"),
            answerText = "\\(\\text{۴}\\)"
        )
        val c = q.withFormulaTokens()
        assertTrue(c.text.contains("$x^2$"))
        assertTrue(c.qHtml.contains("$x^2$"))
        assertTrue(c.answerHtml.contains("$\\text{۴}$"))
        assertEquals("$\\text{۲}$", c.options[0])
        assertEquals("$\\text{۴}$", c.answerText)
        assertFalse("نباید فرمول MathJax باقی بماند", ReferenceBankFormula.hasMathJax(c.text))
        assertFalse("نباید فرمول MathJax باقی بماند", ReferenceBankFormula.hasMathJax(c.qHtml))
    }

    @Test
    fun appAndSiteAndGeneratorCarryTheConverter() {
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankFormula.kt")
        assertTrue("fun mathJaxToTokens(raw: String): String" in models)
        assertTrue("fun separateAdjacent(value: String): String" in models)
        assertTrue("fun ReferenceBankQuestion.withFormulaTokens()" in models)
        val repo = src("app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt")
        assertTrue(".withFormulaTokens()" in repo)
        val mapper = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt")
        assertTrue("ReferenceBankFormula.mathJaxToTokens" in mapper)
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("NativeMathText" in dialog)
        val builderScreen = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("NativeMathText(" in builderScreen)
        assertTrue("question.answerExplanation" in builderScreen)
        assertTrue("توضیح پاسخ / راهنمای معلم" in builderScreen)
        val siteBuilder = src("site/src/builder.js")
        assertTrue("tokenTextarea(ansTa, {small: true})" in siteBuilder)
        assertTrue("توضیح پاسخ / راهنمای معلم" in siteBuilder)
        val siteApp = src("site/src/app.js")
        assertTrue("function openImagePreview(src, alt)" in siteApp)
        assertTrue("arena-image-preview-thumb" in siteApp)
        assertTrue("بستن تصویر" in siteApp)
        assertFalse("بررسی شارد" in ref)
        val ref = src("site/src/refbank.js")
        assertTrue("function mathJaxToTokens(raw)" in ref)
        assertTrue("function tokenizeQuestion(q)" in ref)
        assertTrue("function renderFormulaSpans(root)" in ref)
        assertTrue("renderFormulaSpans(qv)" in ref)
        assertTrue("mathJaxToTokens: mathJaxToTokens" in ref)
        assertTrue("tokenTextarea(ansTa, {small: true})" in src("site/src/builder.js"))
        assertFalse("بررسی شارد" in ref)
        val appShell = src("site/src/app.js")
        assertTrue("function openImagePreview(src, alt)" in appShell)
        assertTrue("arena-image-preview-thumb" in appShell)
        assertTrue("بستن تصویر" in appShell)
        val gen = src("scripts/review_reference_bank.py")
        assertTrue("def mathjax_to_tokens(raw):" in gen)
        assertTrue("def separate_adjacent(value):" in gen)
        assertTrue("\"formula_rows\": 0" in gen)
        val guard = src("scripts/check_reference_bank_formula.py")
        assertTrue("--selftest" in guard)
        assertTrue("--shard" in guard)
    }
}
