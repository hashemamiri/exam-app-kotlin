# هندآف جامع مهاجرت سامانه آزمون از WebView به Native Kotlin

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۲۴ — V55.2 برچسب نسخهٔ پل فرمول + تشخیص‌های صریح؛ پیش از آن: V55.1
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

### قانون قطعی خصوصی‌سازی و پاک‌سازی workspace

- ریپوی Kotlin تا این لحظه **عمومی (public)** است؛ کاربر قصد دارد آن را **private** کند.
- پیش از خصوصی‌شدن، باید **همهٔ سورس لازم برای تغییرات و آپدیت‌های بعدی** یک‌بار
  کلون و در workspace بازسازی شود تا کار ادامه‌یابد:

```bash
cd ~
git clone https://github.com/hashemamiri/exam-app-kotlin.git exam-app-kotlin
cd exam-app-kotlin && git fetch --unshallow
```

- workspace باید **همیشه تمیز** بماند: هیچ فایل موقت، لاگ، بخش (part)، آرشیو
  میانی یا سورس تکراری نباید جا بماند؛ پس از هر تحویل پچ، فقط سورس پروژه،
  پچ و هندآف باقی بماند.
- پس از خصوصی‌شدن، دریافت تازهٔ سورس فقط با دسترسی معتبر (token/SSH) ممکن است؛
  بنابراین کپی محلی/workspace مرجع اصلی ادامهٔ کار است.

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
به‌روزرسانی اجباری docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md در همان پچ
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
docs/fa/APP_UPDATE_SETUP_FA.md
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
docs/fa/AUTH_SESSION_PERSISTENCE_FA.md
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
docs/fa/COMPREHENSIVE_NATIVE_PATCH_FA.md
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
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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
docs/fa/COMPREHENSIVE_CALENDAR_PROFILE_WALLET_V9_FA.md
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
docs/fa/COMPREHENSIVE_OFFLINE_PORTABILITY_PRINT_V10_FA.md
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
docs/fa/FINAL_NATIVE_HARDENING_V11_FA.md
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
docs/fa/CRITICAL_NATIVE_FLOWS_V12_FA.md
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

راهنمای مستقل: `docs/fa/FORMULA_SVG_RENDERING_V13_5_FA.md`.

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

داده‌های جدید کلاس/آزمون با trigger و دانش‌آموز با school_students به مدرسه scope می‌شوند. داده‌های قدیمی school_id=null باقی می‌مانند و بدون انتخاب مدیر وارد آمار مدرسه نمی‌شوند.

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V13.5
```

راهنمای مستقل: `docs/fa/FORMULA_STRUCTURAL_BOXES_V13_6_FA.md`.

---

## ۳۰) V14 — برابری کامل قابلیت‌های فرمول فایل `exam-system.html`

### ورودی ممیزی

```text
exam-system.html
SHA-256: 591dd9feffecea45fe423633643cd518c1db9cad547bbfda709ad93f2cd7e6a1
Size: 1,540,210 bytes / 32,099 lines
```

ممیزی کامل در `docs/fa/FORMULA_FEATURE_AUDIT_EXAM_SYSTEM_FA.md` ثبت شد. HTML، `math.js` و `mathedit.js` فایل جدید با مرجع قبلی byte-identical بودند، اما قابلیت‌های پنهان و باگ‌های runtime نیز شناسایی شدند.

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

راهنمای مستقل: `docs/fa/FORMULA_COMPLETE_PARITY_V14_FA.md`.

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

راهنمای مستقل: `docs/fa/FORMULA_LIBRARY_RUNTIME_HOTFIX_V14_1_FA.md`.

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

راهنمای مستقل: `docs/fa/DYNAMIC_TEACHER_BOTTOM_DOCK_V15_FA.md`.

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
docs/fa/NEUMORPHIC69_NATIVE_INTEGRATION_V16_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

راهنمای مستقل: `docs/fa/NEUMORPHIC69_NATIVE_INTEGRATION_V16_FA.md`.

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
docs/fa/NEUMORPHIC69_DRAWER_REFRESH_V16_1_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

راهنمای مستقل: `docs/fa/NEUMORPHIC69_DRAWER_REFRESH_V16_1_FA.md`.

---

## ۳۵) V17 — رفتار کامل Native مرجع design-69

### مرجع

```text
design-69.html
SHA-256: 8b1970f5317a6736ba20b1c239d6457e8f6739e7222d937a6d384329a95d4ecf
Size: 61,677 bytes
Lines: 467
```

ممیزی کامل HTML/CSS/JavaScript در `docs/fa/DESIGN_69_BEHAVIOR_AUDIT_FA.md` ثبت شد. فایل مرجع Self-contained و فاقد external resource/secret بود، اما فقط مرجع طراحی است و هیچ JavaScript یا WebView آن وارد Runtime نشد.

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
docs/fa/DESIGN_69_BEHAVIOR_AUDIT_FA.md
docs/fa/DESIGN_69_NATIVE_BEHAVIOR_V17_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

راهنمای مستقل: `docs/fa/DESIGN_69_NATIVE_BEHAVIOR_V17_FA.md`.

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

راهنمای مستقل: `docs/fa/NAVIGATION_ACCOUNT_MANAGEMENT_V18_FA.md`.

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
docs/fa/INTERACTION_BUILDER_STUDENT_V19_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

راهنمای مستقل: `docs/fa/INTERACTION_BUILDER_STUDENT_V19_FA.md`.

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
docs/fa/INTERACTION_POLISH_V20_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

راهنمای مستقل: `docs/fa/INTERACTION_POLISH_V20_FA.md`.

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
docs/fa/STUDENT_LIST_BUILDER_SCROLL_V21_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
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

---

## ۴۰) V22 — منوی کلاس و کارت تعاملی دانش‌آموز

### تحویل

```text
Main + student → BulkStudentDialog
Class title + grade in one row
Class + → hanging Existing/New cards
Existing picker gender + grade filters
New → bulk dialog, 1..100 rows
Student card summary name + grade
Female pink / male blue
Independent card expand/collapse
Green/red active toggle + edit pencil + class add
Atomic multi-class membership RPC
Secure optional new password; old password never retrievable
Hamburger: Students / Classes / Calendar
```

### امنیت رمز

رمز قبلی Supabase Auth قابل بازیابی نیست و `plain_password` بازنمی‌گردد. در ویرایش، رمز جدید اختیاری است؛ خالی یعنی بدون تغییر.

### SQL

```text
sql/manual/SQL_NATIVE_STUDENT_MULTI_CLASS_V22.sql
supabase/migrations/20260814_native_student_class_membership_v22.sql
supabase/tests/20260814_v22_integration.sql
```

Readiness: `student_multi_class_ready=true`.

### تست

```text
Kotlin compile                         PASS
JVM tests                              124/124 PASS
Migration parser                       PASS (6 statements)
Test SQL parser                        PASS (4 statements)
Unsafe DML                             0
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     0ea902fe1eb9f6c42b9f32a82dfc733016583afca8f26ba08994d0e01b538b65
```

راهنمای مستقل: `docs/fa/CLASS_STUDENT_CARDS_V22_FA.md`.

---

## ۴۱) V23 — اصلاح تیک، مرکزچین کلاس و انتخاب‌گر چرخشی پایه

### تحویل

```text
Builder save FAB: centered Scaffold slot + bounded native Check icon
Class card action buttons centered
Class + and hanging Existing/New actions centered full-width
Student action touch targets: 58dp; icons: 30/32dp
Student profile copy icon with explicit non-retrievable password notice
Female/Male filters toggle back to All on second tap
Per-grade chips removed from existing-student picker
Shared vertical snapping GradeOdometerPicker
Grade odometer in class, member filter, student edit, bulk rows and official header
Standard preschool..grade 12 order + custom legacy value preservation
```

### امنیت کپی رمز

Supabase Auth رمز فعلی را hash می‌کند و `plain_password` حذف‌شده بازنمی‌گردد. دکمه کپی کارت همه اطلاعات قابل بازیابی و نام کاربری را کپی می‌کند، اما برای رمز قبلی صریحاً «قابل بازیابی نیست» می‌نویسد. رمز plaintext فقط در جریان یک‌بارنمایش ساخت حساب یا تعیین رمز جدید قابل تحویل است.

### فایل‌های کلیدی

```text
docs/fa/INTERACTION_GRADE_ODOMETER_V23_FA.md
app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/test/java/ir/exam/app/ui/app/V23InteractionGradeOdometerTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              130/130 PASS
V23 interaction/grade tests            6/6 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                              PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     115c9c92bebd69be9f403659558292cb6c82e48e87f25045159be81a32e628a7
```

SQL/Edge/Secret/Dependency جدید ندارد.

---

## ۴۲) V24 — تقویم، منو، آزمون‌ساز و ویرایش تصویر

### تحویل

```text
Friday: red-only; no «تعطیل: جمعه» message row
Fast hamburger: 110ms shell fade / 180ms icon morph / no nested stagger
Teacher menu 8 cards; student menu 6 cards
Independent Account and Data destinations
Account cards independently expand/collapse
About: Persian V18..V24 changelog + compact update controls
Compact grade field opens a snapping five-row wheel dialog
Exam Start/End in one colored row
Jalali calendar + left hour / right minute + check/now/cancel
Shuffle question/option bold chips in one row
Long-press drag question reorder + live numbers
Neon numeric question badge; type/score/eye/drag in header
Print-layout controls gated by eye icon
Centered bold case-sensitive chip
Question image management LazyRow + non-overlapping initial positions
Shared image editor: rotate left / square crop / rotate right
Movable square crop with four draggable edges
Green check / live estimated size / red close bottom row
No recoverable old password; successful new password one-time sensitive copy
```

### امنیت رمز

رمز قبلی Supabase Auth یک hash یک‌طرفه است و قابل نمایش یا بازیابی نیست. `plain_password` بازنگشته است. اگر معلم رمز جدید تعیین کند، پس از موفقیت سرور فقط همان رمز جدید در حافظه نشست نمایش داده و با Clipboard حساس قابل کپی می‌شود؛ بستن پنجره آن را از state حذف می‌کند.

### فایل‌های کلیدی

```text
docs/fa/COMPREHENSIVE_UX_V24_FA.md
app/src/main/java/ir/exam/app/ui/calendar/CalendarScreen.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt
app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/test/java/ir/exam/app/ui/app/V24ComprehensiveUxTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              138/138 PASS
V24 comprehensive UX tests              8/8 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                              PASS — 0 error, 21 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     1e6b84f426b2395b886ad9d7271547c9273f613e686bf3649e285759c78a3fec
```

راهنمای مستقل: `docs/fa/COMPREHENSIVE_UX_V24_FA.md`.

SQL/Edge/Secret/Dependency جدید ندارد.

---

## ۴۳) V25 — هدر سراسری و پرداخت ایمن رابط

### تحویل

```text
Shared TopAppBar for every authenticated destination
Old password remains non-retrievable; one-time successful new password only
Nested hamburger animations restored with compact 20+18ms stagger
Menu title beside icon; unchanged 116dp card height
Calendar title / compact student and class subtitles
Now updates hour/minute only; trash clears selected boundary
Remote Persian notes only while an actual update is downloading
Bulk dialog bottom tangent to IME with dynamic list height
Minimal 62x40 score field
Blank new-exam negative marking and attempt cooldown
Centered bold timeout chip / attempts / grade policy
Drag-active question card color + 260ms animateItem placement
Confirmed student-account delete icon
Sampled image decoding: 2600 edge / 7M pixels / OOM guard
```

### امنیت رمز

رمز قبلی Supabase Auth قابل بازیابی نیست و هیچ ذخیره قابل‌بازیابی جدیدی ساخته نشده است. `plain_password` همچنان ممنوع و حذف‌شده است. تنها رمز جدیدی که سرور با موفقیت ثبت کرده، همان یک‌بار در حافظه نشست و Clipboard حساس قابل تحویل است.

### فایل‌های کلیدی

```text
docs/fa/HEADER_SAFETY_POLISH_V25_FA.md
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/test/java/ir/exam/app/ui/app/V25HeaderSafetyPolishTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              148/148 PASS
V25 header/safety/polish tests          10/10 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                              PASS — 0 error, 21 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     834b671c03df33333d6ccff372dc2198391e3f35cb63e3d0f06a0f036f643c1b
```

راهنمای مستقل: `docs/fa/HEADER_SAFETY_POLISH_V25_FA.md`.

SQL/Edge/Secret/Dependency جدید ندارد.

---

## ۴۴) V26 — بازه معتبر، گزینه‌های Drag و تصویر امن

### تحویل

```text
Hide shared TopAppBar while hamburger menu is open
Old password still non-retrievable; no recoverable vault/plain_password
Now fills current Jalali date + hour + minute without auto-confirm
End minimumIso=start + disabled earlier dates + time validation
ViewModel and repository enforce end >= start
Activity/Dialog explicit adjustResize + IME-tangent bulk Surface
Image raw URI preflight to safe sampled local file before preview
Final image edit runs only on safeSource
Question drag closes every accordion first
Multiple choice: Persian letters + formula/camera/thumbnail/drag toolbar
No multiple-choice direction arrows or numeric option labels
Matching: right letters first/top; left Persian numbers second/bottom
No matching direction arrows; per-item formula/camera/drag toolbar
Text and option image thumbnails compact 30dp beside camera
```

### امنیت رمز

Supabase Auth رمز قبلی را فقط به‌صورت hash نگه می‌دارد. ذخیره قابل‌بازیابی جدید ساخته نشده و `plain_password` برنگشته است. فقط رمز جدید ثبت‌شده با موفقیت، همان یک‌بار از حافظه نشست قابل کپی است.

### فایل‌های کلیدی

```text
docs/fa/QUESTION_MEDIA_REORDER_V26_FA.md
app/src/main/AndroidManifest.xml
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/builder/JalaliDateTimePicker.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V26QuestionMediaReorderTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              157/157 PASS
V26 question/media/reorder tests         9/9 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                              PASS — 0 error, 21 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     a134841c1e433edefacf3181ec0e6339cc06a3fdd012decc5bc6ad0d0810235b
```

راهنمای مستقل: `docs/fa/QUESTION_MEDIA_REORDER_V26_FA.md`.

SQL/Edge/Secret/Dependency جدید ندارد.

---

## ۴۵) V27 — داده، تصویر امن و گزینه‌های زنده

### تحویل

```text
Runtime «چهارگزینه‌ای» → «چندگزینه‌ای»
Stable option/matching editor IDs preserved in local drafts
Live 46dp-threshold option and matching reorder with key(id)
Raw image picker never auto-opens editor
Question/option/profile images preprocess before state/editor
Bulk Surface opens TopCenter and fills IME-reduced maxHeight
Installed version visible in About
Remote Persian notes visible before/during download, hidden after
Selected hamburger card full accent tint; dash marker removed
Scrollable Data section
Full-width Storage check/cleanup controls
Real 8MB ExamPackageCodec import into Native builder
Grade wheel Other item + custom text field
No recoverable old-password storage
```

### امنیت رمز

رمز قبلی Supabase Auth قابل بازیابی نیست. هیچ Vault قابل‌بازیابی یا `plain_password` ایجاد نشده است. فقط رمز جدیدی که سرور با موفقیت ثبت کرده، همان یک‌بار در حافظه نشست قابل کپی حساس است.

### فایل‌های کلیدی

```text
docs/fa/DATA_IMAGE_OPTIONS_V27_FA.md
app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/main/java/ir/exam/app/ui/app/Neumorphic69Design.kt
app/src/main/java/ir/exam/app/ui/portability/DataPortabilitySection.kt
app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt
app/src/test/java/ir/exam/app/ui/app/V27DataImageOptionsTest.kt
scripts/verify_native_final.py
```

### تست

```text
Kotlin compile                         PASS
JVM tests                              166/166 PASS
V27 data/image/options tests             9/9 PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                              PASS — 0 error, 21 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     23ba454a550db58422d467f121d9aa8cdb3d429f99315fdaa6088e6fd4db6e4d
```

راهنمای مستقل: `docs/fa/DATA_IMAGE_OPTIONS_V27_FA.md`.

SQL/Edge/Secret/Dependency جدید ندارد.

---

## ۴۶) V28 — جابه‌جایی گزینه، تصویر امن، پنجره گروهی و رشته تحصیلی

### گزارش دستگاه

```text
جابه‌جایی گزینه‌های چندگزینه‌ای و جورکردنی مثل کارت سؤال نبود
پس از انتخاب عکس برای آپلود، برنامه بسته می‌شد
پنجره گروهی هم‌اندازه پنجره تکی نبود و + اسکرول خودکار نداشت
قسمت پایه فیلد رشته نداشت
```

### ۱) جابه‌جایی گزینه‌ها

علت: قرارداد Drag گزینه با کارت سؤال یکی نبود. `pointerInput` به `itemCount` کلید
می‌خورد و وسط gesture بازنشانی می‌شد؛ اسکرول، رنگ فعال و بازخورد لمسی هم نداشت.

```text
rememberUpdatedState برای index و itemCount
pointerInput فقط به description کلید می‌خورد
animateColorAsState برای حالت فعال
onDragScroll → dispatchRawDelta مثل کارت سؤال
HapticFeedback در شروع و هر پرش
userScrollEnabled فهرست هنگام Drag داخلی خاموش می‌شود
هر سه مسیر: چندگزینه‌ای، ستون راست و ستون چپ جورکردنی
```

### ۲) بسته‌شدن برنامه پس از انتخاب تصویر

علت قطعی: `runCatching` فقط `Exception` را می‌گیرد، اما `OutOfMemoryError` یک
`Error` است و از آن عبور می‌کرد و پروسه را می‌کشت. سقف decode هم ثابت بود و به
حافظه آزاد واقعی کاری نداشت.

```text
بودجه پیکسل = حافظه آزاد واقعی JVM ÷ ۴ ÷ ضریب ایمنی
۴ تلاش؛ هر تلاش بودجه و لبه را نصف می‌کند
تلاش‌های بعدی RGB_565
recycle صریح در rotate/crop/scale و finally
recoverCatching → پیام فارسی به‌جای crash
```

### ۳) پنجره گروهی

```text
قبل: widthIn 720dp + height(maxHeight) + padding 12/8
حالا: widthIn 620dp + heightIn(max = availableHeight) + padding 14/10
LazyColumn: weight(1f, fill = false) → با محتوا رشد می‌کند
+ → pendingRevealIndex → دو withFrameNanos → animateScrollToItem
```

اکنون دقیقاً هم‌عرض و هم‌رفتار پنجره تکی است و کارت جدید خودکار ظاهر می‌شود.

### ۴) رشته تحصیلی

ستون‌های واقعی:

```text
public.profiles.field_of_study
public.profiles.hdr_field
public.classes.field_of_study
```

`FieldOfStudyPicker` از همان چرخ Snapدار پایه استفاده می‌کند؛ گزینه «سایر» و ورودی
دستی مثل پایه کار می‌کند. چرخ مشترک پارامتری شد ولی پیش‌فرض پایه تغییر نکرد.

محل‌ها: فرم تکی، فرم گروهی، کارت دانش‌آموز، کلاس جدید/ویرایش، کارت کلاس،
فیلتر اعضا، سربرگ رسمی، چاپ/PDF، XLSX و کپی اطلاعات.

RPCهای جدید:

```text
native_save_student_extra_v28(uuid,text,text,text,text)
native_save_class_v28(uuid,text,text,text)
native_my_classes_v28()
native_save_profile_v28(... , p_hdr_field)
native_export_backup_v3()  → پشتیبان نسخه ۴
native_restore_backup_v3() → نسخه ۱..۴
my_students() و class_roster() با ستون رشته
```

RPCهای قدیمی بدون رشته دیگر از APK صدا زده نمی‌شوند.

### SQL الزامی V28

```text
sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql
```

Readiness باید هشت مقدار true بدهد:

```text
student_field_ready / class_field_ready / header_field_ready
student_extra_ready / class_save_ready / class_list_ready
profile_save_ready  / backup_v4_ready
```

### امنیت رمز

رمز قبلی Supabase Auth hash یک‌طرفه است و قابل بازیابی نیست. ذخیره قابل‌بازیابی
جدیدی ساخته نشده و `plain_password` همچنان حذف‌شده و ممنوع است.

### نتیجه تست V28

```text
Kotlin compile                         PASS
JVM tests                              185/185 PASS
V28 reorder/image/bulk/field tests       19/19 PASS
PostgreSQL 17 migration اجرای اول و دوم  PASS
V28 SQL integration                    12/12 PASS
cross-teacher student/class denial       PASS
student header write denial              PASS
backup v4 round-trip + legacy v3         PASS
function grants anon/authenticated       PASS
unsafe DML in V28 migration                 0
FINAL_NATIVE_VERIFY                    PASS
lintDebug                              PASS — 0 error
assembleDebug                          PASS
APK Signature Scheme v2                Verified
```

راهنمای مستقل: `docs/fa/REORDER_IMAGE_BULK_FIELD_V28_FA.md`.

Edge/Secret/Dependency جدید ندارد؛ فقط SQL V28 یک‌بار اجرا شود.

---

## ۴۷) V29 — جابه‌جایی پایدار گزینه‌ها، آیکن فرمول، نمایش کامل تصویر و پنجره گروهی تک‌کارتی

### وضعیت ورودی

```text
V28 build/release                     → SUCCESS (اعلام کاربر)
خطای دستگاه باقی‌مانده               → جابه‌جایی گزینه‌ها هنوز مثل کارت سؤال نبود
```

### علت قطعی نقص جابه‌جایی V28

`ReorderDragButton` به `pointerInput(description)` کلید می‌خورد و `description`
شامل برچسب گزینه است. با هر جابه‌جایی، برچسب (الف → ب) عوض می‌شود، کلید
`pointerInput` تغییر می‌کند و gesture وسط کار لغو می‌شود؛ بنابراین هر پرش
نیازمند لمس طولانی تازه بود. کارت سؤال به شناسهٔ پایدار کلید می‌خورد و این
مشکل را نداشت.

### اصلاح جابه‌جایی

```text
pointerInput(description)   →  pointerInput(Unit)
آستانه گزینه 46dp           →  ReorderStepDp = 52f (دقیقاً آستانه کارت سؤال)
کارت سؤال                   →  ReorderStepDp.dp.toPx()
```

- gesture وسط کار دیگر بازنشانی نمی‌شود و کشیدن پیوسته تا انتها ادامه دارد؛
- آستانه، رنگ فعال، haptic، rememberUpdatedState و اسکرول زیر انگشت یکسان‌اند؛
- جورکردنی راست/چپ از همان دکمهٔ مشترک استفاده می‌کنند.

### آیکن فرمول در سطر دوربین

- دکمهٔ متنی «درج فرمول» از بخش متن حذف شد؛
- آیکن `Functions` اکنون در `QuestionMediaEditor` کنار آیکن دوربین در همان سطر است؛
- پیش‌نمایش/ویرایش فرمول موجود بدون تغییر ماند.

### نمایش تمام‌صفحه تصویر

```text
فایل جدید: ui/image/FullScreenImageViewer.kt
لمس thumbnail → تمام‌صفحه با ContentScale.Fit
زوم pinch تا ۸ برابر + جابه‌جایی + دوبار لمس
بستن فقط با ضربدر
مداد کوچک → ویرایش دوباره تصویر موجود
```

### ویرایش پس از انتخاب عکس

```text
SingleImagePicker (گزینه/جورکردنی):
  prepare امن → InteractiveImageEditorDialog → تأیید = ذخیره، انصراف = دور انداختن
QuestionMediaEditor (تصویر متن سؤال):
  هر عکس انتخاب‌شده پس از prepare یکی‌یکی وارد ویرایشگر می‌شود؛
  پس از آخرین تأیید همه با هم اضافه می‌شوند؛ انصراف کل صف را می‌اندازد.
ViewModel: replaceImage برای جایگزینی نتیجه ویرایش دوباره.
```

### پنجره گروهی تک‌کارتی

```text
فهرست ردیف‌ها حذف شد؛ در هر لحظه فقط یک کارت دیده می‌شود.
«+» کارت تازه را جایگزین کارت قبلی می‌کند؛ پنجره هرگز بزرگ نمی‌شود.
شماره‌های بالا هر ردیف را بازمی‌گردانند؛ ردیف کامل «✓» دارد.
حذف، ردیف فعال را برمی‌دارد و کارت قبلی را نشان می‌دهد.
ارسال نهایی همچنان همه ردیف‌ها را در بر می‌گیرد (۱..۱۰۰).
```

### فایل‌های کلیدی V29

```text
docs/fa/BUILDER_MEDIA_BULK_V29_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/image/FullScreenImageViewer.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V29ReorderViewerEditBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
app/src/test/java/ir/exam/app/ui/app/V27DataImageOptionsTest.kt
app/src/test/java/ir/exam/app/ui/app/V26QuestionMediaReorderTest.kt
app/src/test/java/ir/exam/app/ui/app/V25HeaderSafetyPolishTest.kt
scripts/verify_native_final.py
```

### امنیت رمز

بدون تغییر: رمز قبلی Supabase Auth غیرقابل بازیابی است، `plain_password`
بازنمی‌گردد و فقط رمز جدید ثبت‌شده یک‌بار با Clipboard حساس قابل کپی است.

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V28 (SQL رشته تحصیلی باید قبلاً اجرا شده باشد)
```

### نتیجه تست V29

```text
Kotlin compile                         PASS
JVM tests                              199/199 PASS
V29 reorder/viewer/edit/bulk tests       14/14 PASS
FINAL_NATIVE_VERIFY                    PASS
lintDebug                              PASS — 0 error
assembleDebug                          PASS
Debug package                          ir.exam.app.native
Debug versionCode                      3 (fallback محلی)
APK Signature Scheme v2                Verified
```

راهنمای مستقل: `docs/fa/BUILDER_MEDIA_BULK_V29_FA.md`.

---

## ۴۸) V30 — جابه‌جایی رنگی و روان، مشخصات آزمون جمع‌شونده، لیست تغییرات، ویرایش تصویر و پنجره گروهی

### وضعیت ورودی

```text
V29 build/device                        → SUCCESS (اعلام کاربر)
گزارش دستگاه                           → گزینه‌ها هنگام درگ رنگی نمی‌شوند و جابه‌جایی نرم نیست
```

### جابه‌جایی گزینه/جورکردنی

```text
علت: رنگ فعال فقط روی دکمهٔ Drag بود نه کارت؛ و کارت‌ها بدون انیمیشن جابه‌جا می‌شدند.
اصلاح:
- کارت گزینه/جورکردنی هنگام درگ primaryContainer می‌شود (animateColorAsState tween 170)
- رنگ به شناسهٔ پایدار گزینه گره خورده تا وسط درگ گم نشود
- AnimatedReorderColumn با فنر (snapTo + spring) — همان حرکت کارت سؤال
- ReorderDragButton پارامتر onActiveChanged گرفت
```

### کارت مشخصات آزمون

```text
پیش‌فرض بسته شد (settingsExpanded = false)
بازکردن کارت سؤال (لمس سربرگ یا دکمهٔ چیدمان) کارت مشخصات را می‌بندد.
```

### لیست تغییرات درباره

```text
کارت «تغییرات نسخه …» بعد از دانلود APK هم دیده می‌شود.
بازشدن صفحهٔ درباره بررسی بروزرسانی را خودکار اجرا می‌کند.
سطرها تمیز (بدون -، • و backtick) و راست‌به‌چپ نمایش داده می‌شوند.
CI یادداشت‌های فارسی واقعی را از text/CHANGELOG_FA.txt می‌خواند و منتشر می‌کند
(حداکثر ۱۲ سطر؛ اگر فایل نبود یادداشت عمومی می‌رود).
```

### ویرایش تصویر — تست و اصلاح

```text
۱) صفحه‌کلید با بازشدن ویرایشگر بسته می‌شود (focusManager.clearFocus)
۲) ارتفاع پنجره ≤ ۹۲٪ صفحه + verticalScroll + imePadding؛ تأیید/انصراف همیشه دیده می‌شوند
۳) هندسهٔ برش به CropGeometry خالص منتقل و با تست ریاضی JVM تضمین شد:
   مربع واقعی در پیکسل برای هر ابعاد/چرخش، clamp مرکز، حداقل/حداکثر ضلع،
   جابه‌جایی مرکز هنگام کشیدن ضلع، تخمین حجم برابر مساحت واقعی.
فایل جدید: app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt
```

### پنجره گروهی

```text
فهرست کلاس‌ها حذف شد.
از داخل کلاس: هیچ نشانی از کلاس‌ها نیست.
از منوی اصلی: فقط یک انتخاب‌گر تک‌خطی کلاس (چون ساخت بدون کلاس ممکن نیست).
شمارهٔ کارت فعال همیشه بالای کارت: «دانش‌آموز ۲ از ۵ ✓» + دکمه‌های قبلی/بعدی، بدون اسکرول.
```

### فایل‌های کلیدی V30

```text
docs/fa/SMOOTH_REORDER_CHANGELOG_V30_FA.md
text/CHANGELOG_FA.txt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
.github/workflows/android.yml
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ReorderAnimation.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt
app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V30SmoothReorderSettingsChangelogTest.kt
app/src/test/java/ir/exam/app/ui/app/V29ReorderViewerEditBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
scripts/verify_native_final.py
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V29
نکته CI: یادداشت‌های نسخه‌های بعدی را در text/CHANGELOG_FA.txt ویرایش کنید.
```

### نتیجه تست V30

```text
Kotlin compile                         PASS
JVM tests                              216/216 PASS
V30 reorder/settings/changelog tests     17/17 PASS
CropGeometry math tests                   5/5 PASS
FINAL_NATIVE_VERIFY                    PASS
lintDebug                              PASS — 0 error
assembleDebug                          PASS
Debug package                          ir.exam.app.native
APK Signature Scheme v2                Verified
```

راهنمای مستقل: `docs/fa/SMOOTH_REORDER_CHANGELOG_V30_FA.md`.

---

## ۴۹) V31 — رفع غیب‌شدن گزینه، مخاطبان در مشخصات، پیغام آپدیت، رفع کرش آپلود و پنجره گروهی بدون کلاس

### وضعیت ورودی

```text
V30 build/device                        → SUCCESS (اعلام کاربر)
گزارش دستگاه                           → گزینه هنگام جابه‌جایی غیب می‌شود؛ آپلود تصویر کرش می‌کند
```

### رفع غیب‌شدن گزینه

```text
علت: انیمیشن سفارشی AnimatedReorderColumn آفست قبلی/جدید کارت را هنگام درگ سریع
      تداخل می‌انداخت و کارت از دید خارج می‌شد.
اصلاح: حذف کامل آن؛ بازگشت به ستون کلیدخوردهٔ پایدار key(optionId)/key(itemId)
      با حفظ رنگ فعال primaryContainer و قرارداد درگ کارت سؤال.
```

### مخاطبان آزمون

- کارت «مخاطبان آزمون» به داخل «مشخصات آزمون» منتقل شد و با آن باز/بسته می‌شود.

### پیغام آپدیت هنگام ورود

- با ورود به حساب، بررسی بروزرسانی خودکار می‌شود.
- اگر آپدیت جدید باشد، پنجرهٔ «بروزرسانی جدید» با سه مورد اول تغییرات،
  دکمهٔ «دریافت نسخه» و «بعداً» ظاهر می‌شود.

### رفع کرش آپلود تصویر

```text
علت: SupabaseQuestionImageUploader محافظ OOM نداشت؛ تصویر بزرگ با
      createBitmap/createScaledBitmap OutOfMemoryError می‌داد (Error با runCatching
      گرفته نمی‌شود) و پروسه کشته می‌شد.
اصلاح: حلقهٔ MAX_ATTEMPTS=4 با catch صریح OutOfMemoryError، بودجهٔ پیکسل از
      حافظهٔ آزاد واقعی (سقف ۷ مگاپیکسل)، نصف‌شدن بودجه و RGB_565 در تلاش‌های بعدی،
      و پیام فارسی به‌جای کرش.
```

### پنجره گروهی بدون کلاس

```text
کلاس‌ها کاملاً حذف شدند؛ ثبت گروهی بدون کلاس (Edge موجود کلاس خالی را می‌پذیرد).
زیر دکمه‌ها فقط لیست شمارهٔ کارت‌ها (ردیف‌های شش‌تایی، بدون اسکرول).
چیدمان کارت: نام/نام خانوادگی، نام پدر/نام کاربری، پایه/رشته، رمز/رمز فعلی.
کادر «رمز فعلی» رمز تعیین‌شده را نگه می‌دارد و با تغییر رمز خودکار به‌روز می‌شود.
دکمهٔ کپی روی کارت دانش‌آموز رمز را از همین کادر (در حافظهٔ نشست) به‌صورت حساس
کپی می‌کند؛ در نبود رمز، پیام «قابل بازیابی نیست» می‌ماند.
```

### فایل‌های کلیدی V31

```text
docs/fa/STABLE_REORDER_UPDATE_PROMPT_V31_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt
app/src/main/java/ir/exam/app/domain/repository/SchoolRepository.kt
app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V31StableReorderUpdatePromptBulkTest.kt
scripts/verify_native_final.py
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد (manage-student از قبل کلاس خالی را می‌پذیرد)
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V30
```

### نتیجه تست V31

```text
Kotlin compile                         PASS
JVM tests                              226/226 PASS
V31 regression tests                     10/10 PASS
FINAL_NATIVE_VERIFY                    PASS
lintDebug                              PASS — 0 error
assembleDebug                          PASS
Debug package                          ir.exam.app.native
APK Signature Scheme v2                Verified
```

راهنمای مستقل: `docs/fa/STABLE_REORDER_UPDATE_PROMPT_V31_FA.md`.

---

## ۵۰) V32 — رفع کرش آپلود، اسکرول شماره کارت‌ها، ویرایش همانند گروهی و کپی رمز

### وضعیت ورودی

```text
V31 build/device                         → SUCCESS (اعلام کاربر)
گزارش دستگاه                            → پس از انتخاب تصویر جهت آپلود، برنامه کرش می‌کند
```

### ۱) رفع کرش آپلود تصویر

