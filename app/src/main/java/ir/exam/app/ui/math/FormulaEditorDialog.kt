package ir.exam.app.ui.math

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.math.NativeMathFormatter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MODE_BOX = "box"
private const val MODE_TYPE = "type"
private const val MODE_GALLERY = "gallery"

@Composable
fun FormulaEditorDialog(onDismiss: () -> Unit, onInsert: (String) -> Unit) {
    val context = LocalContext.current
    val library = remember { FormulaReferenceLibrary.load(context) }
    val store = remember { FormulaReferenceStore(context) }
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var mode by remember { mutableStateOf(MODE_BOX) }
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var natural by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("common") }
    var groupDialog by remember { mutableStateOf<FormulaReferenceGroup?>(null) }
    var quickMenuTitle by remember { mutableStateOf<String?>(null) }
    var quickMenuItems by remember { mutableStateOf<List<FormulaReferenceEntry>>(emptyList()) }
    var symbolQuery by remember { mutableStateOf("") }
    var galleryQuery by remember { mutableStateOf("") }
    var quickConvertOpen by remember { mutableStateOf(false) }
    var quickConvert by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var uppercase by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(store.favorites()) }
    var recentFormulas by remember { mutableStateOf(store.recentFormulas()) }
    var recentSymbols by remember { mutableStateOf(store.recentSymbols()) }
    var error by remember { mutableStateOf<String?>(null) }
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }

    fun setValue(next: TextFieldValue, rememberUndo: Boolean = true) {
        if (rememberUndo && next.text != value.text) {
            undo.add(value)
            if (undo.size > 80) undo.removeAt(0)
            redo.clear()
        }
        val text = next.text.take(8000)
        value = next.copy(text = text, selection = TextRange(next.selection.start.coerceAtMost(text.length), next.selection.end.coerceAtMost(text.length)))
        error = null
    }

    fun replace(text: String) {
        val safe = text.take(8000)
        setValue(TextFieldValue(safe, TextRange(safe.length)))
    }

    fun insert(text: String) {
        val start = value.selection.min.coerceIn(0, value.text.length)
        val end = value.selection.max.coerceIn(start, value.text.length)
        val next = (value.text.substring(0, start) + text + value.text.substring(end)).take(8000)
        setValue(TextFieldValue(next, TextRange((start + text.length).coerceAtMost(next.length))))
    }

    fun backspace() {
        val start = value.selection.min
        val end = value.selection.max
        if (start != end) {
            setValue(TextFieldValue(value.text.removeRange(start, end), TextRange(start)))
        } else if (start > 0) {
            setValue(TextFieldValue(value.text.removeRange(start - 1, start), TextRange(start - 1)))
        }
    }

    fun moveCursor(delta: Int) {
        val position = (value.selection.end + delta).coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(position))
    }

    fun useEntry(entry: FormulaReferenceEntry) {
        insert(entry.tex)
        store.addRecentSymbol(entry)
        recentSymbols = store.recentSymbols()
    }

    fun openMenu(title: String, entries: List<FormulaReferenceEntry>) {
        quickMenuTitle = title
        quickMenuItems = entries
    }

    fun currentTex(): String = when (mode) {
        MODE_TYPE -> NativeMathFormatter.quickToTex(natural)
        else -> value.text
    }.trim()

    val selectedEntries = remember(categoryId, symbolQuery, uppercase, favorites, recentSymbols, library) {
        val base = when (categoryId) {
            "__all" -> library.allItems
            "__favorites" -> favorites
            "__recent_symbols" -> recentSymbols
            "letters" -> (if (uppercase) 'A'..'Z' else 'a'..'z').map {
                FormulaReferenceEntry("حرف $it", it.toString())
            }
            else -> library.categoryById[categoryId]?.items.orEmpty()
        }
        val filtered = if (symbolQuery.isBlank()) {
            base
        } else {
            val query = symbolQuery.trim().lowercase()
            (library.allItems + library.categoryById["unicode"]?.items.orEmpty()).filter {
                it.label.lowercase().contains(query) || it.tex.lowercase().contains(query)
            }
        }
        filtered.distinctBy { it.label + "¦" + it.tex }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("🧮 نوشتن فرمول", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        MODE_BOX to "🖱️ جعبه‌ای",
                        MODE_TYPE to "⌨️ تایپ سریع",
                        MODE_GALLERY to "📚 آماده"
                    ).forEach { (itemMode, label) ->
                        FilterChip(
                            selected = mode == itemMode,
                            onClick = { mode = itemMode },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider()
                Box(Modifier.weight(1f)) {
                    when (mode) {
                        MODE_BOX -> LazyColumn(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "روی کادر SVG بزنید و بنویسید. دکمه‌های ▾ چند حالت دارند؛ کد داخلی فقط در بخش حرفه‌ای قابل مشاهده است.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                if (undo.isNotEmpty()) {
                                                    redo.add(value)
                                                    value = undo.removeAt(undo.lastIndex)
                                                }
                                            },
                                            enabled = undo.isNotEmpty()
                                        ) { Text("↩ بازگشت") }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = {
                                                if (redo.isNotEmpty()) {
                                                    undo.add(value)
                                                    value = redo.removeAt(redo.lastIndex)
                                                }
                                            },
                                            enabled = redo.isNotEmpty()
                                        ) { Text("↪ جلو") }
                                    }
                                    item {
                                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(value.text)) }) {
                                            Text("📋 کپی")
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { clipboard.getText()?.text?.let(::insert) }) {
                                            Text("📥 پیست")
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { zoom = (zoom - .1f).coerceAtLeast(.7f) }) {
                                            Text("A−")
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { zoom = (zoom + .1f).coerceAtMost(1.7f) }) {
                                            Text("A+")
                                        }
                                    }
                                }
                            }
                            item {
                                Text("کادر ساختاری فرمول — نمایش SVG", style = MaterialTheme.typography.labelLarge)
                                SvgFormulaEditorSurface(
                                    value = value,
                                    onValueChange = { setValue(it) },
                                    focusRequester = focusRequester,
                                    zoom = zoom,
                                    onRequestKeyboard = { keyboard?.show() }
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        CategoryButton("⭐ موارد پرکاربرد", categoryId == "common") {
                                            categoryId = "common"
                                            symbolQuery = ""
                                        }
                                    }
                                    items(library.groups, key = { it.key }) { group ->
                                        FormulaCategoryButton(group, false) { groupDialog = group }
                                    }
                                    item {
                                        CategoryButton("🔍 همهٔ نمادها", categoryId == "__all") {
                                            categoryId = "__all"
                                            symbolQuery = ""
                                        }
                                    }
                                    item {
                                        CategoryButton("⚙ یونیکد (۱۲۰۰)", categoryId == "unicode") {
                                            categoryId = "unicode"
                                            symbolQuery = ""
                                        }
                                    }
                                    item {
                                        CategoryButton("⭐ علاقه‌مندی", categoryId == "__favorites") {
                                            categoryId = "__favorites"
                                            symbolQuery = ""
                                        }
                                    }
                                }
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item {
                                        QuickButton("🕘 اخیر") {
                                            openMenu(
                                                "فرمول‌های اخیر",
                                                recentFormulas.mapIndexed { index, tex ->
                                                    FormulaReferenceEntry("فرمول اخیر ${index + 1}", tex)
                                                }
                                            )
                                        }
                                    }
                                    item { QuickButton("✨ تبدیل") { quickConvertOpen = !quickConvertOpen } }
                                    item { FormulaSvgButton("\\log", "لگاریتم") { openMenu("لگاریتم", logItems) } }
                                    item { FormulaSvgButton("\\int", "انتگرال") { openMenu("انتگرال", integralItems) } }
                                    item { FormulaSvgButton("٫ \\%", "اعشار و درصد") { openMenu("اعشار و درصد", percentItems) } }
                                    item { FormulaSvgButton("\\sin", "مثلثات") { openMenu("مثلثات", trigItems) } }
                                }
                            }
                            if (quickConvertOpen) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        OutlinedTextField(
                                            quickConvert,
                                            { quickConvert = it },
                                            label = { Text("مثلاً x^2 + a/b <= sqrt(16)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(onClick = {
                                            val converted = NativeMathFormatter.quickToTex(quickConvert)
                                            if (converted.isNotBlank()) {
                                                insert(converted)
                                                quickConvert = ""
                                                quickConvertOpen = false
                                            }
                                        }) { Text("تبدیل") }
                                        TextButton(onClick = { quickConvertOpen = false }) { Text("بستن") }
                                    }
                                }
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item {
                                        Button(onClick = {
                                            val tex = value.text.trim()
                                            if (tex.isNotBlank()) {
                                                store.addRecentFormula(tex)
                                                onInsert(tex)
                                            }
                                        }) { Text("درج") }
                                    }
                                    item { QuickButton("↵") { insert("\\\\") } }
                                    item {
                                        QuickButton("abc") {
                                            categoryId = "letters"
                                            symbolQuery = ""
                                            uppercase = !uppercase
                                        }
                                    }
                                    item {
                                        FormulaSvgButton("\\frac{a}{b}", "کسر", dropdown = true, iconWidth = 48) {
                                            openMenu("کسر", fractionItems)
                                        }
                                    }
                                    item {
                                        FormulaSvgButton("x^{n}", "توان", dropdown = true, iconWidth = 42) {
                                            openMenu("توان", powerItems)
                                        }
                                    }
                                    item {
                                        FormulaSvgButton("\\sqrt{x}", "رادیکال", dropdown = true, iconWidth = 46) {
                                            openMenu("رادیکال", rootItems)
                                        }
                                    }
                                }
                            }
                            item {
                                FixedFormulaKeypad(
                                    onInsert = ::insert,
                                    onBackspace = ::backspace,
                                    onMove = ::moveCursor,
                                    onKeyboard = {
                                        focusRequester.requestFocus()
                                        keyboard?.show()
                                    },
                                    onClear = { replace("") },
                                    onParenthesis = { insert(it) }
                                )
                            }
                            item {
                                OutlinedTextField(
                                    symbolQuery,
                                    { symbolQuery = it },
                                    label = { Text("🔍 جست‌وجوی نماد یا نام فارسی…") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        (if (symbolQuery.isBlank()) library.categoryById[categoryId]?.label else "نتایج جست‌وجو")
                                            ?: when (categoryId) {
                                                "__all" -> "همهٔ نمادها"
                                                "__favorites" -> "علاقه‌مندی"
                                                "__recent_symbols" -> "اخیر"
                                                else -> "نمادها"
                                            },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (categoryId == "letters") {
                                        TextButton(onClick = { uppercase = !uppercase }) {
                                            Text(if (uppercase) "A→a" else "a→A")
                                        }
                                    }
                                }
                            }
                            item {
                                SymbolGrid(
                                    selectedEntries,
                                    onUse = ::useEntry,
                                    onFavorite = { entry ->
                                        store.toggleFavorite(entry)
                                        favorites = store.favorites()
                                    }
                                )
                            }
                            item {
                                TextButton(onClick = { showCode = !showCode }) {
                                    Text(if (showCode) "بستن کد فرمول" else "کد فرمول (کاربران حرفه‌ای)")
                                }
                            }
                            if (showCode) {
                                item { Text(value.text.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall) }
                            }
                        }

                        MODE_TYPE -> QuickTypePane(
                            natural,
                            { natural = it },
                            onToBox = {
                                replace(NativeMathFormatter.quickToTex(natural))
                                mode = MODE_BOX
                            }
                        )

                        else -> GalleryPane(library, galleryQuery, { galleryQuery = it }) { entry ->
                            replace(entry.tex)
                            mode = MODE_BOX
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(
                        onClick = {
                            val tex = currentTex()
                            if (tex.isBlank()) {
                                onDismiss()
                            } else if (!NativeMathFormatter.isBalanced(tex)) {
                                error = "آکولادهای فرمول متوازن نیستند."
                            } else {
                                store.addRecentFormula(tex)
                                onInsert(tex)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("✅ درج در سؤال") }
                    OutlinedButton(onClick = { if (mode == MODE_TYPE) natural = "" else replace("") }) {
                        Text("🧹 پاک")
                    }
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                }
            }
        }
    }

    groupDialog?.let { group ->
        AlertDialog(
            onDismissRequest = { groupDialog = null },
            title = { Text(group.label) },
            text = {
                LazyColumn {
                    items(group.categories, key = { it.id }) { link ->
                        TextButton(
                            onClick = {
                                categoryId = link.id
                                symbolQuery = ""
                                groupDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(link.label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { groupDialog = null }) { Text("بستن") } }
        )
    }

    quickMenuTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { quickMenuTitle = null },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(quickMenuItems, key = { it.label + "¦" + it.tex }) { entry ->
                        TextButton(
                            onClick = {
                                useEntry(entry)
                                quickMenuTitle = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NativeFormulaIcon(
                                    entry.tex,
                                    Modifier.width(84.dp).height(46.dp),
                                    20.sp,
                                    contentDescription = entry.label
                                )
                                Text(entry.label, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { quickMenuTitle = null }) { Text("بستن") } }
        )
    }
}

@Composable
private fun SvgFormulaEditorSurface(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    zoom: Float,
    onRequestKeyboard: () -> Unit
) {
    Card(Modifier.fillMaxWidth().height(160.dp)) {
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            if (value.text.isBlank()) {
                Text("برای نوشتن فرمول این کادر SVG را لمس کنید")
            } else {
                NativeFormulaView(
                    value.text,
                    Modifier.fillMaxWidth(),
                    (22 * zoom).sp,
                    contentDescription = "فرمول در حال ویرایش"
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .88f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "SVG • جایگاه درج ${value.selection.end + 1}",
                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .graphicsLayer { alpha = .002f },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                decorationBox = { innerField -> innerField() }
            )
        }
    }
    TextButton(
        onClick = {
            focusRequester.requestFocus()
            onRequestKeyboard()
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("⌨️ باز کردن صفحه‌کلید برای ویرایش تصویری") }
}

@Composable
private fun CategoryButton(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun FormulaCategoryButton(group: FormulaReferenceGroup, selected: Boolean, onClick: () -> Unit) {
    val prefix = group.label.substringBefore(' ')
    val title = group.label.substringAfter(' ', group.label)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (prefix in setOf("🔢", "📐", "🚀", "🧪")) {
                    Text(prefix)
                } else {
                    NativeFormulaIcon(prefix, Modifier.size(24.dp), 18.sp, contentDescription = prefix)
                }
                Text(title)
            }
        }
    )
}

@Composable
private fun QuickButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(text) }
}

