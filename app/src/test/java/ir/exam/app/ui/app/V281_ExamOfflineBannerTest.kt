package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V281 — قطع اینترنت وسط آزمون: نوار وضعیت زنده، ساعت آخرین ذخیره، متن‌های بدون واژهٔ فنی. */
class V281_ExamOfflineBannerTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `live connectivity reaches the exam screen`() {
        val n = src("app/src/main/java/ir/exam/app/core/network/NetworkMonitor.kt")
        assertTrue("fun observe(): Flow<Boolean> = callbackFlow" in n && "registerNetworkCallback(request, callback)" in n && "awaitClose" in n)
        val vm = src("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        assertTrue("val online: Boolean = true" in vm && "val draftSavedAtMs: Long = 0L" in vm)
        assertTrue("connectivity.collect { on -> _state.update { it.copy(online = on) } }" in vm)
        assertTrue(".onSuccess { _state.update { it.copy(draftSavedAtMs = System.currentTimeMillis()) } }" in vm)
        val home = src("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt")
        assertTrue("connectivity = network.observe()" in home)
        assertFalse("WorkManager" in home.substringAfter("fun StudentHomeScreen").substringBefore("NeumorphicPanel("))
        val scr = src("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        assertTrue("item { ConnectionBanner(online = state.online, savedAtMs = state.draftSavedAtMs) }" in scr)
        assertTrue("\"اتصال اینترنت قطع است\"" in scr && "پاسخ‌ها روی دستگاه ذخیره شد (" in scr)
    }
}
