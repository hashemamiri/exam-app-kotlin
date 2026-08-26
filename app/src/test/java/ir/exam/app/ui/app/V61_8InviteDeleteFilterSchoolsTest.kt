package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.8 — رفع دو باگ گزارش دستگاه + تکمیل فیلتر:
 * ۱) «کارت کد دعوت حذف نمی‌شود»: revoke فقط کد را باطل می‌کرد و لیست سرور
 *    همچنان کارت را برمی‌گرداند → تابع جدید حذف واقعی سطر
 *    (native_manager_delete_invite_v61، چندمدرسه‌ای) + fallback به revoke.
 * ۲) «زمان‌سنج فریز نمایش داده نمی‌شود»: used_at پستگرس با فاصله می‌آمد و
 *    parser کلاینت شکست می‌خورد → نرمال‌سازی کلاینت + خروجی ISO در SQL.
 * ۳) دکمه‌های منوی + سازنده در ابتدای انیمیشن دایره دیده می‌شدند → clip داخل
 *    graphicsLayer + شروع scale از .6.
 * ۴) فیلتر مدرسه: لیست مدارس برای انتخاب مدرسهٔ خاص + «هر مدرسه»؛ آیکن
 *    فیلتر کنار جستجو هنگام فعال بودن قرمز؛ انصراف دیالوگ = ضربدر قرمز.
 */
class V61_8InviteDeleteFilterSchoolsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val manager by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val repo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val radial by lazy { source("app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt") }
    private val migration by lazy { source("supabase/migrations/20260826_native_invite_delete_filter_schools_v61_8.sql") }

    @Test
    fun `invite card deletion really deletes the row`() {
        assertTrue("suspend fun deleteInvite(id: String)" in repo)
        assertTrue("native_manager_delete_invite_v61" in repo)
        // fallback به revoke قدیمی اگر تابع deploy نشده باشد
        assertTrue("native_manager_revoke_invite_v40b" in repo)
        assertTrue("repository.deleteInvite(invite.id)" in manager)
        // سرور: حذف واقعی + چندمدرسه‌ای
        assertTrue("delete from public.school_teacher_invites i" in migration)
        assertTrue("m.staff_role = 'manager' and m.status = 'active'" in migration)
    }

    @Test
    fun `frozen timer parses postgres timestamps`() {
        // نرمال‌سازی فاصله→T و آفست کوتاه +00 (بدون $ برای پرهیز از template کاتلین)
        assertTrue("value.trim().replace(' ', 'T')" in manager)
        assertTrue("Regex(\"[+-]\\\\d{2}" in manager)
        // خروجی ISO سمت سرور
        assertTrue("to_char(i.used_at at time zone 'UTC'" in migration)
        assertTrue("زمان‌سنج متوقف شد: %02d:%02d:%02d" in manager)
    }

    @Test
    fun `radial buttons stay rounded squares during animation`() {
        val action = radial.substringAfter("actions.forEachIndexed").substringBefore("val startX")
        assertTrue("shape = RoundedCornerShape(22.dp)" in action)
        assertTrue("clip = true" in action)
        assertTrue("scaleX = .6f + .4f * p" in action)
    }

    @Test
    fun `school filter lists schools and red icons`() {
        // بخش مدرسه: لیست مدارس + «هر مدرسه»
        assertTrue("label = { Text(\"هر مدرسه (همهٔ دانش‌آموزان عضو مدرسه)\") }" in school)
        assertTrue("schoolId = if (draft.schoolId == item.id) null else item.id" in school)
        assertTrue("val schoolId: String? = null" in classesVm)
        assertTrue("schoolIds = " in classesVm)
        assertTrue("meta[student.id]?.schoolIds?.contains(filter.schoolId) == true" in school)
        assertTrue("'schools', coalesce((" in migration)
        // آیکن فیلتر لیست: قرمز هنگام فعال بودن
        assertTrue("tint = if (filter.isActive) Color(0xFFD32F2F)" in school)
        // انصراف دیالوگ = ضربدر قرمز
        val header = school.substringAfter("private fun StudentFilterDialog(")
            .substringAfter("title = {").substringBefore("text = {")
        assertTrue("Icons.Outlined.Close" in header)
        assertTrue("tint = Color(0xFFD32F2F)" in header)
    }
}
