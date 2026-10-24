package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V335.17 — بخش انتخاب، جابه‌جایی، تغییر اندازه، ویرایش، تکثیر، قرینه و حذف موارد درج‌شده روی تختهٔ سفید وب
 * بدون فعال کردن پایپ‌لاین ساخت اندروید (نام فایل مطابق الگوی نادیده‌گرفته‌شده در android.yml).
 */
class V335_17_Site_WhiteboardSelectionEditTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun whiteboardSupportsVectorSelectionMoveResizeDeleteAndEditInspector() {
        val student = File(root(), "site/src/student.js").readText()
        assertTrue("tb('select', '↖', 'انتخاب')" in student)
        assertTrue("انتخاب و ویرایش موارد درج‌شده" in student)
        assertTrue("class: 'wb-sec wb-inspector wb-dk-only'" in student)
        assertTrue("class: 'wb-sel-badge'" in student)
        assertTrue("'aria-label': 'ابزار انتخاب'" in student)
        assertTrue("'aria-label': 'انتخاب همه'" in student)
        assertTrue("selActBtn('✏️', 'ویرایش'," in student)
        assertTrue("selActBtn('⧉', 'تکثیر مورد'," in student)
        assertTrue("selActBtn('➕', 'بزرگ‌تر'," in student)
        assertTrue("selActBtn('➖', 'کوچک‌تر'," in student)
        assertTrue("selActBtn('⇄', 'قرینه'," in student)
        assertTrue("selActBtn('🗑', 'حذف مورد'," in student)
        assertTrue("function getHandlesForBox(ub)" in student)
        assertTrue("function computeResizedBox(ob, handle, dx, dy, keepRatio)" in student)
        assertTrue("async function editSelectedItem()" in student)
        assertTrue("function wbPromptText(title, body, label, initialVal, allowEmpty)" in student)
    }

    @Test
    fun promptDialogSupportsOverEngineLayering() {
        val app = File(root(), "site/src/app.js").readText()
        assertTrue("modal-bg' + (document.querySelector('.engine-bg') ? ' over-engine' : '')" in app)
    }

    @Test
    fun siteCssStylesWhiteboardInspectorAndSelectionBadge() {
        val css = File(root(), "site/src/site.css").readText()
        assertTrue(".wb-inspector{" in css)
        assertTrue(".wb-sel-badge{" in css)
        assertTrue(".m-dark .wb-sel-badge.on{" in css)
        assertTrue(".wb-canvas{background:#FFFFFF;" in css)
    }
}
