package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V328 — آزمون‌های متوسطه: بخش مشترک، گام‌های پاسخ، ساخت، rubric و بانک مبحثی در دو رابط. */
class V328_Site_SecondaryAssessmentTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun appStoresRubricPrivatelyAndShowsSeparateStepFields() {
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt")
        val student = src("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        val builder = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val grading = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("rubric باید از JSON عمومی پنهان شود", "values.remove(\"rubric\")" in codec)
        assertTrue("گام‌های سؤال باید در JSON ذخیره شوند", "solutionSteps" in codec)
        assertTrue("پاسخ مرحله‌ای جداگانه ثبت نمی‌شود", "StepwiseEssayAnswer" in student)
        assertTrue("اثبات فرض و خواسته و دلیل ساختاریافته ندارد", "proofAssumptions" in student && "requiresReason" in student && "proofPartAnswer" in grading)
        assertTrue("ارجاع به نتیجهٔ گام قبلی باید در Android نمایش داده شود", "ارجاع به نتیجهٔ گام قبل" in student)
        assertTrue("ساخت هندسی باید به تخته برسد", "answerMode == \"construct\"" in student)
        assertTrue("بخش‌بندی در آزمون نمایش داده نمی‌شود", "question.sectionTitle" in student)
        assertTrue("زمان مستقل بخش در Android به‌عنوان زمان‌سنج ارائه نشده", "sectionRemainingSeconds" in student && "sectionDurationsMinutes" in student)
        val builderRepository = src("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("تنظیم زمان بخش در سازندهٔ Android ذخیره نمی‌شود", "sectionDurations" in builder && "sectionDurations" in builderRepository && "put(" in builderRepository)
        assertTrue("معیار تصحیح به دبیر نمی‌رسد", "question.rubric" in grading)
        assertTrue("فیلد برچسب فصل/مبحث نیست", "فصل یا مبحث (بانک)" in builder)
        assertTrue("فیلتر بانک Android فصل/مبحث ندارد", "tagTopic" in builder && "item.question.topic.equals(tagTopic" in builder)
    }

    @Test
    fun webBuilderAndStudentKeepPrivateRubricAndSharedSections() {
        val builder = src("site/src/builder.js")
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt")
        val student = src("site/src/student.js")
        val admin = src("site/src/admin.js")
        val migration = src("supabase/migrations/20261021_native_section_pick_rules_v328.sql")
        assertTrue("پرسش چندبخشی ذخیره نمی‌شود", "v.passage = q.passage" in builder)
        assertTrue("rubric از کلید پاسخ جدا نشده", "'rubric'" in builder)
        assertTrue("دانش‌آموز پله‌های پاسخ را نمی‌بیند", "q.mode === 'steps'" in student)
        assertTrue("ارجاع به نتیجهٔ گام قبلی باید در وب نمایش داده شود", "ارجاع به نتیجهٔ گام قبل" in student)
        assertTrue("ساخت هندسی در نسخهٔ وب نیست", "q.mode === 'construct'" in student)
        assertTrue("بانک سؤال بر اساس مبحث جست‌وجو نمی‌شود", "qq.topic" in builder)
        assertTrue("فیلترهای موضوع/دشواری/هدف یادگیری وب به کار نرفته‌اند", "!ts.value || String(qq.topic" in builder && "ds.value" in builder && "os.value" in builder)
        assertTrue("rubric در تصحیح وب نمایش داده نمی‌شود", "q.rubric" in admin)
        assertTrue("تصحیح خودکار نقاط در پنل وب نمایش داده نمی‌شود", "Math.hypot" in admin && "constructionTargets" in admin)
        assertTrue("سهم تصادفی بر اساس مبحث/بخش در UI نیست", "topic:" in builder && "section:" in builder)
        assertTrue("سرور سهم مبحث/بخش را اعمال نمی‌کند", "partition by k.rule_key" in migration && "topic:" in migration && "section:" in migration)
        assertTrue("گزینه‌های آزمون (از جمله تنظیم زمان بخش) به دانش‌آموز نمی‌رسند", "'options', coalesce(v_exam.options, '{}'::jsonb)" in migration && "sectionDurations" in builder && "sectionDurations" in src("site/src/student.js"))
        assertTrue("rubric نباید در payload دانش‌آموز برگردد", "- 'rubric')" in migration)
        val partMigration = src("supabase/migrations/20261021_native_part_grading.sql")
        assertTrue("نمرهٔ هر زیربخش در پایگاه داده ذخیره نمی‌شود", "part_grades jsonb" in partMigration && "part_grades =" in partMigration)
        assertTrue("بازخورد هر زیربخش در پایگاه داده ذخیره نمی‌شود", "part_feedback jsonb" in partMigration && "part_feedback =" in partMigration)
        assertTrue("نمرهٔ زیربخش از سقف آن کنترل نمی‌شود", "v_value > v_max" in partMigration)
        assertTrue("مختصات هدف باید فقط در کلید محرمانه باشد", "constructionTargets" in codec && "values.remove(\"constructionTargets\")" in codec)
        assertTrue("نقاط سادهٔ رسم هندسی خودکار تصحیح نمی‌شوند", "v_point_tol" in partMigration && "v_earned_score" in partMigration)
        assertTrue("پاسخ رسم نقطه تعاملی نیست", "constructionAnswer" in student && "constructionLabels" in student)
        assertTrue("وب گام اثبات را جداگانه نمره‌دهی نمی‌کند", "stepScoreInputs" in admin && "native_save_grade_parts_v1" in admin)
    }
}
