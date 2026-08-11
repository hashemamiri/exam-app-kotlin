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
const MAX_OBJECTS = 20_000;

type StorageItem = {
  name: string;
  id?: string | null;
  created_at?: string | null;
  updated_at?: string | null;
  metadata?: Record<string, unknown> | null;
};

type ListedFile = { path: string; createdAt: number };

function collectStrings(value: unknown, output: Set<string>) {
  if (typeof value === 'string') {
    output.add(value);
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item) => collectStrings(item, output));
    return;
  }
  if (value && typeof value === 'object') {
    Object.values(value as Record<string, unknown>).forEach((item) => collectStrings(item, output));
  }
}

function objectPathFromPublicUrl(value: string, bucket: string): string | null {
  const marker = `/storage/v1/object/public/${bucket}/`;
  const index = value.indexOf(marker);
  if (index < 0) return null;
  try {
    return decodeURIComponent(value.slice(index + marker.length).split('?')[0]);
  } catch {
    return null;
  }
}

async function fetchAllRows(
  client: any,
  table: string,
  columns: string,
): Promise<Record<string, unknown>[]> {
  const output: Record<string, unknown>[] = [];
  for (let from = 0; ; from += 1000) {
    const { data, error } = await client.from(table).select(columns).range(from, from + 999);
    if (error) throw new Error(`read_${table}`);
    const rows = (data || []) as Record<string, unknown>[];
    output.push(...rows);
    if (rows.length < 1000) break;
    if (output.length > 100_000) throw new Error(`row_limit_${table}`);
  }
  return output;
}

async function listTree(
  client: any,
  bucket: string,
): Promise<ListedFile[]> {
  const result: ListedFile[] = [];
  const folders = [''];
  while (folders.length) {
    const prefix = folders.shift()!;
    for (let offset = 0; ; offset += 1000) {
      const { data, error } = await client.storage.from(bucket).list(prefix, {
        limit: 1000,
        offset,
        sortBy: { column: 'name', order: 'asc' },
      });
      if (error) throw new Error(`list_${bucket}`);
      const items = (data || []) as StorageItem[];
      for (const item of items) {
        const path = prefix ? `${prefix}/${item.name}` : item.name;
        if (item.id || item.metadata) {
          result.push({
            path,
            createdAt: Date.parse(item.created_at || item.updated_at || '') || 0,
          });
          if (result.length > MAX_OBJECTS) throw new Error(`object_limit_${bucket}`);
        } else {
          folders.push(path);
        }
      }
      if (items.length < 1000) break;
    }
  }
  return result;
}

async function removeChunks(
  client: any,
  bucket: string,
  paths: string[],
) {
  for (let index = 0; index < paths.length; index += 100) {
    const { error } = await client.storage.from(bucket).remove(paths.slice(index, index + 100));
    if (error) throw new Error(`remove_${bucket}`);
  }
}

