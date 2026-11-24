const {test}=require('node:test');
const assert=require('node:assert/strict');
const {parseProgress}=require('../core/progress.cjs');
const {bytes,eta,summary}=require('../public/metrics.js');
test('legge byte, velocità, ETA e dimensione stimata dalla traccia corrente',()=>{
  assert.deepEqual(parseProgress('FLUSIVA: 25.0%|1024|4096|NA|512.5|6|137'),{phase:'download',percent:25,downloadedBytes:1024,totalBytes:4096,totalEstimated:false,speed:512.5,eta:6,formatId:'137'});
  const estimated=parseProgress('FLUSIVA: NA|100|NA|1000|NA|NA|hls');assert.equal(estimated.percent,null);assert.equal(estimated.totalBytes,1000);assert.equal(estimated.totalEstimated,true);assert.equal(estimated.eta,null);
});
test('dati mancanti o non validi non producono tempi o velocità inventati',()=>{
  assert.equal(parseProgress('messaggio normale'),null);
  const unknown=parseProgress('FLUSIVA: NA|NA|NA|NA|NA|-1|NA');assert.equal(unknown.speed,null);assert.equal(unknown.eta,null);assert.equal(unknown.downloadedBytes,null);
  assert.equal(parseProgress('FLUSIVA:100%|12|12|NA|2|0|a').percent,99);
  assert.equal(bytes(null),'—');assert.equal(eta(null),'—');assert.equal(bytes(1024),'1 KB');assert.equal(eta(65),'1 min 5 s');assert.equal(eta(3700),'1 h 1 min');
  assert.match(summary(unknown),/dimensione non disponibile/);assert.equal(summary({phase:'conversion'}),'');
});
