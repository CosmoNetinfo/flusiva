const {test}=require('node:test');
const assert=require('node:assert/strict');
const {createQueue}=require('../core/queue.cjs');
const links=['https://vimeo.com/123','https://vimeo.com/456','https://vimeo.com/789'];
async function until(predicate){const end=Date.now()+3000;while(!predicate()){if(Date.now()>end)throw new Error('Timeout nella coda di test');await new Promise(resolve=>setTimeout(resolve,2));}}
test('esegue un solo job alla volta, continua dopo errore e conserva i metadati',async()=>{
  let concurrent=0,max=0;const order=[];
  const queue=createQueue({run:async(job,{onProgress})=>{concurrent++;max=Math.max(max,concurrent);order.push(job.url);try{onProgress({phase:'download',title:'Titolo reale',percent:40,speed:100,eta:2});await new Promise(resolve=>setTimeout(resolve,3));if(job.url===links[1])throw new Error('Non disponibile');return {path:`/saved/${job.id}.mp4`};}finally{concurrent--;}}});
  queue.add(links);queue.start();queue.start();await until(()=>queue.snapshot().items.every(item=>['done','failed'].includes(item.status)));
  assert.equal(max,1);assert.deepEqual(order,links);assert.deepEqual(queue.snapshot().items.map(item=>item.status),['done','failed','done']);assert.equal(queue.snapshot().items[1].error,'Non disponibile');assert.equal(queue.snapshot().items[0].title,'Titolo reale');
});
test('deduplica i link in attesa e valida l’intero batch prima di modificare la coda',()=>{
  const queue=createQueue({run:async()=>({}),maxItems:2});
  assert.deepEqual(queue.add([links[0],`${links[0]}?tracking=1`]),{added:1,duplicates:1});
  assert.throws(()=>queue.add([links[1],'https://evil.test/video']));assert.equal(queue.snapshot().items.length,1);
  assert.throws(()=>queue.add([links[1],links[2]]),/2 elementi/);assert.equal(queue.snapshot().items.length,1);
  assert.throws(()=>queue.add([]));assert.throws(()=>queue.add(Array(51).fill(links[0])));assert.throws(()=>queue.add([links[1]],{format:'mp3',quality:'--exec'}));
});
test('pausa dopo l’elemento attuale e riprende senza ripetere i file completati',async()=>{
  let finish;const seen=[];
  const queue=createQueue({run:job=>{seen.push(job.url);return new Promise(resolve=>{finish=()=>resolve({path:'/saved/file'});});}});
  queue.add(links.slice(0,2));queue.start();await until(()=>!!finish);queue.pause();finish();await until(()=>queue.snapshot().items[0].status==='done');
  assert.equal(queue.snapshot().items[1].status,'waiting');assert.equal(queue.snapshot().runningId,null);assert.deepEqual(seen,[links[0]]);
  finish=null;queue.start();await until(()=>!!finish);finish();await until(()=>queue.snapshot().items[1].status==='done');assert.deepEqual(seen,links.slice(0,2));
});
test('annulla il file corrente e uno in attesa, quindi consente un tentativo individuale',async()=>{
  let started=false;const queue=createQueue({run:async(_job,{signal})=>{started=true;await new Promise((resolve,reject)=>signal.addEventListener('abort',()=>reject(new Error('cancelled')),{once:true}));}});
  queue.add(links.slice(0,2));const ids=queue.snapshot().items.map(item=>item.id);queue.start();await until(()=>started);
  queue.pause();queue.cancel(ids[1]);queue.cancel(ids[0]);await until(()=>!queue.snapshot().runningId);assert.ok(queue.snapshot().items.every(item=>item.status==='cancelled'));
  queue.retry(ids[1]);assert.equal(queue.snapshot().items[1].status,'waiting');assert.equal(queue.snapshot().items[0].attempts,1);queue.cancelAll();queue.clearFinished();assert.equal(queue.snapshot().items.length,0);
});
test('annullare il selettore di destinazione cancella solo quel job e prosegue',async()=>{
  let calls=0;const queue=createQueue({run:async()=>++calls===1?{cancelled:true}:{path:'/saved/file'}});
  queue.add(links.slice(0,2));queue.start();await until(()=>queue.snapshot().items[1].status==='done');assert.deepEqual(queue.snapshot().items.map(item=>item.status),['cancelled','done']);
});
test('le tracce SoundCloud sono indicate come MP3 anche prima dell’analisi',()=>{
  const queue=createQueue({run:async()=>({})});queue.add(['https://soundcloud.com/artist/track'],{format:'mp4',quality:'best'});const item=queue.snapshot().items[0];assert.equal(item.format,'mp3');queue.cancel(item.id);queue.retry(item.id);assert.equal(queue.snapshot().items[0].format,'mp3');
});
