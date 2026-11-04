package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V337.3 — رفع کرش «مشاهده کارنامه مدرسه»:
 * مقایسهٔ کلاس‌ها در حالت بازِ پیش‌فرض، پیش از آماده‌شدن دادهٔ RPC،
 * نباید به دادهٔ تهی چک‌نات‌ال بزند؛ باید حالت بارگذاری/خطا را نشان دهد.
 */
class V337_3_ManagerReportCrashTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/admin.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `class comparison never force-unwraps nullable stats`() {
        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertFalse("checkNotNull(stats)" in manager)
        assertTrue("var loading by remember { mutableStateOf(expanded) }" in manager)
        assertTrue("loading || (stats == null && error == null) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {" in manager)
        assertTrue("else -> stats?.let { data ->" in manager)
    }

    @Test
    fun `class comparison rpc error is thrown into runCatching explicitly`() {
        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("?.let { message -> throw IllegalStateException(message) }" in manager)
        assertTrue(".onFailure { error = safeManagerError(it); loading = false }" in manager)
    }
}
