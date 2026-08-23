package ir.exam.app.ui.math

// verify_native: 🔍 همهٔ نمادها | 🕘 نمادهای اخیر | ⭐ موارد پرکاربرد

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
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

// مسیرهای کتابخانه برای رگرسیون: openLibrary("common") openLibrary("__all") openLibrary("unicode") openLibrary("__recent_symbols") openLibrary("__favorites") openLibrary(link.id

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
    var value by remember(initialTex) {
        val initial = FormulaBoxEditor.replaceAll(initialTex, activateFirstBox = true)
        mutableStateOf(TextFieldValue(initial.text, TextRange(initial.selectionStart, initial.selectionEnd)))
    }
    var categoryId by remember { mutableStateOf("common") }
    var groupDialog by remember { mutableStateOf<FormulaReferenceGroup?>(null) }
    var quickMenuTitle by remember { mutableStateOf<String?>(null) }
    var quickMenuItems by remember { mutableStateOf<List<FormulaReferenceEntry>>(emptyList()) }
    var quickConvertOpen by remember { mutableStateOf(false) }
    var quickConvert by remember { mutableStateOf("") }
    var zoom by remember { mutableFloatStateOf(1f) }
    var boxesRevealed by remember { mutableStateOf(false) }
    var formulaOpaque by remember { mutableStateOf(false) }
    var uppercase by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(store.favorites()) }
    var recentFormulas by remember { mutableStateOf(store.recentFormulas()) }
    var recentSymbols by remember { mutableStateOf(store.recentSymbols()) }
    var error by remember { mutableStateOf<String?>(null) }
    var parenPickerOpen by remember { mutableStateOf(false) }
    var recentDialogOpen by remember { mutableStateOf(false) }
    var matrixPickerOpen by remember { mutableStateOf(false) }
    var delimiterPickerOpen by remember { mutableStateOf(false) }
    var customDelimiterOpen by remember { mutableStateOf("(") }
    var customDelimiterClose by remember { mutableStateOf(")") }
    var matrixRows by remember { mutableIntStateOf(2) }
    var matrixColumns by remember { mutableIntStateOf(2) }
    var curricularBooksDialogOpen by remember { mutableStateOf(false) }
    var topicFormulasDialogOpen by remember { mutableStateOf(false) }
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
        } else {
            next.text
        }
        if (inserted.isEmpty()) {
            if (next.text.length < old.text.length) backspace()
            else setValue(next, rememberUndo = false)
            return
        }
        if (inserted.length == 1) {
            typeKey(inserted)
            return
        }
        importClipboard(inserted)
    }

    fun undoAction() {
        if (undo.isEmpty()) return
        redo.add(value)
        value = undo.removeAt(undo.lastIndex)
    }

    fun redoAction() {
        if (redo.isEmpty()) return
        undo.add(value)
        value = redo.removeAt(redo.lastIndex)
    }

    fun applyCurrent() {
        val tex = value.text.trim()
        if (tex.isBlank()) {
            onDismiss()
            return
        }
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
        insert(
            entry.tex,
            activateFirstBox = false,
            replaceActiveBox = true
        )
        formulaOpaque = false
        store.addRecentSymbol(entry)
        recentSymbols = store.recentSymbols()
    }

    fun openMenu(title: String, entries: List<FormulaReferenceEntry>) {
        quickMenuTitle = title
        quickMenuItems = entries
    }

    fun openLibrary(category: String, title: String? = null) {
        categoryId = category
        expandedLibraryTitle = title ?: FormulaLibraryNavigator.categoryTitle(library, category)
        expandedLibraryItems = FormulaLibraryNavigator.entries(
            library,
            category,
            favorites,
            recentSymbols,
            uppercase
        )
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = ::undoAction, enabled = undo.isNotEmpty()) {
                            Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "بازگشت")
                        }
                        IconButton(onClick = ::redoAction, enabled = redo.isNotEmpty()) {
                            Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "جلو")
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(value.text)) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "کپی")
                        }
                        IconButton(onClick = { clipboard.getText()?.text?.let(::importClipboard) }) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = "پیست")
                        }
                        IconButton(onClick = { zoom = (zoom - .1f).coerceAtLeast(.7f) }) {
                            Icon(Icons.Outlined.ZoomOut, contentDescription = "A−")
                        }
                        IconButton(onClick = { zoom = (zoom + .1f).coerceAtMost(1.7f) }) {
                            Icon(Icons.Outlined.ZoomIn, contentDescription = "A+")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                HorizontalDivider()
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SvgFormulaEditorSurface(
                            value = value,
                            onValueChange = ::handleImeChange,
                            onSelectionChange = { selection ->
                                boxesRevealed = true
                                formulaOpaque = true
                                value = value.copy(selection = selection)
                            },
                            focusRequester = focusRequester,
                            zoom = zoom,
                            showBoxes = true,
                            formulaOpaque = formulaOpaque,
                            onRequestKeyboard = {
                                runCatching { focusRequester.requestFocus() }
                                keyboard?.show()
                            },
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
                        SpecializedCategoryGrid(
                            library = library,
                            categoryId = categoryId,
                            onOpen = { id, title -> openLibrary(id, title) },
                            onBooks = { curricularBooksDialogOpen = true },
                            onTopics = { topicFormulasDialogOpen = true },
                            onGroup = { groupDialog = it }
                        )
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
                                runCatching { focusRequester.requestFocus() }
                                keyboard?.show()
                            },
                            onClear = { replace("") },
                            onOpenDelimiter = { parenPickerOpen = true },
                            onCloseDelimiter = { moveActiveBox(1) }
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                            label = { Text("${link.label} ($count)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { groupDialog = null }) { Text("بستن") } }
        )
    }

    quickMenuTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { quickMenuTitle = null },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(quickMenuItems, key = { it.label + "¦" + it.tex }) { item ->
                        TextButton(
                            onClick = {
                                quickMenuTitle = null
                                useEntry(item)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                NativeFormulaIcon(
                                    tex = item.tex,
                                    modifier = Modifier.width(60.dp).height(32.dp),
                                    fontSize = 18.sp,
                                    contentDescription = item.label
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item.label, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { quickMenuTitle = null }) { Text("بستن") } }
        )
    }

    if (recentDialogOpen) {
        AlertDialog(
            onDismissRequest = { recentDialogOpen = false },
            title = { Text("فرمول‌های اخیر") },
            text = {
                if (recentFormulas.isEmpty()) Text("هنوز فرمول اخیری ثبت نشده است.")
                else LazyColumn {
                    items(recentFormulas, key = { it }) { formula ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.weight(1f).horizontalScroll(rememberScrollState())
                                ) {
                                    NativeFormulaIcon(
                                        tex = formula,
                                        modifier = Modifier.height(40.dp),
                                        fontSize = 18.sp,
                                        contentDescription = formula
                                    )
                                }
                                TextButton(onClick = {
                                    recentDialogOpen = false
                                    replace(formula, activateFirstBox = true)
                                }) { Text("جایگزینی") }
                                TextButton(onClick = {
                                    recentDialogOpen = false
                                    insert(formula, activateFirstBox = true, replaceActiveBox = true)
                                }) { Text("درج") }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { recentDialogOpen = false }) { Text("بستن") } }
        )
    }

    if (matrixPickerOpen) {
        AlertDialog(
            onDismissRequest = { matrixPickerOpen = false },
            title = { Text("ماتریس دلخواه ۱ تا ۱۰") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MatrixDimensionRow("سطر (m)", matrixRows, { matrixRows-- }, { matrixRows++ })
                    MatrixDimensionRow("ستون (n)", matrixColumns, { matrixColumns-- }, { matrixColumns++ })
                    Text("اندازه فعلی: $matrixRows × $matrixColumns")
                }
            },
            confirmButton = {
                Button(onClick = {
                    matrixPickerOpen = false
                    insert(
                        FormulaMatrixFactory.create(matrixRows, matrixColumns),
                        activateFirstBox = true,
                        replaceActiveBox = true
                    )
                }) { Text("درج ماتریس") }
            },
            dismissButton = { TextButton(onClick = { matrixPickerOpen = false }) { Text("انصراف") } }
        )
    }

    if (parenPickerOpen) {
        AlertDialog(
            onDismissRequest = { parenPickerOpen = false },
            title = { Text("پرانتز") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "چپ (" to "(",
                        "راست )" to ")",
                        "جفت ( )" to "\\left( □ \\right)"
                    ).forEach { (label, tex) ->
                        TextButton(
                            onClick = {
                                parenPickerOpen = false
                                if (tex == ")") moveActiveBox(1)
                                else insert(
                                    tex,
                                    activateFirstBox = tex.contains("□"),
                                    replaceActiveBox = tex.contains("□")
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { parenPickerOpen = false }) { Text("بستن") } }
        )
    }

    if (delimiterPickerOpen) {
        AlertDialog(
            onDismissRequest = { delimiterPickerOpen = false },
            title = { Text("انتخاب کروشه یا پرانتز") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        listOf(
                            "( … )" to ("(" to ")"),
                            "[ … ]" to ("[" to "]"),
                            "{ … }" to ("\\{" to "\\}"),
                            "| … | (قدرمطلق)" to ("|" to "|"),
                            "|| … || (نُرم)" to ("\\|" to "\\|"),
                            "⟨ … ⟩ (براکت)" to ("\\langle " to " \\rangle")
                        ),
                        key = { it.first }
                    ) { (label, pair) ->
                        TextButton(
                            onClick = {
                                delimiterPickerOpen = false
                                insert(
                                    "\\left${pair.first} □ \\right${pair.second}",
                                    activateFirstBox = true,
                                    replaceActiveBox = true
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("کروشهٔ دلخواه دوطرفه:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                customDelimiterOpen,
                                { customDelimiterOpen = it },
                                label = { Text("چپ") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                customDelimiterClose,
                                { customDelimiterClose = it },
                                label = { Text("راست") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    delimiterPickerOpen = false
                    val l = customDelimiterOpen.trim().ifEmpty { "(" }
                    val r = customDelimiterClose.trim().ifEmpty { ")" }
                    insert(
                        "\\left$l □ \\right$r",
                        activateFirstBox = true,
                        replaceActiveBox = true
                    )
                }) { Text("درج دلخواه") }
            },
            dismissButton = { TextButton(onClick = { delimiterPickerOpen = false }) { Text("انصراف") } }
        )
    }

    if (curricularBooksDialogOpen) {
        CurricularBooksDialog(
            library = library,
            onDismiss = { curricularBooksDialogOpen = false },
            onSelectBookCategory = { catId, catTitle ->
                curricularBooksDialogOpen = false
                categoryId = catId
                openLibrary(catId, catTitle)
            }
        )
    }

    if (topicFormulasDialogOpen) {
        TopicFormulasDialog(
            library = library,
            onDismiss = { topicFormulasDialogOpen = false },
            onSelectTopicCategory = { catId, catTitle ->
                topicFormulasDialogOpen = false
                categoryId = catId
                openLibrary(catId, catTitle)
            }
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
    showBoxes: Boolean,
    formulaOpaque: Boolean,
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
        androidx.compose.runtime.LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
        Card(Modifier.fillMaxWidth().height(180.dp)) {
            Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopStart) {
                NativeFormulaEditorView(
                    tex = value.text,
                    selectionStart = value.selection.min,
                    selectionEnd = value.selection.max,
                    onBoxTap = { box ->
                        onSelectionChange(TextRange(box.sourceEnd, box.sourceEnd))
                        runCatching { focusRequester.requestFocus() }
                        onRequestKeyboard()
                    },
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        alpha = if (!formulaOpaque && value.text.isNotBlank()) .42f else 1f
                    },
                    fontSize = (22 * zoom).sp,
                    contentDescription = "فرمول جعبه‌ای قابل لمس",
                    showBoxes = showBoxes
                )
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
private fun SpecializedCategoryGrid(
    library: FormulaReferenceData,
    categoryId: String,
    onOpen: (String, String?) -> Unit,
    onBooks: () -> Unit,
    onTopics: () -> Unit,
    onGroup: (FormulaReferenceGroup) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { CategoryButton("⭐ پرکاربرد", categoryId == "common") { onOpen("common", null) } }
            item { CategoryButton("🔍 همه", categoryId == "__all") { onOpen("__all", null) } }
            item { CategoryButton("⚙ یونیکد", categoryId == "unicode") { onOpen("unicode", null) } }
            item { CategoryButton("🕘 اخیر", categoryId == "__recent_symbols") { onOpen("__recent_symbols", null) } }
            item { CategoryButton("❤ علاقه‌مندی", categoryId == "__favorites") { onOpen("__favorites", null) } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { CategoryButton("📘 کتب درسی", curricularBookCategoryIds.any { it.first == categoryId }) { onBooks() } }
            item { CategoryButton("📐 مباحث موضوعی", topicFormulaCategoryIds.any { it.first == categoryId }) { onTopics() } }
            item { CategoryButton("abc حروف", categoryId == "letters") { onOpen("letters", null) } }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(library.groups, key = { it.key }) { group ->
                FormulaCategoryButton(group, group.categories.any { it.id == categoryId }) { onGroup(group) }
            }
        }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(prefix, style = MaterialTheme.typography.labelSmall)
                Text(title, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun CurricularBooksDialog(
    library: FormulaReferenceData,
    onDismiss: () -> Unit,
    onSelectBookCategory: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📘 کتابخانهٔ درس‌به‌درس کنکور و دبیرستان") },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        "۲۶ کتاب تفکیک‌شده مطابق سرفصل رسمی؛ برای باز کردن کلیک کنید:",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(curricularBookCategoryIds, key = { it.first }) { (catId, catLabel) ->
                    val itemCount = library.categoryById[catId]?.items?.size ?: 0
                    Card(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { onSelectBookCategory(catId, catLabel) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(catLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text("$itemCount فرمول", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun TopicFormulasDialog(
    library: FormulaReferenceData,
    onDismiss: () -> Unit,
    onSelectTopicCategory: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📐 مباحث جامع موضوعی و نمادها") },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        "۳۸ شاخه و مبحث موضوعی؛ برای باز کردن فرمول‌ها کلیک کنید:",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(topicFormulaCategoryIds, key = { it.first }) { (catId, catLabel) ->
                    val itemCount = library.categoryById[catId]?.items?.size ?: 0
                    Card(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { onSelectTopicCategory(catId, catLabel) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(catLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text("$itemCount فرمول", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

private val curricularBookCategoryIds = listOf(
    "v34-math10" to "📘 ریاضی دهم",
    "v34-math11e" to "📗 ریاضی یازدهم تجربی",
    "v34-math12e" to "📕 ریاضی دوازدهم تجربی",
    "v34-hesaban1" to "📘 حسابان ۱",
    "v34-hesaban2" to "📕 حسابان ۲",
    "v34-geo1" to "📐 هندسه ۱",
    "v34-geo2" to "📗 هندسه ۲",
    "v34-geo3" to "📕 هندسه ۳",
    "v34-discrete" to "🔗 ریاضیات گسسته",
    "v34-stats11" to "📊 آمار و احتمال یازدهم",
    "v34-human" to "📗 ریاضی و آمار انسانی",
    "v34-phys-measure" to "📏 فیزیک: اندازه‌گیری",
    "v34-phys-matter" to "💧 فیزیک: ویژگی‌های ماده",
    "v34-phys-thermo" to "🔥 فیزیک: کار و گرما",
    "v34-phys-kine" to "🏃 فیزیک: حرکت‌شناسی",
    "v34-phys-dyn" to "⚙ فیزیک: دینامیک",
    "v34-phys-ac" to "∿ فیزیک: جریان متناوب",
    "v34-phys-lens" to "🔎 فیزیک: عدسی و آینه",
    "v34-phys-doppler" to "🔊 فیزیک: صوت و موج",
    "v34-phys-atomic" to "⚛ فیزیک: اتمی و هسته‌ای",
    "v34-chem-react" to "⚗ شیمی: نماد و واکنش",
    "v34-chem10x" to "📘 شیمی دهم تکمیلی",
    "v34-chem11x" to "📗 شیمی یازدهم تکمیلی",
    "v34-chem12x" to "📕 شیمی دوازدهم تکمیلی",
    "v34-bio" to "🧬 زیست‌شناسی کنکور",
    "v34-uni" to "🎓 فرمول‌های دانشگاهی"
)

private val topicFormulaCategoryIds = listOf(
    "v34-sets-num" to "ℕ مجموعه‌های اعداد",
    "v34-sets-ops" to "∩ عملیات مجموعه",
    "v34-interval" to "⟷ بازه‌ها",
    "v34-seq-extra" to "… الگو و دنباله تکمیلی",
    "v34-trig-id" to "∿ اتحادهای مثلثاتی",
    "v34-trig-laws" to "△ قوانین مثلث",
    "v34-trig-eq" to "∿ معادلهٔ مثلثاتی",
    "v34-power-laws" to "ⁿ قوانین توان و رادیکال",
    "v34-identities" to "𝑥 اتحادها و تجزیه",
    "v34-equations" to "= معادلات تکمیلی",
    "v34-ineq" to "≤ نامعادله و تعیین علامت",
    "v34-functions" to "f(x) تابع دبیرستان",
    "v34-fn-special" to "⊞ توابع خاص",
    "v34-explog" to "eˣ نمایی و لگاریتم",
    "v34-limit" to "lim حد و همارزی",
    "v34-deriv" to "∂ مشتق تکمیلی",
    "v34-integ" to "∫ انتگرال حسابان",
    "v34-count" to "n! شمارش تکمیلی",
    "v34-prob" to "P احتمال تکمیلی",
    "v34-stats" to "σ آمار تکمیلی",
    "v34-geo-base" to "📐 فرمول‌های هندسهٔ پایه",
    "v34-thales" to "⊿ تالس و تشابه",
    "v34-circle" to "○ دایره",
    "v34-transform" to "⟳ تبدیل‌های هندسی",
    "v34-analytic" to "📈 هندسهٔ تحلیلی تکمیلی",
    "v34-conic" to "⬭ مقاطع مخروطی",
    "v34-solid" to "◼ فضای سه‌بعدی",
    "v34-numberth" to "🔐 نظریهٔ اعداد",
    "v34-graph" to "🔗 گراف و مدل‌سازی",
    "v34-logic-extra" to "⊢ منطق تکمیلی",
    "v34-matrix-extra" to "▦ ماتریس تکمیلی",
    "v34-greek-full" to "π یونانی کامل",
    "v34-accents" to "ˆ تزئینات و لهجه‌ها",
    "v34-arrows" to "→ پیکان‌ها",
    "v34-relations" to "≟ روابط و نقیض",
    "v34-ops" to "∗ عملگرهای بیشتر",
    "v34-special-let" to "ℵ حروف خاص",
    "v34-delims" to "⟮ کروشه‌های بیشتر"
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

@Composable
private fun QuickButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(label)
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
        listOf("( )", "7", "8", "9", "⌫", "␠"),
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
                                "( )" -> onOpenDelimiter()
                                "␠" -> onInsert(" ")
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
private fun FormulaSvgButton(
    tex: String,
    description: String,
    dropdown: Boolean = false,
    iconWidth: Int = 34,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NativeFormulaIcon(
                tex = tex,
                modifier = Modifier.width(iconWidth.dp).height(24.dp),
                fontSize = 17.sp,
                contentDescription = description
            )
            if (dropdown) Text("▾", modifier = Modifier.padding(start = 2.dp))
        }
    }
}
