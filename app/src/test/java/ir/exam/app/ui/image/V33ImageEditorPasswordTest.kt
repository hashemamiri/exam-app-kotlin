package ir.exam.app.ui.image

import androidx.compose.ui.geometry.Size
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V33 برای stack trace واقعی و اتصال رمز فعلی به حافظهٔ نشست. */
class V33ImageEditorPasswordTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `unspecified compose size becomes readable before first image frame`() {
        assertEquals(Size(1f, 1f), safeImagePixelSize(Size.Unspecified))
        assertEquals(Size(640f, 480f), safeImagePixelSize(Size(640f, 480f)))
        assertEquals(Size(1f, 1f), safeImagePixelSize(Size(0f, 0f)))
    }

    @Test
    fun `editor never reads dimensions directly from unspecified state`() {
        val editor = source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
        assertTrue("safeImagePixelSize(sourcePixels)" in editor)
        assertTrue("if (size == Size.Unspecified) Size(1f, 1f)" in editor)
        assertFalse("sourcePixels.width" in editor)
        assertFalse("sourcePixels.height" in editor)
        assertTrue("safePixels.width" in editor)
        assertTrue("safePixels.height" in editor)
    }

    @Test
    fun `student editor displays the password known in this session`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val edit = school.substringAfter("private fun StudentEditDialog(")
            .substringBefore("private data class BulkStudentDraft")
        assertTrue("currentPassword: String?" in edit)
        assertTrue("value = currentPassword.orEmpty()" in edit)
        assertTrue("passwordTransformation(passwordVisible)" in edit)
        assertTrue("passwordVisible = !passwordVisible" in edit)
        assertFalse("currentPasswordVisible" in edit)
        assertFalse("رمز فعلی hash شده و قابل نمایش نیست" in edit)
        assertFalse("supportingText" in edit)
    }

    @Test
    fun `known session password follows a username-only edit`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("currentPassword = sessionPassword" in school)
        assertTrue("request.newPassword.isNullOrBlank()" in school)
        assertTrue("knownPasswords[request.username.lowercase()] = sessionPassword" in school)
    }
}
