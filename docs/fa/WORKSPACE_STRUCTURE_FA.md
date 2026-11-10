# ساختار پوشه‌های پروژه و ورک‌اسپیس

## داخل repository

```text
docs/fa/                 مستندات فارسی، هندآف جاری و تحلیل‌ها
sql/manual/              نسخه‌های دستی SQL قابل کپی در Supabase SQL Editor
supabase/migrations/     migrationهای واقعی و ترتیبی دیتابیس
supabase/sql/            فایل‌های نصب/بررسی SQL نسخه‌ها
supabase/functions/      Edge Functionها
text/                    فایل‌های متنی انتشار و changelog
app/                     سورس و تست Android
scripts/                 ابزارهای verify، آزمون و ساخت
.github/workflows/       CI/CD
site/                    سورس و bundleهای سایت
```

فایل‌های زیر عمداً در ریشهٔ مخزن می‌مانند چون Gradle/Git آن‌ها را در همان مسیر لازم دارد:

```text
build.gradle.kts
settings.gradle.kts
gradle.properties
gradlew
gradlew.bat
.gitignore
```

هندآف عملیاتی فعلی:

```text
docs/fa/HANDOFF_CURRENT_FA.md
```

هندآف‌های قدیمی‌تر تاریخی‌اند و نباید مبنای ادامهٔ کار قرار گیرند.

## بیرون repository

ورک‌اسپیس باید lean/source-only بماند. در ریشهٔ `/home/user` فقط این موارد مجازند:

```text
exam-app-kotlin/            مخزن منبع
apply_v*.py                 نصاب فعالِ هنوز اعمال‌نشده، اگر باشد
gh_storage_cleanup.sh       ابزار موقت پاک‌سازی فضا، تا اجرای موفق
reference_bank_publish.zip  شاردها و تصاویر آمادهٔ آپلود
samples/                    نمونهٔ محتوای کاربر؛ خارج از مخزن و باندل
```

قواعد:

- SQL مرجع داخل مخزن است؛ کپی ریشه فقط در صورت نیاز موقت و باید پس از اعمال/جایگزینی پاک شود.
- نصاب جایگزین‌شده پس از اعمال نسخهٔ جدید پاک شود.
- لاگ آپلودی، اسکرین‌شات موقت، `node_modules`، `.gradle`، `build` و خروجی آزمون نباید در ورک‌اسپیس بمانند.
- پوشه‌های قدیمی `patches/built` و `patches/pending` دیگر سیاست جاری نیستند؛ نصاب‌های خوداستخراج‌گر جایگزین آن‌ها شده‌اند.
- فایل‌های ردیابی‌شدهٔ مخزن، از جمله bundleهای سایت، بدون بررسی جداگانه پاک نشوند.

نمونهٔ محتوا فقط در `samples/` نگهداری شود. هیچ فایل بانک سؤال، تصویر یا JSON حجیمی وارد `app/src/main/assets`، `site/` یا باندل استقرار نشود. نگهبان `check_reference_bank_bundle.py` در V360 ساخته شد و در CI این قید را پایش میکند.
