# گزارش ممیزی امنیتی — exam-app-kotlin

**فایل مستقلِ بخش ۲۸۱ هندآف؛ منبع واحد برای پیگیری تغییرات امنیتی.**

**نسخهٔ بررسی‌شده:** HEAD `2e7f0e7` (V74.2) — برنچ `main`
**تاریخ ممیزی:** ۱۴۰۴/۰۶/۱۲ (۲۰۲۶-۰۹-۰۳)
**روش:** بازخوانی مستقیم سورس (کد کاتلین، SQL، Edge Functions، workflow، manifest، assetها) — بدون حدس، بدون اجرای کد روی سرور واقعی
**نتیجهٔ اسکنر پروژه:** `FINAL_NATIVE_VERIFY=PASS`

---

## ۰) خلاصهٔ یک‌خطی

هیچ رمز/کلید/توکنی در سورس نیست و لایهٔ دیتابیس (RLS + Grants + search_path) واقعاً سخت‌گیرانه بسته شده؛
اما **۵ مورد بحرانی/بالا** وجود دارد که مهم‌ترین آن‌ها یک ریسک مالی مستقیم است:
اگر متغیرهای محیطی درگاه پرداخت هنوز روی حالت آزمایشی باشند، **هر کاربر می‌تواند بدون پرداخت، کیف پول خود را شارژ کند**.

| شدت | تعداد | خلاصه |
|---|---|---|
| 🔴 بحرانی | ۱ | شارژ رایگان کیف پول در صورت فعال‌بودن sandbox روی سرور |
| 🟠 بالا | ۳ | تصاویر پاسخ/آواتار عمومی، ارتقای خودکار نقش به مدیر، نبود ۲۸ تابع حیاتی در ریپو |
| 🟡 متوسط | ۶ | افشای ایمیل کارکنان، مانیتور بدون کنترل عضویت، رمزهای قابل‌بازیابی و… |
| 🟢 پایین/سخت‌گیرانه | ۵ | نشست در SharedPreferences، نبود pinning، فونت خارجی و… |

---

## ۱) 🔴 بحرانی

### ۱.۱ — شارژ رایگان کیف پول در صورت فعال‌بودن `sandbox`

**کجا:**
- `supabase/functions/wallet-payment/index.ts` خطوط ۳۱۵ تا ۳۳۶
- `supabase/config.toml` → `verify_jwt = false` برای این تابع

**چه اتفاقی می‌افتد:**
وقتی متغیر محیطی `PAY_PROVIDER=sandbox` و `PAY_ALLOW_SANDBOX=true` باشد، تابع پس از ساخت سفارش،
**بدون هیچ تأییدیهٔ بانکی** مستقیماً `native_credit_wallet_payment` را صدا می‌زند و موجودی را واقعی افزایش می‌دهد:

```ts
if (provider === 'sandbox') {
  const refId = `SB-${orderId}-${Date.now()}`;
  await service.rpc('native_credit_wallet_payment', { p_order: orderId, p_ref: refId });
  return json({ ok: true, credited: true, ... });
}
```

محدودیت‌ها روی کاغذ درست‌اند (۱۰۰هزار تا ۱۰ میلیون تومان، مضرب ۱۰هزار، سقف موجودی ۱۰ میلیون)،
اما **تعداد سفارش نامحدود است** و کل موجودی صرفِ ساخت آزمون/تکثیر/بازیابی می‌شود.

**چرا خطرناک است:**
این دقیقاً همان حالتی است که برای تست فعال کرده بودید. اگر روی پروژهٔ اصلی هنوز همین مقادیر تنظیم شده باشند،
هر کاربرِ واردشده می‌تواند با یک درخواست POST به نشانی عمومی تابع، کیف پولش را رایگان شارژ کند.
نشانی تابع عمومی است و نیاز به ادمین ندارد؛ فقط کافی است JWT یک نشست معتبر را بفرستد.

