package ir.exam.app.ui.app

import ir.exam.app.domain.model.ManagerWalletRules
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** رگرسیون V38: انتقال اتمیک مضرب ۱۰۰۰ و آمار کامل مدرسه. */
class V38ManagerWalletStatsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `transfer accepts arbitrary positive multiples of one thousand`() {
        assertTrue(ManagerWalletRules.isValidTransfer(1_000))
        assertTrue(ManagerWalletRules.isValidTransfer(27_000))
        assertTrue(ManagerWalletRules.isValidTransfer(1_234_000))
        assertFalse(ManagerWalletRules.isValidTransfer(0))
        assertFalse(ManagerWalletRules.isValidTransfer(27_500))
        assertFalse(ManagerWalletRules.isValidTransfer(-1_000))
        assertEquals(1_000L, ManagerWalletRules.TRANSFER_STEP_TOMAN)
    }

    @Test
    fun `sql transfer is atomic idempotent and double entry`() {
        val migration = source("supabase/migrations/20260815_native_manager_wallet_stats_v38.sql")
        assertEquals(migration, source("sql/manual/SQL_NATIVE_MANAGER_WALLET_STATS_V38.sql"))
        listOf(
            "native_scope_new_school_row_v38",
            "trg_scope_new_class_v38",
            "trg_scope_new_exam_v38",
            "manager_wallet_transfers_v38",
            "p_amount_toman%1000<>0",
            "operation_id=p_operation",
            "for update",
            "balance=balance-p_amount_toman",
            "balance=balance+p_amount_toman",
            "school_transfer_to_teacher",
            "school_transfer_from_manager",
            "already_applied"
        ).forEach { assertTrue("missing $it", it in migration) }
    }

    @Test
    fun `manager can top up own wallet through existing secure payment`() {
        val migration = source("supabase/migrations/20260815_native_manager_wallet_stats_v38.sql")
        val app = source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("role in('teacher','manager')" in migration)
        assertTrue("UserRole.MANAGER -> WalletScreen" in app)
        assertFalse("ManagerWalletFoundationScreen" in app)
    }

    @Test
    fun `teacher card supports transfer and displays refreshed balance`() {
        val manager = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        val repository = source("app/src/main/java/ir/exam/app/data/repository/SupabaseManagerRepository.kt")
        assertTrue("مبلغ باید مضرب ۱٬۰۰۰ تومان باشد" in manager)
        assertTrue("ManagerWalletRules.isValidTransfer(amount)" in manager)
        assertTrue("Icons.Outlined.AccountBalanceWallet" in manager)
        assertTrue("\"شارژ کیف پول\"" in manager)
        assertTrue("native_manager_transfer_wallet_v38" in repository)
        assertTrue("ManagerWalletRules.validateTransfer" in repository)
    }

    @Test
    fun `manager cards show complete school statistics and teacher activity`() {
        val migration = source("supabase/migrations/20260815_native_manager_wallet_stats_v38.sql")
        val manager = source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        listOf("answers", "average_percent", "distributed_toman", "teacher_activity", "wallet_balance").forEach {
            assertTrue(it in migration)
        }
        listOf("پاسخ‌ها", "میانگین نمره", "مجموع اعتبار توزیع‌شده", "فعالیت معلم‌ها").forEach {
            assertTrue(it in manager)
        }
    }
}
