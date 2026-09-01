package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.format.CodeFormatter.Dialect
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeFormatterTest {

    @Test
    fun `a style rule is laid out over several lines`() {
        val formatted = CodeFormatter.indent("body{color:red;margin:0}", Dialect.STYLE, width = 2)

        assertEquals(
            """
            body {
              color: red;
              margin: 0
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `a colon in a selector keeps its place`() {
        val formatted = CodeFormatter.indent("a:hover{color:red;}", Dialect.STYLE, width = 2)

        assertEquals("a:hover {\n  color: red;\n}", formatted)
    }

    @Test
    fun `a semicolon inside parentheses does not break the line`() {
        val source = "for (let i = 0; i < 2; i++) { work(); }"
        val formatted = CodeFormatter.indent(source, Dialect.SCRIPT, width = 2)

        assertEquals("for (let i = 0; i < 2; i++) {\n  work();\n}", formatted)
    }

    @Test
    fun `a brace inside a string is not a block`() {
        val formatted = CodeFormatter.indent("""a { content: "{" }""", Dialect.STYLE, width = 2)

        assertEquals("a {\n  content: \"{\"\n}", formatted)
    }

    @Test
    fun `a comment is copied over as it stands`() {
        val formatted = CodeFormatter.indent("a{/* keep  me */ color:red;}", Dialect.STYLE, width = 2)

        assertEquals("a {\n  /* keep  me */ color: red;\n}", formatted)
    }

    @Test
    fun `a template literal keeps its own line breaks`() {
        val source = "const a = `line\n  two`;"
        val formatted = CodeFormatter.indent(source, Dialect.SCRIPT, width = 2)

        assertEquals("const a = `line\n  two`;", formatted)
    }

    @Test
    fun `a brace inside a regular expression is not a block`() {
        val source = "const re = /a{2}/g; work();"
        val formatted = CodeFormatter.indent(source, Dialect.SCRIPT, width = 2)

        assertEquals("const re = /a{2}/g;\nwork();", formatted)
    }

    @Test
    fun `a slash after a value divides rather than opening a literal`() {
        val source = "const half = total / 2; const rest = other / 3;"
        val formatted = CodeFormatter.indent(source, Dialect.SCRIPT, width = 2)

        assertEquals("const half = total / 2;\nconst rest = other / 3;", formatted)
    }

    @Test
    fun `a comment to the end of the line does not swallow the next one`() {
        val source = "const a = 1; // note\nconst b = 2;"
        val formatted = CodeFormatter.indent(source, Dialect.SCRIPT, width = 2)

        assertEquals("const a = 1;\n// note\nconst b = 2;", formatted)
    }

    @Test
    fun `compacting a style sheet drops every optional space`() {
        val source = """
            body {
              color: red;
            }
        """.trimIndent()

        assertEquals("body{color:red;}", CodeFormatter.compact(source, Dialect.STYLE))
    }

    @Test
    fun `compacting a script keeps the line breaks that end a statement`() {
        val source = "const a = 1\nconst b = 2"

        assertEquals("const a=1\nconst b=2", CodeFormatter.compact(source, Dialect.SCRIPT))
    }

    @Test
    fun `an unterminated comment leaves the document untouched`() {
        val broken = "a { /* never closed"

        assertEquals(broken, CodeFormatter.indent(broken, Dialect.STYLE, width = 2))
        assertEquals(broken, CodeFormatter.compact(broken, Dialect.STYLE))
    }

    @Test
    fun `indenting is stable when run twice`() {
        val once = CodeFormatter.indent("a{b:c;d{e:f;}}", Dialect.STYLE, width = 2)

        assertEquals(once, CodeFormatter.indent(once, Dialect.STYLE, width = 2))
    }
}
