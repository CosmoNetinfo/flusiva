(() => {
  const el=id=>document.getElementById(id);
  function render(state){
    const native=state.mode==='native',busy=['checking','downloading','installing'].includes(state.status);
    el('check-updates').disabled=busy||state.status==='downloaded';
    el('download-update').hidden=!native||state.status!=='available';
    el('install-update').hidden=!native||state.status!=='downloaded';
    el('install-update').disabled=!!state.installBlocked;
    el('open-update').hidden=native||!['available','error'].includes(state.status);
    el('update-download-progress').hidden=state.status!=='downloading';
    if(state.progress?.percent===null||!state.progress)el('update-download-progress').removeAttribute('value');else el('update-download-progress').value=state.progress.percent;
    const messages={
      idle:native?'Gli aggiornamenti vengono preparati automaticamente se l’opzione è attiva.':state.reason,
      checking:'Controllo delle Release ufficiali…',
      available:`È disponibile Flusiva ${state.latestVersion}.${native?' Puoi scaricarla dall’app.':' '+(state.reason||'Apri la Release ufficiale per installarla.')}`,
      current:`Flusiva ${state.currentVersion}: nessun aggiornamento disponibile.${!native?' '+state.reason:''}`,
      unpublished:'Non ci sono ancora Release pubblicate.',
      downloading:`Download dell’aggiornamento${state.progress?.percent!=null?` · ${Math.floor(state.progress.percent)}%`:''}${state.progress?` · ${window.flusivaMetrics.bytes(state.progress.transferred)} / ${window.flusivaMetrics.bytes(state.progress.total)} · ${window.flusivaMetrics.bytes(state.progress.speed)}/s`:''}.`,
      downloaded:`Flusiva ${state.latestVersion} è pronta.${state.installBlocked?' Termina i download e la coda prima di installare.':state.reason?' '+state.reason:' Puoi riavviare ora; con gli aggiornamenti automatici attivi verrà installata alla prossima chiusura normale.'}`,
      installing:'Installazione dell’aggiornamento e riavvio…',
      error:state.error||'Aggiornamento non riuscito. Riprova più tardi.'
    };
    el('update-status').textContent=messages[state.status]||state.reason||'';
    el('update-status').classList.toggle('error',state.status==='error');
    const notice=['available','downloading','downloaded'].includes(state.status);
    el('update-notice').hidden=!notice;
    el('update-notice').textContent=state.status==='downloaded'?'Aggiornamento pronto':state.status==='downloading'?'Download aggiornamento':`Flusiva ${state.latestVersion} disponibile`;
  }
  const error=error=>{el('update-status').textContent=error.message;el('update-status').classList.add('error');};
  el('check-updates').addEventListener('click',()=>invoke('checkUpdates').then(render).catch(error));
  el('download-update').addEventListener('click',()=>invoke('downloadUpdate').then(render).catch(error));
  el('install-update').addEventListener('click',()=>invoke('installUpdate').then(render).catch(error));
  el('open-update').addEventListener('click',()=>invoke('openUpdate').catch(error));
  el('update-notice').addEventListener('click',()=>{el('nav-settings').click();el('settings-updates').scrollIntoView({block:'center'});});
  if(window.flusiva){window.flusiva.onUpdateChanged(render);invoke('getUpdateState').then(render).catch(error);}
})();
