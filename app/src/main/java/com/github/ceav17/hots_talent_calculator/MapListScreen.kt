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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp

private const val MAP_TITLE_REFERENCE = "Tomb of the Spider Queen"

private data class MapListItem(
    @DrawableRes val imageResId: Int,
    @StringRes val nameResId: Int,
    val route: String
)

private val AramMaps = AramMap.entries.map { MapListItem(it.imageResId, it.nameResId, it.route) }
private val QuickMatchMaps = BattlegroundMap.entries.map { MapListItem(it.imageResId, it.nameResId, it.route) }
private val RankedMaps = BattlegroundMap.entries.filter { it.inRanked }
    .map { MapListItem(it.imageResId, it.nameResId, it.route) }

@Composable
internal fun MapListScreen(
    mode: MapMode,
    onBack: () -> Unit,
    onMapSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maps = when (mode) {
        MapMode.ARAM -> AramMaps
        MapMode.QUICKMATCH -> QuickMatchMaps
        MapMode.RANKED -> RankedMaps
    }
    val heading = stringResource(R.string.maps_list_title, stringResource(mode.labelResId), maps.size)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    ResponsiveScreen(backgroundResId = R.drawable.storm_loading_generic, modifier = modifier) {
        val titleStyle = mapTitleStyle(maxWidth * 0.9f)
        val closeButtonTop = calculatorBackIconOffset(maxWidth, maxHeight).y
        val headingLeft = 24.dp + CalculatorBackIconSize + 12.dp
        val headingWidth = (maxWidth * 0.95f - headingLeft).coerceAtLeast(1.dp)
        val measuredHeadingWidth = remember(heading, titleStyle, textMeasurer) {
            textMeasurer.measure(heading, titleStyle, softWrap = false, maxLines = 1)
                .size.width.coerceAtLeast(1)
        }
        val headingScale = minOf(1f, with(density) { headingWidth.toPx() } / measuredHeadingWidth)
        val headingStyle = titleStyle.copy(
            fontSize = titleStyle.fontSize * headingScale,
            lineHeight = titleStyle.lineHeight * headingScale
        )
        val headingHeight = maxOf(CalculatorBackIconSize, with(density) { headingStyle.lineHeight.toDp() })
        val headingTop = closeButtonTop + (CalculatorBackIconSize - headingHeight) / 2
        val listTop = maxOf(closeButtonTop + CalculatorBackIconSize, headingTop + headingHeight) + 16.dp
        val columns = if (maxWidth >= 720.dp) 2 else 1
        val cardTitleStyle = mapTitleStyle((maxWidth * 0.9f - 16.dp * (columns - 1)) / columns)


        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.absoluteOffset(x = headingLeft, y = headingTop).size(headingWidth, headingHeight)
        ) {
            OutlinedText(
                text = heading,
                style = headingStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = listTop)
                .fillMaxHeight()
                .fillMaxWidth(0.9f)
        ) {
            items(maps, key = { it.route }) { (imageResId, nameResId, route) ->
                val painter = painterResource(imageResId)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = { onMapSelected(route) }
                    )
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    OutlinedText(
                        text = stringResource(nameResId),
                        style = cardTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.storm_ui_glues_button_close_normal),
            contentDescription = stringResource(R.string.maps_back),
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
internal fun mapTitleStyle(availableWidth: Dp): TextStyle {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = TextStyle(
        color = Color.White,
        fontFamily = MenuFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    )
    val referenceWidth = remember(textMeasurer, density) {
        textMeasurer.measure(
            MAP_TITLE_REFERENCE, titleStyle, softWrap = false, maxLines = 1
        ).size.width.coerceAtLeast(1)
    }
    // Fit long map names without enlarging the text indefinitely on wide screens.
    val fontSize = with(density) {
        (32.sp.toPx() * minOf(1f, availableWidth.toPx() / referenceWidth)).toSp()
    }
    return titleStyle.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f)
}
