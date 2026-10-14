package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V283 — قفل ورود: ۵ رمز اشتباه در ۱۵ دقیقه → ۱۵ دقیقه قفل؛ دروازهٔ سرور + مسیر جایگزین وقتی تابع نیست. */
class V283_LoginLockoutTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `server gate and clients agree`() {
        val sql = src("supabase/migrations/20261014_native_login_lockout_v283.sql")
        assertTrue("create or replace function public.native_login_gate_v283(p_identifier text, p_password text)" in sql)
        assertTrue("v_max constant int := 5;" in sql && "v_window constant interval := interval '15 minutes';" in sql)
        assertTrue("extensions.crypt(p_password, v_user.encrypted_password) = v_user.encrypted_password" in sql)
        assertTrue("update auth.users set banned_until = now() + v_window where id = v_user.id;" in sql)
        assertTrue("grant execute on function public.native_login_gate_v283(text, text) to anon, authenticated;" in sql)
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt")
        assertTrue("val gated = loginGate(clean, password)" in repo && "\"native_login_gate_v283\"," in repo)
        assertTrue("if (\"native_login_gate_v283\" in msg || \"PGRST202\" in msg || \"404\" in msg) return null" in repo)
        val vm = src("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt")
        assertTrue("\"banned\" in text ->" in vm)
        val js = src("site/src/app.js")
        assertTrue("async function loginGate(identifier, password)" in js && "var gated = await loginGate(clean, password);" in js)
        assertTrue("if (/banned/i.test(m)) return" in js)
    }
}
