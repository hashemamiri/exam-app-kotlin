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
        for (m in listOf("maj-math", "maj-phys", "maj-chem", "maj-stat", "maj-cs", "maj-ee", "maj-me", "maj-ce", "maj-econ", "maj-bio", "maj-earth", "maj-chemeng")) {
            assertTrue(m, "[\"$m\", \"" in f)
        }
        for (b in listOf("n-math", "n-phys", "n-chem", "n-stat", "n-cs", "n-ee", "n-me", "n-ce")) {
            assertTrue(b, "    \"$b\": [ /* " in f)
        }
        // هر رشته فهرست یکپارچه است؛ تکراری‌ها بین بلوک‌ها حذف می‌شوند
        assertTrue("if (!seen[it[1]]) { seen[it[1]] = 1; out.push(it); }" in f)
        assertTrue("\"تنش فون‌میزس\"" in f && "\"معادلهٔ نرنست\"" in f && "\"توجه (Attention)\"" in f && "\"ظرفیت باربری ترزاقی\"" in f)
        assertTrue("\\\\bmod" !in f.substring(f.indexOf("/* V292")))
        assertTrue("\\\\Big|" !in f)
    }

    @Test
    fun siteEmbedsSameEditor() {
        val keep = src("site/engines_keep.txt").lines().first { it.isNotBlank() }.trim()
        assertTrue(keep, "🎓 رشته‌های دانشگاهی" in src("site/$keep"))
    }
}
