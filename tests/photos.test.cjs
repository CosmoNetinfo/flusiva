const {test}=require('node:test');const assert=require('node:assert/strict');
const {photosFrom,fetchPhoto,imageExtension}=require('../core/photos.cjs');const {summarize,identify,downloadOptions}=require('../core/platforms.cjs');const {resolveQuality}=require('../core/quality.cjs');
const jpeg=Buffer.from([255,216,255,224,0,0,0,0,255,217]);
test('foto vere alla maggiore dimensione, senza esportare copertine video',()=>{
  const photo={flusiva_media_type:'image',thumbnails:[{url:'https://scontent.cdninstagram.com/small.jpg',width:320,height:320},{url:'https://scontent.cdninstagram.com/original.jpg',width:1080,height:1350}]};
  const data={id:'123',_type:'playlist',entries:[photo,{...photo,flusiva_media_type:'video'}, {...photo,formats:[{vcodec:'h264'}]}]};
  assert.equal(photosFrom(data).length,1);assert.equal(photosFrom(data)[0].width,1080);
  const info=summarize(data,identify('https://instagram.com/p/BqvsDleB3lV/'));assert.equal(info.photos.length,1);assert.equal(info.hasVideo,false);assert.equal(info.hasAudio,false);
  assert.deepEqual(downloadOptions('image','0',info),[]);assert.throws(()=>downloadOptions('image','1',info));assert.throws(()=>downloadOptions('image','--exec',info));assert.throws(()=>downloadOptions('mp4','best',info));
  assert.equal(resolveQuality('image','0',{maximumQuality:true}),'0');
  assert.deepEqual(photosFrom({...photo,flusiva_media_type:undefined}),[]);
  assert.throws(()=>photosFrom({entries:Array(21).fill(photo)}));
});
test('download foto limitato ai CDN Meta, redirect controllati e risposta immagine',async()=>{
  const image=await fetchPhoto('https://scontent.cdninstagram.com/original.jpg',{fetcher:async()=>new Response(jpeg)});assert.deepEqual(image.bytes,jpeg);assert.equal(image.extension,'jpg');
  assert.equal(imageExtension(Buffer.from([137,80,78,71,13,10,26,10])),'png');assert.equal(imageExtension(Buffer.from('RIFF1234WEBP')),'webp');
  for(const url of ['http://scontent.cdninstagram.com/x.jpg','https://cdninstagram.com.evil.test/x.jpg','https://user:password@fbcdn.net/x.jpg','https://fbcdn.net:8443/x.jpg','file:///secret'])await assert.rejects(fetchPhoto(url));
  await assert.rejects(fetchPhoto('https://fbcdn.net/image.jpg',{fetcher:async()=>new Response(null,{status:302,headers:{location:'http://127.0.0.1/secret'}})}));
  await assert.rejects(fetchPhoto('https://fbcdn.net/image.jpg',{fetcher:async()=>new Response('<html>login</html>')}),/immagine/);
  await assert.rejects(fetchPhoto('https://fbcdn.net/image.jpg',{fetcher:async()=>new Response(jpeg,{headers:{'content-length':String(51*1024*1024)}})}),/troppo grande/);
  const controller=new AbortController();controller.abort();await assert.rejects(fetchPhoto('https://fbcdn.net/image.jpg',{signal:controller.signal,fetcher:()=>{throw new Error('Non deve partire');}}));
});
