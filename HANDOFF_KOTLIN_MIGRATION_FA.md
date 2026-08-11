# هندآف جامع مهاجرت سامانه آزمون از WebView به Native Kotlin

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۱۱ — پچ جامع V9 تقویم، پروفایل و کیف پول
**زبان همکاری:** فارسی
**کاربر:** غیر‌برنامه‌نویس؛ دستورها باید ساده، مرحله‌ای و قابل کپی در WSL باشند.

---

## ۱) هدف پروژه

هدف، ساخت نسخهٔ Native Android با Kotlin و Jetpack Compose برای جایگزینی تدریجی برنامهٔ فعلی WebView است.

نسخهٔ قبلی هنوز مرجع کامل قابلیت‌ها است و نباید حذف شود تا زمانی که نسخه Kotlin همهٔ مسیرهای حیاتی را با موفقیت جایگزین کند.

```text
نسخه قبلی:
JavaScript + HTML + CSS داخل WebView

نسخه جدید:
Kotlin + Jetpack Compose + Supabase + Room + GitHub Actions
```

---

## ۲) مسیرهای مهم

### پروژه قدیمی

```text
Windows:
C:\Users\Hashem\Downloads\exam-app

WSL:
/mnt/c/Users/Hashem/Downloads/exam-app

Workspace:
/home/user/exam-app
```

### پروژه Native Kotlin

```text
Windows:
C:\Users\Hashem\Downloads\exam-app-kotlin

WSL:
/mnt/c/Users/Hashem/Downloads/exam-app-kotlin

Workspace:
/home/user/exam-app-kotlin
```

### repositoryهای GitHub

```text
نسخه قدیمی:
exam-app

نسخه Kotlin:
https://github.com/hashemamiri/exam-app-kotlin
```

---

## ۳) اطلاعات Supabase واقعی

### پروژه Supabase اصلی

```text
SUPABASE_URL:
https://eazwuyrymsvdkwckdpco.supabase.co
```

### هشدار مهم

یک پروژهٔ دیگر با شناسهٔ زیر دیده شد و **نباید** برای Kotlin استفاده شود، زیرا جدول‌های پروژه اصلی را ندارد:

```text
niuadepncroqoebrxpqk.supabase.co
```

### Secretهای GitHub لازم در repository Kotlin

```text
SUPABASE_URL
SUPABASE_ANON_KEY
SUPABASE_RELEASE_KEY
ANDROID_KEYSTORE_BASE64
KS_PASS
KEY_ALIAS
KEY_PASS
```

### قانون امنیتی قطعی

هرگز این موارد را در چت، Git، APK، `local.properties` قابل commit یا paste.rs قرار ندهید:

```text
SUPABASE_SERVICE_KEY
service_role
release.keystore
رمز keystore
رمز alias
```

### bucket تصویر

```text
exam-images
```

مسیرهای استاندارد پیشنهادی:

```text
avatars/{userId}/{uuid}.webp
questions/{teacherId}/{examId}/{uuid}.webp
option_images/{teacherId}/{examId}/{uuid}.webp
answers/{studentId}/{examId}/{questionId}/{uuid}.webp
```

---

## ۴) وضعیت GitHub Actions و Gradle

### Gradle Wrapper

در پروژه Kotlin با موفقیت ساخته و به Git ارسال شده است:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

### تنظیمات موفق build

- Java 17
- Gradle 8.11.1
- Kotlin 2.0.21
- Compose Compiler Plugin
- AndroidX فعال
- KSP و Room فعال
- Postgrest و Storage در SupabaseClient نصب شده‌اند
- build در GitHub Actions موفق شده است.

### خطاهای رفع‌شده تا اینجا

```text
Gradle Wrapper executable نبود             → chmod +x و git update-index
Compose Compiler Kotlin 2 فعال نبود       → kotlin plugin compose
AndroidX فعال نبود                         → gradle.properties
وابستگی Ktor اشتباه بود                   → io.ktor:ktor-client-okhttp
JVM 8/17 ناسازگار بود                     → JVM Toolchain 17
syntax چند فایل Kotlin                     → اصلاح UpdateUseCase / Builder / ImageEditor
OTP enum نادرست بود                        → OtpType.Email.EMAIL
Postgrest نصب نشده بود                     → install(Postgrest)
Storage نصب نشده بود                       → install(Storage)
Secretها وارد BuildConfig نمی‌شدند         → خواندن local.properties
```

### workflow فعلی

workflow فایل زیر است:

```text
.github/workflows/android.yml
```

کارهای workflow:

```text
دریافت سورس
→ Java 17
→ Gradle
→ تولید خودکار versionCode از ثانیه‌های گذشته از 2020-01-01 UTC
→ تولید خودکار versionName از تاریخ و ساعت UTC
→ ساخت local.properties از Secretهای GitHub و نسخه خودکار
→ بررسی HTTP جدول profiles
→ testDebugUnitTest
→ آماده‌سازی keystore Release
→ assembleRelease
→ تغییر نام APK با versionName و versionCode
→ تولید update-metadata.txt شامل SHA-256 و اندازه
→ Artifact شامل APK و metadata
→ آپلود خودکار APK در bucket app-updates
→ بررسی URL عمومی دانلود
→ فعال‌سازی تراکنشی نسخه با RPC publish_app_update
```

---

## ۵) package و APKها

### APK آزمایشی

```text
applicationId:
ir.exam.app.native
```

برای نصب کنار نسخه قدیمی WebView استفاده می‌شود.

### APK Release نهایی

```text
applicationId:
ir.exam.app
```

برای جایگزینی نسخه قدیمی باید:

```text
با همان release.keystore اصلی امضا شود
و versionCode آن از نسخه نصب‌شده بیشتر باشد
```

### keystore اصلی

```text
C:\Users\Hashem\Downloads\exam-app\android-app\release.keystore
```

alias شناخته‌شده:

```text
examkey
```

فایل Base64 برای Secret GitHub ساخته شده است:

```text
C:\Users\Hashem\Downloads\EXAM_APP_KEYSTORE_BASE64.txt
```

هش فایل محلی Base64 با keystore اصلی یکسان تأیید شد.

---

## ۶) وضعیت فعلی ورود و OTP

### موارد انجام‌شده

```text
ورود ایمیل/رمز در Kotlin
ارسال درخواست OTP
تأیید OTP با OtpType.Email.EMAIL
خواندن profiles پس از ورود
تشخیص role: teacher / student
نمایش خطاهای Auth بدون نمایش Header و Token
ذخیره خودکار session توسط Supabase Auth
انتظار صریح برای auth.awaitInitialization در شروع سرد
بازیابی خودکار کاربر پس از بستن/بازکردن و بروزرسانی درجا
cache محلی فقط برای نمای پروفایل و role، بدون token
fallback پروفایل cacheشده در قطع موقت اینترنت با تطبیق userId
صفحه loading برای جلوگیری از نمایش لحظه‌ای فرم ورود
صفحه retry در خطای موقت بازیابی به‌جای خروج کاذب از حساب
```

### وضعیت فعلی مشکل OTP

- اتصال Supabase و Postgrest اکنون درست شده است.
- ثبت‌نام عمومی در Kotlin با `createUser = true` فعال شده است.
- برای ساخت خودکار پروفایل کاربر جدید، SQL trigger لازم است.
- Supabase Hosted معمولاً OTP شش‌رقمی می‌فرستد.
- Kotlin در یک مرحله به ۸ رقم محدود شد؛ سپس برای سازگاری پیشنهاد شد ۶ تا ۸ رقم بپذیرد.

