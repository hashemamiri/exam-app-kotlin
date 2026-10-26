package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.31 — نگهبان «نام دامنهٔ اختصاصی پل، تشخیص DNS روی گوشی و چرخش مسیر در ورود»:
 * ۱) پل Netlify باید با نام اختصاصی دامنهٔ خودمان هم ارائه شود و پیش از نام netlify.app بیاید.
 * ۲) صفحهٔ تست اتصال باید نام میزبان و نشانی DNS هر مسیر را نشان دهد.
 * ۳) پخش صوت نباید به نشانی مستقیم Supabase بچسبد (باید از مسیر فعال/پل بخواند).
 * ۴) ورود، در نبود مسیر سالم از سنجش، باید مسیر بعدی را فعال کند.
 */
class V335_31_RelayDiagnosticsTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val diagnostics by lazy { source("app/src/main/java/ir/exam/app/ui/profile/NetworkDiagnostics.kt") }
    private val auth by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt") }
    private val audio by lazy { source("app/src/main/java/ir/exam/app/ui/audio/QuestionAudioPlayer.kt") }

    @Test
    fun `custom-domain netlify relay is declared before plain netlify`() {
        assertTrue(
            "ثابت پل با نام اختصاصی",
            "const val DEFAULT_RELAY_NETLIFY_CUSTOM = \"https://relay2.onlineexam.ir\"" in router
        )
        // V335.32 — ترتیب کامل مسیرها در نگهبان تازهٔ V335_32 بررسی می‌شود؛ این‌جا فقط
        // می‌ماند که نام اختصاصی وجود دارد و نام netlify.app هم حذف نشده است.
        assertTrue("نام netlify.app باید بماند", "DEFAULT_RELAY_NETLIFY," in router)
    }

    @Test
    fun `ride diagnostics show host and dns`() {
        assertTrue("نام میزبان در هر ردیف", "SupabaseRouter.hostOf(probe.base)" in diagnostics)
        assertTrue("خط DNS در هر ردیف", "dnsByBase[probe.base]?.let { dns ->" in diagnostics)
        assertTrue("اندازه‌گیری DNS روی نخ پس‌زمینه", "dnsByBase = withContext(Dispatchers.IO)" in diagnostics)
        assertTrue("تابع اطلاعات DNS", "fun dnsInfo(base: String): String" in router)
        assertTrue("نشانی‌ها در گزارش از حافظه", "\"   DNS: \" + (dnsCache[p.base] ?: \"اندازه‌گیری نشد\")" in router)
        assertTrue("ثبت نتیجهٔ DNS برای گزارش", "fun cacheDns(values: Map<String, String>)" in router)
        assertTrue("ثبت از صفحهٔ تست", "SupabaseRouter.cacheDns(it)" in diagnostics)
    }

    @Test
    fun `audio playback follows the active route`() {
        assertTrue("نشانی از مسیریاب", "val target = ir.exam.app.core.network.SupabaseRouter.rewrite(url)" in audio)
        assertTrue("Storage خودمان با مسیر پل", "if (!ir.exam.app.core.network.SupabaseRouter.isOwnStorageUrl(url)) return emptyMap()" in audio)
        assertTrue("دانلود با کلاینت مشترک", "ir.exam.app.core.network.RelayHttp.client.newCall" in audio)
        assertTrue("دیگر HttpURLConnection مستقیم", "openConnection() as java.net.HttpURLConnection" !in audio)
    }

    @Test
    fun `sign in rotates to the next route when probing finds nothing`() {
        assertTrue("چرخش مسیر در ورود", "SupabaseRouter.rotateNext()" in auth)
        assertTrue("تابع چرخش در مسیریاب", "fun rotateNext(): String {" in router)
        assertTrue("چرخه‌ای بودن", "all[(index + 1).mod(all.size)]" in router)
        assertTrue("فعال‌سازی مسیر تازه", "remember(next)" in router)
    }
}
