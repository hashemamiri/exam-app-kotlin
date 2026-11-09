package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.domain.bank.BankPicker
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V358 — بانک سؤال: درختوارهٔ درس/فصل/مبحث، پالایش دشواری، انتخاب
 * خودکار با نقشهٔ آزمون.
 *
 * مرزهایی که این کلاس پین می‌کند:
 *  • درخت حداکثر سه سطح است و حلقه نمی‌پذیرد.
 *  • حذف دسته هرگز سؤال را حذف نمی‌کند مگر معلم صریح بخواهد.
 *  • یکتایی نام در هر سطح است، نه سراسری؛ «فصل ۱» زیر دو درس مجاز است.
 *  • سطح دشواری سه‌تاست (تصمیم کاربر)؛ سطح چهارم ساخته نشده.
 *  • گزینش با بذر بازتولیدپذیر است و سؤال تکراری برنمی‌دارد.
 *
 * برابری دقیق با جاوااسکریپت در V358_BankPickerParityTest سنجیده می‌شود.
 */
class V358_QuestionBankTest {

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
    // ۱) درخت
    // ------------------------------------------------------------------

    private val tree = listOf(
        BankPicker.Category("1", "ریاضی", null),
        BankPicker.Category("2", "فصل ۱", "1"),
        BankPicker.Category("3", "معادله", "2"),
        BankPicker.Category("4", "فیزیک", null),
        BankPicker.Category("5", "فصل ۱", "4")
    )

    @Test
    fun subtreeIncludesDescendantsAndStopsAtCycles() {
        assertEquals(listOf("1", "2", "3"), BankPicker.subtreeIds(tree, "1").sorted())
        assertEquals(listOf("2", "3"), BankPicker.subtreeIds(tree, "2").sorted())
        assertEquals(listOf("3"), BankPicker.subtreeIds(tree, "3"))
        assertTrue(BankPicker.subtreeIds(tree, null).isEmpty())
        assertTrue(BankPicker.subtreeIds(tree, "999").containsOnlySelf())
        val loop = listOf(BankPicker.Category("1", "الف", "2"), BankPicker.Category("2", "ب", "1"))
        assertEquals(2, BankPicker.subtreeIds(loop, "1").size)
        assertTrue("حلقه نباید بی‌پایان شود", BankPicker.depthOf(loop, "1") <= BankPicker.MAX_DEPTH + 2)
    }

    private fun List<String>.containsOnlySelf() = this == listOf("999")

    @Test
    fun pathShowsFullPathAndDisambiguatesSameName() {
        assertEquals("ریاضی / فصل ۱ / معادله", BankPicker.categoryPath(tree, "3"))
        assertEquals("فیزیک / فصل ۱", BankPicker.categoryPath(tree, "5"))
        assertEquals(3, BankPicker.depthOf(tree, "3"))
        assertEquals(1, BankPicker.depthOf(tree, "1"))
        assertEquals(0, BankPicker.depthOf(tree, "999"))
        // نام تکراری در دو شاخه باید ابهام بدهد، نه انتخاب دلبخواه
        val lookup = BankPicker.findCategory(tree, "فصل ۱")
        assertTrue("نام مبهم باید خطا بدهد", lookup.error != null)
        assertTrue(lookup.error!!.contains("مسیر کامل"))
        // مسیر کامل ابهام را برطرف می‌کند
        assertEquals("5", BankPicker.findCategory(tree, "فیزیک / فصل ۱").id)
    }

    @Test
    fun persianAndLatinDigitsMatchInCategoryNames() {
        assertEquals("2", BankPicker.findCategory(tree, "ریاضی / فصل 1").id)
        assertEquals("2", BankPicker.findCategory(tree, "ریاضی / فصل ۱").id)
    }

    // ------------------------------------------------------------------
    // ۲) پالایش دشواری — سه سطح
    // ------------------------------------------------------------------

    @Test
    fun difficultyHasExactlyThreeLevelsPlusUnset() {
        assertEquals(4, BankPicker.DIFFICULTY_LABELS.size)
        assertEquals("تعیین‌نشده", BankPicker.DIFFICULTY_LABELS[0])
        assertEquals(listOf("ساده", "متوسط", "دشوار"), BankPicker.DIFFICULTY_LABELS.drop(1))
        assertFalse("سطح چهارم نباید ساخته شود", BankPicker.DIFFICULTY_LABELS.any { it.contains("المپیاد") })
        assertEquals(-1, BankPicker.difficultyFromWord("هر"))
        assertEquals(1, BankPicker.difficultyFromWord("آسان"))
        assertEquals(1, BankPicker.difficultyFromWord("ساده"))
        assertEquals(null, BankPicker.difficultyFromWord("المپیادی"))
    }

