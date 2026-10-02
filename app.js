const $=id=>document.getElementById(id);
let recorder,chunks=[],stream,startTime,timerInt,recognition,finalText="",paused=false;
const SpeechRecognition=window.SpeechRecognition||window.webkitSpeechRecognition;
function toast(m){$("toast").textContent=m;$("toast").classList.add("show");setTimeout(()=>$("toast").classList.remove("show"),1800)}
function updateWords(){const n=$("text").value.trim()?$("text").value.trim().split(/\s+/).length:0;$("words").textContent=n+" "+(n===1?"palabra":"palabras")}
function setStatus(t,on=false){$("status").lastElementChild.textContent=t;$("status").classList.toggle("recording",on)}
function clock(){const s=Math.floor((Date.now()-startTime)/1000),m=Math.floor(s/60);$("timer").textContent=String(m).padStart(2,"0")+":"+String(s%60).padStart(2,"0")}
function startRecognition(){if(!SpeechRecognition)return; recognition=new SpeechRecognition();recognition.lang="es-ES";recognition.continuous=true;recognition.interimResults=true;
recognition.onresult=e=>{let interim="";for(let i=e.resultIndex;i<e.results.length;i++){const t=e.results[i][0].transcript;if(e.results[i].isFinal)finalText+=t.trim()+" ";else interim+=t}$("text").value=(finalText+interim).trim();updateWords()};
recognition.onend=()=>{if(recorder&&recorder.state!=="inactive"&&!paused){try{recognition.start()}catch{}}};try{recognition.start()}catch{}}
$("record").onclick=async()=>{try{stream=await navigator.mediaDevices.getUserMedia({audio:true});chunks=[];finalText=$("text").value.trim();if(finalText)finalText+=" ";recorder=new MediaRecorder(stream);recorder.ondataavailable=e=>{if(e.data.size)chunks.push(e.data)};recorder.onstop=()=>{const blob=new Blob(chunks,{type:recorder.mimeType||"audio/webm"});$("audio").src=URL.createObjectURL(blob);$("audio").hidden=false;stream.getTracks().forEach(t=>t.stop())};recorder.start();startTime=Date.now();clock();timerInt=setInterval(clock,1000);startRecognition();$("record").disabled=true;$("pause").disabled=false;$("stop").disabled=false;setStatus("Grabando…",true)}catch(e){toast("No se pudo acceder al micrófono")}};
$("pause").onclick=()=>{if(!recorder)return;if(recorder.state==="recording"){recorder.pause();paused=true;recognition?.stop();$("pause").textContent="▶ Reanudar";setStatus("Grabación en pausa")}else{recorder.resume();paused=false;startRecognition();$("pause").textContent="⏸ Pausar";setStatus("Grabando…",true)}};
$("stop").onclick=()=>{if(!recorder)return;recorder.stop();recognition?.stop();clearInterval(timerInt);$("record").disabled=false;$("pause").disabled=true;$("stop").disabled=true;$("pause").textContent="⏸ Pausar";setStatus("Grabación finalizada");toast("Grabación guardada en esta sesión")};
$("text").oninput=updateWords;$("copy").onclick=async()=>{await navigator.clipboard.writeText($("text").value);toast("Texto copiado")};
$("downloadText").onclick=()=>{const a=document.createElement("a");a.href=URL.createObjectURL(new Blob([$("text").value],{type:"text/plain;charset=utf-8"}));a.download="transcripcion-"+new Date().toISOString().slice(0,10)+".txt";a.click();toast("Texto descargado")};
$("clear").onclick=()=>{if(confirm("¿Borrar la transcripción?")){$("text").value="";finalText="";updateWords()}};
if(!SpeechRecognition)setStatus("Tu navegador puede grabar, pero no admite transcripción en directo");
if("serviceWorker"in navigator)navigator.serviceWorker.register("sw.js");