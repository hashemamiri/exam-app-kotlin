package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V167 — ریل دسکتاپ معلم: «ابزارها» حذف، «کارت‌ها» اضافه (همان ۷ کارت گرادیانی گوشی به‌صورت شبکه). */
class V167_SiteDesktopCardsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `teacher menu has cards instead of tools and cards page mirrors phone deck`() {
        val a = source("site/src/app.js")
        assertTrue("['wallet', '👛', 'کیف پول'], ['cards', '🃏', 'کارت‌ها']" in a)
        assertFalse("['wallet', '👛', 'کیف پول'], ['tools', '🧮', 'ابزارها'], '-', ['profile', '👤', 'پروفایل']\n    ]," in a)
        assertTrue("function pageCards(c)" in a && "cards: pageCards" in a && "M.teacherCards().forEach(function (k)" in a && "class: 'dk-gcard', style: 'background:' + k[3], onclick: k[4]" in a)
        val m = source("site/src/mobile.js")
        assertTrue("function teacherCards() {" in m && "function cardsScreen(c) { cardsDeck(c, 'teacher', teacherCards()); }" in m && "teacherCards: teacherCards, icons: I}" in m)
        for (t in listOf("'آمار'", "'کارنامه'", "'بانک سؤال'", "'تصحیح'", "'مانده'", "'پاسخ'", "'درخواست‌ها'")) assertTrue(t, "[$t, " in m)
        val css = source("site/src/site.css")
        assertTrue(".dk-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr))" in css && ".dk-gcard{min-height:190px;border:0;border-radius:29px;" in css)
    }
}
