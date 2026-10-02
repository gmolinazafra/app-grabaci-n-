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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView;private lateinit var timer:TextView;private lateinit var start:Button;private lateinit var stop:Button;private lateinit var container:LinearLayout
 private val handler=Handler(Looper.getMainLooper()); private var player:MediaPlayer?=null
 private val tick=object:Runnable{override fun run(){refreshState();handler.postDelayed(this,1000)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);status=findViewById(R.id.status);timer=findViewById(R.id.timer);start=findViewById(R.id.startButton);stop=findViewById(R.id.stopButton);container=findViewById(R.id.recordingsContainer);start.setOnClickListener{ensurePermissionAndStart()};stop.setOnClickListener{stopRecording()};refreshList()}
 override fun onResume(){super.onResume();handler.post(tick);refreshList()}
 override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
 override fun onDestroy(){player?.release();super.onDestroy()}
 private fun ensurePermissionAndStart(){val p=mutableListOf(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33)p+=Manifest.permission.POST_NOTIFICATIONS;if(p.any{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED})ActivityCompat.requestPermissions(this,p.toTypedArray(),10)else startRecording()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startRecording()}
 private fun startRecording(){ContextCompat.startForegroundService(this,Intent(this,RecordingService::class.java).setAction(RecordingService.ACTION_START));Handler(Looper.getMainLooper()).postDelayed({refreshState()},250)}
 private fun stopRecording(){
  stop.isEnabled=false
  status.text="Finalizando grabación…"
  stopService(Intent(this,RecordingService::class.java))
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",false).remove("started").apply()
  Handler(Looper.getMainLooper()).postDelayed({refreshState();refreshList();Toast.makeText(this,"Grabación guardada",Toast.LENGTH_SHORT).show()},500)
 }
 private fun refreshState(){val s=getSharedPreferences("state",MODE_PRIVATE);val active=s.getBoolean("recording",false);start.isEnabled=!active;stop.isEnabled=active;status.text=if(active)"🔴 Grabando en segundo plano" else "Listo para grabar";if(active){val sec=(System.currentTimeMillis()-s.getLong("started",System.currentTimeMillis()))/1000;timer.text=String.format(Locale.getDefault(),"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60)}else timer.text="00:00:00"}
 private fun refreshList(){
  container.removeAllViews();val files=File(filesDir,"recordings").listFiles()?.sortedByDescending{it.lastModified()}?:emptyList()
  if(files.isEmpty()){container.addView(text("Todavía no hay grabaciones.",14));return}
  files.forEach{file->
   val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));setBackgroundColor(0xFFFFFFFF.toInt())}
   val title=text("🎙  "+file.nameWithoutExtension,17).apply{setTextColor(0xFF0F172A.toInt());setTypeface(typeface,1)}
   val info=text(SimpleDateFormat("dd/MM/yyyy · HH:mm",Locale.getDefault()).format(Date(file.lastModified()))+"  ·  "+human(file.length()),13)
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(10),0,0)}
   fun btn(label:String,action:()->Unit)=Button(this).apply{text=label;textSize=12f;setOnClickListener{action()};row.addView(this,LinearLayout.LayoutParams(0,dp(52),1f))}
   btn("▶ Oír"){play(file)};btn("✎ Nombre"){rename(file)};btn("↗ Compartir"){share(file)};btn("🗑 Borrar"){delete(file)}
   card.addView(title);card.addView(info);card.addView(row);container.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
  }
 }
 private fun play(file:File){player?.release();player=MediaPlayer().apply{setDataSource(file.absolutePath);prepare();start();Toast.makeText(this@MainActivity,"Reproduciendo "+file.nameWithoutExtension,Toast.LENGTH_SHORT).show();setOnCompletionListener{it.release();player=null}}}
 private fun rename(file:File){val input=EditText(this).apply{setText(file.nameWithoutExtension);selectAll()};AlertDialog.Builder(this).setTitle("Renombrar grabación").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar"){_,_->val name=input.text.toString().trim().replace(Regex("[\\/:*?\"<>|]"),"_");if(name.isNotEmpty()){file.renameTo(File(file.parentFile,name+".m4a"));refreshList()}}.show()}
 private fun share(file:File){val uri=FileProvider.getUriForFile(this,packageName+".files",file);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="audio/mp4";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartir grabación"))}
 private fun delete(file:File){AlertDialog.Builder(this).setTitle("Eliminar grabación").setMessage("¿Seguro que quieres borrar "+file.nameWithoutExtension+"?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->if(file.delete())refreshList()}.show()}
 private fun text(v:String,size:Int)=TextView(this).apply{text=v;textSize=size.toFloat();setTextColor(0xFF64748B.toInt());setPadding(0,dp(4),0,dp(4))}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun human(n:Long)=if(n<1024*1024)(n/1024).toString()+" KB" else String.format(Locale.getDefault(),"%.1f MB",n/1024.0/1024.0)
}
