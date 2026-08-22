# هندآف جامع مهاجرت سامانه آزمون از WebView به Native Kotlin

**آخرین به‌روزرسانی:** ۲۰۲۶-۰۸-۲۲ — V45.8.9 حذف تم روشن استاتیک و رفع صفحهٔ سفید ویرایشگر فرمول روی مبنای V45.8.8
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


---

## ۹۳) V45.6 — بازگرداندن ویرایشگر فرمول WebView پس از revert V45.5

### علت

پس از انتشار V45.5 (revert مجموعه V45.4 تا V45.4.2 و بازگشت به ویرایشگر
فرمول بومی Compose)، درخواست کاربر این بود که همان `math-editor.html`
مستقل (۶۲۴٬۲۰۹ بایت) دوباره به‌عنوان ویرایشگر فرمول در برنامه جایگزین شود.
SHA-256 این فایل با asset ثبت‌شده در V45.4
(`aae5777f9fb8705ccb2ed4a7c52e426e44ab45c7280055f936ed0aff4e917ceb`)
بایت‌به‌بایت برابر بود؛ بنابراین به‌جای بازنویسی، همان تغییرست با
`git revert 05338d2` (بدون commit) روی HEAD فعلی اعمال شد.

### دامنهٔ تغییرات (همان V45.4 + V45.4.1 + V45.4.2)

```text
افزوده:
  app/src/main/assets/math_editor_standalone.html     (624,209 بایت)
  app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt
  docs/fa/MATH_EDITOR_WEBVIEW_V45_4_FA.md
  .gitattributes (علامت‌گذاری asset به‌عنوان binary)

حذف:
  app/src/main/java/ir/exam/app/core/math/FormulaBoxEditor.kt
  app/src/main/java/ir/exam/app/core/math/FormulaMatrixFactory.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaEditorDialog.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaLibraryDialog.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaLibraryNavigator.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaReferenceLibrary.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaReferenceStore.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaSmartHubDialog.kt
  app/src/main/java/ir/exam/app/ui/math/FormulaSmartReference.kt
  app/src/main/assets/formula_library_v13.json
  تست‌های متناظر در app/src/test/...

تغییر:
  app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
    (import و فراخوانی FormulaEditorDialog → MathEditorWebViewDialog)
  app/src/test/java/ir/exam/app/ui/app/Neumorphic69IntegrationTest.kt
    (شمارش فایل‌های مجاز android.webkit)
  app/src/test/java/ir/exam/app/ui/app/V19InteractionTest.kt
    (تست به MathEditorWebViewDialog و asset منتقل شد)
  scripts/verify_native_final.py
    (همان قوانین V45.4.1)
```

نمایش و چاپ فرمول (NativeMathAst / NativeMathSvgRenderer /
NativeMathCanvasRenderer / NativeMathFormatter / NativeNaturalMathConverter /
FormulaTextCodec / OfficialPdfPrintAdapter / PdfExamRenderer) دست‌نخورده
باقی ماند؛ فقط ورودی/ویرایش از طریق WebView است.

### قرارداد bridge (بدون تغییر در asset)

```text
1) بارگذاری file:///android_asset/math_editor_standalone.html
2) آماده‌بودن با poll تابع openMath (80ms تا 8s)
3) seed ورودی qTxt_1 با $tex$ یا $$tex$$ و select
4) openMath('qTxt_1')
5) wrap mfApply → ارسال APPLY با tex به پل → onInsert(tex)
6) closeMath → ارسال CLOSED → onDismiss
دکمهٔ بازگشت Android نیز closeMath را صدا می‌زند.
```

### تست و تحویل

```text
python3 scripts/verify_native_final.py
  → FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3 (EXIT 0)
git diff --check → PASS
sha256sum asset → aae5777f... (مطابق V45.4)
```

تست واحد (`./gradlew testDebugUnitTest lintDebug`) در این محیط به‌دلیل
در دسترس نبودن Android SDK اجرا نشد؛ اما کد بایت‌به‌بایت همان ترکیبی است
که در V45.4.2 قبلاً CI سبز را گذرانده بود و فقط با یک
`git revert --no-commit` بازسازی شده است. انتظار می‌رود پس از push،
GitHub Actions همان ۲۹۷ تست را سبز کند.

### SQL / Secret / وابستگی جدید

ندارد. از `SUPABASE_SERVICE_KEY` یا `service_role` استفاده نشده است. هیچ
تغییری در `applicationId`، امضا، یا `release.keystore` اعمال نشد.

### فایل پچ

```text
/home/user/V45_6_webview_formula_editor.patch   (۵۳۴٬۸۰۵ بایت، در ریشه)
```

پیش‌نیاز: V45.5 (HEAD `05338d2`). پس از اعمال این پچ، نسخهٔ مؤثر برابر
V45.4.2 است و هندآف V45.6 این واقعیت را ثبت می‌کند.


---

## ۹۴) راهنمای عملی: گرفتن سورس، اعمال پچ و push (برای ماشین توسعه با Git/WSL)

این بخش روش کار ثابتی است که در V45.6 روی همین مخزن تست شد و برای هر پچ
بعدی هم تکرار می‌شود.

### ۰) پیش‌نیازهای روی ماشین توسعه

```text
- Git نصب باشد (در WSL: sudo apt install git)
- دسترسی به GitHub با HTTPS (نیازی به SSH نیست؛ هنگام push، Git از
  Credential Manager ویندوز یا PAT ذخیره‌شده استفاده می‌کند)
- در صورت اجرای محلی build/test: JDK 17 و Android SDK (مطابق
  build.gradle.kts پروژه)
- quota مخزن: در زمان نگارش این هندآف مخزن عمومی است اما در حال
  خصوصی‌شدن است؛ بهتر است قبل از خصوصی‌شدن یک‌بار کلون شود.
```

### ۱) کلون تازه (اگر هنوز پروژه محلی ندارید)

```bash
# در WSL، پوشه‌ای که می‌خواهید پروژه در آن قرار گیرد
cd /mnt/c/Users/Hashem/Downloads
git clone https://github.com/hashemamiri/exam-app-kotlin.git
cd exam-app-kotlin
git log --oneline -5        # تأیید آخرین commit
```

اگر قبلاً کلون دارید و فقط می‌خواهید تازه شود:

```bash
cd exam-app-kotlin
git fetch origin
git switch main
git pull --ff-only origin main
```

### ۲) ساخت پچ جدید روی ماشین توسعه‌دهنده (سمت ایجنت)

پچ باید با خودِ Git (نه خروجی `diff` متنی ساده) و در ریشهٔ workspace
قرار گیرد:

```bash
# بعد از اعمال همهٔ تغییرات و stage آن‌ها
git add -A
git status                                 # مرور فایل‌ها

# اگر پچ یک commit جدید است:
git commit -m "feat(vXX.Y): ..."
# یا اگر شامل چند commit است، همه را می‌توان در یک فایل پچ بسته‌بندی کرد.

# تولید فایل پچ از آخرین commit:
git format-patch -1 HEAD --stdout > /home/user/VXX_Y_topic.patch
ls -la /home/user/VXX_Y_topic.patch
```

نکته‌ها:
- هرگز فایل‌های باینری (`release.keystore`، `*.jks`، `local.properties`،
  فایل‌های حاوی `SUPABASE_SERVICE_KEY` یا `service_role`) نباید در پچ
  باشند. این فایل‌ها از قبل در `.gitignore` هستند؛ پوشش را قبل از commit
  با `git status` و `git diff --cached --stat` بازبینی کنید.
- هر پچ باید در همان commit، به‌روزرسانی
  `docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md` را هم داشته باشد (قانون ۴
  بخش قوانین).
- اگر پچ SQL دارد، فایل `.sql` آن فقط روی پروژهٔ اصلی
  `eazwuyrymsvdkwckdpco` اجرا شود؛ هرگز روی
  `niuadepncroqoebrxpqk` (پروژه ممنوع) اجرا نشود.

### ۳) اعمال پچ روی کلون محلی (سمت کاربر/توسعه)

فایل پچ را (مثلاً `V45_6_webview_formula_editor.patch`) در پوشهٔ
`Downloads` ویندوز یا هر مسیر در دسترس دیگری قرار دهید، سپس:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin/exam-app-kotlin

# ۱) اطمینان از تمیز بودن درخت کاری
git status
# خروجی باید "nothing to commit, working tree clean" باشد.

# ۲) اطمینان از تازه بودن main
git pull --ff-only origin main

