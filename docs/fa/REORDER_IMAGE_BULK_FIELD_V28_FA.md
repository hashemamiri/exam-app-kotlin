# V28 — جابه‌جایی گزینه، تصویر امن، پنجره گروهی و رشته تحصیلی

**تاریخ:** ۲۰۲۶-۰۸-۱۴
**پیش‌نیاز:** V27
**SQL جدید:** دارد — `sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql` باید یک‌بار اجرا شود.

---

## ۱) جابه‌جایی گزینه‌ها در سوالات چند گزینه‌ای و جورکردنی

### مشکل واقعی

دکمه Drag گزینه‌ها در V27 «کار می‌کرد» ولی قرارداد آن با کارت سؤال یکی نبود:

```text
کارت سؤال            گزینه/جورکردنی (V27)
لمس طولانی            لمس طولانی
تغییر رنگ کارت        بدون انیمیشن رنگ
اسکرول همراه انگشت    بدون اسکرول
بازخورد لمسی          بدون بازخورد
itemCount زنده        itemCount در pointerInput قفل می‌شد
```

نتیجه: هنگام کشیدن، فهرست حرکت نمی‌کرد، انگشت از دکمه خارج می‌شد و اگر تعداد گزینه‌ها
حین Drag تغییر می‌کرد، `pointerInput` با مقدار قدیمی کار می‌کرد.

### اصلاح

`ReorderDragButton` اکنون دقیقاً همان قرارداد کارت سؤال را دارد:

```text
detectDragGesturesAfterLongPress
آستانه تجمعی ۴۶dp با while
rememberUpdatedState برای index و itemCount
animateColorAsState برای حالت فعال
onDragScroll برای حرکت فهرست زیر انگشت
HapticFeedback در شروع و هر پرش
```

- `pointerInput` دیگر به `itemCount` کلید نمی‌خورد؛ بنابراین gesture وسط کار بازنشانی نمی‌شود.
- هنگام Drag داخلی، `userScrollEnabled` فهرست آزمون‌ساز خاموش می‌شود تا فقط همان انگشت کنترل کند
  و کارت سؤال زیر دست نلغزد.
- هر سه مسیر — چندگزینه‌ای، ستون راست و ستون چپ جورکردنی — از همین یک تابع استفاده می‌کنند.
- شناسه‌های پایدار `optionIds` / `matchingLeftIds` / `matchingRightIds` و `key(...)` حفظ شدند.

---

## ۲) بسته‌شدن برنامه پس از انتخاب تصویر (انتخاب عکس برای آپلود)

### علت قطعی

`LocalImageRepository.prepare` داخل `runCatching` بود، اما:

```text
runCatching فقط Exception را می‌گیرد، نه Error
OutOfMemoryError یک Error است
پس OOM از runCatching عبور می‌کرد و پروسه کشته می‌شد
```

سقف decode هم یک عدد ثابت (۷ مگاپیکسل) بود و به حافظهٔ آزاد واقعی دستگاه کاری نداشت؛
روی گوشی کم‌حافظه یا وقتی چند تصویر پشت‌سرهم انتخاب می‌شد، همان سقف ثابت باز هم OOM می‌داد.
`catch (_: OutOfMemoryError)` قبلی هم فقط دور یک `decodeStream` بود و مراحل rotate/crop/scale را پوشش نمی‌داد.

### اصلاح

```text
بودجه پیکسل = حافظه آزاد واقعی JVM ÷ ۴ بایت ÷ ضریب ایمنی
حداکثر ۴ تلاش؛ هر تلاش بودجه و لبه را نصف می‌کند
تلاش‌های بعدی RGB_565 (نصف حافظه هر پیکسل)
هر مرحله rotate/crop/scale جداگانه bitmap میانی را recycle می‌کند
finally همیشه bitmap کاری را آزاد می‌کند
recoverCatching هر OutOfMemoryError باقی‌مانده را به پیام فارسی تبدیل می‌کند
```

نتیجه: بدترین حالت اکنون یک پیام فارسی «حافظه دستگاه کافی نیست» است، نه بسته‌شدن برنامه.
مسیر امن V27 (پیش‌پردازش URI خام پیش از باز شدن ویرایشگر) دست‌نخورده باقی ماند.

---

## ۳) پنجره گروهی

### قبل

```text
Surface: fillMaxWidth + widthIn(max = 720.dp) + height(maxHeight)
padding: horizontal 12، top 8
LazyColumn: weight(1f) → همیشه کل ارتفاع را می‌گرفت
```

پنجره تکی اما `widthIn(max = 620.dp)` و padding `14/10` داشت و ارتفاعش با محتوا رشد می‌کرد.

### حالا

```text
Surface: widthIn(max = 620.dp) + heightIn(max = availableHeight)
padding: horizontal 14، vertical 10  ← دقیقاً مانند پنجره تکی
LazyColumn: weight(1f, fill = false) ← با محتوا رشد می‌کند
```

با یک ردیف، پنجره کوتاه است؛ با ردیف‌های بیشتر تا سقف فضای بالای کیبورد بزرگ می‌شود.
`SOFT_INPUT_ADJUST_RESIZE` و `imePadding` حفظ شدند.

