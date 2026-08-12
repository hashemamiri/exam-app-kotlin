package ir.exam.app.core.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class XlsxSheet(val name:String,val rows:List<List<Any?>>)

/** تولید مستقیم OOXML واقعی (.xlsx) بدون CSV یا کتابخانه خارجی. */
object XlsxWorkbook {
 fun build(sheets:List<XlsxSheet>):ByteArray{
  require(sheets.isNotEmpty()){ "حداقل یک برگه لازم است." };require(sheets.size<=50)
  val out=ByteArrayOutputStream();ZipOutputStream(out).use { zip ->
   fun put(path:String,text:String){zip.putNextEntry(ZipEntry(path));zip.write(text.toByteArray());zip.closeEntry()}
   put("[Content_Types].xml",contentTypes(sheets.size));put("_rels/.rels",relsRoot());put("xl/workbook.xml",workbook(sheets));put("xl/_rels/workbook.xml.rels",workbookRels(sheets.size));put("xl/styles.xml",styles())
   sheets.forEachIndexed { i,s->put("xl/worksheets/sheet${i+1}.xml",sheet(s.rows)) }
  };return out.toByteArray()
 }
 private fun sheet(rows:List<List<Any?>>):String=buildString{append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"/></sheetViews><sheetData>");rows.take(100000).forEachIndexed{ri,row->append("<row r=\"${ri+1}\">");row.take(500).forEachIndexed{ci,v->val ref="${col(ci)}${ri+1}";when(v){is Number->append("<c r=\"$ref\" s=\"${if(ri==0)1 else 0}\"><v>${v}</v></c>");is Boolean->append("<c r=\"$ref\" t=\"b\"><v>${if(v)1 else 0}</v></c>");else->append("<c r=\"$ref\" t=\"inlineStr\" s=\"${if(ri==0)1 else 0}\"><is><t xml:space=\"preserve\">${esc(v?.toString().orEmpty())}</t></is></c>")};};append("</row>")};append("</sheetData></worksheet>")}
 private fun workbook(s:List<XlsxSheet>)="<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><bookViews><workbookView/></bookViews><sheets>"+s.mapIndexed{i,x->"<sheet name=\"${escAttr(x.name.take(31).ifBlank{"Sheet${i+1}"})}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"}.joinToString("")+"</sheets></workbook>"
 private fun workbookRels(n:Int)="<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+(1..n).joinToString(""){"<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>"}+"<Relationship Id=\"rId${n+1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>"
 private fun relsRoot()="<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"
 private fun contentTypes(n:Int)="<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"+(1..n).joinToString(""){"<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>"
 private fun styles()="<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Arial\"/></font><font><b/><sz val=\"11\"/><name val=\"Arial\"/></font></fonts><fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"2\"><xf fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs></styleSheet>"
 private fun col(i:Int):String{var n=i+1;var r="";while(n>0){r=('A'.code+(n-1)%26).toChar()+r;n=(n-1)/26};return r}
 private fun esc(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").filter{it=='\n'||it=='\t'||it.code>=32}
 private fun escAttr(v:String)=esc(v).replace("\"","&quot;")
}
