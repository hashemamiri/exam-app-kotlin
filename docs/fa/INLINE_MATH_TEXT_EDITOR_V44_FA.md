# V44 — فرمول درون‌متنی در متن سؤال (هم‌رفتار وب‌اپ)

## درخواست کاربر

```text
وقتی فرمول در متن سؤال درج می‌شود → همان‌جا به‌صورت نماد نمایش داده شود، نه کد $...$
لمس نماد فرمول → بازشدن دوبارهٔ ویرایشگر فرمول
نمایش فرمول → بدون نیاز به کادر/فهرست جداگانه
```

## وضعیت قبلی (Kotlin)

```text
OutlinedTextField          → متن خام با کد $...$
NativeMathText            → پیش‌نمایش جداگانه زیر فیلد
ExistingFormulaEditor     → کادر جداگانه با دکمه ویرایش/حذف
```

## رفتار هدف (وب‌اپ / کامپوننت QMF)

```text
یک سطح ویرایش واحد
فرمول‌ها همان‌جا داخل متن به‌صورت نماد رندر می‌شوند
لمس نماد → ویرایشگر فرمول با همان TeX باز می‌شود
```

## تغییرات

### فایل جدید

```text
app/src/main/java/ir/exam/app/ui/math/InlineMathTextEditor.kt
```

- متن را به بخش‌های متناوب `Part.Plain` / `Part.Formula` می‌شکند.
- فرمول‌ها به‌صورت chip با `NativeFormulaIcon` (SVG) داخل متن نمایش داده می‌شوند.
- لمس نماد → `onEditFormula(occurrenceIndex, tex)`.
- دکمه × روی هر فرمول → `onDeleteFormula(occurrenceIndex)`.
- دکمه ∑ انتهای سطر → `onInsertFormula`.
- متن عادی بین و اطراف فرمول‌ها با `BasicTextField` درون‌متنی قابل تایپ است.
- `$` تایپ‌شده در متن عادی حذف می‌شود تا ساختار `$...$` فقط از مسیر ویرایشگر فرمول ساخته شود.

### فایل تغییرکرده

```text
app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt
```

سه جزء قبلی (OutlinedTextField + NativeMathText + ExistingFormulaEditor) با یک
`InlineMathTextEditor` واحد جایگزین شد. باز/ویرایش/حذف از همان متدهای ViewModel
موجود استفاده می‌کنند.

### بدون تغییر

```text
فرمت ذخیره‌سازی $...$ (سازگار با بک‌اند، چاپ/PDF و پیش‌نمایش)
لایه داده (FormulaTextCodec / Repository / ViewModel)
SQL / Edge Function / Secret / Migration / Dependency
```

## عملیات

```text
SQL جدید: ندارد
Edge Function جدید: ندارد
Secret جدید: ندارد
Migration جدید: ندارد
Dependency جدید: ندارد
پیش‌نیاز: V43
```

## بررسی

```text
git diff --check                        → PASS
FINAL_NATIVE_VERIFY                    → باید در WSL اجرا شود
testDebugUnitTest / lintDebug / assembleDebug → باید در WSL/CI اجرا شود
```

## نکته

گزینه‌ها (options) و matching فعلاً از رفتار قبلی (OutlinedTextField +
NativeMathText + ExistingFormulaEditor) استفاده می‌کنند. یکسان‌سازی آن‌ها با همین
کامپوننت در پچ بعدی قابل انجام است.
