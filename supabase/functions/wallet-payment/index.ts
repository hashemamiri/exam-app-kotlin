import { createClient } from 'npm:@supabase/supabase-js@2.112.2';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
};
const JSON_HEADERS = { ...CORS, 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' };
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });

const PROVIDERS = new Set(['zarinpal', 'idpay', 'sandbox']);
const MIN_TOP_UP = 100_000;
const MAX_TOP_UP = 10_000_000;
const STEP = 10_000;

type Provider = 'zarinpal' | 'idpay' | 'sandbox';
type Order = {
  id: number;
  user_id: string;
  amount_toman: number;
  amount_rial: number;
  provider: Provider;
  authority: string | null;
  ref_id: string | null;
  status: string;
};
type GatewayResult = { ok: true; authority: string; url: string } | { ok: false; message: string; code: string };
type VerifyResult = { ok: true; refId: string } | { ok: false; message: string; code: string };

const env = (name: string) => (Deno.env.get(name) || '').trim();
const sandboxAllowed = () => env('PAY_ALLOW_SANDBOX').toLowerCase() === 'true';

function safeProvider(): Provider {
  const value = env('PAY_PROVIDER').toLowerCase();
  if (!PROVIDERS.has(value)) throw publicError('payment_not_configured', 'درگاه پرداخت هنوز پیکربندی نشده است.');
  if (value === 'sandbox' && !sandboxAllowed()) {
    throw publicError('sandbox_disabled', 'درگاه آزمایشی روی سرور مجاز نشده است.');
  }
  return value as Provider;
}

function publicError(code: string, message: string, status = 400) {
  return Object.assign(new Error(message), { publicCode: code, publicStatus: status });
}

function callbackUrl(req: Request): string {
  const requestUrl = new URL(req.url);
  return `${requestUrl.origin}${requestUrl.pathname}?action=verify`;
}

function validateAmount(raw: unknown): number {
  const amount = typeof raw === 'number' ? raw : Number(raw);
  if (!Number.isSafeInteger(amount) || amount < MIN_TOP_UP || amount > MAX_TOP_UP || amount % STEP !== 0) {
    throw publicError('invalid_amount', 'مبلغ باید بین ۱۰۰٬۰۰۰ تا ۱۰٬۰۰۰٬۰۰۰ و مضرب ۱۰٬۰۰۰ تومان باشد.');
  }
  return amount;
}

async function requestGateway(provider: Provider, order: Order, callback: string): Promise<GatewayResult> {
  if (provider === 'sandbox') {
    const authority = `SB-${order.id}-${crypto.randomUUID()}`;
    const url = `${callback}&provider=sandbox&Authority=${encodeURIComponent(authority)}&Status=OK`;
    return { ok: true, authority, url };
  }

  if (provider === 'zarinpal') {
    const merchant = env('PAY_ZARINPAL_MERCHANT');
    if (!/^[0-9a-f-]{36}$/i.test(merchant)) {
      return { ok: false, code: 'zarinpal_not_configured', message: 'مرچنت زرین‌پال پیکربندی نشده است.' };
    }
    const response = await fetch('https://payment.zarinpal.com/pg/v4/payment/request.json', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({
        merchant_id: merchant,
        amount: order.amount_rial,
        currency: 'IRR',
        description: `شارژ کیف پول سامانه آزمون - سفارش ${order.id}`,
        callback_url: callback,
        metadata: { order_id: String(order.id) },
      }),
    });
    const body = await response.json().catch(() => ({}));
    if (response.ok && body?.data?.code === 100 && typeof body?.data?.authority === 'string') {
      const authority = body.data.authority;
      return { ok: true, authority, url: `https://payment.zarinpal.com/pg/StartPay/${authority}` };
    }
    return { ok: false, code: `zarinpal_${body?.errors?.code || response.status}`, message: 'اتصال به زرین‌پال ناموفق بود.' };
  }

  const apiKey = env('PAY_IDPAY_API_KEY');
  if (!apiKey) return { ok: false, code: 'idpay_not_configured', message: 'کلید آیدی‌پی پیکربندی نشده است.' };
  const response = await fetch('https://api.idpay.ir/v1.1/payment', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      'X-API-KEY': apiKey,
      'X-SANDBOX': '0',
    },
    body: JSON.stringify({
      order_id: String(order.id),
      amount: order.amount_rial,
      callback,
      desc: `شارژ کیف پول سامانه آزمون - سفارش ${order.id}`,
    }),
  });
  const body = await response.json().catch(() => ({}));
  if (response.ok && typeof body?.id === 'string' && typeof body?.link === 'string') {
    return { ok: true, authority: body.id, url: body.link };
  }
  return { ok: false, code: `idpay_${body?.error_code || response.status}`, message: 'اتصال به آیدی‌پی ناموفق بود.' };
}

