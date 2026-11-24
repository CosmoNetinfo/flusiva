import { execFileSync } from 'node:child_process';
import { readdir, readFile } from 'node:fs/promises';
for (const directory of ['desktop','core','public','scripts','tests']) {
  for (const name of await readdir(directory)) {
    if (/\.(?:cjs|mjs|js)$/.test(name)) execFileSync(process.execPath,['--check',`${directory}/${name}`],{stdio:'inherit'});
  }
}
const html = await readFile('public/index.html','utf8');
if (!html.includes("script-src 'self'") || !html.includes("connect-src 'none'")) throw new Error('CSP mancante.');
const main = await readFile('desktop/main.cjs','utf8');
if (!main.includes('contextIsolation: true') || !main.includes('sandbox: true') || !main.includes('nodeIntegration: false')) throw new Error('Isolamento Electron mancante.');
console.log('Sintassi e configurazione di sicurezza verificate.');
