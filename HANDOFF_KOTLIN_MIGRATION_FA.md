# هندآف جامع مهاجرت سامانه آزمون از WebView به Native Kotlin

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۱۳ — V21 نوار دانش‌آموز و اسکرول دقیق سؤال
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

---

## ۲۰) Hotfix V9.1 — سیاست سن dependency در Deno

- Supabase JS از 2.112.3 به نسخه بالغ‌تر 2.112.2 pin شد.
- محافظ minimum dependency age خود Deno غیرفعال نشد.
- SQL و Kotlin تغییری نکردند.
- wallet-payment پس از اصلاح دوباره deploy شد.


---

## ۲۱) پچ جامع V10 — آفلاین، انتقال داده، چاپ، فرمول و تحلیل

### وضعیت ورودی

```text
V9 + V9.1 build/release                   → SUCCESS (اعلام کاربر)
V9 SQL readiness                          → 5/5 true
wallet-payment deploy + sandbox secrets   → SUCCESS
V10 reference                             → WebView main@d82b2feedee1 + live schema
```

### قابلیت‌های V10

```text
Room pending_actions + WorkManager network constraint/backoff
کپی تصاویر صف به filesDir خصوصی و پاک‌سازی بعد از رسید
RPC idempotent برای ثبت پاسخ WorkManager
نمای صف، retry، blocked-auth و failed برای دانش‌آموز
پیش‌نویس خودکار Builder و بازیابی پس از process death
Export/Import آزمون با EXAMPKG1 و .azmoon
Backup نسخه‌دار بدون password/token/plain_password
Restore انتخابی، تراکنشی، هزینه‌دار و idempotent
چاپ/PDF رسمی Native چندصفحه‌ای با سربرگ جلالی
چاپ آزمون، کلید و گزارش نمرات
موتور فرمول Native و Formula Editor
تصحیح سؤال‌محور گروهی با batch اتمیک
progress guard و finalize کنترل‌شده
تحلیل دشواری، omission، discrimination، corrected point-biserial و Cronbach alpha
```

### فایل‌های اصلی V10

```text
app/src/main/java/ir/exam/app/data/local/PendingActionEntity.kt
app/src/main/java/ir/exam/app/data/local/PendingActionDao.kt
app/src/main/java/ir/exam/app/data/local/ExamBuilderDraftEntity.kt
app/src/main/java/ir/exam/app/data/local/NativeDatabaseProvider.kt
app/src/main/java/ir/exam/app/data/work/PendingActionWorker.kt
app/src/main/java/ir/exam/app/data/work/PendingActionScheduler.kt
app/src/main/java/ir/exam/app/data/repository/PendingSubmissionCodec.kt
app/src/main/java/ir/exam/app/data/repository/PendingMediaStore.kt
app/src/main/java/ir/exam/app/data/repository/PendingActionRepository.kt
app/src/main/java/ir/exam/app/data/repository/ExamBuilderDraftStore.kt
app/src/main/java/ir/exam/app/data/repository/ExamPackageCodec.kt
app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt
app/src/main/java/ir/exam/app/core/math/NativeMathFormatter.kt
app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt
app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt
app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt
app/src/main/java/ir/exam/app/core/printing/OfficialPrintController.kt
app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt
supabase/migrations/20260811_native_offline_portability_analysis.sql
supabase/tests/20260811_v10_integration.sql
COMPREHENSIVE_OFFLINE_PORTABILITY_PRINT_V10_FA.md
```

### SQL و امنیت

- `native_submit_queued_answer_v1` روی operation UUID قفل advisory می‌گیرد و نتیجه موفق را برای retry نگه می‌دارد.
- `native_bulk_save_question_grades_v1` کل batch را قبل از اولین UPDATE اعتبارسنجی می‌کند.
- `native_finalize_bulk_grades_v1` برای پاسخ‌های تصحیح‌نشده progress همه سؤال‌ها را الزام می‌کند.
- `native_question_analysis_v1` فقط آزمون متعلق به معلم را تحلیل می‌کند.
- `native_export_backup_v1` هیچ رمز، token یا plain_password برنمی‌گرداند.
- `native_restore_backup_v1` شناسه‌ها و teacher id ورودی را نادیده می‌گیرد، شناسه تازه می‌سازد و wallet را در همان تراکنش کم می‌کند.
- همه UPDATE/DELETEها WHERE دارند.

### نتیجه تست V10

```text
Kotlin compile                              → PASS
JVM tests                                   → 38/38 PASS
PostgreSQL 17 migration + second run        → PASS
queued submit + retry idempotency           → PASS
bulk grade atomic validation                → PASS
finalize progress guard                     → PASS
item analysis and reliability               → PASS
backup excludes secrets                     → PASS
paid restore + retry idempotency             → PASS
safeupdate audit                            → PASS
assembleDebug                               → BUILD SUCCESSFUL
lintDebug                                   → BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2                     → Verified
```

### باقی‌مانده نهایی

```text
V11: parity نهایی، hardening کامل RLS/grants، مهاجرت plain_password،
تست واقعی نقش‌های teacher/student، orphan Storage cleanup، retention APK،
regression کامل و قطع مرجع WebView.
```

پس از موفقیت V10 فقط **یک پچ جامع V11** باقی می‌ماند.


---

## ۲۲) V11 نهایی — Hardening، نگهداری و پایان مهاجرت

### وضعیت ورودی

```text
V10 build/release                         → SUCCESS (اعلام کاربر)
Native capabilities V1..V10               → complete
V11 baseline                              → Kotlin/Compose only, no android.webkit import
```

### تغییرات نهایی

```text
manage-student بدون ذخیره plaintext password
ویرایش، reset password و حذف کامل حساب دانش‌آموز در Native
DROP قطعی profiles.plain_password
RPCهای my_students/class_roster بدون رمز
native_ensure_profile_v1 و حذف DML مستقیم profiles از APK
native_set_exam_open_v1 و حذف UPDATE مستقیم exams
RLS همه جدول‌های public
revoke کامل DML مستقیم anon/authenticated
function allowlist و search_path امن برای SECURITY DEFINER
بازنشانی کامل policyهای حساس و Storage
آپلود Storage فقط در مسیر auth.uid
storage-maintenance dry-run/delete-gated
orphan reference scan برای profile/exam/bank/answer/trash/draft
APK retention خودکار در publication workflow
Secret/keystore scan، cert matching و cleanup در CI
Android allowBackup=false و FLAG_SECURE هنگام آزمون
FINAL_NATIVE_VERIFY و regression نقش teacher/student
```

### فایل‌های کلیدی V11

```text
supabase/functions/manage-student/index.ts
supabase/functions/storage-maintenance/index.ts
supabase/config.toml
supabase/migrations/20260812_native_final_hardening.sql
supabase/tests/20260812_v11_security_regression.sql
scripts/verify_native_final.py
.github/workflows/android.yml
app/src/main/AndroidManifest.xml
app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt
app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt
FINAL_NATIVE_HARDENING_V11_FA.md
```