### قانون فنی OTP

```text
OTP استاندارد Supabase Hosted: معمولاً 6 رقم
OTP دقیقاً 8 رقمی: نیازمند Edge Function و جدول OTP اختصاصی است
```

### قالب ایمیل Supabase

مسیر:

```text
Authentication
→ Emails
→ Templates
→ Magic link or OTP
```

متغیر ضروری:

```text
{{ .Token }}
```

استفادهٔ صرف از مورد زیر مناسب اپ Kotlin نیست:

```text
{{ .ConfirmationURL }}
```

---

## ۷) SQLهای موردنیاز

### SQL سازگاری Kotlin

فایل ساخته‌شده:

```text
/home/user/SQL_KOTLIN_NATIVE_COMPAT.sql
```

این فایل شامل:

```text
trigger ساخت خودکار profiles
RLS profiles
app_version و check_app_update
bucket exam-images و policyها
answer_drafts
indexهای exams / answers / sessions
```

### SQLهای بروزرسانی برنامه

```text
supabase/migrations/20260811_app_updates.sql
وضعیت: اجراشده توسط کاربر

supabase/migrations/20260811_app_update_auto_publish.sql
وضعیت: اجراشده پیش از اولین انتشار خودکار V4

supabase/migrations/20260811_app_update_publish_409_hotfix.sql
وضعیت: V4.1 اجراشده؛ delete/insert فعال ولی نتیجه واقعی SQLSTATE 21000 بود

supabase/migrations/20260811_app_update_publish_v42_hotfix.sql
وضعیت: V4.2 اجراشده و فراخوانی مستقیم SQL آن موفق است

supabase/migrations/20260811_app_update_publish_v43_new_rpc.sql
وضعیت: V4.3 اجراشده؛ endpoint جدید فعال است

supabase/migrations/20260811_app_update_publish_v44_safe_delete.sql
وضعیت: V4.4؛ باید یک‌بار پیش از Push بعدی اجرا شود
```

V4.4 بر اساس message واقعی `DELETE requires a WHERE clause` همه DELETEهای تابع انتشار را با شرط `where version_code is not null` سازگار می‌کند. هیچ Secretی داخل SQLها نیست.

### نکتهٔ مهم SQL

این SQL باید فقط روی **Supabase اصلی** اجرا شود:

```text
https://eazwuyrymsvdkwckdpco.supabase.co
```

قبل از اجرا باید جدول‌های اصلی موجود باشند:

```text
profiles
exams
answers
exam_sessions
answer_drafts
question_bank
feedback_bank
classes
wallets
plans
orders
```

اگر query زیر هیچ جدول عمومی برنگرداند، پروژه Supabase اشتباه است:

```sql
select table_schema, table_name
from information_schema.tables
where table_type = 'BASE TABLE'
  and table_schema = 'public'
order by table_name;
```

### SQL trigger ثبت‌نام عمومی

```sql
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
begin
  v_name := coalesce(
    new.raw_user_meta_data ->> 'full_name',
    new.raw_user_meta_data ->> 'display_name',
    split_part(coalesce(new.email, ''), '@', 1),
    'کاربر'
  );

  insert into public.profiles (id, full_name, role, display_name)
  values (new.id, v_name, 'student', v_name)
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;

create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_user();
```

---

## ۸) قابلیت‌های پروژه قبلی — فهرست کامل دسته‌بندی‌شده

### حساب و احراز هویت

```text
ثبت‌نام
ورود با ایمیل و رمز
ورود OTP
بازیابی رمز
تأیید ایمیل
پروفایل
عکس پروفایل
نقش معلم و دانش‌آموز
تنظیمات ظاهری
```

### آزمون‌سازی

```text
ساخت آزمون
ویرایش آزمون
حذف آزمون
تکثیر آزمون
باز و بسته‌کردن آزمون
کد آزمون
مدت آزمون
زمان شروع و پایان
تصادفی‌سازی سؤال
تصادفی‌سازی گزینه
نمره منفی
مخاطبان آزمون
آزمون عمومی
آزمون کلاس
آزمون دانش‌آموز خاص
```

### انواع سؤال

```text
تشریحی
چهارگزینه‌ای
صحیح/غلط
جای خالی
عددی با تلورانس
جورکردنی
```

### امکانات سؤال

```text
بارم
گزینه‌ها
پاسخ صحیح
پاسخ قابل قبول
حساسیت حروف
تصویر سؤال
چند تصویر سؤال
تصویر گزینه
تصویر جورکردنی
فرمول ریاضی
چیدمان متن
قلم
اندازه قلم
ضخیم و مورب
تصویر بالا/پایین/راست/چپ
تصویر آزاد
خط پاسخ
بانک سؤال
```

### دانش‌آموز

```text
ورود با کد آزمون
تایمر
ناوبری سؤال
پاسخ انواع سؤال
ارسال تصویر پاسخ
ذخیره پیش‌نویس
ارسال نهایی
آزمون آفلاین
رسید ارسال
نمایش نتیجه
```

### تصحیح و گزارش

```text
تصحیح خودکار
تصحیح دستی
بازخورد
بانک بازخورد
نمره‌دهی
کارنامه
فهرست نمرات
تحلیل سؤال
نمودار عملکرد
Excel
PDF گزارش
حضور و غیاب آزمون
وضعیت زنده
```

### کلاس و دانش‌آموزان

```text
ساخت کلاس
ویرایش کلاس
حذف کلاس
فهرست اعضا
افزودن دانش‌آموز
حذف دانش‌آموز
فعال/غیرفعال کردن دانش‌آموز
اطلاعات تکمیلی
یادداشت دانش‌آموز
عملیات گروهی
```

### تقویم و پیام

```text
تقویم جلالی
تعطیلات رسمی
رویداد
پیام کلاسی
پیام دانش‌آموزی
ویرایش و حذف رویداد
```

### پرداخت و اشتراک

```text
کیف پول
گردش مالی
اشتراک
طرح‌ها
سهمیه رایگان
کسر و بازگشت اعتبار
پرداخت
سفارش
```

### چاپ و فایل

```text
پیش‌نمایش A4
PDF
چاپ Android
سربرگ امتحان
فونت فارسی
جدول سؤال و بارم
صفحه‌بندی
خروجی Excel
Export و Import آزمون
```

### بروزرسانی و انتشار

```text
نمایش نسخه
بررسی نسخه جدید
یادداشت فارسی نسخه
دانلود APK
نصب APK
GitHub Actions
APK Debug
APK Release
امضای keystore
```

---

## ۹) وضعیت قابلیت‌های Kotlin

### انجام‌شده یا پایهٔ قابل build

