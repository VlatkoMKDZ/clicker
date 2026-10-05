package dev.tapsentry.overlay

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import dev.tapsentry.R
import dev.tapsentry.automation.AutomationEngine
import dev.tapsentry.detection.VisualDetectionEngine
import dev.tapsentry.model.AutomationProfile
import kotlinx.serialization.json.Json

class OverlayService:Service() {
    private lateinit var wm:WindowManager; private var panel:View?=null;private var indicator:View?=null;private var detector:VisualDetectionEngine?=null
    private val json=Json{ignoreUnknownKeys=true;classDiscriminator="kind"};private lateinit var profile:AutomationProfile
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){super.onCreate();wm=getSystemService(WindowManager::class.java);channel();startForeground(7,NotificationCompat.Builder(this,"automation").setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Tap Sentry is ready").setContentText("Overlay automation controls are active").setOngoing(true).build())}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if (intent == null) {
            Log.w("TapSentryOverlay", "Ignoring service start without an intent")
            stopSelf()
            return START_NOT_STICKY
        }
        when(intent.action){ACTION_STOP->{AutomationEngine.stop();stopSelf();return START_NOT_STICKY}}
        profile=runCatching{json.decodeFromString<AutomationProfile>(intent?.getStringExtra(EXTRA_PROFILE)?:"")}.getOrElse{stopSelf();return START_NOT_STICKY}
        showPanel(); AutomationEngine.onClick={x,y->showIndicator(x,y)};AutomationEngine.onMessage={msg->panel?.findViewById<TextView>(R.id.automation_status)?.text=msg}
        val result=intent.getIntExtra(EXTRA_RESULT,Activity.RESULT_CANCELED);val data=if(Build.VERSION.SDK_INT>=33)intent.getParcelableExtra(EXTRA_DATA,Intent::class.java)else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        if(profile.detection.enabled&&result==Activity.RESULT_OK&&data!=null){val metrics=resources.displayMetrics; val projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(result,data);detector=VisualDetectionEngine(projection,metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,profile.detection)}
        AutomationEngine.start(this,profile);return START_NOT_STICKY
    }
    private fun showPanel(){if(panel!=null)return
        val box=LinearLayout(this).apply{id=R.id.controller_panel;orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(12,8,12,8);background=GradientDrawable().apply{cornerRadius=32f;setColor(0xE6221E2F.toInt());setStroke(2,0xFF6750A4.toInt())}}
        val status=TextView(this).apply{id=R.id.automation_status;text="STARTING";setTextColor(Color.WHITE);setPadding(8,0,8,0)};box.addView(status)
        fun button(label:String,action:()->Unit)=Button(this).apply{text=label;minWidth=0;minimumWidth=0;setPadding(8,0,8,0);setOnClickListener{action()}}
        box.addView(button("Ⅱ"){AutomationEngine.pause();status.text="PAUSED"});box.addView(button("▶"){AutomationEngine.resume();status.text="RUNNING"});box.addView(button("■"){AutomationEngine.stop();status.text="STOPPED"});box.addView(button("×"){stopSelf()})
        val lp=params(48,100,Gravity.TOP or Gravity.START);var sx=0f;var sy=0f;var ox=0;var oy=0
        box.setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;ox=lp.x;oy=lp.y;true};MotionEvent.ACTION_MOVE->{lp.x=ox+(e.rawX-sx).toInt();lp.y=oy+(e.rawY-sy).toInt();wm.updateViewLayout(box,lp);true};else->false}}
        panel=box;wm.addView(box,lp)
    }
    private fun showIndicator(x:Int,y:Int){if(!profile.indicator.enabled)return;indicator?.let{runCatching{wm.removeView(it)}}
        val size=listOf(32,48,64)[profile.indicator.size.coerceIn(0,2)];val v=TextView(this).apply{text="◎";textSize=(size/2).toFloat();gravity=Gravity.CENTER;setTextColor(0xFFFFB4AB.toInt())};indicator=v
        wm.addView(v,params(x-size/2,y-size/2,Gravity.TOP or Gravity.START,size,size));v.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(profile.indicator.durationMs).withEndAction{runCatching{wm.removeView(v)};if(indicator===v)indicator=null}.start()
    }
    private fun params(x:Int,y:Int,g:Int,w:Int=WindowManager.LayoutParams.WRAP_CONTENT,h:Int=WindowManager.LayoutParams.WRAP_CONTENT)=WindowManager.LayoutParams(w,h,if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply{gravity=g;this.x=x;this.y=y}
    private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("automation","Automation",NotificationManager.IMPORTANCE_LOW))}
    override fun onDestroy(){AutomationEngine.stop();AutomationEngine.onClick=null;AutomationEngine.onMessage=null;detector?.close();panel?.let{runCatching{wm.removeView(it)}};indicator?.let{runCatching{wm.removeView(it)}};super.onDestroy()}
    companion object{const val EXTRA_PROFILE="profile";const val EXTRA_RESULT="result";const val EXTRA_DATA="data";const val ACTION_STOP="stop"}
}
