# V29 — جابه‌جایی پایدار گزینه‌ها، آیکن فرمول، نمایش کامل تصویر و پنجره گروهی تک‌کارتی

**تاریخ:** ۲۰۲۶-۰۸-۱۴
**پیش‌نیاز:** V28
**SQL جدید:** ندارد
**Edge deploy:** ندارد
**Secret جدید:** ندارد

---

## ۱) جابه‌جایی گزینه‌ها دقیقاً مانند کارت سؤال — علت قطعی و اصلاح

### علت واقعی نقص V28

`ReorderDragButton` در V28 به `pointerInput(description)` کلید می‌خورد و
`description` شامل برچسب گزینه است («نگه‌دارید و الف را جابه‌جا کنید»).
وقتی گزینه جابه‌جا می‌شود، برچسب آن (الف → ب) تغییر می‌کند، کلید
`pointerInput` عوض می‌شود و gesture وسط کار لغو می‌شود. نتیجه: هر پرش
نیازمند لمس طولانی تازه بود و رفتار با کارت سؤال یکی نبود.

### اصلاح

```text
pointerInput(description)  →  pointerInput(Unit)
آستانه مشترک 52dp           →  ReorderStepDp = 52f (همان کارت سؤال)
کارت سؤال                    →  ReorderStepDp.dp.toPx()
```

- gesture دیگر با تغییر برچسب بازنشانی نمی‌شود و کشیدن پیوسته تا انتها ادامه می‌یابد؛
- آستانهٔ کارت سؤال و گزینه/جورکردنی دقیقاً یکی است؛
- rememberUpdatedState، haptic، رنگ فعال و اسکرول زیر انگشت از V28 حفظ شدند.

## ۲) آیکن فرمول در سطر دوربین

- دکمهٔ متنی «درج فرمول» از بخش متن حذف شد.
- `QuestionMediaEditor` حالا آیکن `Functions` را پیش از آیکن دوربین در همان سطر دارد.
- پیش‌نمایش و ویرایش فرمول‌های موجود بدون تغییر مانده است.

## ۳) نمایش تمام‌صفحه تصویر

- فایل جدید: `FullScreenImageViewer.kt`.
- لمس thumbnail تصویر سؤال یا گزینه → تصویر تمام‌صفحه با `ContentScale.Fit`.
- زوم pinch تا ۸ برابر، جابه‌جایی، دوبار لمس برای بزرگ‌نمایی/بازگشت.
- بستن فقط با ضربدر (X).
- آیکن مداد کوچک برای ویرایش دوبارهٔ تصویر موجود باقی است.

## ۴) ویرایش پس از انتخاب عکس

- `SingleImagePicker` (گزینه و جورکردنی): پس از `prepare` امن، ویرایشگر
  `InteractiveImageEditorDialog` باز می‌شود؛ تأیید → ذخیره، انصراف → دورانداختن.
- `QuestionMediaEditor` (تصویر متن سؤال): هر عکس انتخاب‌شده پس از `prepare`
  یکی‌یکی وارد ویرایشگر می‌شود (`batchQueue`)؛ پس از آخرین تأیید همه با هم
  اضافه می‌شوند؛ انصراف کل صف را دور می‌اندازد.
- `replaceImage` در ViewModel برای جایگزینی نتیجهٔ ویرایش دوباره اضافه شد.

## ۵) پنجره گروهی تک‌کارتی

- فهرست ردیف‌ها حذف شد؛ در هر لحظه فقط یک کارت دیده می‌شود.
- لمس «+» کارت تازه را جایگزین کارت قبلی می‌کند و پنجره هرگز بزرگ نمی‌شود.
- شماره‌های بالا (۱، ۲، …) هر ردیف را بازمی‌گردانند؛ ردیف کامل «✓» دارد.
- حذف، ردیف فعال را برمی‌دارد و کارت قبلی را نشان می‌دهد.
- ارسال نهایی همچنان همهٔ ردیف‌ها را در بر می‌گیرد (۱ تا ۱۰۰).

## امنیت رمز دانش‌آموز

بدون تغییر: رمز قبلی Supabase Auth غیرقابل بازیابی است، `plain_password`
بازنمی‌گردد و فقط رمز جدید ثبت‌شده یک‌بار با Clipboard حساس قابل کپی است.

## فایل‌های کلیدی

```text
BUILDER_MEDIA_BULK_V29_FA.md
HANDOFF_KOTLIN_MIGRATION_FA.md
app/src/main/java/ir/exam/app/ui/image/FullScreenImageViewer.kt
app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt
app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt
app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt
app/src/test/java/ir/exam/app/ui/app/V29ReorderViewerEditBulkTest.kt
app/src/test/java/ir/exam/app/ui/app/V28ReorderImageBulkFieldTest.kt
app/src/test/java/ir/exam/app/ui/app/V27DataImageOptionsTest.kt
scripts/verify_native_final.py
```

## تست

```text
Kotlin compile             PASS
JVM tests                  199/199 PASS
V29 regression tests       14/14 PASS
FINAL_NATIVE_VERIFY        PASS
lintDebug                  PASS
assembleDebug              PASS
APK Signature Scheme v2    Verified
```

SQL/Edge/Secret/Dependency جدید ندارد.
