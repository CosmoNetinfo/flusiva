import { mkdir, writeFile, copyFile } from 'node:fs/promises';
await mkdir('docs/brand',{recursive:true});
const dot=(x,y,r=4.5,color='#fff')=>`<circle cx="${x}" cy="${y}" r="${r}" fill="${color}"/>`;
const f = [[20,19],[32,19],[20,31],[32,31],[20,43]].map(([x,y])=>dot(x,y)).join('')+dot(44,19,4.5,'#75d1ff')+dot(44,43,2.5,'#75d1ff');
const wave = [[18,26],[18,38],[32,14],[32,26],[32,38],[32,50],[46,26],[46,38]].map(([x,y],i)=>dot(x,y,4,i>5?'#75d1ff':'#fff')).join('');
const arrow = [[32,14],[32,26],[32,38],[20,32],[44,32],[20,50],[32,50],[44,50]].map(([x,y],i)=>dot(x,y,4,i>4?'#75d1ff':'#fff')).join('');
const mark=(dots,size,x=0,y=0)=>`<g transform="translate(${x} ${y}) scale(${size/64})"><rect width="64" height="64" rx="18" fill="#1761e8"/>${dots}</g>`;
const svg=(w,h,body)=>`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${body}</svg>`;
await copyFile('public/mark.svg','docs/brand/flusiva-symbol.svg');
await writeFile('docs/brand/flusiva-wordmark.svg',svg(310,76,mark(f,64,4,6)+'<text x="84" y="55" font-family="Segoe UI,Arial,sans-serif" font-size="49" font-weight="650" letter-spacing="-2" fill="#11223d">flusiva<tspan fill="#1761e8">.</tspan></text>'));
await writeFile('docs/brand/flusiva-wordmark-white.svg',svg(310,76,mark(f,64,4,6)+'<text x="84" y="55" font-family="Segoe UI,Arial,sans-serif" font-size="49" font-weight="650" letter-spacing="-2" fill="#fff">flusiva<tspan fill="#75d1ff">.</tspan></text>'));
let body='<rect width="1440" height="1000" fill="#f7faff"/>';
const text=(x,y,value,size=18,color='#5b6980',weight=400)=>`<text x="${x}" y="${y}" font-family="Segoe UI,Arial,sans-serif" font-size="${size}" font-weight="${weight}" fill="${color}">${value}</text>`;
body+=text(64,70,'FLUSIVA / STUDIO DEL MARCHIO',14,'#1761e8',600)+text(64,136,'Un segno fatto di piccoli flussi.',44,'#11223d',600)+text(64,175,'Tre direzioni per un’app desktop. Blu, azzurro e bianco. Un simbolo che funziona anche a 16 pixel.',18);
const items=[{x:64,name:'A · Flusso',dots:f,tag:'Direzione scelta',description:'La F rende riconoscibile il nome.',second:'Il punto azzurro apre la sequenza.'},{x:512,name:'B · Traccia',dots:wave,tag:'Alternativa',description:'Una forma vicina al suono.',second:'Meno distintiva per un’app video.'},{x:960,name:'C · Download',dots:arrow,tag:'Alternativa',description:'L’azione è immediatamente leggibile.',second:'Più generica come marchio.'}];
for(const item of items){body+=`<rect x="${item.x}" y="220" width="416" height="380" rx="20" fill="#fff" stroke="${item.x===64?'#1761e8':'#dce5f1'}"/>`+mark(item.dots,112,item.x+32,252)+text(item.x+32,415,item.name,26,'#11223d',600)+text(item.x+32,451,item.tag,14,'#1761e8',600)+text(item.x+32,501,item.description,17)+text(item.x+32,531,item.second,17);}
body+=`<rect x="64" y="632" width="864" height="300" rx="20" fill="#11223d"/>`+mark(f,88,96,668)+text(208,731,'flusiva.',64,'#fff',650)+text(96,804,'Simbolo, wordmark e icona di sistema',18,'#b5d5ff')+text(96,838,'64 px',13,'#b5d5ff')+mark(f,64,96,852)+text(192,838,'32 px',13,'#b5d5ff')+mark(f,32,192,852)+text(264,838,'16 px',13,'#b5d5ff')+mark(f,16,264,852);
body+=text(968,679,'La palette',22,'#11223d',600);
for(const [i,color,name] of [[0,'#1761e8','Blu / azione'],[1,'#75d1ff','Azzurro / dettaglio'],[2,'#ffffff','Bianco / spazio']]){body+=`<rect x="968" y="${704+i*67}" width="44" height="44" rx="12" fill="${color}" stroke="#dce5f1"/>`+text(1030,726+i*67,name,16,'#11223d',600)+text(1030,746+i*67,color.toUpperCase(),13);}
await writeFile('docs/brand/logo-study.svg',svg(1440,1000,body));
await writeFile('docs/brand/study.html','<!doctype html><html lang="it"><meta charset="utf-8"><title>Flusiva — Studio del logo</title><body style="margin:0;overflow:hidden"><img style="display:block" src="./logo-study.svg" width="1440" height="1000" alt="Tre proposte per il logo Flusiva: Flusso, Traccia e Download"></body></html>');
console.log('Studio del logo e varianti SVG pronti.');