```text
ساخت APK GitHub Actions
Gradle/Kotlin/Compose
ورود با رمز
پایه OTP
Supabase Auth
Postgrest
Storage plugin
خواندن profiles
نقش teacher/student
داشبورد معلم پایه
نمایش آزمون‌های معلم پایه
ساخت آزمون پایه
ذخیره exams پایه
دانش‌آموز و ورود با کد پایه
get_exam_for_student پایه
submit_answer پایه
Room پایه
انتخاب چندتصویر
حرکت مستقل تصویر پایه
A4 engine پایه
PDF engine پایه
تصحیح auto پایه
Release signing workflow
منوی همبرگری برای معلم و دانش‌آموز
صفحه درباره و نمایش نسخه واقعی BuildConfig
بررسی نسخه از Supabase app_version
دانلود APK با DownloadManager و نمایش پیشرفت
FileProvider و بازکردن نصب‌کننده Android
کنترل HTTPS، package، versionCode، امضا، اندازه و SHA-256
مجوز REQUEST_INSTALL_PACKAGES و هدایت به تنظیمات Android
نسخه‌گذاری خودکار GitHub Actions بدون Variable دستی
تولید خودکار Artifact و update-metadata.txt
آپلود خودکار APK در Supabase Storage
فعال‌سازی تراکنشی app_version با RPC مدیریتی
صف انتشار concurrency برای جلوگیری از تداخل Buildها
ماندگاری نشست ورود Native در restart و update درجا
بازیابی session بدون flash صفحه ورود
cache امن نمای پروفایل بدون access/refresh token
fallback آفلاین پروفایل با تطبیق دقیق userId
مدیریت Native ویرایش/حذف/تکثیر/وضعیت آزمون
مخاطبان کلاس و دانش‌آموز برای آزمون
کلاس و roster واقعی
ساخت و فعال‌سازی دانش‌آموز با RPC/Edge Function واقعی
آپلود و فشرده‌سازی واقعی تصویر سؤال در exam-images
خروج محلی امن و تعویض حساب
ویرایش و پاسخ کامل matching
تصویر واقعی گزینه و دو طرف matching
تصویر پاسخ دانش‌آموز + draft Room + upload + submit
بانک سؤال Native واقعی: list/add/delete
چرخش EXIF و مجوز پایدار Photo Picker
تصحیح دستی دانش‌آموزی و نمره امن سروری
بانک بازخورد و تأیید نمره خودکار
حضور و غیاب، تمدید زمان و تلاش مجدد
آمار کلی و گزارش کلاس
کارنامه دانش‌آموز و خروجی CSV
چاپ/PDF لیست نمرات با Android PrintManager
```

### قابلیت‌های هنوز کامل نشده یا فقط اسکلت دارند

```text
OTP اختصاصی 8 رقمی
دسته‌بندی پیشرفته و جابه‌جایی بانک سؤال
crop تعاملی تصویر Native
حذف امن فایل‌های orphan از Storage
فونت‌های واقعی res/font
PDF فارسی چندصفحه‌ای پیشرفته و قالب رسمی کامل
تصحیح گروهی سؤال‌محور و میان‌برهای پیشرفته
نمودار و تحلیل دشواری سؤال پیشرفته
WorkManager و صف آفلاین واقعی
تقویم و پیام واقعی
کیف پول و پرداخت واقعی
پاک‌سازی دوره‌ای APKهای قدیمی Storage و retention policy
انتقال نهایی از WebView به Kotlin
```

---

## ۱۰) قانون دریافت سورس Kotlin

فایل استاندارد ارسال سورس:

```text
/home/user/SEND_KOTLIN_SOURCE_sh.txt
```

کاربر آن را در این مسیر قرار می‌دهد:

```text
C:\Users\Hashem\Downloads\SEND_KOTLIN_SOURCE_sh.txt
```

دستور WSL:

```bash
bash /mnt/c/Users/Hashem/Downloads/SEND_KOTLIN_SOURCE_sh.txt
```

نسخه فعلی اسکریپت انتقال، سورس و تنظیمات ضروری را بدون Secret و keystore در بخش‌های 24K می‌فرستد.

خروجی:

```text
KOTLIN_SRC_PART_000:https://paste.rs/...
KOTLIN_SRC_PART_001:https://paste.rs/...
KOTLIN_SRC_PART_COUNT:N
KOTLIN_ARCHIVE_SHA256:...
KOTLIN_BRANCH:main
KOTLIN_COMMIT:...
```

پس از دریافت:

```text
بخش‌ها دقیقاً به ترتیب متصل شوند
SHA-256 آرشیو حتماً تطبیق داده شود
سورس در /home/user/exam-app-kotlin بازسازی شود
همه لینک‌های paste.rs بلافاصله حذف شوند
نتیجهٔ حذف اعلام شود
```

---

## ۱۱) قانون تحویل پچ

هر پچ باید شامل این موارد باشد:

```text
فایل پچ بدون علامت ?
SQL در صورت نیاز
توضیح فارسی کوتاه
دستور جامع WSL
build/test در صورت امکان
به‌روزرسانی اجباری HANDOFF_KOTLIN_MIGRATION_FA.md در همان پچ
commit
آخرین فرمان: git push origin HEAD
```

قانون Git:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin && \
git diff --check && \
git add <files> && \
git diff --cached --stat && \
git commit -m "message" && \
git push origin HEAD
```

---

## ۱۲) درس‌های قطعی و خطاهای مهم گذشته

### اصل اول: حدس‌زدن ممنوع

- اگر APK خطا داد، ابتدا باید خطای واقعی و امن دریافت شود.
- Header، URL کامل، Authorization، apikey و Token نباید در UI نمایش داده شوند.
- برای تشخیص backend، workflow باید فقط status امن HTTP را گزارش کند.

### SupabaseClient Kotlin

داخل `SupabaseProvider.kt` هر سه plugin باید نصب باشند:

```kotlin
install(Auth)
install(Postgrest)
install(Storage)
```

نبودن `Postgrest` خطای زیر ایجاد می‌کند:

```text
Plugin rest not installed or not of type Postgrest
```

### Secretهای Supabase

- `SUPABASE_URL` و `SUPABASE_ANON_KEY` باید متعلق به یک پروژه باشند.
- URL اصلی فعلی: `https://eazwuyrymsvdkwckdpco.supabase.co`
- اگر workflow وضعیت `profiles` را `401` نشان داد، anon key مربوط به همان URL نیست.
- workflow باید قبل از Gradle فایل `local.properties` را از Secretها بسازد.
- `app/build.gradle.kts` باید `local.properties` را مستقیم با `Properties` بخواند.

### OTP

- OTP استاندارد Supabase Hosted معمولاً ۶ رقمی است.
- محدودکردن Kotlin به ۸ رقم با OTP معمولی Supabase سازگار نیست.
- برای OTP ۸ رقمی واقعی، Edge Function، جدول OTP، هش کد، زمان انقضا و SMTP اختصاصی لازم است.
- برای ثبت‌نام عمومی Kotlin، `createUser = true` لازم است.
- trigger `handle_new_user` باید برای ایجاد `profiles` اجرا شود.

### APK

```text
Debug: ir.exam.app.native
Release: ir.exam.app
```

- Debug فقط برای نصب کنار نسخهٔ قدیمی است.
- Release فقط با keystore اصلی و versionCode بالاتر می‌تواند روی نسخه قدیمی نصب شود.
- Artifact Release باید فقط پس از Success کامل GitHub Actions استفاده شود.

### GitHub Actions

- `gradlew` باید executable باشد و در Git mode `100755` داشته باشد.
- Java و Kotlin/KSP باید روی JVM 17 باشند.
- AndroidX باید فعال باشد.
- هنگام paste کردن لاگ GitHub در WSL نباید آن را به‌عنوان دستور اجرا کرد.

