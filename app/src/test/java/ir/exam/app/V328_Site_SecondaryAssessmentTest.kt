package ir.exam.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V328 / V334.0 — سازگاری کامل بازخوانی، نمایش دانش‌آموز و تصحیح دبیر با حذف فیلدهای شلوغ از سازنده. */
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
        assertTrue("rubric باید از JSON عمومی پنهان شود", """values.remove("rubric")""" in codec)
        assertTrue("گام‌های سؤال باید در JSON ذخیره شوند", "solutionSteps" in codec)
        assertTrue("پاسخ مرحله‌ای جداگانه ثبت نمی‌شود", "StepwiseEssayAnswer" in student)
        assertTrue("اثبات فرض و خواسته و دلیل ساختاریافته ندارد", "proofAssumptions" in student && "requiresReason" in student && "proofPartAnswer" in grading)
        assertTrue("ارجاع به نتیجهٔ گام قبلی باید در Android نمایش داده شود", "ارجاع به نتیجهٔ گام قبل" in student)
        assertTrue("ساخت هندسی باید به تخته برسد", """answerMode == "construct"""" in student)
        assertTrue("بخش‌بندی در آزمون نمایش داده نمی‌شود", "question.sectionTitle" in student)
        assertTrue("زمان مستقل بخش در Android به‌عنوان زمان‌سنج ارائه نشده", "sectionRemainingSeconds" in student && "sectionDurationsMinutes" in student)
        val builderRepository = src("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("بازخوانی زمان بخش در مخزن حفظ می‌شود", "sectionDurations" in builderRepository && "put(" in builderRepository)
        assertFalse("فیلد زمان بخش از رابط سازندهٔ Android حذف شده است", "sectionDurations" in builder)
        assertTrue("معیار تصحیح به دبیر نمی‌رسد", "question.rubric" in grading)
        assertFalse("فیلد برچسب فصل/مبحث از کارت سازندهٔ Android حذف شده است", "فصل یا مبحث (بانک)" in builder)
        assertFalse("فیلتر فصل/مبحث از پنجرهٔ بانک Android حذف شده است", "tagTopic" in builder)
    }

    @Test
    fun webBuilderAndStudentKeepPrivateRubricAndSharedSections() {
        val builder = src("site/src/builder.js")
        val codec = src("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt")
        val student = src("site/src/student.js")
        val admin = src("site/src/admin.js")
        val migration = src("supabase/migrations/20261021_native_section_pick_rules_v328.sql")
        assertTrue("سازگاری بازخوانی متن مشترک در کدک وب حفظ می‌شود", "v.passage = q.passage" in builder)
        assertTrue("rubric از خروجی عمومی حذف می‌شود", "delete v.rubric" in builder)
        assertTrue("دانش‌آموز پله‌های پاسخ را می‌بیند", "q.mode === 'steps'" in student)
        assertTrue("ارجاع به نتیجهٔ گام قبلی باید در وب نمایش داده شود", "ارجاع به نتیجهٔ گام قبل" in student)
        assertTrue("ساخت هندسی در نسخهٔ وب برای آزمون‌های قبلی پابرجاست", "q.mode === 'construct'" in student)
        assertFalse("فیلترهای موضوع/دشواری/هدف یادگیری از پنجرهٔ بانک وب حذف شده‌اند", "ts.value" in builder || "ds.value" in builder || "os.value" in builder)
        assertTrue("rubric در تصحیح وب نمایش داده می‌شود", "q.rubric" in admin)
        assertTrue("تصحیح خودکار نقاط در پنل وب نمایش داده می‌شود", "Math.hypot" in admin && "constructionTargets" in admin)
        assertFalse("سهم تصادفی بر اساس مبحث/بخش از رابط سازندهٔ وب حذف شده است", "'topic:'" in builder || "'section:'" in builder)
        assertTrue("سرور سهم مبحث/بخش را برای آزمون‌های قبلی حفظ می‌کند", "partition by k.rule_key" in migration && "topic:" in migration && "section:" in migration)
        assertTrue("گزینه‌های آزمون در کدک و دانش‌آموز حفظ می‌شوند", "'options', coalesce(v_exam.options, '{}'::jsonb)" in migration && "sectionDurations" in builder && "sectionDurations" in student)
        assertTrue("rubric نباید در payload دانش‌آموز برگردد", "- 'rubric')" in migration)
        val partMigration = src("supabase/migrations/20261021_native_part_grading.sql")
        assertTrue("نمرهٔ هر زیربخش در پایگاه داده ذخیره می‌شود", "part_grades jsonb" in partMigration && "part_grades =" in partMigration)
        assertTrue("بازخورد هر زیربخش در پایگاه داده ذخیره می‌شود", "part_feedback jsonb" in partMigration && "part_feedback =" in partMigration)
        assertTrue("نمرهٔ زیربخش از سقف آن کنترل می‌شود", "v_value > v_max" in partMigration)
        assertTrue("مختصات هدف باید فقط در کلید محرمانه باشد", "constructionTargets" in codec && """values.remove("constructionTargets")""" in codec)
        assertTrue("نقاط سادهٔ رسم هندسی خودکار تصحیح می‌شوند", "v_point_tol" in partMigration && "v_earned_score" in partMigration)
        assertTrue("پاسخ رسم نقطه تعاملی است", "constructionAnswer" in student && "constructionLabels" in student)
        assertTrue("وب گام اثبات را جداگانه نمره‌دهی می‌کند", "stepScoreInputs" in admin && "native_save_grade_parts_v1" in admin)
    }
}