### نتیجه تست

```text
Kotlin compile                              → PASS
JVM tests                                   → 39/39 PASS
Deno check 3 Edge Functions                → PASS
PostgreSQL migration + second run          → PASS
V11 teacher/student security regression    → PASS
plain_password schema/function scan        → PASS
RLS/direct mutation/function grants        → PASS
Storage exact policy count                 → PASS
FINAL_NATIVE_VERIFY                        → PASS
assembleDebug                              → BUILD SUCCESSFUL
lintDebug                                  → BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2                    → Verified
Secret scan                                → CLEAN
```

### وضعیت پایان مهاجرت

```text
Native Android Kotlin + Compose + Room + WorkManager + Supabase = runtime نهایی
WebView = فقط آرشیو تاریخی و خارج از APK/runtime
تعداد Patch برنامه‌ریزی‌شده باقی‌مانده = 0
```

پس از اعمال V11 و موفقیت تست واقعی دستگاه، مهاجرت برنامه‌ریزی‌شده کامل است. تغییرات بعدی feature release یا maintenance عادی هستند، نه ادامه مهاجرت WebView.

---

## ۲۳) Hotfix V11.1 — استخراج امن fingerprint امضای Release

### خطای واقعی GitHub Actions

```text
apksigner verify: v2=true
Process completed with exit code 1 پیش از پیام certificate VERIFIED
```

### علت و اصلاح

- APK واقعاً امضای v2 معتبر داشت.
- pipeline دارای `awk ... exit` زیر `set -o pipefail` بود و می‌توانست producer را با SIGPIPE متوقف کند.
- خروجی کامل `apksigner` و `keytool` اکنون ابتدا در متغیر گرفته می‌شود.
- `awk` بدون خروج زودهنگام fingerprint را در END استخراج می‌کند.
- هر fingerprint باید دقیقاً ۶۴ نویسه hex باشد.
- اختلاف واقعی گواهی همچنان Build را متوقف می‌کند و فقط پیام امن چاپ می‌شود.
- SQL، Kotlin و Edge Function تغییری نکردند.

---

## ۲۴) Hotfix V11.2 — JSONB RPC object و اسکرول Drawer

### خطای واقعی دستگاه

```text
Unexpected JSON token at offset 0
Expected start of the array '[' but had '{'
JSON input: {"ok": true, "notes": []}
```

### علت قطعی

- `PostgrestResult.decodeSingle<T>()` در supabase-kt ابتدا payload را به `List<T>` decode و سپس عضو اول را برمی‌دارد.
- RPCهای JSONB سامانه مثل `cal_month` مستقیماً Object برمی‌گردانند، نه آرایهٔ row.
- بنابراین تمام RPCهای Object/JSONB باید از `decodeAs<T>()` استفاده کنند.

### اصلاح

- `decodeSingle` از همه Repositoryها حذف و با `decodeAs` جایگزین شد.
- مسیرهای Auth، Calendar، Wallet، Builder، Grading، Profile، School، Student، Backup و Dashboard پوشش داده شدند.
- تست regression با payload واقعی `{ "ok": true, "notes": [] }` اضافه شد.
- `FINAL_NATIVE_VERIFY` بازگشت decodeSingle به Repositoryها را ممنوع می‌کند.
- کل محتوای `ModalDrawerSheet` داخل Column با `fillMaxHeight + verticalScroll` قرار گرفت.
- SQL، RLS و Edge Function تغییری نکردند.

### نتیجه تست V11.2

```text
Kotlin compile                       → PASS
JVM tests                            → 40/40 PASS
RPC JSON object regression           → PASS
Drawer scroll Compose build          → PASS
FINAL_NATIVE_VERIFY                  → PASS
assembleDebug                        → BUILD SUCCESSFUL
lintDebug                            → BUILD SUCCESSFUL (0 error)
APK Signature Scheme v2              → Verified
GitHub Actions واقعی V11.2           → SUCCESS (اعلام کاربر)
```

---

## ۲۵) V12 جامع — رفع مسیرهای حیاتی Native

### علت ایجاد V12

پایان نقشه V1 تا V11 به معنی پایان معماری مهاجرت بود، نه برابری تمام مسیرهای حیاتی. ممیزی مستقیم سورس پس از V11.2 پنج نقص واقعی را نشان داد:

```text
ورود دانش‌آموز فقط ایمیل می‌پذیرفت و نام کاربری به Auth نگاشت نمی‌شد
ثبت‌نام/بازیابی/تغییر رمز معلم UI کامل نداشت
shuffle_q / shuffle_opt / teacher_message / expires_at / server_now مصرف نمی‌شد
آزمون فعال و deadline پس از process death بازیابی نمی‌شد
my_answers دریافت می‌شد ولی UI فقط تعداد را نشان می‌داد
```

V12 هر پنج مورد را در یک Patch واقعی رفع می‌کند.

### حساب و Auth

```text
نام کاربری دانش‌آموز → نگاشت داخلی قطعی به username@student.exam.local
ورود OTP موجود       → createUser=false
ثبت‌نام معلم          → ایمیل + OTP + نام کاربری + رمز + RPC مالک‌محور
بازیابی رمز معلم      → OTP بدون ساخت حساب + updateUser پس از تأیید
تغییر رمز             → پروفایل و تنظیمات / حساب
تغییر نام کاربری معلم → native_update_my_username_v1
```

- دامنه داخلی دانش‌آموز در UI نمایش داده نمی‌شود.
- managed student، عضو کلاس و حساب دارای `teacher_id` امکان self-promotion ندارند.
- `native_my_registration_state_v1` ثبت‌نام قطع‌شده پس از OTP را بازیابی می‌کند تا کاربر اشتباهاً وارد داشبورد دانش‌آموز نشود.
- رمز فقط به Supabase Auth داده می‌شود و در public، Room، cache یا log ذخیره نمی‌شود.

### آزمون و تایمر

- `StudentExamPayloadCodec` فیلدهای `shuffle_q`, `shuffle_opt`, `teacher_message`, `server_now`, `expires_at` و اطلاعات اختیاری تلاش را مصرف می‌کند.
- تصادفی‌سازی با seed پایدار student/exam انجام می‌شود.
- گزینه و matching به اندیس اصلی سرور نگاشت می‌شوند.
- `PendingSubmissionCodec` پاسخ‌ها را با `originalIndex` به ترتیب اصلی سرور برمی‌گرداند.
- deadline از اختلاف زمان سرور محاسبه می‌شود؛ ساعت خام گوشی مبنا نیست.
- در صفرشدن، یک refresh برای دریافت تمدید معلم انجام می‌شود.
- آزمون بدون مدت دیگر خودکار ارسال نمی‌شود.
- صفحه پیش از شروع، پیام معلم، زمان، تعداد سؤال و وضعیت بازیابی را نشان می‌دهد.

### Room و process death

```text
Room version: 2 → 3
Table: active_exam_sessions
```

