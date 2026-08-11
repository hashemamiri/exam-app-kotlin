package ir.exam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.AppearanceSettings
import ir.exam.app.core.ui.ExamAppTheme
import ir.exam.app.ui.app.ExamApp

/** نقطهٔ ورود Native؛ ظاهر ماندگار پیش از رندر کل برنامه اعمال می‌شود. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            val appearance by appearancePreferences.settings.collectAsState(initial = AppearanceSettings())
            ExamAppTheme(appearance) {
                ExamApp(appearance = appearance)
            }
        }
    }
}
