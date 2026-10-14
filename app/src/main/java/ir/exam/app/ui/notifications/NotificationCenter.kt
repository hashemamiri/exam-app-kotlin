package ir.exam.app.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.repository.AppNotification
import ir.exam.app.data.repository.NotificationFeed
import ir.exam.app.data.repository.SupabaseNotificationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * V284 — زنگولهٔ اعلان در هدر: شمارندهٔ خوانده‌نشده (هر ۹۰ ثانیه تازه می‌شود) و با لمس، فهرست اعلان‌های گذشته باز می‌شود؛
 * باز شدن فهرست = همه خوانده‌شده. لمس هر اعلان → onOpen(page, code) برای رفتن به مقصد (مثل ضربه روی اعلان سیستم).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBell(userId: String, tint: Color, onOpen: (page: String?, code: String?) -> Unit) {
    val repo = remember { SupabaseNotificationRepository() }
    val scope = rememberCoroutineScope()
    var feed by remember(userId) { mutableStateOf<NotificationFeed?>(null) }
    var open by remember(userId) { mutableStateOf(false) }
    var loading by remember(userId) { mutableStateOf(false) }
    var refreshTick by remember(userId) { mutableStateOf(0) }
    LaunchedEffect(userId, refreshTick) {
        while (true) {
            repo.feed().onSuccess { feed = it }
            delay(90_000L)
        }
    }
    val unread = feed?.unread ?: 0
    IconButton(onClick = {
        open = true
        loading = true
        refreshTick++
    }) {
        BadgedBox(badge = { if (unread > 0) Badge { Text(PersianDigits.convert(unread.coerceAtMost(99))) } }) {
            Icon(Icons.Outlined.Notifications, contentDescription = "اعلان‌ها", tint = tint)
        }
    }
    if (open) {
        LaunchedEffect(open) {
            repo.feed().onSuccess { feed = it }
            loading = false
        }
        var tab by remember(open) { mutableStateOf("همه") }
        val all = feed?.items.orEmpty()
        val tabs = listOf("همه") + (SupabaseNotificationRepository.CATEGORIES.map { it.first } + SupabaseNotificationRepository.OTHER)
            .filter { c -> all.any { SupabaseNotificationRepository.categoryOf(it.kind) == c } }
        val shown = if (tab == "همه") all else all.filter { SupabaseNotificationRepository.categoryOf(it.kind) == tab }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("اعلان‌ها") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (tabs.size > 2) {
                        // V284.4 — دسته‌بندی: چیپ‌های افقی با شمارندهٔ خوانده‌نشدهٔ هر دسته
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tabs.forEach { c ->
                                val n = all.count { !it.read && (c == "همه" || SupabaseNotificationRepository.categoryOf(it.kind) == c) }
                                FilterChip(selected = tab == c, onClick = { tab = c }, label = { Text(if (n > 0) "$c (${PersianDigits.convert(n)})" else c) })
                            }
                        }
                    }
                    when {
                        loading && feed == null -> CircularProgressIndicator()
                        shown.isEmpty() -> Text("هنوز اعلانی ندارید.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                        else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                            items(shown, key = { it.id }) { n ->
                                NotificationRow(n) {
                                    // فقط همین اعلان خوانده‌شده می‌شود
                                    feed = feed?.let { f -> f.copy(unread = (f.unread - (if (n.read) 0 else 1)).coerceAtLeast(0), items = f.items.map { if (it.id == n.id) it.copy(read = true) else it }) }
                                    scope.launch { repo.markRead(listOf(n.id)) }
                                    open = false
                                    onOpen(n.page, n.code)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            dismissButton = {
                if (unread > 0) TextButton(onClick = {
                    feed = feed?.let { f -> f.copy(unread = 0, items = f.items.map { it.copy(read = true) }) }
                    scope.launch { repo.markAllRead() }
                }) { Text("همه خوانده شد") }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("بستن") } }
        )
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!n.read) Badge()
            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(relativeTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(n.body, style = MaterialTheme.typography.bodySmall)
    }
}

/** تاریخ شمسی کوتاه + ساعت تهران؛ بدون کتابخانهٔ اضافه. */
internal fun relativeTime(iso: String): String = runCatching {
    val t = java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.of("Asia/Tehran"))
    val j = JalaliCalendar.fromGregorian(t.toLocalDate())
    PersianDigits.convert("${j.month}/${j.day} ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}")
}.getOrDefault("")
