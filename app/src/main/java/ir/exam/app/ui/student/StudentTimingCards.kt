package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.exam.StudentTiming
import kotlinx.coroutines.delay

/**
 * V359 — نمایش زمان هر سؤال و کارت آفلاین، سمت دانش‌آموز.
 *
 * این فایل فقط نمایش می‌دهد؛ تصمیم را [StudentTiming] می‌گیرد و مهلت
 * را سرور می‌سازد. منطق و نمایش جدا ماندند تا منطق بدون اندروید آزمون
 * شود (V359_StudentTimingTest).
 */

/**
 * تراشهٔ شمارش معکوس سؤال. تیک محلی فقط نمایش را تازه می‌کند؛ مهلت
 * از [clock] می‌آید که خودش از سرور تراز شده است.
 */
@Composable
fun QuestionClockChip(clock: StudentTiming.QuestionClock?, primary: Boolean = false) {
    if (clock == null || !clock.limited || clock.locked) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(clock.deadlineMs) {
        while (true) {
            now = System.currentTimeMillis()
            if (StudentTiming.isLocked(clock, now)) break
            delay(1_000L)
        }
    }
    val text = StudentTiming.clockText(clock, now)
    if (text.isEmpty()) return
    val warn = StudentTiming.isWarning(clock, now)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (warn) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                "⏳ " + toPersianDigits(text),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        if (!primary) {
            Text(
                "زمان این سؤال؛ پس از پایان، همین سؤال قفل می‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** پیام قفل سؤال؛ پاسخ ثبت‌شده حفظ می‌شود. */
@Composable
fun QuestionLockedCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            "زمان این سؤال تمام شد؛ پاسخ ثبت‌شده حفظ شده است و امکان ویرایش نیست.",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * کارت حالت آفلاین — همتای `offlineCard()` سایت.
 * برای مناطق کم‌پوشش: بسته پیش از قطع اینترنت گرفته می‌شود، مهلت
 * همان مهلت سرور است، و برگه با نشان «آفلاین» به معلم می‌رسد.
 */
@Composable
fun OfflineExamCard(
    pack: StudentTiming.OfflinePack?,
    practice: Boolean,
    busy: Boolean,
    message: String?,
    onTake: () -> Unit,
    onSubmit: () -> Unit
) {
    if (practice) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pack?.alignedMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val status = StudentTiming.offlineStatus(pack, now)
    Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📴 حالت آفلاین", style = MaterialTheme.typography.titleSmall)
                Text(
                    StudentTiming.offlineLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status == StudentTiming.OfflineStatus.READY) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                StudentTiming.offlineHint(status),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (status == StudentTiming.OfflineStatus.READY && pack != null) {
                val left = ir.exam.app.domain.exam.ExamTiming.remainingMs(pack.alignedMs, now)
                Text(
                    "مهلت باقی‌مانده: " + toPersianDigits(
                        ir.exam.app.domain.exam.ExamTiming.formatClock(left)
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (StudentTiming.canTakePack(pack, practice)) {
                    OutlinedButton(onClick = onTake, enabled = !busy) {
                        Text(if (busy) "در حال گرفتن..." else "گرفتن بستهٔ آفلاین")
                    }
                }
                if (StudentTiming.canSubmitOffline(pack, now)) {
                    Button(onClick = onSubmit, enabled = !busy) {
                        Text(if (busy) "در حال ارسال..." else "ارسال آفلاین")
                    }
                }
            }
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** رقم لاتین به فارسی؛ سایت هم همین کار را می‌کند. */
internal fun toPersianDigits(text: String): String =
    text.map { c -> if (c in '0'..'9') ('\u06F0' + (c - '0')) else c }.joinToString("")
