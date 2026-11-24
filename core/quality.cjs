const {downloadOptions}=require('./youtube.cjs');
function resolveQuality(format,requested,preferences){
  downloadOptions(format,requested);
  return preferences.maximumQuality?(format==='mp3'?'320':'best'):requested;
}
module.exports={resolveQuality};
