package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V284 — مرکز اعلان (زنگوله + فهرست) در اپ و سایت و رویدادهای جدید در SQL. */
class V284_NotificationCenterTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `sql adds history columns, rpcs and new events`() {
        val sql = src("supabase/migrations/20261014_native_notification_center_v284.sql")
        assertTrue("alter table public.push_outbox add column if not exists read_at timestamptz" in sql)
        assertTrue("create or replace function public.native_my_notifications_v284(p_limit int default 50)" in sql)
        assertTrue("create or replace function public.native_notifications_read_v284(p_ids bigint[] default null)" in sql)
        assertTrue("where user_id=auth.uid() and read_at is null and (p_ids is null or id = any(p_ids))" in sql)
        for (f in listOf("push_enqueue_exam_reminders", "push_enqueue_exam_closing", "push_on_class_member_added", "exam_audience_v284"))
            assertTrue(f, "function private.$f(" in sql)
        // بدون شرط توکن: اعلان برای همه ثبت می‌شود
        assertTrue("where p_user is not null\n  on conflict(dedupe_key) do nothing;" in sql)
        assertTrue("a.submitted_at is not null" in sql)
        assertTrue("perform private.push_enqueue_exam_reminders();" in sql && "perform private.push_enqueue_exam_closing();" in sql)
    }

    @Test
    fun `app bell in top bar opens history and navigates like a push tap`() {
        val bell = src("app/src/main/java/ir/exam/app/ui/notifications/NotificationCenter.kt")
        assertTrue("fun NotificationBell(userId: String, tint: Color, onOpen: (page: String?, code: String?) -> Unit)" in bell)
        assertTrue("BadgedBox(badge = { if (unread > 0) Badge" in bell)
        assertTrue("repo.markAllRead()" in bell)
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseNotificationRepository.kt")
        assertTrue("rpc(\"native_my_notifications_v284\"" in repo && "rpc(\"native_notifications_read_v284\"" in repo)
        assertTrue(".decodeAs<JsonObject>()" in repo)
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("ir.exam.app.ui.notifications.NotificationBell(" in app)
        assertTrue("onOpen = { p, c -> ir.exam.app.core.push.PushNavigation.open(p, c) }" in app)
        assertTrue("fun open(page: String?, code: String? = null)" in src("app/src/main/java/ir/exam/app/core/push/PushNavigation.kt"))
        assertEquals("", ir.exam.app.ui.notifications.relativeTime("not-a-date"))
        assertTrue(ir.exam.app.ui.notifications.relativeTime("2026-10-14T06:30:00+00:00").contains("۱۰:۰۰"))
    }

    @Test
    fun `site floating bell shared by mobile and desktop`() {
        val js = src("site/src/push.js")
        assertTrue("id: 'nc-bell'" in js && "native_my_notifications_v284" in js && "native_notifications_read_v284" in js)
        assertTrue("navigate(d.page, {exam_id: d.exam_id, code: d.code})" in js)
        assertTrue("notifications: {refresh: ncRefresh, open: ncOpen}" in js)
        val css = src("site/src/site.css")
        assertTrue(".nc-bell{position:fixed" in css && ".dk .nc-bell{left:118px;bottom:auto;top:16px}" in css)
    }
}
