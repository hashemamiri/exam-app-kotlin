package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V20InteractionPolishTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt").isFile
    }

    @Test
    fun `quick add positions and horizontal cards match final request`() {
        val root = root()
        val add = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt").readText()
        val cards = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").readText()
        val examTop = add.indexOf("title = primaryTitle")
        val student = add.indexOf("title = \"دانش‌آموز جدید\"")
        assertTrue(examTop >= 0 && student > examTop)
        // V61.7 — چیدمان ضربدری: عمل اصلی در گوشهٔ مربع است، نه ستون وسط.
        assertTrue("targetY = -cornerY" in add.substring(examTop, student))
        assertTrue("Key.DirectionLeft" in cards && "Key.DirectionRight" in cards)
        assertFalse("vertical key navigation returned", "Key.DirectionDown" in cards)
        assertFalse("card drag helper text returned", "بکشید" in cards)
    }

    @Test
    fun `password visibility is available in every password input`() {
        val root = root()
        val mainSources = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        val common = File(root, "app/src/main/java/ir/exam/app/ui/common/PasswordVisibility.kt").readText()
        assertTrue("PasswordVisibilityButton" in mainSources)
        assertTrue("پنهان‌کردن رمز" in common && "نمایش رمز" in common)
        val directPasswordTransformations = Regex("PasswordVisualTransformation\\(\\)").findAll(mainSources).count()
        assertTrue("all direct password fields must use shared visibility helper", directPasswordTransformations == 1)
    }

    @Test
    fun `bulk dialog buttons and predictive formula scroll are finalized`() {
        val root = root()
        val school = File(root, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val formula = File(root, "app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").readText()
        assertFalse("bulk title text returned", "Text(\"افزودن گروهی دانش‌آموز\"" in school)
        assertFalse("obsolete bulk window icon returned", "Text(\"▦\"" in school)
        assertTrue("Color(0xFF25A86B)" in school)
        assertTrue("Color(0xFFE5484D)" in school)
        assertTrue("Text(\"+\"" in school && "Text(\"×\"" in school)
        assertTrue("viewport.width * .14f" in formula)
        assertTrue("viewport.width * .62f" in formula)
    }

    @Test
    fun `builder actions are opposite and question cards toggle with exact scroll`() {
        val root = root()
        val builder = File(root, "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        assertTrue("Alignment.CenterStart" in builder)
        assertTrue("Alignment.CenterEnd" in builder)
        assertTrue("expandedQuestionId = null" in builder)
        assertTrue("scrollQuestionToHeader(index)" in builder)
        assertTrue("listState.animateScrollToItem(questionPrefaceCount + questionIndex, 0)" in builder)
        assertTrue(builder.split("withFrameNanos").size - 1 >= 2)
    }

    @Test
    fun `online exam branding and compact exam cards are present`() {
        val root = root()
        val manifest = File(root, "app/src/main/AndroidManifest.xml").readText()
        val dashboard = File(root, "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt").readText()
        assertTrue("android:label=\"آزمون آنلاین\"" in manifest)
        assertTrue("MaterialTheme.typography.titleSmall" in dashboard)
        assertTrue("maxLines = 1" in dashboard)
        assertFalse("old multi-row subject label remains", "Text(\"درس:" in dashboard)
    }
}
