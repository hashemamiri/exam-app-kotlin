package ir.exam.app

import ir.exam.app.domain.bank.BankPicker
import ir.exam.app.domain.bank.ReferenceBankContract
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V360 — پینهای تصمیمهای بانک سؤال مرجع.
 *
 * تصمیمهای کاربر: «تشریحی کوتاه» → «تشریحی»، حذف «درسنامه»،
 * بازبینی تکبهتک و اصلاح دسته، افزودن سطح چهارم، حذف فیلدهای منبع/مجوز،
 * همان S3سازگار فعلی، و استقرار نخست روی آزمایشی.
 */
class V360_ReferenceBankTest {

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

    private fun fixture(): String = src("app/src/test/resources/v360_reference_bank_fixture.json")

    @Test
    fun difficultyFourLevelsAreLiveInBothSharedModules() {
        assertEquals(5, BankPicker.DIFFICULTY_LABELS.size)
        assertEquals("خیلی دشوار", BankPicker.DIFFICULTY_LABELS[4])
        assertEquals(4, BankPicker.difficultyFromWord("خیلی دشوار"))
        assertEquals(4, BankPicker.itemDifficulty(BankPicker.Item("x", difficulty = 4)))
        assertEquals(5, ReferenceBankContract.DIFFICULTY_LABELS.size)
        assertEquals("خیلی دشوار", ReferenceBankContract.DIFFICULTY_LABELS.last())
        val js = src("site/src/bankpick.js")
        assertTrue("سطح چهارم در سایت هم باید باشد", js.contains("'خیلی دشوار'"))
        assertTrue("پالایش سطح چهارم نباید صفر شود", js.contains("d > 4 ? 0 : d"))
    }

    @Test
    fun gradeTwelveAndDifficultyFourClampsAreLive() {
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt")
        assertTrue(codec.contains("coerceIn(1, 4)"))
        assertTrue(codec.contains("coerceIn(0, 12)"))
        assertTrue(codec.contains("question.difficulty in 1..4"))
        assertTrue(codec.contains("question.grade in 1..12"))
        val builder = src("site/src/builder.js")
        assertTrue(builder.contains("Math.min(4,Number(obj.difficulty)"))
        assertTrue(builder.contains("Math.min(12, Number(obj.grade)"))
        assertTrue(builder.contains("q.grade <= 12"))
        val screen = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue(screen.contains("(1..12).map"))
    }

    @Test
    fun reviewScriptPinsUserDecisions() {
        val s = src("scripts/review_reference_bank.py")
        assertTrue("درسنامه باید حذف شود", s.contains("DROP_TYPES = {\"درسنامه\"}"))
        assertTrue("تشریحی کوتاه باید نگاشت شود", s.contains("\"تشریحیکوتاه\": \"essay\""))
        assertTrue("سطح چهارم باید باشد", s.contains("\"خیلی دشوار\": 4"))
        assertTrue("بازبینی تکبهتک باید باشد", s.contains("for d in data:"))
        assertTrue("گزارش بازبینی باید نوشته شود", s.contains("review_report.csv"))
        assertTrue("مانیفست انتشار باید نوشته شود", s.contains("\"manifest.json\""))
        assertTrue("اصلاح دستی دسته باید مستند باشد", s.contains("EXPLICIT_SUBJECT_FIX"))
        assertTrue(
            "سه اصلاح صریح دسته باید مستند باشند",
            s.contains("1285411") && s.contains("1259767") && s.contains("1784392")
        )
    }

    @Test
    fun noSourceOrLicenseFieldsInPublishedFixture() {
        val f = fixture()
        assertTrue("فیلدهای حذفشده باید مستند شوند", f.contains("\"fields_removed\""))
        assertFalse("فیلد منبع نباید در ردیفها باشد", f.contains("\"source\":"))
        assertFalse("فیلد مجوز نباید در ردیفها باشد", f.contains("\"license\":"))
        assertFalse("فیلد بازبینی نباید در ردیفها باشد", f.contains("\"reviewed\":"))
        assertFalse("درسنامه نباید در ردیفها باشد", "\"type_fa\": \"درسنامه\"".let { f.contains(it) })
        assertTrue("نمونهٔ سطح چهارم باید باشد", f.contains("\"difficulty\": 4"))
        assertTrue("نگاشت تشریحی باید باشد", f.contains("\"type\": \"essay\""))
    }

