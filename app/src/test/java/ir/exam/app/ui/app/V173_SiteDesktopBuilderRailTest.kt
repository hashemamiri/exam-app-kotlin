package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V173 — ریل سازندهٔ دسکتاپ مثل FABهای ExamBuilderScreen: آنلاین = افزودن/ذخیره/چشم (پیش‌نمایش دانش‌آموز)؛ چاپی = افزودن/ذخیره/چشم (پیش‌نمایش برگه)/چاپ + ستون «تنظیمات سربرگ». */
class V173_SiteDesktopBuilderRailTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `builder rail and side column mirror the app`() {
        val b = source("site/src/builder.js")
        assertTrue("'سؤال بعدی'" !in b)
        assertTrue("rail.appendChild(rb(EYE, 'پیش‌نمایش دانش‌آموز', function () { studentPreview(); }));" in b)
        assertTrue("rail.appendChild(rb(EYE, 'پیش‌نمایش آزمون', function () { preview(); }));" in b)
        assertTrue("'چاپ آزمون', function (e) { printMenu(e.currentTarget); }" in b)
        assertTrue("text: '🖨 چاپ آزمون (دانش‌آموز)'" in b && "text: '✅ چاپ با کلید (پاسخ‌نامه)'" in b)
        assertTrue("function studentPreview()" in b && "text: 'پیش‌نمایش دانش‌آموز'" in b)
        assertTrue("el('aside', {class: 'b-settings b-hdr card'}, [el('h3', {text: 'تنظیمات سربرگ'})]); S.headerSettingsForm(settings, {autosave: true});" in b)
        val a = source("site/src/app.js")
        assertTrue("function headerSettingsForm(box, opts)" in a && "headerSettingsForm: headerSettingsForm" in a)
        assertTrue("richHtml: richHtml" in source("site/src/student.js"))
        val c = source("site/src/site.css")
        assertTrue(".dk .b-settings::-webkit-scrollbar{display:none}" in c)
        assertTrue(".dk .b-rail{overflow:hidden}" in c && ".dk .b-rail-nums{flex:1 1 auto;min-height:0;overflow-y:auto" in c)
    }
}
