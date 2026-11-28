// Metadata-only probes of public upstream fixtures. No media is downloaded.
const {createDownloader}=require('../core/downloader.cjs');
const {join}=require('node:path');
const {tmpdir}=require('node:os');
const {writeFile,mkdir,stat}=require('node:fs/promises');
const {execFileSync}=require('node:child_process');
const samples=[
  ['youtube','https://www.youtube.com/watch?v=aqz-KE-bpKQ'],
  ['vimeo','https://vimeo.com/76979871'],
  ['dailymotion','https://www.dailymotion.com/video/x5kesuj'],
  ['soundcloud','https://soundcloud.com/ethmusic/lostin-powers-she-so-heavy'],
  ['tiktok','https://www.tiktok.com/@scout2015/video/6718335390845095173'],
  ['instagram','https://www.instagram.com/reel/Chunk8-jurw/'],
  ['facebook','https://www.facebook.com/video.php?v=274175099429670'],
  ['instagram-photo','https://www.instagram.com/p/BqvsDleB3lV/']
];
async function main(){
  const downloader=createDownloader({toolsDir:join(__dirname,'../vendor'),tempRoot:tmpdir(),nodePath:require('electron')});
  const transfer=process.argv.includes('--download-mp4');
  const selected=process.argv.slice(2).filter(value=>value!=='--download-mp4');
  if(selected.some(name=>!samples.some(([platform])=>name===platform)))throw new Error('Piattaforma di prova sconosciuta.');
  const results=await Promise.all(samples.filter(([platform])=>!selected.length||selected.includes(platform)).map(async([platform,url])=>{
    try{
      const info=await downloader.inspect(url);const result={platform,status:'ok',url,title:info.title,duration:info.duration,hasVideo:info.hasVideo,hasAudio:info.hasAudio,heights:info.heights};
      if(transfer){
        const file=await downloader.download(info,info.photos?.length?'image':'mp4',info.photos?.length?'0':'best');
        try{
          const facts=JSON.parse(execFileSync(join(__dirname,'../vendor',process.platform==='win32'?'ffprobe.exe':'ffprobe'),['-v','error','-show_streams','-show_format','-of','json',file.path],{encoding:'utf8',windowsHide:true}));
          if(!facts.streams.some(stream=>stream.codec_type==='video'))throw new Error('Traccia video mancante.');
          result.download={bytes:(await stat(file.path)).size,extension:file.extension||'mp4',duration:Number(facts.format.duration),streams:facts.streams.map(({codec_type,codec_name,width,height})=>({codec_type,codec_name,width,height}))};
        }finally{await file.cleanup();}
      }
      console.log(JSON.stringify(result));return result;
    }
    catch(error){const result={platform,status:'unavailable',url,error:error.message};console.log(JSON.stringify(result));return result;}
  }));
  await mkdir('test-results',{recursive:true});await writeFile(`test-results/${transfer?'platform-downloads':'platform-probes'}.json`,JSON.stringify({checkedAt:new Date().toISOString(),results},null,2));
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
