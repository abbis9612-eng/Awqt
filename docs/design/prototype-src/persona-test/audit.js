const {chromium}=require('playwright');
const CFG={v8:{root:'#screen',setup:"(()=>{const s=document.getElementById('scrub');s.value=910;s.dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('screen').scrollIntoView()})()"},
 v9:{root:'#phone',setup:"document.getElementById('phone').scrollIntoView()"},v10:{root:'#phone',setup:"document.getElementById('phone').scrollIntoView()"},v11:{root:'#phone',setup:"document.getElementById('phone').scrollIntoView()"}};
(async()=>{const b=await chromium.launch();
for(const [k,c] of Object.entries(CFG)){
 const p=await b.newPage({viewport:{width:420,height:1000}});
 await p.goto('file://'+process.cwd()+'/'+k+'.html');await p.waitForTimeout(1000);await p.evaluate(c.setup);await p.waitForTimeout(400);
 const r=await p.evaluate(root=>{
  const R=document.querySelector(root).getBoundingClientRect();const H=Math.min(R.bottom,innerHeight), T=Math.max(R.top,0);
  const lum=c=>{const m=c.match(/[\d.]+/g);if(!m)return null;const [r,g,b,a]=m.map(Number);if(a===0)return null;const f=x=>{x/=255;return x<=.03928?x/12.92:((x+.055)/1.055)**2.4};return .2126*f(r)+.7152*f(g)+.0722*f(b)};
  const bgOf=e=>{for(let n=e;n;n=n.parentElement){const cs=getComputedStyle(n);const m=cs.backgroundColor.match(/[\d.]+/g);if(m&&(m[3]===undefined||+m[3]>0.5))return cs.backgroundColor}return 'rgb(255,255,255)'};
  const sel='button,[role=button],[role=switch],[role=checkbox],[role=tab],a[href],input';
  const inView=e=>{const b=e.getBoundingClientRect();return b.width>1&&b.height>1&&b.bottom>T&&b.top<H&&b.right>R.left&&b.left<R.right};
  const ctrls=[...document.querySelector(root).querySelectorAll(sel)].filter(inView);
  const small=ctrls.filter(e=>{const b=e.getBoundingClientRect();return Math.min(b.width,b.height)<44});
  const w=document.createTreeWalker(document.querySelector(root),NodeFilter.SHOW_TEXT);let n,words=0,sizes=new Set(),minSize=99,low=[];
  while(n=w.nextNode()){const t=n.textContent.trim();if(!t)continue;const el=n.parentElement;const rg=document.createRange();rg.selectNodeContents(n);const bb=rg.getBoundingClientRect();
   if(!(bb.height>0&&bb.bottom>T&&bb.top<H))continue;const cs=getComputedStyle(el);if(cs.visibility==='hidden'||+cs.opacity===0)continue;
   words+=t.split(/\s+/).length;const fs=parseFloat(cs.fontSize);sizes.add(Math.round(fs));minSize=Math.min(minSize,fs);
   const clip=cs.webkitBackgroundClip==='text'||cs.backgroundClip==='text';
   const L1=clip?null:lum(cs.color),L2=lum(bgOf(el));if(L1!=null&&L2!=null){const cr=(Math.max(L1,L2)+.05)/(Math.min(L1,L2)+.05);const need=fs>=24||(fs>=18.66&&+cs.fontWeight>=700)?3:4.5;if(cr<need)low.push(t.slice(0,18)+' '+cr.toFixed(1))}
   if(clip)low.push('[كلمة بحبر جزئي] '+t.slice(0,12))}
  return {controls:ctrls.length,small:small.length,smallEx:small.slice(0,4).map(e=>(e.innerText||e.getAttribute('aria-label')||'').trim().slice(0,14)),words,sizes:[...sizes].sort((a,b)=>a-b),minSize,low:low.slice(0,8),lowN:low.length,screenH:Math.round(H-T)}
 },c.root);
 console.log(k,JSON.stringify(r));
 await p.screenshot({path:`first-${k}.png`});await p.close();}
await b.close()})();
