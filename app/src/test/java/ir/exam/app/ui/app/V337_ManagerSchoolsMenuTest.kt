package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V337 — کارت «مدارس» کنار «داشبورد» در منوی مدیر، مسیر مدرسه → کلاس → اعضا
 * با RPCهای مدیر، و تازه‌سازی هویت بخش کارنامه هنگام کلیک «مشاهده کارنامه مدرسه».
 */
class V337_ManagerSchoolsMenuTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/admin.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `manager hamburger has schools featured card beside dashboard`() {
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("fun openManagerSchools()" in app)
        assertTrue("schoolLaunchAction = SchoolLaunchAction.SHOW_SCHOOLS" in app)
        assertTrue("onManagerSchools = ::openManagerSchools" in app)
        assertTrue("onManagerSchools: () -> Unit = {}" in app)
        assertTrue("val featuredSchoolsCard = if (user.role == UserRole.MANAGER) {" in app)
        assertTrue("\"مدارس\", \"فهرست مدارس و کلاس‌ها\", Design69Icons.SchoolAdd," in app)
        assertTrue("featuredCard2 = featuredSchoolsCard" in app)

        val menu = src("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        assertTrue("featuredCard2: Design69MenuCard? = null," in menu)
        assertTrue("val featuredCards = listOfNotNull(featuredCard, featuredCard2)" in menu)

        val rail = src("app/src/main/java/ir/exam/app/ui/app/TabletDesktopShell.kt")
        assertTrue("featuredCard2: Design69MenuCard? = null," in rail)
        assertTrue("featuredCard2?.let { RailRow(it.title, it.icon, selected = it.selected, onClick = it.onClick) }" in rail)
    }

    @Test
    fun `manager school class roster uses manager RPCs and membership-only delete`() {
        val school = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("var managerRosterClass by remember { mutableStateOf<SchoolClass?>(null) }" in school)
        assertTrue("ManagerSchoolClassRosterScreen(" in school)
        assertTrue("if (managerTeacherPicker) managerRosterClass = it" in school)

        val roster = src("app/src/main/java/ir/exam/app/ui/manager/ManagerSchoolClassRosterScreen.kt")
        assertTrue("internal fun ManagerSchoolClassRosterScreen(" in roster)
        assertTrue("repository.classRoster(schoolClass.id)" in roster)
        assertTrue("repository.schoolStudents()" in roster)
        assertTrue("repository.setClassStudent(schoolClass.id, studentId, add)" in roster)
        assertTrue("حذف از کلاس" in roster)
        assertTrue("حساب دانش‌آموز حذف نمی‌شود." in roster)

        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("native_manager_class_roster_v40c" in repo)
        assertTrue("native_manager_school_students_v40c" in repo)
        assertTrue("native_manager_set_class_student_v40c" in repo)
    }

    @Test
    fun `manager report button swaps dashboard composition to report section`() {
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("import androidx.compose.runtime.key" in app)
        assertTrue("else -> key(managerCardsSection) { ManagerStatsScreen(" in app)
        assertTrue("onOpenReport = { managerCardsSection = \"report\" }" in app)

        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("onOpenReport?.let { QuickPanelCard(\"مشاهده کارنامه مدرسه\", it, Modifier.fillMaxWidth()) }" in manager)
        assertTrue("Text(if (reportMode) \"کارنامه مدرسه\" else \"داشبورد\", style = MaterialTheme.typography.headlineSmall)" in manager)
    }

    @Test
    fun `site mirrors schools card and school-scoped roster navigation`() {
        val mobile = src("site/src/mobile.js")
        assertTrue("m-featured m-featured-two" in mobile)
        assertTrue("text: 'مدارس'" in mobile)
        assertTrue("if (view.panel === 'school') sel = (view.arg && view.arg.students) ? 'students' : 'schools';" in mobile)

        val css = src("site/src/site.css")
        assertTrue(".m-featured.m-featured-two .m-tile{width:48%}" in css)

        val appJs = src("site/src/app.js")
        assertTrue("['school', '🏫', 'مدارس', 'فهرست مدارس و کلاس‌ها']" in appJs)

        val admin = src("site/src/admin.js")
        assertTrue("if (arg.schoolId && arg.openClasses) return managerSchoolClassesPage(c, {id: arg.schoolId, name: arg.schoolName || 'مدرسه'});" in admin)
        assertTrue("schoolName: school.name" in admin)
        assertTrue("text: '→ کلاس‌های مدرسه'" in admin)
        assertTrue("managerSchoolClassesPage(c, x); }" in admin)
    }
}
