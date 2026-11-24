const { test } = require('node:test');
const assert = require('node:assert/strict');
const { mkdtemp, writeFile, readdir, rm } = require('node:fs/promises');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { createDownloader, runTool } = require('../core/downloader.cjs');
test('processo senza shell: righe spezzate, uscita e argomenti letterali',async () => {
  const lines = [];
  const text = await runTool(process.execPath,['-e','process.stdout.write(process.argv[1]+"\\n"); process.stdout.write("fine")','a & b $(x)'],{onLine:line=>lines.push(line)});
  assert.equal(text,'a & b $(x)\nfine'); assert.deepEqual(lines,['a & b $(x)','fine']);
});
test('timeout e annullamento interrompono il processo',async () => {
  await assert.rejects(runTool(process.execPath,['-e','setInterval(()=>{},1000)'],{timeout:150}),/timeout/);
  const controller = new AbortController();
  const pending = runTool(process.execPath,['-e','setInterval(()=>{},1000)'],{signal:controller.signal});
  setTimeout(()=>controller.abort(),150); await assert.rejects(pending,/cancelled/);
});
test('integrazione downloader: progressi reali di protocollo e pulizia dopo il salvataggio',async () => {
  const tempRoot = await mkdtemp(join(tmpdir(),'flusiva-test-'));
  const updates = [];
  try {
    const downloader = createDownloader({toolsDir:'tools',tempRoot,runner:async (_exe,args,options)=>{
      assert.ok(args.includes('--ignore-config')); assert.equal(args.at(-1),'https://www.youtube.com/watch?v=BaW_jenozKc');
      assert.equal(options.env.ELECTRON_RUN_AS_NODE,'1');
      if (args.includes('--dump-single-json')) return JSON.stringify({id:'BaW_jenozKc',title:'Prova',duration:12,formats:[{ext:'mp4',height:720,vcodec:'avc1',acodec:'aac'}]});
      options.onLine('FLUSIVA: 42.5%'); options.onLine('[Merger] Merging formats');
      await writeFile(args[args.indexOf('-o')+1].replace('%(ext)s','mp4'),'fixture'); return '';
    }});
    const info = await downloader.inspect('https://youtu.be/BaW_jenozKc');
    const file = await downloader.download(info,'mp4','best',{onProgress:p=>updates.push(p)});
    assert.match(file.path,/media.mp4$/);assert.deepEqual(updates,[{phase:'download',percent:42.5},{phase:'conversion',percent:null}]);
    await file.cleanup();assert.deepEqual(await readdir(tempRoot),[]);
  } finally {await rm(tempRoot,{recursive:true,force:true});}
});
test('errore di conversione e annullamento non lasciano file temporanei',async () => {
  const tempRoot = await mkdtemp(join(tmpdir(),'flusiva-test-'));
  try {
    for (const raw of ['conversion failed','cancelled']) {
      const downloader=createDownloader({toolsDir:'tools',tempRoot,runner:async(_exe,args)=>{await writeFile(args[args.indexOf('-o')+1].replace('%(ext)s','part'),'partial');throw new Error(raw);}});
      await assert.rejects(downloader.download({url:'https://youtu.be/BaW_jenozKc'},'mp3','320'));
      assert.deepEqual(await readdir(tempRoot),[]);
    }
  } finally {await rm(tempRoot,{recursive:true,force:true});}
});
