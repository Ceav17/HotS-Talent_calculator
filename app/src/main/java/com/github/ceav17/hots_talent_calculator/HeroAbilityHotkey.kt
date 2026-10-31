package com.github.ceav17.hots_talent_calculator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

@Composable
internal fun Modifier.heroAbilityHotkey(
    hotkey: String,
    iconSize: Dp,
    color: Color = Color.White,
    horizontalAnchor: Float = 0.28f,
    verticalAnchor: Float = 0.75f,
    fontScale: Float = 0.25f
): Modifier {
    val textMeasurer = rememberTextMeasurer()
    return drawWithCache {
        val label = textMeasurer.measure(
            text = hotkey,
            style = TextStyle(
                fontFamily = MenuFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (iconSize.toPx() * fontScale).toSp()
            ),
            maxLines = 1,
            softWrap = false
        )
        val position = Offset(
            x = size.width * horizontalAnchor - label.size.width / 2f,
            y = size.height * verticalAnchor - label.size.height / 2f
        )
        val outline = Stroke(width = iconSize.toPx() * fontScale * 0.14f)
        onDrawWithContent {
            drawContent()
            drawText(label, color = Color.Black, topLeft = position, drawStyle = outline)
            // Reset the paint style after the outline pass.
            drawText(label, color = color, topLeft = position, drawStyle = Fill)
        }
    }
}
