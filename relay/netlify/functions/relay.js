// پل واسط Supabase روی Netlify (بدون Cloudflare) — برای شبکهٔ همراه اول
//
// نشانی پایه در اپ:  https://<نام-سایت>.netlify.app
// مسیرها:  /rest/v1/...   /auth/v1/...   /storage/v1/...   /functions/v1/...   /realtime/v1/...
// هیچ کلید یا رمزی در این فایل نیست؛ همان apikey/Authorization که اپ می‌فرستد عبور می‌کند.

const UPSTREAM = (process.env.UPSTREAM || "https://eazwuyrymsvdkwckdpco.supabase.co").replace(/\/+$/, "");
const ALLOWED_PREFIXES = ["/rest/v1", "/auth/v1", "/storage/v1", "/functions/v1", "/realtime/v1"];

const DROP_REQ = new Set([
  "host", "connection", "keep-alive", "transfer-encoding", "upgrade",
  "proxy-authorization", "proxy-connection", "te", "trailer", "content-length",
]);
const DROP_RES = new Set([
  "connection", "keep-alive", "transfer-encoding", "content-encoding",
  "content-length", "set-cookie",
]);

function json(statusCode, obj) {
  return {
    statusCode,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "cache-control": "no-store",
    },
    body: JSON.stringify(obj),
  };
}

/** مسیر اصلی درخواست؛ rawUrl نشانی واقعیِ کاربر پیش از بازنویسی را دارد. */
function originalPath(event) {
  let pathname = "/";
  let search = "";
  const raw = event.rawUrl || "";
  try {
    const u = new URL(raw, "https://relay.local");
    pathname = u.pathname || "/";
    search = u.search || "";
  } catch {
    pathname = event.path || "/";
  }
  // اگر درخواست مستقیم به خود تابع آمده باشد، پیشوند تابع حذف می‌شود.
  const fnPrefix = "/.netlify/functions/relay";
  if (pathname.startsWith(fnPrefix)) pathname = pathname.slice(fnPrefix.length) || "/";
  if (pathname.length > 1 && pathname.endsWith("/")) pathname = pathname.slice(0, -1);
  return { pathname, search };
}

exports.handler = async function (event) {
  const method = (event.httpMethod || "GET").toUpperCase();

  if (method === "OPTIONS") {
    return {
      statusCode: 204,
      headers: {
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS",
        "access-control-allow-headers":
          (event.headers && (event.headers["access-control-request-headers"] || event.headers["Access-Control-Request-Headers"])) || "*",
        "access-control-max-age": "86400",
      },
      body: "",
    };
  }

  const { pathname, search } = originalPath(event);

  if (pathname === "/" || pathname === "/health" || pathname === "/api/health") {
    return json(200, { ok: true, upstream: UPSTREAM, ts: new Date().toISOString() });
  }

  if (!ALLOWED_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + "/"))) {
    return json(404, { ok: false, error: "path_not_allowed" });
  }

  const headers = new Headers();
  for (const [k, v] of Object.entries(event.headers || {})) {
    const lower = k.toLowerCase();
    if (DROP_REQ.has(lower)) continue;
    if (v != null) headers.set(k, String(v));
  }
  headers.set("host", new URL(UPSTREAM).host);

  let body;
  if (!["GET", "HEAD"].includes(method)) {
    if (event.body) {
      body = event.isBase64Encoded
        ? Buffer.from(event.body, "base64")
        : Buffer.from(event.body, "utf8");
    }
  }

  let upstreamRes;
  try {
    upstreamRes = await fetch(UPSTREAM + pathname + search, {
      method,
      headers,
      body,
      redirect: "manual",
    });
  } catch (e) {
    return json(502, { ok: false, error: "upstream_unreachable" });
  }

  const buffer = Buffer.from(await upstreamRes.arrayBuffer());
  const outHeaders = {};
  upstreamRes.headers.forEach((value, key) => {
    const k = key.toLowerCase();
    if (DROP_RES.has(k)) return;
    if (k.startsWith("access-control-")) return; // خودمان می‌گذاریم
    outHeaders[key] = value;
  });
  outHeaders["access-control-allow-origin"] = "*";
  outHeaders["access-control-expose-headers"] = "*";
  outHeaders["cache-control"] = upstreamRes.headers.get("cache-control") || "no-store";

  const type = (upstreamRes.headers.get("content-type") || "").toLowerCase();
  const textual =
    type.startsWith("text/") ||
    type.includes("json") ||
    type.includes("xml") ||
    type.includes("javascript") ||
    type.includes("x-www-form-urlencoded");

  if (textual) {
    return { statusCode: upstreamRes.status, headers: outHeaders, body: buffer.toString("utf8") };
  }
  return {
    statusCode: upstreamRes.status,
    headers: outHeaders,
    body: buffer.toString("base64"),
    isBase64Encoded: true,
  };
};
