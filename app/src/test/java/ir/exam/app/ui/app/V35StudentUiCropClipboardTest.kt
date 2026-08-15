package ir.exam.app.ui.app

import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.ui.classes.studentClipboardText
import ir.exam.app.ui.image.CropEdgeKind
import ir.exam.app.ui.image.CropGeometry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V35 برای پنجره‌های دانش‌آموز، clipboard و حرکت/resize کادر crop. */
class V35StudentUiCropClipboardTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }

    @Test
    fun `selected girl is pink and selected boy is blue in edit and bulk`() {
        val edit = school.substringAfter("private fun StudentEditDialog(")
            .substringBefore("private data class BulkStudentDraft")
        val bulk = school.substringAfter("private fun BulkStudentDialog(")
            .substringBefore("internal fun studentClipboardText")
        listOf(edit, bulk).forEach { section ->
            assertTrue("genderFilterChipColors(Color(0xFFFF5C9A))" in section)
            assertTrue("genderFilterChipColors(Color(0xFF3B9EFF))" in section)
        }
    }

    @Test
    fun `edit toolbar is red close central eye and green check`() {
        val edit = school.substringAfter("private fun StudentEditDialog(")
            .substringBefore("private data class BulkStudentDraft")
        assertTrue(edit.indexOf("Icons.Outlined.Close") < edit.indexOf("Icons.Outlined.Visibility"))
        assertTrue(edit.indexOf("Icons.Outlined.Visibility") < edit.indexOf("Icons.Outlined.Check"))
        assertTrue("Color(0xFFE5484D)" in edit)
        assertTrue("Color(0xFF25A86B)" in edit)
        assertTrue("passwordVisible = !passwordVisible" in edit)
        assertFalse("currentPasswordVisible" in edit)
        assertFalse("trailingIcon" in edit)
        assertEquals(2, edit.split("Modifier.weight(1f).height(64.dp)").size - 1)
        assertEquals(2, edit.split("textStyle = MaterialTheme.typography.titleMedium").size - 1)
    }

    @Test
    fun `clipboard has exactly the requested eight lines and vault password`() {
        val copied = studentClipboardText(
            student = StudentProfile(
                id = "id-1",
                fullName = "سحر امیری",
                firstName = "سحر",
                lastName = "امیری",
                username = "sahar_amiri",
                gender = "female",
                active = true,
                classNames = "دوازدهم الف، کنکور",
                fatherName = "رضا",
                grade = "دوازدهم",
                fieldOfStudy = "علوم تجربی"
            ),
            currentPassword = "SafePass123"
        )
        assertEquals(
            listOf(
                "نام: سحر",
                "نام خانوادگی: امیری",
                "نام پدر: رضا",
                "پایه: دوازدهم",
                "رشته: علوم تجربی",
                "نام کاربری: sahar_amiri",
                "رمز: SafePass123",
                "کلاس‌ها: دوازدهم الف، کنکور"
            ),
            copied.lines()
        )
    }

    @Test
    fun `square and circle center can move freely but stay inside image`() {
        val moved = CropGeometry.moveCenter(.5f, .5f, 40f, -20f, 400f, 200f, .2f, .4f)
        assertEquals(.6f, moved.first, .0001f)
        assertEquals(.4f, moved.second, .0001f)
        val clamped = CropGeometry.moveCenter(.5f, .5f, 9999f, -9999f, 400f, 200f, .2f, .4f)
        assertEquals(.9f, clamped.first, .0001f)
        assertEquals(.2f, clamped.second, .0001f)
    }

    @Test
    fun `dragged edge moves in its direction while opposite edge stays fixed`() {
        val oldSide = .5f
        val oldCenter = .5f
        val minDimension = 400f
        val change = 40f
        val newSide = CropGeometry.resizeSide(oldSide, change, minDimension)

        val rightCenter = CropGeometry.recenterAfterResize(
            CropEdgeKind.RIGHT, change, 400f, 400f, oldCenter, oldCenter
        ).first
        assertEquals(oldCenter - oldSide / 2f, rightCenter - newSide / 2f, .0001f)

        val leftCenter = CropGeometry.recenterAfterResize(
            CropEdgeKind.LEFT, change, 400f, 400f, oldCenter, oldCenter
        ).first
        assertEquals(oldCenter + oldSide / 2f, leftCenter + newSide / 2f, .0001f)

        val bottomCenter = CropGeometry.recenterAfterResize(
            CropEdgeKind.BOTTOM, change, 400f, 400f, oldCenter, oldCenter
        ).second
        assertEquals(oldCenter - oldSide / 2f, bottomCenter - newSide / 2f, .0001f)

        val topCenter = CropGeometry.recenterAfterResize(
            CropEdgeKind.TOP, change, 400f, 400f, oldCenter, oldCenter
        ).second
        assertEquals(oldCenter + oldSide / 2f, topCenter + newSide / 2f, .0001f)
    }

    @Test
    fun `crop frame has invisible resize zones and no edge bars`() {
        val editor = source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
        assertTrue("CropGeometry.moveCenter(" in editor)
        assertTrue(".padding(18.dp)" in editor)
        assertTrue(".pointerInput(circular)" in editor)
        assertFalse("Modifier.width(34.dp).height(5.dp)" in editor)
        assertFalse("Modifier.width(5.dp).height(34.dp)" in editor)
    }

    @Test
    fun `student card compacts name grade field father and username into two rows`() {
        val card = school.substringAfter("private fun StudentCard(")
            .substringBefore("private fun ClassEditorDialog(")
        assertTrue("listOf(student.grade, student.fieldOfStudy)" in card)
        assertTrue("joinToString(\" \")" in card)
        assertTrue("نام پدر: ${'$'}{student.fatherName" in card)
        assertTrue("نام کاربری: ${'$'}{student.username" in card)
        assertFalse("Text(\"رشته: ${'$'}{student.fieldOfStudy" in card)
    }
}