**اقدام فوری (۵ دقیقه، بدون تغییر کد):**
1. وارد پنل Supabase → پروژهٔ `eazwuyrymsvdkwckdpco` → **Edge Functions → wallet-payment → Secrets** شوید.
2. مقدار `PAY_ALLOW_SANDBOX` را `false` و `PAY_PROVIDER` را به `zarinpal` یا `idpay` برگردانید (یا هر دو را پاک کنید).
3. سپس این کوئری را در SQL Editor اجرا کنید تا ببینید آیا شارژ تقلبی ثبت شده یا نه:
   ```sql
   select id, user_id, amount_toman, provider, status, ref_id, paid_at
   from public.wallet_payment_orders
   where provider = 'sandbox'
   order by id desc
   limit 100;
   ```
4. اگر ردیف `paid` با `sandbox` دیدید، باید دستی اصلاح موجودی انجام شود.

**سخت‌سازی پیشنهادی (در صورت تمایل، پچ جداگانه):**
افزودن یک کلید جداگانه (مثلاً `PAY_SANDBOX_TOKEN`) که کلاینت برای شارژ آزمایشی باید ارسال کند،
یا محدود کردن sandbox به فهرست سفید user_id.

---

## ۲) 🟠 بالا

### ۲.۱ — تصاویر پاسخ دانش‌آموز و آواتارها بدون احراز هویت عمومی هستند

**کجا:**
- `supabase/migrations/20260812_native_final_hardening.sql` خط ۳۷۹:
  ```sql
  create policy v11_public_read_exam_images
  on storage.objects for select to public
  using (bucket_id = 'exam-images');
  ```
- `app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt` خط ۷۱:
  ```kotlin
  return uploadAt("answers/$studentId/$examId/$questionId", Uri.parse(uri))
  ```
- خط ۱۳۲ همان فایل: `return bucket.publicUrl(path)` — یعنی URL عمومی ساخته و ذخیره می‌شود.

**چرا خطرناک است:**
- bucket با policy کاملاً عمومی است؛ هر کسی که URL را داشته باشد، **بدون ورود** می‌تواند فایل را ببیند.
- مسیر شامل **شناسهٔ کاربری دانش‌آموز** است.
- نتیجه: عکس دست‌خط دانش‌آموزان (حاوی نام/اطلاعات) و آواتارها در صورت درز یک لینک، عمومی می‌شوند.
- یک نکتهٔ مهم‌تر: در کد `teacher_public_profile` گزینهٔ `avatar_public` وجود دارد و وقتی `false` است
  آواتار به دانش‌آموز نشان داده نمی‌شود — اما چون policy عمومی است، **این گزینه در عمل حفاظتی ایجاد نمی‌کند**
  و فقط حس امنیت کاذب می‌دهد.

**راه‌حل پیشنهادی:**
- policy خواندن عمومی را حذف و به «صاحب فایل + معلم/مدیر مرتبط» محدود کنید، و تصاویر را با
  `createSignedUrl(...)` (لینک موقت) مصرف کنید؛ یا
- bucket را خصوصی کنید و فقط پوشهٔ `questions/` را (در صورت نیاز) عمومی بگذارید.

### ۲.۲ — ارتقای خودکار نقش به «مدیر مدرسه» بدون گارد سمت سرور

**کجا:** `sql/manual/SQL_NATIVE_SCHOOL_MANAGER_V36.sql` خطوط ۶۸ تا ۱۳۸
(همسان در `supabase/migrations/20260815_native_school_manager_v36.sql`)

```sql
grant execute on function public.native_complete_manager_registration_v36(...) to authenticated;
```

**چه می‌کند:** هر کاربرِ واردشده‌ای که پروفایلش `role in ('student','manager')` و `teacher_id is null` باشد
و عضو کلاس/مدرسه نباشد، می‌تواند خود را **مدیر** کند و مدرسهٔ جدید بسازد.

**نقاط ضعف:**
1. بررسی نمی‌کند کاربر واقعاً در جریان ثبت‌نام، نقش «مدیر» را انتخاب کرده باشد
   (فیلد `native_registration_roles` / `registration_role` اصلاً خوانده نمی‌شود) — یعنی گارد فقط سمت کلاینت است.
2. `email_confirmed_at is not null` بررسی نمی‌شود؛ فقط «ایمیل خالی نباشد و @student.exam.local نباشد».
   اگر در تنظیمات Auth گزینهٔ «Confirm email» خاموش باشد، این مسیر کاملاً باز است.
