package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V175 — سازندهٔ دسکتاپ: چهار بخش (ریل راست، مشخصات/سربرگ، ویرایشگر، ریل چپ) هم‌ارتفاع و ثابت؛ اسکرول فقط داخل هر بخش. */
class V175_SiteBuilderFixedPanelsTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }

    @Test
    fun `builder panels are fixed and scroll internally`() {
        val c = File(root(), "site/src/site.css").readText()
        assertTrue(".dk .builder{position:fixed;top:22px;bottom:22px;left:100px;right:100px;display:flex;flex-direction:column;overflow:hidden;z-index:20}" in c)
        assertTrue(".dk .builder .b-settings{position:static;top:auto;max-height:none;height:auto;min-height:0;align-self:stretch;overflow:auto;box-sizing:border-box;margin:0}" in c)
        assertTrue(".dk .builder .b-editor{height:auto;min-height:0;align-self:stretch;overflow:auto;box-sizing:border-box;scrollbar-width:none;margin:0}" in c)
        assertTrue(".dk .builder .b-body{flex:1 1 auto;min-height:0;margin:0;align-items:stretch;grid-template-rows:minmax(0,1fr)}" in c) // V177
        assertTrue(".dk .b-rail,.dk .dk-rail{top:22px;bottom:22px}" in c)
    }
}
