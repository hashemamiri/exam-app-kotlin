package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.7 — بستهٔ درخواست‌های کاربر:
 * ۱) چشم کارت سؤال فقط «پیش‌نمایش دانش‌آموزی» را باز کند (منوی قبلی حذف).
 * ۲) دکمه‌های کارت آزمون: سطر ۱ وسط‌چین (ویرایش/بازکردن-بستن/سطل حذف)،
 *    سطر ۲ وسط‌چین (تکثیر با کسر هزینه/صادرکردن)؛ چاپ‌ها فقط در «چاپ آزمون».
 * ۳) کارت «چاپ آزمون» جایگزین کارت سربرگ منوی معلم؛ دکمهٔ وسط‌چین «سربرگ»
 *    بالای لیست، پنجرهٔ سربرگ رسمی (استان/شهر/منطقه/مدرسه از مدارس عضو یا
 *    سایر/پایه و رشته با چرخ فرم دانش‌آموز/نام درس/تاریخ شمسی/مدت) با
 *    پیش‌نمایش سربرگ کامل.
 * ۴) سربرگ چاپ ۵ سطری سه‌ستونه با آرم (مطابق تصویر کاربر) و قالب ثابت.
 * ۵) دانش‌آموز جدید مدیر: اول انتخاب معلم و کلاس، بعد فرم؛ + کنار جستجو
 *    همان جریان؛ افزودن به کلاس معلم با RPC مدیر.
 * ۶) رفع خطای «more than one row returned by a subquery» داشبورد مدیر
 *    (SQL چندمدرسه‌ای V62.7).
 */
class V62_7PrintCenterStudentPreviewTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val preview by lazy { source("app/src/main/java/ir/exam/app/ui/builder/StudentQuestionPreview.kt") }
    private val dashboard by lazy { source("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt") }
    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val sql by lazy { source("supabase/migrations/20260827_native_manager_summary_multischool_v62_7.sql") }

    @Test
    fun `the eye opens only the student-view preview`() {
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue("onStudentPreview" in editor)
        assertTrue("پیش‌نمایش دانش‌آموزی سؤال" in editor)
        // منوی چندگزینه‌ای قبلی از چشم حذف شد
        assertFalse("DropdownMenu(" in editor)
        // دیالوگ پیش‌نمایش دانش‌آموزی همهٔ انواع سؤال را دارد
        assertTrue("fun StudentQuestionPreviewDialog(" in preview)
        for (needle in listOf("QuestionType.MULTIPLE_CHOICE", "QuestionType.TRUE_FALSE",
            "QuestionType.FILL_BLANK", "QuestionType.NUMERIC", "QuestionType.MATCHING", "QuestionType.ESSAY"))
            assertTrue(needle, needle in preview)
        assertTrue("StudentQuestionPreviewDialog(" in builder)
    }

    @Test
    fun `exam card buttons are two centered rows without print actions`() {
        val card = dashboard.substringAfter("expandedExamId == exam.id")
            .substringBefore("deleteCandidate?.let")
        assertTrue("Alignment.CenterHorizontally" in card)
        assertTrue("Text(\"ویرایش\")" in card)
        assertTrue("if (exam.isOpen) \"بستن\" else \"بازکردن\"" in card)
        assertTrue("contentDescription = \"حذف آزمون\"" in card)
        assertTrue("Text(\"تکثیر با کسر هزینه\")" in card)
        assertTrue("Text(\"صادرکردن\")" in card)
        // چاپ‌ها از کارت آزمون حذف و فقط در صفحهٔ چاپ آزمون هستند
        assertFalse("چاپ برگه" in card)
        assertFalse("چاپ با کلید" in card)
        assertTrue("Text(\"چاپ برگه\")" in printCenter)
        assertTrue("Text(\"چاپ با کلید\")" in printCenter)
    }

    @Test
    fun `print center replaces the header card with a centered header dialog`() {
        // کارت منو: چاپ آزمون به‌جای سربرگ؛ صفحهٔ PRINT
        assertTrue("\"چاپ آزمون\", \"اطلاعات رسمی چاپ آزمون\"" in appShell)
        assertTrue("MainPage.PRINT" in appShell)
        // V63.0.1 — صفحهٔ چاپ حالا پارامتر مداد ویرایش سند دارد.
        assertTrue("ExamPrintCenterScreen(" in appShell)
        // دکمهٔ سربرگ وسط‌چین مثل مشخصات آزمون
        assertTrue("horizontalArrangement = Arrangement.Center" in printCenter)
        assertTrue("if (headerOpen) \"بستن سربرگ\" else \"سربرگ\"" in printCenter)
        // فرم سربرگ: مدرسه از مدارس عضو یا سایر، پایه/رشته با چرخ، تاریخ شمسی
        for (needle in listOf("GradeOdometerPicker(", "FieldOfStudyPicker(",
            "JalaliDateTimeField(", "Text(\"سایر\")", "native_teacher_schools_v61",
            "Text(\"نام درس\")", "Text(\"مدت امتحان\")", "پیش‌نمایش سربرگ"))
            assertTrue(needle, needle in printCenter)
        // پیش‌نمایش همان ۵ سطر چاپ را دارد
        assertTrue("fun HeaderPreview(" in printCenter)
        assertTrue("وزارت آموزش و پرورش جمهوری اسلامی ایران" in printCenter)
    }

    @Test
    fun `official pdf header follows the five-row three-column layout with the emblem`() {
        assertTrue("print/emblem.png" in pdfAdapter)
        assertTrue("drawHeaderCell(" in pdfAdapter)
        // سه ستون با عرض ثابت + برش تا قالب بهم نریزد
        assertTrue("TextUtils.ellipsize" in pdfAdapter)
        for (needle in listOf(
            "وزارت آموزش و پرورش جمهوری اسلامی ایران",
            "اداره کل آموزش و پرورش استان \${header.province}",
            "مدیریت آموزش و پرورش شهر/شهرستان \${header.city}",
            "تاریخ آزمون: \${header.examDate}",
            // V62.8.2 — مدت حالا با پسوند «دقیقه» ساخته می‌شود (الحاق رشته).
            "\"مدت آزمون: \" + header.examDuration",
            "پایه: \${header.grade}"
        )) assertTrue(needle, needle in pdfAdapter)
        // سربرگ override از صفحهٔ چاپ به مسیر چاپ می‌رسد
        val portability = source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt")
        assertTrue("headerOverride: OfficialPrintHeader? = null" in portability)
        assertTrue("headerOverride ?:" in portability)
    }

    @Test
    fun `manager student creation picks teacher and class first everywhere`() {
        // داک + و دکمهٔ + کنار جستجو هر دو اول پنجرهٔ انتخاب معلم/کلاس
        assertTrue("managerCreatePickerOpen = true" in school)
        assertTrue("Text(\"انتخاب معلم و کلاس\")" in school)
        // V62.8.2 — دکمه حالا شرطی است («ادامه و ساخت» یا «ساخت بدون کلاس»).
        assertTrue("ادامه و ساخت دانش‌آموز" in school)
        assertTrue("teacherClassesForPicker" in classesVm)
        // ساخت + عضویت کلاس معلم با RPC مدیر؛ دانش‌آموز به لیست هم اضافه می‌شود
        assertTrue("fun createStudentsBulkForManagerClass(" in classesVm)
        assertTrue("native_manager_set_class_student_v40c" in classesVm)
    }

    @Test
    fun `manager summary sql aggregates all schools`() {
        assertTrue(sql == source("sql/manual/SQL_NATIVE_MANAGER_SUMMARY_MULTISCHOOL_V62_7.sql"))
        assertTrue("more than one row" in sql) // توضیح ریشه در کامنت
        assertTrue("school_id in(select school_id from mine)" in sql)
        assertFalse("from mine join public.schools s on s.id=mine.school_id" in sql)
    }
}
