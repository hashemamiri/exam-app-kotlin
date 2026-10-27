# هم‌گام‌سازی دامنهٔ `onlineexam.ir` با نسخهٔ منتشرشده — V335.39

> این سند فقط دربارهٔ **وب** است: دامنهٔ اصلی روی ابرک آروان. اپ اندروید در این نسخه
> دست‌نخورده است (نسخهٔ اپ همان `1.03.19` می‌ماند).

## ۱) مشکلی که حل می‌شود

`.github/workflows/site.yml` سایت را روی **Cloudflare Pages** منتشر می‌کند، ولی
`onlineexam.ir` روی **ابرک آروان** (IP `95.38.160.168`، وب‌سرور **Caddy**) یک
**کپی ثابت و دستی** سرو می‌کند. نتیجه: دامنه هر بار چند نسخه عقب می‌ماند.

سنجش واقعی در V335.39 (با تطبیق هش چانک‌ها):

| میزبان | JS chunk | CSS chunk | نسخه |
|---|---|---|---|
| `onlineexam.ir` | `aba4ff2c3ab8` | `99dd19e60003` | **V335.32** |
| `onlineexam-1q1.pages.dev` | `8259d0da2ea5` | `b30ca1ed032c` | **V335.38** |

دو مشکل جانبی که همان‌جا پیدا شد:

1. **`/pay-return.html` در ریشهٔ دامنه وجود نداشت** و Caddy به‌جای ۴۰۴، `index.html`
   با کد ۲۰۰ می‌داد ⇒ بازگشت از درگاه زیبال به صفحهٔ فرود می‌رسید. علت: `site.yml`
   نسخهٔ ریشهٔ این فایل را در `site_out/` کپی نمی‌کرد.
2. **هر مسیر ناموجود ۲۰۰ برمی‌گرداند** ⇒ مانیتور آپ‌تایم و سئو «کور» بودند.

## ۲) چه چیزی در این بسته آمده

| فایل | نقش |
|---|---|
| `scripts/sync_arvan_site.py` | ابزار اصلی: `--check` / `--mirror` / `--apply` (بدون هیچ سکرت) |
| `scripts/check_site_sync.py` | نگهبان محلی/آفلاین این نسخه (در `preflight.py` صدا زده می‌شود) |
| `.github/workflows/site.yml` | ۱) کپی `site/pay-return.html` در `site_out/` ۲) گام `SYNC_ARVAN` |
| `docs/ci/site_arvan_sync_step.yml` | قطعهٔ آمادهٔ workflow برای جای‌گذاری دستی (وقتی توکن `workflow` ندارد) |
| `relay/Caddyfile.site.example` | نمونهٔ Caddy با ۴۰۴ واقعی و کش درست |
| `app/src/test/.../V335_39_SiteDomainSyncTest.kt` | ۵ تست نگهبان برای همین قراردادها |
| `docs/fa/ARVAN_SITE_SYNC_FA.md` | همین سند |

## ۳) سه کارکرد ابزار

```bash
# ۱) آیا دامنه عقب است؟  (خروجی: 0 هم‌تراز · 2 عقب · 3 خطای شبکه)
python3 scripts/sync_arvan_site.py --check

# ۲) ساخت آینهٔ کامل از سایت منتشرشده در پوشهٔ site_mirror/ (بدون سکرت، فقط HTTPS)
python3 scripts/sync_arvan_site.py --mirror

# ۳) ارسال روی سرور + بررسی خودکار پس از ارسال
python3 scripts/sync_arvan_site.py --apply --ssh-key ~/.ssh/arvan
```

`--apply` به‌طور پیش‌فرض:

* مقصد را `root@95.38.160.168` می‌گیرد؛
* **ریشهٔ وب را خودش تشخیص می‌دهد** (`--webroot auto`): اول `root` را از
  `/etc/caddy/Caddyfile` می‌خواند، بعد مسیرهای رایج (`/var/www/onlineexam`,
  `/var/www/html`, `/srv/onlineexam`, …) را با وجود `index.html` آزمایش می‌کند؛
* با `rsync -az --delete --delay-updates` ارسال می‌کند (فایل‌های اضافی سرور پاک می‌شوند)؛
* آخر `caddy reload` می‌زند (اگر شکست بخورد مهم نیست، فایل ثابت است)؛
* سه بار `--check` می‌زند تا از هم‌ترازی مطمئن شود.

