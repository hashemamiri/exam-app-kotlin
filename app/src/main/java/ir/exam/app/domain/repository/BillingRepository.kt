package ir.exam.app.domain.repository

import ir.exam.app.domain.model.PaymentLaunch
import ir.exam.app.domain.model.WalletSnapshot

interface BillingRepository {
    suspend fun wallet(): Result<WalletSnapshot>
    suspend fun requestTopUp(amountToman: Long, currentBalanceToman: Long): Result<PaymentLaunch>

    /**
     * V132 — کسرِ هزینهٔ چاپ (۱۰۰۰ تومان به‌ازای هر سؤال) از کیف پول، پیش از بازشدنِ
     * پنلِ چاپ. `operationId` باید uuid یکتا باشد (idempotent در سرور).
     */
    suspend fun chargePrint(examId: String, operationId: String, questionCount: Int, mode: String): Result<PrintChargeResult>
}

data class PrintChargeResult(val costToman: Long, val balanceToman: Long?)