- payload امن بدون answer key، مالک حساب، کد/شناسه و deadline ذخیره می‌شود.
- اولین join آزمون ناشناخته اینترنت می‌خواهد؛ آزمون قبلاً بازشده و draft آن آفلاین قابل ادامه است.
- پس از ارسال یا صف‌شدن نهایی، active session پاک می‌شود.
- draft صف‌شده تا رسید WorkManager حفظ می‌شود.

### جزئیات پاسخ دانش‌آموز

RPCهای جدید:

```text
native_my_answers_v1()
native_my_answer_detail_v1(text)
```

- فقط پاسخ‌های `student_id = auth.uid()` قابل خواندن‌اند.
- قبل از `graded=true` کلید و explanation هرگز برنمی‌گردند.
- بعد از تصحیح، پاسخ صحیح، نمره سؤال، بازخورد و تصویر پاسخ نمایش داده می‌شوند.
- matching قدیمیِ string JSON و matching جدید Object هر دو نمایش داده می‌شوند.

### فایل‌های اصلی V12

```text
app/src/main/java/ir/exam/app/data/repository/AuthIdentifier.kt
app/src/main/java/ir/exam/app/data/repository/StudentExamPayloadCodec.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt
app/src/main/java/ir/exam/app/data/local/ActiveExamSessionEntity.kt
app/src/main/java/ir/exam/app/data/local/ActiveExamSessionDao.kt
app/src/main/java/ir/exam/app/data/local/NativeDatabaseProvider.kt
app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt
app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt
app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt
app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt
app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt
app/src/main/java/ir/exam/app/ui/reports/StudentResultsViewModel.kt
supabase/migrations/20260812_native_critical_flows_v12.sql
supabase/tests/20260812_v12_critical_flows.sql
CRITICAL_NATIVE_FLOWS_V12_FA.md
```

### SQL الزامی V12

```text
SQL_NATIVE_CRITICAL_FLOWS_V12.sql
```

Readiness باید پنج مقدار true برگرداند:

```text
teacher_signup_ready
username_update_ready
registration_resume_ready
answer_list_ready
answer_detail_ready
```

SQL V9/V10/V11 دوباره اجرا نمی‌شود. Edge Function، Secret یا deploy جدید لازم نیست.

### نتیجه تست V12

```text
Kotlin compile                                  → PASS
JVM tests                                       → 50/50 PASS
student username/Auth-domain regression         → PASS
teacher signup/recovery/resume regression       → PASS
stable shuffle + canonical response order       → PASS
answer-key cache stripping                      → PASS
process-death exam + draft restore              → PASS
PostgreSQL 17 migration first + second run      → PASS
managed-student promotion denial                → PASS
ungraded answer-key non-disclosure              → PASS
cross-student detail denial                     → PASS
function grant/revoke audit                     → PASS
FINAL_NATIVE_VERIFY                             → PASS
lintDebug                                       → BUILD SUCCESSFUL (0 error)
assembleDebug                                   → BUILD SUCCESSFUL
Debug package                                   → ir.exam.app.native
APK Signature Scheme v2                         → Verified
```

### وضعیت پس از V12

- پنج مسیر حیاتی گزارش‌شده پوشش داده شدند.
- قابلیت‌های غیرحیاتیِ parity مثل دسته‌بندی پیشرفته بانک، crop تعاملی، نمودار گرافیکی و Excel واقعی همچنان featureهای بعدی‌اند و داخل V12 نیستند.
- پایان V12 فقط پس از اجرای SQL، Build موفق GitHub و تست واقعی teacher/student روی دستگاه تأیید می‌شود.
- Build واقعی V12 توسط کاربر موفق اعلام شد.

---

## ۲۶) V13 برابری کامل با WebView — سه مرحله

کاربر برابری کامل همه موارد، شامل آزمون‌ساز، تصویر، بانک، student UX، مدیریت، Excel، نمودار، قفل، فرمول و چاپ را انتخاب کرد. تحویل به سه Patch مستقل و قابل Build تقسیم شد.

### V13.1 — آزمون‌ساز، رسانه، بانک، فرمول، چاپ، قلم و قفل

قابلیت‌ها:
```text
زمان‌بندی جلالی و ذخیره اتمیک native_save_exam_v2
جابه‌جایی سؤال/گزینه و ۲ تا ۱۰ گزینه
case-sensitive fill blank
matching نامساوی و جابه‌جایی مستقل
تصویر پاسخ ۱ تا ۱۰
تمام styleهای سؤال و خطوط پاسخ
پیش‌نمایش سؤال/A4
crop/rotate/resize واقعی
بانک چنددسته‌ای با جست‌وجو و duplicate guard
فرمول Native دوبعدی + editor کامل‌تر + PDF
فونت‌های واقعی OFL وزیرمتن/شبنم/ساحل
قفل PIN PBKDF2 و device credential
```

SQL:
```text
SQL_NATIVE_FULL_PARITY_STAGE1_V13.sql
```

تست:
```text
Kotlin clean compile              PASS
JVM tests                         52/52 PASS
PostgreSQL 17 migration x2        PASS
V13_FULL_PARITY_PASS              PASS
lintDebug                         PASS (0 error)
assembleDebug                     PASS
APK v2 signature                  Verified
```

مرحله‌های بعدی:
```text
V13.2 student navigation/review/flags/exit guard/media
V13.3 bulk students/notes/XLSX/charts/live/feedback
```

### V13.2 — تجربه کامل دانش‌آموز

```text
شبکه ناوبری همه سؤال‌ها
نشان پاسخ‌داده‌شده و علامت مرور
ذخیره flags و lastQuestionIndex در Room draft
مرور بی‌پاسخ‌ها پیش از ارسال
تأیید صریح ارسال نهایی
Back/Exit guard با حفظ draft و deadline
رندر style سؤال از Builder
crop/rotate تصاویر پاسخ پیش از صف آفلاین
```

نیاز عملیاتی:
```text
SQL جدید: ندارد
Edge deploy: ندارد
Secret: ندارد
پیش‌نیاز: V13.1
```

تست:
```text
Kotlin compile             PASS
JVM tests                  52/52 PASS
lintDebug                  PASS (0 error)
assembleDebug              PASS
APK v2 signature           Verified
```

مرحله باقی‌مانده: V13.3 مدیریت/گزارش و Excel واقعی.

### V13.3 — مدیریت، Excel، نمودار، live و بازخورد

```text
Room 4 و یادداشت خصوصی student
ساخت گروهی ۱..۱۰۰ با manage-student موجود
رمزهای bulk فقط همان بار و XLSX امن
OOXML واقعی چند Sheet برای دانش‌آموز/نمره/کارنامه
نمودار خطی و میله‌ای Compose Canvas
live status با refresh بیست‌ثانیه‌ای
ویرایش/حذف بانک بازخورد با RPC مالک‌محور
FINAL_NATIVE_VERIFY کل سه مرحله
```

نیاز عملیاتی:
```text
SQL جدید: ندارد (توابع لازم در SQL V13.1 هستند)
Edge deploy: ندارد
Secret: ندارد
پیش‌نیاز: V13.1 و V13.2
```

