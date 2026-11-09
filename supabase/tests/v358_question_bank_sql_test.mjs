// V358 — درختوارهٔ بانک سؤال روی PostgreSQL واقعی
import { PGlite } from '@electric-sql/pglite';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT = (() => { let d=path.dirname(fileURLToPath(import.meta.url));
  while(d!==path.dirname(d) && !fs.existsSync(path.join(d,'settings.gradle.kts'))) d=path.dirname(d); return d; })();
const S = n => fs.readFileSync(path.join(ROOT,'supabase','sql',n),'utf8');
process.on('uncaughtException',e=>{console.log('خطای مرگبار: '+e.message);process.exit(1);});
process.on('unhandledRejection',e=>{console.log('خطای مرگبار: '+(e&&e.message||e));process.exit(1);});
const db = new PGlite();
let pass=0, fail=0;
const ok=(n,c,x)=>{if(c){pass++;console.log('  ✓ '+n);}else{fail++;console.log('  ✗ '+n+(x?' — '+x:''));}};
const q=async(s,p)=>(await db.query(s,p)).rows;
const setU=async u=>{await q(`select set_config('test.uid',$1,false)`,[u||'']);};
async function mustFail(n,fn){ try{ await fn(); ok(n+' (باید رد می‌شد)',false);}catch(e){ ok(n,true);} }

await db.exec(`create schema if not exists auth;
create or replace function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('test.uid',true),'')::uuid $$;
do $$ begin if not exists (select 1 from pg_roles where rolname='authenticated') then create role authenticated; end if; end $$;
grant usage on schema public to authenticated;
create table public.bank_categories(id bigserial primary key, teacher_id uuid not null, name text not null, created_at timestamptz not null default now());
create unique index uq_bank_categories_teacher_name on public.bank_categories(teacher_id, lower(name));
create table public.question_bank(id bigserial primary key, teacher_id uuid not null, subject text, question jsonb not null default '{}'::jsonb, created_at timestamptz not null default now());
create table public.bank_question_cats(question_id bigint not null references public.question_bank(id) on delete cascade, category_id bigint not null references public.bank_categories(id) on delete cascade, primary key(question_id,category_id));
alter table public.bank_categories enable row level security;
create policy v13_bank_categories_read on public.bank_categories for select to authenticated using (teacher_id = auth.uid());
grant select on public.bank_categories to authenticated;`);

const T1=crypto.randomUUID(), T2=crypto.randomUUID();
console.log('\n=== ۱) نصب ===');
await db.exec(S('SQL_QUESTION_BANK_V358.sql'));
ok('نصب اجرا شد', true);
await db.exec(S('SQL_QUESTION_BANK_V358.sql'));
ok('اجرای دوباره بی‌خطر است', true);
ok('تریگر تکراری نشد', (await q("select count(*)::int n from pg_trigger where tgname='native_bank_tree_guard_v358' and not tgisinternal"))[0].n===1);

console.log('\n=== ۲) درخت سه‌سطحی ===');
await setU(T1);
const root=(await q("select public.native_bank_category_add_v358('ریاضی',null) r"))[0].r;
const ch=(await q("select public.native_bank_category_add_v358('فصل ۱',$1) r",[root.id]))[0].r;
const tp=(await q("select public.native_bank_category_add_v358('معادله',$1) r",[ch.id]))[0].r;
ok('ریشه ساخته شد', root.ok===true);
ok('فصل زیر درس ساخته شد', ch.parent_id===root.id);
ok('مبحث زیر فصل ساخته شد', tp.parent_id===ch.id);

console.log('\n=== ۳) مرزها ===');
await mustFail('سطح چهارم رد شد', ()=>q("select public.native_bank_category_add_v358('زیادی',$1)",[tp.id]));
await mustFail('والدِ خود رد شد', ()=>q("select public.native_bank_category_move_v358($1,$1)",[root.id]));
await mustFail('حلقه رد شد', ()=>q("select public.native_bank_category_move_v358($1,$2)",[root.id,tp.id]));
await setU(T2);
await mustFail('والدِ معلم دیگر رد شد', ()=>q("select public.native_bank_category_add_v358('دزدی',$1)",[root.id]));
await mustFail('جابه‌جایی دستهٔ دیگری رد شد', ()=>q("select public.native_bank_category_move_v358($1,null)",[root.id]));
await setU(T1);
await mustFail('نام تکراری در همان سطح رد شد', ()=>q("select public.native_bank_category_add_v358('ریاضی',null)"));
const root2=(await q("select public.native_bank_category_add_v358('فیزیک',null) r"))[0].r;
const ch2=(await q("select public.native_bank_category_add_v358('فصل ۱',$1) r",[root2.id]))[0].r;
ok('«فصل ۱» زیر دو درس متفاوت مجاز است', ch2.ok===true);

