// V144 — صدور لینک آپلود موقت (Presigned PUT) برای Cloudflare R2
// کلاینت (سایت/اپ) پس از ورود، {kind:'image'|'audio', exam_id, ext, size} می‌فرستد و
// {upload_url, public_url, headers} می‌گیرد؛ سپس فایل را مستقیم با PUT به R2 می‌فرستد.
// کلیدهای R2 فقط در Secrets این تابع هستند: R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY,
// R2_BUCKET (پیش‌فرض azmoon-media), R2_PUBLIC_BASE (مثلاً https://media.onlineexam.ir)
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
  const k = await crypto.subtle.importKey('raw', key, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return crypto.subtle.sign('HMAC', k, enc.encode(data));
}
const encodeRfc3986 = (s: string) => encodeURIComponent(s).replace(/[!'()*]/g, (c) => '%' + c.charCodeAt(0).toString(16).toUpperCase());

async function presignPut(opts: { accountId: string; accessKey: string; secretKey: string; bucket: string; key: string; contentType: string; expires: number }) {
  const host = `${opts.accountId}.r2.cloudflarestorage.com`;
  const now = new Date();
  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, ''); // YYYYMMDDTHHMMSSZ
  const date = amzDate.slice(0, 8);
  const region = 'auto';
  const scope = `${date}/${region}/s3/aws4_request`;
  const canonicalUri = '/' + opts.bucket + '/' + opts.key.split('/').map(encodeRfc3986).join('/');
  const signedHeaders = 'content-type;host';
  const query: [string, string][] = [
    ['X-Amz-Algorithm', 'AWS4-HMAC-SHA256'],
    ['X-Amz-Credential', `${opts.accessKey}/${scope}`],
    ['X-Amz-Date', amzDate],
    ['X-Amz-Expires', String(opts.expires)],
    ['X-Amz-SignedHeaders', signedHeaders],
  ];
  query.sort((a, b) => (a[0] < b[0] ? -1 : 1));
  const canonicalQuery = query.map(([k, v]) => `${encodeRfc3986(k)}=${encodeRfc3986(v)}`).join('&');
  const canonicalHeaders = `content-type:${opts.contentType}\nhost:${host}\n`;
  const canonicalRequest = ['PUT', canonicalUri, canonicalQuery, canonicalHeaders, signedHeaders, 'UNSIGNED-PAYLOAD'].join('\n');
  const stringToSign = ['AWS4-HMAC-SHA256', amzDate, scope, await sha256(canonicalRequest)].join('\n');
  let k: ArrayBuffer = await hmac(enc.encode('AWS4' + opts.secretKey), date);
  k = await hmac(k, region);
  k = await hmac(k, 's3');
  k = await hmac(k, 'aws4_request');
  const signature = hex(await hmac(k, stringToSign));
  return `https://${host}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${signature}`;
}

Deno.serve(async (request) => {
  if (request.method === 'OPTIONS') return new Response('ok', { headers: CORS });
  if (request.method !== 'POST') return json({ error: 'روش درخواست مجاز نیست' }, 405);
  try {
    const accountId = env('R2_ACCOUNT_ID'), accessKey = env('R2_ACCESS_KEY_ID'), secretKey = env('R2_SECRET_ACCESS_KEY');
    const bucket = env('R2_BUCKET') || 'azmoon-media';
    const publicBase = env('R2_PUBLIC_BASE').replace(/\/+$/, '');
    if (!accountId || !accessKey || !secretKey || !publicBase) return json({ error: 'r2_not_configured', message: 'ذخیره‌سازی R2 روی سرور پیکربندی نشده است.' }, 503);

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

    const uploadUrl = await presignPut({ accountId, accessKey, secretKey, bucket, key, contentType, expires: EXPIRES });
    return json({ upload_url: uploadUrl, public_url: `${publicBase}/${key}`, headers: { 'Content-Type': contentType }, expires_in: EXPIRES, key });
  } catch (error) {
    console.error('media-upload', error);
    return json({ error: 'خطای داخلی صدور لینک آپلود' }, 500);
  }
});
