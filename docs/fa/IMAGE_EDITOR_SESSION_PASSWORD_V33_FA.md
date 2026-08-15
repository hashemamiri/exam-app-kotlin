# V33 — ممیزی کامل مسیر تصویر و اصلاح رمز فعلی نشست

## علت قطعی کرش بر اساس logcat دستگاه

```text
FATAL EXCEPTION: main
java.lang.IllegalStateException: Size is unspecified
InteractiveImageEditorDialog.kt:168
```

`sourcePixels` در اولین composition برابر `Size.Unspecified` بود. در Compose، خواندن
`width` یا `height` از این مقدار مجاز نیست و پیش از آن‌که Coil رویداد `onSuccess`
را برگرداند، ویرایشگر روی thread اصلی بسته می‌شد. بنابراین کرش از شبکه، Supabase،
حجم تصویر یا آپلود نبود و هنوز هیچ درخواست آپلودی آغاز نشده بود.

## اصلاح

تابع واحد `safeImagePixelSize` اضافه شد. مقدار `Size.Unspecified` یا ابعاد صفر را به
`Size(1f, 1f)` تبدیل می‌کند. تمام محاسبات زیر فقط از `safePixels` استفاده می‌کنند:

- نسبت و ابعاد preview؛
- هندسهٔ کادر crop؛
- برآورد حجم crop؛
- ساخت `CropRect` هنگام زدن تیک سبز.

هیچ دسترسی مستقیم `sourcePixels.width` یا `sourcePixels.height` در ویرایشگر باقی
نمانده است.

## نتیجهٔ ممیزی کدبه‌کد سیستم تصویر

| مرحله | فایل/مسیر | نتیجه |
|---|---|---|
| انتخاب تک/چند تصویر | `QuestionMediaEditor.kt`، `QuestionOptionMedia.kt`، `ProfileSettingsScreen.kt`، `StudentExamScreen.kt` | URI null کنترل شده؛ مجوز persistable به‌صورت امن تلاش می‌شود. |
| آماده‌سازی محلی | `LocalImageRepository.kt` | روی IO؛ bounds decode، sampling، سقف پیکسل/لبه، retry برای OOM و recycle در finally برقرار است. |
| ویرایش UI | `InteractiveImageEditorDialog.kt` | علت واقعی crash پیدا و رفع شد؛ loading/error/busy و منبع امن برقرار است. |
| چرخش و crop | `LocalImageRepository.kt` و `CropGeometry.kt` | ابعاد و مختصات محدود می‌شوند؛ bitmap میانی بعد از جایگزینی recycle می‌شود. |
| فشرده‌سازی محلی | `LocalImageRepository.kt` | JPEG در فایل خصوصی برنامه؛ stream با `use` بسته می‌شود. |
| آپلود سؤال/گزینه/پاسخ/آواتار | `SupabaseQuestionImageUploader.kt` | decode نمونه‌برداری‌شده، چهار تلاش OOM، recycle و محدودیت ۸MB برقرار است. |
| مالک‌های آپلود | `SupabaseExamBuilderRepository.kt`، `SupabaseStudentExamRepository.kt`، `SupabaseProfileRepository.kt` | مسیرهای مجزا و bucket مشترک `exam-images`؛ خطا به Result/ViewModel برمی‌گردد. |
| نمایش خطا | ViewModelهای builder/profile/student | خطاهای repository به state تبدیل می‌شوند و نباید process را ببندند. |

## رمز فعلی پنجرهٔ ویرایش دانش‌آموز

- متن توضیحی «رمز فعلی hash شده و قابل نمایش نیست و ...» حذف شد.
- `StudentEditDialog` مقدار `currentPassword` را از `knownPasswords` همان نشست می‌گیرد.
- کادر «رمز فعلی» read-only است و مقدار واقعی شناخته‌شده در همان اجرای برنامه را
  با دکمهٔ نمایش/مخفی‌کردن نشان می‌دهد.
- اگر فقط نام کاربری تغییر کند، رمز نشست با نام کاربری جدید نیز در دسترس می‌ماند.
- رمز تاریخی از hash بازیابی نمی‌شود و هیچ `plain_password` یا ذخیرهٔ دائمی جدیدی
  ایجاد نشده است.

## تست‌ها

- تست اجرایی `safeImagePixelSize(Size.Unspecified)` اضافه شد.
- تست منبع تضمین می‌کند هیچ دسترسی مستقیم به ابعاد `sourcePixels` باقی نماند.
- تست اتصال کادر رمز فعلی به رمز نشست و دکمهٔ نمایش اضافه شد.
- تست‌های قدیمی V22/V24 تا V28/V32 با قرارداد جدید هماهنگ شدند.
- `scripts/verify_native_final.py` برای guard دقیق stack trace و رمز نشست به‌روز شد.

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
Bucket: exam-images (بدون تغییر)
پیش‌نیاز: V32.1
```
