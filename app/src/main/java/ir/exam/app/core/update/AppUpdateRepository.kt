package ir.exam.app.core.update

data class RemoteVersion(val code:Int,val name:String,val notesFa:List<String>,val apkUrl:String)
/** منبع نسخه از Supabase یا API امن؛ UI فقط مدل RemoteVersion را می‌بیند. */
interface AppUpdateRepository { suspend fun latest():Result<RemoteVersion> }
