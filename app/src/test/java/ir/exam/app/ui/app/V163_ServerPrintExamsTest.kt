package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V163 — آزمون‌های چاپی روی سرور (print_exams) برای اپ و سایت؛ حذف افزودن گروهی از گوشی؛ داک کوچک‌تر؛ + کلاس مثل اپ. */
class V163_ServerPrintExamsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `migration defines print_exams table and rpcs`() {
        val sql = source("supabase/migrations/20260911_native_print_exams_v163.sql")
        assertTrue("create table if not exists public.print_exams" in sql)
        for (fn in listOf("native_print_exams_list_v163", "native_print_exam_get_v163", "native_print_exam_save_v163", "native_print_exam_delete_v163")) assertTrue(fn, fn in sql)
        assertTrue("1000" in sql && "native_exam_operations" in sql && "data:image" in sql)
    }

    @Test
    fun `app uses server repository instead of SharedPreferences store`() {
        val repo = source("app/src/main/java/ir/exam/app/data/repository/SupabasePrintExamRepository.kt")
        assertTrue("native_print_exam_save_v163" in repo && "native_print_exams_list_v163" in repo && "migrateLegacy" in repo)
        for (f in listOf("ui/app/ExamApp.kt", "ui/builder/ExamBuilderScreen.kt", "ui/dashboard/TeacherDashboardScreen.kt", "ui/printing/ExamPrintCenterScreen.kt")) {
            val s = source("app/src/main/java/ir/exam/app/$f")
            assertFalse(f, "PrintExamStore(" in s)
            assertTrue(f, "SupabasePrintExamRepository" in s)
        }
        assertTrue("fun applyPrintSaved(" in source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt"))
    }

    @Test
    fun `site uses server print exams and mobile polish`() {
        val b = source("site/src/builder.js")
        assertTrue("native_print_exam_save_v163" in b && "migrateLocalPrintExams" in b && "examsite.printexams.migrated.v163" in b)
        val m = source("site/src/mobile.js")
        assertFalse("bulkDialog" in m)
        assertTrue("closeOverlays()" in m && "scrollIntoView" in m && "m-sheet-fixed" in m)
        val css = source("site/src/site.css")
        assertTrue(".m-dock-panel{pointer-events:auto;height:64px" in css && ".m-sheet-fixed{" in css && ".m-roster-plus{" in css)
        assertTrue("m-roster-opt" in source("site/src/school.js"))
    }
}
