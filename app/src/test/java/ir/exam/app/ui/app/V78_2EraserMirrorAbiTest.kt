package ir.exam.app.ui.app

import androidx.compose.ui.geometry.Offset
import ir.exam.app.ui.printing.ExamDraftMirror
import ir.exam.app.ui.printing.StudioShape
import ir.exam.app.ui.printing.hitShapeIndex
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V78.2 — سه کارِ پایانیِ فاز ۲:
 *  ۱) پاک‌کنِ مستقل در استودیوی تصویر (با همان hit-test انتخاب)
 *  ۲) آینهٔ بومیِ پیش‌نویس، تا پاک‌شدنِ کشِ WebView کارِ کاربر را نبرد
 *  ۳) حذف x86/x86_64 از ABI — حدود ۶٫۴MB کوچک‌تر شدنِ APK
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

    // ---------- آینهٔ پیش‌نویس ----------

    @Test
    fun `draft bridges exist`() {
        assertTrue("window.__qmfDraftSnapshot" in assetText)
        assertTrue("window.__qmfHasLocalDraft" in assetText)
        // آینه فقط وقتی به کار می‌آید که localStorage خالی باشد
        assertTrue("qmf_exam_autosave_azmoon_v1" in assetText)
    }

    @Test
    fun `mirror only accepts something that looks like a draft`() {
        assertTrue(ExamDraftMirror.looksLikeDraft("""{"questions":[],"fields":{}}"""))
        assertFalse(ExamDraftMirror.looksLikeDraft(""))
        assertFalse(ExamDraftMirror.looksLikeDraft("null"))
        assertFalse(ExamDraftMirror.looksLikeDraft("[1,2,3]"))
        assertFalse(ExamDraftMirror.looksLikeDraft("""{"other":1}"""))
        assertFalse(ExamDraftMirror.looksLikeDraft("not json at all"))
    }

    @Test
    fun `mirror is wired into the print dialog`() {
        val dialog = source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("ExamDraftMirror.save(" in dialog)
        assertTrue("ExamDraftMirror.load(" in dialog)
        assertTrue("mirrorDraft()" in dialog)
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
