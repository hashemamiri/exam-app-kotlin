package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.60 — عکس گالری‌ای که معلم روی وایت‌برد درج می‌کند باید به‌صورت تصویر مستقل سؤال ذخیره شود تا
 * تعرفهٔ موجود (هر تصویر ۱٬۰۰۰ تومان، در صورت‌حساب ذخیرهٔ آزمون) برایش کسر شود.
 */
class V335_60_WhiteboardPhotoCostTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `native board keeps gallery picks apart from question images`() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("insertImage(\"pic:\$uri\")" in board)
        assertTrue("ref.startsWith(\"img:\") || ref.startsWith(\"pic:\")" in board)
        assertTrue("filter { it.startsWith(\"pic:\") }.distinct()" in board)
        assertTrue("exportPickedPhotos(context, questionId, pickedRefs" in board)
        assertTrue("هر تصویر ۱٬۰۰۰ تومان" in board)
    }

    @Test
    fun `builder passes free image slots to the board`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("imageSlotsLeft = (10 - question.images.size).coerceAtLeast(0)" in screen)
    }

    @Test
    fun `website board returns captured photos only for the teacher flow`() {
        val student = src("site/src/student.js")
        assertTrue("var photos = (opts && opts.billSources) ? capturedPhotos.slice() : [];" in student)
        assertTrue("if (out.length || photos.length) onDone(out, photos);" in student)
        val builder = src("site/src/builder.js")
        assertTrue("billSources: true" in builder)
        assertTrue("photos || []" in builder)
    }

    @Test
    fun `no new server billing rule is introduced`() {
        val migration = File(root(), "supabase/migrations/20261015_native_photo_fig_cost_v289.sql").readText()
        // تصویرهای سؤال در همان قاعدهٔ موجود (هر تصویر ۱٬۰۰۰ تومان) حساب می‌شوند.
        assertTrue("v_images * 1000" in migration)
        assertFalse("V335_60" in migration)
    }
}
