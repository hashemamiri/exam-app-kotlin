package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankQuestion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V374 — تصویرهای پیوست بانک در بانک، پیش‌نمایش و چاپ دیده می‌شوند.
 *
 * گزارش V373 روی دادهٔ واقعی: ۹۵۳ سؤال تصویرشان فقط در image_urls بود و
 * <img> در متن نداشتند؛ برشِ «فقط q_html» آن‌ها را در همه‌جا حذف می‌کرد.
 */
class V374_BankMediaTest {

    private fun root(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "settings.gradle.kts").isFile) {
            dir = dir.parentFile ?: error("ریشهٔ مخزن پیدا نشد")
        }
        return dir
    }

    private fun src(path: String): String = File(root(), path).readText(Charsets.UTF_8)

    private val models get() = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
    private val site get() = src("site/src/refbank.js")
    private val siteApp get() = src("site/src/app.js")
    private val inliner get() = src("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlImageInliner.kt")
    private val BASE = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank/images/"

    @Test
    fun `attachment images arrive even without img tags`() {
        val q = BASE + "q1.webp"
        val q2 = BASE + "q2.webp"
        val question = ReferenceBankQuestion(
            id = 7,
            type = "multiple",
            options = listOf("الف", "ب"),
            qHtml = "<div>شکل روبه‌رو چه نام دارد؟</div>",
            imageUrls = listOf(q, q2)
        )
        assertEquals(listOf(q, q2), question.questionImageUrls)
        assertTrue("ثابت قاعدهٔ تازه", "inQuestion + imageUrls" in models)
        assertTrue("بدون تکرار", ".distinct()" in models)
    }

    @Test
    fun `option and answer images never enter the question`() {
        val q = BASE + "q.webp"
        val a = BASE + "a.webp"
        val o1 = BASE + "o1.webp"
        val question = ReferenceBankQuestion(
            id = 8,
            type = "multiple",
            options = listOf("الف", "ب"),
            qHtml = "<div>صورت</div>",
            answerHtml = "<div>پاسخ <img src=\"" + a + "\"></div>",
            optionImages = listOf(o1, null),
            imageUrls = listOf(q, a, o1)
        )
        assertEquals(listOf(q), question.questionImageUrls)
        assertEquals(listOf(a), question.answerImageUrls)
    }

    @Test
    fun `site renders attachments next to the text`() {
        assertTrue("پیوست‌ها به متن اضافه می‌شوند", "attachmentImages" in site)
        assertTrue("کلاس تصویر", "ref-q-img" in site)
        assertTrue("قاعدهٔ تازه", "all.forEach" in site)
        assertFalse("پیوست تکراری نمایش داده نشود", "q.indexOf(u) < 0) q.push(u)" !in site)
        assertTrue("گارد V372 حفظ شد", "tinyGuard" in site)
    }

    @Test
    fun `print never drops an image silently`() {
        assertFalse("سایت: حذف بی‌صدا ممنوع", "if (!im) { q.text = q.text.replace" in siteApp)
        assertFalse("سایت: بوم آلوده هم تصویر را حذف نکند", "if (!data) { q.text = q.text.replace" in siteApp)
        assertTrue("اپ: توکن با نشانی اصلی", "tokens.append(imageToken(url))" in inliner)
        assertTrue("اپ: نشانهٔ تازه", "V374" in inliner)
    }
}
