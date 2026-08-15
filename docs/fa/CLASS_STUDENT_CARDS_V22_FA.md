# V22 — منوی کلاس و کارت تعاملی دانش‌آموز

**تاریخ:** ۲۰۲۶-۰۸-۱۴

**پیش‌نیاز:** V21

## افزودن دانش‌آموز از + اصلی

انتخاب «دانش‌آموز جدید» مستقیماً پنجره افزودن گروهی را باز می‌کند. پنجره با یک ردیف آغاز می‌شود و ساخت فقط یک دانش‌آموز نیز مجاز است.

## کارت کلاس

نام کلاس و پایه در یک سطر قرار گرفتند.

داخل هر کلاس سه دکمه قدیمی حذف شدند و فقط یک + باقی است. لمس + دو کارت آویزان باز می‌کند:

```text
افزودن موجود
افزودن جدید
```

- افزودن موجود: لیست حساب‌های موجود با فیلتر همه/دختر/پسر و پایه؛ انتخاب چندنفره.
- افزودن جدید: پنجره گروهی ساختاریافته؛ تعداد 1 تا 100.

## کارت دانش‌آموز

- خلاصه: نام و نام خانوادگی + پایه در یک سطر.
- لمس کارت: باز/بسته‌شدن مستقل.
- رنگ دختر: صورتی نیمه‌شفاف.
- رنگ پسر: آبی نیمه‌شفاف.

کنترل‌های کارت باز:

```text
Toggle سبز = فعال
Toggle قرمز = غیرفعال
مداد = ویرایش
+ = افزودن به چند کلاس
```

+ کلاس‌ها را به‌صورت chip نشان می‌دهد و انتخاب یک یا چند کلاس با RPC اتمیک سروری ثبت می‌شود.

## ویرایش رمز

Supabase Auth رمز قبلی را hash می‌کند و امکان بازیابی یا نمایش آن وجود ندارد؛ `plain_password` نیز عمداً حذف شده است. جایگزین امن:

- فیلد «رمز جدید اختیاری» خالی است؛
- خالی بماند، رمز تغییر نمی‌کند؛
- رمز جدید 8 تا 72 نویسه یا رمز تاس‌ساخته ثبت می‌شود؛
- eye نمایش/پنهان‌کردن دارد؛
- سایر مشخصات هم‌زمان قابل ویرایش‌اند.

## عضویت چندکلاسه

SQL جدید:

```text
sql/manual/SQL_NATIVE_STUDENT_MULTI_CLASS_V22.sql
native_add_student_to_classes_v22(uuid,jsonb)
```

- مالکیت دانش‌آموز و همه کلاس‌ها با `auth.uid()` بررسی می‌شود.
- همه کلاس‌ها پیش از اولین INSERT اعتبارسنجی می‌شوند.
- INSERT در یک تراکنش و با `on conflict do nothing` است.
- public/anon دسترسی ندارند و فقط authenticated مجاز است.

Readiness:

```text
student_multi_class_ready = true
```

## منوی همبرگری

جای کارت دانش‌آموزان و تقویم عوض شد. ترتیب ابتدای منو:

```text
دانش‌آموزان
کلاس‌ها
تقویم و پیام‌ها
```

## تست

```text
Kotlin compile                         PASS
JVM tests                              124/124 PASS
V22 SQL parser                         PASS — 6 statements
V22 test SQL parser                    PASS — 4 statements
Unsafe DML                             0
Quick-to-bulk regression               PASS
Class hanging-menu regression          PASS
Student card/color/icon regression     PASS
Optional password security regression  PASS
Multi-class owner RPC regression       PASS
Hamburger order regression             PASS
FINAL_NATIVE_VERIFY                   PASS
lintDebug                  PASS — 0 error, 22 warning
assembleDebug                         PASS
APK Signature Scheme v2               Verified
Debug APK SHA-256                     0ea902fe1eb9f6c42b9f32a82dfc733016583afca8f26ba08994d0e01b538b65
```

## عملیات

```text
SQL جدید: بله
Edge deploy: ندارد
Secret/Dependency جدید: ندارد
```