نکتهٔ مهم: اگر `/pay-return.html` در ریشهٔ منبع نبود، ابزار آن را از
`/pwa/pay-return.html` کپی می‌کند ⇒ **سکرت فعلی `PAY_CALLBACK_BASE` هم بی‌مشکل کار
می‌کند** و لازم نیست در داشبورد Supabase چیزی را عوض کنید.

## ۴) گام‌های میدانی (ترتیب دقیق)

### گام ۱ — نگهبان و تست‌ها (روی رایانهٔ خودتان)

```bash
cd ~/exam-app-kotlin          # یا مسیر WSL شما
python3 scripts/check_site_sync.py
python3 scripts/preflight.py app/src/test/java/ir/exam/app/V335_39_SiteDomainSyncTest.kt
```

### گام ۲ — دیدن عقب‌ماندگی دامنه

```bash
python3 scripts/sync_arvan_site.py --check
```

اگر «هم‌تراز» بود یعنی کسی زودتر هم‌گام کرده؛ اگر نه، گام ۳.

### گام ۳ — کلید SSH یک‌بار برای همیشه

روی **همین رایانهٔ WSL**:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/arvan -N ""        # اگر کلید ندارید
ssh-copy-id -i ~/.ssh/arvan.pub root@95.38.160.168 # اگر کلید روی سرور نیست
ssh -i ~/.ssh/arvan root@95.38.160.168 "echo OK"   # باید OK چاپ شود
```

### گام ۴ — ارسال (هم‌گام‌سازی دامنه)

```bash
python3 scripts/sync_arvan_site.py --apply --ssh-key ~/.ssh/arvan
```

خروجی مورد انتظار: تشخیص ریشهٔ وب (`/var/www/...`) → فهرست فایل‌های تغییر‌یافته →
`✅ ارسال شد` → `وضعیت: هم‌تراز ✅`.

اگر ریشهٔ وب را پیدا نکرد:

```bash
python3 scripts/sync_arvan_site.py --apply --ssh-key ~/.ssh/arvan --webroot /مسیر/درست
```

### گام ۵ — فعال‌کردن هم‌گام‌سازی خودکار در CI (یک‌بار)

در GitHub → مخزن `hashemamiri/exam-app-kotlin`:

1. **Settings → Secrets and variables → Actions → Variables → New repository variable**
   - نام: `ARVAN_SYNC` — مقدار: `on`
2. همان صفحه → **Secrets → New repository secret**:
   - `ARVAN_HOST` = `root@95.38.160.168`
   - `ARVAN_WEBROOT` = مسیری که در گام ۴ تشخیص داده شد (یا `auto`)
   - `ARVAN_SSH_KEY` = **محتوای کلید خصوصی** (`cat ~/.ssh/arvan`)

> کلید خصوصی فقط داخل Secret می‌رود؛ نه در چت، نه در گیت، نه در APK.

از این پس هر انتشار روی `main`، بعد از Cloudflare Pages، دامنه را هم به‌روز می‌کند.
اگر Secretها خالی باشند، گام با پیام `::warning::` رد می‌شود و CI هرگز نمی‌شکند.

> **اگر توکن گیت شما scope=`workflow` ندارد** (در نسخهٔ V335.32 هم همین مشکل بود):
> نصاب کل نسخه را اعمال و push می‌کند و فقط تغییر `site.yml` را بیرون می‌گذارد.
> در این حالت `check_site_sync.py` به‌جای ✅ هشدار ⚠ می‌دهد و می‌گوید چه چیزی
> باقی مانده؛ بقیهٔ تست‌ها و `preflight` سبز می‌مانند. یک‌بار قطعهٔ
> `docs/ci/site_arvan_sync_step.yml` را در گام ۵ دستی به `site.yml` اضافه کنید.

### گام ۶ — اصلاح ۴۰۴ (اختیاری ولی توصیه‌شده)

`relay/Caddyfile.site.example` را با Caddyfile فعلی سرور مقایسه و بخش‌های مربوط به
`onlineexam.ir` را ادغام کنید، سپس:

```bash
ssh -i ~/.ssh/arvan root@95.38.160.168 \
  "caddy validate --config /etc/caddy/Caddyfile && systemctl reload caddy"
