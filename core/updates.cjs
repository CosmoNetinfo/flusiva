const REPOSITORY='https://github.com/CosmoNetinfo/flusiva';
const ENDPOINT='https://api.github.com/repos/CosmoNetinfo/flusiva/releases/latest';
function versionParts(value){const match=String(value).match(/^v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/);if(!match)throw new Error('Versione della release non valida.');const parts=match.slice(1).map(Number);if(parts.some(part=>!Number.isSafeInteger(part)))throw new Error('Versione della release non valida.');return parts;}
function compareVersions(a,b){const av=versionParts(a),bv=versionParts(b);for(let i=0;i<3;i++)if(av[i]!==bv[i])return av[i]>bv[i]?1:-1;return 0;}
async function checkLatest(currentVersion,{fetcher=fetch}={}){
  versionParts(currentVersion);
  let response;
  try{response=await fetcher(ENDPOINT,{headers:{Accept:'application/vnd.github+json','User-Agent':`Flusiva/${currentVersion}`},redirect:'error',signal:AbortSignal.timeout(15_000)});}catch{throw new Error('Impossibile contattare GitHub. Controlla Internet e riprova.');}
  if(response.status===404)return {status:'unpublished',currentVersion,latestVersion:null,url:`${REPOSITORY}/releases`};
  if([403,429].includes(response.status))throw new Error('GitHub ha limitato le richieste. Riprova più tardi.');
  if(!response.ok)throw new Error('Il controllo degli aggiornamenti non è disponibile. Riprova più tardi.');
  let data;try{data=await response.json();}catch{throw new Error('GitHub ha restituito una risposta non valida.');}
  if(!data||data.draft!==false||data.prerelease!==false)throw new Error('La risposta non contiene una release stabile.');
  const latestVersion=versionParts(data.tag_name).join('.');
  const url=`${REPOSITORY}/releases/tag/${data.tag_name}`;
  if(data.html_url!==url)throw new Error('La release non appartiene al repository ufficiale.');
  return {status:compareVersions(latestVersion,currentVersion)>0?'available':'current',currentVersion,latestVersion,url,publishedAt:Number.isFinite(Date.parse(data.published_at))?new Date(data.published_at).toISOString():null};
}
function createUpdateChecker(currentVersion,options={}){
  let inFlight,last,lastChecked=0;
  return ()=>{
    if(inFlight)return inFlight;
    if(last&&Date.now()-lastChecked<60_000)return Promise.resolve(last);
    inFlight=checkLatest(currentVersion,options).then(result=>{last=result;lastChecked=Date.now();return result;}).finally(()=>{inFlight=null;});
    return inFlight;
  };
}
module.exports={checkLatest,createUpdateChecker,compareVersions};
