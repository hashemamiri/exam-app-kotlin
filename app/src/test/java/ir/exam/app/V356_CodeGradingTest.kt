package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.domain.grading.CodeCheckRow
import ir.exam.app.domain.grading.CodeGrading
import ir.exam.app.domain.grading.CodeSpecRow
import ir.exam.app.domain.grading.CodeTestRow
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V356 — بستهٔ ۳: تصحیح کد، فاز صفر.
 *
 * مرز اصلی این بسته: **کد دانش‌آموز هرگز اجرا نمی‌شود** و هیچ نمره‌ای
 * خودکار ذخیره نمی‌شود. معلم نتیجهٔ هر مورد آزمون را خودش علامت می‌زند
 * و سامانه فقط از وزن‌ها پیشنهاد می‌سازد. این آزمون آن مرزها را پین
 * می‌کند، به‌علاوهٔ رفتار الگوریتم و سیم‌کشی سایت/اپ/SQL.
 *
 * برابری دقیق با جاوااسکریپت در V356_CodeGradingParityTest سنجیده می‌شود.
 */
class V356_CodeGradingTest {

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
    // ۱) قالب موارد آزمون
    // ------------------------------------------------------------------

    @Test
    fun parseTestsReadsLabelWeightAndHidden() {
        val text = """
            مثال ساده | 2 3 => 5
            عدد منفی | 2 | -2 3 => 1
            حالت مرزی | پنهان | 0 0 => 0
        """.trimIndent()
        val r = CodeGrading.parseTests(text)
        assertTrue(r.errors.isEmpty())
        assertEquals(3, r.tests.size)
        assertEquals("مثال ساده", r.tests[0].label)
        assertEquals(1.0, r.tests[0].weight, 1e-9)
        assertEquals(2.0, r.tests[1].weight, 1e-9)
        assertTrue(r.tests[2].hidden)
        assertFalse(r.tests[0].hidden)
    }

    @Test
    fun parseTestsRoundTripsExactly() {
        val tests = listOf(
            CodeGrading.TestCase("خط جدید", "x\ny", "1\n2", 2.0, true),
            CodeGrading.TestCase("تب", "a\tb", "ok", 1.0, false),
            CodeGrading.TestCase("بک‌اسلش", "a\\nb", "c\\d", 1.0, false)
        )
        val back = CodeGrading.parseTests(CodeGrading.formatTests(tests))
        assertTrue(back.errors.isEmpty())
        assertEquals(tests, back.tests)
    }

    @Test
    fun parseTestsReportsErrorsWithLineNumber() {
        val r = CodeGrading.parseTests("خوب | a => b\nبدون جداکننده\nبرچسب | => ")
        assertEquals(1, r.tests.size)
        assertEquals(2, r.errors.size)
        assertEquals(2, r.errors[0].line)
        assertEquals(3, r.errors[1].line)
        assertTrue(r.errors[0].message.contains("=>"))
    }

    @Test
    fun parseTestsEnforcesFortyCaseLimit() {
        val text = (1..45).joinToString("\n") { "t$it | $it => $it" }
        val r = CodeGrading.parseTests(text)
        assertEquals(CodeGrading.MAX_TESTS, r.tests.size)
        assertTrue(r.errors.any { it.message.contains("۴۰") })
        assertEquals(40, CodeGrading.MAX_TESTS)
    }

    @Test
    fun parseTestsIgnoresCommentsAndBlankLines() {
        val r = CodeGrading.parseTests("# راهنما\n\n  \nیک | a => b\n")
        assertTrue(r.errors.isEmpty())
        assertEquals(1, r.tests.size)
    }

    // ------------------------------------------------------------------
    // ۲) نمره از وزن‌ها
    // ------------------------------------------------------------------

    @Test
    fun scoreIsWeightedNotCountBased() {
        val tests = listOf(
            CodeGrading.WeightedTest(1, 1.0),
            CodeGrading.WeightedTest(2, 2.0),
            CodeGrading.WeightedTest(3, 1.0)
        )
        // فقط مورد سنگین قبول → ۲ از ۴ وزن → نصف نمره
        val r = CodeGrading.scoreFromTests(tests, mapOf(2L to CodeGrading.Check(true)), 4.0)
        assertEquals(2.0, r.suggested, 1e-9)
        assertEquals(0.5, r.ratio, 1e-9)
        assertEquals(1, r.passedCount)
        assertEquals(1, r.checkedCount)
        assertFalse("بررسی کامل نیست", r.complete)
    }

