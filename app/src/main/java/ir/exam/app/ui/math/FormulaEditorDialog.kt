package ir.exam.app.ui.math

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

private const val MODE_BOX="box"
private const val MODE_TYPE="type"
private const val MODE_GALLERY="gallery"

@Composable
fun FormulaEditorDialog(onDismiss:()->Unit,onInsert:(String)->Unit){
    val context=LocalContext.current
    val library=remember{FormulaReferenceLibrary.load(context)}
    val store=remember{FormulaReferenceStore(context)}
    val clipboard=LocalClipboardManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val focusRequester=remember{FocusRequester()}
    var mode by remember{mutableStateOf(MODE_BOX)}
    var value by remember{mutableStateOf(TextFieldValue(""))}
    var natural by remember{mutableStateOf("")}
    var categoryId by remember{mutableStateOf("common")}
    var groupDialog by remember{mutableStateOf<FormulaReferenceGroup?>(null)}
    var quickMenuTitle by remember{mutableStateOf<String?>(null)}
    var quickMenuItems by remember{mutableStateOf<List<FormulaReferenceEntry>>(emptyList())}
    var symbolQuery by remember{mutableStateOf("")}
    var galleryQuery by remember{mutableStateOf("")}
    var quickConvertOpen by remember{mutableStateOf(false)}
    var quickConvert by remember{mutableStateOf("")}
    var showCode by remember{mutableStateOf(false)}
    var zoom by remember{mutableFloatStateOf(1f)}
    var uppercase by remember{mutableStateOf(false)}
    var favorites by remember{mutableStateOf(store.favorites())}
    var recentFormulas by remember{mutableStateOf(store.recentFormulas())}
    var recentSymbols by remember{mutableStateOf(store.recentSymbols())}
    var error by remember{mutableStateOf<String?>(null)}
    val undo=remember{mutableStateListOf<TextFieldValue>()}
    val redo=remember{mutableStateListOf<TextFieldValue>()}

    fun setValue(next:TextFieldValue,rememberUndo:Boolean=true){
        if(rememberUndo&&next.text!=value.text){undo.add(value);if(undo.size>80)undo.removeAt(0);redo.clear()}
        value=next.copy(text=next.text.take(8000));error=null
    }
    fun replace(text:String){setValue(TextFieldValue(text.take(8000),TextRange(text.length.coerceAtMost(8000))))}
    fun insert(text:String){
        val a=value.selection.min.coerceIn(0,value.text.length);val b=value.selection.max.coerceIn(a,value.text.length)
        val next=value.text.substring(0,a)+text+value.text.substring(b)
        setValue(TextFieldValue(next.take(8000),TextRange((a+text.length).coerceAtMost(8000))))
    }
    fun backspace(){
        val a=value.selection.min;val b=value.selection.max
        if(a!=b){setValue(TextFieldValue(value.text.removeRange(a,b),TextRange(a)))}
        else if(a>0)setValue(TextFieldValue(value.text.removeRange(a-1,a),TextRange(a-1)))
    }
    fun moveCursor(delta:Int){val p=(value.selection.end+delta).coerceIn(0,value.text.length);value=value.copy(selection=TextRange(p))}
    fun useEntry(entry:FormulaReferenceEntry){insert(entry.tex);store.addRecentSymbol(entry);recentSymbols=store.recentSymbols()}
    fun openMenu(title:String,items:List<FormulaReferenceEntry>){quickMenuTitle=title;quickMenuItems=items}
    fun currentTex():String=when(mode){MODE_TYPE->NativeMathFormatter.quickToTex(natural);else->value.text}.trim()

    val selectedEntries=remember(categoryId,symbolQuery,uppercase,favorites,recentSymbols,library){
        val base=when(categoryId){
            "__all"->library.allItems
            "__favorites"->favorites
            "__recent_symbols"->recentSymbols
            "letters"->(if(uppercase)'A'..'Z' else 'a'..'z').map{FormulaReferenceEntry("حرف $it",it.toString())}
            else->library.categoryById[categoryId]?.items.orEmpty()
        }
        val filtered=if(symbolQuery.isBlank())base else {
            val q=symbolQuery.trim().lowercase()
            (library.allItems+library.categoryById["unicode"]?.items.orEmpty()).filter{it.label.lowercase().contains(q)||it.tex.lowercase().contains(q)}
        }
        filtered.distinctBy{it.label+"¦"+it.tex}
    }

    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
            Column(Modifier.fillMaxSize().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("🧮 نوشتن فرمول",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    TextButton(onClick=onDismiss){Text("✕")}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    listOf(MODE_BOX to "🖱️ جعبه‌ای",MODE_TYPE to "⌨️ تایپ سریع",MODE_GALLERY to "📚 آماده").forEach{(m,label)->
                        FilterChip(selected=mode==m,onClick={mode=m},label={Text(label)},modifier=Modifier.weight(1f))
                    }
                }
                HorizontalDivider()
                Box(Modifier.weight(1f)){
                    when(mode){
                        MODE_BOX->LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            item{Text("روی کادر فرمول بنویسید. دکمه‌های ▾ چند حالت دارند؛ Undo/Redo، کپی/پیست و بزرگ‌نمایی در نوار زیر قرار دارد.",style=MaterialTheme.typography.bodySmall)}
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                item{OutlinedButton(onClick={if(undo.isNotEmpty()){redo.add(value);value=undo.removeAt(undo.lastIndex)}},enabled=undo.isNotEmpty()){Text("↩ بازگشت")}}
                                item{OutlinedButton(onClick={if(redo.isNotEmpty()){undo.add(value);value=redo.removeAt(redo.lastIndex)}},enabled=redo.isNotEmpty()){Text("↪ جلو")}}
                                item{OutlinedButton(onClick={clipboard.setText(AnnotatedString(value.text))}){Text("📋 کپی")}}
                                item{OutlinedButton(onClick={clipboard.getText()?.text?.let(::insert)}){Text("📥 پیست")}}
                                item{OutlinedButton(onClick={zoom=(zoom-.1f).coerceAtLeast(.7f)}){Text("A−")}}
                                item{OutlinedButton(onClick={zoom=(zoom+.1f).coerceAtMost(1.7f)}){Text("A+")}}
                            }}
                            item{Card(Modifier.fillMaxWidth().height(150.dp)){Box(Modifier.fillMaxSize().padding(8.dp),contentAlignment=Alignment.Center){
                                if(value.text.isBlank())Text("پیش‌نمایش فرمول") else NativeFormulaView(value.text,fontSize=(21*zoom).sp)
                            }}}
                            item{OutlinedTextField(value,{setValue(it)},label={Text("کادر ساختاری فرمول")},minLines=2,modifier=Modifier.fillMaxWidth().focusRequester(focusRequester))}
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                item{CategoryButton("⭐ موارد پرکاربرد",categoryId=="common"){categoryId="common";symbolQuery=""}}
                                items(library.groups,key={it.key}){g->CategoryButton(g.label,false){groupDialog=g}}
                                item{CategoryButton("🔍 همهٔ نمادها",categoryId=="__all"){categoryId="__all";symbolQuery=""}}
                                item{CategoryButton("⚙ یونیکد (۱۲۰۰)",categoryId=="unicode"){categoryId="unicode";symbolQuery=""}}
                                item{CategoryButton("⭐ علاقه‌مندی",categoryId=="__favorites"){categoryId="__favorites";symbolQuery=""}}
                            }}
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                item{QuickButton("🕘 اخیر"){openMenu("فرمول‌های اخیر",recentFormulas.map{FormulaReferenceEntry(it.take(70),it)})}}
                                item{QuickButton("✨ تبدیل"){quickConvertOpen=!quickConvertOpen}}
                                item{QuickButton("log"){openMenu("لگاریتم",logItems)}}
                                item{QuickButton("∫"){openMenu("انتگرال",integralItems)}}
                                item{QuickButton("٫ ٪"){openMenu("اعشار و درصد",percentItems)}}
                                item{QuickButton("sin"){openMenu("مثلثات",trigItems)}}
                            }}
                            if(quickConvertOpen)item{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                OutlinedTextField(quickConvert,{quickConvert=it},label={Text("مثلاً x^2 + a/b <= sqrt(16)")},singleLine=true,modifier=Modifier.weight(1f))
                                Button(onClick={val t=NativeMathFormatter.quickToTex(quickConvert);if(t.isNotBlank()){insert(t);quickConvert="";quickConvertOpen=false}}){Text("تبدیل")}
                                TextButton(onClick={quickConvertOpen=false}){Text("بستن")}
                            }}
                            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                item{Button(onClick={val tex=value.text.trim();if(tex.isNotBlank()){store.addRecentFormula(tex);onInsert(tex)}}){Text("درج")}}
                                item{QuickButton("↵"){insert("\\\\")}}
                                item{QuickButton("abc"){categoryId="letters";symbolQuery="";uppercase=!uppercase}}
                                item{QuickButton("کسر ▾"){openMenu("کسر",fractionItems)}}
                                item{QuickButton("xⁿ ▾"){openMenu("توان",powerItems)}}
                                item{QuickButton("√ ▾"){openMenu("رادیکال",rootItems)}}
                            }}
                            item{FixedFormulaKeypad(
                                onInsert=::insert,onBackspace=::backspace,onMove=::moveCursor,
                                onKeyboard={focusRequester.requestFocus();keyboard?.show()},onClear={replace("")},
                                onParenthesis={insert(it)}
                            )}
                            item{OutlinedTextField(symbolQuery,{symbolQuery=it},label={Text("🔍 جست‌وجوی نماد یا نام فارسی…")},singleLine=true,modifier=Modifier.fillMaxWidth())}
                            item{Row(verticalAlignment=Alignment.CenterVertically){
                                Text((if(symbolQuery.isBlank())library.categoryById[categoryId]?.label else "نتایج جست‌وجو")?:when(categoryId){"__all"->"همهٔ نمادها";"__favorites"->"علاقه‌مندی";"__recent_symbols"->"اخیر";else->"نمادها"},modifier=Modifier.weight(1f))
                                if(categoryId=="letters")TextButton(onClick={uppercase=!uppercase}){Text(if(uppercase)"A→a" else "a→A")}
                            }}
                            item{SymbolGrid(selectedEntries,onUse=::useEntry,onFavorite={entry->store.toggleFavorite(entry);favorites=store.favorites()})}
                            item{TextButton(onClick={showCode=!showCode}){Text(if(showCode)"بستن کد فرمول" else "کد فرمول (کاربران حرفه‌ای)")}}
                            if(showCode)item{Text(value.text.ifBlank{"—"},style=MaterialTheme.typography.bodySmall)}
                        }
                        MODE_TYPE->QuickTypePane(natural,{natural=it},onToBox={replace(NativeMathFormatter.quickToTex(natural));mode=MODE_BOX})
                        else->GalleryPane(library,galleryQuery,{galleryQuery=it}){entry->replace(entry.tex);mode=MODE_BOX}
                    }
                }
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(onClick={val tex=currentTex();if(tex.isBlank())onDismiss() else if(!NativeMathFormatter.isBalanced(tex))error="آکولادهای فرمول متوازن نیستند." else{store.addRecentFormula(tex);onInsert(tex)}},modifier=Modifier.weight(1f)){Text("✅ درج در سؤال")}
                    OutlinedButton(onClick={if(mode==MODE_TYPE)natural="" else replace("")}){Text("🧹 پاک")}
                    TextButton(onClick=onDismiss){Text("انصراف")}
                }
            }
        }
    }

    groupDialog?.let{group->AlertDialog(
        onDismissRequest={groupDialog=null},title={Text(group.label)},
        text={LazyColumn{items(group.categories,key={it.id}){link->TextButton(onClick={categoryId=link.id;symbolQuery="";groupDialog=null},modifier=Modifier.fillMaxWidth()){Text(link.label)}}}},
        confirmButton={TextButton(onClick={groupDialog=null}){Text("بستن")}}
    )}
    quickMenuTitle?.let{title->AlertDialog(
        onDismissRequest={quickMenuTitle=null},title={Text(title)},
        text={LazyColumn{items(quickMenuItems){entry->TextButton(onClick={useEntry(entry);quickMenuTitle=null},modifier=Modifier.fillMaxWidth()){Column{Text(entry.label);Text(entry.tex,style=MaterialTheme.typography.bodySmall)}}}}},
        confirmButton={TextButton(onClick={quickMenuTitle=null}){Text("بستن")}}
    )}
}

