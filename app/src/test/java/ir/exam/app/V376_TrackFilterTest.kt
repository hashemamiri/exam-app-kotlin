package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankContract
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V376 — رشتهٔ «علوم و معارف اسلامی» در فیلتر بانک مرجع.
 *
 * دادهٔ واقعی پایهٔ دهم نام رشته را «علوم و معارف اسلامی» می‌آورد، ولی فهرست
 * رشته‌ها فقط «انسانی/تجربی/ریاضی فیزیک/هنرستان/عمومی» داشت؛ هر انتخابی از
 * این خانواده صفر سطر می‌داد. این تست هم مدل، هم سایت و هم مسیر تکه‌ای را
 * قفل می‌کند.
 */
class V376_TrackFilterTest {

    /**
     * در Gradle، user.dir پوشهٔ app/ است؛ تا ریشهٔ مخزن (settings.gradle.kts)
     * بالا می‌رویم — همان الگوی V370_RefBankPerfTest و بقیهٔ تست‌ها.
     */
    private fun root(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "settings.gradle.kts").isFile) {
            dir = dir.parentFile ?: error("ریشهٔ مخزن پیدا نشد")
        }
        return dir
    }

    private fun src(path: String): String = File(root(), path).readText()

    private val models = "app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt"
    private val viewModel = "app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt"
    private val repository = "app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt"
    private val site = "site/src/refbank.js"

    @Test
    fun `religious studies is a selectable track`() {
        assertTrue("رشتهٔ معارف در فهرست اپ", "علوم و معارف اسلامی" in ReferenceBankContract.TRACKS)
        assertTrue("پایهٔ دهم معارف را پیشنهاد می‌دهد", "علوم و معارف اسلامی" in ReferenceBankContract.tracksFor(10))
        assertTrue("زیر پایهٔ دهم رشته ندارد", ReferenceBankContract.tracksFor(9).isEmpty())
        assertTrue("اسم کوتاه هم پذیرفته می‌شود",
            ReferenceBankContract.trackKey("معارف") == ReferenceBankContract.trackKey("علوم و معارف اسلامی"))
        assertTrue("تطبیق سطر با رشتهٔ انتخابی",
            ReferenceBankContract.sameTrack("علوم و معارف اسلامی", "معارف"))
        assertTrue("رشتهٔ خالی یعنی همه", ReferenceBankContract.sameTrack("انسانی", ""))
        assertFalse("رشتهٔ دیگر نباید بگذرد", ReferenceBankContract.sameTrack("انسانی", "معارف"))
    }

    @Test
    fun `grade ten has no general track suggestion`() {
        assertFalse("پایهٔ ۱۰ «عمومی» ندارد", "عمومی" in ReferenceBankContract.tracksFor(10))
        assertTrue("پایهٔ ۱۱ «عمومی» دارد", "عمومی" in ReferenceBankContract.tracksFor(11))
    }

    @Test
    fun `site filter knows the religious studies label`() {
        val js = src(site)
        assertTrue("فهرست سایت", "'علوم و معارف اسلامی'" in js)
        assertTrue("هم‌خوانی نام", "function trackKey(v)" in js)
        assertTrue("پالایش با هم‌خوانی", "function sameTrack(rowTrack, wanted)" in js)
        assertTrue("کشوی رشته از فهرست پایه", "tracksFor(st.grade), st.track" in js)
        assertFalse("تطبیق سخت‌گیرانهٔ قدیمی نماند",
            "if (st.track && String(r.track || '') !== st.track) return false;" in js)
        assertTrue("پایهٔ ۱۰ بدون عمومی (سایت)", "n === 10 && t === 'عمومی'" in js)
    }

    @Test
    fun `grade ten catalog is read in shards`() {
        val js = src(site)
        assertTrue("تکه‌های فهرست (سایت)", "catalog/g' + k + '-s' + i + '.json'" in js)
        assertTrue("رهیافت فهرست تکه‌ای", "function catalogSharded(grade, track)" in js)
        assertTrue("فراخوانی با رشته", "catalog(st.grade, st.track)" in js)
        assertTrue("پاییزگاه شارد کامل", "catalog/g' + k + '.json'" in js)
        val repoText = src(repository)
        assertTrue("اپ: خواندن تکه‌ای", "shardedCatalog" in repoText)
        assertTrue("اپ: مسیر تکه", "catalogShardPath" in src(models))
        assertTrue("اپ: سقف تکه‌ها", "CATALOG_MAX_PARTS = 8" in repoText)
        assertTrue("اپ: تغییر رشته فهرست را از نو می‌خواند",
            "تکهٔ رشتهٔ تازه باید خوانده شود" in src(viewModel))
    }

    @Test
    fun `previous behaviour is untouched`() {
        assertTrue("قاعدهٔ تصویر سؤال (V374)", "imageUrlsFromHtml(qHtml)" in src(models))
        assertTrue("تصویر گزینه در چاپ (V375)", "optImgs" in src("site/src/app.js"))
    }
}