### اسکرول خودکار روی +

```text
لمس + → ردیف جدید اضافه می‌شود
pendingRevealIndex = آخرین اندیس
دو withFrameNanos صبر تا کارت اندازه‌گیری شود
animateScrollToItem(target)
```

بنابراین کارت اطلاعات جدید بلافاصله ظاهر می‌شود و کاربر لازم نیست دستی پایین برود.

---

## ۴) رشته تحصیلی

مورد «رشته» به‌صورت فیلد مستقل با ستون واقعی دیتابیس اضافه شد.

### ستون‌ها

```text
public.profiles.field_of_study   رشته دانش‌آموز
public.profiles.hdr_field        رشته سربرگ رسمی معلم
public.classes.field_of_study    رشته کلاس
```

### رشته‌های استاندارد

```text
ریاضی فیزیک · علوم تجربی · ادبیات و علوم انسانی · علوم و معارف اسلامی
فنی و حرفه‌ای · کاردانش · عمومی · سایر (ورودی دستی)
```

`FieldOfStudyPicker` از همان چرخ Snapدار پایه استفاده می‌کند؛ بنابراین رفتار، گزینه «سایر»
و ورودی دستی دقیقاً مثل پایه است. چرخ مشترک پارامتری شد ولی پیش‌فرض پایه تغییری نکرد.

### محل‌های نمایش

```text
فرم ویرایش دانش‌آموز
فرم گروهی (هر ردیف)
کارت دانش‌آموز (جزئیات)
پنجره کلاس جدید/ویرایش
کارت کلاس
فیلتر انتخاب اعضای موجود
سربرگ رسمی امتحان در پروفایل
چاپ/PDF رسمی
خروجی XLSX دانش‌آموزان
کپی اطلاعات دانش‌آموز
```

### RPCهای جدید

```text
native_save_student_extra_v28(uuid,text,text,text,text)
native_save_class_v28(uuid,text,text,text)
native_my_classes_v28()
native_save_profile_v28(... , p_hdr_field)
native_export_backup_v3()   → پشتیبان نسخه ۴
native_restore_backup_v3()  → نسخه ۱ تا ۴ را می‌پذیرد
my_students() و class_roster() با ستون رشته بازنویسی شدند
```

RPCهای قدیمی بدون رشته (`save_student_extra`, `create_class`, `update_class`) دیگر از APK صدا زده نمی‌شوند.

---

## امنیت

- مالکیت هر نوشتن با `auth.uid()` و `teacher_id` بررسی می‌شود؛ معلم دیگر نمی‌تواند
  دانش‌آموز یا کلاس شما را تغییر دهد.
- دانش‌آموز نمی‌تواند سربرگ معلم را بنویسد.
- هر مقدار حداکثر ۱۲۰ نویسه است.
- همه UPDATE/DELETEها WHERE دارند و با safeupdate سازگارند.
- `revoke ... from public, anon` و `grant ... to authenticated` برای همه توابع جدید.

## امنیت رمز

رمز قبلی Supabase Auth یک hash یک‌طرفه است و قابل بازیابی نیست. ذخیره قابل‌بازیابی جدیدی
ساخته نشده و `plain_password` همچنان حذف‌شده و ممنوع است. فقط رمز جدیدی که سرور با موفقیت
ثبت کرده، همان یک‌بار در حافظه نشست قابل نمایش و کپی حساس است.

---

## فایل‌های کلیدی

```text
docs/fa/REORDER_IMAGE_BULK_FIELD_V28_FA.md
sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql
supabase/migrations/20260814_native_field_of_study_v28.sql
supabase/tests/20260814_v28_integration.sql
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/main/java/ir/exam/app/ui/common/FieldOfStudyPicker.kt
app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt
app/src/main/java/ir/exam/app/domain/model/SchoolModels.kt
app/src/main/java/ir/exam/app/domain/model/ProfileModels.kt
app/src/main/java/ir/exam/app/domain/model/OfficialPrintModels.kt
app/src/main/java/ir/exam/app/domain/repository/SchoolRepository.kt
app/src/main/java/ir/exam/app/data/dto/SchoolDtos.kt
app/src/main/java/ir/exam/app/data/dto/NativeProfileDtos.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt
app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt
app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt
app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt
app/src/main/java/ir/exam/app/ui/reports/ReportPrintHelper.kt
app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
scripts/verify_native_final.py
```

---

## عملیات

```text
۱) اجرای sql/manual/SQL_NATIVE_FIELD_OF_STUDY_V28.sql در SQL Editor پروژه اصلی
   https://eazwuyrymsvdkwckdpco.supabase.co
۲) نتیجه باید هشت مقدار true بدهد
۳) اعمال Patch و Push به repository Kotlin
Edge deploy: ندارد
Secret جدید: ندارد
Dependency جدید: ندارد
```

### Readiness موردانتظار

```text
student_field_ready   true
class_field_ready     true
header_field_ready    true
student_extra_ready   true
class_save_ready      true
class_list_ready      true
profile_save_ready    true
backup_v4_ready       true
```
