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
const userEmail = (username: string) => `${username.toLowerCase()}@student.exam.local`;
const validUsername = (value: string) => /^[a-z0-9_]{4,20}$/.test(value);
const validGender = (value: string) => value === '' || value === 'male' || value === 'female';
const joinName = (first: string, last: string) => [first.trim(), last.trim()].filter(Boolean).join(' ');
const clean = (value: unknown, max: number) => String(value ?? '').trim().slice(0, max);

Deno.serve(async (request) => {
  if (request.method === 'OPTIONS') return new Response('ok', { headers: CORS });
  if (request.method !== 'POST') return json({ error: 'روش درخواست مجاز نیست' }, 405);
  try {
    const contentLength = Number(request.headers.get('content-length') || 0);
    if (contentLength > 262_144) return json({ error: 'حجم درخواست بیش از حد مجاز است' }, 413);
    const authorization = request.headers.get('Authorization') || '';
    if (!authorization.startsWith('Bearer ')) return json({ error: 'ابتدا وارد شوید' }, 401);

    const service = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
      { auth: { persistSession: false, autoRefreshToken: false } },
    );
    const { data: authData, error: authError } = await service.auth.getUser(authorization.slice(7));
    if (authError || !authData.user) return json({ error: 'نشست نامعتبر است' }, 401);
    const teacherId = authData.user.id;
    const { data: teacher } = await service.from('profiles').select('role').eq('id', teacherId).maybeSingle();
    if (teacher?.role !== 'teacher' && teacher?.role !== 'manager') return json({ error: 'فقط کادر مدرسه دسترسی دارد' }, 403);

    const body = await request.json().catch(() => null) as Record<string, unknown> | null;
    if (!body) return json({ error: 'داده درخواست معتبر نیست' }, 400);
    const action = clean(body.action, 32);

    const ensureOwnedClass = async (classId: string) => {
      if (!classId) return true;
      const { data } = await service.from('classes').select('id').eq('id', classId)
        .eq('teacher_id', teacherId).maybeSingle();
      return !!data;
    };
    const ownedStudent = async (id: string) => {
      const { data } = await service.from('profiles')
        .select('id, teacher_id, username, full_name, first_name, last_name, gender')
        .eq('id', id).eq('role', 'student').maybeSingle();
      if (data?.teacher_id === teacherId) return data;
      if (teacher?.role === 'manager' && data?.id) {
        const { data: membership } = await service.from('school_memberships')
          .select('school_id').eq('user_id', teacherId).eq('staff_role', 'manager').eq('status', 'active').maybeSingle();
        if (membership?.school_id) {
          const { data: linked } = await service.from('school_students').select('student_id')
            .eq('school_id', membership.school_id).eq('student_id', data.id).maybeSingle();
          if (linked) return data;
        }
      }
      return null;
    };
    const audit = async (event: string, target: string | null, details: Record<string, unknown> = {}) => {
      try {
        await service.from('student_admin_audit').insert({
          teacher_id: teacherId,
          student_id: target || null,
          action: event,
          details,
        });
      } catch {
        // Audit failure must not expose credentials or roll back an already completed Auth operation.
      }
    };

    if (action === 'create') {
      const firstName = clean(body.first_name, 100);
      const lastName = clean(body.last_name, 100);
      const fullName = joinName(firstName, lastName) || clean(body.full_name, 200);
      const username = clean(body.username, 20).toLowerCase();
      const password = String(body.password || '');
      const gender = clean(body.gender, 12);
      const classId = clean(body.class_id, 64);
      if (!fullName) return json({ error: 'نام دانش‌آموز را وارد کنید' }, 400);
      if (!validUsername(username)) return json({ error: 'نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد' }, 400);
      if (password.length < 8 || password.length > 72) return json({ error: 'رمز باید بین ۸ تا ۷۲ کاراکتر باشد' }, 400);
      if (!validGender(gender)) return json({ error: 'جنسیت نامعتبر است' }, 400);
      if (!(await ensureOwnedClass(classId))) return json({ error: 'کلاس متعلق به این معلم نیست' }, 403);
      const { data: duplicate } = await service.from('profiles').select('id').eq('username', username).maybeSingle();
      if (duplicate) return json({ error: 'این نام کاربری قبلاً استفاده شده است' }, 409);

      const { data: created, error: createError } = await service.auth.admin.createUser({
        email: userEmail(username), password, email_confirm: true,
        user_metadata: { full_name: fullName, role: 'student' },
      });
      if (createError || !created.user) return json({ error: 'ساخت حساب در Auth ناموفق بود' }, 502);
      const studentId = created.user.id;
      const { error: profileError } = await service.from('profiles').upsert({
        id: studentId,
        full_name: fullName,
        first_name: firstName || null,
        last_name: lastName || null,
        gender: gender || null,
        username,
        role: 'student',
        teacher_id: teacherId,
        is_active: true,
      });
      if (profileError) {
        await service.auth.admin.deleteUser(studentId);
        return json({ error: 'ساخت پروفایل دانش‌آموز ناموفق بود' }, 500);
      }
      if (classId) {
        const { error } = await service.from('class_members').insert({ class_id: classId, student_id: studentId });
        if (error) {
          await service.auth.admin.deleteUser(studentId);
          return json({ error: 'عضویت کلاس ثبت نشد و حساب بازگردانی شد' }, 500);
        }
      }
      await service.rpc('native_attach_created_student_v37', { p_actor: teacherId, p_student: studentId });
      await audit('create', studentId, { username, class_id: classId || null });
      return json({ ok: true, id: studentId, username });
    }

    if (action === 'update') {
      const id = clean(body.id, 64);
      const current = await ownedStudent(id);
      if (!current) return json({ error: 'دانش‌آموز یافت نشد یا دسترسی ندارید' }, 403);
      const firstName = clean(body.first_name, 100);
      const lastName = clean(body.last_name, 100);
      const fullName = joinName(firstName, lastName);
      const username = clean(body.username, 20).toLowerCase();
      const gender = clean(body.gender, 12);
      const password = String(body.password || '');
      if (!fullName) return json({ error: 'نام دانش‌آموز را وارد کنید' }, 400);
      if (!validUsername(username)) return json({ error: 'نام کاربری نامعتبر است' }, 400);
      if (password && (password.length < 8 || password.length > 72)) return json({ error: 'رمز باید بین ۸ تا ۷۲ کاراکتر باشد' }, 400);
      if (!validGender(gender)) return json({ error: 'جنسیت نامعتبر است' }, 400);
      if (username !== current.username) {
        const { data: duplicate } = await service.from('profiles').select('id').eq('username', username).neq('id', id).maybeSingle();
        if (duplicate) return json({ error: 'نام کاربری تکراری است' }, 409);
      }

      const before = {
        full_name: current.full_name,
        first_name: current.first_name,
        last_name: current.last_name,
        gender: current.gender,
        username: current.username,
      };
      const profilePatch = {
        full_name: fullName,
        first_name: firstName || null,
        last_name: lastName || null,
        gender: gender || null,
        username,
      };
      const { error: profileError } = await service.from('profiles').update(profilePatch).eq('id', id).eq('teacher_id', teacherId);
      if (profileError) return json({ error: 'ویرایش پروفایل ناموفق بود' }, 500);

      const authPatch: Record<string, unknown> = {
        email: userEmail(username),
        user_metadata: { full_name: fullName, role: 'student' },
      };
      if (password) authPatch.password = password;
      const { error: updateError } = await service.auth.admin.updateUserById(id, authPatch);
      if (updateError) {
        await service.from('profiles').update(before).eq('id', id).eq('teacher_id', teacherId);
        return json({ error: 'ویرایش حساب Auth ناموفق بود؛ پروفایل بازگردانی شد' }, 502);
      }
      await audit('update', id, { username_changed: username !== current.username, password_changed: !!password });
      return json({ ok: true });
    }

    if (action === 'reset_password') {
      const id = clean(body.id, 64);
      const password = String(body.password || '');
      if (!(await ownedStudent(id))) return json({ error: 'دانش‌آموز یافت نشد یا دسترسی ندارید' }, 403);
      if (password.length < 8 || password.length > 72) return json({ error: 'رمز باید بین ۸ تا ۷۲ کاراکتر باشد' }, 400);
      const { error } = await service.auth.admin.updateUserById(id, { password });
      if (error) return json({ error: 'تغییر رمز در Auth ناموفق بود' }, 502);
      await audit('reset_password', id);
      return json({ ok: true });
    }

    if (action === 'delete') {
      const id = clean(body.id, 64);
      if (!(await ownedStudent(id))) return json({ error: 'دانش‌آموز یافت نشد یا دسترسی ندارید' }, 403);
      const { error } = await service.auth.admin.deleteUser(id);
      if (error) return json({ error: 'حذف حساب ناموفق بود' }, 502);
      await audit('delete', id);
      return json({ ok: true });
    }

    if (action === 'bulk') {
      const rows = Array.isArray(body.rows) ? body.rows.slice(0, 100) as Record<string, unknown>[] : [];
      if (!rows.length) return json({ error: 'فهرست دانش‌آموزان خالی است' }, 400);
      const classId = clean(body.class_id, 64);
      if (!(await ensureOwnedClass(classId))) return json({ error: 'کلاس متعلق به این معلم نیست' }, 403);
      const results: Record<string, unknown>[] = [];
      for (const row of rows) {
        // Execute the same validated create path directly through admin APIs.
        const firstName = clean(row.first_name, 100);
        const lastName = clean(row.last_name, 100);
        const fullName = joinName(firstName, lastName);
        const username = clean(row.username, 20).toLowerCase();
        const password = String(row.password || '');
        const gender = clean(row.gender, 12);
        if (!fullName || !validUsername(username) || password.length < 8 || password.length > 72 || !validGender(gender)) {
          results.push({ username, ok: false, message: 'داده نامعتبر' });
          continue;
        }
        const { data: duplicate } = await service.from('profiles').select('id').eq('username', username).maybeSingle();
        if (duplicate) { results.push({ username, ok: false, message: 'تکراری' }); continue; }
        const { data: created, error } = await service.auth.admin.createUser({
          email: userEmail(username), password, email_confirm: true,
          user_metadata: { full_name: fullName, role: 'student' },
        });
        if (error || !created.user) { results.push({ username, ok: false, message: 'خطای Auth' }); continue; }
        const studentId = created.user.id;
        const { error: profileError } = await service.from('profiles').upsert({
          id: studentId, full_name: fullName, first_name: firstName || null, last_name: lastName || null,
          gender: gender || null, username, role: 'student', teacher_id: teacherId, is_active: true,
        });
        if (profileError) {
          await service.auth.admin.deleteUser(studentId);
          results.push({ username, ok: false, message: 'خطای پروفایل' });
          continue;
        }
        if (classId) {
          const { error: memberError } = await service.from('class_members').insert({ class_id: classId, student_id: studentId });
          if (memberError) {
            await service.auth.admin.deleteUser(studentId);
            results.push({ username, ok: false, message: 'خطای عضویت کلاس' });
            continue;
          }
        }
        await service.rpc('native_attach_created_student_v37', { p_actor: teacherId, p_student: studentId });
        results.push({ id: studentId, username, password, ok: true });
      }
      await audit('bulk_create', null, { count: rows.length, success: results.filter((x) => x.ok).length });
      return json({ ok: true, results });
    }

    return json({ error: 'عملیات ناشناخته است' }, 400);
  } catch {
    return json({ error: 'خطای داخلی مدیریت دانش‌آموز' }, 500);
  }
});
