package ir.exam.app.ui.app

import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.ui.classes.studentClipboardText
import ir.exam.app.ui.common.StandardSchoolGrades
import ir.exam.app.ui.common.gradeOdometerValues
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V23InteractionGradeOdometerTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").isFile
    }

    @Test
    fun `builder save fab uses centered scaffold slot and bounded native check`() {
        val builder = File(
            root(),
            "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt"
        ).readText()
        val fab = builder.substringAfter("floatingActionButtonPosition")
            .substringBefore(") { padding ->")
        assertTrue("FabPosition.Center" in fab)
        assertTrue("Icons.Outlined.Check" in fab)
        assertTrue("contentDescription = \"ذخیره آزمون\"" in fab)
        assertTrue("Modifier.align(Alignment.CenterStart).size(56.dp)" in fab)
        assertTrue("modifier = Modifier.size(28.dp)" in fab)
        assertFalse("clippable text glyph returned", "Text(\"✓\"" in fab)
    }

    @Test
    fun `class menus and class card actions are centered`() {
        val school = File(
            root(),
            "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt"
        ).readText()
        val classes = school.substringAfter("private fun ClassesContent(")
            .substringBefore("private fun ClassRosterContent(")
        val roster = school.substringAfter("private fun ClassRosterContent(")
            .substringBefore("private fun StudentsContent(")
        assertTrue("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in classes)
        assertTrue("modifier = Modifier.fillMaxWidth()" in roster)
        assertTrue("modifier = Modifier.align(Alignment.CenterHorizontally)" in roster)
        assertTrue("Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)" in roster)
        assertTrue("expandVertically(expandFrom = Alignment.Top)" in roster)
        assertTrue("shrinkVertically(shrinkTowards = Alignment.Top)" in roster)
    }

    @Test
    fun `student controls are larger and profile copy never invents an old password`() {
        val school = File(
            root(),
            "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt"
        ).readText()
        val card = school.substringAfter("private fun StudentCard(")
            .substringBefore("private fun ClassEditorDialog(")
        assertTrue("Icons.Outlined.ContentCopy" in card)
        assertTrue("copyStudentInformation(" in card)
        assertTrue(card.split("Modifier.weight(1f).height(58.dp)").size - 1 >= 5)
        assertTrue("Modifier.size(32.dp)" in card)
        assertTrue("Modifier.size(30.dp)" in card)

        val copied = studentClipboardText(
            StudentProfile(
                id = "student-id",
                fullName = "سارا احمدی",
                firstName = "سارا",
                lastName = "احمدی",
                username = "sara_ahmadi",
                gender = "female",
                active = true,
                classNames = "هفتم الف",
                fatherName = "رضا",
                grade = "هفتم"
            )
        )
        listOf("نام: سارا", "نام خانوادگی: احمدی", "sara_ahmadi", "رضا", "هفتم", "کلاس‌ها: هفتم الف").forEach {
            assertTrue("missing copied profile value $it", it in copied)
        }
        assertTrue("رمز: —" in copied)
        assertFalse("وضعیت:" in copied)
        assertFalse("شناسه حساب:" in copied)
        assertFalse("جنسیت:" in copied)
        assertFalse("a fake/retrieved password must not be copied", "plain_password" in school)
        assertFalse("student model must not expose password", "student.password" in school)
    }

    @Test
    fun `gender filters toggle off and grade chips are replaced by one odometer`() {
        val school = File(
            root(),
            "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt"
        ).readText()
        val picker = school.substringAfter("private fun MemberPickerDialog(")
            .substringBefore("private fun StudentEditDialog(")
        assertTrue("gender = if (gender == \"female\") null else \"female\"" in picker)
        assertTrue("gender = if (gender == \"male\") null else \"male\"" in picker)
        assertTrue("GradeOdometerPicker(" in picker)
        assertTrue("includeStandardGrades = false" in picker)
        assertTrue("emptyLabel = \"همه پایه‌ها\"" in picker)
        assertTrue("student.gender?.lowercase() == gender" in picker)
        assertTrue("student.grade?.trim() == grade" in picker)
        assertFalse("old per-grade chip loop returned", "grades.forEach" in picker)
    }

    @Test
    fun `the same vertical grade odometer covers every editable grade location`() {
        val root = root()
        val school = File(
            root,
            "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt"
        ).readText()
        val profile = File(
            root,
            "app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt"
        ).readText()
        val odometer = File(
            root,
            "app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt"
        ).readText()

        // V61.5 — یکی برای فیلتر دانش‌آموزان (school) و یکی برای نام مدرسهٔ
        // سربرگ (profile) به چرخ مشترک اضافه شد؛ شمار جدید ۵ و ۲ است.
        assertEquals(5, Regex("GradeOdometerPicker\\(").findAll(school).count())
        assertEquals(2, Regex("GradeOdometerPicker\\(").findAll(profile).count())
        assertFalse(
            "free-text grade input remains in school UI",
            Regex("label\\s*=\\s*\\{\\s*Text\\(\"پایه\"\\)").containsMatchIn(school)
        )
        assertFalse(
            "free-text grade input remains in profile UI",
            Regex("label\\s*=\\s*\\{\\s*Text\\(\"پایه\"\\)").containsMatchIn(profile)
        )
        listOf(
            "rememberSnapFlingBehavior",
            "GradeWheelDialog",
            "LazyColumn",
            "snapshotFlow",
            "Icons.Outlined.UnfoldMore",
            "فهرست را به بالا یا پایین بکشید"
        ).forEach { assertTrue("missing redesigned grade wheel behavior $it", it in odometer) }
        assertFalse("old speedometer icon returned", "Icons.Outlined.Speed" in odometer)
    }

    @Test
    fun `grade options retain standard order all option and custom legacy values`() {
        val filtered = gradeOdometerValues(
            current = "",
            availableGrades = listOf("دهم", "هفتم", "نهم"),
            includeStandardGrades = false
        )
        assertEquals(listOf("", "هفتم", "نهم", "دهم"), filtered)

        val editable = gradeOdometerValues(current = "دهم تجربی")
        assertEquals("", editable.first())
        assertTrue(StandardSchoolGrades.all { it in editable })
        assertEquals(1, editable.count { it == "دهم تجربی" })
    }
}
