import fs from 'node:fs';import vm from 'node:vm';import {spawnSync} from 'node:child_process';import {fileURLToPath} from 'node:url';
const base=fileURLToPath(new URL('../../site/src/',import.meta.url));const source=fs.readFileSync(base+'admin.js','utf8');
const ctx={window:{ExamSite:{en:x=>String(x).replace(/[۰-۹]/g,c=>'۰۱۲۳۴۵۶۷۸۹'.indexOf(c))}},console};vm.createContext(ctx);vm.runInContext(source,ctx);const A=ctx.window.SiteAdmin;const results=[];
function test(id,want,fn){try{let observed=fn();results.push({id,pass:typeof want==='function'?want(observed):JSON.stringify(want)===JSON.stringify(observed),observed})}catch(e){results.push({id,pass:false,error:e.message})}}
const q=(type='multiple',score=3,key={correctOption:0})=>({__type:type,score,__key:key});const valid=(raw,question=q(),neg=1/3)=>{try{return {ok:true,value:A.validateScore(raw,question,neg)}}catch(e){return {ok:false,error:e.message}}};
test('wrong MCQ receives penalty',v=>Math.abs(v+1)<1e-8,()=>A.autoScore(q(),'1',1/3));
test('correct MCQ full score',3,()=>A.autoScore(q(),'0',1/3));
test('blank MCQ no penalty',0,()=>A.autoScore(q(),'',1/3));
test('null MCQ no penalty',0,()=>A.autoScore(q(),null,1/3));
test('disabled penalty',0,()=>A.autoScore(q(),'1',0));
test('wrong truefalse penalty',-0.75,()=>A.autoScore(q('truefalse',3,{correctAnswer:true}),false,0.25));
test('missing truefalse key not auto graded',null,()=>A.autoScore(q('truefalse',3,{}),false,0.25));
test('essay not auto graded',null,()=>A.autoScore(q('essay',3,{}),'text',1/3));
test('wrong numeric no penalty',0,()=>A.autoScore(q('numeric',3,{answer:7}),'8',1/3));
test('numeric tolerance',3,()=>A.autoScore(q('numeric',3,{answer:7,tolerance:0.1}),'7.05',1/3));
test('partial matching',1.5,()=>A.autoScore(q('matching',3,{matchAnswer:{0:0,1:1}}),{0:0,1:0},1/3));
test('save negative boundary',v=>v.ok&&v.value===-1,()=>valid('-1'));
test('save Persian negative score',v=>v.ok&&v.value===-1,()=>valid('-۱'));
test('reject below floor',v=>!v.ok,()=>valid('-1.1'));
test('reject over maximum',v=>!v.ok,()=>valid('3.1'));
test('accept zero',v=>v.ok&&v.value===0,()=>valid('0'));
test('accept maximum',v=>v.ok&&v.value===3,()=>valid('3'));
test('reject null',v=>!v.ok,()=>valid(null));
test('reject empty',v=>!v.ok,()=>valid(''));
test('reject nonnumeric',v=>!v.ok,()=>valid('invalid'));
test('reject NaN',v=>!v.ok,()=>valid('NaN'));
test('reject Infinity',v=>!v.ok,()=>valid('Infinity'));
test('reject negative essay',v=>!v.ok,()=>valid('-1',q('essay',3,{})));
test('reject negative structured question',v=>!v.ok,()=>valid('-1',{...q(),assessmentParts:[{score:3}]}));
// Evaluate the exact save-map statement with the real exported validator.
const line=source.split('\n').find(l=>l.includes('var g = inputs.map'));
test('production save-map preserves negative',true,()=>{const c={inputs:[{value:'-1'}],questions:[q()],ctx:{exam:{neg_marking:1/3}},partGrades:{},validateScore:A.validateScore,num:Number,fa:String};vm.createContext(c);vm.runInContext(line,c);return c.g[0]===-1});
for(const file of fs.readdirSync(base).filter(x=>x.endsWith('.js'))){test('syntax '+file,true,()=>spawnSync('node',['--check',base+file],{encoding:'utf8'}).status===0)}
const summary={total:results.length,pass:results.filter(x=>x.pass).length,fail:results.filter(x=>!x.pass).length};if(process.env.GRADING_TEST_OUTPUT)fs.writeFileSync(process.env.GRADING_TEST_OUTPUT,JSON.stringify({results,summary},null,2));for(const r of results)console.log(r.pass?'PASS':'FAIL',r.id,JSON.stringify(r.observed??r.error));console.log(summary);if(summary.fail)process.exitCode=1;
