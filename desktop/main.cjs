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
    session.defaultSession.setPermissionRequestHandler((_contents,_permission,callback) => callback(false));
    session.defaultSession.setPermissionCheckHandler(() => false);
    const trusted = event => {
      if (!window || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame || event.senderFrame.url.split('#')[0] !== page) throw new Error('Richiesta non autorizzata.');
    };
    const handle = (name, action) => ipcMain.handle(name, async (event, ...args) => {
      trusted(event);
      try { return { ok: true, value: await action(...args) }; }
      catch (error) { return { ok: false, error: error.message }; }
    });
    handle('video:inspect', async url => {
      if (active) throw new Error('Attendi il completamento dell’operazione in corso.');
      active = new AbortController(); inspected = undefined;
      try { inspected = await downloader.inspect(url, active.signal); return inspected; } finally { active = undefined; }
    });
    handle('video:download', async options => {
      if (active) throw new Error('È già in corso un’operazione.');
      if (!inspected) throw new Error('Analizza prima il link di un video o di una traccia.');
      const { format, quality } = options || {};
      validateDownload(format,quality,inspected);
      const controller = new AbortController(); active = controller;
      let result;
      try {
        const preferences=await settings.get();
        const destination=preferences.askWhereToSave?await dialog.showSaveDialog(window,{title:'Salva il download',defaultPath:join(preferences.downloadDirectory,safeFilename(inspected.title,format)),filters:[{name:format.toUpperCase(),extensions:[format]}]}):null;
        if (destination?.canceled || controller.signal.aborted) return { cancelled: true };
        result = await downloader.download(inspected,format,quality,{ signal: controller.signal, onProgress: progress => { if (!window?.isDestroyed()) window.webContents.send('video:progress',progress); } });
        // Keep partial copies away from the chosen filename; preserve an existing file on errors.
        if(destination) {
          const staging = `${destination.filePath}.${randomUUID()}.flusiva-part`;
          try {
            await copyFile(result.path, staging);
            if (controller.signal.aborted) throw new Error('Operazione annullata.');
            await require('node:fs/promises').rename(staging,destination.filePath);
          } finally { await unlink(staging).catch(() => {}); }
          lastSaved=destination.filePath;
        } else lastSaved=await saveUnique(result.path,preferences.downloadDirectory,inspected.title,format);
        if(preferences.openFolderAfterDownload)shell.showItemInFolder(lastSaved);
        return { path: lastSaved };
      } catch(error) {
        if(['EACCES','EPERM','ENOENT'].includes(error.code))throw new Error('La cartella di download non è disponibile o non è scrivibile. Scegli un’altra cartella nelle Impostazioni.');
        if(error.code==='ENOSPC')throw new Error('Spazio su disco insufficiente. Libera spazio e riprova.');
        throw error;
      } finally {try{await result?.cleanup();}finally{active = undefined;} }
    });
    handle('video:cancel', () => { active?.abort(); });
    handle('clipboard:read', () => clipboard.readText().slice(0,2048));
    handle('file:reveal', () => { if (lastSaved) shell.showItemInFolder(lastSaved); });
    handle('settings:get',()=>settings.get());
    handle('settings:update',patch=>{
      if(!patch)throw new Error('Preferenze non valide.');
      if(Object.hasOwn(patch,'downloadDirectory')&&!chosenDirectories.has(patch.downloadDirectory))throw new Error('Usa il selettore per scegliere la cartella.');
      return settings.update(patch);
    });
    handle('settings:choose-folder',async()=>{
      const current=await settings.get();
      const choice=await dialog.showOpenDialog(window,{title:'Cartella dei download',defaultPath:current.downloadDirectory,properties:['openDirectory','createDirectory']});
      if(choice.canceled||!choice.filePaths[0])return null;
      chosenDirectories.add(choice.filePaths[0]);
      return choice.filePaths[0];
    });
    handle('settings:reset',()=>settings.reset());
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
        if (!active) return;
        const choice = dialog.showMessageBoxSync(window, { type: 'question', buttons: ['Continua il download','Esci e annulla'], defaultId: 0, cancelId: 0, message: 'Un’operazione è ancora in corso.', detail: 'Chiudendo Flusiva il download verrà annullato.' });
        if (choice === 0) event.preventDefault(); else active.abort();
      });
      window.on('closed', () => { window = undefined; });
      const loaded = window.loadFile(join(__dirname,'../public/index.html'));
      if (smoke) {
        const timer = setTimeout(() => app.exit(1),30_000);
        loaded.then(async () => {
          const result = await window.webContents.executeJavaScript(`(async () => ({ node: typeof require, about: await window.flusiva.about(), buttons: document.querySelectorAll('[data-format]').length, overflow: document.documentElement.scrollWidth > innerWidth }))()`);
          if (result.node !== 'undefined' || !result.about.ok || result.buttons !== 2 || result.overflow) throw new Error(`Smoke test renderer: ${JSON.stringify(result)}`);
          const preferencesCheck=await window.webContents.executeJavaScript(`(async()=>{
            for(let attempt=0;attempt<100&&document.querySelectorAll('.platform-row').length!==5;attempt++)await new Promise(resolve=>setTimeout(resolve,25));
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
          if(Object.entries(preferencesCheck).some(([key,value])=>key==='platformCount'?value!==5:value!==true))throw new Error(`Smoke impostazioni: ${JSON.stringify(preferencesCheck)}`);
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
          }
          if (process.argv.includes('--brand-study') && !app.isPackaged && process.env.CI !== 'true') {
            await require('node:fs/promises').mkdir(join(__dirname,'../docs/images'),{recursive:true});
            await require('node:fs/promises').copyFile(join(__dirname,'../test-results/desktop.png'),join(__dirname,'../docs/images/flusiva-desktop.png'));
            for(const name of ['settings-light','settings-dark'])await require('node:fs/promises').copyFile(join(__dirname,`../test-results/${name}.png`),join(__dirname,`../docs/images/${name}.png`));
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
    app.on('activate', () => { if (!BrowserWindow.getAllWindows().length) createWindow(); });
  });
  app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
}