تست نهایی:
```text
Kotlin compile             PASS
JVM tests                  53/53 PASS
PostgreSQL migration x2    PASS
V13_FULL_PARITY_PASS       PASS
FINAL_NATIVE_VERIFY        PASS
lintDebug                  PASS (0 error)
assembleDebug              PASS
APK v2 signature           Verified
```

### نتیجه نقشه سه‌مرحله‌ای V13

پس از Build و تست دستگاه هر سه Patch، فهرست «قابلیت‌های ناقص نسبت به WebView» پوشش داده شده است. WebView، plain_password، نمایش رمز قبلی و نصب silent عمداً بازنمی‌گردند و جزو parity مجاز نیستند.

- هر سه Patch V13.1 تا V13.3 توسط کاربر اعمال و تا commit `a0c5071` با موفقیت Push شدند.

---

## ۲۷) V13.4 — همسان‌سازی دقیق ترتیب و نمادهای فرمول با مرجع کاربر

### ورودی قطعی

```text
index.html                         SHA-256 7a33056ad2604bdcca2329f3cba15404df0e9c32180ed9315b941fc169d89c9f
مرجع-کامل-بخش-فرمول.md           SHA-256 b65de8d6285ed60ebc50a1464547a8a268c864cc64083add4ccf5ce5e58e4936
کد-خام-بخش-فرمول.txt             SHA-256 4986c0dff5781ad907cb286040186f1256f7820448564ba850e5d0105d4ac7ea
فقط-html-بخش-فرمول.txt           SHA-256 5f9b14573d49756bb554598665da83bd79b90b10129091837cb7152006d35b83
```

### ترتیب Native

```text
سه حالت جعبه‌ای / تایپ سریع / آماده
راهنما
Undo / Redo / Copy / Paste / A− / A+
پیش‌نمایش و کادر ساختاری
دسته‌های اصلی دقیق مرجع
ردیف اخیر / تبدیل / log / ∫ / ٫٪ / sin
درج / سطر تازه / abc / کسر / توان / رادیکال
keypad چهارردیفی دقیق مرجع
جست‌وجو، کتابخانه و کد فرمول
تایپ سریع و گالری آماده
footer درج / پاک / انصراف
```

### داده مرجع

```text
گروه اصلی                 8
دسته                       77
ورودی دسته‌بندی‌شده       2084
نماد Unicode               1200
فرمول گالری                34
```

- `formula_library_v13.json` از آرایه‌های واقعی مرجع ساخته شد.
- JavaScript یا WebView وارد runtime نشد؛ خواندن داده با Kotlin Serialization و رندر با Compose/Canvas است.
- تبدیل طبیعی، ریشه فرجه‌دار، انتگرال چندگانه، فلش‌ها، مجموعه‌ها، یونانی و نمادهای فیزیک/شیمی توسعه یافت.

### تست

```text
Kotlin compile                    PASS
JVM tests                         56/56 PASS
Formula reference asset          PASS
UI order regression              PASS
Unicode 1200                      PASS
FINAL_NATIVE_VERIFY              PASS
lintDebug                         PASS (0 error)
assembleDebug                     PASS
APK v2 signature                 Verified
```

SQL، Edge Function و Secret جدید لازم نیست.

---

## ۲۸) V13.5 — Hotfix رندر SVG کامل فرمول

### علت

پس از V13.4 داده و ترتیب مرجع کامل بود، اما دو مسیر هنوز می‌توانستند ظاهر کد ایجاد کنند:

1. `NativeFormulaView` nodeها را با `Text`های Compose می‌چید و SVG واقعی نبود؛
2. fallback دستور ناشناخته نام دستور را بدون backslash چاپ می‌کرد و کادر ساختاری نیز TeX را مستقیم نشان می‌داد.

### اصلاح

```text
TeX داخلی → NativeMathParser → MathNode AST → NativeMathSvgRenderer
           → SVG مستقل → Coil SvgDecoder / AndroidSVG → Compose
```

- `NativeMathSvgRenderer.kt` اضافه شد.
- `coil-svg:2.7.0` هم‌نسخه با Coil موجود اضافه شد.
- کسر، رادیکال، توان/زیرنویس، ماتریس، دلیمتر و accent با `path`/`line` SVG رسم می‌شوند.
- glyphهای عددی، حرفی، یونانی و Unicode داخل همان سند SVG قرار دارند.
- کتابخانه، Unicode 1200، علاقه‌مندی، اخیر، منوهای سریع، keypad، ابزارهای کسر/توان/رادیکال، تایپ سریع و گالری از `NativeFormulaIcon`/`NativeFormulaView` SVG استفاده می‌کنند.
- کادر فرمول‌نویسی به سطح تصویری SVG تبدیل شد؛ input نامرئی فقط برای دریافت صفحه‌کلید است.
- TeX فقط در بخش جمع‌شدهٔ حرفه‌ای و پس از انتخاب صریح کاربر قابل مشاهده است.
- نام کد خام از فهرست فرمول‌های اخیر حذف شد.
- همهٔ commandهای واقعی asset پوشش داده شدند و unknown command دیگر نام خام خود را نمایش نمی‌دهد.
- `\\` و `\left ... \right` node ساختاری دارند.
- نمایش فرمول در سؤال و گزینه، حتی نمادهای سادهٔ `alpha`/`times`، از همان `NativeFormulaView` SVG عبور می‌کند و مسیر AnnotatedString خام حذف شد.
- چاپ/PDF از همان AST و Canvas برداری امن باقی ماند.

### امنیت SVG

```text
ورودی XML-escaped
رنگ محدود به #RRGGBB
بدون URL خارجی
بدون href/src
بدون script
بدون foreignObject
بدون JavaScript یا WebView
memory-only generation
```

### ممیزی و تست

```text
Kotlin compile                         PASS
JVM tests                              64/64 PASS
Reference SVG generation              2118/2118 PASS
Unicode exact count                   1200 PASS
Raw unknown command leak                 0 PASS
SVG XML/security tests                   PASS
FINAL_NATIVE_VERIFY                      PASS
lintDebug                        PASS (0 error, 24 warning)
assembleDebug                            PASS
APK v2 signature                      Verified
SvgDecoder + AndroidSVG packaged         PASS
```

هشدارهای lint خطا نیستند و به کدهای قدیمی/نسخه‌های جدید dependency مربوط‌اند.

