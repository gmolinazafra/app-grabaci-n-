package com.limonadaweb.grabatexto
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService:Service(){
 companion object{const val ACTION_START="START";const val CHANNEL="recording";const val ID=785}
 private var recorder:MediaRecorder?=null;private var stopping=false
 override fun onCreate(){super.onCreate();createChannel()}
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(intent?.action==ACTION_START){
   val s=getSharedPreferences("state",MODE_PRIVATE);s.edit().putBoolean("recording",false).remove("error").apply()
   try{
    ServiceCompat.startForeground(this,ID,notification("Preparando micrófono…"),if(Build.VERSION.SDK_INT>=30)ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
    startRecorder()
   }catch(t:Throwable){
    try{recorder?.release()}catch(_:Throwable){};recorder=null
    s.edit().putBoolean("recording",false).putString("error",t.javaClass.simpleName+": "+(t.message?:"sin detalle")).apply()
    stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
   }
  }
  return START_NOT_STICKY
 }
 private fun startRecorder(){
  if(recorder!=null)return
  val dir=File(filesDir,"recordings").apply{mkdirs()}
  val output=File(dir,"Grabacion_"+SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.getDefault()).format(Date())+".m4a")
  val r=if(Build.VERSION.SDK_INT>=31)MediaRecorder(this)else @Suppress("DEPRECATION") MediaRecorder()
  recorder=r
  r.setAudioSource(MediaRecorder.AudioSource.MIC);r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);r.setAudioEncodingBitRate(128000);r.setAudioSamplingRate(44100);r.setOutputFile(output.absolutePath);r.prepare();r.start()
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",true).putLong("started",System.currentTimeMillis()).remove("error").apply()
  getSystemService(NotificationManager::class.java).notify(ID,notification("Grabando · puedes apagar la pantalla"))
 }
 private fun finishRecorder(){
  if(stopping)return;stopping=true
  recorder?.let{r->try{r.stop()}catch(_:Throwable){};try{r.release()}catch(_:Throwable){}};recorder=null
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",false).remove("started").apply()
  stopForeground(STOP_FOREGROUND_REMOVE)
 }
 private fun notification(msg:String):Notification{
  val open=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("GrabaTexto").setContentText(msg).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build()
 }
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Grabación en curso",NotificationManager.IMPORTANCE_LOW))}
 override fun onDestroy(){finishRecorder();super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
}