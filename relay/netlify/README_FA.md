# پل اتصال روی Netlify (بدون Cloudflare) — راهنمای کوتاه

**چرا:** اندازه‌گیری روی دادهٔ همراه اول نشان داد اتصال TCP به IPهای Cloudflare (دامنهٔ پروژهٔ Supabase و
هر سه زیردامنهٔ خودمان) بسته است، در حالی که `netlify.com` و `vercel.com` و `render.com` از همان شبکه
پاسخ سالم دادند. پس ترافیک اپ از این پل رد می‌شود و پل (که در دیتاسنتر است) درخواست را به Supabase می‌رساند.

```
اپ → https://exam-relay-onlineexam.netlify.app/... → Supabase
```

## گام‌ها (یک‌بار، حدود ۵ دقیقه)
1. به `https://app.netlify.com` بروید و **Log in with GitHub** را بزنید (کارت بانکی لازم نیست).
2. **Add new site → Import an existing project → Deploy with GitHub** و مخزن `exam-app-kotlin` را انتخاب کنید.
3. تنظیمات را این‌گونه بگذارید:
   - **Branch to deploy:** `main`
   - **Base directory:** `relay/netlify`
   - Build command و Publish directory را خالی نگذارید — `netlify.toml` خودشان را تعیین می‌کند
     (`functions = functions` و `publish = public`). اگر فرم اجبار کرد، Build command را خالی و
     Publish directory را `public` بگذارید.
4. **Deploy site**. سپس **Site configuration → Change site name** و نام را دقیقاً بگذارید:
   `exam-relay-onlineexam`  → نشانی سایت می‌شود `https://exam-relay-onlineexam.netlify.app`
5. این نشانی را باز کنید؛ باید ببینید:
   `https://exam-relay-onlineexam.netlify.app/api/health` → `{"ok":true,"upstream":"https://…supabase.co", …}`

اگر Netlify گفت این نام قبلاً گرفته شده، نام دیگری بگذارید و فقط همان یک کلمه را به ما بگویید تا در اپ ثبت شود.

## نکات فنی
- هیچ کلید یا رمزی در پل نیست؛ همان `apikey` و `Authorization` که اپ می‌فرستد عبور می‌کند.
- فقط مسیرهای `/rest/v1`, `/auth/v1`, `/storage/v1`, `/functions/v1`, `/realtime/v1` عبور می‌کنند و
  بقیه با `404` بسته می‌شوند.
- تابع روی Netlify به‌صورت پیش‌فرض پاسخ خود را بین ۱۰ تا ۲۶ ثانیه نگه می‌دارد؛ درخواست‌های عادی Supabase
  خیلی سریع‌تر از این هستند.
- سقف حجم بدنهٔ درخواست در پلن رایگان ۶ مگابایت است؛ اپ تصاویر را پیش از آپلود فشرده می‌کند.

## اگر بعداً Vercel یا سرور خودتان را ترجیح دادید
پل Vercel در `relay/vercel/` آماده است. اگر سرور ایرانی گرفتید، همان الگوی `relay/pay_relay.py` با
یک رکورد A ساده (ابر خاکستری) و Caddy کافی است.
