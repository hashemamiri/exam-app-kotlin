package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V260.4 — سایت: کش پروفایل و داشبورد معلم (رندر فوری، تازه‌سازی در پس‌زمینه)؛ پاک شدن با خروج. */
class V260_4_SiteProfileDashCacheTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(p: String) = File(root(), p).readText()

    @Test
    fun `profile and dashboard are cached per uid and cleared on logout`() {
        val a = source("site/src/app.js")
        assertTrue("var LS_PROFILE = 'examsite.profile.v1', LS_DASH = 'examsite.dash.v1.';" in a)
        assertTrue("return o && o.uid === uid ? o.data : null;" in a)
        assertTrue("localStorage.removeItem(LS_PROFILE); Object.keys(localStorage).filter(function (k) { return k.indexOf(LS_DASH) === 0; })" in a)
        assertTrue("if (!requiresSetup) cacheSet(LS_PROFILE, su.id, prof);" in a)
        assertTrue("if (cachedProfile && !cachedProfile.requiresSetup) { user = cachedProfile; profileStale = true; }" in a)
        assertTrue("if (profileStale) currentProfile().then(function (fresh) {" in a)
        assertTrue("function paintTeacherDash(c, r, failed, stale) {" in a)
        assertTrue("if (cachedDash && !c.__dashFresh) { paintTeacherDash(c, cachedDash, [], true); }" in a)
        assertTrue("Array.isArray(r[2]) ? {__n: r[2].length} : r[2]" in a)
        assertTrue(".dash-stale .stat .v{opacity:.55" in source("site/src/site.css"))
    }
}
