package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.R
import fr.lc4918.simplecodeeditor.format.CsvParser
import fr.lc4918.simplecodeeditor.model.CsvTable
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors

private val CELL_WIDTH = 140.dp
private val ROW_HEIGHT = 44.dp

/**
 * The CSV document as a grid of rows and columns.
 *
 * The header and the rows share one sideways scroll, which is what keeps the
 * columns lined up under their titles. The row numbers stay put beside them,
 * as the line numbers do in text mode.
 */
@Composable
fun CsvTableSurface(
    content: String,
    onCellChanged: (row: Int, column: Int, value: String) -> Unit,
    onAddRow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    val table = remember(content) { CsvParser.parse(content) }

    if (table == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.table_not_readable),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val columns = table.columnCount
    val gutterWidth = (28 + 8 * table.rows.size.toString().length).dp
    val horizontal = rememberScrollState()

    Column(modifier = modifier.fillMaxSize().background(colors.codeBackground)) {
        HeaderRow(
            table = table,
            columns = columns,
            gutterWidth = gutterWidth,
            scroll = horizontal,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(table.rows.size) { row ->
                DataRow(
                    table = table,
                    row = row,
                    columns = columns,
                    gutterWidth = gutterWidth,
                    scroll = horizontal,
                    onCellChanged = onCellChanged,
                )
            }
            item {
                TextButton(onClick = onAddRow, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.table_add_row),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    table: CsvTable,
    columns: Int,
    gutterWidth: Dp,
    scroll: ScrollState,
) {
    val colors = LocalEditorColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.gutterBackground)
            .height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(gutterWidth))
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            repeat(columns) { column ->
                Text(
                    text = table.columnName(column),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.codeText,
                    maxLines = 1,
                    modifier = Modifier.width(CELL_WIDTH).padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DataRow(
    table: CsvTable,
    row: Int,
    columns: Int,
    gutterWidth: Dp,
    scroll: ScrollState,
    onCellChanged: (row: Int, column: Int, value: String) -> Unit,
) {
    val colors = LocalEditorColors.current

    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A band rather than a chip: the gutter has to run the height of the
        // row, as the one beside the line numbers does in text mode.
        Box(
            modifier = Modifier
                .width(gutterWidth)
                .fillMaxHeight()
                .background(colors.gutterBackground),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = (row + 1).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.gutterText,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            repeat(columns) { column ->
                TableCell(
                    value = table.cell(row, column),
                    onValueCommitted = { value -> onCellChanged(row, column, value) },
                )
            }
        }
    }
}

/**
 * One editable cell.
 *
 * What is typed is kept here until the cell is left, because handing every
 * keystroke over would rewrite the whole document and read the grid back.
 */
@Composable
private fun TableCell(
    value: String,
    onValueCommitted: (String) -> Unit,
) {
    val colors = LocalEditorColors.current
    val focusManager = LocalFocusManager.current
    var text by remember(value) { mutableStateOf(value) }
    var wasFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .width(CELL_WIDTH)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .onFocusChanged { state ->
                if (wasFocused && !state.isFocused && text != value) onValueCommitted(text)
                wasFocused = state.isFocused
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.codeText),
        cursorBrush = SolidColor(colors.codeText),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                onValueCommitted(text)
                focusManager.clearFocus()
            },
        ),
    )
}