console.log('\n=== ۴) جابه‌جایی ===');
// مقصد هم‌نام دارد؛ باید پیام فارسی بدهد نه خطای خام ایندکس
let collide='';
try { await q("select public.native_bank_category_move_v358($1,$2)",[ch.id,root2.id]); }
catch(e){ collide=String(e.message); }
ok('برخورد نام پیام فارسی می‌دهد', collide.includes('در مقصد، دسته‌ای با همین نام هست'), collide.slice(0,80));
const root3=(await q("select public.native_bank_category_add_v358('شیمی',null) r"))[0].r;
const moved=(await q("select public.native_bank_category_move_v358($1,$2) r",[ch.id,root3.id]))[0].r;
ok('جابه‌جایی به مقصد بی‌برخورد انجام شد', moved.ok===true);
await mustFail('جابه‌جایی با سرریز زیردرخت رد شد', ()=>q("select public.native_bank_category_move_v358($1,$2)",[ch.id,tp.id]));
await q("select public.native_bank_category_move_v358($1,$2)",[ch.id,root.id]);
ok('بازگشت به والد اصلی انجام شد', true);

console.log('\n=== ۵) سؤال‌ها هرگز حذف نمی‌شوند ===');
const qn=(await q("insert into public.question_bank(teacher_id,subject,question) values ($1,'ریاضی','{\"text\":\"سؤال\",\"difficulty\":2,\"topic\":\"معادله\"}'::jsonb) returning id",[T1]))[0].id;
await q("insert into public.bank_question_cats values ($1,$2)",[qn,tp.id]);
const before=(await q("select count(*)::int n from public.question_bank"))[0].n;
await q("delete from public.bank_categories where id=$1",[ch.id]);
ok('زیردسته با والد حذف شد', (await q("select count(*)::int n from public.bank_categories where id=$1",[tp.id]))[0].n===0);
ok('سؤال حذف نشد', (await q("select count(*)::int n from public.question_bank"))[0].n===before);
ok('فقط پیوند دسته رفت', (await q("select count(*)::int n from public.bank_question_cats where question_id=$1",[qn]))[0].n===0);

console.log('\n=== ۶) درخت و شمارش زیردرخت ===');
const c1=(await q("select public.native_bank_category_add_v358('فصل ۲',$1) r",[root.id]))[0].r;
const t1=(await q("select public.native_bank_category_add_v358('هندسه',$1) r",[c1.id]))[0].r;
await q("insert into public.bank_question_cats values ($1,$2),($1,$3)",[qn,c1.id,t1.id]);
const tree=(await q("select public.native_bank_tree_v358() t"))[0].t;
const node=tree.find(x=>x.id===c1.id);
ok('درخت برگردانده شد', Array.isArray(tree) && tree.length>0);
ok('مسیر درست ساخته شد', node && node.path.includes('ریاضی') && node.path.includes('فصل ۲'), node&&node.path);
ok('ژرفا درست است', node && node.depth===2);
ok('شمارش زیردرخت خودش و فرزند را می‌شمارد', node && node.subtree_count===1, JSON.stringify(node));
ok('شمارش خود گره جداست', node && node.count===1);

console.log('\n=== ۷) snapshot غنی شد و قدیمی نشکست ===');
const snap=(await q("select public.native_bank_snapshot_v1() s"))[0].s;
ok('snapshot دستهٔ والد دارد', snap.categories.every(c=>'parent_id' in c));
const it=snap.items.find(x=>x.id===qn);
ok('snapshot دشواری دارد', it && it.difficulty===2, JSON.stringify(it));
ok('snapshot مبحث دارد', it && it.topic==='معادله');
const v1=(await q("select public.native_bank_category_add_v1('زیست') r"))[0].r;
ok('تابع قدیمی هنوز کار می‌کند', v1.ok===true);
const v1dup=(await q("select public.native_bank_category_add_v1('زیست') r"))[0].r;
ok('تابع قدیمی تکراری ریشه را رد می‌کند', !!v1dup.error);

console.log('\n=== ۸) RLS ===');
// RLS برای مالک جدول اعمال نمی‌شود؛ باید نقش را عوض کرد
await setU(T2);
await q('set role authenticated');
const otherCount=(await q("select count(*)::int n from public.bank_categories"))[0].n;
await q('reset role');
ok('معلم دیگر دسته‌های او را نمی‌بیند', otherCount===0, String(otherCount));
await setU(T1);
await q('set role authenticated');
const ownCount=(await q("select count(*)::int n from public.bank_categories"))[0].n;
await q('reset role');
ok('خودش دسته‌هایش را می‌بیند', ownCount>0, String(ownCount));

console.log('\n=== ۹) فایل بررسی ===');
const chk=S('SQL_QUESTION_BANK_V358_CHECK.sql');
const body=chk.slice(chk.indexOf('with checks'), chk.indexOf('-- خلاصهٔ شمارش'));
const rows=await q(body);
const bad=rows.filter(r=>r['درست']!==true);
ok('همهٔ بررسی‌ها true شدند', bad.length===0, JSON.stringify(bad));
console.log('    بررسی نصب: '+rows.filter(r=>r['درست']===true).length+' مورد true');
await db.close();
console.log(`\n${fail===0?'SQL358: PASS':'SQL358: FAIL'} (${pass} سنجه، ${fail} خطا)`);
process.exit(fail===0?0:1);
