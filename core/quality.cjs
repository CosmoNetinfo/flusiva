const {downloadOptions}=require('./youtube.cjs');
function resolveQuality(format,requested,preferences){
  if(format==='image'){if(!/^\d{1,2}$/.test(String(requested)))throw new Error('Foto non valida.');return String(requested);}
  downloadOptions(format,requested);
  return preferences.maximumQuality?(format==='mp3'?'320':'best'):requested;
}
module.exports={resolveQuality};
