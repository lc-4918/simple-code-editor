package fr.lc4918.simplecodeeditor.format

import fr.lc4918.simplecodeeditor.model.FilterOperator
import fr.lc4918.simplecodeeditor.model.SortDirection
import fr.lc4918.simplecodeeditor.model.TreeNode
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonTransformTest {

    private fun tree(source: String) = JsonTree.parse(source)!!

    private fun write(source: String, transform: (TreeNode) -> TreeNode): String =
        JsonWriter.write(transform(tree(source)), width = null)

    @Test
    fun `the members of the elements are offered as fields`() {
        val fields = JsonTransform.fields(tree("""[{"b":1,"a":2},{"c":3}]"""))

        assertEquals(listOf("b", "a", "c"), fields)
    }

    @Test
    fun `an array of plain values offers no field`() {
        assertEquals(emptyList<String>(), JsonTransform.fields(tree("[3,1,2]")))
    }

    @Test
    fun `an array of objects sorts on the chosen member`() {
        val sorted = write("""[{"n":"b","s":9},{"n":"a","s":10}]""") {
            JsonTransform.sort(it, "s", SortDirection.ASCENDING)
        }

        assertEquals("""[{"n":"b","s":9},{"n":"a","s":10}]""", sorted)
    }

    @Test
    fun `an array of plain values sorts on the values themselves`() {
        val sorted = write("[3,1,2]") { JsonTransform.sort(it, null, SortDirection.ASCENDING) }

        assertEquals("[1,2,3]", sorted)
    }

    @Test
    fun `sorting the other way reverses the order`() {
        val sorted = write("[3,1,2]") { JsonTransform.sort(it, null, SortDirection.DESCENDING) }

        assertEquals("[3,2,1]", sorted)
    }

    @Test
    fun `an object sorts its keys`() {
        val sorted = write("""{"b":1,"a":2}""") {
            JsonTransform.sort(it, null, SortDirection.ASCENDING)
        }

        assertEquals("""{"a":2,"b":1}""", sorted)
    }

    @Test
    fun `filtering keeps the elements that match`() {
        val filtered = write("""[{"n":"ada"},{"n":"linus"}]""") {
            JsonTransform.filter(it, "n", FilterOperator.CONTAINS, "ada")
        }

        assertEquals("""[{"n":"ada"}]""", filtered)
    }

    @Test
    fun `filtering an object keeps the keys that match`() {
        val filtered = write("""{"one":1,"two":2}""") {
            JsonTransform.filter(it, null, FilterOperator.CONTAINS, "on")
        }

        assertEquals("""{"one":1}""", filtered)
    }

    @Test
    fun `a number is compared by value and not by spelling`() {
        val sorted = write("[9,10,2]") { JsonTransform.sort(it, null, SortDirection.ASCENDING) }

        assertEquals("[2,9,10]", sorted)
    }
}