@Composable private fun CategoryButton(text:String,selected:Boolean,onClick:()->Unit){FilterChip(selected=selected,onClick=onClick,label={Text(text)})}
@Composable private fun QuickButton(text:String,onClick:()->Unit){OutlinedButton(onClick=onClick){Text(text)}}

@Composable
private fun SymbolGrid(entries:List<FormulaReferenceEntry>,onUse:(FormulaReferenceEntry)->Unit,onFavorite:(FormulaReferenceEntry)->Unit){
    if(entries.isEmpty()){Text("موردی پیدا نشد.");return}
    LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.fillMaxWidth().height(310.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        items(entries,key={it.label+"¦"+it.tex}){entry->Card{Column(Modifier.padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally){
            TextButton(onClick={onUse(entry)},modifier=Modifier.fillMaxWidth()){Column(horizontalAlignment=Alignment.CenterHorizontally){NativeFormulaView(entry.tex,Modifier.fillMaxWidth(),16.sp);Text(entry.label.take(35),style=MaterialTheme.typography.labelSmall)}}
            TextButton(onClick={onFavorite(entry)}){Text("☆")}
        }}}
    }
}

@Composable
private fun FixedFormulaKeypad(
    onInsert:(String)->Unit,onBackspace:()->Unit,onMove:(Int)->Unit,onKeyboard:()->Unit,onClear:()->Unit,onParenthesis:(String)->Unit
){
    val rows=listOf(
        listOf("(" , ")", "7", "8", "9", "⌫"),
        listOf("↑", "↓", "4", "5", "6", "÷"),
        listOf("←", "→", "1", "2", "3", "×"),
        listOf("⌨", "C", "0", "=", "+", "−")
    )
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){rows.forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){row.forEach{key->
        OutlinedButton(onClick={when(key){"(" , ")"->onParenthesis(key);"⌫"->onBackspace();"↑","←"->onMove(-1);"↓","→"->onMove(1);"⌨"->onKeyboard();"C"->onClear();"÷"->onInsert("\\div ");"×"->onInsert("\\times ");"−"->onInsert("-");else->onInsert(key)}},modifier=Modifier.weight(1f)){Text(key)}
    }}}}
}

