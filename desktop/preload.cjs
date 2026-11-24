const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('flusiva', {
  inspect: url => ipcRenderer.invoke('video:inspect', url),
  download: options => ipcRenderer.invoke('video:download', options),
  cancel: () => ipcRenderer.invoke('video:cancel'),
  reveal: () => ipcRenderer.invoke('file:reveal'),
  paste: () => ipcRenderer.invoke('clipboard:read'),
  about: () => ipcRenderer.invoke('app:about'),
  getSettings:()=>ipcRenderer.invoke('settings:get'),
  updateSettings:patch=>ipcRenderer.invoke('settings:update',patch),
  chooseFolder:()=>ipcRenderer.invoke('settings:choose-folder'),
  resetSettings:()=>ipcRenderer.invoke('settings:reset'),
  releases:()=>ipcRenderer.invoke('app:releases'),
  getQueue:()=>ipcRenderer.invoke('queue:get'),
  addToQueue:options=>ipcRenderer.invoke('queue:add',options),
  startQueue:()=>ipcRenderer.invoke('queue:start'),
  pauseQueue:()=>ipcRenderer.invoke('queue:pause'),
  cancelQueueItem:id=>ipcRenderer.invoke('queue:cancel',id),
  cancelQueue:()=>ipcRenderer.invoke('queue:cancel-all'),
  retryQueueItem:id=>ipcRenderer.invoke('queue:retry',id),
  clearQueue:()=>ipcRenderer.invoke('queue:clear'),
  revealQueueItem:id=>ipcRenderer.invoke('queue:reveal',id),
  checkUpdates:()=>ipcRenderer.invoke('updates:check'),
  getUpdateState:()=>ipcRenderer.invoke('updates:get'),
  downloadUpdate:()=>ipcRenderer.invoke('updates:download'),
  installUpdate:()=>ipcRenderer.invoke('updates:install'),
  openUpdate:()=>ipcRenderer.invoke('updates:open'),
  onUpdateChanged:callback=>{const listener=(_event,state)=>callback(state);ipcRenderer.on('updates:changed',listener);return ()=>ipcRenderer.removeListener('updates:changed',listener);},
  onQueueChanged:callback=>{
    const listener=(_event,state)=>callback(state);ipcRenderer.on('queue:changed',listener);
    return ()=>ipcRenderer.removeListener('queue:changed',listener);
  },
  onProgress: callback => {
    const listener = (_event, progress) => callback(progress);
    ipcRenderer.on('video:progress', listener);
    return () => ipcRenderer.removeListener('video:progress', listener);
  }
});
