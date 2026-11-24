const {test}=require('node:test');
const assert=require('node:assert/strict');
const {identify,resolveLink,summarize,thumbnailUrl,downloadOptions}=require('../core/platforms.cjs');
test('normalizza le cinque piattaforme senza mantenere query di tracking',()=>{
  for(const [input,platform,url]of [
    ['https://youtu.be/BaW_jenozKc?si=x','youtube','https://www.youtube.com/watch?v=BaW_jenozKc'],
    ['https://www.tiktok.com/@creator/video/7123456789012345678?share=x','tiktok','https://www.tiktok.com/@creator/video/7123456789012345678'],
    ['https://player.vimeo.com/video/1234567?autoplay=1','vimeo','https://vimeo.com/1234567'],
    ['https://dai.ly/x123abc','dailymotion','https://www.dailymotion.com/video/x123abc'],
    ['https://www.dailymotion.com/video/x123abc_title','dailymotion','https://www.dailymotion.com/video/x123abc'],
    ['https://soundcloud.com/artist/track?utm_source=clipboard','soundcloud','https://soundcloud.com/artist/track']
  ])assert.deepEqual(identify(input),{platform,url});
});
test('rifiuta playlist, domini ingannevoli, file locali e credenziali',()=>{
  for(const input of ['https://youtube.com.evil.test/watch?v=BaW_jenozKc','https://www.tiktok.com.evil.test/@u/video/7123456789012345678','https://vimeo.com/album/123','https://soundcloud.com/artist/sets/album','https://soundcloud.com/artist/tracks','https://soundcloud.com/search/sounds','https://www.tiktok.com/@u','https://vimeo.com:8443/123','https://user:pw@vimeo.com/123','file:///C:/test','--exec calc','https://instagram.com/reel/abc'])assert.throws(()=>identify(input));
});
test('risolve link brevi con redirect manuali e controlla ogni destinazione',async()=>{
  const calls=[];
  const item=await resolveLink('https://vm.tiktok.com/Abc123/',{fetcher:async(url,options)=>{
    calls.push(url);assert.equal(options.redirect,'manual');
    return new Response(null,{status:302,headers:{location:'https://www.tiktok.com/@u/video/7123456789012345678?share=1'}});
  }});
  assert.equal(item.platform,'tiktok');assert.ok(!item.short);assert.equal(calls.length,1);
  for(const destination of ['http://127.0.0.1/secret','https://evil.test/file','https://vimeo.com/123'])await assert.rejects(resolveLink('https://vm.tiktok.com/Abc123/',{fetcher:async()=>new Response(null,{status:302,headers:{location:destination}})}));
  const controller=new AbortController();controller.abort();await assert.rejects(resolveLink('https://on.soundcloud.com/test',{signal:controller.signal,fetcher:()=>{throw new Error('Non dovrebbe partire');}}),/cancelled/);
});
test('limita catene di redirect e non accetta pagine intermedie come video',async()=>{
  let count=0;await assert.rejects(resolveLink('https://on.soundcloud.com/test',{fetcher:async()=>{count++;return new Response(null,{status:302,headers:{location:'https://on.soundcloud.com/test'}});}}),/Troppi/);assert.equal(count,6);
  await assert.rejects(resolveLink('https://vm.tiktok.com/test',{fetcher:async()=>new Response('pagina',{status:200})}),/link completo/);
});
test('metadati audio-only, formati senza DRM e immagini autorizzate',()=>{
  const info=summarize({id:123,title:'Traccia',duration:60,thumbnail:'https://i1.sndcdn.com/art.jpg',formats:[{acodec:'aac',vcodec:'none',ext:'m4a'}]},{platform:'soundcloud',url:'https://soundcloud.com/artist/track'});
  assert.equal(info.hasVideo,false);assert.equal(info.hasAudio,true);assert.equal(info.platformName,'SoundCloud');assert.throws(()=>downloadOptions('mp4','best',info),/solo audio/);assert.ok(downloadOptions('mp3','320',info).includes('320K'));
  assert.equal(thumbnailUrl('https://i1.sndcdn.com.evil.test/art.jpg'),null);assert.equal(thumbnailUrl('file:///test.jpg'),null);assert.equal(thumbnailUrl('https://i.vimeocdn.com/video/image.jpg'),'https://i.vimeocdn.com/video/image.jpg');
  const video=summarize({id:'123',duration:60,formats:[{vcodec:'vp9',height:720,ext:'webm'},{vcodec:'av1',height:2160,ext:'mp4',has_drm:true}]},{platform:'vimeo',url:'https://vimeo.com/123'});
  assert.deepEqual(video.heights,[720]);assert.ok(downloadOptions('mp4','720',video).includes('--remux-video'));
});
test('rifiuta playlist o cambi inattesi della piattaforma nei metadati',()=>{
  for(const data of [{_type:'playlist',entries:[]},{id:123,is_live:true,duration:60},{id:123,duration:0},{id:123,duration:60,webpage_url:'https://soundcloud.com/artist/track'}])assert.throws(()=>summarize(data,{platform:'vimeo',url:'https://vimeo.com/123'}));
});
