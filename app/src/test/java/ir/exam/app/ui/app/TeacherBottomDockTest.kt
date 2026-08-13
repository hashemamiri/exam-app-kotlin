package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherBottomDockTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").isFile
    }

    @Test fun `dock preserves requested rtl five button order and arc actions`() {
        val text = File(root(), "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()
        val markers = listOf(
            "DockItem(\"منو\"",
            "DockItem(\"کیف پول\"",
            "\"افزودن\",",
            "DockItem(\"آزمون‌ها\"",
            "DockItem(\"کارت‌ها\""
        )
        var position = -1
        markers.forEach { marker ->
            val next = text.indexOf(marker, position + 1)
            assertTrue("missing/order: $marker", next > position)
            position = next
        }
        listOf("دانش‌آموز جدید", "آزمون جدید", "کلاس جدید").forEach {
            assertTrue("missing arc action: $it", it in text)
        }
        assertTrue("LayoutDirection.Rtl" in text)
        assertTrue("AnimatedVisibility" in text)
    }

    @Test fun `management cards and real app routes are wired`() {
        val root = root()
        val dock = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val school = File(root, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val grading = File(root, "app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").readText()
        listOf("آمار و گزارش‌ها", "تصحیح", "مانده").forEach {
            assertTrue("missing card: $it", it in dock)
        }
        listOf("onCreateStudent", "onCreateExam", "onCreateClass", "onExams", "onStats", "onPending").forEach {
            assertTrue("route not wired: $it", it in app)
        }
        assertTrue("SchoolLaunchAction.CREATE_STUDENT" in school)
        assertTrue("SchoolLaunchAction.CREATE_CLASS" in school)
        assertTrue("initialPendingOnly" in grading)
        assertTrue("فقط مانده" in grading)
    }
}
