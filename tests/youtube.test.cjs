const { test } = require('node:test');
const assert = require('node:assert/strict');
const { normalizeUrl, summarize, downloadOptions, safeFilename, friendlyError } = require('../core/youtube.cjs');
const id = 'BaW_jenozKc';
test('normalizza link YouTube, Shorts e Music e scarta parametri di playlist',() => {
  for (const url of [`https://youtu.be/${id}?si=test`,`https://www.youtube.com/watch?v=${id}&list=PL123`,`https://youtube.com/shorts/${id}`,`https://music.youtube.com/watch?v=${id}`,`https://m.youtube.com/embed/${id}`]) assert.equal(normalizeUrl(url),`https://www.youtube.com/watch?v=${id}`);
});
test('rifiuta altri host, credenziali, porte, schemi, playlist e argomenti di comando',() => {
  for (const url of ['https://youtube.com.evil.test/watch?v='+id,'https://user:secret@youtube.com/watch?v='+id,'https://youtube.com:8443/watch?v='+id,'file:///tmp/video','--exec calc','https://youtube.com/playlist?list=PL123','https://tiktok.com/video/123','https://youtube.com/watch?v=short',null,{}]) assert.throws(() => normalizeUrl(url));
});
test('metadati espongono solo formati MP4 disponibili e thumbnail su host previsto',() => {
  const result = summarize({id,title:'Titolo',duration:90,uploader:'Canale',thumbnail:'https://evil.test/image',formats:[{ext:'webm',vcodec:'vp9',height:4320},{ext:'mp4',vcodec:'avc1',height:1080},{ext:'mp4',vcodec:'avc1',height:1080},{ext:'mp4',vcodec:'avc1',height:720},{ext:'m4a',acodec:'aac',vcodec:'none'}]});
  assert.deepEqual(result.heights,[1080,720]); assert.equal(result.hasAudio,true); assert.equal(result.thumbnail,`https://i.ytimg.com/vi/${id}/hqdefault.jpg`);
  for (const extra of [{is_live:true},{live_status:'is_upcoming'},{duration:0},{duration:22000}]) assert.throws(() => summarize({id,duration:90,...extra}));
});
test('opzioni qualità non possono iniettare argomenti e MP4 mantiene entrambi i flussi',() => {
  const mp4 = downloadOptions('mp4','1080'); assert.match(mp4[1],/bv\[ext=mp4\]\[height<=1080\]\+ba\[ext=m4a\]/);
  assert.ok(downloadOptions('mp3','320').includes('320K'));
  for (const options of [['mp3','--exec'],['mp4','9999'],['webm','best'],['mp3','best']]) assert.throws(() => downloadOptions(...options));
});
test('nomi file portabili, nomi Windows riservati e errori comprensibili',() => {
  assert.equal(safeFilename('../a:b*?','mp3'),'..ab.mp3'); assert.equal(safeFilename('CON','mp4'),'_CON.mp4'); assert.equal(safeFilename('  ','mp3'),'Flusiva.mp3');
  assert.match(friendlyError('ERROR Sign in to confirm you’re not a bot'),/YouTube ha bloccato/); assert.match(friendlyError('ENOSPC'),/Spazio/);
});
