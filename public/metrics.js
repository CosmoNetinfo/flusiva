(function(root){
  const nf=new Intl.NumberFormat('it-IT',{maximumFractionDigits:1});
  function bytes(value){if(!Number.isFinite(value)||value<0)return '—';const units=['B','KB','MB','GB','TB'];let index=0;while(value>=1024&&index<units.length-1){value/=1024;index++;}return `${nf.format(value)} ${units[index]}`;}
  function eta(value){if(!Number.isFinite(value)||value<0)return '—';const seconds=Math.ceil(value);if(seconds<60)return `${seconds} s`;const minutes=Math.floor(seconds/60);if(minutes<60)return `${minutes} min ${seconds%60} s`;return `${Math.floor(minutes/60)} h ${minutes%60} min`;}
  function summary(progress){if(!progress||progress.phase!=='download')return '';const total=Number.isFinite(progress.totalBytes)?`${progress.totalEstimated?'circa ':''}${bytes(progress.totalBytes)}`:'dimensione non disponibile';return `${bytes(progress.downloadedBytes)} di ${total} · ${Number.isFinite(progress.speed)?`${bytes(progress.speed)}/s`:'velocità in attesa'} · ${Number.isFinite(progress.eta)?`${eta(progress.eta)} rimanenti`:'tempo da stimare'}`;}
  const api={bytes,eta,summary};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.flusivaMetrics=api;
})(typeof window==='undefined'?globalThis:window);
