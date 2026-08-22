# رفع خطای کامپایل دانلود بروزرسانی — V45.2.1

**آخرین بهروزرسانی:** ۲۰۲۶-۰۸-۲۲

## گزارش واقعی CI

```text
e: .../ir/exam/app/core/update/ApkUpdateManager.kt:33:21 Unresolved reference 'PAUSED_WAITING_FOR_WIFI'
> Task :app:compileDebugKotlin FAILED
```

## علت

در پچ V45.1 برای تشخیص «در انتظار شبکه» در DownloadManager، ثابت
`PAUSED_WAITING_FOR_WIFI` در مجموعه `NETWORK_PAUSE_REASONS` استفاده شده بود،
اما این ثابت در کلاس `android.app.DownloadManager` **وجود ندارد**. دو ثابت
دیگر همان مجموعه (`PAUSED_WAITING_FOR_NETWORK` و `PAUSED_QUEUED_FOR_WIFI`)
معتبرند و کامپایلر فقط از همان یک ثابت شکایت کرد.

## اصلاح

- `app/src/main/java/ir/exam/app/core/update/ApkUpdateManager.kt`:
  حذف `PAUSED_WAITING_FOR_WIFI` از `NETWORK_PAUSE_REASONS` — رفتار تشخیص
  «در انتظار شبکه» با دو ثابت معتبر حفظ میشود.
- تست `V45_1UpdateDownloadFixTest`: assert معتبر
  `PAUSED_QUEUED_FOR_WIFI` + assert منفی که `PAUSED_WAITING_FOR_WIFI` دیگر
  در سورس نیست.
- مستندات بهروز شدند.

## تست

```text
FINAL_NATIVE_VERIFY                      → PASS
V45_1 / V45_2 regression tests           → بهروزرسانی شد
git diff --check                         → PASS
testDebugUnitTest / lintDebug            → باید در WSL/GitHub Actions اجرا شود
```

## عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیشنیاز: V45.2 (که شامل V45.1 است)
```
