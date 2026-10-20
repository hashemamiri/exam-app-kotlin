package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V321 — اپ: کارنامهٔ تحلیلی، بررسی نهایی، عکس فوری آفلاین و کلید سریع تیره/روشن. */
class V321_App_SpeedDarkTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(path: String) = File(root(), path).readText()

    @Test
    fun reportsScreenShowsAnalyticsCardAndOfflineBanner() {
        val screen = read("app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt")
        // V321.1 — محاسبه از داخل دامنهٔ LazyColumn بیرون آورده شد (remember در LazyListScope مجاز نیست)
        assertTrue("val classAnalytics = remember(state.rows, selectedExams)" in screen)
        assertTrue("item { ClassAnalyticsCard(classAnalytics) }" in screen)
        assertTrue("remember(state.rows, selectedExams)" in screen)
        assertTrue("if (state.offline) item { OfflineBanner(state.offlineSavedAt) }" in screen)
    }

    @Test
    fun analyticsCardUsesThemeColorsOnly() {
        val card = read("app/src/main/java/ir/exam/app/ui/reports/ClassAnalyticsCard.kt")
        assertTrue("MaterialTheme.colorScheme.primary" in card)
        assertTrue("Color(0xFF" !in card)
    }

    @Test
    fun viewModelRestoresAndSavesOfflineSnapshot() {
        val vm = read("app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt")
        assertTrue("applyOfflineSnapshot()" in vm)
        assertTrue("ReportsSnapshot.write(userKey(), payload)" in vm)
        assertTrue("ReportsSnapshot.read(userKey())" in vm)
        val app = read("app/src/main/java/ir/exam/app/ExamApplication.kt")
        assertTrue("ReportsSnapshot.attachDir(" in app)
    }

    @Test
    fun builderRunsPreflightBeforeSaving() {
        val builder = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("val requestSave: () -> Unit = {" in builder)
        assertTrue("val report = BuilderPreflight.check(state.title, state.questions)" in builder)
        assertTrue("onClick = requestSave," in builder)
        assertTrue("بررسی نهایی — نیاز به اصلاح" in builder)
        // V321.1 — آیکن FactCheck در دسترس نبود؛ دکمهٔ متنی «بررسی» جای آن نشست
        assertTrue("{ Text(\"بررسی\", style = MaterialTheme.typography.labelLarge) }" in builder)
    }

    @Test
    fun darkToggleIsWiredFromTheMainMenu() {
        val menu = read("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        assertTrue("onToggleTheme: (() -> Unit)? = null" in menu)
        assertTrue("if (darkTheme) \"☀️\" else \"🌙\"" in menu)
        val app = read("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("onToggleTheme = toggleTheme" in app)
        assertTrue("appearancePrefs.setTheme(next)" in app)
        // V321.1 — اعلان‌ها باید در همان composable‌ای باشند که منو را می‌سازد (AuthenticatedShell)
        assertTrue("val themeScope = rememberCoroutineScope()" in app)
        // V321.2 — LocalContext.current خودش @Composable است و داخل بلوک remember مجاز نیست
        assertTrue("val themeAppContext = LocalContext.current.applicationContext" in app)
        assertTrue("remember(LocalContext.current" !in app)
    }
}
