package ir.exam.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V325 / V326 / V327 — ارتقای «تنظیمات» در دسکتاپ (سایت) و اپ.
 *
 * پیش از این نسخه، دسکتاپ قفل نداشت، چیدمان تنظیمات همان قالب باریک موبایل بود، اعلان‌ها یک
 * کلید کلی داشتند و هیچ‌جا حجم حافظه/صف آفلاین دیده نمی‌شد. اپ هم مدیریت حافظه و اعلان تفکیکی
 * نداشت. تنظیمات هم بین دستگاه‌ها همگام نمی‌شد. این تست همین چهار محور را تضمین می‌کند.
 */
class V325_SettingsUpgradeTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun source(path: String): String = File(root(), path).readText()

    private fun lock() = source("site/src/lock.js")
    private fun mobile() = source("site/src/mobile.js")
    private fun appJs() = source("site/src/app.js")
    private fun push() = source("site/src/push.js")
    private fun sw() = source("site/pwa/sw.js")
    private fun sync() = source("site/src/sync.js")
    private fun css() = source("site/src/site.css")
    private fun buildSite() = source("site/build_site.py")

    // ── V325: قفل میز کار ──────────────────────────────────────────────────────
    @Test
    fun desktopHasWorkspaceLock() {
        val l = lock()
        assertTrue("ماژول قفل نیست", "window.SiteLock" in l)
        assertTrue("هش SHA-256 نیست", "crypto.subtle.digest('SHA-256'" in l)
        assertTrue("نمک تصادفی نیست", "var salt = randomHex(8)" in l)
        assertTrue("قفل خودکار نیست", "lockNow" in l && "timeoutMin" in l)
        assertTrue("قفل پس از بارگذاری دوباره", "lockAfterReload" in l)
        assertTrue("ذخیرهٔ PIN خام ممنوع", "c.hash = await hash(salt + ':' + pin)" in l)
        assertFalse("PIN نباید خام ذخیره شود", "localStorage.setItem(LS, JSON.stringify(c))" in l && "c.pin" in l)
        // بار اول سایت باید شامل ماژول قفل باشد (پیش از پنل‌ها)
        assertTrue("قفل در باندل بار اول نیست", "\"lock.js\"" in buildSite())
        assertTrue("استایل قفل نیست", ".site-lock.on{display:flex}" in css())
        assertTrue("بخش امنیت در تنظیمات نیست", "['security', 'امنیت']" in mobile())
    }

    // ── V325: چیدمان دسکتاپی تنظیمات + میان‌برها ───────────────────────────────
    @Test
    fun desktopSettingsHaveRealLayoutAndShortcuts() {
        val m = mobile()
        assertTrue("قالب دو ستونهٔ تنظیمات", "class: 'm-settings'" in m && "m-settings-body" in m)
        assertTrue("شبکهٔ دو ستونه در CSS", "grid-template-columns:230px minmax(0,1fr)" in css())
        assertTrue("کارت میان‌برها", "function shortcutsCard()" in m)
        val a = appJs()
        assertTrue("فهرست میان‌برها", "var KBD = [" in a)
        assertTrue("راهنمای میان‌برها", "function kbdHelp()" in a)
        assertTrue("Ctrl+S", "k === 's'" in a)
        assertTrue("Ctrl+P", "k === 'p'" in a)
        assertTrue("Ctrl+Enter", "k === 'enter'" in a)
        assertTrue("رجیستری میان‌بر سازنده", "shortcut: function (kind)" in source("site/src/builder.js"))
    }

    // ── V325/V326: اعلان‌های تفکیکی (سایت و SW و اپ) ──────────────────────────
    @Test
    fun notificationsAreCategorizedEverywhere() {
        val p = push()
        assertTrue("دسته‌ها در push.js نیست", "var CAT_DEF = {grades: true, exam: true, calendar: true, requests: true}" in p)
        assertTrue("نوشتن در IndexedDB برای SW", "indexedDB.open('examsite-push', 1)" in p)
        assertTrue("تنظیم دسته", "cats: cats, setCat: setCat" in p)
        val s = sw()
        assertTrue("SW دسته را نمی‌خواند", "function pushCats()" in s)
        assertTrue("SW اعلان خاموش را نشان می‌دهد", "if (cats && key && cats[key] === false) return;" in s)
        val m = mobile()
        for (label in listOf("نمرهٔ جدید", "باز شدن آزمون", "پیام تقویم", "درخواست‌های مدیر")) {
            assertTrue("دستهٔ $label در تنظیمات سایت نیست", label in m)
        }
        val reg = source("app/src/main/java/ir/exam/app/core/push/PushRegistrar.kt")
        assertTrue("دسته‌ها در اپ نیست", "val CATEGORIES = listOf(\"grades\", \"exam\", \"calendar\", \"requests\")" in reg)
        assertTrue("تصمیم نمایش در اپ", "fun shouldShow(context: Context, page: String?): Boolean" in reg)
        assertTrue("بازهٔ سکوت", "fun inQuietHours(" in reg)
        assertTrue("سرویس از shouldShow استفاده کند",
            "if (!PushRegistrar.shouldShow(applicationContext, message.data[\"page\"])) return" in
                source("app/src/main/java/ir/exam/app/core/push/PushMessagingService.kt"))
    }

    // ── V325/V326: مدیریت حافظه و صف ارسال ────────────────────────────────────
    @Test
    fun storageAndQueueAreManageable() {
        val m = mobile()
        assertTrue("کارت فضای مرورگر", "function storageCard()" in m)
        assertTrue("تخمین فضا", "navigator.storage.estimate" in m)
        assertTrue("پاک‌سازی کش تصاویر در سایت", "clearMediaCache" in m)
        assertTrue("پاک‌سازی کش تصاویر در app.js", "function clearMediaCache()" in appJs())
        assertTrue("اندازهٔ کش تصاویر", "function mediaCacheSize()" in appJs())
        assertTrue("یادآوری پشتیبان در سایت", "آخرین پشتیبان" in m)
        val tools = source("app/src/main/java/ir/exam/app/ui/profile/LocalStorageTools.kt")
        assertTrue("ابزار حافظهٔ اپ نیست", "object LocalStorageTools" in tools)
        assertTrue("پاک‌سازی کش اپ", "suspend fun clearCache(context: Context)" in tools)
        assertTrue("چند مورد در صف", "suspend fun pendingCount(context: Context, userId: String)" in tools)
        assertTrue("تلاش دوبارهٔ صف", "suspend fun retryAll(context: Context, userId: String)" in tools)
        assertTrue("نشانهٔ تاریخ پشتیبان", "fun markBackupNow(context: Context)" in tools)
        assertTrue("کارت حافظه در تنظیمات اپ", "SettingsAccordionCard(title = \"حافظه و ارسال‌های در انتظار\"" in
            source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt"))
        assertTrue("حالت درس‌آزمون", "حالت درس‌آزمون" in
            source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt"))
        assertTrue("ثبت تاریخ پشتیبان پس از خروجی اپ", "LocalStorageTools.markBackupNow(context)" in
            source("app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt"))
    }

    // ── V327: همگام‌سازی تنظیمات با حساب ──────────────────────────────────────
    @Test
    fun settingsSyncAcrossDevices() {
        val sql = source("supabase/migrations/20261021_settings_sync_v327.sql")
        assertTrue("جدول user_settings نیست", "create table if not exists public.user_settings" in sql)
        assertTrue("RLS فعال نیست", "alter table public.user_settings enable row level security" in sql)
        assertTrue("سیاست خواندن", "auth.uid() = user_id" in sql)
        assertTrue("گرنت anon (قاعدهٔ Data API)", "grant select on table public.user_settings to anon;" in sql)
        assertTrue("RPC خواندن", "create or replace function public.native_settings_get_v327()" in sql)
        assertTrue("RPC نوشتن", "create or replace function public.native_settings_put_v327(" in sql)
        assertTrue("security definer", sql.contains("security definer"))

        assertTrue("ماژول همگام‌سازی سایت نیست", "window.SiteSettingsSync" in sync())
        assertTrue("pull/push", "async function pull()" in sync() && "function push(part)" in sync())
        assertTrue("sync.js در باندل بار اول", "\"sync.js\"" in buildSite())
        assertTrue("push ظاهر در سایت", "SiteSettingsSync.push('appearance')" in mobile())
        assertTrue("push سربرگ در سایت", "SiteSettingsSync.push('header')" in appJs())

        val repo = source("app/src/main/java/ir/exam/app/data/repository/SettingsSyncRepository.kt")
        assertTrue("ریپازیتوری همگام‌سازی اپ نیست", "class SettingsSyncRepository(" in repo)
        assertTrue("فراخوانی RPC خواندن", "rpc(\"native_settings_get_v327\")" in repo)
        assertTrue("فراخوانی RPC نوشتن", "\"native_settings_put_v327\"" in repo)
        val vm = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt")
        assertTrue("pull در شروع تنظیمات", "settingsSync.applyRemote(settingsSync.pull())" in vm)
        assertTrue("push پس از تغییر ظاهر", "settingsSync.pushAppearance()" in vm)
    }

    /** نگهبان Data API (V324) باید جدول تازهٔ V327 را هم بپذیرد. */
    @Test
    fun newTableFollowsTheGrantRule() {
        val sql = source("supabase/migrations/20261021_settings_sync_v327.sql")
        assertTrue("گرنت authenticated", "grant select, insert, update, delete on table public.user_settings to authenticated;" in sql)
        assertTrue("گرنت service_role", "grant all on table public.user_settings to service_role;" in sql)
        val guard = source("scripts/check_supabase_data_api_grants.py")
        assertEquals("نگهبان گرنت باید در preflight بماند", true, "check_supabase_data_api_grants.py" in
            source("scripts/preflight.py"))
        assertTrue("اسکریپت نگهبان", guard.contains("DEFAULT_PRIV"))
    }
}