async function verifyGateway(provider: Provider, order: Order, params: URLSearchParams): Promise<VerifyResult> {
  if (provider === 'sandbox') {
    if (!sandboxAllowed() || !order.authority?.startsWith(`SB-${order.id}-`)) {
      return { ok: false, code: 'invalid_sandbox', message: 'پرداخت آزمایشی معتبر نیست.' };
    }
    return { ok: true, refId: `SB-${order.id}-${Date.now()}` };
  }

  if (provider === 'zarinpal') {
    const merchant = env('PAY_ZARINPAL_MERCHANT');
    const response = await fetch('https://payment.zarinpal.com/pg/v4/payment/verify.json', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ merchant_id: merchant, amount: order.amount_rial, authority: order.authority }),
    });
    const body = await response.json().catch(() => ({}));
    const code = Number(body?.data?.code);
    if (response.ok && (code === 100 || code === 101) && body?.data?.ref_id != null) {
      return { ok: true, refId: String(body.data.ref_id) };
    }
    return { ok: false, code: `zarinpal_verify_${body?.errors?.code || response.status}`, message: 'تأیید پرداخت زرین‌پال ناموفق بود.' };
  }

  const apiKey = env('PAY_IDPAY_API_KEY');
  const callbackOrderId = params.get('order_id');
  const callbackAmount = Number(params.get('amount') || order.amount_rial);
  if (callbackOrderId && callbackOrderId !== String(order.id)) {
    return { ok: false, code: 'idpay_order_mismatch', message: 'شناسه سفارش با پرداخت مطابقت ندارد.' };
  }
  if (callbackAmount !== order.amount_rial) {
    return { ok: false, code: 'idpay_amount_mismatch', message: 'مبلغ بازگشتی با سفارش مطابقت ندارد.' };
  }
  const response = await fetch('https://api.idpay.ir/v1.1/payment/verify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      'X-API-KEY': apiKey,
      'X-SANDBOX': '0',
    },
    body: JSON.stringify({ id: order.authority, order_id: String(order.id) }),
  });
  const body = await response.json().catch(() => ({}));
  const status = Number(body?.status);
  if (
    response.ok && (status === 100 || status === 101) &&
    String(body?.id) === order.authority && String(body?.order_id) === String(order.id) &&
    Number(body?.amount) === order.amount_rial && body?.track_id != null
  ) {
    return { ok: true, refId: String(body.track_id) };
  }
  return { ok: false, code: `idpay_verify_${body?.error_code || response.status}`, message: 'تأیید پرداخت آیدی‌پی ناموفق بود.' };
}

async function callbackParameters(req: Request, url: URL): Promise<URLSearchParams> {
  const params = new URLSearchParams(url.searchParams);
  if (req.method === 'POST') {
    const type = req.headers.get('content-type') || '';
    if (type.includes('application/x-www-form-urlencoded')) {
      const body = new URLSearchParams(await req.text());
      body.forEach((value, key) => params.set(key, value));
    } else if (type.includes('application/json')) {
      const body = await req.json().catch(() => ({}));
      Object.entries(body as Record<string, unknown>).forEach(([key, value]) => {
        if (value != null) params.set(key, String(value));
      });
    }
  }
  return params;
}

