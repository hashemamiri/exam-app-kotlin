package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V54.6 — رفع «Maximum call stack size exceeded» گزارش‌شدهٔ دستگاه:
 * زنجیرهٔ بازگشت در پل افزودهٔ exam-editor-bridge (دورهٔ V45.4):
 * input → emit → value() → QMF.syncFromSurface → writeSrc → dispatch('input')
 * → دوباره emit → ... این RangeError کل JS صفحه را پیش از boot ویرایشگر فرمول
 * می‌کشت؛ به همین دلیل پنجرهٔ فرمول فقط صفحهٔ متن سؤال را نشان می‌داد.
 */
class V54_6BridgeRecursionFixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/question_editor/question_editor.html").readText()
    }

    @Test
    fun `emit has a reentry lock and respects the reference fromSurface flag`() {
        val bridge = asset.substringAfter("<script id=\"exam-editor-bridge\">")
            .substringBefore("</script>")
        assertTrue("var emitting = false;" in bridge)
        assertTrue("if (emitting) return;" in bridge)
        assertTrue("finally { emitting = false; }" in bridge)
        // در رویداد مصنوعی writeSrc (پرچم مرجع) دوباره sync نمی‌شود.
        assertTrue("!t._qmfFromSurface" in bridge)
        // listener فقط emit می‌کند؛ qMathSync دوباره داخل listener صدا نمی‌شود.
        assertFalse("qMathSync(t.id); } catch (e) {} emit();" in bridge)
    }
}