---

## ۱۳) آخرین وضعیت

- ورود با رمز قبلاً تا داشبورد معلم موفق شده است.
- بعد از اتصال Postgrest، ارتباط Supabase واقعی شده است.
- OTP عمومی و ایجاد خودکار profiles در حال تکمیل است.
- پچ V1 منوی همبرگری و بروزرسانی امن APK اعمال و روی دستگاه اجرا شده است.
- کاربر فایل `SQL_APP_UPDATE_V1.sql` را در پروژه اصلی Supabase اجرا کرده است.
- پچ V2 اعمال و Push شده است و رفتار جدول خالی را اصلاح می‌کند: نبود انتشار فعال یعنی «برنامه شما به‌روز است»، نه خطا.
- کاربر Variableهای `APP_VERSION_CODE=3` و `APP_VERSION_NAME=1.1.1-native` را ساخت، سپس درخواست کرد نسخه‌گذاری کاملاً خودکار شود.
- پچ V3 اعمال و Push شد و GitHub Actions آن با نسخه‌گذاری خودکار با موفقیت Build شد.
- در V3، GitHub Actions در هر اجرا versionCode و versionName یکتا و بالاتر را از زمان UTC تولید می‌کند.
- Artifact V3 علاوه بر APK نام‌گذاری‌شده، فایل `update-metadata.txt` شامل نسخه، SHA-256 و اندازه واقعی دارد.
- پچ V4 اعمال شد؛ اجرای واقعی Storage status `200` و Public APK verification برابر `OK` بود.
- مرحله RPC فعال‌سازی در اولین اجرای واقعی با HTTP `409` متوقف شد.
- V4.1 در دیتابیس تأیید شد: `delete/insert=true`، `old on conflict=false`، جدول یک ردیف، active صفر و trigger کاربری صفر داشت.
- اجرای واقعی V4.1 به HTTP `400` و SQLSTATE امن `21000` (cardinality violation) رسید.
- V4.2 در دیتابیس فعال است: result=jsonb، returning=false، select_max=false و overload_count=1.
- فراخوانی مستقیم V4.2 در SQL با `P0001 / DIAGNOSTIC_CALL_SUCCEEDED_AND_WAS_ROLLED_BACK` موفقیت بدنه تابع در نشست SQL Editor را ثابت کرد.
- V4.3 تابع مستقل `publish_native_app_release_v1` را ساخت و workflow endpoint خود را به نام جدید تغییر داد.
- اجرای واقعی V4.3 نشان داد نوع Secret برابر `legacy server JWT`، Storage برابر `200` و Public URL برابر `OK` است.
- endpoint جدید نیز HTTP `400 / SQLSTATE 21000` داد؛ بنابراین فرض metadata قدیمی رد شد.
- sanitizer پیام قطعی را نشان داد: `DELETE requires a WHERE clause`.
- علت قطعی، فعال‌بودن `safeupdate` در نشست PostgREST است؛ safeupdate برای DELETE و UPDATE وجود WHERE را الزامی می‌کند.
- ممیزی همه migrationها چهار DELETE بدون WHERE در توابع انتشار پیدا کرد.
- V4.4 همه آن‌ها را به `delete ... where version_code is not null` تغییر داد؛ ستون version_code برابر NOT NULL است.
- کاربر اعلام کرد Build پس از V4.4 موفق شده است؛ انتشار خودکار بروزرسانی اکنون مسیر نهایی خود را دارد.
- منطق JSON، advisory lock، تراکنش، ROW_COUNT و rollback نسخه قبلی حفظ شده است.
- workflow فقط نسخه پاک‌سازی‌شده و کوتاه `code/message/details/hint` را چاپ می‌کند؛ URL، Token، کلید و Header قبل از چاپ حذف می‌شوند.
- مقدار Secret هرگز نباید در چت، Git، APK، Artifact یا لاگ چاپ شود.
- درخواست جدید کاربر: پس از update یا بستن/بازکردن برنامه از حساب خارج نشود.
- پچ V5 برای `awaitInitialization`، بازیابی session، cache نمای پروفایل و UI loading/retry اعمال شد و Build آن موفق بود.
- در آزمایش واقعی صفحه About باز شد، اما بررسی نسخه خطای دقیق `JWT expired` نشان داد.
- علت کدی: `SupabaseAppUpdateRepository` از کلاینت Authدار اصلی استفاده می‌کرد و access token منقضی کاربر به درخواست عمومی app_version تزریق می‌شد.
- V5.1 کلاینت مستقل `PublicUpdateSupabaseProvider` با Postgrest و بدون Auth می‌سازد؛ بررسی نسخه دیگر به JWT کاربر وابسته نیست.
- V5.1 در startup نیز `refreshCurrentSession()` را پیش از نخستین درخواست profiles اجرا می‌کند.
- کاربر اعلام کرد Build V5.1 موفق بوده و پس از بروزرسانی، حساب کاربری حفظ شده و از حساب خارج نشده است.
- نتیجه واقعی ماندگاری نشست روی دستگاه: PASS.
- کاربر برای مرحله بعد «پچ جامع و کامل» می‌خواهد؛ انتخاب حدسی ماژول ممنوع است.
- سورس مرجع WebView با commit `d82b2feedee1` و SHA-256 آرشیو `07efa23ad3ad75a701589de0ba534609b1a9f83b94097df10acd6d1930a864fb` دریافت و بازسازی شد.
- ۲۰۶ فایل مرجع دریافت شد؛ private key و opaque secret وجود نداشت و تنها JWT موجود متعلق به نقش عمومی anon پروژه اصلی بود.
- هر ۵۴ لینک موقت paste.rs پس از تطبیق SHA با HTTP 200 حذف شدند.
- فایل schema زنده Supabase هنوز باید با `SQL_EXPORT_SCHEMA_FOR_COMPREHENSIVE_PATCH.sql` دریافت شود؛ تا قبل از آن نوشتن پچ جامع ممنوع است.
- کاربر درخواست کرده قابلیت‌های باقی‌مانده در چهار پچ یکپارچه انجام شوند؛ اما هر پچ باید واقعاً نوشته، build و تست شود تا پچ صوری یا ناقص تحویل نشود.

---

## ۱۴) وضعیت قطعی ماژول بروزرسانی درون‌برنامه‌ای

### فایل‌های اصلی

```text
app/src/main/java/ir/exam/app/core/update/AppUpdateRepository.kt
app/src/main/java/ir/exam/app/core/update/UpdateUseCase.kt
app/src/main/java/ir/exam/app/core/update/ApkUpdateManager.kt
app/src/main/java/ir/exam/app/data/remote/PublicUpdateSupabaseProvider.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAppUpdateRepository.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/main/java/ir/exam/app/ui/update/UpdateViewModel.kt
app/src/main/res/xml/update_file_paths.xml
.github/workflows/android.yml
supabase/migrations/20260811_app_updates.sql
supabase/migrations/20260811_app_update_auto_publish.sql
supabase/migrations/20260811_app_update_publish_409_hotfix.sql
supabase/migrations/20260811_app_update_publish_v42_hotfix.sql
supabase/migrations/20260811_app_update_publish_v43_new_rpc.sql
supabase/migrations/20260811_app_update_publish_v44_safe_delete.sql
APP_UPDATE_SETUP_FA.md
```

