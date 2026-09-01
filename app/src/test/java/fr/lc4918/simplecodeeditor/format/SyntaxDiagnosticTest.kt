package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.SyntaxProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyntaxDiagnosticTest {

    private fun jsonProblem(source: String) = JsonTree.read(source).diagnostic

    private fun xmlProblem(source: String) = XmlTree.read(source).diagnostic

    @Test
    fun `a document that reads has nothing to report`() {
        assertNull(JsonTree.read("""{"a":1}""").diagnostic)
        assertNull(XmlTree.read("<a><b/></a>").diagnostic)
    }

    @Test
    fun `a missing value is reported where it was expected`() {
        val problem = jsonProblem("""{"a":}""")!!

        assertEquals(SyntaxProblem.UNEXPECTED_CHARACTER, problem.problem)
        assertEquals(5, problem.offset)
        assertEquals(1, problem.line)
        assertEquals(6, problem.column)
    }

    @Test
    fun `a string left open points at its opening quote`() {
        val problem = jsonProblem("""{"a":"never closed""")!!

        assertEquals(SyntaxProblem.UNTERMINATED_STRING, problem.problem)
        assertEquals(5, problem.offset)
    }

    @Test
    fun `a document that stops short says so`() {
        assertEquals(SyntaxProblem.END_OF_DOCUMENT, jsonProblem("""{"a":1""")!!.problem)
    }

    @Test
    fun `content after the document is reported at what follows`() {
        val problem = jsonProblem("""{"a":1} and more""")!!

        assertEquals(SyntaxProblem.TRAILING_CONTENT, problem.problem)
        assertEquals(8, problem.offset)
    }

    @Test
    fun `a line break moves the report to the next line`() {
        val problem = jsonProblem("{\n  \"a\": nope\n}")!!

        assertEquals(2, problem.line)
        // The n of nope, which is where the value should have started.
        assertEquals(8, problem.column)
    }

    @Test
    fun `a missing colon is told apart from a missing separator`() {
        assertEquals(SyntaxProblem.EXPECTED_COLON, jsonProblem("""{"a" 1}""")!!.problem)
        assertEquals(SyntaxProblem.EXPECTED_SEPARATOR, jsonProblem("""{"a":1 "b":2}""")!!.problem)
    }

    @Test
    fun `a bad number is reported where it starts`() {
        val problem = jsonProblem("""[1.2.3]""")!!

        assertEquals(SyntaxProblem.BAD_NUMBER, problem.problem)
        assertEquals(1, problem.offset)
    }

    @Test
    fun `an element left open is reported at the end of the document`() {
        val source = "<a><b>"
        val problem = xmlProblem(source)!!

        assertEquals(SyntaxProblem.UNCLOSED_ELEMENT, problem.problem)
        assertEquals(source.length, problem.offset)
    }

    @Test
    fun `a closing tag that does not match is reported where it opens`() {
        val problem = xmlProblem("<a><b></a></b>")!!

        assertEquals(SyntaxProblem.MISMATCHED_CLOSING_TAG, problem.problem)
        assertEquals(6, problem.offset)
    }

    @Test
    fun `a closing tag with nothing to close is reported`() {
        assertEquals(SyntaxProblem.UNEXPECTED_CLOSING_TAG, xmlProblem("</a>")!!.problem)
    }

    @Test
    fun `a tag left open is reported where it starts`() {
        val problem = xmlProblem("<a><b")!!

        assertEquals(SyntaxProblem.UNTERMINATED_TAG, problem.problem)
        assertEquals(3, problem.offset)
    }

    @Test
    fun `a comment left open is reported`() {
        assertEquals(SyntaxProblem.UNTERMINATED_MARKUP, xmlProblem("<a><!-- open</a>")!!.problem)
    }

    @Test
    fun `an empty document has nothing to read`() {
        assertEquals(SyntaxProblem.END_OF_DOCUMENT, xmlProblem("")!!.problem)
        assertEquals(SyntaxProblem.END_OF_DOCUMENT, jsonProblem("")!!.problem)
    }
}
