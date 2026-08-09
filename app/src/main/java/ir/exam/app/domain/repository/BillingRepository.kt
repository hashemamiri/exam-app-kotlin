package ir.exam.app.domain.repository
import ir.exam.app.domain.model.Subscription
import ir.exam.app.domain.model.Wallet
import kotlinx.coroutines.flow.Flow
interface BillingRepository { fun observeWallet():Flow<Wallet?>; fun observeSubscription():Flow<Subscription?>; suspend fun requestTopUp(amountRials:Long):Result<String>; suspend fun canCreateExam():Result<Boolean> }