@Composable
private fun FormulaSvgButton(
    tex: String,
    description: String,
    dropdown: Boolean = false,
    iconWidth: Int = 42,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
    ) {
        NativeFormulaIcon(
            tex,
            Modifier.width(iconWidth.dp).height(28.dp),
            20.sp,
            contentDescription = description
        )
        if (dropdown) Text("▾")
    }
}

@Composable
private fun SymbolGrid(
    entries: List<FormulaReferenceEntry>,
    onUse: (FormulaReferenceEntry) -> Unit,
    onFavorite: (FormulaReferenceEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Text("موردی پیدا نشد.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier.fillMaxWidth().height(310.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(entries, key = { it.label + "¦" + it.tex }) { entry ->
            Card {
                Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { onUse(entry) }, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NativeFormulaIcon(
                                entry.tex,
                                Modifier.fillMaxWidth().height(44.dp),
                                20.sp,
                                contentDescription = entry.label
                            )
                            Text(entry.label.take(35), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(onClick = { onFavorite(entry) }) { Text("☆") }
                }
            }
        }
    }
}

@Composable
private fun FixedFormulaKeypad(
    onInsert: (String) -> Unit,
    onBackspace: () -> Unit,
    onMove: (Int) -> Unit,
    onKeyboard: () -> Unit,
    onClear: () -> Unit,
    onParenthesis: (String) -> Unit
) {
    val rows = listOf(
        listOf("(", ")", "7", "8", "9", "⌫"),
        listOf("↑", "↓", "4", "5", "6", "÷"),
        listOf("←", "→", "1", "2", "3", "×"),
        listOf("⌨", "C", "0", "=", "+", "−")
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                "(", ")" -> onParenthesis(key)
                                "⌫" -> onBackspace()
                                "↑", "←" -> onMove(-1)
                                "↓", "→" -> onMove(1)
                                "⌨" -> onKeyboard()
                                "C" -> onClear()
                                "÷" -> onInsert("\\div ")
                                "×" -> onInsert("\\times ")
                                "−" -> onInsert("-")
                                else -> onInsert(key)
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        if (key in setOf("⌫", "⌨", "C")) {
                            Text(key)
                        } else {
                            NativeFormulaIcon(
                                key,
                                Modifier.fillMaxWidth().height(25.dp),
                                19.sp,
                                contentDescription = "کلید $key"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTypePane(value: String, onChange: (String) -> Unit, onToBox: () -> Unit) {
    val tex = NativeMathFormatter.quickToTex(value)
    var showCode by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("طبیعی بنویسید؛ نتیجه به‌صورت SVG نمایش داده می‌شود و نیازی به تغییر زبان کیبورد نیست.") }
        item {
            Card(Modifier.fillMaxWidth().height(150.dp)) {
                Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    if (tex.isBlank()) Text("اینجا نتیجهٔ SVG را می‌بینید")
                    else NativeFormulaView(tex, Modifier.fillMaxWidth(), contentDescription = "نتیجه تایپ سریع")
                }
            }
        }
        item {
            OutlinedTextField(
                value,
                onChange,
                label = { Text("مثلاً: 7/8 * 6/8") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                quickTips.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { tip ->
                            Card(Modifier.weight(1f)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    NativeFormulaIcon(
                                        NativeMathFormatter.quickToTex(tip.first),
                                        Modifier.fillMaxWidth().height(32.dp),
                                        18.sp,
                                        contentDescription = tip.second
                                    )
                                    Text(tip.second, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { Button(onClick = onToBox, modifier = Modifier.fillMaxWidth()) { Text("✏️ ویرایش در حالت جعبه‌ای") } }
        item {
            TextButton(onClick = { showCode = !showCode }) {
                Text(if (showCode) "بستن کد فرمول" else "کد فرمول (کاربران حرفه‌ای)")
            }
        }
        if (showCode) item { Text(tex.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun GalleryPane(
    library: FormulaReferenceData,
    query: String,
    onQuery: (String) -> Unit,
    onPick: (FormulaReferenceEntry) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("یکی را انتخاب کنید؛ همهٔ پیش‌نمایش‌ها SVG هستند و سپس می‌توانید عددهایش را تغییر دهید.") }
        item {
            OutlinedTextField(
                query,
                onQuery,
                label = { Text("🔍 جست‌وجو در فرمول‌ها…") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        library.gallery.forEach { group ->
            val shown = group.items.filter {
                query.isBlank() || (it.label + " " + it.tex).lowercase().contains(query.lowercase())
            }
            if (shown.isNotEmpty()) {
                item { Text(group.label, style = MaterialTheme.typography.titleMedium) }
                items(shown, key = { it.label + "¦" + it.tex }) { entry ->
                    Card(Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onPick(entry) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(entry.label)
                                NativeFormulaView(
                                    entry.tex,
                                    Modifier.fillMaxWidth(),
                                    18.sp,
                                    contentDescription = entry.label
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private class FormulaReferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences("formula_reference_history", Context.MODE_PRIVATE)
    private val json = Json

    fun favorites() = readEntries("favorites")
    fun recentSymbols() = readEntries("recent_symbols")
    fun recentFormulas() = readStrings("recent_formulas")

    fun toggleFavorite(entry: FormulaReferenceEntry) {
        val list = favorites().toMutableList()
        val index = list.indexOfFirst { it.label == entry.label && it.tex == entry.tex }
        if (index >= 0) list.removeAt(index) else list.add(0, entry)
        writeEntries("favorites", list.take(60))
    }

    fun addRecentSymbol(entry: FormulaReferenceEntry) {
        writeEntries(
            "recent_symbols",
            (listOf(entry) + recentSymbols().filterNot { it.tex == entry.tex && it.label == entry.label }).take(24)
        )
    }

    fun addRecentFormula(tex: String) {
        writeStrings("recent_formulas", (listOf(tex) + recentFormulas().filterNot { it == tex }).take(20))
    }

    private fun readEntries(key: String) = runCatching {
        val array = json.decodeFromString<List<List<String>>>(preferences.getString(key, "[]") ?: "[]")
        array.mapNotNull { if (it.size >= 2) FormulaReferenceEntry(it[0], it[1]) else null }
    }.getOrDefault(emptyList())

    private fun writeEntries(key: String, list: List<FormulaReferenceEntry>) {
        preferences.edit().putString(key, json.encodeToString(list.map { listOf(it.label, it.tex) })).apply()
    }

    private fun readStrings(key: String) = runCatching {
        json.decodeFromString<List<String>>(preferences.getString(key, "[]") ?: "[]")
    }.getOrDefault(emptyList())

    private fun writeStrings(key: String, list: List<String>) {
        preferences.edit().putString(key, json.encodeToString(list)).apply()
    }
}

private val quickTips = listOf(
    "7/8" to "کسر",
    "x^2" to "توان",
    "x^2^3" to "توانِ توان",
    "sqrt2" to "رادیکال",
    "رادیکال ۵" to "رادیکال",
    "(a+b)/2" to "کسر مرکب",
    "pi" to "π",
    ">=" to "≥",
    "!=" to "≠",
    "*" to "×"
)
private val fractionItems = listOf(
    FormulaReferenceEntry("کسر ساده", "\\frac{a}{b}"),
    FormulaReferenceEntry("کسر مخلوط", "3\\frac{1}{2}"),
    FormulaReferenceEntry("خط کسری", "a/b")
)
private val powerItems = listOf(
    FormulaReferenceEntry("توان n", "x^{n}"),
    FormulaReferenceEntry("مربع", "x^{2}"),
    FormulaReferenceEntry("مکعب", "x^{3}"),
    FormulaReferenceEntry("زیرنویس", "x_{1}")
)
private val rootItems = listOf(
    FormulaReferenceEntry("جذر", "\\sqrt{x}"),
    FormulaReferenceEntry("ریشه سوم", "\\sqrt[3]{x}"),
    FormulaReferenceEntry("ریشه چهارم", "\\sqrt[4]{x}"),
    FormulaReferenceEntry("فرجه دلخواه", "\\sqrt[n]{x}")
)
private val logItems = listOf(
    FormulaReferenceEntry("log", "\\log"),
    FormulaReferenceEntry("ln", "\\ln"),
    FormulaReferenceEntry("log با مبنا", "\\log_{a}(x)"),
    FormulaReferenceEntry("e^x", "e^{x}"),
    FormulaReferenceEntry("10^x", "10^{x}")
)
private val integralItems = listOf(
    FormulaReferenceEntry("انتگرال ساده", "\\int f(x) dx"),
    FormulaReferenceEntry("انتگرال معین", "\\int_{a}^{b} f(x) dx"),
    FormulaReferenceEntry("انتگرال دوگانه", "\\iint"),
    FormulaReferenceEntry("انتگرال سه‌گانه", "\\iiint"),
    FormulaReferenceEntry("انتگرال بسته", "\\oint")
)
private val percentItems = listOf(
    FormulaReferenceEntry("ممیز", "."),
    FormulaReferenceEntry("درصد", "\\%"),
    FormulaReferenceEntry("در هزار", "‰"),
    FormulaReferenceEntry("درصد فرمولی", "\\frac{a}{b} \\times 100")
)
private val trigItems = listOf("sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan")
    .map { FormulaReferenceEntry(it, "\\$it") }