```text
علت: در SupabaseQuestionImageUploader بودجهٔ لبهٔ decode با
      «maxDimension * 2 shr attempt» محاسبه می‌شد و در تلاش اول لبهٔ مجاز را
      به ۴۴۰۰ (دو برابر هدف ۲۲۰۰) می‌رساند؛ در تلاش اول حافظهٔ بی‌مورد صرف
      می‌شد. از سوی دیگر bitmapهای میانی هنگام OutOfMemoryError بازیافت
      نمی‌شدند و در تلاش‌های بعدی حلقهٔ retry نشتی حافظه می‌ماند.
اصلاح:
- بودجهٔ لبهٔ decode → «maxDimension shr attempt» با کف MIN_DECODE_EDGE=640؛
  تلاش اول دقیقاً روی هدف ۲۲۰۰ است و هر تلاش نصف می‌شود.
- uploadOnce کل بدنه را در try/finally گذاشت و bitmap را روی هر مسیر بازیافت می‌کند.
- decodeSampledBitmap با «var current: Bitmap» و catch (t: Throwable) هر bitmap
  میانی را هنگام خطا بازیافت می‌کند تا نشتی بین تلاش‌های retry نماند.
```

### ۲) اسکرول شمارهٔ کارت‌ها در پنجرهٔ گروهی

```text
قبل: شمارهٔ کارت‌ها با rows.indices.chunked(6) در چند سطر ثابت و بدون اسکرول بودند.
حالا: یک LazyRow افقی؛ با LaunchedEffect(activeIndex, rows.size) و
      animateScrollToItem(activeIndex)، شمارهٔ کارت فعال خودکار به دید اسکرول می‌شود.
```

### ۳) پنجرهٔ ویرایش دانش‌آموز همانند پنجرهٔ گروهی

```text
StudentEditDialog بازنویسی شد تا دقیقاً مانند BulkStudentDialog باشد:
- همان wrapper: Dialog + BoxWithConstraints + Surface هم‌عرض ۶۲۰dp + heightIn
  از بالا + SOFT_INPUT_ADJUST_RESIZE.
- به‌جای دکمه‌های +/ایجاد/×: دکمهٔ قرمز «انصراف» و دکمهٔ «ذخیره» در بالا.
- فیلدها پیش‌پر از اطلاعات دانش‌آموز: نام/نام‌خانوادگی، نام پدر/نام کاربری،
  پایه/رشته، رمز جدید اختیاری/رمز فعلی (غیرقابل بازیابی) و دختر/پسر/🎲.
- عنوان «ویرایش دانش‌آموز» حذف شد.
```

### ۴) دکمهٔ کپی روی کارت دانش‌آموز

```text
- رمز همچنان از کادر رمز فعلی (حافظهٔ نشست knownPasswords) برداشته و به‌صورت
  حساس کپی می‌شود.
- اخطار «رمز قبلی در سامانه ذخیره نمی‌شود» از پیام Toast حذف شد؛ در نبود رمز،
  فقط «اطلاعات دانش‌آموز کپی شد.» نمایش داده می‌شود.
- متن fallback کلیپ‌بورد «رمز عبور: قابل بازیابی نیست…» حفظ شد (رمز قبلی hash است).
```

### فایل‌های کلیدی V32

```text
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
text/CHANGELOG_FA.txt
app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V32EditScrollCopyImageTest.kt
app/src/test/java/ir/exam/app/ui/app/V31StableReorderUpdatePromptBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V21StudentBuilderPolishTest.kt
app/src/test/java/ir/exam/app/ui/app/V19InteractionTest.kt
scripts/verify_native_final.py
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V31
```

### نتیجه تست V32

```text
FINAL_NATIVE_VERIFY                     → PASS (اجرای محلی اسکریپت)
V32 upload/bulk/edit/copy source tests  → 7/7 (منبع-اسکریپت)
رگرسیون V19/V21/V31                     → به‌روزرسانی شد
lintDebug / assembleDebug               → باید در WSL با gradlew اجرا شود
```

### نکتهٔ مهم ادامهٔ کار

اگر پس از این پچ باز هم در انتخاب/آپلود تصویر کرش دیدید، **logcat واقعی** همان
لحظه (بدون فیلتر) لازم است؛ بدون آن حدس‌زدن ممنوع است. دستور دستگاه:

```text
adb logcat -d AndroidRuntime:E *:S
```

راهنمای مستقل: `docs/fa/IMAGE_BULK_EDIT_COPY_V32_FA.md`.

## ۵۱) V32.1 — اصلاح تست‌های قدیمی CI پس از اسکرول شمارهٔ کارت‌ها

### علت شکست CI گزارش‌شده

```text
compileDebugKotlin                       → SUCCESS
233 unit tests                           → 230 PASS / 3 FAIL
V28ReorderImageBulkFieldTest             → assertion قدیمی rows.indices.chunked(6)
V29ReorderViewerEditBulkTest             → assertion قدیمی rows.indices.chunked(6)
V30SmoothReorderSettingsChangelogTest    → assertion قدیمی rows.indices.chunked(6)
```

کد اصلی V32 کامپایل شده بود و هر سه شکست فقط از تست‌های منبع‌محور قدیمی بودند
که هنوز چیدمان بدون اسکرول V28 تا V30 را الزام می‌کردند. تست‌ها با رفتار موردنیاز
V32 هماهنگ شدند: `LazyRow`، `rememberLazyListState`، اتصال state و
`animateScrollToItem(activeIndex)`. کد اجرایی برنامه در این اصلاح تغییر نکرد.

### فایل‌ها و عملیات V32.1

```text
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
app/src/test/java/ir/exam/app/ui/app/V29ReorderViewerEditBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V30SmoothReorderSettingsChangelogTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
SQL / Edge Function / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V32
```

### نتیجهٔ بررسی

```text
گزارش GitHub Actions کاربر: compileDebugKotlin → SUCCESS
سه assertion ناسازگار شناسایی و اصلاح شدند.
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در GitHub Actions یا WSL اجرا شود.
```


## ۵۲) V33 — رفع قطعی کرش ویرایشگر تصویر و نمایش رمز فعلی نشست

### ورودی تأییدشده

```text
V32 build                               → SUCCESS (اعلام کاربر)
logcat واقعی دستگاه                     → دریافت شد
Process                                 → ir.exam.app
Exception                               → java.lang.IllegalStateException: Size is unspecified
محل                                     → InteractiveImageEditorDialog.kt:168
Thread                                  → main
```

### علت قطعی کرش

کرش ربطی به Supabase، شبکه یا مرحلهٔ upload نداشت. پس از انتخاب تصویر، ویرایشگر
با `sourcePixels = Size.Unspecified` compose می‌شد. عبارت
`sourcePixels.width.takeIf { ... }` ابتدا getter مربوط به `width` را اجرا می‌کرد؛
Compose برای `Size.Unspecified` عمداً `IllegalStateException` می‌اندازد. این اتفاق
پیش از `AsyncImage.onSuccess` و پیش از شروع هر آپلود روی thread اصلی رخ می‌داد.

### اصلاح تصویر و ممیزی کامل

```text
InteractiveImageEditorDialog:
- safeImagePixelSize(Size.Unspecified) → Size(1f, 1f)
- حذف همه دسترسی‌های مستقیم sourcePixels.width/sourcePixels.height
- استفاده از safePixels در preview، CropGeometry.areaFraction و cropRect

LocalImageRepository:
- IO dispatch، bounds decode، inSampleSize، بودجه حافظه، retry OOM و recycle بازبینی شد.

SupabaseQuestionImageUploader:
- مسیرهای سؤال/گزینه/جورکردنی/پاسخ/آواتار، محدودیت 8MB، retry، recycle،
  bucket exam-images و بازگرداندن خطا بازبینی شد.

Call sites:
- QuestionMediaEditor، SingleImagePicker، ProfileSettingsScreen و StudentExamScreen
  از انتخاب URI تا نمایش editor و تحویل URI آماده‌شده بازبینی شدند.
```

### پنجرهٔ ویرایش دانش‌آموز

```text
- جملهٔ «رمز فعلی hash شده و قابل نمایش نیست و ...» حذف شد.
- currentPassword از knownPasswords همان نشست به StudentEditDialog داده می‌شود.
- کادر «رمز فعلی» مقدار واقعی شناخته‌شدهٔ نشست را read-only نشان می‌دهد.
- دکمهٔ نمایش/مخفی‌کردن مستقل برای رمز فعلی اضافه شد.
- در تغییر صرفاً نام کاربری، نگاشت رمز نشست برای نام جدید حفظ می‌شود.
- رمز قدیمی hash‌شده بازیابی یا به‌صورت دائمی ذخیره نمی‌شود؛ plain_password ممنوع است.
```

### فایل‌های V33

```text
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/image/V33ImageEditorPasswordTest.kt
app/src/test/java/ir/exam/app/ui/app/V22ClassStudentCardsTest.kt
app/src/test/java/ir/exam/app/ui/app/V24ComprehensiveUxTest.kt
app/src/test/java/ir/exam/app/ui/app/V25HeaderSafetyPolishTest.kt
app/src/test/java/ir/exam/app/ui/app/V26QuestionMediaReorderTest.kt
app/src/test/java/ir/exam/app/ui/app/V27DataImageOptionsTest.kt
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
app/src/test/java/ir/exam/app/ui/app/V32EditScrollCopyImageTest.kt
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/IMAGE_EDITOR_SESSION_PASSWORD_V33_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

### تست و عملیات

```text
FINAL_NATIVE_VERIFY                     → PASS
stack-trace regression tests            → اضافه شد
password session regression tests       → اضافه شد
git diff --check                        → PASS (پیش از بسته‌بندی)
testDebugUnitTest / lintDebug           → باید در WSL/GitHub Actions اجرا شود
SQL / Edge / Secret / Migration جدید    → ندارد
Dependency جدید                         → ندارد
پیش‌نیاز                                → V32.1
```

راهنمای مستقل: `docs/fa/IMAGE_EDITOR_SESSION_PASSWORD_V33_FA.md`.


## ۵۳) V34 — ابزارهای گزینه، Vault رمز دستگاه و برش جهت‌دار/دایره‌ای

### وضعیت ورودی

```text
V33 build                               → SUCCESS (اعلام کاربر)
درخواست ۱                               → drag چندگزینه‌ای/جورکردنی کنار فرمول
درخواست ۲                               → تصویر متن سؤال + مداد + ضربدر در یک سطر مانند گزینه
درخواست ۳                               → دو کادر رمز هم‌اندازه و حفظ رمز پس از restart
درخواست ۴                               → ضلع برش در جهت انگشت و قاب دایره‌ای پروفایل
انتخاب امنیتی کاربر                    → Vault رمزنگاری‌شده فقط روی همین دستگاه
```

### تغییرات Builder و تصویر متن سؤال

```text
چندگزینه‌ای: Formula → ReorderDragButton → SingleImagePicker
جورکردنی:   Formula → ReorderDragButton → SingleImagePicker

CompactImageThumbnail:
- Row هم‌سطری به‌جای Box با دکمه‌های overlay
- تصویر 30dp، مداد 24dp، ضربدر 17dp؛ یکسان با SingleImagePicker گزینه‌ها
```

### Vault رمز دانش‌آموز روی دستگاه

`StudentPasswordVault` اضافه شد. کلید AES غیرقابل‌استخراج با
`KeyGenParameterSpec` در `AndroidKeyStore` ساخته می‌شود و رمزها با
`AES/GCM/NoPadding` و IV تصادفی رمز می‌شوند. SharedPreferences تنها
`Base64(iv).Base64(ciphertext+tag)` را نگه می‌دارد. کل رکورد بر پایهٔ شناسهٔ
یکتای دانش‌آموز است تا تغییر نام کاربری و حساب‌های مختلف تداخل نکنند.

```text
پشتیبان‌گیری برنامه                    → android:allowBackup="false"
ذخیره plaintext در SharedPreferences   → ندارد
plain_password در مدل/SQL/Edge          → ندارد و همچنان ممنوع
انتقال به دستگاه دیگر                  → ندارد
پس از حذف app data/uninstall            → رمزها از بین می‌روند
رمزهای تاریخی فقط-hash                  → قابل بازیابی نیستند؛ یک‌بار reset لازم است
```

`knownPasswords` در UI cache زنده باقی ماند، اما پس از بارگیری دانش‌آموزان از
Vault بر پایهٔ `student.id` پر می‌شود. credential ساخت تکی، ساخت گروهی و تغییر
رمز موفق در Vault نوشته می‌شوند. دو `OutlinedTextField` رمز هر دو
`Modifier.weight(1f).height(56.dp)` دارند.

### اصلاح برش

در V33، pointer gesture حرکت کل `CropFrame` روی والد بود و با gesture دستگیره‌ها
همپوشانی داشت؛ در نتیجه هنگام کشیدن ضلع، کادر هم resize و هم move می‌شد. در V34:

```text
- gesture حرکت کادر فقط در Box مرکزی با padding(26.dp)
- دستگیره‌های LEFT/RIGHT/TOP/BOTTOM فقط resize
- CropGeometry.resizeDeltaForEdge برای علامت صحیح هر چهار جهت
- recenterAfterResize برای حرکت مرکز به سمت ضلع کشیده‌شده و ثابت‌ماندن ضلع مقابل
- circular = forceSquare و CircleShape برای قاب پروفایل
- عنوان پروفایل: «برش دایره‌ای پروفایل»
```

خروجی آواتار برای سازگاری Storage مربع است و نمایش/راهنمای crop پروفایل دایره‌ای
است؛ بنابراین گوشه‌های خارج از دایره در UI آواتار دیده نمی‌شوند.

### فایل‌های V34

```text
app/src/main/java/ir/exam/app/data/local/StudentPasswordVault.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/test/java/ir/exam/app/ui/app/V34BuilderVaultCropTest.kt
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/BUILDER_VAULT_CROP_V34_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

### نتیجهٔ بررسی و عملیات

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
V34 source/pure geometry tests          → اضافه شد
testDebugUnitTest / lintDebug           → باید در WSL/GitHub Actions اجرا شود
SQL / Edge Function / Migration         → ندارد
Dependency جدید                         → ندارد (Android Keystore پلتفرم)
پیش‌نیاز                                → V33
```

راهنمای مستقل: `docs/fa/BUILDER_VAULT_CROP_V34_FA.md`.


## ۵۴) V34.1 — اصلاح کامپایل تست V34 در GitHub Actions

### گزارش واقعی CI

```text
compileDebugKotlin                      → SUCCESS
هشدارهای deprecation                    → غیرمسدودکننده
compileDebugUnitTestKotlin              → FAILED
فایل                                    → V34BuilderVaultCropTest.kt:88
خطا                                     → String.count(String) وجود ندارد؛ count تابع predicate می‌خواهد
```

کد اجرایی V34 با موفقیت کامپایل شده بود. خطا فقط در تست جدید بررسی هم‌اندازه بودن
دو کادر رمز بود. عبارت نامعتبر زیر:

```text
edit.count("Modifier.weight(1f).height(56.dp)")
```

با شمارش معتبر و literal-safe زیر جایگزین شد:

```text
Regex(Regex.escape(equalSizeMarker)).findAll(edit).count()
```

### فایل‌ها و نتیجه

```text
app/src/test/java/ir/exam/app/ui/app/V34BuilderVaultCropTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
کد اجرایی برنامه                     → بدون تغییر
FINAL_NATIVE_VERIFY                   → PASS
git diff --check                      → PASS
testDebugUnitTest / lintDebug         → باید در WSL/GitHub Actions تکرار شود
SQL / Edge / Migration / Dependency   → ندارد
پیش‌نیاز                              → V34
```


## ۵۵) V35 — رنگ جنسیت، نوار رمز، کپی دقیق، کارت فشرده و crop آزاد

### ورودی

```text
V34 build                               → SUCCESS (اعلام کاربر)
جنسیت edit/bulk                         → دختر صورتی، پسر آبی هنگام انتخاب
edit password                           → بزرگ‌تر، چشم مرکزی، X قرمز و ✓ سبز
copy card                               → فقط ۸ مشخصهٔ تعیین‌شده + رمز current/Vault
crop                                    → حرکت آزاد، حذف میله ضلع، resize هم‌جهت
student card                            → نام+پایه+رشته و پدر+username در دو سطر
```

### پنجره‌های دانش‌آموز

`genderFilterChipColors` با selected container صورتی `0xFFFF5C9A` و آبی
`0xFF3B9EFF` در `StudentEditDialog` و `BulkStudentDialog` اعمال شد.

در edit، Buttonهای متنی حذف و نوار آیکنی ساخته شد:

```text
Surface قرمز + Close(contentDescription="انصراف")
IconButton مرکزی Visibility/VisibilityOff
Surface سبز + Check(contentDescription="ذخیره")
```

یک state به نام `passwordVisible` هر دو کادر را کنترل می‌کند. trailingIconهای داخل
هر دو فیلد حذف شدند. هر دو فیلد دقیقاً `weight(1f).height(64.dp)` و textStyle برابر
`titleMedium` دارند.

### Clipboard

`studentClipboardText` فقط خطوط زیر را می‌سازد:

```text
نام، نام خانوادگی، نام پدر، پایه، رشته، نام کاربری، رمز، کلاس‌ها
```

عنوان، نام کامل ترکیبی، جنسیت، وضعیت و شناسه حذف شدند. اولویت رمز:
`oneTimePassword` سپس `currentPassword` است؛ currentPassword کارت از cache پرشده
توسط `StudentPasswordVault.read(student.id)` می‌آید. در نبود هر دو، «—» نوشته
می‌شود. علامت Clipboard حساس در صورت وجود رمز حفظ شده است.

### کارت دانش‌آموز

```text
سطر بسته: fullName + فاصله + grade fieldOfStudy
سطر باز:  fatherName در یک ستون + username در ستون دیگر
```

خطوط جدا و تکراری پایه/رشته حذف شدند؛ کلاس‌ها و دکمه‌های عملیات حفظ شدند.

### crop

`CropGeometry.moveCenter` اضافه شد تا حرکت آزاد مرکز مربع و دایره به‌صورت خالص
و قابل تست انجام و در مرز تصویر clamp شود. Gesture داخلی با `padding(18.dp)` از
چهار ناحیهٔ resize جداست. نوارهای لمسی اضلاع ۱۸dp و نامرئی هستند؛ Boxهای سفید
`34×5` و `5×34` حذف شدند. قرارداد `resizeDeltaForEdge` و
`recenterAfterResize` حفظ و با تست ثابت‌ماندن ضلع مقابل پوشش داده شد.

### فایل‌ها

```text
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt
app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt
app/src/test/java/ir/exam/app/ui/app/V23InteractionGradeOdometerTest.kt
app/src/test/java/ir/exam/app/ui/app/V31StableReorderUpdatePromptBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V32EditScrollCopyImageTest.kt
app/src/test/java/ir/exam/app/ui/image/V33ImageEditorPasswordTest.kt
app/src/test/java/ir/exam/app/ui/app/V34BuilderVaultCropTest.kt
app/src/test/java/ir/exam/app/ui/app/V35StudentUiCropClipboardTest.kt
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/STUDENT_UI_CROP_V35_FA.md
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

### بررسی و عملیات

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
V35 pure/source tests                   → اضافه شد
SQL / Edge / Migration / Dependency     → ندارد
پیش‌نیاز                                → V34.1
```

راهنمای مستقل: `docs/fa/STUDENT_UI_CROP_V35_FA.md`.


## ۵۶) V36 — نقش مدیر/معاون، مدرسهٔ مستقل و پوستهٔ مدیریتی

### تصمیم‌های تأییدشده

```text
روش تحویل                             → سه مرحله V36/V37/V38
ثبت‌نام مدیر                          → ایمیل + OTP مانند معلم
tenant                                → هر مدیر یک مدرسهٔ مستقل
مدیر/معاون                            → یک نقش واحد MANAGER
افزودن معلم                           → دعوت امن در V37
کیف پول                               → انتقال تومان از مدیر در V38
آمار                                  → آمار آموزشی کامل در V38
```

### V36 تحویل‌شده

- `UserRole.MANAGER` به مدل، cache، auth repository، profile و تمام whenهای
  exhaustive اضافه شد.
- دکمهٔ «ثبت‌نام» ابتدا `REGISTRATION_ROLE` را باز می‌کند و دو دکمهٔ معلم و
  مدیر/معاون نشان می‌دهد.
- مسیر مدیر شامل فرم اولیه، OTP مستقل و setup مدرسه/نام‌کاربری/رمز است.
- pending role در metadata Auth، RPC registration-state و cache عمومی پروفایل
  حفظ می‌شود تا ادامه setup پس از restart اشتباه به فرم معلم نرود.
- SQL نقش manager، جداول schools/school_memberships، RLS، تکمیل اتمیک ثبت‌نام،
  تغییر نام کاربری staff و summary پایه را اضافه می‌کند.
- پوسته مدیر صفحهٔ HOME=معلم‌ها، دکمه پایین «معلم‌ها»، آیکن Students، کارت‌های
  فقط آمار، و `+` با مقصد معلم جدید دارد.
- menu مدیر ۴ کارت حساب/داده‌ها/تنظیمات/خروج دارد و تقویم/سربرگ حذف شده‌اند.
- صفحات V37 و V38 عمداً foundation امن هستند؛ عملیات ناقص منتشر نشده است.

### SQL

```text
supabase/migrations/20260815_native_school_manager_v36.sql
sql/manual/SQL_NATIVE_SCHOOL_MANAGER_V36.sql
ترتیب: پس از تمام migrationهای V35
اجرای دستی لازم: بله، در SQL Editor پروژه اصلی
Edge deploy: ندارد
Secret جدید: ندارد
```

### فایل‌های کلیدی

```text
app/src/main/java/ir/exam/app/domain/model/AppUser.kt
app/src/main/java/ir/exam/app/domain/repository/AuthRepository.kt
app/src/main/java/ir/exam/app/data/local/AuthUserCache.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt
app/src/main/java/ir/exam/app/ui/auth/AuthViewModel.kt
app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt
app/src/main/java/ir/exam/app/ui/app/ExamApp.kt
app/src/main/java/ir/exam/app/ui/app/TeacherBottomDock.kt
app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt
app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt
app/src/test/java/ir/exam/app/ui/app/V36ManagerFoundationTest.kt
supabase/migrations/20260815_native_school_manager_v36.sql
sql/manual/SQL_NATIVE_SCHOOL_MANAGER_V36.sql
docs/fa/SCHOOL_MANAGER_FOUNDATION_V36_FA.md
```

### نتیجه بررسی

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
V36 manager foundation tests           → اضافه شد
testDebugUnitTest / lintDebug           → باید در WSL/GitHub Actions اجرا شود
Dependency / Edge / Secret جدید         → ندارد
پیش‌نیاز                                → V35
```

راهنمای مستقل: `docs/fa/SCHOOL_MANAGER_FOUNDATION_V36_FA.md`.


## ۵۷) V36.1 — اصلاح تست قدیمی ناوبری پس از افزودن نقش مدیر

### گزارش واقعی CI

```text
compileDebugKotlin                      → SUCCESS
compileDebugUnitTestKotlin              → SUCCESS
256 tests                               → 255 PASS / 1 FAIL
Neumorphic69IntegrationTest:140         → assertion قدیمی mutableStateOf(MainPage.CALENDAR)
```

کد اجرایی V36 کامپایل شده بود. تست V18 هنوز فرض می‌کرد تمام نقش‌ها بدون شرط از
تقویم شروع می‌شوند و محدودهٔ منوی معلم را تا اولین `else` می‌خواند. در V36:

```text
MANAGER                                 → MainPage.HOME (معلم‌ها)
TEACHER / STUDENT                       → MainPage.CALENDAR
مرز منوی معلم                          → else if (UserRole.MANAGER)
```

تست با همین قرارداد جدید هماهنگ شد. کد اجرایی، SQL و قابلیت‌های V36 تغییری نکردند.

### فایل و بررسی

```text
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در CI/WSL تکرار شود
SQL / Edge / Secret / Dependency جدید   → ندارد
پیش‌نیاز                                → V36
```


## ۵۸) V37 — دعوت امن و مدیریت عضویت معلم

### تصمیم‌ها

```text
حذف معلم                              → disable عضویت؛ Auth/آزمون حفظ
داده‌های قبلی معلم                    → انتقال خودکار ممنوع؛ انتخاب مدیر لازم
دعوت                                  → ایمیل + کد hash‌شده TCH، انقضا ۷ روز
```

### اجرا

- school_teacher_invites، school_students و school_admin_audit_v37 ساخته شدند.
- کد دعوت ۲ UUID تصادفی دارد؛ فقط SHA-256 آن ذخیره می‌شود، به ایمیل OTP مقید و
  پس از مصرف/انقضا نامعتبر است.
- مدیر فهرست معلم‌ها، ساخت کد و قطع عضویت را از صفحه معلم‌ها انجام می‌دهد.
- معلم کد را در setup ثبت‌نام وارد می‌کند؛ ثبت‌نام مستقل بدون کد حفظ شده است.
- قطع عضویت فقط status را disabled می‌کند و حساب Auth را حذف نمی‌کند.
- manage-student پس از create و bulk، دانش‌آموز جدید معلم عضو را با helper محدود
  به school_students وصل می‌کند.
- انتقال انتخابی داده‌های قدیمی عمداً خودکار نشده تا مدیر در مرحله مجوزهای داده
  موارد موردنظر را انتخاب کند.

### عملیات الزامی

```text
1) اجرای sql/manual/SQL_NATIVE_SCHOOL_TEACHER_MANAGEMENT_V37.sql
2) deploy تابع manage-student از سورس همین نسخه
3) build/test APK
Secret جدید: ندارد
Dependency جدید: ندارد
```

### بررسی

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در WSL/CI اجرا شود
SQL copy equality                       → PASS
پیش‌نیاز                                → V36.1 + SQL V36
```

راهنما: `docs/fa/SCHOOL_TEACHER_INVITATION_V37_FA.md`.


## ۵۹) V38 — انتقال کیف پول مدیر به معلم و آمار مدرسه

### قرارداد مالی تأییدشده

```text
مبلغ انتقال داخلی                    → دلخواه، مثبت، مضرب ۱٬۰۰۰ تومان
۲۷٬۰۰۰                                → مجاز
۲۷٬۵۰۰                                → غیرمجاز
شارژ بانکی مدیر                       → همان قوانین موجود (حداقل ۱۰۰هزار/گام ۱۰هزار)
هزینه سؤال                            → ۱٬۰۰۰ تومان
```

### پیاده‌سازی

- ManagerWalletRules در domain، UI و repository یک قرارداد واحد دارد.
- مدیر از WalletScreen و Edge پرداخت امن فعلی کیف خودش را شارژ می‌کند؛ RPC سفارش
  پرداخت role manager را نیز می‌پذیرد.
- native_manager_transfer_wallet_v38 عضویت هم‌مدرسه‌ای، مبلغ، موجودی و سقف را
  بررسی و هر دو wallet را FOR UPDATE قفل می‌کند.
- operation UUID + manager_wallet_transfers_v38 idempotency را تضمین می‌کند.
- debit مدیر، credit معلم، دو wallet_tx و audit در همان transaction ثبت می‌شوند.
- کارت معلم موجودی و دکمه شارژ دارد؛ ۲۷۵۰۰ در UI و سرور رد می‌شود.
- summary مدرسه پاسخ‌ها، میانگین درصد، مبلغ توزیع‌شده و teacher_activity شامل
  تعداد آزمون/کلاس/دانش‌آموز/موجودی را برمی‌گرداند.

### عملیات

```text
sql/manual/SQL_NATIVE_MANAGER_WALLET_STATS_V38.sql
supabase/migrations/20260815_native_manager_wallet_stats_v38.sql
Edge deploy جدید                         → ندارد
Secret / Dependency جدید                → ندارد
پیش‌نیاز                                → V37 + SQL/Edge V37
```

### بررسی

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
SQL copy equality                       → PASS
testDebugUnitTest / lintDebug           → باید در WSL/CI اجرا شود
```

راهنما: `docs/fa/MANAGER_WALLET_STATS_V38_FA.md`.


## ۶۰) V38.1 — رفع خطای «این حساب قابل تبدیل به مدیر/معاون نیست»

### گزارش و علت

```text
ایمیل                                 → کاملاً جدید (اعلام کاربر)
SQL V36/V37/V38                       → همگی اجرا شده‌اند
خطا                                  → این حساب قابل تبدیل به مدیر/معاون نیست
شرط مولد خطا                         → role خارج student/manager یا teacher_id غیر null
علت سازگار با وضعیت                  → trigger قدیمی profile تازه را role=teacher ساخته است
```

RPC اکنون role موقت teacher را فقط وقتی می‌پذیرد که هیچ کلاس، آزمون، دانش‌آموز،
عضویت active مدرسه یا عضویت دانش‌آموزی نداشته باشد. حساب معلم واقعی دارای داده
همچنان با پیام صریح رد می‌شود؛ teacher_id غیر null نیز حساب دانش‌آموز مدیریت‌شده
محسوب و رد می‌شود.

```text
sql/manual/SQL_NATIVE_MANAGER_REGISTRATION_V381_HOTFIX.sql
supabase/migrations/20260815_native_manager_registration_v381_hotfix.sql
FINAL_NATIVE_VERIFY                     → PASS
SQL copy equality                       → PASS
App / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → SQL V36
```

راهنما: `docs/fa/MANAGER_REGISTRATION_V381_FA.md`.


## ۶۱) V38.2 — رفع خطای digest دعوت و نشت Header در UI

### گزارش واقعی

```text
خطا                  → function digest(text, unknown) does not exist
RPC                   → native_manager_create_teacher_invite_v37
علت                  → pgcrypto در schema extensions + ورودی text بدون cast bytea
ریسک جانبی screenshot → نمایش URL و Headers شامل Bearer session در UI
```

هر دو hash ساخت و مصرف دعوت اکنون schema-qualified و bytea هستند:
`extensions.digest(convert_to(...,'UTF8'),'sha256')`. خطاهای manager UI نیز قبل از
نمایش در URL/Headers قطع و Authorization/apikey/Bearer حذف می‌شوند.

```text
sql/manual/SQL_NATIVE_INVITE_DIGEST_V382_HOTFIX.sql
FINAL_NATIVE_VERIFY                     → PASS
SQL copy equality                       → PASS
Edge deploy                             → ندارد
```

نکته امنیتی: session token موجود در screenshot افشاشده تلقی می‌شود؛ کاربر باید
فوراً از حساب خارج و sessionهای آن کاربر را در Supabase Auth باطل کند.

## ۶۲) V38.3 — اصلاح کامپایل Regex پاک‌سازی Bearer

### گزارش CI

```text
compileDebugKotlin → FAILED
ManagerFoundationScreens.kt:278 → Unsupported escape sequence
```

در رشتهٔ معمولی Kotlin، `\s` باید در سورس به‌شکل `\\s` نوشته شود. Regex پاک‌سازی
Bearer اصلاح شد؛ رفتار امنیتی همان حذف token است و SQL/Edge تغییری ندارد.

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در CI/WSL تکرار شود
SQL / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → V38.2
```


## ۶۳) V39 — دعوت کوتاه مدرسه و جریان‌های سریع نقش‌محور

### تصمیم‌ها

```text
کد دعوت معلم          → بدون ایمیل، ۶ کاراکتر مانند آزمون
اعتبار                 → یک‌بارمصرف، ۲۴ ساعت
quick add معلم         → کلاس، دانش‌آموز، آزمون
quick add مدیر         → کلاس، دانش‌آموز، دعوت معلم
```

### پیاده‌سازی

- native_manager_create_teacher_invite_v39 بدون پارامتر ایمیل، کد ۶ کاراکتری
  و hash schema-qualified می‌سازد و دعوت قبلی را revoke می‌کند.
- preview/join معلم با ۱۰ تلاش در ۱۰ دقیقه، role teacher، کد active و transaction
  مصرف یک‌باره محافظت شده است.
- AccountSection معلم کارت پیوستن به مدرسه، Search، preview و تأیید دارد.
- Design69QuickAddOverlay با primaryTitle/icon نقش‌محور شد؛ مدیر و معلم هر کدام
  سه عمل صحیح را می‌بینند.
- manage-student role manager را برای ساخت مستقیم می‌پذیرد و helper/trigger
  manager را نیز به school scope متصل می‌کند.
- Design69MainMenuScreen featuredCard وسط‌چین دارد؛ دانش‌آموز کارت آزمون را زیر
  پروفایل می‌بیند. dialog کد را به StudentHome می‌فرستد و preview قبلی پیش از start
  حفظ شده است.

### عملیات

```text
sql/manual/SQL_NATIVE_SHORT_SCHOOL_INVITE_V39.sql
supabase/migrations/20260815_native_short_school_invite_v39.sql
Edge deploy                             → manage-student الزامی
FINAL_NATIVE_VERIFY                     → PASS
SQL copy equality                       → PASS
Secret / Dependency جدید               → ندارد
```

راهنما: `docs/fa/SHORT_INVITE_QUICK_EXAM_V39_FA.md`.

## ۶۴) V39.1 — اصلاح import دکمه ذره‌بین پیوستن به مدرسه

### گزارش CI

```text
compileDebugKotlin → FAILED
ProfileSettingsScreen.kt:558 → Unresolved reference IconButton
ProfileSettingsScreen.kt:569 → خطای cascading context Composable
```

`IconButton` در کارت جدید پیوستن به مدرسه استفاده شده بود اما import مادی آن جا
افتاده بود. import اضافه شد؛ خط دوم پیام cascading خط اول است. کد اجرایی دیگر،
SQL و Edge تغییری ندارند.

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در CI/WSL تکرار شود
SQL / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → V39
```

## ۶۵) V39.2 — اصلاح assertion نادرست ستون email دعوت کوتاه

### گزارش CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
272 tests                                   → 271 PASS / 1 FAIL
V39ShortInviteQuickAddExamTest.kt:30        → marker اشتباه «email,null»
```

SQL درست بود و در فهرست ستون‌ها `email` و در بخش VALUES مقدار `null` قرار داشت،
اما تست به‌اشتباه انتظار داشت این دو در متن SQL کنار هم باشند. تست اکنون فهرست
ستون و VALUES را جداگانه بررسی می‌کند. کد اجرایی، SQL و Edge تغییری ندارند.

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در CI/WSL تکرار شود
SQL / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → V39.1
```


## ۶۶) V40A — مشخصات معلم و منوی دانش‌آموز

### دامنه مرحله

