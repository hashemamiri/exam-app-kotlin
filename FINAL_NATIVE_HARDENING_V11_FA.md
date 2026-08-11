# V11 نهایی — امنیت، حذف رمز plaintext، Storage و قطع WebView

## نتیجه نهایی مهاجرت

این Patch آخرین مرحله برنامه‌ریزی‌شدهٔ مهاجرت است. پس از اعمال و تست دستگاه:

```text
Runtime اصلی: Kotlin + Jetpack Compose
Backend: Supabase RPC/RLS + Edge Functions
Offline: Room + WorkManager
چاپ: Android PdfDocument/PrintManager
WebView runtime: حذف‌شده از سورس Native
```

## تغییرات امنیتی اصلی

### حذف قطعی plain_password
- Edge Function جدید `manage-student` هیچ رمز عبوری در `profiles` ذخیره نمی‌کند.
- رمز ساخت/ریست فقط همان بار در حافظه UI نمایش داده می‌شود.
- ستون `profiles.plain_password` ابتدا null و سپس DROP می‌شود.
- تمام RPCهای قدیمی که ستون را می‌خواندند حذف و نسخه بدون رمز `my_students` و `class_roster` ساخته می‌شود.
- رمز واقعی Auth کاربران تغییر یا حذف نمی‌شود؛ فقط کپی plaintext ناامن حذف می‌شود.
- UI ویرایش، تعیین رمز تازه و حذف کامل حساب دانش‌آموز اضافه می‌شود.

### RLS و Grants
- RLS روی همه جدول‌های public فعال می‌شود.
- تمام مجوزهای INSERT/UPDATE/DELETE مستقیم `anon` و `authenticated` لغو می‌شود.
- APK فقط SELECTهای ضروری را زیر RLS دارد؛ همه mutationها از RPC مالک‌محور عبور می‌کنند.
- همه Functionها ابتدا از PUBLIC/anon/authenticated revoke و سپس فقط allowlist واقعی Native grant می‌شوند.
- تابع قدیمی `submit_answer` مستقیم از APK قابل اجرا نیست؛ wrapper idempotent V10 مجاز است.
- تمام SECURITY DEFINERها `search_path=public,pg_temp` می‌گیرند.
- Policyهای قدیمی جدول‌های حساس حذف و policyهای محدود جایگزین می‌شوند.

### Storage
- تمام policyهای قدیمی `storage.objects` حذف می‌شوند.
- خواندن عمومی فقط برای `exam-images` و `app-updates` باقی می‌ماند.
- آپلود فقط برای کاربر authenticated و فقط در مسیر `{folder}/{auth.uid()}/...` مجاز است.
- Edge Function جدید `storage-maintenance` مرجع‌های واقعی profile/exam/bank/answer/trash/draft را جمع می‌کند.
- ابتدا dry-run؛ حذف واقعی فقط با دو Secret و user id مجاز انجام می‌شود.
- فایل جدیدتر از grace period هرگز orphan محسوب نمی‌شود.

### Android و CI/CD
- `allowBackup=false` برای جلوگیری از خروج Room و پاسخ‌های صف‌شده از sandbox دستگاه.
- `FLAG_SECURE` هنگام آزمون برای جلوگیری از screenshot/casting.
- اسکن Secret و keystore پیش از Build در GitHub Actions.
- Deno check برای هر سه Edge Function.
- تطبیق SHA-256 گواهی APK با keystore همان Build.
- حذف فایل‌های موقت keystore در step با `if: always()`.
- retention خودکار: APK فعال + چهار APK جدید نگهداری و قدیمی‌ترها حذف می‌شوند.
- Workflow دیگر برای health-check به خواندن عمومی `profiles` وابسته نیست.

## ترتیب اجرای اجباری

به دلیل حذف ستون `plain_password`، ترتیب زیر تغییر نکند:

1. Patch را Apply و Stage کنید؛ هنوز Push نکنید.
2. Edge Functionهای `manage-student` و `storage-maintenance` را deploy کنید.
3. SQL نهایی را در SQL Editor اجرا کنید.
4. `native_security_status_v1()` را بررسی کنید.
5. سپس Commit و Push کنید.

اگر SQL قبل از deploy نسخه جدید manage-student اجرا شود، Edge Function قدیمی دیگر با schema سازگار نخواهد بود.

## SQL نهایی

```text
SQL_NATIVE_FINAL_HARDENING_V11.sql
```

بررسی:

```sql
select public.native_security_status_v1();
```

خروجی باید نشان دهد:

```text
plain_password_removed                    true
public_tables_without_rls                 0
anon_mutating_table_grants                0
authenticated_mutating_table_grants       0
security_definer_public_execute           0
student_admin_audit_ready                 true
maintenance_audit_ready                   true
```

## فعال‌سازی اختیاری حذف واقعی Storage

Dry-run بدون Secret حذف قابل استفاده است. برای حذف واقعی، UUID حساب مدیریتی خودتان را از Dashboard/Auth Users بردارید و فقط در Edge Secrets ثبت کنید:

```text
MAINTENANCE_DELETE_ENABLED=true
MAINTENANCE_ALLOWED_USER_ID=<UUID حساب مجاز>
```

UUID یا Access Token را در Chat/Git/APK قرار ندهید. ابتدا از صفحه «پروفایل و تنظیمات → داده‌ها» گزینه «بررسی بدون حذف» را اجرا کنید.

## تست‌های نهایی

```text
Kotlin compile                                 PASS
JVM tests                                      39/39 PASS
Deno check (3 Edge Functions)                  PASS
PostgreSQL migration + second run              PASS
Teacher/student RLS isolation                  PASS
Direct table mutation rejection                PASS
RPC owner/cross-owner regression                PASS
plain_password column/function removal         PASS
Function allowlist regression                  PASS
Storage exact policy regression                PASS
Native security status                         PASS
FINAL_NATIVE_VERIFY                            PASS
assembleDebug                                  BUILD SUCCESSFUL
lintDebug                                      BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2                        Verified
Secret/private-key scan                        CLEAN
```

## تست دستگاه پس از Build

- ورود معلم و دانش‌آموز
- ساخت/ویرایش/ریست رمز/حذف دانش‌آموز
- ساخت، ویرایش، باز/بسته‌کردن و حذف آزمون
- ورود و ارسال آنلاین/آفلاین دانش‌آموز
- تصحیح دانش‌آموزمحور و سؤال‌محور
- گزارش، تحلیل، چاپ آزمون و PDF
- Export/Import و Backup/Restore
- تقویم و پیام هدف‌دار
- کیف پول و sandbox پرداخت
- بررسی dry-run Storage
- بروزرسانی درون‌برنامه‌ای Release امضاشده

سورس WebView فقط به‌عنوان آرشیو تاریخی خارج از runtime Native نگهداری می‌شود و دیگر مرجع اجرای برنامه نیست.
