package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V298 — گردش کار نسخهٔ آزمایشی: apply → staging (اپ beta + سایت staging.onlineexam.pages.dev) → promote.py → main. */
class V298_StagingWorkflowTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun betaBuildIsSeparatePackageWithoutAutoUpdate() {
        val g = src("app/build.gradle.kts")
        assertTrue("val isBetaBuild = localProperties.getProperty(\"APP_BETA\", \"0\").trim() == \"1\"" in g)
        assertTrue("if (isBetaBuild) { applicationIdSuffix = \".beta\"; versionNameSuffix = \"-beta\" }" in g)
        assertTrue("resValue(\"string\", \"app_label\", if (isBetaBuild) \"آزمون (آزمایشی)\" else \"آزمون آنلاین\")" in g)
        assertTrue("buildConfigField(\"Boolean\", \"IS_BETA\", \"\$isBetaBuild\")" in g)
        assertTrue("(if (isBetaBuild) \"ir.exam.app.beta\" else \"ir.exam.app\") in googleServicesPackages" in g)
        assertTrue("android:label=\"@string/app_label\"" in src("app/src/main/AndroidManifest.xml"))
        val vm = src("app/src/main/java/ir/exam/app/ui/update/UpdateViewModel.kt")
        assertTrue("if (BuildConfig.IS_BETA) {" in vm && "به‌روزرسانی خودکار در آن غیرفعال است" in vm)
        // V298.2 — ورود با گوگل در beta پیام روشن می‌دهد (بستهٔ ir.exam.app.beta در Google Cloud ثبت نیست)
        val si = src("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        assertTrue("if (ir.exam.app.BuildConfig.IS_BETA) {" in si && "ورود با گوگل در نسخهٔ آزمایشی فعال نیست" in si)
        assertTrue("https://staging.onlineexam-1q1.pages.dev" in src(".github/workflows/android.yml"))
    }

    @Test
    fun workflowsAndPromoteScript() {
        val a = src(".github/workflows/android.yml")
        assertTrue("branches: [main, staging]" in a)
        assertTrue("if [ \"\$GITHUB_REF_NAME\" = \"staging\" ]; then printf 'APP_BETA=1\\n' >> local.properties" in a)
        assertTrue("if: github.ref_name == 'staging'" in a && "if: github.ref_name != 'staging'" in a)
        assertTrue("beta/exam-app-beta-\$APP_VERSION_NAME.apk" in a && "publish_native_app_release_v1" in a)
        assertTrue("[ \"\$EXISTING\" != \"[]\" ] && [ \"\$GITHUB_REF_NAME\" != \"staging\" ]" in a)
        val s = src(".github/workflows/site.yml")
        assertTrue("branches: [main, staging]" in s && "--branch=\${{ github.ref_name }}" in s && "--branch=main" !in s)
        assertTrue("group: site-deploy-\${{ github.ref_name }}" in s)
        val t = src("docs/tools/installer_template.py")
        // V335.39 — قالب نصاب push شاخهٔ آزمایشی را از push_checked عبور می‌دهد (مدیریت خطای scope=workflow)
        assertTrue("push_checked(\"HEAD:refs/heads/staging\")" in t)
        val p = src("scripts/promote.py")
        assertTrue("\"--status\"" in p && "\"--cancel\"" in p && "stg_sha + \":refs/heads/main\"" in p)
        assertTrue("merge-base\", \"--is-ancestor\"" in p && "head != stg_sha" in p)
    }
}
