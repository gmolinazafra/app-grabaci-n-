package com.limonadaweb.grabatexto
import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {
 companion object { const val ACTION_START="START"; const val ACTION_STOP="STOP"; const val CHANNEL="recording"; const val ID=785 }
 private var recorder: MediaRecorder?=null
 override fun onCreate(){ super.onCreate(); createChannel() }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { when(intent?.action){ ACTION_START->startRecording(); ACTION_STOP->stopRecording() }; return START_STICKY }
 private fun startRecording(){
  if(recorder!=null)return
  val dir=File(filesDir,"recordings").apply{mkdirs()}
  val stamp=SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.getDefault()).format(Date())
  val output=File(dir,"Grabacion_"+stamp+".m4a")
  val r=if(Build.VERSION.SDK_INT>=31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
  recorder=r.apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setAudioEncodingBitRate(128000);setAudioSamplingRate(44100);setOutputFile(output.absolutePath);prepare();start()}
  startForeground(ID,notification("Grabando · puedes apagar la pantalla"))
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",true).putLong("started",System.currentTimeMillis()).apply()
 }
 private fun stopRecording(){ recorder?.let{try{it.stop()}catch(_:Exception){};it.reset();it.release()};recorder=null;getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf() }
 private fun notification(text:String):Notification{
  val stop=Intent(this,RecordingService::class.java).setAction(ACTION_STOP)
  val pi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("GrabaTexto está grabando").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).addAction(android.R.drawable.ic_media_pause,"Finalizar",pi).build()
 }
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Grabación en curso",NotificationManager.IMPORTANCE_LOW).apply{description="Mantiene activa la grabación en segundo plano"})}
 override fun onDestroy(){if(recorder!=null)stopRecording();super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}
