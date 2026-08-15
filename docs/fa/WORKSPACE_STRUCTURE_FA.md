# ساختار پوشه‌های پروژه و ورک‌اسپیس

## داخل repository

```text
docs/fa/                 همه فایل‌های Markdown فارسی و هندآف
sql/manual/              نسخه‌های دستی SQL قابل کپی در Supabase SQL Editor
supabase/migrations/     migrationهای واقعی و ترتیبی دیتابیس
supabase/functions/      Edge Functionها
text/                    فایل‌های متنی انتشار و changelog
app/                     سورس و تست Android
scripts/                 ابزارهای verify
.github/workflows/       CI/CD
```

فایل‌های زیر عمداً در ریشه می‌مانند چون Gradle/Git آن‌ها را در همان مسیر لازم دارد:

```text
build.gradle.kts
settings.gradle.kts
gradle.properties
gradlew
gradlew.bat
.gitignore
```

## بیرون repository

```text
/home/user/patches/built/      پچ‌های دارای build موفق
/home/user/patches/pending/    پچ‌های در انتظار build
```

پس از موفقیت build هر پچ، فایل آن از pending به built منتقل می‌شود.
