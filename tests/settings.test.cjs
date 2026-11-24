const {test}=require('node:test');
const assert=require('node:assert/strict');
const {mkdtemp,readFile,writeFile,readdir,rm}=require('node:fs/promises');
const {tmpdir}=require('node:os');
const {join}=require('node:path');
const {createSettingsStore,validate}=require('../core/settings.cjs');
const {saveUnique}=require('../core/files.cjs');
test('preferenze persistenti, aggiornamenti serializzati e ripristino',async()=>{
  const root=await mkdtemp(join(tmpdir(),'flusiva-settings-test-'));const file=join(root,'settings.json');
  try{
    const store=createSettingsStore(file,root);assert.equal((await store.get()).askWhereToSave,true);
    await Promise.all([store.update({theme:'dark'}),store.update({audioQuality:'192',askWhereToSave:false})]);
    const restarted=createSettingsStore(file,root);const saved=await restarted.get();assert.equal(saved.theme,'dark');assert.equal(saved.audioQuality,'192');assert.equal(saved.askWhereToSave,false);
    assert.equal((await restarted.reset()).theme,'light');assert.deepEqual(await readdir(root),['settings.json']);
  }finally{await rm(root,{recursive:true,force:true});}
});
test('file corrotto recuperabile e valori arbitrari rifiutati',async()=>{
  for(const input of [{theme:'invalid'},{audioQuality:'--exec'},{askWhereToSave:'false'},{defaultFormat:'wav'},{downloadDirectory:'relative/path'},{unknown:true},[],null])assert.throws(()=>validate(input));
  const root=await mkdtemp(join(tmpdir(),'flusiva-settings-test-'));try{await writeFile(join(root,'settings.json'),'invalid JSON');const store=createSettingsStore(join(root,'settings.json'),root);assert.equal((await store.get()).theme,'light');await store.update({reduceMotion:true});assert.equal(JSON.parse(await readFile(join(root,'settings.json'),'utf8')).reduceMotion,true);}finally{await rm(root,{recursive:true,force:true});}
});
test('salvataggio automatico non sovrascrive file già presenti, anche in parallelo',async()=>{
  const root=await mkdtemp(join(tmpdir(),'flusiva-save-test-'));try{
    const source=join(root,'source');await writeFile(source,'nuovo');await writeFile(join(root,'Titolo.mp3'),'originale');
    const paths=await Promise.all([saveUnique(source,root,'Titolo','mp3'),saveUnique(source,root,'Titolo','mp3')]);
    assert.equal(new Set(paths).size,2);assert.equal(await readFile(join(root,'Titolo.mp3'),'utf8'),'originale');for(const path of paths)assert.equal(await readFile(path,'utf8'),'nuovo');
  }finally{await rm(root,{recursive:true,force:true});}
});
