package ir.exam.app.ui.math

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.math.FormulaBoxEditor
import ir.exam.app.core.math.FormulaMatrixFactory
import ir.exam.app.core.math.NativeMathFormatter

private const val MODE_BOX = "box"
private const val MODE_TYPE = "type"
private const val MODE_GALLERY = "gallery"

@Composable
fun FormulaEditorDialog(
    initialTex: String = "",
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit
) {
    val context = LocalContext.current
    val library = remember { FormulaReferenceLibrary.load(context) }
    val store = remember { FormulaReferenceStore(context) }
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var mode by remember { mutableStateOf(MODE_BOX) }
    var value by remember(initialTex) {
        val initial = FormulaBoxEditor.replaceAll(initialTex, activateFirstBox = true)
        mutableStateOf(TextFieldValue(initial.text, TextRange(initial.selectionStart, initial.selectionEnd)))
    }
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
    var notice by remember { mutableStateOf<String?>(null) }
    var smartHubOpen by remember { mutableStateOf(false) }
    var recentDialogOpen by remember { mutableStateOf(false) }
    var matrixPickerOpen by remember { mutableStateOf(false) }
    var delimiterPickerOpen by remember { mutableStateOf(false) }
    var customDelimiterOpen by remember { mutableStateOf("(") }
    var customDelimiterClose by remember { mutableStateOf(")") }
    var matrixRows by remember { mutableIntStateOf(2) }
    var matrixColumns by remember { mutableIntStateOf(2) }
    var expandedLibraryTitle by remember { mutableStateOf<String?>(null) }
    var expandedLibraryItems by remember { mutableStateOf<List<FormulaReferenceEntry>>(emptyList()) }
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

    fun replace(text: String, activateFirstBox: Boolean = false) {
        val result = FormulaBoxEditor.replaceAll(text, activateFirstBox)
        setValue(
            TextFieldValue(
                result.text,
                TextRange(result.selectionStart, result.selectionEnd)
            )
        )
    }

    fun insert(
        text: String,
        activateFirstBox: Boolean = false,
        replaceActiveBox: Boolean = false
    ) {
        val result = FormulaBoxEditor.insert(
            current = value.text,
            selectionStart = value.selection.start,
            selectionEnd = value.selection.end,
            insertion = text,
            activateFirstInsertedBox = activateFirstBox,
            replaceActiveBoxWhenCollapsed = replaceActiveBox
        )
        setValue(
            TextFieldValue(
                result.text,
                TextRange(result.selectionStart, result.selectionEnd)
            )
        )
    }

    fun applyEdit(result: ir.exam.app.core.math.FormulaBoxEditResult, rememberUndo: Boolean = true) {
        setValue(TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd)), rememberUndo)
    }

    fun backspace() {
        applyEdit(FormulaBoxEditor.backspace(value.text, value.selection.start, value.selection.end))
    }

    fun moveActiveBox(delta: Int) {
        val moved = FormulaBoxEditor.moveActiveBox(
            value.text,
            value.selection.start,
            value.selection.end,
            delta
        )
        value = value.copy(selection = TextRange(moved.selectionStart, moved.selectionEnd))
    }

    fun moveSpatialBox(direction: Int) {
        val moved = FormulaBoxEditor.moveSpatialBox(value.text, value.selection.start, value.selection.end, direction)
        value = value.copy(selection = TextRange(moved.selectionStart, moved.selectionEnd))
    }

    fun moveBoundary(first: Boolean) {
        val moved = if (first) FormulaBoxEditor.firstBox(value.text) else FormulaBoxEditor.lastBox(value.text)
        value = value.copy(selection = TextRange(moved.selectionStart, moved.selectionEnd))
    }

    fun typeKey(character: String) {
        applyEdit(FormulaBoxEditor.typeCharacter(value.text, value.selection.start, value.selection.end, character))
    }

    fun importClipboard(text: String) {
        applyEdit(FormulaBoxEditor.importText(text))
    }

    fun handleImeChange(next: TextFieldValue) {
        val old = value
        val start = old.selection.min.coerceIn(0, old.text.length)
        val end = old.selection.max.coerceIn(start, old.text.length)
        val expectedPrefix = old.text.substring(0, start)
        val expectedSuffix = old.text.substring(end)
        val inserted = if (next.text.startsWith(expectedPrefix) && next.text.endsWith(expectedSuffix)) {
            next.text.substring(expectedPrefix.length, next.text.length - expectedSuffix.length)
        } else null
        if (inserted != null && inserted.codePointCount(0, inserted.length) == 1 && inserted in setOf("/", "^", "_", "(", ")", "*", "×", "÷", ">", "<")) {
            typeKey(inserted)
            return
        }
        if (inserted != null && inserted.codePointCount(0, inserted.length) > 1) {
            if (Regex("\\\\[A-Za-z]+|\\$").containsMatchIn(inserted)) {
                importClipboard(inserted)
            } else {
                var result = ir.exam.app.core.math.FormulaBoxEditResult(old.text, old.selection.start, old.selection.end)
                inserted.codePoints().forEach { codePoint ->
                    result = FormulaBoxEditor.typeCharacter(
                        result.text,
                        result.selectionStart,
                        result.selectionEnd,
                        String(Character.toChars(codePoint))
                    )
                }
                applyEdit(result)
            }
            return
        }
        if (next.text.length < old.text.length && old.selection.collapsed) {
            backspace()
            return
        }
        setValue(next)
    }

    fun undoAction() {
        if (undo.isNotEmpty()) {
            redo.add(value)
            value = undo.removeAt(undo.lastIndex)
        }
    }

    fun redoAction() {
        if (redo.isNotEmpty()) {
            undo.add(value)
            value = redo.removeAt(redo.lastIndex)
        }
    }

    fun currentTex(): String = when (mode) {
        MODE_TYPE -> NativeMathFormatter.quickToTex(natural)
        else -> value.text
    }.trim()

    fun applyCurrent() {
        val tex = currentTex()
        if (tex.isBlank()) { onDismiss(); return }
        if (!NativeMathFormatter.isBalanced(tex)) {
            error = "آکولادهای فرمول متوازن نیستند."
            return
        }
        store.addRecentFormula(tex)
        recentFormulas = store.recentFormulas()
        onInsert(tex)
    }

    fun useEntry(entry: FormulaReferenceEntry) {
        if (entry.label.contains("ماتریس دلخواه") || entry.tex == "m\\times n") {
            matrixPickerOpen = true
            return
        }
        // کتابخانه خانهٔ رنگی را جایگزین می‌کند و اولین خانهٔ قالب تازه را فعال نگه می‌دارد.
        insert(
            entry.tex,
            activateFirstBox = true,
            replaceActiveBox = true
        )
        store.addRecentSymbol(entry)
        recentSymbols = store.recentSymbols()
        notice = "✅ ${entry.label} درج شد"
    }

    fun openMenu(title: String, entries: List<FormulaReferenceEntry>) {
        quickMenuTitle = title
        quickMenuItems = entries
    }

    fun openLibrary(category: String, title: String? = null) {
        categoryId = category
        symbolQuery = ""
        expandedLibraryTitle = title ?: FormulaLibraryNavigator.categoryTitle(library, category)
        expandedLibraryItems = FormulaLibraryNavigator.entries(
            library,
            category,
            favorites,
            recentSymbols,
            uppercase
        )
    }

    val selectedEntries = remember(categoryId, symbolQuery, uppercase, favorites, recentSymbols, library) {
        if (symbolQuery.isBlank()) {
            FormulaLibraryNavigator.entries(library, categoryId, favorites, recentSymbols, uppercase)
        } else {
            FormulaLibraryNavigator.search(library, symbolQuery)
        }
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
                OutlinedButton(
                    onClick = { smartHubOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✨ مرکز هوشمند: درس‌ها، قالب‌ها، بسته‌ها و تبدیل شیمی") }
                HorizontalDivider()
                Box(Modifier.weight(1f)) {
                    when (mode) {
                        MODE_BOX -> LazyColumn(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "هر مقدار داخل یک خانه است؛ با لمس خانه رنگ آن عوض می‌شود. عدد، نماد یا کتابخانه دقیقاً در همان خانه درج می‌شود و دکمه‌های ▾ چند حالت دارند.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item {
                                        OutlinedButton(
                                            onClick = ::undoAction,
                                            enabled = undo.isNotEmpty()
                                        ) { Text("↩ بازگشت") }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = ::redoAction,
                                            enabled = redo.isNotEmpty()
                                        ) { Text("↪ جلو") }
                                    }
                                    item {
                                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(value.text)) }) {
                                            Text("📋 کپی")
                                        }
                                    }
                                    item {
                                        OutlinedButton(onClick = { clipboard.getText()?.text?.let(::importClipboard) }) {
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
                                    onValueChange = ::handleImeChange,
                                    onSelectionChange = { selection -> value = value.copy(selection = selection) },
                                    focusRequester = focusRequester,
                                    zoom = zoom,
                                    onRequestKeyboard = { keyboard?.show() },
                                    onBackspace = ::backspace,
                                    onMoveHorizontal = ::moveActiveBox,
                                    onMoveVertical = ::moveSpatialBox,
                                    onMoveBoundary = ::moveBoundary,
                                    onUndo = ::undoAction,
                                    onRedo = ::redoAction,
                                    onCopy = { clipboard.setText(AnnotatedString(value.text)) },
                                    onPaste = { clipboard.getText()?.text?.let(::importClipboard) },
                                    onApply = ::applyCurrent,
                                    onDismiss = onDismiss
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        CategoryButton("⭐ موارد پرکاربرد", categoryId == "common") {
                                            openLibrary("common")
                                        }
                                    }
                                    items(library.groups, key = { it.key }) { group ->
                                        FormulaCategoryButton(
                                            group,
                                            selected = group.categories.any { it.id == categoryId }
                                        ) { groupDialog = group }
                                    }
                                    item {
                                        CategoryButton("🔍 همهٔ نمادها", categoryId == "__all") {
                                            openLibrary("__all")
                                        }
                                    }
                                    item {
                                        CategoryButton("⚙ یونیکد (۱۲۰۰)", categoryId == "unicode") {
                                            openLibrary("unicode")
                                        }
                                    }
                                    item {
                                        CategoryButton("🕘 نمادهای اخیر", categoryId == "__recent_symbols") {
                                            openLibrary("__recent_symbols")
                                        }
                                    }
                                    item {
                                        CategoryButton("⭐ علاقه‌مندی", categoryId == "__favorites") {
                                            openLibrary("__favorites")
                                        }
                                    }
                                }
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    item { QuickButton("🕘 اخیر") { recentDialogOpen = true } }
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
                                            val converted = NativeMathFormatter.smartQuickToTex(quickConvert)
                                            if (converted.isNotBlank()) {
                                                insert(
                                                    converted,
                                                    activateFirstBox = true,
                                                    replaceActiveBox = true
                                                )
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
                                            uppercase = !uppercase
                                            openLibrary("letters")
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
                                    item { QuickButton("▦ ماتریس") { matrixPickerOpen = true } }
                                    item { QuickButton("( ) ▾") { delimiterPickerOpen = true } }
                                }
                            }
                            item {
                                FixedFormulaKeypad(
                                    onInsert = ::typeKey,
                                    onBackspace = ::backspace,
                                    onMoveHorizontal = ::moveActiveBox,
                                    onMoveVertical = ::moveSpatialBox,
                                    onKeyboard = {
                                        focusRequester.requestFocus()
                                        keyboard?.show()
                                    },
                                    onClear = { replace("") },
                                    onOpenDelimiter = { delimiterPickerOpen = true },
                                    onCloseDelimiter = { moveActiveBox(1) }
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
                                replace(NativeMathFormatter.quickToTex(natural), activateFirstBox = true)
                                mode = MODE_BOX
                            }
                        )

                        else -> GalleryPane(library, recentFormulas.take(8), galleryQuery, { galleryQuery = it }) { entry ->
                            replace(entry.tex, activateFirstBox = true)
                            mode = MODE_BOX
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(onClick = ::applyCurrent, modifier = Modifier.weight(1f)) {
                        Text("✅ درج در سؤال")
                    }
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
                        val count = if (link.id == "letters") 52
                        else library.categoryById[link.id]?.items?.size ?: 0
                        FilterChip(
                            selected = categoryId == link.id,
                            onClick = {
                                groupDialog = null
                                openLibrary(link.id, link.label)
                            },
                            label = { Text("${link.label} • $count") },
                            modifier = Modifier.fillMaxWidth()
                        )
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

    if (smartHubOpen) {
        FormulaSmartHubDialog(
            library = library,
            currentTex = currentTex(),
            favorites = favorites,
            recentFormulas = recentFormulas,
            lastFormula = store.lastFormula(),
            onDismiss = { smartHubOpen = false },
            onInsertAtActive = { entry -> useEntry(entry); mode = MODE_BOX },
            onReplaceFormula = { tex -> replace(tex, activateFirstBox = true); mode = MODE_BOX },
            onOpenEntries = { title, entries ->
                expandedLibraryTitle = title
                expandedLibraryItems = entries
            },
            onDeleteRecent = { tex ->
                store.removeRecentFormula(tex)
                recentFormulas = store.recentFormulas()
            },
            onBackspace = ::backspace,
            onNewLine = { insert("\\\\") }
        )
    }

    if (recentDialogOpen) {
        AlertDialog(
            onDismissRequest = { recentDialogOpen = false },
            title = { Text("🕘 فرمول‌های اخیر") },
            text = {
                if (recentFormulas.isEmpty()) Text("هنوز فرمولی ثبت نشده است.")
                else LazyColumn {
                    items(recentFormulas, key = { it }) { tex ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    replace(tex, activateFirstBox = true)
                                    recentDialogOpen = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                NativeFormulaIcon(tex, Modifier.fillMaxWidth().height(42.dp), 18.sp, contentDescription = "فرمول اخیر")
                            }
                            TextButton(onClick = {
                                store.removeRecentFormula(tex)
                                recentFormulas = store.recentFormulas()
                            }) { Text("حذف") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { recentDialogOpen = false }) { Text("بستن") } },
            dismissButton = {
                if (recentFormulas.isNotEmpty()) TextButton(onClick = {
                    store.clearRecentFormulas()
                    recentFormulas = emptyList()
                }) { Text("پاک‌کردن همه") }
            }
        )
    }

    if (matrixPickerOpen) {
        AlertDialog(
            onDismissRequest = { matrixPickerOpen = false },
            title = { Text("▦ ماتریس دلخواه ۱ تا ۱۰") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MatrixDimensionRow("سطر", matrixRows, { matrixRows = (matrixRows - 1).coerceAtLeast(1) }, { matrixRows = (matrixRows + 1).coerceAtMost(10) })
                    MatrixDimensionRow("ستون", matrixColumns, { matrixColumns = (matrixColumns - 1).coerceAtLeast(1) }, { matrixColumns = (matrixColumns + 1).coerceAtMost(10) })
                    NativeFormulaIcon(FormulaMatrixFactory.create(matrixRows, matrixColumns), Modifier.fillMaxWidth().height(140.dp), 18.sp, contentDescription = "پیش‌نمایش ماتریس")
                }
            },
            confirmButton = {
                Button(onClick = {
                    useEntry(FormulaReferenceEntry("ماتریس ${matrixRows}×${matrixColumns}", FormulaMatrixFactory.create(matrixRows, matrixColumns)))
                    matrixPickerOpen = false
                }) { Text("درج ماتریس") }
            },
            dismissButton = { TextButton(onClick = { matrixPickerOpen = false }) { Text("انصراف") } }
        )
    }

    if (delimiterPickerOpen) {
        AlertDialog(
            onDismissRequest = { delimiterPickerOpen = false },
            title = { Text("انتخاب پرانتز و دلیمتر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("جفت‌های آماده")
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(FormulaSmartReference.delimiters, key = FormulaDelimiterPreset::label) { preset ->
                            TextButton(
                                onClick = {
                                    useEntry(FormulaReferenceEntry(preset.label, delimiterTex(preset, "x")))
                                    delimiterPickerOpen = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                NativeFormulaIcon(delimiterTex(preset, "x"), Modifier.width(100.dp).height(40.dp), 19.sp, contentDescription = preset.label)
                                Text(preset.label)
                            }
                        }
                    }
                    Text("انتخاب جداگانهٔ باز و بسته")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listOf("(", "[", "{", "⌊", "⌈", "|")) { char ->
                            FilterChip(selected = customDelimiterOpen == char, onClick = { customDelimiterOpen = char }, label = { Text(char) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listOf(")", "]", "}", "⌋", "⌉", "|")) { char ->
                            FilterChip(selected = customDelimiterClose == char, onClick = { customDelimiterClose = char }, label = { Text(char) })
                        }
                    }
                    val custom = FormulaDelimiterPreset("دلخواه", customDelimiterOpen, customDelimiterClose)
                    NativeFormulaView(delimiterTex(custom, "x"), Modifier.fillMaxWidth(), 20.sp, contentDescription = "دلیمتر دلخواه")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val custom = FormulaDelimiterPreset("دلخواه", customDelimiterOpen, customDelimiterClose)
                    useEntry(FormulaReferenceEntry("دلیمتر دلخواه", delimiterTex(custom, "x")))
                    delimiterPickerOpen = false
                }) { Text("درج دلخواه") }
            },
            dismissButton = { TextButton(onClick = { delimiterPickerOpen = false }) { Text("بستن") } }
        )
    }

    expandedLibraryTitle?.let { title ->
        FormulaLibraryDialog(
            title = title,
            entries = expandedLibraryItems,
            isFavorite = store::isFavorite,
            onUse = { entry ->
                expandedLibraryTitle = null
                useEntry(entry)
            },
            onToggleFavorite = { entry ->
                store.toggleFavorite(entry)
                favorites = store.favorites()
                if (categoryId == "__favorites") expandedLibraryItems = favorites
            },
            onDismiss = { expandedLibraryTitle = null }
        )
    }
}

