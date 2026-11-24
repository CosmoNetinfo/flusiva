const {test}=require('node:test');
const assert=require('node:assert/strict');
const {EventEmitter}=require('node:events');
const {updateCapability,createAutoUpdateService}=require('../core/auto-updates.cjs');
class FakeUpdater extends EventEmitter{
  checks=0;downloads=0;installs=0;quitHooks=0;
  async checkForUpdates(){this.checks++;this.emit('checking-for-update');await Promise.resolve();this.emit('update-available',{version:'0.5.0'});return {updateInfo:{version:'0.5.0'}};}
  async downloadUpdate(){this.downloads++;this.emit('download-progress',{percent:50,transferred:500,total:1000,bytesPerSecond:100});await Promise.resolve();this.emit('update-downloaded',{version:'0.5.0'});return ['/cache/installer'];}
  addQuitHandler(){this.quitHooks++;}
  quitAndInstall(){this.installs++;}
}
const capability={mode:'native',reason:null,installOnQuit:true};
test('capability: macOS richiede Developer ID, AppImage e Debian hanno percorsi distinti',()=>{
  assert.equal(updateCapability({isPackaged:true,platform:'darwin',macSigned:false}).mode,'manual');assert.equal(updateCapability({isPackaged:true,platform:'darwin',macSigned:true}).mode,'native');
  assert.equal(updateCapability({isPackaged:true,platform:'win32'}).mode,'native');assert.equal(updateCapability({isPackaged:false,platform:'win32'}).mode,'manual');
  assert.equal(updateCapability({isPackaged:true,platform:'linux',packageType:'AppImage'}).installOnQuit,true);assert.equal(updateCapability({isPackaged:true,platform:'linux',packageType:'deb'}).installOnQuit,false);assert.equal(updateCapability({isPackaged:true,platform:'linux'}).mode,'manual');
});
test('scarica automaticamente e conserva il pacchetto pronto tra controlli ripetuti',async()=>{
  const updater=new FakeUpdater(),states=[];const service=createAutoUpdateService({currentVersion:'0.4.0',capability,updater,getPreferences:async()=>({autoUpdateEnabled:true}),onChange:state=>states.push(state)});
  await Promise.all([service.check(),service.check()]);assert.equal(updater.checks,1);assert.equal(updater.downloads,1);assert.equal(service.snapshot().status,'downloaded');assert.ok(states.some(state=>state.status==='downloading'&&state.progress?.percent===50));assert.equal(updater.autoInstallOnAppQuit,true);
  await service.check();assert.equal(updater.checks,1);assert.equal(service.snapshot().status,'downloaded');
  service.install();assert.equal(updater.installs,1);assert.equal(service.snapshot().status,'installing');
});
test('non interrompe download o coda, abilita installazione alla chiusura solo quando liberi',async()=>{
  const updater=new FakeUpdater();let busy=true;
  const service=createAutoUpdateService({currentVersion:'0.4.0',capability,updater,getPreferences:async()=>({autoUpdateEnabled:true}),isBusy:()=>busy});
  await service.check();assert.equal(service.snapshot().installBlocked,true);assert.equal(updater.autoInstallOnAppQuit,false);assert.throws(()=>service.install(),/Termina i download/);assert.equal(updater.installs,0);
  busy=false;service.refreshActivity();assert.equal(service.snapshot().installBlocked,false);assert.equal(updater.autoInstallOnAppQuit,true);assert.ok(updater.quitHooks>0);service.install();assert.equal(updater.installs,1);
});
test('preferenza manuale e Debian non installano automaticamente alla chiusura',async()=>{
  const updater=new FakeUpdater();let automatic=false;
  const service=createAutoUpdateService({currentVersion:'0.4.0',capability,updater,getPreferences:async()=>({autoUpdateEnabled:automatic})});
  await service.check();assert.equal(service.snapshot().status,'available');assert.equal(updater.downloads,0);await service.download();assert.equal(updater.autoInstallOnAppQuit,false);
  automatic=true;await service.configure();assert.equal(updater.autoInstallOnAppQuit,true);
  const debUpdater=new FakeUpdater(),deb=createAutoUpdateService({currentVersion:'0.4.0',capability:{...capability,installOnQuit:false},updater:debUpdater,getPreferences:async()=>({autoUpdateEnabled:true})});await deb.check();assert.equal(debUpdater.autoInstallOnAppQuit,false);deb.install();assert.equal(debUpdater.installs,1);
});
test('errori di download o verifica non eseguono installer',async()=>{
  const updater=new FakeUpdater();updater.downloadUpdate=async()=>{updater.emit('error',new Error('sha512 mismatch'));throw new Error('sha512 mismatch');};
  const service=createAutoUpdateService({currentVersion:'0.4.0',capability,updater,getPreferences:async()=>({autoUpdateEnabled:true})});
  await assert.rejects(service.check());assert.equal(service.snapshot().status,'error');assert.equal(updater.autoInstallOnAppQuit,false);assert.throws(()=>service.install());assert.equal(updater.installs,0);
});
test('modalità Mac non firmata mantiene il controllo ma non simula installazioni',async()=>{
  const service=createAutoUpdateService({currentVersion:'0.4.0',capability:{mode:'manual',reason:'Firma Apple necessaria',installOnQuit:false},getPreferences:async()=>({}),manualCheck:async()=>({status:'available',latestVersion:'0.5.0'})});
  const result=await service.check();assert.equal(result.status,'available');assert.equal(result.mode,'manual');await assert.rejects(service.download(),/Firma Apple/);assert.throws(()=>service.install());
});
