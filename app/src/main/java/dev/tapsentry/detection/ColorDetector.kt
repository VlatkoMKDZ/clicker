package dev.tapsentry.detection

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ColorDetector {
    fun hsv(color:Int):FloatArray {
        val r=((color shr 16) and 255)/255f;val g=((color shr 8) and 255)/255f;val b=(color and 255)/255f
        val hi=max(r,max(g,b));val lo=min(r,min(g,b));val d=hi-lo
        val h=when{d==0f->0f;hi==r->60f*(((g-b)/d)%6f);hi==g->60f*((b-r)/d+2f);else->60f*((r-g)/d+4f)}.let{if(it<0)it+360 else it}
        return floatArrayOf(h,if(hi==0f)0f else d/hi,hi)
    }
    fun distance(a:Int,b:Int):Float {
        val x=hsv(a); val y=hsv(b); val hue=min(abs(x[0]-y[0]),360-abs(x[0]-y[0]))/1.8f
        return sqrt(hue*hue + (x[1]-y[1])*(x[1]-y[1])*10000 + (x[2]-y[2])*(x[2]-y[2])*10000)/1.732f
    }
    fun matches(color:Int,target:Int,tolerance:Float)=distance(color,target)<=tolerance
    fun representative(pixels:IntArray):Int {
        if(pixels.isEmpty()) return 0
        var r=0L; var g=0L; var b=0L
        pixels.forEach { r+=(it shr 16) and 255;g+=(it shr 8) and 255;b+=it and 255 }
        return (0xFF shl 24) or ((r/pixels.size).toInt() shl 16) or ((g/pixels.size).toInt() shl 8) or (b/pixels.size).toInt()
    }
}

class ConfirmationGate(private val required:Int) {
    var consecutive=0; private set
    fun update(match:Boolean):Boolean { consecutive=if(match) consecutive+1 else 0; return consecutive>=required.coerceAtLeast(1) }
    fun reset(){consecutive=0}
}
