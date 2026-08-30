# شروع Native Kotlin

این پوشه شروع مرحلهٔ اول مهاجرت است. مسیر: `exam-app-kotlin/`.

## چیزهای پیاده‌سازی‌شده
- پروژه Android Kotlin با package اصلی `ir.exam.app`
- Jetpack Compose و Material 3
- Navigation مرکزی
- مدل اولیه کاربر و نقش‌ها
- پوستهٔ مرکزی
- Activity Native بدون WebView
- صفحه ورود و داشبورد اولیه

## ارتباط با برنامه قبلی
- `android-app/src/ir/exam/app/MainActivity.java` به تدریج با `MainActivity.kt` جایگزین می‌شود.
- `src/js/auth.js` در فاز Auth به `ui/auth`، `domain` و `data` منتقل می‌شود.
- `src/js/builder.js` در فاز آزمون‌ساز به `ui/builder` منتقل می‌شود.

## اجرای محلی
پروژه را با Android Studio باز کنید و Gradle Sync را اجرا کنید. سپس configuration `app` را اجرا کنید.

## مرحله ۲ — ورود و OTP
- `data/remote/SupabaseProvider.kt`: اتصال امن با anon key
- `domain/repository/AuthRepository.kt`: قرارداد مستقل از Supabase
- `data/repository/SupabaseAuthRepository.kt`: ورود رمز و OTP
- `ui/auth/AuthViewModel.kt`: state، اعتبار جریان و خطاها

### تنظیم محرمانه
در فایل `local.properties` که نباید commit شود:
```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```
هرگز service_role را در APK یا GitHub قرار ندهید.

## مرحله ۳ — آزمون دانش‌آموز
- مدل‌های Native آزمون، سؤال و پاسخ در `domain/model/ExamModels.kt`
- قرارداد دریافت آزمون و ثبت پاسخ در `domain/repository/ExamRepository.kt`
- قرارداد ذخیرهٔ پیش‌نویس آفلاین در `AnswerDraftRepository.kt`
- تایمر، ورود با کد، ناوبری سؤال و ثبت نهایی در `StudentExamViewModel.kt`
- UI پایهٔ پاسخ تشریحی و چهارگزینه‌ای در `StudentExamScreen.kt`

ارتباط: `StudentExamViewModel` از `ExamRepository` برای Supabase و از `AnswerDraftRepository` برای Room استفاده می‌کند. در مرحلهٔ بعد، پیاده‌سازی واقعی Repositoryها با Supabase و Room افزوده می‌شود.

## مرحله ۴ — Room، پیش‌نویس آفلاین و لایهٔ شبکه
- `data/local`: Entity، DAO و AppDatabase برای پاسخ‌های موقت.
- `RoomAnswerDraftRepository`: ذخیره و بازیابی پاسخ‌ها، خارج از UI.
- `NetworkMonitor`: تشخیص اینترنت پیش از ارسال.
- `QueuedExamRepository`: نقطهٔ مرکزی کنترل ارسال؛ در مرحله‌های بعد WorkManager و صف PendingAction به آن افزوده می‌شود.

ارتباط: `StudentExamViewModel` فقط با قراردادهای domain کار می‌کند. Room، شبکه و Supabase همگی زیر Repository پنهان هستند.

## مرحله ۵ — آزمون‌ساز معلم
- `QuestionDraft.kt`: مدل draft و نوع‌های سؤال.
- `ExamBuilderViewModel.kt`: افزودن، ویرایش، حذف و جابه‌جایی سؤال‌ها.
- `ExamBuilderScreen.kt`: فرم Compose آزمون و ویرایشگر پایه.

این مرحله جایگزین منطق پایهٔ `builder.js` است. تصویر، قلم، جای‌گذاری آزاد و PDF عمداً به مراحل ۶ و ۷ جدا شده‌اند تا state آزمون‌ساز پایدار بماند.

## مرحله ۶ — تصویر، ویرایش و جای‌گذاری آزاد
- `QuestionImage`: شناسه، URI، مسیر ابری، وضعیت ویرایش و مختصات مستقل هر تصویر.
- `A4ImagePositioner`: مختصات را با میلی‌متر A4 نگه می‌دارد؛ افزایش سطر متن موجب حرکت تصویر نمی‌شود.
- `ImageRepository`: آماده‌سازی و آپلود را جدا می‌کند؛ از بازشدن دوبارهٔ editor جلوگیری می‌شود.
- `ImageEditorViewModel`: state برش، چرخش، تأیید و لغو.
- `FreeImageCanvas`: drag هر تصویر را فقط روی همان شناسه اعمال می‌کند.

در مرحلهٔ ۷، پیاده‌سازی Bitmap/Exif، آپلود واقعی Supabase Storage و PDF A4 به این قراردادها متصل می‌شوند.

## مرحله ۷ — موتور واحد سند و چاپ (WYSIWYG)
- `UnifiedDocumentEngine` در `OfficialPdfPrintAdapter.kt`: یک چیدمان واحد برای
  چاپ رسمی و ویرایشگر سند؛ سربرگ فقط صفحهٔ ۱، امضا فقط صفحهٔ آخر.
- `OfficialPrintController`: چاپ از مسیر PrintManager (گفتگوی چاپ سیستم).
- `ExamDocumentEditorScreen.kt`: ویرایشگر سند Word-مانند با صفحه‌بندی واقعی A4.
- `WordPageLayout.kt`: ثابت‌های A4 و حاشیهٔ 14mm و helperهای تبدیل mm/pt.
- `OfficialPrintModels.kt`: مدل‌های چاپی مستقل از Compose/View.

قانون اصلی: پیش‌نمایش و ویرایشگر layout جداگانه ندارند؛ هر دو از همان موتور
واحد استفاده می‌کنند. فونت فارسی چاپ `B Nazanin` از `assets/fonts/bnazanin.ttf`
بارگذاری می‌شود و در نبود آن وزیرمتن.

## مرحله ۸ — تصحیح، بازخورد و گزارش
- `AutoGrader`: قرارداد تصحیح خودکار به‌ازای هر نوع سؤال.
- `MultipleChoiceAutoGrader` و `NumericAutoGrader`: نمونه‌های جدا و تست‌پذیر.
- `GradeExamUseCase`: ترکیب تصحیح خودکار با صف سؤال‌های تشریحی.
- `GradingViewModel`: نمره و بازخورد دستی با محاسبهٔ مجدد نمرهٔ کل.
- `ReportsViewModel`: فهرست کارنامه‌ها و جست‌وجوی دانش‌آموز.

## مرحله ۹ — کلاس، تقویم، پیام، کیف پول و اشتراک
- مدل‌های کلاس، دانش‌آموز، رویداد تقویم، کیف پول و اشتراک.
- قراردادهای `SchoolRepository` و `BillingRepository` برای Supabase/Edge Function.
- ViewModelهای مستقل برای کلاس‌ها، تقویم و پرداخت.
- پرداخت داخل APK نباید کلید درگاه داشته باشد؛ فقط URL یا token امن از Edge Function دریافت می‌شود.