    @Test
    fun filterCombinesSubtreeAndDifficulty() {
        val items = (1..9).map {
            BankPicker.Item(id = it.toString(), text = "پرسش $it", difficulty = (it % 3) + 1,
                catIds = listOf(if (it <= 6) "3" else "5"))
        }
        assertEquals(9, BankPicker.filterItems(items, tree, BankPicker.FilterOpts()).size)
        assertEquals(6, BankPicker.filterItems(items, tree, BankPicker.FilterOpts(categoryId = "1")).size)
        assertEquals(3, BankPicker.filterItems(items, tree, BankPicker.FilterOpts(categoryId = "4")).size)
        val easyInMath = BankPicker.filterItems(items, tree, BankPicker.FilterOpts(categoryId = "1", difficulty = 1))
        assertTrue(easyInMath.isNotEmpty())
        assertTrue(easyInMath.all { it.difficulty == 1 })
        // دشواری نامعتبر به تعیین‌نشده برمی‌گردد، نه سطح چهارم
        assertEquals(0, BankPicker.itemDifficulty(BankPicker.Item("x", difficulty = 7)))
        assertEquals(0, BankPicker.itemDifficulty(BankPicker.Item("x", difficulty = -2)))
    }

    // ------------------------------------------------------------------
    // ۳) نقشهٔ آزمون
    // ------------------------------------------------------------------

    @Test
    fun blueprintParsesPersianDigitsAndReportsErrors() {
        val ok = BankPicker.parseBlueprint("همه | هر | ۵", tree)
        assertTrue(ok.errors.isEmpty())
        assertEquals(5, ok.total)
        assertEquals(1, ok.rules.size)
        val bad = BankPicker.parseBlueprint("همه | هر | 0\nناموجود | هر | 2\nبدون جداکننده", tree)
        assertEquals(3, bad.errors.size)
        assertTrue(bad.rules.isEmpty())
        assertTrue(BankPicker.parseBlueprint("# فقط توضیح\n\n", tree).rules.isEmpty())
    }

    @Test
    fun blueprintRoundTripsExactly() {
        val text = "ریاضی | متوسط | 3\nهمه | هر | 2"
        val parsed = BankPicker.parseBlueprint(text, tree)
        assertTrue(parsed.errors.isEmpty())
        assertEquals(text, BankPicker.formatBlueprint(parsed.rules))
    }

    @Test
    fun pickNeverRepeatsAndReportsShortagePerRule() {
        val items = (1..6).map { BankPicker.Item(id = it.toString(), difficulty = 1, catIds = listOf("3")) }
        val rules = listOf(
            BankPicker.Rule("همه", null, -1, 4),
            BankPicker.Rule("همه", null, -1, 4)
        )
        val r = BankPicker.pick(items, tree, rules, 42)
        assertEquals(6, r.total)
        assertEquals(6, r.picked.map { it.id }.toSet().size)
        assertEquals(0, r.perRule[0].shortage)
        assertEquals(2, r.perRule[1].shortage)
        assertTrue(BankPicker.summary(r).contains("کمبود"))
    }

    @Test
    fun blueprintLimitsAreEnforced() {
        assertEquals(40, BankPicker.MAX_RULES)
        assertEquals(500, BankPicker.MAX_PICK)
        assertEquals(200, BankPicker.MAX_COUNT)
        assertTrue(BankPicker.parseBlueprint("همه | هر | 201", tree).errors.isNotEmpty())
        val many = (1..50).joinToString("\n") { "همه | هر | 20" }
        assertTrue("سقف مجموع باید اعمال شود", BankPicker.parseBlueprint(many, tree).errors.isNotEmpty())
    }

    // ------------------------------------------------------------------
    // ۴) SQL
    // ------------------------------------------------------------------

