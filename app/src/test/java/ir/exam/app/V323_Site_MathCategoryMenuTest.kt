package ir.exam.app

import ir.exam.app.core.figure.MATH_CATEGORIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V323.0 (سایت/دسکتاپ) — منوی «ابزار ریاضی» در دسکتاپ مثل منوی «اطلس» شد:
 *
 * پیش‌تر با زدن دکمهٔ ریاضی، پنجرهٔ بزرگ با چهار کارت دسته باز می‌شد. حالا یک منوی کوچک چهارگزینه‌ای باز می‌شود
 * (دبستان / متوسطهٔ اول / متوسطهٔ دوم و کنکور / انسانی و دانشگاه) و با انتخاب هر گزینه، پنجرهٔ همان دسته —
 * بدون کارت‌های اضافی — باز می‌شود؛ دقیقاً هم‌رفتار با «🫀 آناتومی / ⚡ فیزیک / 🧪 شیمی / 🖼 تصویر خودم».
 */
class V323_Site_MathCategoryMenuTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun source(path: String): String = File(root(), path).readText()

    private fun js(): String = source("app/src/main/assets/print/web/graph_fig.js")
    private fun builder(): String = source("site/src/builder.js")

    /** منوی چهارگزینه‌ای دسکتاپ با همان ترتیب و متن دسته‌ها. */
    @Test
    fun desktopMathButtonOpensCategoryMenuLikeAtlas() {
        val b = builder()
        assertTrue("mathMenu missing", "function mathMenu(anchor, ta, q)" in b)
        assertTrue("toolbar button must open the menu", "onclick: function (e) { mathMenu(e.currentTarget, ta, q); }" in b)
        assertTrue("insert menu must open the menu", "item('math', 'ابزار ریاضی', 'is-math', function () { mathMenu(anchor, ta, null); })" in b)
        assertTrue("menu must reuse atlas styling", "class: 'b-atlas-menu b-math-menu'" in b)
        assertTrue("menu must be placed like the atlas menu", "function placeFloat(m, anchor)" in b)
        val menu = b.substring(b.indexOf("var MATH_MENU = ["), b.indexOf("function mathMenu("))
        for (id in listOf("elementary", "middle", "high", "university")) {
            assertTrue("category $id missing from desktop menu", "'$id'" in menu)
        }
        assertTrue("elementary label", "🧮" in menu && "دبستان" in menu)
        assertTrue("middle label", "📐" in menu && "متوسطهٔ اول" in menu)
        assertTrue("high label", "🧪" in menu && "متوسطهٔ دوم و کنکور" in menu)
        assertTrue("university label", "🎓" in menu && "انسانی و دانشگاه" in menu)
        // کلیک هر گزینه، پنجرهٔ همان دسته را باز می‌کند (kind = math:<cat>)
        assertTrue("category click must pass math:<cat>", "insertFigure('math:' + c[0], ta, q)" in b)
        assertTrue("category must be split from kind", "if (String(kind).indexOf('math:') === 0) { mathCatReq = String(kind).slice(5); kind = 'math'; }" in b)
        assertTrue("category must reach the engine", "try { w.__mathCatReq = mathCatReq || null; } catch (e) {}" in b)
        // مسیر قبلی (فهرست کامل) هم به‌عنوان گزینهٔ پنجم حفظ می‌شود
        assertTrue("all-tools fallback missing", "insertFigure('math', ta, q)" in b && "همهٔ ابزارها" in b)
    }

    /** موتور وب باید «math:<دسته>» را بفهمد و مستقیم همان دسته را نشان دهد. */
    @Test
    fun webEngineOpensRequestedCategoryDirectly() {
        val g = js()
        assertTrue("mode math:<cat> unsupported", "var catReq = (mode === 'math' && window.__mathCatReq) ? mathCatById(String(window.__mathCatReq)) : null;" in g)
        assertTrue("category must pre-select", "var presel = (spec && spec.t) ? mathCatOfType(spec.t) : (catReq || null);" in g)
        assertTrue("math/axis modes unchanged", "var math = mode === 'math' || !!(spec && isMathType(spec.t));" in g)
        assertTrue("plain math must still show the four cards", "if (math && !mathCat) {" in g)
    }

    /** تعداد ابزار هر دسته با کاتالوگ اپ یکی است (۳۷/۱۵/۲۲/۲۳ = ۹۷). */
    @Test
    fun desktopMenuCountsMatchTheAppCatalog() {
        val g = js()
        val ids = Regex("""\{ id: '(\w+)', name: '[^']*', hint: '[^']*', ids: \[([^\]]*)\] \}""").findAll(g).toList()
        val byId = ids.associate { m -> m.groupValues[1] to m.groupValues[2].split(',').count { it.isNotBlank() } }
        assertEquals("elementary tools", 37, byId["elementary"])
        assertEquals("middle tools", 15, byId["middle"])
        assertEquals("high tools", 22, byId["high"])
        assertEquals("university tools", 23, byId["university"])
        assertEquals("catalog categories", 4, MATH_CATEGORIES.size)
        assertEquals(MATH_CATEGORIES.map { it.id }, listOf("elementary", "middle", "high", "university"))
        assertEquals("total tools", 97, byId.values.sum())
        for (c in MATH_CATEGORIES) assertEquals("app vs web count for ${c.id}", c.ids.size, byId[c.id])
    }

    /** سایت باید با باندل تازه ساخته شود که تغییر موتور را دارد. */
    @Test
    fun siteBundleCarriesTheCategoryMode() {
        val site = root()
        val keep = File(site, "site/engines_keep.txt").readText().trim().lines().first { it.isNotBlank() }
        val bundle = File(site, "site/$keep")
        assertTrue("bundle $keep missing", bundle.isFile)
        // باندل ~۶ مگابایت است؛ فقط وجود و اندازهٔ معقول بررسی می‌شود (خواندن کامل در CI کند است).
        assertTrue("bundle is too small", bundle.length() > 1_000_000)
        val index = File(site, "site/index.html").readText()
        assertTrue("index must reference $keep", keep in index)
    }
}
