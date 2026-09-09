package ir.exam.app.ui.figure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ir.exam.app.ui.math.QuestionToolIcons
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * V134 — یک آیکنِ «گالری» به‌جای سه آیکنِ آناتومی/فیزیک/شیمی در نوار ابزارِ آزمون‌ساز.
 * با لمس، این پنجره ۴ انتخاب دارد: آناتومی، فیزیک، شیمی و «تصویر» (گالری یا دوربین).
 * گزینهٔ چهارم عکسِ خودِ کاربر را در همان ویرایشگرِ آناتومی برای نشانه‌گذاری/نام‌گذاری
 * باز می‌کند (خروجی `{k:'a', t:'photo', X:{img:dataURL, marks…}}`).
 */
enum class FigureGalleryChoice(val label: String) {
    ANATOMY("آناتومی"),
    PHYSICS("فیزیک"),
    CHEMISTRY("شیمی"),
    PHOTO("تصویر")
}

private fun choiceIcon(choice: FigureGalleryChoice): ImageVector = when (choice) {
    FigureGalleryChoice.ANATOMY -> QuestionToolIcons.Anatomy
    FigureGalleryChoice.PHYSICS -> QuestionToolIcons.Physics
    FigureGalleryChoice.CHEMISTRY -> QuestionToolIcons.Chemistry
    FigureGalleryChoice.PHOTO -> Icons.Outlined.Photo
}

@Composable
fun FigureGalleryChooserDialog(
    onDismiss: () -> Unit,
    onChoice: (FigureGalleryChoice) -> Unit,
    /** تصویرِ انتخاب‌شده از گالری/دوربین به‌صورت data-URL (JPEG). */
    onPhoto: (dataUrl: String) -> Unit
) {
    val context = LocalContext.current
    var photoMenu by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val data = runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
                val bytes = input.use { it.readBytes() }
                encodePhotoDataUrl(bytes)
            }.getOrNull()
            if (data != null) onPhoto(data)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingCameraFile
        pendingCameraFile = null
        if (ok && f != null) {
            val data = runCatching { encodePhotoDataUrl(f.readBytes()) }.getOrNull()
            f.delete()
            if (data != null) onPhoto(data)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (photoMenu) "تصویر از کجا؟" else "گالری شکل‌ها") },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (photoMenu) photoMenu = false else onDismiss() }) {
                Text(if (photoMenu) "بازگشت" else "انصراف")
            }
        },
        text = {
            if (photoMenu) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GalleryTile(Icons.Outlined.Photo, "گالری", Modifier.weight(1f)) {
                        galleryLauncher.launch("image/*")
                    }
                    GalleryTile(Icons.Outlined.PhotoCamera, "دوربین", Modifier.weight(1f)) {
                        val dir = File(context.cacheDir, "studio").apply { mkdirs() }
                        val f = File.createTempFile("atlas-", ".jpg", dir)
                        pendingCameraFile = f
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                        cameraLauncher.launch(uri)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(FigureGalleryChoice.entries.toList(), key = { it.name }) { choice ->
                        GalleryTile(choiceIcon(choice), choice.label) {
                            if (choice == FigureGalleryChoice.PHOTO) photoMenu = true else onChoice(choice)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun GalleryTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * V134 — عکسِ کاربر با سقفِ ضلع ۱۲۸۰px و JPEG کیفیت ۸۲ به data-URL تبدیل می‌شود تا
 * توکنِ `%%FIG%%` داخل متنِ سؤال و پیش‌نمایشِ چاپ سبک بماند.
 */
internal fun encodePhotoDataUrl(bytes: ByteArray, maxEdge: Int = 1280): String? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
    val bmp: Bitmap = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null
    val bos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos)
    return "data:image/jpeg;base64," +
        android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
}
