package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V59.2 — هفت گزارش/درخواست دستگاه:
 * ۱) خطای «عملیات ناشناخته» حذف حساب: نسخهٔ سرور manage-student قدیمی است؛
 *    کلاینت حالا JSON خام خطا را تمیز می‌کند و راهنمای deploy می‌دهد.
 * ۲) جملهٔ «هزینه هر سؤال مشمول...» حذف شد.
 * ۳) «آزمون نیمه‌کاره»: پس از خروج از صفحهٔ آزمون، کارت «پیوستن به آزمون»
 *    در داشبورد (resumableExamAvailable + rejoinActiveExam)؛ پس از kill
 *    برنامه هم restore خودکار قبلی برقرار است.
 * ۴) آفلاین: مسیر صف WorkManager از قبل بود (enqueueSubmission + Worker با
 *    NetworkType.CONNECTED که با بسته‌بودن برنامه هم اجرا می‌شود) — تست قفل.
 * ۵) باگ ندیدن پیام تقویم توسط دانش‌آموز: cal_month فقط مالک
 *    (profiles.teacher_id) را می‌دید؛ حالا معلم‌های لینک‌شده
 *    (teacher_student_links) هم شامل‌اند.
 * ۶) قفل گذشتهٔ تقویم: ساخت/ویرایش پیام فقط امروز/آینده؛ حذف آزاد
 *    (سرور + UI).
 * ۷) بنر «پیام جدید دارید» در داشبورد دانش‌آموز؛ لمس → دیالوگ پیام +
 *    علامت دیده‌شدن (cal_unseen_v59 / cal_mark_seen_v59).
 */
class V59_2CalendarNotifyFixesTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val profileRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val studentVm by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt") }
    private val home by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt") }
    private val calendarScreen by lazy { source("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt") }
    private val calendarRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseCalendarRepository.kt") }
    private val migration by lazy { source("supabase/migrations/20260825_native_calendar_notify_v59.sql") }
    private val scheduler by lazy { source("app/src/main/java/ir/exam/app/data/work/PendingActionScheduler.kt") }
    private val queued by lazy { source("app/src/main/java/ir/exam/app/data/repository/QueuedExamRepository.kt") }

    @Test
    fun `delete account errors are humanized and stale server is explained`() {
        assertTrue("نسخهٔ سرور به‌روز نیست" in profileRepo)
        assertTrue("عملیات ناشناخته" in profileRepo)
        // استخراج پیام از JSON خام خطای Edge
        assertTrue("Regex(\"\\\"error\\\"" in profileRepo)
    }

    @Test
    fun `the per-question cost sentence is gone`() {
        assertFalse("هزینه هر سؤال مشمول" in builder)
    }

    @Test
    fun `students can rejoin a half-finished exam from the dashboard`() {
        assertTrue("val resumableExamAvailable: Boolean = false" in studentVm)
        assertTrue("resumableExamAvailable = true" in studentVm)
        assertTrue("fun rejoinActiveExam()" in studentVm)
        assertTrue("آزمون نیمه‌تمام دارید" in home)
        assertTrue("پیوستن به آزمون" in home)
        assertTrue("onClick = viewModel::rejoinActiveExam" in home)
    }

    @Test
    fun `offline submissions stay queued and flush even when the app is closed`() {
        // آفلاین → صف؛ WorkManager با محدودیت شبکه حتی بعد از بستن برنامه اجرا می‌شود.
        assertTrue("SubmissionOutcome.Queued(pending.enqueueSubmission(payload))" in queued)
        assertTrue("NetworkType.CONNECTED" in scheduler)
    }

    @Test
    fun `linked students now receive their teachers calendar notes`() {
        val calMonth = migration.substringAfter("create or replace function public.cal_month")
            .substringBefore("create or replace function public.cal_save_note")
        assertTrue("n.teacher_id = v_teacher" in calMonth)
        assertTrue("from public.teacher_student_links l" in calMonth)
        assertTrue("l.student_id = v_uid and l.teacher_id = n.teacher_id" in calMonth)
    }

    @Test
    fun `past days are create and edit locked but deletable`() {
        // سرور
        assertTrue("if p_date < current_date then" in migration)
        assertTrue("برای روزهای گذشته فقط حذف پیام ممکن است" in migration)
        // UI: دکمهٔ پیام و آیکن ویرایش برای گذشته مخفی؛ حذف می‌ماند
        assertTrue("val dayIsPast = day != null &&" in calendarScreen)
        assertTrue("if (isTeacher && day != null && !dayIsPast) {" in calendarScreen)
        assertTrue("if (!dayIsPast) IconButton(onClick = { onEdit(note) }" in calendarScreen)
        assertTrue("حذف پیام" in calendarScreen)
    }

    @Test
    fun `students get a new-message banner that opens and marks seen`() {
        assertTrue("cal_unseen_v59" in migration)
        assertTrue("cal_mark_seen_v59" in migration)
        assertTrue("native_calendar_seen" in migration)
        assertTrue("student_id = auth.uid()" in migration)
        assertTrue("suspend fun unseenNotes(): Result<List<CalendarNote>>" in calendarRepo)
        assertTrue("suspend fun markSeen(noteId: String): Result<Unit>" in calendarRepo)
        assertTrue("پیام جدید دارید" in home)
        assertTrue("openedNote = unseenNotes.firstOrNull()" in home)
        assertTrue("calendarRepo.markSeen(id)" in home)
        assertTrue("Text(\"خواندم\")" in home)
    }
}
