#!/usr/bin/env node
/* Regression tests for the actual site catalog code. Run: node scripts/check_v377_site.js */
'use strict';
const fs = require('fs'), assert = require('assert');
const source = fs.readFileSync('site/src/refbank.js', 'utf8');
const start = source.indexOf('  var CATALOG_MAX_PARTS = 32;');
const end = source.indexOf('  function questions(grade)', start);
assert(start >= 0 && end > start);
const files = {
  'catalog/g1.json': [
    {id: 1, grade: 1, subject: 'ریاضی'}, {id: 2, grade: 1, subject: 'فارسی'},
    {id: 3, grade: 1, subject: 'علوم تجربی'}
  ],
  'catalog/g10-s1.json': {track: 'ریاضی فیزیک', rows: [{id: 10, grade: 10, subject: 'ریاضی', track: ''}]},
  'catalog/g10-s2.json': {track: 'ریاضی فیزیک', rows: [{id: 11, grade: 10, subject: 'فیزیک', track: ''}]},
  'catalog/g10-s22.json': {track: 'علوم و معارف اسلامی', rows: [{id: 12, grade: 10, subject: 'احکام', track: ''}]}
};
let failPath = '';
function getJSON(path) {
  if (path === failPath) return Promise.reject(new Error('HTTP 500'));
  if (!(path in files)) return Promise.reject(new Error('HTTP 404'));
  return Promise.resolve(JSON.parse(JSON.stringify(files[path])));
}
const build = new Function('getJSON', 'tokenizeRows', 'cache', source.slice(start, end) + '\nreturn {catalog, catalogSharded};');
const imageStart = source.indexOf('function rowImageCount(r) {');
const imageEnd = source.indexOf('\n    }', imageStart);
assert(imageStart >= 0 && imageEnd > imageStart);
const imageCount = new Function(source.slice(imageStart, imageEnd + 6) + '\nreturn rowImageCount;')();
assert.strictEqual(imageCount({ image_urls: ['a.webp'], has_image: false, image_count: 0 }), 1);
assert.strictEqual(imageCount({ image_urls: ['o.webp'], option_images: ['o.webp'] }), 0);
(async () => {
  let api = build(getJSON, x => x, {catalog:{}});
  assert.deepStrictEqual((await api.catalog(1, '')).map(x=>x.subject), ['ریاضی','فارسی','علوم تجربی']);
  let rows = await api.catalog(10, '');
  assert.strictEqual(rows.length, 3);
  assert.deepStrictEqual(rows.filter(x=>x.track==='ریاضی فیزیک').map(x=>x.subject), ['ریاضی','فیزیک']);
  assert.strictEqual(rows.filter(x=>x.track==='علوم و معارف اسلامی').length, 1);
  failPath='catalog/g10-s2.json'; api=build(getJSON, x=>x, {catalog:{}});
  await assert.rejects(api.catalog(10,''), /HTTP 500/);
  failPath=''; assert.strictEqual((await api.catalog(10,'')).length,3);
  console.log('PASS: full grade, all shards, tracks, non-404 errors, retry');
})().catch(e=>{console.error(e);process.exitCode=1});
