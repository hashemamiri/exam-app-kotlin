# V13.5 — رندر SVG نمادها و ویرایشگر فرمول

**تاریخ:** ۲۰۲۶-۰۸-۱۲

## درخواست اصلاحی

نمادها و فرمول‌ها نباید به‌شکل کد خام TeX مانند `\frac`، `\Delta` یا نام دستور ناشناخته در رابط دیده شوند. نمایش دیداری باید در این سه ناحیه SVG باشد:

1. کتابخانه و گالری نمادها؛
2. دکمه‌ها، منوهای سریع و keypad؛
3. کادر فرمول‌نویسی، پیش‌نمایش و تایپ سریع.

## پیاده‌سازی

### مسیر امن رندر

```text
TeX داخلی
   ↓
NativeMathParser
   ↓
MathNode AST بسته
   ↓
NativeMathSvgRenderer
   ↓
SVG مستقل و XML-escaped
   ↓
Coil SvgDecoder / AndroidSVG
   ↓
Compose UI
```

- `NativeMathSvgRenderer.kt` سند SVG واقعی با `viewBox` و اندازهٔ مستقل تولید می‌کند.
- کسر، خط کسری، رادیکال، توان/زیرنویس، ماتریس، دلیمتر کشیده و accent با elementهای برداری SVG رسم می‌شوند.
- glyphهای عددی، حرفی، یونانی و Unicode داخل همان سند SVG قرار می‌گیرند.
- SVG فقط از elementهای بسته و کنترل‌شدهٔ `svg`, `g`, `text`, `path`, `line`, `circle` ساخته می‌شود.
- رنگ فقط با قالب امن `#RRGGBB` پذیرفته می‌شود.
- متن کاربر پیش از ورود به XML escape می‌شود.
- URL خارجی، `href`، `src`، `script`، `foreignObject` و JavaScript تولید نمی‌شود.
- فایل یا شبکه برای ساخت SVG لازم نیست و سند در حافظه ساخته می‌شود.

### رابط

- `NativeFormulaView` دیگر AST را با `Text`های Compose نمایش نمی‌دهد؛ SVG تولیدشده را decode می‌کند.
- `NativeFormulaIcon` برای آیکون ثابت SVG در کارت کتابخانه، منو، دکمه و keypad اضافه شد.
- کارت‌های همهٔ نمادها، Unicode 1200، علاقه‌مندی و اخیر SVG هستند.
- گزینه‌های منوی کسر، توان، رادیکال، لگاریتم، انتگرال، درصد و مثلثات SVG هستند.
- دکمه‌های ریاضی ردیف سریع و کلیدهای عدد/عملگر keypad SVG هستند.
- گالری آماده و نتیجهٔ تایپ سریع SVG هستند.
- کادر ساختاری به ویرایشگر تصویری SVG تبدیل شد؛ ورودی صفحه‌کلید به‌شکل نامرئی فقط state داخلی را تغییر می‌دهد.
- کد TeX فقط پس از انتخاب صریح «کد فرمول (کاربران حرفه‌ای)» نمایش داده می‌شود.
- نام فرمول اخیر دیگر از کد خام ساخته نمی‌شود.
- نمایش سؤال/گزینه برای معلم و دانش‌آموز نیز از همین SVG عبور می‌کند؛ حتی فرمول‌های سادهٔ `$\\alpha$` یا `$\\times$` دیگر مسیر Text/AnnotatedString ندارند.

### parser

- همهٔ دستورهای موجود در ۲۰۸۴ ورودی دسته‌بندی‌شده و ۳۴ فرمول گالری audit شدند.
- فرمان‌های تکمیلی یونانی، تابع، جهت، مجموعه، دلیمتر و عملگر به glyph دیداری نگاشت شدند.
- `\\` به سطر تازهٔ ساختاری تبدیل می‌شود.
- `\left ... \right` به node دلیمتر ساختاری تبدیل می‌شود.
- دستور ناشناخته هرگز نام خام خود را در UI نشان نمی‌دهد و با جایگزین دیداری امن نمایش داده می‌شود.

## داده و ذخیره‌سازی

- TeX همچنان قالب داخلی و قابل‌ویرایش ذخیره/درج است؛ این داده در حالت عادی رابط نمایش داده نمی‌شود.
- JSON مرجع تغییر ماهوی نکرد و JavaScript مرجع داخل APK اجرا نمی‌شود.
- SVGها هنگام نمایش از AST ساخته و با کلید پایدار در memory cache نگه‌داری می‌شوند.
- PDF همچنان از همان AST و Canvas برداری امن استفاده می‌کند.

## تست قطعی

```text
Kotlin compile                         PASS
JVM tests                              64/64 PASS
All reference formulas SVG generation 2118/2118 PASS
Unicode library                         1200 PASS
Unknown command raw-code leak               0 PASS
SVG XML validity/security                  PASS
Formula UI raw Text(entry.tex)              0 PASS
FINAL_NATIVE_VERIFY                       PASS
lintDebug                         PASS — 0 error, 24 warning
assembleDebug                             PASS
APK Signature Scheme v2               Verified
Coil SvgDecoder packaged                  PASS
AndroidSVG packaged                       PASS
```

هشدارهای lint مربوط به موارد قدیمی و اعلان نسخه‌های جدید dependency هستند؛ خطای lint وجود ندارد.

## عملیات استقرار

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration دیتابیس: ندارد
پیش‌نیاز: V13.4
```

پس از Build در GitHub Actions، تست دستگاه باید کتابخانه، منوها، keypad، کادر SVG، تایپ سریع، گالری، نمایش سؤال/گزینه و حالت روشن/تیره را پوشش دهد.
