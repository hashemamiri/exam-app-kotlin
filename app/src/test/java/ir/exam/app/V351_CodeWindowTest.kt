package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V351 — خواسته‌های کاربر روی پنجرهٔ کدنویسی اپ و رنگ پنجرهٔ دانش‌آموز سایت.
 *
 * اپ: کد چپ‌به‌راست، آیکن کنار OCR در ردیف دوم، بی کادر جست‌وجوی زبان،
 * قاب و جایگاه مثل پنجرهٔ دانش‌آموز، بی هدر و بی عنوان‌های راهنما و بی
 * جملهٔ «کد اجرا نمی‌شود».
 * سایت: ضربدر قرمز، ایجاد بنفش، + سبز — همان رنگ‌های اپ.
 */
class V351_CodeWindowTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String): String {
        val f = File(root(), path)
        assertTrue("فایل پیدا نشد: $path", f.exists())
        return f.readText()
    }

    // ---------- ۱) کد چپ‌به‌راست ----------

    @Test
    fun codeIsWrittenLeftToRight() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("جهت متن چپ‌به‌راست", s.contains("textDirection = TextDirection.Ltr"))
        assertTrue("چپ‌چین", s.contains("textAlign = TextAlign.Left"))
        assertTrue(
            "شمارهٔ خط سمت چپ می‌افتد",
            s.contains("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)")
        )
        // هر دو در همان سبک مونو که به کادر و شمارهٔ خط داده می‌شود
        val mono = s.substring(s.indexOf("val mono = TextStyle("), s.indexOf("val caret ="))
        assertTrue(mono.contains("TextDirection.Ltr"))
        assertTrue(mono.contains("TextAlign.Left"))
    }

    @Test
    fun codePreviewIsAlsoLeftToRight() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeBlockView.kt")
        assertTrue("جهت پیش‌نمایش", s.contains("textDirection = TextDirection.Ltr"))
        assertTrue("چپ‌چین پیش‌نمایش", s.contains("textAlign = TextAlign.Left"))
        assertTrue(
            "شمارهٔ خط پیش‌نمایش سمت چپ",
            s.contains("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)")
        )
    }

    // ---------- ۲) آیکن کنار OCR در ردیف دوم ----------

    @Test
    fun codeIconSitsNextToOcrInTheSecondRow() {
        val media = src("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        assertTrue("پارامتر تازه", media.contains("onOpenCode: (() -> Unit)? = null"))
        val ocrAt = media.indexOf("Icons.Outlined.DocumentScanner")
        val codeAt = media.indexOf("QuestionToolIcons.Code")
        assertTrue("آیکن OCR هست", ocrAt > 0)
        assertTrue("آیکن کد بعد از OCR است", codeAt > ocrAt)
        assertTrue("برچسب دسترس‌پذیری", media.contains("contentDescription = \"درج کد برنامه‌نویسی در متن سؤال\""))
        // و دیگر در ردیف اول نیست
        val field = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertFalse("دکمهٔ ردیف اول برداشته شد", field.contains("NativeCodeToolButton"))
    }

    @Test
    fun codeToolGoesThroughTheFieldControllerKeepingTheCaret() {
        val ctl = src("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt")
        assertTrue("مسیر نیتیو", ctl.contains("internal var nativeOpenCode: (() -> Boolean)? = null"))
        assertTrue("شناخت نام ابزار", ctl.contains("if (name == \"code\") {"))
        val field = src("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertTrue("ثبت مسیر", field.contains("controller.nativeOpenCode = {"))
        assertTrue("محل مکان‌نما حفظ می‌شود", field.contains("currentInsertCode(currentInsertAt)"))
        assertTrue("کپچر تازه‌شونده", field.contains("val currentInsertCode by rememberUpdatedState(onInsertCode)"))
        val builder = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("سیم‌کشی دکمه", builder.contains("onOpenCode = { questionFieldController.openTool(\"code\") }"))
    }

    // ---------- ۳) بی کادر جست‌وجوی زبان ----------

    @Test
    fun languagePickerHasNoSearchBox() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeLanguagePickerDialog.kt")
        assertFalse("کادر جست‌وجو رفت", s.contains("OutlinedTextField"))
        assertFalse("وضعیت جست‌وجو رفت", s.contains("var query"))
        assertFalse("برچسب جست‌وجو رفت", s.contains("جست‌وجوی زبان"))
        assertTrue("همهٔ زبان‌ها نشان داده می‌شوند", s.contains("val shown = CodeLanguages.all"))
        assertTrue("۳۲ زبان دست‌نخورده", s.contains("LazyVerticalGrid("))
    }

    // ---------- ۴) قاب و جایگاه مثل پنجرهٔ دانش‌آموز ----------

    @Test
    fun codeWindowUsesTheStudentDialogShell() {
        val code = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        val student = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        // همان چهار مشخصهٔ قاب پنجرهٔ دانش‌آموز
        val shell = listOf(
            "DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)",
            "WindowCompat.setDecorFitsSystemWindows(window, false)",
            "Modifier.fillMaxSize().systemBarsPadding().imePadding()",
            "contentAlignment = Alignment.TopCenter",
            "Modifier.fillMaxWidth().widthIn(max = 620.dp).heightIn(max = availableHeight)"
        )
        shell.forEach { piece ->
            assertTrue("قاب دانش‌آموز: $piece", student.contains(piece))
            assertTrue("قاب کد همان است: $piece", code.contains(piece))
        }
        assertTrue("سطح برجستگی یکسان", code.contains("tonalElevation = 6.dp"))
    }

    // ---------- ۵) هدر، عنوان‌ها و جملهٔ پانویس حذف ----------

    @Test
    fun headerGuideLabelsAndFootnoteAreGone() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertFalse("هدر عنوان رفت", s.contains("Text(title, style = MaterialTheme.typography.titleMedium)"))
        assertFalse("عنوان علائم رفت", s.contains("Text(\"علائم\""))
        assertFalse("عنوان آماده رفت", s.contains("Text(\"آمادهٔ"))
        assertFalse("جملهٔ پانویس رفت", s.contains("کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود. تصحیح دستی است."))
        // خودِ دکمه‌ها می‌مانند (تصمیم کاربر)
        assertTrue("دکمه‌های علائم هستند", s.contains("items(lang.symbols) { sym ->"))
        assertTrue("دکمه‌های آماده هستند", s.contains("items(lang.snippets) { snip ->"))
        assertTrue("ذخیرهٔ متنی ماند", s.contains("Button(onClick = { onDone(lang.id, field.text) }) { Text(doneLabel) }"))
        assertTrue("انصراف متنی ماند", s.contains("OutlinedButton(onClick = onDismiss) { Text(\"انصراف\") }"))
    }

    // ---------- ۶) رنگ پنجرهٔ دانش‌آموز سایت، مثل اپ ----------

    @Test
    fun siteStudentButtonsMatchTheAppColours() {
        val js = src("site/src/school.js")
        assertTrue("+ سبز شد", js.contains("class: 'btn st-add', text: '+'"))
        assertTrue("ایجاد بنفش شد", js.contains("class: 'btn st-create'"))
        assertTrue("ضربدر قرمز شد", js.contains("class: 'btn st-close', text: '×'"))
        val css = src("site/src/site.css")
        assertTrue(css.contains(".st-new-bar .btn.st-add{background:#25A86B"))
        assertTrue(css.contains(".st-new-bar .btn.st-create{background:#6C63F5"))
        assertTrue(css.contains(".st-new-bar .btn.st-close{background:#E5484D"))
    }

    @Test
    fun appColoursAreTheSourceOfTruth() {
        val student = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        // همان سه رنگی که سایت باید تقلید کند
        assertTrue("سبز اپ", student.contains("containerColor = Color(0xFF25A86B)"))
        assertTrue("قرمز اپ", student.contains("containerColor = Color(0xFFE5484D)"))
        val theme = src("app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt")
        assertTrue("بنفش پیش‌فرض اپ", theme.contains("Color(0xFF6C63F5)"))
        val css = src("site/src/site.css")
        listOf("25A86B" to "25A86B", "E5484D" to "E5484D", "6C63F5" to "6C63F5").forEach { (app, site) ->
            assertEquals(app, site)
            assertTrue("رنگ $site در سایت هست", css.contains(site))
        }
    }

    // ---------- ۷) تغییرات فقط در اپ است، سایت دست‌نخورده ----------

    @Test
    fun siteCodeEditorKeptItsOwnLook() {
        val js = src("site/src/code.js")
        // تصمیم کاربر: پنجرهٔ کد سایت تغییر نکند
        assertTrue("عنوان علائم سایت ماند", js.contains("علائم"))
        assertTrue("پنجرهٔ کد سایت هنوز عنوان دارد", js.contains("o.title || 'محیط کدنویسی'"))
    }
}