@Composable
private fun QuickTypePane(value:String,onChange:(String)->Unit,onToBox:()->Unit){
    val tex=NativeMathFormatter.quickToTex(value)
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("طبیعی بنویسید؛ خودش به TeX تبدیل می‌شود. نیازی به تغییر زبان کیبورد نیست.")}
        item{Card(Modifier.fillMaxWidth().height(150.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){if(tex.isBlank())Text("اینجا نتیجه را می‌بینید") else NativeFormulaView(tex)}}}
        item{OutlinedTextField(value,onChange,label={Text("مثلاً: 7/8 * 6/8")},modifier=Modifier.fillMaxWidth())}
        item{Column(verticalArrangement=Arrangement.spacedBy(5.dp)){quickTips.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){row.forEach{tip->Card(Modifier.weight(1f)){Row(Modifier.padding(8.dp)){Text(tip.first,modifier=Modifier.weight(1f));Text(tip.second)}}}}}}}
        item{Button(onClick=onToBox,modifier=Modifier.fillMaxWidth()){Text("✏️ ویرایش در حالت جعبه‌ای")}}
        item{Text("کد فرمول: ${tex.ifBlank{"—"}}",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable
private fun GalleryPane(library:FormulaReferenceData,query:String,onQuery:(String)->Unit,onPick:(FormulaReferenceEntry)->Unit){
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("یکی را انتخاب کنید، سپس عددها و بخش‌هایش را در حالت جعبه‌ای تغییر دهید.")}
        item{OutlinedTextField(query,onQuery,label={Text("🔍 جست‌وجو در فرمول‌ها…")},modifier=Modifier.fillMaxWidth())}
        library.gallery.forEach{group->
            val shown=group.items.filter{query.isBlank()||(it.label+" "+it.tex).lowercase().contains(query.lowercase())}
            if(shown.isNotEmpty()){item{Text(group.label,style=MaterialTheme.typography.titleMedium)};items(shown){entry->Card(Modifier.fillMaxWidth()){TextButton(onClick={onPick(entry)},modifier=Modifier.fillMaxWidth()){Column{Text(entry.label);NativeFormulaView(entry.tex,Modifier.fillMaxWidth(),17.sp)}}}}}
        }
    }
}

