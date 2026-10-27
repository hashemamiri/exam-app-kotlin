package ir.exam.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.delay

/**
 * V75.7 — نگه‌داریِ یک رشتهٔ حساس (نشستِ ورود) به‌صورت رمزنگاری‌شده.
 *
 * - کلید AES غیرقابل‌استخراج در Android Keystore ساخته می‌شود.
 * - در SharedPreferences فقط IV و ciphertextِ احرازشدهٔ AES/GCM نوشته می‌شود،
 *   نه خودِ نشست؛ بنابراین با روت‌بودن دستگاه یا دسترسی به فایل‌های برنامه،
 *   access token و refresh token قابل خواندن نیستند.
 * - کلید با حذف برنامه از بین می‌رود و نشستِ قبلی دیگر قابل بازیابی نیست.
 *
 * V335.47 — درسِ میدانی «بعد از هر آپدیت باید دوباره وارد شوم»: خواندنِ قبلی هر استثنایی را
 * «خرابی» حساب می‌کرد و **نشست را پاک** می‌کرد. اما AndroidKeyStore در اولین اجرا پس از
 * جایگزینیِ بسته (یا درست بعد از بوت) می‌تواند موقتاً در دسترس نباشد؛ همان استثنای گذرا
 * نشستِ سالم را نابود می‌کرد. حالا فقط خرابیِ ساختاری پاک می‌شود و خطای گذرا با تلاش
 * دوباره و بدون حذف داده رد می‌شود.
 */
class SecureSessionStorage(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun write(value: String) {
        if (value.isBlank()) {
            clear()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + SEPARATOR +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(KEY_VALUE, payload).commit()) { "ذخیرهٔ رمزنگاری‌شده انجام نشد." }
    }

    /** V335.47 — آخرین خطای خواندن (فقط نام خطا، هرگز محتوای نشست) برای تشخیص. */
    @Volatile
    var lastError: String? = null
        private set

    @Synchronized
    fun read(): String? = readInternal()

    /**
     * V335.47 — خواندن با تلاش دوباره. خطای گذرای Keystore (اولین اجرا بعد از آپدیت،
     * یا لحظهٔ باز شدن قفل صفحه) نباید نشست را از بین ببرد.
     */
    suspend fun readWithRetry(attempts: Int = 3): String? {
        var waitMs = 120L
        repeat(attempts.coerceAtLeast(1)) { index ->
            val value = readInternal()
            if (value != null) return value
            val error = lastError ?: return null          // نشستی ذخیره نشده است
            if (index == attempts - 1) {
                Log.w(TAG, "نشست خوانده نشد پس از تلاش دوباره: $error")
                return null
            }
            delay(waitMs)
            waitMs *= 3
        }
        return null
    }

    private fun readInternal(): String? {
        val payload = preferences.getString(KEY_VALUE, null) ?: run {
            lastError = null
            return null
        }
        val parts = payload.split(SEPARATOR, limit = 2)
        if (parts.size != 2) {                            // ساختار خراب: قطعاً غیرقابل بازیابی
            lastError = "corrupt-payload"
            clear()
            return null
        }
        val iv: ByteArray
        val encrypted: ByteArray
        try {
            iv = Base64.decode(parts[0], Base64.NO_WRAP)
            encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        } catch (error: IllegalArgumentException) {
            lastError = "bad-base64"
            clear()
            return null
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(AAD)
            lastError = null
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (error: Throwable) {
            // V335.47 — گذرا یا کشنده؟ پاک نمی‌کنیم؛ فقط ثبت می‌کنیم تا تلاش دوباره/اجرای بعدی شانس داشته باشد.
            lastError = "decrypt:" + (error.javaClass.simpleName ?: "unknown")
            Log.w(TAG, "رمزگشایی نشست ناموفق بود ($lastError) — داده دست‌نخورده ماند")
            null
        }
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_VALUE).commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "secure_session_v75"
        const val KEY_VALUE = "session"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "exam_app_secure_session_v75"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val SEPARATOR = "."
        const val TAG = "SessionDiag"
        val AAD = "secure_session_v75".toByteArray(StandardCharsets.UTF_8)
    }
}
