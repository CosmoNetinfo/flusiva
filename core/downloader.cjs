const { spawn } = require('node:child_process');
const { join } = require('node:path');
const { mkdtemp, readdir, rm, stat } = require('node:fs/promises');
const { identify,resolveLink,summarize,downloadOptions,friendlyError } = require('./platforms.cjs');
const {PROGRESS_TEMPLATE,parseProgress}=require('./progress.cjs');

function stopChild(child) {
  if (!child.pid || child.exitCode !== null) return;
  if (process.platform === 'win32') {
    // Only the process tree created by this operation.
    spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' }).on('error', () => child.kill()).on('exit', code => { if (code !== 0) child.kill(); });
  } else {
    try { process.kill(-child.pid,'SIGTERM'); } catch { child.kill('SIGTERM'); }
  }
  const force = setTimeout(() => { if (child.exitCode === null) { try { process.platform !== 'win32' ? process.kill(-child.pid,'SIGKILL') : child.kill('SIGKILL'); } catch {} } },3000);
  force.unref();
}

function runTool(executable, args, { signal, onLine = () => {}, timeout = 90_000, env = process.env } = {}) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(new Error('cancelled'));
    const child = spawn(executable, args, { windowsHide: true, shell: false, detached: process.platform !== 'win32', env, stdio: ['ignore','pipe','pipe'] });
    let output = '', errors = '', pending = '', failure;
    const abort = () => { failure = new Error('cancelled'); stopChild(child); };
    const timer = setTimeout(() => { failure = new Error('timeout'); stopChild(child); }, timeout);
    signal?.addEventListener('abort', abort, { once: true });
    child.stdout.on('data', chunk => {
      const str = chunk.toString();
      output += str;
      if (output.length > 16 * 1024 * 1024) { failure = new Error('Output troppo grande'); stopChild(child); }
      pending += str;
      const lines = pending.split(/\r?\n/); pending = lines.pop();
      lines.forEach(onLine);
    });
    child.stderr.on('data', chunk => { errors = (errors + chunk).slice(-16_384); });
    const clean = () => { clearTimeout(timer); signal?.removeEventListener('abort', abort); };
    child.on('error', error => { clean(); reject(error); });
    child.on('close', code => { clean(); if (pending) onLine(pending); failure || code !== 0 ? reject(failure || new Error(errors || `Exit ${code}`)) : resolve(output); });
  });
}

function createDownloader({ toolsDir, tempRoot, nodePath = process.execPath, runner = runTool }) {
  const yt = join(toolsDir, process.platform === 'win32' ? 'yt-dlp.exe' : 'yt-dlp');
  const common = ['--ignore-config','--no-playlist','--no-cache-dir','--no-warnings','--socket-timeout','25','--retries','3','--ffmpeg-location',toolsDir,'--js-runtimes',`node:${nodePath}`];
  const env = { ...process.env, ELECTRON_RUN_AS_NODE: '1' };
  return {
    async inspect(value, signal) {
      const item=await resolveLink(value,{signal});
      let info;
      try { info=JSON.parse(await runner(yt,[...common,'--dump-single-json','--skip-download','--',item.url],{signal,env})); }
      catch(error){throw new Error(friendlyError(error.message,item.platform));}
      return summarize(info,item);
    },
    async download(info, format, quality, { signal, onProgress = () => {} } = {}) {
      const item=identify(info.url);
      if(item.short)throw new Error('Analizza prima il link completo del contenuto.');
      const url=item.url;
      const options = downloadOptions(format, quality,info);
      const directory = await mkdtemp(join(tempRoot, 'flusiva-'));
      try {
        await runner(yt, [...common,...options,'--newline','--progress','--progress-delta','0.5','--progress-template',PROGRESS_TEMPLATE,'--max-filesize','8G','-o',join(directory,'media.%(ext)s'),'--',url], {
          env, signal, timeout: 2 * 60 * 60 * 1000,
          onLine(line) {
            const progress=parseProgress(line);
            if(progress)onProgress(progress);
            else if (/\[(ExtractAudio|Merger|Metadata|VideoRemuxer)\]/.test(line)) onProgress({ phase: 'conversion', percent: null });
          }
        });
        if (signal?.aborted) throw new Error('cancelled');
        const names = await readdir(directory);
        const filename = names.find(name => name === `media.${format}`);
        if (!filename || !(await stat(join(directory,filename))).size) throw new Error('Nessun file prodotto');
        return { path: join(directory,filename), cleanup: () => rm(directory,{ recursive: true, force: true }) };
      } catch (error) {
        await rm(directory, { recursive: true, force: true });
        throw new Error(friendlyError(error.message,info.platformName||item.platform));
      }
    }
  };
}

module.exports = { createDownloader, runTool };
