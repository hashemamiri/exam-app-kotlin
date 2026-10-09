package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V232.9 — اعلان وب (PWA): بدون SDK فایربیس؛ پیکربندی از Secrets در CI؛ SW اعلان را می‌سازد و مقصد را نگه می‌دارد. */
class V232_SitePushTest {
    private fun src(rel: String): String = File(listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }, rel).readText()

    @Test
    fun configInjectedOnlyInCi() {
        val tpl = src("site/src/template.html")
        assertTrue("var FIREBASE_WEB_CONFIG = null;" in tpl && "var FIREBASE_VAPID_KEY = \"\";" in tpl && "FIREBASE_WEB: FIREBASE_WEB_CONFIG, FIREBASE_VAPID_KEY: FIREBASE_VAPID_KEY" in tpl)
        val ci = src(".github/workflows/site.yml")
        assertTrue("FIREBASE_WEB_CONFIG_JSON: \${{ secrets.FIREBASE_WEB_CONFIG_JSON }}" in ci && "FIREBASE_VAPID_KEY: \${{ secrets.FIREBASE_VAPID_KEY }}" in ci)
        assertTrue("for k in ('apiKey', 'projectId', 'appId', 'messagingSenderId') if k in cfg" in ci && "web push: disabled (secrets missing)" in ci)
        assertTrue("read(os.path.join(SITE, \"src\", \"push.js\"))" in src("site/build_site.py"))
    }

    @Test
    fun pushModuleContracts() {
        val p = src("site/src/push.js")
        assertTrue("https://firebaseinstallations.googleapis.com/v1" in p && "https://fcmregistrations.googleapis.com/v1" in p)
        assertTrue("a[0] = 0x70 + (a[0] % 16)" in p && "authVersion: 'FIS_v2'" in p && "'x-goog-firebase-installations-auth': 'FIS ' + fis" in p)
        assertTrue("applicationServerKey: vapidBytes(VAPID)" in p && "applicationPubKey: VAPID" in p)
        assertTrue("S.rpcObj('native_push_register_v1', {p_token: token, p_platform: 'web'})" in p && "S.rpcObj('native_push_unregister_v1', {p_token: t})" in p)
        assertTrue("Notification.requestPermission()" in p && "id: 'push-offer'" in p && "d.type === 'push-nav'" in p)
        assertTrue("if (page === 'grades') return role === 'student' ? ['grades'] : (role === 'teacher' ? ['grading'] : null);" in p)
        assertFalse("firebasejs" in p || "gstatic.com" in p)
        assertTrue("window.SitePush = {available: available" in p)
    }

    @Test
    fun serviceWorkerAndUi() {
        val sw = src("site/pwa/sw.js")
        assertTrue("self.addEventListener('push'" in sw && "self.addEventListener('notificationclick'" in sw && "c.postMessage({type: 'push-nav'" in sw && "self.clients.openWindow('/' + q)" in sw)
        assertTrue("if (window.SitePush && window.SitePush.available()) c.appendChild(card('اعلان‌ها'" in src("site/src/mobile.js"))
        assertTrue("if (arg && arg.code) inp.value = String(arg.code);" in src("site/src/student.js"))
        val fn = src("supabase/functions/push-dispatch/index.ts")
        assertTrue("if (platform === 'web') {" in fn && "message.webpush = { headers: { Urgency: 'high', TTL: '86400' } };" in fn)
    }
}
