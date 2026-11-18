#!/usr/bin/env node
/* V377 — آزمون واقعی کد سایت روی دادهٔ شبیه سرور (بدون مرورگر).
   اجرا:  node scripts/check_v377_site.js
   سه چیزی را می‌سنجد که روی سایت زنده ایراد داشت:
     ۱) تکه‌های فهرست ({"grade","track","subject","rows"}) خوانده می‌شوند؛
     ۲) پیش‌فرض «همهٔ رشته‌ها» بزرگ‌ترین تکه را می‌آورد، نه اولین تکه؛
     ۳) «با تصویر» فقط سؤال‌هایی با تصویر صورت سؤال. */
'use strict';
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const src = fs.readFileSync(path.join(root, 'site/src/refbank.js'), 'utf8');

function grab(start, end) {
  const i = src.indexOf(start);
  const j = src.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error('قطعه پیدا نشد: ' + start);
  return src.slice(i, j + end.length);
}
function grabBlock(start) { // تا بستن تابع در ستون ۲
  const i = src.indexOf(start);
  const j = src.indexOf('\n  }', i);
  if (i < 0 || j < 0) throw new Error('تابع پیدا نشد: ' + start);
  return src.slice(i, j + 4);
}

// ---- دادهٔ شبیه سرور (همان ساختار واقعیِ catalog/g10-sN.json) ----
const rowsMath = [];
for (let i = 0; i < 40; i++) {
  rowsMath.push({
    id: 2640738 + i, grade: 10, track: 'ریاضی فیزیک', subject: 'ریاضی (1)', chapter: 'فصل ٣',
    topic: '', area: '', type_fa: 'تستی', type: 'multiple', difficulty: 2, difficulty_fa: 'متوسط',
    score: 1, text: '', has_image: false, image_count: 0,
    image_urls: i % 4 === 0 ? ['https://x/m' + i + '.webp'] : []
  });
}
const DATA = {
  'g10-s1': { grade: 10, track: 'ریاضی فیزیک', subject: 'ریاضی (1)', rows: rowsMath },
  'g10-s9': { grade: 10, track: 'عمومی', subject: 'نگارش (١)', rows: [
    { id: 2033283, grade: 10, track: 'عمومی', subject: 'نگارش (١)', chapter: 'درس اول',
      topic: '', area: '', type_fa: 'تستی', type: 'multiple', difficulty: 1, difficulty_fa: 'ساده',
      score: 1, text: '', has_image: false, image_count: 0, image_urls: [] }
  ] },
  'g10-s10': { grade: 10, track: 'علوم و معارف اسلامی', subject: 'احکام (١) دختران', rows: [
    { id: 1500100001, grade: 10, track: 'علوم و معارف اسلامی', subject: 'احکام (١) دختران',
      chapter: 'درس چهاردهم', topic: '', area: '', type_fa: 'تستی', type: 'multiple',
      difficulty: 1, difficulty_fa: 'ساده', score: 1, text: '', has_image: false,
      image_count: 0, image_urls: [] }
  ] },
  'g10-s22': { grade: 10, track: 'علوم و معارف اسلامی', subject: 'نگارش (١)', rows: [
    { id: 1501000011, grade: 10, track: 'علوم و معارف اسلامی', subject: 'نگارش (١)',
      chapter: 'درس دوم', topic: '', area: '', type_fa: 'تستی', type: 'multiple',
      difficulty: 1, difficulty_fa: 'ساده', score: 1, text: '', has_image: true, image_count: 1,
      image_urls: ['https://x/q.webp'] }
  ] }
};

global.getJSON = function (p) {
  const key = p.replace('catalog/', '').replace('.json', '');
  return new Promise((res, rej) => setTimeout(() => {
    if (Object.prototype.hasOwnProperty.call(DATA, key)) res(JSON.parse(JSON.stringify(DATA[key])));
    else rej(new Error('404 ' + p));
  }, 0));
};

const code = [
  src.slice(src.indexOf('var TRACK_ALIASES'), src.indexOf('function sameTrack')),
  src.slice(src.indexOf('var CATALOG_MAX_PARTS'), src.indexOf('function shardUrls')),
  grabBlock('function shardUrls(k) {'),
  grabBlock('function shardMatches'),
  grabBlock('function readShardHead'),
  grabBlock('function catalogRows'),
  grabBlock('function readCatalogShard'),
  grabBlock('function catalogSharded'),
  grabBlock('function biggestShard'),
  grabBlock('function catalog(grade, track) {'),
  grab('function rowImageCount(r) {', '\n    }'),
  grab('function imgUrls', '\n  }'),
  'function tokenizeRows(r){return Array.isArray(r)?r:r;}',
  'var cache={catalog:{}}; var pagedIdx={};',
  'return {catalog:catalog, catalogSharded:catalogSharded, rowImageCount:rowImageCount, CATALOG_MAX_PARTS:CATALOG_MAX_PARTS};'
].join('\n');
const M = new Function(code)();

let pass = 0, fail = 0;
const t = (name, ok, extra) => { ok ? (pass++, console.log('✅ ' + name)) : (fail++, console.log('❌ ' + name + (extra === undefined ? '' : ' → ' + extra))); };

t('سقف تکه‌ها ۳۲ است', M.CATALOG_MAX_PARTS === 32, M.CATALOG_MAX_PARTS);
t('سطر بی‌متن با image_urls پر، تصویردار است',
  M.rowImageCount({ image_urls: ['a.webp'], has_image: false, image_count: 0 }) === 1);
t('تصویر گزینه، تصویر سؤال نیست',
  M.rowImageCount({ image_urls: ['o.webp'], option_images: ['o.webp'] }) === 0);

(async () => {
  let rows = await M.catalog(10, '');
  t('بدون رشته: بزرگ‌ترین تکه (۴۰ سطر ریاضی)', rows.length === 40, rows.length);
  t('بدون رشته: رشتهٔ تکه هم ریاضی است', rows[0] && rows[0].track === 'ریاضی فیزیک');

  rows = await M.catalog(10, 'معارف');
  t('رشتهٔ «معارف» ⇒ ۲ سطر معارف', rows.length === 2, rows.length);
  t('همه از رشتهٔ علوم و معارف اسلامی',
    rows.every(r => r.track === 'علوم و معارف اسلامی'));

  rows = await M.catalog(10, 'علوم و معارف اسلامی');
  t('نام کامل رشته هم همان است', rows.length === 2, rows.length);

  rows = await M.catalog(10, 'ریاضی فیزیک');
  t('رشتهٔ ریاضی ⇒ ۴۰ سطر', rows.length === 40, rows.length);

  console.log(fail ? '\n' + fail + ' شکست' : '\nهمهٔ ' + pass + ' سنجه پاس شد');
  process.exit(fail ? 1 : 0);
})().catch(e => { console.log('❌ خطا:', e.message); process.exit(1); });
