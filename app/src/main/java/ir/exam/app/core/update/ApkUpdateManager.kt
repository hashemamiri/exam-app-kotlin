package ir.exam.app.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import ir.exam.app.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/**
 * اگر DownloadManager در این مدت هیچ بایتی جلو نرود، دانلود «متوقف» تلقی می‌شود و
 * با پیام روشن لغو می‌شود تا کاربر بی‌نهایت در حالت بی‌صدا منتظر نماند.
 */
private const val DOWNLOAD_STALL_TIMEOUT_MS = 120_000L

/** دلایل توقف دانلود که مربوط به نبود شبکه‌اند و باید به کاربر اعلام شوند. */
private val NETWORK_PAUSE_REASONS = setOf(
    DownloadManager.PAUSED_WAITING_FOR_NETWORK,
    DownloadManager.PAUSED_WAITING_FOR_WIFI,
    DownloadManager.PAUSED_QUEUED_FOR_WIFI
)

/** پیشرفت دانلود مستقل از UI. totalBytes در بعضی سرورها تا چند لحظه نامشخص است. */
data class ApkDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val waitingForNetwork: Boolean = false
)

/**
 * دانلود APK را به DownloadManager سیستم می‌سپارد و پیش از نصب، اندازه، هش،
 * package name، versionCode و در صورت وجود نسخه نصب‌شده، امضای آن را کنترل می‌کند.
 */
class ApkUpdateManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)

    suspend fun download(
        remote: RemoteVersion,
        onProgress: (ApkDownloadProgress) -> Unit
    ): Result<File> = runCatching {
        require(remote.apkUrl.toSafeHttpsUri() != null) {
            "نشانی دانلود APK امن یا معتبر نیست."
        }

        val downloadsRoot = requireNotNull(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ) { "حافظه برنامه برای دانلود بروزرسانی در دسترس نیست." }
        val updatesDirectory = File(downloadsRoot, "updates")
        check(updatesDirectory.exists() || updatesDirectory.mkdirs()) {
            "پوشه دانلود بروزرسانی ساخته نشد."
        }

        updatesDirectory.listFiles()
            ?.filter { it.extension.equals("apk", ignoreCase = true) }
            ?.forEach(File::delete)

        val safeVersionName = remote.name.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val fileName = "exam-app-${remote.code}-$safeVersionName.apk"
        val destination = File(updatesDirectory, fileName)

        val request = DownloadManager.Request(remote.apkUrl.toSafeHttpsUri()!!)
            .setTitle("بروزرسانی آزمون آنلاین ${remote.name}")
            .setDescription("در حال دریافت نسخه جدید")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                "updates/$fileName"
            )

        val downloadId = downloadManager.enqueue(request)
        try {
            awaitDownload(downloadId, destination, onProgress)
            validateDownloadedApk(destination, remote)
            destination
        } catch (error: Throwable) {
            downloadManager.remove(downloadId)
            destination.delete()
            throw error
        }
    }

    fun canRequestPackageInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            appContext.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${appContext.packageName}")
    )

    fun launchInstaller(apkPath: String): Result<Unit> = runCatching {
        check(canRequestPackageInstalls()) {
            "اجازه نصب برنامه از این منبع هنوز فعال نشده است."
        }

        val apk = File(apkPath)
        check(apk.isFile && apk.length() > 0L) {
            "فایل APK دانلودشده پیدا نشد؛ دوباره آن را دانلود کنید."
        }

        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    private suspend fun awaitDownload(
        downloadId: Long,
        destination: File,
        onProgress: (ApkDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        var lastBytes = 0L
        var lastProgressAtMs = System.currentTimeMillis()
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                check(cursor != null && cursor.moveToFirst()) {
                    "دانلود در DownloadManager پیدا نشد."
                }

                val status = cursor.longValue(DownloadManager.COLUMN_STATUS).toInt()
                val downloaded = cursor.longValue(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    .coerceAtLeast(0L)
                val total = cursor.longValue(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    .takeIf { it > 0L }
                val waitingForNetwork = status == DownloadManager.STATUS_PAUSED &&
                    cursor.longValue(DownloadManager.COLUMN_REASON).toInt() in NETWORK_PAUSE_REASONS
                onProgress(ApkDownloadProgress(downloaded, total, waitingForNetwork))

                if (downloaded != lastBytes) {
                    lastBytes = downloaded
                    lastProgressAtMs = System.currentTimeMillis()
                } else if (
                    status != DownloadManager.STATUS_SUCCESSFUL &&
                    System.currentTimeMillis() - lastProgressAtMs >= DOWNLOAD_STALL_TIMEOUT_MS
                ) {
                    // DownloadManager بعضی مواقع بی‌صدا در PAUSED/در انتظار شبکه می‌ماند؛
                    // بدون این کنترل کاربر هیچ پیامی نمی‌دید.
                    downloadManager.remove(downloadId)
                    error("دانلود بروزرسانی متوقف شده است؛ اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.")
                }

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        check(destination.isFile && destination.length() > 0L) {
                            "DownloadManager پایان دانلود را اعلام کرد، اما فایل APK موجود نیست."
                        }
                        return@withContext
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.longValue(DownloadManager.COLUMN_REASON).toInt()
                        error(downloadFailureMessage(reason))
                    }
                }
            }
            delay(750)
        }
    }

    private fun validateDownloadedApk(file: File, remote: RemoteVersion) {
        remote.sizeBytes?.let { expected ->
            check(file.length() == expected) {
                "اندازه فایل دانلودشده با نسخه ثبت‌شده در سرور یکسان نیست."
            }
        }

        remote.sha256?.let { expected ->
            val actual = file.sha256()
            check(actual.equals(expected, ignoreCase = true)) {
                "اعتبار SHA-256 فایل APK تأیید نشد؛ نصب برای امنیت متوقف شد."
            }
        }

        val packageInfo = appContext.packageManager.archiveInfo(file)
            ?: error("فایل دریافت‌شده یک APK معتبر نیست.")
        val archivePackage = packageInfo.packageName.orEmpty()
        val allowedPackages = if (BuildConfig.DEBUG) {
            setOf(appContext.packageName, "ir.exam.app")
        } else {
            setOf(appContext.packageName)
        }
        check(archivePackage in allowedPackages) {
            "نام بسته APK با آزمون آنلاین مطابقت ندارد؛ نصب متوقف شد."
        }

        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val installedPackage = appContext.packageManager.installedInfoOrNull(archivePackage)
        val installedVersionCode = installedPackage?.let(PackageInfoCompat::getLongVersionCode)
            ?: if (archivePackage == appContext.packageName) BuildConfig.VERSION_CODE.toLong() else 0L
        check(archiveVersionCode > installedVersionCode) {
            "versionCode فایل APK باید از نسخه نصب‌شده بیشتر باشد."
        }
        check(archiveVersionCode == remote.code.toLong()) {
            "versionCode فایل APK با اطلاعات نسخه سرور مطابقت ندارد."
        }

        if (installedPackage != null) {
            val installedSignatures = installedPackage.signingDigests()
            val archiveSignatures = packageInfo.signingDigests()
            check(
                installedSignatures.isNotEmpty() &&
                    archiveSignatures.isNotEmpty() &&
                    installedSignatures.intersect(archiveSignatures).isNotEmpty()
            ) {
                "امضای APK با نسخه نصب‌شده یکسان نیست؛ بروزرسانی متوقف شد."
            }
        }
    }
}

