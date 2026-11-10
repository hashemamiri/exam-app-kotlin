// V358 — تولید بردارهای برابری بانک سؤال از پیمانهٔ سایت.
// اجرا: node scripts/gen_bankpick_vectors.mjs
// خروجی: app/src/test/resources/v358_bankpick_vectors.json
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ROOT = (() => {
  let d = path.dirname(fileURLToPath(import.meta.url));
  while (d !== path.dirname(d) && !fs.existsSync(path.join(d, 'settings.gradle.kts'))) d = path.dirname(d);
  return d;
})();
const B = require(path.join(ROOT, 'site', 'src', 'bankpick.js'));

const fa = (n) => String(n).split('').map((d) => (d >= '0' && d <= '9' ? String.fromCharCode(0x6f0 + Number(d)) : d)).join('');
const ar = (n) => String(n).split('').map((d) => (d >= '0' && d <= '9' ? String.fromCharCode(0x660 + Number(d)) : d)).join('');

const CATS = [
  { id: 1, name: 'ریاضی', parent_id: null },
  { id: 2, name: 'فصل ' + fa(1), parent_id: 1 },
  { id: 3, name: 'معادله', parent_id: 2 },
  { id: 4, name: 'هندسه', parent_id: 2 },
  { id: 5, name: 'فصل ' + fa(2), parent_id: 1 },
  { id: 6, name: 'فیزیک', parent_id: null },
  { id: 7, name: 'فصل ' + fa(1), parent_id: 6 },
  { id: 8, name: 'نور', parent_id: 7 }
];

function makeItems(n, offset) {
  const out = [];
  for (let i = 1; i <= n; i++) {
    out.push({
      id: i + (offset || 0),
      subject: i % 2 ? 'ریاضی' : 'فیزیک',
      question: {
        text: 'پرسش ' + fa(i) + ' دربارهٔ ' + (i % 3 ? 'معادله' : 'نور'),
        difficulty: i % 5,
        topic: i % 3 ? 'معادله' : 'نور',
        grade: (i % 6) + 1
      },
      cat_ids: [CATS[i % CATS.length].id]
    });
  }
  return out;
}

const V = { latinDigits: [], subtree: [], path: [], depth: [], filter: [], diffWord: [], parse: [], format: [], shuffle: [], pick: [] };

// --- latinDigits ---
const digitInputs = ['', '123', fa(123), ar(456), 'a' + fa(7) + 'b' + ar(8), '۰۱۳۴۵۶۷۸۹', '٠١٢٣٤٥٦٧٨٩', '  ' + fa(42) + '  ', 'x/y' + fa(9)];
digitInputs.forEach((s) => V.latinDigits.push({ input: s, output: B.latinDigits(s) }));

// --- subtree / path / depth ---
[null, '', 1, 2, 3, 6, 7, 99].forEach((r) => {
  V.subtree.push({ categories: CATS, root: r == null ? null : String(r), ids: B.subtreeIds(CATS, r == null ? null : String(r)) });
});
CATS.forEach((c) => {
  V.path.push({ categories: CATS, id: String(c.id), path: B.categoryPath(CATS, String(c.id)) });
  V.depth.push({ categories: CATS, id: String(c.id), depth: B.depthOf(CATS, String(c.id)) });
});
V.path.push({ categories: CATS, id: '999', path: B.categoryPath(CATS, '999') });
V.depth.push({ categories: CATS, id: '999', depth: B.depthOf(CATS, '999') });
// حلقهٔ مصنوعی — نباید بی‌پایان شود
const LOOP = [{ id: 1, name: 'الف', parent_id: 2 }, { id: 2, name: 'ب', parent_id: 1 }];
V.subtree.push({ categories: LOOP, root: '1', ids: B.subtreeIds(LOOP, '1') });
V.path.push({ categories: LOOP, id: '1', path: B.categoryPath(LOOP, '1') });
V.depth.push({ categories: LOOP, id: '1', depth: B.depthOf(LOOP, '1') });

// --- filter ---
const ITEMS = makeItems(24, 0);
const optsList = [
  {}, { text: 'معادله' }, { text: fa(5) }, { text: 'نور' },
  { categoryId: '1' }, { categoryId: '2' }, { categoryId: '6' }, { categoryId: '999' },
  { difficulty: 0 }, { difficulty: 1 }, { difficulty: 2 }, { difficulty: 3 }, { difficulty: 4 }, { difficulty: 4 },
  { subject: 'ریاضی' }, { subject: 'فیزیک' }, { grade: 1 }, { grade: 6 },
  { categoryId: '1', difficulty: 2 }, { categoryId: '2', difficulty: 1, subject: 'ریاضی' },
  { text: 'پرسش', categoryId: '1', difficulty: 3 }
];
optsList.forEach((o) => {
  V.filter.push({ items: ITEMS, categories: CATS, opts: o, ids: B.filterItems(ITEMS, CATS, o).map((x) => String(x.id)) });
});

// --- difficultyFromWord ---
['', 'هر', 'همه', 'any', '*', '-', '0', '1', '2', '3', 'ساده', 'آسان', 'easy', 'متوسط', 'medium',
  'دشوار', 'سخت', 'hard', 'تعیین‌نشده', 'نامشخص', 'unset', 'المپیادی', 'xyz', fa(2), ar(3), '  آسان  ',
  '4', 'خیلی دشوار', 'بسیار دشوار', 'veryhard'
].forEach((w) => V.diffWord.push({ word: w, value: B.difficultyFromWord(w) }));