    @Test
    fun migrationBuildsTreeSafely() {
        val sql = src("supabase/sql/SQL_QUESTION_BANK_V358.sql")
        assertTrue("ستون والد نیست", sql.contains("add column if not exists parent_id bigint"))
        assertTrue("حذف والد آبشاری نیست", sql.contains("on delete cascade"))
        assertTrue("نگهبان ژرفا نیست", sql.contains("native_bank_tree_guard_v358"))
        assertTrue("حلقه بسته نشده", sql.contains("این جابه‌جایی حلقه می‌سازد."))
        assertTrue("سقف ژرفا اعمال نشده", sql.contains("if v_depth > 3 then"))
        assertTrue("والدِ خود بسته نشده", sql.contains("یک دسته نمی‌تواند والد خودش باشد."))
        assertTrue("برخورد نام پیام فارسی ندارد", sql.contains("در مقصد، دسته‌ای با همین نام هست."))
        assertTrue("ایندکس سراسری برداشته نشده", sql.contains("drop index if exists public.uq_bank_categories_teacher_name"))
        assertTrue("ایندکس هر سطح ساخته نشده", sql.contains("uq_bank_categories_parent_name"))
        assertTrue("تابع قدیمی باید بماند", sql.contains("native_bank_category_add_v1(p_name text)"))
        assertTrue("snapshot باید دشواری بدهد", sql.contains("'difficulty', coalesce"))
        assertFalse("سؤال نباید حذف شود", sql.contains("delete from public.question_bank"))
        val check = src("supabase/sql/SQL_QUESTION_BANK_V358_CHECK.sql")
        assertTrue(check.contains("نصب V358 انجام شده است"))
        assertTrue("بررسی نباید روی پایگاه نصب‌نشده سقوط کند",
            check.contains("raise notice 'هنوز نصب نشده است. ابتدا فایل نصب را اجرا کنید.'"))
    }

    // ------------------------------------------------------------------
    // ۵) سیم‌کشی اپ و سایت
    // ------------------------------------------------------------------

