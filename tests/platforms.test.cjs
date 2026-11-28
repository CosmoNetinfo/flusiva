const {test}=require('node:test');
const assert=require('node:assert/strict');
const {identify,resolveLink,summarize,thumbnailUrl,downloadOptions,friendlyError}=require('../core/platforms.cjs');
test('normalizza le sette piattaforme senza mantenere query di tracking',()=>{
  for(const [input,platform,url]of [
    ['https://youtu.be/BaW_jenozKc?si=x','youtube','https://www.youtube.com/watch?v=BaW_jenozKc'],
    ['https://www.tiktok.com/@creator/video/7123456789012345678?share=x','tiktok','https://www.tiktok.com/@creator/video/7123456789012345678'],
    ['https://player.vimeo.com/video/1234567?autoplay=1','vimeo','https://vimeo.com/1234567'],
    ['https://dai.ly/x123abc','dailymotion','https://www.dailymotion.com/video/x123abc'],
    ['https://www.dailymotion.com/video/x123abc_title','dailymotion','https://www.dailymotion.com/video/x123abc'],
    ['https://soundcloud.com/artist/track?utm_source=clipboard','soundcloud','https://soundcloud.com/artist/track'],
    ['https://www.instagram.com/reel/Chunk8-jurw/?igsh=tracking','instagram','https://www.instagram.com/reel/Chunk8-jurw/'],
    ['https://m.instagram.com/creator/reels/Chunk8-jurw/?utm_source=x','instagram','https://www.instagram.com/reel/Chunk8-jurw/'],
    ['https://instagram.com/p/aye83DjauH/?img_index=1','instagram','https://www.instagram.com/p/aye83DjauH/'],
    ['https://instagram.com/tv/Chunk8-jurw/','instagram','https://www.instagram.com/tv/Chunk8-jurw/'],
    ['https://m.facebook.com/watch/?v=274175099429670&ref=sharing','facebook','https://www.facebook.com/video.php?v=274175099429670'],
    ['https://www.facebook.com/reel/274175099429670/?mibextid=x','facebook','https://www.facebook.com/video.php?v=274175099429670'],
    ['https://web.facebook.com/creator/videos/video-title/274175099429670/','facebook','https://www.facebook.com/video.php?v=274175099429670'],
    ['https://facebook.com/creator/videos/274175099429670/','facebook','https://www.facebook.com/video.php?v=274175099429670'],
    ['https://www.facebook.com/video/video.php?v=274175099429670','facebook','https://www.facebook.com/video.php?v=274175099429670'],
    ['https://facebook.com/creator/posts/pfbidAbc12345/?tracking=1','facebook','https://www.facebook.com/creator/posts/pfbidAbc12345/']
  ])assert.deepEqual(identify(input),{platform,url});
});
test('rifiuta playlist, domini ingannevoli, file locali e credenziali',()=>{
  for(const input of ['https://youtube.com.evil.test/watch?v=BaW_jenozKc','https://www.tiktok.com.evil.test/@u/video/7123456789012345678','https://vimeo.com/album/123','https://soundcloud.com/artist/sets/album','https://soundcloud.com/artist/tracks','https://soundcloud.com/search/sounds','https://www.tiktok.com/@u','https://vimeo.com:8443/123','https://user:pw@vimeo.com/123','file:///C:/test','--exec calc','https://instagram.com/creator/','https://instagram.com/stories/creator/123','https://instagram.com/share/stories/abc','https://instagram.com/reels/audio/123','https://instagram.com.evil.test/reel/abc','https://facebook.com.evil.test/watch?v=123','https://facebook.com/creator/videos/','https://facebook.com/watch/?v=bad','https://facebook.com/share/p/abc','https://facebook.com/l.php?u=https://evil.test','https://fb.watch.evil.test/abc','https://facebook.com:8443/reel/123','https://user:pass@facebook.com/reel/123'])assert.throws(()=>identify(input),undefined,input);
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
test('i link Facebook condivisi restano nella stessa piattaforma a ogni redirect',async()=>{
  for(const short of ['https://fb.watch/Abc123/?tracking=1','https://www.facebook.com/share/v/Abc123/','https://m.facebook.com/share/r/Abc123/']) {
    assert.equal(identify(short).short,true);
    const resolved=await resolveLink(short,{fetcher:async()=>new Response(null,{status:302,headers:{location:'https://m.facebook.com/reel/274175099429670/?tracking=1'}})});
    assert.deepEqual(resolved,{platform:'facebook',url:'https://www.facebook.com/video.php?v=274175099429670'});
    for(const location of ['https://www.instagram.com/reel/Chunk8-jurw/','http://127.0.0.1/video','https://facebook.com/login/?next=/reel/123','https://facebook.com.evil.test/reel/123'])await assert.rejects(resolveLink(short,{fetcher:async()=>new Response(null,{status:302,headers:{location}})}));
  }
});
test('i link Instagram share vengono risolti senza consentire altre destinazioni',async()=>{
  for(const url of ['https://instagram.com/share/BAWVJE-rtk','https://instagram.com/share/p/BACiUUUYQV/','https://www.instagram.com/share/reel/BARSSL4rTu/']){
    assert.equal(identify(url).short,true);
    const item=await resolveLink(url,{fetcher:async()=>new Response(null,{status:302,headers:{location:'/reel/Chunk8-jurw/?igsh=x'}})});
    assert.deepEqual(item,{platform:'instagram',url:'https://www.instagram.com/reel/Chunk8-jurw/'});
    await assert.rejects(resolveLink(url,{fetcher:async()=>new Response(null,{status:302,headers:{location:'https://facebook.com/reel/123456/'}})}));
  }
});
test('Reel e video Meta conservano fonte, qualità e copertine consentite',()=>{
  for(const [platform,url,canonical,thumbnail] of [
    ['instagram','https://instagram.com/reel/Chunk8-jurw/','https://www.instagram.com/p/Chunk8-jurw/','https://scontent.cdninstagram.com/image.jpg'],
    ['facebook','https://facebook.com/reel/274175099429670/','https://www.facebook.com/watch/?v=274175099429670','https://scontent.fra1.fbcdn.net/image.jpg']
  ]) {
    const item=identify(url);
    const info=summarize({id:'123',duration:8,webpage_url:canonical,thumbnail,formats:[{vcodec:'h264',acodec:'aac',height:1080,ext:'mp4'}]},item);
    assert.equal(info.platform,platform);assert.equal(info.url,identify(canonical).url);assert.equal(info.thumbnail,thumbnail);assert.equal(info.hasVideo,true);
    const options=downloadOptions('mp4','best',info);assert.equal(options[1],'bv*+ba/b[vcodec!=none]');assert.ok(options.includes('--remux-video'));assert.ok(!options.includes('--recode-video'));
    assert.throws(()=>summarize({_type:'playlist',entries:[]},item),/caroselli/);
  }
  assert.equal(thumbnailUrl('https://fbcdn.net.evil.test/image.jpg'),null);
  assert.equal(thumbnailUrl('https://evilcdninstagram.com/image.jpg'),null);
  assert.match(friendlyError('This post is not accessible without being logged-in','instagram'),/Instagram.*accesso/);
  assert.match(friendlyError('No video in this post','facebook'),/Facebook.*singolo video/);
});
test('metadati audio-only, formati senza DRM e immagini autorizzate',()=>{
  const info=summarize({id:123,title:'Traccia',duration:60,thumbnail:'https://i1.sndcdn.com/art.jpg',formats:[{acodec:'aac',vcodec:'none',ext:'m4a'}]},{platform:'soundcloud',url:'https://soundcloud.com/artist/track'});
  assert.equal(info.hasVideo,false);assert.equal(info.hasAudio,true);assert.equal(info.platformName,'SoundCloud');assert.throws(()=>downloadOptions('mp4','best',info),/solo audio/);assert.ok(downloadOptions('mp3','320',info).includes('320K'));
  assert.equal(thumbnailUrl('https://i1.sndcdn.com.evil.test/art.jpg'),null);assert.equal(thumbnailUrl('file:///test.jpg'),null);assert.equal(thumbnailUrl('https://i.vimeocdn.com/video/image.jpg'),'https://i.vimeocdn.com/video/image.jpg');
  const video=summarize({id:'123',duration:60,formats:[{vcodec:'vp9',height:720,ext:'webm'},{vcodec:'av1',height:2160,ext:'mp4',has_drm:true}]},{platform:'vimeo',url:'https://vimeo.com/123'});
  assert.deepEqual(video.heights,[720]);assert.ok(downloadOptions('mp4','720',video).includes('--remux-video'));
});
test('i metadati Meta possono omettere durata e codec senza inventare i dati',()=>{
  for(const [platform,url] of [['instagram','https://instagram.com/reel/Chunk8-jurw/'],['facebook','https://facebook.com/reel/123456/']]) {
    const item=identify(url),raw={id:'123',formats:[{ext:'mp4',video_ext:'mp4'}]};
    const info=summarize(raw,item);assert.equal(info.duration,null);assert.equal(info.hasVideo,true);assert.equal(info.hasAudio,false);
    assert.equal(downloadOptions('mp4','best',info)[1],'bv*/b[vcodec!=none]');
    for(const duration of [0,-1,21601,'8'])assert.throws(()=>summarize({...raw,duration},item));
    assert.throws(()=>summarize({...raw,formats:[{ext:'jpg'}]},item));
  }
});
test('rifiuta playlist o cambi inattesi della piattaforma nei metadati',()=>{
  for(const data of [{_type:'playlist',entries:[]},{id:123,is_live:true,duration:60},{id:123,duration:0},{id:123,duration:60,webpage_url:'https://soundcloud.com/artist/track'}])assert.throws(()=>summarize(data,{platform:'vimeo',url:'https://vimeo.com/123'}));
});