# ۳) اعمال پچ با git am (تاریخچه/نویسنده حفظ می‌شود)
git am /mnt/c/Users/Hashem/Downloads/VXX_Y_topic.patch
```

خروجی موفق:

```text
Applying: feat(vXX.Y): ...
```

اگر `git am` با تعارض متوقف شد:

```bash
# دیدن فایل‌های تعارضی
git status
# رفع تعارض در فایل‌ها با ویرایشگر، سپس:
git add <resolved-files>
git am --continue
# یا انصراف کامل:
git am --abort
```

روش جایگزین بدون حفظ نویسنده (فقط در صورتی که `git am` جواب نداد):

```bash
git apply --whitespace=nowarn /mnt/c/Users/Hashem/Downloads/VXX_Y_topic.patch
git add -A
git commit -m "feat(vXX.Y): ..."
```

### ۴) بررسی پیش از push

```bash
# مرور commit جدید
git log --oneline -3
git status -sb          # باید "ahead 1" (یا بیشتر) بگوید

# اسکریپت تأیید نهایی (بدون نیاز به SDK):
python3 scripts/verify_native_final.py
# خروجی منتظر:
#   FINAL_NATIVE_VERIFY=PASS kotlin_files=... edge_functions=...

# بررسی whitespace/پیام خطای ساده:
git diff --check HEAD~1 HEAD
```

در صورت دسترسی به JDK 17 و Android SDK (اختیاری، چون CI هم اجرا می‌کند):

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

### ۵) push به مخزن اصلی

```bash
git push origin HEAD
```

خروجی موفق چیزی شبیه:

```text
Enumerating objects: ... done.
...
To https://github.com/hashemamiri/exam-app-kotlin.git
   05338d2..9e29baf  HEAD -> main
```

پس از push، GitHub Actions خودش اجرا می‌شود:
- Workflow: «ساخت APK آزمایشی و Release Kotlin»
- آدرس: `https://github.com/hashemamiri/exam-app-kotlin/actions`
- اگر run قرمز شد، لاگ همان مرحله را بگیرید و همراه شمارهٔ run
  بفرستید؛ بدون لاگ واقعی هیچ حدسی زده نمی‌شود (اصل اول).

### ۶) اگر محیطی که پچ را ساخته نمی‌تواند push کند (مثل sandbox بدون اعتبار)

در این حالت دو فایل در ریشهٔ workspace تحویل داده می‌شود:

| فایل | کاربرد |
|---|---|
| `VXX_Y_topic.patch` | `git am` روی کلون دیگر (توصیه‌شده) |
| `VXX_Y_topic.bundle` | `git fetch <bundle> && git merge --ff-only FETCH_HEAD` |

استفاده از bundle:

```bash
git fetch /path/to/VXX_Y_topic.bundle HEAD
git merge --ff-only FETCH_HEAD
git push origin HEAD
```

### ۷) بازگرداندن یک push در صورت نیاز

هرگز history را روی `main` با `push --force` بازنویسی نکنید. اگر پچِ
push‌شده باید خنثی شود، یک commit جدید به‌عنوان revert بسازید:

```bash
git revert <SHA-commit>
git push origin HEAD
```

برای بازگرداندن تغییری که قبلاً خودش revert شده بود (همان الگوی V45.6):

```bash
git revert --no-edit <SHA-revert-commit>
git push origin HEAD
```

### ۸) یادآوری‌های امنیتی (غیرقابل مصالحه)

```text
- هرگز SUPABASE_SERVICE_KEY یا service_role را در کد، Git، چت،
  فایل پچ، APK یا local.properties قابل commit قرار ندهید.
- release.keystore و رمز/alias آن فقط روی ماشین امن توسعه محلی بماند.
- applicationId برای دیباگ ir.exam.app.native و برای release
  ir.exam.app است؛ تغییری داده نشود مگر با تصمیم صریح.
- SQL فقط روی پروژهٔ eazwuyrymsvdkwckdpco اجرا شود؛
  niuadepncroqoebrxpqk ممنوع است.
```

### ۹) چک‌لیست کوتاه هر پچ

```text
[ ] پچ در ریشهٔ /home/user با نام VXX_Y_topic.patch
[ ] docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md در همان پچ به‌روزرسانی شد
[ ] SQL در صورت نیاز و فقط روی پروژه اصلی
[ ] python3 scripts/verify_native_final.py → PASS
[ ] git diff --check → PASS
[ ] git status تمیز و فقط فایل‌های لازم staged
[ ] git am روی کلون محلی موفق (تست اعمال)
[ ] git push origin HEAD
[ ] مانیتور کردن run Actions تا سبز شدن
```

---

## ۹۵) سوابق دستورات V45.6 (نمونهٔ واقعی از همین نسخه)

این خروجی واقعی push در V45.6 است و الگوی مرجع برای پچ‌های بعد است:

```text
hashem@Hashem:/mnt/c/Users/Hashem/Downloads/exam-app-kotlin/exam-app-kotlin$
$ git am /mnt/c/Users/Hashem/Downloads/V45_6_webview_formula_editor.patch
Applying: feat(v45.6): bring back the WebView formula editor with standalone math-editor.html

$ git push origin HEAD
Enumerating objects: 63, done.
Counting objects: 100% (63/63), done.
Delta compression using up to 16 threads
Compressing objects: 100% (23/23), done.
Writing objects: 100% (36/36), 155.94 KiB | 14.18 MiB/s, done.
Total 36 (delta 14), reused 21 (delta 5), pack-reused 0 (from 0)
remote: Resolving deltas: 100% (14/14), completed with 14 local objects.
To https://github.com/hashemamiri/exam-app-kotlin.git
   05338d2..9e29baf  HEAD -> main
```

- کامیت محلی ایجنت: `eb267394af10175c9828fd31e39083d3c0812776`
- کامیت روی GitHub پس از `git am`: `9e29baf9d...` (محتوای درخت یکسان،
  فقط SHA به‌دلیل متفاوت بودن timestamp/committer بازمحاسبه شد)
