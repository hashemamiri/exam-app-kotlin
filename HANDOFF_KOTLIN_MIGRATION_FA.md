# هندآف جامع مهاجرت سامانه آزمون از WebView به Native Kotlin

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۱۱ — پچ V3 نسخه‌گذاری خودکار GitHub Actions
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
```

### قابلیت‌های هنوز کامل نشده یا فقط اسکلت دارند

```text
OTP اختصاصی 8 رقمی
ویرایش آزمون ذخیره‌شده
حذف و تکثیر آزمون
مخاطبان آزمون
کلاس و دانش‌آموز واقعی
بانک سؤال واقعی
آپلود واقعی exam-images
برش/چرخش/فشرده‌سازی واقعی تصویر
تصویر پاسخ دانش‌آموز
فونت‌های واقعی res/font
PDF فارسی کامل و چندصفحه‌ای
چاپ Android کامل
تصحیح تشریحی کامل
گزارش، کارنامه و Excel واقعی
WorkManager و صف آفلاین واقعی
تقویم و پیام واقعی
کیف پول و پرداخت واقعی
انتشار خودکار APK در Storage و فعال‌سازی خودکار metadata نسخه (فعلاً دستی و امن)
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
- پچ V3 وابستگی workflow به آن دو Variable را حذف می‌کند؛ وجود یا حذف آن‌ها اثری روی Build ندارد.
- در V3، GitHub Actions در هر اجرا versionCode و versionName یکتا و بالاتر را از زمان UTC تولید می‌کند.
- Artifact V3 علاوه بر APK نام‌گذاری‌شده، فایل `update-metadata.txt` شامل نسخه، SHA-256 و اندازه واقعی دارد.
- برای بروزرسانی واقعی بعدی باید APK امضاشده در bucket `app-updates` آپلود و metadata آن فعال شود.
- مهم‌ترین کار بعدی: تکمیل ماژول‌های باقی‌مانده به‌صورت واقعی، نه فقط اسکلت.
- کاربر درخواست کرده قابلیت‌های باقی‌مانده در چهار پچ یکپارچه انجام شوند؛ اما هر پچ باید واقعاً نوشته، build و تست شود تا پچ صوری یا ناقص تحویل نشود.

---

## ۱۴) وضعیت قطعی ماژول بروزرسانی درون‌برنامه‌ای

### فایل‌های اصلی

```text
app/src/main/java/ir/exam/app/core/update/AppUpdateRepository.kt
app/src/main/java/ir/exam/app/core/update/UpdateUseCase.kt
app/src/main/java/ir/exam/app/core/update/ApkUpdateManager.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAppUpdateRepository.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/main/java/ir/exam/app/ui/update/UpdateViewModel.kt
app/src/main/res/xml/update_file_paths.xml
.github/workflows/android.yml
supabase/migrations/20260811_app_updates.sql
APP_UPDATE_SETUP_FA.md
```

### رفتار نهایی

```text
منوی همبرگری → درباره و بروزرسانی
بررسی app_version با RLS
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
ثبت ردیف انتشار فعال: هنوز باید برای APK واقعی انجام شود
```

### قانون نسخه

- نسخه Release اصلی فقط با package `ir.exam.app` و همان keystore قبلی قابل جایگزینی است.
- `versionCode` APK جدید باید از نسخه نصب‌شده و مقدار ردیف قبلی بیشتر باشد.
- در GitHub Actions، `APP_VERSION_CODE` به‌صورت خودکار از تعداد ثانیه‌های گذشته از `2020-01-01 UTC` ساخته می‌شود.
- در GitHub Actions، `APP_VERSION_NAME` از تاریخ و ساعت UTC ساخت با قالب `YYYY.MM.DD.HHMMSS-native` ساخته می‌شود.
- Variableهای GitHub با نام `APP_VERSION_CODE` و `APP_VERSION_NAME` دیگر خوانده نمی‌شوند و نیاز به ویرایش ندارند.
- Build محلی خارج از CI همچنان fallback برابر `3` و `1.1.1-native` دارد.
- هر Artifact شامل APK با نام نسخه‌دار و فایل `update-metadata.txt` است.
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

### قانون دائمی هندآف

از این پچ به بعد، هر پچ تحویلی باید فایل زیر را نیز به‌روزرسانی کند:

```text
HANDOFF_KOTLIN_MIGRATION_FA.md
```

هندآف باید حداقل شامل وضعیت قابلیت جدید، فایل‌های تغییرکرده، SQL اجراشده یا باقی‌مانده، نسخه Build و نتیجه build/test باشد.
