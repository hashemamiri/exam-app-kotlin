package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V154 — ورود/ثبت‌نام سایت در گوشی عیناً مثل SignInScreen اپ (پوستهٔ یخی، تب‌های نقش، OTP باکسی، مراحل بازیابی). */
class V154_SiteMobileAuthTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `mobile auth mirrors SignInScreen texts and flow`() {
        val m = source("site/src/mobile.js")
        assertTrue("function paintAuth(root)" in m && "function authActive()" in m)
        for (t in listOf("'ورود به حساب'", "'ساخت حساب جدید'", "'به سامانهٔ آزمون و ارزشیابی خوش آمدید'", "['مدیر/معاون', 'معلم', 'دانش‌آموز']", "['معلم', 'مدیر/معاون']",
            "'ورود با رمز عبور'", "'ورود با کد ایمیل'", "'ورود با گوگل'", "'رمز را فراموش کرده‌ام'", "'ارسال کد تأیید'", "'ثبت‌نام با گوگل'", "'تأیید کد'", "'ارسال دوباره کد'",
            "'بازیابی رمز عبور'", "'تعیین رمز تازه'", "'ذخیره رمز و ورود'", "['ایمیل', 'کد بازیابی', 'رمز جدید']", "'تکمیل ثبت‌نام و ورود'", "'ساخت مدرسه و ورود'", "'انصراف و خروج'")) assertTrue(t, t in m)
        val app = source("site/src/app.js")
        assertTrue("window.SiteMobile.authActive()" in app && "auth: {keyReady: KEY_READY" in app)
        val css = source("site/src/site.css")
        assertTrue("#E8F6FB,#D0EBF7,#BFE3F5" in css && ".ice-hero{width:84px;height:84px;border-radius:22px" in css && ".ice-btn{width:100%;height:52px;border:0;border-radius:14px" in css && ".ice-tabs{position:relative;height:46px" in css && ".ice-otp-b{" in css)
    }
}