3. کاربری که همین الان `manager` است می‌تواند دوباره صدا بزند و مدرسهٔ دوم/سوم بسازد.

**همین مشکل برای نقش معلم هم هست:**
`20260812_native_critical_flows_v12.sql` خط ۱۱ — `native_complete_teacher_registration_v1`
فقط چک می‌کند ایمیل خالی نباشد و `@student.exam.local` نباشد؛ تأیید ایمیل چک نمی‌شود.

**راه‌حل:** افزودن شرط
`and u.email_confirmed_at is not null` (برای هر دو تابع) و خواندن نقش انتخابی ثبت‌نام از جدول
`native_registration_roles` به‌جای اعتماد به کلاینت.

### ۲.۳ — ۲۸ تابع حیاتی فقط روی سرورند و در ریپو وجود ندارند

**کجا:** `supabase/migrations/20260812_native_final_hardening.sql` خطوط ۲۹۵ تا ۳۲۰ (فهرست allowlist)

این توابع در allowlist مجاز شده‌اند اما **هیچ تعریفی از آن‌ها در ریپو نیست** (میراث دورهٔ WebView هستند
و ریپوی قدیمی `exam-app` هم now خصوصی/غیرقابل دسترس است):

```text
get_exam_for_student   submit_answer (از طریق execute دینامیک)
my_answers  my_grades  my_classes  create_class  update_class  delete_class
class_roster_pick  add_students_to_class  remove_student_from_class
my_students_for_pick  save_student_extra  set_student_active
bank_add  bank_list  bank_del  bank_move  fb_add  fb_list
exam_attendance  exam_attend_summary  exam_live_status  exam_autograde_info
approve_auto_grades  unapprove_grade  reset_student_attempt  extend_student_time
get_exam_audience  set_exam_audience  teacher_public_profile
```

**چرا این یک ردفلگ جدی است:**
- تحویل آزمون به دانش‌آموز (`get_exam_for_student`)، ثبت پاسخ (`submit_answer`)،
  تصحیح و حضور‌و‌غیاب — یعنی **حساس‌ترین منطق برنامه** — تحت کنترل نسخه نیستند.
- `native_submit_queued_answer_v1` با `execute 'select public.submit_answer($1,$2,$3,$4)'`
  مستقیماً همان تابع قدیمی را صدا می‌زند؛ بنابراین هر ضعفی در آن (کنترل مهلت، جلوگیری از ارسال مجدد،
  کنترل مخاطب آزمون) مستقیماً به نسخهٔ Native منتقل می‌شود و شما نمی‌توانید آن را در کد ببینید.
- اگر کسی در پنل Supabase تابعی را دستی تغییر دهد، هیچ اثری در Git نمی‌ماند.

**اقدام:** اجرای این کوئری در SQL Editor و ذخیرهٔ خروجی در ریپو (برای ممیزی بعدی من هم لازم است):

```sql
select p.proname, pg_get_functiondef(p.oid)
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname in ('get_exam_for_student','submit_answer','my_answers','my_grades','my_classes',
                    'create_class','update_class','delete_class','class_roster_pick',
                    'add_students_to_class','remove_student_from_class','my_students_for_pick',
                    'save_student_extra','set_student_active','bank_add','bank_list','bank_del',
                    'bank_move','fb_add','fb_list','exam_attendance','exam_attend_summary',
                    'exam_live_status','exam_autograde_info','approve_auto_grades','unapprove_grade',
                    'reset_student_attempt','extend_student_time','get_exam_audience','set_exam_audience')
order by p.proname;
```

---

## ۳) 🟡 متوسط

### ۳.۱ — افشای ایمیل معلم/مدیر به کاربر ناشناس (Username Enumeration)

**کجا:** `supabase/migrations/20260825_native_staff_login_google_v60.sql` خط ۵ و خط ۳۳

```sql
create or replace function public.native_staff_login_email_v1(p_username text) ... security definer
grant execute on function public.native_staff_login_email_v1(text) to anon, authenticated;
```

