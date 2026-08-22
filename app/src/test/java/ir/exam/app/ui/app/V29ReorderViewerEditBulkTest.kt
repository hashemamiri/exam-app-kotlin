package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V29:
 * ۱) جابه‌جایی گزینه/جورکردنی دقیقاً مثل کارت سؤال (gesture وسط کار بازنشانی نمی‌شود)
 * ۲) آیکن فرمول زیر کادر متن سؤال و آیکن دوربین در ردیف رسانهٔ سؤال
 * ۳) لمس thumbnail تصویر → نمایش تمام‌صفحه با زوم و ضربدر بستن
 * ۴) پس از انتخاب عکس → بخش ویرایش تصویر باز می‌شود
 * ۵) پنجره گروهی: «+» کارت تازه را جایگزین کارت قبلی می‌کند و پنجره بزرگ نمی‌شود
 */
class V29ReorderViewerEditBulkTest {
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
    private val viewModel by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
    }
    private val media by lazy {
        source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
    }
    private val inlineMathEditor by lazy {
        source("app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt")
    }
    private val viewer by lazy {
        source("app/src/main/java/ir/exam/app/ui/image/FullScreenImageViewer.kt")
    }
    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }

    // ============================================================
    // ۱) جابه‌جایی گزینه‌ها
    // ============================================================

    @Test
    fun `option drag never restarts when the label changes after a move`() {
        // برچسب گزینه پس از جابه‌جایی عوض می‌شود؛ اگر pointerInput به برچسب
        // کلید بخورد، gesture وسط کار لغو می‌شود و هر پرش نیازمند لمس طولانی تازه است.
        assertTrue("pointerInput(Unit)" in optionUi)
        assertFalse("pointerInput(description)" in optionUi)
    }

    @Test
    fun `option and question card share the exact same drag threshold`() {
        assertTrue("const val ReorderStepDp: Float = 52f" in optionUi)
        assertTrue("ReorderStepDp.dp.toPx()" in builder)
        assertTrue("while (abs(dragAccumulator) >= dragStepPx)" in builder)
        assertTrue("while (abs(accumulated) >= stepPx)" in optionUi)
    }

    @Test
    fun `both matching columns use the stable shared drag`() {
        assertTrue("ReorderDragButton(" in optionUi)
        assertTrue("detectDragGesturesAfterLongPress" in optionUi)
        assertTrue("rememberUpdatedState(currentIndex)" in optionUi)
        assertTrue("onMove(dragIndex, delta)" in optionUi)
    }

    // ============================================================
    // ۲) آیکن فرمول زیر کادر متن و آیکن دوربین در ردیف رسانه
    // ============================================================

    @Test
    fun `formula icon sits below question text while camera stays in media row`() {
        // V45 عمداً آیکن فرمول را از QuestionMediaEditor به نوار زیر کادر متن منتقل کرد.
        assertTrue("Icons.Outlined.PhotoCamera" in media)
        assertFalse("Icons.Outlined.Functions" in media)
        assertTrue(
            "ToolbarButton(Icons.Outlined.Functions, \"درج فرمول\", onInsertFormula)" in inlineMathEditor
        )
        assertTrue("onInsertFormula = { formulaTarget = FormulaTarget(\"question\") }" in builder)
        assertFalse("OutlinedButton(onClick = { formulaTarget = FormulaTarget(\"question\") })" in builder)
    }

    // ============================================================
    // ۳) نمایش تمام‌صفحه تصویر
    // ============================================================

    @Test
    fun `tapping an added image opens a full-screen zoomable viewer closed by X`() {
        assertTrue("detectTransformGestures" in viewer)
        assertTrue("MAX_ZOOM" in viewer)
        assertTrue("DOUBLE_TAP_ZOOM" in viewer)
        assertTrue("Icons.Outlined.Close" in viewer)
        assertTrue("onDoubleTap" in viewer)
        assertTrue("usePlatformDefaultWidth = false" in viewer)
    }

    @Test
    fun `question and option thumbnails open the viewer`() {
        assertTrue("onView = { viewerUri = image.uri }" in media)
        assertTrue("viewerUri?.let" in media)
        assertTrue("FullScreenImageViewer(uri = uri, onDismiss = { viewerUri = null })" in media)
        assertTrue("viewing = value" in optionUi)
        assertTrue("FullScreenImageViewer" in optionUi)
    }

    @Test
    fun `existing images can still be re-edited with a small edit affordance`() {
        assertTrue("Icons.Outlined.Edit" in optionUi)
        assertTrue("Icons.Outlined.Edit" in media)
        assertTrue("reEditTarget = image.id to Uri.parse(image.uri)" in media)
        assertTrue("fun replaceImage" in viewModel)
        assertTrue("onReplace = { imageId, uri -> viewModel.replaceImage(question.id, imageId, uri) }" in builder)
    }

    // ============================================================
    // ۴) ویرایش پس از انتخاب عکس
    // ============================================================

    @Test
    fun `picked images open the editor before being added`() {
        assertTrue("safeUris += it.uri.toString()" in media)
        assertTrue("batchQueue = safeUris.map(Uri::parse)" in media)
        assertTrue("batchQueue.firstOrNull()" in media)
        assertTrue("InteractiveImageEditorDialog(" in media)
        assertTrue("onAdd(batchResults.toList())" in media)
    }

    @Test
    fun `option picker opens the editor right after a safe prepare`() {
        assertTrue("onSuccess { editing = Uri.parse(it.uri.toString()) }" in optionUi)
        assertTrue("InteractiveImageEditorDialog(" in optionUi)
        assertTrue("onChange(it.toString())" in optionUi)
    }

    @Test
    fun `cancelling the editor discards the picked image`() {
        assertTrue("batchQueue = emptyList()" in media)
        assertTrue("batchResults.clear()" in media)
        assertTrue("onDismiss = { editing = null }" in optionUi)
    }

    // ============================================================
    // ۵) پنجره گروهی
    // ============================================================

    private fun bulkSection(): String =
        school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")

    @Test
    fun `bulk plus replaces the visible card and the window keeps its size`() {
        val bulk = bulkSection()
        assertTrue("activeIndex = rows.lastIndex" in bulk)
        assertTrue("val row = rows[index]" in bulk)
        assertFalse("LazyColumn(" in bulk)
        assertFalse("weight(1f, fill = false)" in bulk)
        assertFalse("rowsListState" in bulk)
        assertFalse("pendingRevealIndex" in bulk)
    }

    @Test
    fun `bulk rows stay reachable through scrollable numbered chips`() {
        val bulk = bulkSection()
        assertTrue("LazyRow(" in bulk)
        assertTrue("state = numberListState" in bulk)
        assertTrue("selected = activeIndex == index" in bulk)
        assertTrue("rowComplete" in bulk)
        assertTrue("rowComplete(rows[index])" in bulk)
    }

    @Test
    fun `bulk delete removes the active row and shows the previous card`() {
        val bulk = bulkSection()
        assertTrue("rows.removeAt(index)" in bulk)
        assertTrue("activeIndex = (index - 1).coerceAtLeast(0)" in bulk)
    }

    // ============================================================
    // امنیت و سازگاری
    // ============================================================

    @Test
    fun `viewer only ever opens the stored prepared image`() {
        listOf(media, optionUi).forEach { text ->
            assertTrue("prepare(ImageEditRequest(uri))" in text)
        }
        // نمایش فقط از مقدار ذخیره‌شدهٔ پیش‌آماده است، نه URI خام دوربین.
        assertTrue("onView = { viewerUri = image.uri }" in media)
        assertTrue("clickable { viewing = value }" in optionUi)
    }
}
