# پچ جامع V7 — رسانه، Matching، پاسخ تصویری و بانک سؤال

## انجام‌شده

### Matching
- ساخت و ویرایش ردیف‌های چپ و راست
- افزودن و حذف ردیف
- تعیین اتصال صحیح هر مورد چپ به راست
- تصویر مستقل برای هر دو ستون
- حفظ و تبدیل ساختار WebView شامل `leftItems/rightItems/matchAnswer`
- پاسخ دانش‌آموز و ارسال map واقعی

### تصاویر گزینه و سؤال
- Photo Picker بدون مجوز گسترده گالری
- مجوز پایدار URI برای ذخیره draft
- نمایش، تعویض و حذف تصویر گزینه
- آپلود در `option_images/{teacherId}/{examId}`
- آپلود matching در `matching/{teacherId}/{examId}`
- جلوگیری از آپلود دوباره URLهای قبلی
- اصلاح جهت EXIF
- resize تا 2200px و فشرده‌سازی WebP/JPEG

### پاسخ تصویری دانش‌آموز
- حالت غیرفعال، اختیاری یا اجباری برای هر سؤال
- حداکثر ۱ تا ۱۰ تصویر
- ذخیره پاسخ متنی و URI تصاویر در Room
- سازگاری با JSON draft قدیمی
- آپلود در `answers/{studentId}/{examId}/{questionId}`
- ارسال `p_images` و `p_meta` واقعی به `submit_answer`
- جلوگیری از ارسال نهایی در صورت نبود تصویر اجباری

### بانک سؤال
- فهرست واقعی با `bank_list`
- ذخیره سؤال با `bank_add`
- حذف با `bank_del`
- ذخیره answer key همراه سؤال بانک
- افزودن به آزمون با id جدید برای جلوگیری از تداخل

## SQL

این پچ SQL جدید ندارد و از RPCهای زنده موجود استفاده می‌کند:

```text
bank_list
bank_add
bank_del
submit_answer
```

## محدودیت شفاف

- crop تعاملی هنوز متصل نیست؛ rotate بر اساس EXIF و resize/compress واقعی است.
- پاک‌سازی خودکار orphanهای Storage نیازمند reference counting است.
- دسته‌بندی پیشرفته بانک سؤال در این UI نیست؛ list/add/delete واقعی است.

## تست

```text
Kotlin compile                    PASS
Matching codec round-trip         PASS
Answer key public separation      PASS
Required image validation         PASS
Room draft new/legacy JSON tests   2/2 PASS
JVM tests                         17/17 PASS
assembleDebug                     BUILD SUCCESSFUL
lintDebug                         BUILD SUCCESSFUL
APK Signature Scheme v2           Verified
```
