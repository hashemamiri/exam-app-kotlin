package ir.exam.app.core.primary

import ir.exam.app.ui.builder.PrimaryBank
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V306 / ۱۲ — بانک آمادهٔ ابتدایی: فایل asset سالم، همهٔ سؤال‌ها قابل افزودن، پاسخ‌ها معتبر. */
class V306_PrimaryBankTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private val items by lazy {
        PrimaryBank.parse(File(root(), "app/src/main/assets/${PrimaryBank.ASSET}").readText(Charsets.UTF_8))
    }

    @Test
    fun `bank has thirty unique items across five subjects and six grades`() {
        assertEquals(30, items.size)
        assertEquals(30, items.map { it.id }.toSet().size)
        assertEquals(5, items.map { it.subject }.toSet().size)
        assertEquals((1..6).toSet(), items.map { it.grade }.toSet())
    }

    @Test
    fun `every item converts to a draft with the right type`() {
        items.forEach { item ->
            val draft = PrimaryBank.toDraft(item)
            assertNotNull("سؤال ${item.id} قابل افزودن نیست", draft)
            assertEquals(item.text, draft!!.text)
            when (item.type) {
                "multiple" -> assertEquals(QuestionType.MULTIPLE_CHOICE, draft.type)
                "fill" -> assertEquals(QuestionType.FILL_BLANK, draft.type)
                "numeric" -> assertEquals(QuestionType.NUMERIC, draft.type)
                "truefalse" -> assertEquals(QuestionType.TRUE_FALSE, draft.type)
                else -> throw AssertionError("نوع ناشناخته: ${item.type}")
            }
        }
    }

    @Test
    fun `answers are valid and stay inside the answer key only`() {
        items.filter { it.type == "numeric" }.forEach { assertTrue(it.answer.toDoubleOrNull() != null) }
        items.filter { it.type == "multiple" }.forEach { assertTrue(it.correct in it.options.indices) }
        items.filter { it.type == "truefalse" }.forEach { assertTrue(it.answer == "true" || it.answer == "false") }
    }

    @Test
    fun `invalid multiple choice without correct index is rejected`() {
        val bad = items.first { it.type == "multiple" }.copy(correct = 99)
        assertEquals(null, PrimaryBank.toDraft(bad))
    }
}
