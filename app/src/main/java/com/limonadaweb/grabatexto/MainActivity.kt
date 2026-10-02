package com.limonadaweb.grabatexto
import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView;private lateinit var timer:TextView;private lateinit var start:Button;private lateinit var stop:Button;private lateinit var container:LinearLayout;private lateinit var home:View;private lateinit var recordings:View
 private val handler=Handler(Looper.getMainLooper());private var player:MediaPlayer?=null
 private val tick=object:Runnable{override fun run(){refreshState();handler.postDelayed(this,500)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
  val bottom=findViewById<View>(R.id.bottomBar);ViewCompat.setOnApplyWindowInsetsListener(bottom){v,i->val nav=i.getInsets(WindowInsetsCompat.Type.navigationBars());v.setPadding(v.paddingLeft,v.paddingTop,v.paddingRight,nav.bottom+dp(7));i};ViewCompat.requestApplyInsets(bottom)
  status=findViewById(R.id.status);timer=findViewById(R.id.timer);start=findViewById(R.id.startButton);stop=findViewById(R.id.stopButton);container=findViewById(R.id.recordingsContainer);home=findViewById(R.id.homeScreen);recordings=findViewById(R.id.recordingsScreen)
  findViewById<Button>(R.id.homeTab).setOnClickListener{home.visibility=View.VISIBLE;recordings.visibility=View.GONE};findViewById<Button>(R.id.recordingsTab).setOnClickListener{home.visibility=View.GONE;recordings.visibility=View.VISIBLE;refreshList()}
  start.setOnClickListener{begin()};stop.setOnClickListener{finishRecording()};refreshList()
 }
 override fun onResume(){super.onResume();handler.post(tick)}
 override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
 override fun onDestroy(){player?.release();super.onDestroy()}
 private fun begin(){
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),44);return}
  launchRecorder()
 }
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==44){if(g.isNotEmpty()&&g[0]==PackageManager.PERMISSION_GRANTED)launchRecorder()else{status.text="Permiso de micrófono denegado";Toast.makeText(this,"Activa el permiso de micrófono para poder grabar",Toast.LENGTH_LONG).show()}}}
 private fun launchRecorder(){try{start.isEnabled=false;status.text="Iniciando grabación…";ContextCompat.startForegroundService(this,Intent(this,RecordingService::class.java).setAction(RecordingService.ACTION_START));handler.postDelayed({val active=getSharedPreferences("state",MODE_PRIVATE).getBoolean("recording",false);refreshState();if(!active){start.isEnabled=true;status.text="No se pudo iniciar";val err=getSharedPreferences("state",MODE_PRIVATE).getString("error","Error desconocido"); status.text="Fallo: "+err; Toast.makeText(this,"No pudo grabar: "+err,Toast.LENGTH_LONG).show()}},900)}catch(e:Exception){start.isEnabled=true;status.text="Error al iniciar";Toast.makeText(this,e.javaClass.simpleName+": "+(e.message?:"error de micrófono"),Toast.LENGTH_LONG).show()}}
 private fun finishRecording(){stop.isEnabled=false;status.text="Guardando…";stopService(Intent(this,RecordingService::class.java));handler.postDelayed({refreshState();refreshList();home.visibility=View.GONE;recordings.visibility=View.VISIBLE},700)}
 private fun refreshState(){val s=getSharedPreferences("state",MODE_PRIVATE);val active=s.getBoolean("recording",false);start.isEnabled=!active;stop.isEnabled=active;status.text=if(active)"🔴 GRABANDO" else if(status.text=="Guardando…")"Listo para grabar" else if(!status.text.toString().startsWith("Error")&&!status.text.toString().startsWith("No se")&&!status.text.toString().startsWith("Fallo"))"Listo para grabar" else status.text;if(active){val sec=(System.currentTimeMillis()-s.getLong("started",System.currentTimeMillis()))/1000;timer.text=String.format(Locale.getDefault(),"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60)}else timer.text="00:00:00"}
 private fun refreshList(){container.removeAllViews();val fs=File(filesDir,"recordings").listFiles{f->f.extension=="m4a"}?.sortedByDescending{it.lastModified()}?:emptyList();if(fs.isEmpty()){container.addView(label("Todavía no hay grabaciones.",15));return};fs.forEach{file->val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));setBackgroundColor(0xFFFFFFFF.toInt())};card.addView(label("🎙 "+file.nameWithoutExtension,17,true));card.addView(label(SimpleDateFormat("dd/MM/yyyy · HH:mm",Locale.getDefault()).format(Date(file.lastModified()))+" · "+human(file.length()),13));val r1=row();button(r1,"▶ Oír"){play(file)};button(r1,"✎ Nombre"){rename(file)};val r2=row();button(r2,"↗ Compartir"){share(file)};button(r2,"🗑 Borrar"){delete(file)};card.addView(r1);card.addView(r2);container.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})}}
 private fun play(f:File){player?.release();player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start();setOnCompletionListener{it.release();player=null}}}
 private fun rename(f:File){val i=EditText(this).apply{setText(f.nameWithoutExtension);selectAll()};AlertDialog.Builder(this).setTitle("Renombrar").setView(i).setNegativeButton("Cancelar",null).setPositiveButton("Guardar"){_,_->val n=i.text.toString().trim().replace(Regex("[\\/:*?\"<>|]"),"_");if(n.isNotEmpty()){f.renameTo(File(f.parentFile,n+".m4a"));refreshList()}}.show()}
 private fun share(f:File){val u=FileProvider.getUriForFile(this,packageName+".files",f);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="audio/mp4";putExtra(Intent.EXTRA_STREAM,u);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartir grabación"))}
 private fun delete(f:File){AlertDialog.Builder(this).setTitle("Eliminar grabación").setMessage("¿Seguro que quieres borrarla?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->f.delete();refreshList()}.show()}
 private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(6),0,0)}
 private fun button(r:LinearLayout,t:String,a:()->Unit){r.addView(Button(this).apply{text=t;textSize=13f;setOnClickListener{a()}},LinearLayout.LayoutParams(0,dp(52),1f))}
 private fun label(v:String,s:Int,b:Boolean=false)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(if(b)0xFF0F172A.toInt()else 0xFF64748B.toInt());if(b)setTypeface(typeface,1);setPadding(0,dp(3),0,dp(3))}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun human(n:Long)=if(n<1048576)(n/1024).toString()+" KB" else String.format(Locale.getDefault(),"%.1f MB",n/1048576.0)
}
