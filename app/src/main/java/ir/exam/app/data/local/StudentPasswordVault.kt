package ir.exam.app.data.local

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
 * گاوصندوق محلی رمز دانش‌آموزان بر پایهٔ شناسهٔ یکتای دانش‌آموز برای همان نصب برنامه.
 *
 * - کلید AES غیرقابل‌استخراج داخل Android Keystore ساخته می‌شود.
 * - SharedPreferences فقط IV و ciphertext احرازشدهٔ AES/GCM را نگه می‌دارد.
 * - AndroidManifest پشتیبان‌گیری برنامه را غیرفعال کرده است؛ داده به دستگاه دیگر
 *   منتقل نمی‌شود و با حذف data برنامه از بین می‌رود.
 * - این کلاس هیچ ارتباطی با hash سرور یا plain_password دیتابیس ندارد.
 */
class StudentPasswordVault(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun write(studentId: String, password: String) {
        val entry = entryKey(studentId) ?: return
        if (password.isBlank()) {
            preferences.edit().remove(entry).commit()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(entry.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(password.toByteArray(StandardCharsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + SEPARATOR +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(entry, payload).commit()) { "ذخیره رمزنگاری‌شده انجام نشد." }
    }

    @Synchronized
    fun read(studentId: String?): String? {
        val entry = entryKey(studentId) ?: return null
        val payload = preferences.getString(entry, null) ?: return null
        return runCatching {
            val parts = payload.split(SEPARATOR, limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(entry.toByteArray(StandardCharsets.UTF_8))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrElse {
            // payload خراب یا کلید invalidate شده هرگز به‌عنوان رمز نمایش داده نشود.
            preferences.edit().remove(entry).commit()
            null
        }
    }

    @Synchronized
    fun remove(studentId: String?) {
        entryKey(studentId)?.let { preferences.edit().remove(it).commit() }
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

    private fun entryKey(studentId: String?): String? = studentId
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let { ENTRY_PREFIX + it }

    private companion object {
        const val PREFERENCES_NAME = "student_password_vault_v34"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "exam_app_student_password_v34"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val ENTRY_PREFIX = "credential_"
        const val SEPARATOR = "."
    }
}
