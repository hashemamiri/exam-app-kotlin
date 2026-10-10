package ir.exam.app.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ir.exam.app.MainActivity
import ir.exam.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * V232 — دریافت اعلان‌های FCM (نمرهٔ جدید، باز شدن آزمون، درخواست مدیر). متن از سرور (push_outbox) می‌آید؛
 * هیچ دادهٔ حساسی در اعلان نیست. با ضربه، اپ باز می‌شود و extra «push_page» صفحهٔ مقصد را می‌گوید.
 */
class PushMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { PushRegistrar.onNewToken(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!PushRegistrar.isEnabled(applicationContext)) return
        val title = message.notification?.title ?: message.data["title"] ?: "آزمون آنلاین"
        val body = message.notification?.body ?: message.data["body"] ?: return
        ensureChannel(applicationContext)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("push_page", message.data["page"] ?: "")
            putExtra("push_exam_id", message.data["exam_id"] ?: "")
            putExtra("push_code", message.data["code"] ?: "") // V232.9 — کد آزمون برای «آزمون باز شد»
        }
        val id = (message.data["outbox_id"]?.toIntOrNull() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        val pending = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        // V232.5 — lint MissingPermission: در اندروید ۱۳+ بدون مجوز POST_NOTIFICATIONS اعلان نشان داده نمی‌شود
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try { NotificationManagerCompat.from(this).notify(id, notification) } catch (_: SecurityException) { }
    }

    companion object {
        const val CHANNEL_ID = "exam_events"
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "رویدادهای آزمون", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "نمرهٔ جدید، باز شدن آزمون و درخواست‌های مدیر"
                }
            )
        }
    }
}
