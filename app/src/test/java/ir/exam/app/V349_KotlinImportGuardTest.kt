package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V349.1 — نگهبان importهای داخلی.
 *
 * چرا: در V349 یک import اشتباه (QuestionType از بستهٔ نادرست) باعث شکست
 * کامپایل در CI شد. نگهبان‌های پرانتز/گیومه آن را نمی‌دیدند. این آزمون
 * نام‌های اعلام‌شدهٔ هر بسته را فهرست می‌کند و هر import داخلی را می‌سنجد.
 *
 * نسخهٔ کامل‌ترِ همین بررسی: scripts/check_kotlin_imports.py
 */
class V349_KotlinImportGuardTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val packageLine = Regex("^package\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
    private val importLine = Regex("^import\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
    private val typeDecl = Regex(
        "(?:class|interface|object|typealias)\\s+([A-Za-z_][A-Za-z0-9_]*)"
    )
    private val memberDecl = Regex(
        "(?:fun|val|var)\\s+(?:<[^>]+>\\s*)?(?:[\\w.<>,?\\s]+\\.)?([A-Za-z_][A-Za-z0-9_]*)"
    )
    private val generated = setOf("BuildConfig", "R", "Manifest")

    private fun sources(): List<File> {
        val base = File(root(), "app/src")
        return base.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun everyInternalImportResolvesToADeclaredName() {
        val files = sources()
        val declared = HashMap<String, MutableSet<String>>()
        val texts = HashMap<File, String>()
        for (f in files) {
            val text = f.readText()
            texts[f] = text
            val pkg = packageLine.find(text)?.groupValues?.get(1) ?: continue
            val names = declared.getOrPut(pkg) { HashSet() }
            typeDecl.findAll(text).forEach { names.add(it.groupValues[1]) }
            memberDecl.findAll(text).forEach { names.add(it.groupValues[1]) }
        }
        val problems = mutableListOf<String>()
        for (f in files) {
            val text = texts[f] ?: continue
            for (match in importLine.findAll(text)) {
                val imp = match.groupValues[1]
                if (!imp.startsWith("ir.exam.app") && !imp.startsWith("ir.onlineexam")) continue
                if (imp.endsWith(".*")) continue
                val name = imp.substringAfterLast('.')
                if (name in generated) continue
                val base = imp.substringBeforeLast('.', "")
                if (base.isEmpty()) continue
                val owners = declared[base]
                if (owners != null) {
                    if (name !in owners) {
                        problems.add("${f.name}: $imp (نام در بستهٔ $base نیست)")
                    }
                    continue
                }
                // عضو تودرتو: والد باید بسته باشد و نام کلاس در آن اعلام شده باشد
                val parent = base.substringBeforeLast('.', "")
                val owner = base.substringAfterLast('.')
                if (parent.isNotEmpty() && declared[parent]?.contains(owner) == true) continue
                problems.add("${f.name}: $imp (بستهٔ $base نیست)")
            }
        }
        assertTrue("خطای import پیدا شد:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    /** پس‌زمینهٔ مشخصِ V349: QuestionType در ui.builder است، نه domain.model. */
    @Test
    fun questionTypeIsImportedFromItsRealPackage() {
        // رشته از دو تکه ساخته می‌شود تا خودِ این فایل در جست‌وجو پیدا نشود
        val wrong = "import ir.exam.app.domain.model" + ".QuestionType"
        for (f in sources()) {
            assertTrue(
                "${f.name} باید QuestionType را از ir.exam.app.ui.builder بگیرد",
                !f.readText().contains(wrong)
            )
        }
        val draft = File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").readText()
        assertTrue("QuestionType باید در ui/builder/QuestionDraft.kt اعلام شود", draft.contains("enum class QuestionType"))
        val section = File(root(), "app/src/main/java/ir/exam/app/ui/builder/BuilderCodeSection.kt").readText()
        assertTrue("BuilderCodeSection در همان بسته است و به import نیاز ندارد", section.contains("package ir.exam.app.ui.builder"))
        assertTrue(section.contains("QuestionType.ESSAY"))
    }

    @Test
    fun localImportCheckerIsPresent() {
        val script = File(root(), "scripts/check_kotlin_imports.py")
        assertTrue("نگهبان محلی import باید بماند", script.exists())
        assertTrue(script.readText().contains("Unresolved reference"))
    }
}