### رفتار نهایی

```text
منوی همبرگری → درباره و بروزرسانی
بررسی app_version با RLS
کلاینت بررسی نسخه مستقل از Auth و JWT کاربر
JWT expired کاربر روی check update اثر ندارد
جدول خالی → برنامه به‌روز است
نسخه جدید → نمایش نام، کد و یادداشت فارسی
دانلود مستقیم با DownloadManager
بررسی package/version/signature/size/SHA-256
بازکردن نصب‌کننده با FileProvider
تأیید نهایی نصب توسط Android
```

### وضعیت Supabase

```text
پروژه صحیح: eazwuyrymsvdkwckdpco.supabase.co
SQL_APP_UPDATE_V1.sql: اجراشده توسط کاربر
جدول: public.app_version
bucket: app-updates
SQL انتشار خودکار: 20260811_app_update_auto_publish.sql (اجراشده)
اولین نتیجه واقعی: Storage=200، Public URL=OK، RPC=409
نتیجه V4.1: Storage=200، Public URL=OK، RPC=400، SQLSTATE=21000
تشخیص schema: result=jsonb، returning=false، select_max=false، overloads=1، rows=1، active=0، rules/triggers=0
فراخوانی مستقیم SQL: موفق و rollback‌شده با diagnostic P0001
V4.3 واقعی: key=legacy JWT، Storage=200، Public=OK، endpoint جدید فعال
پیام خطای قطعی: DELETE requires a WHERE clause
تنظیم مرتبط: safeupdate.enabled در نشست PostgREST
Hotfix بعدی: 20260811_app_update_publish_v44_safe_delete.sql (اجرای یک‌باره لازم)
RPC فعال: publish_native_app_release_v1 با خروجی jsonb
Secret یک‌باره GitHub: SUPABASE_RELEASE_KEY
انتشار نسخه فعال پس از V4.4: خودکار
```

### قانون نسخه

- نسخه Release اصلی فقط با package `ir.exam.app` و همان keystore قبلی قابل جایگزینی است.
- `versionCode` APK جدید باید از نسخه نصب‌شده و مقدار ردیف قبلی بیشتر باشد.
- در GitHub Actions، `APP_VERSION_CODE` به‌صورت خودکار از تعداد ثانیه‌های گذشته از `2020-01-01 UTC` ساخته می‌شود.
- در GitHub Actions، `APP_VERSION_NAME` از تاریخ و ساعت UTC ساخت با قالب `YYYY.MM.DD.HHMMSS-native` ساخته می‌شود.
- Variableهای GitHub با نام `APP_VERSION_CODE` و `APP_VERSION_NAME` دیگر خوانده نمی‌شوند و نیاز به ویرایش ندارند.
- Build محلی خارج از CI همچنان fallback برابر `3` و `1.1.1-native` دارد.
- هر Artifact شامل APK با نام نسخه‌دار و فایل `update-metadata.txt` است.
- `SUPABASE_RELEASE_KEY` فقط در GitHub Actions Secrets نگهداری می‌شود و هرگز نباید echo یا در فایل خروجی نوشته شود.
- RPC انتشار برای public، anon و authenticated ممنوع و فقط برای نقش سروری مجاز است.
- نصب بی‌صدا در Android عادی ممکن نیست و تأیید صفحه نصب سیستم الزامی است.

### نتیجه تأیید پچ V2

```text
./gradlew testDebugUnitTest      → BUILD SUCCESSFUL
./gradlew assembleDebug          → BUILD SUCCESSFUL
Debug package                    → ir.exam.app.native
Debug versionCode                → 3
Debug versionName                → 1.1.1-native
APK Signature Scheme v2          → Verified
```

### نتیجه تأیید پچ V3

```text
YAML workflow parse               → OK
فرمول نسخه خودکار                 → OK
./gradlew testDebugUnitTest        → BUILD SUCCESSFUL
./gradlew assembleDebug            → BUILD SUCCESSFUL
نسخه خودکار نمونه                 → 2026.08.11.042412-native
versionCode خودکار نمونه           → 208585452
Debug package                      → ir.exam.app.native
APK Signature Scheme v2            → Verified
update-metadata.txt در workflow     → تولید خودکار پس از Release
SQL جدید                           → نیاز ندارد
```

### نتیجه تأیید پچ V4

```text
YAML workflow parse                       → OK
bash -n همه run blockهای workflow         → OK
PostgreSQL parser برای migration جدید      → OK (8 statements)
Mock Supabase Storage upload              → HTTP 200
Mock public APK verification              → OK
Mock publish_app_update RPC               → HTTP 200
JSON payload/version/SHA/size              → Verified
./gradlew testDebugUnitTest                → BUILD SUCCESSFUL
انتشار واقعی Storage                      → HTTP 200
بررسی واقعی URL عمومی                     → OK
فعال‌سازی واقعی RPC                       → HTTP 409؛ نیازمند Hotfix V4.1
```

### نتیجه تأیید Hotfix V4.1

```text
PostgreSQL parser تابع اصلاح‌شده           → OK
PostgreSQL parser فایل Hotfix              → OK (6 statements)
YAML و bash -n workflow                    → OK
Mock مسیر موفق Storage/Public/RPC          → 200 / OK / 200
Mock مسیر تعارض RPC                        → HTTP 409
Safe diagnostic code                       → 23505
عدم چاپ message/details آزمایشی            → Verified
ریسک ازبین‌رفتن نسخه قبلی هنگام شکست       → ندارد؛ عملیات تراکنشی است
آزمایش واقعی V4.1                          → Storage=200 / Public=OK / RPC=400
SQLSTATE واقعی V4.1                        → 21000
```

### نتیجه تأیید Hotfix V4.2

```text
تابع قبلی با امضای دقیق drop می‌شود         → Verified in SQL
نوع خروجی جدید                              → jsonb
SELECT ... INTO                             → حذف کامل
RETURNING ... INTO                          → حذف کامل
قفل انتشار هم‌زمان                          → pg_advisory_xact_lock
کنترل تعداد INSERT                          → GET DIAGNOSTICS ROW_COUNT
PostgreSQL parser سه migration نهایی         → OK
PostgreSQL 17 integration test              → PASS
schema آزمایشی id ثابت/singleton             → PASS
انتشار متوالی versionCodeهای 208600001/2     → PASS
نتیجه هر RPC                                → JSON ok=true
تعداد نهایی ردیف/active                      → 1 / 1
دسترسی نقش anon                             → permission denied (PASS)
SQL جدید                                    → 20260811_app_update_publish_v42_hotfix.sql
آزمایش واقعی REST                           → همچنان SQLSTATE 21000
فراخوانی مستقیم SQL                         → PASS / rollback diagnostic
safe_sqlstate مستقیم                        → P0001 (موفقیت عمدی تشخیص)
safe_message مستقیم                         → DIAGNOSTIC_CALL_SUCCEEDED_AND_WAS_ROLLED_BACK
نتیجه آن مرحله                              → بدنه PostgreSQL سالم؛ علت REST بعداً با sanitizer تعیین شد
```

### نتیجه تأیید Hotfix V4.3

