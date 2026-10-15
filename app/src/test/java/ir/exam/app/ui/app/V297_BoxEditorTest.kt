package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V297 — بازبینی ویرایشگر جعبه‌ای فرمول (ناوبری، پاک‌کردن، توان/اندیس، بازخوانی TeX). اپ و سایت از یک formula.html. */
class V297_BoxEditorTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun f(): String = File(root(), "app/src/main/assets/formula_editor/formula.html").readText()

    @Test
    fun navigationAndDeletion() {
        val s = f()
        assertTrue("function mbAllSlotsOf(" in s && "function mbNodeIsEmpty(" in s)      // ← → بین خانه‌های کسر/ماتریس
        assertTrue("if (inf2.node && inf2.node.t === 'par')" in s)                           // «)» تا پرانتز دربرگیرنده بیرون می‌رود
        assertTrue("const hasBase = (i > 0) && (mbAtomStart(arr, i) < i);" in s)             // x² بعد از پایه
        assertTrue("MB.cur = { arr: n.a, i: 0 }; /* V297" in s)                               // ∛ مکان‌نما زیر رادیکال
    }

    @Test
    fun scriptsAndAtoms() {
        val s = f()
        assertTrue("const MB_ATOM_OPSYM = " in s)                                             // π/2 → \frac{π}{2}
        assertTrue("\\u0370-\\u03FF\\u221E" in s)                                             // حروف یونانی اتم‌اند
        assertTrue("MathParser.prototype.mergeScript = function" in s)                        // x^{2}_{i} روی‌هم
        assertTrue("function mbCleanEmpty(" in s && "MB_CLEAN_NOTE > 0" in s)                 // جعبه‌های خالی هنگام درج
        assertTrue("!/^[a-zA-Z]+$/.test(w)" in s)                                             // تایپ سریع: a_{ij}
    }

    @Test
    fun texReparseIsLossless() {
        val s = f()
        assertTrue("if (!name && (t[n] === ',' || t[n] === ';' || t[n] === ':'))" in s)     // \alpha,\beta حذف نمی‌شود
        assertTrue("inner.forEach(x => { if (x.t === 'c' || x.t === 'text') run += x.v; else { flush(); out.push(x); } });" in s) // \mathrm{m/s^{2}}
        assertTrue("return [mbChar(' '), mbText('(mod '" in s)                               // \pmod
    }
}
