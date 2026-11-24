import { createRequire } from 'node:module';
import { mkdir, writeFile, readFile, copyFile, chmod, readdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
const require = createRequire(import.meta.url);
const root = fileURLToPath(new URL('../',import.meta.url));
const vendor = join(root,'vendor');
const release = '2026.11.23';
const assets = {
  'win32-x64': ['yt-dlp.exe','622ed48023cac1daa52a3bf3a6ac38f03514de214b5f0fa069389fd0f6d17e5c'],
  'linux-x64': ['yt-dlp_linux','249ddb058234276bc449b9cd9492aad9f133bda5923c61a9e39983ae114ef17d'],
  'darwin-x64': ['yt-dlp_macos','57654854886d0398a798895aadf5173e2749fbb5e249b9f3728022ab376e11fb'],
  'darwin-arm64': ['yt-dlp_macos','57654854886d0398a798895aadf5173e2749fbb5e249b9f3728022ab376e11fb']
};
const entry = assets[`${process.platform}-${process.arch}`];
if (!entry) throw new Error('Piattaforma non supportata: usare Windows/Linux x64 oppure macOS x64/arm64.');
await mkdir(vendor,{ recursive:true });
const exe = process.platform === 'win32' ? '.exe' : '';
const ytPath = join(vendor,`yt-dlp${exe}`);
let downloaded;
try { downloaded = await readFile(ytPath); } catch { /* First build. */ }
if (!downloaded || createHash('sha256').update(downloaded).digest('hex') !== entry[1]) {
  console.log(`Download yt-dlp ${release} (${entry[0]})…`);
  const response = await fetch(`https://github.com/yt-dlp/yt-dlp/releases/download/${release}/${entry[0]}`,{ signal:AbortSignal.timeout(180_000) });
  if (!response.ok) throw new Error(`Download yt-dlp: HTTP ${response.status}`);
  downloaded = Buffer.from(await response.arrayBuffer());
  if (createHash('sha256').update(downloaded).digest('hex') !== entry[1]) throw new Error('Checksum yt-dlp non corrispondente.');
  await writeFile(ytPath,downloaded);
}
const ffmpegPath = require('ffmpeg-static');
try { await readFile(ffmpegPath); } catch {
  execFileSync(process.execPath,[join(dirname(require.resolve('ffmpeg-static')),'install.js')],{stdio:'inherit',timeout:240_000});
}
const ffprobe = require('@ffprobe-installer/ffprobe');
await copyFile(ffmpegPath,join(vendor,`ffmpeg${exe}`));
await copyFile(ffprobe.path,join(vendor,`ffprobe${exe}`));
const licenses = join(vendor,'licenses');
await mkdir(licenses,{ recursive:true });
for (const [label, directory] of [['ffmpeg',dirname(ffmpegPath)],['ffprobe',dirname(ffprobe.path)]]) {
  for (const name of await readdir(directory)) {
    if (/license|copying|readme/i.test(name)) await copyFile(join(directory,name),join(licenses,`${label}-${name}`));
  }
}
const licenseResponse = await fetch(`https://raw.githubusercontent.com/yt-dlp/yt-dlp/${release}/THIRD_PARTY_LICENSES.txt`,{ signal:AbortSignal.timeout(60_000) });
if (!licenseResponse.ok) throw new Error('Impossibile includere le licenze di yt-dlp.');
await writeFile(join(licenses,'yt-dlp-THIRD_PARTY_LICENSES.txt'),await licenseResponse.text());
await copyFile(join(root,'THIRD_PARTY_NOTICES.md'),join(licenses,'THIRD_PARTY_NOTICES.md'));
const versions = { platform:process.platform,arch:process.arch,ytDlp:release };
await copyFile(join(dirname(ffmpegPath),'LICENSE'),join(licenses,'GPL-3.0.txt'));
for (const name of [`yt-dlp${exe}`,`ffmpeg${exe}`,`ffprobe${exe}`]) {
  if (!exe) await chmod(join(vendor,name),0o755);
  const args = name.startsWith('yt-dlp') ? ['--version'] : ['-version'];
  const output = execFileSync(join(vendor,name),args,{ encoding:'utf8',windowsHide:true,timeout:30_000,env:{...process.env,ELECTRON_RUN_AS_NODE:'1'} });
  console.log(`${name}: ${output.split(/\r?\n/)[0]}`);
  versions[name.replace('.exe','')] = output.split(/\r?\n/)[0];
}
await writeFile(join(vendor,'versions.json'),JSON.stringify(versions,null,2));
console.log('Strumenti pronti.');
