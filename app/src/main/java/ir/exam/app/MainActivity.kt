package ir.exam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ir.exam.app.ui.app.ExamApp
import ir.exam.app.core.ui.ExamAppTheme

/** نقطهٔ ورود Native؛ جایگزین MainActivity مبتنی بر WebView در نسخهٔ قبلی. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExamAppTheme { ExamApp() } }
    }
}
