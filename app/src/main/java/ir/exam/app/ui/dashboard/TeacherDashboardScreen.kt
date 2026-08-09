package ir.exam.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** مقصد اولیهٔ معلم؛ در مراحل بعد به آزمون‌ساز، تصحیح، کلاس‌ها و گزارش وصل می‌شود. */
@Composable fun TeacherDashboardScreen() = Scaffold { padding ->
    Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("داشبورد معلم", style = MaterialTheme.typography.headlineMedium)
        Text("زیرساخت Native Kotlin آماده است. مرحلهٔ بعد: Auth و Supabase.")
    }
}