    @Test
    fun uncheckedTestsAreNotCountedAsPassed() {
        val tests = listOf(CodeGrading.WeightedTest(1, 1.0), CodeGrading.WeightedTest(2, 1.0))
        val r = CodeGrading.scoreFromTests(tests, emptyMap(), 4.0)
        assertEquals(0.0, r.suggested, 1e-9)
        assertEquals(0, r.checkedCount)
        assertFalse(r.complete)
        assertTrue(r.hasTests)
    }

    @Test
    fun scoreNeverExceedsMaxAndRoundsToQuarter() {
        val tests = listOf(CodeGrading.WeightedTest(1, 1.0), CodeGrading.WeightedTest(2, 2.0))
        val r = CodeGrading.scoreFromTests(tests, mapOf(1L to CodeGrading.Check(true)), 2.0)
        // یک‌سوم از ۲ = ۰٫۶۶ → ۰٫۷۵
        assertEquals(0.75, r.suggested, 1e-9)
        val full = CodeGrading.scoreFromTests(
            tests, mapOf(1L to CodeGrading.Check(true), 2L to CodeGrading.Check(true)), 1.0)
        assertEquals(1.0, full.suggested, 1e-9)
        assertTrue(full.complete)
    }

    @Test
    fun noTestsMeansNoSuggestion() {
        val r = CodeGrading.scoreFromTests(emptyList(), emptyMap(), 4.0)
        assertFalse(r.hasTests)
        assertFalse(r.complete)
        assertEquals(0.0, r.suggested, 1e-9)
        assertEquals("مورد آزمونی تعریف نشده", CodeGrading.summary(emptyList(), emptyMap()))
    }

    @Test
    fun summaryDistinguishesStages() {
        val tests = listOf(CodeGrading.WeightedTest(1, 1.0), CodeGrading.WeightedTest(2, 1.0))
        assertEquals("هنوز بررسی نشده", CodeGrading.summary(tests, emptyMap()))
        assertEquals("بررسی ناقص", CodeGrading.summary(tests, mapOf(1L to CodeGrading.Check(true))))
        assertEquals("بررسی کامل", CodeGrading.summary(
            tests, mapOf(1L to CodeGrading.Check(true), 2L to CodeGrading.Check(false))))
    }

    // ------------------------------------------------------------------
    // ۳) مقایسهٔ خروجی
    // ------------------------------------------------------------------

    @Test
    fun outputComparisonNormalizesWhitespaceAndLineEndings() {
        assertTrue(CodeGrading.outputMatches("5\n", "  5  \n\n"))
        assertTrue(CodeGrading.outputMatches("a\r\nb", "a\nb"))
        assertTrue(CodeGrading.outputMatches("a\n\nb", "a\nb"))
        assertFalse(CodeGrading.outputMatches("5", "6"))
    }

    @Test
    fun outputComparisonOptionsAreHonoured() {
        assertFalse(CodeGrading.outputMatches("OK", "ok"))
        assertTrue(CodeGrading.outputMatches("OK", "ok", CodeGrading.Options(ignoreCase = true)))
        assertFalse("با خاموش بودن حذف خط خالی، تفاوت باید بماند",
            CodeGrading.outputMatches("a\n\nb", "a\nb", CodeGrading.Options(collapseBlank = false)))
        assertFalse("با خاموش بودن پیرایش، فاصله باید بماند",
            CodeGrading.outputMatches("a", "  a  ", CodeGrading.Options(trimLines = false)))
    }

    // ------------------------------------------------------------------
    // ۴) خواندن پاسخ سرور
    // ------------------------------------------------------------------

