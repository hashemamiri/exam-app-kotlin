package ir.exam.app.domain.model

data class WalletTransaction(
    val id: Long,
    val amountToman: Long,
    val reason: String,
    val balanceAfterToman: Long,
    val createdAt: String
)

data class WalletSnapshot(
    val balanceToman: Long,
    val currency: String = "toman",
    val transactions: List<WalletTransaction> = emptyList()
)

data class PaymentLaunch(
    val orderId: Long,
    val checkoutUrl: String? = null,
    val provider: String,
    val sandbox: Boolean,
    val credited: Boolean = false,
    val balanceAfterToman: Long? = null
)

object WalletRules {
    const val QUESTION_COST_TOMAN = 1_000L
    const val MIN_TOP_UP_TOMAN = 100_000L
    const val MAX_BALANCE_TOMAN = 10_000_000L
    const val TOP_UP_STEP_TOMAN = 10_000L

    fun validateTopUp(amount: Long, currentBalance: Long) {
        require(amount >= MIN_TOP_UP_TOMAN) { "حداقل شارژ ۱۰۰٬۰۰۰ تومان است." }
        require(amount % TOP_UP_STEP_TOMAN == 0L) { "مبلغ شارژ باید مضربی از ۱۰٬۰۰۰ تومان باشد." }
        require(amount <= MAX_BALANCE_TOMAN) { "مبلغ شارژ از سقف مجاز بیشتر است." }
        require(currentBalance + amount <= MAX_BALANCE_TOMAN) { "موجودی پس از شارژ از سقف ۱۰٬۰۰۰٬۰۰۰ تومان بیشتر می‌شود." }
    }
}
