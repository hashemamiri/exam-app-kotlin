package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankMedia
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.ui.bank.toBankQuestionOption
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V363 — متن سؤال بانک مرجع در همهٔ پایه‌ها.
 *
 * ‏شاردهای پایه‌های ۶، ۷ و ۸ فیلد `text` را خالی گذاشته‌اند و متن کامل فقط
 * در `q_html` است. فهرست بانک و سؤالی که به آزمون افزوده می‌شود همان `text`
 * را می‌خواندند؛ نتیجه: سطر و آزمون بی‌متن، ولی پیش‌نمایش سالم.
 */
class V363_ReferenceBankTextTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String = File(root(), path).readText(Charsets.UTF_8)

    private val bridgePath = "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt"
    private val mapperPath = "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt"
    private val dialogPath = "app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt"
    private val sitePath = "site/src/refbank.js"

    // ۱) پل اصلی ------------------------------------------------------------

    @Test
    fun emptyTextFallsBackToQuestionHtml() {
        // نمونهٔ واقعی پایهٔ هشتم: text خالی، q_html پر
        val html = "<div><span>به پرسش زیر دربارهٔ بخش‌پذیری</span> <span>دقت کنید</span></div>"
        assertEquals("به پرسش زیر دربارهٔ بخش‌پذیری دقت کنید",
            ReferenceBankMedia.displayText("", html))
    }

    @Test
    fun readyTextWinsOverHtml() {
        // پایه‌های ۱ تا ۵ متن آماده دارند؛ نباید بازنویسی شود.
        assertEquals("متن آمادهٔ سازنده", ReferenceBankMedia.displayText("متن آمادهٔ سازنده", "<div>چیز دیگر</div>"))
        assertEquals("متن آمادهٔ سازنده", ReferenceBankQuestion(text = "متن آمادهٔ سازنده", qHtml = "<div>چیز دیگر</div>").displayText)
    }

    @Test
    fun zeroWidthNonJoinerSurvives() {
        // plainText قدیمی نیم‌فاصله را می‌شکست («می شود»)؛ متن سؤال نباید بشکند.
        assertEquals("می‌شود ۲ + ۳ برابر ۵", ReferenceBankMedia.displayText("", "<div>می&zwnj;شود ۲ + ۳ برابر ۵</div>"))
        assertEquals("نیم‌فاصلهٔ خودِ متن هم دست‌نخورده می‌ماند",
            "می\u200cشود", ReferenceBankMedia.displayText("می\u200cشود", ""))
    }

    @Test
    fun snippetKeepsWordsAndFormulaBoundaries() {
        val long = "الف ".repeat(120) + "$\\displaystyle x$"
        val cut = ReferenceBankMedia.displayText(long, "", 60)
        assertTrue("برش باید کوتاه باشد: ${cut.length}", cut.length <= 61)
        assertTrue("برش باید با «…» تمام شود", cut.endsWith("…"))
        assertEquals("تعداد «$» باید جفت باشد", 0, cut.count { it == '$' } % 2)
        assertTrue("کلمه نباید نصفه بماند", !cut.removeSuffix("…").endsWith("ال"))
    }

    @Test
    fun unbalancedFormulaIsNeverLeftOpen() {
        // ۱۷ سؤال پایهٔ هشتم در خودِ منبع وسط فرمول بریده شده‌اند.
        val broken = "اگر $\\displaystyle x=\\text{۲}$ و $\\displaystyle y"
        val shown = ReferenceBankMedia.balanceMath(broken)
        assertEquals("تعداد «$» باید جفت باشد", 0, shown.count { it == '$' } % 2)
        assertTrue(shown.endsWith("…"))

        val cutLong = ReferenceBankMedia.displayText("ب ".repeat(50) + "$x$ و $y", "", 40)
        assertEquals(0, cutLong.count { it == '$' } % 2)
    }

    // ۲) مپر آزمون ---------------------------------------------------------

    @Test
    fun questionAddedToExamKeepsItsText() {
        val question = ReferenceBankQuestion(
            id = 6001,
            grade = 6,
            type = "multiple",
            text = "",
            qHtml = "<div>حاصل ۲ + ۳ کدام است؟</div>",
            options = listOf("۴", "۵", "۶", "۷"),
            correctIndex = 1
        )
        val draft = question.toBankQuestionOption(includeAnswer = false).question
        assertEquals(QuestionType.MULTIPLE_CHOICE, draft.type)
        assertEquals("حاصل ۲ + ۳ کدام است؟", draft.text)
        assertEquals(4, draft.options.size)
    }

    @Test
    fun blankOptionsBecomeEssayUnlessTheyHaveImages() {
        val blankOptions = ReferenceBankQuestion(
            id = 6002,
            grade = 6,
            type = "multiple",
            text = "",
            qHtml = "<div>سؤالی که گزینه‌هایش خالی است؟</div>",
            options = listOf("", ""),
            correctIndex = 0
        )
        assertEquals(QuestionType.ESSAY, blankOptions.toBankQuestionOption(false).question.type)

        val withImages = ReferenceBankQuestion(
            id = 6003,
            grade = 6,
            type = "multiple",
            text = "",
            qHtml = "<div>سؤال تصویری</div>",
            options = listOf("", ""),
            correctIndex = 0,
            optionImages = listOf("https://media.example/o1.webp", "https://media.example/o2.webp")
        )
        assertEquals(QuestionType.MULTIPLE_CHOICE, withImages.toBankQuestionOption(false).question.type)
    }

    // ۳) پین‌ها -------------------------------------------------------------

    @Test
    fun appSiteToolAndWorkflowsStayWired() {
        val models = src(bridgePath)
        val dialog = src(dialogPath)
        val site = src(sitePath)
        assertTrue("پل متن در مدل", "fun displayText(text: String, qHtml: String, limit: Int = 0)" in models)
        assertTrue("متن بدون شکستن نیم‌فاصله", "fun htmlToText(html: String): String" in models)
        assertTrue("بستن فرمول باز", "fun balanceMath(value: String): String" in models)
        assertTrue("کلید شارد تازه", "SHARD_VERSION = \"v3630\"" in models)
        assertTrue("سطر بی‌متن راهنما می‌گیرد", "در فهرست این پایه نیست" in dialog)

        assertTrue("سایت همان پل را دارد", "plainQuestion" in site)
        assertTrue("سایت برش امن دارد", "function safeSnippet(value, limit)" in site)
        assertTrue("کلید شارد سایت", "SHARD_VERSION = 'v3630'" in site)

        val tool = File(root(), "scripts/repair_reference_bank_text.py")
        assertTrue("ابزار ترمیم شارد باید موجود باشد", tool.isFile)
        val text = tool.readText(Charsets.UTF_8)
        assertTrue("\"--check\"" in text)
        assertTrue("\"--with-questions\"" in text)
        assertTrue("\"--local-dir\"" in text)

        val guard = File(root(), "scripts/check_reference_bank_text.py")
        assertTrue("نگهبان متن", guard.isFile)
        assertTrue("نگهبان در گردش‌کار اپ",
            File(root(), ".github/workflows/android.yml").readText(Charsets.UTF_8).contains("check_reference_bank_text"))
        assertTrue("نگهبان در گردش‌کار سایت",
            File(root(), ".github/workflows/site.yml").readText(Charsets.UTF_8).contains("check_reference_bank_text"))
    }
}
