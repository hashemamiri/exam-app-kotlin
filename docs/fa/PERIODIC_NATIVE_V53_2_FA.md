# V53.2 — جدول تناوبی کاملاً Native

**پیش‌نیاز:** V53.1 + V53.1.1 (اعمال و build شده)
**SQL / Edge Function / Secret / Migration / Dependency جدید:** ندارد

---

## ۱) دامنه

آیکن «درج جدول تناوبی» (که در V53.1 نیتیو شد) اکنون به‌جای بازکردن ابزار مرجع
داخل WebView، یک ویرایشگر کاملاً Native باز می‌کند. رندر دانش‌آموز و چاپ/PDF نیز
در همین مرحله Native شد.

## ۲) داده

- `PeriodicElements.kt` — استخراج **برنامه‌ای** (نه رونویسی دستی) هر ۱۱۸ عنصر از
  آرایهٔ `EL` ماژول `periodic-fig-js` مرجع: عدد اتمی، نماد، نام فارسی، گروه،
  دوره، دسته. دوره‌های دادهٔ ۸ و ۹ همان دو ردیف لانتانید/اکتینید مرجع‌اند.
- ۱۱ دستهٔ رنگی (`CATS`) و نام فارسی دسته‌ها (`CN`) عیناً حفظ شدند.

## ۳) قرارداد داده — بدون تغییر نسبت به مرجع

```text
{k:'p', t:preset, X:{title, Z, hid[], hidZ[], hideCols[], hideRows[], hideF}}
presets: full / main / noF / noZ
```

توکن‌های ساخته‌شده با WebView قدیمی در Native باز/رندر می‌شوند و برعکس.

## ۴) رندر

- `PeriodicSvgRenderer.kt` — SVG امن (`svg/rect/text` فقط): شبکه ۱۸×۷ +
  سرستون گروه با ارقام فارسی + شماره دوره + خانه‌های رنگی دسته + عدد اتمی
  اختیاری + ستارهٔ `*`/`**` خانهٔ گروه ۳ دوره‌های ۶/۷ + بلوک جداگانه لانتانید/اکتینید.
- عنصر حذف‌شده مثل مرجع (`is-off`) خانهٔ خاکستری خالی می‌شود؛ ستون/سطر حذف‌شده
  کلاً رسم نمی‌شود.
- مسیر مشترک: `FigureSvgRenderer.render` برای `k='p'` به این رندرگر می‌سپارد؛
  بنابراین Builder، آزمون دانش‌آموز (`NativeMathText`) و PDF
  (`figureBitmap` + AndroidSVG از V53.1) بدون هیچ تغییر دیگری کار می‌کنند.
- پلاک موقت V53.1 اکنون فقط برای `k∈{a,s}` است.

## ۵) ویرایشگر Native

`PeriodicEditorDialog.kt`:

```text
چهار حالت مرجع: کامل / گروه اصلی / بدون f / بدون عدد اتمی
دو حالت لمس: حذف عنصر / حذف عدد اتمی
لمس سرستون گروه (۱..۱۸) → حذف/بازگردانی ستون
لمس شماره دوره (۱..۷)   → حذف/بازگردانی سطر
سوییچ نمایش عدد اتمی و لانتانید/اکتینید
chipهای بازگردانی تک‌مورد (گروه/دوره/عنصر/Z) + «بازگردانی همه»
عنوان دلخواه
```

«گروه اصلی» مثل مرجع گروه‌های ۳..۱۲ را حذف و بلوک f را مخفی می‌کند.

خروجی از طریق `insertFigureJson` در محل مکان‌نمای کادر WebView درج می‌شود
(همان مسیر V53.1 جدول؛ با همان fallback به ViewModel).

## ۶) فایل‌ها

```text
app/src/main/java/ir/exam/app/core/figure/PeriodicElements.kt        (جدید)
app/src/main/java/ir/exam/app/core/figure/PeriodicSvgRenderer.kt     (جدید)
app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt      (جدید)
app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt              (buildPeriodic/xIntList)
app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt       (مسیر k='p')
app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt   (onInsertPeriodic)
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt        (periodicTarget/دیالوگ)
app/src/test/java/ir/exam/app/ui/app/V53_2PeriodicNativeTest.kt      (جدید)
scripts/verify_native_final.py                                       (قرارداد V53.2)
text/CHANGELOG_FA.txt
docs/fa/PERIODIC_NATIVE_V53_2_FA.md                                  (جدید)
docs/fa/HANDOFF_KOTLIN_MIGRATION_FA.md
```

## ۷) محدودیت ثبت‌شده

- ویرایش دوبارهٔ توکن تناوبیِ موجود هنوز با دوبار-کلیک داخل WebView به ابزار
  مرجع می‌رود (همان رفتار V53.1)؛ اتصال دوبار-کلیک به ویرایشگرهای Native در
  V53.3 همراه آناتومی/فیزیک/شیمی تحویل می‌شود.

## ۸) مرحلهٔ بعد

```text
V53.3 → آناتومی + فیزیک/شیمی Native (اطلس تصاویر asset + ویرایشگر + رندر)
        + اتصال ویرایش دوبار-کلیک توکن‌ها به ویرایشگرهای Native + رگرسیون کل
```