```text
نام RPC جدید                                → publish_native_app_release_v1
وابستگی به تابع قدیمی                       → ندارد
نوع خروجی                                   → jsonb
workflow endpoint                           → /rest/v1/rpc/publish_native_app_release_v1
قفل و جایگزینی تراکنشی                      → حفظ‌شده
SQL جدید                                    → 20260811_app_update_publish_v43_new_rpc.sql
PostgreSQL 17 integration test              → PASS
انتشار متوالی با نام RPC جدید               → PASS (208700001/2)
نتیجه JSON هر فراخوانی                       → ok=true
تعداد نهایی ردیف/active                      → 1 / 1
دسترسی anon                                 → permission denied (PASS)
workflow YAML و bash -n                     → PASS
Mock endpoint جدید Storage/Public/RPC       → 200 / OK / 200
Mock کلید JWT قدیمی                         → PASS (apikey + Bearer)
Mock کلید opaque با sb_secret_              → PASS (apikey only)
عدم ارسال sb_secret_ در Authorization       → Verified
Sanitizer کد/پیام/details/hint               → URL/Token/Key/Header redacted
پاسخ خام Supabase                           → چاپ نمی‌شود
آزمایش واقعی key mode                       → legacy server JWT
آزمایش واقعی Storage/Public                 → 200 / OK
آزمایش واقعی RPC جدید                       → HTTP 400 / SQLSTATE 21000
safe_message واقعی                          → DELETE requires a WHERE clause
```

### نتیجه تأیید Hotfix V4.4

```text
علت خطا                                     → safeupdate نیازمند WHERE
ممیزی migrationها                           → 4 DELETE بدون WHERE پیدا شد
اصلاح همه DELETEها                          → where version_code is not null
version_code در schema                      → NOT NULL
منطق تراکنش/advisory lock/ROW_COUNT          → بدون تغییر حفظ شد
SQL جدید                                    → 20260811_app_update_publish_v44_safe_delete.sql
ممیزی همه DELETE/UPDATEهای migration         → 8 statement، همگی WHERE دارند
DELETE/UPDATE بدون WHERE پس از اصلاح         → صفر مورد
PostgreSQL 17 integration test              → PASS
انتشار متوالی versionCodeهای 208800001/2     → PASS
تعداد نهایی ردیف/active                      → 1 / 1
GitHub Actions واقعی پس از V4.4             → SUCCESS (اعلام کاربر)
انتشار خودکار نهایی                         → مسیر workflow کامل بدون failure
```

---

## ۱۵) ماندگاری نشست ورود و JWT — پچ V5 / V5.1

### علت خروج کاذب قبلی

- پلاگین Supabase نشست را ذخیره می‌کرد، اما `AuthViewModel` همیشه با `user=null` شروع می‌شد.
- UI پیش از تمام‌شدن load از storage فوراً صفحه ورود را نمایش می‌داد.
- repository هیچ مسیر `restoreSession` و هیچ انتظار `awaitInitialization` نداشت.

### جریان جدید startup

```text
شروع برنامه
→ نمایش صفحه «در حال بازیابی نشست ورود»
→ auth.awaitInitialization()
→ currentUserOrNull پس از load واقعی storage
→ auth.refreshCurrentSession() برای JWT منقضی
→ تطبیق cache profile با userId نشست
→ تلاش حداکثر 5 ثانیه برای تازه‌سازی profiles
→ fallback به cache هم‌هویت در قطع موقت اینترنت
→ ورود مستقیم به dashboard
```

### فایل‌های V5

```text
app/src/main/java/ir/exam/app/data/local/AuthUserCache.kt
app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt
app/src/main/java/ir/exam/app/data/remote/PublicUpdateSupabaseProvider.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAppUpdateRepository.kt
app/src/main/java/ir/exam/app/ui/update/UpdateViewModel.kt
app/src/main/java/ir/exam/app/domain/repository/AuthRepository.kt
app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/test/java/ir/exam/app/testing/MainDispatcherRule.kt
app/src/test/java/ir/exam/app/ui/auth/AuthViewModelTest.kt
AUTH_SESSION_PERSISTENCE_FA.md
```

### اصول امنیتی

- access token و refresh token در cache سفارشی نوشته نمی‌شوند؛ مدیریت آن‌ها فقط با Supabase Auth است.
- cache فقط id، نام، ایمیل، role و avatar را نگه می‌دارد.
- cache فقط در صورت برابری دقیق userId با نشست Supabase استفاده می‌شود.
- نبود session معتبر، cache قدیمی را پاک می‌کند.
- خطای network دیگر به‌عنوان «نبود profile» تفسیر و موجب ساخت student ناخواسته نمی‌شود.

### محدودیت قطعی Android

- Update درجا فقط با package و امضای یکسان داده برنامه را حفظ می‌کند.
- Release به Release با `ir.exam.app` و keystore اصلی نشست را حفظ می‌کند.
- Debug با `ir.exam.app.native` و Release با `ir.exam.app` storage جدا دارند.
- Uninstall، Clear data یا revoke شدن refresh token طبیعتاً ورود مجدد می‌خواهد.

### نتیجه تست V5

```text
AuthViewModel restore tests                 → 3/3 PASS
کل تست‌های JVM                             → 8/8 PASS
./gradlew testDebugUnitTest                 → BUILD SUCCESSFUL
./gradlew assembleDebug                     → BUILD SUCCESSFUL
Debug APK                                   → ir.exam.app.native
APK Signature Scheme v2                     → Verified
```

### نتیجه تست V5.1

```text
خطای واقعی قبل از اصلاح                     → JWT expired در check update
کلاینت app_version                          → Postgrest-only / بدون Auth
تزریق access token کاربر                    → حذف‌شده از check update
refreshCurrentSession در startup             → اضافه و compile شده
AuthViewModel restore tests                 → 3/3 PASS
کل تست‌های JVM                             → 8/8 PASS
./gradlew testDebugUnitTest                 → BUILD SUCCESSFUL
./gradlew assembleDebug                     → BUILD SUCCESSFUL
GitHub Actions واقعی V5.1                   → SUCCESS (اعلام کاربر)
حفظ حساب پس از بروزرسانی روی دستگاه         → PASS (اعلام کاربر)
SQL جدید                                    → نیاز ندارد
```

---

## ۱۶) پچ جامع V6 مدیریت Native

### مبنای قطعی

```text
Kotlin baseline: V5.1
WebView reference: main@d82b2feedee1
Old source archive SHA-256: 07efa23ad3ad75a701589de0ba534609b1a9f83b94097df10acd6d1930a864fb
Live schema snapshot: 2026-08-11T10:52:42Z
Public tables: 31
Storage buckets: apk / app-updates / exam-images
```

### قابلیت‌های V6

```text
ویرایش آزمون ذخیره‌شده
باز/بسته‌کردن آزمون
تکثیر تراکنشی آزمون
حذف تراکنشی آزمون و وابستگی‌ها
جداسازی answer key در exam_keys
سازگاری JSON سؤال WebView/Native
مخاطب همه/کلاس/دانش‌آموز
آپلود واقعی و فشرده‌سازی تصویر سؤال
فهرست، ساخت، ویرایش و حذف امن کلاس
نمای roster و افزودن/خروج عضو
فهرست و جست‌وجوی دانش‌آموز
فعال/غیرفعال‌کردن دانش‌آموز
ساخت حساب با manage-student بدون کلید مدیریتی در APK
خروج محلی امن و تعویض حساب
```

### فایل‌های اصلی V6

