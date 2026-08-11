package ir.exam.app.domain.repository

import ir.exam.app.domain.model.PaymentLaunch
import ir.exam.app.domain.model.WalletSnapshot

interface BillingRepository {
    suspend fun wallet(): Result<WalletSnapshot>
    suspend fun requestTopUp(amountToman: Long, currentBalanceToman: Long): Result<PaymentLaunch>
}
