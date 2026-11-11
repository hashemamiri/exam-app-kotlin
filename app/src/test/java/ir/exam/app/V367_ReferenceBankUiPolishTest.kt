package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V367 — صیقل پنجرهٔ بانک مرجع (خواستهٔ کاربر):
 *  ۱) نیم‌فاصله: «پایه‌ها»، «درس‌ها»، «فصل‌ها»، «سطح‌ها»، «رشته‌ها»، «نوع‌ها»، «انتخاب‌شده»، «سؤال‌ها».
 *  ۲) سایت: همهٔ ابزارهای فهرست در یک ردیف هدر و ثابت.
 *  ۳) اپ: دکمه‌های «افزودن پاسخ» آیکن‌اند (هیچ برچسب متنی نمانده) و دسترس‌پذیری حفظ شده.
 *  ۴) «سطح» شناختی از فیلد area خوانده می‌شود و تا داده نرسد چیزی نشان داده نمی‌شود.
 */
class V367_ReferenceBankUiPolishTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.isFile)
        return f.readText()
    }

    @Test
    fun labelsUseZwnjAndNoSpacedPluralsRemain() {
        val site = src("site/src/refbank.js")
        assertTrue("همهٔ پایه‌ها" in site)
        assertTrue("همهٔ درس‌ها" in site)
        assertTrue("همهٔ فصل‌ها" in site)
        assertTrue("همهٔ سطح‌ها" in site)
        assertTrue("همهٔ رشته‌ها" in site)
        assertTrue("همهٔ نوع‌ها" in site)
        assertTrue("انتخاب‌شده" in site)
        assertTrue("سؤال‌ها" in site)
        assertFalse("همهٔ پایه ها" in site)
        assertFalse("انتخاب شده" in site)
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("همهٔ پایه‌ها" in dialog)
        assertTrue("همهٔ درس‌ها" in dialog)
        assertTrue("همهٔ فصل‌ها" in dialog)
        assertTrue("همهٔ سطح‌ها" in dialog)
        assertTrue("انتخاب‌شده" in dialog)
        assertFalse("همهٔ پایه ها" in dialog)
        assertFalse("انتخاب شده" in dialog)
    }

    @Test
    fun siteResultsHeaderIsOneStickyRow() {
        val site = src("site/src/refbank.js")
        assertTrue("refbank-one-row" in site)
        assertTrue("ابزارهای بانک سؤال" in site)
        assertTrue("انتخاب/برداشتن صفحه" in site)
        assertTrue("افزودن گروهی" in site)
        assertTrue("refbank-search-input" in site)
        val css = src("site/src/site.css")
        assertTrue(".refbank-one-row{" in css)
        assertTrue(".refbank-header{position:sticky" in css)
    }

    @Test
    fun appAnswerButtonsAreIconsOnly() {
        val dialog = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
        assertTrue("contentDescription = \"افزودن پاسخ\"" in dialog)
        assertFalse("FilterChip" in dialog)
        assertFalse("leadingIcon" in dialog)
        assertTrue("Icons.Outlined.Check" in dialog)
    }

    @Test
    fun cognitiveLevelReadsAreaAndStaysHiddenWhenEmpty() {
        val site = src("site/src/refbank.js")
        assertTrue("function levelOf(r)" in site)
        assertTrue("r.area || r.area_fa" in site)
    }
}
