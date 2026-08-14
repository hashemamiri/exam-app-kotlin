package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V27DataImageOptionsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `multiple choice terminology replaces four choice everywhere at runtime`() {
        val main = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertTrue("QuestionType.MULTIPLE_CHOICE -> \"چندگزینه‌ای\"" in main)
        assertFalse("old four-choice wording returned", Regex("چهار.?گزینه").containsMatchIn(main))
    }

    @Test
    fun `option and matching reorder live with stable editor ids`() {
        val model = source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt")
        val viewModel = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        val optionUi = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        listOf("optionIds", "matchingLeftIds", "matchingRightIds").forEach {
            assertTrue("missing stable id field $it", it in model)
        }
        assertTrue("ensureEditorIds" in viewModel)
        assertTrue("while (abs(accumulated) >= stepPx)" in optionUi)
        assertTrue("onMove(dragIndex, delta)" in optionUi)
        assertTrue("dragIndex = target" in optionUi)
        assertTrue("key(itemId)" in optionUi)
        assertTrue("key(optionId)" in builder)
    }

    @Test
    fun `raw picker images are processed before add and editor opens only safe files`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val option = source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        listOf(media, option, profile).forEach { text ->
            assertTrue("missing safe repository prepare", "prepare(ImageEditRequest(uri))" in text)
        }
        assertFalse("main picker still opens raw editor", "editQueue.firstOrNull" in media)
        assertTrue("safeUris += it.uri.toString()" in media)
        assertTrue("onSuccess { editing = Uri.parse(it.uri.toString()) }" in option)
        assertTrue("onSuccess { avatarEditing = it.uri }" in profile)
    }

    @Test
    fun `bulk panel opens at top and fills ime reduced height`() {
        val school = source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        val bulk = school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")
        assertTrue("contentAlignment = Alignment.TopCenter" in bulk)
        // V29: فقط یک کارت دیده می‌شود و پنجره با «+» بزرگ نمی‌شود.
        assertTrue("heightIn(max = availableHeight)" in bulk)
        assertTrue("Modifier.fillMaxWidth().padding(14.dp)" in bulk)
        assertTrue("activeIndex = rows.lastIndex" in bulk)
        assertFalse("bottom alignment returned", "Alignment.BottomCenter" in bulk)
    }

    @Test
    fun `about shows installed version and remote notes until download completes`() {
        val about = source("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
        assertTrue("نسخه نصب‌شده: ${'$'}{BuildConfig.VERSION_NAME}" in about)
        assertTrue("state.downloadedApkPath == null && it.notesFa.isNotEmpty()" in about)
        assertTrue("ChangeListCard(\"تغییرات نسخه ${'$'}{remote.name}\"" in about)
        assertFalse("persistent local history returned", "localReleaseNotesFa" in about)
    }

    @Test
    fun `old student password remains non recoverable`() {
        val main = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(main))
        assertFalse("password retrieval API returned", "getPassword(" in main)
        assertTrue("رمز فعلی hash شده و قابل نمایش نیست" in main)
        assertTrue("copyOneTimeCredential" in main)
    }

    @Test
    fun `selected hamburger card changes color without dash marker`() {
        val tile = source("app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt")
            .substringAfter("fun NeumorphicMenuTile(").substringBefore("/** Morph واقعی")
        assertTrue("colors.accent.copy(alpha = .14f)" in tile)
        assertTrue("if (selected)" in tile)
        assertFalse("old dash marker returned", ".width(18.dp)" in tile && ".height(5.dp)" in tile)
    }

    @Test
    fun `data section scrolls has complete storage buttons and real exam import`() {
        val data = source("app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt")
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("fillMaxSize().verticalScroll(rememberScrollState())" in data)
        assertTrue("Text(\"وارد کردن آزمون\"" in data)
        assertTrue("ExamPackageCodec.decode(raw)" in data)
        assertTrue("importExam.launch" in data)
        assertTrue("onImportExam = onImportExam" in profile)
        assertTrue("onImportExam = { draft ->" in app)
        val storage = data.substringAfter("Text(\"نگهداری امن Storage\"")
        assertTrue(storage.split("modifier = Modifier.fillMaxWidth()").size - 1 >= 2)
    }

    @Test
    fun `grade wheel includes other and opens a custom input`() {
        val grade = source("app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt")
        assertTrue("OtherGradeValue" in grade)
        assertTrue("values + OtherGradeValue" in grade)
        assertTrue("OtherGradeValue -> \"سایر\"" in grade)
        assertTrue("customMode = true" in grade)
        // V28: برچسب ورودی دستی پارامتری شد تا رشته هم از همین چرخ استفاده کند،
        // ولی پیش‌فرض پایه دست‌نخورده است.
        assertTrue("customLabel: String = \"سایر پایه\"" in grade)
        assertTrue("label = { Text(customLabel) }" in grade)
    }
}
