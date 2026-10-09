package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V247 — کیف پول برنامه مثل سایت (دکمهٔ تعرفه‌ها، بدون متن راهنما)، اینست پنجره‌های تمام‌صفحه، و ۲۷ محور در موتور وب. */
class V247_WalletTariffAxisParityTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `wallet screen has tariff dialog and no helper texts`() {
        val w = source("app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt")
        assertTrue("private fun TariffDialog(" in w && "\"تعرفه‌ها\"" in w && "\"بازیابی پشتیبان: هر سؤال\"" in w)
        assertFalse("هزینه هر سؤال:" in w)
        assertFalse("Edge Function" in w)
        assertFalse("پس از بازگشت از درگاه" in w)
        assertFalse("supportingText" in w)
    }

    @Test
    fun `full screen figure dialogs respect system bars`() {
        for (f in listOf("FigurePickerDialog", "AtlasEditorDialog", "PeriodicEditorDialog", "TableEditorDialog", "ZoomableFigureDialog")) {
            val s = source("app/src/main/java/ir/exam/app/ui/figure/$f.kt")
            assertTrue(f, "systemBarsPadding()" in s && "import androidx.compose.foundation.layout.systemBarsPadding" in s)
        }
    }

    @Test
    fun `web graph engine renders all 27 axis types without the native bridge`() {
        val g = source("app/src/main/assets/print/web/graph_fig.js")
        val gallery = source("app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt")
        val ids = Regex("FigureTemplate\\(\"(ax[a-z0-9]+)\"").findAll(gallery).map { it.groupValues[1] }.toSet()
        assertTrue(ids.size == 27)
        assertTrue("function axisExSvg(t, X, title)" in g && "var exSvg = axisExSvg(t, X, title); if (exSvg) return exSvg;" in g)
        for (id in ids) {
            assertTrue(id, "{ id: '$id'," in g)
            assertTrue(id, "'$id'" in g.substringAfter("function axisExSvg(").substringBefore("return null;") || id in listOf("axnum", "axxy", "axq1", "axgrid", "axpol", "ax3d"))
            assertTrue(id, (" $id: [" in g) || id in listOf("axnum", "axxy", "axq1", "axgrid", "axpol", "ax3d"))
        }
    }

    @Test
    fun `site tariff dialog has red close in title row and no close button`() {
        val a = source("site/src/admin.js")
        val d = a.substringAfter("function tariffDlg()").substringBefore("function topUpCard")
        assertTrue("class: 'tariff-head'" in d && "class: 'x tariff-x'" in d)
        assertFalse("text: 'بستن'" in d)
        assertTrue(".modal .x.tariff-x{position:static" in source("site/src/site.css"))
    }
}
