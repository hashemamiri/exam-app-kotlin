import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../../',import.meta.url));
const source=fs.readFileSync(root+'site/src/extras.js','utf8');
const ctx={window:{ExamSite:{}},console,Map};vm.createContext(ctx);vm.runInContext(source,ctx);
const {independentRoster:R,answerStudentKey:K}=ctx.window.SiteExtras;
const tests=[];function test(name,fn){fn();tests.push({name,pass:true});console.log('PASS',name)}
const e=[{id:'one'}], a={one:[{id:'a',student_id:'s1',student_name:'مینا',graded:true},{id:'b',student_id:'s1',student_name:'مینا',graded:true},{id:'c',student_id:'s2',graded:false},{id:'g1',student_id:null,student_name:'مهمان اول',graded:true},{id:'g2',student_id:null,student_name:'مهمان دوم',graded:true}],other:[{id:'x',student_id:'other',graded:true}]};
test('no exams has no roster',()=>assert.equal(R([],a).length,0));
test('no answers has no roster',()=>assert.equal(R(e,{}).length,0));
test('only selected exam respondents',()=>assert.equal(R(e,a).length,3));
test('repeated attempts deduplicate account',()=>assert.equal(R(e,a).filter(x=>x.id==='s1').length,1));
test('pending response does not become zero-grade report',()=>assert(!R(e,a).some(x=>x.id==='s2')));
test('other exam does not leak into selected report',()=>assert(!R(e,a).some(x=>x.id==='other')));
test('null student IDs remain separate',()=>assert.notEqual(K(a.one[3]),K(a.one[4])));
test('name comes from own answer',()=>assert.equal(R(e,a)[0].full_name,'مینا'));
test('blank name has readable fallback',()=>assert.equal(R(e,{one:[{id:'a',graded:true}]} )[0].full_name,'دانش‌آموز'));
// Verify actual UI + fetch/compute behavior with a DOM only when an explicit test dependency is supplied.
if(process.env.REPORT_JSDOM_MODULE){
 const {JSDOM}=await import(process.env.REPORT_JSDOM_MODULE);
 async function page({classes=[],schools=[],answers=a.one,delay=false}={}){
  const dom=new JSDOM('<body><main></main></body>',{runScripts:'outside-only'});const w=dom.window;let release;const calls=[];const prints=[];
  function el(tag,attrs={},children=[]){const n=w.document.createElement(tag);for(const [k,v] of Object.entries(attrs)){if(k==='text')n.textContent=v;else if(k==='html')n.innerHTML=v;else if(k.startsWith('on'))n.addEventListener(k.slice(2),v);else n.setAttribute(k,v)}for(const c of children||[])if(c)n.appendChild(c);return n}
  w.ExamSite={el,fa:String,esc:s=>String(s),en:String,num:Number,toast:()=>{},errMsg:e=>e.message,user:()=>({id:'teacher'}),loading:n=>{n.textContent='loading'},emptyBox:(i,t)=>el('p',{text:t}),showErr:(n,e)=>{throw e},fmtScore:String,printSection:(t,b)=>prints.push([t,b.textContent]),
   rpc:async(name)=>{calls.push(name);if(name==='class_roster')return[{id:'s1',full_name:'مینا'}];return classes},rpcObj:async()=>({items:schools}),
   select:async(table,query)=>{calls.push([table,query]);if(table==='exams')return[{id:'one',title:'آزمون من',total_score:10}];if(delay)await new Promise(r=>release=r);return answers.map(x=>({...x,exam_id:'one',total_grade:6,meta:{max_score:8}}))}};
  w.eval(source);await w.SiteExtras.reportsPage(w.document.querySelector('main'),{section:'grades'});await new Promise(r=>setTimeout(r,0));return{dom,w,calls,prints,release:()=>release?.()};
 }
 async function scenario(name,fn){await fn();tests.push({name,pass:true});console.log('PASS',name)}
 await scenario('no school or class: exam picker and report button available',async()=>{const p=await page();assert(p.w.document.querySelector('.rp-show'));assert.equal(p.w.document.querySelector('.rp-show').disabled,false);assert(p.w.document.body.textContent.includes('آزمون‌ها'));p.dom.window.close()});
 await scenario('no class: actual table, per-answer max and print modal',async()=>{const p=await page();p.w.document.querySelector('.rp-show').click();assert.equal(p.w.document.querySelectorAll('.rp-grades tbody tr').length,3);assert(p.w.document.querySelector('.rp-grades').textContent.includes('75.0'));p.w.document.querySelector('.rp-ic').click();assert(p.w.document.querySelector('.rp-card-modal'));const print=[...p.w.document.querySelectorAll('.rp-card-modal button')].find(n=>n.textContent.includes('PDF'));print.click();assert.equal(p.prints.length,1);p.dom.window.close()});
 await scenario('pending-only exam: empty state without manufactured grades',async()=>{const p=await page({answers:[{id:'p',student_id:'s',graded:false}]});p.w.document.querySelector('.rp-show').click();assert.equal(p.w.document.querySelectorAll('.rp-grades tbody tr').length,0);assert(p.w.document.body.textContent.includes('پاسخ تصحیح‌شده‌ای'));p.dom.window.close()});
 await scenario('slow answers: show waits then actually refreshes independent report',async()=>{const p=await page({delay:true});p.w.document.querySelector('.rp-show').click();assert.equal(p.w.document.querySelectorAll('.rp-grades tbody tr').length,0);p.release();await new Promise(r=>setTimeout(r,0));assert.equal(p.w.document.querySelectorAll('.rp-grades tbody tr').length,3);p.dom.window.close()});
 await scenario('existing class selection still limits roster',async()=>{const p=await page({classes:[{id:'class1',name:'کلاس من'}]});[...p.w.document.querySelectorAll('.rp-row')].find(n=>n.textContent.includes('. کلاس')).click();const radio=p.w.document.querySelectorAll('.rp-pick-row input')[1];radio.checked=true;radio.dispatchEvent(new p.w.Event('change'));[...p.w.document.querySelectorAll('.rp-pick-modal button')].find(n=>n.textContent==='تأیید').click();await new Promise(r=>setTimeout(r,0));p.w.document.querySelector('.rp-show').click();assert.equal(p.w.document.querySelectorAll('.rp-grades tbody tr').length,1);assert(p.calls.includes('class_roster'));p.dom.window.close()});
}
// Limited Android source wiring checks, NOT a Compose/Gradle runtime test.
const model=fs.readFileSync(root+'app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt','utf8');
const screen=fs.readFileSync(root+'app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt','utf8');
test('Android no mandatory class early-return',()=>assert(!model.includes('if (snapshot.selectedClass == null) return@launch')));
test('Android roster uses only fetched selected answer map',()=>assert(model.includes('answerMap.values.flatMap { it.entries }')));
test('Android guest grouping uses answer identity',()=>assert(model.includes('.groupBy { it.studentId ?: "answer:${it.id}" }')));
test('Android independent report button allowed',()=>assert(screen.includes('(state.selectedClass == null || state.selectedStudentIds.isNotEmpty())')));
const result={scope:'pure helper + optional jsdom actual web report flow; Android checks are SOURCE WIRING ONLY, not runtime/Compose',passed:tests.length,tests};if(process.env.REPORT_TEST_OUTPUT)fs.writeFileSync(process.env.REPORT_TEST_OUTPUT,JSON.stringify(result,null,2));console.log('ALL',tests.length,'PASS');
