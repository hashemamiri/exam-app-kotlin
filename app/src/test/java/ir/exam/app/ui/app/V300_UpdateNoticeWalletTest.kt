package ir.exam.app.ui.app

import ir.exam.app.core.update.ReleaseNotes
import ir.exam.app.core.update.UpdateNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V300 — (۱) لیست تغییرات: عنوان‌های «Vxxx (اپ 1.01.99 + سایت) — …» درست تجزیه می‌شوند، بلوک‌های «فقط سایت/مستندات»
 * در پنجرهٔ بروزرسانی و اعلان نمی‌آیند و Vxxx به کاربر نشان داده نمی‌شود؛ (۲) اعلان محلی «نسخهٔ جدید برنامه» در زنگوله؛
 * (۳) کیف پول: پیام فارسی سرور از بدنهٔ خطای wallet-payment به کاربر می‌رسد.
 */
class V300_UpdateNoticeWalletTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String): String = File(root(), path).readText()

    private val lines = listOf(
        "V299 (فقط سایت) — صفحهٔ ورود",
        "- کاشی‌های معرفی ۸ تا شد.",
        "V298.3 (فقط مستندات/ابزار تحویل) — مقصد نصب",
        "- هر بستهٔ apply با مقصد مشخص.",
        "V298.2 (اپ 1.01.99) — پیام روشن",
        "- دکمهٔ «ورود با گوگل» توضیح می‌دهد. اپ و سایت.",
        "V297 (اپ 1.01.97 + سایت) — ویرایشگر فرمول",
        "- جابه‌جایی با کلیدهای ← →؛ مثل V296 (اپ و سایت)",
        "V207 — سایت دسکتاپ: کادر بالا حذف شد؛ کد آزمون دیده می‌شود."
    )

    @Test
    fun parenthesizedHeadersAndScopes() {
        val blocks = ReleaseNotes.parse(lines)
        assertEquals(listOf("V299", "V298.3", "V298.2", "V297", "V207"), blocks.map { it.version })
        assertEquals(listOf(false, false, true, true, true), blocks.map { it.appAffecting })
        assertEquals("1.01.99", blocks[2].appVersion)
        assertEquals("", blocks[4].appVersion)
        assertEquals("ویرایشگر فرمول", blocks[3].summary)
    }

    @Test
    fun onlyAppBulletsWithoutInternalVersion() {
        val latest = ReleaseNotes.latestBullets(lines)
        assertEquals(listOf("دکمهٔ «ورود با گوگل» توضیح می‌دهد"), latest)
        val notice = ReleaseNotes.noticeBullets(lines)
        assertTrue(notice.none { "V2" in it || "اپ و سایت" in it })
        assertTrue(notice.contains("جابه‌جایی با کلیدهای ← →؛ مثل"))
        assertEquals(listOf("V298.2", "V297", "V207"), ReleaseNotes.lastVersions(lines).map { it.version })
        // تیتری که به «در اپ» ختم می‌شود دست نمی‌خورد
        assertEquals("نمایش فرمول در اپ", ReleaseNotes.cleanBullet("نمایش فرمول در اپ"))
    }

    @Test
    fun realChangelogTopAppBlockHasBullets() {
        val top = ReleaseNotes.appBlocks(src("text/CHANGELOG_FA.txt").lines().take(60)).first()
        assertTrue(top.appAffecting)
        assertTrue(top.bullets.isNotEmpty())
        assertTrue(top.bullets.none { Regex("""\bV\d""").containsMatchIn(it) })
    }

    @Test
    fun updateNoticePublishesOncePerVersion() {
        UpdateNotice.clear()
        UpdateNotice.publish("1.02.00", lines)
        val info = UpdateNotice.info.value!!
        assertEquals("1.02.00", info.versionName)
        assertFalse(UpdateNotice.seen.value)
        UpdateNotice.seen.value = true
        UpdateNotice.publish("1.02.00", lines)
        assertTrue(UpdateNotice.seen.value)
        UpdateNotice.publish("1.02.01", lines)
        assertFalse(UpdateNotice.seen.value)
        assertTrue(UpdateNotice.body(info).startsWith("• "))
        UpdateNotice.clear()
    }

    @Test
    fun wiring() {
        val bell = src("app/src/main/java/ir/exam/app/ui/notifications/NotificationCenter.kt")
        assertTrue("UpdateNotice.info.collectAsState()" in bell && "UpdateNotice.LOCAL_ID" in bell && "UpdateNotice.onOpen?.invoke()" in bell)
        assertTrue("\"برنامه\" to setOf(\"app_update\")" in src("app/src/main/java/ir/exam/app/data/repository/SupabaseNotificationRepository.kt"))
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("UpdateNotice.publish(remote.name, remote.notesFa)" in app && "UpdateNotice.onOpen = { updatePromptDismissed = false }" in app)
        val about = src("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
        assertTrue("block.appVersion" in about && "\${block.version}" !in about)
        val ci = src(".github/workflows/android.yml")
        assertTrue("skipping = bool(scope) and (\"اپ\" not in scope)" in ci)
        val billing = src("app/src/main/java/ir/exam/app/data/repository/SupabaseBillingRepository.kt")
        assertTrue("catch (e: RestException)" in billing && "walletServerMessage(e.error)" in billing)
    }

    @Test
    fun walletServerMessageParsesEdgeFunctionBody() {
        val f = Class.forName("ir.exam.app.data.repository.SupabaseBillingRepositoryKt").getDeclaredMethod("walletServerMessage", String::class.java)
        f.isAccessible = true
        assertEquals("درگاه پرداخت هنوز پیکربندی نشده است.", f.invoke(null, """{"error":"درگاه پرداخت هنوز پیکربندی نشده است.","code":"payment_not_configured"}"""))
        assertEquals(null, f.invoke(null, """{"error":"internal"}"""))
        assertEquals(null, f.invoke(null, "<html>502</html>"))
    }
}
