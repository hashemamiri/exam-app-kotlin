package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankRow
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V377 — متن سؤال‌های بی‌متن فهرست + فیلتر درست «با تصویر».
 *
 * سه چیزی که در سایت زنده دیده شد و این تست قفل می‌کند:
 *   ۱) فهرست پایهٔ دهم و رشتهٔ معارف ستون text خالی دارد؛
 *   ۲) در همان ردیف‌ها has_image=false است ولی image_urls پر است؛
 *   ۳) فیلتر «با تصویر» باید تصویر صورت سؤال را بشمارد، نه تصویر گزینه را.
 */
class V377_BankTextImageTest {

    /** در Gradle، user.dir پوشهٔ app/ است؛ تا ریشهٔ مخزن بالا می‌رویم. */
    private fun root(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "settings.gradle.kts").isFile) {
            dir = dir.parentFile ?: error("ریشهٔ مخزن پیدا نشد")
        }
        return dir
    }

    private fun src(path: String): String = File(root(), path).readText()

    private val models = "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt"
    private val repository = "app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt"
    private val viewModel = "app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt"
    private val site = "site/src/refbank.js"

    private fun row(
        imageUrls: List<String> = emptyList(),
        hasImage: Boolean = false,
        imageCount: Int = 0
    ) = ReferenceBankRow(
        id = 1, text = "", imageUrls = imageUrls,
        hasImage = hasImage, imageCount = imageCount
    )

    // ۱) تصویر صورت سؤال، نه تصویر گزینه ------------------------------------

    @Test
    fun `row with real question image counts as image row`() {
        val r = row(imageUrls = listOf("https://x/q.webp"))
        assertEquals(1, ReferenceBankContract.questionImageCount(r))
        assertTrue(r.hasQuestionImage())
    }

    @Test
    fun `row without any url and without count is not an image row`() {
        val r = row(imageUrls = emptyList(), hasImage = false, imageCount = 0)
        assertEquals(0, ReferenceBankContract.questionImageCount(r))
        assertFalse(r.hasQuestionImage())
    }

    @Test
    fun `has_image stays false on grade ten rows and still counts`() {
        // دادهٔ واقعی: has_image=false، image_count=0، image_urls پر.
        val r = row(imageUrls = listOf("https://x/a.webp"), hasImage = false, imageCount = 0)
        assertTrue(r.hasQuestionImage())
    }

    @Test
    fun `empty urls fall back to image_count`() {
        val r = row(imageUrls = emptyList(), hasImage = false, imageCount = 2)
        assertEquals(2, ReferenceBankContract.questionImageCount(r))
        assertTrue(r.hasQuestionImage())
    }

    // ۲) شاردهای فهرست -------------------------------------------------------

    @Test
    fun `catalog shards are read up to thirty two parts`() {
        val repo = src(repository)
        assertTrue("CATALOG_MAX_PARTS = 32" in repo)
        assertTrue("decodeShard" in repo)
        assertTrue("shard.track" in repo)
    }

    @Test
    fun `app reads shards even when no track is chosen`() {
        val repo = src(repository)
        val body = repo.substringAfter("suspend fun catalog(grade: Int, track: String = \"\")")
        val shardCall = body.indexOf("shardedCatalog(grade, track)")
        val fullFile = body.indexOf("catalogPath(grade)")
        assertTrue("پایهٔ دهم به بالا تکه‌ای خوانده می‌شود", shardCall in 0 until fullFile)
    }

    // ۳) سایت ----------------------------------------------------------------

    @Test
    fun `site understands wrapped shard files`() {
        val js = src(site)
        assertTrue("function catalogRows(data)" in js)
        assertTrue("Array.isArray(data.rows)" in js)
        assertTrue("var CATALOG_MAX_PARTS = 32;" in js)
    }

    @Test
    fun `site counts question images not option images`() {
        val js = src(site)
        assertTrue("function rowImageCount(r)" in js)
        assertTrue("function hasImageRow(r) {" in js)
        assertTrue("st.image === 'option'" in js)
        assertTrue("با تصویر سؤال" in js)
    }

    @Test
    fun `detail load error is retryable`() {
        val js = src(site)
        assertTrue("detailFailed" in js)
        assertTrue("تلاش دوباره" in js)
    }

    // ۴) رفتار قبلی دست‌نخورده ------------------------------------------------

    @Test
    fun `previous behaviour is untouched`() {
        val js = src(site)
        assertTrue("function catalog(grade, track)" in js)
        assertTrue("catalog/g' + k + '-s' + i + '.json'" in js)
        assertTrue("function sameTrack(rowTrack, wanted)" in js)
        val m = src(models)
        assertTrue("علوم و معارف اسلامی" in m)
        assertTrue("fun tracksFor(grade: Int)" in m)
        val vm = src(viewModel)
        assertTrue("sameTrack(r.track, referenceTrack)" in vm)
    }
}