این تابع SECURITY DEFINER است، از `auth.users` می‌خواند و **ایمیل کامل** کاربر را به anonymous برمی‌گرداند.
هر کسی بدون ورود می‌تواند نام‌های کاربری را جست‌وجو کند و ایمیل واقعیِ ورود معلم/مدیر را به دست آورد
(هدف برای حملهٔ password-spraying و فیشینگ). پیام خطا هم یکسان است (خوب)، اما ایمیل در حالت موفق لو می‌رود.

**راه‌حل:** حذف grant از `anon` (فقط `authenticated`) یا حذف کامل تابع و جایگزینی با
نگاشت سمت کلاینتِ امن‌تر؛ در هر صورت پاسخ نباید شامل ایمیلِ کاربرِ دیگر باشد.

### ۳.۲ — گزارش مانیتورینگ آزمون: بدون کنترل عضویت و بدون محدودیت اندازه

**کجا:** `supabase/migrations/20260825_native_exam_monitor_v58.sql` خط ۲۰ و خط ۸۱

- تابع فقط بررسی می‌کند `auth.uid()` خالی نباشد؛ **بررسی نمی‌کند** دانش‌آموز واقعاً شرکت‌کنندهٔ آن آزمون باشد
  یا آزمون اصلاً وجود داشته باشد. هر کاربر واردشده می‌تواند برای هر `exam_id` دلخواه ردیف بسازد.
- `p_report` هیچ محدودیت اندازه‌ای ندارد و با عملگر `||` روی ردیف قبلی **جمع** می‌شود
  (`report = public.native_exam_monitor.report || excluded.report`) → امکان رشد بی‌رویهٔ دیتابیس (DoS ارزان)
  و آلوده‌کردن گزارش‌های معلم با دادهٔ جعلی.

**راه‌حل:** بررسی存在 آزمون + عضویت/مخاطب، محدود کردن اندازهٔ jsonb (مثل ۸KB) و جایگزینی به‌جای الحاق.

### ۳.۳ — فایل خروجی/واردی آزمون حاوی پاسخ‌نامه است

**کجا:** `app/src/main/java/ir/exam/app/data/repository/ExamPackageCodec.kt` خطوط ۵۰ تا ۵۷

```kotlin
val combined = JsonArray(encoded.publicQuestions.mapIndexed { index, item ->
    val public = item.jsonObject
    val answer = encoded.answerKey.getOrNull(index)?.jsonObject ?: JsonObject(emptyMap())
    JsonObject(public + (answer - "i"))
})
```

فایل `.azmoon` سؤال‌ها **به‌همراه کلید پاسخ** را در قالب Base64 ذخیره می‌کند (رمزنگاری نیست، فقط کدگذاری است).
اگر معلمی فایل آزمون را برای همکار/دانش‌آموز بفرستد، پاسخ‌ها هم منتقل می‌شوند.

**نکتهٔ مثبت:** بخش import واقعاً تمیز است — محدودیت ۸ مگابایت، حداکثر ۵۰۰ سؤال، فقط URLهای `https://`،
محدودیت طول فیلدها و اعتبارسنجی نوع فایل. مشکل از نظر امنیتِ حافظه/تزریق نیست، فقط افشای داده است.

**راه‌حل:** هنگام Export یک هشدار صریح نمایش دهید یا گزینهٔ «بدون پاسخ‌نامه» اضافه کنید.

### ۳.۴ — ذخیرهٔ رمزهای قابل‌بازیابی دانش‌آموزان روی دستگاه معلم

**کجا:** `app/src/main/java/ir/exam/app/data/local/StudentPasswordVault.kt`

کلاس با Android Keystore + AES/GCM رمزنگاری می‌کند (از نظر فنی درست)، اما واقعیت این است که
**رمزِ plaintext دانش‌آموزان در دستگاه معلم قابل بازیابی باقی می‌ماند**. این با شعار
«رمز قبلی قابل بازیابی نیست» در تضاد است و در صورت دسترسی فیزیکی/بدافزار به دستگاه معلم،
رمز همهٔ دانش‌آموزان آن معلم لو می‌رود (امکان جعل هویت در آزمون).

**راه‌حل:** ذخیرهٔ موقت در حافظه (تا زمان نمایش یک‌باره) به‌جای ماندگاری روی دیسک.

