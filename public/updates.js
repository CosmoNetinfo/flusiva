(() => {
  const el=id=>document.getElementById(id);let checkedAtStartup=false;
  async function check(){
    el('check-updates').disabled=true;el('update-status').textContent='Controllo delle Release ufficiali…';el('update-status').classList.remove('error');
    try{
      const result=await invoke('checkUpdates');
      el('open-update').hidden=result.status!=='available';el('update-notice').hidden=result.status!=='available';
      el('update-status').textContent=result.status==='available'?`È disponibile Flusiva ${result.latestVersion}. Versione installata: ${result.currentVersion}. Scarica l’installer dalla Release ufficiale.`:result.status==='unpublished'?'Non ci sono ancora Release pubblicate.':`Flusiva ${result.currentVersion}: nessun aggiornamento disponibile.`;
      if(result.status==='available')el('update-notice').textContent=`Flusiva ${result.latestVersion} disponibile`;
    }catch(error){el('update-status').textContent=error.message;el('update-status').classList.add('error');}
    finally{el('check-updates').disabled=false;}
  }
  el('check-updates').addEventListener('click',check);
  for(const id of ['open-update','update-notice'])el(id).addEventListener('click',()=>invoke('openUpdate').catch(error=>{el('update-status').textContent=error.message;}));
  document.addEventListener('flusiva:preferences',event=>{if(!checkedAtStartup){checkedAtStartup=true;if(event.detail.checkUpdatesOnStartup)void check();}});
})();
