package ir.exam.app.ui.app

import ir.exam.app.ui.image.CropEdgeKind
import ir.exam.app.ui.image.CropGeometry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V30:
 * ۱) جابه‌جایی گزینه/جورکردنی مثل کارت سؤال: کارت رنگی می‌شود و حرکت روان است
 * ۲) کارت مشخصات آزمون پیش‌فرض بسته است و با بازشدن کارت سؤال بسته می‌شود
 * ۳) لیست تغییرات فارسی در صفحهٔ درباره
 * ۴) هندسهٔ برش ویرایشگر تصویر (تست واقعی ریاضی)
 * ۵) پنجره گروهی بدون کلاس‌ها و با شمارهٔ همیشگی کارت
 */
class V30SmoothReorderSettingsChangelogTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val optionUi by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
    }
    private val builder by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
    }
    private val about by lazy {
        source("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
    }
    private val editor by lazy {
        source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt")
    }
    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }

    // ============================================================
    // ۱) جابه‌جایی رنگی و روان گزینه‌ها
    // ============================================================

    @Test
    fun `dragged option card is highlighted like the question card`() {
        // کارت گزینه در حال درگ باید primaryContainer شود؛ دقیقاً مثل کارت سؤال.
        assertTrue("onActiveChanged = { active ->" in builder)
        assertTrue("optionDragId == optionId" in builder)
        assertTrue("label = \"option-card-color\"" in builder)
        assertTrue("CardDefaults.cardColors(containerColor = optionCardColor)" in builder)
        assertTrue("label = \"question-drag-color\"" in builder)
    }

    @Test
    fun `matching item cards are highlighted with the same contract`() {
        assertTrue("dragActiveId == itemId" in optionUi)
        assertTrue("label = \"matching-card-color\"" in optionUi)
        assertTrue("onActiveChanged = { active -> dragActiveId = if (active) itemId else null }" in optionUi)
        assertTrue("fun ReorderDragButton" in optionUi)
        assertTrue("onActiveChanged: (Boolean) -> Unit = {}" in optionUi)
    }

    @Test
    fun `option and matching cards keep the stable keyed list while dragging`() {
        // کارت‌ها در یک ستون کلیدخوردهٔ پایدار می‌مانند؛ انیمیشن جابه‌جایی سفارشی
        // حذف شد چون باعث غیب‌شدن گزینه هنگام درگ می‌شد.
        assertTrue("key(optionId) {" in builder)
        assertTrue("key(itemId) {" in optionUi)
        assertFalse("AnimatedReorderColumn(" in builder)
        assertFalse("AnimatedReorderColumn(" in optionUi)
    }

    @Test
    fun `active state survives moves because it follows the stable item id`() {
        // رنگ فعال به شناسهٔ پایدار گزینه گره خورده، نه ایندکس که با جابه‌جایی عوض می‌شود.
        assertTrue("optionDragId = if (active) optionId else null" in builder)
        assertTrue("dragActiveId = if (active) itemId else null" in optionUi)
    }

    // ============================================================
    // ۲) کارت مشخصات آزمون
    // ============================================================

    @Test
    fun `exam settings card is collapsed by default`() {
        assertTrue("var settingsExpanded by rememberSaveable { mutableStateOf(false) }" in builder)
    }

    @Test
    fun `opening a question card closes the exam settings card`() {
        assertTrue("settingsExpanded = false" in builder)
        assertTrue("بازکردن کارت سؤال، کارت مشخصات آزمون را می‌بندد." in builder)
        assertTrue("visible = settingsExpanded" in builder)
    }

    // ============================================================
    // ۳) لیست تغییرات درباره
    // ============================================================

    @Test
    fun `changelog is shown even while the update is downloaded`() {
        assertTrue("state.update?.takeIf { it.notesFa.isNotEmpty() }" in about)
        assertFalse("downloadedApkPath == null && it.notesFa.isNotEmpty()" in about)
        assertTrue("ChangeListCard(\"تغییرات نسخه ${'$'}{remote.name}\"" in about)
        // بررسی خودکار هنگام بازشدن صفحه تا لیست تغییرات بدون لمس دکمه دیده شود.
        assertTrue("LaunchedEffect(Unit) { viewModel.check(BuildConfig.VERSION_CODE) }" in about)
    }

    @Test
    fun `changelog bullets are cleaned for readable Persian`() {
        assertTrue("removePrefix(\"-\")" in about)
        assertTrue("removePrefix(\"•\")" in about)
        assertTrue("replace(\"`\", \"\")" in about)
        assertTrue("Text(\"• ${'$'}clean\"" in about)
    }

    @Test
    fun `workflow publishes real Persian notes from the changelog file`() {
        val workflow = source(".github/workflows/android.yml")
        assertTrue("text/CHANGELOG_FA.txt" in workflow)
        assertTrue("removeprefix(\"-\")" in workflow)
        assertTrue("p_notes_fa" in workflow)
        val changelog = source("text/CHANGELOG_FA.txt")
        assertTrue("جابه‌جایی" in changelog)
        assertTrue("لیست" in changelog)
    }

    // ============================================================
    // ۴) هندسهٔ برش تصویر — تست واقعی ریاضی
    // ============================================================

    @Test
    fun `crop is exactly square in pixels for any rotation and aspect`() {
        val cases = listOf(
            Triple(4000f, 2000f, 0),
            Triple(4000f, 2000f, 90),
            Triple(2000f, 4000f, 270),
            Triple(3000f, 3000f, 90)
        )
        cases.forEach { (w, h, rotation) ->
            val rect = CropGeometry.cropRect(.5f, .5f, .78f, w, h, rotation)
            // CropRect روی تصویر چرخیده اعمال می‌شود؛ بنابراین ابعاد چرخیده ملاک است.
            val (rotatedW, rotatedH) = CropGeometry.rotatedSize(w, h, rotation)
            val widthPx = rect.width * rotatedW
            val heightPx = rect.height * rotatedH
            // عرض و ارتفاع خروجی باید دقیقاً برابر باشند (مربع واقعی).
            assertEquals("مربع نبود برای $w×$h و چرخش $rotation", widthPx, heightPx, 0.01f)
            // خروجی باید داخل تصویر بماند.
            assertTrue(rect.left >= 0f && rect.top >= 0f)
            assertTrue(rect.left + rect.width <= 1.001f)
            assertTrue(rect.top + rect.height <= 1.001f)
        }
    }

    @Test
    fun `crop center is clamped inside the image`() {
        // مرکز خیلی بیرون‌زده باید داخل بازهٔ مجاز کشیده شود.
        val rect = CropGeometry.cropRect(2f, -1f, .5f, 2000f, 1000f, 0)
        assertTrue(rect.left >= 0f && rect.top >= 0f)
        assertTrue(rect.left + rect.width <= 1.001f && rect.top + rect.height <= 1.001f)
        // ضلع کوتاه ۱۰۰۰ پیکسل است؛ کادر ۵۰۰px → کسر عرض ۰٫۲۵ و کسر ارتفاع ۰٫۵.
        assertEquals(.25f, rect.width, 0.001f)
        assertEquals(.5f, rect.height, 0.001f)
        assertEquals(500f, rect.width * 2000f, 0.01f)
        assertEquals(500f, rect.height * 1000f, 0.01f)
    }

    @Test
    fun `resize respects minimum and maximum side`() {
        assertEquals(CropGeometry.MIN_SIDE, CropGeometry.resizeSide(.1f, 10f, 1000f), 0.001f)
        assertEquals(CropGeometry.MAX_SIDE, CropGeometry.resizeSide(.99f, 1000f, 1000f), 0.001f)
        val grown = CropGeometry.resizeSide(.5f, 100f, 1000f)
        assertTrue(grown > .5f && grown < CropGeometry.MAX_SIDE)
    }

    @Test
    fun `resize recenters toward the dragged edge`() {
        // کشیدن لبهٔ راست به بیرون باید مرکز را به راست ببرد.
        val (x, y) = CropGeometry.recenterAfterResize(CropEdgeKind.RIGHT, 100f, 1000f, 1000f, .5f, .5f)
        assertTrue(x > .5f)
        assertEquals(.5f, y, 0.0001f)
        val (x2, y2) = CropGeometry.recenterAfterResize(CropEdgeKind.TOP, 100f, 1000f, 1000f, .5f, .5f)
        assertEquals(.5f, x2, 0.0001f)
        assertTrue(y2 < .5f)
    }

    @Test
    fun `estimate fraction matches the crop square area`() {
        // کادر ۱۰۰۰×۱۰۰۰ از تصویر ۴۰۰۰×۲۰۰۰ = یک‌هشتم مساحت.
        val fraction = CropGeometry.areaFraction(.5f, 4000f, 2000f, 0)
        assertEquals(.125f, fraction, 0.001f)
        val rotated = CropGeometry.areaFraction(.5f, 4000f, 2000f, 90)
        assertEquals(.125f, rotated, 0.001f)
    }

    @Test
    fun `editor closes the keyboard and stays within the screen`() {
        assertTrue("focusManager.clearFocus()" in editor)
        assertTrue("verticalScroll(rememberScrollState())" in editor)
        assertTrue("imePadding()" in editor)
        assertTrue("heightIn(max = maxDialogHeight.dp)" in editor)
        assertTrue("screenHeightDp * .92f" in editor)
        assertTrue("CropGeometry.cropRect(" in editor)
        assertTrue("CropGeometry.resizeSide(" in editor)
    }

    // ============================================================
    // ۵) پنجره گروهی
    // ============================================================

    private fun bulkSection(): String =
        school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")

    @Test
    fun `bulk window no longer displays any class controls`() {
        val bulk = bulkSection()
        // کلاس به‌کلی حذف شده است؛ دانش‌آموزها بدون نیاز به انتخاب کلاس ثبت می‌شوند.
        assertFalse("classId" in bulk)
        assertFalse("DropdownMenu(" in bulk)
        assertFalse("کلاس را انتخاب کنید" in bulk)
        assertTrue("onCreate: (List<NewStudentRequest>) -> Unit" in bulk)
        assertTrue("NewStudentRequest(" in bulk)
        assertTrue("null" in bulk)
    }

    @Test
    fun `active card number is automatically scrolled into view`() {
        val bulk = bulkSection()
        // شماره‌ها افقی اسکرول می‌شوند و شمارهٔ کارت فعال خودکار در دید قرار می‌گیرد.
        assertTrue("rememberLazyListState()" in bulk)
        assertTrue("LaunchedEffect(activeIndex, rows.size)" in bulk)
        assertTrue("numberListState.animateScrollToItem(activeIndex)" in bulk)
        assertTrue("selected = activeIndex == index" in bulk)
        assertFalse("horizontalScroll(rememberScrollState())" in bulk)
    }
}
