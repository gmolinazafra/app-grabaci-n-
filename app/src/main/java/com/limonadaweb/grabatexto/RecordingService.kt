package com.limonadaweb.grabatexto
import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService : Service() {
 companion object { const val ACTION_START="START"; const val CHANNEL="recording"; const val ID=785 }
 private var recorder:MediaRecorder?=null
 private var stopping=false
 override fun onCreate(){super.onCreate();createChannel()}
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { if(intent?.action==ACTION_START) startRecording(); return START_NOT_STICKY }
 private fun startRecording(){
  if(recorder!=null)return
  val dir=File(filesDir,"recordings").apply{mkdirs()}
  val output=File(dir,"Grabacion_"+SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.getDefault()).format(Date())+".m4a")
  val r=if(Build.VERSION.SDK_INT>=31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
  recorder=r.apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setAudioEncodingBitRate(128000);setAudioSamplingRate(44100);setOutputFile(output.absolutePath);prepare();start()}
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",true).putLong("started",System.currentTimeMillis()).apply()
  ServiceCompat.startForeground(this,ID,notification(),if(Build.VERSION.SDK_INT>=30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
 }
 private fun finishRecording(){
  if(stopping)return;stopping=true
  recorder?.let{r->try{r.stop()}catch(_:RuntimeException){};try{r.reset()}catch(_:Exception){};try{r.release()}catch(_:Exception){}};recorder=null
  getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",false).remove("started").apply()
  stopForeground(STOP_FOREGROUND_REMOVE)
 }
 private fun notification():Notification{
  val open=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("GrabaTexto está grabando").setContentText("Toca para volver y finalizar").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build()
 }
 private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Grabación en curso",NotificationManager.IMPORTANCE_LOW))}
 override fun onDestroy(){finishRecording();super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}