private class FormulaReferenceStore(context:Context){
    private val p=context.getSharedPreferences("formula_reference_history",Context.MODE_PRIVATE)
    private val json=Json
    fun favorites()=readEntries("favorites")
    fun recentSymbols()=readEntries("recent_symbols")
    fun recentFormulas()=readStrings("recent_formulas")
    fun toggleFavorite(entry:FormulaReferenceEntry){val list=favorites().toMutableList();val i=list.indexOfFirst{it.label==entry.label&&it.tex==entry.tex};if(i>=0)list.removeAt(i)else list.add(0,entry);writeEntries("favorites",list.take(60))}
    fun addRecentSymbol(entry:FormulaReferenceEntry){writeEntries("recent_symbols",(listOf(entry)+recentSymbols().filterNot{it.tex==entry.tex&&it.label==entry.label}).take(24))}
    fun addRecentFormula(tex:String){writeStrings("recent_formulas",(listOf(tex)+recentFormulas().filterNot{it==tex}).take(20))}
    private fun readEntries(key:String)=runCatching{val arr=json.decodeFromString<List<List<String>>>(p.getString(key,"[]")?:"[]");arr.mapNotNull{if(it.size>=2)FormulaReferenceEntry(it[0],it[1])else null}}.getOrDefault(emptyList())
    private fun writeEntries(key:String,list:List<FormulaReferenceEntry>){p.edit().putString(key,json.encodeToString(list.map{listOf(it.label,it.tex)})).apply()}
    private fun readStrings(key:String)=runCatching{json.decodeFromString<List<String>>(p.getString(key,"[]")?:"[]")}.getOrDefault(emptyList())
    private fun writeStrings(key:String,list:List<String>){p.edit().putString(key,json.encodeToString(list)).apply()}
}

