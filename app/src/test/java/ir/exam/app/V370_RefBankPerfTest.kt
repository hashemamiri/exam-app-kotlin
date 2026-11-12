package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankContract
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V370 — کارایی بانک مرجع: کش محلی، شارد صفحه‌ای، اسکلتون، پیش‌خوانی.
 *
 * تست، متن سورس سایت و اپ را می‌خواند تا قاعده‌ها برنگردند (مثل بازگشت
 * no-store یا حذف اسکلتون). تنظیمات مسیر و کش هم مستقیم سنجیده می‌شوند.
 */
class V370_RefBankPerfTest {

    private fun root(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "settings.gradle.kts").isFile) {
            dir = dir.parentFile ?: error("ریشهٔ مخزن پیدا نشد")
        }
        return dir
    }

    private fun src(path: String): String = File(root(), path).readText()

    private val site get() = src("site/src/refbank.js")
    private val repo get() = src("app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt")

    @Test
    fun `paths of paged shards are fixed`() {
        assertEquals("v3701", ReferenceBankContract.PAGED_VERSION)
        assertEquals(50, ReferenceBankContract.PAGE_CHUNK)
        assertEquals("questions/g7.idx.json", ReferenceBankContract.questionIndexPath(7))
        assertEquals("questions/g7.p3.json", ReferenceBankContract.questionPagePath(7, 3))
    }

    @Test
    fun `site keeps a local cache and asks with no-cache`() {
        assertTrue("کش محلی مرورگر", "indexedDB" in site)
        assertTrue("تازه‌آوری از سرور فقط با اعتبارسنجی", "cache: 'no-cache'" in site)
        assertFalse("no-store نباید برگردد", "cache: 'no-store'" in site)
        assertTrue("پیش‌خوانی صفحهٔ بعد", "requestIdleCallback" in site)
        assertTrue("اسکلتون سطر", "ref-skel-line" in site)
        assertTrue("تصویر تنبل", "decoding" in site)
    }

    @Test
    fun `site falls back to the whole shard`() {
        assertTrue("فهرست صفحه‌ها", "questions/g' + k + '.idx.json" in site)
        assertTrue("تکهٔ صفحه", "'.p' + p + '.json'" in site)
        assertTrue("پاییزگاه روشن", "if (!idx) return questions(k);" in site)
        assertTrue("برگشت در خطای تکه", "chunks.some(function (c) { return c === null; })" in site)
    }

    @Test
    fun `app caches shards on disk`() {
        assertTrue("کش دیسکی", "Cache(File(cacheDir, CACHE_DIR)" in repo)
        assertTrue("پنجرهٔ پنج‌دقیقه", "max-age=" in repo)
        assertTrue("نسخهٔ قالب صفحه", "ReferenceBankContract.PAGED_VERSION" in repo)
        assertTrue("پاییزگاه شارد کامل", "ReferenceBankContract.questionsPath(grade)" in repo)
    }

    @Test
    fun `app loads details for page ids only`() {
        val vm = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt")
        assertTrue("شناسه‌های صفحه", "referenceRepository.questions(grade, needIds)" in vm)
        assertTrue("پیش‌خوانی صفحهٔ بعد", "prefetchNextReferencePage()" in vm)
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("اسکلتون در پنجره", "ReferenceBankSkeleton()" in dialog)
    }
}
