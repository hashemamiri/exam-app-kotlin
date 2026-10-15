package ir.exam.app.ui.app

import ir.exam.app.core.io.SpreadsheetImport
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V263 — پاریتهٔ اپ با سایت V230: بانک مدرسه (اشتراک/کپی)، مقایسهٔ کلاس‌ها در داشبورد مدیر،
 * و ورود گروهی دانش‌آموز از اکسل/CSV در اپ.
 */
class V263_AppParitySchoolBankCompareExcelAppTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String) = File(root(), path).readText()

    @Test
    fun `csv rows map to students like the site`() {
        val csv = "نام,نام خانوادگی,نام پدر,پایه,رشته,جنسیت,نام کاربری,رمز\nعلی,رضایی,حسن,دهم,ریاضی,پسر,ali1402,secret123\nسارا,کریمی,,یازدهم,تجربی,دختر,,"
        val rows = SpreadsheetImport.toStudents(SpreadsheetImport.readRows(csv.toByteArray(Charsets.UTF_8), "students.csv"))
        assertEquals(2, rows.size)
        assertEquals("علی", rows[0].first)
        assertEquals("رضایی", rows[0].last)
        assertEquals("male", rows[0].gender)
        assertEquals("ali1402", rows[0].username)
        assertEquals("secret123", rows[0].password)
        assertEquals("female", rows[1].gender)
        assertEquals("", rows[1].password)
    }

    @Test
    fun `xlsx with shared strings is parsed`() {
        val shared = """<?xml version="1.0" encoding="UTF-8"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>نام</t></si><si><t>نام خانوادگی</t></si><si><t>مینا</t></si><si><t>احمدی</t></si></sst>"""
        val sheet = """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row><row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2" t="s"><v>3</v></c></row></sheetData></worksheet>"""
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("xl/sharedStrings.xml")); zip.write(shared.toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml")); zip.write(sheet.toByteArray()); zip.closeEntry()
            }
        }.toByteArray()
        val rows = SpreadsheetImport.toStudents(SpreadsheetImport.readRows(bytes, "list.xlsx"))
        assertEquals(1, rows.size)
        assertEquals("مینا", rows[0].first)
        assertEquals("احمدی", rows[0].last)
    }

    @Test
    fun `bank repository and view model expose school bank`() {
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseExamBuilderRepository.kt")
        assertTrue("\"native_bank_set_shared_v1\"" in repo)
        assertTrue("\"native_school_bank_v1\"" in repo)
        assertTrue("\"native_bank_copy_from_school_v1\"" in repo)
        assertTrue("shared = row[\"shared\"]?.jsonPrimitive?.booleanOrNull ?: false" in repo)
        val vm = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankViewModel.kt")
        assertTrue("fun setShared(id: Long, shared: Boolean)" in vm)
        assertTrue("fun copyFromSchool(id: Long)" in vm)
        val screen = src("app/src/main/java/ir/exam/app/ui/bank/QuestionBankScreen.kt")
        assertTrue("Text(\"بانک مدرسه\")" in screen)
        assertTrue("Text(\"اشتراک با مدرسه\"" in screen)
        assertTrue("enabled = !item.already" in screen)
    }

    @Test
    fun `manager dashboard compares classes and bulk dialog imports excel`() {
        val manager = src("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt")
        assertTrue("\"native_manager_class_stats_v1\"" in manager)
        assertTrue("Text(\"مقایسهٔ کلاس‌ها\"" in manager)
        assertTrue("StatChip(\"پراکندگی\"" in manager)
        val school = src("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
        assertTrue("SpreadsheetImport.toStudents" in school)
        assertTrue("{ Text(\"اکسل\", maxLines = 1, softWrap = false) }" in school) // V291
        assertTrue("\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"" in school)
    }
}
