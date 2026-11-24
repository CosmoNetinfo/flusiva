// Optional manual integration check against Blender's Creative Commons short film.
const { createDownloader } = require('../core/downloader.cjs');
const { join } = require('node:path');
const { tmpdir } = require('node:os');
const { execFileSync } = require('node:child_process');
const { stat } = require('node:fs/promises');
async function main() {
  const tools = join(__dirname,'../vendor');
  const exe = process.platform === 'win32' ? '.exe' : '';
  const electron = require('electron');
  const downloader = createDownloader({toolsDir:tools,tempRoot:tmpdir(),nodePath:electron});
  const info = await downloader.inspect(process.argv[2] || 'https://www.youtube.com/watch?v=aqz-KE-bpKQ');
  console.log(`Video test: ${info.title}, ${info.duration}s`);
  for (const [format,quality] of [['mp4','360'],['mp3','320']]) {
    const result = await downloader.download(info,format,quality);
    try {
      const output = JSON.parse(execFileSync(join(tools,`ffprobe${exe}`),['-v','quiet','-show_streams','-show_format','-of','json',result.path],{encoding:'utf8',windowsHide:true}));
      if (!output.streams.some(stream=>stream.codec_type==='audio') || (format==='mp4' && !output.streams.some(stream=>stream.codec_type==='video'))) throw new Error(`Tracce ${format} mancanti`);
      console.log(`${format.toUpperCase()} PASS: ${(await stat(result.path)).size} byte, ${output.streams.map(stream=>stream.codec_name).join('+')}`);
    } finally { await result.cleanup(); }
  }
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