```text
V40A → پروفایل معلم + محدودسازی حساب/منوی دانش‌آموز + مرکز تنظیمات
V40B → کارت معلم و دعوت دسته‌ای ۱..۵
V40C → مدیریت کلاس/دانش‌آموز و مجوزهای حذف
```

### تغییرات

- profiles.employee_code و profiles.phone و دو RPC owner-only معلم اضافه شد.
- NativeProfile/repository/viewmodel کارت مشخصات معلم را load/save می‌کنند.
- کد پرسنلی اختیاری با حداکثر ۳۰ حرف انگلیسی/عدد/_/- و تلفن اختیاری با الگوی
  09 + 9 رقم در app و SQL اعتبارسنجی می‌شوند.
- دانش‌آموز email را در مشخصات نمی‌بیند و سه کارت تغییر username/email/password
  در LazyColumn اصلاً compose نمی‌شوند.
- کارت داده‌ها از menu دانش‌آموز حذف و ترتیب دقیق سه ردیف اعمال شد.
- Appearance/About بدون horizontalScroll و با CenterHorizontally هستند.

```text
sql/manual/SQL_NATIVE_TEACHER_PROFILE_V40A.sql
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
SQL copy equality                       → PASS
Edge deploy                             → ندارد
پیش‌نیاز                                → V39.2
```

راهنما: `docs/fa/TEACHER_PROFILE_STUDENT_MENU_V40A_FA.md`.


## ۶۷) V40B — کارت معلم و دعوت دسته‌ای

### تغییرات

- ManagerTeachersScreen در حالت عادی فقط کارت معلم دارد؛ دکمه عمومی دعوت حذف شد.
- کارت از RPC نام، employee_code، phone، status و wallet را می‌گیرد؛ لمس کارت چهار
  IconButton Toggle/Login/Wallet/Delete را ظاهر می‌کند.
- Toggle بین active/disabled است. Delete عضویت را removed می‌کند و Auth/آزمون‌ها
  را حذف نمی‌کند. ورود، نقطه شروع context مدیریت کلاس است که در V40C تکمیل می‌شود.
- quick-add دعوت، inviteMode جدا باز می‌کند؛ لیست معلم مخفی، ساخت کد وسط‌چین و
  dialog FilterChipهای ۱..۵ است.
- هر کد مستقل ۲۴ساعته است؛ display_code برای نمایش مدیریتی کوتاه‌عمر ذخیره می‌شود،
  verification همچنان با hash است. لیست ۷ روز اخیر، remaining/used/revoked و
  revoke کد مصرف‌نشده را نشان می‌دهد.
- status memberships با removed گسترش یافت بدون حذف user.

```text
sql/manual/SQL_NATIVE_MANAGER_TEACHER_CARDS_V40B.sql
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
SQL copy equality                       → PASS
Edge deploy                             → ندارد
پیش‌نیاز                                → V40A
مرحله بعد                               → V40C مدیریت کلاس و دانش‌آموز
```

راهنما: `docs/fa/MANAGER_TEACHER_CARDS_INVITES_V40B_FA.md`.

## ۶۸) V40B.1 — هماهنگ‌سازی دو تست قدیمی با قرارداد V40B

### گزارش CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
281 tests                                   → 279 PASS / 2 FAIL
V37TeacherInvitationTest.kt:28              → marker تاریخی V37 ناخواسته به نام RPC V40B تغییر کرده بود
V38ManagerWalletStatsTest.kt:63              → دکمه متنی شارژ در V40B به آیکن کیف پول تبدیل شده است
```

تست V37 دوباره migration تاریخی خودش (`native_manager_disable_teacher_v37`) را
بررسی می‌کند. تست V38 به‌جای `Text("شارژ")`، آیکن AccountBalanceWallet و
contentDescription «شارژ کیف پول» را بررسی می‌کند. کد اجرایی، SQL و Edge بدون
تغییرند.

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
testDebugUnitTest / lintDebug           → باید در CI/WSL تکرار شود
SQL / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → V40B
```


## ۶۹) V40C — مدیریت کلاس/دانش‌آموز و مجوزهای حذف

- onManageTeacher اکنون ManagerTeacherClassScreen را باز می‌کند؛ session مدیر حفظ
  می‌شود و impersonation وجود ندارد.
- RPCهای manager برای list/create/delete class، roster، school students و add/remove
  membership همگی school_id و membership manager را بررسی می‌کنند.
- my_students برای staff مدرسه school_students را برمی‌گرداند و can_manage برای
  مدیر=true و معلم فقط مالک حساب است.
- StudentProfile/DTO canManageAccount گرفت. global list فقط برای مجازها edit/delete
  نشان می‌دهد.
- ClassRoster onDelete به removeStudentFromClass وصل و contentDescription «حذف از
  کلاس» است. global list «حذف حساب دانش‌آموز» را نگه می‌دارد.
- delete class فقط public.classes را حذف می‌کند؛ profiles/Auth دست‌نخورده است.
- Edge manager برای update/delete حساب فقط پس از بررسی manager membership +
  school_students اجازه می‌دهد.

```text
sql/manual/SQL_NATIVE_MANAGER_CLASS_STUDENTS_V40C.sql
Edge deploy                             → manage-student الزامی
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
SQL copy equality                       → PASS
پیش‌نیاز                                → V40B.1
```

راهنما: `docs/fa/MANAGER_CLASS_STUDENT_PERMISSIONS_V40C_FA.md`.

## ۷۰) V40C.1 — پوشه‌بندی دائمی مستندات، SQL و فایل‌های انتشار

### ساختار جدید

```text
docs/fa/*.md             → همه Markdownها و هندآف
sql/manual/SQL_*.sql     → نسخه‌های دستی SQL
text/CHANGELOG_FA.txt → changelog مورد استفاده workflow
```

مهاجرت‌های واقعی در `supabase/migrations` و فایل‌های ضروری Gradle در ریشه باقی
ماندند. تمام pathهای workflow، verify، unit test و ارجاع‌های مستندات به مسیر جدید
به‌روزرسانی شدند. راهنما: `docs/fa/WORKSPACE_STRUCTURE_FA.md`.

```text
FINAL_NATIVE_VERIFY                     → PASS
git diff --check                        → PASS
پچ سازمان‌دهی                           → تا build در patches/pending
SQL / Edge / Secret / Dependency        → بدون تغییر
پیش‌نیاز                                → V40C
```

## ۷۱) V41A — پالایش کارت معلم و کدهای دعوت

- عنوان تکراری معلم‌ها و نام مدرسه از body حذف شد.
- کد پرسنلی/تلفن یک‌سطر، آیکن‌ها بزرگ و toggle سبز/قرمز شد.
- عنوان دعوت به TopAppBar منتقل و bottom «معلم‌ها» حالت دعوت را می‌بندد.
- تایمر هر ثانیه، تراشه وضعیت عادی/سبز/قرمز و حذف فوری کارت اضافه شد.

```text
FINAL_NATIVE_VERIFY → PASS
SQL/Edge → ندارد
پچ → pending تا build
```

## ۷۲) قانون نمایش فایل‌های تحویلی در صفحهٔ اول Workspace

از V41A به بعد این قرارداد برای تمام تحویل‌ها الزامی است:

1. جدیدترین پچ و نسخهٔ قابل مشاهدهٔ `HANDOFF_KOTLIN_MIGRATION_FA.md` ابتدا در ریشهٔ Workspace (`/home/user`) قرار می‌گیرند تا در صفحهٔ اول قابل دسترس باشند.
2. با درخواست یا تولید پچ جدید، پچ قبلی از ریشهٔ Workspace برداشته و مطابق وضعیت build به `patches/pending/` یا `patches/built/` منتقل می‌شود.
3. فایل Hand-off همیشه در ریشهٔ Workspace باقی می‌ماند و نسخهٔ آن با فایل canonical مخزن در `docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md` همگام می‌شود.
4. فایل canonical داخل مخزن در هر پچ به‌روزرسانی می‌شود؛ کپی ریشهٔ Workspace صرفاً برای دسترسی سریع است و جزو سورس Git نیست.
5. اگر پچ جدید بر پچ قبلی وابسته باشد، دستور تحویل باید پیش‌نیاز را صریح اعلام کند یا یک پچ cumulative سازگار با HEAD کاربر ارائه دهد.

## ۷۳) V41A.1 — رفع خطای کامپایل Icon

CI پچ V41A سه پرانتز پایانی جاافتاده در فراخوانی‌های `Icon` مربوط به ورود، کیف پول و حذف معلم را در `ManagerFoundationScreens.kt` آشکار کرد. هر سه فراخوانی اصلاح شدند. پچ V41A.1 یک hotfix افزایشی و فاقد تغییر SQL/Edge است.

## ۷۴) V41A.2 — عبور state عنوان دعوت به Shell

CI پس از عبور از مرحله KSP نشان داد `managerInviteHeader` در scope تابع `AuthenticatedShell` تعریف نشده است. state اکنون به‌صورت پارامتر صریح از `AuthenticatedExamApp` به `AuthenticatedShell` ارسال می‌شود؛ بنابراین عنوان TopAppBar بدون اتکا به scope بیرونی کامپایل می‌شود. این hotfix افزایشی فاقد SQL/Edge است.

## ۷۵) V41A.3 — اصلاح syntax تست V41

CI پس از کامپایل موفق سورس اصلی، خطای quoting در assertionهای تک‌خطی `V41InviteTeacherCardPolishTest` را نشان داد. تست به ساختار چندخطی خوانا بازنویسی شد و شمارش سه آیکن ۳۲dp بدون رشته‌های نقل‌قول‌دار شکننده بررسی می‌شود. این hotfix افزایشی فاقد SQL/Edge است.

## ۷۶) V41A.4 — حفظ قرارداد متن شماره تلفن V40B

CI تمام سورس و تست‌های V41 را کامپایل کرد و از ۲۹۰ تست فقط قرارداد تاریخی V40B در متن `شماره تلفن:` شکست خورد؛ علت کوتاه‌شدن برچسب به `تلفن:` بود. برچسب کامل `شماره تلفن:` بازگردانده شد و چیدمان یک‌سطر V41 بدون تغییر باقی ماند. این hotfix افزایشی فاقد SQL/Edge است.

## ۷۷) V41B — گردش‌کار امن تأیید معلم

- درخواست‌های مدیر برای ویرایش/حذف کلاس و حساب دانش‌آموز معلم در `manager_approval_requests` با انقضای ۲۴ ساعت و وضعیت‌های auditشونده ثبت می‌شوند.
- معلم از کارت «درخواست‌های مدیر» در داشبورد تأیید یا رد می‌کند.
- کلاس پس از تأیید در transaction تابع PostgreSQL اجرا می‌شود؛ حساب دانش‌آموز پس از تأیید با retry مدیر در Edge Function اجرا و `executed_at` ثبت می‌شود.
- membership کلاس عمداً به RPC V40C مستقل باقی مانده است.
- SQL canonical و manual copy باید پیش از deploy Edge اجرا شوند.

## ۷۸) V41B.1 — مجوز پروفایل، دعوت و کارت درخواست‌ها

- خطای `permission denied for function native_my_profile` با migration افزایشی grant رفع شد؛ grantهای RPC دعوت نیز برای deploymentهای ناقص تثبیت شدند.
- کارت دعوت پیش از درخواست شبکه به‌صورت optimistic حذف می‌شود و شمارش معکوس `HH:MM:SS` هر ثانیه به‌روزرسانی می‌شود.
- inbox درخواست مدیر از داشبورد آزمون حذف و فقط در کارت ششم «درخواست‌ها» و مقصد اختصاصی آن قرار گرفت.

## ۷۹) V42 — کارت‌های کلاس و دانش‌آموز در منوی مدیر

- تعداد کارت‌های منوی همبرگری مدیر/معاون از ۴ به ۶ رسید.
- کارت «کلاس‌ها» مقصد مدرسه را در حالت کلاس و کارت «دانش‌آموزان» همان مقصد را در حالت دانش‌آموز باز می‌کند.
- تقویم و سربرگ همچنان مطابق قرارداد V36 در منوی مدیر نمایش داده نمی‌شوند.
- پنجره دعوت معلم همچنان فقط فهرست و مدیریت کدهای دعوت را نمایش می‌دهد.
- این پچ فاقد SQL و Edge Function است.

## ۸۰) V42.1 — اصلاح syntax تست کارت‌های منوی مدیر

CI نشان داد دو assertion جدید V36 به‌دلیل escape نشدن نقل‌قول‌های عنوان کارت‌ها کامپایل نمی‌شوند. assertionها به بررسی مستقیم متن فارسی بدون نقل‌قول تو‌در‌تو تبدیل شدند؛ سورس اصلی V42 پیش از آن با موفقیت کامپایل شده بود. این hotfix فاقد SQL و Edge است.

## ۸۱) V42.2 — جداسازی قطعی مقصد «معلم‌ها» از دعوت

ریشهٔ باگ، local بودن `inviteMode` در `ManagerTeachersScreen` بود: لمس bottom dock فقط عنوان Header را reset می‌کرد و state صفحهٔ دعوت باز می‌ماند. اکنون `managerTeacherListKey` از Shell به صفحه ارسال می‌شود؛ هر لمس «معلم‌ها» context مدیریت معلم را null، حالت دعوت را false و فهرست معلمان را reload می‌کند. دکمه `+` همچنان تنها مسیر ورود به دعوت است. این hotfix فاقد SQL/Edge است.

## ۸۲) V42.3 — همگام‌سازی تست Header دعوت با block جدید مدیر

CI سورس اصلی و ۲۹۸ تست را با موفقیت گذراند؛ تنها assertion تاریخی V41 که عبارت تک‌خطی `if (...) managerInviteHeader = false` را الزام می‌کرد، پس از تبدیل منطق مدیر به block چندخطی V42.2 شکست خورد. assertion به بررسی رفتار پایدار `managerInviteHeader = false` و سیگنال `managerTeacherListKey += 1` تغییر کرد. این hotfix فاقد SQL/Edge است.

## ۸۳) V42.4 — رفع رقابت state بین + دعوت و دکمه معلم‌ها

علت باگ جدید باقی‌ماندن `teacherListRequested > 0` بود: هنگام ورود مجدد به صفحه با +، دو `LaunchedEffect` مستقل اجرا می‌شدند و effect فهرست پس از effect دعوت، `inviteMode` را false می‌کرد. اکنون تنها یک effect با ورودی authoritative یعنی `inviteModeRequested = managerInviteHeader` وجود دارد. مقدار true از + فقط دعوت‌ها و مقدار false از bottom dock فقط معلم‌ها را load می‌کند. بارگذاری اولیهٔ تکراری نیز حذف شد. فاقد SQL/Edge.

## ۸۴) V43 — فهرست مستقل دانش‌آموزان معلم

- جدول `teacher_student_links` ارتباط دائمی و بدون انتقال مالکیت حساب را ثبت می‌کند.
- `my_students` برای معلم فقط owner و link صریح را برمی‌گرداند؛ فهرست کل مدرسه مختص مدیر می‌ماند.
- roster کلاس متعلق به معلم همه اعضای همان کلاس را نشان می‌دهد و برای عضو خارج از فهرست دکمه «افزودن به لیست دانش‌آموزان من» دارد.
- link پس از حذف عضویت کلاس باقی می‌ماند و تأیید مدیر لازم ندارد؛ ایجاد link فقط با عضویت واقعی دانش‌آموز در کلاس `teacher_id=auth.uid()` مجاز است.
- `add_students_to_class` فقط دانش‌آموز owner/link را به کلاس‌های معلم، از جمله کلاس ساخته‌شده توسط مدیر برای او، اضافه می‌کند.

## ۸۵) V44 — فرمول درون‌متنی در متن سؤال (هم‌رفتار وب‌اپ)

### درخواست کاربر

- وقتی فرمول در متن سؤال درج می‌شود، همان‌جا به‌صورت نماد (SVG) نمایش داده شود، نه کد `$...$`.
- با لمس نماد، ویرایشگر فرمول دوباره باز شود.
- برای نمایش فرمول، کادر یا فهرست جداگانه لازم نباشد.

### تغییرات

- فایل جدید `app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt`:
  - متن سؤال را به بخش‌های متناوب متن/فرمول می‌شکند.
  - هر `$...$` به‌صورت chip با `NativeFormulaIcon` (SVG) رندر می‌شود؛ لمس نماد ویرایشگر فرمول را با همان TeX باز می‌کند.
  - دکمه × روی هر فرمول حذف مستقیم و دکمه ∑ انتهای سطر درج فرمول جدید است.
  - متن عادی بین/اطراف فرمول‌ها به‌صورت درون‌متنی قابل تایپ است؛ `$` تایپ‌شده حذف می‌شود تا ساختار `$...$` فقط از مسیر ویرایشگر ساخته شود.
- در `ExamBuilderScreen.kt` متن سؤال جایگزین شد: `OutlinedTextField` (نمایش کد خام) + پیش‌نمایش جدا `NativeMathText` + کادر جدا `ExistingFormulaEditor` حذف و یک `InlineMathTextEditor` واحد جایگزین شد.
- ذخیره همچنان با فرمت `$...$` است؛ باز/ویرایش/حذف از همان `insertFormula` / `deleteFormula` / `updateText` ViewModel موجود استفاده می‌کنند و لایه داده و بک‌اند تغییری نکرده است.
- گزینه‌ها و matching فعلاً از رفتار قبلی استفاده می‌کنند (یکسان‌سازی در ادامه قابل انجام است).

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V43
```

### بررسی

```text
git diff --check                        → PASS
FINAL_NATIVE_VERIFY                    → باید در WSL اجرا شود
testDebugUnitTest / lintDebug / assembleDebug → باید در WSL/CI اجرا شود
```

راهنمای مستقل: `docs/fa/INLINE_MATH_TEXT_EDITOR_V44_FA.md`.

## ۸۶) V44.1 — رفع پر شدن سهمیه Artifact و مسدود نشدن انتشار Supabase

علت: مرحله آپلود Artifact پیش از مرحله انتشار Supabase قرار دارد و شکست آن
(به‌دلیل پر شدن سهمیه) کل job را متوقف می‌کرد؛ در نتیجه APK هرگز در Supabase
بارگذاری نمی‌شد. سهمیه گیت‌هاب نیز فقط هر ۶ تا ۱۲ ساعت بازمحاسبه می‌شود.

اصلاح در .github/workflows/android.yml:
- continue-on-error: true → شکست آپلود Artifact دیگر job را متوقف نمی‌کند
- retention-days: 1 → artifactها پس از یک روز خودکار پاک می‌شوند

عملیات: SQL/Edge/Secret/Migration/Dependency جدید ندارد. پیش‌نیاز: V44.

## ۸۷) V45 — درج شکل و نمودار + جابجایی آیکن فرمول

### آیکن فرمول
- آیکن ∑ از داخل کادر متن سؤال حذف شد.
- نوار ابزار زیر کادر متن سؤال سه دکمه دارد: درج فرمول / درج شکل / درج نمودار.
- آیکن فرمول تکراری از QuestionMediaEditor حذف شد (فقط زیر کادر می‌ماند).

### درج شکل و نمودار (هم‌قالب وب‌اپ)
- شکل‌ها با قالب %%FIG:{json}%% درون متن سؤال ذخیره می‌شوند (سازگار با وب‌اپ).
- ۳۴ شکل هندسی و ۵ نمودار (خط، سهمی، سینوسی، نمایی، ستونی).
- رندر SVG امن و مستقل (بدون WebView) با Coil SvgDecoder.
- لمس شکل درون‌متنی → بازشدن انتخاب‌گر برای ویرایش؛ × حذف.
- نمایش شکل در آزمون‌ساز، پیش‌نمایش سؤال و نمای دانش‌آموز از طریق NativeMathText.

### فایل‌های جدید
core/figure/FigureSpec.kt, FigureGallery.kt, FigureSvgRenderer.kt
core/text/RichText.kt
ui/figure/InlineFigureView.kt, FigurePickerDialog.kt

### عملیات
SQL/Edge/Secret/Migration/Dependency جدید: ندارد. پیش‌نیاز: V44.

### محدودیت نسخه اول
- ویرایش برچسب رأس/ضلع/زاویه و پارامتر زاویه/خطوط موازی هنوز نیست (شکل با مقادیر پیش‌فرض درج می‌شود).
- شکل‌ها در خروجی PDF/چاپ A4 هنوز رندر نمی‌شوند (فقط در نمای Compose).

## ۸۷) V45 — درج شکل و نمودار + جابجایی آیکن فرمول

### آیکن فرمول
- آیکن ∑ از داخل کادر متن سؤال حذف شد.
- نوار ابزار زیر کادر متن سؤال سه دکمه دارد: درج فرمول / درج شکل / درج نمودار.
- آیکن فرمول تکراری از QuestionMediaEditor حذف شد (فقط زیر کادر می‌ماند).

### درج شکل و نمودار (هم‌قالب وب‌اپ)
- شکل‌ها با قالب %%FIG:{json}%% درون متن سؤال ذخیره می‌شوند (سازگار با وب‌اپ).
- ۳۴ شکل هندسی و ۵ نمودار (خط، سهمی، سینوسی، نمایی، ستونی).
- رندر SVG امن و مستقل (بدون WebView) با Coil SvgDecoder.
- لمس شکل درون‌متنی → بازشدن انتخاب‌گر برای ویرایش؛ × حذف.
- نمایش شکل در آزمون‌ساز، پیش‌نمایش سؤال و نمای دانش‌آموز.

### تست و verify
- تست V29 و verify_native_final.py با رفتار جدید هماهنگ شدند.
- FINAL_NATIVE_VERIFY → PASS.

### عملیات
SQL/Edge/Secret/Migration/Dependency جدید: ندارد. پیش‌نیاز: V44.

### محدودیت نسخه اول
- ویرایش برچسب رأس/ضلع/زاویه و پارامتر زاویه/خطوط موازی هنوز نیست.
- شکل‌ها در خروجی PDF/چاپ A4 هنوز رندر نمی‌شوند.

---

## ۸۵) V45.1 — رفع دانلود بروزرسانی

### گزارش دستگاه

«برنامه از قسمت بروزرسانی دانلود نمی‌شود» — نسخه جدید دیده می‌شود، دکمه
«دریافت نسخه» لمس می‌شود، اما هیچ اتفاقی نمی‌افتد (نه پیشرفت، نه خطا، نه نصب).

### علت قطعی (ممیزی سورس)

- پنجره «بروزرسانی جدید» هنگام ورود با لمس «دریافت نسخه» فوراً بسته می‌شد و
  دانلود در پس‌زمینه اجرا می‌شد؛ هیچ UI پیشرفت، نمایش خطا یا مسیر نصب خودکار
  در `ExamApp.kt` وجود نداشت.
- `ApkUpdateManager.awaitDownload` فقط `SUCCESSFUL`/`FAILED` را مدیریت می‌کرد و
  وضعیت `PAUSED` (در انتظار شبکه) تا بی‌نهایت بی‌صدا می‌ماند.
- `scripts/verify_native_final.py` قرارداد قدیمی V29 (آیکن فرمول کنار دوربین)
  را الزام می‌کرد در حالی که V45 عمداً آن را به زیر کادر متن منتقل کرده بود؛
  `FINAL_NATIVE_VERIFY` روی HEAD خام FAIL بود و CI پچ بعدی را بلاک می‌کرد.

### تغییرات

```text
ExamApp.kt:
- دیالوگ UpdatePromptDialog با سه حالت آماده/دانلود/خطا؛ هنگام دانلود باز می‌ماند
- نوار پیشرفت + «در انتظار اتصال اینترنت…» + متن بایت/مگابایت
- خطای واقعی + «تلاش دوباره» + «دریافت با مرورگر»
- نصب خودکار پس از کامل‌شدن دانلود با مسیر مجوز نصب (مانند صفحه درباره)

AboutScreen.kt:
- متن پیشرفت مشترک state.progressText + دکمه «دریافت با مرورگر» هنگام خطا

ApkUpdateManager.kt:
- waitingForNetwork در پیشرفت؛ تشخیص توقف ۱۲۰ ثانیه‌ای بی‌صدا و لغو با پیام روشن

UpdateViewModel.kt:
- UpdateState.waitingForNetwork و progressText

scripts/verify_native_final.py:
- قرارداد آیکن فرمول با طراحی V45 هماهنگ شد → FINAL_NATIVE_VERIFY=PASS
```

### تست

```text
FINAL_NATIVE_VERIFY                      → PASS
V45_1UpdateDownloadFixTest               → اضافه شد
git diff --check                         → PASS
testDebugUnitTest / lintDebug / assembleDebug → باید در WSL/GitHub Actions اجرا شود
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V45 (HEAD a7e60f6)
```

راهنمای مستقل: `docs/fa/UPDATE_DOWNLOAD_FIX_V45_1_FA.md`.

---

## ۸۶) V45.2 — رفع 404 بررسی بروزرسانی در GitHub Actions

### گزارش واقعی

```text
Supabase public update RPC status: 404
Error: Process completed with exit code 1.
```

مرحله «بررسی امن اتصال عمومی Supabase» در CI با 404 متوقف می‌شود و APK ساخته
و منتشر نمی‌شود.

### علت

- از V11 به بعد این مرحله RPC `check_app_update` را صدا می‌زند؛ 404 یعنی
  تابع در پروژه‌ای که `SUPABASE_URL` اشاره می‌کند وجود ندارد.
- برنامه Kotlin از این RPC استفاده نمی‌کند؛ مستقیماً جدول `app_version` را
  با کلید anon و RLS می‌خواند. تابع فقط «سازگار برای کلاینت‌های دیگر» است و
  نباید CI را بلاک کند.

### تغییرات

```text
.github/workflows/android.yml:
- بررسی اتصال عمومی حالا مسیر واقعی برنامه را تست می‌کند:
  GET /rest/v1/app_version?select=version_code&is_active=eq.true&limit=1
- RPC جانبی check_app_update فقط به‌صورت اطلاعاتی چاپ می‌شود و بلاک نمی‌کند

sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql (جدید):
- تشخیص وجود تابع/جدول + drop-first تابع قدیمی WebView (حل 42P13) و بازسازی
  استاندارد تابع برای کلاینت‌های دیگر

scripts/verify_native_final.py:
- دو require جدید: بررسی CI باید مسیر app_version را تست کند و RPC جانبی
  نباید بلاک‌کننده باشد

V45_2CiUpdateCheckFixTest (جدید):
- رگرسیون سه‌گانه: مسیر app_version، اطلاعاتی‌بودن RPC، فایل بازسازی SQL
```

### تست

```text
FINAL_NATIVE_VERIFY                      → PASS
V45_2CiUpdateCheckFixTest                → اضافه شد
git diff --check                         → PASS
testDebugUnitTest / lintDebug            → باید در WSL/GitHub Actions اجرا شود
```

### عملیات برای کاربر

1) اجرای `sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql` در SQL
Editor پروژه اصلی و بررسی خروجی تشخیصی (وجود تابع و جدول).
2) اگر `app_version_table` هم null بود، `SUPABASE_URL` در GitHub Secrets با
پروژه اصلی `eazwuyrymsvdkwckdpco` مقایسه شود.
3) اعمال پچ V45.2، commit و push، اجرای دوباره GitHub Actions.

### عملیات

```text
SQL جدید: sql/manual/SQL_NATIVE_RESTORE_CHECK_APP_UPDATE_V452.sql (اجرای دستی)
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V45 (یا V45.1)
```

راهنمای مستقل: `docs/fa/CI_UPDATE_CHECK_FIX_V45_2_FA.md`.


---

## ۸۷) V45.2.1 — رفع خطای کامپایل دانلود بروزرسانی

### گزارش واقعی CI

```text
e: .../ir/exam/app/core/update/ApkUpdateManager.kt:33:21 Unresolved reference 'PAUSED_WAITING_FOR_WIFI'
> Task :app:compileDebugKotlin FAILED
```

### علت

در پچ V45.1 ثابت `PAUSED_WAITING_FOR_WIFI` در `NETWORK_PAUSE_REASONS` استفاده
شده بود، اما این ثابت در `android.app.DownloadManager` وجود ندارد. دو ثابت
دیگر همان مجموعه (`PAUSED_WAITING_FOR_NETWORK` و `PAUSED_QUEUED_FOR_WIFI`)
معتبرند.

### اصلاح

```text
ApkUpdateManager.kt:
- حذف PAUSED_WAITING_FOR_WIFI از NETWORK_PAUSE_REASONS؛ رفتار تشخیص
  «در انتظار شبکه» با دو ثابت معتبر حفظ می‌شود

V45_1UpdateDownloadFixTest:
- assert معتبر PAUSED_QUEUED_FOR_WIFI + assert منفی عدم وجود
  PAUSED_WAITING_FOR_WIFI

مستندات و changelog: به‌روزرسانی شدند
```

### تست

```text
FINAL_NATIVE_VERIFY                      → PASS
V45_1 / V45_2 regression tests           → به‌روزرسانی شد
git diff --check                         → PASS
testDebugUnitTest / lintDebug            → باید در WSL/GitHub Actions اجرا شود
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V45.2 (شامل V45.1)
```

راهنمای مستقل: `docs/fa/COMPILE_HOTFIX_V45_2_1_FA.md`.

---

## ۸۸) V45.2.2 — هماهنگ‌سازی دو تست رگرسیون قدیمی CI

### گزارش واقعی CI

در اجرای `./gradlew testDebugUnitTest lintDebug`، کامپایل Kotlin، پردازش KSP
و lint تا مرحلهٔ اجرای تست‌ها پیش رفتند، اما دو assertion منبع‌محور قدیمی شکست
خوردند:

```text
V29ReorderViewerEditBulkTest > formula icon sits in the same row as the question camera FAILED
    java.lang.AssertionError at V29ReorderViewerEditBulkTest.kt:76

V31StableReorderUpdatePromptBulkTest > app entry checks for updates and shows a prompt when one exists FAILED
    java.lang.AssertionError at V31StableReorderUpdatePromptBulkTest.kt:91

313 tests completed, 2 failed
> Task :app:testDebugUnitTest FAILED
```

پیام اولیهٔ daemon با `code: 0` علت نهایی نبود؛ daemon دوباره آماده شد و
تست‌ها اجرا شدند. هشدارهای deprecated نیز خطا نیستند.

### علت قطعی

- تست V29 هنوز وجود `Icons.Outlined.Functions` و `onFormula` را در
  `QuestionMediaEditor` می‌خواست. طراحی V45 عمداً آیکن فرمول را به
  `InlineMathTextEditor`، زیر کادر متن سؤال، منتقل کرده و در رسانه فقط دوربین
  باقی مانده است.
- تست V31 هنوز فراخوانی مستقیم `updateViewModel.downloadAndInstall()` داخل
  `onClick` را می‌خواست. طراحی V45.1 این عمل را با callback واقعی دیالوگ وصل
  می‌کند: `onDownload = updateViewModel::downloadAndInstall`؛ به این ترتیب
  دیالوگ هنگام دانلود بسته نمی‌شود و پیشرفت/خطا را نمایش می‌دهد.

هر دو شکست assertion منسوخ‌شده بودند و از خطای جدید در کد اجرایی خبر نمی‌دادند.

### اصلاح

```text
V29ReorderViewerEditBulkTest.kt:
- بررسی دوربین در QuestionMediaEditor
- بررسی آیکن فرمول در InlineMathTextEditor و اتصال onInsertFormula در builder

V31StableReorderUpdatePromptBulkTest.kt:
- بررسی callback واقعی onDownload به‌جای ساختار قدیمی onClick

docs/fa/REGRESSION_TEST_ALIGNMENT_V45_2_2_FA.md:
- گزارش علت قطعی، دستور WSL و وضعیت عملیات
```

کد اجرایی برنامه، SQL، Edge Function، Secret، Migration و Dependency جدیدی در
این اصلاح تغییر نکرده است.

### تست و عملیات

```text
FINAL_NATIVE_VERIFY                     → باید PASS بماند
git diff --check                        → باید PASS باشد
testDebugUnitTest / lintDebug           → پس از اعمال V45.2.2 اجرا شود
```

پیش‌نیاز اعمال پچ: V45.2.1. فایل پچ:
`patches/pending/V45_2_2_regression_test_alignment.patch`.
پس از سبزشدن Actions، مرحلهٔ بعد `assembleRelease`، انتشار APK و آزمایش واقعی
دانلود و نصب روی دستگاه است.

راهنمای مستقل: `docs/fa/REGRESSION_TEST_ALIGNMENT_V45_2_2_FA.md`.

---

## ۸۹) V45.3 — فرمول واقعاً درون‌خطی و درج دو مرحله‌ای شکل/نمودار

### درخواست تأییدشده

در کادر متن سؤال باید بتوان بعد از درج فرمول، ادامهٔ جمله را نوشت و فرمول
بین دو بخش متن بماند؛ همچنین لمس «درج شکل» فقط پنجرهٔ شکل و لمس «درج نمودار»
فقط پنجرهٔ نمودار را باز کند. قبل از ویرایش، نوع شکل یا نمودار باید انتخاب شود.

### اصلاح فرمول

```text
RichTextSplitter.kt:
- بخش خالی متن در ابتدا/انتهای توکن‌ها را هم نگه می‌دارد.
- بعد از فرمول پایانی یک Text خالی ساخته می‌شود تا ادامهٔ جمله قابل تایپ باشد.

InlineMathTextEditor.kt:
- کادرهای متن دارای محتوا دیگر با fillMaxWidth کل سطر را نمی‌گیرند.
- FlowRow متن، فرمول و ادامهٔ متن را کنار هم می‌چیند و فقط در صورت پرشدن واقعی
  عرض، wrapping انجام می‌شود.
```

قالب ذخیره‌سازی فرمول (`$...$`) و بازسازی متن بدون توکن تغییر نکرده است.

### اصلاح شکل و نمودار

```text
FigurePickerDialog.kt:
- FigureTypePickerDialog فقط نوع‌های شکل هندسی یا فقط نوع‌های نمودار را،
  بر اساس آیکن انتخاب‌شده، نمایش می‌دهد.
- FigurePickerDialog اکنون مرحلهٔ ویرایش همان نوع انتخاب‌شده است.
- GeometryEditorPane برچسب‌ها/اندازه‌های مربوط و پیش‌نمایش را ویرایش می‌کند.
- GraphEditorPane پارامترها، عنوان و پیش‌نمایش همان نمودار را ویرایش می‌کند.
- تب مشترک شکل/نمودار حذف شد.

