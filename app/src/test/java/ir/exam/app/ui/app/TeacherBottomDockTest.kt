package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherBottomDockTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").isFile
    }

    @Test
    fun `dock preserves requested rtl five button order and exact icon motion`() {
        val dock = File(root(), "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()
        listOf(
            "TeacherDockAction.MENU",
            "TeacherDockAction.WALLET",
            "TeacherDockAction.CREATE",
            "TeacherDockAction.EXAMS",
            "TeacherDockAction.CARDS"
        ).forEach { assertTrue("missing dock contract $it", it in dock) }
        listOf(
            "Design69MorphingMenuIcon",
            "Design69Icons.Wallet",
            "Design69Icons.Exams",
            "Design69Icons.Cards",
            "rotationY = 180f * wave",
            "rippleProgress.animateTo(1f, tween(520))",
            ".size(44.dp)",
            ".size(58.dp)",
            "if (!expanded)",
            "LayoutDirection.Rtl"
        ).forEach { assertTrue("missing dock behavior $it", it in dock) }
    }

    @Test
    fun `real add and management routes are wired without demo sheet`() {
        val root = root()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val add = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt").readText()
        val cards = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").readText()
        val school = File(root, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val grading = File(root, "app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").readText()

        listOf("onCreateStudent", "onCreateExam", "onCreateClass").forEach {
            assertTrue("quick action not wired: $it", it in add && it in app)
        }
        listOf("onStats", "onQuestionBank", "onGrading", "onPending", "onAnswers").forEach {
            assertTrue("management action not wired: $it", it in cards && it in app)
        }
        assertTrue("SchoolLaunchAction.CREATE_STUDENT" in school)
        assertTrue("SchoolLaunchAction.CREATE_CLASS" in school)
        assertTrue("initialPendingOnly" in grading)
        assertTrue("initialGradedOnly" in grading)
        assertTrue("فقط مانده" in grading)
        assertTrue("فقط پاسخ" in grading)
        assertFalse("legacy management bottom sheet remains", "ModalBottomSheet" in File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText())
    }
}
