const LIMIT=50*1024*1024;
function photoUrl(value){
  try{const u=new URL(value);if(u.protocol==='https:'&&!u.username&&!u.password&&!u.port&&['cdninstagram.com','fbcdn.net'].some(d=>u.hostname===d||u.hostname.endsWith(`.${d}`)))return u.href;}catch{}
  return null;
}
function photosFrom(info){
  const entries=info.entries||[info];
  if(!Array.isArray(entries)||entries.length>20)throw new Error('Il post contiene troppe immagini. Massimo 20 elementi.');
  return entries.flatMap(entry=>{
    // A video's thumbnail is never offered as a photograph.
    if(entry.flusiva_media_type!=='image'||entry.formats?.length||entry.is_live||entry.has_drm)return [];
    const candidates=(entry.thumbnails||[]).filter(t=>photoUrl(t.url));
    const best=candidates.map((t,index)=>({...t,index,area:(Number(t.width)||0)*(Number(t.height)||0)})).sort((a,b)=>a.area-b.area||a.index-b.index).at(-1);
    return best?[{url:photoUrl(best.url),width:Number(best.width)||null,height:Number(best.height)||null}]:[];
  });
}
function imageExtension(bytes){
  if(bytes.length>=3&&bytes[0]===255&&bytes[1]===216&&bytes[2]===255)return 'jpg';
  if(bytes.length>=8&&bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))return 'png';
  if(bytes.length>=12&&bytes.toString('ascii',0,4)==='RIFF'&&bytes.toString('ascii',8,12)==='WEBP')return 'webp';
  throw new Error('Instagram non ha restituito un’immagine JPEG, PNG o WebP valida.');
}
async function fetchPhoto(value,{signal,fetcher=fetch}={}){
  let url=photoUrl(value);if(!url)throw new Error('Indirizzo dell’immagine non valido.');
  for(let hop=0;hop<6;hop++){
    signal?.throwIfAborted();
    const response=await fetcher(url,{redirect:'manual',signal:AbortSignal.any([...(signal?[signal]:[]),AbortSignal.timeout(60000)]),headers:{Referer:'https://www.instagram.com/'}});
    if([301,302,303,307,308].includes(response.status)){
      await response.body?.cancel();const next=response.headers.get('location');url=next&&photoUrl(new URL(next,url).href);if(!url)throw new Error('Reindirizzamento dell’immagine non valido.');continue;
    }
    if(!response.ok){await response.body?.cancel();throw new Error(`HTTP Error ${response.status}`);}
    if(Number(response.headers.get('content-length'))>LIMIT){await response.body?.cancel();throw new Error('Immagine troppo grande (massimo 50 MiB).');}
    const reader=response.body.getReader(),chunks=[];let size=0;
    try{while(true){signal?.throwIfAborted();const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>LIMIT)throw new Error('Immagine troppo grande (massimo 50 MiB).');chunks.push(Buffer.from(value));}}finally{await reader.cancel();}
    const bytes=Buffer.concat(chunks);return {bytes,extension:imageExtension(bytes)};
  }
  throw new Error('Troppi reindirizzamenti dell’immagine.');
}
module.exports={photoUrl,photosFrom,imageExtension,fetchPhoto};
