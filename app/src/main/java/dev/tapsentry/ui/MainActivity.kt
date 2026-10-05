package dev.tapsentry.ui

import android.Manifest
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tapsentry.accessibility.TapAccessibilityService
import dev.tapsentry.model.*
import dev.tapsentry.overlay.OverlayService
import dev.tapsentry.storage.ProfileStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{TapSentryTheme{App()}}}}

@Composable private fun TapSentryTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=if(androidx.compose.foundation.isSystemInDarkTheme())darkColorScheme(primary=Color(0xFFD0BCFF))else lightColorScheme(primary=Color(0xFF6750A4)),content=content)}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(){
    val context=LocalContext.current;val activity=context as Activity;val store=remember{ProfileStore(context)}
    var profiles by remember{mutableStateOf(store.loadAll().ifEmpty{listOf(AutomationProfile())})};var selected by remember{mutableIntStateOf(0)};var page by remember{mutableIntStateOf(0)};var message by remember{mutableStateOf<String?>(null)}
    val profile=profiles[selected.coerceIn(profiles.indices)]
    fun update(p:AutomationProfile){profiles=profiles.toMutableList().also{it[selected]=p};store.save(profiles)}
    fun launch(result:Int=Activity.RESULT_CANCELED,data:Intent?=null){
        if(!Settings.canDrawOverlays(context)){message="Allow ‘display over other apps’ first";context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}")));return}
        if(TapAccessibilityService.instance==null){message="Enable Tap Sentry in Accessibility settings first";context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return}
        val i=Intent(context,OverlayService::class.java).putExtra(OverlayService.EXTRA_PROFILE,Json.encodeToString(profile)).putExtra(OverlayService.EXTRA_RESULT,result).putExtra(OverlayService.EXTRA_DATA,data)
        androidx.core.content.ContextCompat.startForegroundService(context,i);activity.moveTaskToBack(true)
    }
    val capture=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==Activity.RESULT_OK&&it.data!=null)launch(it.resultCode,it.data)else message="Screen capture permission was denied"}
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    Scaffold(topBar={TopAppBar(title={Column{Text("Tap Sentry",fontWeight=FontWeight.Bold);Text("Local, private automation",style=MaterialTheme.typography.labelSmall)}},actions={IconButton(onClick={page=1}){Icon(Icons.Default.Settings,"Settings")}})},bottomBar={NavigationBar{NavigationBarItem(page==0,{page=0},icon={Icon(Icons.Default.PlayArrow,null)},label={Text("Automation")});NavigationBarItem(page==1,{page=1},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")});NavigationBarItem(page==2,{page=2},icon={Icon(Icons.Default.Security,null)},label={Text("Privacy")})}}),snackbarHost={SnackbarHost(remember{SnackbarHostState()})}){pad->
        when(page){0->AutomationPage(profile,profiles,selected,{selected=it},{list->profiles=list;selected=selected.coerceAtMost(list.lastIndex);store.save(list)},{update(it)},onStart={if(Build.VERSION.SDK_INT>=33)notifications.launch(Manifest.permission.POST_NOTIFICATIONS);if(profile.detection.enabled)capture.launch(context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())else launch()});1->SettingsPage(profile,::update);else->PrivacyPage()}
        message?.let{AlertDialog(onDismissRequest={message=null},confirmButton={TextButton({message=null}){Text("OK")}},title={Text("Action needed")},text={Text(it)})}
        Spacer(Modifier.padding(pad))
    }
}

@Composable private fun AutomationPage(p:AutomationProfile,all:List<AutomationProfile>,selected:Int,onSelected:(Int)->Unit,onProfiles:(List<AutomationProfile>)->Unit,onUpdate:(AutomationProfile)->Unit,onStart:()->Unit){
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=80.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("PROFILE / SCRIPT",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){var open by remember{mutableStateOf(false)};OutlinedButton({open=true},Modifier.fillMaxWidth()){Text(p.name);Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(open,{open=false}){all.forEachIndexed{i,v->DropdownMenuItem({Text(v.name)},{onSelected(i);open=false})}}};IconButton({onProfiles(all+AutomationProfile(name="Profile ${all.size+1}"))}){Icon(Icons.Default.Add,"Create")};IconButton({onProfiles(all+p.copy(id=java.util.UUID.randomUUID().toString(),name=p.name+" copy"))}){Icon(Icons.Default.ContentCopy,"Duplicate")};IconButton({if(all.size>1)onProfiles(all.filterIndexed{i,_->i!=selected})}){Icon(Icons.Default.Delete,"Delete")}};OutlinedTextField(p.name,{onUpdate(p.copy(name=it))},label={Text("Profile name")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item{PermissionCard()}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("ACTION SEQUENCE",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);Row{IconButton({onUpdate(p.copy(actions=p.actions+MacroAction.Tap()))}){Icon(Icons.Default.TouchApp,"Add tap")};IconButton({onUpdate(p.copy(actions=p.actions+MacroAction.Wait()))}){Icon(Icons.Default.Timer,"Add wait")};IconButton({onUpdate(p.copy(actions=p.actions+MacroAction.Swipe()))}){Icon(Icons.Default.Swipe,"Add swipe")}}}}
        itemsIndexed(p.actions){i,a->ActionCard(i,a,{updated->onUpdate(p.copy(actions=p.actions.toMutableList().also{it[i]=updated}))},{if(i>0)onUpdate(p.copy(actions=p.actions.toMutableList().also{val x=removeAt(i);add(i-1,x)}))},{onUpdate(p.copy(actions=p.actions.filterIndexed{j,_->j!=i}))})}
        item{VisualCard(p,onUpdate)}
        item{Button(onStart,Modifier.fillMaxWidth().height(56.dp)){Icon(Icons.Default.OpenInNew,null);Spacer(Modifier.width(8.dp));Text("Launch floating controls")};Text("Tap Sentry will move behind the target app. Position markers using normalized coordinates.",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable private fun PermissionCard(){val c=LocalContext.current;val enabled=TapAccessibilityService.instance!=null;Card{Column(Modifier.padding(16.dp)){Text("Required access",fontWeight=FontWeight.Bold);Status("Accessibility gestures",enabled);Status("Display over apps",Settings.canDrawOverlays(c));Row{OutlinedButton({c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}){Text("Accessibility")};Spacer(Modifier.width(8.dp));OutlinedButton({c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${c.packageName}")))}){Text("Overlay")}};Text("Android always asks you to grant these privileges. Tap Sentry cannot bypass system security.",style=MaterialTheme.typography.bodySmall)}}}
@Composable private fun Status(name:String,yes:Boolean){Row(verticalAlignment=Alignment.CenterVertically){Text(if(yes)"●" else "○",color=if(yes)Color(0xFF2E7D32)else MaterialTheme.colorScheme.error);Spacer(Modifier.width(8.dp));Text("$name · ${if(yes)"Enabled" else "Disabled"}")}}

@Composable private fun ActionCard(index:Int,a:MacroAction,onChange:(MacroAction)->Unit,onUp:()->Unit,onDelete:()->Unit){Card{Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("${index+1}. ${a::class.simpleName}",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onUp){Icon(Icons.Default.ArrowUpward,"Move up")};IconButton(onDelete){Icon(Icons.Default.Close,"Delete")}};when(a){is MacroAction.Tap->{NumberField("X (0–100%)",(a.point.x*100).toInt()){onChange(a.copy(point=a.point.copy(x=(it/100f).coerceIn(0f,1f))))};NumberField("Y (0–100%)",(a.point.y*100).toInt()){onChange(a.copy(point=a.point.copy(y=(it/100f).coerceIn(0f,1f))))};NumberField("Interval ms",a.intervalMs.toInt()){onChange(a.copy(intervalMs=it.toLong().coerceAtLeast(1)))};NumberField("Press duration ms",a.durationMs.toInt()){onChange(a.copy(durationMs=it.toLong().coerceAtLeast(1)))}};is MacroAction.Wait->NumberField("Wait ms",a.durationMs.toInt()){onChange(a.copy(durationMs=it.toLong().coerceAtLeast(1)))};is MacroAction.Swipe->Text("Swipe ${(a.start.x*100).toInt()}%, ${(a.start.y*100).toInt()}% → ${(a.end.x*100).toInt()}%, ${(a.end.y*100).toInt()}% · ${a.durationMs} ms");is MacroAction.Loop->Text(if(a.count==0)"Infinite loop" else "${a.count} loops");is MacroAction.Stop->Text("Stop the current session")}}}}
@Composable private fun NumberField(label:String,value:Int,onValue:(Int)->Unit){OutlinedTextField(value.toString(),{it.toIntOrNull()?.let(onValue)},label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}

@Composable private fun VisualCard(p:AutomationProfile,onUpdate:(AutomationProfile)->Unit){val d=p.detection;Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Visibility,null);Spacer(Modifier.width(8.dp));Text("Auto Stop on Visual Change",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(d.enabled,{onUpdate(p.copy(detection=d.copy(enabled=it)))})};Text("Only a small normalized region is sampled in memory. No image is stored or sent.");Text("Target color",style=MaterialTheme.typography.labelMedium);Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){Surface(color=Color(d.targetColor),shape=MaterialTheme.shapes.small){Spacer(Modifier.fillMaxSize())}};NumberField("Color tolerance (0–100)",d.tolerance.toInt()){onUpdate(p.copy(detection=d.copy(tolerance=it.coerceIn(0,100).toFloat())))};NumberField("Confirmation frames (1/2/3/5)",d.confirmations){onUpdate(p.copy(detection=d.copy(confirmations=it.coerceIn(1,5))))};NumberField("Detection interval ms",d.intervalMs.toInt()){onUpdate(p.copy(detection=d.copy(intervalMs=it.toLong().coerceAtLeast(50))))};NumberField("Stop delay / indicator hold ms",d.stopDelayMs.toInt()){onUpdate(p.copy(detection=d.copy(stopDelayMs=it.toLong().coerceIn(0,5000))))};NumberField("Additional clicks",d.additionalClicks){onUpdate(p.copy(detection=d.copy(additionalClicks=it.coerceAtLeast(0))))};Text("Region: ${(d.region.left*100).toInt()}–${(d.region.right*100).toInt()}% × ${(d.region.top*100).toInt()}–${(d.region.bottom*100).toInt()}%",style=MaterialTheme.typography.bodySmall)}}}

@Composable private fun SettingsPage(p:AutomationProfile,onUpdate:(AutomationProfile)->Unit){Column(Modifier.fillMaxSize().padding(16.dp).padding(top=72.dp,bottom=80.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("GENERAL",color=MaterialTheme.colorScheme.primary);Row(verticalAlignment=Alignment.CenterVertically){Text("Click indicator",Modifier.weight(1f));Switch(p.indicator.enabled,{onUpdate(p.copy(indicator=p.indicator.copy(enabled=it)))})};NumberField("Indicator duration (50–1000 ms)",p.indicator.durationMs.toInt()){onUpdate(p.copy(indicator=p.indicator.copy(durationMs=it.toLong().coerceIn(50,1000))))};NumberField("Start delay ms",p.startDelayMs.toInt()){onUpdate(p.copy(startDelayMs=it.toLong().coerceAtLeast(0)))};Text("DEBUG",color=MaterialTheme.colorScheme.primary);Text("To test visual detection without a game, enable visual auto-stop and monitor a region containing a color-changing app or system surface. The live detector uses exactly the same confirmation and tolerance logic.")}}
@Composable private fun PrivacyPage(){Column(Modifier.padding(16.dp).padding(top=72.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.Security,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary);Text("Private by design",style=MaterialTheme.typography.headlineSmall);Text("Tap Sentry has no account, network permission, analytics, advertising, backend, or cloud storage. Profiles remain in app-private local storage.");Text("Accessibility",fontWeight=FontWeight.Bold);Text("Used only to dispatch gestures you configure. The service requests no window-content access.");Text("Screen capture",fontWeight=FontWeight.Bold);Text("Requested by Android only when visual auto-stop is enabled. Frames are sampled in memory inside the chosen region and immediately discarded.")}}
