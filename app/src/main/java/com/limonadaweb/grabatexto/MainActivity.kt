package com.limonadaweb.grabatexto
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView;private lateinit var timer:TextView;private lateinit var start:Button;private lateinit var stop:Button;private lateinit var list:TextView
 private val handler=Handler(Looper.getMainLooper())
 private val tick=object:Runnable{override fun run(){refreshState();handler.postDelayed(this,1000)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);status=findViewById(R.id.status);timer=findViewById(R.id.timer);start=findViewById(R.id.startButton);stop=findViewById(R.id.stopButton);list=findViewById(R.id.recordings);start.setOnClickListener{ensurePermissionAndStart()};stop.setOnClickListener{stopRecording()};refreshList()}
 override fun onResume(){super.onResume();handler.post(tick);refreshList()}
 override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
 private fun ensurePermissionAndStart(){val p=mutableListOf(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33)p+=Manifest.permission.POST_NOTIFICATIONS;if(p.any{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED})ActivityCompat.requestPermissions(this,p.toTypedArray(),10)else startRecording()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startRecording()}
 private fun startRecording(){ContextCompat.startForegroundService(this,Intent(this,RecordingService::class.java).setAction(RecordingService.ACTION_START));Handler(Looper.getMainLooper()).postDelayed({refreshState()},250)}
 private fun stopRecording(){startService(Intent(this,RecordingService::class.java).setAction(RecordingService.ACTION_STOP));Handler(Looper.getMainLooper()).postDelayed({refreshState();refreshList()},500)}
 private fun refreshState(){val s=getSharedPreferences("state",MODE_PRIVATE);val active=s.getBoolean("recording",false);start.isEnabled=!active;stop.isEnabled=active;status.text=if(active)"🔴 Grabando en segundo plano" else "Listo para grabar";if(active){val sec=(System.currentTimeMillis()-s.getLong("started",System.currentTimeMillis()))/1000;timer.text=String.format(Locale.getDefault(),"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60)}else timer.text="00:00:00"}
 private fun refreshList(){val files=File(filesDir,"recordings").listFiles()?.sortedByDescending{it.lastModified()}?:emptyList();list.text=if(files.isEmpty())"Todavía no hay grabaciones." else files.take(10).joinToString("\n\n"){file->"🎧 "+file.nameWithoutExtension+"\n"+human(file.length())+" · "+SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(file.lastModified()))}}
 private fun human(n:Long)=if(n<1024*1024)(n/1024).toString()+" KB" else String.format(Locale.getDefault(),"%.1f MB",n/1024.0/1024.0)
}
