const { readFile,writeFile,mkdir,rename,unlink }=require('node:fs/promises');
const { dirname,isAbsolute }=require('node:path');
const { randomUUID }=require('node:crypto');
const DEFAULTS=Object.freeze({downloadDirectory:'',askWhereToSave:true,defaultFormat:'mp4',videoQuality:'best',audioQuality:'320',theme:'light',openFolderAfterDownload:false,reduceMotion:false,checkUpdatesOnStartup:false,maximumQuality:true});
function validate(input,base=DEFAULTS) {
  if(!input||typeof input!=='object'||Array.isArray(input))throw new Error('Impostazioni non valide.');
  for(const key of Object.keys(input))if(!Object.hasOwn(DEFAULTS,key))throw new Error('Impostazione sconosciuta.');
  const next={...base,...input};
  if(typeof next.downloadDirectory!=='string'||next.downloadDirectory.length>4096||next.downloadDirectory.includes('\0')||(next.downloadDirectory&&!isAbsolute(next.downloadDirectory)))throw new Error('Cartella di download non valida.');
  for(const key of ['askWhereToSave','openFolderAfterDownload','reduceMotion','checkUpdatesOnStartup','maximumQuality'])if(typeof next[key]!=='boolean')throw new Error('Preferenza non valida.');
  for(const [key,choices] of [['defaultFormat',['mp3','mp4']],['videoQuality',['best','4320','2160','1440','1080','720','480','360','240','144']],['audioQuality',['320','256','192','128']],['theme',['light','dark','system']]])if(!choices.includes(next[key]))throw new Error('Valore di preferenza non valido.');
  return next;
}
function createSettingsStore(file,defaultDirectory) {
  const defaults={...DEFAULTS,downloadDirectory:defaultDirectory};
  let queue=Promise.resolve();
  const read=async()=>{
    try{const saved=JSON.parse(await readFile(file,'utf8'));return validate(saved,defaults);}
    catch(error){if(error.code==='ENOENT'||error instanceof SyntaxError||!error.code)return {...defaults};throw new Error('Impossibile leggere le impostazioni locali.');}
  };
  const update=patch=>{
    const task=queue.then(async()=>{
      const next=validate(patch,await read());const temp=`${file}.${randomUUID()}.tmp`;
      await mkdir(dirname(file),{recursive:true});
      try{await writeFile(temp,JSON.stringify(next,null,2),{encoding:'utf8',mode:0o600});await rename(temp,file);}finally{await unlink(temp).catch(()=>{});}
      return next;
    });
    queue=task.catch(()=>{});return task;
  };
  return {get:()=>queue.then(read),update,reset:()=>update(defaults)};
}
module.exports={DEFAULTS,validate,createSettingsStore};
