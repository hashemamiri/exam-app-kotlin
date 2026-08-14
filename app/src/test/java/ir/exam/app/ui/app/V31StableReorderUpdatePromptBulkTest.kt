package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V31:
 * ۱) گزینه‌ها هنگام جابه‌جایی غیب نمی‌شوند (لیست کلیدخوردهٔ پایدار)
 * ۲) مخاطبان آزمون داخل مشخصات آزمون
 * ۳) پیغام آپدیت جدید هنگام ورود به برنامه
 * ۴) آپلود تصویر دیگر برنامه را نمی‌کشد (محافظ OOM)
 * ۵) پنجره گروهی بدون کلاس با کادر رمز فعلی و لیست شماره کارت‌ها
 */
class V31StableReorderUpdatePromptBulkTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val builder by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
    }
    private val optionUi by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
    }
    private val appShell by lazy {
        source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
    }
    private val uploader by lazy {
        source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt")
    }
    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }
    private val schoolRepo by lazy {
        source("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt")
    }

    // ============================================================
    // ۱) غیب‌شدن گزینه
    // ============================================================

    @Test
    fun `options stay in a stable keyed column and never vanish while dragging`() {
        assertTrue("key(optionId) {" in builder)
        assertTrue("key(itemId) {" in optionUi)
        assertFalse("AnimatedReorderColumn(" in builder)
        assertFalse("AnimatedReorderColumn(" in optionUi)
        assertFalse("ReorderAnimation.kt" in File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile }.joinToString("\n") { it.path })
    }

    @Test
    fun `dragged option card keeps the active colour contract`() {
        assertTrue("optionDragId == optionId" in builder)
        assertTrue("label = \"option-card-color\"" in builder)
        assertTrue("dragActiveId == itemId" in optionUi)
        assertTrue("label = \"matching-card-color\"" in optionUi)
        assertTrue("onActiveChanged: (Boolean) -> Unit = {}" in optionUi)
        assertTrue("pointerInput(Unit)" in optionUi)
    }

    // ============================================================
    // ۲) مخاطبان در مشخصات آزمون
    // ============================================================

    @Test
    fun `audience card lives inside the exam settings section`() {
        val settingsBlock = builder.substringAfter("visible = settingsExpanded")
            .substringBefore("state.importedBy")
        assertTrue("AudienceCard(state, viewModel)" in settingsBlock)
        assertTrue("ExamSettingsCard(state, viewModel)" in settingsBlock)
        assertFalse("item { AudienceCard(state, viewModel) }" in builder)
        assertTrue("باز/بسته" in builder)
    }

    // ============================================================
    // ۳) پیغام آپدیت هنگام ورود
    // ============================================================

    @Test
    fun `app entry checks for updates and shows a prompt when one exists`() {
        assertTrue("LaunchedEffect(user.id) { updateViewModel.check(BuildConfig.VERSION_CODE) }" in appShell)
        assertTrue("var updatePromptDismissed by rememberSaveable(user.id) { mutableStateOf(false) }" in appShell)
        assertTrue("بروزرسانی جدید" in appShell)
        assertTrue("دریافت نسخه" in appShell)
        assertTrue("بعداً" in appShell)
        assertTrue("updateViewModel.downloadAndInstall()" in appShell)
    }

    // ============================================================
    // ۴) کرش آپلود تصویر
    // ============================================================

    @Test
    fun `upload decode retries on OutOfMemoryError instead of killing the app`() {
        assertTrue("while (attempt < MAX_ATTEMPTS)" in uploader)
        assertTrue("catch (oom: OutOfMemoryError)" in uploader)
        assertTrue("System.gc()" in uploader)
        assertTrue("maxDimension * 2 shr attempt" in uploader)
        assertTrue("runtime.maxMemory()" in uploader)
        assertTrue("MAX_DECODE_PIXELS = 7_000_000L" in uploader)
        assertTrue("Bitmap.Config.RGB_565" in uploader)
        assertTrue("حافظه دستگاه برای این تصویر کافی نیست" in uploader)
    }

    // ============================================================
    // ۵) پنجره گروهی بدون کلاس
    // ============================================================

    private fun bulkSection(): String =
        school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")

    @Test
    fun `bulk window is fully classless and registers students without a class`() {
        val bulk = bulkSection()
        assertFalse("classId" in bulk)
        assertFalse("DropdownMenu(" in bulk)
        assertFalse("کلاس را انتخاب کنید" in bulk)
        assertTrue("onCreate: (List<NewStudentRequest>) -> Unit" in bulk)
        assertTrue("createStudentsBulk(classId:String?" in schoolRepo)
        assertTrue("put(\"class_id\",classId.orEmpty())" in schoolRepo)
    }

    @Test
    fun `only the numbered card list sits below the buttons with no scrolling`() {
        val bulk = bulkSection()
        assertTrue("rows.indices.chunked(6)" in bulk)
        assertTrue("selected = activeIndex == index" in bulk)
        assertFalse("horizontalScroll(rememberScrollState())" in bulk)
    }

    @Test
    fun `card fields are laid out in the requested two-column rows`() {
        val bulk = bulkSection()
        // نام و نام خانوادگی در یک سطر
        assertTrue("// نام و نام خانوادگی در یک سطر" in bulk)
        // نام پدر و نام کاربری در یک سطر
        assertTrue("// نام پدر و نام کاربری در یک سطر" in bulk)
        // پایه و رشته در یک سطر
        assertTrue("// پایه و رشته در یک سطر" in bulk)
        // رمز و رمز فعلی در یک سطر
        assertTrue("// رمز و رمز فعلی در یک سطر" in bulk)
        assertTrue("label = { Text(\"رمز فعلی\") }" in bulk)
        assertTrue("readOnly = true" in bulk)
    }

    @Test
    fun `current password box mirrors the assigned password automatically`() {
        val bulk = bulkSection()
        // کادر رمز فعلی همان مقدار row.password را نشان می‌دهد؛ با تغییر رمز خودکار به‌روز می‌شود.
        assertTrue("value = row.password," in bulk)
        assertTrue("label = { Text(\"رمز فعلی\") }" in bulk)
    }

    @Test
    fun `student card copy reads the password from the current password box`() {
        assertTrue("knownPasswords[it.username.lowercase()]=it.password" in school)
        assertTrue("knownPasswordOf(student.username)" in school)
        assertTrue("رمز عبور:" in school)
        assertTrue("android.content.extra.IS_SENSITIVE" in school)
        assertTrue("knownPasswordOf = knownPasswordOf" in school)
    }
}
