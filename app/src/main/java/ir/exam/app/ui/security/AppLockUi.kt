package ir.exam.app.ui.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ir.exam.app.core.security.AppLockManager

@Composable
fun AppLockGate(userId:String,content: @Composable ()->Unit){
 val context=LocalContext.current;val manager=remember{AppLockManager(context)};var locked by remember(userId){mutableStateOf(manager.enabled(userId))};val lifecycle=LocalLifecycleOwner.current
 DisposableEffect(lifecycle,userId){val o=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_STOP&&manager.enabled(userId))locked=true};lifecycle.lifecycle.addObserver(o);onDispose{lifecycle.lifecycle.removeObserver(o)}}
 if(!locked){content();return}
 var pin by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};var attempts by remember{mutableIntStateOf(0)};var blockedUntil by remember{mutableLongStateOf(0L)}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==Activity.RESULT_OK){locked=false;attempts=0}}
 Column(Modifier.fillMaxSize().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Text("برنامه قفل است",style=MaterialTheme.typography.headlineSmall);Text("برای دسترسی به اطلاعات آزمون، پین یا قفل امن دستگاه را تأیید کنید.")
  OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(8);error=null},label={Text("پین ۴ تا ۸ رقم")},visualTransformation=PasswordVisualTransformation())
  Button(enabled=pin.length>=4&&System.currentTimeMillis()>=blockedUntil,onClick={if(manager.verify(userId,pin)){locked=false;attempts=0}else{attempts++;error="پین نادرست است.";if(attempts>=5){blockedUntil=System.currentTimeMillis()+30_000;attempts=0;error="۳۰ ثانیه صبر کنید."}}},modifier=Modifier.fillMaxWidth()){Text("بازکردن")}
  if(manager.deviceCredential(userId))OutlinedButton(onClick={val km=context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager;val intent=km.createConfirmDeviceCredentialIntent("بازکردن سامانه آزمون","اثر انگشت، چهره، الگو یا رمز دستگاه");if(intent!=null)launcher.launch(intent)else error="قفل امن دستگاه فعال نیست."},modifier=Modifier.fillMaxWidth()){Text("اثر انگشت / قفل دستگاه")}
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }
}

@Composable
fun AppLockSettings(userId:String){
 val context=LocalContext.current;val manager=remember{AppLockManager(context)};var enabled by remember(userId){mutableStateOf(manager.enabled(userId))};var device by remember(userId){mutableStateOf(manager.deviceCredential(userId))};var pin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var message by remember{mutableStateOf<String?>(null)}
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("قفل برنامه",style=MaterialTheme.typography.titleMedium);Text("پس از رفتن برنامه به پس‌زمینه، دسترسی دوباره قفل می‌شود.")
  if(!enabled){OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(8)},label={Text("پین جدید")},visualTransformation=PasswordVisualTransformation());OutlinedTextField(confirm,{confirm=it.filter(Char::isDigit).take(8)},label={Text("تکرار پین")},visualTransformation=PasswordVisualTransformation());Button(enabled=pin.length in 4..8&&pin==confirm,onClick={runCatching{manager.configure(userId,pin)}.onSuccess{enabled=true;pin="";confirm="";message="قفل فعال شد."}.onFailure{message=it.message}}){Text("فعال‌سازی")}}
  else{Row(verticalAlignment=Alignment.CenterVertically){Text("استفاده از اثر انگشت/قفل دستگاه",Modifier.weight(1f));Switch(device,{device=it;manager.setDeviceCredential(userId,it)})};OutlinedButton(onClick={manager.disable(userId);enabled=false;message="قفل غیرفعال شد."}){Text("غیرفعال‌کردن قفل")}}
  message?.let{Text(it)}
 }}
}
