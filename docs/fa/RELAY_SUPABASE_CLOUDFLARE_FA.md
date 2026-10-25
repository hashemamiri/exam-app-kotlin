# پل Supabase روی Cloudflare — رفع «اپ با اینترنت همراه اول کار نمی‌کند» (V335.20)

**ریشهٔ مشکل:** سایت روی دامنهٔ خودمان (`onlineexam.ir` روی Cloudflare) باز می‌شود، ولی اپ برای ورود و
همهٔ کارها به `https://eazwuyrymsvdkwckdpco.supabase.co` وصل می‌شود و دادهٔ همراه اول این دامنه را
باز نمی‌کند. به همین دلیل: روی Wi‑Fi کار می‌کند، با دادهٔ همراه اول هنگام ورود قطع می‌شود.

**راه‌حل:** یک پل کوچک روی دامنهٔ خودمان (`sb.onlineexam.ir`) که همان درخواست‌ها را به Supabase می‌رساند.
اپ از نسخهٔ `1.03.02` به بعد مسیر سالم را خودکار انتخاب می‌کند (و در «تنظیمات → داده‌ها → تست اتصال به سرور»
قابل دیدن و قفل‌کردن دستی است).

```
اپ  →  https://sb.onlineexam.ir/auth/v1/...  →  Cloudflare Worker  →  https://eazwuyrymsvdkwckdpco.supabase.co
```

---

## گام ۱ — ساخت Worker در Cloudflare
1. وارد **dash.cloudflare.com** شوید → منوی چپ → **Workers & Pages** → **Create** → **Worker**.
2. نام: `supabase-relay` → **Deploy** (نسخهٔ نمونه ساخته می‌شود).
3. دکمهٔ **Edit code** را بزنید؛ کل محتوای فایل را پاک کنید و کد فایل
   `relay/cloudflare-worker-supabase.js` از پروژه را جای‌گذاری کنید → **Deploy**.

## گام ۲ — گذاشتن نشانی پروژه (متغیر محیطی)
1. در همان Worker → **Settings** → **Variables and Secrets** → **Add**.
2. Name: `SUPABASE_ORIGIN` · Value: `https://eazwuyrymsvdkwckdpco.supabase.co` → **Save and deploy**.
   (اگر مقدار را نگذارید، همان نشانی پیش‌فرض داخل کد استفاده می‌شود.)

## گام ۳ — وصل کردن زیردامنه
1. در Worker → **Settings** → **Domains & Routes** → **Add** → **Custom domain**.
2. بنویسید: `sb.onlineexam.ir` → **Add domain** → **Activate domain**.
   (Cloudflare خودش رکورد DNS و گواهی HTTPS را می‌سازد؛ نیازی به کار دستی نیست.)

## گام ۴ — آزمایش پل
در مرورگر باز کنید:

```
https://sb.onlineexam.ir/auth/v1/health
```

باید چیزی شبیه این ببینید: `{"date":"…","description":"GoTrue is a user registration and authentication API","name":"GoTrue",…}`
اگر ۵۰۲ یا صفحهٔ خطای Cloudflare دیدید، گام ۲ (متغیر `SUPABASE_ORIGIN`) را بررسی کنید.

## گام ۵ — آزمایش در اپ
1. اپ نسخهٔ `1.03.02` را نصب کنید.
2. با **دادهٔ همراه اول** باز کنید → **تنظیمات → داده‌ها → تست اتصال به سرور** → **تست اتصال**.
3. باید «مسیر جایگزین» با وضعیت **سالم** دیده شود و کنار آن برچسب **فعال** باشد؛ سپس ورود انجام می‌شود.

---

## گزینهٔ جایگزین: پل روی سرور ابرآروان (همان VPS درگاه پرداخت)
اگر Worker را نمی‌خواهید، همان سرور `pay.onlineexam.ir` این کار را می‌کند. در فایل `/etc/caddy/Caddyfile`
این بلوک را اضافه کنید و `systemctl reload caddy` بزنید:

```
sb.onlineexam.ir {
    reverse_proxy https://eazwuyrymsvdkwckdpco.supabase.co {
        header_up Host eazwuyrymsvdkwckdpco.supabase.co
    }
}
```

سپس در Cloudflare یک رکورد `A` با نام `sb` و مقدار **IP همان سرور** و حالت **DNS only (ابر خاکستری)** بسازید.
آزمون: `curl -s https://sb.onlineexam.ir/auth/v1/health` باید نام `GoTrue` را برگرداند.

---

## اگر زیردامنهٔ دیگری ساختید
در فایل `local.properties` کنار `SUPABASE_URL` این خط را اضافه کنید (چند نشانی را با ویرگول جدا کنید):

```
SUPABASE_RELAY_URLS=https://sb.onlineexam.ir
```

تا وقتی این خط نباشد، اپ همان `https://sb.onlineexam.ir` را امتحان می‌کند و اگر نبود، مستقیم به Supabase وصل می‌شود.

## عیب‌یابی
| علامت | علت محتمل | کار |
|---|---|---|
| `/auth/v1/health` صفحهٔ ۵۰۲ می‌دهد | `SUPABASE_ORIGIN` اشتباه یا خالی | گام ۲ |
| صفحهٔ «خطای ۱۰۱۶ / DNS» | زیردامنه فعال نشده | گام ۳ و صبر ۲–۳ دقیقه |
| در اپ هر دو مسیر «ناموفق» | اینترنت قطع است یا هر دو دامنه بسته‌اند | «تست اتصال» را با Wi‑Fi هم بزنید |
| ورود انجام شد ولی تصویرها نمی‌آید | نسخهٔ قدیمی اپ | نسخهٔ `1.03.02` را نصب کنید |