    @Test
    fun guardAndUploaderExistAndAreWiredIntoCI() {
        val guard = src("scripts/check_reference_bank_bundle.py")
        assertTrue("نگهبان خودآزمون دارد", guard.contains("--selftest"))
        assertTrue("نگهبان فایل فهرست را رد میکند", guard.contains("FORBIDDEN_NAMES"))
        val uploader = src("scripts/upload_reference_bank.py")
        assertTrue("بارگذار فقطخواندنی نیست", uploader.contains("--apply"))
        assertTrue("بارگذار پیشوند جداگانه دارد", uploader.contains("PREFIX = \"reference-bank\""))
        assertFalse("هیچ کلیدی در کد درونخطی نشود", uploader.contains("S3_SECRET_ACCESS_KEY = \""))
        val android = src(".github/workflows/android.yml")
        val siteYml = src(".github/workflows/site.yml")
        assertTrue(android.contains("check_reference_bank_bundle"))
        assertTrue(siteYml.contains("check_reference_bank_bundle.py"))
        assertTrue(src("scripts/check_workflow_scripts.py").contains("loop_start"))
    }

    @Test
    fun sitePointsAtObjectStorageAndNotAtBundledQuestions() {
        val tpl = src("site/src/template.html")
        val idx = src("site/index.html")
        assertTrue(tpl.contains("__REF_BANK_BASE__"))
        assertTrue(idx.contains("https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank"))
        val ref = src("site/src/refbank.js")
        assertTrue(ref.contains("__REF_BANK_BASE__"))
        assertTrue(ref.contains("loading: 'lazy'"))
        assertTrue(ref.contains("S.go('builder', {refAdd: out})"))
        assertFalse("هیچ فهرست حجیمی داخل باندل سایت نشود", idx.contains("\"catalog_rows\": 2418"))
        val school = src("site/src/school.js")
        assertTrue(school.contains("بانک مرجع"))
    }

    @Test
    fun appReferenceBankFilesExistAndUseRemoteShards() {
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
        val repo = src("app/src/main/java/ir/exam/app/data/repository/ReferenceBankRepository.kt")
        val vm = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt")
        val screen = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        val mapper = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankMapper.kt")
        assertTrue(models.contains("MAX_GRADE = 12"))
        assertTrue(repo.contains("ReferenceBankContract.catalogPath") && repo.contains("ReferenceBankContract.questionsPath"))
        assertTrue(models.contains("catalog/g") && models.contains("questions/g"))
        assertTrue(vm.contains("referenceOpen") && vm.contains("loadReferenceCatalog"))
        assertTrue(screen.contains("ReferenceBankDialog"))
        assertTrue(mapper.contains("toBankQuestionOption"))
        assertTrue(models.contains("BASE = \"https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank\""))
    }

    @Test
    fun parityVectorsIncludeLevelFour() {
        val v = src("app/src/test/resources/v358_bankpick_vectors.json")
        assertTrue(v.contains("\"word\": \"خیلی دشوار\""))
        assertTrue(v.contains("\"difficulty\": 4"))
        val gen = src("scripts/gen_bankpick_vectors.mjs")
        assertTrue(gen.contains("difficulty: i % 5"))
        assertTrue(gen.contains("'همه | خیلی دشوار | 2'"))
    }

    @Test
    fun nativePreviewKeepsOldWebViewPinIntact() {
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("پیشنمایش باید تصویر بومی داشته باشد", dialog.contains("AsyncImage"))
        assertTrue("متن HTML باید بومی ساده شود", dialog.contains("Html.fromHtml"))
        val pin = src("app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt")
        assertTrue("پین قدیمی نباید حذف شود", pin.contains("WebView must not enter native runtime"))
        assertTrue("استثناهای قدیمی باید برقرار بمانند", pin.contains("QuestionTextFieldWebView.kt"))
    }

    @Test
    fun versionAndChangelogAgreeWithV360() {
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("شمارهٔ نسخه نباید ثابت شود", version.split(".").size == 3 && version.isNotBlank())
        val first = src("text/CHANGELOG_FA.txt").lines().first()
        assertTrue("تیتر باید شمارهٔ نسخه داشته باشد", Regex("^V\\d{3}(\\.\\d+)?\\b").containsMatchIn(first))
        assertTrue(first.contains("اپ $version"))
    }
}
