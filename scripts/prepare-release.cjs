const {readdir,mkdir,copyFile,writeFile,stat}=require('node:fs/promises');
const {createReadStream}=require('node:fs');
const {createHash}=require('node:crypto');
const {join,resolve,basename}=require('node:path');
async function hashFile(file,algorithm,encoding){const hash=createHash(algorithm);for await(const chunk of createReadStream(file))hash.update(chunk);return hash.digest(encoding);}
async function prepareRelease({sourceDirectory,outputDirectory,version,releaseDate=new Date().toISOString()}){
  if(!/^\d+\.\d+\.\d+$/.test(version))throw new Error('Versione di release non valida.');
  const source=resolve(sourceDirectory),output=resolve(outputDirectory);
  if(output===source||output.startsWith(source+require('node:path').sep))throw new Error('La destinazione deve essere esterna agli artifact.');
  const names=[`Flusiva-${version}-win-x64.exe`,`Flusiva-${version}-linux-x86_64.AppImage`,`Flusiva-${version}-linux-amd64.deb`,`Flusiva-${version}-mac-x64.zip`,`Flusiva-${version}-mac-arm64.zip`,`Flusiva-${version}-mac-x64.dmg`,`Flusiva-${version}-mac-arm64.dmg`];
  const allowed=new Set([...names,...names.map(name=>name+'.blockmap')]);const files=new Map();
  async function walk(directory){for(const entry of await readdir(directory,{withFileTypes:true})){if(entry.isSymbolicLink())throw new Error('Un artifact contiene un link simbolico.');const file=join(directory,entry.name);if(entry.isDirectory())await walk(file);else if(allowed.has(entry.name)){const size=(await stat(file)).size;if(!size)throw new Error('Artifact vuoto.');const sha512=await hashFile(file,'sha512','base64');const previous=files.get(entry.name);if(previous&&previous.sha512!==sha512)throw new Error('Artifact diversi con lo stesso nome.');files.set(entry.name,{file,url:basename(file),size,sha512});}}}
  await walk(source);for(const name of names)if(!files.has(name))throw new Error(`Artifact mancante: ${name}`);
  await mkdir(output,{recursive:true});if((await readdir(output)).length)throw new Error('La cartella di pubblicazione deve essere vuota.');
  for(const [name,file]of files)await copyFile(file.file,join(output,name));
  const manifests={
    'latest.yml':[names[0]],
    'latest-linux.yml':[names[1],names[2]],
    'latest-mac.yml':[names[3],names[4],names[5],names[6]]
  };
  for(const [name,artifacts]of Object.entries(manifests)){
    const entries=artifacts.map(name=>{const {url,size,sha512}=files.get(name);return {url,size,sha512};});
    // JSON is a YAML subset and is accepted by electron-updater's YAML reader.
    await writeFile(join(output,name),JSON.stringify({version,files:entries,path:entries[0].url,sha512:entries[0].sha512,releaseDate},null,2)+'\n');
  }
  const sums=[];for(const name of (await readdir(output)).sort())sums.push(`${await hashFile(join(output,name),'sha256','hex')}  ${name}`);
  await writeFile(join(output,'SHA256SUMS.txt'),sums.join('\n')+'\n');
  return {version,files:[...files.keys()],manifests:Object.keys(manifests)};
}
module.exports={prepareRelease,hashFile};
if(require.main===module)prepareRelease({sourceDirectory:process.argv[2]||'build-artifacts',outputDirectory:process.argv[3]||'installers',version:require('../package.json').version}).then(result=>console.log(JSON.stringify(result))).catch(error=>{console.error(error.message);process.exitCode=1;});
