package fr.lc4918.simplecodeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.lc4918.simplecodeeditor.ui.theme.CodeTextStyle
import fr.lc4918.simplecodeeditor.ui.theme.LocalEditorColors

/**
 * Editing surface in text mode.
 *
 * This is the plain version of the surface: a numbered gutter next to freely
 * editable monospaced text. Syntax highlighting and block folding arrive with
 * the embedded editor, which replaces the body of this composable.
 */
@Composable
fun EditorTextSurface(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEditorColors.current
    val lineCount = remember(content) { content.count { it == '\n' } + 1 }
    val gutterWidth = gutterWidthFor(lineCount)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.codeBackground),
    ) {
        val viewportHeight = maxHeight

        // Drawn behind the numbers so the gutter colour reaches the bottom of
        // the viewport even when the document is shorter than the screen.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(gutterWidth)
                .background(colors.gutterBackground),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Gutter(lineCount = lineCount, width = gutterWidth)
            BasicTextField(
                value = content,
                onValueChange = onContentChanged,
                // The sideways scroll leaves the width unbounded, so long lines
                // extend instead of wrapping, which keeps the gutter aligned.
                // The minimum height makes the whole empty area focusable.
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = 240.dp)
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 8.dp),
                textStyle = CodeTextStyle.copy(color = colors.codeText),
                cursorBrush = SolidColor(colors.codeText),
            )
        }
    }
}

@Composable
private fun Gutter(lineCount: Int, width: Dp) {
    val colors = LocalEditorColors.current

    Column(modifier = Modifier.width(width).padding(end = 8.dp)) {
        repeat(lineCount) { index ->
            Text(
                text = (index + 1).toString(),
                style = CodeTextStyle,
                color = colors.gutterText,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Wide enough for the highest line number, plus the padding on both sides. */
private fun gutterWidthFor(lineCount: Int): Dp =
    (20 + 9 * lineCount.toString().length).dp
