const {test}=require('node:test');
const assert=require('node:assert/strict');
const {mkdtemp,mkdir,writeFile,readFile,rm}=require('node:fs/promises');
const {join}=require('node:path');
const {tmpdir}=require('node:os');
const {prepareRelease,hashFile}=require('../scripts/prepare-release.cjs');
async function fixture(root){
  const source=join(root,'artifacts');await mkdir(source);
  for(const [dir,suffixes]of [['windows',['win-x64.exe']],['linux',['linux-x86_64.AppImage','linux-amd64.deb']],['intel',['mac-x64.zip','mac-x64.dmg']],['arm',['mac-arm64.zip','mac-arm64.dmg']],['android',['android.apk']]]){
    await mkdir(join(source,dir));for(const suffix of suffixes)await writeFile(join(source,dir,`Flusiva-0.4.0-${suffix}`),`fixture ${suffix}`);
    if(['intel','arm'].includes(dir))await writeFile(join(source,dir,'latest-mac.yml'),'duplicate builder metadata');
  }return source;
}
test('feed unico Mac con entrambe le architetture, SHA-512 e dimensioni degli artifact effettivi',async()=>{
  const root=await mkdtemp(join(tmpdir(),'flusiva-release-test-'));try{
    const source=await fixture(root),output=join(root,'installers');await prepareRelease({sourceDirectory:source,outputDirectory:output,version:'0.4.0'});
    const mac=JSON.parse(await readFile(join(output,'latest-mac.yml'),'utf8'));assert.equal(mac.version,'0.4.0');assert.equal(mac.files.length,4);assert.ok(mac.files.some(file=>file.url.endsWith('mac-arm64.zip')));assert.ok(mac.files.some(file=>file.url.endsWith('mac-x64.zip')));
    const {MacUpdater}=require('electron-updater');
    const nativeFiles=mac.files.map(info=>({info,url:new URL(info.url,'https://github.com/CosmoNetinfo/flusiva/releases/download/v0.4.0/')}));
    assert.ok(MacUpdater.filterFilesForArch(nativeFiles,true).every(file=>file.info.url.includes('arm64')));
    assert.ok(MacUpdater.filterFilesForArch(nativeFiles,false).every(file=>file.info.url.includes('x64')));
    for(const file of mac.files){assert.equal(file.sha512,await hashFile(join(output,file.url),'sha512','base64'));assert.equal(file.size,Buffer.byteLength(`fixture ${file.url.replace('Flusiva-0.4.0-','')}`));}
    const linux=JSON.parse(await readFile(join(output,'latest-linux.yml'),'utf8'));assert.ok(linux.files.some(file=>file.url.endsWith('.deb')));assert.ok(linux.files.some(file=>file.url.endsWith('.AppImage')));
    assert.match(await readFile(join(output,'SHA256SUMS.txt'),'utf8'),/latest-mac.yml/);
    await assert.rejects(prepareRelease({sourceDirectory:source,outputDirectory:output,version:'0.4.0'}),/vuota/);
  }finally{await rm(root,{recursive:true,force:true});}
});
test('rifiuta release incomplete, versioni inattese e collisioni di artifact',async()=>{
  const root=await mkdtemp(join(tmpdir(),'flusiva-release-test-'));try{
    const source=await fixture(root),output=join(root,'installers');await assert.rejects(prepareRelease({sourceDirectory:source,outputDirectory:output,version:'../invalid'}));
    await rm(join(source,'arm','Flusiva-0.4.0-mac-arm64.zip'));await assert.rejects(prepareRelease({sourceDirectory:source,outputDirectory:output,version:'0.4.0'}),/mancante/);
    await writeFile(join(source,'arm','Flusiva-0.4.0-mac-arm64.zip'),'restored');await writeFile(join(source,'intel','Flusiva-0.4.0-win-x64.exe'),'collision');await assert.rejects(prepareRelease({sourceDirectory:source,outputDirectory:output,version:'0.4.0'}),/stesso nome/);
  }finally{await rm(root,{recursive:true,force:true});}
});
