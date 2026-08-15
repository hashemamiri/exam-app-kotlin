# V38.2 — رفع digest دعوت و جلوگیری از نمایش Header حساس

خطای `function digest(text, unknown) does not exist` به‌دلیل نصب pgcrypto در schema
`extensions` و ارسال text بدون تبدیل صریح به bytea بود. هر دو مسیر ساخت و مصرف
کد اکنون از `extensions.digest(convert_to(value,'UTF8'),'sha256')` استفاده می‌کنند.

همچنین خطاهای manager UI پیش از نمایش در URL و Headers قطع و Authorization، apikey
و Bearer پاک می‌شوند تا هیچ session token در صفحه یا اسکرین‌شات دیده نشود.

```text
SQL: SQL_NATIVE_INVITE_DIGEST_V382_HOTFIX.sql
Edge deploy: ندارد
Secret/Dependency: ندارد
پیش‌نیاز: SQL V37
```