@Composable
private fun SvgFormulaEditorSurface(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSelectionChange: (TextRange) -> Unit,
    focusRequester: FocusRequester,
    zoom: Float,
    onRequestKeyboard: () -> Unit,
    onBackspace: () -> Unit,
    onMoveHorizontal: (Int) -> Unit,
    onMoveVertical: (Int) -> Unit,
    onMoveBoundary: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Card(Modifier.fillMaxWidth().height(180.dp)) {
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            NativeFormulaEditorView(
                tex = value.text,
                selectionStart = value.selection.min,
                selectionEnd = value.selection.max,
                onBoxTap = { box ->
                    onSelectionChange(TextRange(box.sourceStart, box.sourceEnd))
                    focusRequester.requestFocus()
                    onRequestKeyboard()
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = (22 * zoom).sp,
                contentDescription = "فرمول جعبه‌ای قابل لمس"
            )
            if (value.text.isBlank()) {
                Text(
                    "خانهٔ خالی را لمس کنید",
                    modifier = Modifier.align(Alignment.TopCenter),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .90f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "خانهٔ فعال رنگی است • لمس برای انتخاب",
                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            // فقط اتصال امن به IME؛ یک لایهٔ نامرئی بزرگ دیگر روی جعبه‌ها قرار نمی‌گیرد.
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(1.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        handleFormulaKeyEvent(
                            event,
                            onBackspace,
                            onMoveHorizontal,
                            onMoveVertical,
                            onMoveBoundary,
                            onUndo,
                            onRedo,
                            onCopy,
                            onPaste,
                            onApply,
                            onDismiss
                        )
                    }
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
    ) { Text("⌨️ نوشتن در خانهٔ فعال") }
    }
}

private fun handleFormulaKeyEvent(
    event: KeyEvent,
    onBackspace: () -> Unit,
    onMoveHorizontal: (Int) -> Unit,
    onMoveVertical: (Int) -> Unit,
    onMoveBoundary: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val modifier = event.isCtrlPressed || event.isMetaPressed
    if (modifier) {
        return when (event.key) {
            Key.Z -> { if (event.isShiftPressed) onRedo() else onUndo(); true }
            Key.Y -> { onRedo(); true }
            Key.C -> { onCopy(); true }
            Key.V -> { onPaste(); true }
            else -> false
        }
    }
    return when (event.key) {
        Key.Backspace -> { onBackspace(); true }
        Key.DirectionLeft -> { onMoveHorizontal(-1); true }
        Key.DirectionRight -> { onMoveHorizontal(1); true }
        Key.DirectionUp -> { onMoveVertical(-1); true }
        Key.DirectionDown -> { onMoveVertical(1); true }
        Key.Tab -> { onMoveHorizontal(if (event.isShiftPressed) -1 else 1); true }
        Key.MoveHome -> { onMoveBoundary(true); true }
        Key.MoveEnd -> { onMoveBoundary(false); true }
        Key.Enter, Key.NumPadEnter -> { onApply(); true }
        Key.Escape -> { onDismiss(); true }
        else -> false
    }
}

@Composable
private fun MatrixDimensionRow(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onMinus, enabled = value > 1) { Text("−") }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onPlus, enabled = value < 10) { Text("+") }
    }
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

