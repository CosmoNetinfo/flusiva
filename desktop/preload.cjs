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
  onProgress: callback => {
    const listener = (_event, progress) => callback(progress);
    ipcRenderer.on('video:progress', listener);
    return () => ipcRenderer.removeListener('video:progress', listener);
  }
});