    @Test
    fun serverRowsParse() {
        val specs = CodeSpecRow.listFrom(Json.parseToJsonElement(
            """[{"question_index":0,"language":"python","reference":"print(1)",
                 "ignore_case":false,"trim_lines":true,"collapse_blank":true}]"""
        ) as JsonArray)
        assertEquals(1, specs.size)
        assertEquals("python", specs[0].language)
        assertTrue(specs[0].options.trimLines)
        assertFalse(specs[0].options.ignoreCase)

        val tests = CodeTestRow.listFrom(Json.parseToJsonElement(
            """[{"id":7,"question_index":0,"ord":0,"label":"مثال","input":"2 3",
                 "expected":"5","weight":2,"hidden":true}, 9]"""
        ) as JsonArray)
        assertEquals(1, tests.size)
        assertEquals(7L, tests[0].id)
        assertEquals(2.0, tests[0].weight, 1e-9)
        assertTrue(tests[0].hidden)
        assertEquals(7L, tests[0].weighted.id)
    }

    @Test
    fun checkRowsGroupByAnswerAndQuestion() {
        val rows = CodeCheckRow.listFrom(Json.parseToJsonElement(
            """[{"answer_id":"a1","question_index":0,"test_id":7,"passed":true,"actual":"5"},
                {"answer_id":"a1","question_index":0,"test_id":8,"passed":false,"actual":""},
                {"answer_id":"a2","question_index":0,"test_id":7,"passed":true,"actual":"5"}]"""
        ) as JsonArray)
        assertEquals(3, rows.size)
        val grouped = CodeCheckRow.group(rows)
        assertEquals(2, grouped.size)
        assertEquals(2, grouped["a1:0"]!!.size)
        assertEquals(true, grouped["a1:0"]!![7L]!!.passed)
        assertEquals(false, grouped["a1:0"]!![8L]!!.passed)
        // کلید باید همان قالب سایت باشد: answer:question
        assertEquals("a1:0", CodeCheckRow.key("a1", 0))
    }

    // ------------------------------------------------------------------
    // ۵) پین سیم‌کشی و مرزها
    // ------------------------------------------------------------------