- CI run: `#136` روی commit `9e29baf9d`
- درخت V45.6 معادل درخت V45.4.2 (که CI قبلاً در run #134 سبز کرده بود)
  است؛ تنها والد آن V45.5 است که خودش revert همان تغییرست بوده.


---

## ۹۶) V45.7 — تزریق کتابخانهٔ کامل V34 (کتب درسی / نماد و تزئین / زیست و دانشگاه) به WebView

### علت

پس از انتشار V45.6، کاربر گزارش داد که در دیالوگ ویرایشگر فرمولِ APK،
سه گروه «📚 کتاب درسی ریاضی»، «✏ نماد و تزئین» و «🧬 زیست و دانشگاه»
وجود ندارند. بررسی سورس `66.html` نشان داد این سه گروه و حدود ۶۰ دستهٔ
درسی تکمیلی، بخشی از رشتهٔ `MATH_EDITOR_HTML` نیستند؛ بلکه توسط تابع
**میزبان** صفحه به‌نام `installLibV34(w)` پس از بارگذاری iframe به
کتابخانه افزوده می‌شدند (خطوط ۱۱۲۵۹ تا ۱۱۴۰۴ فایل `66.html`). چون در
حالت standalone هیچ میزبانی این تابع را صدا نمی‌زد، `MB_PAD` فقط شامل
دسته‌های پایه بود (۷ تب اولیه + ۳۲ تب curricular + ۱۰ فیزیک/شیمی + ...).

### ریشهٔ فنی

```text
در 66.html:
  <iframe id="mathFrame" srcdoc="...MATH_EDITOR_HTML..."></iframe>
  سپس تابع میزبان installBridge():
    installLibV34(contentWindow);
  که داخل iframe اجرا می‌شود و:
    - 64 پد v34-* را به MB_PAD.push می‌کند (مجموعهٔ کامل ۱۵۱ تب)
    - به ۸ گروه موجود، دسته‌های تکمیلی را می‌افزاید
    - 3 گروه جدید می‌سازد: school / type / bio
    - سه تراشه با data-v34 به .mb-chip-scroll اضافه می‌کند
    - توابع M_FUNS/MB_SYM_TEX را برای نمادهای جدید بسط می‌دهد
    - mbInsertTemplate را با replace‌های یونیکد wrap می‌کند
```

در standalone، asset `math_editor_standalone.html` بایت‌به‌بایت همان
چیزی بود که در V45.4 اضافه شد و حاوی کتابخانهٔ پایه بود، اما بدنهٔ
`installLibV34` در آن وجود نداشت.

### تغییرات

```text
افزوده:
  app/src/main/assets/formula/install_lib_v34.js
    - بدنهٔ کامل تابع installLibV34 (w) استخراج‌شده از 66.html
      خطوط ۱۱۲۵۹ تا ۱۱۴۰۴ (بدون هیچ تغییری)؛ ۳۷٬۹۳۵ کاراکتر،
      64 پد v34-*، گارد __libV34 برای idempotence،
      node --check → SYNTAX OK.

تغییر:
  app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt
    - تابع readInstallLibV34() با کش‌گذاری یک‌باره (cachedInstallLibV34)
      فایل asset را با UTF-8 می‌خواند.
    - در bootstrapScript (همان IIFE که قبل از openMath اجرا می‌شود):
        * متن installLibV34 در بالای بلوک قرار می‌گیرد (تعریف تابع در
          scope جهانیِ eval، یعنی همان lexical scope که MB_PAD و MB_GROUPS
          به‌صورت const سطح‌بالا در آن تعریف شده‌اند)
        * installLibV34(window) بلافاصله صدا زده می‌شود
        * سپس همان منطق قبلی (m.value، setSelectionRange، wrap
          mfApply/closeMath و openMath('qTxt_1')) اجرا می‌گردد.
    - asset بایت‌به‌بایت دست‌نخورده باقی ماند (همان قول V45.4)؛
      تمام محتوای V34 از asset جداگانه تزریق می‌شود.

  app/src/test/java/ir/exam/app/ui/app/V19InteractionTest.kt
    - تست جدید: formula editor bootstraps the V34 curricular library
      school/type/bio
      * تأیید می‌کند asset وجود دارد و با «function installLibV34(w)» شروع می‌شود
      * وجود school/type/bio و شناسه‌های کلیدی v34-math10, v34-hesaban1,
        v34-discrete, v34-accents, v34-arrows, v34-special-let,
        v34-bio, v34-uni, v34-stats, v34-prob
      * تأیید می‌کند installLibV34(window) در دیالوگ قبل از
        window.openMath('qTxt_1') اجرا می‌شود
      * گارد __libV34 برای جلوگیری از تزریق دوباره

  scripts/verify_native_final.py
    - بخش V45.7: بررسی وجود و اندازهٔ asset (>30KB)، شروع با
      «function installLibV34(w)»، وجود __libV34 و ۱۱ نشانهٔ کلیدی،
      و این که installLibV34(window) در دیالوگ جلوتر از
      openMath('qTxt_1') آمده باشد.
```

### اعتبارسنجی

```text
node --check install_lib_v34.js                → SYNTAX OK
python3 scripts/verify_native_final.py         → FINAL_NATIVE_VERIFY=PASS
git diff --check                               → PASS
jsdom end-to-end (با تقلید از Android evaluateJavascript):
  - MB_GROUPS: 11 گروه (۸ اصلی + school + type + bio)
  - MB_PAD: 151 تب
  - .mb-chip-scroll: 15 تراشه (۱۲ قبلی + ۳ جدید با[data-v34])
  - school → 10 زیرشاخه (ریاضی دهم/یازدهم/دوازدهم، حسابان ۱/۲، هندسه ۱/۲/۳،
    گسسته، آمار و احتمال)
  - type   → 6 زیرشاخه (تزئینات، پیکان‌ها، حروف خاص، کروشه، عملگرها،
    یونانی کامل)
  - bio    → 4 زیرشاخه (زیست‌شناسی، دانشگاه، آمار تکمیلی، احتمال تکمیلی)
  - v34-math10 → 18 آیتم
  - ۰ خطای JS
```

### مسیرهای کاربر پس از این پچ

- باز کردن دیالوگ ویرایش فرمول → نوار تراشه‌ها با کشیدن افقی به انتها
  می‌رسد به «📚 کتاب درسی ریاضی»، «✏ نماد و تزئین»، «🧬 زیست و دانشگاه».
- کلیک روی هرکدام → منوی شناور زیرشاخه‌ها → انتخاب دسته → پنل مرکزی
  قالب‌ها → لمس قالب → درج در جعبه.
- جستجو از طریق «🔍 جست‌وجوی نماد یا نام فارسی» شامل همهٔ ۱۵۱ تب می‌شود
  (چون `mbSearchSymbols` روی همهٔ `MB_PAD` می‌گردد).

### SQL / Secret / وابستگی

ندارد. هیچ کلیدی وارد/خارج نشد، `applicationId`/keystore دست‌نخورده،
هیچ وابستگی Gradle جدیدی اضافه نشد. asset جدید فقط متن/داده است (۴۵ کیلوبایت).

### فایل پچ

```text
/home/user/V45_7_v34_formula_library_injection.patch   (پس از commit ساخته می‌شود)
```

پیش‌نیاز: V45.6 (درخت نهایی شامل WebView ویرایشگر).

---

## ۹۷) V45.7.2 — رفع کامپایل K2 با انتقال خواندن asset به Context سطح Composable

### علت

پس از push پچ V45.7، CI روی `compileDebugKotlin` با این خطا شکست خورد:

```text
e: MathEditorWebViewDialog.kt:318:13 Functions which invoke @Composable
   functions must be marked with the @Composable annotation
e: MathEditorWebViewDialog.kt:319:18 @Composable invocations can only happen
   from the context of a @Composable function
e: MathEditorWebViewDialog.kt:319:18 Function invocation
   'MathEditorWebViewDialog(...)' expected.
```

هات‌فیکس V45.7.1 بدنهٔ تابع را از expression-body با `?: run { ... }` به
بدنهٔ بلوکی با if/return تغییر داد ولی خطا روی خطوط جدید (۳۲۲ و ۳۲۷) تکرار
شد. نتیجه: مشکل از `run` یا `elvis` نبود؛ کامپایلر K2 در این نسخه هر بار
که در یک تابع غیر-@Composable به `MathEditorWebViewDialog::class.java`
ارجاع داده می‌شد، آن ارجاع را به‌عنوان فراخوانی تابع composable تعبیر
می‌کرد.

### اصلاح

```text
- حذف کامل readInstallLibV34 و cachedInstallLibV34 از MathEditorWebViewDialog
- فایل جدید: app/src/main/java/ir/exam/app/ui/math/FormulaV34Library.kt
    object FormulaV34Library {
      fun load(context: Context): String   // assets.open(...).readBytes()
    }
  این object هیچ ارجاعی به MathEditorWebViewDialog ندارد.
- در MathEditorWebViewDialog (سطح @Composable):
    val appContext = LocalContext.current.applicationContext
    val v34Source = remember { FormulaV34Library.load(appContext) }
    val bootstrap = remember(initialTex, v34Source) {
      bootstrapScript(initialTex, v34Source)
    }
  این کد داخل بدنه composable اجرا می‌شود و K2 روی آن اعتراضی ندارد.
- bootstrapScript اکنون یک پارامتر v34Source می‌گیرد و به‌جای فراخوانی
  هر بار installLibV34، با گارد window.__mbV34Installed بدنه را فقط یک‌بار
  در هر بار بارگذاری صفحه eval می‌کند.
- تست V19 و verify_native_final.py به‌روزرسانی شدند:
    * وجود FormulaV34Library.load(context: Context)
    * مسیر asset در object (نه در دیالوگ)
    * استفاده از LocalContext.current در دیالوگ
    * ترتیب installLibV34(window) قبل از window.openMath('qTxt_1')
    * وجود گارد __mbV34Installed
```

### اعتبارسنجی

```text
python3 scripts/verify_native_final.py
  → FINAL_NATIVE_VERIFY=PASS kotlin_files=169 edge_functions=3
git diff --check                       → PASS
jsdom e2e (همان مدل evaluateJavascript):
  groups=11 pads=151 chips=15 v34chips=3
  بدون خطای JS
```

### نکتهٔ معماری

از این به بعد هر asset یا منبعی که در @Composable لازم است باید با
`LocalContext.current` یا از طریق یک object مستقل خوانده شود و هرگز نام
همان تابع composable درون بدنهٔ یک تابع کمکی غیر-composable ظاهر نشود
(محدودیت K2 در این نسخه).

---

## ۹۸) V45.7.3 — fallback 100vh برای 100dvh در WebViewهای قدیمی

### علت

پس از نصب APK حاوی V45.7.2، کاربر گزارش داد:
- «پنجرهٔ فرمول چیزی رو نشون نمیده»
- «پنجرهٔ ایجاد آزمون بهم ریخته»

بررسی jsdom نشان داد منطق و داده‌های V45.7.2 درست کار می‌کنند (مدال باز
می‌شود، canvas رندر می‌شود، ۱۵۱ تب و ۱۵ تراشه در دسترس‌اند). علت محتمل در
خود فایل asset بود:

```css
#mfModal.box-fullscreen .mf-box {
  height: 100dvh;      /* Chrome/WebView < 108 نمی‌فهمد */
}
```

در WebViewهای قدیمی (پیش از Chrome 108، اواخر ۲۰۲۲) واحد `dvh` ناشناخته
است و چون هیچ `height` پیش‌فرض دیگری برای همان قانون نوشته نشده بود، مقدار
به `auto` می‌افتاد و جعبه می‌توانست ارتفاع صفر بگیرد. پیامد همان «چیزی
دیده نمی‌شود» بود.

### اصلاح

ثابت جدید `VIEWPORT_FALLBACK_JS` در `MathEditorWebViewDialog.kt` در
ابتدای bootstrap پیش از هر چیز یک `<style>` به `<head>` اضافه می‌کند:

```javascript
#mfModal.box-fullscreen .mf-box{
  height:100vh !important; max-height:100vh !important;
  height:100dvh !important; max-height:100dvh !important;
}
#mfModal.box-fullscreen #mfPad.mb-library-open .mb-library-panel{...}
#mfModal.box-fullscreen #mfPad.mb-smart-hub .mb-smart-shell{...}
```

در مرورگرهای جدید، `100dvh` دوم `100vh` را بازنویسی می‌کند (همان رفتار
اصلی)؛ در مرورگرهای قدیمی، `100dvh` ناشناخته رد می‌شود و `100vh` کار
می‌کند. asset اصلی بایت‌به‌بایت دست‌نخورده ماند.

### فایل‌ها

```text
MathEditorWebViewDialog.kt
  - افزودن ثابت VIEWPORT_FALLBACK_JS
  - bootstrapScript ابتدا این قطعه را پیش از بدنهٔ اصلی برمی‌گرداند
V19InteractionTest.kt
  - assertion برای وجود VIEWPORT_FALLBACK_JS و هر دو واحد 100vh/100dvh
verify_native_final.py
  - بررسی قراردادی V45.7.3
FormulaV34Library.kt
  - اصلاح یک typo در KDoc (ُclazz → clazz)؛ بدون تغییر کد
```

### اعتبارسنجی

```text
python3 scripts/verify_native_final.py
  → FINAL_NATIVE_VERIFY=PASS kotlin_files=169 edge_functions=3
git diff --check → PASS
jsdom: مدال باز می‌شود، 151 تب، 15 تراشه، canvas دارای محتوا
```

### یادآوری برای «پنجرهٔ ایجاد آزمون به‌هم ریخته»

از V45.6 تا V45.7.3 هیچ تغییری در چیدمان `ExamBuilderScreen.kt` داده نشده
به‌جز خط `import` و فراخوانی `MathEditorWebViewDialog`. این فایل در V45.6
در CI و آزمون‌های دستی سالم بود. اگر در V45.7.1 یا V45.7 چیدمان به‌هم
ریخته، محتمل‌ترین علت نصب یک APK از کامیت‌های شکست‌خورده است که فایل‌ها را
نیمه‌کاره داشته‌اند. پس از نصب APK سبز V45.7.2/V45.7.3 اگر مشکل باقی بود،
یک اسکرین‌شات یا logcat از همان لحظه لازم است (اصل اول: بدون خطای واقعی
حدس نمی‌زنیم).

---

## ۹۹) V45.7.4 — بستن ایمن دیالوگ و تثبیت پین والد مدال

### علت

پس از V45.7.3 کاربر گزارش داد:
- «پس از کلیک روی آیکن فرمول یک صفحه خالی نمایش داده می‌شود»
- «با برگشتن، پنجرهٔ ایجاد آزمون بهم می‌ریزد»

بررسی مسیر `onDismissRequest` نشان داد دو نقطه‌ضعف داشت:

1. بعد از `evaluateJavascript(CLOSE_MATH_JS, ...)`، بستن دیالوگ کاملاً
   وابسته به این بود که `closeMath()` در JS پل `onClosed` را صدا بزند.
   اگر در WebView قدیمی یک استثنا در مسیر `closeMath` می‌افتاد یا پل
   تحویل نمی‌شد، دیالوگ باز می‌ماند و Compose زیر آن در یک حالت
   نیمه‌مخرب رندر می‌شد (همان «بهم ریختن»).
2. والد `#mfModal` در CSS اصلی فقط با `inset:0` پین شده بود که در
   WebViewهای بسیار قدیمی ممکن است ناشناخته باشد؛ جعبه با `height:100dvh`
   بود ولی والد می‌توانست ارتفاع کامل نگیرد.

### اصلاح

- `dismissOnce.fire()` با `AtomicBoolean` تضمین می‌کند `onDismiss` فقط
  یک‌بار و حتماً اجرا شود (پل یا تایمر، هرکدام اول شد).
- در `onDismissRequest` پس از ارسال `CLOSE_MATH_JS`، یک `postDelayed`
  با `DISMISS_FALLBACK_MS = 250ms` دیالوگ را در صورت سکوت پل می‌بندد.
  در حالت طبیعی پل خیلی زودتر آتش می‌شود و `AtomicBoolean` جلو تکرار
  را می‌گیرد.
- `mainHandler.removeCallbacksAndMessages(null)` در `onDispose` اضافه
  شد تا پیام معلق به یک هندلر/کانتکست ازبین‌رفته نرسد.
- رنگ پس‌زمینهٔ WebView با تم تیره ویرایشگر (0xFF0F0C29) هماهنگ شد تا
  اگر لحظه‌ای بین لود HTML و آماده شدن دیده شد، فلاش سفید نزند.
- CSS تزریقی گسترش یافت:
  - `#mfModal{top:0;right:0;bottom:0;left:0;height:100vh;height:100dvh}`
  - `#mfModal.box-fullscreen #mfP_box{height:100%!important}`
  - `body.math-open .demo-wrap{display:none!important}` تا محتوای دموی
    پشت جعبه دیده نشود.

### فایل‌ها

```text
MathEditorWebViewDialog.kt
  - dismissOnce با AtomicBoolean
  - postDelayed به‌عنوان fallback بستن
  - removeCallbacksAndMessages در onDispose
  - پس‌زمینهٔ تیرهٔ WebView
  - گسترش VIEWPORT_FALLBACK_JS
V19InteractionTest.kt
  - assertion برای والد پین‌شده، پنهان‌سازی demo-wrap و fallback بستن
```

### اعتبارسنجی

```text
python3 scripts/verify_native_final.py
  → FINAL_NATIVE_VERIFY=PASS kotlin_files=169 edge_functions=3
git diff --check → PASS
بریس/پرانتز MathEditorWebViewDialog.kt: 99/99 و 189/189
```

### توصیه برای تست دستی

1. روی دستگاه/شبیه‌ساز با WebView قدیمی (مثلاً Android 8/9) پنجرهٔ فرمول
   را باز کنید — باید canvas فرمول و نوار تراشه‌ها دیده شوند.
2. دکمهٔ بازگشت دستگاه را بزنید — باید فوراً به پنجرهٔ ایجاد آزمون
   برگردید و چیدمان سالم باشد.
3. روی ثبت فرمول بزنید — باید متن `$...$` در فیلد درج شود و پنجره بسته
   شود.
4. اگر هنوز صفحه خالی است، logcat با فیلد `chromium|System.err|ConsoleMessage`
   را بفرستید؛ بدون خطای واقعی حدس اضافه نمی‌زنیم.

---

## ۱۰۰) V45.7.5 — تضمین باز شدن مدال و لاگ‌گیری از خطاهای WebView

### علت

پس از V45.7.4 کاربر اسکرین‌شاتی فرستاد که نشان می‌داد به‌جای ویرایشگر،
**صفحهٔ دمای خود فایل HTML** (سؤال ۱، گزینه ۱ و ۲، سؤال ۲) نمایش داده
می‌شود و در textareaها متن خام TeX (مثل `حاصل عبارت ...`) بدون رندر
دیده می‌شود. این یعنی:

1. فایل asset لود شده (صفحه دیده می‌شود).
2. اما `openMath('qTxt_1')` به‌نحوی مدال را باز نکرده است: نه کلاس
   `open` روی `#mfModal` و نه `box-fullscreen`.
3. رویداد `load` صفحه هم احتمالاً یا قبل از فراخوانی آمادگی ما آتش نکرده
   یا اثرش به جایی نرسیده که `qMathSync` را برای فیلدهای دمو اجرا کند.

در V45.7.3/4 ما فقط ارتفاع جعبه را اصلاح کرده بودیم ولی اگر
`openMath` در همان ابتدا با یک استثنا متوقف شود (مثلاً تفاوت رفتار
در WebView قدیمی یا یکی از توابع وابسته)، هیچ fallbackای برای باز
کردن مستقیم مدال وجود نداشت.

### اصلاح

- **پل تشخیصی**: تابع `AndroidMathBridge.log(msg)` اضافه شد و یک
  `WebChromeClient` کنسول JS را به logcat (تگ `MathEditorWebView`)
  هدایت می‌کند. حالا اگر خطایی در صفحه رخ دهد در logcat دیده می‌شود.
- **باز کردن ایمن مدال**: پس از `openMath('qTxt_1')`، یک بلوک
  جداگانه با try/catch خودش کلاس‌های `modal open box-fullscreen`
  را روی `#mfModal` می‌گذارد و `display:flex` را ست می‌کند و
  `body.math-open` را اضافه می‌کند؛ سپس `mbDraw` و در صورت
  خالی بودن `#mfTabs` یک‌بار `buildMathPad(MB_PAD_ACTIVE)` را
  صدا می‌زند. اگر `openMath` درست کار کرده باشد این فراخوانی‌ها
  بی‌اثر هستند؛ اگر نکرده باشند، کاربر ویرایشگر را می‌بیند.
- ایزوله کردن کامل خطای V34: بدنهٔ `installLibV34` درون try/catch
  خودش قرار گرفت تا اگر در WebView خاصی استثنا داد، مانع باز شدن
  ویرایشگر نشود.
- `initMathEdit()` صریحاً پیش از openMath صدا زده می‌شود تا پد تب‌دار
  حتی اگر رویداد load هنوز اجرا نشده باشد، ساخته شود.

### فایل‌ها

```text
MathEditorWebViewDialog.kt
  - افزودن WebChromeClient برای هدایت console.* به logcat
  - متد جدید @JavascriptInterface log روی پل
  - initMathEdit پیش از openMath
  - try/catch مجزا برای V34
  - force-open مدال با کلاس‌ها و buildMathPad
V19InteractionTest.kt
  - assertion برای AndroidMathBridge.log، force modal classes و
    WebChromeClient
```

### اعتبارسنجی

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=169 edge_functions=3
git diff --check → PASS
بریس/پرانتز MathEditorWebViewDialog.kt: 126/126 و 242/242
```

### اگر هنوز صفحه خالی/دما دیده شد

از کاربر logcat خواسته می‌شود:

```bash
adb logcat -c
# باز کردن دیالوگ فرمول
adb logcat -d | grep -iE "MathEditorWebView|chromium|console" > log.txt
```

خط‌های `JS[ERROR]` یا `bootstrap ...` دقیقاً محل خطا را نشان می‌دهند و
بدون حدس می‌توان علت را رفع کرد.

---

## ۱۰۱) V45.8 — جایگزینی asset با نسخهٔ تک‌فایلی formula-editor-window

### زمینه

پس از چندین تلاش (V45.7 تا V45.7.5) برای رندر درست پنجرهٔ فرمول روی
برخی WebViewها، کاربر یک فایل جدید به نام `formula-editor-window.html`
ارائه داد که «نسخهٔ پنجره‌ای» ویرایشگر است. این نسخه:
- هستهٔ ویرایشگر و CSS را دارد
- کتابخانهٔ V34 (`installLibV34` و `installLibV34Direct`) را به‌صورت
  درون‌ساخت داخل خودش دارد
- یک `<script id="host-bridge">` دارد که همهٔ وصله‌های رفتاری میزبان
  را اعمال می‌کند (wrap پنجره، keypad، autoGrow، theme، V34 و …)
- یک `<script id="auto-open">` در انتهای فایل بود که در نسخهٔ وب
  باعث می‌شد با باز شدن فایل پنجرهٔ ویرایشگر باز شود و اگر بسته شد
  دوباره باز شود — این رفتار برای اندروید مزاحم است و حذف شد
- یک اسکریپت چالش Cloudflare به انتهای فایل چسبیده بود که هنگام دانلود
  با مرورگر به HTML تزریق شده بود؛ حذف شد

### تغییرات

1. **جایگزینی asset**: `app/src/main/assets/math_editor_standalone.html`
   با فایل جدید (پس از حذف auto-open و Cloudflare) بازنویسی شد.
2. **MathEditorWebViewDialog ساده شد**:
   - ارجاع به `FormulaV34Library` و `LocalContext` برای لود asset
     حذف شد (V34 داخل خود فایل است).
   - تزریق بدنه `install_lib_v34.js` و گارد `__mbV34Installed` حذف شد.
   - فقط seed متن، wrap `mfApply`/`closeMath`، فراخوانی
     `openMath('qTxt_1')` و force-open کلاس‌ها باقی ماند.
   - `VIEWPORT_FALLBACK_JS`، `dismissOnce`، تایمر
     `DISMISS_FALLBACK_MS` و پل تشخیصی `AndroidMathBridge.log` حفظ
     شدند (همان بهسازی‌های V45.7.3/4/5).
3. **فایل‌های حذف‌شده**:
   - `app/src/main/java/ir/exam/app/ui/math/FormulaV34Library.kt`
   - پوشهٔ `app/src/main/assets/formula/` (شامل install_lib_v34.js)
4. **آزمون‌ها و تأییدها به‌روزرسانی شدند**:
   - V19InteractionTest حالا بررسی می‌کند که asset تک‌فایلی شامل
     installLibV34، host-bridge، توکن‌های V34 باشد و auto-open و
     Cloudflare در آن نباشد؛ فایل‌های قدیمی حذف شده باشند.
   - verify_native_final.py به‌جای چک کردن فایل جدا، درون asset
     جدید دنبال installLibV34 می‌گردد و حذف فایل‌های قدیمی را هم
     تأیید می‌کند.

### اعتبارسنجی

```text
python3 scripts/verify_native_final.py
  → FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
node --check روی هر سه <script> جداگانه در asset → OK
jsdom (host-bridge فعال):
  openMath/closeMath/mfApply = function
  installLibV34 = function
  hostBuildTag: v36 · V34: ✓ 64
  پس از openMath: mfModal className = modal open box-fullscreen
  canvas children = 1
  خطا = ۰
```

### یادداشت‌های سازگاری

- فایل جدید از `100dvh` استفاده می‌کند (سه مورد)؛ `VIEWPORT_FALLBACK_JS`
  همچنان `100vh` را قبل از آن تزریق می‌کند، پس WebViewهای قدیمی
  سازگار می‌مانند.
- برخلاف نسخهٔ قبلی، در این فایل V34 به‌صورت خودکار توسط host-bridge
  با setInterval تا ۲۰ بار تلاش (هر ۱۲۰ms) نصب می‌شود؛ حتی اگر
  آماده‌سازی صفحه دیر انجام شود، در نهایت وصل می‌شود.
- چون auto-open حذف شد، باز کردن پنجره کاملاً توسط پل اندروید کنترل
  می‌شود (یک‌بار openMath در bootstrap) و دکمهٔ بازگشت به‌درستی به
  پنجرهٔ ایجاد آزمون برمی‌گردد.

---

## ۱۰۲) V45.8.2 — فیکس چیدمان با ابعاد پیکسلی واقعی

### علت

پس از V45.8 کاربر گزارش داد روی شبیه‌ساز با زدن آیکن فرمول، صفحهٔ
تمام‌تیره‌ای ظاهر می‌شود که فقط برچسب `v36 · V34: ✓ 64` در پایین آن
دیده می‌شود. این برچسب توسط host-bridge به `document.body` افزوده
می‌شود و دیدنش یعنی JS اجرا شده؛ اما خود مدال و فرزندانش رندر
نمی‌شوند.

محتمل‌ترین علت: تکیهٔ CSS به `100dvh` (و قبل‌تر `inset:0`) در
برخی WebViewهای قدیمی یا حالت‌های خاص شبیه‌ساز، یا رفتار grid با
`minmax(0,1fr)` در یک والد با ارتفاع درصدی/غیرصریح. تزریق قبلی فقط
یک `height:100vh` قبل از `100dvh` می‌گذاشت ولی اگر WebView حتی
`100vh` را در زنجیرهٔ والد/فرزند به‌درستی منتشر نکند، نتیجه صفر
می‌شود.

### اصلاح

۱. **CSS تزریقی قوی‌تر** در `VIEWPORT_FALLBACK_JS`:
   - مدال با `position:fixed` و چهارضلع صفر و عرض/ارتفاع ۱۰۰٪
   - `.mf-box` با `position:absolute` و چهارضلع صفر
   - `#mfP_box` با `position:absolute`، چهارضلع صفر، و
     `display:flex; flex-direction:column` (جایگزین grid)
   - `.mb-wrap` با `flex:1 1 auto` و `min-height:0`
   - `.mb-canvas` با `flex:1 1 auto`
   - `.mb-chip-scroll` و `.mb-key-section` و `.mb-fixed-keypad`
     با `flex:0 0 auto` و display صریح
   - همه با `!important` تا قوانین `100dvh` asset را خنثی کنند.

۲. **فیکس پیکسلی با JS** در تابع جدید `window.__mbForceLayout`:
   - ارتفاع و عرض واقعی viewport را با `innerHeight/clientHeight`
     می‌خواند و همان مقدار پیکسلی را به‌صورت inline روی mfModal،
     mf-box و mfP_box می‌گذارد.
   - بلافاصله پس از openMath و در ۱۰۰ms و ۴۰۰ms دوباره اجرا
     می‌شود تا اگر چیدمان در ابتدا صفر بود، در نهایت اعمال شود.
   - یک `mbDraw()` نیز صدا می‌زند تا اگر رندر اولیه به‌خاطر ابعاد
     صفر، خروجی خالی داده بود دوباره رسم کند.

این اصلاح به‌عنوان تضمین دوم پشت سر CSSِ تزریقی عمل می‌کند؛ در
مرورگرهای جدید قوانین `!important` حرف اول را می‌زنند و در
WebViewهای مشکل‌دار، JS پیکسل‌های صریح را اعمال می‌کند.

### اعتبارسنجی

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
jsdom با همان CSS/JS تزریقی:
  #mfModal        display=flex position=fixed   100%
  .mf-box         display=block position=absolute 100%
  #mfP_box        display=flex position=absolute 100%
  .mb-wrap        display=flex  flex=1 1 auto
  .mb-canvas      display=flex  flex=1 1 auto  (canvas populated)
  .mb-chip-scroll display=grid  flex=0 0 auto
  .mb-key-section display=flex  flex=0 0 auto
```

---

## ۱۰۳) V45.8.3 — رفع باز نشدن کتابخانه‌ها/پنل‌های فرمول

### علت

پس از تثبیت چیدمان اصلی فرمول، کاربر گزارش داد «کتابخانه‌ها کار نمی‌کنند».
بررسی CSS نشان داد در فیکس V45.8.2 این قانون تزریقی وجود داشت:

```css
#mfModal.box-fullscreen #mfPad { display:none !important; }
```

در حالی که خود asset برای کتابخانه و اسمارت‌هاب از همین عنصر `#mfPad`
با کلاس‌های `mb-library-open` و `mb-smart-hub` استفاده می‌کند و قانون
اصلی asset می‌خواست آن‌ها را با `display:flex !important` نشان دهد.
به‌خاطر یکسان بودن specificity، ترتیب منبع باعث می‌شد قانون مخفی‌کنندهٔ
ما برنده شود و پنل کتابخانه هرگز نمایش داده نشود.

همچنین پنل‌های کتابخانه از `position:fixed; inset:0` استفاده می‌کردند
که در WebView قدیمی ممکن بود مانند مشکل قبلی ارتفاع صفر بگیرد.

### اصلاح

- قانون مخفی‌سازی فقط وقتی `#mfPad` را پنهان می‌کند که نه
  `mb-library-open` و نه `mb-smart-hub` داشته باشد:
  `#mfPad:not(.mb-library-open):not(.mb-smart-hub)`.
- برای حالت‌های باز، مستقیم `display:flex` و موقعیت fixed با
  `top/right/bottom/left:0`، `100vw/100vh`، `z-index:12040` و
  پس‌زمینه نیمه‌شفاف ست شد تا حتی اگر `inset` یا `100dvh` پشتیبانی
  نشود، پنل کل صفحه را بپوشاند.
- پنل داخلی هم `max-height:84vh` قبل از `min(84dvh,...)` گرفت.

### اعتبارسنجی

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
بریس/پرانتز MathEditorWebViewDialog.kt: 133/133 و 232/232
git diff --check → PASS
```

---

## ۱۰۴) V45.8.4 — حذف حالت میانی موبایلی و تثبیت بازگشت

### مشکل

کاربر گزارش داد:
- تصویر ۱: با زدن فرمول، یک نسخهٔ موبایلیِ غیر تمام‌صفحه (bottom-sheet)
  باز می‌شود که دکمه‌های «درج در سؤال/پاک/انصراف» و نوار ابزار بالا
  دیده می‌شود (همان ظاهر موبایل بدون `box-fullscreen`).
- تصویر ۲: با زدن بازگشت، یک نسخهٔ خالی از ویرایشگر در حالت
  تمام‌صفحه دیده می‌شود و به پنجرهٔ ایجاد آزمون برنمی‌گردد.

### علت

1. در لحظهٔ باز شدن، یک فاصلهٔ کوتاه بین لود HTML و اجرای bootstrap
   وجود داشت که طی آن asset با ظاهر پایین‌صفحهٔ موبایل دیده می‌شد.
2. در `onDismissRequest`، ما `closeMath` را اجرا می‌کردیم و سپس با
   `postDelayed(250ms)` منتظر پل می‌ماندیم. اجرای `closeMath` کلاس
   `box-fullscreen` را برمی‌داشت و در همان ۲۵۰ms دیالوگ Compose هنوز
   باز بود؛ نتیجه یک حالت میانی با پس‌زمینهٔ خالی بود.
3. قانون `.modal.open .modal-box { will-change: transform }` یک
   استاکینگ کانتکست می‌ساخت که می‌توانست روی `position:fixed` کتابخانه‌ها
   اثر بگذارد (در حالت تمام‌صفحه اصلاً نباید transform وجود داشته باشد).

### اصلاح

- **حذف تأخیر بستن**: `onDismissRequest` حالا بلافاصله
  `dismissOnce.fire()` را صدا می‌زند؛ closeMath فقط به‌صورت best-effort
  برای پاکسازی حالت JS ارسال می‌شود و Compose فوراً بسته می‌شود.
- **سرکوب closeMath در طول بسته شدن**: پرچم `__mbSuppressClose` باعث
  می‌شود wrapper ما از فراخوانی inner `closeMath` (که کلاس‌ها را حذف
  می‌کرد) صرف‌نظر کند.
- **قفل کلاس با MutationObserver**: بعد از openMath یک
  `MutationObserver` کلاس‌های `open box-fullscreen` و `math-open` را
  در صورت حذف، مجدداً برمی‌گرداند تا هیچ‌گاه به حالت میانی سقوط
  نکنیم.
- **حذف transform**: در CSS تزریقی برای `.mf-box` و `#mfModal` در حالت
  تمام‌صفحه، `transform:none !important; will-change:auto !important;`
  اضافه شد تا استاکینگ کانتکست از بین برود و fixed-position
  کتابخانه‌ها نسبت به viewport عمل کند.
- اعمال کلاس‌های fullscreen **پیش از** فراخوانی `openMath` تا حتی
  قبل از اجرای آن، ظاهر درست باشد.

### اعتبارسنجی

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
بریس/پرانتز MathEditorWebViewDialog.kt: 140/140 و 251/251
git diff --check → PASS
```

---

## ۱۰۵) V45.8.5 — رفع کار نکردن کتابخانه‌ها (پاپ‌آپ و چیپ‌های V34)

### مشکل

با زدن چیپ‌های کتابخانه (مثل «📚 کتاب درسی ریاضی»، «✏ نماد و تزئین»،
«🧬 زیست و دانشگاه»، یا دسته‌های ۸گانه) هیچ پاپ‌آپی باز نمی‌شد.

### علت‌ها (دو علت جدا)

1. **چیپ‌های V34 به‌صورت event-listener و با `w.eval` در
   host-bridge بایند می‌شدند.** در WebView اندروید `w === window`
   است ولی `eval` در یک اسکوپ غیرمستقیم کد را اجرا می‌کند و در
   برخی ترکیب‌ها `const`/`let` سطح ماژول را روی `window`
   نمی‌بیند؛ در نتیجه ارجاع به `MB_GROUPS` ممکن بود با
   ReferenceError شکست بخورد و click بی‌اثر شود.
2. **موقعیت‌دهی `mbVarOpen` به offsetWidth/offsetHeight در لحظهٔ
   نمایش وابسته بود.** این مقدار در برخی WebViewها در همان تیک
   صفر بود (محتوا هنوز رنگ نشده بود) و منو با `left=6, top=6`
   بیرون دید کاربر قرار می‌گرفت.

### اصلاح

- **CSS تزریقی قوی برای `#mbVar`:**
  `position:fixed; top:50%; left:50%; transform:translate(-50%,-50%)`
  با `z-index:2147483646` تا هر جور مقدار inline از توابع
  داخلی را خنثی کند و منو حتماً وسط صفحه و روی مدال دیده شود.
- **wrapper روی `mbVarOpen`:** هر بار که منویی باز می‌شود، در
  ۰، ۳۰ و ۱۲۰ms مرکزسازی دوباره اجرا می‌شود (هم برای پاپ‌آپ
  اولیه و هم برای انتخاب‌های بعدی که به `mbOpenItemLibrary` با
  کلاس `mb-library-open` می‌رسند).
- **بایند مستقیم چیپ‌های V34 بدون eval:** سه چیپ
  `[data-v34="1"]` پیدا و روی آن‌ها یک listener در فاز capture
  اضافه می‌شود که مستقیماً
  `window.mbGroupLibrary('school'|'type'|'bio')` را صدا
  می‌زند؛ این نسخه از تابع با آرگومان رشته‌ای کار می‌کند و
  وابسته به eval نیست. در ۰ و ۲۰۰ و ۶۰۰ms برای اطمینان از
  ساخته شدن چیپ‌ها توسط host-bride دوباره بایند می‌شود.
- MutationObserver روی `#mbVar` موقعیت و کلاس را در طول عمر
  دیالوگ تثبیت می‌کند.

### اعتبارسنجی (jsdom)

```text
پس از کلیک روی «📚 کتاب درسی ریاضی»:
  mbVar: display=block position=fixed
         transform=translate(-50%,-50%) width=360px
  اولین دسته: 📘 ریاضی دهم
پس از کلیک روی دسته:
  mfPad className = 'mf-pad mb-library-open'
  mfPad display  = flex  position = fixed
  mfPad children = 1
```

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
بریس/پرانتز MathEditorWebViewDialog.kt: 160/160 و 308/308
```


---

## ۱۰۶) V45.8.6 — واگذاری چیدمان به CSS بومی asset (رفع صفحهٔ تیره)

### مشکل

پس از V45.8.5، روی دستگاه ویرایشگر به‌صورت صفحه‌ای کاملاً تیره با فقط
بَج نسخهٔ `v36 · V34: ✓ 64` در گوشه پایین‌چپ باز می‌شد. بدنهٔ ویرایشگر
(بوم فرمول، ریل تراشه‌ها و کیپد) رندر نمی‌شد.

### علت ریشه‌ای

در asset، `#mfP_box` در حالت تمام‌صفحه با گرید سه‌ردیفه‌ای چیده
می‌شود (خط ۶۶۲۸):

```css
#mfModal.box-fullscreen #mfP_box {
  display:grid !important;
  grid-template-rows:minmax(0,1fr) auto auto;
  height:100%; min-height:0; overflow:hidden;
}
```

ردیف‌ها به‌ترتیب `mb-wrap` (بوم)، `mb-chip-scroll` (ریل تراشه‌ها) و
`mb-fixed-keypad` (کیپد) هستند. قواعد تهاجمی V45.8.2..5 روی همین
عنصر مقادیر زیر را با `!important` ست می‌کردند:

- `position:absolute; top:0; left:0; right:0; bottom:0`
- `display:flex !important; flex-direction:column`
- ارتفاع/عرض پیکسلی ثابت از `innerHeight/innerWidth`

این قواعد گرید دارِ سه‌ردیفه را به یک flexbox تک‌محوره تبدیل می‌کرد؛
بچه‌ها در یک flex فاقد `flex:1` و ارتفاع صفر جمع می‌شدند و کل
#mfP_box به یک ناحیهٔ تیرهٔ خالی فرومی‌ریخت (بَج نسخه چون روی body
بود با z-index بالا دیده می‌شد). این رگرسیون در jsdom آشکار نمی‌شد
چون jsdom لِی‌اوت واقعی و ارتفاع محاسبه‌شده را ندارد.

