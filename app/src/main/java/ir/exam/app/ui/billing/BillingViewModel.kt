package ir.exam.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseBillingRepository
import ir.exam.app.domain.model.PaymentLaunch
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.domain.model.WalletSnapshot
import ir.exam.app.domain.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingState(
    val wallet: WalletSnapshot? = null,
    val topUpAmount: String = WalletRules.MIN_TOP_UP_TOMAN.toString(),
    val payment: PaymentLaunch? = null,
    val loading: Boolean = true,
    val startingPayment: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class BillingViewModel(
    private val repository: BillingRepository = SupabaseBillingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(BillingState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, message = null) }
        repository.wallet()
            .onSuccess { wallet -> _state.update { it.copy(loading = false, wallet = wallet) } }
            .onFailure { error -> _state.update { it.copy(loading = false, error = safeBillingError(error)) } }
    }

    fun setTopUpAmount(value: String) {
        _state.update { it.copy(topUpAmount = value.filter(Char::isDigit).take(8), error = null, message = null) }
    }

    fun selectPreset(amount: Long) {
        _state.update { it.copy(topUpAmount = amount.toString(), error = null, message = null) }
    }

    fun startPayment() = viewModelScope.launch {
        val wallet = state.value.wallet ?: return@launch
        val amount = state.value.topUpAmount.toLongOrNull()
        if (amount == null) {
            _state.update { it.copy(error = "مبلغ شارژ را وارد کنید.") }
            return@launch
        }
        _state.update { it.copy(startingPayment = true, payment = null, error = null, message = null) }
        repository.requestTopUp(amount, wallet.balanceToman)
            .onSuccess { payment ->
                _state.update {
                    it.copy(
                        startingPayment = false,
                        payment = payment,
                        message = if (payment.sandbox) "درگاه آزمایشی باز می‌شود؛ وجه واقعی کسر نخواهد شد." else "درگاه بانکی آماده است."
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(startingPayment = false, error = safeBillingError(error)) } }
    }

    fun paymentOpened() {
        _state.update { it.copy(payment = null) }
    }
}

private fun safeBillingError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .replace(Regex("""https?://\S+"""), "")
    .take(260)
    .ifBlank { "عملیات کیف پول ناموفق بود." }
