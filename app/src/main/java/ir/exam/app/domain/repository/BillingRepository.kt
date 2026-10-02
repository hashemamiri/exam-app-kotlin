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
    suspend fun chargePrint(examId: String, operationId: String, questionCount: Int, mode: String, content: String = ""): Result<PrintChargeResult>

    /**
     * V202.1 — برآورد پیش از چاپ: اگر همین محتوا قبلاً (در اپ یا سایت) پرداخت شده باشد `paid=true`
     * و چاپ بدون پنجرهٔ هزینه آغاز می‌شود. `content` فقط برای پیش‌نویس محلی («local») به کار می‌رود.
     */
    suspend fun quotePrint(examId: String, questionCount: Int, content: String = ""): Result<PrintQuoteResult>
}

data class PrintChargeResult(val costToman: Long, val balanceToman: Long?, val alreadyPaid: Boolean = false)
data class PrintQuoteResult(val paid: Boolean, val dueToman: Long, val questions: Int)

/** V202.1 — متن محتوا برای هش پیش‌نویس محلی (اپ و سایت یکسان: متن سؤال‌ها با خط جدید) */
fun printContentKey(questionTexts: List<String>): String = questionTexts.joinToString("\n") { it.trim() }
