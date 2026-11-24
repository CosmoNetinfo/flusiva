const {readdir,readFile}=require('node:fs/promises');
const {join,dirname}=require('node:path');
const asar=require('@electron/asar');
async function verify(){
  const configs=[];
  async function walk(directory,depth=0){if(depth>7)return;for(const entry of await readdir(directory,{withFileTypes:true})){if(entry.isSymbolicLink())continue;const path=join(directory,entry.name);if(entry.isDirectory())await walk(path,depth+1);else if(entry.name==='app-update.yml')configs.push(path);}}
  await walk('release');if(!configs.length)throw new Error('Configurazione updater non inclusa nei pacchetti.');
  for(const file of configs){
    const text=await readFile(file,'utf8');
    for(const [field,value]of [['provider','github'],['owner','CosmoNetinfo'],['repo','flusiva']])if(!new RegExp(`^${field}:\\s*${value}\\s*$`,'m').test(text))throw new Error(`Feed inatteso: ${file}`);
    const dependency=JSON.parse(asar.extractFile(join(dirname(file),'app.asar'),join('node_modules','electron-updater','package.json')).toString());
    if(dependency.version!=='6.8.11')throw new Error('Versione updater non inclusa correttamente.');
    console.log(`Updater configurato e incluso: ${file}`);
  }
}
verify().catch(error=>{console.error(error.message);process.exitCode=1;});