```text
app/src/main/java/ir/exam/app/data/dto/ExamDetailDto.kt
app/src/main/java/ir/exam/app/data/dto/SchoolDtos.kt
app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt
supabase/migrations/20260811_native_comprehensive_management.sql
COMPREHENSIVE_NATIVE_PATCH_FA.md
```

### SQL و امنیت

- `native_delete_exam(text)` و `native_duplicate_exam(text)` تنها RPCهای جدید هستند.
- مالکیت آزمون با `auth.uid()` کنترل می‌شود.
- حذف پاسخ‌ها، draft، attempts، audience، sessions، keys و exam در یک تراکنش انجام می‌شود.
- همه DELETEها WHERE دارند و safeupdate audit صفر مورد ناامن برگرداند.
- answer key از public questions جداست.
- DTO Native عمداً ستون `plain_password` را decode یا نمایش نمی‌دهد.
- حذف کلاس و خروج عضو، حساب دانش‌آموز را حذف نمی‌کند.

### نتیجه تست V6

```text
Kotlin compile                              → PASS
JVM tests                                   → 13/13 PASS
ExamQuestionCodec tests                     → 2/2 PASS
ClassesViewModel tests                      → 2/2 PASS
AuthViewModel tests                         → 4/4 PASS
PostgreSQL 17 integration                   → PASS
duplicate exam + copied key                 → PASS
delete exam + answers/sessions cleanup      → PASS
safeupdate audit                            → PASS
assembleDebug                               → BUILD SUCCESSFUL
lintDebug                                   → BUILD SUCCESSFUL
Debug APK signature v2                      → Verified
GitHub Actions واقعی V6                     → SUCCESS (اعلام کاربر)
آپدیت درون‌برنامه‌ای به V6                  → SUCCESS (اعلام کاربر)
```

### محدودیت ثبت‌شده، نه پنهان

- محدودیت matching و تصویر گزینه/پاسخ در V7 رفع شد.
- فایل Storage هنگام حذف آزمون پاک نمی‌شود تا URL مشترک آزمون تکثیرشده نشکند.
- حذف کامل حساب دانش‌آموز از Native عمداً ارائه نشده و نیازمند تأیید امنیتی جداست.
- schema زنده ستون legacy `plain_password` دارد؛ Native آن را decode/نمایش نمی‌دهد. حذف ستون نیازمند مهاجرت هماهنگ WebView و Edge Function است.

---

## ۱۷) پچ جامع V7 رسانه، matching، پاسخ تصویری و بانک سؤال

### قابلیت‌ها

```text
ویرایش کامل دو ستون matching
تعیین جفت صحیح هر ردیف matching
تصویر مستقل برای دو طرف matching
انتخاب، حذف، فشرده‌سازی و آپلود تصویر گزینه
تنظیم تصویر پاسخ: غیرفعال/اختیاری/اجباری
تعداد مجاز تصویر پاسخ 1 تا 10
انتخاب و پیش‌نمایش تصویر پاسخ در آزمون دانش‌آموز
ذخیره URI پاسخ تصویری در Room draft
آپلود پاسخ‌ها در answers/{studentId}/{examId}/{questionId}
ارسال p_images واقعی به submit_answer
نمایش تصاویر سؤال و گزینه در آزمون
رابط پاسخ matching دانش‌آموز
بانک سؤال واقعی با bank_list/bank_add/bank_del
ذخیره سؤال همراه answer key در بانک
افزودن سؤال بانک به آزمون با id جدید
اصلاح خودکار چرخش EXIF
مجوز پایدار Photo Picker برای draftهای آفلاین
```

### فایل‌های اصلی V7

```text
app/src/main/java/ir/exam/app/domain/model/ExamModels.kt
app/src/main/java/ir/exam/app/domain/repository/AnswerDraftRepository.kt
app/src/main/java/ir/exam/app/data/dto/QuestionBankDto.kt
app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt
app/src/main/java/ir/exam/app/data/repository/RoomAnswerDraftRepository.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt
app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt
```

### امنیت و سازگاری

- answer key همچنان فقط در `exam_keys` است.
- بانک سؤال فقط با RPCهای مالک‌محور موجود کار می‌کند.
- URL قبلی دوباره آپلود نمی‌شود.
- URI محلی فقط پس از فشرده‌سازی به Storage می‌رود.
- تصویر اجباری قبل از submit کنترل می‌شود.
- Room JSON قدیمی بدون wrapper همچنان decode می‌شود.
- هیچ SQL یا Secret جدیدی لازم نیست.

### نتیجه تست V7

```text
Kotlin compile                              → PASS
JVM tests                                   → 17/17 PASS
matching codec round-trip                   → PASS
required response image guard              → PASS
assembleDebug                               → BUILD SUCCESSFUL
lintDebug                                   → BUILD SUCCESSFUL
APK Signature Scheme v2                     → Verified
GitHub Actions واقعی V7                     → SUCCESS (اعلام کاربر)
```

### محدودیت باقی‌مانده

- crop تعاملی هنوز به ویرایشگر bitmap متصل نشده؛ EXIF rotate و resize/compress واقعی است.
- پاک‌سازی orphanهای Storage نیازمند reference counting است.
- دسته‌بندی پیشرفته بانک سؤال در UI بعدی تکمیل می‌شود؛ افزودن/فهرست/حذف واقعی است.

---

## ۱۸) پچ جامع V8 ارزیابی، حضور، تحلیل و گزارش

### قابلیت‌ها

```text
فهرست آزمون برای تصحیح
نمای پاسخ هر دانش‌آموز و هر سؤال
نمای تصاویر پاسخ
ثبت نمره سؤال با کنترل بازه
بازخورد متنی و بانک بازخورد
تأیید نمره‌های خودکار
لغو تأیید نمره
حضور و غیاب آزمون
وضعیت شروع/ارسال/غیبت/تصحیح
تمدید زمان دانش‌آموز
اجازه تلاش مجدد با حفظ کپی
آمار تعداد آزمون/پاسخ/تصحیح/مانده/میانگین
گزارش کلاس بر اساس roster واقعی
انتخاب آزمون‌های گزارش
لیست نمرات و میانگین درصد
خروجی CSV سازگار با Excel
چاپ Android و ذخیره PDF از PrintManager
صفحه نتایج و نمرات دانش‌آموز
خروجی CSV کارنامه دانش‌آموز
تصحیح خودکار MC/TF/Fill/Numeric/Matching
```

### فایل‌های اصلی V8

```text
app/src/main/java/ir/exam/app/data/dto/GradingDtos.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseGradingRepository.kt
app/src/main/java/ir/exam/app/domain/grading/AutoGrader.kt
app/src/main/java/ir/exam/app/domain/model/GradingModels.kt
app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt
app/src/main/java/ir/exam/app/ui/grading/GradingViewModel.kt
app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt
app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt
app/src/main/java/ir/exam/app/ui/reports/ReportPrintHelper.kt
app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt
app/src/main/java/ir/exam/app/ui/reports/StudentResultsViewModel.kt
supabase/migrations/20260811_native_grading_reports.sql
```

### SQL و امنیت

