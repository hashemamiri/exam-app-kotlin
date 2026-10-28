package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.61 — سه اصلاح تجربه:
 * ۱) ورود گوگل: بدون متن انتظار، انیمیشن گوگل وسط صفحه + تلاش دوم و پیام دقیق خطا.
 * ۲) حذف خط پذیرش شرایط از صفحهٔ ثبت‌نام (اپ و سایت).
 * ۳) دسته‌بندی ابزارهای وایت‌برد در اپ و سایت و دکمهٔ ثبتِ همیشه‌پیدا در سایت.
 */
class V335_61_BoardGroupsAndGoogleWaitTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `google sign in retries and reports precise errors`() {
        val screen = src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        assertTrue("credentialManager.getCredential(request = request(false), context = context)" in screen)
        assertTrue("credentialManager.getCredential(request = request(true), context = context)" in screen)
        assertTrue("NoCredential" in screen && "ProviderConfiguration" in screen)
        assertTrue("private fun googleSignInErrorMessage(error: Throwable)" in screen)
        assertFalse("CircularProgressIndicator" in screen)
    }

    @Test
    fun `legal consent line is gone from sign up`() {
        assertFalse("LegalConsentLine" in src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt"))
        assertFalse("را می‌پذیرید" in src("site/src/mobile.js"))
        assertFalse("را می‌پذیرید" in src("site/src/app.js"))
        assertTrue("LegalCard" in src("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt"))
    }

    @Test
    fun `native board tools are grouped into short categories`() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("private val BOARD_TOOL_GROUPS = listOf(\"قلم و انتخاب\", \"اشکال\", \"درج…\", \"ابزار هندسی\", \"رنگ و ضخامت\")" in board)
        assertTrue("var toolGroup by remember { mutableIntStateOf(0) }" in board)
        assertTrue("if (toolGroup == 3)" in board && "if (toolGroup == 4)" in board)
    }

    @Test
    fun `website board collapses tool groups and keeps the confirm button visible`() {
        val js = src("site/src/student.js")
        assertTrue("function wbCollapsible(secEl, open)" in js)
        assertTrue("wbGroup(rightSide," in js && "wbGroup(leftSide," in js)
        val css = src("site/src/site.css")
        assertTrue(".wb-sec-head" in css && ".wb-sec-body[hidden]" in css)
        assertTrue("position:sticky;bottom:0" in css.replace(" ", ""))
    }
}
