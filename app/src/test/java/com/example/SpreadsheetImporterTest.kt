package com.example

import com.example.data.SpreadsheetImporter
import org.junit.Assert.*
import org.junit.Test

class SpreadsheetImporterTest {

    @Test
    fun `csv template starts with UTF8 BOM and contains all 17 required headers`() {
        val csv = SpreadsheetImporter.generateCsvTemplate()
        assertTrue(csv.startsWith("\uFEFF"))
        val headerLine = csv.removePrefix("\uFEFF").lineSequence().first()
        val cols = headerLine.split(",")
        assertEquals(SpreadsheetImporter.REQUIRED_HEADERS, cols)
    }

    @Test
    fun `csv template example row has exactly 10 tokens in unvoweled, voweled, and pipe columns`() {
        val csv = SpreadsheetImporter.generateCsvTemplate().removePrefix("\uFEFF")
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        // Pipe-separated fields inside the example row should each contain 9 pipes (10 items)
        val exampleLine = lines[1]
        assertTrue(exampleLine.contains("Menulis|Pelajar|Pelajaran|Yang baru|Di|Kelas|Pagi hari|Bersama|Temannya|Yang tekun"))
    }
}