    @Test
    fun repositoryWiresAllFourV356Rpcs() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt")
        listOf(
            "native_code_spec_get_v356", "native_code_spec_save_v356",
            "native_code_checks_v356", "native_code_check_save_v356"
        ).forEach { assertTrue("تابع در مخزن صدا زده نشده: $it", repo.contains(it)) }
        assertTrue(repo.contains("cleared_checks"))
    }

    @Test
    fun noCodeExecutionAnywhereInNewModules() {
        val files = listOf(
            "site/src/codegrade.js",
            "app/src/main/java/ir/exam/app/domain/grading/CodeGrading.kt",
            "app/src/main/java/ir/exam/app/ui/grading/GradingCodeContent.kt"
        )
        val banned = listOf("eval(", "new Function(", "ProcessBuilder", "Runtime.getRuntime", "exec(")
        files.forEach { f ->
            val text = src(f)
            banned.forEach { b -> assertFalse("اجرای کد نباید وجود داشته باشد: $f :: $b", text.contains(b)) }
        }
        assertTrue(src("site/src/codegrade.js").contains("کد دانش‌آموز"))
        assertTrue(src("app/src/main/java/ir/exam/app/ui/grading/GradingCodeContent.kt").contains("کد اجرا نمی‌شود"))
    }

    @Test
    fun siteAndAppShareTestLineFormatAndLimits() {
        val site = src("site/src/codegrade.js")
        val app = src("app/src/main/java/ir/exam/app/domain/grading/CodeGrading.kt")
        assertTrue(site.contains("MAX_TESTS = 40"))
        assertTrue(app.contains("MAX_TESTS = 40"))
        assertTrue(site.contains("MAX_LABEL = 120"))
        assertTrue(app.contains("MAX_LABEL = 120"))
        assertTrue(site.contains("=>"))
        assertTrue(app.contains("=>"))
        assertTrue("پنهان باید در هر دو پشتیبانی شود", site.contains("پنهان") && app.contains("پنهان"))
    }

    @Test
    fun siteBundlesCodeGradeBeforeAdmin() {
        val build = src("site/build_site.py")
        val cg = build.indexOf("\"codegrade.js\"")
        val admin = build.indexOf("\"admin.js\"")
        assertTrue("codegrade.js باید در بسته باشد", cg > 0)
        assertTrue("codegrade.js باید پیش از admin.js بیاید", cg < admin)
        val adminSrc = src("site/src/admin.js")
        assertTrue(adminSrc.contains("codeSpecEditor(ctx, loadCodeSpecs)"))
        assertTrue(adminSrc.contains("codeChecklistPanel(ctx, q, i, a, r, inp, codeState)"))
        assertTrue(adminSrc.contains("اعمال پیشنهاد"))
        assertTrue("عنوان وضعیت باید تازه شود", adminSrc.contains("headTitle.textContent"))
    }

    @Test
    fun appScreenWiresChecklistAndEditor() {
        val screen = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue(screen.contains("CodeChecklistCard("))
        assertTrue(screen.contains("CodeSpecEditorDialog("))
        assertTrue(screen.contains("Text(\"موارد آزمون کد\")"))
        assertTrue("اعمال پیشنهاد باید به کادر نمره برود", screen.contains("viewModel.setScore(submission.id, qi, v.toString())"))
        val vm = src("app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt")
        assertTrue(vm.contains("CodeGrading.scoreFromTests("))
        assertTrue(vm.contains("if (value == \"grading\") loadCodeGrading()"))
    }

    @Test
    fun migrationUsesTextIdsAndKeepsTestsTeacherOnly() {
        val sql = src("supabase/sql/SQL_CODE_GRADING_V356.sql")
        assertTrue(sql.contains("exam_id        text not null"))
        assertTrue(sql.contains("native_code_spec_get_v356(p_exam text)"))
        assertTrue(sql.contains("native_code_checks_v356(p_exam text)"))
        assertTrue(sql.contains("e.id::text = code_specs.exam_id"))
        assertTrue(sql.contains("e.id::text = code_tests.exam_id"))
        assertTrue(sql.contains("e.id::text = code_checks.exam_id"))
        assertFalse("نباید سیاست نوشتن ساخته شود", sql.contains("for insert on public.code_tests"))
        assertTrue("چک‌لیست یتیم باید پاک شود", sql.contains("delete from public.code_checks"))
        assertTrue(sql.contains("cleared_checks"))
        val check = src("supabase/sql/SQL_CODE_GRADING_V356_CHECK.sql")
        assertTrue(check.contains("native_code_spec_save_v356(text,integer,text,text,jsonb,boolean,boolean,boolean)"))
        assertFalse(check.contains("native_code_spec_get_v356(uuid)"))
    }

    @Test
    fun messageNumberGuardIsWiredIntoCi() {
        val guard = src("scripts/check_message_numbers.py")
        assertTrue(guard.contains("numeral_runs"))
        assertTrue("ریشه باید پوشهٔ مخزن باشد", guard.contains("resolve().parent.parent"))
        val ci = src(".github/workflows/android.yml")
        assertTrue("نگهبان رقم فارسی باید در CI اجرا شود", ci.contains("check_message_numbers"))
    }

    @Test
    fun versionBumpedAndChangelogIsValid() {
        // V356.1 — عدد نسخه این‌جا سخت‌رمز نمی‌شود؛ وگرنه هر انتشار
        // بعدی این آزمون را می‌شکست. چیزی که باید همیشه درست بماند،
        // هم‌خوانی فایل نسخه با بلوک بالای فهرست تغییرات است.
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب شمارهٔ نسخه نامعتبر است: $version", version.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        val lines = src("text/CHANGELOG_FA.txt").lines()
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertTrue("بلوک بالا باید شمارهٔ نسخه باشد", top.version.matches(Regex("V\\d+(\\.\\d+)?")))
            assertTrue(top.appAffecting)
            assertEquals("فایل نسخه و بلوک بالای فهرست تغییرات باید یکی باشند", version, top.appVersion)
            assertTrue(top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach { assertFalse(it.contains(Regex("\\bV\\d"))) }
        }
        // V356.1 — این وعده به بلوک مخصوص V356 گره خورده، نه بلوک بالا؛
        // وگرنه هر انتشار بعدی این سنجه را می‌شکست.
        val block = ReleaseNotes.parse(src("text/CHANGELOG_FA.txt").lines())
            .firstOrNull { it.version == "V356" }
        assertTrue("بلوک V356 در فهرست تغییرات نیست", block != null)
        assertTrue("وعدهٔ اجرا نکردن کد باید در بلوک V356 بماند",
            block!!.bullets.any { it.contains("اجرا نمی‌شود") })
    }
}
