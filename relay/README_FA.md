# پلِ پرداخت با IP ثابت (V187) — راهنمای قدم‌به‌قدم

**چرا:** شاپرک از ۱۵ شهریور ۱۴۰۵ فقط از IPهای ثبت‌شده در پنل زرین‌پال تراکنش می‌پذیرد. سایت روی Cloudflare Pages و
منطق پرداخت روی Supabase Edge Function است که IP ثابت ندارند. این پل، یک سرور کوچک ایرانی با IP ثابت است که فقط
دو درخواست «ایجاد تراکنش» و «تأیید» را به زرین‌پال می‌رساند. همه‌چیز دیگر (کیف پول، سفارش‌ها، بازگشت بانک) همان
`wallet-payment` قبلی است.

```
اپ/سایت → Supabase wallet-payment ──(PAY_RELAY_URL + توکن)──► پل (IP ثابت) ──► payment.zarinpal.com
```

## گام ۰ — چه چیزهایی لازم است
- یک سرور ابری ایرانی (VPS) با **IPv4 ثابت**: ابر آروان (کوچک‌ترین پلن، اوبونتو 22.04/24.04) کافی است.
- یک زیردامنه، مثلاً `pay.onlineexam.ir`.
- Supabase CLI روی WSL (قبلاً برای deploy استفاده کرده‌اید).

## گام ۱ — ساخت سرور و پیدا کردن IP
1. پنل آروان → «سرور ابری» → ساخت سرور: اوبونتو، کوچک‌ترین سایز، **IP عمومی فعال**.
2. پس از ساخت، در صفحهٔ سرور، **IP عمومی (IPv4)** نوشته شده — همین IP را یادداشت کنید (این همان IPی است که در زرین‌پال ثبت می‌شود).
3. برای اطمینان، بعد از ورود به سرور: `curl -4 ifconfig.me` باید همان IP را چاپ کند.

## گام ۲ — DNS در Cloudflare
Cloudflare → دامنهٔ onlineexam.ir → DNS → Add record:
- Type `A` · Name `pay` · IPv4 = IP سرور · **Proxy status: DNS only (ابر خاکستری)** ← مهم؛ اگر نارنجی باشد، Caddy نمی‌تواند گواهی بگیرد.

## گام ۳ — نصب روی سرور (یک‌بار؛ در ترمینال سرور با ssh)
```bash
ssh root@IP_SERVER
apt update && apt install -y python3 curl debian-keyring debian-archive-keyring apt-transport-https
# Caddy (HTTPS خودکار)
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
apt update && apt install -y caddy

useradd -r -s /usr/sbin/nologin relay
mkdir -p /opt/pay-relay
```
حالا فایل‌های این پوشه را از کامپیوتر خودتان روی سرور بگذارید (در WSL، پوشهٔ پروژه):
```bash
scp relay/pay_relay.py root@IP_SERVER:/opt/pay-relay/pay_relay.py
scp relay/pay-relay.service root@IP_SERVER:/etc/systemd/system/pay-relay.service
scp relay/Caddyfile root@IP_SERVER:/etc/caddy/Caddyfile
```
دوباره در سرور:
```bash
TOKEN=$(openssl rand -hex 32); echo "RELAY_TOKEN=$TOKEN" > /etc/pay-relay.env; chmod 600 /etc/pay-relay.env; echo "توکن: $TOKEN"
# اگر زیردامنه‌تان pay.onlineexam.ir نیست: nano /etc/caddy/Caddyfile و خط اول را عوض کنید
systemctl daemon-reload && systemctl enable --now pay-relay && systemctl restart caddy
ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw --force enable
curl -s https://pay.onlineexam.ir/health      # باید {"ok": true} بدهد
```
**توکن** را جایی امن نگه دارید؛ در چت/گیت نگذارید.

## گام ۴ — اتصال Supabase به پل (در WSL)
```bash
cd /mnt/c/Users/Hashem/Downloads/exam-app-kotlin
supabase secrets set PAY_RELAY_URL=https://pay.onlineexam.ir PAY_RELAY_TOKEN=توکن_گام_۳
supabase functions deploy wallet-payment --no-verify-jwt
```
(اگر هنوز نکرده‌اید: `PAY_PROVIDER=zarinpal` و `PAY_ZARINPAL_MERCHANT=…` هم با همین دستور secrets set.)

## گام ۵ — ثبت IP در زرین‌پال
پنل زرین‌پال → درگاه → تنظیمات درگاه → «IP سرور» → IP گام ۱ را ثبت کنید (فقط IPv4).

## گام ۶ — آزمایش
در اپ یا سایت: کیف پول → شارژ → مبلغ کم → باید به صفحهٔ زرین‌پال بروید و پس از پرداخت، برگردید و موجودی اضافه شود.
لاگ پل روی سرور: `journalctl -u pay-relay -f` (فقط مسیر و کد وضعیت چاپ می‌شود).

## عیب‌یابی
| علامت | علت محتمل |
|---|---|
| `curl …/health` جواب نمی‌دهد | DNS هنوز پخش نشده یا ابر نارنجی است؛ `systemctl status caddy` |
| خطای «اتصال به زرین‌پال ناموفق بود» با کد `zarinpal_401` | توکن Supabase با `/etc/pay-relay.env` یکی نیست |
| کد `zarinpal_502` | سرور به zarinpal.com دسترسی ندارد (فایروال آروان/خروجی) |
| زرین‌پال خطای IP می‌دهد | IP ثبت‌شده ≠ خروجی `curl -4 ifconfig.me` روی سرور |

هزینهٔ ماهانه: کوچک‌ترین VPS آروان (حدوداً ۲۰۰–۴۰۰ هزار تومان). CPU/RAM مصرفی پل ناچیز است.