    @Test
    fun appRepositoryAndViewModelUseTheTree() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue(repo.contains("native_bank_category_add_v358"))
        assertTrue(repo.contains("native_bank_category_move_v358"))
        assertTrue(repo.contains("parentId = row[\"parent_id\"]"))
        val model = src("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt")
        assertTrue(model.contains("val parentId: Long? = null"))
        val vm = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt")
        assertTrue(vm.contains("val difficulty: Int? = null"))
        assertTrue(vm.contains("BankPicker.filterItems"))
        assertTrue(vm.contains("fun selectDifficulty"))
        assertTrue(vm.contains("fun moveCategory"))
    }

    @Test
    fun appScreensExposeDifficultyAndBlueprint() {
        val screen = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue(screen.contains("BankPicker.DIFFICULTY_LABELS"))
        assertTrue(screen.contains("viewModel.selectDifficulty"))
        assertTrue(screen.contains("زیرِ "))
        val builder = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue(builder.contains("BankBlueprintDialog("))
        assertTrue(builder.contains("انتخاب خودکار از بانک"))
        val dialog = src("app/src/main/java/ir/exam/app/ui/builder/BankBlueprintDialog.kt")
        assertTrue(dialog.contains("BankPicker.parseBlueprint"))
        assertTrue(dialog.contains("BankPicker.pick"))
        assertTrue(dialog.contains("قرعهٔ تازه"))
        assertTrue(dialog.contains("کمبود"))
    }

    @Test
    fun siteMirrorsTheSameBehaviour() {
        val school = src("site/src/school.js")
        assertTrue(school.contains("BP.filterItems"))
        assertTrue(school.contains("native_bank_category_add_v358"))
        assertTrue(school.contains("BP.categoryPath"))
        val builder = src("site/src/builder.js")
        assertTrue(builder.contains("function blueprintDialog"))
        assertTrue(builder.contains("BP.parseBlueprint"))
        assertTrue(builder.contains("BP.pick"))
        assertTrue(builder.contains("قرعهٔ تازه"))
        val build = src("site/build_site.py")
        val bp = build.indexOf("\"bankpick.js\"")
        val bd = build.indexOf("\"builder.js\"")
        assertTrue("bankpick.js در بسته نیست", bp > 0)
        assertTrue("باید پیش از builder.js بیاید", bp < bd)
    }

    @Test
    fun mockServerCarriesTheTreeHandlers() {
        val mock = src("scripts/mock_supabase_server.py")
        listOf("native_bank_category_add_v358", "native_bank_category_move_v358", "native_bank_tree_v358")
            .forEach { assertTrue("هندلر نیست: $it", mock.contains(it)) }
        assertTrue("snapshot ماک باید والد بدهد", mock.contains("\"parent_id\": c.get(\"parent_id\")"))
    }

    @Test
    fun pickerModuleStaysPure() {
        val site = src("site/src/bankpick.js")
        listOf("fetch(", "eval(", "new Function(", "XMLHttpRequest", "document.").forEach {
            assertFalse("پیمانه باید خالص بماند: $it", site.contains(it))
        }
        val app = src("app/src/main/java/ir/exam/app/domain/bank/BankPicker.kt")
        listOf("SupabaseProvider", "android.", "System.currentTimeMillis").forEach {
            assertFalse("پیمانه باید خالص بماند: $it", app.contains(it))
        }
    }

    // ------------------------------------------------------------------
    // ۶) V358.1 — پوشش آزمون محلی
    //    در V358 تنها ۱۲۸ آزمون از ۱۶۵ دستی انتخاب و اجرا شد و یک
    //    آزمونِ اسکن منبع از قلم افتاد و CI را شکست. انتخاب دستی،
    //    خودِ نقص بود؛ حالا اجراکننده همهٔ آزمون‌های خالص را می‌یابد.
    // ------------------------------------------------------------------

    @Test
    fun localTestRunnerDiscoversTestsInsteadOfHandPicking() {
        val runner = src("scripts/run_local_kotlin_tests.py")
        assertTrue("باید آزمون‌ها را کشف کند", runner.contains("test_base.rglob(\"*.kt\")"))
        assertTrue("باید وابستگی اندرویدی را تشخیص دهد", runner.contains("androidx\\."))
        assertTrue("پلاگین serialization باید اعمال شود", runner.contains("-Xplugin="))
        assertTrue("موارد اجرا‌نشده باید گزارش شوند", runner.contains("نیازمند CI"))
        assertTrue("شکست واقعی باید از محدودیت ابزار جدا شود", runner.contains("شکست واقعی"))
        // فهرست دستیِ کلاس‌ها نباید برگردد
        assertFalse("فهرست دستی کلاس آزمون ممنوع", runner.contains("JUnitCore ir.exam.app.V3"))
    }

    @Test
    fun bankFilterLabelDoesNotCollideWithRemovedEditorControl() {
        val builder = src("site/src/builder.js")
        assertTrue("برچسب پالایش باید روشن باشد", builder.contains("پالایش دشواری"))
        val cleanup = src("app/src/test/java/ir/exam/app/V334_0_BuilderCleanupTest.kt")
        assertTrue("مرز پالایش/ویرایش باید پین شود",
            cleanup.contains("bankDifficultyFilterIsBackButPerQuestionEditorStaysRemoved"))
        assertTrue("تصمیم باید مستند شود", cleanup.contains("کاربر صریحاً پالایش دشواری را در بانک خواست"))
    }

    @Test
    fun installerExplainsAlreadyAppliedInsteadOfScaringTheUser() {
        val tpl = src("docs/tools/installer_template.py")
        assertTrue("پیام روشن نیست", tpl.contains("از قبل روی این شاخه نصب شده است"))
        assertTrue("نام نسخه باید گفته شود", tpl.contains("VERSION +"))
        assertTrue("راهنمای انتخاب نصاب درست نیست", tpl.contains("apply_v358_1*"))
        assertTrue("نباید چیزی خراب شده تلقی شود", tpl.contains("چیزی خراب نشد"))
        val sim = src("scripts/sim_installer.py")
        assertTrue("شبیه‌ساز باید رفتار تازه را بپذیرد", sim.contains("modern ="))
        assertTrue("شبیه‌ساز باید رفتار قدیم را هم بپذیرد", sim.contains("legacy ="))
    }

    @Test
    fun versionAndChangelogAgree() {
        val version = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب نسخه نامعتبر: $version", version.matches(Regex("[0-9]+[.][0-9]+[.][0-9]+")))
        val lines = src("text/CHANGELOG_FA.txt").lines()
        listOf(40, 60).forEach { take ->
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertTrue(top.appAffecting)
            assertEquals(version, top.appVersion)
            assertTrue(top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            top.bullets.forEach { assertFalse(it.contains(Regex("\\bV\\d"))) }
        }
        val block = ReleaseNotes.parse(lines).firstOrNull { it.version == "V358" }
        assertTrue("بلوک V358 نیست", block != null)
        assertTrue(block!!.bullets.any { it.contains("بانک") })
    }
}
