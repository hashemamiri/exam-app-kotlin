package ir.exam.app

import ir.exam.app.ui.code.CodeLanguageIcons
import ir.exam.app.ui.code.CodeLanguages
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V352 — چهار خواستهٔ کاربر:
 *
 * ۱) در سایت هم آیکن زبان‌ها SVG شود.
 * ۲) در هدر پنجرهٔ ویرایشگر کد، آیکن SVG باشد — هم سایت هم اپ.
 * ۳) در اپ، ذخیره تیک سبز و انصراف ضربدر قرمز شود و وسطشان
 *    «Ln 6, Col 34 · 90/20000» بنشیند.
 * ۴) در سایت، کد داخل متن سؤال است؛ پیش‌نمایش تکراری زیر کادر لازم نیست
 *    (روی دسکتاپ حذف، روی موبایل حفظ — تصمیم کاربر).
 */
class V352_IconsAndActionsTest {

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

    // ---------- ۱ و ۲) نشان‌های سایت از همان منبع اپ ----------

    @Test
    fun siteHasTheSameSvgMarksAsTheApp() {
        val js = src("site/src/code.js")
        assertTrue("بلوک تولیدشده هست", js.contains("BEGIN GENERATED LANG ICONS"))
        assertTrue("پایان بلوک هست", js.contains("END GENERATED LANG ICONS"))
        assertTrue("صادر شده", js.contains("icon: iconSvg, inkOn: inkOn"))
        // هر ۳۲ مسیر باید عیناً همان مسیر اپ باشد
        CodeLanguages.all.forEach { lang ->
            val d = CodeLanguageIcons.pathOf(lang.id)
            assertTrue("مسیر سایت برای " + lang.id, js.contains(lang.id + ": {d: '" + d + "'"))
            val stroked = if (CodeLanguageIcons.isStroked(lang.id)) 1 else 0
            assertTrue("گونهٔ نشان " + lang.id, js.contains(", s: " + stroked + "}"))
        }
    }

    @Test
    fun sitePickerTilesUseSvgNotLetters() {
        val js = src("site/src/code.js")
        assertTrue(
            "کاشی نشان می‌گیرد",
            js.contains("el('span', {class: 'cx-tile-badge', style: '--cx:' + L.color, html: iconSvg(L.id, inkOn(L.color))})")
        )
        assertFalse("حرف اختصاری کاشی رفت", js.contains("class: 'cx-tile-badge', text: L.short"))
        val css = src("site/src/site.css")
        assertTrue("اندازهٔ نشان کاشی", css.contains(".cx-tile-badge svg{width:20px;height:20px;display:block}"))
    }

    @Test
    fun siteEditorHeaderUsesSvgAndFollowsLanguageChange() {
        val js = src("site/src/code.js")
        assertTrue(
            "هدر نشان می‌گیرد",
            js.contains("var badge = el('span', {class: 'cx-badge cx-badge-icon', style: '--cx:' + L.color, html: iconSvg(L.id, inkOn(L.color))})")
        )
        assertTrue(
            "با تغییر زبان عوض می‌شود",
            js.contains("badge.innerHTML = iconSvg(L.id, inkOn(L.color))")
        )
        assertFalse("نشان متنی هدر رفت", js.contains("badge.textContent = L.short"))
        val css = src("site/src/site.css")
        assertTrue(css.contains(".cx-badge.cx-badge-icon svg{width:17px;height:17px;display:block}"))
    }

    @Test
    fun bothSidesPickInkByBackgroundBrightness() {
        val js = src("site/src/code.js")
        // همان آستانه و همان دو رنگ جوهر در اپ
        assertTrue("آستانهٔ روشنایی سایت", js.contains("> 0.6 ? '#111111' : '#ffffff'"))
        assertTrue("ضرایب روشنایی", js.contains("0.2126 * r + 0.7152 * g + 0.0722 * b"))
        val app = src("app/src/main/java/ir/exam/app/ui/code/CodeLanguagePickerDialog.kt")
        assertTrue("آستانهٔ اپ", app.contains("tile.luminance() > 0.6f"))
        assertTrue("جوهر تیرهٔ اپ", app.contains("Color(0xFF111111)"))
    }

