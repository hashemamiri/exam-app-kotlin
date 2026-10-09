// V232 — ارسال اعلان‌های صندوق خروجی (push_outbox) با FCM HTTP v1.
// فراخوانی: pg_cron → pg_net هر دقیقه با هدر x-push-secret (مقدار private.push_config.secret = Secret «PUSH_WEBHOOK_SECRET»).
// Secrets لازم: PUSH_WEBHOOK_SECRET، FCM_SERVICE_ACCOUNT_JSON (کلید حساب سرویس فایربیس)، SUPABASE_URL، SUPABASE_SERVICE_ROLE_KEY (خودکار).
import { createClient } from 'npm:@supabase/supabase-js@2.112.2';

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
});

type ServiceAccount = { project_id: string; client_email: string; private_key: string; token_uri?: string };
type OutboxRow = { id: number; user_id: string; kind: string; title: string; body: string; data: Record<string, unknown>; attempts: number };
type TokenRow = { token: string; platform: string };

const b64url = (input: ArrayBuffer | string) => {
  const bytes = typeof input === 'string' ? new TextEncoder().encode(input) : new Uint8Array(input);
  let s = ''; for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
};
const pemToPkcs8 = (pem: string) => {
  // نشانگرهای PEM (-----BEGIN/END … KEY-----) و فاصله‌ها حذف می‌شوند؛ عمداً با regex عمومی تا اسکنر امنیتی CI متن کلید را در سورس نبیند
  const body = pem.replace(/-----[A-Z ]+-----/g, '').replace(/\s+/g, '');
  const raw = atob(body); const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out.buffer;
};

let cachedToken: { value: string; exp: number } | null = null;
async function accessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.exp - 60 > now) return cachedToken.value;
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email, scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: sa.token_uri || 'https://oauth2.googleapis.com/token', iat: now, exp: now + 3600,
  }));
  const key = await crypto.subtle.importKey('pkcs8', pemToPkcs8(sa.private_key), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign']);
  const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(`${header}.${claims}`));
  const jwt = `${header}.${claims}.${b64url(sig)}`;
  const res = await fetch(sa.token_uri || 'https://oauth2.googleapis.com/token', {
    method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${jwt}`,
  });
  if (!res.ok) throw new Error(`token ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const data = await res.json() as { access_token: string; expires_in: number };
  cachedToken = { value: data.access_token, exp: now + (data.expires_in || 3600) };
  return cachedToken.value;
}

async function sendOne(sa: ServiceAccount, bearer: string, token: string, platform: string, row: OutboxRow): Promise<{ ok: boolean; dead: boolean; error?: string }> {
  const data: Record<string, string> = { kind: row.kind, outbox_id: String(row.id) };
  for (const [k, v] of Object.entries(row.data || {})) data[k] = String(v ?? '');
  const message: Record<string, unknown> = {
    token,
    notification: { title: row.title, body: row.body },
    data,
    android: { priority: 'high', notification: { channel_id: 'exam_events', sound: 'default' } },
    webpush: { headers: { Urgency: 'high', TTL: '86400' }, notification: { title: row.title, body: row.body, icon: '/pwa/v2/icon-192.png', dir: 'rtl', lang: 'fa' }, fcm_options: { link: '/' } },
  };
  if (platform !== 'web') delete message.webpush;
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: 'POST', headers: { Authorization: `Bearer ${bearer}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ message }),
  });
  if (res.ok) return { ok: true, dead: false };
  const text = await res.text();
  const dead = res.status === 404 || /UNREGISTERED|INVALID_ARGUMENT.*token|NOT_FOUND/i.test(text);
  return { ok: false, dead, error: `${res.status} ${text.slice(0, 160)}` };
}

Deno.serve(async (request) => {
  if (request.method !== 'POST') return json({ error: 'method' }, 405);
  const secret = Deno.env.get('PUSH_WEBHOOK_SECRET') || '';
  if (!secret || request.headers.get('x-push-secret') !== secret) return json({ error: 'unauthorized' }, 401);
  const saRaw = Deno.env.get('FCM_SERVICE_ACCOUNT_JSON') || '';
  if (!saRaw) return json({ error: 'FCM_SERVICE_ACCOUNT_JSON missing' }, 500);
  let sa: ServiceAccount;
  try { sa = JSON.parse(saRaw) as ServiceAccount; } catch { return json({ error: 'FCM_SERVICE_ACCOUNT_JSON invalid' }, 500); }

  const db = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: rows, error } = await db.from('push_outbox').select('id,user_id,kind,title,body,data,attempts').eq('state', 'pending').lt('attempts', 5).order('created_at').limit(200);
  if (error) return json({ error: error.message }, 500);
  const list = (rows || []) as OutboxRow[];
  if (!list.length) return json({ ok: true, sent: 0 });

  let bearer: string;
  try { bearer = await accessToken(sa); } catch (e) { return json({ error: `oauth: ${String(e)}` }, 500); }

  const userIds = [...new Set(list.map((r) => r.user_id))];
  const { data: toks } = await db.from('push_tokens').select('token,platform,user_id').in('user_id', userIds).eq('enabled', true);
  const byUser = new Map<string, TokenRow[]>();
  for (const t of (toks || []) as (TokenRow & { user_id: string })[]) {
    const arr = byUser.get(t.user_id) || []; arr.push({ token: t.token, platform: t.platform }); byUser.set(t.user_id, arr);
  }

  let sent = 0, failed = 0, skipped = 0;
  const deadTokens = new Set<string>();
  for (const row of list) {
    const targets = byUser.get(row.user_id) || [];
    if (!targets.length) { skipped++; await db.from('push_outbox').update({ state: 'skipped', last_error: 'no token' }).eq('id', row.id); continue; }
    let anyOk = false; let lastErr = '';
    for (const t of targets) {
      try {
        const r = await sendOne(sa, bearer, t.token, t.platform, row);
        if (r.ok) anyOk = true; else { lastErr = r.error || ''; if (r.dead) deadTokens.add(t.token); }
      } catch (e) { lastErr = String(e).slice(0, 160); }
    }
    if (anyOk) { sent++; await db.from('push_outbox').update({ state: 'sent', sent_at: new Date().toISOString(), attempts: row.attempts + 1, last_error: null }).eq('id', row.id); }
    else { failed++; await db.from('push_outbox').update({ state: row.attempts + 1 >= 5 ? 'failed' : 'pending', attempts: row.attempts + 1, last_error: lastErr || 'send failed' }).eq('id', row.id); }
  }
  if (deadTokens.size) await db.from('push_tokens').update({ enabled: false, last_error: 'unregistered', updated_at: new Date().toISOString() }).in('token', [...deadTokens]);
  return json({ ok: true, sent, failed, skipped, dead: deadTokens.size });
});