### استقرار

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
پیش‌نیاز: V13.4
```

راهنمای مستقل: `FORMULA_SVG_RENDERING_V13_5_FA.md`.

---

## ۲۹) V13.6 — جعبه‌های لمسی، درج صحیح کتابخانه و رادیکال کش‌پذیر

### درخواست کاربر

```text
تمام مقدارهای قابل ویرایش مانند جعبه باشند.
لمس جعبه رنگ فعال را تغییر دهد.
کتابخانه‌ها درست در خانهٔ فعال درج شوند.
خط بالای رادیکال همراه محتوا کشیده شود.
```

### معماری اجراشده

- `MathNode.Symbol` بازهٔ `sourceStart/sourceEnd/editable` دارد.
- parser برای plain text، command، تابع، Unicode مکمل، گروه خالی و سلول ماتریس offset دقیق نگه می‌دارد.
- `MathSvgEditBox` مختصات جعبه را در فضای SVG نگه می‌دارد.
- SVG حالت ویرایش برای هر مقدار `<rect>` تولید می‌کند.
- جعبه عادی از outline پوسته و جعبه فعال از primary پوسته استفاده می‌کند.
- `NativeFormulaEditorView` با `detectTapGestures` جعبه لمس‌شده را hit-test می‌کند.
- BasicTextField نامرئی از `fillMaxSize` به `1dp` محدود شد و دیگر لمس SVG را نمی‌پوشاند.
- مقدارهای plain مجاور مانند `123` به یک جعبهٔ واحد merge می‌شوند.
- فرمول و گروه خالی نیز خانهٔ قابل لمس دارند.
- `FormulaBoxEditor` درج، replace، انتخاب خانه فعال و حرکت امن میان خانه‌ها را متمرکز می‌کند.
- کتابخانه در selection جمع‌شده نیز جعبهٔ رنگی را کامل پیدا و جایگزین می‌کند.
- اولین خانهٔ قالب درج‌شده خودکار فعال می‌شود.
- فلش‌های keypad بین خانه‌ها حرکت می‌کنند و دیگر وارد متن فرمان TeX نمی‌شوند.
- گروه والد دستهٔ جاری selected است و شمار ورودی زیرگروه‌ها نمایش داده می‌شود.
- Loader یکتایی category/group، اعتبار linkها، خالی نبودن دسته‌ها و کامل بودن label/tex را بررسی می‌کند.

### رادیکال

- endpoint خط افقی از عرض واقعی radicand محاسبه می‌شود.
- metadata تستی `MathSvgRadicalBar` ابتدا/انتهای خط را ثبت می‌کند.
- با عدد، حرف، نماد، توان یا کسر بلندتر، endpoint تا بعد از آخرین جعبه جابه‌جا می‌شود.
- Canvas/PDF نیز از قبل عرض body را اندازه می‌گیرد و رفتار برداری حفظ شده است.

### ممیزی کتابخانه

```text
دسته یکتا                         77/77
پیوند گروه معتبر                 75/75
نماد Unicode                   1200/1200
رندر SVG مرجع                 2118/2118
درج در خانهٔ فعال             2118/2118
پیوند شکسته                           0
دسته غیرقابل دسترسی                  0
ورودی ناقص label/tex                  0
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              73/73 PASS
Touch/source-range tests                  PASS
Active color tests                        PASS
Empty-box tests                           PASS
Safe box navigation                      PASS
Radical overbar stretch                  PASS
Supplementary Unicode                    PASS
FINAL_NATIVE_VERIFY                      PASS
lintDebug                     PASS (0 error, 24 warning)
assembleDebug                            PASS
APK v2 signature                      Verified
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V13.5
```

راهنمای مستقل: `FORMULA_STRUCTURAL_BOXES_V13_6_FA.md`.

---

## ۳۰) V14 — برابری کامل قابلیت‌های فرمول فایل `exam-system.html`

### ورودی ممیزی

```text
exam-system.html
SHA-256: 591dd9feffecea45fe423633643cd518c1db9cad547bbfda709ad93f2cd7e6a1
Size: 1,540,210 bytes / 32,099 lines
```

ممیزی کامل در `FORMULA_FEATURE_AUDIT_EXAM_SYSTEM_FA.md` ثبت شد. HTML، `math.js` و `mathedit.js` فایل جدید با مرجع قبلی byte-identical بودند، اما قابلیت‌های پنهان و باگ‌های runtime نیز شناسایی شدند.

### دامنهٔ تحویل

- Smart Hub واقعی با trigger؛
- ۵ درس، ۳۰ قالب، ۸ بسته، ۶ دلیمتر، ۸ کلید درشت؛
- آخرین فرمول، recent قابل حذف، favorites و recent symbols؛
- سه mode همیشه قابل دسترسی؛
- gallery همراه recent؛
- search و نام نمادها؛
- تایپ طبیعی کامل و chemistry normalization؛
- `x^2^3`, fraction grouping, relations, arrows؛
- اصلاح `⇌ → \rightleftharpoons`؛
- custom matrix picker ۱..۱۰ و شش environment؛
- انتخاب جداگانهٔ دلیمتر باز/بسته؛
- slash-to-fraction و structural IME؛
- spatial navigation، Tab/Home/End و Ctrl shortcuts؛
- auto-scroll دوبعدی خانه فعال؛
- safe clipboard import؛
- long-press favorites با haptic؛
- پشتیبانی `sfrac/nicefrac/root/underline/widehat/mbox/quad/qquad`؛
- استخراج/ویرایش/حذف مستقیم `$...$` در سؤال، option و دو ستون matching؛
- SVG Native در همهٔ نمایش‌ها و PDF/Canvas AST مشترک.

### اصلاح باگ‌های مرجع

```text
mode/search/footer hidden              → visible Native controls
Smart Hub without trigger              → full-screen reachable dialog
Unicode 1200 showing ~139              → exact 1200 direct entries
mostly HTML/text math                   → SVG rendering
1s/2s long-press mismatch              → Android standard + haptic
broken equilibrium quick conversion    → valid command
matrix cursor restore bug              → source ranges + geometry
manual exact $...$ selection           → direct occurrence editor
fake lazy loading                      → no JS loader
duplicate JS functions                 → one Kotlin path
body-wide MutationObserver             → scoped recomposition
raw unknown command                    → safe visual fallback
```

### دادهٔ قطعی

```text
main groups                 8
category links             75
categories                 77
library entries          2084
Unicode                  1200
fixed gallery formulas     34
Smart lessons               5
Smart templates            30
Smart packs                 8
Smart delimiters            6
Smart big keys              8
```

### امنیت

- JavaScript/WebView صفر؛
- SVG memory-only و XML-escaped؛
- allowlist element؛
- بدون URL/href/src/script/foreignObject؛
- بدون SQL/Edge/Secret/Migration/Dependency جدید.

### تست

```text
Kotlin compile                         PASS
JVM tests                              88/88 PASS
Reference SVG + insertion           2118/2118 PASS
Unicode                              1200 PASS
Matrix sizes 1..10                    100 PASS
Smart data/count/link integrity          PASS
Natural + chemistry converter            PASS
Inline occurrence edit/delete            PASS
Structural typing + spatial nav           PASS
FINAL_NATIVE_VERIFY                      PASS
lintDebug                     PASS (0 error, 24 warning)
assembleDebug                            PASS
APK Signature Scheme v2               Verified
```

راهنمای مستقل: `FORMULA_COMPLETE_PARITY_V14_FA.md`.

---

## ۳۱) V14.1 — Hotfix کتابخانه‌های فرمول

### گزارش دستگاه

کاربر پس از Build موفق V14 اعلام کرد کتابخانه‌های فرمول کار نمی‌کنند.

### علت

```text
JSON/library data             سالم
TeX parser/insertion tests    سالم
UI visibility route          معیوب
```

کلیک گروه/دسته فقط state دسته را تغییر می‌داد، اما Grid نمادها پایین‌تر از keypad داخل scroll باقی می‌ماند. هیچ کتابخانه‌ای بلافاصله روی صفحه باز نمی‌شد؛ بنابراین رفتار واقعی دستگاه با تست داده پوشش داده نشده بود.

### اصلاح

- navigator خالص برای تمام routeهای کتابخانه؛
- Dialog تمام‌صفحه با search و count؛
- بازشدن فوری common/all/unicode/recent/favorites/letters؛
- بازشدن فوری همه ۷۵ زیرگروه؛
- استفاده Smart Hub از همان مسیر؛
- کارت clickable + دکمهٔ صریح «درج»؛
- long-press favorite؛
- پیام موفقیت پس از درج؛
- empty/error state قابل مشاهده؛
- decoder مشترک Runtime/Test.

### تست

```text
Groups                            8/8 PASS
Category links                  75/75 PASS
Categories                      77/77 PASS
Unicode direct route         1200/1200 PASS
Reference render/insert      2118/2118 PASS
JVM tests                        91/91 PASS
FINAL_NATIVE_VERIFY                PASS
lintDebug             PASS (0 error, 24 warning)
assembleDebug                      PASS
APK v2 signature                Verified
```

SQL، Edge Function، Secret، Migration یا dependency جدید ندارد.

راهنمای مستقل: `FORMULA_LIBRARY_RUNTIME_HOTFIX_V14_1_FA.md`.

---

## ۳۲) V15 — نوار ثابت پایین معلم

### درخواست

```text
۵ دکمه راست‌به‌چپ:
منو / کیف پول / + / آزمون‌ها / کارت‌ها

