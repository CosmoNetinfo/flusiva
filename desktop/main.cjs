const { app, BrowserWindow, ipcMain, dialog, shell, clipboard, session } = require('electron');
const { join } = require('node:path');
const { pathToFileURL } = require('node:url');
const { copyFile, unlink } = require('node:fs/promises');
const { randomUUID } = require('node:crypto');
const { createDownloader } = require('../core/downloader.cjs');
const { safeFilename } = require('../core/youtube.cjs');
const {PLATFORMS,downloadOptions:validateDownload}=require('../core/platforms.cjs');
const {createSettingsStore}=require('../core/settings.cjs');
const {saveUnique}=require('../core/files.cjs');
const {createQueue}=require('../core/queue.cjs');
const {createDesktopUpdater}=require('./updater.cjs');
const {resolveQuality}=require('../core/quality.cjs');

let window, active, inspected, lastSaved;
const page = pathToFileURL(join(__dirname,'../public/index.html')).href;
const smokeMode=process.argv.some(arg=>['--smoke-test','--brand-study'].includes(arg));
if(smokeMode)app.setPath('userData',require('node:fs').mkdtempSync(join(app.getPath('temp'),'flusiva-ui-test-')));
const single = app.requestSingleInstanceLock();
if (!single) app.quit();
else {
  app.on('second-instance', () => { if (window) { if (window.isMinimized()) window.restore(); window.focus(); } });
  app.whenReady().then(() => {
    app.setAppUserModelId('info.cosmonet.flusiva');
    const downloader = createDownloader({ toolsDir: app.isPackaged ? join(process.resourcesPath,'tools') : join(__dirname,'../vendor'), tempRoot: app.getPath('temp') });
    const settings=createSettingsStore(join(app.getPath('userData'),'settings.json'),app.getPath('downloads'));
    const chosenDirectories=new Set();
    const send=(channel,value)=>{if(window&&!window.isDestroyed())window.webContents.send(channel,value);};
    let autoUpdates;
    async function saveDownload(info,{format,quality},{signal,onProgress},preferences){
      validateDownload(format,quality,info);
      let result;
      try{
        if(format==='image')result=await downloader.download(info,format,quality,{signal,onProgress});
        const extension=result?.extension||format;
        const destination=preferences.askWhereToSave?await dialog.showSaveDialog(window,{title:'Salva il download',defaultPath:join(preferences.downloadDirectory,safeFilename(info.title,extension)),filters:[{name:extension.toUpperCase(),extensions:[extension]}]}):null;
        if(destination?.canceled||signal.aborted)return {cancelled:true};
        result ||= await downloader.download(info,format,quality,{signal,onProgress});
        onProgress({phase:'saving',percent:null});
        if(signal.aborted)throw new Error('Operazione annullata.');
        let savedPath;
        if(destination){
          const staging=`${destination.filePath}.${randomUUID()}.flusiva-part`;
          try{await copyFile(result.path,staging);if(signal.aborted)throw new Error('Operazione annullata.');await require('node:fs/promises').rename(staging,destination.filePath);}finally{await unlink(staging).catch(()=>{});}
          savedPath=destination.filePath;
        }else savedPath=await saveUnique(result.path,preferences.downloadDirectory,info.title,extension);
        lastSaved=savedPath;
        if(preferences.openFolderAfterDownload)shell.showItemInFolder(savedPath);
        return {path:savedPath};
      }catch(error){
        if(['EACCES','EPERM','ENOENT'].includes(error.code))throw new Error('La cartella di download non è disponibile o non è scrivibile. Scegli un’altra cartella nelle Impostazioni.');
        if(error.code==='ENOSPC')throw new Error('Spazio su disco insufficiente. Libera spazio e riprova.');
        throw error;
      }finally{await result?.cleanup();}
    }
    const queue=createQueue({
      onChange:state=>{send('queue:changed',state);autoUpdates?.refreshActivity();},
      async run(job,{signal,onProgress}){
        const preferences=await settings.get();
        const info=await downloader.inspect(job.url,signal);
        const format=info.photos?.length?'image':info.hasVideo?job.requestedFormat:'mp3';
        const quality=resolveQuality(format,format==='image'?'0':format===job.requestedFormat?job.requestedQuality:preferences.audioQuality,preferences);
        onProgress({phase:'inspect',title:info.title,platformName:info.platformName,format,quality});
        return saveDownload(info,{format,quality},{signal,onProgress},preferences);
      }
    });
    autoUpdates=createDesktopUpdater({app,getPreferences:()=>settings.get(),isBusy:()=>!!active||queue.hasUnfinished(),onChange:state=>send('updates:changed',state),disabled:smokeMode});
    session.defaultSession.setPermissionRequestHandler((_contents,_permission,callback) => callback(false));
    session.defaultSession.setPermissionCheckHandler(() => false);
    const trusted = event => {
      if (!window || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame || event.senderFrame.url.split('#')[0] !== page) throw new Error('Richiesta non autorizzata.');
    };
    const handle = (name, action) => ipcMain.handle(name, async (event, ...args) => {
      trusted(event);
      try {
        if(['video:inspect','video:download','queue:add','queue:start'].includes(name)&&autoUpdates.snapshot().status==='installing')throw new Error('Installazione in corso. Attendi il riavvio di Flusiva.');
        return { ok: true, value: await action(...args) };
      }
      catch (error) { return { ok: false, error: error.message }; }
    });
    handle('video:inspect', async url => {
      if (active||queue.isBusy()) throw new Error('Attendi il download in corso o metti in pausa la coda.');
      active = new AbortController(); inspected = undefined;
      autoUpdates.refreshActivity();
      try { inspected = await downloader.inspect(url, active.signal); return inspected; } finally { active = undefined;autoUpdates.refreshActivity(); }
    });
    handle('video:download', async options => {
      if (active||queue.isBusy()) throw new Error('Attendi il download in corso o metti in pausa la coda.');
      if (!inspected) throw new Error('Analizza prima il link di un video o di una traccia.');
      const { format, quality } = options || {};
      validateDownload(format,quality,inspected);
      const controller = new AbortController(); active = controller;
      autoUpdates.refreshActivity();
      try {
        const preferences=await settings.get();
        const effectiveQuality=resolveQuality(format,quality,preferences);
        return await saveDownload(inspected,{format,quality:effectiveQuality},{signal:controller.signal,onProgress:progress=>send('video:progress',progress)},preferences);
      } finally {active=undefined;autoUpdates.refreshActivity();}
    });
    handle('video:cancel', () => { active?.abort(); });
    handle('queue:get',()=>queue.snapshot());
    handle('queue:add',({urls,format,quality}={})=>queue.add(urls,{format,quality}));
    handle('queue:start',()=>{if(active)throw new Error('Attendi la fine del download singolo.');return queue.start();});
    handle('queue:pause',()=>queue.pause());
    handle('queue:cancel',id=>queue.cancel(id));
    handle('queue:cancel-all',()=>queue.cancelAll());
    handle('queue:retry',id=>queue.retry(id));
    handle('queue:clear',()=>queue.clearFinished());
    handle('queue:reveal',id=>{const item=queue.snapshot().items.find(job=>job.id===id);if(item?.status==='done'&&item.path)shell.showItemInFolder(item.path);});
    handle('updates:get',()=>autoUpdates.snapshot());
    handle('updates:check',()=>autoUpdates.check());
    handle('updates:download',()=>autoUpdates.download());
    handle('updates:install',()=>autoUpdates.install());
    handle('updates:open',()=>shell.openExternal('https://github.com/CosmoNetinfo/flusiva/releases/latest'));
    handle('clipboard:read', () => clipboard.readText().slice(0,102400));
    handle('file:reveal', () => { if (lastSaved) shell.showItemInFolder(lastSaved); });
    handle('settings:get',()=>settings.get());
    handle('settings:update',async patch=>{
      if(!patch)throw new Error('Preferenze non valide.');
      if(Object.hasOwn(patch,'downloadDirectory')&&!chosenDirectories.has(patch.downloadDirectory))throw new Error('Usa il selettore per scegliere la cartella.');
      const updated=await settings.update(patch);await autoUpdates.configure();return updated;
    });
    handle('settings:choose-folder',async()=>{
      const current=await settings.get();
      const choice=await dialog.showOpenDialog(window,{title:'Cartella dei download',defaultPath:current.downloadDirectory,properties:['openDirectory','createDirectory']});
      if(choice.canceled||!choice.filePaths[0])return null;
      chosenDirectories.add(choice.filePaths[0]);
      return choice.filePaths[0];
    });
    handle('settings:reset',async()=>{const reset=await settings.reset();await autoUpdates.configure();return reset;});
    handle('app:releases',()=>shell.openExternal('https://github.com/CosmoNetinfo/flusiva/releases'));
    handle('app:about', async () => {
      let tools={};try{tools=JSON.parse(await require('node:fs/promises').readFile(join(app.isPackaged?join(process.resourcesPath,'tools'):join(__dirname,'../vendor'),'versions.json'),'utf8'));}catch{}
      return { version: app.getVersion(), platform: process.platform, arch: process.arch,platforms:PLATFORMS,engine:tools.ytDlp||'Incluso' };
    });
    function createWindow() {
      const smoke = process.argv.includes('--smoke-test') || process.argv.includes('--brand-study');
      window = new BrowserWindow({ show: !smoke, width: 1160, height: 900, minWidth: 760, minHeight: 680, backgroundColor: '#f7faff', title: 'Flusiva', autoHideMenuBar: true, icon: join(__dirname,'../build/icon.png'), webPreferences: { preload: join(__dirname,'preload.cjs'), contextIsolation: true, nodeIntegration: false, sandbox: true, webSecurity: true } });
      window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
      window.webContents.on('will-navigate', event => event.preventDefault());
      window.on('close', event => {
        if (!active&&!queue.hasUnfinished()) return;
        const choice = dialog.showMessageBoxSync(window, { type: 'question', buttons: ['Continua i download','Esci e annulla'], defaultId: 0, cancelId: 0, message: 'Ci sono download in corso o in attesa.', detail: 'Chiudendo Flusiva annulli il download attuale e la coda di questa sessione.' });
        if (choice === 0) event.preventDefault(); else{active?.abort();queue.cancelAll();}
      });
      window.on('closed', () => { window = undefined; });
      const loaded = window.loadFile(join(__dirname,'../public/index.html'));
      if (smoke) {
        const timer = setTimeout(() => app.exit(1),30_000);
        loaded.then(async () => {
          const result = await window.webContents.executeJavaScript(`(async () => ({ node: typeof require, about: await window.flusiva.about(), buttons: document.querySelectorAll('[data-format]').length, overflow: document.documentElement.scrollWidth > innerWidth }))()`);
          if (result.node !== 'undefined' || !result.about.ok || result.buttons !== 3 || result.overflow) throw new Error(`Smoke test renderer: ${JSON.stringify(result)}`);
          const preferencesCheck=await window.webContents.executeJavaScript(`(async()=>{
            for(let attempt=0;attempt<100&&document.querySelectorAll('.platform-row').length!==${PLATFORMS.length};attempt++)await new Promise(resolve=>setTimeout(resolve,25));
            const original=await window.flusiva.getSettings();
            const saved=await window.flusiva.updateSettings({theme:'dark',defaultFormat:'mp3',audioQuality:'192'});
            const persisted=await window.flusiva.getSettings();
            const invalid=await window.flusiva.updateSettings({theme:'invalid'});
            const deniedPath=await window.flusiva.updateSettings({downloadDirectory:'/unauthorized'});
            const reset=await window.flusiva.resetSettings();
            document.getElementById('nav-settings').click();
            await new Promise(r=>setTimeout(r,100));
            const navigation=!document.getElementById('settings-view').hidden&&document.getElementById('top').hidden;
            const platformCount=document.querySelectorAll('.platform-row').length;
            document.getElementById('nav-downloads').click();
            return {original:original.ok,saved:saved.ok,persisted:persisted.value?.theme==='dark'&&persisted.value?.audioQuality==='192',invalid:!invalid.ok,deniedPath:!deniedPath.ok,reset:reset.value?.theme==='light',navigation,platformCount};
          })()`);
          if(Object.entries(preferencesCheck).some(([key,value])=>key==='platformCount'?value!==PLATFORMS.length:value!==true))throw new Error(`Smoke impostazioni: ${JSON.stringify(preferencesCheck)}`);
          const photosCheck=await window.webContents.executeJavaScript(`(()=>{
            video={platform:'instagram',platformName:'Instagram',hasVideo:false,hasAudio:false,photos:[{width:1080,height:1350},{width:1080,height:1080}]};format='image';updateFormat();
            const valid=!document.querySelector('[data-format="image"]').hidden&&document.querySelector('[data-format="mp4"]').disabled&&document.querySelector('[data-format="mp3"]').disabled&&document.getElementById('quality').options.length===2&&!document.getElementById('quality').disabled&&!document.getElementById('download-button').disabled;
            document.getElementById('quality').value='1';const choice=document.getElementById('quality').value==='1';
            video=null;format='mp4';updateFormat();return valid&&choice;
          })()`);
          if(!photosCheck)throw new Error('Smoke foto Instagram: scelta della foto o formati non coerenti.');
          const queueCheck=await window.webContents.executeJavaScript(`(async()=>{
            document.getElementById('queue-composer').open=true;
            document.getElementById('queue-links').value='https://instagram.com/share/BAWVJE-rtk\\nhttps://facebook.com/reel/123456/';
            document.getElementById('queue-form').requestSubmit();
            let snapshot;
            for(let attempt=0;attempt<100;attempt++){snapshot=(await window.flusiva.getQueue()).value;if(snapshot.items.length===2&&document.querySelectorAll('.queue-row').length===2)break;await new Promise(resolve=>setTimeout(resolve,20));}
            const added=snapshot.items.length===2&&snapshot.paused&&document.querySelectorAll('.queue-row').length===2;
            const names=document.getElementById('queue-list').textContent;
            const platforms=names.includes('Instagram')&&names.includes('Facebook')&&!names.includes('undefined');
            const invalid=await window.flusiva.addToQueue({urls:['https://evil.test/video'],format:'mp4',quality:'best'});
            await window.flusiva.cancelQueueItem(snapshot.items[0].id);
            await window.flusiva.retryQueueItem(snapshot.items[0].id);
            const retried=(await window.flusiva.getQueue()).value.items[0].status==='waiting';
            await window.flusiva.cancelQueue();await window.flusiva.clearQueue();
            const cleared=(await window.flusiva.getQueue()).value.items.length===0;
            document.getElementById('queue-composer').open=false;
            document.getElementById('queue-announcement').textContent='';
            const controls=['check-updates','download-update','install-update','update-download-progress','metric-speed'].every(id=>!!document.getElementById(id))&&document.getElementById('settings-updates').closest('form')?.id==='settings-form';
            const updateState=await window.flusiva.getUpdateState();
            const refusedInstall=await window.flusiva.installUpdate();
            const updater=updateState.ok&&updateState.value.mode==='manual'&&!refusedInstall.ok;
            window.scrollTo(0,0);
            return {added,invalid:!invalid.ok,retried,cleared,controls,updater,platforms};
          })()`);
          if(Object.values(queueCheck).some(value=>value!==true))throw new Error(`Smoke coda: ${JSON.stringify(queueCheck)}`);
          const { execFileSync } = require('node:child_process');
          const tools = app.isPackaged ? join(process.resourcesPath,'tools') : join(__dirname,'../vendor');
          for (const binary of ['yt-dlp','ffmpeg','ffprobe']) execFileSync(join(tools,binary+(process.platform === 'win32' ? '.exe' : '')),[binary === 'yt-dlp' ? '--version' : '-version'],{windowsHide:true,timeout:15_000,stdio:'pipe'});
          const runtime = execFileSync(process.execPath,['--version'],{env:{...process.env,ELECTRON_RUN_AS_NODE:'1'},windowsHide:true,encoding:'utf8',timeout:15_000});
          if (!/^v\d+\./.test(runtime)) throw new Error('Runtime Node non disponibile.');
          if (process.env.CI !== 'true') {
            const shots = app.isPackaged ? join(app.getPath('temp'),'flusiva-smoke') : join(__dirname,'../test-results');
            await require('node:fs/promises').mkdir(shots,{recursive:true});
            await new Promise(resolve => setTimeout(resolve,800));
            await require('node:fs/promises').writeFile(join(shots,'desktop.png'),(await window.webContents.capturePage()).toPNG());
            await window.webContents.executeJavaScript("document.getElementById('nav-settings').click()");
            await new Promise(resolve=>setTimeout(resolve,200));
            await require('node:fs/promises').writeFile(join(shots,'settings-light.png'),(await window.webContents.capturePage()).toPNG());
            await window.webContents.executeJavaScript("document.documentElement.dataset.theme='dark'");
            await require('node:fs/promises').writeFile(join(shots,'settings-dark.png'),(await window.webContents.capturePage()).toPNG());
            await window.webContents.executeJavaScript("document.documentElement.dataset.theme='light';document.getElementById('nav-downloads').click()");
            await window.webContents.executeJavaScript("window.flusiva.addToQueue({urls:['https://www.youtube.com/watch?v=aqz-KE-bpKQ','https://soundcloud.com/ethmusic/lostin-powers-she-so-heavy'],format:'mp4',quality:'best'})");
            await window.webContents.executeJavaScript("document.getElementById('queue-composer').open=true;document.getElementById('nav-queue').click()");
            await new Promise(resolve=>setTimeout(resolve,200));
            await require('node:fs/promises').writeFile(join(shots,'queue.png'),(await window.webContents.capturePage()).toPNG());
            await window.webContents.executeJavaScript("(async()=>{await window.flusiva.cancelQueue();await window.flusiva.clearQueue();document.getElementById('queue-composer').open=false;document.getElementById('nav-settings').click();document.getElementById('settings-updates').scrollIntoView({block:'center'});})()");
            await new Promise(resolve=>setTimeout(resolve,200));
            await require('node:fs/promises').writeFile(join(shots,'updates.png'),(await window.webContents.capturePage()).toPNG());
            await window.webContents.executeJavaScript("document.getElementById('nav-downloads').click()");
          }
          if (process.argv.includes('--brand-study') && !app.isPackaged && process.env.CI !== 'true') {
            await require('node:fs/promises').mkdir(join(__dirname,'../docs/images'),{recursive:true});
            await require('node:fs/promises').copyFile(join(__dirname,'../test-results/desktop.png'),join(__dirname,'../docs/images/flusiva-desktop.png'));
            for(const name of ['settings-light','settings-dark','queue','updates'])await require('node:fs/promises').copyFile(join(__dirname,`../test-results/${name}.png`),join(__dirname,`../docs/images/${name}.png`));
            window.setContentSize(1440,1000);
            await window.loadFile(join(__dirname,'../docs/brand/study.html'));
            await require('node:fs/promises').writeFile(join(__dirname,'../docs/brand/logo-study.png'),(await window.webContents.capturePage()).toPNG());
          }
          console.log(`Flusiva smoke PASS: bridge isolato, interfaccia, tre binari, runtime ${runtime.trim()}`);
          clearTimeout(timer); app.exit(0);
        }).catch(error => { console.error(error); app.exit(1); });
      }
    }
    createWindow();
    if(!smokeMode){
      const poll=async()=>{try{if(await autoUpdates.configure())await autoUpdates.check();}catch{ /* The state event reports network and provider errors. */ }};
      const initialCheck=setTimeout(poll,10_000);initialCheck.unref();
      const periodicCheck=setInterval(poll,6*60*60*1000);periodicCheck.unref();
      app.on('before-quit',()=>{clearTimeout(initialCheck);clearInterval(periodicCheck);});
    }
    app.on('activate', () => { if (!BrowserWindow.getAllWindows().length) createWindow(); });
  });
  app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
}
