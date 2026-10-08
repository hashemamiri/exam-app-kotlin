// V144/V144.1 — صدور لینک آپلود موقت (Presigned PUT) برای هر ذخیره‌ساز سازگار با S3
// (ابر آروان، لیارا، پارس‌پک، Cloudflare R2، MinIO…). کلاینت پس از ورود
// {kind:'image'|'audio', folder, exam_id, ext, size} می‌فرستد و {upload_url, public_url, headers} می‌گیرد؛
// سپس فایل را مستقیم با PUT به ذخیره‌ساز می‌فرستد. کلیدها فقط در Secrets این تابع هستند:
//   S3_ENDPOINT        مثلاً https://s3.ir-thr-at1.arvanstorage.ir  (R2: https://<account>.r2.cloudflarestorage.com)
//   S3_ACCESS_KEY_ID, S3_SECRET_ACCESS_KEY
//   S3_BUCKET          پیش‌فرض azmoon-media
//   S3_REGION          پیش‌فرض auto (آروان: هر مقداری می‌پذیرد؛ R2: auto)
//   S3_PUBLIC_BASE     آدرس عمومی خواندن، مثلاً https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir یا https://media.onlineexam.ir
// (نام‌های قدیمی R2_* هم برای سازگاری خوانده می‌شوند.)
import { createClient } from 'npm:@supabase/supabase-js@2.112.2';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { ...CORS, 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
});
const env = (name: string) => (Deno.env.get(name) || '').trim();

