package ir.exam.app.core.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN فقط به شکل PBKDF2+salt در sandbox ذخیره می‌شود؛ خود PIN هرگز نوشته نمی‌شود. */
class AppLockManager(context:Context){private val p=context.applicationContext.getSharedPreferences("native_app_lock",Context.MODE_PRIVATE)
 fun enabled(user:String)=p.getBoolean("enabled_$user",false)
 fun deviceCredential(user:String)=p.getBoolean("device_$user",true)
 fun setDeviceCredential(user:String,value:Boolean){p.edit().putBoolean("device_$user",value).apply()}
 fun configure(user:String,pin:String){require(pin.matches(Regex("^[0-9]{4,8}$"))){"پین باید ۴ تا ۸ رقم باشد."};val salt=ByteArray(16).also{SecureRandom().nextBytes(it)};val hash=derive(pin,salt);p.edit().putString("salt_$user",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("hash_$user",Base64.encodeToString(hash,Base64.NO_WRAP)).putBoolean("enabled_$user",true).commit()}
 fun disable(user:String){p.edit().remove("salt_$user").remove("hash_$user").putBoolean("enabled_$user",false).commit()}
 fun verify(user:String,pin:String):Boolean{val salt=p.getString("salt_$user",null)?.let{Base64.decode(it,Base64.NO_WRAP)}?:return false;val expected=p.getString("hash_$user",null)?.let{Base64.decode(it,Base64.NO_WRAP)}?:return false;return java.security.MessageDigest.isEqual(expected,derive(pin,salt))}
 private fun derive(pin:String,salt:ByteArray)=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pin.toCharArray(),salt,120_000,256)).encoded
}