```

اثر: مسیرهای ناموجود ۴۰۴ واقعی می‌دهند (به‌جای index با کد ۲۰۰) و دارایی‌های
hash‌دار با `immutable` کش می‌شوند.

## ۴.۱) تازگی صفحه‌های حساس در وب‌سرور (V335.45 / V335.46)

اگر مرورگر نسخهٔ کش‌شدهٔ صفحهٔ بازگشت درگاه را نشان دهد، این ابزار همان مسیرها را به
بلوک `@nocache` در Caddyfile اضافه می‌کند — ایمن و تکرارپذیر:

```bash
python3 scripts/caddy_ensure_nocache.py --ssh-key ~/.ssh/arvan --dry-run   # فقط diff
python3 scripts/caddy_ensure_nocache.py --ssh-key ~/.ssh/arvan             # اعمال
```

- **پیش‌فرض:** `/pay-return.html` و `/pwa/pay-return.html` در همهٔ بلوک‌های `@nocache`.
- **مسیر دلخواه (قابل تکرار):** `--path /exam.html --path /wallet.html`
- **ایمنی:** پشتیبان `.bak-<timestamp>` → `caddy validate` روی فایل موقت → جایگزینی → `systemctl reload caddy`؛ در خطا خودکار به پشتیبان برمی‌گردد. اجرای دوباره هیچ تغییری نمی‌دهد.
- **تأیید:** `curl -sI https://onlineexam.ir/pay-return.html | grep -i cache-control` باید `no-store, must-revalidate` بدهد.

## ۵) بررسی نهایی

```bash
python3 scripts/sync_arvan_site.py --check      # باید: ✅ هم‌تراز + /pay-return.html موجود
```

از گوشی یا سرور ایرانی (که این سندباکس به آن دسترسی ندارد):

```bash
curl -sI https://onlineexam.ir/ | head -3
curl -s https://onlineexam.ir/pwa/pay-return.html | head -3
```

## ۶) عیب‌یابی

| نشانه | علت | راه‌حل |
|---|---|---|
| `خطای شبکه در https://onlineexam.ir/` | مسدودی/قطعی مسیر | از شبکهٔ دیگر امتحان کنید؛ `--check` به مسیر ایرانی نیاز دارد |
| `دسترسی ناموفق — published HTTP 0` | Cloudflare Pages در دسترس نیست | `--source` را به نشانی دیگر بدهید |
| `❌ آینه معتبر نیست: چانک‌های JS کم‌اند` | پاسخ ناقص/fallback | دوباره اجرا کنید؛ یا `--allow-missing` (توصیه نمی‌شود) |
| `ریشهٔ وب پیدا نشد` | Caddyfile غیرمعمول | `--webroot` را دستی بدهید |
| `rsync: command not found` | rsync روی سرور نیست | `apt install -y rsync` روی آروان |
| دامنه به‌روز شد ولی در گوشی قدیمی است | کش سرویس‌ورکر | `/pwa/sw.js` با `no-store` است؛ یک‌بار بستن/باز کردن تب یا پاک‌کردن کش PWA |
| `git push` رد شد: `refusing to allow a Personal Access Token to create or update workflow` | توکن گیت scope=`workflow` ندارد | نصاب خودش این حالت را می‌گیرد: تغییر workflow را کنار می‌گذارد، فایل `site_v335_39_workflow.yml.pending` را کنار خودش می‌نویسد و بقیه را push می‌کند. بعد محتوای `docs/ci/site_arvan_sync_step.yml` را در GitHub → Actions → `site.yml` جای‌گذاری کنید (یا توکن با scope=`workflow` بدهید) |

## ۷) بازگشت به عقب (rollback)

آینهٔ نسخهٔ قبلی روی سرور نمی‌ماند؛ برای بازگشت:

```bash
git checkout <commit-قبلی> -- .   # روی رایانه
python3 scripts/sync_arvan_site.py --mirror --source https://<نشانی-نسخهٔ-قبلی>
python3 scripts/sync_arvan_site.py --apply --ssh-key ~/.ssh/arvan
```

یا سریع‌تر: انتشار مجدد Pages از کامیت سالم، سپس `--apply --from-dir` روی خروجی آن.
