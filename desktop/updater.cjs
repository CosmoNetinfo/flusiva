const {readFileSync,existsSync}=require('node:fs');
const {join,dirname,resolve}=require('node:path');
const {spawnSync}=require('node:child_process');
const {updateCapability,createAutoUpdateService}=require('../core/auto-updates.cjs');
function isDeveloperIdSigned(execPath){
  const bundle=resolve(dirname(execPath),'../..');
  const details=spawnSync('/usr/bin/codesign',['-dv','--verbose=4',bundle],{encoding:'utf8',timeout:10_000});
  if(details.status!==0||!/^Authority=Developer ID Application:/m.test(details.stderr||''))return false;
  return spawnSync('/usr/bin/codesign',['--verify','--deep','--strict',bundle],{encoding:'utf8',timeout:10_000}).status===0;
}
function createDesktopUpdater({app,getPreferences,isBusy,onChange,disabled=false}){
  let packageType=process.env.APPIMAGE?'AppImage':null;
  if(process.platform==='linux'&&!process.env.APPIMAGE&&existsSync(join(process.resourcesPath,'package-type')))packageType=readFileSync(join(process.resourcesPath,'package-type'),'utf8').trim();
  const capability=updateCapability({isPackaged:app.isPackaged&&!disabled,platform:process.platform,packageType,macSigned:app.isPackaged&&!disabled&&process.platform==='darwin'?isDeveloperIdSigned(process.execPath):false});
  let updater=null;
  if(capability.mode==='native'){
    const native=require('electron-updater');
    // APPIMAGE is authoritative even if a combined Linux build also left a Debian marker.
    updater=process.platform==='linux'?(packageType==='AppImage'?new native.AppImageUpdater():new native.DebUpdater()):native.autoUpdater;
    updater.setFeedURL({provider:'github',owner:'CosmoNetinfo',repo:'flusiva',private:false,releaseType:'release'});
  }
  return createAutoUpdateService({currentVersion:app.getVersion(),capability,updater,getPreferences,isBusy,onChange});
}
module.exports={createDesktopUpdater,isDeveloperIdSigned};
