package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V25HeaderSafetyPolishTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `every authenticated section gets a builder style top app bar`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("TopAppBar(" in app)
        assertTrue("page.sectionTitle(user.role, profileDestination, schoolStudentsSelected)" in app)
        listOf(
            "آزمون‌ها", "تقویم", "دانش‌آموزان", "کلاس‌ها", "بانک سؤال",
            "تصحیح پاسخ‌ها", "گزارش‌ها", "کیف پول", "حساب", "داده‌ها", "تنظیمات"
        ).forEach { assertTrue("missing section header $it", it in app) }
    }

    @Test
    fun `old password is never made recoverable while new password remains one time`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val viewModel = source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt")
        val allMain = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(allMain))
        assertFalse("old-password retrieval returned", "getPassword(" in allMain)
        assertTrue("رمز فعلی hash شده و قابل نمایش نیست" in school)
        assertTrue("copyOneTimeCredential" in school)
        assertTrue("lastCredential = StudentCredential(request.id, request.username, password)" in viewModel)
    }

    @Test
    fun `nested hamburger animation returns with compact timing and horizontal card title`() {
        val menu = source("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        val tile = source("app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt")
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("AnimatedVisibility" in menu)
        assertTrue("slideInHorizontally" in menu && "slideInVertically" in menu)
        assertTrue("val delay = 20 + index * 18" in menu)
        assertFalse("old slow stagger returned", "delay = 120 + index * 40" in menu)
        val tileBody = tile.substringAfter("fun NeumorphicMenuTile(").substringBefore("/** Morph واقعی")
        assertTrue("horizontalArrangement = Arrangement.spacedBy(9.dp)" in tileBody)
        assertTrue("modifier = Modifier.weight(1f)" in tileBody)
        assertTrue("\"تقویم\", \"رویدادها و پیام‌ها\"" in app)
        assertTrue("\"دانش‌آموزان\", \"فهرست و وضعیت\"" in app)
        assertTrue("\"کلاس‌ها\", \"فهرست و مدیریت\"" in app)
        assertFalse("old calendar title returned", "\"تقویم و پیام‌ها\"" in app)
    }

    @Test
    fun `now fills date and clock without auto confirming and trash clears boundary`() {
        val picker = source("app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt")
        assertTrue("val now = LocalDateTime.now()" in picker)
        assertTrue("val today = JalaliCalendar.fromGregorian(now.toLocalDate())" in picker)
        assertTrue("selected = today" in picker)
        assertTrue("hour = now.hour.toString()" in picker)
        assertTrue("minute = now.minute.toString()" in picker)
        assertFalse("now must not auto confirm boundary", "onConfirm(Instant.now()" in picker)
        assertTrue("onClear" in picker)
        assertTrue("Icons.Outlined.Delete" in picker)
        assertTrue("contentDescription = \"حذف زمان تعیین‌شده\"" in picker)
    }

    @Test
    fun `release notes only appear for a real update during download`() {
        val about = source("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
        assertTrue("state.downloadedApkPath == null && it.notesFa.isNotEmpty()" in about)
        assertFalse("persistent local history returned", "localReleaseNotesFa" in about)
        assertFalse("notes shown for every available state", "state.update?.takeIf { it.notesFa.isNotEmpty() }" in about)
    }

    @Test
    fun `bulk dialog bottom is ime tangent and list fits remaining height`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val bulk = school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")
        assertTrue("BoxWithConstraints" in bulk)
        assertTrue("Modifier.fillMaxSize().imePadding()" in bulk)
        assertTrue("contentAlignment = Alignment.TopCenter" in bulk)
        // V29: پنجره گروهی تک‌کارتی است؛ «+» کارت تازه را جایگزین می‌کند و پنجره بزرگ نمی‌شود.
        assertTrue("heightIn(max = availableHeight)" in bulk)
        assertTrue("activeIndex = rows.lastIndex" in bulk)
        assertFalse("bulk list returned", "LazyColumn(" in bulk)
        assertFalse("bottom gap above keyboard returned", "padding(horizontal = 12.dp, vertical = 8.dp)" in bulk)
    }

    @Test
    fun `score field is minimal and zero defaults are visually empty`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val model = source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt")
        assertTrue("private fun MinimalScoreField" in builder)
        assertTrue("Modifier.width(62.dp).height(40.dp)" in builder)
        assertTrue("BasicTextField(" in builder)
        assertTrue("\"بارم\"" in builder)
        assertTrue("val negativeMarking: String = \"\"" in model)
        assertTrue("val attemptCooldown: String = \"\"" in model)
    }

    @Test
    fun `timeout attempts and grading controls are centered chips`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val settings = builder.substringAfter("private fun ExamSettingsCard(").substringBefore("private fun AudienceCard(")
        assertTrue("label = \"اتمام تلاش در پایان زمان\"" in settings)
        assertTrue("selected = state.attemptOnTimeout" in settings)
        assertTrue("Text(\"تعداد تلاش مجاز\", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)" in settings)
        assertTrue("Text(\"سیاست نمره\", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)" in settings)
        assertTrue(settings.split("Alignment.CenterHorizontally").size - 1 >= 2)
    }

    @Test
    fun `question drag highlights and uses animated placement`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("Modifier.animateItem(" in builder)
        assertTrue("placementSpec = tween(260" in builder)
        assertTrue("var dragActive" in builder)
        assertTrue("dragActive = true" in builder)
        assertTrue("MaterialTheme.colorScheme.primaryContainer" in builder)
        assertTrue("label = \"question-drag-color\"" in builder)
    }

    @Test
    fun `student deletion is confirmed and large images use sampled decoding`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val imageRepo = source("app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt")
        assertTrue("Icons.Outlined.Delete" in school)
        assertTrue("title = { Text(\"حذف دانش‌آموز\") }" in school)
        assertTrue("viewModel.deleteStudent(student.id)" in school)
        assertTrue("این عملیات برگشت‌پذیر نیست" in school)
        assertTrue("decodeSampled(request.source, attempt)" in imageRepo)
        assertTrue("inJustDecodeBounds = true" in imageRepo)
        assertTrue("inSampleSize = sample" in imageRepo)
        assertTrue("MAX_DECODE_PIXELS = 7_000_000L" in imageRepo)
        assertTrue("catch (oom: OutOfMemoryError)" in imageRepo)
    }
}
