package ir.exam.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * V75.7 — نگه‌داریِ یک رشتهٔ حساس (نشستِ ورود) به‌صورت رمزنگاری‌شده.
 *
 * - کلید AES غیرقابل‌استخراج در Android Keystore ساخته می‌شود.
 * - در SharedPreferences فقط IV و ciphertextِ احرازشدهٔ AES/GCM نوشته می‌شود،
 *   نه خودِ نشست؛ بنابراین با روت‌بودن دستگاه یا دسترسی به فایل‌های برنامه،
 *   access token و refresh token قابل خواندن نیستند.
 * - کلید با حذف برنامه از بین می‌رود و نشستِ قبلی دیگر قابل بازیابی نیست.
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

    @Synchronized
    fun read(): String? {
        val payload = preferences.getString(KEY_VALUE, null) ?: return null
        return runCatching {
            val parts = payload.split(SEPARATOR, limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(AAD)
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrElse {
            // payload خراب یا کلید از بین رفته: نشستِ نیمه‌کاره پاک می‌شود.
            clear()
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
        val AAD = "secure_session_v75".toByteArray(StandardCharsets.UTF_8)
    }
}