ExamBuilderScreen.kt:
- انتخاب آیکن، target را در مرحلهٔ chooseType قرار می‌دهد.
- پس از انتخاب template، فقط همان target به مرحلهٔ ویرایش منتقل می‌شود.
```

برای شکل/نمودار موجود، لمس خود شکل مستقیماً ویرایشگر همان نوع را باز می‌کند.

### تست و عملیات

```text
V45_3InlineFigureFlowTest       → اضافه شد
RichTextTest                    → پوشش محل تایپ بعد از فرمول اضافه شد
FINAL_NATIVE_VERIFY             → PASS
git diff --check                → PASS
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
```

پیش‌نیاز: V45.2.2. فایل پچ:
`patches/pending/V45_3_inline_formula_figure_flow.patch`.
پس از اجرای Actions، باید `testDebugUnitTest lintDebug` سبز شود و سپس
`assembleRelease` و آزمایش دستی کادر متن و هر دو جریان درج انجام شود.

راهنمای مستقل: `docs/fa/INLINE_FORMULA_FIGURE_FLOW_V45_3_FA.md`.

---

<<<<<<< HEAD
## ۹۰) V45.4 — جایگزینی ویرایشگر فرمول بومی با WebView (استخراج کد به کد از 66.html)

### هدف

حذف کامل ویرایشگر فرمول بومی (Compose) و جایگزینی آن با **همان صفحهٔ ویرایشگر
فرمول نسخهٔ وب 66.html**، به‌صورت «بدون تغییر و کد به کد» — یعنی asset استخراج‌شده
بایت‌به‌بایت با رشتهٔ `MATH_EDITOR_HTML` در 66.html یکسان است و هیچ خطی از آن
ویرایش نشده است. نمایش فرمول‌ها در متن سؤال/گزینه/تصحیح/گزارش/چاپ PDF
**Native می‌ماند** (تغییری نکرده است)؛ فقط ورودی/ویرایش فرمول WebView شده است.

### استخراج و تأیید asset

```text
مبدأ: رشتهٔ MATH_EDITOR_HTML در 66.html (اسکریپت math-host-script)
خروجی: app/src/main/assets/math_editor_standalone.html
اندازه: 624,209 بایت
SHA-256: aae5777f9fb8705ccb2ed4a7c52e426e44ab45c7280055f936ed0aff4e917ceb
تأیید: استخراج با دو مفسر مستقل (Python + Node.js) — خروجی یکسان (identical=true)
منابع خارجی در صفحه: ندارد (کاملاً آفلاین؛ فقط localStorage برای فرمول‌های اخیر)
```

### حذف‌شده (ویرایشگر بومی و فقط وابستگی‌های خودش)

```text
app/src/main/java/ir/exam/app/ui/math/:
  FormulaEditorDialog.kt      (دیالوگ/پد ویرایشگر بومی، 1296 خط)
  FormulaSmartHubDialog.kt
  FormulaLibraryDialog.kt
  FormulaLibraryNavigator.kt
  FormulaReferenceLibrary.kt
  FormulaReferenceStore.kt
  FormulaSmartReference.kt
app/src/main/java/ir/exam/app/core/math/:
  FormulaBoxEditor.kt         (محدود به ویرایشگر)
  FormulaMatrixFactory.kt     (محدود به ویرایشگر)
app/src/main/assets/formula_library_v13.json  (کتابخانهٔ بومی؛ صفحهٔ وب کتابخانهٔ خودش را دارد)
app/src/test/.../FormulaBoxEditorTest.kt, FormulaMatrixFactoryTest.kt,
  ui/math/FormulaReferenceAssetTest.kt, FormulaLibraryNavigatorTest.kt,
  FormulaSmartReferenceTest.kt
```

### نگه‌داشته‌شده (نمایش/چاپ — طبق تصمیم «editor_only»)

```text
- NativeFormulaView.kt / NativeMathText.kt          → رندر فرمول در نمایش
- NativeMathAst.kt / NativeMathSvgRenderer.kt / NativeMathCanvasRenderer.kt
- NativeMathFormatter.kt / NativeNaturalMathConverter.kt / FormulaTextCodec.kt
- چاپ PDF (OfficialPdfPrintAdapter.kt / PdfExamRenderer.kt) — بدون تغییر
- ExistingFormulaEditor (FormulaInlineEditor.kt) — چیپ‌های ویرایش/حذف فرمول موجود
- InlineMathTextEditor.kt — کادر متن سؤال با چیپ فرمول‌ها
```

### فایل جدید

```text
app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt
اجرای صفحهٔ ویرایشگر مستقل در WebView ایزوله + Bridge اندروید
امضای کاملاً سازگار با FormulaEditorDialog قدیمی:
  MathEditorWebViewDialog(initialTex, onDismiss, onInsert)
نقطهٔ اتصال: ExamBuilderScreen.kt — فقط import و نام دیالوگ عوض شد؛
  FormulaTarget و viewModel.insertFormula بدون هیچ تغییری
```

### پروتکل هدایت صفحهٔ وب (بدون تغییر در خود صفحه)

```text
1) qTxt_1.value = "$initialTex$"  (برای درج جدید: "$$") و انتخاب یک‌سرهٔ آن
2) openMath('qTxt_1')  → ویرایشگر با همان فرمول قبلی باز می‌شود
3) mfApply (ثبت): کل انتخاب را با "$tex$" جایگزین می‌کند و خودش closeMath می‌زند
   → Bridge مقدار نهایی را می‌گیرد، $…$ را باز می‌کند → onInsert(tex)
4) closeMath (بدون ثبت) → onDismiss
تزریق: فقط wrap کردن mfApply/closeMath در لحظهٔ انتها (همان الگوی bridge
میزبان در 66.html)؛ هیچ تغییری در فایل asset داده نشده است.
```

### ویژگی‌های WebView

```text
- javaScriptEnabled + domStorageEnabled (فرمول‌های اخیر localStorage صفحه)
- allowFileAccess برای asset محلی؛ هیچ دسترسی شبکه/محتوا داده نمی‌شود
- all navigations مسدود (shouldOverrideUrlLoading → true)
- آماده‌بودن صفحه با poll (80ms تا 8s) مثل میزبان 66.html
- دکمهٔ بازگشت Android → closeMath → بستن بدون ثبت
- پیام خطا + «تلاش مجدد» اگر صفحه بارگیری نشود
```

### تست و عملیات

```text
V19InteractionTest:
  - تست قبلی «FormulaEditorDialog.kt» به MathEditorWebViewDialog.kt منتقل شد
    (بررسی asset، آدرس صفحه، پل و openMath('qTxt_1'))
  - تأیید وجود function openMath(targetId) و function mfApply() در asset
git diff --check    → PASS
SQL / Edge Function / Secret / Migration / Dependency جدید: ندارد
minSdk 26: WebView همیشه در دسترس است؛ تنظیمات خاصی لازم نیست
```

پیش‌نیاز: V45.3. بعد از اجرای CI انتظار `testDebugUnitTest lintDebug` سبز است.
آزمایش دستی: ساخت آزمون → متن سؤال → «∑ فرمول» → ویرایش فرمول موجود، درج
فرمول جدید، فرمول‌های اخیر، بستن با دکمهٔ بازگشت، و چاپ/پیش‌نمایش (نمایش فرمول
در PDF همچنان Native است).

راهنمای مستقل: `docs/fa/MATH_EDITOR_WEBVIEW_V45_4_FA.md`.


---

## ۹۱) V45.4.1 — هات‌فیکس final-verify پس از جایگزینی ویرایشگر فرمول

### علت

پس از push پچ V45.4، CI روی مرحلهٔ زیر شکست خورد:

```text
Run python3 scripts/verify_native_final.py
FileNotFoundError: .../FormulaEditorDialog.kt (فایل حذف‌شده)
```

`scripts/verify_native_final.py` هنوز به فایل‌های ویرایشگر بومی حذف‌شده
ارجاع می‌داد و چند بررسی (مثل ممنوعیت `android.webkit`) باید برای معماری
جدید WebView بازتعریف می‌شد.

### تغییرات اسکریپت

```text
- formula_editor → MathEditorWebViewDialog.kt
- حذف readهای فایل‌های حذف‌شده (FormulaBoxEditor / FormulaReferenceLibrary /
  FormulaLibraryDialog / FormulaLibraryNavigator / FormulaSmartHubDialog /
  FormulaSmartReference) و حذف بررسی‌های مربوط به آن‌ها
- formula_library_v13.json → math_editor_standalone.html
  (اندازه > 500KB + وجود function openMath(targetId) و function mfApply())
- محدودیت android.webkit: دقیقاً فقط MathEditorWebViewDialog.kt مجاز است
  (بررسی شمارشی = ۱ فایل با همین نام)
- بررسی‌های نگه‌داشت رندر Native (SVG/canvas/codec/natural) حفظ شد
- بررسی‌های جدید V45.4: full-screen WebView + AndroidMathBridge +
  قرارداد qTxt_1/openMath/mfApply/closeMath + اتصال در ExamBuilderScreen +
  نبودِ هیچ باقی‌ماندهٔ ویرایشگر بومی در main_text
- چک LTR ویرایشگر (مربوط به پد بومی) حذف شد؛ LTR نمایشی حفظ شد
```

### نتیجه

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
python3 scripts/verify_native_final.py → EXIT 0
```

پیش‌نیاز: V45.4. بعد از push، CI باید سبز شود.

## ۹۲) V45.4.2 — هماهنگ‌سازی تست رگرسیون Neumorphic69 با WebView ویرایشگر

### علت

پس از push پچ‌های V45.4 و V45.4.1، مرحلهٔ verify سبز شد اما
`testDebugUnitTest` روی یک تست قدیمی شکست:

```text
Neumorphic69IntegrationTest > native shell uses dual shadows without demo data or web runtime FAILED
    java.lang.AssertionError at Neumorphic69IntegrationTest.kt:179
297 tests completed, 1 failed
```

خط ۱۷۹ هنوز قانون قدیمی «هیچ فایلی در main نباید `android.webkit` داشته
باشد» را چک می‌کرد؛ در حالی که از V45.4 دقیقاً یک فایل مجاز
(`MathEditorWebViewDialog.kt`) میزبان WebView ویرایشگر فرمول است.

### تغییر (فقط همان یک assert)

فایل: `app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt`

```text
- assertFalse("WebView must not enter native runtime", "android.webkit" in mainSources)
+ فایل‌های دارای "android.webkit" شمارش می‌شوند و باید دقیقاً برابر
+ [MathEditorWebViewDialog.kt] باشند (همان قانونی که در V45.4.1 به
+ scripts/verify_native_final.py اضافه شد).
```

بقیهٔ بررسی‌های تست (نئومورفیک، بستهٔ دمو، موجودی جعلی کیف پول) دست‌نخورده
ماند. هیچ فایل دیگری تغییر نکرد.

### نتیجه

```text
python3 scripts/verify_native_final.py → FINAL_NATIVE_VERIFY=PASS (EXIT 0)
git diff --check → PASS
```

پیش‌نیاز: V45.4 و V45.4.1. بعد از push، انتظار می‌رود CI کاملاً سبز شود
(تنها تست شکست‌خورده همین بود؛ ۲۹۶ تست دیگر سبز بودند).

## ۹۳) V45.5 — بازگردانی کامل ویرایشگر فرمول به نسخهٔ بومی (revert پچ‌های V45.4 تا V45.4.2)

### علت

پس از نصب APK حاوی ویرایشگر WebView (پچ‌های V45.4/V45.4.1/V45.4.2)، کاربر
گزارش داد: «ویرایشگر باگ دارد؛ هیچ چیز نشان نمی‌دهد» (صفحهٔ خالی روی دستگاه
واقعی). به تصمیم کاربر، همهٔ تغییرات این مسیر برگردانده شد و ویرایشگر
فرمول بومی همان نسخهٔ v45.3 (کامیت `4f1757a`) دوباره برقرار است.

### چه چیزهایی برگشت (دقیقاً وضعیت v45.3)

```text
بازگردانده شد:
- ui/math/FormulaEditorDialog.kt / FormulaSmartHubDialog.kt /
  FormulaLibraryDialog.kt / FormulaLibraryNavigator.kt /
  FormulaReferenceLibrary.kt / FormulaReferenceStore.kt /
  FormulaSmartReference.kt
- core/math/FormulaBoxEditor.kt / FormulaMatrixFactory.kt
- assets/formula_library_v13.json
- ۵ تست حذف‌شده (FormulaBoxEditorTest، FormulaMatrixFactoryTest،
  FormulaReferenceAssetTest، FormulaLibraryNavigatorTest،
  FormulaSmartReferenceTest)
- ExamBuilderScreen.kt (اتصال دوباره به FormulaEditorDialog)
- V19InteractionTest.kt و Neumorphic69IntegrationTest.kt (نسخهٔ v45.3)
- scripts/verify_native_final.py (نسخهٔ v45.3 — بدون استثنای WebView)

حذف شد:
- app/src/main/assets/math_editor_standalone.html
- ui/math/MathEditorWebViewDialog.kt
- .gitattributes
- docs/fa/MATH_EDITOR_WEBVIEW_V45_4_FA.md
```

تنها تفاوت درخت با `4f1757a` همین سند هندآف است (بخش‌های ۹۰ تا ۹۳ برای
سابقه نگه داشته شده‌اند؛ SHA-256 asset و پروتکل bridge در بخش ۹۰ ثبت است
تا در صورت تلاش دوباره در آینده قابل استفاده باشد).

### نتیجه

```text
python3 scripts/verify_native_final.py → FINAL_NATIVE_VERIFY=PASS (EXIT 0)
git diff --check → PASS
```

پس از push، CI باید مانند v45.3 سبز شود و اپ همان ویرایشگر فرمول بومی
قبلی را داشته باشد.
=======
## ۱۱۹. بازگشت به V45.3 و ادغام کتابخانه جامع کتب درسی و موضوعی (V50.1)

**تاریخ:** ۲۰۲۶-۰۸-۲۳

### هدف

کدبیس با حفظ ساختار نیتیو و بدون WebView نسخهٔ V45.3، به تمام ۶۴ پد و دسته‌بندی کتابخانهٔ تخصصی کتب درسی دبیرستان و کنکور و مباحث موضوعی مجهز شد (مجموعاً ۱۴۱ دسته‌بندی و ۲٬۸۳۷ فرمول/نماد).

### تغییرات اعمال‌شده

1. **کتابخانه فرمول (`formula_library_v13.json`):**
   - ادغام ۶۴ دسته‌بندی تخصصی شامل کتب درسی (ریاضی ۱۰، ۱۱، ۱۲، حسابان ۱ و ۲، هندسه ۱، ۲ و ۳، گسسته، آمار و احتمال، انسانی، فیزیک دهم، یازدهم، دوازدهم، شیمی دهم، یازدهم، دوازدهم، زیست‌شناسی و دانشگاه).
   - اضافه شدن گروه نهم «📚 کتب درسی و تکمیلی» در کنار ۸ گروه پایه‌ای و تخصیص دسته‌ها به گروه‌های مربوطه.
2. **رندر و پارسر نیتیو (`NativeMathAst.kt` و `NativeMathSvgRenderer.kt`):**
   - پشتیبانی ۱۰۰٪ از دستورات و نمادهای لاتک تکمیلی (`\binom`, `\bot`, `\top`, `\vdash`, `\models`, `\neg`, `\mid`, `\nmid`, `\setminus`, `\subsetneq`, `\nsubseteq`, `\vee`, `\wedge`, `\triangle`, `\ell`, `\ddot`, `\tilde`, `\overbrace`, `\underbrace`, `\overset`, `\underset`, `\xrightarrow`, `\pmod`).
3. **مرکز هوشمند فرمول (`FormulaSmartReference.kt`):**
   - اضافه شدن درس کتب درسی با قالب‌های آماده کتب مدارس ایران به Smart Hub.
4. **تست و اعتبارسنجی:**
   - تست کامل AST، رندر برداری SVG، صحت تمام ۲٬۸۳۷ فرمول و پاس شدن `FINAL_NATIVE_VERIFY=PASS`.

>>>>>>> 5f6bd91

## V50.7.1 — نمایش شرطی جعبه‌های ویرایشگر فرمول

جعبه‌های ساختاری فرمول پس از درج فرمول نمایش داده نمی‌شوند. در مقدار خالی، خانهٔ خالی قابل مشاهده است؛ پس از لمس یک خانه، فقط خانهٔ فعال نمایش داده می‌شود. این رفتار در رندر SVG نیتیو اعمال شد و درج/کیپد همچنان کار می‌کند.

```text
SQL / Edge / Secret / Dependency جدید: ندارد
FINAL_NATIVE_VERIFY → PASS
```


---

## ۱۲۳) V52 — پالایش ویرایشگر فرمول، کیبورد و کتابخانه

**تاریخ:** ۲۰۲۶-۰۸-۲۳

### تحویل

- رفع کرش آیکن کیبورد نرم‌افزاری: حذف WebView جدا و `requestFocus` داخل `runCatching`.
- نوار دسته‌ها در سه سطر تخصصی به‌علاوه میان‌بر کتب/مباحث/گروه‌ها.
- پنجره کتابخانه بدون جستجو، نام دسته، دکمه درج و ستاره؛ علاقه‌مندی فقط با لمس ۲ ثانیه‌ای.
- مکان‌نما به‌صورت پیش‌فرض در بالای کادر؛ پس از درج یا لمس، نشانگر جلوی فرمول می‌ماند.
- پرانتز کیپد یک دکمه است و پنجره چپ/راست/جفت باز می‌کند؛ دکمه فاصله اضافه شد.
- جمله «خانه خالی را لمس کنید» و پیام تیک سبز «فرمول درج شد» حذف شدند.
- فرمول تازه‌درج‌شده ابتدا کدر است و با لمس شفاف می‌شود.
- حد زیر `lim` و حدود انتگرال بالا/پایین نماد رسم می‌شوند.
- ۲۴ فرمول بریده‌شده کتابخانه (هوپیتال، مشتق، تشابه، دوپلر و …) اصلاح شد.

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V45.3
```


## ۱۲۴) V53.1 — کادر متن سؤال WebView، نوار ۸ آیکن Native و جدول Native

### درخواست و تصمیم‌ها

```text
درخواست: متن سؤال جدید جایگزین قبلی؛ آیکن‌های جدول/آناتومی/تناوبی/فیزیک/شیمی
         کنار فرمول/شکل/نمودار؛ همهٔ آیکن‌ها Native به‌جز فرمول و متن سؤال.
تأیید کاربر: ویرایشگر کاملاً Native برای ۵ ابزار (سه مرحله V53.1..V53.3)،
             کادر متن سؤال WebView، رندر دانش‌آموز/PDF در همین نقشه.
فایل ارسالی question_editor.html: جایگزین نشد — با asset مخزن یکسان بود و فقط
             یک اسکریپت tracking Cloudflare اضافه داشت؛ asset مخزن تمیزتر است.
پچ pending قبلی (V50_0_revert_to_v45_3.patch): طبق انتخاب کاربر دست‌نخورده ماند.
```

### تحویل V53.1

```text
QuestionTextFieldWebView + QuestionEditorFieldController   → کادر متن سؤال WebView محلی و امن
اسکریپت افزودهٔ exam-editor-native-tools در asset           → درج توکن در مکان‌نما /
                                                              openTool / onOverlayChanged
پرچم ?nativeTools=1                                         → مخفی‌سازی toolbar داخلی HTML
                                                              فقط برای کادر متن سؤال
QuestionToolIcons (ImageVector خالص از SVGهای مرجع)          → ۸ آیکن Native با ترتیب مرجع:
                                فرمول، شکل، نمودار، جدول، آناتومی، تناوبی، فیزیک، شیمی
QuestionTextWebSection                                      → جایگزین InlineMathTextEditor
                                                              در کارت سؤال Builder
TableEditorDialog (کاملاً Native)                            → ۱۸ سبک مرجع، ۱..۱۵×۱..۱۰،
                                عنوان، ویرایش خانه‌ها، پیش‌نمایش زنده، نمونهٔ هر سبک
TableSvgRenderer                                            → SVG امن با قواعد isHead/sample مرجع
FigureSpec.buildTable/tableCells/kind/isTable               → همان قرارداد {k:'t',t,X,C} مرجع
FigureSvgRenderer                                           → k='t' به TableSvgRenderer؛
                                k∈{a,p,s} پلاک عنوان‌دار امن تا V53.2/V53.3
OfficialPdfPrintAdapter                                     → %%FIG%% با AndroidSVG به تصویر
                                برداری در PDF (قبلاً JSON خام چاپ می‌شد)
شکل/نمودار                                                  → همان ویرایشگرهای Native V45.3؛
                                خروجی در محل مکان‌نمای WebView درج می‌شود
آناتومی/تناوبی/فیزیک/شیمی                                   → آیکن Native از الان؛ ابزار مرجع
                                داخل WebView تا تحویل V53.2/V53.3
```

### امنیت

```text
WebView مجاز: FormulaEditorDialog / QuestionEditorWebView /
              QuestionEditorWebViewDialog / QuestionTextFieldWebView (فهرست verify)
ناوبری خارجی مسدود، دسترسی فایل خاموش، Secret/token صفر
SVG جدول: XML-escaped، بدون script/href/foreignObject
کد مرجع HTML دست‌نخورده؛ فقط بلوک پل افزوده شد
```

### عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency: androidsvg-aar:1.4 (صریح‌سازی وابستگی transitive موجود coil-svg)
پیش‌نیاز: V52
```

### تست V53.1

```text
FINAL_NATIVE_VERIFY                    → PASS (اجرای محلی + قرارداد جدید V53.1)
V53WebFieldNativeToolsTableTest        → 6 تست منبع‌محور اضافه شد
Neumorphic69IntegrationTest            → فهرست WebView مجاز به‌روزرسانی شد
testDebugUnitTest / lintDebug / build  → باید در WSL/GitHub Actions اجرا شود
```

### باقی‌مانده

```text
V53.2 → جدول تناوبی Native (۱۱۸ عنصر فارسی + ویرایشگر + رندر دانش‌آموز/PDF)
V53.3 → آناتومی + فیزیک/شیمی Native (اطلس asset + ویرایشگر + رندر) + رگرسیون کل
```

راهنمای مستقل: `docs/fa/WEB_FIELD_NATIVE_TOOLS_TABLE_V53_1_FA.md`.


## ۱۲۵) V53.1.1 — هماهنگ‌سازی دو تست با طراحی قطعی V53.1

### گزارش واقعی CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
332 tests                                       → 330 PASS / 2 FAIL
V29ReorderViewerEditBulkTest.kt:85              → assertion قدیمی InlineMathTextEditor/FormulaTarget("question")
V53WebFieldNativeToolsTableTest.kt:104          → کلمات href/foreignObject در «کامنت مستندات» TableSvgRenderer
                                                   با جست‌وجوی متنی ساده برخورد می‌کردند
```

کد اجرایی V53.1 سالم کامپایل شده بود؛ هر دو شکست فقط تستی بودند:

۱) تست V29 هنوز الزام می‌کرد فرمول از نوار InlineMathTextEditor با
`FormulaTarget("question")` باز شود؛ در طراحی قطعی V53.1 نوار زیر کادر متن،
`QuestionTextWebSection` است و فرمول با `controller.openTool("formula")` داخل
همان WebView باز می‌شود. assertion با همین قرارداد جدید هماهنگ شد.

۲) تست V53 وجود واژه‌های `href` و `foreignObject` را در کل فایل ممنوع کرده بود،
اما این واژه‌ها فقط در کامنت مستندات فارسی فایل بودند نه markup تولیدی.
assertion به بررسی tag/attr واقعی (`href=`، `<foreignObject`، `<style`، `<script`)
دقیق شد.

### فایل‌ها و عملیات

```text
app/src/test/java/ir/exam/app/ui/app/V29ReorderViewerEditBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V53WebFieldNativeToolsTableTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
کد اجرایی برنامه                      → بدون تغییر
SQL / Edge / Secret / Dependency      → ندارد
پیش‌نیاز                              → V53.1 (اعمال‌شده روی HEAD کاربر)
FINAL_NATIVE_VERIFY                   → PASS
git diff --check                      → PASS
testDebugUnitTest / lintDebug         → باید در CI تکرار شود
```


## ۱۲۶) V53.2 — جدول تناوبی کاملاً Native

### وضعیت ورودی

```text
V53.1 + V53.1.1 build/device            → SUCCESS (اعلام کاربر)
آیکن تناوبی                             → از V53.1 نیتیو بود؛ عملکرد هنوز ابزار WebView مرجع بود
```

### تحویل

```text
PeriodicElements.kt        → استخراج برنامه‌ای ۱۱۸ عنصر مرجع (نماد/نام فارسی/گروه/دوره/دسته)
                             + ۱۱ دسته رنگی CATS و نام فارسی CN
