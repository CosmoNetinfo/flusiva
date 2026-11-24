const {randomUUID}=require('node:crypto');
const {identify,downloadOptions}=require('./platforms.cjs');
const TERMINAL=new Set(['done','failed','cancelled']);
function createQueue({run,onChange=()=>{},maxItems=100}){
  const items=[];let paused=true,running=null,controller=null,scheduled=false;
  const snapshot=()=>({paused,runningId:running?.id||null,items:items.map(item=>({...item,progress:item.progress?{...item.progress}:null}))});
  const emit=()=>onChange(snapshot());
  const schedule=()=>{if(scheduled)return;scheduled=true;queueMicrotask(()=>{scheduled=false;void pump();});};
  async function pump(){
    if(paused||running)return;
    const item=items.find(job=>job.status==='waiting');
    if(!item){paused=true;emit();return;}
    running=item;controller=new AbortController();const signal=controller.signal;
    item.status='inspecting';item.attempts++;item.error=null;item.progress=null;emit();
    try{
      const result=await run({...item},{signal,onProgress:progress=>{
        if(running!==item||signal.aborted)return;
        const states={inspect:'inspecting',download:'downloading',conversion:'converting',saving:'saving'};
        if(states[progress.phase])item.status=states[progress.phase];
        for(const key of ['title','platformName','format','quality'])if(typeof progress[key]==='string')item[key]=progress[key];
        item.progress={...progress};emit();
      }});
      if(result?.cancelled)item.status='cancelled';
      else{item.status='done';item.path=result?.path||null;}
    }catch(error){item.status=signal.aborted?'cancelled':'failed';item.error=signal.aborted?null:String(error.message||'Download non riuscito.').slice(0,2000);}
    finally{item.progress=null;running=null;controller=null;emit();schedule();}
  }
  return {
    snapshot,
    isBusy:()=>!!running||(!paused&&items.some(item=>item.status==='waiting')),
    hasUnfinished:()=>!!running||items.some(item=>item.status==='waiting'),
    add(urls,{format='mp4',quality='best'}={}){
      if(!Array.isArray(urls)||!urls.length||urls.length>50)throw new Error('Inserisci da 1 a 50 link, uno per riga.');
      downloadOptions(format,quality);
      const candidates=urls.map((url,index)=>{
        try{return identify(url);}catch(error){throw new Error(`Link ${index+1}: ${error.message}`);}
      });
      const existing=new Set(items.filter(item=>!TERMINAL.has(item.status)).map(item=>`${item.url}|${item.requestedFormat}|${item.requestedQuality}`));
      const unique=[];let duplicates=0;
      for(const candidate of candidates){const key=`${candidate.url}|${format}|${quality}`;if(existing.has(key)){duplicates++;continue;}existing.add(key);unique.push(candidate);}
      if(items.length+unique.length>maxItems)throw new Error(`La coda può contenere ${maxItems} elementi. Rimuovi quelli terminati e riprova.`);
      for(const candidate of unique)items.push({id:randomUUID(),url:candidate.url,platform:candidate.platform,title:candidate.url,format:candidate.platform==='soundcloud'?'mp3':format,quality,requestedFormat:format,requestedQuality:quality,status:'waiting',progress:null,path:null,error:null,attempts:0});
      emit();if(!paused)schedule();return {added:unique.length,duplicates};
    },
    start(){paused=false;emit();schedule();return snapshot();},
    pause(){paused=true;emit();return snapshot();},
    cancel(id){
      const item=items.find(job=>job.id===id);if(!item)throw new Error('Elemento della coda non trovato.');
      if(item===running){controller.abort();item.status='cancelling';}else if(item.status==='waiting')item.status='cancelled';
      emit();return snapshot();
    },
    cancelAll(){paused=true;for(const item of items)if(item.status==='waiting')item.status='cancelled';if(controller){controller.abort();running.status='cancelling';}emit();return snapshot();},
    retry(id){const item=items.find(job=>job.id===id);if(!item||!['failed','cancelled'].includes(item.status))throw new Error('Puoi ritentare soltanto un elemento fallito o annullato.');item.status='waiting';item.error=null;item.path=null;item.progress=null;item.format=item.platform==='soundcloud'?'mp3':item.requestedFormat;item.quality=item.requestedQuality;emit();if(!paused)schedule();return snapshot();},
    clearFinished(){for(let index=items.length-1;index>=0;index--)if(TERMINAL.has(items[index].status))items.splice(index,1);emit();return snapshot();}
  };
}
module.exports={createQueue};
