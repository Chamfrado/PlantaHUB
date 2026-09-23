// Captura a página em fatias rolando o #root (o scroller real do app); só lê.
const path=require('path'),os=require('os');
const {chromium}=require(require.resolve('playwright',{paths:[path.join(os.homedir(),'.resp-tools','node_modules')]}));
const [,,url,out,storage,widths='360,768,1366',H='2000']=process.argv;
(async()=>{const b=await chromium.launch();
for(const w of widths.split(',').map(Number)){const c=await b.newContext({viewport:{width:w,height:+H},...(storage&&storage!=='-'?{storageState:storage}:{})});const p=await c.newPage();
await p.goto(url,{waitUntil:'networkidle'});await p.waitForTimeout(400);
const total=await p.evaluate(()=>document.getElementById('root').scrollHeight);let k=0;
for(let y=0;y<total;y+=+H,k++){await p.evaluate(y=>document.getElementById('root').scrollTo(0,y),y);await p.waitForTimeout(350);await p.screenshot({path:`${out}/fatia-${w}-${k}.png`});}
const v=await p.evaluate((W)=>[...document.querySelectorAll('#root *')].filter(e=>{const r=e.getBoundingClientRect();if(!(r.width>0&&r.height>0&&(r.right>W+1||r.left<-1)))return false;let a=e.parentElement;while(a&&a.id!=='root'){const s=getComputedStyle(a);if(/(auto|scroll|hidden|clip)/.test(s.overflowX)){const ar=a.getBoundingClientRect();if(r.right>ar.right+1||r.left<ar.left-1)return false}a=a.parentElement}return true}).slice(0,20).map(e=>{const r=e.getBoundingClientRect();return e.tagName.toLowerCase()+'.'+String(e.className).slice(0,60)+' ['+Math.round(r.left)+','+Math.round(r.right)+']'}),w);
const cx=await p.evaluate(()=>[...document.querySelectorAll('#root *')].filter(e=>{const s=getComputedStyle(e);if(s.display==='inline'||!/visible/.test(s.overflowX))return false;const r=e.getBoundingClientRect();return r.width>0&&e.scrollWidth>e.clientWidth+2&&e.clientWidth>0}).slice(0,15).map(e=>e.tagName.toLowerCase()+'.'+String(e.className).slice(0,50)+' client='+e.clientWidth+' scroll='+e.scrollWidth));
console.log(w+'px transbordando-a-propria-caixa:',cx.length?'\n  '+cx.join('\n  '):'nenhum');
console.log(w+'px fatias='+k+' vazando-da-viewport(sem contêiner que corte):',v.length?'\n  '+v.join('\n  '):'nenhum');await c.close()}await b.close()})();