    @Test
    fun appEditorHeaderUsesTheSameIconLibrary() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("نشان در هدر", s.contains("CodeLanguageIcons.of(lang.id)"))
        assertTrue("کاشی هدر", s.contains("modifier = Modifier.size(28.dp)"))
        assertTrue("اندازهٔ نشان هدر", s.contains("Modifier.padding(5.dp).size(18.dp)"))
        assertTrue("روشنایی‌سنجی هدر", s.contains("headerTile.luminance() > 0.6f"))
        assertFalse("حرف اختصاری هدر رفت", s.contains("Text(lang.short,"))
        // ایمپورت افزونهٔ روشنایی باید باشد؛ یک‌بار جا افتادنش کامپایل را شکست
        assertTrue("ایمپورت luminance", s.contains("import androidx.compose.ui.graphics.luminance"))
    }

    // ---------- ۳) تیک سبز، موقعیت وسط، ضربدر قرمز ----------

    @Test
    fun appActionsAreGreenTickAndRedCross() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("تیک سبز", s.contains("Surface(color = Color(0xFF25A86B)"))
        assertTrue("آیکن تیک", s.contains("Icon(Icons.Outlined.Check, contentDescription = doneLabel, tint = Color.White)"))
        assertTrue("ضربدر قرمز", s.contains("Surface(color = Color(0xFFE5484D)"))
        assertTrue("آیکن ضربدر", s.contains("Icon(Icons.Outlined.Close, contentDescription = \"انصراف\", tint = Color.White)"))
        assertTrue("تیک همان ذخیره را می‌کند", s.contains("IconButton(onClick = { onDone(lang.id, field.text) })"))
        assertTrue("ضربدر همان انصراف", s.contains("IconButton(onClick = onDismiss)"))
        // دکمه‌های متنی پیشین رفته باشند
        assertFalse("دکمهٔ متنی ذخیره رفت", s.contains("Button(onClick = { onDone(lang.id, field.text) }) { Text(doneLabel) }"))
        assertFalse("دکمهٔ متنی انصراف رفت", s.contains("OutlinedButton(onClick = onDismiss) { Text(\"انصراف\") }"))
    }

    @Test
    fun positionAndCharCountSitBetweenTheTwoButtons() {
        val s = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        val label = "\"Ln \$lineNo, Col \$colNo · \${field.text.length}/\${CodeText.MAX}\""
        assertTrue("قالب موقعیت و شمار نویسه", s.contains(label))
        val tickAt = s.indexOf("Icon(Icons.Outlined.Check")
        val posAt = s.indexOf(label)
        val crossAt = s.indexOf("Icon(Icons.Outlined.Close")
        assertTrue("تیک هست", tickAt > 0)
        assertTrue("موقعیت بعد از تیک", posAt > tickAt)
        assertTrue("ضربدر بعد از موقعیت", crossAt > posAt)
        assertTrue("ردیف وسط‌چین", s.contains("Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)"))
        // و دیگر در نوار زبان تکرار نمی‌شود
        assertEquals("یک‌بار نوشته می‌شود", 1, s.split("Ln \$lineNo, Col \$colNo").size - 1)
    }

    @Test
    fun theCounterMatchesTheSiteFormat() {
        val js = src("site/src/code.js")
        // سایت پیش‌تر همین قالب را داشت؛ اپ باید هم‌خوان باشد
        assertTrue("قالب سایت", js.contains("Ln ") && js.contains("Col "))
        val app = src("app/src/main/java/ir/exam/app/ui/code/CodeEditorDialog.kt")
        assertTrue("جداکنندهٔ نقطه‌ای", app.contains(" · "))
        assertTrue("سقف نویسه از همان مرجع", app.contains("\${CodeText.MAX}"))
    }

    // ---------- ۴) پیش‌نمایش تکراری سایت ----------

    @Test
    fun desktopCodePreviewIsGoneButMobileKeepsIt() {
        val css = src("site/src/site.css")
        assertFalse("استثنای کد رفت", css.contains(".b-live.cx-has-code"))
        assertTrue("پیش‌نمایش دسکتاپ پنهان می‌ماند", css.contains(".dk .b-editor .b-live{display:none!important}"))
        val js = src("site/src/builder.js")
        assertFalse("کلاس cx-has-code دیگر گذاشته نمی‌شود", js.contains("classList.toggle('cx-has-code'"))
        // روی موبایل هیچ قاعده‌ای پیش‌نمایش را پنهان نمی‌کند
        // تنها قاعدهٔ پنهان‌سازی باید دسکتاپ‌محدود باشد، نه همه‌جایی
        val hides = Regex("[^\\n]*\\.b-live\\{display:none[^\\n]*").findAll(css).map { it.value }.toList()
        assertTrue("قاعدهٔ پنهان‌سازی هست", hides.isNotEmpty())
        assertTrue("همهٔ قاعده‌ها دسکتاپ‌محدودند", hides.all { it.contains(".dk ") })
        // موتور پیش‌نمایش برای موبایل سر جای خود است
        assertTrue("livePreview هنوز هست", js.contains("function livePreview(box, text)"))
        assertTrue("برای کد هم رندر می‌کند", js.contains("var hasCode = !!(C && C.hasFence(text))"))
    }

    // ---------- نگهبان تازهٔ ایمپورت افزونه‌ها ----------

    @Test
    fun importGuardNowCoversExtensionFunctions() {
        val g = src("scripts/check_missing_imports.py")
        assertTrue("نقشهٔ افزونه‌ها", g.contains("EXTENSIONS = {"))
        assertTrue("صدای نقطه‌ای", g.contains("EXT_CALL = re.compile("))
        assertTrue("نمونهٔ واقعی", g.contains("'luminance': 'androidx.compose.ui.graphics.luminance'"))
        assertTrue("پیام خطا", g.contains("استفاده شده ولی ایمپورت نشده"))
    }

    // ---------- یک منبع، دو خروجی ----------

    @Test
    fun generatorWritesBothOutputsFromOneSource() {
        val g = src("scripts/gen_code_lang_icons.py")
        assertTrue("منبع مشترک", g.contains("docs/tools/code_lang_icons.json"))
        assertTrue("خروجی کاتلین", g.contains("CodeLanguageIcons.kt"))
        assertTrue("خروجی سایت", g.contains("write_js(data, ids)"))
        assertTrue("قالب جدا", g.contains("docs/tools/code_icons_js_template.txt"))
        assertFalse("به /tmp تکیه ندارد", g.contains("/tmp/langicons"))
        val tpl = src("docs/tools/code_icons_js_template.txt")
        assertTrue("جای ردیف‌ها", tpl.contains("@@ROWS@@"))
        assertTrue("تابع نشان", tpl.contains("function iconSvg(id, fg)"))
        assertTrue("تابع جوهر", tpl.contains("function inkOn(hex)"))
    }
}
