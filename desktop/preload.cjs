const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('flusiva', {
  inspect: url => ipcRenderer.invoke('video:inspect', url),
  download: options => ipcRenderer.invoke('video:download', options),
  cancel: () => ipcRenderer.invoke('video:cancel'),
  reveal: () => ipcRenderer.invoke('file:reveal'),
  paste: () => ipcRenderer.invoke('clipboard:read'),
  about: () => ipcRenderer.invoke('app:about'),
  onProgress: callback => {
    const listener = (_event, progress) => callback(progress);
    ipcRenderer.on('video:progress', listener);
    return () => ipcRenderer.removeListener('video:progress', listener);
  }
});