> **اصلاحیهٔ V75.9 (۲۰۲۶-۰۹-۰۳):** عنوانِ این مورد گمراه‌کننده بود. رمزها **متن ساده نیستند**؛
> `StudentPasswordVault` آن‌ها را با کلیدِ غیرقابل‌استخراجِ Android Keystore و AES/GCM
> رمزنگاری می‌کند و با `allowBackup=false` به دستگاه دیگر منتقل نمی‌شوند. چیزی که باقی
> می‌ماند «قابلیتِ بازیابی روی همان دستگاه» است (یک تصمیمِ طراحی برای راحتیِ معلم)،
> نه ذخیرهٔ متن ساده. با تصمیمِ شما این رفتار حفظ می‌شود و فقط ریسکِ باقیمانده ثبت می‌گردد:
> اگر دستگاهِ معلم روت‌شده یا به بدافزار آلوده باشد و هم‌زمان قفلِ صفحه دور زده شود،
> مهاجم می‌تواند رمزها را بازیابی کند. راهکارِ کامل، نمایشِ یک‌باره و نگه‌داری در حافظه است
> که به معنای واردکردنِ دوبارهٔ رمز پس از هر بار بستن برنامه است.

### ۳.۵ — نشست ورود در SharedPreferencesِ رمزنگاری‌نشده

**کجا:** `app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt`
هیچ `sessionManager` سفارشی تنظیم نشده است، بنابراین supabase-kt از پیش‌فرض خودش
(`SettingsSessionManager` → SharedPreferences معمولی) استفاده می‌کند و access/refresh token را
به‌صورت plaintext ذخیره می‌کند.

نکتهٔ مثبت: `android:allowBackup="false"` است، پس از مسیر پشتیبان‌گیری قابل استخراج نیست؛
اما روی دستگاه روت‌شده یا با بدافزارِ دارای دسترسی root قابل خواندن است.
(نکتهٔ خوب دیگر: هیچ `Log.d/e` ای در کل کد اصلی وجود ندارد — یعنی هیچ نشتی لاگی ندارید.)

**راه‌حل:** `EncryptedSharedPreferences` به‌عنوان SessionManager سفارشی (نیازمند افزودن وابستگی
`androidx.security:security-crypto`).

### ۳.۶ بدون محدودیت نرخ (Rate limiting) در Edge Function ساخت حساب

**کجا:** `supabase/functions/manage-student/index.ts` — اکشن‌های `create` و `bulk`

احراز هویت، نقش، مالکیت کلاس و محدودیت حجم (۲۵۶KB) درست‌اند؛ اما هیچ محدودیتی روی تعداد
حساب‌های ساخته‌شده نیست (`bulk` تا ۱۰۰ حساب در هر درخواست، بدون سقف روزانه و بدون کسر اعتبار).
یک حساب معلمِ مخرب می‌تواند هزاران حساب دانش‌آموز در Auth بسازد (مصرف سهمیه، هرزنامه، هزینه).

**نکتهٔ مثبت:** هنگام ساخت، رمزها در پاسخ `bulk` برمی‌گردند — از نظر طراحی لازم است،
اما یعنی رمز plaintext روی شبکه و در حافظهٔ کلاینت هست.

---

## ۴) 🟢 پایین / سخت‌گیرانه

