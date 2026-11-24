const {test}=require('node:test');
const assert=require('node:assert/strict');
const {resolveQuality}=require('../core/quality.cjs');
const {downloadOptions,identify}=require('../core/platforms.cjs');
test('qualità massima prevale sui limiti manuali senza ignorare input non validi',()=>{
  assert.equal(resolveQuality('mp4','720',{maximumQuality:true}),'best');assert.equal(resolveQuality('mp3','192',{maximumQuality:true}),'320');
  assert.equal(resolveQuality('mp4','720',{maximumQuality:false}),'720');assert.equal(resolveQuality('mp3','192',{maximumQuality:false}),'192');
  assert.throws(()=>resolveQuality('mp3','--exec',{maximumQuality:true}));
});
test('non preferisce un MP4 di qualità inferiore a una sorgente video migliore',()=>{
  const video=downloadOptions('mp4','best',{platform:'tiktok',hasVideo:true,hasAudio:true});
  assert.equal(video[1],'bv*+ba/b[vcodec!=none]');assert.ok(!video[1].includes('[ext=mp4]'));assert.equal(video[video.indexOf('--format-sort')+1],'res,fps');
  const audio=downloadOptions('mp3','320',{platform:'soundcloud',hasAudio:true});assert.equal(audio[1],'bestaudio/best');assert.equal(audio[audio.indexOf('--format-sort')+1],'quality,abr,asr');assert.ok(audio.includes('320K'));
});
test('un link Spotify non viene presentato come download MP3 disponibile',()=>{
  assert.throws(()=>identify('https://open.spotify.com/track/0Lr4kGOYn9l83EjuK6cZFQ'),/non fornisce un MP3 completo/);
});
