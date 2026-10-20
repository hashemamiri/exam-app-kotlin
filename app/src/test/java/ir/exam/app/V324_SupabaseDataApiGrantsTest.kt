package ir.exam.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V324 — تغییر «Data API» سوپابیس (۳۰ اکتبر): جدول تازه در schema عمومی بدون GRANT صریح
 * برای anon / authenticated / service_role از PostgREST و توابع لبه در دسترس نیست.
 *
 * این تست مثل `scripts/check_supabase_data_api_grants.py` عمل می‌کند و در CI جلوی مهاجرتی را
 * می‌گیرد که `create table` دارد ولی `grant` را جا انداخته است.
 */
class V324_SupabaseDataApiGrantsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun migrations(): List<File> =
        File(root(), "supabase/migrations").listFiles { f -> f.name.endsWith(".sql") }!!.sortedBy { it.name }

    private fun sql(): String = migrations().joinToString("\n") { it.readText() }
        .replace(Regex("/\\*[\\s\\S]*?\\*/"), " ")
        .replace(Regex("--[^\\n]*"), " ")

    @Test
    fun everyPublicTableCreatedByMigrationsHasExplicitGrants() {
        val all = sql()
        val tables = Regex(
            "create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:(public|private|auth|storage)\\s*\\.\\s*)?([a-zA-Z0-9_]+)",
            RegexOption.IGNORE_CASE
        ).findAll(all)
            .filter { (it.groupValues[1].ifEmpty { "public" }).lowercase() == "public" }
            .map { it.groupValues[2].lowercase() }.toSortedSet()
        assertTrue("هیچ جدولی در مهاجرت‌ها پیدا نشد", tables.size >= 40)
        for (t in tables) {
            val line = Regex(
                "grant\\s+[a-z, ]+?\\s+on\\s+(?:table\\s+)?(?:public\\s*\\.\\s*)?$t\\s+to\\s+([a-z_, ]+)",
                RegexOption.IGNORE_CASE
            ).findAll(all).flatMap { m -> m.groupValues[1].lowercase().split(',').map { it.trim() } }.toSet()
            assertTrue("جدول $t گرنت anon ندارد", "anon" in line)
            assertTrue("جدول $t گرنت authenticated ندارد", "authenticated" in line)
            assertTrue("جدول $t گرنت service_role ندارد", "service_role" in line)
        }
    }

    @Test
    fun futureTablesKeepWorkingViaDefaultPrivileges() {
        val all = sql()
        for (role in listOf("anon", "authenticated", "service_role")) {
            assertTrue(
                "alter default privileges برای $role نیست؛ جدول‌های آینده و db reset بی‌دسترسی می‌شوند",
                Regex(
                    "alter\\s+default\\s+privileges[\\s\\S]{0,200}?in\\s+schema\\s+public\\s+grant[\\s\\S]{0,80}?to\\s+[a-z_, ]*$role",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(all)
            )
        }
    }

    @Test
    fun privateSchemaStaysPrivate() {
        val all = sql()
        // push_config در schema خصوصی است و نباید به anon/authenticated گرنت بگیرد
        val granted = Regex(
            "grant\\s+[a-z, ]+?\\s+on\\s+(?:table\\s+)?(?:private\\s*\\.\\s*)?push_config\\s+to\\s+([a-z_, ]+)",
            RegexOption.IGNORE_CASE
        ).findAll(all).flatMap { m -> m.groupValues[1].lowercase().split(',').map { it.trim() } }.toSet()
        assertFalse("push_config نباید به anon گرنت بدهد", "anon" in granted)
        assertFalse("push_config نباید به authenticated گرنت بدهد", "authenticated" in granted)
        assertTrue("فایل مهاجرت گرنت‌ها وجود ندارد",
            migrations().any { it.name.contains("explicit_data_api_grants") })
    }

    @Test
    fun tablesUsedByEdgeFunctionsAreGranted() {
        val all = sql()
        val fnDir = File(root(), "supabase/functions")
        val used = fnDir.listFiles { f -> f.isDirectory }!!.flatMap { d ->
            d.listFiles { f -> f.name.endsWith(".ts") }!!.flatMap { f ->
                Regex("from\\('([a-z_]+)'\\)").findAll(f.readText()).map { it.groupValues[1] }.toList()
            }
        }.toSortedSet()
        assertTrue("تابعی پیدا نشد", used.isNotEmpty())
        for (t in used) {
            val created = Regex("create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:public\\s*\\.\\s*)?$t\\b", RegexOption.IGNORE_CASE).containsMatchIn(all)
            if (!created) continue // جدول‌های قدیمی داشبورد (profiles، classes…) که در مهاجرت‌ها نیستند
            assertTrue("جدول $t که تابع لبه استفاده می‌کند گرنت service_role ندارد",
                Regex("grant[^;]*on\\s+(?:table\\s+)?(?:public\\s*\\.\\s*)?$t\\s+to\\s+[a-z_, ]*service_role", RegexOption.IGNORE_CASE).containsMatchIn(all))
        }
    }

    @Test
    fun guardScriptIsWiredIntoPreflight() {
        val pre = File(root(), "scripts/preflight.py").readText()
        assertTrue("preflight باید نگهبان گرنت را اجرا کند",
            "check_supabase_data_api_grants.py" in pre)
        assertTrue("اسکریپت نگهبان وجود ندارد", File(root(), "scripts/check_supabase_data_api_grants.py").isFile)
    }
}