const MAX_IMAGE = 8 * 1024 * 1024;
const MAX_AUDIO = 3 * 1024 * 1024;
const IMAGE_EXT: Record<string, string> = { webp: 'image/webp', jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png' };
const AUDIO_EXT: Record<string, string> = { m4a: 'audio/mp4', mp4: 'audio/mp4', webm: 'audio/webm', ogg: 'audio/ogg', mp3: 'audio/mpeg', wav: 'audio/wav' };
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const EXPIRES = 300; // ثانیه

// ---------- AWS SigV4 (presigned URL) بدون وابستگی خارجی ----------
const enc = new TextEncoder();
const hex = (buf: ArrayBuffer) => Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
const sha256 = async (s: string) => hex(await crypto.subtle.digest('SHA-256', enc.encode(s)));
async function hmac(key: ArrayBuffer | Uint8Array, data: string): Promise<ArrayBuffer> {
  const raw = key instanceof Uint8Array ? key.slice().buffer as ArrayBuffer : key;
  const k = await crypto.subtle.importKey('raw', raw, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return crypto.subtle.sign('HMAC', k, enc.encode(data));
}
const encodeRfc3986 = (s: string) => encodeURIComponent(s).replace(/[!'()*]/g, (c) => '%' + c.charCodeAt(0).toString(16).toUpperCase());

async function presignPut(opts: { endpoint: string; region: string; accessKey: string; secretKey: string; bucket: string; key: string; contentType: string; expires: number; publicAcl: boolean }) {
  const host = new URL(opts.endpoint).host;
  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, ''); // YYYYMMDDTHHMMSSZ
  const date = amzDate.slice(0, 8);
  const region = opts.region;
  const scope = `${date}/${region}/s3/aws4_request`;
  const canonicalUri = '/' + opts.bucket + '/' + opts.key.split('/').map(encodeRfc3986).join('/');
  // V160.2 — آروان برای هر شیء ACL جداگانه دارد؛ بدون x-amz-acl: public-read شیء خصوصی می‌شود و
  // خواندنش با HTTP 403 می‌شکند (حتی وقتی صندوقچه «عمومی» است). هدر باید امضا و توسط کلاینت ارسال شود.
  // V160.4 — کلاینت‌های قدیمی (APK قبل از V160.2 / سایت کش‌شده) هدر x-amz-acl را نمی‌فرستند؛ اگر امضا شامل آن باشد
  // SignatureDoesNotMatch می‌گیرند. فقط وقتی کلاینت اعلام کند (body.acl === 'public-read') امضا می‌شود.
  const signedHeaders = opts.publicAcl ? 'content-type;host;x-amz-acl' : 'content-type;host';
  const query: [string, string][] = [
    ['X-Amz-Algorithm', 'AWS4-HMAC-SHA256'],
    ['X-Amz-Credential', `${opts.accessKey}/${scope}`],
    ['X-Amz-Date', amzDate],
    ['X-Amz-Expires', String(opts.expires)],
    ['X-Amz-SignedHeaders', signedHeaders],
  ];
  query.sort((a, b) => (a[0] < b[0] ? -1 : 1));
  const canonicalQuery = query.map(([k, v]) => `${encodeRfc3986(k)}=${encodeRfc3986(v)}`).join('&');
  const canonicalHeaders = `content-type:${opts.contentType}\nhost:${host}\n` + (opts.publicAcl ? 'x-amz-acl:public-read\n' : '');
  const canonicalRequest = ['PUT', canonicalUri, canonicalQuery, canonicalHeaders, signedHeaders, 'UNSIGNED-PAYLOAD'].join('\n');
  const stringToSign = ['AWS4-HMAC-SHA256', amzDate, scope, await sha256(canonicalRequest)].join('\n');
  let k: ArrayBuffer = await hmac(enc.encode('AWS4' + opts.secretKey), date);
  k = await hmac(k, region);
  k = await hmac(k, 's3');
  k = await hmac(k, 'aws4_request');
  const signature = hex(await hmac(k, stringToSign));
  return `https://${host}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${signature}`;
}

/* V221 — حذف فایل‌های آپلودشده (action: 'delete', urls: [...]): فقط مالک (شناسهٔ کاربر در بخش دوم مسیر کلید)
   می‌تواند حذف کند؛ هم اشیای S3/R2 (publicBase) و هم باکت exam-images سوپابیس. best-effort و idempotent (404 = موفق). */
const OWNED_FOLDERS = ['questions', 'option_images', 'matching_images', 'audio', 'answers', 'profiles'];
const SUPABASE_BUCKET = 'exam-images';
const MAX_DELETE = 60;
async function hmacRaw(key: ArrayBuffer | Uint8Array, data: string): Promise<ArrayBuffer> { return hmac(key, data); }
async function s3Delete(opts: { endpoint: string; region: string; accessKey: string; secretKey: string; bucket: string; key: string }): Promise<boolean> {
  const host = new URL(opts.endpoint).host;
  const amzDate = new Date().toISOString().replace(/[:-]|\.\d{3}/g, '');
  const date = amzDate.slice(0, 8);
  const scope = `${date}/${opts.region}/s3/aws4_request`;
  const payloadHash = await sha256('');
  const canonicalUri = '/' + opts.bucket + '/' + opts.key.split('/').map(encodeRfc3986).join('/');
  const canonicalHeaders = `host:${host}\nx-amz-content-sha256:${payloadHash}\nx-amz-date:${amzDate}\n`;
  const signedHeaders = 'host;x-amz-content-sha256;x-amz-date';
  const canonicalRequest = ['DELETE', canonicalUri, '', canonicalHeaders, signedHeaders, payloadHash].join('\n');
  const stringToSign = ['AWS4-HMAC-SHA256', amzDate, scope, await sha256(canonicalRequest)].join('\n');
  let k: ArrayBuffer = await hmacRaw(enc.encode('AWS4' + opts.secretKey), date);
  k = await hmacRaw(k, opts.region); k = await hmacRaw(k, 's3'); k = await hmacRaw(k, 'aws4_request');
  const signature = hex(await hmacRaw(k, stringToSign));
  const auth = `AWS4-HMAC-SHA256 Credential=${opts.accessKey}/${scope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;
  const res = await fetch(`https://${host}${canonicalUri}`, { method: 'DELETE', headers: { Authorization: auth, 'x-amz-content-sha256': payloadHash, 'x-amz-date': amzDate } });
  return res.ok || res.status === 404;
}
function ownedKey(key: string, userId: string): boolean {
  const parts = key.split('/');
  return parts.length >= 3 && OWNED_FOLDERS.includes(parts[0]) && parts[1] === userId && !key.includes('..');
}
async function handleDelete(service: any, userId: string, body: Record<string, unknown>, s3: { endpoint: string; region: string; accessKey: string; secretKey: string; bucket: string; publicBase: string } | null) {
  const urls = Array.isArray(body.urls) ? (body.urls as unknown[]).filter((u) => typeof u === 'string').slice(0, MAX_DELETE) as string[] : [];
  const supabaseBase = env('SUPABASE_URL').replace(/\/+$/, '');
  const supaMarker = `${supabaseBase}/storage/v1/object/public/${SUPABASE_BUCKET}/`;
  const supaPaths: string[] = [];
  let deleted = 0, skipped = 0;
  for (const raw of urls) {
    const url = raw.split('?')[0];
    if (s3 && url.startsWith(s3.publicBase + '/')) {
      let key = '';
      try { key = decodeURIComponent(url.slice(s3.publicBase.length + 1)); } catch { skipped++; continue; }
      if (!ownedKey(key, userId)) { skipped++; continue; }
      try { if (await s3Delete({ ...s3, key })) deleted++; else skipped++; } catch { skipped++; }
    } else if (url.startsWith(supaMarker)) {
      let path = '';
      try { path = decodeURIComponent(url.slice(supaMarker.length)); } catch { skipped++; continue; }
      if (ownedKey(path, userId)) supaPaths.push(path); else skipped++;
    } else skipped++;
  }
  if (supaPaths.length) {
    const { error } = await service.storage.from(SUPABASE_BUCKET).remove(supaPaths);
    if (error) skipped += supaPaths.length; else deleted += supaPaths.length;
  }
  return json({ deleted, skipped });
}

Deno.serve(async (request) => {
  if (request.method === 'OPTIONS') return new Response('ok', { headers: CORS });
  if (request.method !== 'POST') return json({ error: 'روش درخواست مجاز نیست' }, 405);
  try {
    /* V221 — حذف: نیاز به پیکربندی S3 ندارد (برای باکت سوپابیس هم کار می‌کند) و برای هر نقش مجاز است (فقط فایل‌های خودِ کاربر) */
    const body0 = await request.clone().json().catch(() => ({})) as Record<string, unknown>;
    if (body0.action === 'delete') {
      const authorization0 = request.headers.get('Authorization') || '';
      if (!authorization0.startsWith('Bearer ')) return json({ error: 'ابتدا وارد شوید' }, 401);
      const service0 = createClient(env('SUPABASE_URL'), env('SUPABASE_SERVICE_ROLE_KEY'), { auth: { persistSession: false, autoRefreshToken: false } });
      const { data: auth0, error: authErr0 } = await service0.auth.getUser(authorization0.slice(7));
      if (authErr0 || !auth0.user) return json({ error: 'نشست نامعتبر است' }, 401);
      const ep = (env('S3_ENDPOINT') || (env('R2_ACCOUNT_ID') ? `https://${env('R2_ACCOUNT_ID')}.r2.cloudflarestorage.com` : '')).replace(/\/+$/, '');
      const ak = env('S3_ACCESS_KEY_ID') || env('R2_ACCESS_KEY_ID'), sk = env('S3_SECRET_ACCESS_KEY') || env('R2_SECRET_ACCESS_KEY');
      const pb = (env('S3_PUBLIC_BASE') || env('R2_PUBLIC_BASE')).replace(/\/+$/, '');
      const s3 = (/^https:\/\//.test(ep) && ak && sk && pb) ? { endpoint: ep, region: env('S3_REGION') || 'auto', accessKey: ak, secretKey: sk, bucket: env('S3_BUCKET') || env('R2_BUCKET') || 'azmoon-media', publicBase: pb } : null;
      return await handleDelete(service0, auth0.user.id, body0, s3);
    }
    const endpoint = (env('S3_ENDPOINT') || (env('R2_ACCOUNT_ID') ? `https://${env('R2_ACCOUNT_ID')}.r2.cloudflarestorage.com` : '')).replace(/\/+$/, '');
    const accessKey = env('S3_ACCESS_KEY_ID') || env('R2_ACCESS_KEY_ID'), secretKey = env('S3_SECRET_ACCESS_KEY') || env('R2_SECRET_ACCESS_KEY');
    const bucket = env('S3_BUCKET') || env('R2_BUCKET') || 'azmoon-media';
    const region = env('S3_REGION') || 'auto';
    const publicBase = (env('S3_PUBLIC_BASE') || env('R2_PUBLIC_BASE')).replace(/\/+$/, '');
    if (!/^https:\/\//.test(endpoint) || !accessKey || !secretKey || !publicBase) return json({ error: 'r2_not_configured', message: 'ذخیره‌سازی S3 روی سرور پیکربندی نشده است.' }, 503);

    const authorization = request.headers.get('Authorization') || '';
    if (!authorization.startsWith('Bearer ')) return json({ error: 'ابتدا وارد شوید' }, 401);
    const service = createClient(env('SUPABASE_URL'), env('SUPABASE_SERVICE_ROLE_KEY'), { auth: { persistSession: false, autoRefreshToken: false } });
    const { data: authData, error: authError } = await service.auth.getUser(authorization.slice(7));
    if (authError || !authData.user) return json({ error: 'نشست نامعتبر است' }, 401);
    const userId = authData.user.id;
    const { data: profile } = await service.from('profiles').select('role').eq('id', userId).maybeSingle();
    const role = String(profile?.role || '').toLowerCase();
    if (role !== 'teacher' && role !== 'manager') return json({ error: 'فقط معلم/مدیر می‌تواند فایل بارگذاری کند' }, 403);

    const body = await request.json().catch(() => ({})) as Record<string, unknown>;
    const kind = body.kind === 'audio' ? 'audio' : 'image';
    const ext = String(body.ext || (kind === 'audio' ? 'm4a' : 'webp')).toLowerCase().replace(/[^a-z0-9]/g, '');
    const table = kind === 'audio' ? AUDIO_EXT : IMAGE_EXT;
    const contentType = table[ext];
    if (!contentType) return json({ error: 'پسوند فایل مجاز نیست' }, 400);
    const size = Number(body.size || 0);
    const limit = kind === 'audio' ? MAX_AUDIO : MAX_IMAGE;
    if (!Number.isFinite(size) || size <= 0 || size > limit) return json({ error: `حجم فایل باید حداکثر ${Math.round(limit / 1024 / 1024)} مگابایت باشد` }, 400);
    const examId = String(body.exam_id || '').trim();
    if (!/^[A-Za-z0-9_-]{1,64}$/.test(examId)) return json({ error: 'شناسهٔ آزمون معتبر نیست' }, 400);
    const name = crypto.randomUUID();
    if (!UUID_RE.test(name)) return json({ error: 'uuid' }, 500);
    const FOLDERS = kind === 'audio' ? ['audio'] : ['questions', 'option_images', 'matching_images'];
    const folder = FOLDERS.includes(String(body.folder || '')) ? String(body.folder) : FOLDERS[0];
    const key = `${folder}/${userId}/${examId}/${name}.${ext}`;

    const publicAcl = String(body.acl || '') === 'public-read';
    const uploadUrl = await presignPut({ endpoint, region, accessKey, secretKey, bucket, key, contentType, expires: EXPIRES, publicAcl });
    const headers: Record<string, string> = { 'Content-Type': contentType };
    if (publicAcl) headers['x-amz-acl'] = 'public-read';
    return json({ upload_url: uploadUrl, public_url: `${publicBase}/${key}`, headers, expires_in: EXPIRES, key, public_acl: publicAcl });
  } catch (error) {
    console.error('media-upload', error);
    return json({ error: 'خطای داخلی صدور لینک آپلود' }, 500);
  }
});
