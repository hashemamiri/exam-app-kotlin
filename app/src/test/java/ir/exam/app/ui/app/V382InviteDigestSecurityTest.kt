package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V382InviteDigestSecurityTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    @Test
    fun `invite digest is schema qualified bytea and UI strips headers`() {
        val root = root()
        val migration = File(root, "supabase/migrations/20260815_native_invite_digest_v382_hotfix.sql").readText()
        val copy = File(root, "sql/manual/SQL_NATIVE_INVITE_DIGEST_V382_HOTFIX.sql").readText()
        val manager = File(root, "app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt").readText()
        assertEquals(migration, copy)
        assertTrue("extensions.digest(convert_to(v_token,'UTF8'),'sha256')" in migration)
        assertTrue("extensions.digest(convert_to(btrim(coalesce(p_invite_code,'')),'UTF8'),'sha256')" in migration)
        assertFalse("encode(digest(" in migration)
        // V209 — پاک‌سازی به UserFacingError منتقل شد (همان قواعد: URL/Headers/authorization/apikey/bearer)
        assertTrue("UserFacingError.of(error" in manager)
        val ufe = File(root, "app/src/main/java/ir/exam/app/core/network/UserFacingError.kt").readText()
        assertTrue("substringBefore(\"URL:\")" in ufe)
        assertTrue("substringBefore(\"Headers:\")" in ufe)
        assertTrue("authorization" in ufe.lowercase())
        assertTrue("apikey" in ufe.lowercase())
        assertTrue("bearer\\\\s+" in ufe.lowercase())
    }
}
