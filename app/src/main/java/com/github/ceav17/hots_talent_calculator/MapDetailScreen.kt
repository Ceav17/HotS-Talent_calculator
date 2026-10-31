package com.github.ceav17.hots_talent_calculator

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MapDetailScreen(
    @DrawableRes imageResId: Int,
    @StringRes nameResId: Int,
    @StringRes backLabelResId: Int,
    pageTag: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val title = stringResource(nameResId)

    ResponsiveScreen(
        backgroundResId = R.drawable.storm_ui_draft_background,
        modifier = modifier.testTag(pageTag)
    ) {
        val baseTitleStyle = mapTitleStyle(maxWidth * 0.9f)
        val closeButtonTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        val titleLeft = 24.dp + CalculatorBackIconSize + 12.dp
        val titleWidth = (maxWidth * 0.95f - titleLeft).coerceAtLeast(1.dp)
        val measuredTitleWidth = remember(title, baseTitleStyle, textMeasurer) {
            textMeasurer.measure(title, baseTitleStyle, softWrap = false, maxLines = 1)
                .size.width.coerceAtLeast(1)
        }
        // Long names must also fit beside the close button, not just in the map list.
        val titleScale = minOf(1f, with(density) { titleWidth.toPx() } / measuredTitleWidth)
        val titleStyle = baseTitleStyle.copy(
            fontSize = baseTitleStyle.fontSize * titleScale,
            lineHeight = baseTitleStyle.lineHeight * titleScale
        )
        val titleHeight = maxOf(CalculatorBackIconSize, with(density) { titleStyle.lineHeight.toDp() })
        val titleTop = closeButtonTop + (CalculatorBackIconSize - titleHeight) / 2
        val contentTop = maxOf(closeButtonTop + CalculatorBackIconSize, titleTop + titleHeight) + 16.dp


        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.absoluteOffset(x = titleLeft, y = titleTop).size(titleWidth, titleHeight)
        ) {
            OutlinedText(
                text = title,
                style = titleStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth().testTag("map_detail_title")
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentTop)
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
                .testTag("map_detail_content")
        ) {
            item(key = "map_image") {
                val painter = painterResource(imageResId)
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            content()
        }

        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(backLabelResId),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .absoluteOffset(x = 24.dp, y = closeButtonTop)
                .size(CalculatorBackIconSize)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
        )
    }
}

@Composable
internal fun MapInfo(@DrawableRes imageResId: Int, text: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.widthIn(max = 128.dp).fillMaxWidth().height(76.dp)
        )
        OutlinedText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontFamily = MenuFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
