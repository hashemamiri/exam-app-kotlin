package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V58.1 (پچ ۲ از ۳) — نظارت آزمون و گزارش‌ها برای معلم:
 * ۱) تشخیص تلاش اسکرین‌شات (API 34+ ScreenCaptureCallback؛ خود تصویر با
 *    FLAG_SECURE سیاه می‌ماند) و ضبط صفحه (API 35+) و خروج/بستن برنامه با
 *    lifecycle — همه فقط «ثبت» می‌شوند.
 * ۲) شمارش رویدادها + زمان ورود/خروج + مدت پاسخ‌گویی و بازدید هر سؤال در
 *    ViewModel؛ ارسال فوری با native_monitor_upsert_v1 و همراه ارسال نهایی
 *    در monitor_report داخل p_meta.
 * ۳) دکمهٔ «گزارش‌ها» روی کارت آزمون در تصحیح و نظارت، کنار «ورود به
 *    تصحیح»؛ پنجرهٔ لیست دانش‌آموزان با برچسب‌های فارسی رویدادها.
 * ۴) SQL: جدول native_exam_monitor با RLS مالک + RPCهای upsert/list؛
 *    خواندن فقط برای معلمِ همان آزمون (teacher_id).
 */
class V58_1ExamMonitorReportsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val studentVm by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt") }
    private val studentRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt") }
    private val gradingRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt") }
    private val gradingScreen by lazy { source("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt") }
    private val migration by lazy { source("supabase/migrations/20260825_native_exam_monitor_v58.sql") }
    private val manifest by lazy { source("app/src/main/AndroidManifest.xml") }

    @Test
    fun `security events are detected and counted on the student side`() {
        assertTrue("ScreenCaptureCallback" in student)
        assertTrue("screenshot_attempt" in student)
        assertTrue("SCREEN_RECORDING_STATE_VISIBLE" in student)
        assertTrue("screen_record_attempt" in student)
        assertTrue("app_leave" in student)
        assertTrue("app_close" in student)
        assertTrue("exam_screen_leave" in student)
        assertTrue("DETECT_SCREEN_CAPTURE" in manifest)
        assertTrue("DETECT_SCREEN_RECORDING" in manifest)
        // FLAG_SECURE قبلی حفظ شده — جلوگیری واقعی + ثبت تلاش
        assertTrue("FLAG_SECURE" in student)
    }

    @Test
    fun `view model aggregates events timings and visits per question`() {
        assertTrue("fun recordSecurityEvent(kind: String)" in studentVm)
        assertTrue("questionTimeSpentMs" in studentVm)
        assertTrue("questionVisits" in studentVm)
        assertTrue("entered_at_epoch_ms" in studentVm)
        assertTrue("fun monitorReport()" in studentVm)
        // ثبت فوری روی سرور + همراه ارسال نهایی
        assertTrue("exams.reportMonitor(examId, monitorReport().toString())" in studentVm)
        assertTrue("monitorReportJson = monitorReport().toString()" in studentVm)
    }

    @Test
    fun `monitor report reaches the server in meta and via upsert rpc`() {
        assertTrue("native_monitor_upsert_v1" in studentRepo)
        assertTrue("put(\"monitor_report\", it)" in studentRepo)
        assertTrue("native_monitor_list_v1" in gradingRepo)
    }

    @Test
    fun `teacher sees the reports button next to grading entry`() {
        assertTrue("Text(\"گزارش‌ها\")" in gradingScreen)
        assertTrue("viewModel.openMonitorReports(item.id)" in gradingScreen)
        assertTrue("MonitorReportsDialog(" in gradingScreen)
        // برچسب‌های فارسی رویدادها
        assertTrue("تلاش برای اسکرین‌شات" in gradingScreen)
        assertTrue("تلاش برای ضبط صفحه" in gradingScreen)
        assertTrue("خارج شدن از برنامه" in gradingScreen)
        assertTrue("بستن برنامه" in gradingScreen)
        assertTrue("خارج شدن از صفحه آزمون" in gradingScreen)
        assertTrue("مدت پاسخ‌گویی هر سؤال" in gradingScreen)
    }

    @Test
    fun `sql migration keeps reports owner-only`() {
        assertTrue("create table if not exists public.native_exam_monitor" in migration)
        assertTrue("enable row level security" in migration)
        assertTrue("student_id = auth.uid()" in migration)
        assertTrue("e.teacher_id = v_uid" in migration)
        assertTrue("native_monitor_upsert_v1" in migration)
        assertTrue("native_monitor_list_v1" in migration)
        // ادغام گزارش تا رویدادهای قبلی از بین نروند
        assertTrue("report = public.native_exam_monitor.report || excluded.report" in migration)
        assertFalse("service_role" in migration)
    }
}
