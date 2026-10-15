package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V307 / ۱۲ — هم‌ترازی بانک آمادهٔ ابتدایی: اپ و سایت از یک فایل JSON استفاده می‌کنند. */
class V307_Site_PrimaryBankTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    @Test
    fun `app shows primary bank only in primary mode`() {
        val screen = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        assertTrue(screen.contains("if (state.primaryMode) {\n                    OutlinedButton(onClick = { primaryBankOpen = true }"))
        assertTrue(screen.contains("PrimaryBankDialog(viewModel = viewModel"))
        val vm = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").readText()
        assertTrue(vm.contains("fun addPrimaryBankDrafts(drafts: List<QuestionDraft>)"))
    }

    @Test
    fun `site builder embeds the same bank json and offers it in primary mode`() {
        val builder = File(root(), "site/src/builder.js").readText()
        assertTrue(builder.contains("var PRIMARY_BANK = /*__PRIMARY_BANK__*/"))
        assertTrue(builder.contains("if (bankAllowed() && state.primaryMode && !state.bankEdit) list.appendChild"))
        assertTrue(builder.contains("'🏫 بانک آمادهٔ ابتدایی'"))
        val build = File(root(), "site/build_site.py").readText()
        assertTrue(build.contains("primary_bank.json"))
        assertTrue(build.contains("site_js.replace(\"/*__PRIMARY_BANK__*/\", bank_json)"))
    }
}
