// Persona walkthrough harness: each simulated person sees only what is on the phone screen,
// taps the control whose words best match their goal, scrolls within their patience, and gives up
// when they run out of taps. Same tasks, same clock (15:10), three designs.
const {chromium}=require('playwright');
const fs=require('fs');
const OUT=process.cwd();
const D='/home/user/Awqt/docs/design/';

const ADAPTERS={
 v8:{file:D+'yawm-ink-v8.html',root:'#screen',scroller:'#screen',
   async setup(p){await p.evaluate(()=>{const s=document.getElementById('scrub');s.value=910;s.dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('screen').scrollIntoView()})},
   async startOnb(p){await p.evaluate(()=>{document.getElementById('obBtn').click();document.getElementById('screen').scrollIntoView()})},
   async isMain(p){return p.evaluate(()=>[...document.querySelectorAll('#screen button')].some(b=>b.innerText.trim()==='المهام'&&b.getBoundingClientRect().height>0))},
   async setTime(p,target){let taps=0;for(let i=0;i<120;i++){const cur=await p.evaluate(()=>{const ed=[...document.querySelectorAll('#screen [data-at]')];if(!ed.length)return null;const txt=document.getElementById('screen').innerText;return txt});if(cur==null)return -1;
     const now=await p.evaluate(()=>{const b=document.querySelector('#screen [data-at]');let n=b;for(let k=0;k<6&&n;k++){n=n.parentElement;const m=n&&n.innerText.match(/(\d\d):(\d\d)/);if(m)return +m[1]*60+ +m[2]}return null});
     if(now==null||now===target)return taps;const d=target<now?'-15':'15';await p.click(`#screen [data-at="${d}"]`);taps++;await p.waitForTimeout(80);}return taps},
   timeVisible:p=>p.evaluate(()=>!!document.querySelector('#screen [data-at]'))},
 v9:{file:D+'yawm-simple-v9.html',root:'#phone',scroller:'#phone .scroll',
   async setup(p){await p.evaluate(()=>document.getElementById('phone').scrollIntoView())},
   async startOnb(p){await p.evaluate(()=>{document.getElementById('restart').click();document.getElementById('phone').scrollIntoView()})},
   async isMain(p){return p.evaluate(()=>!!document.querySelector('#phone .tabs'))},
   async setTime(p,target){let taps=0;const read=()=>p.evaluate(()=>{const v=document.querySelector('#phone .stepper .v');if(!v)return null;const m=v.innerText.match(/(\d\d):(\d\d)/);return +m[1]*60+ +m[2]});
     const start=await read();if(start==null)return -1;const d=target-start;const h=Math.round(d/60);const plan=[];
     for(let i=0;i<Math.abs(h);i++)plan.push(h<0?-60:60);const r=d-h*60;for(let i=0;i<Math.abs(r)/5;i++)plan.push(r<0?-5:5);
     for(const v of plan){await p.click(`#phone [data-act="am"][data-v="${v}"]`);taps++;await p.waitForTimeout(60)}return taps},
   timeVisible:p=>p.evaluate(()=>!!document.querySelector('#phone .stepper .v')&&!!document.querySelector('#phone [data-act="am"]'))},
 v10:{file:D+'yawm-prayers-v10.html',root:'#phone',scroller:'#phone .scroll',
   async setup(p){await p.evaluate(()=>document.getElementById('phone').scrollIntoView())},
   async startOnb(p){await p.evaluate(()=>{document.querySelector('[data-view="onb"]').click();document.getElementById('phone').scrollIntoView()})},
   async isMain(p){return p.evaluate(()=>!!document.querySelector('#phone .fab'))},
   async setTime(){return -1},timeVisible:async()=>false},
 v11:{file:D+'yawm-prayers-v11.html',root:'#phone',scroller:'#phone .scroll',
   async setup(p){await p.evaluate(()=>document.getElementById('phone').scrollIntoView())},
   async startOnb(p){await p.evaluate(()=>{document.querySelector('[data-view="onb"]').click();document.getElementById('phone').scrollIntoView()})},
   async isMain(p){return p.evaluate(()=>!!document.querySelector('#phone .fab'))},
   async setTime(){return -1},timeVisible:async()=>false},
};

