package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V159 — مشخصات آزمون (عنوان/درس/مدت) داخل پنجرهٔ «مشخصات آزمون» + استودیوی تصویر سایت (آینهٔ ExamImageStudioDialog). */
class V159_SiteImageStudioTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `title subject duration live in settings dialog like ExamSettingsCard`() {
        val b = source("site/src/builder.js")
        assertTrue("inp('عنوان آزمون', state.title, function (v) { state.title = v; mark(); })," in b)
        assertFalse("el('div', {class: 'grid3'}, [title, subject, duration])" in b)
        // چاپی: مثل askPrintName اپ، نام هنگام ذخیره پرسیده می‌شود
        assertTrue("S.promptDlg('ذخیره آزمون چاپی'" in b)
        assertTrue("if (state.mode !== 'print' && !state.title.trim())" in b)
        assertTrue("function promptDlg(title, body, label, value, okLabel)" in source("site/src/app.js"))
    }

    @Test
    fun `image studio mirrors app tabs and tools`() {
        val s = source("site/src/studio.js")
        for (t in listOf("🖼️ تصویر و برش", "✏️ طراحی و علامت", "📐 صاف‌سازی", "📷 دوربین", "🖼️ گالری", "تصویر از کجا بیاید؟",
            "'arrow2'", "'highlighter'", "'censor'", "'eyedropper'", "'eraser'", "تفکیک چندسؤاله", "سفیدسازی اسکن",
            "حذف سایه و زردی", "حذف نویز و لکه", "برش خودکار حاشیه", "تشخیص خودکار زاویه", "هر بخش → سؤال جداگانه")) {
            assertTrue("studio.js باید شامل «$t» باشد", t in s)
        }
        val b = source("site/src/builder.js")
        assertTrue("window.SiteStudio.open({existing: q.images," in b)
        assertTrue("onSplitToQuestions: function (urls)" in b)
        assertTrue("studio.js" in source("site/build_site.py"))
    }
}
