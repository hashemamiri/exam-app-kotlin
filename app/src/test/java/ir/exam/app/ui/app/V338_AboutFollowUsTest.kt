package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V338 — «ما را دنبال کنید» در بخش «درباره».
 *
 * قراردادهای نگهبان:
 *  • دکمه زیر بخش بروزرسانی و بالای «تماس با ما و نظرات» نشان داده می‌شود؛
 *  • پنجره فقط آیکن رسمی دارد (هیچ نوشته‌ای کنار آیکن‌ها چاپ نمی‌شود)؛
 *  • هر هفت نشانی رسمی دقیقاً همان‌هایی هستند که کاربر داده است؛
 *  • آیکن‌ها منبع داخلی اپ‌اند (وابسته به شبکه نیستند) و بازکردن کانال خطا را
 *    به پیام روشن تبدیل می‌کند، نه کرش.
 */
class V338_AboutFollowUsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "text/APP_VERSION.txt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    private val aboutPath = "app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt"
    private val socialPath = "app/src/main/java/ir/exam/app/ui/update/SocialChannels.kt"

    @Test
    fun `follow-us button sits below the update block and above contact card`() {
        val about = src(aboutPath)
        val update = "Text(if (state.checking) \"در حال بررسی\" else \"بررسی بروزرسانی\")"
        val follow = "item { FollowUsButton(onError = viewModel::reportInstallError) }"
        val contact = "item { ContactCard(onError = viewModel::reportInstallError) }"
        assertTrue("دکمه بررسی بروزرسانی باید دست‌نخورده بماند", update in about)
        assertTrue("دکمه ما را دنبال کنید باید به صفحه درباره اضافه شود", follow in about)
        assertTrue("کارت تماس با ما باید دست‌نخورده بماند", contact in about)
        assertTrue(
            "ترتیب باید بررسی بروزرسانی ← ما را دنبال کنید ← تماس با ما باشد",
            about.indexOf(update) < about.indexOf(follow) && about.indexOf(follow) < about.indexOf(contact)
        )
    }

    @Test
    fun `all seven official channel addresses are exact`() {
        val social = src(socialPath)
        assertTrue("SocialChannel(\"ایتا\", \"https://eitaa.com/onlineexam_ir\", R.drawable.social_eitaa)," in social)
        assertTrue("SocialChannel(\"بله\", \"https://ble.ir/onlineexam_ir\", R.drawable.social_bale)," in social)
        assertTrue("SocialChannel(\"روبیکا\", \"https://rubika.ir/onlineexam_ir\", R.drawable.social_rubika)," in social)
        assertTrue("SocialChannel(\"سروش\", \"https://splus.ir/onlineexam_ir\", R.drawable.social_soroush)," in social)
        assertTrue("SocialChannel(\"شاد\", \"https://shad.ir/onlineexam_ir\", R.drawable.social_shad)," in social)
        assertTrue("SocialChannel(\"تلگرام\", \"https://t.me/onlineexam_ir\", R.drawable.social_telegram)," in social)
        assertTrue("SocialChannel(\"آپارات\", \"https://www.aparat.com/onlineexam_ir\", R.drawable.social_aparat)" in social)
        assertTrue("چیدمان باید چهارتایی باشد تا ردیف دوم سه آیکن وسط‌چین بماند", "SOCIAL_CHANNELS.chunked(4)" in social)
        assertTrue("جای خالی ردیف دوم باید هم‌وزن پر شود", "repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }" in social)
    }

    @Test
    fun `popup shows official icons only with no text labels`() {
        val social = src(socialPath)
        assertTrue("برچسب دسترس‌پذیری باید نام کانال باشد", "contentDescription = channel.label," in social)
        assertTrue("آیکن باید از منبع داخلی خوانده شود", "painter = painterResource(channel.iconRes)," in social)
        assertFalse("نام کانال نباید به‌صورت نوشته کنار آیکن چاپ شود", "Text(channel.label)" in social)
        assertFalse("چیدمان آزمایشی نباید استفاده شود", "androidx.compose.foundation.layout.FlowRow" in social)
        assertTrue("دکمه باید همان عنوان درخواستی کاربر را داشته باشد", "Text(\"ما را دنبال کنید\")" in social)
        assertTrue("پنجره باید دکمه بستن داشته باشد", "confirmButton = { TextButton(onClick = { open = false }) { Text(\"بستن\") } }" in social)
    }

    @Test
    fun `opening a channel degrades to a clear message instead of crashing`() {
        val social = src(socialPath)
        assertTrue("val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)" in social)
        assertTrue("} catch (error: ActivityNotFoundException) {" in social)
        assertTrue("onError(IllegalStateException(\"برنامه یا مرورگری برای بازکردن این کانال پیدا نشد.\", error))" in social)
        assertTrue("} catch (error: SecurityException) {" in social)
    }

    @Test
    fun `official icon drawables are bundled inside the app`() {
        listOf("eitaa", "bale", "rubika", "soroush", "shad", "telegram", "aparat").forEach { key ->
            val icon = File(root(), "app/src/main/res/drawable-nodpi/social_$key.png")
            assertTrue("آیکن رسمی باید داخل اپ بسته‌بندی شود: ${icon.path}", icon.isFile && icon.length() > 0)
        }
    }

    @Test
    fun `app version is bumped for this release`() {
        assertTrue("نسخه اپ باید برای این انتشار بالا رفته باشد", src("text/APP_VERSION.txt").trim() == "1.03.55")
        val first = src("text/CHANGELOG_FA.txt").lineSequence().first()
        assertTrue("خط اول CHANGELOG باید نسخه این انتشار باشد", first.startsWith("V338") && "1.03.55" in first)
    }
}