### اصلاحی که انجام شد

حذف تمام تزریق‌های جاوااسکریپتی چیدمان و اعتماد به CSS بومی asset:

1. **حذف `__mbForceLayout`** که هر ۱۰۰/۴۰۰ms روی `#mfModal`,
   `.mf-box`, `#mfP_box` پوزیشن/نمایش پیکسلی absolute و flex
   تحمیل می‌کرد.
2. **حذف `__mbSuppressClose` و قفل MutationObserver** که کلاس‌های
   `box-fullscreen` را روی مدال دوباره می‌چسباند؛ با حذفشان همان
   `closeMath` بومی asset مدال را به حالت اولیه برمی‌گرداند و چون
   Compose هم‌زمان دیالوگ را unmount می‌کند، حالت میانی موبایلی
   فرصت نمایش پیدا نمی‌کند.
3. **ساده‌سازی `VIEWPORT_FALLBACK_JS` به حداقل لازم:**
   - `#mfModal{position:fixed; four-edge:0; display:none; z-index:…}`
     و `#mfModal.modal.open{display:flex}`
   - `#mfModal.box-fullscreen .mf-box{height:100vh; height:100dvh;
     width:100%; margin:0; border:0; border-radius:0; max:0}`
   - پنهان‌سازی `body.math-open .demo-wrap`
   - مرکزصفحه‌کردن `#mbVar.mb-var.open`
   - هیچ قاعده‌ای روی `#mfP_box`, `.mb-wrap`, `.mb-canvas`,
     `.mb-chip-scroll`, `.mb-keypad` یا هر ردیف گرید دیگر وجود
     ندارد — همگی با CSS خود asset رندر می‌شوند.
