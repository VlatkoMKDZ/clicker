package dev.tapsentry.detection

import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import dev.tapsentry.automation.AutomationEngine
import dev.tapsentry.model.DetectionConfig

class VisualDetectionEngine(private val projection:MediaProjection, private val width:Int, private val height:Int, private val density:Int, private val config:DetectionConfig) {
    private val thread=HandlerThread("visual-detection").apply{start()}; private val handler=Handler(thread.looper)
    private val reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2); private val gate=ConfirmationGate(config.confirmations)
    private var last=0L
    private val callback=object:MediaProjection.Callback(){override fun onStop(){reader.setOnImageAvailableListener(null,null)}}
    private val display=projection.run { registerCallback(callback,handler);createVirtualDisplay("TapSentry region monitor",width,height,density,0,reader.surface,null,handler) }
    init { reader.setOnImageAvailableListener({ r ->
        val image=r.acquireLatestImage()?:return@setOnImageAvailableListener
        try { val now=System.currentTimeMillis();if(now-last<config.intervalMs)return@setOnImageAvailableListener;last=now
            val plane=image.planes[0];val buf=plane.buffer;val stride=plane.rowStride;val pixelStride=plane.pixelStride
            val l=(config.region.left*width).toInt().coerceIn(0,width-1);val t=(config.region.top*height).toInt().coerceIn(0,height-1);val rr=(config.region.right*width).toInt().coerceIn(l+1,width);val bb=(config.region.bottom*height).toInt().coerceIn(t+1,height)
            var red=0L;var green=0L;var blue=0L;var count=0
            val step=4
            for(y in t until bb step step)for(x in l until rr step step){val pos=y*stride+x*pixelStride;if(pos+2<buf.limit()){red+=(buf.get(pos).toInt() and 255);green+=(buf.get(pos+1).toInt() and 255);blue+=(buf.get(pos+2).toInt() and 255);count++}}
            if(count>0){val c=android.graphics.Color.rgb((red/count).toInt(),(green/count).toInt(),(blue/count).toInt());if(gate.update(ColorDetector.matches(c,config.targetColor,config.tolerance)))AutomationEngine.visualMatch()}
        } finally {image.close()}
    },handler) }
    fun close(){reader.setOnImageAvailableListener(null,null);display.release();reader.close();projection.unregisterCallback(callback);projection.stop();thread.quitSafely()}
}
