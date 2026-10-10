package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V254 — سازندهٔ سایت: آیکون‌های برداری سربرگ سؤال، «افزودن به بانک»، افزودن یک/چند سؤال از بانک. */
class V254_SiteBuilderBankTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `question header uses svg icons and has add-to-bank`() {
        val b = source("site/src/builder.js")
        for (k in listOf("up", "down", "copy", "bank", "trash")) assertTrue(k, "$k: '<svg viewBox=\"0 0 24 24\"" in b)
        assertTrue("html: QI.up" in b && "html: QI.down" in b && "html: QI.copy" in b && "html: QI.bank" in b && "html: QI.trash" in b)
        assertFalse("html: '↑'" in b || "html: '⧉'" in b || "html: '🗑'" in b)
        assertTrue("title: 'افزودن به بانک سؤال', html: QI.bank, onclick: function () { addToBank(q); }" in b)
        assertTrue("async function addToBank(q0)" in b && "S.rpcObj('native_bank_add_v2', {p_question: comb, p_subject: (state.subject || '').trim(), p_cats: []})" in b)
    }

    @Test
    fun `bank picker supports one or many`() {
        val b = source("site/src/builder.js")
        assertTrue("function addItems(arr)" in b && "text: 'افزودن انتخاب‌شده‌ها'" in b && "text: 'انتخاب همهٔ نتایج'" in b)
        assertTrue("onclick: function () { addItems([it]); }" in b && "picked = lastFiltered.slice()" in b)
        assertTrue(".b-bank-item>input[type=checkbox]" in source("site/src/site.css"))
    }
}
