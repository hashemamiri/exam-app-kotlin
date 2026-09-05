package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V24ComprehensiveUxTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `friday stays red without a holiday message row`() {
        val calendar = source("app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt")
        val selected = calendar.substringAfter("private fun SelectedDayCard(")
            .substringBefore("private fun HolidayRow(")
        assertFalse("Friday holiday text returned", "HolidayRow(\"جمعه\")" in selected)
        assertTrue("selected.officialHolidays.forEach" in selected)
        assertTrue("day.isHoliday ->" in calendar)
        assertTrue("MaterialTheme.colorScheme.error" in calendar)
    }

    @Test
    fun `hamburger is fast and account data are independent routes`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        val menu = source("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        val icons = source("app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt")
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("TEACHER_CARD_COUNT = 8" in menu)
        assertTrue("STUDENT_CARD_COUNT = 6" in menu)
        assertTrue("nested slide animation missing", "slideInHorizontally" in menu)
        assertTrue("controlled stagger missing", "val delay = 20 + index * 18" in menu)
        assertFalse("old slow nested visibility returned", "delay = 120 + index * 40" in menu)
        assertTrue("enter = fadeIn(tween(110))" in app)
        assertTrue("animationSpec = tween(180)" in icons)
        listOf("ProfileSettingsDestination.ACCOUNT", "ProfileSettingsDestination.DATA").forEach {
            assertTrue("missing independent menu destination $it", it in app && it in profile)
        }
        assertFalse("account remains under SettingsSection", "SettingsSection.ACCOUNT" in profile)
        assertFalse("data remains under SettingsSection", "SettingsSection.DATA" in profile)
    }

    @Test
    fun `account cards independently expand and collapse`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val account = profile.substringAfter("private fun AccountSection(")
            .substringBefore("private fun HeaderSection(")
        assertTrue("AccountAccordionCard" in account)
        assertTrue("expandedCard = if (expandedCard == card) null else card" in account)
        assertTrue("AnimatedVisibility" in account)
        assertTrue("Icons.Outlined.ExpandLess" in account)
        assertTrue("Icons.Outlined.ExpandMore" in account)
        listOf("مشخصات حساب", "تغییر نام کاربری", "تغییر ایمیل", "تغییر رمز عبور", "قفل برنامه").forEach {
            assertTrue("missing account card $it", it in account)
        }
    }

    @Test
    fun `about page only shows remote Persian notes while downloading`() {
        val about = source("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
        assertFalse("local persistent release history returned", "localReleaseNotesFa" in about)
        assertTrue("state.update?.takeIf { it.notesFa.isNotEmpty() }" in about)
        assertFalse("downloadedApkPath == null && it.notesFa.isNotEmpty()" in about)
        assertTrue("ChangeListCard" in about)
        assertFalse("old identity card returned", "AppIdentityCard" in about)
        assertFalse("package id prose returned", "شناسه بسته" in about)
        assertFalse("long install-security explanation returned", "APK فقط از نشانی HTTPS" in about)
    }

    @Test
    fun `exam window uses two colored buttons and calendar time dialog`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val picker = source("app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt")
        val settings = builder.substringAfter("private fun ExamSettingsCard(")
            .substringBefore("private fun AudienceCard(")
        assertTrue("JalaliDateTimeField(\n                    \"شروع\"" in settings)
        assertTrue("JalaliDateTimeField(\n                    \"پایان\"" in settings)
        assertTrue("Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)" in settings)
        listOf(
            "DateMonthGrid",
            "DateWeekHeader",
            "LocalLayoutDirection provides LayoutDirection.Ltr",
            "label = { Text(\"ساعت\") }",
            "label = { Text(\"دقیقه\") }",
            "Text(\"اکنون\"",
            "Icons.Outlined.Check",
            "Icons.Outlined.Close",
            "Color(0xFF19945B)",
            "Color(0xFFD63B49)"
        ).forEach { assertTrue("missing date-time behavior $it", it in picker) }
    }

    @Test
    fun `question card has chips drag live neon number score and print eye`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val editor = builder.substringAfter("private fun QuestionEditor(")
            .substringBefore("private fun QuestionStyleControls(")
        assertTrue("BoldToggleChip" in builder)
        assertTrue("تصادفی‌سازی سؤال‌ها" in builder && "تصادفی‌سازی گزینه‌ها" in builder)
        assertTrue("detectDragGesturesAfterLongPress" in editor)
        assertTrue("onMove(delta)" in editor)
        assertTrue("PersianDigits.convert(index + 1)" in editor)
        assertFalse("question prefix returned", "Text(\"سؤال ${'$'}{index + 1}" in editor)
        assertTrue("drawCircle" in editor && "Stroke(width = 2.dp.toPx())" in editor)
        assertTrue("MinimalScoreField(" in editor)
        assertTrue("private fun MinimalScoreField" in builder && "\"بارم\"" in builder)
        assertTrue("question.type.faLabel()" in editor)
        assertTrue("Icons.Outlined.Visibility" in editor)
        // V88.4 — «چیدمان و ظاهر چاپ» از کارتِ آزمونِ آنلاین برداشته شد
        // (تنظیماتِ کاغذ آنجا معنا نداشت) و همان کنترل‌ها اکنون در
        // آزمون‌سازِ چاپی بومی‌اند.
        assertFalse("print layout returned to the online card", "visible = styleExpanded" in editor)
        assertTrue("حساس به حروف بزرگ و کوچک" in editor)
        assertTrue("fontWeight = if (selected) FontWeight.Bold" in builder)
        assertFalse("question reorder arrows returned", "Text(\"↑\")" in editor || "Text(\"↓\")" in editor)
    }

    @Test
    fun `question images are side by side and crop editor only exposes requested tools`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val editor = source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
        assertTrue("LazyRow" in media)
        assertTrue("items(images, key = MediaDraft::id)" in media)
        assertTrue("freePlacement" in media)
        listOf("RotateLeft", "RotateRight", "Icons.Outlined.Crop", "CropFrame", "CropEdgeKind.LEFT", "CropEdgeKind.RIGHT", "CropEdgeKind.TOP", "CropEdgeKind.BOTTOM").forEach {
            assertTrue("missing crop tool $it", it in editor)
        }
        assertFalse("old crop sliders returned", "Slider(" in editor)
        assertFalse("old aspect buttons returned", "۴:۳" in editor || "کامل" in editor)
        assertTrue("حجم تقریبی" in editor)
        assertTrue("Icons.Outlined.Check" in editor && "Icons.Outlined.Close" in editor)
    }

    @Test
    fun `student password remains non-retrievable but a successful new password is one-time copyable`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val viewModel = source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt")
        val allMain = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse("recoverable password storage returned", Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(allMain))
        assertTrue("value = currentPassword.orEmpty()" in school)
        assertTrue("currentPassword: String?" in school)
        assertTrue("lastCredential = StudentCredential(request.id, request.username, password)" in viewModel)
        assertTrue("copyOneTimeCredential" in school)
        assertTrue("android.content.extra.IS_SENSITIVE" in school)
        assertTrue("کپی اطلاعات و رمز جدید" in school)
    }
}
