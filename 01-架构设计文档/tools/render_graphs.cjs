const fs = require('fs');
const path = require('path');
const vizmod = require('C:/Users/76004/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@viz-js/viz/dist/viz.cjs');
const dir=path.resolve(__dirname,'../images');
const figs=JSON.parse(fs.readFileSync(path.join(dir,'figures.json'),'utf8'));
function q(s){return JSON.stringify(s);}
function width(s,size){return Math.ceil(Array.from(s).reduce((a,c)=>a+(c.charCodeAt(0)>255?size:size*.56),0));}
function esc(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
vizmod.instance().then(viz=>{
  for(const f of figs){
    if(f.type!=='graph')continue;
    const direction=f.id==='3-1-modules'?'LR':'TB';
    const lines=['digraph G {',`graph [rankdir=${direction}, bgcolor="white", pad="0.2", nodesep="0.42", ranksep="0.65", splines=polyline, fontname="Microsoft YaHei"];`,'node [shape=box, style="rounded,filled", fillcolor="#EDF4F7", color="#245268", fontname="Microsoft YaHei", fontsize=16, margin="0.18,0.12"];','edge [color="#50687C", fontname="Microsoft YaHei", fontsize=12, arrowsize=0.7];'];
    for(const [id,label] of f.nodes) {
      const nodeWidth=(Math.max(...label.split('\n').map(x=>width(x,16)))+36)/72;
      lines.push(q(id)+' [label='+q(label)+', width='+nodeWidth.toFixed(3)+'];');
    }
    for(const [a,b,label,type] of f.edges){
      const lab=label?`<<TABLE BORDER="0" CELLBORDER="0" CELLPADDING="2"><TR><TD WIDTH="${width(label,12)+8}" HEIGHT="22">${esc(label)}</TD></TR></TABLE>>`:'""';
      lines.push(q(a)+' -> '+q(b)+' [label='+lab+', style='+q(type==='async'?'dashed':'solid')+'];');
    }
    lines.push('}');
    const dot=lines.join('\n'); fs.writeFileSync(path.join(dir,f.id+'.dot'),dot);
    fs.writeFileSync(path.join(dir,f.id+'.svg'),viz.renderString(dot,{format:'svg',engine:'dot'}));
  }
  console.log('Graphviz SVG generated: '+figs.filter(f=>f.type==='graph').length);
}).catch(e=>{console.error(e);process.exitCode=1;});