4. **حذف override سابق روی `#mfPad.mb-library-open`** که با
   `position:fixed; inset:0` با قاعدهٔ بومی asset در خط ۶۶۷۳ تداخل
   می‌کرد؛ asset خودش این پنل را تمام‌صفحه می‌گذارد.
5. بایند مستقیم چیپ‌های V34 و wrapper مرکزکنندهٔ `mbVarOpen` از
   V45.8.5 دست‌نخورده باقی ماند (کارکرد کتابخانه‌ها حفظ شد).

### اعتبارسنجی

- **jsdom (end-to-end، با CSS تزریقی جدید):**
  - پس از `openMath`: `mfModal.className = "modal open box-fullscreen"`.
  - کلیک چیپ «📚 کتاب درسی ریاضی» → `mbVar.open = true` با ۱۰ دسته.
  - کلیک روی «📘 ریاضی دهم» → `mfPad.className = "mf-pad mb-library-open"`
    و یک `.mb-library-panel` به‌عنوان فرزند.
  - `mfApply()` → مقدار `$x^2$` با پل به اندروید رسید.
  - `closeMath()` → کلاس‌های مدال به `modal` و `body` به `""`
    برگشت؛ پل `onClosed` صدا زده شد.
- **بررسی استاتیک CSS:** grep تأیید کرد که دیگر هیچ قاعدهٔ
  `position:absolute` یا `display:flex !important` روی `#mfP_box`
  از سمت اندروید تزریق نمی‌شود و گرید سه‌ردیفهٔ بومی asset
    (خط ۶۶۲۸) حاکم است.