Deno.serve(async (request) => {
  if (request.method === 'OPTIONS') return new Response('ok', { headers: CORS });
  if (request.method !== 'POST') return json({ error: 'روش درخواست مجاز نیست' }, 405);
  try {
    const authorization = request.headers.get('Authorization') || '';
    if (!authorization.startsWith('Bearer ')) return json({ error: 'ابتدا وارد شوید' }, 401);
    const service = createClient(env('SUPABASE_URL'), env('SUPABASE_SERVICE_ROLE_KEY'), {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: authData, error: authError } = await service.auth.getUser(authorization.slice(7));
    if (authError || !authData.user) return json({ error: 'نشست نامعتبر است' }, 401);
    const userId = authData.user.id;
    const { data: profile } = await service.from('profiles').select('role').eq('id', userId).maybeSingle();
    if (profile?.role !== 'teacher') return json({ error: 'فقط معلم دسترسی دارد' }, 403);

    const body = await request.json().catch(() => ({})) as Record<string, unknown>;
    const dryRun = body.dry_run !== false;
    const graceDays = Math.min(90, Math.max(1, Number(body.grace_days || 7)));
    const retention = Math.min(20, Math.max(2, Number(body.apk_retention || 5)));
    if (!dryRun) {
      if (env('MAINTENANCE_DELETE_ENABLED').toLowerCase() !== 'true') {
        return json({ error: 'حذف واقعی در تنظیمات Edge Function فعال نشده است' }, 403);
      }
      if (!env('MAINTENANCE_ALLOWED_USER_ID') || env('MAINTENANCE_ALLOWED_USER_ID') !== userId) {
        return json({ error: 'این حساب اجازه اجرای حذف واقعی را ندارد' }, 403);
      }
      const { data: recent } = await service.from('maintenance_audit').select('id')
        .eq('requested_by', userId).eq('dry_run', false)
        .gte('created_at', new Date(Date.now() - 10 * 60_000).toISOString()).limit(1);
      if (recent?.length) return json({ error: 'بین دو پاک‌سازی واقعی حداقل ده دقیقه فاصله لازم است' }, 429);
    }

    const rawReferences = new Set<string>();
    const [profiles, exams, bank, answers, answerTrash, answerDrafts, activeVersions] = await Promise.all([
      fetchAllRows(service, 'profiles', 'avatar_url'),
      fetchAllRows(service, 'exams', 'questions'),
      fetchAllRows(service, 'question_bank', 'question'),
      fetchAllRows(service, 'answers', 'response_images'),
      fetchAllRows(service, 'answers_trash', '*'),
      fetchAllRows(service, 'answer_drafts', '*'),
      fetchAllRows(service, 'app_version', 'apk_url,is_active,published_at'),
    ]);
    [profiles, exams, bank, answers, answerTrash, answerDrafts]
      .forEach((rows) => rows.forEach((row) => collectStrings(row, rawReferences)));
    const examReferences = new Set<string>();
    rawReferences.forEach((value) => {
      const path = objectPathFromPublicUrl(value, 'exam-images');
      if (path) examReferences.add(path);
    });

    const cutoff = Date.now() - graceDays * 86_400_000;
    const examObjects = await listTree(service, 'exam-images');
    const orphanPaths = examObjects
      .filter((item) => item.createdAt > 0 && item.createdAt < cutoff && !examReferences.has(item.path))
      .map((item) => item.path);

    const apkObjects = (await listTree(service, 'app-updates'))
      .filter((item) => item.path.toLowerCase().endsWith('.apk'))
      .sort((a, b) => b.createdAt - a.createdAt);
    const activeApkPaths = new Set<string>();
    activeVersions.forEach((row) => {
      if (row.is_active !== true || typeof row.apk_url !== 'string') return;
      const path = objectPathFromPublicUrl(row.apk_url, 'app-updates');
      if (path) activeApkPaths.add(path);
    });
    const keepApks = new Set(apkObjects.slice(0, retention).map((item) => item.path));
    activeApkPaths.forEach((path) => keepApks.add(path));
    const oldApks = apkObjects
      .filter((item) => item.createdAt > 0 && item.createdAt < cutoff && !keepApks.has(item.path))
      .map((item) => item.path);

    if (!dryRun) {
      await removeChunks(service, 'exam-images', orphanPaths);
      await removeChunks(service, 'app-updates', oldApks);
    }
    await service.from('maintenance_audit').insert({
      requested_by: userId,
      dry_run: dryRun,
      orphan_candidates: orphanPaths.length,
      apk_candidates: oldApks.length,
      deleted_objects: dryRun ? 0 : orphanPaths.length,
      deleted_apks: dryRun ? 0 : oldApks.length,
      grace_days: graceDays,
    });

    return json({
      ok: true,
      dry_run: dryRun,
      referenced_exam_objects: examReferences.size,
      scanned_exam_objects: examObjects.length,
      orphan_candidates: orphanPaths.length,
      scanned_apks: apkObjects.length,
      apk_candidates: oldApks.length,
      deleted_objects: dryRun ? 0 : orphanPaths.length,
      deleted_apks: dryRun ? 0 : oldApks.length,
    });
  } catch (error) {
    const code = error instanceof Error ? error.message.slice(0, 80) : 'maintenance_failed';
    return json({ error: 'پاک‌سازی کامل نشد', code }, 500);
  }
});
