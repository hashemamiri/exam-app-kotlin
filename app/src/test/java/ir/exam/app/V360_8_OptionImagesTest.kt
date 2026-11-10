package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankQuestion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V360.8 — تصویر گزینههای تستی فقط کنار همان گزینه، نه در متن سؤال.
 *
 * پیشتر شاردها تصویر گزینهها را بیصاحب در image_urls داشتند و اپ/سایت
 * هر تصویری را که پاسخ نبود «تصویر سؤال» حساب میکردند. اکنون شارد فیلد
 * option_images دارد و مدل اپ سه دستهٔ جدا میسازد.
 */
class V360_8_OptionImagesTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String = File(root(), path).readText()

    private val base = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank/images/"

    @Test
    fun optionImagesStayAttachedToTheirOwnOptions() {
        val q = base + "q.webp"
        val a = base + "a.webp"
        val o1 = base + "o1.webp"
        val o2 = base + "o2.webp"
        val question = ReferenceBankQuestion(
            id = 1,
            type = "multiple",
            options = listOf("الف", "ب"),
            qHtml = "<div>صورت <img src=\"" + q + "\"></div>",
            answerHtml = "<div>پاسخ <img src=\"" + a + "\"></div>",
            optionImages = listOf(o1, o2),
            imageUrls = listOf(q, o1, o2, a)
        )
        assertEquals(listOf(q), question.questionImageUrls)
        assertEquals(listOf(a), question.answerImageUrls)
        assertEquals(listOf(o1, o2), question.optionImages)
    }

    @Test
    fun questionTextNeverReceivesOptionImages() {
        val q = base + "q.webp"
        val o1 = base + "o1.webp"
        val o2 = base + "o2.webp"
        val question = ReferenceBankQuestion(
            id = 2,
            type = "multiple",
            options = listOf("الف", "ب", "ج", "د"),
            qHtml = "<div>متن سؤال</div>",
            optionImages = listOf(o1, o2, null, null),
            imageUrls = listOf(q, o1, o2)
        )
        assertTrue("هیچ تصویر گزینهای نباید وارد صورت سؤال شود",
            question.questionImageUrls.none { it in question.optionImages.filterNotNull() })
        assertEquals(listOf(o1, o2, null, null), question.optionImages)
    }

    @Test
    fun artifactQuotaNeverBlocksAppBuild() {
        val android = src(".github/workflows/android.yml")
        assertTrue("پر شدن سهمیهٔ آرتیفکت فقط هشدار است" in android)
        assertTrue("continue-on-error: true" in android)
        val guard = src("scripts/check_actions_storage.py")
        assertTrue("uses: actions/upload-artifact@" in guard)
        assertTrue("continue-on-error: true" in guard)
    }

    @Test
    fun shardGeneratorAndBothClientsCarryTheField() {
        val review = src("scripts/review_reference_bank.py")
        assertTrue("\"option_images\": option_images" in review)
        assertTrue("image_urls_in_order" in review)
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
        assertTrue("@SerialName(\"option_images\")" in models)
        assertTrue("imageUrlsFromHtml(qHtml)" in models)
        val mapper = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt")
        assertTrue("optionImages = optionImagesForDraft" in mapper)
        assertTrue("questionImageUrls.take(8)" in mapper)
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("question.optionImages.getOrNull(i)" in dialog)
        val screen = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("q.optionImages.getOrNull(index)" in screen)
        val ref = src("site/src/refbank.js")
        assertTrue("p.option_images" in ref)
        assertTrue("q.optionImages = q.type === 'multiple'" in ref)
        val uploader = src("scripts/upload_reference_bank.py")
        assertTrue("\"--only\"" in uploader)
        val guard = src("scripts/check_reference_bank_option_images.py")
        assertTrue("--selftest" in guard)
        assertTrue("--shard" in guard)
    }
}