+ → کمان دانش‌آموز جدید / آزمون جدید / کلاس جدید
کارت‌ها → آمار / تصحیح / مانده
```

### پیاده‌سازی

- `TeacherBottomDock` کاملاً Native Compose؛
- RTL صریح؛
- dock ثابت در Scaffold معلم؛
- gradient، shadow، active indicator، scale، rotation و arc animation؛
- drawer واقعی از دکمه راست؛
- WalletScreen واقعی؛
- Builder واقعی؛
- SchoolManagement با `SchoolLaunchAction.CREATE_STUDENT/CREATE_CLASS`؛
- TeacherDashboard به‌عنوان مدیریت آزمون‌ها؛
- ModalBottomSheet سه کارت؛
- ReportsScreen برای آمار؛
- GradingScreen برای تصحیح؛
- pending-only filter برای مانده؛
- دانش‌آموز بدون تغییر و با top hamburger قبلی؛
- Builder بدون dock برای جلوگیری از مزاحمت.

### تست

```text
Dock order RTL                    PASS
Five main actions                 PASS
Three arc actions                 PASS
Three management cards            PASS
Real route wiring                 PASS
Pending-only grading              PASS
Kotlin compile                    PASS
JVM tests                         93/93 PASS
FINAL_NATIVE_VERIFY              PASS
lintDebug               PASS (0 error, 24 warning)
assembleDebug                    PASS
APK v2 signature              Verified
```

SQL، Edge Function، Secret، Migration و dependency جدید ندارد.

راهنمای مستقل: `DYNAMIC_TEACHER_BOTTOM_DOCK_V15_FA.md`.

---

## ۳۳) V16 — ادغام Native طرح نئومورفیک ۶۹

### مرجع دریافتی و اعتبارسنجی

```text
Archive: neumorphic69-compose_all.tar.gz
SHA-256: 0bb34550506669b4ec3a0f07fdb2e43b6176c66b22fd767133e4488bd35b7fa3
Size: 270642 bytes
Files: 20
Directories: 16
Unsafe paths: 0
Kotlin lines: 1502
```

مرجع یک پروژه Compose مستقل با package آزمایشی و چند صفحه demo بود. ادغام مستقیم پروژه، Gradle، Manifest، داده کیف پول، آزمون‌ها و کارت‌های ساختگی ممنوع شد. فقط زبان طراحی، سایه‌ها، پالت‌ها و وزن‌های واقعی فونت استخراج و به state و routeهای واقعی سامانه متصل شدند.

### تحویل واقعی

```text
Neumorphic69Design با سایه روشن/تیره Canvas Native
حالت raised/pressed و انیمیشن لمس
authenticated shell صریح RTL
Drawer سمت راست با پروفایل و routeهای واقعی
سربرگ نئومورفیک و adaptive width تا 900dp
بازطراحی icon-only نوار V15 بدون تغییر عملکرد
کمان دانش‌آموز/آزمون/کلاس واقعی
Sheet نئومورفیک آمار/تصحیح/مانده واقعی
کارت موجودی گرادیانی با balance واقعی و hide/show
پنل شارژ و transaction واقعی
پنل آزمون‌های واقعی معلم
پنل join و pending queue واقعی دانش‌آموز
چهار پالت + depth 8..22 ماندگار در DataStore
تم روشن/تیره/dynamic سازگار
وزن Medium/Bold واقعی Vazirmatn
mode اجرایی 100755 برای gradlew
```

### فایل‌های V16

```text
NEUMORPHIC69_NATIVE_INTEGRATION_V16_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/core/ui/AppearancePreferences.kt
app/src/main/java/ir/exam/app/core/ui/ExamAppTheme.kt
app/src/main/java/ir/exam/app/core/ui/PersianFonts.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt
app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt
app/src/main/res/font/vazirmatn_medium.ttf
app/src/main/res/font/vazirmatn_bold.ttf
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
gradlew
scripts/verify_native_final.py
```

### مواردی که حفظ شدند

- تمام قابلیت‌های V1 تا V15 و Hotfixهای فرمول؛
- ترتیب و رفتار واقعی dock V15؛
- student بدون dock پایین و با menu بالایی؛
- Builder بدون dock؛
- Auth، session، Supabase، Room، WorkManager، چاپ و بروزرسانی؛
- dark/light، dynamic colors، font scale و انتخاب فونت قبلی؛
- عدم استفاده از WebView/JavaScript؛
- عدم ورود داده demo یا placeholder به Runtime.

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز Patch: V14.1 + V15
Build نهایی: GitHub Actions
```

### تست V16

```text
Kotlin compile                         PASS
JVM tests                              97/97 PASS
Neumorphic design/contract regression PASS
Reference font SHA regression         PASS
No demo package/data regression       PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 24 warning
assembleDebug                         PASS
Debug package                         ir.exam.app.native
APK Signature Scheme v2               Verified
Debug APK SHA-256                     975d79e127a749209e1999be03d84e6f28dd92e7dfc6d21eba7c106dbd6a37df
```

