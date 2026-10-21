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
        assertTrue("ساخت هندسی باید به تخته برسد", "answerMode == \"construct\"" in student)
        assertTrue("بخش‌بندی در آزمون نمایش داده نمی‌شود", "question.sectionTitle" in student)
        assertTrue("معیار تصحیح به دبیر نمی‌رسد", "question.rubric" in grading)
        assertTrue("فیلد برچسب فصل/مبحث نیست", "فصل یا مبحث (بانک)" in builder)
    }

    @Test
    fun webBuilderAndStudentKeepPrivateRubricAndSharedSections() {
        val builder = src("site/src/builder.js")
        val student = src("site/src/student.js")
        val admin = src("site/src/admin.js")
        val migration = src("supabase/migrations/20261021_native_section_pick_rules_v328.sql")
        assertTrue("پرسش چندبخشی ذخیره نمی‌شود", "v.passage = q.passage" in builder)
        assertTrue("rubric از کلید پاسخ جدا نشده", "'rubric'" in builder)
        assertTrue("دانش‌آموز پله‌های پاسخ را نمی‌بیند", "q.mode === 'steps'" in student)
        assertTrue("ساخت هندسی در نسخهٔ وب نیست", "q.mode === 'construct'" in student)
        assertTrue("بانک سؤال بر اساس مبحث جست‌وجو نمی‌شود", "qq.topic" in builder)
        assertTrue("rubric در تصحیح وب نمایش داده نمی‌شود", "q.rubric" in admin)
        assertTrue("سهم تصادفی بر اساس مبحث/بخش در UI نیست", "topic:" in builder && "section:" in builder)
        assertTrue("سرور سهم مبحث/بخش را اعمال نمی‌کند", "partition by k.rule_key" in migration && "topic:" in migration && "section:" in migration)
        assertTrue("rubric نباید در payload دانش‌آموز برگردد", "- 'rubric')" in migration)
    }
}
