/* The renderer can only call the narrow, sandboxed preload API. */
const api = window.flusiva;
const $ = id => document.getElementById(id);
let video = null, format = 'mp4', busy = false, cancelled = false;
let queueRunning=false;
document.addEventListener('flusiva:queue',event=>{const state=event.detail;queueRunning=!!state.runningId||(!state.paused&&state.items.some(item=>item.status==='waiting'));setBusy(busy);});
let preferences={defaultFormat:'mp4',videoQuality:'best',audioQuality:'320',askWhereToSave:true};
document.addEventListener('flusiva:preferences',event=>{
  preferences=event.detail;
  if(!busy){format=video?.photos?.length?'image':video?.hasVideo===false?'mp3':preferences.defaultFormat;updateFormat();}
});
const setText = (id, text) => { $(id).textContent = text; };
function showError(message) { setText('url-error',message); $('url-error').hidden = false; $('video-url').setAttribute('aria-invalid','true'); }
function clearError() { $('url-error').hidden = true; $('video-url').removeAttribute('aria-invalid'); }
function setBusy(value) {
  busy = value;
  const locked=value||queueRunning;
  ['video-url','paste-button','inspect-button','quality'].forEach(id => { $(id).disabled = locked; });
  $('quality').disabled=locked||(format!=='image'&&preferences.maximumQuality===true);
  document.querySelectorAll('[data-format]').forEach(button => { button.disabled = locked; });
  $('download-button').disabled = locked || !video || (format === 'mp4' && !video.hasVideo) || (format === 'mp3' && !video.hasAudio);
  $('enqueue-current').disabled=value||!video||format==='image';
  document.querySelector('[data-format="mp4"]').disabled=locked||video?.hasVideo===false;
  document.querySelector('[data-format="mp3"]').disabled=locked||video?.hasAudio===false;
  document.querySelector('[data-format="image"]').hidden=!video?.photos?.length;
  document.dispatchEvent(new CustomEvent('flusiva:single-busy',{detail:value}));
}
function updateFormat() {
  if(format==='image'){
    document.querySelectorAll('[data-format]').forEach(button=>{button.classList.toggle('selected',button.dataset.format===format);button.setAttribute('aria-pressed',String(button.dataset.format===format));});
    document.querySelector('label[for="quality"]').textContent='Foto del post';
    $('quality').replaceChildren();(video?.photos||[]).forEach((photo,index)=>$('quality').add(new Option(`Foto ${index+1}${photo.width&&photo.height?` · ${photo.width}×${photo.height}`:''}`,String(index))));
    document.querySelector('#download-button span').textContent='Scarica la foto da Instagram';
    setText('quality-note','Immagine alla migliore dimensione fornita, senza ricompressione.');setText('download-hint','Scegli la foto del post da salvare.');setBusy(busy);return;
  }
  document.querySelectorAll('[data-format]').forEach(button => { const selected = button.dataset.format === format; button.classList.toggle('selected',selected); button.setAttribute('aria-pressed',String(selected)); });
  document.querySelector('label[for="quality"]').textContent = format === 'mp4' ? 'Qualità del video' : 'Qualità dell’audio';
  setText('quality-note',preferences.maximumQuality?(format==='mp4'?'Qualità massima: migliore sorgente video e audio.':'Migliore sorgente audio → MP3 a 320 kbps.'):format === 'mp4' ? 'La risoluzione dipende dal video originale.' : 'La conversione non migliora l’audio originale.');
  $('quality').replaceChildren();
  const choices = format === 'mp3' ? [['320','320 kbps'],['256','256 kbps'],['192','192 kbps'],['128','128 kbps']] : [['best','Migliore disponibile'],...[4320,2160,1440,1080,720,480,360,240,144].filter(height=>!video||(height<=Math.max(...video.heights,0)&&video.heights.some(available=>available<=height))).map(height => [String(height),`${height}p${height === 2160 ? ' · 4K' : height === 4320 ? ' · 8K' : ''}`])];
  choices.forEach(([value,label]) => $('quality').add(new Option(label,value)));
  const preferred=preferences.maximumQuality?(format==='mp3'?'320':'best'):(format==='mp3'?preferences.audioQuality:preferences.videoQuality);
  if(choices.some(([value])=>value===preferred))$('quality').value=preferred;
  document.querySelector('#download-button span').textContent = video?`Scarica ${format.toUpperCase()} da ${video.platformName}`:(format === 'mp4' ? 'Scarica il video' : 'Scarica l’audio');
  setText('download-hint',!video?'Analizza un link per iniziare il download.':format==='mp4'&&!video.hasVideo?'Questo contenuto è solo audio. Scegli MP3.':format==='mp3'&&!video.hasAudio?'Questo contenuto non ha una traccia audio.':preferences.askWhereToSave?'Sceglierai dove salvare il file sul computer.':'Il file sarà salvato nella cartella delle Impostazioni.');
  setBusy(busy);
}
function result(title,message,failed = false) {
  $('result').hidden = false; $('result').classList.toggle('failed',failed);
  setText('result-title',title); setText('result-message',message);
  $('reveal-button').hidden = failed;
}
async function invoke(method,...args) {
  if (!api) throw new Error('Apri Flusiva dall’app installata per utilizzare i download.');
  const response = await api[method](...args);
  if (!response.ok) throw new Error(response.error);
  return response.value;
}
$('link-form').addEventListener('submit',async event => {
  event.preventDefault(); if (busy) return; clearError();
  if (!$('video-url').value.trim()) { showError('Incolla il link del video che vuoi scaricare.'); $('video-url').focus(); return; }
  setBusy(true); video = null; $('result').hidden = true; $('video-preview').hidden = true;
  setText('inspect-button','Analisi in corso…');
  $('progress-panel').hidden = false; $('download-progress').removeAttribute('value');
  $('download-metrics').hidden=true;
  setText('progress-label','Recupero delle informazioni…');
  setText('progress-note','Connessione alla piattaforma dal tuo computer.');
  try {
    video = await invoke('inspect',$('video-url').value);
    setText('video-title',video.title); setText('video-channel',`${video.platformName} · ${video.channel}`);
    if(video.photos?.length)format='image';else if(!video.hasVideo)format='mp3';else if(!video.hasAudio)format='mp4';
    const seconds = Math.floor(video.duration); const hours = Math.floor(seconds / 3600);
    setText('video-duration',video.duration==null?'Durata non fornita · verifica sul file':`${hours ? `${hours}:` : ''}${String(Math.floor(seconds / 60) % 60).padStart(hours ? 2 : 1,'0')}:${String(seconds % 60).padStart(2,'0')}`);
    $('thumbnail').src = video.thumbnail||'./mark.svg'; $('video-preview').hidden = false;
  } catch (error) { showError(error.message); }
  finally { $('progress-panel').hidden = true; setText('inspect-button','Analizza il link'); setBusy(false); updateFormat(); }
});
$('video-url').addEventListener('input',() => { clearError(); video = null; format=preferences.defaultFormat; $('video-preview').hidden = true; $('result').hidden = true; updateFormat(); });
$('paste-button').addEventListener('click',async () => {
  try { $('video-url').value = await invoke('paste'); $('video-url').dispatchEvent(new Event('input')); $('video-url').focus(); }
  catch (error) { showError(error.message); }
});
document.querySelectorAll('[data-format]').forEach(button => button.addEventListener('click',() => { if (!busy) { format = button.dataset.format; updateFormat(); } }));
$('download-button').addEventListener('click',async () => {
  if (!video || busy) return;
  clearError(); setBusy(true); cancelled = false; $('result').hidden = true;
  $('progress-panel').hidden = false; $('download-progress').removeAttribute('value');
  $('download-metrics').hidden=true;
  setText('progress-label',format==='image'?'Download dell’immagine originale…':preferences.askWhereToSave?'Scegli dove salvare il file…':'Preparazione del download…'); setText('progress-note',format==='image'?'Preparazione della foto senza ricompressione.':preferences.askWhereToSave?'Il download inizierà dopo la scelta della cartella.':'Il file sarà salvato nella tua cartella preferita.');
  try {
    const saved = await invoke('download',{ format, quality: $('quality').value });
    if (!saved.cancelled) result('Download completato',saved.path);
  } catch (error) { if (!cancelled) result('Download non riuscito',error.message,true); }
  finally { $('progress-panel').hidden = true; setBusy(false); }
});
$('cancel-button').addEventListener('click',async () => { cancelled = true; setText('progress-label','Annullamento in corso…'); await invoke('cancel').catch(() => {}); });
$('reveal-button').addEventListener('click',() => invoke('reveal').catch(error => showError(error.message)));
$('help-button').addEventListener('click',() => $('help-dialog').showModal());
$('close-help').addEventListener('click',() => $('help-dialog').close());
$('thumbnail').addEventListener('error',()=>{if(!$('thumbnail').src.endsWith('/mark.svg'))$('thumbnail').src='./mark.svg';});
$('enqueue-current').addEventListener('click',async()=>{
  if(!video||busy)return;
  try{const result=await invoke('addToQueue',{urls:[video.url],format,quality:$('quality').value});document.dispatchEvent(new CustomEvent('flusiva:queue-added',{detail:result}));}
  catch(error){showError(error.message);}
});
if (api) {
  api.onProgress(progress => {
    if (cancelled) return;
    if (progress.phase === 'download') {
      if(progress.percent===null)$('download-progress').removeAttribute('value');else $('download-progress').value=progress.percent;
      $('download-metrics').hidden=false;
      setText('metric-bytes',`${window.flusivaMetrics.bytes(progress.downloadedBytes)}${progress.totalBytes?` / ${progress.totalEstimated?'circa ':''}${window.flusivaMetrics.bytes(progress.totalBytes)}`:''}`);
      setText('metric-speed',progress.speed===null?'—':`${window.flusivaMetrics.bytes(progress.speed)}/s`);
      setText('metric-eta',window.flusivaMetrics.eta(progress.eta));
      setText('progress-label',`Download in corso${progress.percent===null?'':` · ${Math.floor(progress.percent)}%`}`);
      setText('progress-note','Video e audio possono essere scaricati in tracce separate.');
    } else { $('download-metrics').hidden=true; $('download-progress').removeAttribute('value'); setText('progress-label',progress.phase==='saving'?'Salvataggio del file…':format === 'mp3' ? 'Conversione in MP3…' : 'Unione di video e audio…'); setText('progress-note','Il tempo necessario dipende dal file e dal computer.'); }
  });
  invoke('about').then(info => { setText('version',`Flusiva ${info.version}`); setText('system-info',`${({win32:'Windows',darwin:'macOS',linux:'Linux'})[info.platform] || info.platform} · ${info.arch} · versione ${info.version}`); }).catch(() => {});
} else { setText('download-hint','I download sono disponibili nell’app desktop Flusiva.'); }
updateFormat();
