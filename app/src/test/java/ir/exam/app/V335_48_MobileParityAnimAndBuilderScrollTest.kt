package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.48 — هم‌سانیِ گوشی با اپ در دو جا:
 *
 * ۱) گذارِ صفحه‌ها: اپ در `ExamApp.kt` با `AnimatedContent` هر صفحه را جداگانه می‌آورد
 *    (کیف پول عمودی ۵۸۰، خانه از چپ ۵۸۰، کارت‌ها مقیاسی ۶۶۰، بقیه از راست ۵۲۰).
 *    سایت تنها انیمیشن ورودِ کارت‌ها را داشت و خودِ صفحه بی‌گذار ظاهر می‌شد.
 * ۲) کارت سؤال: در اپ `scrollQuestionToHeader` کارت را زیر نوار بالا می‌آورد؛ در سایت
 *    کارتِ بازشده پایینِ صفحه می‌ماند و «نصفه» دیده می‌شد.
 */
class V335_48_MobileParityAnimAndBuilderScrollTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val css = src("site/src/site.css")
    private val mobile = src("site/src/mobile.js")
    private val builder = src("site/src/builder.js")

    @Test
    fun `page transitions mirror the native AnimatedContent specs`() {
        assertTrue("کیف پول عمودی", "@keyframes m-page-up" in css)
        assertTrue("خانه از چپ", "@keyframes m-page-from-left" in css)
        assertTrue("بقیه از راست", "@keyframes m-page-from-right" in css)
        assertTrue("کارت‌ها مقیاسی", "@keyframes m-page-scale" in css)
        assertTrue("محو مشترک", "@keyframes m-page-fade" in css)
        assertTrue("۵۸۰ میلی‌ثانیه", "m-page-up 580ms" in css)
        assertTrue("۶۶۰ میلی‌ثانیه", "m-page-scale 660ms" in css)
        assertTrue("۵۲۰ میلی‌ثانیه", "m-page-from-right 520ms" in css)
        assertTrue("احترام به کاهش حرکت", ".m-mode .m-content{animation:none!important}" in css)
    }

    @Test
    fun `mobile shell tags each panel with its transition`() {
        assertTrue("تابع نگاشت", "function pageAnim(page)" in mobile)
        assertTrue("کیف پول", "'m-pg-wallet'" in mobile)
        assertTrue("کارت‌ها", "'m-pg-cards'" in mobile)
        assertTrue("داشبورد", "'m-pg-home'" in mobile)
        assertTrue("پیش‌فرض", "'m-pg-other'" in mobile)
        assertTrue("روی محتوای معلم/مدیر", "+ ' ' + pageAnim(page)" in mobile)
        assertTrue("روی محتوای دانش‌آموز", "m-student-content ' + pageAnim(page)" in mobile)
    }

    @Test
    fun `builder scrolls the opened question under the header`() {
        assertTrue("هم‌ارز scrollQuestionToHeader", "function mScrollToQuestion(i)" in builder)
        assertTrue("دو فریم صبر", "requestAnimationFrame(function ()" in builder)
        assertTrue("جبران نوار بالا", "var off = (head ? Math.round(head.getBoundingClientRect().height) : 56) + 6;" in builder)
        assertTrue("کلیک روی کارت", "drawEditor(); mScrollToQuestion(i);" in builder)
        assertTrue("افزودن از ردیف دکمه‌ها", "drawEditor(); mScrollToQuestion(state.selected);" in builder)
        assertTrue("میان‌بر سؤال تازه", "mScrollToQuestion(state.selected); };" in builder)
    }
}