راهنمای مستقل: `NEUMORPHIC69_NATIVE_INTEGRATION_V16_FA.md`.

---

## ۳۴) V16.1 — شبکه کارت‌های Drawer و Pull-to-Refresh

### درخواست دستگاه/کاربر

```text
پروفایل اولین و بزرگ‌ترین کارت Drawer باشد.
بقیه کارت‌ها دو ستونه و هر ردیف دقیقاً دو کارت باشد.
دکمه + دایره کامل شود.
دکمه بروزرسانی داشبورد حذف و Pull-to-Refresh جایگزین شود.
```

### پیاده‌سازی

- کارت پروفایل واقعی با ارتفاع ۱۴۸dp، عرض کامل و مسیر مستقیم ProfileSettings؛
- کارت‌های منو با ارتفاع ۱۱۶dp و شبکه ثابت دو ستونه؛
- ۱۰ کارت معلم در ۵ ردیف کامل؛
- ۶ کارت دانش‌آموز در ۳ ردیف کامل؛
- کارت «آزمون جدید» واقعی برای زوج‌ماندن آخرین ردیف معلم؛
- همه مسیرهای Dashboard، Calendar، School، Grading، Reports، Wallet، Settings، About، Builder و Sign-out واقعی؛
- دکمه مرکزی ۷۰×۷۰dp با radius=35dp و لایه داخلی `CircleShape/clip=true`؛
- حذف کامل دکمه متنی بروزرسانی از داشبورد معلم؛
- `PullToRefreshBox` متصل مستقیم به `viewModel.load`؛
- تبدیل داشبورد به یک LazyColumn واحد برای gesture صحیح و جلوگیری از scroll تو در تو.

### فایل‌ها

```text
NEUMORPHIC69_DRAWER_REFRESH_V16_1_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
scripts/verify_native_final.py
```

### عملیات

```text
SQL/Edge/Secret/Migration/Dependency جدید: ندارد
پیش‌نیاز: V16
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              99/99 PASS
Drawer profile/grid contract           PASS
Perfect circle regression              PASS
Pull-to-refresh regression             PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 24 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     9e5ea5d0276bb833f36cf45c1ce3e9c2ea47e1aca1fbe4b2ad4ea1f9a84690f9
```

راهنمای مستقل: `NEUMORPHIC69_DRAWER_REFRESH_V16_1_FA.md`.

---

## ۳۵) V17 — رفتار کامل Native مرجع design-69

### مرجع

```text
design-69.html
SHA-256: 8b1970f5317a6736ba20b1c239d6457e8f6739e7222d937a6d384329a95d4ecf
Size: 61,677 bytes
Lines: 467
```

ممیزی کامل HTML/CSS/JavaScript در `DESIGN_69_BEHAVIOR_AUDIT_FA.md` ثبت شد. فایل مرجع Self-contained و فاقد external resource/secret بود، اما فقط مرجع طراحی است و هیچ JavaScript یا WebView آن وارد Runtime نشد.

### تصمیم‌های کاربر

```text
منوی تمام‌صفحه + برگشت به آخرین صفحه
+ متحرک تا مرکز با عملیات واقعی دانش‌آموز/آزمون/کلاس
کارت مدیریتی آمار/تصحیح/مانده با swipe چهارجهته
۱۰ کارت واقعی معلم و ۶ کارت واقعی دانش‌آموز
AboutScreen واقعی و AlertDialog امن خروج
ImageVector/Canvas خطی + همه micro-animationهای مرجع
refresh واقعی روی لمس دوباره مقصد فعال
```

### قابلیت‌ها

- حذف `ModalNavigationDrawer` و جایگزینی با full-page menu state-preserving؛
- morph همبرگر به × طی 420ms برای پایین معلم و بالای دانش‌آموز؛
- کارت پروفایل 148dp و کارت منو 116dp؛
- grid دو ستونه کامل با stagger 120ms + 40ms؛
- مجموعه خطی Native برای Wallet/Add/Exams/Cards/Calendar/Classes/Students/Update/Settings/Logout/Reports/Grading/Dashboard/quick actions؛
- flip کیف پول، bounce آزمون، wiggle کارت، lift/dot فعال و ripple 520ms؛
- دکمه + دایره 70dp با travel 620ms، rotation 135° و گزینه‌های مثلثی؛
- صفحه stack کارت‌های مدیریتی با threshold 52dp، چهار جهت، بازگشت لغو، keyboard و مسیر واقعی؛
- active Wallet/Exams/Cards به‌ترتیب refresh/refresh/cycle؛
- PullToRefresh داشبورد و حذف دکمه بروزرسانی؛
- نمایش‌ندادن دامنه داخلی Auth دانش‌آموز؛
- حفظ تم، پالت، عمق، Builder، Auth، Room، Supabase و همه قابلیت‌های V1..V16.

### فایل‌ها

```text
DESIGN_69_BEHAVIOR_AUDIT_FA.md
DESIGN_69_NATIVE_BEHAVIOR_V17_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt
app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
app/src/test/java/ir/exam/app/ui/app/TeacherBottomDockTest.kt
scripts/verify_native_final.py
```

### عملیات

```text
پیش‌نیاز: V16
V16.1 جداگانه لازم نیست؛ تغییرات آن داخل V17 است.
SQL/Edge/Secret/Migration/Dependency جدید: ندارد
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              101/101 PASS
Design69 behavior/source regression    PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 24 warning
assembleDebug                         PASS
Debug package                         ir.exam.app.native
APK Signature Scheme v2               Verified
Debug APK SHA-256                     bab45f4cfdadba570765886ceefb88758585e283eb37cede96717545f8523c92
```

راهنمای مستقل: `DESIGN_69_NATIVE_BEHAVIOR_V17_FA.md`.

---

## ۳۶) V18 — ناوبری، حساب، سربرگ و مدیریت پنج‌کارت

### درخواست

- هاله فعال dock کوچک‌تر، + کوچک و هم‌سطح dock و بدون رد هنگام انتقال؛
- پنج کارت آمار/بانک سؤال/تصحیح/مانده/پاسخ؛
- توضیح کارت فعال زیر stack و حذف ردیف دکمه‌های تکراری؛
- کارت آزمون خلاصه و عملیات expand-on-tap؛
- حذف عنوان‌های تکراری؛
- منوی دقیق پروفایل/تقویم/کلاس/دانش‌آموز/سربرگ/تنظیمات/خروج؛
- تنظیمات ظاهر/حساب/داده‌ها/درباره؛
- سربرگ دارای پایه؛
- صفحه پیش‌فرض تقویم؛
- تفکیک پروفایل و حساب؛
- تغییر ایمیل تأییدشده؛
- قفل فقط با روش امن دستگاه.

### تحویل

