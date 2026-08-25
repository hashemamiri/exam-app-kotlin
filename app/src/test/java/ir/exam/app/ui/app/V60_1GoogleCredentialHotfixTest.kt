package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V60.1 — گزارش دستگاه: «پس از انتخاب جیمیل اتفاقی نمی‌افتد».
 * دو ریشه:
 * ۱) completeGoogleRegistration نتیجهٔ refreshCurrentUser را در state
 *    نمی‌نشاند؛ حتی اگر ورود موفق می‌شد AuthGate همچنان SignInScreen را
 *    نشان می‌داد.
 * ۲) پلاگین compose-auth روی برخی دستگاه‌ها callback موفقیت را گم می‌کرد.
 * راه‌حل: مسیر رسمی مستندات Supabase — Credential Manager مستقیم →
 * GoogleIdTokenCredential → auth.signInWith(IDToken با nonce خام) → ثبت نقش
 * → user در state (ورود خودکار). پلاگین و وابستگی compose-auth حذف شد.
 */
class V60_1GoogleCredentialHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }
    private val authVm by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt") }
    private val provider by lazy { source("app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt") }
    private val gradle by lazy { source("app/build.gradle.kts") }

    @Test
    fun `google button uses the direct credential manager flow with a hashed nonce`() {
        assertTrue("CredentialManager.create(context)" in signIn)
        assertTrue("GetGoogleIdOption.Builder()" in signIn)
        assertTrue("setServerClientId(ir.exam.app.BuildConfig.GOOGLE_WEB_CLIENT_ID)" in signIn)
        // nonce خام به Supabase و hash آن به گوگل می‌رود (قرارداد رسمی).
        assertTrue("MessageDigest.getInstance(\"SHA-256\")" in signIn)
        assertTrue(".setNonce(hashedNonce)" in signIn)
        assertTrue("viewModel.signInWithGoogleIdToken(googleCredential.idToken, rawNonce, role)" in signIn)
        // لغو توسط کاربر خطا نیست
        assertTrue("GetCredentialCancellationException" in signIn)
    }

    @Test
    fun `id token sign in lands the user in auth state`() {
        assertTrue("signInWith(IDToken)" in authVm.replace(" ", "").let { if ("signInWith(IDToken)" in it) "signInWith(IDToken)" else authVm })
        assertTrue("nonce = rawNonce" in authVm)
        assertTrue("val user = repository.refreshCurrentUser().getOrThrow()" in authVm)
        // V60.2: مسیر مشترک acceptAuthenticatedUser (حساب تازه → صفحهٔ تکمیل).
        assertTrue("acceptAuthenticatedUser(user)" in authVm)
    }

    @Test
    fun `the compose auth plugin is fully removed`() {
        assertFalse("compose-auth" in gradle)
        assertFalse("ComposeAuth" in provider)
        assertFalse("rememberSignInWithGoogle" in signIn)
        assertTrue("androidx.credentials:credentials-play-services-auth" in gradle)
        assertTrue("googleid" in gradle)
    }
}
