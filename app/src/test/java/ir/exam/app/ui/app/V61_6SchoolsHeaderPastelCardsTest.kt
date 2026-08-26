package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.6 — شش اصلاح درخواستی پس از بیلد موفق V61.5:
 * ۱) نمای مدارس: هدر صفحه «مدرسه من»؛ عنوان داخلی «مدارس من» حذف شد.
 * ۲) دکمهٔ «پیوستن به مدرسه» (معلم) و «ساخت مدرسه جدید» (مدیر) هم‌ردیف
 *    «بازگشت به کلاس‌ها».
 * ۳) تاس فرم دانش‌آموز مثل چشم بدون کادر (IconButton).
 * ۴) دکمهٔ آمار داک مدیر → سه کارت مدارس/کارنامه/وضعیت.
 * ۵) دکمه‌های مخاطبان تقویم در یک سطر وسط‌چین.
 * ۶) رنگ پاستلی اختصاصی هر نوع سؤال در منوی + سازنده و کارت سؤال.
 */
class V61_6SchoolsHeaderPastelCardsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val manager by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val calendar by lazy { source("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt") }
    private val radial by lazy { source("app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val draft by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt") }

    @Test
    fun `schools view header and inline action buttons`() {
        // هدر «مدرسه من» از ExamApp؛ عنوان داخلی حذف شد
        assertTrue("\"مدرسه من\"" in app)
        assertTrue("onSchoolsOpenChanged = { schoolsViewOpen = it }" in app)
        assertFalse("Text(\"مدارس من\"" in school)
        // دکمه‌ها هم‌ردیف بازگشت به کلاس‌ها
        val row = school.substringAfter("// V61.6 — عنوان داخلی «مدارس من» حذف شد")
            .substringBefore("if (schools.isEmpty())")
        assertTrue("Text(\"ساخت مدرسه جدید\")" in row)
        assertTrue("Text(\"پیوستن به مدرسه\")" in row)
        assertTrue("Text(\"بازگشت به کلاس‌ها\")" in row)
        // معلم=پیوستن، مدیر=ساخت
        assertTrue("onJoinSchool = if (!managerTeacherPicker) {" in school)
    }

    @Test
    fun `dice buttons are frameless like the eye`() {
        // هر دو تاس (ویرایش تکی و گروهی) IconButton بدون کادرند
        val edit = school.substringAfter("private fun StudentEditDialog(")
            .substringBefore("private data class BulkStudentDraft")
        val bulk = school.substringAfter("private fun BulkStudentDialog(")
            .substringBefore("internal fun studentClipboardText")
        assertFalse("OutlinedButton" in edit.substringAfter("رمز جدید اختیاری"))
        assertTrue(edit.indexOf("IconButton(\n                                    onClick = { newPassword = generatePassword(10) }") > 0)
        assertFalse("OutlinedButton" in bulk.substringAfter("Text(\"دختر\")"))
    }

    @Test
    fun `manager stats dock opens three cards first`() {
        assertTrue("fun ManagerCardsScreen(" in manager)
        for (needle in listOf("Triple(\"مدارس\"", "Triple(\"کارنامه\"", "Triple(\"وضعیت\"")) {
            assertTrue(needle, needle in manager)
        }
        assertTrue("when (managerCardsSection) {" in app)
        assertTrue("SchoolLaunchAction.SHOW_SCHOOLS" in app && "SHOW_SCHOOLS" in school)
        // بازکردن دوباره از کارت‌ها شروع می‌شود
        assertTrue("managerCardsSection = null" in app)
    }

    @Test
    fun `calendar audience chips are one centered row`() {
        val audience = calendar.substringAfter("// V61.1 — مخاطبان و دکمه‌ها وسط‌چین")
            .substringBefore("if (editor.audience == CalendarAudience.SCHOOLS)")
        assertTrue("Row(" in audience)
        assertTrue("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in audience)
    }

    @Test
    fun `question types carry dedicated pastel colors everywhere`() {
        // نگاشت رنگ‌ها طبق لیست کاربر
        for (needle in listOf(
            "QuestionType.ESSAY -> 0xFFFFD1DC",
            "QuestionType.MULTIPLE_CHOICE -> 0xFFAEC6CF",
            "QuestionType.TRUE_FALSE -> 0xFFB4EEB4",
            "QuestionType.FILL_BLANK -> 0xFFFDFD96",
            "QuestionType.NUMERIC -> 0xFFC3B1E1",
            "QuestionType.MATCHING -> 0xFFFFDAB9"
        )) assertTrue(needle, needle in draft)
        // منوی + با رنگ‌ها؛ نعنایی/لاوندر برای دو عمل غیرسوالی
        assertTrue("QuestionType.ESSAY.pastelColor()" in radial)
        assertTrue("0xFF98FF98" in radial && "0xFFE6E6FA" in radial)
        assertTrue(".background(action.background?.let(::Color) ?: colors.surface)" in radial)
        // کارت سؤال به رنگ نوع خودش
        assertTrue("Color(question.type.pastelColor()).copy(alpha = .38f)" in builder)
    }
}
