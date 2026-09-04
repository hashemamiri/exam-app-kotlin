package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.8 — پاک‌سازی کدِ مردهٔ «ویرایشگر فرمول دقیق» از asset چاپ:
 * `EXACT_MATH_EDITOR_B64` رشتهٔ خالی بود و توابع `exactMathEditorHtml` و
 * `bootExactMathEditor` هیچ فراخوانی نداشتند (مسیرِ مرده و شکسته).
 * این تست هم حذفشان را تثبیت می‌کند و هم مطمئن می‌شود ابزارهای زنده
 * (ویرایشگر فرمولِ واقعی، استودیوی تصویر و پل‌های بومی) دست‌نخورده مانده‌اند.
 */
class V76_8AssetDeadCodeCleanupTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val assetText by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `dead exact math editor code is gone`() {
        assertFalse("EXACT_MATH_EDITOR_B64" in assetText)
        assertFalse("function exactMathEditorHtml" in assetText)
        assertFalse("function bootExactMathEditor" in assetText)
    }

    @Test
    fun `live tooling survived the cleanup`() {
        assertTrue("MATH_EDITOR_HTML" in assetText)
        assertTrue("id=\"mathEditorFrame\"" in assetText)
        // V77.1 — استودیوی HTML عمداً حذف شد؛ دیگر نباید باشد
        assertFalse("id=\"qimgStudioSrc\"" in assetText)
    }

    @Test
    fun `native bridges are untouched`() {
        assertTrue("window.__qmfAddQuestionImage" in assetText)
        assertTrue("window.__qmfExportJson" in assetText)
        assertTrue("window.__qmfSetFields" in assetText)
        assertTrue("window.__qmfSaveNow" in assetText)
        assertTrue("window.__qmfQuestionImages" in assetText)
        assertTrue("window.__qmfRemoveQuestionImage" in assetText)
        assertTrue("window.__qmfReplaceQuestionImage" in assetText)
        assertTrue("window.__qmfSplitQuestion" in assetText)
        // V77.1 — پلِ استودیوی HTML عمداً حذف شده است
        assertFalse("window.__qmfOpenLegacyStudio" in assetText)
    }
}
