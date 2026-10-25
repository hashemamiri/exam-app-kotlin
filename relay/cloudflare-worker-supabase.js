/**
 * V335.20 — پل Supabase روی Cloudflare Workers
 *
 * چرا: دامنهٔ `*.supabase.co` از شبکهٔ برخی اپراتورهای ایرانی (نمونه: دادهٔ همراه اول) در دسترس نیست،
 * در حالی که دامنهٔ خودمان (onlineexam.ir روی Cloudflare) باز می‌شود. این Worker همان درخواست‌های
 * اپ (ورود، پرس‌وجوها، فضای ذخیره‌سازی تصویر/صوت و توابع لبه) را بدون تغییر به پروژهٔ Supabase می‌رساند.
 *
 * نصب: Cloudflare → Workers & Pages → Create → Worker → این فایل را جای‌گذاری کنید →
 * Variables → `SUPABASE_ORIGIN` را روی نشانی پروژه بگذارید (بدون اسلش انتهایی) →
 * Settings → Domains & Routes → Add → Custom domain: `sb.onlineexam.ir`
 * آزمون: مرورگر یا ترمینال → https://sb.onlineexam.ir/auth/v1/health  → باید {"name":"GoTrue",...} بدهد.
 *
 * نکته‌های امنیتی: هیچ کلید یا توکنی در این فایل نیست؛ اپ همان `apikey` و `Authorization` خودش را
 * می‌فرستد و Worker فقط آن‌ها را عبور می‌دهد. دسترسی به مسیرهای دیگر Supabase باز است، ولی همان
 * کدهای دسترسی خود پروژه (RLS و JWT) تعیین‌کننده‌اند؛ این پل هیچ دسترسی تازه‌ای نمی‌سازد.
 */

const DEFAULT_ORIGIN = "https://eazwuyrymsvdkwckdpco.supabase.co";

export default {
  async fetch(request, env) {
    const origin = (env && env.SUPABASE_ORIGIN ? env.SUPABASE_ORIGIN : DEFAULT_ORIGIN).replace(/\/+$/, "");
    const url = new URL(request.url);

    // صفحهٔ ریشه فقط برای بررسی سلامت پل
    if (url.pathname === "/" || url.pathname === "") {
      return new Response(
        JSON.stringify({ ok: true, relay: "supabase", hint: "مسیرهای /auth/v1, /rest/v1, /storage/v1, /functions/v1" }),
        { status: 200, headers: { "content-type": "application/json; charset=utf-8" } }
      );
    }

    const target = new URL(url.pathname + url.search, origin);

    const headers = new Headers(request.headers);
    // هدرهای مخصوص Cloudflare و هدر Host را حذف می‌کنیم تا Supabase درخواست سالم ببیند.
    ["host", "cf-connecting-ip", "cf-ipcountry", "cf-ray", "cf-visitor", "cf-worker",
     "x-forwarded-proto", "x-forwarded-for", "x-real-ip", "cdn-loop"].forEach((h) => headers.delete(h));

    const init = {
      method: request.method,
      headers,
      redirect: "manual",
      // فشرده‌سازی را به لبه واگذار می‌کنیم
      cf: { cacheTtl: -1, cacheEverything: false }
    };
    if (request.method !== "GET" && request.method !== "HEAD" && request.body) {
      init.body = request.body;
      // ارسال بدنهٔ جریانی (Stream) نیاز به duplex دارد
      init.duplex = "half";
    }

    let upstream;
    try {
      upstream = await fetch(target.toString(), init);
    } catch (e) {
      return new Response(
        JSON.stringify({ error: "relay_upstream_unreachable", detail: String(e && e.message ? e.message : e) }),
        { status: 502, headers: { "content-type": "application/json; charset=utf-8" } }
      );
    }

    const outHeaders = new Headers(upstream.headers);
    // بدنهٔ برگشتی از لبه از قبل باز شده است؛ این دو هدر باید برداشته شوند.
    outHeaders.delete("content-encoding");
    outHeaders.delete("content-length");

    // بازنویسی نشانی‌های مطلق Supabase به دامنهٔ پل (برای ریدایرکت‌ها و لینک‌های امضاشده)
    const relayOrigin = url.origin;
    const location = outHeaders.get("location");
    if (location && location.startsWith(origin)) {
      outHeaders.set("location", relayOrigin + location.substring(origin.length));
    }
    outHeaders.set("access-control-allow-origin", request.headers.get("origin") || "*");
    outHeaders.set("access-control-allow-headers", "*");
    outHeaders.set("access-control-allow-methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: outHeaders });
    }

    return new Response(upstream.body, { status: upstream.status, headers: outHeaders });
  }
};
