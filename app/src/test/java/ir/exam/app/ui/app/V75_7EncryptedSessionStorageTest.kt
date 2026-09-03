package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.7 — نشستِ ورود دیگر در SharedPreferencesِ ساده نیست (بند ۳.۵ گزارش امنیتی):
 * کلید در Android Keystore است و خودِ نشست به‌صورت AES/GCM رمزنگاری می‌شود.
 */
class V75_7EncryptedSessionStorageTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(relative: String): String = File(root(), relative).readText()

    private val storage by lazy { source("app/src/main/java/ir/exam/app/core/security/SecureSessionStorage.kt") }
    private val manager by lazy { source("app/src/main/java/ir/exam/app/data/remote/EncryptedSessionManager.kt") }
    private val provider by lazy { source("app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt") }
    private val application by lazy { source("app/src/main/java/ir/exam/app/ExamApplication.kt") }
    private val manifest by lazy { source("app/src/main/AndroidManifest.xml") }

    @Test
    fun `session payload is encrypted with a keystore backed key`() {
        assertTrue("AndroidKeyStore" in storage)
        assertTrue("AES/GCM/NoPadding" in storage)
        assertTrue("GCMParameterSpec" in storage)
        assertTrue("setRandomizedEncryptionRequired(true)" in storage)
    }

    @Test
    fun `custom session manager replaces the plain preferences storage`() {
        assertTrue(": SessionManager" in manager)
        assertTrue("override suspend fun saveSession(session: UserSession)" in manager)
        assertTrue("override suspend fun loadSession(): UserSession?" in manager)
        assertTrue("override suspend fun deleteSession()" in manager)
    }

    @Test
    fun `supabase auth is configured with the encrypted session manager`() {
        assertTrue("sessionManager = EncryptedSessionManager(context)" in provider)
        assertTrue("fun attach(context: android.content.Context)" in provider)
    }

    @Test
    fun `application class provides the context before anything else`() {
        assertTrue("class ExamApplication : Application()" in application)
        assertTrue("SupabaseProvider.attach(this)" in application)
        assertTrue(manifest.contains("android:name=\".ExamApplication\""))
    }
}