- `python3 scripts/verify_native_final.py` →
  `FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3`.
- کامپایل Kotlin روی این باکس به‌دلیل عدم دسترسی شبکه به مخازن
  KSP انجام‌پذیر نیست؛ اما تست‌ها و اسکریپت‌های پوشش همگی
  پاس‌اند و تغییرات فقط یک بلوک String و دو تابع را در بر
  می‌گیرد. روی سیستم کاربر با `gradlew assembleDebug` کامپایل
  شود.

### درس‌آموخته

برای سومین بار (V45.8.3، V45.8.4 و V45.8.5) همان اشتباه تکرار
شد: **بازنویسی چیدمان به‌جای اعتماد به CSS asset**. asset در
حالت تمام‌صفحه گرید پیچیدهٔ سه‌ردیفه‌ای دارد که باید محترم
شمرده شود. اندروید فقط باید کلاس `box-fullscreen` را روی مدال
بگذارد و مابقی را به asset بسپارد. هر قاعدهٔ `position:absolute`
یا `display:flex !important` روی ردیف‌های این گرید، بچه‌ها را
به ارتفاع صفر می‌برد.

### فایل‌های تغییریافته

- `app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt`
  — حذف `__mbForceLayout`، `__mbSuppressClose`، MutationObserver
  قفل تمام‌صفحه، ساده‌سازی `VIEWPORT_FALLBACK_JS`، حفظ بایند V34
  و wrapper مرکزکننده.
