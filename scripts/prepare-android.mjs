import {mkdir,readFile,writeFile,copyFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {join} from 'node:path';
const directory=new URL('../vendor-android/',import.meta.url);
await mkdir(directory,{recursive:true});
await copyFile(new URL('../extractor-plugins/flusiva/yt_dlp_plugins/extractor/flusiva.py',import.meta.url),new URL('flusiva.py',directory));
const sdk=process.env.ANDROID_HOME||process.env.ANDROID_SDK_ROOT||(process.platform==='win32'?join(process.env.LOCALAPPDATA,'Android','Sdk'):null);
if(!sdk)throw new Error('Configura ANDROID_HOME e installa NDK 27.0.12077973.');
const host=process.platform==='win32'?'windows-x86_64':process.platform==='darwin'?'darwin-x86_64':'linux-x86_64';
const ndk=join(sdk,'ndk','27.0.12077973','toolchains','llvm','prebuilt',host);
for(const [abi,triple]of [['arm64-v8a','aarch64-linux-android'],['armeabi-v7a','arm-linux-androideabi'],['x86_64','x86_64-linux-android']]){
  const target=new URL(`../vendor-android-jni/${abi}/`,import.meta.url);await mkdir(target,{recursive:true});
  await copyFile(join(ndk,'sysroot','usr','lib',triple,'libc++_shared.so'),new URL('libc++_shared.so',target));
}
await copyFile(join(ndk,'NOTICE'),new URL('NDK-NOTICE.txt',directory));
const version='2026.11.23',sha256='e8665ed64d6b9172773e812f891fb707a0cb1ee8d9ef4fc1acb00cabee4c5256';
const destination=new URL('yt-dlp',directory);
let bytes;try{bytes=await readFile(destination);}catch{}
if(!bytes||createHash('sha256').update(bytes).digest('hex')!==sha256){
  let error;
  for(let attempt=0;attempt<3;attempt++)try{
    const response=await fetch(`https://github.com/yt-dlp/yt-dlp/releases/download/${version}/yt-dlp`,{signal:AbortSignal.timeout(180000)});
    if(!response.ok)throw new Error(`Download yt-dlp Android: HTTP ${response.status}`);
    bytes=Buffer.from(await response.arrayBuffer());
    if(createHash('sha256').update(bytes).digest('hex')!==sha256)throw new Error('Checksum yt-dlp Android non corrispondente.');
    await writeFile(destination,bytes);error=null;break;
  }catch(failure){error=failure;}
  if(error)throw error;
}
await writeFile(new URL('engine-info.json',directory),JSON.stringify({version,sha256}));
await copyFile(new URL('../LICENSE',import.meta.url),new URL('LICENSE.txt',directory));
const readme=await readFile(new URL('../README.md',import.meta.url),'utf8');
const responsible=readme.split('## Uso responsabile')[1].split('## Licenza')[0].trim();
await writeFile(new URL('NOTICE.txt',directory),responsible+'\n\nFlusiva: GPL-3.0-only. Il testo integrale è incluso nell’APK in LICENSE.txt.\n\n'+await readFile(new URL('../THIRD_PARTY_NOTICES.md',import.meta.url),'utf8'));
console.log(`Motore Android ${version} preparato e verificato SHA-256.`);
