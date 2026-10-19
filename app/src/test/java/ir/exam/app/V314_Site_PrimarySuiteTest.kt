package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V314.0 — هم‌ترازی وب (سایت) با اندروید در بستهٔ ۵گانهٔ ارتقای ابتدایی.
 */
class V314_Site_PrimarySuiteTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun siteHasAllFiveElementaryCategories() {
        val builder = File(root(), "site/src/builder.js").readText()
        val student = File(root(), "site/src/student.js").readText()
        val admin = File(root(), "site/src/admin.js").readText()
        val app = File(root(), "site/src/app.js").readText()
        val graphFig = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()

        // ۱) گونه‌های تعاملی جدید: بخش‌کردن و صداکشی + دسته‌بندی در سبد
        assertTrue(builder.contains("['syllable', 'بخش‌کردن و صداکشی']"))
        assertTrue(builder.contains("['bucket', 'دسته‌بندی در سبد']"))
        assertTrue(student.contains("function syllableAnswer("))
        assertTrue(student.contains("function bucketAnswer("))

        // ۲) ۱۴ ابزار جدید ریاضی/علوم ابتدایی در graph_fig.js (۶ ابزار V314 + ۸ ابزار V315)
        for (k in listOf(
            "axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern",
            "axtenframe", "axsym", "axspin", "axbeaker", "axgeoboard", "axdice", "axmularea", "axvenn"
        )) {
            assertTrue("Missing $k in graph_fig.js", graphFig.contains(k))
        }

        // ۳) بانک آمادهٔ غنی‌شده + جستجو + افزودن همه در builder.js
        assertTrue(builder.contains("PRIMARY_BANK.extra_items"))
        assertTrue(builder.contains("➕ افزودن همه ("))

        // ۴) مهرهای تشویقی معلم و کارنامهٔ توصیفی به تفکیک درس
        assertTrue(admin.contains("🌟 آفرین قهرمان! خیلی خوب درخشیدی"))
        assertTrue(app.contains("📋 کارنامهٔ توصیفی به تفکیک درس"))

        // ۵) تعامل لمسی مستقیم و منوی کشویی همهٔ شکل‌های ابتدایی در وب (bindClock + GF_DROPDOWNS)
        assertTrue(graphFig.contains("if (t === 'axfrac')"))
        assertTrue(graphFig.contains("if (t === 'axabacus')"))
        assertTrue(graphFig.contains("if (t === 'axthermo')"))
        assertTrue(graphFig.contains("var GF_DROPDOWNS ="))
        assertTrue(graphFig.contains("'axsaw:X.mode'"))
        assertTrue(graphFig.contains("'axsym:X.axis'"))
        assertTrue(graphFig.contains("'axbeaker:X.unit'"))
        assertTrue(graphFig.contains("direction=\"ltr\""))
    }
}