PeriodicSvgRenderer.kt     → SVG امن (svg/rect/text): شبکه ۱۸×۷، ارقام فارسی،
                             ستاره‌های */** گروه ۳ دوره ۶/۷، بلوک جدا لانتانید/اکتینید،
                             احترام کامل به X مرجع: Z/hid/hidZ/hideCols/hideRows/hideF/title
PeriodicEditorDialog.kt    → ویرایشگر کاملاً Native: ۴ حالت مرجع (کامل/گروه اصلی/بدون f/
                             بدون عدد اتمی)، دو حالت لمس (حذف عنصر/حذف عدد اتمی)،
                             لمس سرستون/دوره برای حذف ستون/سطر، chipهای بازگردانی،
                             بازگردانی همه، عنوان
FigureSpec                 → buildPeriodic + xIntList؛ قرارداد {k:'p',...} مرجع بدون تغییر
FigureSvgRenderer          → k='p' به رندر Native؛ پلاک موقت فقط برای k∈{a,s}
QuestionTextWebSection     → آیکن تناوبی به ویرایشگر Native (onInsertPeriodic)؛
                             openTool("periodic") حذف شد
ExamBuilderScreen          → periodicTarget + درج در محل مکان‌نمای WebView با fallback
رندر دانش‌آموز/PDF          → خودکار از مسیر مشترک V53.1 (NativeMathText / figureBitmap)
```

### سازگاری داده

توکن‌های k='p' ساخته‌شده با WebView قدیمی در Native رندر می‌شوند و برعکس؛
هیچ مهاجرت داده‌ای لازم نیست.

### محدودیت ثبت‌شده

ویرایش دوبار-کلیک توکن موجود داخل WebView هنوز ابزار مرجع را باز می‌کند؛
اتصال آن به ویرایشگرهای Native در V53.3 تحویل می‌شود.

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V53.1 + V53.1.1
```

### تست V53.2

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V53.2: دقیقاً ۱۱۸ عنصر،
                                          مسیر مشترک k='p'، ویرایشگر بدون WebView،
                                          برچسب‌های مرجع، SVG امن)
V53_2PeriodicNativeTest                → ۵ تست منبع‌محور جدید؛ شبیه‌سازی محلی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```

### باقی‌مانده

```text
V53.3 → آناتومی + فیزیک/شیمی Native + ویرایش دوبار-کلیک Native + رگرسیون کل
```

راهنمای مستقل: `docs/fa/PERIODIC_NATIVE_V53_2_FA.md`.


## ۱۲۷) V53.3 — آناتومی + فیزیک/شیمی Native و ویرایش دوبار-کلیک (پایان V53)

### وضعیت ورودی

```text
V53.2 build/device                      → SUCCESS (اعلام کاربر)
باقی‌مانده از نقشه V53                  → آناتومی/فیزیک/شیمی + ویرایش dblclick
```

### تحویل

```text
اطلس تصاویر asset          → استخراج برنامه‌ای ۶۷ تصویر آناتومی + ۷۰ تصویر علوم از
                              base64 مرجع به assets/figure_atlas (~2.3MB)
AtlasCatalog               → ۶۷ نوع آناتومی + کپشن + ۱۵ دسته؛ ۷۰ نوع علوم +
                              ۸ دسته فیزیک + ۶ دسته شیمی؛ ۷۷ نگاشت فایل با aliasها؛
                              scienceDomain همان inferDomain مرجع
AtlasEditorDialog          → ویرایشگر Native: دسته‌بندی، thumbnail، نشانه‌گذاری
                              لمسی شماره‌دار (سقف ۱۲ مثل مرجع، شمارهٔ آزاد بعدی)،
                              برچسب/حذف نشانه، سوییچ‌های عنوان/جای پاسخ/نمایش نام‌ها،
                              پیش‌فرض‌های مرجع bodyF/cSim/beak
AtlasFigureView            → نمای دانش‌آموز/Builder: تصویر + Canvas فلش‌ها + جای پاسخ
AtlasBitmapRenderer        → چاپ/PDF بدون WebView
AtlasMarkPainter           → هندسهٔ خالص فلش/ارقام فارسی (تست‌پذیر JVM)
FigureSpec                 → marks()/buildAtlas/AtlasMark با قرارداد درصدی مرجع
ویرایش دوبار-کلیک          → dblclick توکن k∈{t,p,a,s} در WebView (فقط nativeTools=1)
                              → ExamEditorNative.onEditFigure → ویرایشگر Native همان نوع
                              → applyEditedToken جایگزینی همان توکن / cancelEditToken
آیکن‌ها                    → آناتومی/فیزیک/شیمی هم اکنون Native؛ هیچ openTool ابزار
                              مرجع در نوار نمانده (فرمول WebView مصوب است)
```

### سازگاری داده

قرارداد `{k:'a'|'s', t, X:{title,lab,blank,mkName,marks[]}}` مرجع بدون تغییر؛
توکن‌های قدیمی WebView در Native رندر/ویرایش می‌شوند و برعکس. هندسه/نمودار
(k خالی) عمداً همان مسیر مرجع GeoFig را در dblclick نگه داشتند.

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V53.2
حجم APK: ~2.3MB بیشتر (تصاویر اطلس asset)
```

### تست V53.3

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V53.3: شمارش دقیق ۶۷/۷۰
                                          نوع و ۶۷/۷۰ فایل asset، بدون webkit در
                                          اجزای اطلس، مسیر dblclick کامل)
V53_3AtlasNativeTest                   → ۶ تست منبع‌محور جدید؛ شبیه‌سازی محلی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```

### وضعیت نقشه V53

```text
V53.1 ✔ / V53.2 ✔ / V53.3 ← این پچ
پس از build موفق این پچ، نقشهٔ V53 کامل است؛ هر ۸ ابزار متن سؤال Native
(به‌جز فرمول و کادر متن که به انتخاب صریح کاربر WebView هستند).
```

راهنمای مستقل: `docs/fa/ATLAS_NATIVE_V53_3_FA.md`.


## ۱۲۸) V53.3.1 — هماهنگ‌سازی تست V53.2 با مسیر متمرکز deliverFigure

### گزارش واقعی CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
343 tests                                       → 342 PASS / 1 FAIL
V53_2PeriodicNativeTest.kt:96                   → assertion به متن inline قدیمی V53.2 وابسته بود
```

کد اجرایی V53.3 سالم کامپایل شد. علت شکست: V53.3 منطق درج/ویرایش هر چهار
ابزار را عمداً در تابع متمرکز `deliverFigure` گذاشت (برای پشتیبانی جایگزینی
توکن dblclick)؛ تست V53.2 هنوز متن inline قدیمی
`insertFigureJson(spec.toJson())` را مستقیم داخل بلوک `periodicTarget?.let`
الزام می‌کرد. assertion اکنون قرارداد جدید را بررسی می‌کند: عبور بلوک از
`deliverFigure(spec, target.occurrenceIndex)` و وجود درج مکان‌نما + fallback
داخل خود `deliverFigure`. اسکن سایر تست‌های V53 الگوی کهنهٔ مشابه پیدا نکرد.

### فایل‌ها و عملیات

```text
app/src/test/java/ir/exam/app/ui/app/V53_2PeriodicNativeTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
کد اجرایی برنامه                      → بدون تغییر
SQL / Edge / Secret / Dependency      → ندارد
پیش‌نیاز                              → V53.3 (اعمال‌شده روی HEAD کاربر)
FINAL_NATIVE_VERIFY                   → PASS
git diff --check                      → PASS
testDebugUnitTest / lintDebug         → باید در CI تکرار شود
```


## ۱۲۹) V53.4 — پنجرهٔ فرمول تمام‌صفحهٔ WebView و رفع سه اشکال دستگاه

### گزارش واقعی دستگاه (پس از V53.3)

```text
۱) در کادر متن سؤال، یک کادر دیگر (قاب/برچسب HTML) داخل قاب Native دیده می‌شد
۲) با زدن آیکن فرمول صفحه تاریک می‌شد و ویرایشگر باز نمی‌شد
۳) درخواست کاربر: پنجرهٔ فرمول همه‌جا کاملاً WebView (تمام‌صفحه) باشد
۴) کتابخانهٔ نمودار Native فقط ۵ نوع دارد؛ مرجع ۶۱ نوع
```

### علت قطعی اشکال ۲

iframe داخلی `mathEditorFrame` مرجع با `position:fixed; inset:0` نسبت به viewport
همان WebView کوچک ۳۰۰dp باز می‌شد؛ boot ویرایشگر داخل آن قاب کوچک عملاً فقط
پس‌زمینهٔ تیره را نشان می‌داد. راه‌حل: در حالت `nativeTools=1`، تابع
`__openMathEditor` بازتعریف شد تا متن + محدودهٔ انتخاب (شامل محدودهٔ فرمول در
حال ویرایش از `_qmfPending`) با `ExamEditorNative.onOpenFormula` به Native برود.

### تحویل

```text
FormulaHostDialog (WebView مصوب جدید)   → Dialog تمام‌صفحه با asset محلی و حالت
                                           ?formulaHost=1: پوستهٔ صفحه مخفی و
                                           ویرایشگر فرمول مرجع مستقیم باز می‌شود؛
                                           ExamEditorFormula.begin(text,selStart,selEnd)
پایان کار                               → بسته‌شدن ویرایشگر مرجع (overlay=false پس
                                           از باز شدن) متن نهایی را به Native برمی‌گرداند
متن سؤال                                → onOpenFormula → FormulaHostTarget →
                                           updateText + sync کادر WebView
گزینه‌ها و جورکردنی                     → همان پنجرهٔ تمام‌صفحه (انتخاب کاربر:
                                           «همه‌جا»)؛ متن کامل فیلد + محدودهٔ
                                           occurrence فرمول برای ویرایش/درج؛
                                           AlertDialog کوچک V45.4 از Builder حذف شد
کادر دوم                                → قاب/برچسب/padding داخلی HTML در حالت
                                           nativeTools با CSS تزریقی مخفی شد؛ فقط
                                           قاب Native می‌ماند
verify                                  → FormulaHostDialog.kt به فهرست WebView مجاز
                                           اضافه شد + قرارداد V53.4
```

### تصمیم کاربر دربارهٔ نمودار

تکمیل کتابخانهٔ نمودار (۶۱ نوع مرجع در برابر ۵ نوع Native فعلی) به‌صورت
«همه Native در چند پچ» انتخاب شد → نقشهٔ V54 (چندمرحله‌ای) پس از build این پچ.

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V53.3 + V53.3.1
کد مرجع HTML: دست‌نخورده؛ فقط بلوک‌های افزودهٔ nativeTools/formulaHost
```

### تست V53.4

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V53.4)
V53_4FormulaHostFrameTest              → ۴ تست منبع‌محور جدید؛ شبیه‌سازی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```

### باقی‌مانده

```text
V54 (چندمرحله‌ای) → تکمیل Native کتابخانهٔ نمودار تا ۶۱ نوع مرجع
```


## ۱۳۰) V53.4.1 — افزودن FormulaHostDialog به فهرست WebView مجاز تست Neumorphic

### گزارش واقعی CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
347 tests                                       → 346 PASS / 1 FAIL
Neumorphic69IntegrationTest.kt:180              → «WebView must not enter native runtime»
```

کد اجرایی V53.4 سالم کامپایل شد. علت شکست: تست Neumorphic فهرست استثنای WebView
مخصوص به خود را دارد و در V53.4 فقط فهرست `verify_native_final.py` به‌روزرسانی
شده بود؛ `FormulaHostDialog.kt` (WebView مصوب پنجرهٔ تمام‌صفحهٔ فرمول) به فهرست
تست اضافه نشده بود. اکنون هر دو فهرست هم‌ارز هستند. اسکن شبیه‌سازی‌شده:
هیچ `android.webkit` خارج از فهرست مجاز وجود ندارد.

### فایل‌ها و عملیات

```text
app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
کد اجرایی برنامه                      → بدون تغییر
SQL / Edge / Secret / Dependency      → ندارد
پیش‌نیاز                              → V53.4 (اعمال‌شده روی HEAD کاربر)
FINAL_NATIVE_VERIFY                   → PASS
git diff --check                      → PASS
testDebugUnitTest / lintDebug         → باید در CI تکرار شود
```


## ۱۳۱) V54.1 — مرحلهٔ اول تکمیل کتابخانهٔ نمودار Native

### وضعیت ورودی

```text
V53.4 + V53.4.1 build/device            → SUCCESS (اعلام کاربر) — نقشه V53 کامل شد
کتابخانهٔ نمودار مرجع                   → ۶۱ نوع؛ Native تا این پچ فقط ۵ نوع
تصمیم کاربر                             → تکمیل کامل Native در چند پچ (نقشه V54)
```

### تحویل V54.1 — ۲۰ نوع جدید

```text
ChartSvgRenderer.kt (جدید)  → pie / donut / lchr / area / step / sarea / hbar /
                               cmp / hcmp / stack / st100 / scat / bub / hist /
                               pareto / gauge / radar / combo / lolli / funn
قرارداد داده                → همان کلیدهای X مرجع: labs/vals/vals2/vals3/s1..s3/
                               xs/ys/zs/val/vmin/vmax؛ توکن‌های WebView قدیمی
                               بدون مهاجرت رندر می‌شوند
مسیر مشترک                  → FigureSvgRenderer به ChartSvgRenderer مسیر می‌دهد؛
                               isGeometry انواع جدید را نمودار می‌شناسد →
                               دانش‌آموز/چاپ/PDF/ویرایش دوبار-کلیک خودکار
گالری                       → FigureGallery از ۵ به ۲۵ قالب با نام فارسی مرجع
ویرایشگر                    → paramFields با برچسب فارسی fieldsFor مرجع؛
                               تفکیک کلیدهای متنی/عددی (TEXT_PARAM_KEYS)؛
                               پیش‌فرض‌های قالب در فرم initialParams
```

### باقی‌ماندهٔ نقشه V54 (ثبت‌شده، نه پنهان)

```text
V54.2 → box/ohlc/fall/ctrl/venn/tree/sun/waff/pict/heat/hmap/bull/pyra/mekko
V54.3 → flow/gantt/time/dumb/slope/spark/stream/viol/strip/stem/rose/word +
        map/surf/smat/dend/sank/chrd/netw/bmap/calh/plot و رگرسیون کل نقشه
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V53.4.1
```

### تست V54.1

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V54.1: هر ۲۰ نوع در
                                          رندرگر و گالری، مسیر مشترک، SVG امن)
V54_1ChartLibraryStage1Test            → ۴ تست منبع‌محور جدید؛ شبیه‌سازی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```


## ۱۳۲) V54.2 — مرحلهٔ دوم کتابخانهٔ نمودار Native

### وضعیت ورودی

```text
V54.1 build/device                      → SUCCESS (اعلام کاربر)
پوشش نمودار پس از V54.1                 → ۲۵ نوع از ۶۱ نوع مرجع
```

### تحویل V54.2 — ۱۴ نوع جدید (مجموع ۳۹)

```text
ChartSvgRendererStage2.kt (جدید) → box جعبه‌ای / ohlc سهام / fall آبشاری /
                                    ctrl کنترلی (mean/UCL/LCL خودکار یا دستی) /
                                    venn ون ۲و۳تایی / tree نقشه درختی /
                                    sun خورشیدی دوحلقه / waff وافل ۱۰×۱۰ /
                                    pict پیکتوگرام با unit / heat کانتور /
                                    hmap حرارتی / bull گلوله‌ای با هدف /
                                    pyra هرم جمعیت / mekko مکّو
قرارداد داده                     → کلیدهای X مرجع: mins/q1s/meds/q3s/maxs،
                                    opens/highs/lows/closes، mean/ucl/lcl،
                                    n/ab/ac/bc/abc، labs2/vals2، rows/cols، unit
اعداد فارسی                      → faFloat: ارقام ۰..۹ فارسی و ممیز ٫ مرجع
مسیر مشترک                       → SUPPORTED = STAGE1 + Stage2 → دانش‌آموز/PDF/
                                    dblclick خودکار
گالری                            → از ۲۵ به ۳۹ قالب با نام فارسی مرجع
ویرایشگر                         → paramFields مرحلهٔ دوم با برچسب‌های fieldsFor
                                    مرجع؛ کلیدهای چندمقداری متنی ذخیره می‌شوند
```

### باقی‌ماندهٔ نقشه V54

```text
V54.3 → flow/gantt/time/dumb/slope/spark/stream/viol/strip/stem/rose/word/
        map/surf/smat/dend/sank/chrd/netw/bmap/calh/plot (۲۲ نوع پایانی)
        + رگرسیون کل نقشه V54
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V54.1
```

### تست V54.2

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V54.2)
V54_2ChartLibraryStage2Test            → ۴ تست منبع‌محور جدید؛ شبیه‌سازی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```


## ۱۳۳) V54.3 — مرحلهٔ پایانی کتابخانهٔ نمودار Native (پایان نقشه V54)

### وضعیت ورودی

```text
V54.2 build/device                      → SUCCESS (اعلام کاربر)
پوشش نمودار پس از V54.2                 → ۳۹ نوع از ۶۱ نوع مرجع
```

### تحویل V54.3 — ۲۲ نوع پایانی (پوشش ۶۱/۶۱)

```text
ChartSvgRendererStage3.kt (جدید) →
  plot محور مختصات شبکه‌دار / flow فلوچارت (شکل تصمیم با «؟») /
  gantt گانت (شروع/مدت) / time تایملاین متناوب بالا-پایین /
  dumb دمبل / slope شیب دوستونه / spark اسپارک‌لاین با نقطهٔ پایانی /
  stream جریانی متقارن سه‌سری / viol ویولن / strip نوار نقطه‌ای jitter قطعی /
  stem ساقه و برگ (گروه‌بندی ده‌دهی) / smat ماتریس پراکندگی ۳×۳ /
  dend دندروگرام ادغام جفتی / sank سنکی (قالب A-C:8) /
  chrd کورد (ماتریس سطری) / netw شبکه‌ای (یال‌های A-B) /
  map نقشه‌ای choropleth شبکه‌ای / bmap نقشه حبابی /
  surf سطحی ایزومتریک (nrows/ncols) / calh تقویم حرارتی هفتگی از شنبه /
  rose گل رز قطبی ۸جهته / word ابر واژه مارپیچی قطعی
قرارداد داده                     → کلیدهای X مرجع بدون تغییر؛ sank/netw قالب‌های
                                    رشته‌ای مرجع (A-C:8 / A-B) را parse می‌کنند
مسیر مشترک                       → SUPPORTED = STAGE1+2+3 → دانش‌آموز/PDF/dblclick
گالری                            → از ۳۹ به ۶۱ قالب؛ تست پوشش خودکار «هر id
                                    داخل TYPES مرجع باید قالب Native داشته باشد»
                                    (col با قالب bar هم‌ارز مرجع پوشش دارد)
ویرایشگر                         → paramFields پایانی با برچسب‌های fieldsFor مرجع
```

### پایان نقشه V54

```text
V54.1 ✔ (۲۰ نوع) / V54.2 ✔ (۱۴ نوع) / V54.3 ← این پچ (۲۲ نوع)
پس از build موفق این پچ، کتابخانهٔ نمودار Native با پوشش کامل ۶۱/۶۱ نوع مرجع
تمام است؛ verify و تست JVM پوشش را برای همیشه قفل می‌کنند.
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V54.2
```

### تست V54.3

```text
FINAL_NATIVE_VERIFY                    → PASS (+ قرارداد V54.3 و پوشش ۶۱/۶۱ خودکار)
V54_3ChartLibraryFinalTest             → ۵ تست منبع‌محور جدید؛ شبیه‌سازی PASS
git diff --check                       → PASS
testDebugUnitTest / lintDebug          → باید در CI اجرا شود
```


## ۱۳۴) V54.3.1 — هماهنگ‌سازی تست V54.2 و رفع باگ enforcement اسکریپت verify

### گزارش واقعی CI

```text
compileDebugKotlin / compileDebugUnitTestKotlin → SUCCESS
360 tests                                       → 359 PASS / 1 FAIL
V54_2ChartLibraryStage2Test.kt:43               → needle تک‌خطی قدیمی SUPPORTED
```

### دو علت قطعی

۱) تست V54.2 متن تک‌خطی `SUPPORTED = STAGE1 + Stage2.SUPPORTED` را الزام می‌کرد؛
V54.3 عمداً این تعریف را چندخطی سه‌مرحله‌ای کرد (`STAGE1 + Stage2 + Stage3`).
assertion اکنون قرارداد پایدار را بررسی می‌کند: وجود `Stage2.SUPPORTED` داخل
تعریف `val SUPPORTED` (بدون حساسیت به شکستن خط یا افزودن مرحله‌های بعدی).

۲) باگ ساختاری کشف‌شده در `verify_native_final.py`: بلوک
`if errors: FAIL/sys.exit(1)` فقط یک‌بار در میانهٔ فایل (پیش از بلوک‌های الحاقی
V53.x/V54.x) اجرا می‌شد؛ بنابراین همهٔ requireهای بلوک‌های جدید فقط به
`errors` اضافه می‌شدند و هرگز enforce نمی‌شدند — verify محلی همیشه PASS چاپ
می‌کرد حتی وقتی قرارداد کهنهٔ V54.2 واقعاً شکسته بود (به همین دلیل شکست فقط در
CI دیده شد). بررسی نهایی `errors` پیش از چاپ PASS اضافه شد؛ از این پس همهٔ
قراردادهای V53.1 تا V54.3 واقعاً اجرا می‌شوند. needle کهنهٔ V54.2 نیز در خود
verify به بررسی declaration پایدار اصلاح شد.

### فایل‌ها و عملیات

```text
app/src/test/java/ir/exam/app/ui/app/V54_2ChartLibraryStage2Test.kt
scripts/verify_native_final.py
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
کد اجرایی برنامه                      → بدون تغییر
SQL / Edge / Secret / Dependency      → ندارد
پیش‌نیاز                              → V54.3 (اعمال‌شده روی HEAD کاربر)
FINAL_NATIVE_VERIFY                   → PASS با enforcement کامل (EXIT=0)
git diff --check                      → PASS
testDebugUnitTest / lintDebug         → باید در CI تکرار شود
```


## ۱۳۵) V54.4 — برابری بایت‌به‌بایت کادر متن سؤال و پنجرهٔ فرمول با مرجع

### گزارش واقعی دستگاه (پس از build موفق V54.3.1)

```text
۱) کادر متن سؤال «وب‌ویو به‌نظر نمی‌رسید»
۲) آیکن فرمول → صفحهٔ سفید؛ بازگشت → پیام «ویرایشگر متن سؤال بارگیری نشد»
۳) درخواست صریح: پنجرهٔ فرمول و کادر متن دقیقاً و بایت‌به‌بایت مانند
   question_editor.html باشند
```

### سه علت قطعی و اصلاح

```text
۱) ظاهر غیرمرجع کادر:
   CSS تزریقی V53.4 برچسب/قاب/padding مرجع را حذف می‌کرد و Compose قاب و برچسب
   خودش را می‌کشید — نتیجه شبیه کادر Native بود نه صفحهٔ مرجع.
   → حذف کامل CSS دستکاری قاب از asset و حذف قاب/برچسب Compose؛ اکنون markup و
     استایل مرجع بایت‌به‌بایت رندر می‌شود؛ تنها استثنا مخفی‌کردن نوار ابزار
     داخلی است (آیکن‌ها در Compose هستند).

۲) پیام کاذب «بارگیری نشد» و صفحهٔ سفید:
   onReceivedError برای خطای «هر subresource» شلیک می‌شد — از جمله favicon
   خودکار مرورگر روی دامنهٔ محلی بدون DNS — نه فقط صفحهٔ اصلی. در پنجرهٔ فرمول
   نیز پوستهٔ میزبان visibility:hidden بود و تا boot ویرایشگر فقط سفیدی دیده می‌شد.
   → onError فقط برای request.isForMainFrame؛ مسیرهای خارج از /question-editor/
     پاسخ خالی امن (نه null) می‌گیرند؛ مخفی‌سازی پوستهٔ میزبان حذف شد و پس‌زمینهٔ
     Dialog همان رنگ صفحهٔ مرجع (#e9eef5) شد.

۳) پنجرهٔ فرمول غیرمرجع:
   X شناور Compose روی پنجره اضافه بود.
   → حذف X و هر عنصر Compose؛ پنجره WebView خالص تمام‌صفحه است؛ بستن با
     دکمه‌های خود ویرایشگر مرجع (رویداد overlay=false) یا Back سیستم که متن
     نهایی را برمی‌گرداند.
```

### سایر تحویل‌ها

```text
بازنویسی تمیز بلوک پل asset (exam-editor-native-tools) با حفظ کامل قراردادهای
V53.1..V53.4: درج توکن مکان‌نما، dblclick چهار نوع، applyEditedToken،
onOpenFormula، ExamEditorFormula.begin و onOverlayChanged
closeOverlays جدید: Back سیستم لایه‌های تمام‌صفحهٔ مرجع (فرمول/ابزارها) داخل
WebView کادر متن را می‌بندد (BackHandler وقتی overlay باز است)
هماهنگی تست V53.4 و قرارداد verify با طراحی قطعی V54.4
```

### فایل‌ها

```text
app/src/main/assets/question_editor/question_editor.html   (بازنویسی بلوک پل)
app/src/main/assets/question_editor/version.txt
app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt
app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt
app/src/test/java/ir/exam/app/ui/app/V54_4ReferenceParityFixTest.kt   (جدید)
app/src/test/java/ir/exam/app/ui/app/V53_4FormulaHostFrameTest.kt
scripts/verify_native_final.py
text/CHANGELOG_FA.txt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
کد مرجع HTML: دست‌نخورده؛ فقط بلوک افزودهٔ پل بازنویسی شد
پیش‌نیاز: V54.3.1
FINAL_NATIVE_VERIFY (با enforcement کامل)  → PASS, EXIT=0
V54_4ReferenceParityFixTest                → ۵ تست جدید؛ شبیه‌سازی PASS
git diff --check                           → PASS
testDebugUnitTest / lintDebug              → باید در CI اجرا شود
```


## ۱۳۶) V54.5 — رفع boot نشدن ویرایشگر فرمول و تشخیص امن خطای JS

### گزارش واقعی دستگاه (دو اسکرین‌شات پس از V54.4)

```text
کارت سؤال: ظاهر مرجع کادر متن درست شد (برچسب/قاب مرجع، بدون قاب تکراری) ✔
آیکن فرمول: پنجرهٔ تمام‌صفحه باز می‌شود و پوستهٔ مرجع را سالم نشان می‌دهد،
            اما ویرایشگر فرمول boot نمی‌شود و صفحه بی‌واکنش می‌ماند
```

### علت قطعی

`shouldOverrideUrlLoading` در هر دو WebView برای «همهٔ» ناوبری‌ها true
برمی‌گرداند. برخلاف مرورگر دسکتاپ، WebView اندروید ناوبری فریم‌های فرعی
(iframe `mathEditorFrame` مرجع هنگام `document.open/write`) را هم از همین مسیر
عبور می‌دهد؛ true برگرداندن برای فریم فرعی، بارگذاری سند ویرایشگر را بی‌صدا
لغو می‌کرد — بدون هیچ خطایی، چون همهٔ فراخوانی‌ها در try/catch بی‌صدا بودند و
WebChromeClient هم وجود نداشت که console را نشان دهد.

### اصلاح

```text
هر دو WebView: فریم فرعی هرگز مسدود نمی‌شود (return false)؛ برای main frame
فقط مقصدهای غیر از exam-editor.local/about مسدود می‌مانند (امنیت حفظ شد).
asset: گیرندهٔ سراسری error/unhandledrejection با پیام پاک‌سازی‌شده (URL حذف)،
گزارش به ExamEditorNative.onError؛ begin بدون catch بی‌صدا؛ نگهبان ۷ثانیه‌ای
FORMULA_BOOT_TIMEOUT اگر iframe نمایان نشود.
FormulaHostDialog: WebChromeClient برای خطاهای console (سطح ERROR، پاک‌سازی
URL) + نمایش امن «خطای ویرایشگر: …» پایین پنجره به‌جای صفحهٔ بی‌واکنش.
```

اگر پس از این پچ باز هم ویرایشگر باز نشد، پیام قرمز پایین پنجره خطای واقعی
را نشان می‌دهد و اصلاح بعدی بر اساس همان پیام خواهد بود، نه حدس.

### فایل‌ها و عملیات

```text
app/src/main/assets/question_editor/question_editor.html
app/src/main/assets/question_editor/version.txt
app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt
app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt
app/src/test/java/ir/exam/app/ui/app/V54_5FormulaBootDiagnosticsTest.kt (جدید)
text/CHANGELOG_FA.txt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
کد مرجع HTML: دست‌نخورده؛ فقط بلوک پل افزوده
پیش‌نیاز: V54.4
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | git diff --check → PASS
testDebugUnitTest / lintDebug → باید در CI اجرا شود
```


## ۱۳۷) V54.6 — رفع بازگشت بی‌نهایت پل exam-editor-bridge

### گزارش واقعی دستگاه (اسکرین‌شات پس از V54.5)

```text
پنجرهٔ فرمول باز می‌شود اما به‌جای ویرایشگر، صفحهٔ متن سؤال دیده می‌شود و
پایین صفحه پیام تشخیصی جدید V54.5 خطای واقعی را نشان می‌دهد:
CONSOLE: Uncaught RangeError: Maximum call stack size exceeded
```

یعنی زیرساخت تشخیص V54.5 دقیقاً کار خودش را کرد و خطای واقعی را آشکار کرد.

### علت قطعی

زنجیرهٔ بازگشت بی‌نهایت در پل «افزودهٔ» exam-editor-bridge (از دورهٔ V45.4؛
کد مرجع سالم است):

```text
input روی qTxt_main
→ listener پل: emit()
→ value(): QMF.syncFromSurface(t)
→ writeSrc مرجع: dispatchEvent('input')   ← رویداد مصنوعی
→ دوباره listener پل: emit() → ... تا سقف پشته
```

در صفحهٔ formulaHost که begin() بلافاصله رویداد input واقعی می‌فرستد، این
چرخه فوراً منفجر می‌شد و کل JS صفحه — از جمله boot ویرایشگر فرمول — می‌مرد.
در حالت کادر کوچک متن سؤال هم همین چرخه با هر تایپ در کمین بود.

### اصلاح (فقط در بلوک افزودهٔ پل؛ کد مرجع دست‌نخورده)

```text
۱) قفل reentry برای emit (var emitting)
۲) احترام به پرچم مرجع _qmfFromSurface: در رویداد مصنوعی writeSrc دوباره
   syncFromSurface صدا نمی‌شود؛ مقدار همان لحظه گزارش می‌شود
۳) حذف qMathSync تکراری از listener پل (خود مرجع در upgrade به input گوش
   می‌دهد و رندر می‌کند)
```

### فایل‌ها و عملیات

```text
app/src/main/assets/question_editor/question_editor.html   (فقط بلوک پل bridge)
app/src/main/assets/question_editor/version.txt
app/src/test/java/ir/exam/app/ui/app/V54_6BridgeRecursionFixTest.kt (جدید)
text/CHANGELOG_FA.txt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V54.5
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | git diff --check → PASS
testDebugUnitTest / lintDebug → باید در CI اجرا شود
```


## ۱۳۸) V54.7 — مسیر جایگزین srcdoc برای boot ویرایشگر فرمول در WebView

### گزارش دستگاه (پس از V54.6)

```text
recursion رفع شد (خطای قرمز نیست، placeholder سالم است)
اما آیکن فرمول هنوز فقط «نمای کلی صفحه» را نشان می‌دهد؛ ویرایشگر باز نمی‌شود
درخواست کاربر: تست و بررسی کن
```

### تست اجرایی واقعی (نه فقط تحلیل متن)

برای اولین بار صفحه با jsdom (مرورگر استاندارد headless) واقعاً اجرا شد:

```text
حالت عادی (doc.write سالم):
  ExamEditorFormula.begin() → frame display=block ، mfModal=«modal open
  box-fullscreen» ، openMath/mfApply آماده ، overlay=true به Native
  → مسیر مرجع در مرورگر استاندارد ۱۰۰٪ سالم است
حالت شبیه‌سازی WebView معیوب (document.write خنثی):
  دقیقاً همان علامت دستگاه: display هرگز block نمی‌شود
```

نتیجهٔ قطعی: شکست دستگاه مخصوص رفتار `document.write` روی iframe در WebView
اندروید است (سند intercepted از دامنهٔ مجازی)؛ کد مرجع بی‌گناه است.

### اصلاح (بلوک افزودهٔ ExamEditorFormula؛ کد مرجع دست‌نخورده)

```text
پس از __openMathEditor، اگر تا ۲.۵ ثانیه openMath/mfApply/closeMath داخل iframe
نیامد: iframe با srcdoc = همان MATH_EDITOR_HTML مرجع بازسازی می‌شود (Android
WebView srcdoc را از 4.4 کامل اجرا می‌کند) و پس از آماده‌شدن، دوباره
__openMathEditor «مرجع» صدا زده می‌شود. چون ready مرجع هنوز false است، boot
مرجع poll تازه روی iframe جدید می‌سازد؛ installBridge ، تم میزبان و کل ادامهٔ
مسیر ۱۰۰٪ همان کد مرجع است. مسیر عادی دست‌نخورده و fallback فقط در نبود
ویرایشگر فعال می‌شود (در مرورگر استاندارد هرگز).
تشخیص‌های مرحله‌بندی‌شده: FALLBACK_UNAVAILABLE / SRCDOC_BOOT_TIMEOUT /
OPEN_MATH_RETRY / FORMULA_BOOT_TIMEOUT (۱۲ثانیه).
```

### صحت‌سنجی fallback با تست اجرایی

```text
جایگزینی iframe                         → PASS (nf !== f)
srcdoc = MATH_EDITOR_HTML کامل (579KB)  → PASS (شامل openMath و </script> سالم)
عدم فعال‌شدن fallback در مسیر سالم      → PASS (srcdoc=false در حالت عادی)
محدودیت jsdom: srcdoc را اجرا نمی‌کند؛ اجرای نهایی srcdoc فقط روی دستگاه
قابل تأیید است — به همین دلیل تشخیص‌های مرحله‌بندی‌شده حفظ شدند.
```

### فایل‌ها و عملیات

```text
app/src/main/assets/question_editor/question_editor.html (بلوک ExamEditorFormula)
app/src/main/assets/question_editor/version.txt
app/src/test/java/ir/exam/app/ui/app/V54_7SrcdocFallbackTest.kt (جدید)
text/CHANGELOG_FA.txt
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V54.6
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | git diff --check → PASS
```


## ۱۳۹) V55 — پنجرهٔ فرمول: فایل مستقل formula.html کاربر

### ورودی

```text
کاربر فایل formula.html (نسخهٔ standalone ویرایشگر فرمول، 840KB) را فرستاد:
«با لمس آیکن فرمول، این فایل در برنامه باز شود»
تصمیم‌های کاربر: بلوک پل افزوده مجاز / بستن = برگشت به برنامه /
همه‌جا (متن سؤال + گزینه‌ها + جورکردنی) / tracking کلادفلر دست نخورد
```

### ساختار فایل کاربر (ممیزی)

```text
همان ویرایشگر مرجع اما standalone: textarea مخفی qTxt_1 + mfModal +
بلوک auto-open (باز شدن خودکار پنجره + بازگشایی خودکار پس از هر بستن)
mfApply: درج $tex$ در qTxt_1 و closeMath
دو بلوک tracking کلادفلر انتهای فایل (بی‌اثر در برنامه؛ شبکهٔ خارجی مسدود)
```

### تحویل

```text
asset جدید: app/src/main/assets/formula_editor/formula.html
  = فایل کاربر + یک بلوک پل افزوده (exam-formula-native-bridge) قبل از auto-open:
  - ExamFormulaHost.begin(text, selStart, selEnd): متن فیلد مقصد در qTxt_1 +
    ریست پرچم جلسه؛ auto-open مرجع خودش پنجره را باز می‌کند
  - wrapper بیرونی closeMath: بستن واقعی + گزارش متن نهایی
    (onTextChanged) + onEditorClosed به Native
  - خنثی‌سازی بازگشایی خودکار مرجع فقط پس از بستن (پرچم __aoNativeClosing)؛
    با begin بعدی ریست می‌شود
  - گیرندهٔ خطای JS با پیام پاک‌سازی‌شده (بدون URL)
FormulaHostDialog: بارگیری formula-editor/formula.html؛ سرو فقط پوشهٔ
  formula_editor؛ پایان کار با رویداد صریح onEditorClosed (نه polling)
دامنهٔ اعمال: متن سؤال + گزینه‌ها + جورکردنی (مسیر واحد FormulaHostDialog از V53.4)
مسیر srcdoc-fallback V54.7 در question_editor برای dblclick فرمول داخل کادر
  متن بدون تغییر ماند (آن مسیر جدا است و آسیبی ندیده)
```

### تست اجرایی jsdom (نه فقط تحلیل متن)

```text
begin('متن اولیه ',10,10)            → mfModal open=true
حالت تایپ سریع «1/2 + x^2» + mfApply → متن نهایی: «پیش $\frac{1}{2} + x^{2}$»
پس از درج                            → پنجره بسته می‌ماند (بازگشایی خودکار خنثی)
رویدادها به Native                   → onTextChanged + onEditorClosed
جلسهٔ دوم begin                      → پنجره دوباره باز می‌شود (ریست پرچم)
```

### هماهنگی تست/verify

```text
V53_4 / V54_4 needles → قرارداد V55 (ExamFormulaHost / onEditorClosed / پوشهٔ جدید)
verify: قرارداد V55 + به‌روزرسانی سه needle قدیمی host
اسکن سراسری سازگاری needle تست‌ها با سورس: صفر mismatch
V55StandaloneFormulaTest: ۳ تست جدید
```

### عملیات

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
حجم APK: ~0.8MB بیشتر (asset فرمول مستقل)
پیش‌نیاز: V54.7
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | git diff --check → PASS
testDebugUnitTest / lintDebug → باید در CI اجرا شود
```


## ۱۴۰) V55.1 — رفع صفحهٔ سفید پنجرهٔ فرمول (race زمان‌بندی WebView)

### گزارش دستگاه (پس از V55)

```text
با زدن دکمهٔ فرمول فقط صفحهٔ سفید باز می‌شود.
سرنخ قطعی اسکرین‌شات: badge «v36 · V34: ✓ 64» خود فایل در گوشهٔ صفحه دیده
می‌شود → JS فایل کامل اجرا شده؛ فقط مودال باز نشده است.
```

### علت قطعی

race زمان‌بندی WebView: `onPageFinished` می‌تواند قبل از پایان parse اسکریپت
بزرگ (840KB) برسد → `ExamFormulaHost.begin` وقتی هنوز undefined بود صدا می‌شد
و هیچ اتفاقی نمی‌افتاد؛ auto-open مرجع هم به رویداد load وابسته است که در
سند intercepted ممکن است دیر بیاید یا قبل از begin رفته باشد.

### رفع دوطرفه (تأییدشده با تست اجرایی jsdom با شبیه‌سازی race)

```text
JS (بلوک پل): begin تا «بازشدن واقعی مودال» هر ۱۲۰ms تلاش می‌کند (سقف ۱۰
ثانیه، سپس خطای تشخیصی FORMULA_OPEN_TIMEOUT به Native)؛ پرچم
__examFormulaHostReady برای هم‌قدمی Kotlin.
Kotlin: فراخوانی begin با callback نتیجه؛ تا وقتی پل تعریف نشده هر 150ms
تکرار (سقف ~۱۰ ثانیه).
تست اجرایی: openMath عمداً دزدیده و ۶۰۰ms بعد برگردانده شد → مودال باز شد،
بدون خطا؛ سناریوهای V55 (درج/بستن/جلسهٔ دوم) همچنان سالم.
```

### فایل‌ها و عملیات

```text
app/src/main/assets/formula_editor/formula.html (بلوک پل)
app/src/main/assets/formula_editor/version.txt
app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt
app/src/test/java/ir/exam/app/ui/app/V55_1FormulaOpenRetryTest.kt (جدید)
text/CHANGELOG_FA.txt | docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | git diff --check → PASS
```


## ۱۴۱) V55.2 — برچسب نسخهٔ پل فرمول و تشخیص‌های صریح

### تحلیل گزارش دوم «صفحهٔ سفید»

```text
اسکرین‌شات دوم دقیقاً همان فایل قبلی بود (photo_..._04-47-20.jpg، حجم/نام یکسان،
ساعت 04:47 = قبل از تحویل V55.1) و هیچ پیام قرمز FORMULA_OPEN_TIMEOUT نداشت —
درحالی‌که V55.1 پس از ۱۰ ثانیه حتماً خطا نشان می‌داد.
جمع شواهد: به احتمال بسیار زیاد build تست‌شده هنوز V55.1 را نداشت.
فرضیهٔ کاربر («نیتیو فایل مستقل را اجرا نمی‌کند») با مدرک رد شد: badge «v36»
خودِ فایل روی صفحه است یعنی WebView فایل را کامل بارگیری و JS را اجرا کرده.
تست‌های اجرایی jsdom نیز مسیر را در هر دو حالت (load عادی/دیرهنگام) سالم
نشان می‌دهند.
```

### تحویل V55.2 — حذف ابهام برای همیشه

```text
برچسب سبز N55.2 پل                    → کنار badge مرجع؛ روی دستگاه فوراً معلوم
                                         می‌شود کدام نسخهٔ asset واقعاً اجراست
FORMULA_OPEN_TIMEOUT غنی               → اکنون وضعیت openMath/modal className/qTxt
                                         را هم گزارش می‌کند
BRIDGE_NOT_READY (Kotlin)              → اگر پل پس از ~۱۰ ثانیه تعریف نشده باشد،
                                         پیام قرمز صریح به‌جای سکوت
تست اجرایی jsdom (بازسازی کامل)        → درج «$\frac{1}{2}+x^{2}$» + بستن + جلسهٔ
                                         دوم + race دیرهنگام openMath: همگی PASS
هماهنگی تست V55.1 با retry ارتقایافته  → انجام شد
```

### راهنمای قطعی تست دستگاه

```text
پس از build این پچ، در پنجرهٔ فرمول گوشهٔ پایین باید «دو» برچسب باشد:
چپ: v36 (مرجع) — راست: N55.2 (پل)
- اگر N55.2 نبود → build/asset قدیمی است (کش gradle یا apply نشدن پچ)
- اگر بود و پنجره باز نشد → یکی از دو پیام قرمز دقیقاً علت را می‌گوید
```

### عملیات

```text
فایل‌ها: formula.html (بلوک پل) / version.txt / FormulaHostDialog.kt /
V55_2BridgeDiagnosticsTest.kt (جدید) / V55_1 test هماهنگ / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.1
FINAL_NATIVE_VERIFY → PASS, EXIT=0 | سازگاری needle سراسری → صفر mismatch
```

## ۱۴۲) V55.3 — رفع «صفحهٔ خالی با مودالِ باز» (باگ رسم WebView)

### تحلیل عکس سوم (photo_2026-08-24_08-09-17.jpg) — این بار قطعی

```text
۱) برچسب سبز N55.2 دیده می‌شود → asset جدید قطعاً روی دستگاه اجراست
   (فرضیهٔ «بیلد قدیمی» برای همیشه رد شد).
۲) هیچ پیام قرمز FORMULA_OPEN_TIMEOUT یا BRIDGE_NOT_READY نیست →
   begin اجرا شده و مودال ظرف ۱۰ ثانیه کلاس open را گرفته
   (وگرنه V55.2 حتماً خطا چاپ می‌کرد).
۳) رنگ کل صفحه در عکس دقیقاً #E9EEF4 است = خودِ --bg1 تم روشن؛ یعنی
   پس‌زمینهٔ مودال تمام‌صفحه «رسم می‌شود» ولی بچه‌هایش (بوم، تراشه‌ها،
   کلیدها) paint نمی‌شوند.
۴) دایرهٔ خاکستری لبهٔ راست عکس حباب سیستم/اپ دیگری است، ربطی به برنامه ندارد.
جمع شواهد = باگ compositing کلاسیک WebView اندروید:
WebView شفاف (setBackgroundColor TRANSPARENT) + backdrop-filter:blur(5px)
روی .modal مرجع + will-change:transform روی .modal.open .modal-box (خط ~۳۳۰۹)
→ لایهٔ کامپوزیت خالی. jsdom این را نمی‌بیند چون paint ندارد.
```

### تحویل V55.3 — سه لایه

```text
۱) asset: بلوک nativePaintFix فقط وقتی window.ExamEditorNative هست تزریق
   می‌شود (مرورگر عادی دست‌نخورده): backdrop-filter مودال none،
   will-change مودال‌باکس auto، min-height مودال تمام‌صفحه. در حالت
   تمام‌صفحه پس‌زمینهٔ مودال مات است پس این تغییر بصری ندارد.
۲) Kotlin: پس‌زمینهٔ WebView مات #E9EEF5 (همان --bg1) به‌جای TRANSPARENT.
۳) تشخیص FORMULA_BLANK_LAYOUT: ۱.۳ ثانیه پس از بازشدن واقعی مودال،
   ابعاد getBoundingClientRect عناصر mfModal/mfP_box/mbCanvas + اندازهٔ
   viewport + نسخهٔ Chrome دستگاه اندازه‌گیری و اگر بوم عملاً نامرئی بود
   قرمز گزارش می‌شود — اگر paint-fix کافی نبود، دیگر داده داریم نه حدس.
برچسب پل: N55.3 | version.txt: v55.3-paint-fix
```

### تست‌ها

```text
jsdom (بازسازی): تزریق nativePaintFix ✓ · begin→open ✓ · تایپ سریع
«1/2 + x^2» → درج «$\frac{1}{2} + x^{2}$» + onEditorClosed ✓ · عدم reopen ✓ ·
جلسهٔ دوم ✓ · FORMULA_BLANK_LAYOUT در jsdom (ابعاد صفر) عمداً فعال شد ✓ =
اثبات اجرای detector پس از open.
تست منبع جدید: V55_3PaintFixTest · هماهنگی: V55_2 (برچسب regex نسخه‌مستقل).
verify: سه require جدید V55.3 · اسکن needle سراسری با نگاشت متغیر→فایل:
صفر mismatch در ۱۸ تست · FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
گوشهٔ پایین باید «N55.3» باشد (نه N55.2).
- N55.3 هست + پنجره کامل دیده می‌شود → حل شد؛ V55_3 به patches/built برود.
- N55.3 هست + باز هم خالی → پیام قرمز FORMULA_BLANK_LAYOUT پایین صفحه
  ظاهر می‌شود؛ همان متن را کامل بفرست (ابعاد واقعی + نسخهٔ Chrome داخلش است).
- N55.3 نیست → پچ روی بیلد اعمال نشده.
```

### عملیات

```text
فایل‌ها: formula.html (nativePaintFix + layoutCheck + برچسب N55.3) /
version.txt / FormulaHostDialog.kt (پس‌زمینهٔ مات) /
V55_3PaintFixTest.kt (جدید) / V55_2 test هماهنگ / verify (۳ require جدید) /
changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.2
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۳) V55.3.1 — hotfix تست: needle نادرست در V55_3PaintFixTest

### گزارش CI کاربر

```text
V55_3PaintFixTest > blank layout diagnostic ... FAILED at :53
381 tests completed, 1 failed — کد اصلی SUCCESS (الگوی همیشگی: فقط تست).
```

### علت (اشتباه خودم در V55.3، نه کد اصلی)

```text
assertion خط ۵۳ می‌خواست «Chrome» در ۶۰۰ کاراکترِ «بعد از» marker
FORMULA_BLANK_LAYOUT باشد؛ ولی در asset، نسخهٔ Chrome قبل از رشتهٔ خطا
استخراج می‌شود (var ua = ...match(/Chrome\/[\d.]+/)... چند خط قبل از
ExamEditorNative.onError) و بعد از marker فقط + ua می‌آید.
اسکن سراسری قبلی این را نگرفت چون فقط الگوی سادهٔ «"needle" in var» را
می‌بیند، نه substringAfter/take.
```

### رفع

```text
assertion به دو چک قطعی تبدیل شد:
- الگوی «Chrome\/[\d.]+» در کل asset (همان regex استخراج نسخهٔ Chrome)
- «window.innerWidth» در ۶۰۰ کاراکتر بعد از marker (واقعاً آنجاست)
شبیه‌سازی python هر ۱۱ assertion هر ۳ تست V55_3 → همه True.
اسکن سراسری ۴۰ needle در ۱۸ تست → صفر mismatch. verify → PASS EXIT=0.
هیچ فایل غیرتستی تغییر نکرد.
```

### عملیات

```text
فایل‌ها: V55_3PaintFixTest.kt / هندآف
پیش‌نیاز: V55.3 | نتیجهٔ تست دستگاه V55.3 هنوز نامشخص (منتظر گزارش N55.3)
```

## ۱۴۴) V55.4 — بزرگ‌سازی پنجره‌های کتابخانهٔ فرمول داخل برنامه

### گزارش دستگاه

```text
«بیلد موفق بود. پنجره کتابخانه ها کوچک است» → V55.3 مشکل صفحهٔ خالی را حل کرد ✅
مثال کاربر: دکمهٔ «اعداد و محاسبات» → پنجرهٔ باریک.
پرسش‌ها: کدام پنجره؟ منوی دسته‌ها (mbVar) | در مرورگر بزرگ‌تر است؟ کاربر گفت
«در مرورگر بزرگ‌تر است؛ فقط در برنامه کوچک شده» | اندازهٔ خواسته: بزرگ‌تر ولی
نه تمام‌صفحه.
```

### تشخیص

```text
دو پنجرهٔ درگیر در مرجع:
۱) منوی شناور دسته‌ها  .mb-var → max-width:min(320px,92vw), max-height:62vh
۲) پنل مرکزی کتابخانه .mb-library-panel → width:min(94vw,640px), max-height:84dvh
روی موبایل 92vw≈همان عرض صفحه است؛ «باریک‌تر بودن در برنامه» ناشی از حس بصری
پنجرهٔ تمام‌صفحه + آیتم‌های ریز مرجع است. طبق قانون «مرجع دست‌نخورده»، CSS مرجع
ویرایش نشد؛ override فقط داخل برنامه تزریق می‌شود.
```

### تحویل V55.4

```text
بلوک تزریقی جدید nativeLibrarySize (فقط وقتی window.ExamEditorNative هست؛
همان الگوی nativePaintFix):
- .mb-var: min-width:min(340px,88vw)، max-width:min(480px,94vw)،
  max-height:76vh، padding/عنوان بزرگ‌تر؛ دکمه‌های دسته min-height:48px و
  فونت 1.02rem (لمس‌پذیر).
- .mb-library-panel: width:min(96vw,720px)، max-height:92dvh.
- آیتم‌های کتابخانه .mfk: min-height:52px، فونت 1.18rem؛ دکمهٔ «× بستن» بزرگ‌تر.
برچسب پل: N55.4 | version.txt: v55.4-library-size | مرجع در مرورگر بی‌تغییر.
```

### تست‌ها

```text
jsdom اجرایی: تزریق هر دو style ✓ · mbGroupLibrary('num') → منوی ۲۲ دسته باز ✓ ·
انتخاب دسته → کتابخانهٔ ۲۱ آیتمی با پنل ✓ · style تزریقی بعد از سبک‌های مرجع
در سند (برد !important) ✓ · برچسب N55.4 ✓ · بدون خطا ✓
تست منبع جدید: V55_4LibrarySizeTest (۳ تست، شامل چک «مرجع دست‌نخورده»)
verify: دو require جدید V55.4 · اسکن سراسری ۴۸ needle در ۱۹ تست → صفر mismatch ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
برچسب گوشهٔ پایین باید «N55.4» باشد. در پنجرهٔ فرمول:
- لمس «اعداد و محاسبات» → منوی دسته‌ها باید پهن‌تر با دکمه‌های بلندتر باشد
- انتخاب یک دسته → پنجرهٔ نمادها باید تقریباً تمام عرض و ارتفاع را بگیرد
  (ولی تمام‌صفحه نیست — حاشیهٔ تیرهٔ اطراف می‌ماند)
```

### عملیات

```text
فایل‌ها: formula.html (nativeLibrarySize + برچسب N55.4) / version.txt /
V55_4LibrarySizeTest.kt (جدید) / verify (۲ require جدید) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.3.1
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۵) V55.5 — اجرای قطعی اندازهٔ منوی دسته‌ها با style مستقیم

### گزارش دستگاه (عکس photo_2026-08-24_09-32-54.jpg، برچسب N55.4)

```text
«پنجره بسیار باریک است.» عکس: پنجرهٔ فرمول کاملاً سالم (بوم/تراشه‌ها/کلیدها) ✅
ولی منوی «اعداد و محاسبات» فقط یک نوار سفید یک‌ردیفه (فقط «➕ نمادهای پایه»)
با ارتفاع ~۲۹px CSS، چسبیده به بالای تراشه‌ها. یعنی CSS تزریقی V55.4 روی
WebView دستگاه اثر نکرده است.
```

### تشخیص (اندازه‌گیری، نه حدس)

```text
- Chromium واقعی (playwright، viewport موبایل): همین فایل، همان مسیر لمس →
  منو 340x695 با ۲۲ دستهٔ ۴۸px؛ یعنی فایل درست است، WebView دستگاه cascade
  تزریقی (یا min()/dvh در آن context) را اعمال نمی‌کند — همان خانوادهٔ رفتار
  متفاوت WebView که V55.3 هم دیدیم.
- در عکس، ارتفاع هر آیتم ~۲۹px = دقیقاً padding 6px مرجع، پس قاعده‌های
  «min-height:48px !important» تزریقی هم به عنصر نرسیده‌اند.
```

### تحویل V55.5 — بی‌نیاز از cascade

```text
بلوک nativeMenuEnforce (فقط داخل برنامه؛ polling 250ms):
- منوی دسته‌ها (#mbVar وقتی .mbv-cat دارد): style مستقیم روی خود عنصر با
  setProperty(..., 'important') — عرض ۹۴٪ صفحه (سقف 480px)، ارتفاع تا ۸۰٪،
  ردیف‌ها ۴۸px با فونت بزرگ، وسط‌چین بر اساس ابعاد واقعی window.
- پنل کتابخانهٔ نمادها: عرض ۹۶٪ (سقف 720px)، ارتفاع تا ۹۲٪ — همان روش.
- منوهای کوچک variants (mbv-i) عمداً دست‌نخورده؛ پس از بستن، unsize()
  استایل‌های inline را پاک می‌کند تا اندازهٔ مرجع برگردد.
- تشخیص: اگر ۵۰۰ms پس از اعمال هنوز h<180 یا w<220 بود → پیام قرمز
  MENU_RECT/PANEL_RECT با ابعاد واقعی + ابعاد فرزند + نسخهٔ Chrome.
برچسب پل: N55.5 | version.txt: v55.5-menu-enforce
CSS تزریقی V55.4 حذف نشد (در WebViewهای سالم همچنان کمک می‌کند).
```

### تست‌ها

```text
Chromium (playwright) اجرایی — ۴ سناریو:
S1 مسیر عادی: منو 387x732، ردیف 48px ✓
S2 شبیه‌سازی دستگاه خراب (حذف هر دو style تزریقی): باز هم 387x732 ✓
   (اثبات اینکه enforce به CSS تزریقی وابسته نیست)
S3 انتخاب دسته → پنل کتابخانه 378px عرض، ۲۱ آیتم ✓
S4 منوی variants: دستکاری نشد و پس از پاک‌سازی به عرض مرجع 220 برگشت ✓
تست منبع جدید: V55_5MenuEnforceTest (۳ تست) · دوباره‌سنجی بلوک‌محور همهٔ
تست‌های V55.x قبلی → سبز · verify: دو require جدید V55.5 · اسکن سراسری
۵۲ needle در ۲۰ تست → صفر mismatch · FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
برچسب باید «N55.5» باشد. لمس «اعداد و محاسبات»:
- منو باید تقریباً تمام عرض و وسط صفحه، با ۲۲ ردیف بلند و اسکرول باشد
- اگر باز هم باریک بود → پیام قرمز MENU_RECT پایین صفحه ظاهر می‌شود؛
  کامل بفرست (ابعاد + نسخهٔ Chrome دستگاه داخلش است)
```

### عملیات

```text
فایل‌ها: formula.html (nativeMenuEnforce + برچسب N55.5) / version.txt /
V55_5MenuEnforceTest.kt (جدید) / verify (۲ require جدید) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.4
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۶) V55.6 — حذف برچسب‌ها، بازشدن فوری، جاشدن فرمول‌ها در کادر

### گزارش دستگاه (عکس photo_2026-08-24_10-17-31.jpg، N55.5)

```text
V55.5 موفق: پنل کتابخانه بزرگ و درست باز می‌شود ✅. سه درخواست جدید:
۱) برچسب N55.5 و «v36 · V34: ✓ 64» حذف شود
۲) بازشدن پنجره‌های کتابخانه تأخیر دارد
۳) فرمول‌های پهن (در عکس: دترمینان ۳×۳) از کادر بیرون می‌زنند
```

### تحویل V55.6 (همه فقط داخل برنامه؛ مرجع در مرورگر دست‌نخورده)

```text
۱) برچسب‌ها: کد ساخت برچسب سبز N55.x کلاً حذف شد؛ نسخهٔ پل فقط پرچم
   window.__nativeBridgeVersion='N55.6' (برای پیام‌های خطا). badge مرجع
   «v36 · V34» با hideBadges فقط وقتی ExamEditorNative هست مخفی می‌شود
   (display:none important + چند بار retry چون مرجع دیر می‌سازدش).
۲) تأخیر: علت، polling ۲۵۰ms بود. حالا wrapper مستقیم روی چهار تابع بازکنندهٔ
   مرجع (mbGroupLibrary/mbOpenSymbolLibrary/mbShowSymbolCategory/
   mbOpenItemLibrary) enforce را «بلافاصله» اجرا می‌کند (اندازه‌گیری: ~۱۱ms).
   نصب wrapper دوباره پس از load (کتابخانهٔ V34/Word توابع را بازتعریف می‌کنند).
   polling ۲۵۰ms به‌عنوان پشتیبان مسیرهای فرعی ماند.
۳) فرمول‌های پهن: fitLibraryItems پس از هر بازشدن پنل، هر آیتم .mfk که
   scrollWidth > clientWidth دارد را با گام ×۰.۸۸ (حداکثر ۸ گام، کف 9px)
   کوچک می‌کند تا کامل جا شود؛ فونت آیتم‌های عادی دست نمی‌خورد.
version.txt: v55.6-clean-badges-fit | پیام BRIDGE_NOT_READY → asset v55.6
```

### تست‌ها

```text
Chromium (playwright):
- داخل برنامه: هر دو برچسب غایب ✓ · منو فوری (~۱۱ms) با ابعاد کامل ✓ ·
  ۷ دستهٔ سنگین (ماتریس‌ها/مشتقات/انتگرال‌ها/اتحادهای مثلثاتی/تبدیل‌ها/اتحادها):
  صفر آیتم سرریز؛ دترمینان ۳×۳ با فونت جمع‌شده داخل کادر ✓ (اسکرین‌شات) ·
  جریان کامل درج نماد پس از کتابخانه سالم ✓
- مرورگر عادی (بدون پل): badge مرجع نمایان (block)، منو اندازهٔ مرجع،
  enforce نصب نشده ✓ — یعنی فایل برای استفادهٔ مستقل تغییری نکرده.
تست منبع جدید: V55_6CleanBadgesFitTest (۳ تست) · V55_2 test هماهنگ شد
(برچسب → پرچم نسخه + hideBadges؛ دقت: bt.textContent مرجعِ badge خودش می‌ماند،
پس needle حذف باید «bt.id = 'nativeBridgeTag'» باشد نه bt.textContent).
verify: دو require جدید V55.6 · اسکن سراسری ۶۳ needle در ۲۱ تست + ده چک
بلوک‌محور → صفر mismatch · FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
(برچسبی دیگر روی صفحه نیست؛ نسخه را از این پس از پیام‌های خطا یا
version.txt داخل APK تشخیص می‌دهیم)
۱) پایین صفحه هیچ برچسبی نباشد
۲) «اعداد و محاسبات» → منو باید بدون مکث محسوس بزرگ باز شود
۳) دسته «ماتریس/دترمینان» → فرمول دترمینان ۳×۳ باید کامل داخل کادر باشد
```

### عملیات

```text
فایل‌ها: formula.html (hideBadges + wrappers فوری + fitLibraryItems؛ حذف
tagBridge) / version.txt / FormulaHostDialog.kt (پیام v55.6) /
V55_6CleanBadgesFitTest.kt (جدید) / V55_2 test هماهنگ / verify (۲ require) /
changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.5
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۷) V55.7 — کشیده‌شدن کادر متن سؤال با محتوا

### گزارش دستگاه (عکس photo_2026-08-24_10-38-03.jpg)

```text
«کادر متن سوال وب ویو نیست. با افزودن فرمول یا درج شکل کادر کشیده نمی‌شود و
اندازه کادر تعداد مشخصی سطر است. قابل اسکرول نیست. خود کادر درون یک پس‌زمینه
است. ظاهراً دارد تمام question_editor.html را رندر می‌کند. تست کن.»
```

### تشخیص (اندازه‌گیری Chromium با viewport دقیقاً هم‌اندازهٔ WebView برنامه)

```text
- فرضیهٔ «کل فایل رندر می‌شود» رد شد: در حالت nativeTools فقط برچسب «متن سؤال»
  + سطح تایپ qmf-surface visible است (۲ عنصر). WebView هست ولی ارتفاعش ثابت
  320dp بود درحالی‌که محتوای صفحه فقط ~۱۲۱px است → ~۲۰۰dp پس‌زمینهٔ خاکستری
  مرجع (bg #e9eef5) زیر کادر دیده می‌شد = همان «کادر درون یک پس‌زمینه».
- «کشیده نشدن»: سطح تایپ مرجع max-height:min(56vh,460px) دارد؛ در WebView
  320dp یعنی سقف ~179px، بعد اسکرول داخلی (که در WebView تو در توی Compose
  عملاً غیرقابل استفاده است) → حس «تعداد ثابت سطر + غیرقابل اسکرول».
```

### تحویل V55.7 (سه قسمت؛ همه فقط حالت nativeTools — مرجع در مرورگر دست‌نخورده)

```text
۱) HTML: در nativeToolbarHide سه قاعده اضافه شد: qmf-surface بدون سقف/اسکرول
   داخلی؛ textarea بدون سقف؛ پس‌زمینهٔ html/body شفاف.
۲) HTML: reportHeight — ResizeObserver روی shell/body + رویداد input +
   interval 400ms؛ ارتفاع واقعی shell → ExamEditorNative.onContentHeight.
۳) Kotlin: FieldBridge.onContentHeight(41..20000) → QuestionTextWebSection
   ارتفاع پویا contentHeightDp.coerceIn(150,4000).dp به‌جای 320dp ثابت
   (overlay باز: همان 560dp). اسکرول = صفحهٔ اصلی برنامه.
version.txt (question_editor): v55.7-autogrow-field
```

### تست‌ها (همه Chromium واقعی)

```text
- setValue ۸ سطری از Native → گزارش ارتفاع 130→261 ✓
- درج جدول → 307 ✓ · متن طولانی → 526 ✓ · اسکرول داخلی surface پس از
  همگام‌سازی ارتفاع: ندارد ✓ (اسکرین‌شات‌ها)
- پل فرمول (openTool('formula') → onOpenFormula len/sel درست) ✓ ·
  onTextChanged پس از درج ✓ · بدون هیچ خطای JS ✓
- مرورگر عادی بدون پل: maxH=448px مرجع، پس‌زمینهٔ خاکستری مرجع، بدون گزارشگر ✓
تست منبع جدید: V55_7AutoGrowFieldTest (۳ تست) · verify: دو require جدید V55.7 ·
اسکن سراسری ۷۳ needle در ۲۲ تست → صفر mismatch · هیچ تستی به 320dp وابسته
نبود · FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) کادر متن سؤال باید بدون حاشیهٔ خاکستری اضافه، هم‌اندازهٔ محتوا باشد
۲) چند سطر متن بنویس/جدول درج کن → کادر باید بلند شود و صفحهٔ «ساخت آزمون»
   (نه داخل کادر) اسکرول بخورد
۳) فرمول ∑ همچنان پنجرهٔ تمام‌صفحه را باز کند
```

### عملیات

```text
فایل‌ها: question_editor.html (nativeToolbarHide گسترده + reportHeight) /
version.txt (question_editor) / QuestionTextFieldWebView.kt (onContentHeight) /
QuestionTextWebSection.kt (ارتفاع پویا) / V55_7AutoGrowFieldTest.kt (جدید) /
verify (۲ require) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.6
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۸) V55.8 — کادر ثابت با اسکرول داخلی، کوچک‌سازی درج‌ها، رفع فلاش boot، منوهای کیپد

### گزارش دستگاه (پس از V55.7) + پاسخ‌های ask_user

```text
۱) «خود کادر متن سوال اسکرول می‌شود و نه داخل کادر» → پرسیدم؛ انتخاب صریح:
   «داخل خود کادر اسکرول شود» (رفتار V55.7 برعکس خواسته بود).
۲) «فرمول‌ها و شکل‌ها و هر چیز درج‌شده کوچک‌تر شود» → انتخاب: ~۶۰٪.
۳) «برای یک لحظه فایل question_editor.html رندر می‌شود و سپس غیب می‌شود»
۴) «پنجره‌های دکمه‌های کیپد فرمول هنوز باریک هستند»
```

### تحویل V55.8 (همه فقط داخل برنامه)

```text
۱) کادر ثابت: در nativeToolbarHide سقف min-height:150px/max-height:260px +
   overflow-y:auto + ‎-webkit-overflow-scrolling:touch جایگزین «بدون سقف» V55.7
   شد؛ گزارش ارتفاع (onContentHeight) و ارتفاع پویای Compose سر جایشان ماندند —
   با سقف 260 خودکار کادر کوتاه/ثابت می‌شود و متنِ زیاد داخل کادر اسکرول می‌خورد.
۲) کوچک‌سازی درج‌ها: .qmf-fig با zoom:.6 (شکل/نمودار/جدول/تناوبی/اطلس) و
   .qmf-atom با zoom:.75 (فرمول‌ها) — فقط پیش‌نمایش؛ توکن/TeX واقعی دست‌نخورده.
۳) فلاش صفحهٔ مرجع: علت، اجرای بلوک Native در «انتهای» فایل ~5MB بود؛ تا آن
   لحظه صفحهٔ کامل مرجع دیده می‌شد. بلوک جدید exam-editor-native-boot در
   ابتدای head (فقط nativeTools): body مخفی + پس‌زمینه شفاف؛ بلوک انتهایی
   پرده (nativeBootHide) را برمی‌دارد؛ پشتیبان ۶ ثانیه‌ای هم دارد.
۴) منوهای کیپد فرمول: wrapper های enforce به mbVarShow/mbParPicker/mbLogMenu/
   mbIntegralMenu/mbPercentMenu/mbTrigMenu وصل شد؛ selector پشتیبان polling هم
   '.mbv-cat, .mbv-i, .mbv-q' شد؛ آیتم‌های mbv-i → 52px و mbv-q/o/c → 46px.
   (تست V55_5 با selector جدید هماهنگ شد؛ unsize پس از بستن سر جای خود.)
version.txt ها: question_editor=v55.8-fixed-box-shrink،
formula_editor=v55.8-keypad-menus
```

### تست‌ها (Chromium واقعی)

```text
- پردهٔ boot هنگام load دیده شد (body hidden) و پس از پایان راه‌اندازی برداشته
  شد (bootGone=true, bodyVisible=true) ✓
- سقف کادر 260px/min 150px/اسکرول داخلی فعال با محتوای بلند ✓ (اسکرین‌شات:
  دو جدول کوچک‌شده + فرمول + متن؛ innerScroll=true)
- zoom درج‌ها: fig=0.6، atom=0.75 ✓
- منوی log کیپد: عرض 387، آیتم 52px ✓ · منوی پرانتز: دکمه‌های 46x46 ✓
- مرورگر عادی (بدون پل): پرده تزریق نمی‌شود، body visible، سقف مرجع 448px،
  منوی کیپد همان 220px مرجع ✓
تست منبع جدید: V55_8FixedBoxShrinkTest (۴ تست) · V55_7 و V55_5 tests هماهنگ ·
verify: سه require جدید V55.8 (و به‌روزرسانی require V55.7) · اسکن سراسری
۸۷ needle در ۲۳ تست + ۱۱ چک بلوک‌محور → صفر mismatch · PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) باز کردن «ساخت آزمون»: هیچ فلاش صفحهٔ مرجع دیده نشود
۲) متن بلند بنویس → کادر حداکثر ~۲۶۰ نقطه بلند می‌شود و داخلش اسکرول می‌خورد
۳) جدول/شکل درج کن → پیش‌نمایش جمع‌وجور (~۶۰٪) و جا برای متن باز
۴) در پنجرهٔ فرمول: لمس دکمه‌های کیپد که منو دارند (log، انتگرال، ٪، مثلثات،
   پرانتز) → منوی بزرگ وسط صفحه
```

### عملیات

```text
فایل‌ها: question_editor.html (boot curtain + سقف ثابت + zoom درج‌ها) /
formula.html (منوهای کیپد) / هر دو version.txt / V55_8FixedBoxShrinkTest.kt
(جدید) / V55_7 و V55_5 tests هماهنگ / verify / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.7
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۴۹) V55.9 — قطع overlay مرجع با کلیک روی توکن + جداسازی WebView هر سؤال

### گزارش دستگاه (عکس photo_2026-08-24_11-43-36.jpg، پس از V55.8)

```text
۱) «کادر خاکستری هنوز وجود دارد» (عکس: توکن آناتومی داخل کادر + پس‌زمینهٔ
   خاکستری بزرگ زیر آن)
۲) «با کلیک روی آناتومی درج‌شده برای ویرایش، کل کادر انتخاب می‌شود و کادر
   خاکستری ظاهر می‌شود»
۳) «اگر در کادر متن سؤال ۱ چیزی بنویسم/درج کنم در کادر متن تمام سؤالات
   ظاهر می‌شود»
```

### تشخیص (بازتولید در Chromium)

```text
۱و۲) کلیک «تکی» روی توکن آناتومی → anOverlay مرجع (ویرایشگر تمام‌صفحهٔ
   خاکستری) داخل همان WebView کوچک باز می‌شود؛ polling ما آن را overlayOpen
   می‌بیند و Compose ارتفاع را 560dp می‌کند = «کادر خاکستری بزرگ». شنوندهٔ
   قبلی ما فقط dblclick بود و چون دیر ثبت می‌شد، شنونده‌های click مرجع
   (openFromEl) جلوتر اجرا می‌شدند.
۳) مسیر HTML/localStorage تبرئه شد (دو صفحهٔ هم‌context در Chromium: متن منتقل
   نشد؛ کلیدهای localStorage فقط مربوط به فرمول‌های اخیر است). ریشه در Compose
   است: در LazyColumn با بازیافت composition، AndroidView (WebView) یک سؤال
   با closureهای factory سؤال قبلی (bridge/onValueChanged) برای سؤال دیگر
   نگه داشته می‌شود → تایپ در یک کادر به state سؤال‌های دیگر می‌رود.
```

### تحویل V55.9

```text
۱) asset (بلوک boot در head — فاز capture مقدم بر همهٔ شنونده‌های مرجع):
   گیرندهٔ click+dblclick برای .qmf-fig با kind∈{t,p,a,s} — preventDefault +
   stopImmediatePropagation و تحویل به __nativeFigEdit (پنجرهٔ ضدتکرار ۷۰۰ms
   برای click,click,dblclick). سایر انواع (هندسه/نمودار مرجع) دست‌نخورده.
   بلوک اصلی: window.__nativeFigEdit → pendingEditJson + onEditFigure (شنوندهٔ
   dblclick قدیمی حذف شد — منطق تحویل همان V53.3 است).
۲) Kotlin: key(controller) دور AndroidView در QuestionTextFieldWebView —
   هر سؤال WebView مخصوص خودش؛ با بازیافت، WebView سؤال قبلی dispose می‌شود.
version.txt: v55.9-native-fig-edit
```

### تست‌ها (Chromium واقعی)

```text
- کلیک تکی آناتومی: هیچ overlay مرجع باز نشد؛ onEditFigure=1 ✓
- click,click,dblclick واقعی = فقط ۱ ویرایش؛ ویرایش دوم پس از مکث = ۲ ✓
- رویداد کاذب onOverlayChanged(true) دیگر صادر نمی‌شود ✓
- توکن هندسه (مرجع): dblclick → gfOverlay مرجع همچنان باز می‌شود ✓
- تراز آکولاد/پرانتز فایل Kotlin پس از key() سالم ✓
تست منبع جدید: V55_9NativeFigEditIsolationTest (۳ تست) · verify: دو require
جدید V55.9 · اسکن سراسری ۹۱ needle در ۲۴ تست → صفر mismatch · PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) آناتومی درج کن → یک کلیک روی آن → مستقیم ویرایشگر Native آناتومی باز شود؛
   هیچ کادر خاکستری‌ای ظاهر نشود
۲) دو سؤال بساز؛ در سؤال ۱ تایپ کن → متن فقط در سؤال ۱ بماند (اسکرول بالا/پایین
   هم تست شود چون بازیافت LazyColumn با اسکرول رخ می‌دهد)
```

### عملیات

```text
فایل‌ها: question_editor.html (click-capture در boot + __nativeFigEdit؛ حذف
شنوندهٔ dblclick قدیمی) / version.txt / QuestionTextFieldWebView.kt (key) /
V55_9NativeFigEditIsolationTest.kt (جدید) / verify (۲ require) / changelog /
هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.8
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۰) V55.10 — انتخاب/حذف/ویرایش توکن‌ها با کلیک + آزادسازی اسکرول کادر

### درخواست‌های کاربر (پس از V55.9، پیش از build آن)

```text
۱) هر چیز درج‌شده با «یک کلیک انتخاب» شود و با «کلیک دوم به ویرایشگر» برود
۲) با کلیک اول «ضربدر» گوشهٔ توکن بیاید تا قابل حذف باشد
۳) قبل و بعد توکن‌ها بتوان تایپ کرد
۴) «کادر متن سؤال اسکرول‌پذیر نیست»
```

### تحویل V55.10

```text
۱و۲) گیرندهٔ capture بلوک boot (بازنویسی V55.9) برای «همهٔ» توکن‌های .qmf-fig:
   - کلیک اول: selectFig → کلاس is-on مرجع + دکمهٔ ✕ قرمز (native-fig-x)
     گوشهٔ توکن؛ هیچ ویرایشگری باز نمی‌شود.
   - کلیک روی ✕: removeToken → حذف «%%FIG:raw%%» (+ \n بعدی) از textarea +
     dispatch input واقعی → sync مرجع و onTextChanged به Native.
   - کلیک دوم روی توکن انتخاب‌شده: t/p/a/s → __nativeFigEdit (ویرایشگر
     Native)؛ بقیه (هندسه/نمودار) → dblclick synthetic با پرچم __nativeAllow
     که از گیرندهٔ ما عبور و به ویرایشگر مرجع می‌رسد.
   - dblclick واقعی کاربر بلعیده می‌شود (توالی click,click خودش کار را کرده).
۳) کلیک بیرون توکن: فقط clearSel بدون بلعیدن رویداد → caret مرجع برای تایپ
   قبل/بعد توکن می‌نشیند (تایپ دور توکن تست شد؛ توکن حفظ می‌شود).
۴) اسکرول: ریشه = LazyColumn ژست عمودی WebView را می‌قاپد. HTML در reportHeight
   پرچم onScrollableChanged(surf.scrollHeight>clientHeight) می‌فرستد؛
   controller.innerScrollable در Kotlin ست می‌شود و WebView سفارشی در
   onTouchEvent با requestDisallowInterceptTouchEvent(true) در ACTION_DOWN
   (و آزادسازی در UP/CANCEL) والد را کنار می‌زند.
version.txt: v55.10-select-delete-type
```

### تست‌ها (Chromium واقعی — ۷ سناریو در یک صفحه)

```text
S1 کلیک اول آناتومی: is-on + ✕، صفر overlay ✓
S2 کلیک دوم: onEditFigure('a') و لغو انتخاب ✓
S3 هندسه: کلیک اول انتخاب؛ کلیک دوم gfOverlay مرجع ✓
S4 حذف با ✕: توکن از متن textarea حذف شد (figCount 2→1) ✓
S5 کلیک بیرون: انتخاب و ✕ پاک شدند ✓
S6 تایپ قبل/بعد: متن دور توکن، توکن سالم ✓
S7 پرچم اسکرول: onScrollableChanged(true) پس از متن بلند ✓
تست منبع جدید: V55_10SelectDeleteTypeTest (۳ تست) · V55_9 test هماهنگ شد
(ضدتکرار زمانی lastFigEdit → بلعیدن dblclick) · verify: دو require جدید ·
اسکن سراسری ۹۵ needle → صفر mismatch · تراز آکولاد Kotlin سالم · PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) جدول/آناتومی درج کن → کلیک اول: هالهٔ انتخاب + ✕ قرمز گوشهٔ توکن
۲) کلیک دوم → ویرایشگر Native همان نوع
۳) ✕ → توکن حذف شود
۴) کلیک قبل/بعد توکن → تایپ همان‌جا بنشیند
۵) متن بلند → اسکرول داخل کادر با انگشت کار کند (لیست سؤال‌ها ندزددش)
```

### عملیات

```text
فایل‌ها: question_editor.html (select/delete/edit + پرچم اسکرول) / version.txt /
QuestionTextFieldWebView.kt (innerScrollable + onTouchEvent) /
V55_10SelectDeleteTypeTest.kt (جدید) / V55_9 test هماهنگ / verify (۲ require) /
changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.9
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۱) 📌 مرجع دائمی: قالب پچ، هات‌فیکس و دستور جامع اعمال/پوش

این بخش «قرارداد رسمی» تحویل پچ در این پروژه است؛ هر تحویل باید دقیقاً همین
قالب را داشته باشد.

### ۱۵۱.۱) نام‌گذاری فایل پچ

```text
پچ اصلی:      V<نسخه>_<زیرنسخه>_<توضیح-کوتاه-انگلیسی>.patch
              مثال: V55_10_select_delete_type_scroll.patch
هات‌فیکس:     V<نسخه>_<زیرنسخه>_<شماره>_<توضیح>_hotfix.patch
              مثال: V55_3_1_test_needle_hotfix.patch
              (هات‌فیکس = فقط رفع شکست CI/تست همان نسخه؛ بدون قابلیت جدید)
قواعد نام:    بدون علامت ? و فاصله و حروف فارسی در نام فایل؛ فقط حروف انگلیسی،
              عدد و زیرخط. پچ در مسیر دانلود ویندوز تحویل می‌شود:
              C:\Users\Hashem\Downloads  (در WSL: /mnt/c/Users/Hashem/Downloads)
```

### ۱۵۱.۲) محتوای اجباری هر پچ (چک‌لیست سازنده)

```text
□ کد اصلی تغییر (asset/Kotlin) — WebView فقط در فهرست مجاز ۵ فایلی
□ تست منبع جدید V<نسخه>Test.kt برای همین تغییر
□ هماهنگ‌سازی تست‌های قدیمی ناسازگارشده (اسکن سراسری needle قبل از تحویل)
□ require های جدید در scripts/verify_native_final.py (و اجرای آن: PASS EXIT=0)
□ یک خط فارسی کاربرپسند بالای text/CHANGELOG_FA.txt (هات‌فیکسِ فقط-تست: لازم نیست)
□ بخش جدید در docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md (همیشه؛ حتی هات‌فیکس)
□ به‌روزرسانی version.txt asset تغییرکرده
□ هرگز: secret/token/URL خصوصی در پچ یا چت
□ ساخت پچ روی «وضعیت بازسازی‌شدهٔ کاربر»: clone تمیز HEAD کاربر + اعمال
  ترتیبی همهٔ patches/built/ + پچ pending ریشه → کپی فایل‌ها → git diff
  (--binary اگر باینری هست) → git apply --check باید OK بدهد
```

### ۱۵۱.۳) قالب تحویل در چت

```text
۱) توضیح فارسی ساده: چه مشکلی بود، ریشه چه بود (با مدرک، نه حدس)، چه شد
۲) فایل پچ در ریشهٔ workspace (کاربر همان را در Downloads ویندوز می‌گذارد)
۳) «دستور جامع» طبق ۱۵۱.۴ — آماده برای کپی یک‌جا در WSL
۴) راهنمای تست دستگاه: چه چیزی را چک کند و اگر خراب بود چه پیامی را بفرستد
```

### ۱۵۱.۴) دستور جامع اعمال و پوش (قالب استاندارد)

جای `<PATCH>` نام فایل پچ و جای `<پیام>` پیام commit را بگذارید؛ بقیه ثابت است:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin
git apply --check /mnt/c/Users/Hashem/Downloads/<PATCH>.patch && echo "OK"
git apply /mnt/c/Users/Hashem/Downloads/<PATCH>.patch
git add -A
git --no-pager diff --cached --stat
git commit -m "<پیام>"
git push origin HEAD
```

نمونهٔ واقعی (V55.10):

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin
git apply --check /mnt/c/Users/Hashem/Downloads/V55_10_select_delete_type_scroll.patch && echo "OK"
git apply /mnt/c/Users/Hashem/Downloads/V55_10_select_delete_type_scroll.patch
git add -A
git --no-pager diff --cached --stat
git commit -m "feat: token select/delete/edit clicks, free typing around tokens, inner scroll unlock V55.10"
git push origin HEAD
```

```text
نکته‌های ثابت این دستور:
- خط دوم فقط «آزمایش» است؛ اگر OK چاپ نشد ادامه نده و خروجی خطا را بفرست.
- همیشه «git --no-pager diff» (بدون no-pager یک‌بار کاربر در pager گیر کرد).
- پیام commit: انگلیسی، با پیشوند feat:/fix:/test:/docs: و شمارهٔ نسخه در انتها.
- آخرین فرمان همیشه git push origin HEAD است؛ CI خودکار اجرا می‌شود.
- پچ‌ها وابسته به ترتیب‌اند: اگر پچ قبلی هنوز اعمال نشده، اول همان را بزن.
```

### ۱۵۱.۵) پس از اعمال — چرخهٔ گزارش

```text
۱) نتیجهٔ CI (سبز/قرمز + متن شکست) را در چت بگذار.
   قرمزِ فقط-تست → هات‌فیکس V<x>_<y>_<z>_hotfix می‌گیرد (الگوی رایج).
۲) نتیجهٔ تست دستگاه را طبق «راهنمای تست دستگاه» همان پچ گزارش بده؛
   اگر پیام قرمز تشخیصی دیدی، متن کاملش را بفرست (اساس قانون «حدس ممنوع»).
۳) پس از build سبز + تأیید دستگاه، پچ از ریشهٔ workspace به patches/built/
   منتقل و هندآف sync می‌شود (کار سازندهٔ پچ، نه کاربر).
```

### ۱۵۱.۶) ترتیب کامل زنجیرهٔ پچ‌ها (تا این لحظه)

```text
patches/built/ (به ترتیب اعمال):
V53_1 → V53_1_1 → V53_2 → V53_3 → V53_3_1 → V53_4 → V53_4_1 →
V54_1 → V54_2 → V54_3 → V54_3_1 → V54_4 → V54_5 → V54_6 → V54_7 →
V55 → V55_1 → V55_2 → V55_3 → V55_3_1 → V55_4 → V55_5 → V55_6 →
V55_7 → V55_8 → V55_9
pending ریشهٔ workspace: V55_10_select_delete_type_scroll.patch
دست‌نخورده طبق انتخاب کاربر: patches/pending/V50_0_revert_to_v45_3.patch
```

## ۱۵۲) V55.11 — ✕ کامل برای همهٔ درج‌شده‌ها (شامل فرمول) + رفع مکان‌نما

### گزارش دستگاه (پس از V55.10)

```text
۱) «با کلیک اول که ضربدر می‌آید ضربدر نصفه است و فرمول ضربدر ندارد»
۲) «کادر متن سؤال کلیک‌پذیر نیست؛ نمی‌توانم قبل/بعد چیز درج‌شده کلیک کنم تا
   مکان‌نما بیاید و تایپ کنم»
```

### تشخیص (اندازه‌گیری Chromium با تپ لمسی واقعی)

```text
۱) ✕ با top:-8px بیرون مرز توکن بود و overflow مرجع (auto روی جدول qmf-tab،
   hidden روی اطلس) آن را می‌برید (xFullyVisible=false, xTopVsFig=-5).
   با zoom:.6 کوچک‌سازی V55.8 هم ✕ عملاً 17x17 دیده می‌شد.
   فرمول‌ها .qmf-atom اند نه .qmf-fig — گیرندهٔ V55.10 اصلاً پوششش نمی‌داد.
۲) تپ روی سطح تایپ → activeElement=TEXTAREA! کل کادر داخل <label class=field>
   مرجع است و رفتار استاندارد label، کلیک را به کنترل خودش (textarea مخفی
   opacity:0) هدیه می‌کند؛ فوکوس از سطح contenteditable می‌پرد و caret نمی‌نشیند.
```

### تحویل V55.11 (همه در بلوک boot؛ فقط حالت nativeTools)

```text
۱) attachX(el): ✕ داخل مرز توکن (top:2px;left:2px، 26px) + zoom معکوس
   (1/zoom والد) تا کوچک‌سازی V55.8 اندازهٔ لمسش را نخورَد. ناظر ۳۰۰ms:
   ✕ را روی «عنصر انتخاب‌شدهٔ» جاری (.qmf-atom.is-on یا .qmf-fig.is-on)
   می‌گذارد و از بقیه برمی‌دارد — فرمول‌ها هم ✕ دارند؛ انتخاب/ویرایش فرمول
   همان مسیر مرجع می‌ماند (کلیک اول انتخاب، کلیک دوم ویرایشگر فرمول).
   حذف فرمول = dispatch Backspace مرجع روی سطح (اصلاح دقیق منبع توسط خود
   مرجع) + گزارش onTextChanged.
   حذف شکل: کلیک روی ✕ حتی وقتی ناظر چسبانده باشد → removeToken قطعی.
۲) click حبابی روی .qmf-surface: preventDefault (لغو «هدیهٔ فوکوس» label) و
   اگر فوکوس روی سطح نبود focus + caretRangeFromPoint از نقطهٔ لمس.
   (فاز حباب است تا منطق مرجع و گیرندهٔ capture توکن‌ها اول کارشان را بکنند.)
version.txt: v55.11-x-caret-fix
```

### تست‌ها (Chromium، تپ لمسی واقعی)

```text
- تپ روی متن/ناحیهٔ خالی: فوکوس سطح + caret داخل سطح ✓ · تایپ واقعی «ز» در
  منبع نشست ✓
- فرمول: تپ اول → is-on مرجع + ✕ کامل (26x26، بریده‌نشده، hit-test بالای آن
  خودش) ✓ · تپ ✕ → فرمول از منبع حذف و onTextChanged ✓
- جدول: ✕ داخل مرز (xInside=true) ✓ · حذف با ✕ → توکن از منبع رفت ✓
- رگرسیون V55.10: انتخاب→ویرایش native آناتومی، هندسه→ویرایشگر مرجع، بدون
  overlay ناخواسته — همه سبز ماند ✓
تست منبع جدید: V55_11XCaretFixTest (۳ تست) · verify: دو require جدید ·
اسکن سراسری ۹۵ needle در ۲۶ تست → صفر mismatch · PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) فرمول و جدول درج کن → کلیک اول روی هرکدام: ✕ قرمز «کامل» گوشهٔ بالای توکن
۲) ✕ → حذف (هم فرمول هم جدول)
۳) کلیک روی متن یا فضای خالی کادر → مکان‌نما بنشیند و تایپ همان‌جا برود
۴) کلیک دوم توکن → ویرایشگر (فرمول → پنجرهٔ فرمول؛ جدول/آناتومی → Native)
```

### عملیات

```text
فایل‌ها: question_editor.html (attachX + ناظر + caret فیکس) / version.txt /
V55_11XCaretFixTest.kt (جدید) / verify (۲ require) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.10 (و V55.10.1 مستندات)
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۳) V55.12 — جریان دومرحله‌ای اطلس/نمودار، شماره در انتهای پیکان، حذف برچسب‌ها

### درخواست‌های کاربر

```text
۱) لمس آیکن‌های نمودار/آناتومی/فیزیک/شیمی مثل «درج شکل»: اول پنجرهٔ انتخاب،
   بعد پنجرهٔ ویرایش؛ در پنجرهٔ ویرایش انتخابِ نوع نمایش داده نشود.
۲) شمارهٔ علامتگذاری تصاویر در «انتهای» پیکان باشد نه ابتدا.
۳) کادرهای برچسب تصاویر در پنجرهٔ ویرایش نمایش داده نشود.
۴) نمودارها در پنجرهٔ انتخاب هر سطر ۲ تا؛ جملهٔ «برای ویرایش انتخاب کنید» حذف.
۵) هندآف همیشه به‌روز باشد (بخش ۱۵۱ = قرارداد دائمی؛ این بخش طبق همان).
```

### تحویل V55.12

```text
۱) AtlasTypePickerDialog جدید (تمام‌صفحه، چیپ دسته‌ها + شبکهٔ ۲ستونهٔ انواع با
   تصویر بندانگشتی) در AtlasEditorDialog.kt؛ AtlasTarget با chooseType/presetType؛
   builder: آیکن‌ها → chooseType=true → انتخاب نوع → AtlasEditorDialog با
   presetType (بدون چیپ دسته و LazyRow انواع — typeId ثابت val).
   dblclick/ویرایش توکن موجود مثل قبل مستقیم به ویرایشگر می‌رود (initialSpec).
   نمودار از قبل دومرحله‌ای بود (FigureTypePickerDialog) — فقط چیدمانش عوض شد.
۲) شمارهٔ نشانه: دایرهٔ شماره از start به end منتقل شد در هر ۳ رندرکننده:
   بوم ویرایشگر (AtlasEditorDialog)، AtlasFigureView (دانش‌آموز)،
   AtlasBitmapRenderer (چاپ/PDF). فرمت دادهٔ marks تغییری نکرد.
۳) OutlinedTextField برچسب هر نشانه حذف؛ به‌جایش ردیف فشردهٔ چیپ «شماره + ✕»
   برای حذف تکی + دکمهٔ «پاک‌کردن همه» ماند. (سوییچ «نمایش نام‌ها» می‌ماند؛
   برچسب‌های قدیمی specهای موجود حفظ می‌شوند.)
۴) پنجرهٔ انتخاب نمودار: LazyColumn تک‌ستونه → LazyVerticalGrid Fixed(2) با
   سلول فشرده؛ جملهٔ راهنما حذف شد.
نکته: needle قدیمی «برای ویرایش انتخاب کنید» حتی در کامنت هم نباید بماند
(verify با «not in» چک می‌کند و کامنت فارسی خودم اول باعث FAIL شد — درس عبرت).
```

### تست‌ها

```text
تست منبع جدید: V55_12AtlasFlowArrowTest (۴ تست: جریان دومرحله‌ای/بدون انتخاب
نوع در ویرایش، شماره در انتهای پیکان در ۳ رندرکننده، بدون کادر برچسب با حفظ
حذف تکی، شبکهٔ ۲ستونهٔ نمودار بدون جملهٔ راهنما)
هماهنگی: V53_3AtlasNativeTest (needle به chooseType=true + AtlasTypePickerDialog)
verify: ۵ require جدید V55.12 · اسکن سراسری بهبود یافته (پشتیبانی source() و
${'$'}؛ ۵۰۰ needle در ۵۹ تست) → صفر mismatch · تراز آکولاد/پرانتز (با حذف
رشته‌ها) در ۵ فایل تغییرکرده صفر · FINAL_NATIVE_VERIFY=PASS EXIT=0
توجه: تست اجرایی Chromium برای این پچ ممکن نیست (Composeِ خالص، نه WebView)؛
اثبات از مسیر تست‌های منبع + CI کامپایل واقعی است.
```

### راهنمای تست دستگاه

```text
۱) آیکن نمودار → پنجرهٔ انتخاب ۲تایی در هر سطر، بدون جملهٔ اضافه؛ انتخاب →
   پنجرهٔ ویرایش همان نمودار (بدون فهرست انواع)
۲) آیکن آناتومی/فیزیک/شیمی → پنجرهٔ انتخاب با دسته‌ها و شبکهٔ ۲ستونه؛
   انتخاب → ویرایش همان تصویر بدون انتخاب نوع و بدون کادرهای برچسب
۳) کشیدن پیکان روی تصویر → دایرهٔ شماره در «انتهای» پیکان (نوک فلش)
۴) درج و نمای دانش‌آموز/پیش‌نمایش A4 هم شماره را انتهای پیکان نشان دهد
```

### عملیات

```text
فایل‌ها: AtlasEditorDialog.kt (AtlasTypePickerDialog + حذف انتخاب نوع/برچسب‌ها +
شماره انتها) / AtlasFigureView.kt / AtlasBitmapRenderer.kt /
FigurePickerDialog.kt (شبکهٔ ۲ستونه) / ExamBuilderScreen.kt (AtlasTarget دومرحله‌ای) /
V55_12AtlasFlowArrowTest.kt (جدید) / V53_3 test هماهنگ / verify (۵ require) /
changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.11
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۴) V55.13 — ویرایش Native هندسه/نمودار با کلیک دوم + جدول تناوبی LTR

### گزارش دستگاه (عکس‌های photo_2026-08-24_14-24-*.jpg، پس از V55.11)

```text
۱) «با کلیک اول انتخاب می‌شود و ضربدر می‌آید و کار می‌کند اما با کلیک دوم
   کادر خاکستری می‌آید» (عکس: توکن هندسهٔ مربع + overlay خاکستری مرجع)
۲) «جدول تناوبی از راست به چپ است که باید از چپ به راست باشد»
```

### تشخیص

```text
۱) کلیک دوم توکن‌های k='g' (هندسه/نمودار) و kind خالی طبق طراحی V55.10 به
   refEdit → ویرایشگر «مرجع» می‌رفت که داخل WebView کوچک همان کادر خاکستری
   است؛ درحالی‌که این انواع از V45.3 ویرایشگر Native دارند (FigurePickerDialog).
۲) برنامه RTL است؛ Rowهای Compose در PeriodicTouchGrid (شبکهٔ لمسی ویرایشگر
   تناوبی) از راست چیده می‌شدند → گروه ۱ سمت راست. مرجع خودش .ptb را
   direction:ltr می‌کند؛ رندر SVG (دانش‌آموز/چاپ) مختصات مطلق دارد و سالم بود.
```

### تحویل V55.13

```text
۱) asset (بلوک boot): کلیک دومِ k='g' و '' هم به __nativeFigEdit می‌رود؛
   refEdit فقط پشتیبان انواع ناشناخته. Kotlin (ExamBuilderScreen):
   onEditFigureToken شاخهٔ "g", "" → FigureTarget با تشخیص GEOMETRY/GRAPH از
   GRAPH_FIGURES؛ onInsert مسیر editingWebToken → applyEditedFigureJson
   (جایگزینی همان توکن) و onDismiss → cancelEditFigure.
۲) PeriodicEditorDialog: CompositionLocalProvider(LayoutDirection.Ltr) دور
   PeriodicTouchGrid — گروه ۱ سمت چپ مثل جدول تناوبی استاندارد.
version.txt: v55.13-geo-native-ltr
```

### تست‌ها

```text
Chromium: کلیک اول هندسه → انتخاب+✕، صفر overlay ✓ · کلیک دوم هندسه و نمودار
→ onEditFigure('g') دو بار و صفر overlay مرجع ✓
تست منبع جدید: V55_13GeoNativeLtrTest (۲ تست) · verify: دو require جدید ·
اسکن سراسری ۵۰۳ needle → صفر mismatch · تراز آکولاد (بدون رشته‌ها) صفر ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) شکل هندسی درج کن → کلیک اول انتخاب؛ کلیک دوم → پنجرهٔ «ویرایش شکل» برنامه
   (نه کادر خاکستری)؛ تغییر بده و اعمال کن → همان توکن به‌روز شود
۲) همین را با نمودار تست کن → پنجرهٔ «ویرایش نمودار»
۳) آیکن جدول تناوبی → شبکهٔ ویرایشگر از چپ به راست (گروه ۱ چپ) باشد
```

### عملیات

```text
فایل‌ها: question_editor.html (مسیر کلیک دوم g/'') / version.txt /
ExamBuilderScreen.kt (route g/'' + جایگزینی توکن) / PeriodicEditorDialog.kt
(LTR) / V55_13GeoNativeLtrTest.kt (جدید) / verify (۲ require) / changelog /
هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.12
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۵) V55.14 — سطل زبالهٔ حذف سؤال، رفع تداخل مکعب/جعبه‌ای، دستگیره‌های برش

پچ اول از تحویل دومرحله‌ای (انتخاب کاربر در ask_user)؛ پچ دوم (V55.15: کادر
گزینه‌ها شبیه کادر سؤال + دکمهٔ + با پنجرهٔ ۸ ابزاره) جداگانه ساخته می‌شود.

### درخواست‌ها و رفع

```text
۱) «دکمهٔ حذف سؤال حذف؛ سطل زباله کنار بارم + تأیید»:
   TextButton «حذف سؤال» حذف شد؛ IconButton سطل (Icons.Outlined.Delete،
   رنگ error) کنار MinimalScoreField + AlertDialog «سؤال n برای همیشه حذف
   شود؟» با دکمهٔ قرمز حذف/انصراف.
۲) «مکعب‌مستطیل به‌شکل نمودار جعبه‌ای درج می‌شود؛ در پنجرهٔ درج شکل هم
   جعبه‌ای دیده می‌شود»: تداخل شناسه — «box» هم مکعب‌مستطیل GEOMETRY_FIGURES
   بود هم نمودار جعبه‌ایِ ChartSvgRendererStage2؛ renderBody اول نمودار را
   چک می‌کند → box همیشه جعبه‌ای رندر می‌شد. رفع: شناسهٔ هندسه cuboid شد؛
   svgOf مرجع HTML هم cuboid→box نگاشت گرفت (یک خط افزوده در ابتدای svgOf؛
   کد بدنهٔ مرجع دست‌نخورده). renderBody کاتلین: شاخهٔ هندسه
   "cube","cuboid","box" (box قدیمی بدون دادهٔ نمودار = هندسه). ابعاد
   مکعب‌مستطیل (w=70) هم از حالت مکعبی درآمد.
۳) «برش تصویر: اضلاع جابه‌جا نمی‌شوند؛ گوشه‌ها هم باشند؛ حرکت آزاد»:
   دستگیره‌های «نامرئی» V35 عملاً قابل کشف نبودند. اکنون: میله‌های سفید
   مرئی وسط ۴ ضلع + مربع‌های سفید ۴ گوشه (لمس 32dp)؛ resize بردار (dx,dy)؛
   CropGeometry: چهار عضو جدید enum گوشه + resizeDeltaForCorner (مؤلفهٔ
   غالب) + recenterAfterResize چهار حالت گوشه (گوشهٔ مقابل ثابت)؛ حرکت
   آزاد کادر از ناحیهٔ داخلی (padding 22dp) حفظ شد. مسیر ذخیره
   (cropRect/LocalImageRepository) تغییری نکرد.
```

### تست‌ها

```text
جدید: V55_14TrashCuboidCropTest (۴ تست؛ شامل تست اجرایی JVM ریاضیات گوشه‌ها)
هماهنگ: V34BuilderVaultCropTest (امضای جدید resize)، V35StudentUiCropClipboardTest
(دستگیره‌های مرئی به‌جای نامرئی) · verify: بندهای V34/V35 به‌روز + دو require
جدید V55.14 · شبیه‌سازی دقیق تست‌های V24/V26 (متغیر substring/فایل دیگر که
اسکنر ساده mismatch کاذب می‌داد) → همه سبز · verify PASS EXIT=0
هشدار اسکنر: ۱۴ mismatch گزارش‌شده همگی کاذب بودند (متغیرهای substring و
فایل‌های repo/supabase خارج از نگاشت اسکنر) — با شبیه‌سازی دقیق تأیید شد.
```

### راهنمای تست دستگاه

```text
۱) روی کارت سؤال، کنار بارم سطل زبالهٔ قرمز؛ لمس → پنجرهٔ تأیید؛ حذف → سؤال برود
۲) درج شکل → مکعب‌مستطیل → داخل متن سؤال باید مکعب‌مستطیل سه‌بعدی دیده شود
   (نه نمودار جعبه‌ای)؛ پنجرهٔ درج شکل هم پیش‌نمایش درست
۳) ویرایش تصویر → برش: میله‌های سفید اضلاع و مربع‌های گوشه دیده و کشیده شوند؛
   کل کادر با کشیدن وسطش آزادانه جابه‌جا شود؛ خروجی برش همان کادر باشد
```

### عملیات

```text
فایل‌ها: ExamBuilderScreen.kt (سطل+تأیید) / FigureGallery.kt + FigureSvgRenderer.kt
+ question_editor.html (cuboid) / CropGeometry.kt + InteractiveImageEditorDialog.kt
(گوشه‌ها/دستگیره‌های مرئی) / V55_14TrashCuboidCropTest.kt (جدید) / V34+V35 tests
هماهنگ / verify / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.13
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۶) V55.14.1 — hotfix تست: الگوی ممنوعهٔ V24 در متن تأیید حذف

### گزارش CI کاربر

```text
V24ComprehensiveUxTest > question card ... FAILED at :106
416 tests completed, 1 failed — کد اصلی SUCCESS (الگوی همیشگی: فقط تست).
```

### علت (اشتباه V55.14 خودم)

```text
خط ۱۰۶ تست V24 (قانون قدیمی «پیشوند شماره‌دار سؤال روی کارت برنگردد»):
assertFalse("Text(\"سؤال ${index + 1}" in editor)
و متن پنجرهٔ تأیید V55.14 دقیقاً «Text("سؤال ${index + 1} برای همیشه حذف
شود؟")» بود. اسکن سراسری این را نگرفت چون needle آن با ${'$'} کدگذاری شده و
جزو ۱۴ mismatch «کاذب» طبقه‌بندی شده بود — در حالی که این یکی واقعی بود.
درس (بار دوم؛ V55.12 هم مشابه بود): needleهای assertFalse حتی در کامنت هم
نباید ظاهر شوند؛ نسخهٔ اول hotfix هم دقیقاً در «کامنت» همین الگو را داشت.
```

### رفع

```text
متن تأیید: «این سؤال برای همیشه حذف شود؟» (بدون پیشوند شماره‌دار؛ کامنت هم
پاک). شبیه‌سازی دقیق assertion خط ۱۰۶ → False (سالم)؛ تست‌های V55_14 و
require های verify دست‌نخورده سبز (needle آن‌ها «برای همیشه حذف شود؟» است
که هنوز موجود است). verify PASS EXIT=0. فقط ExamBuilderScreen.kt + هندآف.
```

### عملیات

```text
فایل‌ها: ExamBuilderScreen.kt (فقط متن تأیید) / هندآف
پیش‌نیاز: V55.14 | تست دستگاه V55.14 طبق راهنمای بخش ۱۵۵
```

## ۱۵۷) V55.15 — رفع قفل کادر برش (stale lambda) + نمودار جعبه‌ای مکعب‌نما

### گزارش دستگاه (پس از build موفق V55.14/V55.14.1)

```text
۱) «مربع برش حرکت آزادانه ندارد»
۲) «نمودار جعبه‌ای در متن سؤال به شکل مکعب است» (قرینهٔ باگ V55.14!)
```

### تشخیص

```text
۱) stale-lambda کلاسیک Compose: pointerInput(circular) با کلید ثابت هرگز
   restart نمی‌شود؛ closure اولین composition مرکز کهنه (safeCenterX/Y همان
   لحظه) را نگه می‌دارد → هر drag از مرکز اولیه حساب و clamp می‌شود و کادر
   عملاً قفل می‌ماند. resize هم دقیقاً همین مشکل را داشت (بعد از اولین تغییر
   از اندازهٔ کهنه ادامه می‌داد).
۲) buildGraphSpec خروجی «بدون k» می‌ساخت. مرجعِ کادر متن توکن بدون k را به
   ماژول هندسه می‌دهد (GeoFig)؛ و چون svgOf هندسه از V55.14 نگاشت cuboid→box
   دارد و خودش هم t='box' هندسی را می‌فهمد، نمودار جعبه‌ای مکعب‌مستطیل رندر
   می‌شد. بازتولید Chromium: توکن بدون k → polygon مکعب؛ k='g' → نمودار.
```

### تحویل V55.15

```text
۱) CropFrame/CropHandle: rememberUpdatedState(onMove/onResize) و فراخوانی
   نسخهٔ همیشه-تازه (currentOnMove/currentOnResize) داخل pointerInput.
۲) buildGraphSpec: root["k"]=JsonPrimitive("g") — همهٔ specهای نمودار ساخته/
   ویرایش‌شده در FigurePickerDialog با برچسب ماژول نمودار مرجع درج می‌شوند.
   (specهای جعبه‌ایِ قبلاً درج‌شدهٔ بدون k پس از یک بار ویرایش درست می‌شوند.)
تأیید Chromium: k='g',t=box → ۵ مستطیل نمودار بدون polygon؛ cuboid با/بی k
→ polygon مکعب‌مستطیل ✓
```

### تست‌ها

```text
جدید: V55_15CropMoveBoxChartTest (۲ تست) · هماهنگ: V55_14 (needleهای
onMove/onResize → currentOnMove/currentOnResize) · verify: دو require جدید ·
اسکن سراسری ۵۲۲ needle → صفر mismatch · شبیه‌سازی خط ۱۰۶ V24 → سالم ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### راهنمای تست دستگاه

```text
۱) ویرایش تصویر → برش: کادر با کشیدن وسطش «پیوسته» جابه‌جا شود (نه فقط یک
   تکان)؛ اضلاع/گوشه‌ها هم پیوسته resize کنند
۲) درج نمودار → جعبه‌ای → در متن سؤال، نمودار جعبه‌ای واقعی (مستطیل‌های
   چارک با whisker) دیده شود؛ مکعب‌مستطیل هندسه هم همچنان مکعب باشد
```

### عملیات

```text
فایل‌ها: InteractiveImageEditorDialog.kt (rememberUpdatedState) /
FigurePickerDialog.kt (k='g') / V55_15CropMoveBoxChartTest.kt (جدید) /
V55_14 test هماهنگ / verify (۲ require) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.14.1
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۸) V55.16 — کادر گزینه‌ها شبیه کادر سؤال + دکمهٔ + با پنجرهٔ ۸ ابزار

پچ دوم تحویل دومرحله‌ای (بخش ۱۵۵). انتخاب‌های ask_user قبلی: کادر «سریع Native
با پیش‌نمایش زیر آن» (نه WebView سنگین برای هر گزینه).

### درخواست‌ها و تحویل

```text
۱) «کادر متن گزینه‌های چندگزینه‌ای و جورکردنی شبیه کادر متن سؤال شود»:
   - OutlinedTextField ها گرد شدند (RoundedCornerShape(14.dp)) مثل حس کادر سؤال؛
   - پیش‌نمایش زندهٔ NativeMathText اکنون علاوه بر '$' با "%%FIG:" هم فعال
     می‌شود → شکل/نمودار/جدول/تناوبی/اطلس درج‌شده در گزینه هم زیر کادر رندر
     زنده دارد (NativeMathText از قبل توکن FIG را می‌فهمید؛ فقط شرط نمایش
     ناقص بود).
۲) «به‌جای آیکن فرمول، دکمهٔ + با پنجرهٔ ۸ آیکن»:
   - فایل جدید OptionInsertTools.kt: OptionInsertButton (آیکن + با tint
     primary) و OptionInsertToolsDialog (AlertDialog با LazyVerticalGrid
     Fixed(4) — ۸ ابزار با آیکن‌های QuestionToolIcons و برچسب فارسی).
   - چندگزینه‌ای: دکمهٔ + → insertMenuFor=InsertMenuRef("option", index, label).
   - جورکردنی: MatchingItemTools همان callback قدیمی onFormula را به دکمهٔ +
     داد؛ در builder، درجِ تازه (occurrence=null و tex خالی) به پنجرهٔ ۸ ابزار
     می‌رود و «ویرایش فرمول موجود» (ExistingFormulaEditor) مسیر قبلی می‌ماند.
   - مسیریابی ابزارها: FORMULA → FormulaTarget همان فیلد (FormulaHostDialog)؛
     بقیه → fieldInsertTarget=ref + همان targetهای موجود (figure/table/
     periodic/atlas با chooseType). خروجی: appendTokenToField توکن
     %%FIG:json%% را به انتهای متن همان فیلد (updateOption/updateMatchingText)
     اضافه می‌کند؛ deliverFigure و onInsert مسیر شکل/نمودار شاخهٔ fieldRef
     گرفتند. انصراف در همهٔ مسیرها (cancelFigureEditing، dismiss دو picker)
     fieldInsertTarget را پاک می‌کند تا درج بعدی متن سؤال به گزینه نرود.
```

### هماهنگی‌های اجباری (needleهای قدیمی Icons.Outlined.Functions)

```text
- V26QuestionMediaReorderTest: دو needle → OptionInsertButton(
- V34BuilderVaultCropTest: ترتیب ابزارها با OptionInsertButton( سنجیده می‌شود
- verify بند ۵۰۳ (multiple_choice) و بندهای V34 → OptionInsertButton(
- Icons.Outlined.Functions فقط در InlineMathTextEditor (بند V45) می‌ماند.
```

### تست‌ها

```text
جدید: V55_16OptionInsertToolsTest (۳ تست) · verify: ۳ require جدید V55.16 ·
شبیه‌سازی ۲۰ چک دقیق + اسکن سراسری ۵۳۷ needle → صفر mismatch · تراز آکولاد
۳ فایل صفر · FINAL_NATIVE_VERIFY=PASS EXIT=0 · Kotlin 2.0.21 → enum.entries OK
```

### راهنمای تست دستگاه

```text
۱) سؤال چندگزینه‌ای: روی کارت هر گزینه دکمهٔ + (به‌جای آیکن فرمول) → پنجرهٔ
   ۸ ابزار؛ فرمول → پنجرهٔ فرمول همان گزینه؛ جدول/شکل/... → ویرایشگر Native و
   پس از درج، پیش‌نمایش زیر کادر همان گزینه دیده شود
۲) جورکردنی: همین رفتار در موردهای راست/چپ
۳) کادرهای متن گزینه/جورکردنی گرد و شبیه کادر سؤال
۴) رگرسیون: درج از آیکن‌های زیر کادر متن سؤال همچنان به متن سؤال برود
```

### عملیات

```text
فایل‌ها: OptionInsertTools.kt (جدید) / ExamBuilderScreen.kt (منو+مسیریابی+
appendTokenToField+پاک‌سازی هدف) / QuestionOptionMedia.kt (+، کادر گرد،
پیش‌نمایش FIG) / V55_16OptionInsertToolsTest.kt (جدید) / V26+V34 tests هماهنگ /
verify (بندهای V34/۵۰۳ به‌روز + ۳ require جدید) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.15
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۵۹) V55.17 — آیکن بانک، تراشهٔ نمایش توکن‌ها، رفع حرکت معکوس برش

### گزارش دستگاه (عکس photo_2026-08-24_20-38-47.jpg — V55.16 روی دستگاه است)

```text
۱) «دکمهٔ ذخیره در بانک حذف؛ کنار سطل زباله آیکن ذخیره در بانک»
۲) «با درج چیز در گزینه‌ها، کادر متن گزینه پر از کد می‌شود» (عکس: JSON خام
   توکن %%FIG%% داخل کادر گزینهٔ الف؛ پیش‌نمایش زیر آن درست است)
۳) «حرکت آزادانهٔ مربع برش در جهت مخالف حرکت می‌کند»
```

### تشخیص و تحویل

```text
۱) OutlinedButton متنی «ذخیره در بانک» حذف؛ IconButton با
   Icons.Outlined.BookmarkAdd (رنگ primary) کنار سطل زباله در ردیف بارم.
   (هیچ تست/verify به دکمهٔ متنی وابسته نبود؛ آیکن‌های extended موجودند.)
۲) طراحی: مقدار واقعی فیلد همان توکن می‌ماند (منبع حقیقت). فایل جدید
   FigTokenVisuals.kt: VisualTransformation که هر %%FIG:{json}%% را در
   «نمایش» به تراشهٔ ⟦نوع⟧ (جدول/جدول تناوبی/آناتومی/فیزیک-شیمی/عنوان نمودار)
   با رنگ primary تبدیل می‌کند؛ نگاشت offset اتمی: caret داخل توکن به مرز
   انتهای تراشه می‌چسبد؛ یکنواختی نگاشت با شبیه‌سازی python و «تست اجرایی
   JVM واقعی» (filter + originalToTransformed/transformedToOriginal روی
   متن دو-توکنه) اثبات شد. به سه کادر (گزینه + راست/چپ جورکردنی) وصل شد.
۳) ریشه: برنامه RTL است و Modifier.offset «جهت‌آگاه» است — x مثبت در RTL به
   چپ اعمال می‌شود؛ هندسهٔ برش (frameLeft/moveCenter) برای LTR نوشته شده.
   کل بوم برش (BoxWithConstraints تا CropFrame) داخل
   CompositionLocalProvider(LayoutDirection.Ltr) پیچیده شد؛ همان الگوی
   V55.13 جدول تناوبی. (drag.x خام لمس بود و درست؛ فقط اعمال offset آینه می‌شد.)
```

### تست‌ها

```text
جدید: V55_17BankIconChipRtlCropTest (۴ تست؛ شامل تست اجرایی JVM یکنواختی
نگاشت تراشه) · verify: ۳ require جدید V55.17 · شبیه‌سازی ۸ چک دقیق + اسکن
سراسری ۵۴۲ needle → صفر mismatch · تراز آکولاد (بدون رشته/کامنت) صفر ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
یادآوری: تست‌های JVM از androidx.compose.ui.text استفاده می‌کنند (مثل
سایر تست‌ها که کلاس‌های Compose را import می‌کنند؛ unit test با کلاس‌های
JVMِ کتابخانهٔ ui-text مشکلی ندارد).
```

### راهنمای تست دستگاه

```text
۱) ردیف بارم: آیکن بانک (نشان + بوک‌مارک) کنار سطل زباله؛ لمس → سؤال در
   بانک ذخیره شود (دکمهٔ متنی پایین کارت دیگر نیست)
۲) درج جدول/آناتومی در گزینه → داخل کادر فقط «⟦جدول⟧/⟦آناتومی⟧» رنگی دیده
   شود نه JSON؛ پیش‌نمایش واقعی زیر کادر بماند؛ تایپ قبل/بعد تراشه سالم
۳) ویرایش تصویر → برش: کادر دقیقاً هم‌جهت انگشت حرکت کند
```

### عملیات

```text
فایل‌ها: ExamBuilderScreen.kt (آیکن بانک + حذف دکمهٔ متنی + vt گزینه) /
QuestionOptionMedia.kt (vt جورکردنی) / FigTokenVisuals.kt (جدید) /
InteractiveImageEditorDialog.kt (بوم LTR) / V55_17BankIconChipRtlCropTest.kt
(جدید) / verify (۳ require) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.16
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۰) V55.18 — اسکرول نرم دوطرفهٔ کارت‌ها، منوی چشم، سربرگ فشرده با ص/غ

### درخواست‌های کاربر

```text
۱) «اسکرول کارت‌های مانده/پاسخ/تصحیح به چپ نرم است اما به راست نرم نیست»
۲) «آیکن چشم هم پیش‌نمایش چاپ این سؤال و هم پیش‌نمایش کامل A4 را باز کند؛
   با بستن یکی، پیش‌نمایش کامل A4 (دیگری) نمایش داده نشود»
۳) «صحیح/غلط روی کارت سؤال ص/غ شود و فاصلهٔ آیکن‌ها کمتر تا نوع سؤال کامل
   نمایش داده شود»
```

### تشخیص و تحویل

```text
۱) TeacherManagementCardsScreen (پشتهٔ ۶ کارت آمار/بانک/تصحیح/مانده/پاسخ/
   درخواست‌ها): translation فقط روی کارت «فعال» اعمال می‌شود. در کشیدن به
   راست (direction=-1) کارت فعالِ جدید همان کارتی است که الان بیرون پرید و
   snapTo(0f) فوری باعث «پرش» ورودش می‌شد (به چپ مشکل نبود چون کارت جدید از
   پشته با انیمیشن‌های stack بالا می‌آمد). رفع: پس از خروج، برای direction=-1
   کارت جدید از همان سمت خروج (snapTo(targetX/Y)) وارد و با
   tween(300, FastOutSlowInEasing) به مرکز می‌آید؛ مسیر چپ دست‌نخورده.
۲) آیکن چشم (قبلاً toggle چیدمان چاپ): اکنون DropdownMenu سه‌گزینه‌ای —
   «پیش‌نمایش چاپ این سؤال» (onPreview → previewQuestion)، «پیش‌نمایش کامل
   A4» (onPreviewAll جدید → previewAll همان state دکمهٔ پایین صفحه)، و
   «چیدمان و ظاهر چاپ» (همان styleExpanded). دو پیش‌نمایش state مستقل دارند
   و بستن هرکدام فقط خودش را می‌بندد (خواستهٔ «با بستن، A4 نیاید» چون
   جدا بازش می‌کنی برقرار است). VisibilityOff دیگر استفاده نمی‌شود.
۳) سربرگ کارت: برچسب TRUE_FALSE به‌صورت inline «ص/غ» (faLabel دست‌نخورده —
   قرارداد V19/V24 که faLabel کامل و needle «question.type.faLabel()» را در
   editor می‌خواهند حفظ شد)؛ فاصلهٔ Row سربرگ 6dp→2dp؛ دایرهٔ شماره 39→37dp؛
   آیکن‌های بانک/حذف/چشم/درگ 42→38dp. MinimalScoreField طبق قرارداد V25
   (width 62×40) عمداً دست‌نخورده ماند — تلاش اولیه برای کوچک‌کردنش verify را
   شکست و برگردانده شد.
```

### تست‌ها

```text
جدید: V55_18SmoothCardsEyeMenuTest (۳ تست) · verify: ۳ require جدید V55.18 ·
اسکن سراسری ۵۵۱ needle → صفر mismatch · شبیه‌سازی بخش‌محور V24 (Visibility/
visible=styleExpanded/faLabel()/الگوی ممنوعه) سبز · تراز آکولاد صفر ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
نکتهٔ اسکنر: substringBefore("} else {") در تست اول به else داخلی می‌خورد؛
با substringAfter("if (direction == -1) {")+substringAfter("} else {") رفع شد.
```

### راهنمای تست دستگاه

```text
۱) کارت‌های مدیریت معلم: کشیدن به «راست» هم مثل چپ نرم باشد (کارت جدید از
   همان سمت وارد شود، بدون پرش)
۲) آیکن چشم کارت سؤال → منوی ۳گزینه‌ای؛ پیش‌نمایش این سؤال و A4 هر کدام جدا
   باز/بسته شوند؛ چیدمان چاپ هم از همان منو
۳) کارت سؤال صحیح/غلط: برچسب «ص/غ» و ردیف آیکن‌ها جمع‌وجورتر؛ نوع سؤال‌های
   بلند (چندگزینه‌ای/جورکردنی) کامل دیده شوند
```

### عملیات

```text
فایل‌ها: TeacherManagementCardsScreen.kt (ورود نرم direction=-1) /
ExamBuilderScreen.kt (منوی چشم + onPreviewAll + ص/غ + فشرده‌سازی سربرگ) /
V55_18SmoothCardsEyeMenuTest.kt (جدید) / verify (۳ require) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.17
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۱) V55.18.1 — هات‌فیکس: کشیدن به راست کارت‌ها تک‌فاز و بدون پرش

### گزارش دستگاه (پس از V55.18)

```text
«همچنان اسکرول کارت‌های مانده، پاسخ و تصحیح و... به چپ نرم انجام می‌شود اما
به راست نرم نیست.»
```

### ریشه (با شبیه‌سازی فریم‌به‌فریم اثبات شد)

```text
راه‌حل V55.18 دوفازی بود:
فاز۱: کارت فعال با tween(280) به بیرون صفحه می‌رفت (translationX=+520dp)
فاز۲: activeIndex عوض می‌شد؛ در کشیدن به راست کارت قدیمی هنوز مرئی است
      (relative=1 در پشته) اما translation فقط روی کارت «فعال» اعمال
      می‌شود → همان کارت از بیرون صفحه به جایگاه پشته «تلپورت» می‌کرد.
به‌علاوه کل حرکت ۲۸۰+۳۰۰=۵۸۰ms بود در برابر یک فاز ۲۸۰ms جهت چپ.
(در کشیدن به چپ کارت قدیمی relative=7 می‌شود و اصلاً رندر نمی‌شود؛ برای
همین تلپورت آن هرگز دیده نمی‌شد و چپ همیشه نرم بود.)
```

### راه‌حل

```text
کشیدن به راست تک‌فاز و هم‌زمان شد:
- state جدید: returningIndex + returnX/returnY (Animatable)
- در settle شاخهٔ direction == -1: returningIndex=activeIndex،
  returnX.snapTo(x) و returnY.snapTo(y) (نقطهٔ رهاشدن انگشت)،
  dragX.snapTo(targetX) (کارت واردشونده بیرون صفحه)، تغییر activeIndex،
  سپس هم‌زمان returnX/returnY→0 و dragX→0 با tween(300, FastOutSlowIn)؛
  در پایان returningIndex=-1.
- graphicsLayer: کارت index==returningIndex && !active حالا
  translationX/Y = returnX/returnY.value می‌گیرد (به‌جای 0f تلپورتی).
- شاخهٔ چپ (else) دقیقاً رفتار قبلی: خروج انیمیت‌شده + snapTo(0f).
```

### تأیید

```text
جدید: V55_18_1SmoothRightReturnHotfixTest (۳ تست: state برگشت / تک‌فازبودن
شاخهٔ راست و دست‌نخوردگی چپ / اعمال translation کارت برگشتی در graphicsLayer)
verify: require جدید V55.18.1 (requireهای V55.18 هم برقرار می‌مانند چون
dragX.animateTo(0f, tween(300, ...)) و if (direction == -1) { هنوز در کدند)
شبیه‌سازی python: پیوستگی مکان هر دو کارت در ۵ فریم، بدون تلپورت، تک‌فاز
FINAL_NATIVE_VERIFY=PASS EXIT=0 · تست V55_18 قدیمی همچنان سبز
```

### راهنمای تست دستگاه

```text
کارت‌های مدیریت معلم (آمار/بانک/تصحیح/مانده/پاسخ/درخواست‌ها):
- کشیدن به راست: کارت زیر انگشت نرم به پشته برگردد و «هم‌زمان» کارت قبلی
  از راست وارد شود؛ یک حرکت پیوستهٔ ~۳۰۰ms مثل جهت چپ، بدون هیچ پرشی.
- کشیدن به چپ: مثل قبل.
```

### عملیات

```text
فایل‌ها: TeacherManagementCardsScreen.kt (returningIndex/returnX/returnY +
graphicsLayer) / V55_18_1SmoothRightReturnHotfixTest.kt (جدید) /
verify (require V55.18.1) / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.18
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۲) V56.0 — بهینه‌سازی تبلت، پچ ۱ از ۳: زیرساخت + انتخاب چیدمان در ظاهر

### درخواست کاربر

```text
«برنامه رو برای تبلت بهینه کن. در بخش ظاهر یک قسمت حالت انتخاب خودکار تبلت
و گوشی بذار. در حالت تبلت چینش تغییر کند و اگر لازم است در ۳ پچ بده.»
```

### تحویل پچ ۱

```text
- DeviceLayoutMode جدید (AUTO/PHONE/TABLET) در AppearancePreferences با کلید
  DataStore «device_layout»؛ پیش‌فرض AUTO؛ مقدار نامعتبر → AUTO.
- فایل جدید core/ui/DeviceLayout.kt:
  LocalTabletLayout (staticCompositionLocalOf { false })،
  TABLET_MIN_SMALLEST_WIDTH_DP=600 (استاندارد اندروید)،
  isTabletDevice() از LocalConfiguration.smallestScreenWidthDp،
  resolveTabletLayout(mode) برای ترکیب انتخاب کاربر + تشخیص خودکار.
- ExamAppTheme: LocalTabletLayout provides tabletLayout برای کل درخت UI.
- بخش «ظاهر»: کارت «چیدمان دستگاه» با ۳ چیپ خودکار/گوشی/تبلت + متن
  «چیدمان فعلی: تبلت/گوشی» + setDeviceLayoutMode در ViewModel.
- «بازگردانی تنظیمات ظاهری» (reset) این تنظیم را هم به خودکار برمی‌گرداند
  (store.clear همان قبلی است، کد جدید لازم نداشت).
```

## ۱۶۳) V56.1 — بهینه‌سازی تبلت، پچ ۲ از ۳: چیدمان صفحه‌های اصلی

```text
- منوی اصلی Design69MainMenuScreen: در تبلت ۳ ستون (TABLET_COLUMNS=3) و
  سقف پهنای TABLET_MAX_WIDTH_DP=840dp؛ گوشی مثل قبل COLUMNS=2 و 560dp
  (قرارداد Neumorphic69IntegrationTest دست‌نخورده: COLUMNS==2). ردیف ناقص
  تبلت (۸ کارت معلم = ۳+۳+۲) با Spacer(weight) پر می‌شود.
- کارت‌های مدیریت TeacherManagementCardsScreen: پشته در تبلت سقف 620dp.
- سازندهٔ آزمون ExamBuilderScreen: LazyColumn در تبلت وسط با سقف 760dp
  (wrapContentWidth(CenterHorizontally) + widthIn)؛ گوشی بدون Modifier اضافه.
- داشبورد معلم 760dp، بانک سؤال 760dp، بخش ظاهر تنظیمات 680dp — همه فقط
  در تبلت؛ در گوشی شاخهٔ else Modifier (بدون تغییر رفتار).
```

## ۱۶۴) V56.2 — بهینه‌سازی تبلت، پچ ۳ از ۳: شبکه‌های پنجره‌های انتخاب

```text
- FigurePickerDialog: هندسه Adaptive(104dp→140dp در تبلت)؛ نمودار
  Fixed(2→3 در تبلت). رشتهٔ «GridCells.Fixed(2)» عمداً در کد ماند چون
  needle تست V55_12AtlasFlowArrowTest است (درس تکراری needleها).
- AtlasEditorDialog (انتخاب نوع آناتومی/فیزیک/شیمی): Fixed(2→3 در تبلت).
```

### تأیید V56.x

```text
جدید: V56_0TabletLayoutFoundationTest (۴ تست)، V56_1TabletScreensLayoutTest
(۳ تست)، V56_2TabletDialogGridsTest (۲ تست) · verify: ۶ require جدید V56.x ·
شبیه‌سازی python همهٔ assertionهای جدید + قراردادهای قدیمی (V55_12 Fixed(2)
در segment FigureTypePickerDialog، V19 bottom=112dp، V25 بارم 62x40،
Neumorphic69 COLUMNS=2) سبز · اسکن سراسری ۵۹۹ needle → فقط همان هشدار کاذب
شناخته‌شدهٔ V55_16 (\$ escape) · اسکن assertFalse در فایل‌های تغییرکرده →
همهٔ ۵۴ مورد بررسی شد: متغیر تست از فایل/سگمنت دیگری خوانده می‌شود، تداخلی
نیست · تراز آکولاد صفر در هر ۱۲ فایل · FINAL_NATIVE_VERIFY=PASS EXIT=0
kotlin_files=198 (DeviceLayout.kt جدید)
```

### راهنمای تست دستگاه

```text
گوشی (بدون تغییر رفتار): منو ۲ستونه، همهٔ صفحه‌ها مثل قبل.
تنظیمات → ظاهر → «چیدمان دستگاه»:
۱) خودکار: روی تبلت (صفحهٔ ≥600dp) چیدمان تبلت، روی گوشی چیدمان گوشی.
۲) تبلت (حتی روی گوشی برای آزمایش): منوی اصلی ۳ستونه و پهن‌تر؛ کارت‌های
   مدیریت با پهنای محدود وسط؛ سازندهٔ آزمون/داشبورد/بانک سؤال ستون وسط؛
   پنجره‌های درج شکل/نمودار/آناتومی ۳ستونه.
۳) گوشی: همه‌جا چیدمان گوشی حتی روی تبلت.
تنظیم بعد از بستن برنامه باقی بماند؛ «بازگردانی تنظیمات ظاهری» → خودکار.
```

### عملیات

```text
پچ ۱ (V56_0_tablet_layout_foundation): AppearancePreferences/DeviceLayout(جدید)/
ExamAppTheme/ProfileSettingsScreen/ProfileSettingsViewModel + تست V56_0
پچ ۲ (V56_1_tablet_screens_layout): Design69MainMenuScreen/
TeacherManagementCardsScreen/ExamBuilderScreen/TeacherDashboardScreen/
QuestionBankScreen/ProfileSettingsScreen + تست V56_1
پچ ۳ (V56_2_tablet_dialog_grids): FigurePickerDialog/AtlasEditorDialog +
تست V56_2 + verify(۶ require) + changelog + هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V55.18.1 — ترتیب اعمال: پچ ۱ ← پچ ۲ ← پچ ۳
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۵) V57.0 — نمایش سطربندی‌شده، زوم شکل‌ها، تناوبی افقی، کادرهای نامگذاری

### درخواست‌های کاربر

```text
۱) «کادر متن سؤال و گزینه‌ها سطر سطر باشد؛ اینتر معلم برای دانش‌آموز هم سطر
   جدید باشد و به هم نریزد»
۲) «تصاویر/نمودارها/شکل‌ها برای دانش‌آموز زوم داشته باشند و اگر در سطرشان
   چیز دیگری نوشته شده، به سطر پایین‌تر بیایند و کامل نمایش داده شوند»
۳) «جدول تناوبی برای دانش‌آموز قابلیت بازشدن افقی داشته باشد»
۴) «نشانه‌های پیکان‌دار آناتومی/فیزیک/شیمی: دانش‌آموز بتواند در کادرهای
   ایجادشده تایپ کند»
```

### طراحی و تحویل

```text
۱) RichTextSplitter.splitRows (RichText.kt): '\n' → سطر جدید؛ شکل همیشه سطر
   تمام‌عرض خودش (اگر سطرش محتوا داشته باشد به سطر بعد می‌رود)؛ فرمول inline
   می‌ماند؛ سطر خالیِ خودکارِ بعد از توکن شکل (که ویرایشگر معلم می‌گذارد)
   حذف؛ سطرهای خالی عمدی معلم حفظ. NativeMathText حالا Column از سطرهاست
   (نام پارامتر حلقهٔ سطر عمداً segments ماند — needle قدیمی verify
   «segments.forEach»). امضای NativeMathText سازگار است؛ همهٔ ۱۱ مصرف‌کنندهٔ
   قبلی بدون تغییر (پیش‌فرض‌ها) همان رفتار را دارند و سطربندی خودکار می‌گیرند.
۲) ZoomableFigureDialog جدید: تمام‌صفحه، detectTransformGestures با
   coerceIn(1f,6f) + pan + دوضربه بازنشانی؛ در NativeMathText با
   zoomableFigures=true (فقط StudentExamScreen — متن سؤال و گزینه‌ها) لمس
   شکل آن را باز می‌کند؛ تصاویر سؤال/گزینه هم با clickable همین دیالوگ.
۳) kind=='p' → rotatable=true؛ چیپ «نمایش افقی» با requiredSize(maxH,maxW)
   + rotationZ=90f؛ پیش‌فرض افقی باز می‌شود.
۴) AtlasFigureView: پارامترهای blankAnswers/onBlankAnswer؛ در حالت
   دانش‌آموز هر نشانه (که برچسب معلم‌داده ندارد) OutlinedTextField
   «نام بخش n» دارد؛ زوم اطلس فقط با لمس خود تصویر (onImageTap) تا کادرها
   آزاد باشند. ذخیره: AtlasBlankAnswerCodec (جدید) پاسخ‌ها را به‌صورت خطوط
   «n) پاسخ» بالای متن آزاد در همان TextAnswer ادغام می‌کند —
   format/parse/freeText/merge؛ قرارداد سرور و draft و تصحیح تغییری ندارد و
   معلم همین خطوط را در تصحیح می‌بیند. کادر «پاسخ شما» فقط بخش آزاد را
   ویرایش می‌کند و خطوط نامگذاری را پاک نمی‌کند.
```

### تأیید

```text
جدید: V57_0StudentRichViewTest — ۳ تست اجرایی JVM (splitRows دو سناریو +
round-trip کدک) و ۳ تست اتصال UI. شبیه‌سازی python وفادار splitRows و کدک
سبز. verify: ۶ require جدید V57.0؛ needle قدیمی «segments.forEach» در
NativeMathText حفظ شد؛ V53_3AtlasNativeTest («AtlasFigureView(» در mathText،
بدون android.webkit) همچنان سبز. FINAL_NATIVE_VERIFY=PASS EXIT=0
kotlin_files=200 (ZoomableFigureDialog + AtlasBlankAnswerCodec جدید).
```

### راهنمای تست دستگاه

```text
معلم: سؤالی چندسطری با اینتر + شکل وسط سطر + فرمول وسط جمله بسازد.
دانش‌آموز:
۱) سطربندی دقیقاً مثل معلم؛ شکل وسط سطر → سطر مستقل تمام‌عرض.
۲) لمس شکل/نمودار/تصویر → پنجرهٔ زوم؛ دو انگشت بزرگ‌نمایی، دوضربه بازنشانی.
۳) جدول تناوبی → بازشدن افقی (چیپ «نمایش افقی» برای برگرداندن).
۴) سؤال آناتومی/فیزیک/شیمی با پیکان‌های بی‌برچسب → زیر تصویر برای هر شماره
   کادر «نام بخش n»؛ تایپ و رفتن به سؤال بعد و برگشت → پاسخ‌ها بمانند؛
   در تصحیح معلم پاسخ‌ها به‌صورت «1) ...» دیده شوند.
```

### عملیات

```text
پچ: V57_0_student_rows_zoom_atlas_blanks — فایل‌ها: RichText.kt (splitRows) /
NativeMathText.kt (بازنویسی سطری + zoomableFigures + atlasBlankAnswers) /
ZoomableFigureDialog.kt (جدید) / AtlasBlankAnswerCodec.kt (جدید) /
AtlasFigureView.kt (کادرهای تایپ + onImageTap) / StudentExamScreen.kt
(اتصال زوم و کدک) / V57_0StudentRichViewTest.kt (جدید) / verify / changelog
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V56.2
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۶) V58.0 — بازطراحی صفحهٔ آزمون دانش‌آموز + تایمر هوشمند (پچ ۱ از ۳)

### درخواست‌های کاربر و تحویل

```text
۱) «نوار بالای پنجرهٔ زوم روی جدول تناوبی افقی می‌افتد»: ZoomableFigureDialog
   به Column تبدیل شد؛ نوار بالا جدا و چرخش در BoxWithConstraints(weight=1)
   زیر آن — requiredSize(maxH,maxW) از محدودهٔ زیر نوار محاسبه می‌شود.
۲) «حذف هدر/نام آزمون + سطر اسکرول شماره سؤال‌ها با آیکن قبلی/بعدی دو سر +
   حذف دکمه‌های قبلی/بعدی پایین + نوار پایین: خروج|زمان‌سنج|ارسال»: در
   StudentExamContent سطر Row با horizontalScroll و IconButtonهای
   KeyboardArrowRight/Left؛ bottomBar = خروج + ExamCountdownText + ارسال.
۳) «تایمر تا شروع پاسخ‌گویی شروع نشود»: startTimer از openExam حذف و به
   startExam منتقل شد (started=true).
۴) «ویرایش وسط آزمون → پنجرهٔ موارد + توقف تایمر»: watchExamChanges هر ۲۰
   ثانیه refreshActiveExam و diffExams (عنوان/تعداد/متن/بارم/مهلت/حذف)؛
   examChangeNotes → AlertDialog؛ timerPaused تا بستن؛ زمان مکث با
   pausedTotalMs به مهلت اضافه می‌شود (deadline + pausedTotalMs - now).
۵) «پیام به بانک سؤال اضافه شد»: state.notice جدید + Snackbar در builder.
۶) «زمان‌سنج رنگی»: ExamCountdownText سبز→نارنجی(<۵۰٪)→قرمز(<۱۵٪ یا ۵ دقیقه).
۷) «علامت مرور با نگه‌داشتن ۲ ثانیه»: combinedClickable(onLongClick=toggleFlag)
   روی چیپ شماره؛ دکمهٔ متنی حذف؛ بند verify قدیمی به‌روز شد.
۸) «کادر نامگذاری در پنل معلم لازم نیست»: CSS بلوک nativeToolbarHide حالا
   .an-af را پنهان می‌کند + showAtlasBlanks=false در پیش‌نمایش‌های builder؛
   version.txt کادر → v58.0-teacher-no-blanks.
```

## ۱۶۷) V58.1 — نظارت آزمون + گزارش‌ها برای معلم (پچ ۲ از ۳)

```text
- تشخیص‌ها (فقط ثبت؛ FLAG_SECURE همچنان جلوی تصویر را می‌گیرد):
  اسکرین‌شات: Activity.ScreenCaptureCallback (API 34+) + مجوز مانیفست
  DETECT_SCREEN_CAPTURE؛ ضبط صفحه: addScreenRecordingCallback (API 35+) +
  DETECT_SCREEN_RECORDING؛ خروج از برنامه (ON_PAUSE=app_leave،
  ON_STOP=app_close) و خروج از صفحهٔ آزمون (onDispose=exam_screen_leave).
- ViewModel: recordSecurityEvent (ثبت فوری best-effort با
  native_monitor_upsert_v1) + markQuestionEnter/questionTimeSpentMs/
  questionVisits + monitorReport() = {entered_at, left_at, events,
  question_time_ms, question_visits}؛ همراه submit در
  p_meta.monitor_report هم می‌رود.
- زنجیره: ExamRepository.reportMonitor (پیش‌فرض خالی) → Queued (فقط آنلاین)
  → Supabase RPC. SubmittedExam.monitorReportJson +
  PendingSubmissionPayload.monitor_report (سازگار عقب‌رو، nullable).
- معلم: دکمهٔ «گزارش‌ها» کنار «ورود به تصحیح» روی کارت آزمون → 
  MonitorReportsDialog: هر دانش‌آموز با برچسب فارسی رویدادها + زمان
  ورود/خروج + مدت پاسخ‌گویی/بازدید هر سؤال.
- SQL جدید supabase/migrations/20260825_native_exam_monitor_v58.sql:
  جدول native_exam_monitor (PK exam+student، RLS مالک) +
  native_monitor_upsert_v1 (ادغام ||) + native_monitor_list_v1 (فقط
  e.teacher_id = v_uid؛ نام از profiles).
```

## ۱۶۸) V58.2 — نمودار پاسخ دانش‌آموز با اجازهٔ معلم (پچ ۳ از ۳)

```text
- معلم: چیپ «نمودار پاسخ دانش‌آموز» فعال/غیرفعال در کارت سؤال (کنار تصویر
  پاسخ)؛ QuestionDraft.allowAnswerGraph؛ ExamQuestionCodec کلید
  allowAnswerGraph در JSON سؤال؛ StudentExamPayloadCodec →
  QuestionPresentation.allowAnswerGraph.
- دانش‌آموز: StudentAnswerGraph زیر پاسخ — «رسم نمودار پاسخ» → همان جریان
  دومرحله‌ای معلم (FigureTypePickerDialog(GRAPH) → FigurePickerDialog با
  پارامترها مثل سهمی)؛ توکن %%FIG:...%% در «همان TextAnswer» (جایگزینی
  بازهٔ توکن قبلی/افزودن به انتها/حذف فقط بازهٔ توکن) — قرارداد سرور
  دست‌نخورده و معلم در تصحیح همان نمودار را با NativeMathText می‌بیند.
- نمایش زندهٔ نمودار پاسخ با zoomableFigures=true.
```

### تأیید V58.x

```text
جدید: V58_0StudentExamUxTimerTest (۶ تست)، V58_1ExamMonitorReportsTest
(۵ تست)، V58_2StudentAnswerGraphTest (۳ تست) · verify: بند قدیمی «علامت
برای مرور» → onLongClick + ۱۰ require جدید V58.x · شبیه‌سازی python همهٔ
assertionها (پس از اصلاح ۲ needle بخش‌محور) سبز · اسکن سراسری ۷۰۰ needle →
فقط هشدار کاذب شناخته‌شدهٔ V55_16 · اسکن assertFalse فایل‌های تغییرکرده →
۲۰ مورد بررسی شد؛ همه از فایل/سگمنت دیگر می‌خوانند · تراز آکولاد/پرانتز
(خارج رشته‌ها) صفر · FINAL_NATIVE_VERIFY=PASS EXIT=0 kotlin_files=200
نکته: تست ViewModel قدیمی (StudentExamViewModelTest) به تایمر وابسته نیست؛
submit بدون startExam همچنان کار می‌کند (رفتار حفظ شد).
```

### راهنمای تست دستگاه

```text
پچ ۱: زوم تناوبی → نوار روی جدول نیفتد؛ صفحهٔ آزمون بدون هدر؛ سطر شماره‌ها
اسکرول + آیکن دو سر؛ پایین: خروج|زمان‌سنج|ارسال؛ تایمر فقط بعد «شروع
پاسخ‌گویی»؛ ویرایش آزمون توسط معلم وسط آزمون → پنجرهٔ موارد + توقف تایمر؛
آیکن بانک → پیام «به بانک سؤال اضافه شد»؛ رنگ تایمر سبز→نارنجی→قرمز؛
نگه‌داشتن شمارهٔ سؤال ~۲ ثانیه → ★؛ کادر متن معلم بدون کادرهای نامگذاری.
پچ ۲ (اول SQL را در Supabase اجرا کنید): تلاش اسکرین‌شات (اندروید ۱۴+)/ضبط
(۱۵+)/رفتن به Home وسط آزمون → بعد از ارسال، معلم: تصحیح و نظارت → کارت
آزمون → «گزارش‌ها».
پچ ۳: معلم چیپ «نمودار پاسخ دانش‌آموز» را فعال کند → دانش‌آموز «رسم نمودار
پاسخ» → انتخاب مثلا سهمی → پارامترها → درج؛ ویرایش/حذف؛ معلم در تصحیح
نمودار را ببیند.
```

### عملیات

```text
پچ ۱ (V58_0_student_exam_ux_timer): ZoomableFigureDialog/StudentExamScreen/
StudentExamViewModel/StudentHomeScreen/ExamBuilderScreen/ExamBuilderViewModel/
QuestionDraft(notice)/NativeMathText(showAtlasBlanks)/QuestionOptionMedia/
question_editor.html+version.txt/V58_0 تست/verify/changelog/هندآف
پچ ۲ (V58_1_exam_monitor_reports): AndroidManifest(دو مجوز DETECT)/
StudentExamScreen(callbackها)/StudentExamViewModel(گزارش)/ExamRepository/
QueuedExamRepository/SupabaseStudentExamRepository/PendingSubmissionCodec/
ExamModels(monitorReportJson)/SupabaseGradingRepository/GradingViewModel/
GradingScreen(دکمه+دیالوگ)/SQL migration جدید/V58_1 تست
پچ ۳ (V58_2_student_answer_graph): QuestionDraft/ExamQuestionCodec/
ExamModels(QuestionPresentation)/StudentExamPayloadCodec/ExamBuilderScreen/
ExamBuilderViewModel/StudentExamScreen(StudentAnswerGraph)/V58_2 تست
SQL: فقط پچ ۲ (20260825_native_exam_monitor_v58.sql — باید در Supabase اجرا
شود) · Edge/Secret/Dependency جدید: ندارد
پیش‌نیاز: V57.0 — ترتیب: پچ ۱ ← پچ ۲ ← پچ ۳
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```

## ۱۶۹) V58.0.1 — هات‌فیکس: خطای کامپایل import داخلی weight

### گزارش CI

```text
e: ZoomableFigureDialog.kt:13:43 Cannot access
'val RowColumnParentData?.weight: Float': it is internal in file.
> Task :app:compileDebugKotlin FAILED
```

### ریشه و راه‌حل

```text
پچ V58.0 هنگام بازنویسی ZoomableFigureDialog (جداکردن نوار بالا از محتوای
چرخان) به‌اشتباه import سطح‌بالای
androidx.compose.foundation.layout.weight را اضافه کرد. weight فقط عضو
RowScope/ColumnScope است؛ آن import به property داخلیِ
RowColumnParentData.weight resolve می‌شود و کامپایل می‌شکند.
راه‌حل: حذف همان یک خط import. هر دو استفادهٔ weight فایل داخل scope
درست‌اند (Text داخل Row سطر بالا؛ BoxWithConstraints داخل Column) و بدون
import کامپایل می‌شوند. اسکن سراسری: هیچ فایل دیگری این import را ندارد
(StudentExamScreen weight دارد ولی import اشتباه ندارد — داخل RowScope).
```

### تأیید

```text
جدید: V58_0_1WeightImportHotfixTest (۲ تست: حذف import در همین فایل + اسکن
اجرایی کل سورس برای import های scoped ممنوع weight/align) · verify: require
جدید V58.0.1 · تست‌های V58_0/V57_0 (needleهای weight ندارند) دست‌نخورده سبز ·
FINAL_NATIVE_VERIFY=PASS EXIT=0
```

### عملیات

```text
پچ: V58_0_1_weight_import_hotfix — فایل‌ها: ZoomableFigureDialog.kt (حذف ۱
import) / V58_0_1WeightImportHotfixTest.kt (جدید) / verify / changelog / هندآف
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: V58.2 (هر سه پچ V58 اعمال شده)
FINAL_NATIVE_VERIFY → PASS, EXIT=0
```