- `app/src/test/java/ir/exam/app/ui/app/V19InteractionTest.kt`
  — به‌روزرسانی اسیارشن‌ها به‌جای بررسی `__mbForceLayout`.
- `scripts/verify_native_final.py` — به‌روزرسانی بررسی‌ها.

---

## ۱۰۷) V45.8.8 — تثبیت ترتیب ثبت و بستن فرمول روی مبنای V45.8.6

### مبنای دقیق

```text
HEAD کاربر: 08bdc09d4bf83d94054b4398e3095a0b8152bfb4
V45.8.6: واگذاری چیدمان به CSS بومی asset
```

پچ قدیمی V45.8.7 برای commit `fd8ebc3` ساخته شده بود و به‌درستی روی این HEAD
اعمال نشد. ممیزی مبنای جدید نشان داد کوتیشن معیوب V45.8.5 قبلاً در V45.8.6
حذف شده و چیدمان تهاجمی نیز بازنشسته شده است؛ بنابراین V45.8.8 فقط اصلاح لازم
و سازگار با همین HEAD را تحویل می‌دهد و هیچ override چیدمان قدیمی را برنمی‌گرداند.

### اصلاح پل ثبت

`mfApply` داخلی asset در پایان `closeMath()` را فراخوانی می‌کند. اکنون:

