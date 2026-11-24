const PROGRESS_TEMPLATE='download:FLUSIVA:%(progress._percent_str)s|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s|%(info.format_id)s';
const number=value=>{if(value===undefined||String(value).trim()==='')return null;const parsed=Number(value);return Number.isFinite(parsed)&&parsed>=0?parsed:null;};
function parseProgress(line){
  if(!line.startsWith('FLUSIVA:'))return null;
  const [percentage,downloaded,total,estimated,speed,eta,formatId]=line.slice(8).split('|');
  const percent=number(percentage.trim().replace('%',''));
  const totalBytes=number(total),estimate=number(estimated);
  return {phase:'download',percent:percent===null?null:Math.min(99,percent),downloadedBytes:number(downloaded),totalBytes:totalBytes||estimate||null,totalEstimated:!totalBytes&&!!estimate,speed:number(speed),eta:number(eta),formatId:formatId&&formatId!=='NA'?formatId.slice(0,160):null};
}
module.exports={PROGRESS_TEMPLATE,parseProgress};
