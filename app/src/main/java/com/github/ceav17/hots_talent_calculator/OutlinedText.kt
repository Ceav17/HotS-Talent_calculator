package com.github.ceav17.hots_talent_calculator

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp

@Composable
internal fun OutlinedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true
) {
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    // Reuse Text's layout and semantics; only replace the glyph painting.
    Text(
        text = text,
        style = style,
        maxLines = maxLines,
        softWrap = softWrap,
        onTextLayout = { layout.value = it },
        modifier = modifier.drawWithCache {
            val outline = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
            onDrawWithContent {
                val label = layout.value
                if (label == null) {
                    drawContent()
                } else {
                    drawText(label, color = Color.Black, drawStyle = outline)
                    // Reset the paragraph paint to filled glyphs after drawing the outline.
                    drawText(label, color = label.layoutInput.style.color, drawStyle = Fill)
                }
            }
        }
    )
}
