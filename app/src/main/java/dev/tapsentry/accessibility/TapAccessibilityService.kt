package dev.tapsentry.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class TapAccessibilityService: AccessibilityService() {
    override fun onServiceConnected() { instance=this }
    override fun onDestroy() { if(instance===this) instance=null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    suspend fun gesture(x1:Float,y1:Float,x2:Float=x1,y2:Float=y1,duration:Long=50):Boolean = suspendCancellableCoroutine { cont ->
        val path=Path().apply { moveTo(x1,y1); if(x1!=x2||y1!=y2) lineTo(x2,y2) }
        val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,duration.coerceAtLeast(1))).build()
        val accepted=dispatchGesture(gesture,object:GestureResultCallback(){override fun onCompleted(g:GestureDescription?){if(cont.isActive)cont.resume(true)};override fun onCancelled(g:GestureDescription?){if(cont.isActive)cont.resume(false)}},null)
        if(!accepted && cont.isActive) cont.resume(false)
    }
    companion object { @Volatile var instance:TapAccessibilityService?=null; private set }
}
