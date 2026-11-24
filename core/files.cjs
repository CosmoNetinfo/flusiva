const { copyFile }=require('node:fs/promises');
const { constants }=require('node:fs');
const { join,parse }=require('node:path');
const { safeFilename }=require('./youtube.cjs');
async function saveUnique(source,directory,title,format) {
  const {name,ext}=parse(safeFilename(title,format));
  for(let index=0;index<1000;index++) {
    const destination=join(directory,`${name}${index?` (${index})`:''}${ext}`);
    try{await copyFile(source,destination,constants.COPYFILE_EXCL);return destination;}
    catch(error){if(error.code!=='EEXIST')throw error;}
  }
  throw new Error('Troppi file con lo stesso nome. Scegli un’altra cartella.');
}
module.exports={saveUnique};
