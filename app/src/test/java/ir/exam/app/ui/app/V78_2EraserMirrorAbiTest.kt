package ir.exam.app.ui.app

import androidx.compose.ui.geometry.Offset
import ir.exam.app.ui.printing.StudioShape
import ir.exam.app.ui.printing.hitShapeIndex
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V78.2 — دو کارِ فاز ۲:
 *  ۱) پاک‌کنِ مستقل در استودیوی تصویر (با همان hit-test انتخاب)
 *  ۳) حذف x86/x86_64 از ABI — حدود ۶٫۴MB کوچک‌تر شدنِ APK
 *
 * V100 — دو تستِ «آینهٔ بومیِ پیش‌نویس» (ExamDraftMirror) با حذفِ کاملِ
 * «آزمون‌ساز چاپی» حذف شدند: پنجرهٔ چاپ دیگر پیش‌نویسِ خودکار بازیابی نمی‌کند.
 */
class V78_2EraserMirrorAbiTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }
    private val assetText by lazy { source("app/src/main/assets/print/exam_print.html") }
    private val gradle by lazy { source("app/build.gradle.kts") }

    // ---------- پاک‌کن ----------

    @Test
    fun `eraser tool exists next to the other draw tools`() {
        assertTrue("ToolChip(\"🧹 پاک‌کن\")" in studio)
        assertTrue("setDraw(\"eraser\")" in studio)
        // ابزارهای قبلی نرفته باشند
        listOf("➡️ فلش", "🖍️ هایلایتر", "🚫 سانسور", "💧 قطره‌چکان").forEach {
            assertTrue("ابزار $it گم شده", it in studio)
        }
    }

    @Test
    fun `eraser and selection share one hit test`() {
        assertTrue("internal fun hitShapeIndex(" in studio)
        // هم پاک‌کنِ ضربه‌ای، هم کشیدن، هم انتخاب
        assertTrue(studio.split("hitShapeIndex(shapes, nx, ny)").size - 1 >= 3)
    }

    @Test
    fun `hit test finds the topmost shape and respects locking`() {
        val a = StudioShape(type = "rect", points = listOf(Offset(0.1f, 0.1f), Offset(0.3f, 0.3f)))
        val b = StudioShape(type = "rect", points = listOf(Offset(0.15f, 0.15f), Offset(0.35f, 0.35f)))
        // رویی برنده است
        assertEquals(1, hitShapeIndex(listOf(a, b), 0.2f, 0.2f))
        // خارج از همه
        assertEquals(-1, hitShapeIndex(listOf(a, b), 0.9f, 0.9f))
        // قفل‌شده انتخاب/پاک نمی‌شود
        assertEquals(0, hitShapeIndex(listOf(a, b.copy(locked = true)), 0.2f, 0.2f))
        // پنهان هم مصون است
        assertEquals(0, hitShapeIndex(listOf(a, b.copy(hidden = true)), 0.2f, 0.2f))
        // شکل بی‌نقطه نادیده گرفته می‌شود
        assertEquals(-1, hitShapeIndex(listOf(StudioShape(type = "rect")), 0.2f, 0.2f))
        assertEquals(-1, hitShapeIndex(emptyList(), 0.2f, 0.2f))
    }

    // ---------- پل‌های ماندگارِ صفحه ----------

    @Test
    fun `draft snapshot bridge still exists in the page`() {
        // خودِ پل‌های صفحه می‌مانند (ذخیرهٔ خودکار)؛ مصرف‌کنندهٔ بومیِ آن‌ها
        // (آینهٔ پیش‌نویس) با V100 حذف شد.
        assertTrue("window.__qmfDraftSnapshot" in assetText)
        assertTrue("window.__qmfHasLocalDraft" in assetText)
        assertTrue("qmf_exam_autosave_azmoon_v1" in assetText)
    }

    // ---------- حجم APK ----------

    @Test
    fun `only the two real arm abis are packaged`() {
        assertTrue("abiFilters += listOf(\"armeabi-v7a\", \"arm64-v8a\")" in gradle)
        assertFalse("\"x86\"" in gradle)
        assertFalse("\"x86_64\"" in gradle)
    }

    @Test
    fun `ocr language data is still shipped uncompressed`() {
        // حذف ABI نباید به OCR دست بزند
        assertTrue("noCompress += \"traineddata\"" in gradle)
        assertTrue(File(root(), "app/src/main/assets/tessdata/fas.traineddata").isFile)
    }

    @Test
    fun `out of scope areas remain untouched`() {
        listOf("function printStudent", "function printTeacher", "function renderPreview", "function renderEditor")
            .forEach { assertTrue(it in assetText) }
    }
}
