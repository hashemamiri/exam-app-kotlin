package ir.exam.app.ui.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V289 — «تصویر خودم + فلش‌گذاری» مثل بقیهٔ تصاویر ۱۰۰۰ تومان: شمارش کلاینت (برآورد) = شمارش سرور. */
class V289_PhotoFigureCostTest {
    private val photo = "%%FIG:{\"k\":\"a\",\"t\":\"photo\",\"X\":{\"img\":\"data:image/png;base64,AAAA\",\"marks\":[]}}%%"
    private val atlas = "%%FIG:{\"k\":\"a\",\"t\":\"heart\",\"X\":{}}%%"

    @Test
    fun countsOnlyPhotoFigures() {
        assertEquals(0, countPhotoFigures(null, "", "متن ساده \$x^2\$ ۵۰%% تخفیف"))
        assertEquals(1, countPhotoFigures("الف $photo ب"))
        assertEquals(0, countPhotoFigures("الف $atlas ب"))
        assertEquals(2, countPhotoFigures("$photo$atlas$photo"))
        assertEquals(3, countPhotoFigures("x $photo", photo, atlas, photo))
    }

    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }

    @Test
    fun clientAndServerUseTheSameRule() {
        val draft = File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").readText()
        assertTrue("photoFigureCount() // V289" in draft)
        val sql = File(root(), "supabase/migrations/20261015_native_photo_fig_cost_v289.sql").readText()
        assertTrue("native_photo_fig_images_v289" in sql)
        assertTrue("public.native_photo_fig_images_v289(p_q), 1), 0)" in sql)
        assertTrue("select unnest(public.native_photo_fig_images_v289(q))" in sql)
        assertTrue("or u like 'photo:%'" in sql)
        assertTrue("«تصویر خودم»" in File(root(), "app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt").readText())
        assertTrue("«تصویر خودم»" in File(root(), "site/src/admin.js").readText())
    }
}
