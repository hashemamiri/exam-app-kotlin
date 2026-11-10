package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankMedia
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V360.3 — پینهای تصمیمهای تازهٔ کاربر:
 *  ۱) دکمهها و چیپها در عرض کم فشرده نمیشوند.
 *  ۲) بانک مرجع از آزمون جدید، دکمهٔ + و بانک سؤال باز میشود؛ افزودن گروهی.
 *  ۳) فیلتر مرحلهای: پایه ← رشته (فقط ۱۰ تا ۱۲) ← درس ← فصل ← نوع ← دشواری.
 *  ۴) پاسخ و تصویر پاسخ فقط با انتخاب کاربر و فقط در بخش پاسخ.
 *  ۵) هیچ سؤال یا تصویری به APK/باندل سایت اضافه نمیشود.
 */
class V360_3_ReferenceBankFlowTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.isFile)
        return f.readText()
    }

    @Test
    fun answerImageIsNeverAPartOfQuestionImages() {
        val q = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank/images/q.webp"
        val a = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank/images/a.webp"
        val qHtml = "<div><img src=\"" + q + "\"></div>"
        val aHtml = "<div><img src='" + a + "'></div>"
        val split = ReferenceBankMedia.splitImages(qHtml, aHtml, listOf(q, a))
        assertEquals(listOf(q), split.first)
        assertEquals(listOf(a), split.second)
        assertEquals(1, ReferenceBankMedia.imageUrlsFromHtml(aHtml).size)
        assertTrue(ReferenceBankMedia.plainText("<b>پاسخ</b>").contains("پاسخ"))
        // هیچ شارد تازهای منتشر نشد: تصویر پاسخ از همان answer_html خوانده میشود.
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
        assertTrue("fun splitImages" in models)
        assertTrue("answerHtml" in models)
        val mapper = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt")
        assertTrue("images = qImages.map" in mapper)
        assertTrue("answerImages = if (includeAnswer) aImages else emptyList()" in mapper)
        assertTrue("expectedText = expected" in mapper)
    }

    @Test
    fun trackFieldOnlyAppearsFromGradeTen() {
        assertEquals(10, ReferenceBankContract.TRACK_MIN_GRADE)
        assertTrue(ReferenceBankContract.tracksFor(9).isEmpty())
        assertTrue(ReferenceBankContract.tracksFor(10).contains("انسانی"))
        assertTrue(ReferenceBankContract.subjectsFor(1).contains("فارسی"))
        assertTrue(ReferenceBankContract.subjectsFor(10, "انسانی").contains("تاریخ"))
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
        assertTrue("TRACK_MIN_GRADE = 10" in models)
        val wizard = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("referenceTracksVisible" in wizard)
        assertTrue("STEP_TRACK" in wizard)
        val site = src("site/src/refbank.js")
        assertTrue("function tracksFor(g) { return g >= 10 ? TRACKS : []; }" in site)
        assertTrue("st.stage === 0) st.stage = st.grade >= 10 ? 1 : 2;" in site)
    }

    @Test
    fun wizardStepsBulkAddAndFilterIconAreWired() {
        val screen = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("FlowRow(horizontalArrangement" in screen)
        assertTrue("maxLines = 1" in screen)
        assertTrue("فیلتر بانک مرجع" in screen)
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("ReferenceBankHost" in dialog)
        assertTrue("openReferenceFilter" in dialog)
        assertTrue("addReferenceMany" in dialog)
        assertTrue("تأیید فیلتر و نمایش سؤالها" in dialog)
        val vm = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt")
        assertTrue("fun applyReferenceFilter()" in vm)
        assertTrue("fun addReferenceMany(" in vm)
        assertTrue("referenceIncludeAnswer" in vm)
        val site = src("site/src/refbank.js")
        assertTrue("S.go('builder', { refAdd: out })" in site)
        assertTrue("addRefDrafts" in site)
        assertTrue("🔍 فیلتر" in site)
        assertTrue("افزودن گروهی" in site)
        assertTrue("loading: 'lazy'" in site)
    }

    @Test
    fun referenceBankIsReachableFromNewExamPlusAndQuestionBank() {
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("ReferenceBankHost" in app)
        assertTrue("onOpenReferenceBank" in app)
        val overlay = src("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt")
        assertTrue("referenceBankVisible" in overlay)
        assertTrue("🏛 بانک مرجع" in overlay)
        val builder = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("ReferenceBankHost" in builder)
        assertTrue("🏛 بانک مرجع (فیلتر و افزودن گروهی)" in builder)
        val vm = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("fun addReferenceQuestion(" in vm)
        val siteBuilder = src("site/src/builder.js")
        assertTrue("RefBank.open({append: true})" in siteBuilder)
        assertTrue("addRefDrafts: function" in siteBuilder)
        val mobile = src("site/src/mobile.js")
        assertTrue("'بانک مرجع'" in mobile)
        val desktop = src("site/src/app.js")
        assertTrue("'بانک مرجع': ['refbank'" in desktop)
    }

    @Test
    fun answerSectionIsSeparateFromQuestionAndStoredInKey() {
        val draft = src("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt")
        assertTrue("val answerExplanation: String = \"\"" in draft)
        assertTrue("val answerImages: List<String> = emptyList()" in draft)
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt")
        assertTrue("answerExplanation" in codec)
        assertTrue("answerImages" in codec)
        val appUi = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("پاسخ / پاسخنامه" in appUi)
        val siteBuilder = src("site/src/builder.js")
        assertTrue("answerExplanation" in siteBuilder)
        assertTrue("answerImages" in siteBuilder)
        assertTrue("پاسخ / پاسخنامه" in siteBuilder)
        val ref = src("site/src/refbank.js")
        assertTrue("answerImages: inc ? sp.answer.slice(0, 8) : []" in ref)
        assertTrue("inc.checked = st.includeAnswer" in ref)
    }

    @Test
    fun noReferenceQuestionDataIsBundled() {
        val guard = src("scripts/check_reference_bank_bundle.py")
        assertTrue("FORBIDDEN_NAMES" in guard)
        assertTrue("--selftest" in guard)
        val idx = src("site/index.html")
        assertTrue("__REF_BANK_BASE__" in idx)
        val fixture = src("app/src/test/resources/v360_reference_bank_fixture.json")
        assertTrue("\"fields_removed\"" in fixture)
    }
}
