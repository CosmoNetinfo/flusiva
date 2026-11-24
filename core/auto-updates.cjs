const {createUpdateChecker,compareVersions}=require('./updates.cjs');
function updateCapability({isPackaged,platform,packageType,macSigned=false}){
  if(!isPackaged)return {mode:'manual',reason:'L’installazione automatica è disponibile nell’app installata.',installOnQuit:false};
  if(platform==='darwin'&&!macSigned)return {mode:'manual',reason:'Questa build macOS non è firmata con Developer ID. L’aggiornamento automatico richiede una build firmata Apple.',installOnQuit:false};
  if(platform==='linux'&&!['AppImage','deb'].includes(packageType))return {mode:'manual',reason:'Usa il pacchetto AppImage o Debian installato per ricevere aggiornamenti automatici.',installOnQuit:false};
  return {mode:'native',reason:platform==='linux'&&packageType==='deb'?'L’installazione del pacchetto Debian può richiedere l’autorizzazione del sistema.':null,installOnQuit:!(platform==='linux'&&packageType==='deb')};
}
function createAutoUpdateService({currentVersion,capability,updater=null,getPreferences,isBusy=()=>false,onChange=()=>{},manualCheck=createUpdateChecker(currentVersion)}){
  let state={status:'idle',mode:capability.mode,reason:capability.reason,currentVersion,latestVersion:null,progress:null,error:null,installBlocked:false};
  let checking=null,downloading=null,automatic=true;
  const snapshot=()=>({...state,progress:state.progress?{...state.progress}:null});
  function emit(){state.installBlocked=isBusy();onChange(snapshot());}
  function configureQuit(){if(updater){updater.autoInstallOnAppQuit=capability.installOnQuit&&automatic&&!isBusy()&&state.status==='downloaded';if(updater.autoInstallOnAppQuit&&typeof updater.addQuitHandler==='function')updater.addQuitHandler();}}
  function set(patch){state={...state,...patch};configureQuit();emit();}
  const validVersion=info=>{if(!info||compareVersions(info.version,currentVersion)<=0)throw new Error('La versione proposta non è un aggiornamento valido.');return info.version;};
  if(updater){
    updater.autoDownload=false;updater.allowPrerelease=false;updater.allowDowngrade=false;updater.autoRunAppAfterInstall=true;updater.autoInstallOnAppQuit=false;
    updater.on('checking-for-update',()=>{if(!['downloading','downloaded','installing'].includes(state.status))set({status:'checking',error:null});});
    updater.on('update-available',info=>{try{const version=validVersion(info);if(!['downloading','downloaded','installing'].includes(state.status))set({status:'available',latestVersion:version,error:null,progress:null});}catch(error){set({status:'error',error:error.message});}});
    updater.on('update-not-available',()=>{if(!['downloading','downloaded','installing'].includes(state.status))set({status:'current',error:null,progress:null});});
    updater.on('download-progress',progress=>{const finite=value=>Number.isFinite(value)&&value>=0?value:null;set({status:'downloading',progress:{percent:finite(progress.percent),transferred:finite(progress.transferred),total:finite(progress.total),speed:finite(progress.bytesPerSecond)},error:null});});
    updater.on('update-downloaded',info=>{try{set({status:'downloaded',latestVersion:validVersion(info),progress:null,error:null});}catch(error){set({status:'error',error:error.message});}});
    updater.on('error',()=>set({status:'error',progress:null,error:'Aggiornamento non riuscito. Il pacchetto non è stato installato. Controlla Internet e riprova.'}));
  }
  const service={
    snapshot,
    async check(){
      if(['downloading','downloaded','installing'].includes(state.status))return snapshot();
      if(checking)return checking;
      checking=(async()=>{
        const preferences=await getPreferences();automatic=preferences.autoUpdateEnabled!==false;
        set({status:'checking',error:null});
        try{
          if(capability.mode==='manual'){
            const result=await manualCheck();set({status:result.status,latestVersion:result.latestVersion,progress:null,error:null});
          }else{
            await updater.checkForUpdates();
            if(automatic&&state.status==='available')await service.download();
          }
          return snapshot();
        }catch(error){set({status:'error',progress:null,error:capability.mode==='manual'?error.message:'Impossibile verificare o scaricare l’aggiornamento. Riprova più tardi.'});throw new Error(state.error);}
      })().finally(()=>{checking=null;});
      return checking;
    },
    async download(){
      if(capability.mode!=='native')throw new Error(capability.reason);
      if(state.status==='downloaded')return snapshot();
      if(downloading)return downloading;
      if(state.status!=='available')throw new Error('Controlla prima se è disponibile un aggiornamento.');
      downloading=(async()=>{set({status:'downloading',progress:null,error:null});try{await updater.downloadUpdate();if(state.status!=='downloaded')throw new Error('Il download non è stato verificato.');return snapshot();}catch{set({status:'error',progress:null,error:'Download o verifica dell’aggiornamento non riusciti. Nessuna installazione eseguita.'});throw new Error(state.error);}})().finally(()=>{downloading=null;});
      return downloading;
    },
    install(){
      if(capability.mode!=='native'||state.status!=='downloaded')throw new Error('L’aggiornamento non è ancora pronto per l’installazione.');
      if(isBusy())throw new Error('Termina i download e la coda prima di installare l’aggiornamento.');
      set({status:'installing',error:null});updater.quitAndInstall(false,true);return snapshot();
    },
    refreshActivity(){const blocked=isBusy();configureQuit();if(blocked!==state.installBlocked){state.installBlocked=blocked;emit();}},
    async configure(){const preferences=await getPreferences();automatic=preferences.autoUpdateEnabled!==false;configureQuit();return automatic;}
  };
  return service;
}
module.exports={updateCapability,createAutoUpdateService};
