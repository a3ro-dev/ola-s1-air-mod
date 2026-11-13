// 4x4 Bayer ordered dither. <canvas data-dither="x|y|r" data-from="0" data-to="1" data-color="#3DA5FF" data-cell="3">
const B=[0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5];
function paint(c){
  const cell=+c.dataset.cell||3,col=c.dataset.color||'#3DA5FF',dir=c.dataset.dither||'x';
  const a=+(c.dataset.from??0),b=+(c.dataset.to??1),p=c.dataset.t?+c.dataset.t:1;
  const w=c.clientWidth,h=c.clientHeight;if(!w||!h)return;
  c.width=Math.ceil(w/cell);c.height=Math.ceil(h/cell);
  const g=c.getContext('2d');g.fillStyle=col;
  for(let y=0;y<c.height;y++)for(let x=0;x<c.width;x++){
    let t=dir==='y'?y/c.height:dir==='r'?Math.hypot(x/c.width-.5,y/c.height-.5)*1.6:x/c.width;
    const d=(a+(b-a)*t)*p;
    if(d>(B[(y&3)*4+(x&3)]+.5)/16)g.fillRect(x,y,1,1);
  }
}
const run=()=>document.querySelectorAll('canvas[data-dither]').forEach(paint);
addEventListener('load',run);addEventListener('resize',run);
export{paint};
