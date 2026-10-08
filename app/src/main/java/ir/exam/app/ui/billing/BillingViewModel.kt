package ir.exam.app.ui.billing

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.ui.PullRefreshGate
import ir.exam.app.core.network.UserFacingError
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
    val refreshing: Boolean = false, // V227 — فقط برای نشانگر «کشیدن برای بازخوانی»
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

    // V227 — بازخوانی با کشیدن صفحه: پرچم جداگانهٔ refreshing (نه loading که با حافظهٔ موقت false می‌ماند و نه actionLoading)؛
    // یک بازخوانی هم‌زمان، حداقل ۵۰۰ms نمایش تا نشانگر Material3 بین حالت‌ها گیر نکند.
    // V229 — منطق مشترک در PullRefreshGate (تست واحد V229_PullRefreshGateTest)
    private val refreshGate = PullRefreshGate(viewModelScope)
    fun refresh() {
        refreshGate.run({ r -> _state.update { it.copy(refreshing = r) } }) { load().join() }
    }

    fun load() = viewModelScope.launch {
        // V212 — کیف پول قبلی فوراً نشان داده می‌شود؛ موجودی تازه در پس‌زمینه جایگزین می‌شود.
        val cached = SessionCache.get<WalletSnapshot>(CACHE_WALLET)
        _state.update { it.copy(loading = cached == null, wallet = cached ?: it.wallet, error = null, message = null) }
        repository.wallet()
            .onSuccess { wallet -> SessionCache.put(CACHE_WALLET, wallet); _state.update { it.copy(loading = false, wallet = wallet) } }
            .onFailure { error -> _state.update { it.copy(loading = false, error = if (cached == null) safeBillingError(error) else null) } }
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
                if (payment.credited && payment.sandbox) {
                    val refreshed = repository.wallet().getOrNull()
                    _state.update { current ->
                        val fallback = current.wallet?.let { wallet ->
                            wallet.copy(
                                balanceToman = payment.balanceAfterToman ?: wallet.balanceToman
                            )
                        }
                        current.copy(
                            startingPayment = false,
                            payment = null,
                            wallet = refreshed ?: fallback,
                            message = "اعتبار آزمایشی با تأیید سرور به کیف پول افزوده شد."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            startingPayment = false,
                            payment = payment,
                            message = "درگاه بانکی آماده است."
                        )
                    }
                }
            }
            .onFailure { error -> _state.update { it.copy(startingPayment = false, error = safeBillingError(error)) } }
    }

    fun paymentOpened() {
        _state.update { it.copy(payment = null) }
    }
}

private const val CACHE_WALLET = "billing.wallet" // V212

private fun safeBillingError(error: Throwable): String = UserFacingError.of(error, "عملیات کیف پول ناموفق بود.") // V209 — بدون متن فنی
