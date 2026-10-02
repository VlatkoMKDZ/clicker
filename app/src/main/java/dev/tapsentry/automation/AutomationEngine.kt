package dev.tapsentry.automation

import android.content.Context
import android.view.WindowManager
import dev.tapsentry.accessibility.TapAccessibilityService
import dev.tapsentry.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

object AutomationEngine {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private var job:Job?=null
    private val _state=MutableStateFlow(SessionState.IDLE); val state=_state.asStateFlow()
    var onClick:((Int,Int)->Unit)?=null; var onMessage:((String)->Unit)?=null
    @Volatile private var profile:AutomationProfile?=null
    @Volatile private var additional=-1
    fun start(context:Context,p:AutomationProfile) {
        if(job?.isActive==true)return; profile=p; additional=-1
        val bounds=context.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        job=scope.launch { try {
            _state.value=SessionState.STARTING; delay(p.startDelayMs); _state.value=SessionState.RUNNING
            var index=0
            while(isActive) {
                while(_state.value==SessionState.PAUSED) delay(50)
                val actions=p.actions.filter{it.enabled}; if(actions.isEmpty()) break
                if(index>=actions.size) index=0
                when(val a=actions[index++]) {
                    is MacroAction.Tap -> repeat(a.repetitions.coerceAtLeast(1)) {
                        if(additional==0){finishAutoStop(p);return@launch}
                        val (x,y)=a.point.pixels(bounds.width(),bounds.height()); TapAccessibilityService.instance?.gesture(x.toFloat(),y.toFloat(),duration=a.durationMs) ?: error("Accessibility service is not enabled")
                        withContext(Dispatchers.Main.immediate){onClick?.invoke(x,y)}; if(additional>0)additional--; delay(interval(a.intervalMs,a.randomMs))
                    }
                    is MacroAction.Swipe -> { val(sx,sy)=a.start.pixels(bounds.width(),bounds.height());val(ex,ey)=a.end.pixels(bounds.width(),bounds.height());TapAccessibilityService.instance?.gesture(sx.toFloat(),sy.toFloat(),ex.toFloat(),ey.toFloat(),a.durationMs)?:error("Accessibility service is not enabled") }
                    is MacroAction.Wait -> delay(a.durationMs)
                    is MacroAction.Loop -> { if(a.count==0)index=0 }
                    is MacroAction.Stop -> break
                }
            }; if(_state.value!=SessionState.STOPPED) _state.value=SessionState.STOPPED
        } catch(_:CancellationException){} catch(e:Exception){_state.value=SessionState.ERROR;onMessage?.invoke(e.message?:"Automation failed")} }
    }
    fun visualMatch(){ val p=profile?:return; if(_state.value!=SessionState.RUNNING)return; _state.value=SessionState.AUTO_STOP; onMessage?.invoke("AUTO STOP"); additional=p.detection.additionalClicks; if(additional==0) scope.launch{finishAutoStop(p)} else _state.value=SessionState.RUNNING }
    private suspend fun finishAutoStop(p:AutomationProfile){delay(p.detection.stopDelayMs);job?.cancel();_state.value=SessionState.STOPPED;onMessage?.invoke("STOPPED · visual target detected")}
    fun pause(){if(_state.value==SessionState.RUNNING)_state.value=SessionState.PAUSED}
    fun resume(){if(_state.value==SessionState.PAUSED)_state.value=SessionState.RUNNING}
    fun stop(){job?.cancel();job=null;_state.value=SessionState.STOPPED}
    fun interval(base:Long,random:Long)=if(random<=0)base.coerceAtLeast(1) else (base+Random.nextLong(-random,random+1)).coerceAtLeast(1)
}
