# V50.16 — کادر ساختاری فرمول، صددرصد Canvas نیتیو

کادر ساختاری فرمول از WebView/HTML/JavaScript جدا شد. اکنون فرمول با
`NativeMathParser` به AST تبدیل، با `NativeMathCanvasRenderer` مستقیماً روی
Android Canvas رسم و لمس خانه‌ها در Compose به بازهٔ واقعی متن وصل می‌شود.

## تغییرات

- حذف کامل `android.webkit.WebView`، `JavascriptInterface` و `evaluateJavascript`؛
- حذف asset قدیمی `formula_canvas_frame.html`؛
- اضافه‌شدن `NativeMathCanvasEditorView` برای رسم و hit-test جعبه‌ها؛
- حفظ قرارداد `FormulaBoxEditor`، انتخاب خانه، کیپد، undo/redo و کتابخانه؛
- بدون SQL، Edge Function، Secret یا dependency جدید.

## اعتبارسنجی

```text
FINAL_NATIVE_VERIFY → PASS
WebView/HTML runtime → حذف شد
```

Build کامل Android باید در CI یا محیطی با cache وابستگی‌های Gradle اجرا شود.
