package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankContract
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V372 — گارد تصویر «تکه‌ای» در سایت و اپ.
 *
 * قاعده باید در سه جا یکی باشد: ابزار داده (پایتون)، گارد سایت (جاوااسکریپت)،
 * و اپ ([ReferenceBankContract.isTinyImage]). این تست همان هم‌خوانی و
 * پنهان‌شدنِ تکه‌ها را قفل می‌کند.
 */
class V372_TinyImageGuardTest {

    private fun root(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (!File(dir, "settings.gradle.kts").isFile) {
            dir = dir.parentFile ?: error("ریشهٔ مخزن پیدا نشد")
        }
        return dir
    }

    private fun src(path: String): String = File(root(), path).readText(Charsets.UTF_8)

    private val site get() = src("site/src/refbank.js")
    private val dialog get() = src("app/src/main/java/ir/exam/app/ui/bank/ReferenceBankDialog.kt")
    private val guard get() = src("app/src/main/java/ir/exam/app/ui/bank/RefBankImage.kt")

    @Test
    fun `the tiny rule matches the data tool`() {
        assertEquals(40, ReferenceBankContract.MIN_IMAGE_SIDE)
        assertEquals(1200, ReferenceBankContract.MIN_IMAGE_AREA)
        assertTrue("تکهٔ «؟»", ReferenceBankContract.isTinyImage(16, 32))
        assertTrue("مربع ریز", ReferenceBankContract.isTinyImage(30, 30))
        assertFalse("نوار باریک و بلند واقعی", ReferenceBankContract.isTinyImage(30, 400))
        assertFalse("شکل هندسی", ReferenceBankContract.isTinyImage(300, 200))
        assertFalse("اندازهٔ نامعلوم", ReferenceBankContract.isTinyImage(0, 0))
    }

    @Test
    fun `site hides tiny images`() {
        assertTrue("ثابت اندازه", "var TINY_SIDE = 40;" in site)
        assertTrue("ثابت مساحت", "var TINY_AREA = 1200;" in site)
        assertTrue("پنهان‌سازی", "im.style.display = 'none';" in site)
        assertTrue("شنود بارش تصویر بانک", "reference-bank/images/" in site)
        assertTrue("گارد در lazyImgs", "tinyGuard(im);" in site)
        assertTrue("سنجش اندازهٔ طبیعی", "im.naturalWidth" in site)
        assertTrue("نشانه‌گذاری", "__tinyGuard" in site)
    }

    @Test
    fun `app draws every bank image through the guard`() {
        assertTrue("گارد اپ", "fun RefBankImage(" in guard)
        assertTrue("قاعدهٔ مشترک", "ReferenceBankContract.isTinyImage(" in guard)
        assertTrue("سنجش پس از بار شدن", "onSuccess" in guard)
        assertFalse("پنجرهٔ بانک نباید مستقیم AsyncImage بکشد", "AsyncImage(" in dialog)
        assertTrue("پنجره از گارد استفاده می‌کند", dialog.contains("RefBankImage("))
    }

    @Test
    fun `shard cache key was bumped`() {
        val models = src("app/src/main/java/ir/exam/app/domain/bank/ReferenceBankModels.kt")
        assertTrue("کلید تازه در اپ", "v3710" in models)
        assertTrue("کلید تازه در سایت", "v3710" in site)
        assertFalse("کلید کهنه نماند", "v3630" in site || "v3630" in models)
    }
}
