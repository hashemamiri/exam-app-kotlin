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

## مرحله ۷ — PDF رسمی و موتور مستقل چاپ
- `OfficialPrintLayoutEngine` در `OfficialPdfPrintAdapter.kt`: خروجی PDF رسمیِ
  A4 برای آزمون و کارنامه؛ سربرگ فقط صفحهٔ ۱ و پاصفحه روی همهٔ صفحه‌ها رسم
  می‌شود.
- `PrintFigureMetrics.kt` و `PrintTextSpanSegments.kt`: دادهٔ اندازه/جای شکل و
  استایل متن را فقط از مدل domain به PDF می‌رسانند؛ موتور PDF به state یا ابزار
  رابط کاربری وابسته نیست.
- `NativeExamPdfDocument.kt`: یک PDF A4 بومی و immutable را با
  `OfficialPrintLayoutEngine` می‌سازد. `NativeExamPdfPrintAdapter` دقیقاً همان
  فایل را به Android Print Framework می‌دهد؛ مسیر چاپ آزمون هیچ WebView یا
  HTML ندارد.
- `NativeExamPdfPreviewDialog.kt`: صفحه‌های همان فایل را با `PdfRenderer`
  نمایش می‌دهد. overlay کاملاً Native روی PDF، gesture جابه‌جایی/تغییر اندازهٔ
  شکل، فاصلهٔ بین سؤال‌ها و بازکردن ابزار بومی ویرایش شکل را نگه می‌دارد.
- `PrintPreviewLayoutCodec.kt`: وضعیت پایدار شکل‌ها را با میلی‌متر PDF ذخیره و
  چیدمان‌های پیکسلیِ قدیمی را فقط هنگام خواندن به قالب جدید منتقل می‌کند.
- `PrintHeaderSettings.kt` و `header_settings_schema.json`: تنظیمات مشترک هفت
  قالب سربرگ در سازندهٔ بومی و renderer PDF بومی.
- `OfficialPrintModels.kt` و `PrintableFromDrafts.kt`: مدل canonical چاپ و
  تبدیل دادهٔ سازندهٔ بومی به آن مدل.

قانون اصلی: ساخت و ویرایش آزمون فقط در سازندهٔ بومی انجام می‌شود. مسیر چاپ
نه renderer موازی و نه ویرایشگر سندِ جداگانه دارد؛ فقط مدل canonical را یک‌بار
به PDF رسمی می‌دهد و preview/Print Framework همان فایل را مصرف می‌کنند.
`PrintPreviewLayoutCodec` صرفاً دادهٔ ذخیره‌شدهٔ قدیمی را هنگام خواندن مهاجرت
می‌دهد و renderer تازه‌ای نیست. فونت فارسی PDF `B Nazanin` از
`assets/fonts/bnazanin.ttf` بارگذاری می‌شود و در نبود آن وزیرمتن استفاده می‌شود.

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