const PERSONAS=[
 {id:'P1',name:'أبو أحمد، 58، متقاعد، تقنية ضعيفة',patience:6,scroll:1,avoid:{'الإعدادات':9,'إعدادات':9}},
 {id:'P2',name:'سارة، 29، موظفة مستعجلة',patience:5,scroll:1,avoid:{}},
 {id:'P3',name:'حسين، 21، طالب، سريع بالتلفون',patience:10,scroll:4,avoid:{}},
 {id:'P4',name:'أم علي، 40، ربة بيت، تقنية متوسطة',patience:7,scroll:2,avoid:{'فقرة':2,'تركيز':2,'محاسبة':2,'بالبال':2,'الإعدادات':3}},
];

const TASKS=[
 {id:'T3',name:'علّم الشغلة اللي هسه «خلصت»',kw:[['خلّصت',4],['صلّيت',3],['تم',1]],success:'doneMark'},
 {id:'T4',name:'أضف مهمة: اتصل بالمصلّح باچر',kw:[['مهمة',3],['أضف',2],['+',1],['المهام',2]],type:'اتصل بالمصلّح باچر',submit:['حطّها','احفظ','حفظ'],success:'textAfter',want:'المصلّح'},
 {id:'T5',name:'سوّي منبّه 05:45 كل يوم',kw:[['منبّه',4],['المنبّه',3],['جديد',2],['+',1],['أضف',1]],type:'منبّه الساعة 05:45',typeOnly:['شنو'],time:345,need:['كل يوم'],submit:['احفظ','حطّها','حفظ'],success:'alarmDaily'},
 {id:'T6',name:'شوكت أنام الليلة وشوكت أصحى؟',kw:[['تنام',3],['النوم',3],['الليلة',2],['الليل',2],['تصحى',2]],success:'sleepSeen'},
 {id:'T8',name:'أضف «قراءة كتاب بعد العشاء» كل يوم',kw:[['كل يوم',4],['يومك',3],['فقرة',2],['ثابتة',2],['أضف',2],['+',1]],type:'قراءة كتاب بعد العشاء',typeOnly:['شنو'],need:['كل يوم'],submit:['حطّها','احفظ','حفظ'],success:'routine'},
 {id:'T7',name:'أول مرة: من الترحيب لحد يومي',kw:[['اسمح بالتنبيهات',3],['التالي',4],['يلا',4],['نبني',2],['وريني',4],['تمام',3],['هذا يومي',4],['ابدأ',3],['جاهز',2]],success:'main',onb:true},
];

async function candidates(p,a){
 return p.evaluate(({root})=>{
  const R=document.querySelector(root).getBoundingClientRect();
  const vis=e=>{const r=e.getBoundingClientRect();if(r.width<2||r.height<2)return false;const top=Math.max(r.top,R.top,0),bot=Math.min(r.bottom,R.bottom,innerHeight);if(bot-top<8)return false;
    const cx=r.left+r.width/2,cy=(top+bot)/2;const hit=document.elementFromPoint(cx,cy);return hit&&(hit===e||e.contains(hit)||hit.contains(e))};
  const SEL='button,[role=button],[role=switch],[role=checkbox],[role=tab],a[href],input';
  const els=[...document.querySelector(root).querySelectorAll(SEL)].filter(vis);
  const uniq=els;
  return uniq.map((e,i)=>{e.setAttribute('data-pid',i);return {pid:i,tag:e.tagName,text:(e.innerText||e.getAttribute('aria-label')||e.placeholder||e.value||'').replace(/\s+/g,' ').trim(),
    pressed:e.getAttribute('aria-pressed')==='true'||e.getAttribute('aria-checked')==='true',
    disabled:e.disabled||e.getAttribute('aria-disabled')==='true'||+getComputedStyle(e).opacity<0.6,
    input:e.tagName==='INPUT'&&(!e.type||e.type==='text'||e.type==='search'),ph:(e.placeholder||'')+' '+(e.labels&&e.labels[0]?e.labels[0].innerText:''),val:e.value||''}});
 },{root:a.root});
}
const rootText=(p,a)=>p.evaluate(r=>{const el=document.querySelector(r);const t=el.innerText;const iv=[...el.querySelectorAll('input')].map(i=>i.value).join(' ');return {t,iv}},a.root);
const visibleText=(p,a)=>p.evaluate(r=>{const R=document.querySelector(r).getBoundingClientRect();const w=document.createTreeWalker(document.querySelector(r),NodeFilter.SHOW_TEXT);let s='',n;
  while(n=w.nextNode()){const rg=document.createRange();rg.selectNodeContents(n);const b=rg.getBoundingClientRect();if(b.height>0&&b.bottom>Math.max(R.top,0)&&b.top<Math.min(R.bottom,innerHeight))s+=n.textContent+' '}return s.replace(/\s+/g,' ')},a.root);
