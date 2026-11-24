const MAX_DURATION = 6 * 60 * 60;

function normalizeUrl(value) {
  if (typeof value !== 'string' || value.length > 2048) throw new Error('Incolla un link di YouTube valido.');
  let url;
  try { url = new URL(value.trim()); } catch { throw new Error('Incolla un link di YouTube completo, incluso https://.'); }
  if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password || url.port) throw new Error('Il link di YouTube non è valido.');
  const host = url.hostname.toLowerCase();
  let id;
  if (host === 'youtu.be') id = url.pathname.slice(1).split('/')[0];
  else if (['youtube.com', 'www.youtube.com', 'm.youtube.com', 'music.youtube.com'].includes(host)) {
    if (url.pathname === '/watch') id = url.searchParams.get('v');
    else id = url.pathname.match(/^\/(?:shorts|embed|live)\/([^/]+)\/?$/)?.[1];
  }
  if (!/^[a-zA-Z0-9_-]{11}$/.test(id || '')) throw new Error('Inserisci il link di un singolo video YouTube. Le altre piattaforme non sono ancora supportate.');
  return `https://www.youtube.com/watch?v=${id}`;
}

function summarize(info) {
  if (!info || !/^[\w-]{11}$/.test(info.id || '')) throw new Error('YouTube non ha restituito un video valido.');
  if (info.is_live || info.live_status === 'is_upcoming') throw new Error('Le dirette in corso o programmate non sono supportate.');
  if (!Number.isFinite(info.duration) || info.duration <= 0 || info.duration > MAX_DURATION) throw new Error('Scegli un video con durata massima di 6 ore.');
  const formats = info.formats || [];
  const heights = [...new Set(formats.filter(f => f.vcodec && f.vcodec !== 'none' && f.ext === 'mp4' && f.height).map(f => f.height))].sort((a,b) => b-a);
  const thumbnail = `https://i.ytimg.com/vi/${info.id}/hqdefault.jpg`;
  return { id: info.id, url: normalizeUrl(`https://youtu.be/${info.id}`), title: String(info.title || 'Video YouTube'), channel: String(info.uploader || info.channel || 'YouTube'), duration: info.duration, thumbnail, heights, hasAudio: formats.some(f => f.acodec && f.acodec !== 'none') };
}

function downloadOptions(format, quality) {
  if (!['mp3', 'mp4'].includes(format)) throw new Error('Scegli MP4 oppure MP3.');
  if (format === 'mp3') {
    if (!['320', '256', '192', '128'].includes(String(quality))) throw new Error('Qualità MP3 non valida.');
    return ['-f', 'bestaudio/best', '-x', '--audio-format', 'mp3', '--audio-quality', `${quality}K`, '--embed-metadata'];
  }
  if (quality !== 'best' && !['4320','2160','1440','1080','720','480','360','240','144'].includes(String(quality))) throw new Error('Qualità video non valida.');
  const height = quality === 'best' ? '' : `[height<=${quality}]`;
  return ['-f', `bv[ext=mp4]${height}+ba[ext=m4a]/b[ext=mp4]${height}`, '--merge-output-format', 'mp4', '--embed-metadata', '--postprocessor-args', 'Merger+ffmpeg_o:-movflags +faststart'];
}

function safeFilename(title, extension) {
  const stem = String(title).replace(/[<>:"/\\|?*\u0000-\u001f\u007f]/g, '').replace(/[. ]+$/g, '').trim().slice(0, 140) || 'Flusiva';
  return `${/^(con|prn|aux|nul|com[0-9]|lpt[0-9])(?:\.|$)/i.test(stem) ? '_' : ''}${stem}.${extension}`;
}

function friendlyError(raw) {
  const text = String(raw);
  if (/aborted|cancelled/i.test(text)) return 'Operazione annullata.';
  if (/ENOSPC|No space left/i.test(text)) return 'Spazio su disco insufficiente. Libera spazio e riprova.';
  if (/Sign in|bot|cookies|HTTP Error 403/i.test(text)) return 'YouTube ha bloccato questa richiesta o richiede l’accesso. Riprova più tardi o aggiorna Flusiva.';
  if (/private|unavailable|removed|not available|copyright/i.test(text)) return 'Il video non è disponibile, è privato o ha restrizioni di accesso.';
  if (/timed out|timeout|network|resolve|connection|ENOTFOUND/i.test(text)) return 'Connessione a YouTube non riuscita. Controlla Internet e riprova.';
  if (/Requested format/i.test(text)) return 'Questa qualità MP4 non è disponibile. Scegli un’altra qualità oppure MP3.';
  if (/ENOENT|tools|ffmpeg|ffprobe/i.test(text)) return 'Manca uno strumento di conversione. Reinstalla Flusiva o esegui npm run prepare:tools in sviluppo.';
  return 'Impossibile completare l’operazione. Riprova o controlla se è disponibile una nuova versione di Flusiva.';
}

module.exports = { normalizeUrl, summarize, downloadOptions, safeFilename, friendlyError };
