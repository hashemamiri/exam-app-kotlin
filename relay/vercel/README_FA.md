# پل اتصال روی Vercel (بدون Cloudflare) — راهنمای کوتاه

**چرا:** اندازه‌گیری روی دادهٔ همراه اول نشان داد اتصال TCP به IPهای Cloudflare بسته است؛ هم دامنهٔ
پروژهٔ Supabase و هم `sb.onlineexam.ir` و `relay.onlineexam.ir` روی همان IPها هستند. در همان لحظه،
`vercel.com` و نشانی لبهٔ آن از همان شبکه پاسخ سالم دادند. پس همین پوشه یک پل کوچک است که درخواست
اپ را از دیتاسنتر Vercel به Supabase می‌رساند.

```
اپ → https://exam-relay-onlineexam.vercel.app/api/... → Supabase
```

## پیش‌نیاز
- یک حساب رایگان Vercel (ورود با همان حساب گیت‌هاب کافی است).
- مخزن `exam-app-kotlin` روی گیت‌هاب (که همین حالا دارید).

## گام‌ها (یک‌بار)
1. به `https://vercel.com` بروید و **Continue with GitHub** را بزنید.
2. **Add New… → Project** و مخزن `exam-app-kotlin` را Import کنید.
3. در صفحهٔ تنظیمات:
   - **Project Name** را دقیقاً بگذارید: `exam-relay-onlineexam`
   - **Root Directory** → `Edit` → پوشهٔ `relay/vercel` را انتخاب کنید.
   - Framework Preset: `Other` — Build Command و Output Directory را خالی بگذارید.
4. **Deploy** را بزنید (حدود ۳۰ ثانیه).
5. نشانی `https://exam-relay-onlineexam.vercel.app/api/health` را باز کنید؛ باید ببینید:
   `{"ok":true,"upstream":"https://<project>.supabase.co", ...}`

اگر Vercel گفت این نام قبلاً گرفته شده، نام دیگری بگذارید و همان را به ما بگویید تا در اپ ثبت شود.

## نکات فنی
- این فایل هیچ کلید یا رمزی ندارد؛ همان `apikey` و `Authorization` که اپ می‌فرستد عبور می‌کند.
- فقط مسیرهای `/rest/v1`, `/auth/v1`, `/storage/v1`, `/functions/v1`, `/realtime/v1` عبور می‌کنند و
  بقیه با `404` بسته می‌شوند.
- بدنهٔ درخواست تا حدود ۴٫۵ مگابایت (سقف پلن رایگان Vercel) پشتیبانی می‌شود؛ برای تصاویر بزرگ‌تر
  اپ همین حالا فشرده‌سازی می‌کند، ولی اگر روزی فایل بزرگ‌تر لازم شد باید به پلن بالاتر یا سرور خودمان رفت.

## اگر بعداً سرور ایرانی گرفتید
همان `relay/pay_relay.py` الگو است؛ یک سرویس Caddy ساده با یک رکورد A ساده (بدون Cloudflare) هم
همین کار را می‌کند و فقط باید نشانی‌اش با `SUPABASE_RELAY_URLS` در زمان ساخت به اپ داده شود.