- `__mbApplyInFlight` پیش از ورود به asset فعال می‌شود؛
- `closeMath` داخلی هنگام ثبت، پیش از اجرای close و `onClosed` بازمی‌گردد؛
- مقدار نهایی سؤال با `AndroidMathBridge.onApplyResult` تحویل می‌شود؛
- پرچم در `finally` حتی هنگام exception آزاد می‌شود؛
- close عادی کاربر همچنان `ic.apply` و سپس `onClosed` را اجرا می‌کند.

این ترتیب مانع می‌شود callback بستن پیش از callback نتیجه، `AtomicBoolean` پل را
settle کند و فرمول درج‌شده از دست برود.

### تست

```text
سه script داخلی asset با node --check            → PASS
bootstrap تزریقی Kotlin با node --check           → PASS
FINAL_NATIVE_VERIFY                               → PASS
V45_8_8FormulaBridgeOrderTest                     → اضافه شد
Patch apply-check روی 08bdc09                     → PASS
```

```text
SQL / Edge / Secret / Migration / Dependency جدید: ندارد
پیش‌نیاز: commit 08bdc09 (V45.8.6)
```

---

## ۱۰۸) V45.8.9 — حذف تم روشن استاتیک و رفع صفحهٔ سفید فرمول روی V45.8.8

### مبنا و خطای واقعی

مبنای الزامی این پچ، HEAD پوش‌شدهٔ کاربر است:

```text
1a4765d9026602a755f3cbca0944773cad1395cb
fix(v45.8.8): stabilize formula apply and close ordering
```

ممیزی مستقیم همین HEAD نشان داد:

```text
ERROR_STATIC_THEME_PRESENT
FORCED_LAYOUT_OK
ERROR_EARLY_STRIP_MISSING
```

هش SHA-256 فایل asset کاربر نیز با asset مبنای V45.8.6 یکسان بود:

```text
bf6b07f8895e23d9b958e908bcc83c207678f5b5042fc50886bd5dd538b99f85
```

بنابراین علت صفحهٔ سفید حدس نیست: داخل asset یک تگ استاتیک
`<style id="hostThemeOverride">` مانده بود که متغیرهای تم دارک را به رنگ‌های
روشن و بوم را به سفید بازنویسی می‌کرد. پچ‌های قدیمی V45.8.7 به‌دلیل ساخته‌شدن
روی تاریخچه‌ای با commit ID متفاوت، روی شاخهٔ واقعی کاربر قابل اعمال نبودند.

### اصلاح V45.8.9

1. بلوک استاتیک ۲۱۷ خطی `hostThemeOverride` از
   `app/src/main/assets/math_editor_standalone.html` حذف شد. API پویای
   `window.__mathHostTheme.on()/.off()` دست‌نخورده ماند.
2. `EARLY_THEME_STRIP_JS` در `onPageStarted` نصب شد تا اگر asset آینده دوباره
   تگ تم روشن را تزریق کرد، MutationObserver آن را بلافاصله حذف کند.
3. پس‌زمینهٔ دارک `var(--bg1)` و رنگ `var(--text)` در fallback حداقلی WebView
   صریح شد، بدون هیچ override روی `#mfP_box` یا سه ردیف گرید asset.
4. اصلاح ترتیب apply/close در V45.8.8 بدون تغییر حفظ شد؛ تست آن نیز باقی است.
5. verifier و تست واحد V45.8.9 نبود تگ استاتیک، نصب محافظ زودهنگام و حفظ
   چیدمان بومی را کنترل می‌کنند.

### اعتبارسنجی انجام‌شده

```text
FINAL_NATIVE_VERIFY=PASS kotlin_files=168 edge_functions=3
ASSET_EXACT_REMOVAL=PASS (فقط همان style و newline بعدش حذف شد)
ASSET_NODE_SYNTAX=PASS (هر سه بلوک script)
KOTLIN_DELIMITER_COUNT=PASS (brace 137/137؛ parenthesis 272/272)
PATCH_WHITESPACE_CHECK=PASS
jsdom: hostThemeOverride=false؛ bg1=#0f0c29؛ text=#fff
jsdom: chips=grid؛ keypad=grid؛ V34 categories=10؛ library panel=true
```

اجرای Gradle در sandbox واقعاً امتحان شد، اما پیش از کامپایل سورس به‌دلیل
resolve نشدن plugin زیر از مخازن بیرونی متوقف شد:

```text
com.google.devtools.ksp:2.0.21-1.0.28 — plugin artifact not found
```

این محدودیت شبکه/مخزن محیط تست است، نه خطای کامپایل گزارش‌شده از فایل‌های
V45.8.9؛ به همین دلیل build نهایی باید در WSL/GitHub Actions کاربر اجرا شود.

### فایل‌های پچ

```text
V45_8_9_white_formula_page_fix_on_1a4765d.patch
V45_8_9_HANDOFF_FA.md
```

### اعمال در WSL

پیش از اعمال، دستور اول باید دقیقاً `1a4765d` را نشان دهد:

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin/exam-app-kotlin
git log --oneline -1
git status --short --branch
git am --whitespace=nowarn /mnt/c/Users/Hashem/Downloads/V45_8_9_white_formula_page_fix_on_1a4765d.patch
git push origin HEAD
```

اگر HEAD متفاوت بود یا `git am` خطا داد، `--continue`، `--skip` یا reset اجرا
نشود؛ ابتدا `git am --abort` و خروجی کامل ارسال شود.

### build و test روی سیستم کاربر

```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin/exam-app-kotlin
python3 scripts/verify_native_final.py
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:assembleDebug
```

پذیرش دستی روی دستگاه:

- آیکن فرمول باید ویرایشگر دارک را بدون فلش/صفحهٔ سفید باز کند؛
- بوم، ریل تراشه‌ها و کیپد هم‌زمان دیده شوند؛
- سه چیپ V34 و کتابخانه‌هایشان باز شوند؛
- «ثبت» ابتدا فرمول را تحویل دهد و سپس دیالوگ بسته شود؛
- «بازگشت/بستن» بدون ثبت، دیالوگ را عادی ببندد.

### وضعیت داده و امنیت

```text
SQL / Supabase / Edge Function / Migration / Dependency جدید: ندارد
پروژهٔ Supabase: بدون تغییر
```

هیچ `SUPABASE_SERVICE_KEY`، کلید `service_role`، فایل `release.keystore`، رمز
keystore/alias یا `local.properties` در پچ، Git یا APK قرار نگرفته است.
