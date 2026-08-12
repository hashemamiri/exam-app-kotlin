package ir.exam.app.ui.math

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ir.exam.app.core.math.NativeMathFormatter

private data class FormulaTemplate(val label:String,val tex:String,val group:String)
private val templates=listOf(
 FormulaTemplate("کسر","\\frac{a}{b}","پایه"),FormulaTemplate("رادیکال","\\sqrt{x}","پایه"),FormulaTemplate("توان","x^{2}","پایه"),FormulaTemplate("زیرنویس","a_{1}","پایه"),
 FormulaTemplate("انتگرال","\\int_{a}^{b} f(x) dx","آنالیز"),FormulaTemplate("مجموع","\\sum_{i=1}^{n} x_i","آنالیز"),FormulaTemplate("حد","lim_{x\\rightarrow a} f(x)","آنالیز"),
 FormulaTemplate("ماتریس","\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}","جبر"),FormulaTemplate("معادله درجه۲","x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}","جبر"),
 FormulaTemplate("مثلثات","\\sin^2(x)+\\cos^2(x)=1","مثلثات"),FormulaTemplate("بردار","\\vec{F}=m\\vec{a}","فیزیک"),FormulaTemplate("مجموعه","A\\subseteq B\\cap C","مجموعه")
)
private val symbols=listOf("+","−","×","÷","=","≠","≈","≤","≥","±","∞","π","θ","α","β","γ","Δ","∑","∫","√","→","∈","∉","⊂","⊆","∪","∩","∀","∃")

@Composable
fun FormulaEditorDialog(onDismiss:()->Unit,onInsert:(String)->Unit){
 val context=LocalContext.current;val history=remember{FormulaHistoryStore(context)};val clipboard=LocalClipboardManager.current
 var mode by remember{mutableStateOf("جعبه‌ای")};var value by remember{mutableStateOf(TextFieldValue(""))};var error by remember{mutableStateOf<String?>(null)}
 val undo=remember{mutableStateListOf<String>()};val redo=remember{mutableStateListOf<String>()};var group by remember{mutableStateOf("همه")};var favorites by remember{mutableStateOf(history.favorites())};var recent by remember{mutableStateOf(history.recent())}
 fun replace(next:String){if(value.text!=next){undo.add(value.text);if(undo.size>50)undo.removeAt(0);redo.clear()};value=TextFieldValue(next,TextRange(next.length));error=null}
 fun insert(text:String){val a=value.selection.start.coerceIn(0,value.text.length);val b=value.selection.end.coerceIn(a,value.text.length);val n=value.text.substring(0,a)+text+value.text.substring(b);undo.add(value.text);redo.clear();value=TextFieldValue(n,TextRange(a+text.length));error=null}
 val tex=runCatching{NativeMathFormatter.quickToTex(value.text)}.getOrDefault(value.text)
 AlertDialog(onDismissRequest=onDismiss,title={Text("ویرایشگر کامل فرمول Native")},text={LazyColumn(Modifier.fillMaxWidth().heightIn(max=620.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("جعبه‌ای","تایپ سریع","آماده").forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m)})}}}
  item{OutlinedTextField(value,{value=it.copy(text=it.text.take(4000));error=null},label={Text("TeX یا تایپ سریع")},minLines=3,modifier=Modifier.fillMaxWidth())}
  item{NativeFormulaView(tex,Modifier.fillMaxWidth())}
  item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
   OutlinedButton(onClick={if(undo.isNotEmpty()){redo.add(value.text);replace(undo.removeAt(undo.lastIndex))}},enabled=undo.isNotEmpty()){Text("↩")}
   OutlinedButton(onClick={if(redo.isNotEmpty()){undo.add(value.text);value=TextFieldValue(redo.removeAt(redo.lastIndex))}},enabled=redo.isNotEmpty()){Text("↪")}
   OutlinedButton(onClick={clipboard.setText(AnnotatedString(value.text))}){Text("کپی")}
   OutlinedButton(onClick={clipboard.getText()?.text?.let(::insert)}){Text("پیست")}
   OutlinedButton(onClick={if(value.text in favorites){history.removeFavorite(value.text)}else history.addFavorite(value.text);favorites=history.favorites()}){Text(if(value.text in favorites)"★" else "☆")}
  }}
  if(mode=="جعبه‌ای"){
   item{symbols.chunked(7).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){row.forEach{s->FilterChip(selected=false,onClick={insert(symbolTex(s))},label={Text(s)})}}}}
   item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("\\frac{}{}","\\sqrt{}","^{}","_{}","\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}").forEach{t->FilterChip(false,{insert(t)},{Text(t.take(10))})}}}
  }
  if(mode=="آماده"){
   item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("همه","پایه","جبر","آنالیز","مثلثات","فیزیک","مجموعه").forEach{g->FilterChip(group==g,{group=g},{Text(g)})}}}
   items(templates.filter{group=="همه"||it.group==group}.size){idx->val t=templates.filter{group=="همه"||it.group==group}[idx];Button(onClick={replace(t.tex)},modifier=Modifier.fillMaxWidth()){Text(t.label)}}
  }
  if(favorites.isNotEmpty())item{Text("علاقه‌مندی‌ها");favorites.take(8).forEach{f->TextButton(onClick={replace(f)}){Text(f.take(60))}}}
  if(recent.isNotEmpty())item{Text("فرمول‌های اخیر");recent.take(8).forEach{f->TextButton(onClick={replace(f)}){Text(f.take(60))}}}
  error?.let{item{Text(it)}}
 }},confirmButton={Button(enabled=value.text.isNotBlank(),onClick={if(!NativeMathFormatter.isBalanced(tex))error="آکولادها متوازن نیستند." else{history.addRecent(tex);onInsert(tex)}}){Text("درج")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})
}

private fun symbolTex(s:String)=mapOf("×" to "\\times ","÷" to "\\div ","≠" to "\\neq ","≈" to "\\approx ","≤" to "\\leq ","≥" to "\\geq ","±" to "\\pm ","∞" to "\\infty ","π" to "\\pi ","θ" to "\\theta ","α" to "\\alpha ","β" to "\\beta ","γ" to "\\gamma ","Δ" to "\\Delta ","∑" to "\\sum ","∫" to "\\int ","√" to "\\sqrt{}","→" to "\\rightarrow ","∈" to "\\in ","∉" to "\\notin ","⊂" to "\\subset ","⊆" to "\\subseteq ","∪" to "\\cup ","∩" to "\\cap ","∀" to "\\forall ","∃" to "\\exists ")[s]?:s
private class FormulaHistoryStore(context:Context){private val p=context.getSharedPreferences("formula_history",Context.MODE_PRIVATE)
 fun recent()=p.getStringSet("recent",emptySet()).orEmpty().toList();fun favorites()=p.getStringSet("fav",emptySet()).orEmpty().toList()
 fun addRecent(v:String){p.edit().putStringSet("recent",(listOf(v)+recent()).distinct().take(20).toSet()).apply()}
 fun addFavorite(v:String){if(v.isNotBlank())p.edit().putStringSet("fav",(favorites()+v).take(30).toSet()).apply()};fun removeFavorite(v:String){p.edit().putStringSet("fav",favorites().filterNot{it==v}.toSet()).apply()}}
