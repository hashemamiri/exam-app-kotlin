package ir.exam.app

import ir.exam.app.core.update.ReleaseNotes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V352.2 — چرا CI اپ شکست و چه چیزی درست شد.
 *
 * دو تستِ از پیش موجود (V208 و V322) شکل بلوک بالای لیست تغییرات را پین
 * کرده‌اند: هر تیتر حداکثر ۲۰۰ نویسه. در نسخهٔ پیشین یک تیتر ۲۲۳ نویسه‌ای
 * نوشته شد و هر دو تست پس از ۱۰ دقیقه ساخت شکستند. هیچ نگهبان محلی‌ای
 * این را نمی‌گرفت؛ نگهبان‌های پایتونی فقط سورس کاتلین و سایت را می‌سنجیدند.
 *
 * این آزمون سه چیز را پین می‌کند:
 *  ۱) نگهبان تازهٔ check_changelog.py وجود دارد و همان قاعده‌ها را دارد؛
 *  ۲) در CI ثبت شده است، پس دیگر پس از ساخت طولانی شکست نمی‌خوریم؛
 *  ۳) بلوک بالای لیست تغییرات واقعاً همان قاعده‌ها را پاس می‌کند.
 */
class V352_2_ChangelogGuardTest {

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

    /** نگهبان باید همان سقف و همان تعداد تیترِ موتور را داشته باشد. */
    @Test
    fun guardMirrorsTheEngineConstants() {
        val g = src("scripts/check_changelog.py")
        assertTrue("سقف تعداد تیتر پین نشده", g.contains("MAX_BULLETS = 6"))
        assertEquals("سقف نگهبان با موتور یکی نیست", ReleaseNotes.MAX_BULLETS, 6)
        assertTrue("سقف نویسه پین نشده", g.contains("MAX_BULLET_LEN = 200"))
        assertTrue("مسیر لیست تغییرات پین نشده", g.contains("text/CHANGELOG_FA.txt"))
    }

    /**
     * موتور جاوا به‌صورت پیش‌فرض ASCII است؛ پایتون یونیکد. بدون پرچم
     * re.ASCII مرزِ \bV\d در متن فارسی متفاوت عمل می‌کند و آینه دروغ می‌گوید.
     */
    @Test
    fun guardUsesAsciiRegexLikeTheJvm() {
        val g = src("scripts/check_changelog.py")
        assertTrue("پرچم ASCII تعریف نشده", g.contains("A = re.ASCII"))
        assertTrue("الگوی شمارهٔ داخلی ASCII نیست", g.contains("""INTERNAL_VERSION = re.compile(r"\bV\d+(?:\.\d+)*\b", A)"""))
        assertTrue("الگوی عنوان ASCII نیست", g.contains("?$\", A)"))
        assertTrue("چندفاصله ASCII نیست", g.contains("""MULTI_SPACE = re.compile(r"\s{2,}", A)"""))
    }

    /** ترتیب پاک‌سازی تیتر باید عیناً مثل موتور باشد، وگرنه طول فرق می‌کند. */
    @Test
    fun guardKeepsTheCleaningOrder() {
        val g = src("scripts/check_changelog.py")
        val body = g.substringAfter("def clean_bullet(raw):").substringBefore("def split_summary")
        val steps = listOf(
            "raw.replace(\"`\", \"\").strip()",
            "INTERNAL_VERSION.sub",
            "EMPTY_PARENS.sub",
            "MULTI_SPACE.sub",
            "TRAILING_SCOPE.sub",
            "rstrip(\"،;؛\")"
        )
        var at = -1
        for (s in steps) {
            val i = body.indexOf(s)
            assertTrue("مرحلهٔ پاک‌سازی نبود: $s", i >= 0)
            assertTrue("ترتیب پاک‌سازی جابه‌جا شده: $s", i > at)
            at = i
        }
        assertTrue("بُرش به سقف تیترها نیست", g.contains("cleaned[:MAX_BULLETS]"))
    }

    /** نگهبان باید در CI و پیش از Gradle اجرا شود، همراه آزمون خودش. */
    @Test
    fun guardIsRegisteredInCiBeforeGradle() {
        val ci = src(".github/workflows/android.yml")
        assertTrue("نگهبان در حلقهٔ CI نیست", ci.contains("check_site_pins check_site_sync check_changelog; do"))
        assertTrue("آزمون خودِ نگهبان در CI نیست", ci.contains("python3 scripts/check_changelog.py --selftest"))
        val guards = ci.indexOf("check_site_sync check_changelog; do")
        val gradle = ci.indexOf("testDebugUnitTest")
        assertTrue("گام Gradle پیدا نشد", gradle > 0)
        assertTrue("نگهبان باید پیش از Gradle باشد", guards in 1 until gradle)
    }

    /** خودِ نگهبان باید حالت‌های رد را بشناسد، نه فقط حالت سالم را. */
    @Test
    fun guardHasNegativeSelfTests() {
        val g = src("scripts/check_changelog.py")
        assertTrue("حالت خودآزمون نیست", g.contains("def selftest():"))
        assertTrue("حالت تیتر بلند نیست", g.contains("(\"تیتر بلند\""))
        assertTrue("حالت مرزی ۲۰۰ نیست", g.contains("(\"تیتر ۲۰۰ نویسه (مرز)\""))
        assertTrue("حالت فقط سایت نیست", g.contains("(\"فقط سایت\""))
        assertTrue("حالت بدون تیتر نیست", g.contains("(\"بدون تیتر\""))
        assertTrue("پرچم خودآزمون نیست", g.contains("\"--selftest\" in sys.argv"))
    }

    /** و سرانجام: بلوک واقعیِ بالای لیست تغییرات باید پاس شود. */
    @Test
    fun currentTopBlockSatisfiesBothOlderTests() {
        val lines = src("text/CHANGELOG_FA.txt").lines()
        assertTrue("خط اول عنوان نسخه نیست", lines.first().startsWith("V"))
        for (take in listOf(40, 60)) {
            val top = ReleaseNotes.parse(lines.take(take)).first()
            assertTrue("بلوک بالا مربوط به اپ نیست", top.appAffecting)
            assertTrue("تعداد تیتر خارج از بازه", top.bullets.size in 1..ReleaseNotes.MAX_BULLETS)
            val tooLong = top.bullets.filter { it.length > 200 }
            assertTrue("تیتر بلندتر از ۲۰۰: $tooLong", tooLong.isEmpty())
            val internal = top.bullets.filter { Regex("""\bV\d""").containsMatchIn(it) }
            assertTrue("شمارهٔ داخلی در تیتر: $internal", internal.isEmpty())
        }
    }

    /** شمارهٔ نسخه در فایل و در خط اول لیست تغییرات باید یکی باشد. */
    @Test
    fun versionFileAgreesWithChangelogHeadline() {
        val v = src("text/APP_VERSION.txt").trim()
        assertTrue("قالب نسخه درست نیست: $v", Regex("""^\d{1,2}\.\d{2}\.\d{2}$""").matches(v))
        val first = src("text/CHANGELOG_FA.txt").lines().first()
        assertTrue("خط اول نسخهٔ $v را ندارد: $first", first.contains(v))
    }
}