| # | مورد | کجا | توضیح |
|---|---|---|---|
| ۱ | ارجاع خارجیِ بی‌اثر به Google Fonts | `assets/print/exam_print.html` خط ۷ | درخواست عملاً توسط `shouldInterceptRequest` مسدود می‌شود (مسیر `/css2` با پیشوند `/print/` شروع نمی‌شود ⇒ پاسخ خالی)، بنابراین نشتی ندارد؛ اما فونت هیچ‌وقت بارگیری نمی‌شود و خروجی چاپ ممکن است با طراحی متفاوت باشد. اگر روزی منطقِ interception تغییر کند، همین خط تبدیل به نشت اطلاعات (IP/User-Agent به گوگل) می‌شود. |
| ۲ | تزریق JSON در `evaluateJavascript` | `ExamHtmlPrintDialog.kt` خط ۱۶۹ | payload با kotlinx.serialization ساخته شده (JSON معتبر)، بنابراین تزریق کد ممکن نیست؛ فقط در صورت وجود U+2028/U+2029 در متن سؤال احتمال خطای تجزیه هست. پیشنهاد: `JSON.parse('...')` با رشتهٔ escaped. |
| ۳ | نبود Certificate Pinning | ندارد | روی شبکهٔ مخرب، مهاجم با گواهی جعلی می‌تواند توکن را بخواند. پیاده‌سازی آن ریسکِ «خراب شدن برنامه هنگام تعویض گواهی» دارد؛ تصمیم با شما. |
| ۴ | پایگاه دادهٔ Room رمزنگاری نشده | `NativeDatabaseProvider.kt` | پیش‌نویس پاسخ‌ها و صف آفلاین روی دیسک plaintext است. با `allowBackup=false` ریسک کم است. |
| ۵ | قفل برنامه فقط با اثرانگشت/رمز دستگاه | `AppLockManager.kt` | بقایای PIN قدیمی پاک می‌شود (خوب) و فقط BiometricPrompt استفاده شده (روش رسمی)؛ اما امنیت آن در سطح امنیت قفل خودِ گوشی است. |

---

## ۵) چیزهایی که واقعاً خوب هستند (تأییدشده)

```text
✅ هیچ کلید/رمز/توکن/سرویس‌کی در سورس، SQL، تست یا workflow نیست
   (SUPABASE_SERVICE_ROLE_KEY فقط از طریق Deno.env در Edge Function خوانده می‌شود)

✅ RLS روی همهٔ جدول‌های public فعال است (V11 تمام جدول‌ها + هر ۱۷ جدول جدید بعد از آن)

✅ V11 دسترسی مستقیم anon/authenticated به همهٔ جدول‌ها را revoke کرده؛
   فقط SELECT روی profiles/exams/exam_keys/answers/classes/class_members با policy مالک‌محور

✅ همهٔ توابع SECURITY DEFINER دارای set search_path هستند (بررسی خودکار: صفر مورد فاقد آن)

✅ ستون profiles.plain_password کاملاً حذف شده و هیچ ردی از آن در کد اصلی نیست

✅ کنترل مالکیت در توابع بحرانی درست است: native_save_grade، native_submit_queued_answer_v1،
   native_my_answer_detail_v1 (کلید پاسخ را قبل از تصحیح هرگز برنمی‌گرداند)،
   توابع مدیر مدرسه (همگی school_memberships با staff_role='manager' را چک می‌کنند)

✅ عملیات مالی اتمیک و idempotent است: native_credit_wallet_payment،
   native_create_wallet_payment_order، native_exam_operations.operation_id

✅ Edge Function مدیریت دانش‌آموز: JWT را با auth.getUser بررسی می‌کند، نقش را چک می‌کند،
   محدودیت حجم دارد و مالکیت کلاس/دانش‌آموز را کنترل می‌کند (از جمله تأیید معلم برای عملیات مدیر)

✅ Manifest: allowBackup=false، usesCleartextTraffic=false، FileProvider غیر-exported،
   فقط MainActivity آن هم با MAIN/LAUNCHER

✅ مکانیزم بروزرسانی APK بسیار خوب است: فقط HTTPS، تطبیق اندازه، SHA-256، نام بسته،
   versionCode بالاتر و تطبیق امضا با نسخهٔ نصب‌شده

✅ WebViewها سخت‌گیرانه‌اند: allowFileAccess=false، allowUniversalAccessFromFileURLs=false،
   مسدود کردن ناوبری خارجی، مسدود کردن مسیرهای غیر asset، آزادسازی در onRelease

✅ FLAG_SECURE روی صفحهٔ آزمون دانش‌آموز فعال است

✅ هیچ Log.* در کد اصلی ⇒ نشتی اطلاعات در لاگ‌کت وجود ندارد

✅ ورود گوگل با nonce هش‌شده (روش صحیح مستندات Supabase)
```

---

## ۶) اولویت‌بندی پیشنهادی

