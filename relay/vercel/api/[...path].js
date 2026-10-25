// پل واسط Supabase روی Vercel — نسخهٔ بدون Cloudflare (برای شبکهٔ همراه اول)
//
// نشانی پایه در اپ:  https://<نام-پروژه>.vercel.app/api
// مسیرها:  /api/rest/v1/...   /api/auth/v1/...   /api/storage/v1/...   /api/functions/v1/...
// هیچ کلید یا رمزی در این فایل نیست؛ همان apikey/Authorization که اپ می‌فرستد عبور می‌کند.

const UPSTREAM = (process.env.UPSTREAM || "https://eazwuyrymsvdkwckdpco.supabase.co").replace(/\/+$/, "");

const ALLOWED_PREFIXES = ["/rest/v1", "/auth/v1", "/storage/v1", "/functions/v1", "/realtime/v1"];

// هدرهای hop-by-hop که نباید منتقل شوند
const DROP_REQ = new Set([
  "host", "connection", "keep-alive", "transfer-encoding", "upgrade",
  "proxy-authorization", "proxy-connection", "te", "trailer", "content-length",
]);
const DROP_RES = new Set([
  "connection", "keep-alive", "transfer-encoding", "content-encoding",
  "content-length", "set-cookie",
]);

function sendJson(res, status, obj) {
  res.statusCode = status;
  res.setHeader("content-type", "application/json; charset=utf-8");
  res.setHeader("access-control-allow-origin", "*");
  res.setHeader("cache-control", "no-store");
  res.end(JSON.stringify(obj));
}

module.exports = async function handler(req, res) {
  if (req.method === "OPTIONS") {
    res.statusCode = 204;
    res.setHeader("access-control-allow-origin", "*");
    res.setHeader("access-control-allow-methods", "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS");
    res.setHeader("access-control-allow-headers", req.headers["access-control-request-headers"] || "*");
    res.setHeader("access-control-max-age", "86400");
    res.end();
    return;
  }

  // مسیر از catch-all خوانده می‌شود: /api/<...path>
  const parts = req.query && req.query.path ? [].concat(req.query.path) : [];
  const pathname = "/" + parts.join("/");
  let search = "";
  try {
    search = new URL(req.url, "https://relay.local").search || "";
  } catch {
    search = "";
  }

  if (pathname === "/" || pathname === "/health") {
    sendJson(res, 200, { ok: true, upstream: UPSTREAM, ts: new Date().toISOString() });
    return;
  }

  if (!ALLOWED_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + "/"))) {
    sendJson(res, 404, { ok: false, error: "path_not_allowed" });
    return;
  }

  // بدنهٔ درخواست (بدون bodyParser)
  let body;
  if (!["GET", "HEAD"].includes(req.method)) {
    const chunks = [];
    for await (const chunk of req) chunks.push(chunk);
    body = chunks.length ? Buffer.concat(chunks) : undefined;
  }

  const headers = new Headers();
  for (const [k, v] of Object.entries(req.headers)) {
    if (DROP_REQ.has(k.toLowerCase())) continue;
    if (Array.isArray(v)) v.forEach((x) => headers.append(k, x));
    else if (v != null) headers.set(k, String(v));
  }
  headers.set("host", new URL(UPSTREAM).host);

  let upstreamRes;
  try {
    upstreamRes = await fetch(UPSTREAM + pathname + search, {
      method: req.method,
      headers,
      body,
      redirect: "manual",
    });
  } catch (e) {
    sendJson(res, 502, { ok: false, error: "upstream_unreachable" });
    return;
  }

  const buf = Buffer.from(await upstreamRes.arrayBuffer());
  res.statusCode = upstreamRes.status;
  upstreamRes.headers.forEach((value, key) => {
    const k = key.toLowerCase();
    if (DROP_RES.has(k)) return;
    if (k.startsWith("access-control-")) return; // خودمان می‌گذاریم
    res.setHeader(key, value);
  });
  res.setHeader("access-control-allow-origin", "*");
  res.setHeader("access-control-expose-headers", "*");
  res.setHeader("cache-control", upstreamRes.headers.get("cache-control") || "no-store");
  res.end(buf);
};

module.exports.config = { api: { bodyParser: false } };
