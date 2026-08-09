package ir.exam.app.core.network
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
/** پیش از ارسال پاسخ، وضعیت اینترنت بررسی می‌شود؛ تصمیم نهایی با Repository است. */
class NetworkMonitor(context:Context){private val cm=context.getSystemService(ConnectivityManager::class.java)
 fun isOnline():Boolean=cm.activeNetwork?.let{cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}==true
}
