const youtube = require('./youtube.cjs');
const {photosFrom}=require('./photos.cjs');
const PLATFORMS = Object.freeze([
  { id:'youtube', name:'YouTube', formats:'MP4 e MP3', description:'Video, Shorts e YouTube Music' },
  { id:'tiktok', name:'TikTok', formats:'MP4 e MP3', description:'Singoli video pubblici, anche da link brevi' },
  { id:'instagram', name:'Instagram', formats:'MP4 e MP3', description:'Reel e post con un singolo video pubblico' },
  { id:'facebook', name:'Facebook', formats:'MP4 e MP3', description:'Video e Reel pubblici, anche da link condivisi' },
  { id:'vimeo', name:'Vimeo', formats:'MP4 e MP3', description:'Singoli video pubblici' },
  { id:'dailymotion', name:'Dailymotion', formats:'MP4 e MP3', description:'Singoli video pubblici e link dai.ly' },
  { id:'soundcloud', name:'SoundCloud', formats:'MP3', description:'Singole tracce pubbliche' }
]);
function parseUrl(value) {
  if(typeof value!=='string'||value.length>2048) throw new Error('Incolla il link completo di un video o di una traccia.');
  let url;try{url=new URL(value.trim());}catch{throw new Error('Incolla un link completo, incluso https://.');}
  if(!['https:','http:'].includes(url.protocol)||url.username||url.password||url.port)throw new Error('Il link non è valido.');
  return url;
}
function identify(value) {
  const url=parseUrl(value),host=url.hostname.toLowerCase(),path=url.pathname;
  if(['open.spotify.com','spotify.link','www.spotify.com','spotify.com'].includes(host))throw new Error('Spotify non fornisce un MP3 completo scaricabile tramite questa integrazione. Non è possibile esportare il brano da questo link.');
  if(['youtube.com','www.youtube.com','m.youtube.com','music.youtube.com','youtu.be'].includes(host))return {platform:'youtube',url:youtube.normalizeUrl(value)};
  if(['www.tiktok.com','tiktok.com','m.tiktok.com'].includes(host)) {
    const match=path.match(/^\/@([\w.-]+)\/video\/(\d{10,25})\/?$/);
    if(match)return {platform:'tiktok',url:`https://www.tiktok.com/@${match[1]}/video/${match[2]}`};
    if(/^\/t\/[A-Za-z0-9_-]+\/?$/.test(path))return {platform:'tiktok',url:`https://www.tiktok.com${path}`,short:true};
  }
  if(['vm.tiktok.com','vt.tiktok.com'].includes(host)&&/^\/[A-Za-z0-9_-]+\/?$/.test(path))return {platform:'tiktok',url:`https://${host}${path}`,short:true};
  if(['instagram.com','www.instagram.com','m.instagram.com'].includes(host)) {
    if(/^\/share\/(?:(?:reel|p)\/)?[A-Za-z0-9_-]{3,100}\/?$/.test(path))return {platform:'instagram',url:`https://www.instagram.com${path}`,short:true};
    const match=path.match(/^\/(?:(?!share\/|stories\/|accounts\/|explore\/)[\w.]+\/)?(p|tv|reels?)\/([A-Za-z0-9_-]{3,64})\/?$/);
    if(match)return {platform:'instagram',url:`https://www.instagram.com/${match[1]==='reels'?'reel':match[1]}/${match[2]}/`};
  }
  if(['facebook.com','www.facebook.com','m.facebook.com','web.facebook.com','mbasic.facebook.com'].includes(host)) {
    const queryId=/^\/(?:watch\/?|video\.php|video\/video\.php)$/.test(path)?url.searchParams.get('v'):null;
    const pathId=path.match(/^\/(?:reel\/|[\w.-]+\/videos\/(?:[\w.-]+\/)?)(\d{1,30})\/?$/)?.[1];
    const id=pathId||queryId;
    if(/^\d{1,30}$/.test(id||''))return {platform:'facebook',url:`https://www.facebook.com/video.php?v=${id}`};
    const post=path.match(/^\/([\w.-]+)\/posts\/(pfbid[A-Za-z0-9]{5,200}|\d{1,30})\/?$/);
    if(post)return {platform:'facebook',url:`https://www.facebook.com/${post[1]}/posts/${post[2]}/`};
    if(/^\/share\/(?:v|r)\/[A-Za-z0-9_-]{3,100}\/?$/.test(path))return {platform:'facebook',url:`https://www.facebook.com${path}`,short:true};
  }
  if(['fb.watch','www.fb.watch'].includes(host)&&/^\/[A-Za-z0-9_-]{3,100}\/?$/.test(path))return {platform:'facebook',url:`https://fb.watch${path}`,short:true};
  if(['vimeo.com','www.vimeo.com','player.vimeo.com'].includes(host)) {
    const match=path.match(/^\/(?:video\/)?(\d{1,15})\/?$/);
    if(match)return {platform:'vimeo',url:`https://vimeo.com/${match[1]}`};
  }
  if(['dailymotion.com','www.dailymotion.com','geo.dailymotion.com','dai.ly'].includes(host)) {
    const match=path.match(host==='dai.ly'?/^\/([a-zA-Z0-9]{3,20})\/?$/:/^\/(?:embed\/)?video\/([a-zA-Z0-9]{3,20})(?:_[^/]*)?\/?$/);
    if(match)return {platform:'dailymotion',url:`https://www.dailymotion.com/video/${match[1]}`};
  }
  if(['soundcloud.com','www.soundcloud.com','m.soundcloud.com'].includes(host)) {
    const match=path.match(/^\/([\w-]+)\/([\w-]+)\/?$/);
    if(match&&!['sets','tracks','likes','reposts','albums'].includes(match[2])&&!['discover','search','you','charts','stations'].includes(match[1]))return {platform:'soundcloud',url:`https://soundcloud.com/${match[1]}/${match[2]}`};
  }
  if(['on.soundcloud.com','soundcloud.app.goo.gl'].includes(host)&&/^\/[a-zA-Z0-9_-]+\/?$/.test(path))return {platform:'soundcloud',url:`https://${host}${path}`,short:true};
  throw new Error('Scegli un singolo contenuto di YouTube, TikTok, Instagram, Facebook, Vimeo, Dailymotion o SoundCloud. Profili, storie e playlist non sono supportati.');
}
async function resolveLink(value,{signal,fetcher=fetch}={}) {
  let item=identify(value);
  if(!item.short)return item;
  const platform=item.platform;
  for(let count=0;count<6;count++) {
    if(signal?.aborted)throw new Error('cancelled');
    const response=await fetcher(item.url,{method:'GET',redirect:'manual',signal:AbortSignal.any([...(signal?[signal]:[]),AbortSignal.timeout(15_000)]),headers:{'User-Agent':'Mozilla/5.0'}});
    try {
      if(![301,302,303,307,308].includes(response.status))throw new Error('Il link abbreviato non rimanda a un singolo contenuto pubblico. Prova a copiare il link completo.');
      const location=response.headers.get('location');
      if(!location)throw new Error('Il link abbreviato non contiene una destinazione.');
      item=identify(new URL(location,item.url).href);
      if(item.platform!==platform)throw new Error('Il link abbreviato rimanda a una piattaforma diversa.');
      if(!item.short)return item;
    }finally{await response.body?.cancel();}
  }
  throw new Error('Troppi reindirizzamenti. Copia il link completo del contenuto.');
}
function thumbnailUrl(value) {
  try {
    const url=new URL(value);
    const domains=['ytimg.com','tiktokcdn.com','tiktokcdn-us.com','tiktokcdn-eu.com','muscdn.com','ibytedtos.com','byteoversea.com','vimeocdn.com','sndcdn.com','dmcdn.net','cdninstagram.com','fbcdn.net'];
    if(url.protocol==='https:'&&!url.username&&!url.password&&!url.port&&domains.some(d=>url.hostname===d||url.hostname.endsWith(`.${d}`)))return url.href;
  }catch{}
  return null;
}
function summarize(info,item) {
  if(item.platform==='instagram'&&info&&info.id!=null) {
    const photos=photosFrom(info);
    if(photos.length){
      const canonical=identify(info.webpage_url||item.url);
      if(canonical.short||canonical.platform!==item.platform)throw new Error('La piattaforma ha restituito un link inatteso.');
      return {id:String(info.id),platform:'instagram',platformName:'Instagram',url:canonical.url,title:String(info.title||'Foto Instagram').replace(/^Video by /,'Foto di ').slice(0,1000),channel:String(info.uploader||'Instagram').slice(0,500),duration:null,thumbnail:photos[0].url,heights:[],hasVideo:false,hasAudio:false,photos};
    }
  }
  if(!info||info._type==='playlist'||info.entries||info.id===undefined||!['string','number'].includes(typeof info.id))throw new Error('Scegli un singolo video o una traccia. Album, caroselli e playlist non sono supportati.');
  if(info.is_live||['is_live','is_upcoming'].includes(info.live_status))throw new Error('Le dirette in corso o programmate non sono supportate.');
  const unknownDuration=['instagram','facebook'].includes(item.platform)&&info.duration==null;
  if(!unknownDuration&&(!Number.isFinite(info.duration)||info.duration<=0||info.duration>21600))throw new Error('Scegli un contenuto con durata massima di 6 ore.');
  const formats=(info.formats||[]).filter(f=>!f.has_drm);
  const video=formats.filter(f=>f.ext!=='mhtml'&&((f.vcodec&&f.vcodec!=='none')||(f.vcodec==null&&f.video_ext&&f.video_ext!=='none')));
  const platform=PLATFORMS.find(p=>p.id===item.platform);
  if(!video.length&&!formats.some(f=>f.acodec&&f.acodec!=='none'))throw new Error('Il contenuto non offre formati audio o video scaricabili.');
  const heights=[...new Set(video.map(f=>Math.round(f.height)).filter(h=>Number.isFinite(h)&&h>0))].sort((a,b)=>b-a);
  const canonical=identify(info.webpage_url||item.url);
  if(canonical.short||canonical.platform!==item.platform)throw new Error('La piattaforma ha restituito un link inatteso.');
  return {id:String(info.id),platform:item.platform,platformName:platform.name,url:canonical.url,title:String(info.title||'Contenuto senza titolo').slice(0,1000),channel:String(info.uploader||info.channel||platform.name).slice(0,500),duration:unknownDuration?null:info.duration,thumbnail:item.platform==='youtube'?`https://i.ytimg.com/vi/${info.id}/hqdefault.jpg`:thumbnailUrl(info.thumbnail),heights,hasVideo:item.platform!=='soundcloud'&&video.length>0,hasAudio:formats.some(f=>f.acodec&&f.acodec!=='none')};
}
function downloadOptions(format,quality,info) {
  if(format==='image'){
    if(info?.platform!=='instagram'||!/^\d{1,2}$/.test(String(quality))||!info.photos?.[Number(quality)])throw new Error('Scegli una foto del post Instagram.');
    return [];
  }
  if(format==='mp3') {
    if(info&&!info.hasAudio)throw new Error('Questo contenuto non ha una traccia audio disponibile.');
    return [...youtube.downloadOptions(format,quality),'--format-sort','quality,abr,asr'];
  }
  youtube.downloadOptions(format,quality); // Shared quality validation.
  if(info?.platform==='soundcloud'||info?.hasVideo===false)throw new Error('Questo contenuto è solo audio. Scegli MP3.');
  const height=quality==='best'?'':`[height<=${quality}]`;
  const audio=info?.hasAudio===false?`bv*${height}`:`bv*${height}+ba`;
  return ['-f',`${audio}/b[vcodec!=none]${height}`,'--format-sort','res,fps','--merge-output-format','mp4','--remux-video','mp4','--embed-metadata','--postprocessor-args','Merger+ffmpeg_o:-movflags +faststart'];
}
function friendlyError(raw,platform='la piattaforma') {
  platform=PLATFORMS.find(item=>item.id===platform)?.name||platform;
  const text=String(raw);
  if(/aborted|cancelled/i.test(text))return 'Operazione annullata.';
  if(/Sign in|log(?:ged)?[ -]?in|login|bot|cookies|HTTP Error (401|403|429)|rate.?limit|empty media response/i.test(text))return `${platform} ha limitato la richiesta o richiede l’accesso. Prova un contenuto pubblico accessibile senza login, riprova più tardi o aggiorna Flusiva.`;
  if(/no video|no formats|does not contain.*video/i.test(text))return `${platform} non ha restituito un video scaricabile. Usa il link di un singolo video pubblico, non di una foto, un album o un profilo.`;
  if(/timed out|timeout|network|resolve|connection|ENOTFOUND/i.test(text))return `Connessione a ${platform} non riuscita. Controlla Internet e riprova.`;
  return youtube.friendlyError(raw).replaceAll('YouTube',platform);
}
module.exports={PLATFORMS,identify,resolveLink,thumbnailUrl,summarize,downloadOptions,friendlyError};
