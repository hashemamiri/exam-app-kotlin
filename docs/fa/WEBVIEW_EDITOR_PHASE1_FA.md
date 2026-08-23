# مرحلهٔ ۱ ادغام WebView ویرایشگر سؤال

## وضعیت

مرحلهٔ ۱ تکمیل شد. این مرحله فقط POC مستقل است و هنوز به آزمون‌ساز اصلی متصل نشده است.

## فایل‌ها

```text
app/src/main/assets/question_editor/question_editor.html
app/src/main/assets/question_editor/version.txt
app/src/main/java/ir/exam/app/ui/math/QuestionEditorWebView.kt
app/src/test/java/ir/exam/app/ui/math/QuestionEditorWebViewPocTest.kt
```

## رفتار

- HTML به‌صورت local از assets بارگذاری می‌شود.
- اسکریپت Cloudflare و وابستگی challenge از نسخهٔ اپ حذف شده است.
- WebView از origin کنترل‌شدهٔ `https://exam-editor.local` استفاده می‌کند.
- منابع فقط از مسیر `question_editor/` در assets خوانده می‌شوند.
- navigation خارجی مسدود است.
- مسیرهای دارای `..` پذیرفته نمی‌شوند.
- JavaScript و DOM storage برای خود editor فعال‌اند؛ دسترسی فایل و محتوای خارجی بسته است.
- bridge فقط callbackهای متن، آماده‌شدن و خطا را دارد.
- هیچ دسترسی به Supabase، Auth، session، token یا Secret وجود ندارد.

## قرارداد فعلی POC

```text
Native → window.ExamEditor.setValue(value)
WebView → ExamEditorNative.onTextChanged(value)
WebView → ExamEditorNative.onReady()
WebView → ExamEditorNative.onError(code)
```

## عمداً انجام نشده

- اتصال به `ExamBuilderScreen`؛
- جایگزینی `FormulaEditorDialog`؛
- اتصال گزینه‌ها و Matching؛
- حذف مسیر Native؛
- ذخیره‌سازی در WebView؛
- دسترسی شبکه.

## معیارهای قبولی مرحلهٔ ۱

```text
asset local loading                 PASS by implementation
external navigation block           PASS by implementation
path traversal guard                 PASS by implementation
bridge callback contract             PASS by source tests
Supabase/token absence               PASS by source tests
Native rollback                      PASS — Native path unchanged
```

## Build

اجرای `./gradlew :app:compileDebugKotlin --no-daemon` در این workspace به خطای محیطی dependency متوقف شد و به کد POC مربوط نیست:

```text
Plugin com.google.devtools.ksp:2.0.21-1.0.28 was not found
```

قبل از ورود به مرحلهٔ ۲ باید این dependency در CI یا محیط دارای repository/cache معتبر حل شود و تست‌های Gradle روی دستگاه/CI اجرا شوند.

## مرحلهٔ بعد

مرحلهٔ ۲ فقط پس از تأیید build شروع می‌شود و ابتدا WebView را به **متن سؤال** متصل می‌کند؛ گزینه‌ها و Matching همچنان در آن مرحله نیز بعد از تست مستقل متصل خواهند شد.
