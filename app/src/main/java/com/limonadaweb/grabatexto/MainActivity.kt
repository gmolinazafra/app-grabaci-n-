package com.limonadaweb.grabatexto

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView; private lateinit var timer:TextView; private lateinit var start:Button; private lateinit var stop:Button
 private lateinit var container:LinearLayout; private lateinit var home:LinearLayout; private lateinit var recordingsScreen:LinearLayout
 private val handler=Handler(Looper.getMainLooper()); private var player:MediaPlayer?=null
 private val client=OkHttpClient.Builder().callTimeout(java.time.Duration.ofMinutes(10)).build()
 private val tick=object:Runnable{override fun run(){refreshState();handler.postDelayed(this,1000)}}

 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
  status=findViewById(R.id.status);timer=findViewById(R.id.timer);start=findViewById(R.id.startButton);stop=findViewById(R.id.stopButton)
  container=findViewById(R.id.recordingsContainer);home=findViewById(R.id.homeScreen);recordingsScreen=findViewById(R.id.recordingsScreen)
  findViewById<Button>(R.id.homeTab).setOnClickListener{showHome()};findViewById<Button>(R.id.recordingsTab).setOnClickListener{showRecordings()}
  findViewById<Button>(R.id.apiButton).setOnClickListener{apiDialog()}
  start.setOnClickListener{ensurePermissionAndStart()};stop.setOnClickListener{stopRecording()};refreshList()
 }
 override fun onResume(){super.onResume();handler.post(tick);refreshList()}
 override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
 override fun onDestroy(){player?.release();super.onDestroy()}
 private fun showHome(){home.visibility=View.VISIBLE;recordingsScreen.visibility=View.GONE}
 private fun showRecordings(){home.visibility=View.GONE;recordingsScreen.visibility=View.VISIBLE;refreshList()}
 private fun ensurePermissionAndStart(){val p=mutableListOf(Manifest.permission.RECORD_AUDIO);if(Build.VERSION.SDK_INT>=33)p+=Manifest.permission.POST_NOTIFICATIONS;if(p.any{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED})ActivityCompat.requestPermissions(this,p.toTypedArray(),10)else startRecording()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startRecording()}
 private fun startRecording(){ContextCompat.startForegroundService(this,Intent(this,RecordingService::class.java).setAction(RecordingService.ACTION_START));handler.postDelayed({refreshState()},250)}
 private fun stopRecording(){stop.isEnabled=false;status.text="Finalizando…";stopService(Intent(this,RecordingService::class.java));getSharedPreferences("state",MODE_PRIVATE).edit().putBoolean("recording",false).remove("started").apply();handler.postDelayed({refreshState();refreshList();showRecordings();Toast.makeText(this,"Grabación guardada",Toast.LENGTH_SHORT).show()},700)}
 private fun refreshState(){val s=getSharedPreferences("state",MODE_PRIVATE);val active=s.getBoolean("recording",false);start.isEnabled=!active;stop.isEnabled=active;status.text=if(active)"🔴 Grabando · puedes apagar la pantalla" else "Listo para grabar";if(active){val sec=(System.currentTimeMillis()-s.getLong("started",System.currentTimeMillis()))/1000;timer.text=String.format(Locale.getDefault(),"%02d:%02d:%02d",sec/3600,(sec%3600)/60,sec%60)}else timer.text="00:00:00"}

 private fun refreshList(){container.removeAllViews();val files=File(filesDir,"recordings").listFiles{f->f.extension=="m4a"}?.sortedByDescending{it.lastModified()}?:emptyList()
  if(files.isEmpty()){container.addView(label("Todavía no hay grabaciones.",15));return}
  files.forEach{file->
   val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));setBackgroundColor(0xFFFFFFFF.toInt())}
   card.addView(label("🎙  "+file.nameWithoutExtension,18,true))
   card.addView(label(SimpleDateFormat("dd/MM/yyyy · HH:mm",Locale.getDefault()).format(Date(file.lastModified()))+" · "+human(file.length()),13))
   val transcript=File(file.parentFile,file.nameWithoutExtension+".txt")
   card.addView(label(if(transcript.exists())"✓ Transcripción guardada" else "Sin transcripción",13))
   val r1=row(); button(r1,"▶ Oír"){play(file)};button(r1,"✨ Transcribir"){transcribe(file)}
   val r2=row(); button(r2,"📝 Texto"){openText(transcript)};button(r2,"✎ Nombre"){rename(file)}
   val r3=row(); button(r3,"↗ Compartir"){share(file)};button(r3,"🗑 Borrar"){delete(file)}
   card.addView(r1);card.addView(r2);card.addView(r3)
   container.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)})
  }
 }
 private fun transcribe(file:File){
  val key=getSharedPreferences("settings",MODE_PRIVATE).getString("api_key","")?:""
  if(key.isBlank()){Toast.makeText(this,"Configura primero la clave de transcripción",Toast.LENGTH_LONG).show();apiDialog();return}
  Toast.makeText(this,"Transcribiendo…",Toast.LENGTH_SHORT).show()
  val body=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("model","gpt-transcribe").addFormDataPart("languages[]","es").addFormDataPart("file",file.name,file.asRequestBody("audio/mp4".toMediaType())).build()
  val req=Request.Builder().url("https://api.openai.com/v1/audio/transcriptions").header("Authorization","Bearer "+key).post(body).build()
  client.newCall(req).enqueue(object:Callback{
   override fun onFailure(call:Call,e:IOException){runOnUiThread{Toast.makeText(this@MainActivity,"Error de conexión: "+e.message,Toast.LENGTH_LONG).show()}}
   override fun onResponse(call:Call,response:Response){response.use{val raw=it.body?.string()?:"";runOnUiThread{
    if(!it.isSuccessful){Toast.makeText(this@MainActivity,"No se pudo transcribir ("+it.code+")",Toast.LENGTH_LONG).show();return@runOnUiThread}
    try{val text=JSONObject(raw).getString("text");File(file.parentFile,file.nameWithoutExtension+".txt").writeText(text);refreshList();showTextEditor(File(file.parentFile,file.nameWithoutExtension+".txt"))}catch(e:Exception){Toast.makeText(this@MainActivity,"Respuesta de transcripción no válida",Toast.LENGTH_LONG).show()}
   }}}
  })
 }
 private fun apiDialog(){val input=EditText(this).apply{hint="sk-…";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setText(getSharedPreferences("settings",MODE_PRIVATE).getString("api_key",""))};AlertDialog.Builder(this).setTitle("Clave para transcribir").setMessage("Se guarda únicamente en este teléfono.").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar"){_,_->getSharedPreferences("settings",MODE_PRIVATE).edit().putString("api_key",input.text.toString().trim()).apply()}.show()}
 private fun openText(file:File){if(!file.exists()){Toast.makeText(this,"Esta grabación aún no está transcrita",Toast.LENGTH_SHORT).show();return};showTextEditor(file)}
 private fun showTextEditor(file:File){val input=EditText(this).apply{setText(file.readText());gravity=Gravity.TOP;minLines=10;setPadding(dp(16),dp(12),dp(16),dp(12))};AlertDialog.Builder(this).setTitle("Transcripción").setView(input).setNeutralButton("Copiar"){_,_->(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Transcripción",input.text));Toast.makeText(this,"Texto copiado",Toast.LENGTH_SHORT).show()}.setNegativeButton("Cerrar",null).setPositiveButton("Guardar"){_,_->file.writeText(input.text.toString())}.show()}
 private fun play(file:File){player?.release();player=MediaPlayer().apply{setDataSource(file.absolutePath);prepare();start();Toast.makeText(this@MainActivity,"Reproduciendo",Toast.LENGTH_SHORT).show();setOnCompletionListener{it.release();player=null}}}
 private fun rename(file:File){val input=EditText(this).apply{setText(file.nameWithoutExtension);selectAll()};AlertDialog.Builder(this).setTitle("Renombrar").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar"){_,_->val n=input.text.toString().trim().replace(Regex("[\\/:*?\"<>|]"),"_");if(n.isNotEmpty()){val txt=File(file.parentFile,file.nameWithoutExtension+".txt");file.renameTo(File(file.parentFile,n+".m4a"));if(txt.exists())txt.renameTo(File(txt.parentFile,n+".txt"));refreshList()}}.show()}
 private fun share(file:File){val uri=FileProvider.getUriForFile(this,packageName+".files",file);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="audio/mp4";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartir grabación"))}
 private fun delete(file:File){AlertDialog.Builder(this).setTitle("Eliminar grabación").setMessage("¿Seguro que quieres borrarla?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar"){_,_->File(file.parentFile,file.nameWithoutExtension+".txt").delete();file.delete();refreshList()}.show()}
 private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,dp(7),0,0)}
 private fun button(row:LinearLayout,t:String,a:()->Unit){row.addView(Button(this).apply{text=t;textSize=12f;setOnClickListener{a()}},LinearLayout.LayoutParams(0,dp(50),1f))}
 private fun label(v:String,s:Int,b:Boolean=false)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(if(b)0xFF0F172A.toInt() else 0xFF64748B.toInt());if(b)setTypeface(typeface,1);setPadding(0,dp(3),0,dp(3))}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun human(n:Long)=if(n<1048576)(n/1024).toString()+" KB" else String.format(Locale.getDefault(),"%.1f MB",n/1048576.0)
}
