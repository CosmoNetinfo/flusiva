/* The renderer can only call the narrow, sandboxed preload API. */
const api = window.flusiva;
const $ = id => document.getElementById(id);
let video = null, format = 'mp4', busy = false, cancelled = false;
const setText = (id, text) => { $(id).textContent = text; };
function showError(message) { setText('url-error',message); $('url-error').hidden = false; $('video-url').setAttribute('aria-invalid','true'); }
function clearError() { $('url-error').hidden = true; $('video-url').removeAttribute('aria-invalid'); }
function setBusy(value) {
  busy = value;
  ['video-url','paste-button','inspect-button','quality'].forEach(id => { $(id).disabled = value; });
  document.querySelectorAll('[data-format]').forEach(button => { button.disabled = value; });
  $('download-button').disabled = value || !video || (format === 'mp4' && !video.heights.length) || (format === 'mp3' && !video.hasAudio);
}
function updateFormat() {
  document.querySelectorAll('[data-format]').forEach(button => { const selected = button.dataset.format === format; button.classList.toggle('selected',selected); button.setAttribute('aria-pressed',String(selected)); });
  document.querySelector('label[for="quality"]').textContent = format === 'mp4' ? 'Qualità del video' : 'Qualità dell’audio';
  setText('quality-note',format === 'mp4' ? 'La risoluzione dipende dal video originale.' : 'La conversione non migliora l’audio originale.');
  $('quality').replaceChildren();
  const choices = format === 'mp3' ? [['320','320 kbps'],['256','256 kbps'],['192','192 kbps'],['128','128 kbps']] : [['best','Migliore disponibile'],...(video?.heights || []).filter(height => [4320,2160,1440,1080,720,480,360,240,144].includes(height)).map(height => [String(height),`${height}p${height === 2160 ? ' · 4K' : height === 4320 ? ' · 8K' : ''}`])];
  choices.forEach(([value,label]) => $('quality').add(new Option(label,value)));
  document.querySelector('#download-button span').textContent = format === 'mp4' ? 'Scarica il video' : 'Scarica l’audio';
  setText('download-hint',!video ? 'Analizza un link per iniziare il download.' : format === 'mp4' && !video.heights.length ? 'Questo video non offre un formato MP4 disponibile.' : format === 'mp3' && !video.hasAudio ? 'Questo video non offre una traccia audio disponibile.' : 'Sceglierai dove salvare il file sul computer.');
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
  setText('progress-label','Recupero delle informazioni…');
  setText('progress-note','Connessione a YouTube dal tuo computer.');
  try {
    video = await invoke('inspect',$('video-url').value);
    setText('video-title',video.title); setText('video-channel',video.channel);
    const seconds = Math.floor(video.duration); const hours = Math.floor(seconds / 3600);
    setText('video-duration',`${hours ? `${hours}:` : ''}${String(Math.floor(seconds / 60) % 60).padStart(hours ? 2 : 1,'0')}:${String(seconds % 60).padStart(2,'0')}`);
    $('thumbnail').src = video.thumbnail; $('video-preview').hidden = false;
  } catch (error) { showError(error.message); }
  finally { $('progress-panel').hidden = true; setText('inspect-button','Analizza il link'); setBusy(false); updateFormat(); }
});
$('video-url').addEventListener('input',() => { clearError(); video = null; $('video-preview').hidden = true; $('result').hidden = true; updateFormat(); });
$('paste-button').addEventListener('click',async () => {
  try { $('video-url').value = await invoke('paste'); $('video-url').dispatchEvent(new Event('input')); $('video-url').focus(); }
  catch (error) { showError(error.message); }
});
document.querySelectorAll('[data-format]').forEach(button => button.addEventListener('click',() => { if (!busy) { format = button.dataset.format; updateFormat(); } }));
$('download-button').addEventListener('click',async () => {
  if (!video || busy) return;
  clearError(); setBusy(true); cancelled = false; $('result').hidden = true;
  $('progress-panel').hidden = false; $('download-progress').removeAttribute('value');
  setText('progress-label','Scegli dove salvare il file…'); setText('progress-note','Il download inizierà dopo la scelta della cartella.');
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
if (api) {
  api.onProgress(progress => {
    if (cancelled) return;
    if (progress.phase === 'download') {
      $('download-progress').value = progress.percent;
      setText('progress-label',`Download in corso · ${Math.floor(progress.percent)}%`);
      setText('progress-note','Video e audio possono essere scaricati in tracce separate.');
    } else { $('download-progress').removeAttribute('value'); setText('progress-label',format === 'mp3' ? 'Conversione in MP3…' : 'Unione di video e audio…'); setText('progress-note','Il tempo necessario dipende dal file e dal computer.'); }
  });
  invoke('about').then(info => { setText('version',`Flusiva ${info.version}`); setText('system-info',`${({win32:'Windows',darwin:'macOS',linux:'Linux'})[info.platform] || info.platform} · ${info.arch} · versione ${info.version}`); }).catch(() => {});
} else { setText('download-hint','I download sono disponibili nell’app desktop Flusiva.'); }
updateFormat();