- تنها RPC جدید `native_save_grade(text,jsonb,text)` است.
- تابع مالکیت آزمون را با `auth.uid()` کنترل می‌کند.
- تعداد gradeها باید دقیقاً با تعداد سؤال‌ها برابر باشد.
- هر نمره باید بین صفر و بارم همان سؤال باشد.
- total_grade در سرور محاسبه می‌شود، نه کلاینت.
- UPDATE دارای WHERE و سازگار با safeupdate است.
- بازخورد حداکثر ۲۰۰۰ کاراکتر ذخیره می‌شود.

### نتیجه تست V8

```text
Kotlin compile                              → PASS
JVM tests                                   → 19/19 PASS
AutoGrader tests                            → 2/2 PASS
PostgreSQL 17 native_save_grade             → PASS
invalid score rejection                     → PASS
previous valid grade preservation           → PASS
safeupdate audit                            → PASS
assembleDebug                               → BUILD SUCCESSFUL
lintDebug                                   → BUILD SUCCESSFUL
```

### نقشه تعداد پچ‌های باقی‌مانده

از زمان تحویل V8، سه پچ جامع دیگر برای رسیدن به parity پایدار برنامه لازم است:

```text
V9  تقویم جلالی + پیام هدف‌دار + پروفایل/سربرگ + تنظیمات + کیف پول
V10 WorkManager/صف آفلاین + backup/import/export + چاپ/PDF/فرمول تکمیلی
V11 نهایی‌سازی parity + hardening امنیت/RLS/grants + تست واقعی نقش‌ها و regression
```

کاربر موفقیت V8 را تأیید کرد. پس از تحویل V9 فقط V10 و V11 باقی می‌مانند.

### قانون دائمی هندآف

از این پچ به بعد، هر پچ تحویلی باید فایل زیر را نیز به‌روزرسانی کند:

```text
HANDOFF_KOTLIN_MIGRATION_FA.md
```

هندآف باید حداقل شامل وضعیت قابلیت جدید، فایل‌های تغییرکرده، SQL اجراشده یا باقی‌مانده، نسخه Build و نتیجه build/test باشد.


---

## ۱۹) پچ جامع V9 تقویم، پروفایل، تنظیمات و کیف پول

### وضعیت ورودی

```text
V8 build/install                           → SUCCESS (اعلام کاربر)
Live schema                                → 31 public table confirmed
Calendar/wallet/profile legacy source      → audited against main@d82b2feedee1
Payment API                                → checked against official Zarinpal/IDPay docs
```

### قابلیت‌های کامل‌شده

```text
تبدیل Native جلالی Borkowski و شبکه ماه 1400..1500
تعطیلات رسمی server-driven برای 1403..1405 + جمعه
پیام روزانه معلم با مخاطب همه/کلاس/دانش‌آموز
فیلتر قطعی پیام دانش‌آموز در security-definer RPC
پروفایل، آواتار، نام نمایشی و teacher public badge
ذخیره استان/شهر/منطقه/مدرسه برای سربرگ
تم system/light/dark، dynamic color و font scale در DataStore
موجودی و 50 گردش اخیر کیف پول با واحد تومان
کسر اتمیک هزینه ساخت/ویرایش/تکثیر آزمون
محافظ operation_id در برابر double charge هنگام retry
سفارش پرداخت و credit فقط در Edge Function/service_role
زرین‌پال، آیدی‌پی و sandbox صریح و کنترل‌شده
```

### فایل‌های اصلی V9

```text
app/src/main/java/ir/exam/app/core/calendar/JalaliCalendar.kt
app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt
app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt
app/src/main/java/ir/exam/app/data/dto/CalendarDtos.kt
app/src/main/java/ir/exam/app/data/dto/NativeProfileDtos.kt
app/src/main/java/ir/exam/app/data/dto/BillingDtos.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseCalendarRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseBillingRepository.kt
app/src/main/java/ir/exam/app/domain/model/CalendarModels.kt
app/src/main/java/ir/exam/app/domain/model/ProfileModels.kt
app/src/main/java/ir/exam/app/domain/model/BillingModels.kt
app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt
app/src/main/java/ir/exam/app/ui/calendar/CalendarViewModel.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt
app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt
app/src/main/java/ir/exam/app/ui/billing/BillingViewModel.kt
supabase/migrations/20260811_native_calendar_profile_wallet.sql
supabase/functions/wallet-payment/index.ts
supabase/config.toml
supabase/tests/20260811_v9_integration.sql
COMPREHENSIVE_CALENDAR_PROFILE_WALLET_V9_FA.md
.github/workflows/android.yml
```

### SQL و امنیت

- `native_save_exam_v1(jsonb)` ذخیره exam، key، audience و debit را در یک تراکنش انجام می‌دهد.
- قانون هزینه روی fingerprint سؤال و کلید پاسخ در سرور محاسبه می‌شود.
- `native_exam_operations.operation_id` نتیجه retry را بدون کسر یا درج دوباره برمی‌گرداند.
- `native_duplicate_exam_v2(text,uuid)` نیز اتمیک و idempotent است.
- `native_wallet_snapshot()` فقط کیف پول `auth.uid()` را برمی‌گرداند.
- `native_create/set/fail/credit_wallet_payment` از authenticated/anon revoke و فقط به service_role داده شده‌اند.
- `wallet_topup` و `wallet_refund` قدیمی از authenticated revoke شدند؛ APK امکان شارژ رایگان ندارد.
- callback بانکی عمومی است، ولی شروع سفارش POST در خود تابع با JWT و `auth.getUser` بررسی می‌شود.
- authority، order id، مبلغ و پاسخ verify در سرور تطبیق می‌شوند.
- `native_my_profile()` ستون‌های مجاز را برمی‌گرداند و `plain_password` وارد پاسخ Native نمی‌شود.
- تمام UPDATE/DELETEهای migration دارای WHERE هستند.

### نتیجه تست V9

```text
Kotlin compile                              → PASS
JVM tests                                   → 26/26 PASS
Jalali 1400..1500 full round-trip           → PASS
PostgreSQL 17 migration                     → PASS
PostgreSQL second execution                 → PASS
Calendar teacher/member/non-member          → PASS
Profile avatar ownership                    → PASS
Exam create/edit/answered-edit/duplicate    → PASS
Insufficient wallet atomic rollback         → PASS
Exam operation idempotency                  → PASS
Payment credit idempotency                  → PASS
Function grants audit                       → PASS
Deno check wallet-payment                   → PASS
assembleDebug                               → BUILD SUCCESSFUL
lintDebug                                   → BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2                     → Verified
```

### راه‌اندازی خارجی الزامی

```text
1) اجرای SQL_NATIVE_CALENDAR_PROFILE_WALLET_V9.sql در SQL Editor پروژه اصلی
2) اعمال Patch V9 در repository Kotlin
3) deploy تابع wallet-payment با Supabase CLI و --no-verify-jwt
4) برای تست: PAY_PROVIDER=sandbox + PAY_ALLOW_SANDBOX=true
5) برای واقعی: merchant/API key فقط در Edge Function Secrets، هرگز Git/Chat/APK
```

### بدهی‌های باقی‌مانده

```text
V10: WorkManager/pending_actions، backup/restore، import/export، چاپ رسمی چندصفحه‌ای، فرمول و تحلیل تکمیلی
V11: parity نهایی، RLS/grants کامل، مهاجرت plain_password، تست واقعی teacher/student، orphan cleanup و regression
```

پس از موفقیت V9، تعداد پچ‌های جامع باقی‌مانده: **۲ پچ (V10 و V11)**.
