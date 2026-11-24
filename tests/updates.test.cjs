const {test}=require('node:test');
const assert=require('node:assert/strict');
const {checkLatest,compareVersions,createUpdateChecker}=require('../core/updates.cjs');
const release={tag_name:'v0.3.0',html_url:'https://github.com/CosmoNetinfo/flusiva/releases/tag/v0.3.0',draft:false,prerelease:false,published_at:'2026-11-24T12:00:00Z'};
const response=(data=release,status=200)=>({ok:status>=200&&status<300,status,json:async()=>data});
test('confronta numericamente le versioni e accetta soltanto release stabili ufficiali',async()=>{
  assert.equal(compareVersions('0.10.0','0.9.9'),1);assert.equal(compareVersions('1.0.0','0.99.0'),1);assert.equal(compareVersions('v0.3.0','0.3.0'),0);
  assert.throws(()=>compareVersions('0.3.0-beta.1','0.2.0'));
  const result=await checkLatest('0.2.0',{fetcher:async(url,options)=>{assert.equal(url,'https://api.github.com/repos/CosmoNetinfo/flusiva/releases/latest');assert.equal(options.redirect,'error');assert.equal(options.headers.Authorization,undefined);return response();}});
  assert.equal(result.status,'available');assert.equal(result.latestVersion,'0.3.0');assert.equal((await checkLatest('0.3.0',{fetcher:async()=>response()})).status,'current');
  for(const data of [{...release,html_url:'https://evil.test/download'},{...release,prerelease:true},{...release,draft:true},{...release,tag_name:'../unsafe'}])await assert.rejects(checkLatest('0.2.0',{fetcher:async()=>response(data)}));
});
test('gestisce assenza di release, limite richieste, rete e JSON non valido',async()=>{
  assert.equal((await checkLatest('0.3.0',{fetcher:async()=>response(null,404)})).status,'unpublished');
  for(const status of [403,429,500])await assert.rejects(checkLatest('0.3.0',{fetcher:async()=>response(null,status)}));
  await assert.rejects(checkLatest('0.3.0',{fetcher:async()=>{throw new Error('offline');}}),/contattare GitHub/);
  await assert.rejects(checkLatest('0.3.0',{fetcher:async()=>({ok:true,status:200,json:async()=>{throw new Error('invalid');}})}),/risposta non valida/);
});
test('condivide richieste simultanee e riusa brevemente l’esito',async()=>{
  let calls=0;const check=createUpdateChecker('0.2.0',{fetcher:async()=>{calls++;return response();}});
  const results=await Promise.all([check(),check(),check()]);assert.equal(calls,1);assert.ok(results.every(item=>item.status==='available'));await check();assert.equal(calls,1);
});
