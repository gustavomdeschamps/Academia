import { mkdir, readFile, writeFile, copyFile, readdir, unlink } from 'node:fs/promises';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const web=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const root=path.dirname(web), output=path.join(root,'.build/site');
let sharp;
try{sharp=createRequire(import.meta.url)('sharp');}catch{if(process.env.PULSO_IMAGE_CODEC_DIR)sharp=createRequire(path.join(process.env.PULSO_IMAGE_CODEC_DIR,'package.json'))('sharp');}
const extension=sharp?'.webp':'.png';
await mkdir(output,{recursive:true});
async function copyTree(from,to){await mkdir(to,{recursive:true});for(const entry of await readdir(from,{withFileTypes:true})){const src=path.join(from,entry.name),dst=path.join(to,entry.name);if(entry.isDirectory())await copyTree(src,dst);else await copyFile(src,dst);}}
await copyTree(path.join(web,'src'),path.join(output,'src'));
await copyFile(path.join(web,'index.html'),path.join(output,'index.html'));
await copyFile(path.join(web,'favicon.svg'),path.join(output,'favicon.svg'));
// Somente os nove arquivos de imagem aprovados; nenhum banco, senha local ou captura.
for(const folder of ['imagens','retratos']){
  await mkdir(path.join(output,'assets',folder),{recursive:true});
  for(const entry of await readdir(path.join(root,'assets',folder),{withFileTypes:true})){
    if(!entry.isFile()||!entry.name.endsWith('.png'))throw Error('Imagem inesperada.');
    const source=path.join(root,'assets',folder,entry.name),destination=path.join(output,'assets',folder,entry.name.replace('.png',extension));
    if(sharp){const before=await sharp(source).metadata();await sharp(source).webp({lossless:true,effort:5}).toFile(destination);const after=await sharp(destination).metadata();if(before.width!==after.width||before.height!==after.height)throw Error('A resolução de uma imagem mudou.');}
    else await copyFile(source,destination);
    await unlink(path.join(output,'assets',folder,entry.name.replace('.png',sharp?'.png':'.webp'))).catch(error=>{if(error.code!=='ENOENT')throw error;});
  }
}
if(sharp)for(const file of ['index.html','src/app.js'])await writeFile(path.join(output,file),(await readFile(path.join(output,file),'utf8')).replaceAll('.png','.webp'));
const files=[];async function list(dir){for(const entry of await readdir(dir,{withFileTypes:true})){const file=path.join(dir,entry.name);if(entry.isDirectory())await list(file);else files.push(path.relative(output,file).replaceAll('\\','/'));}}await list(output);
if(files.some(file=>!(/^(src\/|assets\/(imagens|retratos)\/|index\.html$|favicon\.svg$|vercel\.json$)/.test(file))))throw Error('Arquivo inesperado na publicação.');
const headers=JSON.parse(await readFile(path.join(root,'vercel.json'),'utf8')).headers;
await writeFile(path.join(output,'vercel.json'),JSON.stringify({framework:null,headers},null,2));
console.log(`Site preparado: ${new Set([...files,'vercel.json']).size} arquivos. Imagens ${sharp?'WebP sem perda':'PNG originais'}, sem corte ou redução.`);