const rowHas=(p,a,x,y)=>p.evaluate(([r,x,y])=>[...document.querySelector(r).querySelectorAll('*')].some(e=>{const t=e.innerText||'';return t.length<70&&t.includes(x)&&t.includes(y)}),[a.root,x,y]);
const doneCount=s=>(s.match(/تم|ألغي|✓|خلصت/g)||[]).length;

async function check(task,p,a,ctx){
 const {t,iv}=await rootText(p,a);
 switch(task.success){
  case 'doneMark':return doneCount(t)>ctx.before;
  case 'textAfter':return ctx.submitted&&!iv.includes(task.want)&&(t.includes(task.want)||/انحطّت|انضافت/.test(t));
  case 'alarmDaily':return (ctx.submitted||ctx.timeSet)&&!iv.includes('05:45')&&await rowHas(p,a,'05:45','كل يوم');
  case 'sleepSeen':{const v=await visibleText(p,a);return v.includes('21:00')&&v.includes('04:28')}
  case 'routine':return !!ctx.routine||(ctx.submitted&&!iv.includes('كتاب')&&await rowHas(p,a,'كتاب','كل يوم'));
  case 'main':return a.isMain(p);
 }
}

async function runOne(browser,vk,persona,task){
 const a=ADAPTERS[vk];
 const page=await browser.newPage({viewport:{width:420,height:1000}});page.setDefaultTimeout(2500);
 const errs=[];page.on('pageerror',e=>errs.push(e.message));
 await page.goto('file://'+OUT+'/'+vk+'.html');await page.waitForTimeout(900);
 await a.setup(page);await page.waitForTimeout(300);
 if(task.onb){await a.startOnb(page);await page.waitForTimeout(300)}
 const ctx={before:doneCount((await rootText(page,a)).t),typed:false,submitted:false,timeSet:false,routine:false};
 const log=[];let taps=0,scrolls=0,typing=0,choicesSeen=0,result=null;const clicked=new Set();
 if(await check(task,page,a,ctx)){result='ok'}
 for(let step=0;!result&&step<30;step++){
  const c=await candidates(page,a);
  if(task.onb)choicesSeen=Math.max(choicesSeen,c.length);
  // type when the right field is on screen
  const field=c.find(x=>x.input&&!ctx.typed&&task.type&&(!task.typeOnly||task.typeOnly.some(w=>x.ph.includes(w))));
  if(field){await page.click(`[data-pid="${field.pid}"]`);await page.keyboard.type(task.type,{delay:5});ctx.typed=true;typing++;log.push('كتب: '+task.type);await page.waitForTimeout(250);continue}
  if(task.time&&!ctx.timeSet&&await a.timeVisible(page)){const n=await a.setTime(page,task.time);if(n>=0){taps+=n;ctx.timeSet=true;log.push(`ضبط الساعة بـ ${n} ضغطات`);if(await check(task,page,a,ctx)){result='ok';break}continue}}
  if((ctx.typed||ctx.timeSet)&&task.need){const nd=c.find(x=>!x.input&&!x.pressed&&!clicked.has(x.text)&&task.need.some(w=>x.text===w));
   if(nd){taps++;clicked.add(nd.text);log.push('اختار: «'+nd.text+'»');await page.click(`[data-pid="${nd.pid}"]`).catch(()=>{});await page.waitForTimeout(250);continue}}
  let best=null,bs=0;
  for(const x of c){
   if(x.input)continue;
   let s=0;
   if(ctx.typed||ctx.timeSet){for(const w of (task.submit||[]))if(x.text.includes(w))s+=6}
   for(const [w,wt] of task.kw)if(x.text.includes(w))s+=wt;
   for(const [w,pen] of Object.entries(persona.avoid))if(x.text.includes(w))s-=pen;
   if(!task.onb&&clicked.has(x.text)&&!(ctx.typed&&(task.submit||[]).some(w=>x.text.includes(w))))s-=10;
   if(s>bs||(s===bs&&best&&x.text.length<best.text.length)){bs=s;best=x}
  }
  if(best&&bs>=1&&best.disabled&&task.onb){const opt=c.find(x=>!x.input&&!x.disabled&&x!==best&&!/رجوع|تخطّى|سكّر/.test(x.text)&&!clicked.has(x.text));
   if(opt){taps++;clicked.add(opt.text);log.push('اختار جواب: «'+opt.text.slice(0,30)+'»');await page.click(`[data-pid="${opt.pid}"]`).catch(()=>{});await page.waitForTimeout(300);continue}}
  if(best&&bs>=1){
   taps++;clicked.add(best.text);log.push('ضغط: «'+best.text.slice(0,40)+'»');
   if((task.submit||[]).some(w=>best.text.includes(w)))ctx.submitted=true;
   if(task.id==='T8'&&/يومك|فقرة ثابتة|عدّل يومك/.test(best.text))ctx.routine='entry';
   await page.click(`[data-pid="${best.pid}"]`).catch(()=>{});await page.waitForTimeout(380);
   if(await check(task,page,a,ctx)){result=ctx.routine==='entry'?'entry':'ok';break}
   if(ctx.submitted&&['alarmDaily','routine','textAfter'].includes(task.success)){const {iv}=await rootText(page,a);if(!task.type||!iv.includes(task.type.split(' ')[0])){result='wrong';break}}
   if(taps>=persona.patience){result='gave_up';break}
   continue;
  }
  if(scrolls<persona.scroll){scrolls++;log.push('نزل لتحت');await page.evaluate(s=>{const el=document.querySelector(s);el.scrollBy(0,el.clientHeight*0.7)},a.scroller);await page.waitForTimeout(250);
   if(await check(task,page,a,ctx)){result='ok';break}continue}
  result='lost';log.push('ما لكه شي يناسبه');break;
 }
 const shot=`shot-${vk}-${persona.id}-${task.id}.png`;
 await page.locator(a.root).screenshot({path:shot}).catch(()=>{});
 const {t}=await rootText(page,a);const toast=(t.match(/انحطّت[^\n]*|المنبّه [^\n]*شغّال|تم ✓/)||[''])[0];
 await page.close();
 return {v:vk,p:persona.id,t:task.id,result:result||'gave_up',taps,scrolls,typing,choicesSeen,log,toast,errs,shot};
}

(async()=>{
 for(const [k,a] of Object.entries(ADAPTERS)){
  fs.writeFileSync(`${k}.html`,'<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body>'+fs.readFileSync(a.file,'utf8')+'</body></html>');
 }
 const b=await chromium.launch();const rows=[];
 for(const vk of Object.keys(ADAPTERS))for(const pe of PERSONAS)for(const t of TASKS){rows.push(await runOne(b,vk,pe,t));process.stdout.write('.')}
 await b.close();
 fs.writeFileSync('results.json',JSON.stringify(rows,null,1));
 console.log('\n'+rows.map(r=>`${r.v}\t${r.p}\t${r.t}\t${r.result}\ttaps=${r.taps}\tscroll=${r.scrolls}\t${r.toast}\t| ${r.log.join(' → ')}${r.errs.length?' ERR '+r.errs.join(';'):''}`).join('\n'));
})();
