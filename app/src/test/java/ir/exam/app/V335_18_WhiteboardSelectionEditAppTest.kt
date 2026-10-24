package ir.exam.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.18 — هم‌ترازی کامل تختهٔ سفید اپ اندروید با بخش «انتخاب و ویرایش موارد درج‌شده»ی سایت:
 * انتخاب تکی/چندگانه و کمند، جابه‌جایی، تغییر اندازه با هشت دستگیره و دو سرِ خط، ویرایش متن/برچسب،
 * تکثیر، قرینه، بزرگ/کوچک‌تر، حذف، سبک خط سه‌حالته، توپرِ ملایم، چهار ابزار تازه، کپی صفحه،
 * ذخیرهٔ PNG و تاریخچهٔ چهل‌مرحله‌ای برگشت/جلو (همه روی کلیدهای ماوس و لمس یکسان).
 */
class V335_18_WhiteboardSelectionEditAppTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt").isFile) {
            dir = dir.parentFile
        }
        return dir ?: File("")
    }

    private fun src(path: String): String = File(root(), path).readText()

    @Test
    fun whiteboardHasSelectionMoveResizeEditDuplicateFlipAndDelete() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        // مدل انتخاب چندگانه و ابزار انتخاب
        assertTrue("multiPick" in board)
        assertTrue("val selectedIds = remember(questionId) { mutableStateListOf<Long>() }" in board)
        assertTrue("انتخاب و ویرایش موارد درج‌شده" in board)
        assertTrue("↖ ابزار انتخاب" in board)
        assertTrue("⊕ انتخاب چندگانه" in board)
        assertTrue("⊞ انتخاب همه" in board)
        assertTrue("✏️ ویرایش" in board)
        assertTrue("⧉ تکثیر مورد" in board)
        assertTrue("➕ بزرگ‌تر" in board)
        assertTrue("➖ کوچک‌تر" in board)
        assertTrue("⇄ قرینه" in board)
        assertTrue("🗑 حذف مورد" in board)
        // موتور تغییر اندازه و جابه‌جایی
        assertTrue("internal enum class BoardHandle(val hx: Float, val hy: Float)" in board)
        assertTrue("internal fun boardHandleAt(box: BoardBox, p: Offset, density: Float, endpointItem: BoardStroke?): BoardHandle?" in board)
        assertTrue("internal fun resizedBoardBox(box: BoardBox, h: BoardHandle, dx: Float, dy: Float, keepRatio: Boolean, minSize: Float): BoardBox" in board)
        assertTrue("internal fun scaleBoardStroke(s: BoardStroke, from: BoardBox, to: BoardBox): BoardStroke" in board)
        assertTrue("internal fun flipBoardStroke(s: BoardStroke, horizontal: Boolean): BoardStroke" in board)
        assertTrue("resizedBoardBox(sb, hh, acc.x, acc.y, shiftDown, 16f * density)" in board)
        assertTrue("startItems.forEach { replaceOne(it.moved(acc)) }" in board)
        assertTrue("marquee = BoardBox(p.x, p.y, p.x, p.y)" in board)
        assertTrue("boardHandlePoint(box, h)" in board)
        // عملیات‌های بخش انتخاب
        assertTrue("fun duplicateSelected()" in board)
        assertTrue("fun flipSelected(horizontal: Boolean)" in board)
        assertTrue("fun scaleSelected(factor: Float)" in board)
        assertTrue("fun deleteSelected()" in board)
        assertTrue("fun nudgeBy(dx: Float, dy: Float)" in board)
        assertTrue("fun selectAllOnPage()" in board)
        assertTrue("fun openEditorForSelection()" in board)
        // میان‌برها
        assertTrue("e.isCtrlPressed && e.key == Key.A" in board)
        assertTrue("e.isCtrlPressed && e.key == Key.D" in board)
        assertTrue("e.key == Key.Delete || e.key == Key.Backspace" in board)
        assertTrue("(if (e.isShiftPressed) 10f else 2f) * density" in board)
        // ویرایش متن/برچسب و چشم‌پوشی از کادر انتخاب در خروجی
        assertTrue("برچسب داخل شکل (اختیاری)" in board)
        assertTrue("renderBoard(canvas, w.toFloat(), h.toFloat(), background, strokes, density, bitmaps, emptySet())" in board)
        assertFalse("selectedId?.let { id -> items.firstOrNull { it.id == id } }" in board)
    }

    @Test
    fun whiteboardCarriesShapesStrokeStylesPagesAndPngExport() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        // چهار ابزار تازه + آیکن‌های برداری اختصاصی
        assertTrue("ARROW2(\"پیکان دوسر\")" in board)
        assertTrue("RTRIANGLE(\"مثلث قائم\")" in board)
        assertTrue("DIAMOND(\"لوزی\")" in board)
        assertTrue("AXES(\"محور سریع\")" in board)
        assertTrue("ARROW2 -> BoardVectorIcons.DoubleArrow" in board)
        assertTrue("RTRIANGLE -> BoardVectorIcons.RightTriangle" in board)
        assertTrue("AXES -> BoardVectorIcons.Axes" in board)
        assertTrue("internal object BoardVectorIcons {" in board)
        assertTrue("arrowHead(b, a, paint.strokeWidth)" in board)
        assertTrue("private fun drawQuickAxes(canvas: Canvas, r: RectF, s: BoardStroke, density: Float)" in board)
        assertTrue("private fun shapePathOf(tool: BoardTool, r: RectF): Path?" in board)
        // سبک خط و توپرِ ملایم
        assertTrue("internal enum class BoardDash(val label: String)" in board)
        assertTrue("DASHED(\"خط‌چین\")" in board)
        assertTrue("DOTTED(\"نقطه‌چین\")" in board)
        assertTrue("private fun dashEffectOf(s: BoardStroke, density: Float)" in board)
        assertTrue("▣ توپر ملایم" in board)
        assertTrue("private fun drawShapeLabel(canvas: Canvas, s: BoardStroke, box: BoardBox, density: Float)" in board)
        assertTrue("drawShapeLabel(canvas, s, boundsOf(s, density), density)" in board)
        // کپی صفحه، ذخیرهٔ PNG و تاریخچه
        assertTrue("⧉ کپی صفحه" in board)
        assertTrue("⬇ PNG" in board)
        assertTrue("imagePicker.launch(\"image/*\")" in board)
        assertTrue("private fun exportBoardPng(" in board)
        assertTrue("DocumentIo.writeBytes(context, uri, bytes)" in board)
        assertTrue("private const val UNDO_LIMIT = 40" in board)
        assertTrue("undoStack.add(strokes.toList())" in board)
        // ذخیرهٔ پیش‌نویس با فیلدهای تازه (سازگار با پیش‌نویس قدیمی)
        assertTrue("\"lbl\" to JsonPrimitive(s.label)" in board)
        assertTrue("\"dash\" to JsonPrimitive(s.dash.name)" in board)
        assertTrue("\"fill\" to JsonPrimitive(s.fill)" in board)
        // راهنما
        assertTrue("HelpRow(BoardTool.ARROW2.icon, \"پیکان دوسر\"," in board)
        assertTrue("HelpRow(BoardTool.AXES.icon, \"محور سریع\"," in board)
        assertTrue("HelpRow(Icons.Outlined.ContentCopy, \"تکثیر مورد\"," in board)
    }
}
