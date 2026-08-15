package ir.exam.app.ui.app

import ir.exam.app.ui.builder.persianOptionLetter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V26QuestionMediaReorderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `hamburger hides the shared header`() {
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("topBar = {\n                        if (!menuOpen)" in app)
        assertTrue("TopAppBar(" in app)
    }

    @Test
    fun `old password remains impossible while one-time new password stays safe`() {
        val main = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(main))
        assertFalse("old password retrieval returned", "getPassword(" in main)
        assertTrue("value = currentPassword.orEmpty()" in main)
        assertTrue("android.content.extra.IS_SENSITIVE" in main)
    }

    @Test
    fun `now fills both date and time and end cannot precede start`() {
        val picker = source("app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val viewModel = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        val repository = source("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("selected = today" in picker && "visibleYear = today.year" in picker)
        assertTrue("hour = now.hour.toString()" in picker && "minute = now.minute.toString()" in picker)
        assertTrue("minimumIso = state.opensAtIso" in builder)
        assertTrue("زمان پایان نمی‌تواند قبل از زمان شروع باشد" in picker)
        assertTrue("instantBefore(value, it)" in viewModel)
        assertTrue("!Instant.parse(state.closesAtIso).isBefore(Instant.parse(state.opensAtIso))" in repository)
    }

    @Test
    fun `bulk dialog is adjust resize and ime tangent in upper available region`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val manifest = source("app/src/main/AndroidManifest.xml")
        val bulk = school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")
        assertTrue("android:windowSoftInputMode=\"adjustResize\"" in manifest)
        assertTrue("SOFT_INPUT_ADJUST_RESIZE" in bulk)
        assertTrue("Modifier.fillMaxSize().imePadding()" in bulk)
        assertTrue("contentAlignment = Alignment.TopCenter" in bulk)
        assertTrue("heightIn(max = availableHeight)" in bulk)
        // V29: تک‌کارتی؛ افزودن ردیف پنجره را بزرگ نمی‌کند.
        assertTrue("activeIndex = rows.lastIndex" in bulk)
        assertFalse("LazyColumn(" in bulk)
    }

    @Test
    fun `large raw images are preflighted before the editor renders`() {
        val editor = source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
        val repository = source("app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt")
        assertTrue("repository.prepare(ImageEditRequest(source))" in editor)
        assertTrue("safeSource = prepared.uri" in editor)
        assertTrue("model = safeSource" in editor)
        assertTrue("enabled = !busy && !preparing && safeSource != null" in editor)
        assertTrue("decodeSampled(request.source, attempt)" in repository)
        assertTrue("MAX_DECODE_PIXELS = 7_000_000L" in repository)
    }

    @Test
    fun `question drag closes every accordion before moving`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("onDragStarted = { expandedQuestionId = null }" in builder)
        assertTrue("onDragStarted()" in builder)
    }

    @Test
    fun `multiple choice uses Persian letters compact media tools and drag`() {
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val multiple = builder.substringAfter("QuestionType.MULTIPLE_CHOICE ->")
            .substringBefore("QuestionType.TRUE_FALSE ->")
        assertTrue("val optionLabel = persianOptionLetter(index)" in multiple)
        assertTrue("Icons.Outlined.Functions" in multiple)
        assertTrue("SingleImagePicker(" in multiple)
        assertTrue("ReorderDragButton(" in multiple)
        assertFalse("option direction arrows returned", "↑ گزینه" in multiple || "↓ گزینه" in multiple)
        assertFalse("numeric option label returned", "گزینه ${'$'}{index + 1}" in multiple)
        assertEquals("الف", persianOptionLetter(0))
        assertEquals("ب", persianOptionLetter(1))
        assertEquals("و", persianOptionLetter(29))
    }

    @Test
    fun `matching puts right letter column first and left numeric column second`() {
        val matching = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        val body = matching.substringAfter("fun MatchingQuestionEditor(")
        assertTrue(body.indexOf("Text(\"ستون راست\"") < body.indexOf("Text(\"ستون چپ\""))
        val right = body.substringAfter("Text(\"ستون راست\"").substringBefore("Text(\"ستون چپ\"")
        val left = body.substringAfter("Text(\"ستون چپ\"")
        assertTrue("val label = persianOptionLetter(index)" in right)
        assertTrue("val label = PersianDigits.convert(index + 1)" in left)
        assertTrue("MatchingItemTools(" in right && "MatchingItemTools(" in left)
        assertTrue("ReorderDragButton" in matching)
        assertFalse("matching direction arrows returned", "Text(\"↑\")" in matching || "Text(\"↓\")" in matching)
        assertTrue("Icons.Outlined.Functions" in matching)
        assertTrue("Icons.Outlined.PhotoCamera" in matching)
    }

    @Test
    fun `all inserted images are icon sized beside camera without management cards`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val option = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        assertTrue("Icons.Outlined.PhotoCamera" in media)
        assertTrue("modifier = Modifier.size(30.dp)" in media)
        assertFalse("large management card returned", "Card(Modifier.width(156.dp))" in media)
        assertFalse("image resize slider returned", "Slider(" in media)
        assertTrue("Icons.Outlined.PhotoCamera" in option)
        assertTrue("modifier = Modifier.size(30.dp)" in option)
        assertFalse("old 72dp preview returned", "Modifier.size(72.dp)" in option)
    }
}