// --- parseBlueprint ---
const texts = [
  '', '# فقط توضیح', '\n\n',
  'همه | هر | 5',
  'همه | هر | ' + fa(5),
  'فصل ' + fa(1) + ' | آسان | ' + fa(3),
  'فصل 1 | آسان | 3',
  'ریاضی | متوسط | ' + ar(4),
  'ریاضی / فصل ' + fa(1) + ' / معادله | دشوار | 2',
  'همه | هر | 5\nهمه | هر | 5',
  'همه | خیلی دشوار | 2',
  'نور | ساده | 2\nمعادله | دشوار | 3',
  'بدون جداکننده',
  'همه | هر',
  'همه | هر | 5 | زیاد',
  'ناموجود | هر | 5',
  'همه | نامعتبر | 5',
  'همه | هر | 0',
  'همه | هر | 201',
  'همه | هر | -3',
  'همه | هر | 2.5',
  'همه | هر | abc',
  'همه | هر | 0x10',
  'همه | هر | ' + fa(201),
  'فصل ' + fa(1) + ' | هر | 2',
  '  همه  |  هر  |  7  ',
  '# توضیح\nهمه | هر | 1'
];
texts.forEach((t) => {
  const p = B.parseBlueprint(t, CATS);
  V.parse.push({ text: t, categories: CATS, rules: p.rules, errors: p.errors, total: p.total });
});
// سقف قاعده‌ها
let many = '';
for (let i = 0; i < 45; i++) many += 'همه | هر | 1\n';
{
  const p = B.parseBlueprint(many, CATS);
  V.parse.push({ text: many, categories: CATS, rules: p.rules, errors: p.errors, total: p.total });
}
// ابهام نام (دو «فصل ۱»)
{
  const p = B.parseBlueprint('فصل ' + fa(1) + ' | هر | 2', CATS);
  V.parse.push({ text: 'فصل ' + fa(1) + ' | هر | 2', categories: CATS, rules: p.rules, errors: p.errors, total: p.total });
}

// --- formatBlueprint ---
[
  [], [{ category: 'همه', categoryId: null, difficulty: -1, count: 5 }],
  [{ category: 'ریاضی', categoryId: '1', difficulty: 2, count: 3 }],
  [
    { category: 'فصل ' + fa(1), categoryId: '2', difficulty: 1, count: 4 },
    { category: 'همه', categoryId: null, difficulty: 0, count: 2 },
    { category: 'نور', categoryId: '8', difficulty: 3, count: 1 }
  ]
].forEach((r) => V.format.push({ rules: r, text: B.formatBlueprint(r) }));

// --- shuffled / rng ---
[0, 1, 42, -7, 123456, 2147483647].forEach((s) => {
  V.shuffle.push({ arr: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], seed: s, out: B.shuffled([1, 2, 3, 4, 5, 6, 7, 8, 9, 10], s) });
  V.shuffle.push({ arr: [], seed: s, out: B.shuffled([], s) });
  V.shuffle.push({ arr: [7], seed: s, out: B.shuffled([7], s) });
});

// --- pick ---
const ruleSets = [
  [],
  [{ category: 'همه', categoryId: null, difficulty: -1, count: 5 }],
  [{ category: 'همه', categoryId: null, difficulty: 1, count: 4 }],
  [{ category: 'ریاضی', categoryId: '1', difficulty: -1, count: 6 }],
  [{ category: 'فیزیک', categoryId: '6', difficulty: 2, count: 3 }],
  [
    { category: 'همه', categoryId: null, difficulty: 1, count: 3 },
    { category: 'همه', categoryId: null, difficulty: 1, count: 3 }
  ],
  [
    { category: 'ریاضی', categoryId: '1', difficulty: -1, count: 4 },
    { category: 'فیزیک', categoryId: '6', difficulty: -1, count: 4 }
  ],
  [{ category: 'همه', categoryId: null, difficulty: -1, count: 100 }],
  [{ category: 'نور', categoryId: '8', difficulty: 3, count: 5 }]
];
ruleSets.forEach((rs) => {
  [0, 1, 42, 999].forEach((seed) => {
    const r = B.pick(ITEMS, CATS, rs, seed);
    V.pick.push({
      items: ITEMS, categories: CATS, rules: rs, seed,
      ids: r.picked.map((x) => String(x.id)), perRule: r.perRule, total: r.total, summary: B.summary(r)
    });
  });
});
// بانک خالی
{
  const rs = [{ category: 'همه', categoryId: null, difficulty: -1, count: 5 }];
  const r = B.pick([], CATS, rs, 42);
  V.pick.push({ items: [], categories: CATS, rules: rs, seed: 42, ids: [], perRule: r.perRule, total: r.total, summary: B.summary(r) });
}

const counts = Object.fromEntries(Object.entries(V).map(([k, v]) => [k, v.length]));
const total = Object.values(counts).reduce((a, b) => a + b, 0);
const out = path.join(ROOT, 'app', 'src', 'test', 'resources', 'v358_bankpick_vectors.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(V, null, 1), 'utf8');
console.log('بردارها:', JSON.stringify(counts));
console.log('مجموع:', total, '→', path.relative(ROOT, out));
