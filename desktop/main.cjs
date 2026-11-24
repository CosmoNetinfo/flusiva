const { app, BrowserWindow, ipcMain, dialog, shell, clipboard, session } = require('electron');
const { join } = require('node:path');
const { pathToFileURL } = require('node:url');
const { copyFile, unlink } = require('node:fs/promises');
const { randomUUID } = require('node:crypto');
const { createDownloader } = require('../core/downloader.cjs');
const { safeFilename, downloadOptions } = require('../core/youtube.cjs');

let window, active, inspected, lastSaved;
const page = pathToFileURL(join(__dirname,'../public/index.html')).href;
const single = app.requestSingleInstanceLock();
if (!single) app.quit();
else {
  app.on('second-instance', () => { if (window) { if (window.isMinimized()) window.restore(); window.focus(); } });
  app.whenReady().then(() => {
    app.setAppUserModelId('info.cosmonet.flusiva');
    const downloader = createDownloader({ toolsDir: app.isPackaged ? join(process.resourcesPath,'tools') : join(__dirname,'../vendor'), tempRoot: app.getPath('temp') });
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
      if (!inspected) throw new Error('Analizza prima un link di YouTube.');
      const { format, quality } = options || {};
      downloadOptions(format, quality);
      const controller = new AbortController(); active = controller;
      let result;
      try {
        const destination = await dialog.showSaveDialog(window, { title: 'Salva il download', defaultPath: join(app.getPath('downloads'),safeFilename(inspected.title,format)), filters: [{ name: format.toUpperCase(), extensions: [format] }] });
        if (destination.canceled || controller.signal.aborted) return { cancelled: true };
        result = await downloader.download(inspected,format,quality,{ signal: controller.signal, onProgress: progress => { if (!window?.isDestroyed()) window.webContents.send('video:progress',progress); } });
        // Keep partial copies away from the chosen filename; preserve an existing file on errors.
        const staging = `${destination.filePath}.${randomUUID()}.flusiva-part`;
        try {
          await copyFile(result.path, staging);
          if (controller.signal.aborted) throw new Error('Operazione annullata.');
          await require('node:fs/promises').rename(staging,destination.filePath);
        } finally { await unlink(staging).catch(() => {}); }
        lastSaved = destination.filePath;
        return { path: lastSaved };
      } finally { await result?.cleanup(); active = undefined; }
    });
    handle('video:cancel', () => { active?.abort(); });
    handle('clipboard:read', () => clipboard.readText().slice(0,2048));
    handle('file:reveal', () => { if (lastSaved) shell.showItemInFolder(lastSaved); });
    handle('app:about', () => ({ version: app.getVersion(), platform: process.platform, arch: process.arch }));
    function createWindow() {
      const smoke = process.argv.includes('--smoke-test');
      window = new BrowserWindow({ show: !smoke, width: 1120, height: 830, minWidth: 760, minHeight: 680, backgroundColor: '#f8f8f8', title: 'Flusiva', autoHideMenuBar: true, icon: join(__dirname,'../build/icon.png'), webPreferences: { preload: join(__dirname,'preload.cjs'), contextIsolation: true, nodeIntegration: false, sandbox: true, webSecurity: true } });
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
          const { execFileSync } = require('node:child_process');
          const tools = app.isPackaged ? join(process.resourcesPath,'tools') : join(__dirname,'../vendor');
          for (const binary of ['yt-dlp','ffmpeg','ffprobe']) execFileSync(join(tools,binary+(process.platform === 'win32' ? '.exe' : '')),[binary === 'yt-dlp' ? '--version' : '-version'],{windowsHide:true,timeout:15_000,stdio:'pipe'});
          const runtime = execFileSync(process.execPath,['--version'],{env:{...process.env,ELECTRON_RUN_AS_NODE:'1'},windowsHide:true,encoding:'utf8',timeout:15_000});
          if (!/^v\d+\./.test(runtime)) throw new Error('Runtime Node non disponibile.');
          const shots = app.isPackaged ? join(app.getPath('temp'),'flusiva-smoke') : join(__dirname,'../test-results');
          await require('node:fs/promises').mkdir(shots,{recursive:true});
          await new Promise(resolve => setTimeout(resolve,800));
          await require('node:fs/promises').writeFile(join(shots,'desktop.png'),(await window.webContents.capturePage()).toPNG());
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
