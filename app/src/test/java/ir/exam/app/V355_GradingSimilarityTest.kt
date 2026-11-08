package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.domain.grading.EssayKeywordRow
import ir.exam.app.domain.grading.Similarity
import ir.exam.app.domain.grading.SimilarityReviewRow
import ir.exam.app.domain.grading.SimilarityReviewStatus
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V355 — بستهٔ ۲: تشخیص شباهت پاسخ‌ها و دستیار کلیدواژه‌ای تشریحی.
 *
 * دو چیز این‌جا پین می‌شود:
 *  ۱) رفتار الگوریتم (کپی با نام متغیر متفاوت گرفته شود، پاسخ کوتاه
 *     هشدار ندهد، و هیچ‌جا نمره خودکار تغییر نکند)؛
 *  ۲) سیم‌کشی واقعی سایت/اپ/SQL، از جمله نوع text شناسه‌ها که در V354
 *     باعث شکست نصب شده بود.
 *
 * برابری دقیق کاتلین با جاوااسکریپت در V355_SimilarityParityTest سنجیده
 * می‌شود؛ این فایل رفتار و سیم‌کشی را می‌سنجد.
 */
class V355_GradingSimilarityTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.exists())
        return f.readText()
    }

    // ------------------------------------------------------------------
    // ۱) رفتار الگوریتم
    // ------------------------------------------------------------------

    private val essayA = "قضیه فیثاغورس در مثلث قائم الزاویه بیان می کند که مجذور وتر برابر است با مجموع مجذور دو ضلع دیگر"
    private val essayB = "قضيه فيثاغورس در مثلث قائم الزاويه بيان مي كند که مجذور وتر برابر است با مجموع مجذور دو ضلع ديگر"
    private val essayC = "امروز هوا بسیار خوب بود و من به پارک رفتم و با دوستانم بازی کردم و خیلی خوش گذشت"

    @Test
    fun persianArabicLetterFoldingMakesCopyIdentical() {
        assertEquals(Similarity.essayTokens(essayA), Similarity.essayTokens(essayB))
        val r = Similarity.compare(Similarity.essayTokens(essayA), Similarity.essayTokens(essayB), 0)
        assertNotNull(r)
        assertEquals(1.0, r!!.score, 1e-9)
    }

    @Test
    fun unrelatedEssaysScoreZero() {
        val r = Similarity.compare(Similarity.essayTokens(essayA), Similarity.essayTokens(essayC), 0)!!
        assertEquals(0.0, r.score, 1e-9)
    }

    @Test
    fun persianAndArabicDigitsFoldToLatin() {
        assertEquals(Similarity.essayTokens("۱۲۳"), Similarity.essayTokens("123"))
        assertEquals(Similarity.essayTokens("٧٨٩"), Similarity.essayTokens("789"))
        assertEquals(listOf("123"), Similarity.essayTokens("۱۲۳"))
    }

    @Test
    fun zwnjBecomesSpace() {
        assertEquals(Similarity.normalizeText("می‌روم"), "می روم")
    }

    @Test
    fun renamedVariablesAndDifferentCommentsStillMatch() {
        val a = "```python\ndef add(x, y):\n    # جمع دو عدد\n    return x + y\n```"
        val b = "```python\ndef add(first, second):\n    # توضیح کاملاً متفاوت\n    return first + second\n```"
        assertEquals(Similarity.codeTokens(a), Similarity.codeTokens(b))
        val r = Similarity.compare(Similarity.codeTokens(a), Similarity.codeTokens(b), 0)!!
        assertEquals(1.0, r.score, 1e-9)
    }

    @Test
    fun codeTokenizerMasksStringsAndNumbers() {
        assertEquals(listOf("ID", "=", "STR", ";", "ID", "=", "NUM"), Similarity.codeTokens("x = \"hi\"; y = 3.14"))
        // کلیدواژه حفظ می‌شود، شناسه نه
        assertTrue(Similarity.codeTokens("return total").contains("return"))
        assertTrue(Similarity.codeTokens("return total").contains("ID"))
    }

    @Test
    fun shortAnswersAreNotCompared() {
        assertNull("پاسخ کوتاه نباید هشدار بدهد", Similarity.compare(listOf("بله", "درست", "است"), listOf("بله", "درست", "است")))
        assertNull(Similarity.compare(emptyList(), emptyList()))
        assertEquals(8, Similarity.MIN_TOKENS)
    }

    @Test
    fun containmentCatchesPartialCopy() {
        val long = (essayA + " " + essayC)
        val r = Similarity.compare(Similarity.essayTokens(essayA), Similarity.essayTokens(long), 0)!!
        assertTrue("پوشش باید کپی جزئی را بگیرد", r.containment > 0.9)
        assertTrue("اشتراک کمتر از پوشش است", r.jaccard < r.containment)
        assertEquals(r.containment, r.score, 1e-9)
    }

    @Test
    fun pairKeyIsOrderIndependent() {
        assertEquals("a|b", Similarity.pairKey("a", "b"))
        assertEquals("a|b", Similarity.pairKey("b", "a"))
        assertEquals("x|x", Similarity.pairKey("x", "x"))
    }

    @Test
    fun extractUnitsSkipsLonelyAndShortAnswers() {
        val qs = listOf(Similarity.QuestionShape("text", emptyList()))
        val answers = listOf(
            Similarity.RawAnswer("a1", listOf(essayA)),
            Similarity.RawAnswer("a2", listOf(essayB)),
            Similarity.RawAnswer("a3", listOf("کوتاه"))
        )
        val units = Similarity.extractUnits(qs, answers)
        assertEquals(1, units.size)
        assertEquals(2, units[0].byAnswer.size)
        assertFalse(units[0].byAnswer.containsKey("a3"))
        val hits = Similarity.findSimilar(units)
        assertEquals(1, hits.size)
        assertEquals("a1|a2", hits[0].pairKey)
    }

    @Test
    fun subPartsAreComparedSeparately() {
        val raw1 = """{"parts":{"p1":{"answer":"$essayA"},"p2":{"answer":"$essayC"}}}"""
        val raw2 = """{"parts":{"p1":{"answer":"$essayB"},"p2":{"answer":"پاسخ کاملاً متفاوت و بی ربط به دیگری که طول کافی دارد"}}}"""
        val qs = listOf(Similarity.QuestionShape("proof", listOf(
            Similarity.PartShape("p1", false), Similarity.PartShape("p2", false))))
        val units = Similarity.extractUnits(qs, listOf(
            Similarity.RawAnswer("a1", listOf(raw1)), Similarity.RawAnswer("a2", listOf(raw2))))
        assertEquals(2, units.size)
        assertEquals("p1", units[0].partKey)
        val hits = Similarity.findSimilar(units)
        assertEquals("فقط زیربخش یکسان باید هشدار بدهد", 1, hits.size)
        assertEquals("p1", hits[0].partKey)
    }

    @Test
    fun codeAnswersAreTaggedAsCode() {
        val code = "```python\ndef f(a, b):\n    return a + b + a * b - a\n```"
        val qs = listOf(Similarity.QuestionShape("code", emptyList()))
        val units = Similarity.extractUnits(qs, listOf(
            Similarity.RawAnswer("a1", listOf(code)), Similarity.RawAnswer("a2", listOf(code))))
        assertEquals("code", units[0].kind)
    }

    @Test
    fun higherThresholdReportsFewerPairs() {
        val units = Similarity.extractUnits(
            listOf(Similarity.QuestionShape("text", emptyList())),
            listOf(
                Similarity.RawAnswer("a1", listOf(essayA)),
                Similarity.RawAnswer("a2", listOf(essayA + " " + essayC)),
                Similarity.RawAnswer("a3", listOf(essayB))
            )
        )
        val loose = Similarity.findSimilar(units, 0.6)
        val strict = Similarity.findSimilar(units, 0.99)
        assertTrue(loose.size >= strict.size)
        assertTrue(strict.all { it.score >= 0.99 })
        // ترتیب نزولی امتیاز
        assertEquals(loose.map { it.score }.sortedDescending(), loose.map { it.score })
    }

    // ------------------------------------------------------------------
    // ۲) دستیار کلیدواژه‌ای
    // ------------------------------------------------------------------

    @Test
    fun keywordAssistWeightsAndAlternatives() {
        val kws = listOf(
            Similarity.EssayKeyword("فیثاغورس", 2.0),
            Similarity.EssayKeyword("وتر", 1.0),
            Similarity.EssayKeyword("مجذور|توان دو", 1.0),
            Similarity.EssayKeyword("قائم الزاویه", 1.5)
        )
        val r = Similarity.essayAssist(kws, essayA, 4.0)
        assertEquals(4, r.matched.size)
        assertEquals(0, r.missing.size)
        assertEquals(1.0, r.ratio, 1e-9)
        assertEquals(4.0, r.suggested, 1e-9)
        assertEquals(5.5, r.weightTotal, 1e-9)
    }

    @Test
    fun keywordAssistPartialCreditIsRoundedToQuarter() {
        val kws = listOf(Similarity.EssayKeyword("فیثاغورس", 1.0), Similarity.EssayKeyword("نیامده", 2.0))
        val r = Similarity.essayAssist(kws, essayA, 2.0)
        assertEquals(listOf("فیثاغورس"), r.matched)
        assertEquals(listOf("نیامده"), r.missing)
        // نسبت ۱/۳ از ۲ = ۰٫۶۶۶ → گِرد به ۰٫۷۵
        assertEquals(0.75, r.suggested, 1e-9)
    }

    @Test
    fun keywordAssistNeverExceedsMaxAndHandlesEmpty() {
        val kws = listOf(Similarity.EssayKeyword("فیثاغورس", 1.0))
        assertEquals(0.0, Similarity.essayAssist(kws, "", 5.0).suggested, 1e-9)
        assertEquals(0.0, Similarity.essayAssist(emptyList(), essayA, 5.0).suggested, 1e-9)
        assertTrue(Similarity.essayAssist(kws, essayA, 1.0).suggested <= 1.0)
    }

    @Test
    fun keywordMatchRequiresContiguousPhrase() {
        val tk = Similarity.essayTokens("مثلث قائم الزاویه است")
        assertTrue(Similarity.keywordMatch("قائم الزاویه", tk))
        assertFalse("عبارت باید پشت‌سرهم باشد", Similarity.keywordMatch("الزاویه قائم", tk))
        assertTrue(Similarity.keywordMatch("ناموجود|قائم الزاویه", tk))
        assertFalse(Similarity.keywordMatch("", tk))
    }

    // ------------------------------------------------------------------
    // ۳) خواندن پاسخ سرور
    // ------------------------------------------------------------------

    @Test
    fun keywordRowsParse() {
        val arr = Json.parseToJsonElement(
            """[{"id":1,"question_index":0,"part_key":"","keyword":"وتر","weight":2.5},
                {"id":2,"question_index":1,"part_key":"p2","keyword":"گام","weight":1}, 7]"""
        ) as JsonArray
        val rows = EssayKeywordRow.listFrom(arr)
        assertEquals(2, rows.size)
        assertEquals("وتر", rows[0].keyword)
        assertEquals(2.5, rows[0].weight, 1e-9)
        assertEquals("p2", rows[1].partKey)
    }

    @Test
    fun reviewRowsParseAndLookupKeyMatchesSite() {
        val arr = Json.parseToJsonElement(
            """[{"id":1,"pair_key":"a1|a2","question_index":0,"part_key":"","status":"confirmed","note":"کپی"}]"""
        ) as JsonArray
        val rows = SimilarityReviewRow.listFrom(arr)
        assertEquals(1, rows.size)
        assertEquals(SimilarityReviewStatus.CONFIRMED, rows[0].status)
        // کلید باید دقیقاً همان قالب سایت باشد: pair#question#part
        assertEquals("a1|a2#0#", rows[0].lookupKey)
        assertEquals(rows[0].lookupKey, SimilarityReviewRow.lookupKey("a1|a2", 0, ""))
    }

    @Test
    fun reviewStatusFallsBackSafely() {
        assertEquals(SimilarityReviewStatus.OPEN, SimilarityReviewStatus.fromWire("نامعلوم"))
        assertEquals(SimilarityReviewStatus.OPEN, SimilarityReviewStatus.fromWire(null))
        assertEquals(SimilarityReviewStatus.CHECKED, SimilarityReviewStatus.fromWire("checked"))
    }

    // ------------------------------------------------------------------
    // ۴) پین سیم‌کشی
    // ------------------------------------------------------------------

    @Test
    fun repositoryWiresAllFourV355Rpcs() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        listOf(
            "native_essay_keywords_v355", "native_essay_keywords_save_v355",
            "native_similarity_reviews_v355", "native_similarity_review_save_v355"
        ).forEach { assertTrue("تابع در مخزن صدا زده نشده: $it", repo.contains(it)) }
        assertTrue("تأیید تخلف باید توضیح بخواهد", repo.contains("برای تأیید تخلف، توضیح دست‌کم ۳ نویسه لازم است."))
    }

    @Test
    fun gradingScreenAndViewModelWireSimilarityTab() {
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue(vm.contains("\"similarity\""))
        assertTrue(vm.contains("if (value == \"similarity\") loadSimilarity()"))
        assertTrue(vm.contains("Similarity.extractUnits(shapes, raws)"))
        assertTrue("پیام باید بگوید نمره تغییر نکرد", vm.contains("نمره‌ای تغییر نکرد."))
        val screen = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue(screen.contains("SimilarityContent(state, viewModel)"))
        assertTrue(screen.contains("Text(\"شباهت پاسخ‌ها\")"))
    }

    @Test
    fun siteBundlesSimilarityModuleBeforeAdmin() {
        val build = src("site/build_site.py")
        val sim = build.indexOf("\"similarity.js\"")
        val admin = build.indexOf("\"admin.js\"")
        assertTrue("similarity.js باید در بسته باشد", sim > 0)
        assertTrue("similarity.js باید پیش از admin.js بیاید", sim < admin)
        val adminSrc = src("site/src/admin.js")
        assertTrue(adminSrc.contains("['similarity', '🔍 شباهت پاسخ‌ها']"))
        assertTrue(adminSrc.contains("tabSimilarity(body, ctx)"))
        assertTrue(adminSrc.contains("SIM.essayAssist("))
        assertTrue("اعمال پیشنهاد باید دستی باشد", adminSrc.contains("اعمال پیشنهاد"))
    }

    @Test
    fun siteAndAppShareTheSameKeywordLineFormat() {
        val adminSrc = src("site/src/admin.js")
        val appSrc = src("app/src/main/java/ir/exam/app/ui/grading/GradingSimilarityContent.kt")
        // هر دو «= وزن» را با آخرین «=» جدا می‌کنند تا بدل‌های «|» نشکنند
        assertTrue(adminSrc.contains("line.lastIndexOf('=')"))
        assertTrue(appSrc.contains("line.lastIndexOf('=')"))
        assertTrue(adminSrc.contains("items.length > 60"))
        assertTrue(appSrc.contains("items.size > 60"))
    }

    @Test
    fun migrationUsesTextIdsAndNoWritePolicy() {
        val sql = src("supabase/sql/SQL_GRADING_SIMILARITY_V355.sql")
        assertTrue(sql.contains("exam_id        text not null"))
        assertTrue(sql.contains("native_essay_keywords_v355(p_exam text)"))
        assertTrue(sql.contains("native_similarity_reviews_v355(p_exam text)"))
        assertTrue(sql.contains("e.id::text = essay_keywords.exam_id"))
        assertTrue(sql.contains("e.id::text = similarity_reviews.exam_id"))
        assertFalse("نباید سیاست نوشتن ساخته شود", sql.contains("for insert on public.essay_keywords"))
        val check = src("supabase/sql/SQL_GRADING_SIMILARITY_V355_CHECK.sql")
        assertTrue(check.contains("native_essay_keywords_save_v355(text,integer,text,jsonb)"))
        assertFalse(check.contains("native_essay_keywords_v355(uuid)"))
    }

    @Test
    fun versionBumpedAndChangelogIsValid() {
        assertEquals("1.03.72", src("text/APP_VERSION.txt").trim())
        val lines = src("text/CHANGELOG_FA.txt").lines()
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertEquals("V355", top.version)
            assertTrue(top.appAffecting)
            assertEquals("1.03.72", top.appVersion)
            assertTrue(top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach { assertFalse(it.contains(Regex("\\bV\\d"))) }
        }
    }
}
