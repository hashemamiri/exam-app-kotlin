package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V292 — کتابخانهٔ «رشته‌های دانشگاهی» در ویرایشگر فرمول (اپ و سایت از یک formula.html). */
class V292_MajorsLibraryTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun majorsGroupReplacesSubjectGroup() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("/* V292 — کتابخانهٔ «رشته‌های دانشگاهی»" in f)
        assertTrue("/* V288 — کتابخانهٔ «دانشگاه و تحصیلات تکمیلی»" !in f)
        assertTrue("b.textContent = '🎓 رشته‌های دانشگاهی';" in f)
        for (m in listOf("maj-math", "maj-phys", "maj-chem", "maj-stat", "maj-cs", "maj-ee", "maj-me", "maj-ce", "maj-cheme", "maj-mat", "maj-ie", "maj-aero", "maj-pet", "maj-arch", "maj-geo", "maj-med", "maj-pharm", "maj-nurs", "maj-bio", "maj-agri", "maj-econ", "maj-acc", "maj-mgmt", "maj-psy", "maj-bme", "maj-robot", "maj-poly", "maj-env", "maj-astro", "maj-gis", "maj-dent", "maj-vet", "maj-nutr", "maj-sport", "maj-radiol", "maj-act", "maj-nuc", "maj-marine", "maj-trans", "maj-optic", "maj-crypto", "maj-ai", "maj-epid", "maj-physio", "maj-audio", "maj-soc", "maj-food", "maj-music")) { // V295: ۴۸ رشته
            assertTrue(m, "[\"$m\", \"" in f)
        }
        for (b in listOf("n-math", "n-phys", "n-chem", "n-stat", "n-cs", "n-ee", "n-me", "n-ce", "n-med", "n-pharm", "n-nurs", "n-bio", "n-agri", "n-econ", "n-acc", "n-mgmt", "n-psy", "n-cheme", "n-mat", "n-ie", "n-arch", "n-pet", "n-aero", "n-geo", "n-bme", "n-dent", "n-vet", "n-nutr", "n-sport", "n-radiol", "n-poly", "n-env", "n-robot", "n-act", "n-astro", "n-gis", "n-nuc", "n-marine", "n-trans", "n-optic", "n-crypto", "n-ai", "n-epid", "n-physio", "n-audio", "n-soc", "n-food", "n-music")) {
            assertTrue(b, "    \"$b\": [ /* " in f)
        }
        // هر رشته فهرست یکپارچه است؛ تکراری‌ها بین بلوک‌ها حذف می‌شوند
        assertTrue("if (!seen[it[1]]) { seen[it[1]] = 1; out.push(it); }" in f)
        assertTrue("\"تنش فون‌میزس\"" in f && "\"معادلهٔ نرنست\"" in f && "\"توجه (Attention)\"" in f && "\"ظرفیت باربری ترزاقی\"" in f)
        assertTrue("\\\\bmod" !in f.substring(f.indexOf("/* V292")))
        assertTrue("\\\\Big|" !in f)
        // V293: شبکهٔ چندستونه با سرفصل گروه‌های آزمایشی؛ کلاس پس از انتخاب رشته برداشته می‌شود
        assertTrue("function decorateUniPopover() {" in f && "pop.classList.add('word-cats');" in f)
        assertTrue("W.mbShowSymbolCategory = function () { var pop = document.getElementById('mbVar'); if (pop) pop.classList.remove('word-cats');" in f)
        // V295: مرحلهٔ ۴ (۱۲ رشتهٔ جدید)
        assertTrue("\"فرمول شش عاملی\"" in f && "\"ارتفاع متاسنتریک\"" in f && "\"هدف PPO\"" in f && "\"زمان واخنش سابین\"" in f)
        // V294: مرحلهٔ ۳ (۱۲ رشتهٔ جدید) + جادادن دوبارهٔ پنجره در موبایل؛ بدون \\AA و \\mathring
        assertTrue("\"مدل هاجکین-هاکسلی\"" in f && "\"امید زندگی\"" in f && "\"انرژی فوتون\"" in f)
        assertTrue("/* V294: پس از پهن‌شدنِ پنجره" in f)
        assertTrue("\\\\AA" !in f.substring(f.indexOf("/* V292")) && "\\\\mathring" !in f.substring(f.indexOf("/* V292")))
        assertTrue("\"کلیرانس کراتینین (کاکرافت-گالت)\"" in f && "\"معادلهٔ موشک تسیولکوفسکی\"" in f && "\"آلفای کرونباخ\"" in f && "\"معادلهٔ آرچی\"" in f)
    }

    @Test
    fun siteEmbedsSameEditor() {
        val keep = src("site/engines_keep.txt").lines().first { it.isNotBlank() }.trim()
        assertTrue(keep, "🎓 رشته‌های دانشگاهی" in src("site/$keep"))
    }
}
