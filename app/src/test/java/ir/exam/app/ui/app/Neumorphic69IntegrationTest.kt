package ir.exam.app.ui.app

import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.NeumorphicPalette
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Neumorphic69IntegrationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt").isFile
    }

    @Test
    fun `dock contracts keep rtl real-action order`() {
        assertEquals(
            listOf(
                TeacherDockAction.MENU,
                TeacherDockAction.WALLET,
                TeacherDockAction.CREATE,
                TeacherDockAction.EXAMS,
                TeacherDockAction.CARDS
            ),
            TeacherDockContract.order
        )
        assertEquals(
            listOf(
                TeacherQuickCreateAction.STUDENT,
                TeacherQuickCreateAction.EXAM,
                TeacherQuickCreateAction.CLASS
            ),
            TeacherDockContract.quickCreateOrder
        )
        assertEquals(
            listOf(
                TeacherManagementAction.STATS,
                TeacherManagementAction.GRADING,
                TeacherManagementAction.PENDING
            ),
            TeacherDockContract.managementOrder
        )
    }

    @Test
    fun `appearance defaults and bounds match reference design`() {
        val settings = AppearanceSettings()
        assertEquals(NeumorphicPalette.INDIGO_MINT, settings.neumorphicPalette)
        assertEquals(14f, settings.neumorphicDepth)
        assertEquals(8f, AppearancePreferences.MIN_NEO_DEPTH)
        assertEquals(22f, AppearancePreferences.MAX_NEO_DEPTH)
        assertEquals(4, NeumorphicPalette.values().size)
    }

    @Test
    fun `native shell uses dual shadows without importing demo screens`() {
        val root = root()
        val design = File(root, "app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt").readText()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val dock = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()
        val mainSources = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        listOf("setShadowLayer", "lightShadow", "darkShadow", "pressed", "NeumorphicTopBar").forEach {
            assertTrue("missing native design marker: $it", it in design)
        }
        assertTrue("Neumorphic69Provider(depth = appearance.neumorphicDepth)" in app)
        assertTrue("TeacherBottomDock(" in app)
        assertTrue("ModalBottomSheet" in dock)
        assertFalse("standalone demo package must not enter runtime", "com.example.neumorphic69" in mainSources)
        assertFalse("standalone fake wallet balance must not enter runtime", "۱۲٬۴۸۰٬۰۰۰" in mainSources)
    }

    @Test
    fun `reference font weights are copied exactly`() {
        val root = root()
        val expected = mapOf(
            "vazirmatn_medium.ttf" to "b986623e4ddef10755e04be39f8ea7bcb1dc08bfe8dd0aa6af395736f256ad4a",
            "vazirmatn_bold.ttf" to "f635fdbea28f265de395ba83b4b1570dcf2f58d13c65469e61903b1c2d2ae723"
        )
        expected.forEach { (name, hash) ->
            val file = File(root, "app/src/main/res/font/$name")
            assertTrue("missing $name", file.isFile)
            assertEquals(hash, sha256(file))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
