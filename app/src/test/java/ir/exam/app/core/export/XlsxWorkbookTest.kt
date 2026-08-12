package ir.exam.app.core.export

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxWorkbookTest {
 @Test fun `produces real multi-sheet OOXML with RTL worksheet`() {
  val bytes=XlsxWorkbook.build(listOf(XlsxSheet("نمرات",listOf(listOf("نام","نمره"),listOf("علی",18.5))),XlsxSheet("خلاصه",listOf(listOf("تعداد",1)))))
  assertTrue(bytes.size>1000)
  val entries=mutableMapOf<String,String>();ZipInputStream(ByteArrayInputStream(bytes)).use{zip->while(true){val e=zip.nextEntry?:break;entries[e.name]=zip.readBytes().toString(Charsets.UTF_8)}}
  assertTrue("xl/workbook.xml" in entries);assertTrue("xl/worksheets/sheet2.xml" in entries);assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("rightToLeft=\"1\""));assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("علی"))
 }
}