```text
همین امروز (بدون کد):
  🔴 بررسی و غیرفعال‌سازی PAY_ALLOW_SANDBOX / PAY_PROVIDER در پنل Supabase
  🔴 اجرای کوئری بررسی سفارش‌های sandbox پرداخت‌شده

این هفته (نیازمند SQL):
  🟠 خصوصی‌سازی bucket exam-images (حذف policy عمومی + لینک موقت)
  🟠 افزودن شرط email_confirmed_at و نقشِ انتخابی به ثبت‌نام معلم/مدیر
  🟡 حذف grant تابع native_staff_login_email_v1 از anon
  🟡 محدودسازی native_monitor_upsert_v1 (عضویت در آزمون + اندازهٔ گزارش)

کمی بعد (نیازمند پچ کد):
  🟡 هشدار «خروجی حاوی پاسخ‌نامه» هنگام Export
  🟡 حذف ماندگاری رمز دانش‌آموز (فقط حافظه)
  🟢 EncryptedSharedPreferences برای نشست
  🟢 حذف/توضیح ارجاع Google Fonts در فایل چاپ

ممیزی تکمیلی ( blocking ):
  🟠 استخراج تعریف ۲۸ تابع قدیمی از سرور و ممیزی get_exam_for_student و submit_answer
```

---

## ۷) محدودیت این گزارش

موارد زیر **درون ریپو نیستند** و فقط با دسترسی به پنل Supabase/دستگاه قابل بررسی‌اند؛
اگر بخواهید، با خروجی آن‌ها همین گزارش را کامل می‌کنم:

```text
۱) تنظیمات Auth: Confirm email روشن است؟ محدودیت نرخ OTP؟ طول عمر توکن؟
۲) مقادیر واقعی Secretهای Edge Function (موضوع بند ۱.۱)
۳) تعریف واقعی ۲۸ تابع قدیمی روی سرور (موضوع بند ۲.۳)
۴) policyهای واقعیِ Storage روی سرور (ممکن است با migrationها تفاوت کرده باشد)
۵) بررسی ترافیک واقعی برنامه با یک پروکسی (آیا دادهٔ حساسی بیرون می‌رود؟)
```


---

## ۵) جدول پیگیریِ اصلاحات (به‌روز‌شده: ۲۰۲۶-۰۹-۰۳)

| مورد | نسخهٔ اصلاح | وضعیت | نیاز به عملیاتِ دستی |
|---|---|---|---|
| 🔴 شارژ رایگان کیف پول در sandbox | **V75.0** | رفع‌شده در کد | `deploy wallet-payment` + «غیرفعال‌کردن sandbox روی سرور» و بررسی سفارش‌های قدیمی |
| 🟠 تصاویر و آواتارِ عمومی | **V75.8** | رفع‌شده | اجرای SQL (باکت خصوصی می‌شود) + تست تصویرها روی دستگاه |
| 🟠 ارتقای نقش به مدیر/معلم | **V75.1** | رفع‌شده | اجرای SQL |
| 🟠 ۲۸ تابعِ قدیمی خارج از ریپو | **V75.6** | دسترسیِ ناشناس بسته شد | اجرای SQL + اجرای کوئری استخراج و فرستادن خروجی برای ممیزی |
| 🟡 افشای ایمیل کادر مدرسه | **V75.2** | محدودسازیِ نرخ اضافه شد | اجرای SQL |
| 🟡 مانیتور بدون کنترل و سقف | **V75.3** | رفع‌شده | اجرای SQL |
| 🟡 پاسخنامه در فایل .azmoon | **V75.4** | انتخابِ معلم هنگام صدور | نیاز به عملیات ندارد |
| 🟡 رمزهای قابل‌بازیابیِ دانش‌آموز | — | **اشتباهِ گزارش (اصلاح‌شده در V75.9)** | ندارد؛ در صورت تمایل به حافظه منتقل می‌شود |
| 🟡 نشست در SharedPreferences ساده | **V75.7** | نشست رمزنگاری شد | کاربران یک‌بار باید دوباره وارد شوند |
| 🟡 ساختِ دانش‌آموز بدون محدودیت نرخ | **V75.5** | سهمیهٔ ساعتی/روزانه | اجرای SQL + `deploy manage-student` |

نکته: ردیف‌های «پایین/سخت‌گیرانه» (Certificate Pinning، Room رمزنگاری‌نشده، فونت خارجی،
قفل برنامه، تزریق JSON) تصمیمِ محصول هستند و در این سری اصلاح نشدند.
