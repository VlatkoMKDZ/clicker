package dev.tapsentry.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable data class NormalizedPoint(val x: Float, val y: Float) {
    fun pixels(width: Int, height: Int) = x.coerceIn(0f, 1f).times(width).roundToInt() to y.coerceIn(0f, 1f).times(height).roundToInt()
}
@Serializable data class NormalizedRegion(val left: Float=.4f, val top: Float=.4f, val right: Float=.6f, val bottom: Float=.55f) {
    fun valid() = left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f && right > left && bottom > top
}
@Serializable sealed class MacroAction {
    abstract val enabled: Boolean
    @Serializable @SerialName("tap") data class Tap(val point: NormalizedPoint=.5f.let { NormalizedPoint(it,it) }, val durationMs: Long=50, val intervalMs: Long=500, val repetitions: Int=1, val randomMs: Long=0, override val enabled:Boolean=true): MacroAction()
    @Serializable @SerialName("swipe") data class Swipe(val start:NormalizedPoint=NormalizedPoint(.3f,.5f), val end:NormalizedPoint=NormalizedPoint(.7f,.5f), val durationMs:Long=400, override val enabled:Boolean=true):MacroAction()
    @Serializable @SerialName("wait") data class Wait(val durationMs:Long=500, override val enabled:Boolean=true):MacroAction()
    @Serializable @SerialName("loop") data class Loop(val count:Int=0, override val enabled:Boolean=true):MacroAction() // 0 = infinite
    @Serializable @SerialName("stop") data class Stop(override val enabled:Boolean=true):MacroAction()
}
@Serializable data class DetectionConfig(
    val enabled:Boolean=false, val region:NormalizedRegion=NormalizedRegion(), val targetColor:Int=0xFFB3261E.toInt(),
    val tolerance:Float=18f, val hueTolerance:Float=20f, val saturationTolerance:Float=.35f, val valueTolerance:Float=.35f,
    val intervalMs:Long=200, val confirmations:Int=2, val stopDelayMs:Long=300, val additionalClicks:Int=0, val showRegion:Boolean=true
)
@Serializable data class IndicatorConfig(val enabled:Boolean=true, val size:Int=2, val durationMs:Long=300)
@Serializable data class AutomationProfile(val id:String=java.util.UUID.randomUUID().toString(), val name:String="My automation", val actions:List<MacroAction> = listOf(MacroAction.Tap()), val detection:DetectionConfig=DetectionConfig(), val indicator:IndicatorConfig=IndicatorConfig(), val startDelayMs:Long=500, val keepAwake:Boolean=false)

enum class SessionState { IDLE, STARTING, RUNNING, PAUSED, AUTO_STOP, STOPPED, ERROR }
