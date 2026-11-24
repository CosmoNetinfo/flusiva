(() => {
  const el=id=>document.getElementById(id);
  let saved=null,draftDirectory=null,initializing=true;
  const fields={defaultFormat:'default-format',videoQuality:'default-video-quality',audioQuality:'default-audio-quality',theme:'app-theme',askWhereToSave:'ask-where',openFolderAfterDownload:'open-folder',reduceMotion:'reduce-motion',autoUpdateEnabled:'check-updates-startup',maximumQuality:'maximum-quality'};
  document.querySelector('.settings-actions').before(el('settings-updates'));
  const systemTheme=matchMedia('(prefers-color-scheme: dark)');
  function appearance(preferences) {
    const theme=preferences.theme==='system'?(systemTheme.matches?'dark':'light'):preferences.theme;
    document.documentElement.dataset.theme=theme;
    document.documentElement.dataset.motion=preferences.reduceMotion?'reduced':'full';
  }
  function apply(preferences) {
    saved=preferences;draftDirectory=null;
    for(const [key,id] of Object.entries(fields)) {
      const control=el(id);if(control.type==='checkbox')control.checked=preferences[key];else control.value=preferences[key];
    }
    el('download-directory').value=preferences.downloadDirectory;
    appearance(preferences);
    document.dispatchEvent(new CustomEvent('flusiva:preferences',{detail:preferences}));
    el('default-video-quality').disabled=preferences.maximumQuality;
    el('default-audio-quality').disabled=preferences.maximumQuality;
  }
  function status(message,failed=false) {
    el('settings-status').textContent=message;el('settings-status').classList.toggle('error',failed);
  }
  function draft() {
    const value={};
    for(const [key,id]of Object.entries(fields))value[key]=el(id).type==='checkbox'?el(id).checked:el(id).value;
    if(draftDirectory)value.downloadDirectory=draftDirectory;
    return value;
  }
  function navigate(settings) {
    el('top').hidden=settings;el('settings-view').hidden=!settings;
    (settings?el('nav-settings'):el('nav-downloads')).setAttribute('aria-current','page');
    (settings?el('nav-downloads'):el('nav-settings')).removeAttribute('aria-current');
    document.querySelector('.skip-link').href=settings?'#settings-title':'#top';
    window.scrollTo(0,0);
    if(settings)el('settings-title').focus({preventScroll:true});
  }
  el('nav-settings').addEventListener('click',()=>navigate(true));
  el('nav-downloads').addEventListener('click',()=>navigate(false));
  el('settings-form').addEventListener('change',()=>{
    if(initializing)return;
    status('Modifiche da salvare');
    appearance(draft());
    el('default-video-quality').disabled=el('maximum-quality').checked;
    el('default-audio-quality').disabled=el('maximum-quality').checked;
  });
  systemTheme.addEventListener('change',()=>{if(saved)appearance(el('settings-view').hidden?saved:draft());});
  el('settings-form').addEventListener('submit',async event=>{
    event.preventDefault();el('save-settings').disabled=true;
    try{apply(await invoke('updateSettings',draft()));status('Preferenze salvate');}
    catch(error){status(error.message,true);}
    finally{el('save-settings').disabled=false;}
  });
  el('choose-folder').addEventListener('click',async()=>{
    el('choose-folder').disabled=true;
    try{const chosen=await invoke('chooseFolder');if(chosen){draftDirectory=chosen;el('download-directory').value=chosen;status('Modifiche da salvare');}}
    catch(error){status(error.message,true);}
    finally{el('choose-folder').disabled=false;}
  });
  el('reset-settings').addEventListener('click',async()=>{
    try{apply(await invoke('resetSettings'));status('Preferenze ripristinate');}catch(error){status(error.message,true);}
  });
  el('view-releases').addEventListener('click',()=>invoke('releases').catch(error=>status(error.message,true)));
  if(window.flusiva) {
    Promise.all([invoke('getSettings'),invoke('about')]).then(([preferences,about])=>{
      apply(preferences);
      for(const platform of about.platforms) {
        const row=document.createElement('div');row.className='platform-row';
        const mark=document.createElement('span');mark.className=`platform-letter platform-${platform.id}`;mark.textContent=platform.name.slice(0,1);mark.setAttribute('aria-hidden','true');
        const description=document.createElement('div');const title=document.createElement('strong');title.textContent=platform.name;const detail=document.createElement('span');detail.className='small';detail.textContent=platform.description;description.append(title,detail);
        const formats=document.createElement('span');formats.className='platform-formats';formats.textContent=platform.formats;
        row.append(mark,description,formats);el('platform-list').append(row);
      }
      el('settings-version').textContent=`Versione ${about.version} · ${({win32:'Windows',darwin:'macOS',linux:'Linux'})[about.platform]||about.platform} ${about.arch}`;
      el('engine-version').textContent=`Motore yt-dlp ${about.engine} · FFmpeg e FFprobe inclusi`;
      initializing=false;
    }).catch(error=>{status(error.message,true);el('save-settings').disabled=true;});
  }else{initializing=false;status('Apri l’app desktop per salvare le preferenze.');el('save-settings').disabled=true;}
})();
