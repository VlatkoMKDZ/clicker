package dev.tapsentry

import dev.tapsentry.automation.AutomationEngine
import dev.tapsentry.detection.ColorDetector
import dev.tapsentry.detection.ConfirmationGate
import dev.tapsentry.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {
    @Test fun normalizedCoordinatesScaleAcrossOrientations(){assertEquals(500 to 400,NormalizedPoint(.5f,.25f).pixels(1000,1600));assertEquals(800 to 250,NormalizedPoint(.5f,.25f).pixels(1600,1000))}
    @Test fun hsvDistanceRecognizesTolerance(){fun rgb(r:Int,g:Int,b:Int)=(0xFF shl 24)or(r shl 16)or(g shl 8)or b;val red=rgb(220,30,30);assertTrue(ColorDetector.matches(red,rgb(215,32,31),5f));assertFalse(ColorDetector.matches(red,rgb(0,0,255),20f))}
    @Test fun confirmationRejectsTransientFrame(){val gate=ConfirmationGate(2);assertFalse(gate.update(true));assertFalse(gate.update(false));assertFalse(gate.update(true));assertTrue(gate.update(true))}
    @Test fun intervalNeverBecomesNonPositive(){repeat(100){assertTrue(AutomationEngine.interval(5,100)>=1)}}
    @Test fun profileRoundTripsWithMacroSequence(){val p=AutomationProfile(actions=listOf(MacroAction.Tap(),MacroAction.Wait(123),MacroAction.Swipe(),MacroAction.Loop()));val json=Json{classDiscriminator="kind"};assertEquals(p,json.decodeFromString<AutomationProfile>(json.encodeToString(p)))}
    @Test fun regionsMustHaveAreaAndRemainNormalized(){assertTrue(NormalizedRegion().valid());assertFalse(NormalizedRegion(.8f,.2f,.3f,.5f).valid())}
    @Test fun stopDelayAndAdditionalClicksPersist(){val d=DetectionConfig(stopDelayMs=987,additionalClicks=3);assertEquals(987,d.stopDelayMs);assertEquals(3,d.additionalClicks)}
}
