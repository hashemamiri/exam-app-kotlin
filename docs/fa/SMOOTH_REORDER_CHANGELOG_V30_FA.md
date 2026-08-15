# V30 — جابه‌جایی رنگی و روان، مشخصات آزمون جمع‌شونده، لیست تغییرات، ویرایش تصویر و پنجره گروهی

**تاریخ:** ۲۰۲۶-۰۸-۱۵
**پیش‌نیاز:** V29
**SQL جدید:** ندارد
**Edge deploy:** ندارد
**Secret جدید:** ندارد

---

## ۱) جابه‌جایی گزینه‌ها و جورکردنی دقیقاً مانند کارت سؤال

### مشکل دستگاه

در V29 gesture درست شده بود اما دو تفاوت با کارت سؤال باقی مانده بود:
کارت گزینه هنگام کشیدن رنگی نمی‌شد و حرکت گزینه‌ها ناگهانی (snap) بود.

### اصلاح

```text
رنگ فعال: کارت گزینه/جورکردنی هنگام درگ primaryContainer می‌شود
          (animateColorAsState با tween 170 — دقیقاً همان قرارداد کارت سؤال)
حرکت روان: AnimatedReorderColumn (فایل ReorderAnimation.kt) موقعیت قبلی هر آیتم را
           به‌خاطر می‌سپارد و با فنر از جای قبلی به جای جدید سر می‌دهد — همان حس animateItem کارت سؤال
رنگ به شناسهٔ پایدار گزینه گره خورده تا وسط درگ گم نشود
ReorderDragButton پارامتر onActiveChanged گرفت و شروع/پایان درگ را اعلام می‌کند
```

## ۲) کارت مشخصات آزمون

```text
پیش‌فرض بسته: settingsExpanded = false
بازکردن کارت سؤال → بسته‌شدن خودکار کارت مشخصات آزمون
(هم در لمس روی سربرگ سؤال و هم در بازشدن با دکمهٔ چیدمان چاپ)
```

## ۳) لیست تغییرات در صفحهٔ درباره

```text
کارت «تغییرات نسخه …» دیگر بعد از دانلود APK پنهان نمی‌شود.
با بازشدن صفحهٔ درباره، بررسی بروزرسانی خودکار انجام می‌شود و لیست تغییرات
بدون نیاز به لمس دکمه نمایش می‌یابد.
هر سطر تمیز می‌شود (حذف - ، • و backtick) و راست‌به‌چپ فارسی نمایش می‌یابد.
GitHub Actions یادداشت‌های فارسی واقعی را از text/CHANGELOG_FA.txt در ریشهٔ repository
می‌خواند و با RPC انتشار به Supabase می‌فرستد؛ اگر فایل نبود، یادداشت عمومی می‌رود.
از این به بعد برای تغییر یادداشت‌های نسخه فقط text/CHANGELOG_FA.txt را ویرایش کنید
(هر سطر = یک مورد، حداکثر ۱۲ سطر ارسال می‌شود).
```

## ۴) بخش ویرایش تصویر — تست و اصلاح

### مشکلات پیدا و رفع‌شده

```text
۱) با بازشدن ویرایشگر، صفحه‌کلید از فیلد متن قبلی باز می‌ماند؛
   در دستگاه‌های کوچک دکمه‌های تأیید/انصراف زیر صفحه می‌رفتند.
   → با بازشدن پنجره، focus پاک می‌شود و صفحه‌کلید بسته می‌شود.

۲) ارتفاع پنجره از ۹۲٪ صفحه بیشتر نمی‌شود، محتوا در صورت نیاز اسکرول
   می‌شود و imePadding رعایت است؛ تأیید/انصراف همیشه در دسترس‌اند.

۳) هندسهٔ برش (چرخش، مربع‌بودن، مرکز، اندازه) به شیء خالص CropGeometry
   منتقل و با تست واقعی ریاضی JVM تضمین شد:
   - مربع واقعی در پیکسل برای هر ابعاد و هر چرخش؛
   - مرکز همیشه داخل تصویر؛
   - حداقل/حداکثر اندازهٔ ضلع؛
   - جابه‌جایی مرکز هنگام کشیدن هر ضلع؛
   - تخمین حجم برابر مساحت واقعی برش.
```

فایل جدید: `app/src/main/java/ir/exam/app/ui/image/CropGeometry.kt`

## ۵) پنجره گروهی

```text
فهرست کلاس‌ها از پنجره حذف شد.
وقتی پنجره از داخل یک کلاس باز شود، هیچ نشانی از کلاس‌ها ندارد.
فقط وقتی از منوی اصلی (+) باز شود، یک انتخاب‌گر تک‌خطی کلاس دیده می‌شود
(چون ساخت دانش‌آموز بدون کلاس ممکن نیست).
شمارهٔ کارت در حال ویرایش همیشه بالای کارت نمایش داده می‌شود:
«دانش‌آموز ۲ از ۵ ✓» با دکمه‌های قبلی/بعدی — بدون هیچ اسکرولی.
```

## امنیت رمز دانش‌آموز

بدون تغییر: رمز قبلی Supabase Auth غیرقابل بازیابی است، `plain_password`
بازنمی‌گردد و فقط رمز جدید ثبت‌شده یک‌بار با Clipboard حساس قابل کپی است.

## فایل‌های کلیدی

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

## تست

```text
Kotlin compile             PASS
JVM tests                  216/216 PASS
V30 regression tests        17/17 PASS
CropGeometry math tests      5/5 PASS
FINAL_NATIVE_VERIFY        PASS
lintDebug                  PASS — 0 error
assembleDebug              PASS
Debug package              ir.exam.app.native
APK Signature Scheme v2    Verified
```

SQL/Edge/Secret/Dependency جدید ندارد.