@OptIn(ExperimentalFoundationApi::class)
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
    val haptic = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier.fillMaxWidth().height(310.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(entries, key = { it.label + "¦" + it.tex }) { entry ->
            Card(
                Modifier.combinedClickable(
                    onClick = { onUse(entry) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavorite(entry)
                    }
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NativeFormulaIcon(
                        entry.tex,
                        Modifier.fillMaxWidth().height(44.dp),
                        20.sp,
                        contentDescription = entry.label
                    )
                    Text(entry.label.take(35), style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { onFavorite(entry) }) { Text("☆ علاقه‌مندی") }
                }
            }
        }
    }
}

@Composable
private fun FixedFormulaKeypad(
    onInsert: (String) -> Unit,
    onBackspace: () -> Unit,
    onMoveHorizontal: (Int) -> Unit,
    onMoveVertical: (Int) -> Unit,
    onKeyboard: () -> Unit,
    onClear: () -> Unit,
    onOpenDelimiter: () -> Unit,
    onCloseDelimiter: () -> Unit
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
                                "(" -> onOpenDelimiter()
                                ")" -> onCloseDelimiter()
                                "⌫" -> onBackspace()
                                "↑" -> onMoveVertical(-1)
                                "↓" -> onMoveVertical(1)
                                "←" -> onMoveHorizontal(-1)
                                "→" -> onMoveHorizontal(1)
                                "⌨" -> onKeyboard()
                                "C" -> onClear()
                                "÷" -> onInsert("÷")
                                "×" -> onInsert("×")
                                "−" -> onInsert("-")
                                else -> onInsert(key)
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        if (key in setOf("⌫", "⌨", "C")) Text(key)
                        else NativeFormulaIcon(
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
    recentFormulas: List<String>,
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
        val shownRecent = recentFormulas.filter {
            query.isBlank() || it.lowercase().contains(query.lowercase())
        }
        if (shownRecent.isNotEmpty()) {
            item { Text("🕘 اخیراً", style = MaterialTheme.typography.titleMedium) }
            items(shownRecent, key = { it }) { tex ->
                Card(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onPick(FormulaReferenceEntry("فرمول اخیر", tex)) }, modifier = Modifier.fillMaxWidth()) {
                        NativeFormulaView(tex, Modifier.fillMaxWidth(), 18.sp, contentDescription = "فرمول اخیر")
                    }
                }
            }
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
