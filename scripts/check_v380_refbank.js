#!/usr/bin/env node
/* Real catalog metadata audited 2026-11; no question text or images included. */
'use strict';
const fs = require('fs'), assert = require('assert');
const src = fs.readFileSync('site/src/refbank.js', 'utf8');
const fixture = JSON.parse(fs.readFileSync('scripts/fixtures/v380_reference_catalog_metadata.json', 'utf8'));
function grab(start, end) { const a=src.indexOf(start), b=src.indexOf(end,a); assert(a>=0 && b>=0, start); return src.slice(a,b+end.length); }
const chunks = [
  src.slice(src.indexOf('var TRACK_ALIASES'), src.indexOf('var TYPES')),
  grab('function uniq(rows, key) {', '\n  }'),
  grab('function subjectOptions() {', '\n    }'),
  grab('function trackOptions() {', '\n    }'),
  'return {subjectOptions,trackOptions};'
];
let st = {grade: 0, track:'', rows:fixture.map(x=>({grade:x.grade,track:x.track,subject:x.subject}))};
const {subjectOptions, trackOptions} = new Function('st',chunks.join('\n'))(st);
for(let g=1;g<=10;g++) {
  st.grade=g;st.track='';
  const expected=new Set(fixture.filter(r=>r.grade===g).map(r=>r.subject));
  assert.deepStrictEqual(new Set(subjectOptions()), expected, 'grade '+g);
}
st.grade=10;
const expectedTracks=new Set(['عمومی','ریاضی فیزیک','علوم تجربی','علوم و معارف اسلامی']);
assert.deepStrictEqual(new Set(trackOptions()),expectedTracks);
for (const track of expectedTracks) {
 st.track=track;
 assert.deepStrictEqual(new Set(subjectOptions()),new Set(fixture.filter(x=>x.grade===10 && x.track===track).map(x=>x.subject)),track);
}
st.track='معارف';assert.deepStrictEqual(new Set(subjectOptions()),new Set(fixture.filter(x=>x.grade===10 && x.track==='علوم و معارف اسلامی').map(x=>x.subject)));
for (const g of [11,12]) {st.grade=g;st.track='';assert.deepStrictEqual(subjectOptions(),[]);assert.deepStrictEqual(trackOptions(),[])}
console.log('PASS: actual catalog labels: grades 1-10, all four grade-10 tracks, alias, empty grades 11-12');