private fun String.toSafeHttpsUri(): Uri? {
    val uri = runCatching { Uri.parse(trim()) }.getOrNull() ?: return null
    return uri.takeIf {
        it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank()
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.archiveInfo(file: File): PackageInfo? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getPackageArchiveInfo(
        file.absolutePath,
        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> getPackageArchiveInfo(
        file.absolutePath,
        PackageManager.GET_SIGNING_CERTIFICATES
    )
    else -> getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
}

@Suppress("DEPRECATION")
private fun PackageManager.installedInfoOrNull(packageName: String): PackageInfo? = runCatching {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> getPackageInfo(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        else -> getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    }
}.getOrNull()

@Suppress("DEPRECATION")
private fun PackageInfo.signingDigests(): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = signingInfo ?: return emptySet()
        if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
    } else {
        this.signatures.orEmpty()
    }
    return signatures.map { signature -> signature.toByteArray().sha256() }.toSet()
}

private fun File.sha256(): String = FileInputStream(this).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count > 0) digest.update(buffer, 0, count)
    }
    digest.digest().toHex()
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(this).toHex()

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

private fun android.database.Cursor.longValue(column: String): Long =
    getLong(getColumnIndexOrThrow(column))

private fun downloadFailureMessage(reason: Int): String = when (reason) {
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "فضای ذخیره‌سازی برای دانلود APK کافی نیست."
    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "حافظه مقصد برای دانلود در دسترس نیست."
    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "فایل بروزرسانی از قبل وجود دارد؛ دوباره تلاش کنید."
    DownloadManager.ERROR_CANNOT_RESUME -> "ادامه دانلود ممکن نشد؛ دوباره تلاش کنید."
    DownloadManager.ERROR_HTTP_DATA_ERROR,
    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "سرور دانلود APK پاسخ معتبر نداد."
    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "نشانی دانلود APK تغییر مسیرهای بیش از حد دارد."
    else -> "دانلود APK ناموفق بود (کد $reason)."
}
