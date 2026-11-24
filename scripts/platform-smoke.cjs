// Metadata-only probes of public upstream fixtures. No media is downloaded.
const {createDownloader}=require('../core/downloader.cjs');
const {join}=require('node:path');
const {tmpdir}=require('node:os');
const {writeFile,mkdir}=require('node:fs/promises');
const samples=[
  ['youtube','https://www.youtube.com/watch?v=aqz-KE-bpKQ'],
  ['vimeo','https://vimeo.com/76979871'],
  ['dailymotion','https://www.dailymotion.com/video/x5kesuj'],
  ['soundcloud','https://soundcloud.com/ethmusic/lostin-powers-she-so-heavy'],
  ['tiktok','https://www.tiktok.com/@scout2015/video/6718335390845095173']
];
async function main(){
  const downloader=createDownloader({toolsDir:join(__dirname,'../vendor'),tempRoot:tmpdir(),nodePath:require('electron')});
  const results=await Promise.all(samples.map(async([platform,url])=>{
    try{const info=await downloader.inspect(url);const result={platform,status:'ok',url,title:info.title,duration:info.duration,hasVideo:info.hasVideo,hasAudio:info.hasAudio,heights:info.heights};console.log(JSON.stringify(result));return result;}
    catch(error){const result={platform,status:'unavailable',url,error:error.message};console.log(JSON.stringify(result));return result;}
  }));
  await mkdir('test-results',{recursive:true});await writeFile('test-results/platform-probes.json',JSON.stringify({checkedAt:new Date().toISOString(),results},null,2));
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