function callbackAuthority(params: URLSearchParams): string {
  return params.get('Authority') || params.get('authority') || params.get('id') || '';
}

function callbackCanceled(provider: Provider, params: URLSearchParams): boolean {
  const status = (params.get('Status') || params.get('status') || '').toUpperCase();
  if (provider === 'zarinpal' || provider === 'sandbox') return status !== 'OK';
  return status !== '' && !['10', '100', '101'].includes(status);
}

function escapeHtml(value: unknown): string {
  return String(value ?? '').replace(/[&<>'"]/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;',
  }[char] || char));
}

function paymentPage(ok: boolean, message: string, reference = ''): Response {
  const title = ok ? 'پرداخت موفق' : 'پرداخت ناموفق';
  const safeReference = reference ? `<p>شماره پیگیری:<br><code>${escapeHtml(reference)}</code></p>` : '';
  return new Response(`<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title>
<style>body{font-family:Tahoma,sans-serif;background:#101827;color:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:20px}.card{max-width:420px;background:#1f2937;border:1px solid #374151;border-radius:18px;padding:30px;text-align:center}.icon{font-size:54px}p{line-height:1.9;color:#d1d5db}code{direction:ltr;display:inline-block;background:#111827;padding:6px 12px;border-radius:8px;color:#93c5fd}b{display:block;margin-top:18px;color:#a7f3d0}</style></head>
<body><main class="card"><div class="icon">${ok ? '✅' : '❌'}</div><h2>${title}</h2>
<p>${escapeHtml(message)}</p>${safeReference}<b>به برنامه برگردید و موجودی را تازه‌سازی کنید.</b></main></body></html>`, {
    status: ok ? 200 : 400,
    headers: { ...CORS, 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' },
  });
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: CORS });
  const url = new URL(req.url);
  const service = createClient(env('SUPABASE_URL'), env('SUPABASE_SERVICE_ROLE_KEY'), {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // بازگشت بانک عمومی است، اما authority فقط با سفارش سرور و verify رسمی پذیرفته می‌شود.
  if (url.searchParams.get('action') === 'verify' || req.method === 'GET') {
    try {
      const params = await callbackParameters(req, url);
      const authority = callbackAuthority(params);
      if (!authority || authority.length > 200) return paymentPage(false, 'اطلاعات بازگشت ناقص است.');
      const { data: orderData, error: orderError } = await service
        .from('wallet_payment_orders').select('*').eq('authority', authority).maybeSingle();
      if (orderError || !orderData) return paymentPage(false, 'سفارش پرداخت یافت نشد.');
      const order = orderData as Order;
      if (order.status === 'paid') return paymentPage(true, 'این پرداخت قبلاً با موفقیت ثبت شده است.', order.ref_id || '');
      if (callbackCanceled(order.provider, params)) {
        await service.rpc('native_fail_wallet_payment_order', { p_order: order.id, p_status: 'canceled', p_code: 'user_canceled' });
        return paymentPage(false, 'پرداخت لغو شد.');
      }

      await service.from('wallet_payment_orders').update({ status: 'verifying' })
        .eq('id', order.id).in('status', ['requested', 'verifying']);
      const verified = await verifyGateway(order.provider, order, params);
      if (!verified.ok) {
        // خطای verify ممکن است موقت باشد؛ سفارش باز می‌ماند تا callback دوباره قابل بررسی باشد.
        await service.from('wallet_payment_orders').update({ status: 'requested', error_code: verified.code })
          .eq('id', order.id).eq('status', 'verifying');
        return paymentPage(false, verified.message);
      }
      const { data: credit, error: creditError } = await service.rpc('native_credit_wallet_payment', {
        p_order: order.id,
        p_ref: verified.refId,
      });
      if (creditError || credit?.error) return paymentPage(false, 'پرداخت تأیید شد اما ثبت موجودی نیازمند بررسی پشتیبانی است.');
      return paymentPage(true, 'مبلغ با موفقیت به کیف پول افزوده شد.', verified.refId);
    } catch {
      return paymentPage(false, 'بررسی پرداخت کامل نشد؛ دوباره از برنامه موجودی را تازه‌سازی کنید.');
    }
  }

  if (req.method !== 'POST') return json({ error: 'روش درخواست مجاز نیست.', code: 'method_not_allowed' }, 405);
  try {
    const authorization = req.headers.get('Authorization') || '';
    if (!authorization.startsWith('Bearer ')) throw publicError('unauthorized', 'ابتدا وارد شوید.', 401);
    const token = authorization.slice(7);
    const { data: userData, error: userError } = await service.auth.getUser(token);
    if (userError || !userData.user) throw publicError('invalid_session', 'نشست ورود معتبر نیست.', 401);

    const body = await req.json().catch(() => { throw publicError('invalid_json', 'داده درخواست نامعتبر است.'); });
    const amountToman = validateAmount((body as Record<string, unknown>).amount_toman);
    const provider = safeProvider();
    const { data: created, error: createError } = await service.rpc('native_create_wallet_payment_order', {
      p_user: userData.user.id,
      p_amount_toman: amountToman,
      p_provider: provider,
    });
    if (createError) throw publicError('order_create_failed', 'ساخت سفارش ناموفق بود.', 409);
    if (created?.error) throw publicError('order_rejected', String(created.error), 409);

    const orderId = Number(created.id);
    const order: Order = {
      id: orderId,
      user_id: userData.user.id,
      amount_toman: amountToman,
      amount_rial: amountToman * 10,
      provider,
      authority: null,
      ref_id: null,
      status: 'pending',
    };
    let gateway: GatewayResult;
    try {
      gateway = await requestGateway(provider, order, callbackUrl(req));
    } catch {
      await service.rpc('native_fail_wallet_payment_order', {
        p_order: orderId, p_status: 'failed', p_code: 'gateway_network_error',
      });
      throw publicError('gateway_network_error', 'ارتباط با درگاه برقرار نشد؛ دوباره تلاش کنید.', 502);
    }
    if (!gateway.ok) {
      await service.rpc('native_fail_wallet_payment_order', {
        p_order: orderId, p_status: 'failed', p_code: gateway.code,
      });
      throw publicError(gateway.code, gateway.message, 502);
    }
    const { data: started, error: startError } = await service.rpc('native_set_wallet_payment_authority', {
      p_order: orderId,
      p_authority: gateway.authority,
    });
    if (startError || started?.error) {
      await service.rpc('native_fail_wallet_payment_order', {
        p_order: orderId, p_status: 'failed', p_code: 'authority_store_failed',
      });
      throw publicError('order_start_failed', 'شروع سفارش ناموفق بود.', 500);
    }

    // اعتبار آزمایشی فقط در sandbox مجاز سرور و از همان RPC اتمیک credit می‌شود.
    // APK هیچ مسیر مستقیمی برای افزایش موجودی ندارد و provider واقعی هرگز وارد این شاخه نمی‌شود.
    if (provider === 'sandbox') {
      const refId = `SB-${orderId}-${Date.now()}`;
      const { data: credit, error: creditError } = await service.rpc('native_credit_wallet_payment', {
        p_order: orderId,
        p_ref: refId,
      });
      if (creditError || credit?.error) {
        await service.rpc('native_fail_wallet_payment_order', {
          p_order: orderId, p_status: 'failed', p_code: 'sandbox_credit_failed',
        });
        throw publicError('sandbox_credit_failed', 'ثبت اعتبار آزمایشی کامل نشد.', 500);
      }
      return json({
        ok: true,
        credited: true,
        order_id: orderId,
        provider,
        sandbox: true,
        balance: credit?.balance ?? null,
      });
    }

    return json({ ok: true, credited: false, url: gateway.url, order_id: orderId, provider, sandbox: false });
  } catch (error) {
    const known = error as Error & { publicCode?: string; publicStatus?: number };
    return json({
      error: known.publicCode ? known.message : 'شروع پرداخت ناموفق بود.',
      code: known.publicCode || 'payment_start_failed',
    }, known.publicStatus || 500);
  }
});
