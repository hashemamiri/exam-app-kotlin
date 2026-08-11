package ir.exam.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WalletRulesTest {
    @Test
    fun `valid top up respects toman unit and cap`() {
        WalletRules.validateTopUp(100_000, 0)
        WalletRules.validateTopUp(500_000, 9_500_000)
        assertEquals(1_000L, WalletRules.QUESTION_COST_TOMAN)
    }

    @Test
    fun `top up below minimum or above balance cap is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { WalletRules.validateTopUp(90_000, 0) }
        assertThrows(IllegalArgumentException::class.java) { WalletRules.validateTopUp(110_001, 0) }
        assertThrows(IllegalArgumentException::class.java) { WalletRules.validateTopUp(100_000, 9_950_000) }
    }
}
