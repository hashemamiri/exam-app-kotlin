# V13.4 — همسان‌سازی بخش فرمول با مرجع پیوست

## مبنا
چهار فایل پیوست کاربر بدون حدس بررسی شدند:

```text
index.html                         7a33056ad2604bdcca2329f3cba15404df0e9c32180ed9315b941fc169d89c9f
مرجع-کامل-بخش-فرمول.md           b65de8d6285ed60ebc50a1464547a8a268c864cc64083add4ccf5ce5e58e4936
کد-خام-بخش-فرمول.txt             4986c0dff5781ad907cb286040186f1256f7820448564ba850e5d0105d4ac7ea
فقط-html-بخش-فرمول.txt           5f9b14573d49756bb554598665da83bd79b90b10129091837cb7152006d35b83
```

## ترتیب UI
1. عنوان و سه حالت: جعبه‌ای، تایپ سریع، آماده
2. راهنما
3. Undo، Redo، Copy، Paste، A−، A+
4. پیش‌نمایش و کادر ساختاری
5. ردیف دسته‌ها دقیقاً با ترتیب مرجع
6. ردیف شش‌تایی: اخیر، تبدیل، log، انتگرال، درصد، sin
7. درج، سطر تازه، abc، کسر، توان، رادیکال
8. صفحه‌کلید ثابت چهارردیفی با همان ترتیب
9. جست‌وجوی نماد
10. کتابخانه/تب انتخاب‌شده
11. کد فرمول
12. حالت تایپ سریع با همان مثال‌ها
13. گالری آماده با گروه‌های مرجع
14. درج در سؤال، پاک، انصراف

## کتابخانه مرجع Native
تعریف‌های `MB_PAD`, `CURRICULUM_PAD`, `PHYSCHEM_PAD`, `EXTRA_PAD`, `NSC_MATH_PADS`, `NSC_PHYS_PADS`, `MB_GROUPS` و `MF_GALLERY` از مرجع به داده امن Native تبدیل شدند:

```text
گروه اصلی                 8
دسته                       77
ورودی دسته‌بندی‌شده       2084
نماد Unicode               1200
فرمول گالری                34
```

Asset نهایی:
```text
app/src/main/assets/formula_library_v13.json
```

هیچ JavaScript یا WebView اجرا نمی‌شود. داده‌ها با kotlinx.serialization خوانده و با Compose/Canvas Native نمایش داده می‌شوند.

## قابلیت‌ها
- دسته‌ها و زیرگروه‌ها با همان نام و ترتیب مرجع
- همه نمادها، Unicode 1200، جست‌وجو، علاقه‌مندی و اخیر
- منوهای کسر/توان/رادیکال/log/انتگرال/درصد/مثلثات
- حروف کوچک/بزرگ
- keypad دقیق مرجع
- گالری هندسه، جبر، مثلثات، آمار و آنالیز
- تبدیل طبیعی نمونه‌های `7/8`, `(a+b)/2`, `sqrt2`, `رادیکال ۵`, `pi`, `>=`, `!=`, `*`
- ریشه با فرجه، انتگرال‌های چندگانه، فلش‌ها، مجموعه‌ها، یونانی، فیزیک و شیمی
- رندر Compose و PDF از AST مشترک

## تست
```text
Kotlin compile                    PASS
JVM tests                         56/56 PASS
Formula reference asset tests    PASS
UI order regression              PASS
Unicode exact count              1200 PASS
Native quick conversion          PASS
Native parser/indexed root       PASS
FINAL_NATIVE_VERIFY              PASS
lintDebug                         PASS (0 error)
assembleDebug                     PASS
APK Signature Scheme v2           Verified
```

SQL، Edge Function و Secret جدید لازم نیست.