private val quickTips=listOf("7/8" to "کسر","x^2" to "توان","x^2^3" to "توانِ توان","sqrt2" to "رادیکال","رادیکال ۵" to "رادیکال","(a+b)/2" to "کسر مرکب","pi" to "π",">=" to "≥","!=" to "≠","*" to "×")
private val fractionItems=listOf(FormulaReferenceEntry("کسر ساده","\\frac{a}{b}"),FormulaReferenceEntry("کسر مخلوط","3\\frac{1}{2}"),FormulaReferenceEntry("خط کسری","a/b"))
private val powerItems=listOf(FormulaReferenceEntry("توان n","x^{n}"),FormulaReferenceEntry("مربع","x^{2}"),FormulaReferenceEntry("مکعب","x^{3}"),FormulaReferenceEntry("زیرنویس","x_{1}"))
private val rootItems=listOf(FormulaReferenceEntry("جذر","\\sqrt{x}"),FormulaReferenceEntry("ریشه سوم","\\sqrt[3]{x}"),FormulaReferenceEntry("ریشه چهارم","\\sqrt[4]{x}"),FormulaReferenceEntry("فرجه دلخواه","\\sqrt[n]{x}"))
private val logItems=listOf(FormulaReferenceEntry("log","\\log"),FormulaReferenceEntry("ln","\\ln"),FormulaReferenceEntry("log با مبنا","\\log_{a}(x)"),FormulaReferenceEntry("e^x","e^{x}"),FormulaReferenceEntry("10^x","10^{x}"))
private val integralItems=listOf(FormulaReferenceEntry("انتگرال ساده","\\int f(x) dx"),FormulaReferenceEntry("انتگرال معین","\\int_{a}^{b} f(x) dx"),FormulaReferenceEntry("انتگرال دوگانه","\\iint"),FormulaReferenceEntry("انتگرال سه‌گانه","\\iiint"),FormulaReferenceEntry("انتگرال بسته","\\oint"))
private val percentItems=listOf(FormulaReferenceEntry("ممیز","."),FormulaReferenceEntry("درصد","\\%"),FormulaReferenceEntry("در هزار","‰"),FormulaReferenceEntry("درصد فرمولی","\\frac{a}{b} \\times 100"))
private val trigItems=listOf("sin","cos","tan","cot","sec","csc","arcsin","arccos","arctan").map{FormulaReferenceEntry(it,"\\$it")}
