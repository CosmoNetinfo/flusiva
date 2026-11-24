// Original Flusiva mark. Generate a PNG and a PNG-backed ICO without extra packages.
import { mkdir, writeFile } from 'node:fs/promises';
import { deflateSync } from 'node:zlib';
const n = 512, image = Buffer.alloc((n * 4 + 1) * n);
function crc32(data) { let crc = 0xffffffff; for (const byte of data) { crc ^= byte; for (let bit=0;bit<8;bit++) crc = (crc>>>1)^((crc&1)?0xedb88320:0); } return (crc^0xffffffff)>>>0; }
function chunk(name,data) { const type = Buffer.from(name); const len=Buffer.alloc(4);len.writeUInt32BE(data.length);const crc=Buffer.alloc(4);crc.writeUInt32BE(crc32(Buffer.concat([type,data])));return Buffer.concat([len,type,data,crc]); }
function segment(x,y,ax,ay,bx,by) { const dx=bx-ax,dy=by-ay;const t=Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)));return Math.hypot(x-ax-t*dx,y-ay-t*dy); }
for(let y=0;y<n;y++) for(let x=0;x<n;x++) {
  const px=x/8,py=y/8;const cx=Math.max(19,Math.min(45,px)),cy=Math.max(19,Math.min(45,py));
  const inside=Math.hypot(px-cx,py-cy)<=19;
  const curved=px<=31&&py<=26&&Math.abs(Math.hypot(px-31,py-26)-9)<=3;
  const white=segment(px,py,22,26,22,45)<=3||segment(px,py,31,17,44,17)<=3||segment(px,py,22,31,39,31)<=3||curved;
  const offset=y*(n*4+1)+1+x*4;
  image[offset]=white?255:31;image[offset+1]=white?255:107;image[offset+2]=white?255:58;image[offset+3]=inside?255:0;
}
const header=Buffer.alloc(13);header.writeUInt32BE(n,0);header.writeUInt32BE(n,4);header[8]=8;header[9]=6;
const png=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(image)),chunk('IEND',Buffer.alloc(0))]);
await mkdir('build',{recursive:true});await writeFile('build/icon.png',png);
// Windows accepts PNG-backed ICO images at 256px; build a second exact-size PNG.
const small=Buffer.alloc((256*4+1)*256);
for(let y=0;y<256;y++)for(let x=0;x<256;x++)image.copy(small,y*1025+1+x*4,(y*2)*2049+1+x*8,(y*2)*2049+1+x*8+4);
header.writeUInt32BE(256,0);header.writeUInt32BE(256,4);
const iconPng=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(small)),chunk('IEND',Buffer.alloc(0))]);
const ico=Buffer.alloc(22);ico.writeUInt16LE(1,2);ico.writeUInt16LE(1,4);ico.writeUInt16LE(1,10);ico.writeUInt16LE(32,12);ico.writeUInt32LE(iconPng.length,14);ico.writeUInt32LE(22,18);
await writeFile('build/icon.ico',Buffer.concat([ico,iconPng]));
