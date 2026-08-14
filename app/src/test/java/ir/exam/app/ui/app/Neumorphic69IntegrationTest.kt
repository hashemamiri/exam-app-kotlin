package ir.exam.app.ui.app

import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.NeumorphicPalette
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Neumorphic69IntegrationTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt").isFile
    }

    @Test
    fun `dock contracts keep rtl real-action order`() {
        assertEquals(
            listOf(
                TeacherDockAction.MENU,
                TeacherDockAction.WALLET,
                TeacherDockAction.CREATE,
                TeacherDockAction.EXAMS,
                TeacherDockAction.CARDS
            ),
            TeacherDockContract.order
        )
        assertEquals(
            listOf(
                TeacherQuickCreateAction.STUDENT,
                TeacherQuickCreateAction.EXAM,
                TeacherQuickCreateAction.CLASS
            ),
            TeacherDockContract.quickCreateOrder
        )
        assertEquals(
            listOf(
                TeacherManagementAction.STATS,
                TeacherManagementAction.GRADING,
                TeacherManagementAction.PENDING
            ),
            TeacherDockContract.managementOrder
        )
    }

    @Test
    fun `appearance defaults and bounds match native design`() {
        val settings = AppearanceSettings()
        assertEquals(NeumorphicPalette.INDIGO_MINT, settings.neumorphicPalette)
        assertEquals(14f, settings.neumorphicDepth)
        assertEquals(8f, AppearancePreferences.MIN_NEO_DEPTH)
        assertEquals(22f, AppearancePreferences.MAX_NEO_DEPTH)
        assertEquals(4, NeumorphicPalette.values().size)
    }

    @Test
    fun `full page menu contract keeps complete real two-column rows`() {
        assertEquals(2, Design69MenuContract.COLUMNS)
        assertTrue(Design69MenuContract.PROFILE_HEIGHT_DP > Design69MenuContract.CARD_HEIGHT_DP)
        assertEquals(8, Design69MenuContract.TEACHER_CARD_COUNT)
        assertEquals(6, Design69MenuContract.STUDENT_CARD_COUNT)
        assertTrue(Design69MenuContract.isCompleteGrid(Design69MenuContract.TEACHER_CARD_COUNT))
        assertTrue(Design69MenuContract.isCompleteGrid(Design69MenuContract.STUDENT_CARD_COUNT))
        assertFalse(Design69MenuContract.isCompleteGrid(9))
    }

    @Test
    fun `quick add and management cards preserve selected real contracts`() {
        assertEquals(3, Design69QuickAddContract.ACTION_COUNT)
        assertEquals(135, Design69QuickAddContract.OPEN_ROTATION_DEGREES)
        assertEquals(5, Design69ManagementCardsContract.CARD_COUNT)
        assertEquals(52, Design69ManagementCardsContract.DRAG_THRESHOLD_DP)
    }

    @Test
    fun `reference navigation motion is native and full page`() {
        val root = root()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val icons = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt").readText()
        val menu = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt").readText()
        val dock = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()

        assertTrue("Design69MainMenuScreen(" in app)
        assertTrue("menuOpen = !menuOpen" in app)
        assertTrue("BackHandler(enabled = menuOpen" in app)
        assertFalse("side drawer must not remain", "ModalNavigationDrawer" in app)
        assertTrue("Design69MorphingMenuIcon" in icons)
        assertTrue("Design69Icons.Wallet" in dock)
        assertTrue("DockMotion.WALLET" in dock)
        assertTrue("rippleProgress.animateTo(1f, tween(520))" in dock)
        assertFalse("nested menu slide animation returned", "slideInHorizontally" in menu)
        assertFalse("slow stagger delay returned", "delay = 120 + index * 40" in menu)
        assertTrue("enter = fadeIn(tween(110))" in app)
        assertTrue("animationSpec = tween(180)" in icons)
    }

    @Test
    fun `quick add cards and refresh paths are real and reachable`() {
        val root = root()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val add = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt").readText()
        val cards = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt").readText()
        val dashboard = File(root, "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt").readText()
        val wallet = File(root, "app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt").readText()

        listOf("دانش‌آموز جدید", "آزمون جدید", "کلاس جدید").forEach {
            assertTrue("missing real add action $it", it in add)
        }
        assertTrue("OPEN_ROTATION_DEGREES * travel.value" in add)
        assertTrue("detectDragGestures" in cards)
        assertTrue("DRAG_THRESHOLD_DP = 52" in cards)
        assertTrue("Key.DirectionLeft" in cards && "Key.DirectionRight" in cards)
        assertFalse("vertical card navigation returned", "Key.DirectionDown" in cards)
        listOf("آمار", "بانک سؤال", "تصحیح", "مانده", "پاسخ").forEach {
            assertTrue("missing management card $it", it in cards)
        }
        assertTrue("cards[activeIndex].subtitle" in cards)
        assertFalse("management cards must not repeat as buttons", "cards.forEachIndexed" in cards)
        assertTrue("PullToRefreshBox(" in dashboard)
        assertTrue("onRefresh = viewModel::load" in dashboard)
        assertFalse("manual dashboard refresh button returned", "به‌روزرسانی" in dashboard)
        assertTrue("dashboardRefreshKey += 1" in app)
        assertTrue("walletRefreshKey += 1" in app)
        assertTrue("LaunchedEffect(refreshKey)" in wallet)
    }

    @Test
    fun `v18 compact navigation profile account and exams match requested behavior`() {
        val root = root()
        val app = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val dock = File(root, "app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt").readText()
        val menu = File(root, "app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt").readText()
        val profile = File(root, "app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").readText()
        val dashboard = File(root, "app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt").readText()
        val lock = File(root, "app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt").readText()

        assertTrue("mutableStateOf(MainPage.CALENDAR)" in app)
        listOf("تقویم و پیام‌ها", "کلاس‌ها", "دانش‌آموزان", "سربرگ", "حساب", "داده‌ها", "تنظیمات", "خروج").forEach {
            assertTrue("missing hamburger item $it", it in app)
        }
        val teacherMenu = app.substringAfter("val menuCards = if (user.role == UserRole.TEACHER)")
            .substringBefore("} else {")
        listOf("داشبورد معلم", "تصحیح و حضور", "آمار و گزارش‌ها", "درباره و بروزرسانی", "آزمون جدید").forEach {
            assertFalse("removed hamburger item returned: $it", it in teacherMenu)
        }
        assertFalse("removed profile sentence returned", "مشاهده و ویرایش حساب و تنظیمات" in menu)
        assertTrue(".size(44.dp)" in dock)
        assertTrue(".size(58.dp)" in dock)
        assertTrue("if (!expanded)" in dock)
        listOf("SettingsSection.APPEARANCE", "SettingsSection.ABOUT").forEach {
            assertTrue("missing settings section $it", it in profile)
        }
        assertFalse("account still nested in settings tabs", "SettingsSection.ACCOUNT" in profile)
        assertFalse("data still nested in settings tabs", "SettingsSection.DATA" in profile)
        assertTrue("ProfileSettingsDestination.ACCOUNT" in profile)
        assertTrue("ProfileSettingsDestination.DATA" in profile)
        assertTrue("تغییر ایمیل" in profile && "AppLockSettings" in profile)
        assertTrue("expandedExamId" in dashboard && "AnimatedVisibility" in dashboard)
        assertTrue("BiometricPrompt" in lock && "OutlinedTextField" !in lock)
    }

    @Test
    fun `native shell uses dual shadows without demo data or web runtime`() {
        val root = root()
        val design = File(root, "app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt").readText()
        val mainSources = File(root, "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        listOf("setShadowLayer", "lightShadow", "darkShadow", "pressed", "NeumorphicTopBar").forEach {
            assertTrue("missing native design marker: $it", it in design)
        }
        assertFalse("standalone demo package must not enter runtime", "com.example.neumorphic69" in mainSources)
        assertFalse("fake wallet balance must not enter runtime", "۱۲٬۴۸۰٬۰۰۰" in mainSources)
        assertFalse("WebView must not enter native runtime", "android.webkit" in mainSources)
    }

    @Test
    fun `reference font weights are copied exactly`() {
        val root = root()
        val expected = mapOf(
            "vazirmatn_medium.ttf" to "b986623e4ddef10755e04be39f8ea7bcb1dc08bfe8dd0aa6af395736f256ad4a",
            "vazirmatn_bold.ttf" to "f635fdbea28f265de395ba83b4b1570dcf2f58d13c65469e61903b1c2d2ae723"
        )
        expected.forEach { (name, hash) ->
            val file = File(root, "app/src/main/res/font/$name")
            assertTrue("missing $name", file.isFile)
            assertEquals(hash, sha256(file))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
