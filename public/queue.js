(() => {
  const el=id=>document.getElementById(id),rows=new Map();
  let state={paused:true,runningId:null,items:[]},singleBusy=false;
  let defaults={videoQuality:'best',audioQuality:'320',defaultFormat:'mp4'};
  const labels={waiting:'In attesa',inspecting:'Analisi del link',downloading:'Download in corso',converting:'Preparazione del file',saving:'Salvataggio',cancelling:'Annullamento',done:'Completato',failed:'Non riuscito',cancelled:'Annullato'};
  const terminal=new Set(['done','failed','cancelled']);
  function announce(text,failed=false){el('queue-announcement').textContent=failed?'':text;el('queue-error').hidden=!failed;el('queue-error').textContent=failed?text:'';}
  const button=(label,action)=>{const item=document.createElement('button');item.type='button';item.className='button secondary';item.textContent=label;item.addEventListener('click',()=>action().catch(error=>announce(error.message,true)));return item;};
  function createRow(job){
    const row=document.createElement('li');row.className='queue-row';row.dataset.id=job.id;
    const number=document.createElement('span');number.className='queue-position';
    const body=document.createElement('div');body.className='queue-row-body';
    const title=document.createElement('strong');title.className='queue-row-title';
    const detail=document.createElement('span');detail.className='queue-row-detail';
    const meter=document.createElement('progress');meter.max=100;meter.setAttribute('aria-label','Avanzamento del file');
    const message=document.createElement('p');message.className='queue-row-message';
    const actions=document.createElement('div');actions.className='queue-row-actions';
    body.append(title,detail,meter,message);row.append(number,body,actions);
    const record={row,number,title,detail,meter,message,actions,status:null};rows.set(job.id,record);return record;
  }
  function render(value){
    state=value;
    const unfinished=value.items.filter(job=>!terminal.has(job.status));
    el('queue-count').textContent=String(value.items.length);
    el('queue-nav-count').textContent=unfinished.length?String(unfinished.length):'';
    el('queue-empty').hidden=value.items.length>0;
    el('queue-start').disabled=singleBusy||!value.items.some(job=>job.status==='waiting')||!value.paused;
    el('queue-start').textContent=value.runningId&&value.paused?'Riprendi coda':'Avvia coda';
    el('queue-pause').disabled=value.paused;
    el('queue-clear').disabled=!value.items.some(job=>terminal.has(job.status));
    el('queue-cancel-all').disabled=!unfinished.length;
    el('queue-status').textContent=value.runningId?(value.paused?'In pausa dopo il file attuale.':'I file vengono scaricati in sequenza.'):value.items.some(job=>job.status==='waiting')?'Coda pronta. Premi Avvia coda per iniziare.':'La coda resta disponibile finché Flusiva è aperta.';
    const ids=new Set(value.items.map(job=>job.id));
    for(const [id,record]of rows)if(!ids.has(id)){record.row.remove();rows.delete(id);}
    value.items.forEach((job,index)=>{
      const record=rows.get(job.id)||createRow(job);
      if(!record.row.isConnected)el('queue-list').append(record.row);
      record.number.textContent=String(index+1);record.title.textContent=job.title;record.title.title=job.title;
      record.detail.textContent=`${job.platformName||({youtube:'YouTube',tiktok:'TikTok',instagram:'Instagram',facebook:'Facebook',vimeo:'Vimeo',soundcloud:'SoundCloud',dailymotion:'Dailymotion'})[job.platform]} · ${job.format==='image'?'FOTO':job.format.toUpperCase()} · ${labels[job.status]}`;
      record.row.dataset.status=job.status;
      record.meter.hidden=!value.runningId||value.runningId!==job.id;
      if(job.progress?.phase==='download'&&job.progress.percent!==null)record.meter.value=job.progress.percent;else record.meter.removeAttribute('value');
      record.message.textContent=job.error||window.flusivaMetrics.summary(job.progress)||(job.status==='done'?'File salvato sul computer.':'');
      record.message.classList.toggle('error',job.status==='failed');
      if(record.status!==job.status){
        record.status=job.status;record.actions.replaceChildren();
        if(job.status==='done')record.actions.append(button('Mostra file',()=>invoke('revealQueueItem',job.id)));
        else if(['failed','cancelled'].includes(job.status))record.actions.append(button('Riprova file',()=>invoke('retryQueueItem',job.id)));
        else if(job.status!=='cancelling')record.actions.append(button('Annulla file',()=>invoke('cancelQueueItem',job.id)));
      }
    });
    document.dispatchEvent(new CustomEvent('flusiva:queue',{detail:value}));
  }
  function choices(){
    const audio=el('queue-format').value==='mp3';
    const options=audio?[['320','320 kbps'],['256','256 kbps'],['192','192 kbps'],['128','128 kbps']]:[['best','Migliore disponibile'],['2160','4K · 2160p'],['1440','1440p'],['1080','1080p'],['720','720p'],['480','480p'],['360','360p']];
    const preferred=defaults.maximumQuality?(audio?'320':'best'):(audio?defaults.audioQuality:defaults.videoQuality);
    if(!options.some(([value])=>value===preferred))options.push([preferred,`${preferred}p`]);
    el('queue-quality').replaceChildren(...options.map(([value,label])=>new Option(label,value)));
    el('queue-quality').value=preferred;
    el('queue-quality').disabled=defaults.maximumQuality===true;
  }
  document.addEventListener('flusiva:preferences',event=>{defaults=event.detail;el('queue-format').value=defaults.defaultFormat;choices();});
  document.addEventListener('flusiva:single-busy',event=>{singleBusy=event.detail;el('queue-start').disabled=singleBusy||!state.paused||!state.items.some(job=>job.status==='waiting');});
  document.addEventListener('flusiva:queue-added',event=>{announce(`${event.detail.added} aggiunti · ${event.detail.duplicates} duplicati saltati`);document.querySelector('.queue-panel').scrollIntoView({behavior:'auto',block:'start'});});
  el('nav-queue').addEventListener('click',()=>{el('nav-downloads').click();document.querySelector('.queue-panel').scrollIntoView({behavior:'auto',block:'start'});});
  el('queue-format').addEventListener('change',choices);
  el('queue-form').addEventListener('submit',async event=>{
    event.preventDefault();
    const urls=el('queue-links').value.split(/\r?\n/).map(line=>line.trim()).filter(Boolean);
    try{const added=await invoke('addToQueue',{urls,format:el('queue-format').value,quality:el('queue-quality').value});el('queue-links').value='';announce(`${added.added} aggiunti · ${added.duplicates} duplicati saltati`);}
    catch(error){announce(error.message,true);}
  });
  el('queue-paste').addEventListener('click',async()=>{try{el('queue-links').value=await invoke('paste');el('queue-links').focus();}catch(error){announce(error.message,true);}});
  for(const [id,method]of [['queue-start','startQueue'],['queue-pause','pauseQueue'],['queue-clear','clearQueue'],['queue-cancel-all','cancelQueue']])el(id).addEventListener('click',()=>invoke(method).catch(error=>announce(error.message,true)));
  if(window.flusiva){window.flusiva.onQueueChanged(render);invoke('getQueue').then(render).catch(error=>announce(error.message,true));}
  choices();
})();