```text
Active halo 44dp / plus 58dp centered
No bottom plus composable while shared add is open
Management cards 5 / no duplicate buttons
Standalone QuestionBank manager + owner update RPC
Grading pendingOnly + gradedOnly
Exam card accordion actions
Teacher textual top bar removed
Student compact 54dp menu-only bar
Teacher hamburger grid 6 exact destinations
Default MainPage.CALENDAR
Profile avatar + display name only
Header province/city/district/school/grade
Settings appearance/account/data/about
Account details/username/email/password/system lock
BiometricPrompt + DEVICE_CREDENTIAL only
Backup format v3 with header grade
```

### SQL

```text
supabase/migrations/20260813_native_navigation_account_v18.sql
supabase/tests/20260813_v18_integration.sql
```

Readiness: پنج مقدار true. SQL فقط روی پروژه اصلی `eazwuyrymsvdkwckdpco` اجرا شود.

### امنیت

- `native_bank_update_question_v1` فقط سؤال متعلق به `auth.uid()` را ویرایش می‌کند.
- امضای قدیمی `native_save_profile` حذف می‌شود تا PostgREST ambiguity نداشته باشد.
- email change فقط از Supabase Auth است و confirmation واقعی لازم دارد.
- PIN/Pattern سفارشی ذخیره نمی‌شود؛ BiometricPrompt رسمی Android روش دستگاه را انتخاب می‌کند.
- بقایای hash/salt PIN نسخه قبلی پاک می‌شوند.
- internal student Auth email نمایش داده نمی‌شود.
- UPDATE/DELETE بدون WHERE صفر.

### تست

```text
Kotlin compile                         PASS
JVM tests                              105/105 PASS
Migration parser                       PASS (23 statements)
Test SQL parser                        PASS (4 statements)
Unsafe DML                             0
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
Biometric packaged                    PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     8812bd60fae6489ece6485f3421a62416444db053f8cbfe20c963780c7374803
```

راهنمای مستقل: `NAVIGATION_ACCOUNT_MANAGEMENT_V18_FA.md`.

---

## ۳۷) V19 — تعامل هم‌زمان، آزمون‌ساز شعاعی و فرم دانش‌آموز

### قابلیت‌ها

- حرکت هم‌زمان + و سه گزینه واقعی؛ رسیدن هم‌زمان و رسم خط‌چین در ۱۲٪ پایانی؛
- auto-credit اتمیک و idempotent فقط در sandbox مجاز Edge Function؛
- هیچ credit RPC در APK؛
- کارت آزمون فشرده و accordion عملیات؛
- کارت‌های مدیریتی فقط swipe افقی و بدون متن راهنما؛
- برند «آزمون آنلاین» در manifest/UI/PDF/update؛
- Dialog افزودن تکی در بالا با چهار ردیف دو/سه‌ستونه و imePadding؛
- Dialog گروهی کارت‌بندی‌شده تا ۱۰۰ ردیف؛
- پیشنهاد `first_last` از نام فارسی و suffix برای تکراری‌ها؛
- Formula Editor صریح LTR و auto-scroll خانه فعال؛
- مشخصات آزمون expand/collapse؛
- + آزمون‌ساز متحرک تا مرکز، × قرمز و ۸ عمل شعاعی؛
- import فایل و انتخاب بانک سؤال از radial menu؛
- کارت سؤال accordion؛ همیشه یک کارت باز؛ کارت جدید auto-open و auto-scroll؛
- تیک شناور برای تأیید هزینه/ذخیره؛ bottom padding برابر 112dp؛
- حذف refresh دستی از تقویم، کلاس، دانش‌آموز، بانک، کیف پول و آزمون‌ها؛
- PullToRefreshBox در همه مسیرهای فوق؛
- جداسازی نمای کلاس و همه دانش‌آموزان از طریق منوی اصلی.

### امنیت sandbox

```text
PAY_PROVIDER=sandbox
PAY_ALLOW_SANDBOX=true
```

فقط در این حالت `wallet-payment` پس از ساخت سفارش و authority آزمایشی، `native_credit_wallet_payment` را سروری اجرا می‌کند. provider واقعی هیچ auto-credit ندارد.

### فایل‌های کلیدی

```text
INTERACTION_BUILDER_STUDENT_V19_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
supabase/functions/wallet-payment/index.ts
app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/PersianUsernameSuggester.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt
app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt
app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt
app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt
app/src/main/java/ir/exam/app/ui/billing/BillingViewModel.kt
app/src/main/java/ir/exam/app/ui/billing/WalletScreen.kt
app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt
app/src/test/java/ir/exam/app/ui/app/V19InteractionTest.kt
```

### عملیات

```text
SQL جدید: ندارد
Migration جدید: ندارد
Secret جدید: ندارد
Dependency Android جدید: ندارد
Edge deploy: wallet-payment لازم است
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              110/110 PASS
Persian username regression           PASS
Builder radial/accordion regression   PASS
Student compact/bulk regression       PASS
Formula LTR/auto-scroll regression    PASS
Sandbox server-credit regression      PASS
Deno check wallet-payment             PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     a0e9785ca749cc10f51bb8f4f708d1c3d30359290b0bcb9274bd8c25dc8631e2
```

راهنمای مستقل: `INTERACTION_BUILDER_STUDENT_V19_FA.md`.

---

## ۳۸) V20 — پالایش تعامل، رمز و اسکرول هوشمند

### تحویل

```text
Quick-add top = exam / right = student / left = class
Exam cards = exact two-line collapsed summary
Shared password eye in every password input
Bulk title removed + window icon
Bulk footer = green + / centered submit / red ×
Predictive formula scroll before viewport edge
Builder + and ✓ on opposite sides
Question card second tap closes
Question open scroll = exact item offset 0 below header
Management cards horizontal-only
All card drag helper text removed
```

### فایل‌های کلیدی

```text
INTERACTION_POLISH_V20_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/common/PasswordVisibility.kt
app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt
app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt
app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt
app/src/test/java/ir/exam/app/ui/app/V20InteractionPolishTest.kt
```

### عملیات

```text
SQL/Edge/Secret/Dependency جدید: ندارد
پیش‌نیاز: V19
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              115/115 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     1613f90adc4b162ac3dde17aba9fb0ca001a2a4364f60c3bf22410a97816382e
```

راهنمای مستقل: `INTERACTION_POLISH_V20_FA.md`.

---

## ۳۹) V21 — نوار دانش‌آموز و اسکرول دقیق سؤال

### تحویل

```text
Student toolbar: Excel / + / Search
Single-account button removed from student list
Animated search field below toolbar
Close clears query and restores search icon
Bulk top controls: green + / centered Create / red ×
Bulk table/title control removed
Material/Neumorphic button content centered
Formula scroll: 14% look-ahead, 62% horizontal target
Builder FABs at opposite physical sides
Question opening waits two frames
Exact animateScrollToItem(index, 0) below TopAppBar
```

### فایل‌های کلیدی

```text
STUDENT_LIST_BUILDER_SCROLL_V21_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/test/java/ir/exam/app/ui/app/V21StudentBuilderPolishTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              119/119 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     3c563f64d530e6b79d0360d116a425f18879272a063bfb0e274bfd40022a1254
```

SQL/Edge/Secret/Dependency جدید ندارد.
