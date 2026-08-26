package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.9 — سه درخواست:
 * ۱) آیکن‌های حرفه‌ای پنجرهٔ +: نشان افزودن یکدست (addBadge) + آیکن‌های جدید
 *    SchoolAdd (ساختمان مدرسه) و TeacherInvite (معلم + پاکت دعوت).
 * ۲) پنل مدیر پیش‌فرض داشبورد؛ دکمهٔ آمار داک پشتهٔ کارتی مثل معلم با
 *    کارت‌های مدارس/کارنامه/وضعیت (ManagerManagementCardsScreen).
 * ۳) فیلتر: بخش مدرسه فقط لیست مدارس (حذف «هر مدرسه») و «عضو نشده» آخر.
 */
class V61_9IconsDashboardCardsFilterTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val icons by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt") }
    private val add by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt") }
    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val stack by lazy { source("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }

    @Test
    fun `quick add icons are professional with shared add badge`() {
        // نشان افزودن مشترک + دو آیکن جدید
        assertTrue("private fun PathBuilder.addBadge(" in icons)
        assertTrue("val SchoolAdd: ImageVector by lazy" in icons)
        assertTrue("val TeacherInvite: ImageVector by lazy" in icons)
        // هر سه آیکن سؤال/دانش‌آموز/کلاس از نشان مشترک استفاده می‌کنند
        assertTrue(icons.split("addBadge(").size - 1 >= 5)
        // اتصال: مدرسه جدید و دعوت معلم
        assertTrue("icon = Design69Icons.SchoolAdd" in add)
        assertTrue("Design69Icons.TeacherInvite else Design69Icons.ExamAdd" in app)
    }

    @Test
    fun `manager defaults to dashboard and stats opens teacher-style cards`() {
        // پیش‌فرض: صفحهٔ CARDS با بخش status (داشبورد)
        assertTrue("mutableStateOf(if (user.role == UserRole.MANAGER) MainPage.CARDS else MainPage.CALENDAR)" in app)
        assertTrue("mutableStateOf<String?>(\"status\")" in app)
        // کارت منوی داشبورد مستقیم status را باز می‌کند
        assertTrue("fun openManagerDashboard()" in app)
        assertTrue("onClick = { select(onManagerDashboard) }" in app)
        // پشتهٔ کارتی مشترک با سه کارت مدیر
        assertTrue("fun ManagerManagementCardsScreen(" in stack)
        assertTrue("private fun ManagementCardsStack(" in stack)
        assertTrue("null -> ManagerManagementCardsScreen(" in app)
        assertTrue("cycleKey = cardsCycleKey," in app)
        // ManagerCardsScreen سادهٔ قدیمی حذف شد
        assertFalse("fun ManagerCardsScreen(" in source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt"))
    }

    @Test
    fun `filter school section lists only schools and unassigned is last`() {
        val dialog = school.substringAfter("private fun StudentFilterDialog(")
            .substringBefore("private fun StudentCard(")
        assertFalse("هر مدرسه" in dialog)
        // ترتیب: عضو نشده بعد از مدرسه و معلم
        val schoolKey = dialog.indexOf("key = \"school\"")
        val teacherKey = dialog.indexOf("key = \"teacher\"")
        val unassignedKey = dialog.indexOf("key = \"unassigned\"")
        assertTrue(schoolKey in 0 until teacherKey && teacherKey < unassignedKey)
    }
}
