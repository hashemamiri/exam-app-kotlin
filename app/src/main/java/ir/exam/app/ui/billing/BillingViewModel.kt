package ir.exam.app.ui.billing
import androidx.lifecycle.ViewModel
import ir.exam.app.domain.model.Subscription
import ir.exam.app.domain.model.Wallet
import kotlinx.coroutines.flow.*
data class BillingState(val wallet:Wallet?=null,val subscription:Subscription?=null,val paymentUrl:String?=null,val loading:Boolean=false,val error:String?=null)
/** نمایش کیف پول و اشتراک؛ پرداخت واقعی باید از Edge Function امن و مرورگر سفارشی شروع شود. */
class BillingViewModel:ViewModel(){private val _state=MutableStateFlow(BillingState());val state=_state.asStateFlow();fun setAccount(wallet:Wallet?,sub:Subscription?){_state.update{it.copy(wallet=wallet,subscription=sub)}};fun showPayment(url:String){_state.update{it.copy(paymentUrl=url)}}}
